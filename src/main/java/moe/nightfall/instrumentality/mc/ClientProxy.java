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
package moe.nightfall.instrumentality.mc;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import moe.nightfall.instrumentality.Loader;
import moe.nightfall.instrumentality.ModelCache;
import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.mc.gui.EditorHostGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ClientProxy extends CommonProxy {
    public static final String protocolId = "MMC";

    public static KeyBinding editorBinding;

    // Assigns usernames to models.
    public static ConcurrentHashMap<String, MHolder> knownModels = new ConcurrentHashMap<String, MHolder>();

    @Override
    public void preInit() {
        super.preInit();
        editorBinding = new KeyBinding("key.mmc_editor", Keyboard.KEY_EQUALS, "key.categories.mikumikucraft");
        ClientRegistry.registerKeyBinding(editorBinding);
        try {
            Loader.setup();
            Loader.currentFileListeners.add(new Runnable() {
                @Override
                public void run() {
                    sendMMCChatAnnounce(null);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMMCAnnounce(String targ) {
        // TODO: Delegate for other comms protocols!
        sendMMCChatAnnounce(targ);
    }

    private void sendMMCChatAnnounce(String targ) {
        String pmxHash = null;
        String cf = Loader.currentFile;
        String prefix = "/me changes into ";
        if (targ != null) {
            prefix = "/msg " + targ + " reply:";
        }
        if (cf != null) {
            try {
                pmxHash = ModelCache.createManifestForLocal(cf).filesToHashes.get("mdl.pmx");
            } catch (IOException ioe) {
                System.err.println("Cannot send MMC-Chat protocol message:");
                ioe.printStackTrace();
            }
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null)
            if (pmxHash != null) {
                mc.thePlayer.sendChatMessage(prefix + Loader.currentFile.substring(0, 9) + "¬" + pmxHash + ":" + mc.thePlayer.getDisplayName() + ":set:" + protocolId);
            } else {
                mc.thePlayer.sendChatMessage(prefix + "normal¬" + mc.thePlayer.getDisplayName() + ":unset:" + protocolId);
            }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent ejwe) {
        if (ejwe.entity instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer) ejwe.entity;
            Minecraft m = Minecraft.getMinecraft();
            if (m.thePlayer == null)
                return;
            // we are *not* going to query ourselves!
            if (((EntityPlayer) ejwe.entity).getDisplayName().equalsIgnoreCase(m.thePlayer.getDisplayName()))
                return;
            if (m.thePlayer != ejwe.entity) {
                // Ok, this is another player. Do we know their model?
                MHolder em = knownModels.get(ep.getDisplayName());
                if (em != null) {
                    // Yes, we do!
                    InstanceCache.setModel(ep, em.held);
                } else {
                    // We don't - query them
                    knownModels.put(ep.getDisplayName(), new MHolder(null));
                    m.thePlayer.sendChatMessage("/msg " + ep.getDisplayName() + " MMC query(ignore) ¬:" + m.thePlayer.getDisplayName() + ":query:" + protocolId);
                }
            }
        }
    }

    @SubscribeEvent
    public void onIncomingChat(ClientChatReceivedEvent event) {
        // MMC-Chat uses emotes to send change announces, this makes things partially easier and partially harder
        // It *also* uses private messages to send queries/responses to queries.
        // It's basically an inter-client protocol.
        String uText = event.message.getUnformattedText();
        if (uText.endsWith(":" + protocolId)) {
            String cmd = uText.substring(uText.lastIndexOf((int) '¬') + 1);
            String[] args = cmd.split(":");
            if (args.length >= 3) {
                cmd = args[args.length - 2];
                String from = args[args.length - 3];
                if (from.equalsIgnoreCase(Minecraft.getMinecraft().thePlayer.getDisplayName()))
                    return; // nope!
                if (cmd.equalsIgnoreCase("unset"))
                    updateRemoteModel(args[args.length - 3], null, null);
                if (cmd.equalsIgnoreCase("set"))
                    if (args.length >= 4)
                        updateRemoteModel(args[args.length - 3], miniDM(args[args.length - 4]), null);
                if (cmd.equalsIgnoreCase("query"))
                    sendMMCAnnounce(args[args.length - 3]);
            }
        }
    }

    private HashMap<String, String> miniDM(String arg) {
        HashMap<String, String> s = new HashMap<String, String>();
        s.put("mdl.pmx", arg);
        return s;
    }

    private void updateRemoteModel(final String user, final HashMap<String, String> dataManifest, final ModelCache.IPMXLocator serv) {
        EntityPlayer ep = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(user);
        if (dataManifest == null) {
            knownModels.put(user, new MHolder(null));
            if (ep != null)
                InstanceCache.setModel(ep, null);
            return;
        }
        new Thread() {
            @Override
            public void run() {
                PMXModel pm = ModelCache.getByManifest(dataManifest, serv);
                knownModels.put(user, new MHolder(pm));
                EntityPlayer ep2 = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(user);
                if (ep2 != null)
                    InstanceCache.queueChange(ep2, pm);
            }
        }.start();
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (editorBinding.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new EditorHostGui());
        }
    }

    private double interpolate(double last, double current, float partialTicks) {
        return last + (current - last) * partialTicks;
    }

    @SubscribeEvent
    public void onTickRender(TickEvent.RenderTickEvent rte) {
        if (rte.phase == TickEvent.Phase.START)
            InstanceCache.update(rte.renderTickTime / 20F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerRender(RenderPlayerEvent.Pre event) {

        EntityPlayer player = event.entityPlayer;

        double x = interpolate(player.lastTickPosX, player.posX, event.partialRenderTick);
        double y = interpolate(player.lastTickPosY, player.posY, event.partialRenderTick);
        double z = interpolate(player.lastTickPosZ, player.posZ, event.partialRenderTick);

        x -= RenderManager.renderPosX;
        y -= RenderManager.renderPosY;
        z -= RenderManager.renderPosZ;

        InstanceCache.ModelCacheEntry mce = InstanceCache.getModel(player);
        if (mce == null) {
            if (player == Minecraft.getMinecraft().thePlayer) {

                PMXModel newMdl = null;
                if (Loader.currentFile != null)
                    newMdl = ModelCache.getLocal(Loader.currentFile);
                mce = InstanceCache.setModel(player, newMdl);
                final InstanceCache.ModelCacheEntry mceF = mce;

                // The local player uses currentFile
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        PMXModel newMdl = null;
                        if (Loader.currentFile != null)
                            newMdl = ModelCache.getLocal(Loader.currentFile);
                        EntityPlayer ep = mceF.playerRef.get();
                        if (ep != null)
                            InstanceCache.setModel(ep, newMdl);
                    }
                };

                // InstanceCache will automatically delete any currentFile hook we leave here
                mce.cfHook = r;

                Loader.currentFileListeners.add(r);
            } else {
                return;
            }
        }
        PlayerInstance model = mce.value;
        if (model == null)
            return;
        model.apply(player, event.partialRenderTick);
        model.render(player, x, y, z, event.partialRenderTick);

        event.setCanceled(true);
    }

    private static class MHolder {
        public PMXModel held;

        public MHolder(PMXModel v) {
            held = v;
        }
    }
}
