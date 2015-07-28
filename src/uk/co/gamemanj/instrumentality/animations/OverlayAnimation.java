package uk.co.gamemanj.instrumentality.animations;

import uk.co.gamemanj.instrumentality.PoseBoneTransform;
import uk.co.gamemanj.instrumentality.animations.IAnimation;

/**
 * Adds the PBTs together.
 * Created on 26/07/15.
 */
public class OverlayAnimation implements IAnimation {
    public IAnimation[] subAnimations;
    public OverlayAnimation(IAnimation[] subAnimations) {
        this.subAnimations=subAnimations;
    }
    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        PoseBoneTransform result=new PoseBoneTransform();
        for (IAnimation ia : subAnimations) {
            PoseBoneTransform pbt=ia.getBoneTransform(boneName);
            if (pbt==null)
                continue;
            result.X0+=pbt.X0;
            result.Y0+=pbt.Y0;
            result.Z0+=pbt.Z0;
            result.X1+=pbt.X1;
            result.Y1+=pbt.Y1;
            result.Z1+=pbt.Z1;
            result.X2+=pbt.X2;
            result.Y2+=pbt.Y2;
            result.Z2+=pbt.Z2;
            result.TX0+=pbt.TX0;
            result.TY0+=pbt.TY0;
            result.TZ0+=pbt.TZ0;
        }

        return result;
    }

    @Override
    public void update(double deltaTime) {
        for (IAnimation ia : subAnimations)
            ia.update(deltaTime);
    }
}
