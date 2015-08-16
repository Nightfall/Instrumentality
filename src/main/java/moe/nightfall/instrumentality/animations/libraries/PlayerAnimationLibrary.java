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
        pbt = new PoseBoneTransform();
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
        return new String[]{"idle", "use", "block"};
    }
}
