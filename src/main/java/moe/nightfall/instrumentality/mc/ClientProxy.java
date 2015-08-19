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
import moe.nightfall.instrumentality.Main;
import moe.nightfall.instrumentality.mc.gui.EditorHostGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.input.Keyboard;

public class ClientProxy extends CommonProxy {

    public static KeyBinding editorBinding;

    @Override
    public void preInit() {
        super.preInit();
        editorBinding = new KeyBinding("key.mmc_editor", Keyboard.KEY_EQUALS, "key.categories.mikumikucraft");
        ClientRegistry.registerKeyBinding(editorBinding);
        try {
            Loader.setup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        PlayerInstance model = InstanceCache.getModel(player);
        if (model == null)
            return; // this means we don't do any "special stuff"
        model.apply(player, event.partialRenderTick);
        model.render(player, x, y, z, event.partialRenderTick);

        event.setCanceled(true);
    }
}
