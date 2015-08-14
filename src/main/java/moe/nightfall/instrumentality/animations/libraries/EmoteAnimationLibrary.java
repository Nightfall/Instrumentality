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
            pa.hashMap.put("l_shouler", pbt);
            pbt = new PoseBoneTransform();
            pbt.X0 = 0.63f;
            pbt.Y0 = 0.435f;
            pa.hashMap.put("l_ellbow", pbt);
            return pa;
        }
        // This pose was actually developed during work on the attempt at a gendo...
        // Also: I think upperclass shock?
        if (poseName.equalsIgnoreCase("creepy_laugh")) {
            PoseAnimation pa = PlayerAnimationLibrary.createIdlePoseAnimation();
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(20);
            pbt.Y0 = (float) Math.toRadians(-30);
            pa.hashMap.put("l_shouler", pbt);
            pbt = new PoseBoneTransform();
            pbt.Y0 = -0.39f;
            pbt.Z0 = -3.12f;
            pbt.X1 = 0.81f;
            pbt.Y1 = 1.23f;
            pa.hashMap.put("l_ellbow", pbt);
            return pa;
        }
        return null;
    }

    @Override
    public String[] getPoses() {
        return new String[]{"hai", "creepy_laugh"};
    }
}
