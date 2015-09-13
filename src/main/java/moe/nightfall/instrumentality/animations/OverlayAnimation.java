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

import java.util.LinkedList;

/**
 * Adds the PBTs together.
 * Created on 26/07/15.
 */
public class OverlayAnimation implements IAnimation {
    public LinkedList<IAnimation> subAnimations = new LinkedList<IAnimation>();

    public OverlayAnimation(IAnimation... subAnimations) {
        for (int i = 0; i < subAnimations.length; i++)
            this.subAnimations.add(subAnimations[i]);
    }

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        PoseBoneTransform result = new PoseBoneTransform();
        for (IAnimation ia : subAnimations) {
            PoseBoneTransform pbt = ia.getBoneTransform(boneName);
            if (pbt == null)
                continue;
            result.X0 += pbt.X0;
            result.Y0 += pbt.Y0;
            result.Z0 += pbt.Z0;
            result.X1 += pbt.X1;
            result.Y1 += pbt.Y1;
            result.X2 += pbt.X2;
            result.TX0 += pbt.TX0;
            result.TY0 += pbt.TY0;
            result.TZ0 += pbt.TZ0;
        }

        return result;
    }

    @Override
    public void update(double deltaTime) {
        for (IAnimation ia : subAnimations)
            ia.update(deltaTime);
    }
}
