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

import moe.nightfall.instrumentality.PoseBoneTransform

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

    // Find the transition we're in.
    // If both int values are the same, there is no transition.
    // In this case, the value may actually be invalid, if there are no frames at all.
    // The float value is for interpolation.
    // It is 0 in the case where there is no transition, but this should not be relied upon.
    def getEarlyLate(frame: Int, subframe: Float): (Int, Int, Float) = {
        if (frameMap.contains(frame))
            return (frame, frame, 0)
        var earlyFrame = Int.MaxValue
        var lateFrame = Int.MinValue
        for ((k: Int, v: PoseAnimation) <- frameMap) yield {
            // If we're earlier than our current earliest, but later than the target...
            if (k < earlyFrame)
                if (k > frame)
                    earlyFrame = k
            // If we're later than our latest, but earlier than the target...
            if (k > lateFrame)
                if (k < frame)
                    lateFrame = k
        }
        if (earlyFrame == Int.MaxValue) {
            if (lateFrame == Int.MinValue)
                return (frame, frame, 0) // ...
            return (lateFrame, lateFrame, 0)
        }
        if (lateFrame == Int.MinValue)
            return (earlyFrame, earlyFrame, 0)
        // Simple 3-frame "example case" to explain to myself how this works
        // For frame 1, subframe 0.5, we'd want a value of 0.75
        // so it's subframe / (amount of frames-1)
        val framesM1 = lateFrame - earlyFrame
        (earlyFrame, lateFrame, ((frame - earlyFrame) + subframe) / framesM1.toFloat)
    }

    // Creates or retrieves an interpolated PoseAnimation.
    // Note that the data-linking done in earlier versions is gone.
    // Now, the instance is *NOT* "linked".

    def doInterpolate(frame: Int): PoseAnimation = {
        val earlyLate = getEarlyLate(frame, 0)
        if (earlyLate._1 == earlyLate._2)
            return new PoseAnimation(frameMap.getOrElse(earlyLate._1, new PoseAnimation()))
        new PoseAnimation(frameMap(earlyLate._1), frameMap(earlyLate._2), earlyLate._3)
    }

    // Allows getting a specific bone transform.
    // Useful if you don't want the memory overhead of a whole PoseAnimation.
    def getBoneTransform(boneName: String, pos: Float): Option[PoseBoneTransform] = {
        val frame = pos * (lenFrames - 1)
        val framei = Math.floor(frame).toInt
        val subframe = frame - framei

        val earlyLate = getEarlyLate(framei, subframe)
        if (earlyLate._1 == earlyLate._2) {
            val g = frameMap.get(earlyLate._1)
            if (g.isDefined)
                return g.get.getBoneTransform(boneName)
            return None
        }
        val pbtA = frameMap(earlyLate._1).getBoneTransform(boneName).orNull
        val pbtB = frameMap(earlyLate._2).getBoneTransform(boneName).orNull
        if (pbtA == null)
            if (pbtB == null)
                return None
        Some(new PoseBoneTransform(pbtA, pbtB, earlyLate._3))
    }
}
