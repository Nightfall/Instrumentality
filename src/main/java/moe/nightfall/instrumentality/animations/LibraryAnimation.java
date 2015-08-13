package moe.nightfall.instrumentality.animations;

import moe.nightfall.instrumentality.PoseBoneTransform;

/**
 * Created on 28/07/15.
 */
public class LibraryAnimation implements IAnimation {
    private IAnimation currentPose;
    private IAnimation lastPose;
    public float transitionValue, transitionSpeed = 4.0f;

    public void setCurrentPose(IAnimation ia, float transitionTime, boolean skipAheadPrevious) {
        transitionSpeed=1.0f/transitionTime;
        if (transitionValue >= 1.0f) {
            lastPose = currentPose;
        } else if (transitionValue <= 0.0f) {
            // Do nothing, lastPose stays the same
        } else {
            if (skipAheadPrevious) {
                lastPose = currentPose;
            } else {
                IAnimation oA = currentPose; // modulated by transitionValue
                IAnimation oB = lastPose; // modulated by 1-transitionValue
                if (oA != null)
                    oA = new StrengthMultiplyAnimation(oA, transitionValue);
                if (oB != null)
                    oB = new StrengthMultiplyAnimation(oB, 1 - transitionValue);
                // Um, we can't save the transitioned state as an IAnimation without indexing the thing...
                // Or, just freezing the transition at that state via 2 StrengthMultiplies and an Overlay.
                // Another reason I like being able to structure animations like this.
                // NOTE: Technically, if you keep on changing, you can freeze multiple layers of animations like this.
                if (oA != null) {
                    if (oB != null) {
                        lastPose = new OverlayAnimation(new IAnimation[]{oA, oB});
                    } else {
                        lastPose = new OverlayAnimation(new IAnimation[]{oA});
                    }
                } else {
                    if (oB != null) {
                        lastPose = new OverlayAnimation(new IAnimation[]{oB});
                    } else {
                        lastPose = null;
                    }
                }
            }
        }
        currentPose = ia;
        transitionValue = 0.0f;
    }

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        PoseBoneTransform A = null;
        if (lastPose != null)
            A = lastPose.getBoneTransform(boneName);
        PoseBoneTransform B = null;
        if (currentPose != null)
            B = currentPose.getBoneTransform(boneName);
        return new PoseBoneTransform(A, B, transitionValue);
    }

    @Override
    public void update(double deltaTime) {
        transitionValue += deltaTime * transitionSpeed;
        if (transitionValue >= 1.0f)
            transitionValue = 1.0f;
    }
}
