/*
 * Copyright (c) 2015, Nightfall Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
        transitionSpeed = 1.0f / transitionTime;
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

    public IAnimation getTarget() {
        return currentPose;
    }
}
