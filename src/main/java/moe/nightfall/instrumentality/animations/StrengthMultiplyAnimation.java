package moe.nightfall.instrumentality.animations;

import moe.nightfall.instrumentality.PoseBoneTransform;

/**
 * Created on 25/07/15.
 */
public class StrengthMultiplyAnimation implements IAnimation {
    public float mulAmount = 1.0f;
    public IAnimation beingFaded;

    public StrengthMultiplyAnimation(IAnimation wa) {
        beingFaded = wa;
    }

    public StrengthMultiplyAnimation(IAnimation oB, float v) {
        beingFaded = oB;
        mulAmount = v;
    }

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        PoseBoneTransform pbt = beingFaded.getBoneTransform(boneName);
        if (pbt == null)
            return null;
        // We're going to modify this instance, some things may not like that
        pbt = new PoseBoneTransform(pbt);
        pbt.X0 *= mulAmount;
        pbt.X1 *= mulAmount;
        pbt.X2 *= mulAmount;
        pbt.Y0 *= mulAmount;
        pbt.Y1 *= mulAmount;
        pbt.Z0 *= mulAmount;
        pbt.TX0 *= mulAmount;
        pbt.TY0 *= mulAmount;
        pbt.TZ0 *= mulAmount;
        return pbt;
    }

    @Override
    public void update(double deltaTime) {
        beingFaded.update(deltaTime);
    }
}
