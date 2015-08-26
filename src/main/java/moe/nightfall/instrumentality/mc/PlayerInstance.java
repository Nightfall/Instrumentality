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

import moe.nightfall.instrumentality.Loader;
import moe.nightfall.instrumentality.PMXInstance;
import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.animations.*;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

public class PlayerInstance {

    public final PMXInstance pmxInst;

    public final LibraryAnimation libanim;
    public final PlayerControlAnimation pcanim;

    private IAnimation useAnimation, idleAnimation;

    public double clippingPoint=0d;
    
    public PlayerInstance(PMXModel file) {
        pmxInst = new PMXInstance(file);

        libanim = new LibraryAnimation();
        libanim.transitionValue = 1F;

        WalkingAnimation wa = new WalkingAnimation();
        StrengthMultiplyAnimation smaW = new StrengthMultiplyAnimation(wa);

        pcanim = new PlayerControlAnimation(wa, smaW);
        pcanim.walkingFlag = true;

        pmxInst.anim = new OverlayAnimation(smaW, this.pcanim, this.libanim);

        useAnimation = Loader.animLibs[1].getPose("use");
        idleAnimation = Loader.animLibs[1].getPose("idle");
    }

    public void update(double v) {
        pmxInst.update(v);
        clippingPoint+=v/2.0d;
        if (clippingPoint >= 1.1d)
            clippingPoint = 1.1d;
    }

    private double interpolate(double last, double current, float partialTicks) {
        return last + (current - last) * partialTicks;
    }

    public void render(EntityPlayer player, double x, double y, double z, float partialTick) {

        // TODO: make this per-model somehow.
        float adjustFactor = player.isSneaking() ? 0.1f : 0.05f;
        float scale = 1F / (pmxInst.theModel.height / player.height);

        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick);

        GL11.glPushMatrix();
        GL11.glRotated(180 - rotBody, 0, 1, 0);
        GL11.glTranslated(x, ((y - player.height) + player.eyeHeight) + adjustFactor, z);
        GL11.glScalef(scale, scale, scale);
        // I fixed the triangle order, but skirts do not play well with culling
        GL11.glDisable(GL11.GL_CULL_FACE);
        int lv=player.worldObj.getBlockLightValue_do((int)player.posX, (int)player.posY, (int)player.posZ, true);
        pmxInst.render(Loader.shaderBoneTransform, lv/15f, lv/15f, lv/15f, (float)clippingPoint);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    public void apply(EntityPlayer player, float partialTick) {

        double pitch = interpolate(player.prevRotationPitch, player.rotationPitch, partialTick);
        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick);

        // The interpolated values act a bit weird
        pcanim.lookDir = (float) Math.toRadians(player.rotationYawHead);
        pcanim.lookUD = (float) (-pitch / 140.0f);
        pcanim.bodyRotation = (float) Math.toRadians(rotBody);
        pcanim.sneakStateTarget = player.isSneaking() ? 1.0f : 0.0f;

        if (player.isSwingInProgress) {
            if (libanim.getTarget() != useAnimation)
                libanim.setCurrentPose(useAnimation, 0.2f, false);
        } else {
            if (libanim.getTarget() != idleAnimation)
                libanim.setCurrentPose(idleAnimation, 0.1f, false);
        }

        if (player.capabilities.isFlying)
            pcanim.sneakStateTarget = -1.0f;
        pcanim.walkingFlag = ((player.lastTickPosX != player.posX) || (player.lastTickPosZ != player.posZ));
        double xSpd = Math.abs(player.lastTickPosX - player.posX);
        double zSpd = Math.abs(player.lastTickPosZ - player.posZ);
        float spdMul = 5.0f;
        if (player.isSneaking())
            spdMul = 10.0f;
        pcanim.walking.speed = spdMul * ((float) Math.sqrt((xSpd * xSpd) + (zSpd * zSpd)));
    }

    public void cleanupGL() {
        pmxInst.cleanupGL();
    }
}
