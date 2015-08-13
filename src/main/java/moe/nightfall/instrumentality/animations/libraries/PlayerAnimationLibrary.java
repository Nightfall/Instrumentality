package moe.nightfall.instrumentality.animations.libraries;

import moe.nightfall.instrumentality.PoseBoneTransform;
import moe.nightfall.instrumentality.animations.IAnimation;
import moe.nightfall.instrumentality.animations.IAnimationLibrary;
import moe.nightfall.instrumentality.animations.PoseAnimation;

/**
 * Moved here since it's unlikely having 2 separate animation controls... well, animation SYSTEMS even,
 * for left and right body will end very well
 * (see: infamous one-handed-gendo)
 * This way, the arms do one thing at a time.
 * Not counting walking, that is...
 * (attack & walk is a common thing, and we can't stop the player from emoting on the go)
 * Created on 13/08/15.
 */
public class PlayerAnimationLibrary implements IAnimationLibrary {
    public static PoseAnimation createIdlePoseAnimation() {
        PoseAnimation pa = new PoseAnimation();
        PoseBoneTransform pbt = new PoseBoneTransform();
        pbt.X0 = (float) Math.toRadians(20);
        pbt.Y0 = (float) Math.toRadians(-30);
        pa.hashMap.put("l_shouler", pbt);
        pbt=new PoseBoneTransform();
        // NOTES: l_shouler relative to r_shouler, X is not inversed, Y is. Z not checked.
        pbt.X0 = (float) Math.toRadians(20);
        pbt.Y0 = (float) Math.toRadians(30);
        pa.hashMap.put("r_shouler", pbt);
        return pa;
    }
    @Override
    public IAnimation getPose(String poseName) {
        if (poseName.equalsIgnoreCase("idle")) {
            PoseAnimation pa = createIdlePoseAnimation();
            return pa;
        }
        if (poseName.equalsIgnoreCase("use")) {
            PoseAnimation pa = createIdlePoseAnimation();
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(20);
            pbt.Y0 = (float) Math.toRadians(30);
            pa.hashMap.put("r_shouler", pbt);

            pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(40);
            pbt.Z0 = (float) Math.toRadians(80);
            pa.hashMap.put("r_ellbow", pbt);

            return pa;
        }
        if (poseName.equalsIgnoreCase("block")) {
            PoseAnimation pa = createIdlePoseAnimation();
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(35.3f);
            pbt.Y0 = (float) Math.toRadians(-6f);
            pbt.Z0 = (float) Math.toRadians(-1.7f);
            pa.hashMap.put("r_shouler", pbt);

            pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(43.5f);
            pbt.Y0 = (float) Math.toRadians(103);
            pbt.Z0 = (float) Math.toRadians(20);
            pbt.X1 = (float) Math.toRadians(-47.7f);
            pa.hashMap.put("r_ellbow", pbt);

            return pa;
        }
        return null;
    }

    @Override
    public String[] getPoses() {
        return new String[] {"idle","use","block"};
    }
}
