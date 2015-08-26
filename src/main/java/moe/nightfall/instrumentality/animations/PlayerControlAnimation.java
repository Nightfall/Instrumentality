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
package moe.nightfall.instrumentality.animations;

import moe.nightfall.instrumentality.PoseBoneTransform;

/**
 * Another reason I like programmatically modifying bone positions!
 * Note that to avoid complications involving stacking animations, this won't attempt to twist the legs/lower body area.
 * Also, you may want to change the way the target speed is calculated.
 * <p/>
 * Right now it's based on sneakState, but you may want it to be calculated based on horizontal velocity.
 * Also: Read the variable comments below, you "control" the system via these variables
 * <p/>
 * Created on 27/07/15.
 */
public class PlayerControlAnimation implements IAnimation {

    /*
            "L_longhair_01",
            "R_longhair_01",
            "L_longhair_02",
            "R_longhair_02",
            "L_longhair_03",
            "R_longhair_03",
            -0.2f * wStrength,
            0.15f * wStrength,
            -0.25f * wStrength,
            0.2f * wStrength,
            -0.375f * wStrength,
            0.15f * wStrength,

    */

    /**
     * 1.0f and -1.0f are directly back-left, back-right. 0.0f is looking directly ahead.
     */
    public float lookLR = 1.0f;
    /**
     * 1.0f and -1.0f are directly up/down. 0.0f is looking directly ahead.
     */
    public float lookUD = 0.1f;

    /**
     * Look dir relative to bodyRotation
     */
    public float lookDir;

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
     * Used to work out hair sway :)
     * Can be any value, the system will normalize to a 0 to PI*2 value, and then finally to a difference.
     */
    public float bodyRotation = 0.0f;

    /**
     * Used to work out how much Miku's head has rotated
     */
    public float lastLRValue = 0.0f;

    /**
     * Set to 1.0f for sneaking,-1.0f for flying
     */
    public float sneakStateTarget = 0;
    public float sneakState = 0;

    /**
     * This is used so Miku's feet don't magically turn along with her body.
     */
    public float directionAdjustment = 0.0f;

    /**
     * Used for transitioning to/from the walking animation.
     */
    public boolean walkingFlag = false;

    /**
     * Control of the Walking Animation.
     */
    public WalkingAnimation walking;

    /**
     * Controls the strength of the walking animation.
     * Used to fade in/out the walking animation depending on if the player is, you know, walking.
     */
    public StrengthMultiplyAnimation walkingStrengthControl;

    public PlayerControlAnimation(WalkingAnimation wa, StrengthMultiplyAnimation wastr) {
        walking = wa;
        walkingStrengthControl = wastr;
    }

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        if (boneName.equalsIgnoreCase("neck"))
            return getPlayerLookPBT(0.6f, 0.20f);
        if (boneName.equalsIgnoreCase("head"))
            return getPlayerLookPBT(0.6f, 0.20f);
        if (boneName.equalsIgnoreCase("spine01"))
            return getPlayerLookPBT(0.10f, 0.05f);
        if (boneName.equalsIgnoreCase("L_eye_ctrl"))
            return getPlayerLookPBT(0.25f, 0.30f);
        if (boneName.equalsIgnoreCase("R_eye_ctrl"))
            return getPlayerLookPBT(0.25f, 0.30f);
        if (boneName.equalsIgnoreCase("eyes_ctrl")) {
            PoseBoneTransform pbt = new PoseBoneTransform();
//            pbt.TZ0 = -0.1f;
            return pbt;
        }
        if (boneName.equalsIgnoreCase("leg_L"))
            return getLegTransform(false);
        if (boneName.equalsIgnoreCase("knee_L"))
            return getKneeTransform(false);
        if (boneName.equalsIgnoreCase("ankle_L"))
            return getAnkleTransform(false);
        if (boneName.equalsIgnoreCase("leg_R"))
            return getLegTransform(true);
        if (boneName.equalsIgnoreCase("knee_R"))
            return getKneeTransform(true);
        if (boneName.equalsIgnoreCase("ankle_R"))
            return getAnkleTransform(true);
        if (boneName.equalsIgnoreCase("root")) {
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.Z0 = directionAdjustment;
            if (sneakState > 0) {
                double bob = 0.3f + (Math.sin((walking.time) * Math.PI * 4) * 0.5f);
                bob *= walkingStrengthControl.mulAmount;
                pbt.TZ0 = ((-sneakState) * (1.2f + (float) bob));
                return pbt;
            }
            return pbt;
        }
        if (boneName.equalsIgnoreCase("L_longhair_01"))
            return getLonghairTransform(false, 0);
        if (boneName.equalsIgnoreCase("R_longhair_01"))
            return getLonghairTransform(true, 0);
        if (boneName.equalsIgnoreCase("L_longhair_02"))
            return getLonghairTransform(false, 1);
        if (boneName.equalsIgnoreCase("R_longhair_02"))
            return getLonghairTransform(true, 1);
        if (boneName.equalsIgnoreCase("L_longhair_03"))
            return getLonghairTransform(false, 2);
        if (boneName.equalsIgnoreCase("R_longhair_03"))
            return getLonghairTransform(true, 2);
        return null;
    }

    private PoseBoneTransform getLonghairTransform(boolean b, int segment) {
        PoseBoneTransform pbt = new PoseBoneTransform();

        if (segment == 0) {
            pbt.Y0 = ((float) (Math.sin((lookUD) * Math.PI) * 0.7f)) - 0.2f;
            if (pbt.Y0 < 0)
                pbt.Y0 = 0;
            if (lookUD < 0.226f)
                pbt.Y0 = 0.257f;
        } else {
            if (segment == 1) {
                pbt.Y0 = (float) (Math.sin(lookUD - 0.2f) * 0.7f);
                if (pbt.Y0 > 0)
                    pbt.Y0 = 0;
                if (pbt.Y0 < 0)
                    pbt.Y0 *= -6.0f;
                if (lookUD < 0.226f)
                    pbt.Y0 *= 0.8f;
            }
        }
        pbt.Y0 *= ((segment != 0) ? -0.50f : 2);
        pbt.Y0 *= (b ? 1 : -1);
        pbt.X0 = hairSway / 2.0f;
        pbt.X0 *= ((segment != 0) ? 1 : 2);
        if (hairSway > 0) {
            pbt.X0 *= (b ? 0.7f : -1);
        } else {
            pbt.X0 *= (b ? 1 : -0.7f);
        }
        return pbt;
    }

    private PoseBoneTransform getLegTransform(boolean b) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        if (sneakState < 0) {
            pbt.X0 = 0.2f * -sneakState;
            return pbt;
        }
        pbt.Y0 = (0.5f) * sneakState;
        if (!b)
            pbt.Y0 = -pbt.Y0;
        return pbt;
    }

    private PoseBoneTransform getKneeTransform(boolean b) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        if (sneakState < 0)
            return pbt;
        pbt.Y0 = (-1.0f) * sneakState;
        if (!b)
            pbt.Y0 = -pbt.Y0;
        return pbt;
    }

    private PoseBoneTransform getAnkleTransform(boolean b) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        if (sneakState < 0)
            return pbt;
        pbt.Y0 = (-0.5f) * sneakState;
        if (!b)
            pbt.Y0 = -pbt.Y0;
        return pbt;
    }

    private PoseBoneTransform getPlayerLookPBT(float lr, float ud) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        pbt.Y1 += lookUD * ud * Math.PI;
        pbt.Z0 += lookLR * lr * Math.PI;
        return pbt;
    }

    @Override
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
        if (sneakState < -1.0f)
            sneakState = -1.0f;
        if (sneakState > 1.0f)
            sneakState = 1.0f;

        if (walkingFlag) {
            walkingStrengthControl.mulAmount += deltaTime * 8.0f;
            float limit = sneakState < 0 ? 0.1f : 1.0f;
            if (walkingStrengthControl.mulAmount > limit)
                walkingStrengthControl.mulAmount = limit;
        } else {
            walkingStrengthControl.mulAmount -= deltaTime * 8.0f;
            if (walkingStrengthControl.mulAmount < 0) {
                walkingStrengthControl.mulAmount = 0;
                walking.time = 0;
            }
        }
        // Time to calculate Miku's "hair physics"
        hairSwayVel += (float) ((-hairSway) * deltaTime * 4.0f);
        hairSway += hairSwayVel * deltaTime * 4.0f;
        hairSwayVel -= hairSwayVel * (deltaTime / 1.0f);

        /*
         * Documentation on what this does:
         * bodyRotation is where MC wants root to be facing(and where Vic's code right now makes us face)
         * directionAdjustment is basically "how much are we working against bodyRotation".
         * hairSwayVel is adjusted with the data we get, though that needs to be changed to use bodyRotation+directionAdjustment+lookDir
         * lookLR is the final look value
         * lookDir is the input look value
         */

        float bRotation = bodyRotation;
        // Normalize between 0 and PI*2
        while (bRotation < 0)
            bRotation += Math.PI * 2;
        while (bRotation > (Math.PI * 2))
            bRotation -= Math.PI * 2;
        float distRotation = angleDist(bRotation, lastTotalRotation);
        lastTotalRotation = bRotation;

        directionAdjustment += distRotation;
        while (directionAdjustment < -Math.PI)
            directionAdjustment += Math.PI * 2;
        while (directionAdjustment > Math.PI)
            directionAdjustment -= Math.PI * 2;

        float daTarget = (-lookDir) + bRotation;
        while (daTarget < -Math.PI)
            daTarget += Math.PI * 2;
        while (daTarget > Math.PI)
            daTarget -= Math.PI * 2;
        if (walkingFlag || (sneakState < 0)) {
            float waChange = (float) (Math.PI * deltaTime);
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

        float finalRot = (-bRotation) + directionAdjustment;

        finalRot = lookDir + finalRot;

        while (finalRot < -Math.PI)
            finalRot += Math.PI * 2;
        while (finalRot > Math.PI)
            finalRot -= Math.PI * 2;

        lookLR = (float) (-finalRot / (Math.PI * 2));

        float lrValue = (lookLR * 1.2f);
        hairSwayVel += (distRotation / 2.0f) + ((lrValue - lastLRValue) * 2);
        lastLRValue = lrValue;
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
}
