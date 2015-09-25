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

;

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.eventhandler.{EventPriority, SubscribeEvent}
import cpw.mods.fml.common.gameevent.{InputEvent, TickEvent}
import moe.nightfall.instrumentality.{Loader, ModelCache, PMXModel}
import moe.nightfall.instrumentality.mc.gui.EditorHostGui
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.event.RenderPlayerEvent
import org.lwjgl.input.Keyboard
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.RenderHandEvent
import org.lwjgl.opengl.GL11
import net.minecraft.client.renderer.entity.RenderPlayer

object ClientProxy {
    val protocolId = "MMC"
    var editorBinding: KeyBinding = null

    // Assigns usernames to models.
    val knownModels: collection.concurrent.TrieMap[String, MHolder] = collection.concurrent.TrieMap()

    class MHolder(val held: PMXModel)

}

class ClientProxy extends CommonProxy {

    override def preInit() {
        super.preInit
        ClientProxy.editorBinding = new KeyBinding("key.mmc_editor", Keyboard.KEY_EQUALS, "key.categories.mikumikucraft")
        ClientRegistry.registerKeyBinding(ClientProxy.editorBinding)

        Loader.setup()
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

    private def miniDM(arg: String): Map[String, String] = {
        return Map("mdl.pmx" -> arg)
    }

    private def updateRemoteModel(user: String, dataManifest: Map[String, String], serv: ModelCache.IPMXLocator) {
        val ep = Minecraft.getMinecraft.theWorld.getPlayerEntityByName(user);
        if (dataManifest == null) {
            ClientProxy.knownModels += user -> new ClientProxy.MHolder(null)
            if (ep != null)
                InstanceCache.setModel(ep, null)
            return
        }
        new Thread {
            val pm = ModelCache.getByManifest(dataManifest, serv);
            ClientProxy.knownModels += user -> new ClientProxy.MHolder(pm)
            val ep2 = Minecraft.getMinecraft.theWorld.getPlayerEntityByName(user);
            //				if (ep2 != null)
            //					InstanceCache.queueChange(ep2, pm);
        }.start()
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
        if (rte.phase == TickEvent.Phase.START)
            InstanceCache.update(rte.renderTickTime / 20d)
    }
    
    private def cachedModel(player : EntityPlayer) : Option[ModelCacheEntry] = {
        var mce = InstanceCache.getModel(player)
        if (mce == null) {
            if (player == Minecraft.getMinecraft.thePlayer) {

                var newMdl: PMXModel = null
                if (Loader.currentFile != null)
                    newMdl = ModelCache.getLocal(Loader.currentFile)
                mce = InstanceCache.setModel(player, newMdl)
                val mceF = mce

                // InstanceCache will automatically delete any currentFile hook
                // we leave here
                mce.cfHook = () => {
                    var newMdl: PMXModel = null
                    if (Loader.currentFile != null)
                        newMdl = ModelCache.getLocal(Loader.currentFile)
                    val ep = mceF.playerRef.get()
                    if (ep != null)
                        InstanceCache.setModel(ep, newMdl)
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
        if (model == null) return // TODO I don't see why this would be needed.
        
        var x = interpolate(player.lastTickPosX, player.posX, event.partialRenderTick)
        var y = interpolate(player.lastTickPosY, player.posY, event.partialRenderTick)
        var z = interpolate(player.lastTickPosZ, player.posZ, event.partialRenderTick)

        x -= RenderManager.renderPosX
        y -= RenderManager.renderPosY
        z -= RenderManager.renderPosZ


        model.apply(player, event.partialRenderTick)
        model.render(player, x, y, z, 0, event.partialRenderTick)

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
        if (model == null) return // TODO I don't see why this would be needed. If the model is null, the cache should be empty as well.
        
        model.apply(player, event.partialTicks)
        
        // TODO Need to fix the camera, this differs from model to model.
        model.render(player, 0, 0, 0, 2.25, event.partialTicks)
        
        event.setCanceled(true)
    }
}
