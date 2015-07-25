package uk.co.gamemanj.instrumentality;

/**
 * Created on 25/07/15.
 */
public class FadeInAnimation implements IAnimation {
    public double mulAmount=0,speed=1.0d;
    public IAnimation beingFaded;

    public FadeInAnimation(IAnimation wa) {
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
        return pbt;
    }

    @Override
    public void update(double deltaTime) {
        mulAmount+=deltaTime*speed;
        if (mulAmount>1.0d)
            mulAmount=1.0d;
        beingFaded.update(deltaTime);
    }
}
