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
package moe.nightfall.instrumentality.animations

/**
 * A keyframe-based animation,
 * which may be based on whatever time scale or otherwise the using code wants.
 * Note that the timescale used is 100 frames in a given animation.
 * There is no FPS. See below.
 * Take it or leave it...
 *
 * NOTE: Since this instance is shared between other instances,
 * and it is modified at the slightest whim,
 * this is not an Animation object.
 *
 * Also, note that the FPS isn't defined for a reason.
 * The idea is that it should be fine to reduce this into an object taking a number from 0 to 1,
 * and giving you a frame.
 * In other words, turning "lookL" and "lookR" into a single entity.
 * It also allows adjusting the way transitions work for things like falling, item-holding, etc.
 * And it's consistent!
 * What more could I ask for :)
 * Created on 12/10/15.
 */

class KeyframeAnimationData {
    // Default. Could be changed, but won't.
    var lenFrames = 100
    // The mapping of integers to keyframes.
    var frameMap = Map[Int, PoseAnimation]()

    // Creates or retrieves an interpolated PoseAnimation.
    // The "or retrieves" part means that you should not modify it.
    def doInterpolate(frame: Int): PoseAnimation = {
        val cFrameAnim = frameMap.getOrElse(frame, new PoseAnimation())
        var earlyFrame = (frame, cFrameAnim)
        var lateFrame = (frame, cFrameAnim)
        for ((k: Int, v: PoseAnimation) <- frameMap) yield {
            if (k < earlyFrame._1)
                earlyFrame = (k, v)
            if (k > lateFrame._1)
                lateFrame = (k, v)
        }
        // If one of the sides returns the frame itself, that's where we are.
        if (earlyFrame._1 == frame)
            return earlyFrame._2
        if (lateFrame._1 == frame)
            return lateFrame._2
        var point = 0f
        if (lateFrame._1 != earlyFrame._1)
            point = (frame - earlyFrame._1) / (lateFrame._1 - earlyFrame._1).toFloat
        new PoseAnimation(earlyFrame._2, lateFrame._2, point)
    }
}
