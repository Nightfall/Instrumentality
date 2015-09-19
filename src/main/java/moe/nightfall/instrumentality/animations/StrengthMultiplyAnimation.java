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
 * Created on 25/07/15.
 */
public class StrengthMultiplyAnimation implements Animation {
    public float mulAmount = 1.0f;
    public Animation beingFaded;

    public StrengthMultiplyAnimation(Animation wa) {
        beingFaded = wa;
    }

    public StrengthMultiplyAnimation(Animation oB, float v) {
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
