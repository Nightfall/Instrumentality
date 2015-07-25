package uk.co.gamemanj.instrumentality;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created on 24/07/15.
 */
public class WalkingAnimation implements IAnimation {
    public float directionAdjustment, time = 0, wtime = 0;
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
        float stime = atime + 0.50f;
        if (stime > 1.0f)
            stime -= 1.0f;
        if (boneName.equalsIgnoreCase("leg_R"))
            return getLegTransform(true, stime);
        if (boneName.equalsIgnoreCase("knee_R"))
            return getKneeTransform(true, stime);
        if (boneName.equalsIgnoreCase("ankle_R"))
            return getAnkleTransform(true, stime);
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
                "hip"
        };
        float[] amount = {
                0.05f,
                0.05f,
                -0.2f,
                0.15f,
                -0.25f,
                0.2f,
                -0.375f,
                0.15f,
                0.01f,
                0.01f,
                0.01f
        };
        for (int ai = 0; ai < wiggle.length; ai++) {
            if (wiggle[ai].equalsIgnoreCase(boneName)) {
                PoseBoneTransform pbt = new PoseBoneTransform();
                pbt.X1 = (float) Math.sin(wtime * (Math.PI * 2)) * amount[ai];
                if (boneName.contains("longhair_02"))
                    pbt.Y0 = boneName.contains("L") ? 0.5f : -0.5f;
                return pbt;
            }
        }
        return null;
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
        wtime += deltaTime;
        while (time > 1.0f)
            time -= 1.0f;
    }
}
