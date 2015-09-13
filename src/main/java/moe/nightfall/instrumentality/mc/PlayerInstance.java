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
import moe.nightfall.instrumentality.animations.NewPCAAnimation;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class PlayerInstance {

    public final PMXInstance pmxInst;

    public double clippingPoint = 0d;

    /**
     * Miku's hair position.
     */
    public float hairSway = 0.5f;

    /**
     * Miku's hair velocity.
     * This is not directly modified by the head movement, instead it changes hairSway.
     */
    public float hairSwayVel = 0.0f;

    /**
     * Used to work out how much Miku has rotated
     */
    public float lastTotalRotation = 0.0f;

    /**
     * Used to work out how much Miku's head has rotated
     */
    public double lastLRValue = 0.0f;

    /**
     * Set to 1.0f for sneaking,-1.0f for flying
     */
    public float sneakStateTarget = 0;
    public float sneakState = 0;

    /**
     * This is used so Miku's feet don't magically turn along with her body.
     */
    public float directionAdjustment = 0.0f;

    public float daTarget = 0.0f;

    public boolean resetFrame = true;

    public NewPCAAnimation anim;

    public PlayerInstance(PMXModel file) {
        pmxInst = new PMXInstance(file);
        pmxInst.anim = anim = new NewPCAAnimation(file.poses);
    }

    public void update(double deltaTime) {
        double sChange = deltaTime * 8.0f;
        if (sneakStateTarget == 0) {
            if (sneakState < 0)
                if (sneakState + sChange > 0)
                    sneakState = 0;
            if (sneakState > 0)
                if (sneakState - sChange < 0)
                    sneakState = 0;
        }

        if (sneakState < sneakStateTarget) {
            sneakState += sChange;
        } else if (sneakState > sneakStateTarget) {
            sneakState -= sChange;
        }
        if (sneakState < 0.0f)
            sneakState = 0.0f;
        if (sneakState > 1.0f)
            sneakState = 1.0f;

        // Time to calculate Miku's "hair physics"
        hairSwayVel += (float) ((-hairSway) * deltaTime * 4.0f);
        hairSway += hairSwayVel * deltaTime * 4.0f;
        hairSwayVel -= hairSwayVel * (deltaTime / 1.0f);

        pmxInst.update(deltaTime);
        clippingPoint += deltaTime / 2.0d;
        if (clippingPoint >= 1.1d)
            clippingPoint = 1.1d;

        if ((anim.walkStrength > 0) || (sneakState < 0)) {
            double waChange = Math.PI * deltaTime * 10;
            if (directionAdjustment < daTarget) {
                directionAdjustment += waChange;
                if (directionAdjustment > daTarget)
                    directionAdjustment = daTarget;
            } else {
                directionAdjustment -= waChange;
                if (directionAdjustment < daTarget)
                    directionAdjustment = daTarget;
            }
        }
    }

    private double interpolate(double last, double current, float partialTicks) {
        return last + (current - last) * partialTicks;
    }

    public void render(EntityPlayer player, double x, double y, double z, float partialTick) {
        float adjustFactor = player.isSneaking() ? 0.14f : 0.07f;
        float scale = 1F / (pmxInst.theModel.height / player.height);

        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick);

        GL11.glPushMatrix();
        GL11.glTranslated(x, ((y - player.height) + player.eyeHeight) + adjustFactor, z);
        GL11.glRotated(180 - rotBody, 0, 1, 0);
        GL11.glScalef(scale, scale, scale);
        // I fixed the triangle order, but skirts do not play well with culling
        GL11.glDisable(GL11.GL_CULL_FACE);
        int lv = player.worldObj.getBlockLightValue_do((int) player.posX, (int) player.posY, (int) player.posZ, true);
        GL11.glRotated(Math.toDegrees(directionAdjustment), 0, 1, 0);
        pmxInst.render(Loader.shaderBoneTransform, lv / 15f, lv / 15f, lv / 15f, (float) clippingPoint);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    public void apply(EntityPlayer player, float partialTick) {

        double pitch = interpolate(player.prevRotationPitch, player.rotationPitch, partialTick);
        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick);

        anim.lookUD = (float) (-pitch / 90.0f);

        sneakStateTarget = 0;
        if (player.isSneaking())
            sneakStateTarget = 1;

        /*
         * Documentation on what this does:
         * bodyRotation is where MC wants root to be facing(and where Vic's code right now makes us face)
         * directionAdjustment is basically "how much are we working against bodyRotation".
         * hairSwayVel is adjusted with the data we get, though that needs to be changed to use bodyRotation+directionAdjustment+lookDir
         * lookLR is the final look value
         * lookDir is the input look value
         */

        float bRotation = (float) Math.toRadians(rotBody);
        // Normalize between 0 and PI*2
        while (bRotation < 0)
            bRotation += Math.PI * 2;
        while (bRotation > (Math.PI * 2))
            bRotation -= Math.PI * 2;
        float distRotation = angleDist(bRotation, lastTotalRotation);
        lastTotalRotation = bRotation;

        if (resetFrame)
            distRotation = 0;

        directionAdjustment += distRotation;
        while (directionAdjustment < -Math.PI)
            directionAdjustment += Math.PI * 2;
        while (directionAdjustment > Math.PI)
            directionAdjustment -= Math.PI * 2;

        float lookDir = (float) Math.toRadians(player.rotationYawHead);

        daTarget = (-lookDir) + bRotation;
        while (daTarget < -Math.PI)
            daTarget += Math.PI * 2;
        while (daTarget > Math.PI)
            daTarget -= Math.PI * 2;

        if (resetFrame)
            directionAdjustment = daTarget;

        float finalRot = (-bRotation) + directionAdjustment;

        finalRot = lookDir + finalRot;

        while (finalRot < -Math.PI)
            finalRot += Math.PI * 2;
        while (finalRot > Math.PI)
            finalRot -= Math.PI * 2;

        anim.lookLR = (float) (-finalRot / Math.PI);

        double lrValue = (anim.lookLR * 1.2f);
        hairSwayVel += (distRotation / 2.0f) + ((lrValue - lastLRValue) * 2);
        lastLRValue = lrValue;

        resetFrame = false;

        if (player.isSwingInProgress) {
//            if (libanim.getTarget() != useAnimation)
//                libanim.setCurrentPose(useAnimation, 0.2f, false);
            //TODO SWING
        } else {
//            if (libanim.getTarget() != idleAnimation)
//                libanim.setCurrentPose(idleAnimation, 0.1f, false);
        }

        double spdMul = 0;
        if ((player.lastTickPosX != player.posX) || (player.lastTickPosZ != player.posZ)) {
            double dX = Math.abs(player.lastTickPosX - player.posX);
            double dZ = Math.abs(player.lastTickPosZ - player.posZ);
            double dist = Math.sqrt((dX * dX) + (dZ * dZ));
            spdMul = dist * 17.5d;
        }
        anim.walkSpeed = spdMul;
        anim.walkStrength = 1.0d;

        double fallVel = player.lastTickPosY - player.posY;
        anim.fallStrength = fallVel;
    }

    private float angleDist(float totalRotation, float lastTotalRotation) {
        float a = totalRotation - lastTotalRotation;
        float b;
        if (totalRotation < lastTotalRotation) {
            // given a TR of 0.20, a LTR of 0.80 and a L of 1:
            // TR+(1-LTR)=0.40
            b = -adSpecial(totalRotation, lastTotalRotation);
        } else {
            // given a LTR of 0.20, a TR of 0.80 and a L of 1:
            // LTR+(1-TR)=0.80
            b = adSpecial(lastTotalRotation, totalRotation);
        }
        if (Math.abs(a) > Math.abs(b))
            return b;
        return a;
    }

    private float adSpecial(float lower, float upper) {
        return (float) (lower + ((Math.PI * 2) - upper));
    }

    public void cleanupGL() {
        pmxInst.cleanupGL();
    }
}
