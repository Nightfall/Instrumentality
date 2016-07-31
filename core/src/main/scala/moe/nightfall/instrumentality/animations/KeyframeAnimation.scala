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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES LOSS OF USE, DATA, OR PROFITS OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package moe.nightfall.instrumentality.animations

import moe.nightfall.instrumentality.PoseBoneTransform

/**
 * A keyframe animation.
 * Created on 16/10/15.
 */
class KeyframeAnimation(val kad: KeyframeAnimationData, val pingPong: Boolean, var loop: Boolean, var speed: Double) extends Animation {
    var pos = 0d
    var reverse = false

    override def update(dt: Double) {
        pos += speed * (if (reverse) -dt else dt)
        if (pos >= 1) {
            pos = 1
            if (loop)
                if (pingPong)
                    reverse = !reverse
                else
                    pos = 0

            reverse ^= pingPong
        }
        if (pos <= 0) {
            pos = 0
            reverse ^= pingPong
        }
    }

    /**
     * Gets the current transform for a bone.
     * Note that this can be called from any thread, so you shouldn't change state based upon it.
     * The idea is that this is simply a getter.
     *
     * @param boneName The name of a bone.
     * @return None for no transform(optimization), or the transform to apply to the bone.
     */
    override def getBoneTransform(boneName: String): Option[PoseBoneTransform] = kad.getBoneTransform(boneName, pos.toFloat)
}
