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
 * Created on 24/07/15.
 */
public class WalkingAnimation implements IAnimation {
    public float time = 0, wtime = 0;
    /**
     * Controlled by MC
     */
    public float speed = 1.0f;

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
        PoseBoneTransform interpA = new PoseBoneTransform(0.13499999f, 0, 0.71999985f, 0.13999994f, 0.26f);
        PoseBoneTransform interpM = new PoseBoneTransform(0, 0, 0, 0.11000006f, 0.35000002f);
        PoseBoneTransform interpB = new PoseBoneTransform(0, 1.0400001f, 0.06499989f, 0.77999985f, -0.4150001f);
        float t = (float) (atime * Math.PI * 2);
        if (b)
            t += Math.PI;
        t = (float) Math.sin(t);
        float strength = speed;
        if (t < 0) {
            PoseBoneTransform pbt = new PoseBoneTransform(interpM, interpA, -t * strength);
            if (b)
                return flipShoulderPBT(pbt);
            return pbt;
        } else {
            PoseBoneTransform pbt = new PoseBoneTransform(interpM, interpB, t * strength);
            if (b)
                return flipShoulderPBT(pbt);
            return pbt;
        }
    }

    private PoseBoneTransform flipShoulderPBT(PoseBoneTransform pbt) {
        PoseBoneTransform rpbt = new PoseBoneTransform(pbt);
        //rpbt.X0=-pbt.X0;
        rpbt.Y0 = -pbt.Y0;
        rpbt.Z0 = -pbt.Z0;
        //rpbt.X1=-pbt.X1;
        rpbt.Y1 = -pbt.Y1;
        return rpbt;
    }

    private PoseBoneTransform getElbowTransform(boolean b, float atime) {
        PoseBoneTransform interpA = new PoseBoneTransform(0.28999993f, -0.28000003f, -0.33f, 0.56500053f, -0.089999996f);
        PoseBoneTransform interpB = new PoseBoneTransform(0.0f, 0.0f, 0, 0.23499939f, 0.0f);
        float t = (float) (atime * Math.PI * 2);
        if (b)
            t += Math.PI;
        PoseBoneTransform pbt = new PoseBoneTransform(interpA, interpB, (float) (Math.sin(t) + 1.0f) / 2.0f);
        return null;
    }

    private PoseBoneTransform getKneeTransform(boolean LR, float shiftedTime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        pbt.Y0 = (float) (Math.sin(shiftedTime * Math.PI) - 1.0f);

        if (!LR)
            pbt.Y0 = -pbt.Y0;

        return pbt;
    }

    private PoseBoneTransform getLegTransform(boolean LR, float shiftedTime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
        float sinMul = 0.25f;
        if (shiftedTime > 0.5f) {
            sinMul = 0.125f;
        }
        pbt.Y0 = (float) Math.sin(shiftedTime * (Math.PI * 2)) * sinMul;
        pbt.Y0 += 0.1f;

        if (!LR)
            pbt.Y0 = -pbt.Y0;

        return pbt;
    }

    private PoseBoneTransform getAnkleTransform(boolean LR, float shiftedTime) {
        PoseBoneTransform pbt = new PoseBoneTransform();
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
        if (!LR)
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
