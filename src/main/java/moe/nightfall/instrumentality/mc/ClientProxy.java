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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import moe.nightfall.instrumentality.IMaterialBinder;
import moe.nightfall.instrumentality.Main;
import moe.nightfall.instrumentality.PMXFile;
import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.animations.IAnimation;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

public class ClientProxy extends CommonProxy {

    private IAnimation useAnimation, idleAnimation;

    @Override
    public void preInit() {
        super.preInit();

        try {
            Main.setup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        useAnimation = Main.animLibs[1].getPose("use");
        idleAnimation = Main.animLibs[1].getPose("idle");
    }

    private double interpolate(double last, double current, float partialTicks) {
        return last + (current - last) * partialTicks;
    }

    @SubscribeEvent
    public void onTickRender(TickEvent.RenderTickEvent rte) {
        if (rte.phase == TickEvent.Phase.START)
            Main.pm[0].update(rte.renderTickTime / 20.0f);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerRender(RenderPlayerEvent.Pre event) {

        EntityPlayer player = event.entityPlayer;

        double x = interpolate(player.lastTickPosX, player.posX, event.partialRenderTick);
        double y = interpolate(player.lastTickPosY, player.posY, event.partialRenderTick);
        double z = interpolate(player.lastTickPosZ, player.posZ, event.partialRenderTick);

        double pitch = interpolate(player.prevRotationPitch, player.rotationPitch, event.partialRenderTick);
        double yaw = interpolate(player.prevRotationYaw, player.rotationYaw, event.partialRenderTick);
        double yawHead = interpolate(player.prevRotationYawHead, player.rotationYawHead, event.partialRenderTick);
        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, event.partialRenderTick);

        x -= RenderManager.renderPosX;
        y -= RenderManager.renderPosY;
        z -= RenderManager.renderPosZ;

        // TODO: Vic, may I ask why you're using Main (the testbench) code... It worries me...
        //       Plus, you realize this only works for one player?

        // The interpolated values act a bit weird
        Main.pca[0].lookLR = (float) ((player.renderYawOffset - player.rotationYaw) / 180f);
        Main.pca[0].lookUD = (float) (-pitch / 140.0f);
        Main.pca[0].bodyRotation = (float) Math.toRadians(rotBody);
        Main.pca[0].sneakStateTarget = player.isSneaking() ? 1.0f : 0.0f;
        if (player.isSwingInProgress) {
            if (Main.lib[0].getTarget() != useAnimation)
                Main.lib[0].setCurrentPose(useAnimation, 0.2f, false);
        } else {
            if (Main.lib[0].getTarget() != idleAnimation)
                Main.lib[0].setCurrentPose(idleAnimation, 0.1f, false);
        }
        if (player.capabilities.isFlying)
            Main.pca[0].sneakStateTarget = -1.0f;
        Main.pca[0].walkingFlag = ((player.lastTickPosX != player.posX) || (player.lastTickPosZ != player.posZ));
        double xSpd = Math.abs(player.lastTickPosX - player.posX);
        double zSpd = Math.abs(player.lastTickPosZ - player.posZ);
        float spdMul = 5.0f;
        if (player.isSneaking())
            spdMul = 10.0f;
        Main.pca[0].walking.speed = spdMul * ((float) Math.sqrt((xSpd * xSpd) + (zSpd * zSpd)));

        // TODO: make this per-model somehow.
        float adjustFactor = player.isSneaking() ? 0.1f : 0.05f;

        PMXModel model = Main.pm[0];
        float scale = 1F / (model.height / player.height);

        GL11.glPushMatrix();
        GL11.glRotated(180 - rotBody, 0, 1, 0);
        GL11.glTranslated(x, ((y - player.height) + player.eyeHeight) + adjustFactor, z);
        GL11.glScalef(scale, scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        // I fixed the triangle order, but skirts do not play well with culling
        GL11.glDisable(GL11.GL_CULL_FACE);

        model.render(new IMaterialBinder() {
            @Override
            public void bindMaterial(PMXFile.PMXMaterial texture) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, Main.materialTextures.get(texture));
            }
        }, Main.shaderBoneTransform);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();

        event.setCanceled(true);
    }
}
