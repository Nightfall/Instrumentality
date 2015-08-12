package moe.nightfall.instrumentality.animations.libraries;

import moe.nightfall.instrumentality.PoseBoneTransform;
import moe.nightfall.instrumentality.animations.IAnimation;
import moe.nightfall.instrumentality.animations.IAnimationLibrary;
import moe.nightfall.instrumentality.animations.PoseAnimation;

/**
 * Emotes (includes her "idle" pose)
 * Created on 29/07/15.
 */
public class EmoteAnimationLibrary implements IAnimationLibrary {
    /**
     * Special PBT modified from Main (Let's hope the code never caches a copy)
     */
    public static PoseBoneTransform debugPbt = new PoseBoneTransform();

    public EmoteAnimationLibrary() {
    }

    @Override
    public IAnimation getPose(String poseName) {
        if (poseName.equalsIgnoreCase("idle")) {
            PoseAnimation pa = new PoseAnimation();
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(20);
            pbt.Y0 = (float) Math.toRadians(-30);
            pa.hashMap.put("r_shouler", pbt);
            return pa;
        }
        // some things mean others depending on context, did you know?
        if (poseName.equalsIgnoreCase("hai") || poseName.equalsIgnoreCase("@one_butterflies_haven_keynsham_quiet")) {
            PoseAnimation pa = new PoseAnimation();
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.Z0 = -0.1f;
            pbt.X1 = -0.54f;
            pbt.Y1 = 1.64f;
            pa.hashMap.put("r_shouler", pbt);
            pbt = new PoseBoneTransform();
            pbt.X0 = 0.63f;
            pbt.Y0 = 0.435f;
            pa.hashMap.put("r_ellbow", pbt);
            return pa;
        }
        // This pose was actually developed during work on onehandedgendo...
        // Also: I think upperclass shock?
        if (poseName.equalsIgnoreCase("creepy_laugh")) {
            PoseAnimation pa = new PoseAnimation();
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(20);
            pbt.Y0 = (float) Math.toRadians(-30);
            pa.hashMap.put("r_shouler", pbt);
            pbt = new PoseBoneTransform();
            pbt.Y0 = -0.39f;
            pbt.Z0 = -3.12f;
            pbt.X1 = 0.81f;
            pbt.Y1 = 1.23f;
            pa.hashMap.put("r_ellbow", pbt);
            return pa;
        }
        if (poseName.equalsIgnoreCase("gendo")) {
            PoseAnimation pa = new PoseAnimation();
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.X0 = 1.12f;
            pbt.Y0 = 0.65f;
            pbt.Z0 = -0.65f;
            pa.hashMap.put("r_shouler", pbt);
            pbt = new PoseBoneTransform();
            pbt.X0 = 0.10f;
            pbt.Y0 = 0.28f;
            pbt.Z0 = -1.49f;
            pbt.X1 = 2.72f;
            pbt.Y1 = 0.04f;
            pa.hashMap.put("r_ellbow", debugPbt);
            pbt = new PoseBoneTransform();
            pbt.Z0 = -0.45f;
            pbt.X1 = 0.22f;
            pbt.Y1 = -1.37f;
            pa.hashMap.put("r_hand", pbt);
            pbt = new PoseBoneTransform();
            pbt.Y0 = -0.33f;
            pa.hashMap.put("fore1_r", pbt);
            pbt = new PoseBoneTransform();
            pbt.Y0 = -0.43f;
            pa.hashMap.put("middle1_r", pbt);
            pbt = new PoseBoneTransform();
            pbt.Y0 = -0.53f;
            pa.hashMap.put("third1_r", pbt);
            pbt = new PoseBoneTransform();
            pbt.Y0 = -0.63f;
            pa.hashMap.put("little1_r", pbt);
            for (int i = 0; i < 1; i++) {
                pbt = new PoseBoneTransform();
                pbt.Y0 = -0.1f;
                pa.hashMap.put("fore" + (i + 2) + "_r", pbt);
                pbt = new PoseBoneTransform();
                pbt.Y0 = -0.1f;
                pa.hashMap.put("middle" + (i + 2) + "_r", pbt);
                pbt = new PoseBoneTransform();
                pbt.Y0 = -0.1f;
                pa.hashMap.put("third" + (i + 2) + "_r", pbt);
            }
            return pa;
        }
        return null;
    }

    @Override
    public String[] getPoses() {
        return new String[]{"idle", "hai", "creepy_laugh", "gendo"};
    }
}
