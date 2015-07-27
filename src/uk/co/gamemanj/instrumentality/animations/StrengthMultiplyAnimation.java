package uk.co.gamemanj.instrumentality.animations;

import uk.co.gamemanj.instrumentality.PoseBoneTransform;

/**
 * Created on 25/07/15.
 */
public class StrengthMultiplyAnimation implements IAnimation {
    public float mulAmount=1.0f;
    public IAnimation beingFaded;

    public StrengthMultiplyAnimation(IAnimation wa) {
        beingFaded=wa;
    }

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        PoseBoneTransform pbt=beingFaded.getBoneTransform(boneName);
        if (pbt==null)
            return null;
        pbt.X0*=mulAmount;
        pbt.X1*=mulAmount;
        pbt.X2*=mulAmount;
        pbt.Y0*=mulAmount;
        pbt.Y1*=mulAmount;
        pbt.Y2*=mulAmount;
        pbt.Z0*=mulAmount;
        pbt.Z1*=mulAmount;
        pbt.Z2*=mulAmount;
        pbt.TX0*=mulAmount;
        pbt.TY0*=mulAmount;
        pbt.TZ0*=mulAmount;
        return pbt;
    }

    @Override
    public void update(double deltaTime) {
        beingFaded.update(deltaTime);
    }
}
