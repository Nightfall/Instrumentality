/*
 * Copyright (c) 2015, Nightfall Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package moe.nightfall.instrumentality.mc

import java.io.IOException

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.eventhandler.{EventPriority, SubscribeEvent}
import cpw.mods.fml.common.gameevent.{InputEvent, TickEvent}
import moe.nightfall.instrumentality.ModelCache.IPMXLocator
import moe.nightfall.instrumentality.animations.AnimSet
import moe.nightfall.instrumentality.mc.gui.EditorHostGui
import moe.nightfall.instrumentality.mc.network.{RequestFileMessage, SendSHAMessage}
import moe.nightfall.instrumentality.{Loader, ModelCache, PMXModel}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.{RenderHandEvent, RenderPlayerEvent}
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import org.lwjgl.input.Keyboard

import scala.concurrent.Lock

object ClientProxy {

    var editorBinding: KeyBinding = null

    // Assigns usernames to models.
    val knownModels: collection.concurrent.TrieMap[String, MHolder] = collection.concurrent.TrieMap()
    // Used when the system is serving data to find out where data is
    val hashToFile: collection.concurrent.TrieMap[String, String] = collection.concurrent.TrieMap()

    // Used by network code, could be called from other threads(!)
    def updateRemoteModel(user: String, dataManifest: Map[String, String], animSet: AnimSet) {
        if (dataManifest == null) {
            ClientProxy.knownModels += user -> new ClientProxy.MHolder(null, null)
            InstanceCache.queueChange(user, null, animSet)
            return
        }
        new Thread {
            override def run() {
                val pm = ModelCache.getByManifest(dataManifest, new IPMXLocator {
                    override def getData(hash: String): Array[Byte] = blockingReceiveData(hash)
                })
                if (pm == null)
                    System.err.println("Download of model was unsuccessful!")
                ClientProxy.knownModels += user -> new ClientProxy.MHolder(pm, animSet)
                InstanceCache.queueChange(user, pm, animSet)
            }
        }.start()
    }

    def getSelfSHA: SendSHAMessage = {
        // Hello server, we're an MMC user!
        val msg = new SendSHAMessage()
        msg.player = Minecraft.getMinecraft.thePlayer.getCommandSenderName
        msg.dataManifest = if (Loader.currentFile != null) {
            val fth = ModelCache.createManifestForLocal(Loader.currentFile)
            val nbt = new NBTTagCompound()
            for ((k, v) <- fth.filesToHashes) {
                hashToFile += v -> (ModelCache.modelRepository + "/" + Loader.currentFile + "/" + k)
                nbt.setString(k, v)
            }
            Some(nbt, fth.animSet)
        } else {
            None
        }
        msg
    }

    def blockingReceiveData(hash: String): Array[Byte] = {
        var dataReceived: Option[Array[Byte]] = None
        var seq: Array[RequestFileMessage] = null
        var seqLock = new Lock()
        MikuMikuCraft.proxy.clientCallbackOnData.put((hash, (pkt) => {
            System.out.println(hash + "==" + pkt.fileHash + ":" + pkt.seqNum + " size " + pkt.seqSize)
            // TODO: Adjust this. Right now: 32kb * 1024 == 32MB.
            if (pkt.seqSize > 1024) {
                // This person's playing a joke on us
                true
            } else {
                seqLock.acquire()
                if (seq == null)
                    seq = new Array[RequestFileMessage](pkt.seqSize)
                if ((pkt.seqNum < 0) || (pkt.seqNum >= seq.length)) {
                    seqLock.release()
                    true
                } else {
                    seq(pkt.seqNum) = pkt
                    val result = seq.filter(_ == null).size == 0
                    if (result) {
                        // before killing the callback, put everything into place
                        var totalSize = 0
                        seq.foreach((rfm) => totalSize += rfm.fileData.get.length)
                        val data = new Array[Byte](totalSize)
                        var pos = 0
                        seq.foreach((rfm) => {
                            rfm.fileData.get.copyToArray(data, pos)
                            pos += rfm.fileData.get.length
                        })
                        dataReceived = Some(data)
                    }
                    seqLock.release()
                    result
                }
            }
            false
        }), Unit)
        val req = new RequestFileMessage()
        req.fileData = None
        req.fileHash = hash
        MikuMikuCraft.mikuNet.sendToServer(req)
        var tries = 0
        // Apparently Minecraft thinks it's the only class in the universe that needs to shutdown. IT'S WRONG!
        // As it is, since I can't get information on if Minecraft is running,
        // here's the workaround: we're not communicating with the server when the world is null anyway,
        // and it's kind enough to set it to null while shutting down.
        // So this is the best compromise I can think of.
        while ((tries < 200) & (Minecraft.getMinecraft.theWorld != null)) {
            tries += 1
            Thread.sleep(100)
            val res = dataReceived
            if (res.isDefined)
                return res.get
        }
        throw new IOException("Retrieval timed out of " + hash)
    }

    class MHolder(val held: PMXModel, val heldSet: AnimSet)
}

class ClientProxy extends CommonProxy {

    override def preInit() {
        super.preInit
        ClientProxy.editorBinding = new KeyBinding("key.mmc_editor", Keyboard.KEY_EQUALS, "key.categories.mikumikucraft")
        ClientRegistry.registerKeyBinding(ClientProxy.editorBinding)

        Loader.setup(new MinecraftApplicationHost)
        Loader.currentFileListeners += (() => {
            MikuMikuCraft.mikuNet.sendToServer(ClientProxy.getSelfSHA)
        })
    }

    /*
	 * private void sendMMCAnnounce(String targ) { // TODO: Delegate for other
	 * comms protocols! sendMMCChatAnnounce(targ); }
	 * 
	 * private void sendMMCChatAnnounce(String targ) { String pmxHash = null;
	 * String cf = Loader.currentFile; String prefix = "/me changes into "; if
	 * (targ != null) { prefix = "/msg " + targ + " reply:"; } if (cf != null) {
	 * try { pmxHash =
	 * ModelCache.createManifestForLocal(cf).filesToHashes.get("mdl.pmx"); }
	 * catch (IOException ioe) {
	 * System.err.println("Cannot send MMC-Chat protocol message:");
	 * ioe.printStackTrace(); } } Minecraft mc = Minecraft.getMinecraft(); if
	 * (mc.thePlayer != null) if (pmxHash != null) { int l = 9; if
	 * (Loader.currentFile.length() < 9) l = Loader.currentFile.length();
	 * mc.thePlayer.sendChatMessage(prefix + Loader.currentFile.substring(0, l)
	 * + "¬" + pmxHash + ":" + mc.thePlayer.getDisplayName() + ":set:" +
	 * protocolId); } else { mc.thePlayer.sendChatMessage(prefix + "normal¬" +
	 * mc.thePlayer.getDisplayName() + ":unset:" + protocolId); } }
	 * 
	 * @SubscribeEvent public void onEntityJoin(EntityJoinWorldEvent ejwe) { if
	 * (ejwe.entity instanceof EntityPlayer) { EntityPlayer ep = (EntityPlayer)
	 * ejwe.entity; Minecraft m = Minecraft.getMinecraft(); if (m.thePlayer ==
	 * null) return; // we are *not* going to query ourselves! if
	 * (((EntityPlayer)
	 * ejwe.entity).getDisplayName().equalsIgnoreCase(m.thePlayer
	 * .getDisplayName())) return; if (m.thePlayer != ejwe.entity) { // Ok, this
	 * is another player. Do we know their model? MHolder em =
	 * knownModels.get(ep.getDisplayName()); if (em != null) { // Yes, we do!
	 * InstanceCache.setModel(ep, em.held); } else { // We don't - query them
	 * knownModels.put(ep.getDisplayName(), new MHolder(null));
	 * m.thePlayer.sendChatMessage("/msg " + ep.getDisplayName() +
	 * " MMC query(ignore) ¬:" + m.thePlayer.getDisplayName() + ":query:" +
	 * protocolId); } } } }
	 * 
	 * @SubscribeEvent public void onIncomingChat(ClientChatReceivedEvent event)
	 * { // MMC-Chat uses emotes to send change announces, this makes things
	 * partially easier and partially harder // It *also* uses private messages
	 * to send queries/responses to queries. // It's basically an inter-client
	 * protocol. String uText = event.message.getUnformattedText(); if
	 * (uText.endsWith(":" + protocolId)) { String cmd =
	 * uText.substring(uText.lastIndexOf((int) '¬') + 1); String[] args =
	 * cmd.split(":"); if (args.length >= 3) { cmd = args[args.length - 2];
	 * String from = args[args.length - 3]; if
	 * (from.equalsIgnoreCase(Minecraft.getMinecraft
	 * ().thePlayer.getDisplayName())) return; // nope! if
	 * (cmd.equalsIgnoreCase("unset")) updateRemoteModel(args[args.length - 3],
	 * null, null); if (cmd.equalsIgnoreCase("set")) if (args.length >= 4)
	 * updateRemoteModel(args[args.length - 3], miniDM(args[args.length - 4]),
	 * null); if (cmd.equalsIgnoreCase("query"))
	 * sendMMCAnnounce(args[args.length - 3]); } } }
	 */

    @SubscribeEvent
    override def onEntityJoin(entityJoinWorldEvent: EntityJoinWorldEvent): Unit = {
        // avoid making requests to localhost
        // if server wants a SHA, we'll know!
        if (entityJoinWorldEvent.entity != Minecraft.getMinecraft.thePlayer)
            super.onEntityJoin(entityJoinWorldEvent)
    }

    @SubscribeEvent
    def onKeyInput(event: InputEvent.KeyInputEvent) {
        if (ClientProxy.editorBinding.isPressed()) {
            Minecraft.getMinecraft.displayGuiScreen(new EditorHostGui)
        }
    }

    private def interpolate(last: Double, current: Double, partialTicks: Float) = last + (current - last) * partialTicks

    @SubscribeEvent
    def onTickRender(rte: TickEvent.RenderTickEvent) {
        if (rte.phase == TickEvent.Phase.START) {
            //TODO: Are we sure this is accurate? The animations are randomly speeding up/slowing down on occasion, can't work out if the timer's off or the anims are.
            InstanceCache.update(rte.renderTickTime / 20d)
        }
    }
    
    private def cachedModel(player : EntityPlayer) : Option[ModelCacheEntry] = {
        var mce = InstanceCache.getModel(player)
        if (mce == null) {
            if (player == Minecraft.getMinecraft.thePlayer) {

                var newMdl: PMXModel = null
                var newAni: AnimSet = null
                if (Loader.currentFile != null) {
                    newMdl = ModelCache.getLocal(Loader.currentFile)
                    newAni = newMdl.defaultAnims
                }
                mce = InstanceCache.setModel(player, newMdl, newAni)
                val mceF = mce

                // InstanceCache will automatically delete any currentFile hook
                // we leave here
                mce.cfHook = () => {
                    var newMdl: PMXModel = null
                    var newAni: AnimSet = null
                    if (Loader.currentFile != null) {
                        newMdl = ModelCache.getLocal(Loader.currentFile)
                        newAni = newMdl.defaultAnims
                    }
                    val ep = mceF.playerRef.get()
                    if (ep != null)
                        InstanceCache.setModel(ep, newMdl, newAni)
                }

                Loader.currentFileListeners += mce.cfHook
            } else return None
        }
        return Some(mce)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    def onPlayerRender(event: RenderPlayerEvent.Pre) {
        val player = event.entityPlayer

        val cache = cachedModel(player)
        if (cache.isEmpty) return // We don't have anything to render
        val model = cache.get.value
        if (model == null) return
        // TODO I don't see why this would be needed. If the model is null, the cache should be empty as well.
        //      Not true. The model can be null if that player WANTS to be Steve.
        //      The cache is only empty if we don't actually know what that player is.

        var x = interpolate(player.lastTickPosX, player.posX, event.partialRenderTick)
        var y = interpolate(player.lastTickPosY, player.posY, event.partialRenderTick)
        var z = interpolate(player.lastTickPosZ, player.posZ, event.partialRenderTick)

        x -= RenderManager.renderPosX
        y -= RenderManager.renderPosY
        z -= RenderManager.renderPosZ

        // Animation time tends to randomly slow down / speed up in-game. I'm blaming partialRenderTick.
        model.apply(player, event.partialRenderTick)
        model.render(player, x, y, z, event.partialRenderTick, false)

        event.setCanceled(true)
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    def onFirstPersonRender(event : RenderHandEvent) {
        
        val player = Minecraft.getMinecraft.thePlayer
        if (player == null) return
        if (Minecraft.getMinecraft.gameSettings.thirdPersonView != 0) return
        val cache = cachedModel(player)
        if (cache.isEmpty) return // We don't have anything to render
        val model = cache.get.value
        if (model == null) return
        model.apply(player, event.partialTicks)

        model.render(player, 0, 0, 0, event.partialTicks, true)
        
        event.setCanceled(true)
    }
}
