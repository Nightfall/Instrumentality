package uk.co.gamemanj.instrumentality.animations.libraries;

import uk.co.gamemanj.instrumentality.Main;
import uk.co.gamemanj.instrumentality.PoseBoneTransform;
import uk.co.gamemanj.instrumentality.animations.IAnimation;
import uk.co.gamemanj.instrumentality.animations.IAnimationLibrary;
import uk.co.gamemanj.instrumentality.animations.PoseAnimation;

/**
 * Vic: Keep the one-handed Gendo... or else!
 * Created on 29/07/15.
 */
public class EmoteAnimationLibrary implements IAnimationLibrary {
    /**
     * Special PBT modified from Main (Let's hope the code never caches a copy)
     */
    public PoseBoneTransform debugPbt = new PoseBoneTransform();

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
        if (poseName.equalsIgnoreCase("hai!") || poseName.equalsIgnoreCase("@one_butterflies_haven_keynsham_quiet")) {
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
        if (poseName.equalsIgnoreCase("creepy_laugh") || poseName.equalsIgnoreCase("upperclass_shock")) {
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
        if (poseName.equalsIgnoreCase("one_handed_gendo")) {
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
            pa.hashMap.put("r_ellbow", pbt);
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
            return pa;
        }
        return null;
    }

    @Override
    public String[] getPoses() {
        return new String[]{"idle", "hai!", "@one_butterflies_haven_keynsham_quiet", "creepy_laugh", "upperclass_shock", "one_handed_gendo"};
    }
}
