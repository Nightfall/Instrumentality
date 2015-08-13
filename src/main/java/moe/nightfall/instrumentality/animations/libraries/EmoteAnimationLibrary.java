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

        if (poseName.equalsIgnoreCase("hai")) {
            PoseAnimation pa = PlayerAnimationLibrary.createIdlePoseAnimation();
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
        // This pose was actually developed during work on the attempt at a gendo...
        // Also: I think upperclass shock?
        if (poseName.equalsIgnoreCase("creepy_laugh")) {
            PoseAnimation pa = PlayerAnimationLibrary.createIdlePoseAnimation();
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
        return null;
    }

    @Override
    public String[] getPoses() {
        return new String[]{"hai", "creepy_laugh"};
    }
}
