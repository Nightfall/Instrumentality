package uk.co.gamemanj.instrumentality.animations;

import uk.co.gamemanj.instrumentality.PoseBoneTransform;
import uk.co.gamemanj.instrumentality.animations.IAnimation;
import uk.co.gamemanj.instrumentality.animations.libraries.EmoteAnimationLibrary;

/**
 * Created on 24/07/15.
 */
public class WalkingAnimation implements IAnimation {
    public float time = 0, wtime = 0;
    /**
     * Controlled by PlayerControlAnimation, for sneaking.
     */
    public float speed = 1.0f;

    /**
     * Find a way to use this, it makes walking just that tad bit more realistic :)
     */
    public float directionAdjustment=0.0f;

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        float atime = time + 0.65f;
        if (atime > 1.0f)
            atime -= 1.0f;
        if (boneName.equalsIgnoreCase("leg_L"))
            return getLegTransform(false, atime);
        if (boneName.equalsIgnoreCase("knee_L"))
            return getKneeTransform(false, atime);
        if (boneName.equalsIgnoreCase("ankle_L"))
            return getAnkleTransform(false, atime);
        if (boneName.equalsIgnoreCase("L_shouler"))
            return getShoulderTransform(false, atime);
        if (boneName.equalsIgnoreCase("L_ellbow"))
            return getElbowTransform(true, atime);

        float stime = atime + 0.50f;
        if (stime > 1.0f)
            stime -= 1.0f;
        if (boneName.equalsIgnoreCase("leg_R"))
            return getLegTransform(true, stime);
        if (boneName.equalsIgnoreCase("knee_R"))
            return getKneeTransform(true, stime);
        if (boneName.equalsIgnoreCase("ankle_R"))
            return getAnkleTransform(true, stime);
        // These are an exception to the stime rule
        if (boneName.equalsIgnoreCase("R_shouler"))
            return getShoulderTransform(true, atime);
        if (boneName.equalsIgnoreCase("R_ellbow"))
            return getElbowTransform(true, atime);
        String[] wiggle = {
                "head",
                "neck",
                "L_longhair_01",
                "R_longhair_01",
                "L_longhair_02",
                "R_longhair_02",
                "L_longhair_03",
                "R_longhair_03",
                "spine00",
                "spine01",
                "hip",
                "root",
                /* If you uncomment this and the 2 lines below relating to it, you're a pervert.
                "Skrt_LF_01",
                "Skrt_RF_01"
                */
        };
        float[] amount = {
                0.01f,
                0.01f,
                -0.2f/2,
                0.15f/2,
                -0.25f/2,
                0.2f/2,
                -0.375f/2,
                0.15f/2,
                0.02f,
                0.02f,
                0.02f,
                0.02f,
                /* See above comment.
                2.0f,
                2.0f
                */
        };
        for (int ai = 0; ai < wiggle.length; ai++) {
            if (wiggle[ai].equalsIgnoreCase(boneName)) {
                PoseBoneTransform pbt = new PoseBoneTransform();
                pbt.X1 = (float) Math.sin(wtime * (Math.PI * 2)) * amount[ai];
                if (boneName.equalsIgnoreCase("spine00")) {
                    pbt.Y0 = (float) Math.sin(wtime * Math.PI * (speed)) * (speed / 40.0f);
                }
                return pbt;
            }
        }
        return null;
    }

    private PoseBoneTransform getShoulderTransform(boolean b, float atime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        float t = (float) (atime * Math.PI * 2);
        if (b)
            t += Math.PI;
        float ofs = 0.92f;
        pbt.X0 = (float) (Math.sin(t) - ofs);
        pbt.Y0 = 0.29f;
        pbt.Z0 = -0.24f;
        pbt.X0 *= 0.693;
        if (b) {
            pbt.X0 = -pbt.X0;
            pbt.Y0 = -pbt.Y0;
            pbt.Z0 = -pbt.Z0;
        }
        return pbt;
    }

    private PoseBoneTransform getElbowTransform(boolean b, float atime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        float t = (float) (atime * Math.PI * 2);
        if (!b)
            t += Math.PI;
        pbt.Z0 = (float) (Math.sin(t)) + 1.0f;
        pbt.X0 = (float) (Math.sin(t) * 2.0f);
        pbt.Z0 *= -0.1f;
        pbt.X0 *= -0.4f;
        if (b) {
            pbt.X0 = -pbt.X0;
            pbt.Z0 = -pbt.Z0;
        }
        return pbt;
    }

    private PoseBoneTransform getKneeTransform(boolean LR, float shiftedTime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        pbt.Z0 = directionAdjustment / 3.0f;

        pbt.Y0 = (float) (Math.sin(shiftedTime * Math.PI) - 1.0f);

        if (LR)
            pbt.Y0 = -pbt.Y0;

        return pbt;
    }

    private PoseBoneTransform getLegTransform(boolean LR, float shiftedTime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        pbt.Z0 = directionAdjustment / 3.0f;

        float sinMul = 0.25f;
        if (shiftedTime > 0.5f) {
            sinMul = 0.125f;
        }
        pbt.Y0 = (float) Math.sin(shiftedTime * (Math.PI * 2)) * sinMul;
        pbt.Y0 += 0.1f;

        if (LR)
            pbt.Y0 = -pbt.Y0;

        return pbt;
    }

    private PoseBoneTransform getAnkleTransform(boolean LR, float shiftedTime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        pbt.Z0 = directionAdjustment / 3.0f;

        float lB = 0.5f;
        if (shiftedTime > 0.5f) {
            lB -= shiftedTime - 0.5f;
        }
        pbt.Y0 = lB;
        if (shiftedTime < 0.5f) {
            pbt.Y0 = 0;
            if (shiftedTime > 0.25f) {
                pbt.Y0 = (float) Math.sin((shiftedTime - 0.25f) * (Math.PI * 2)) * 0.25f;
                if (shiftedTime > 0.375f) {
                    float lerpval = (shiftedTime - 0.375f) * 8.0f;
                    float lA = 0.192f;
                    pbt.Y0 = lA * (1.0f - lerpval);
                    pbt.Y0 += lB * (lerpval);
                }
            }
        }
        pbt.Y0 -= 0.4;
        if (LR)
            pbt.Y0 = -pbt.Y0;
        return pbt;
    }

    @Override
    public void update(double deltaTime) {
        time += deltaTime * speed;
        wtime += deltaTime * speed;
        while (time > 1.0f)
            time -= 1.0f;
    }
}
