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

import scala.collection.mutable
import scala.collection.mutable.{HashMap, Map}

/**
 * Created on 13/09/15.
 */
class NewPCAAnimation(var poseSet: AnimSet) extends Animation {

    private var walkCycleTime: Double = _
    private var idleCycleTime: Double = _
    var lookLR, lookUD, fallStrength: Double = _
    var walkStrengthTarget = 0d
    var swingTime = -1d
    var itemHoldStrengthTarget = 0d
    var itemHoldStrength = 0d

    val walkAnimation = new KeyframeAnimation(poseSet.allPoses("walk"), false, true, 1.0d)
    val walkStrength = new StrengthMultiplyAnimation(walkAnimation)

    val idleAnimation = new KeyframeAnimation(poseSet.allPoses("idle"), false, true, 0.25d)

    val rootAnimation = new OverlayAnimation(idleAnimation, walkStrength)

    override def getBoneTransform(boneName: String) = rootAnimation.getBoneTransform(boneName)

    def strengthChange(deltaTime: Double, in: Double, targ: Double) = {
        var res = in
        if (res < targ) {
            res += deltaTime
            if (res > targ)
                res = targ
        } else {
            res -= deltaTime
            if (res < targ)
                res = targ
        }
        res
    }

    def advancePos(deltaTime: Double, fl: Float, pos: KeyframeAnimation) {
        pos.pos += deltaTime * fl
        if (pos.pos < 0)
            pos.pos = 0
        if (pos.pos > 1)
            pos.pos = 1
    }

    override def update(deltaTime: Double) {

        /*
         * NOTE: This code has to keep in mind the structure of the poses,
         * since there's no magic thing to add parents if we miss one.
         * There's a reference in AnimSet, or just open up the workbench and look at the PoseTree.
         * Each animation needs it's parent and that parent etc. to be at greater or equal strength to itself.
         * More or less.
         * TBH, just keep the strength modifiers at a minimum, try to use positions where possible.
         * It's more flexible that way.
         */
        //        map.put("falling", fallStrength)
        //        if (lookLR < 0)
        //            map.put("lookR", -lookLR)
        //        else if (lookLR > 0)
        //            map.put("lookL", lookLR)

        //        if (lookUD < 0)
        //            map.put("lookD", -lookUD)
        //        else if (lookUD > 0)
        //            map.put("lookU", lookUD)

        walkStrength.mulAmount = strengthChange(deltaTime * 4, walkStrength.mulAmount, walkStrengthTarget)
        itemHoldStrength = strengthChange(deltaTime, itemHoldStrength, itemHoldStrengthTarget)

        //        map.put("holdItem", itemHoldStrength)
        if (swingTime != -1) {
            swingTime += deltaTime * 2
            if (swingTime > 1) {
                swingTime = 1
            } else {
                //                setupCycle(swingTime, Array(null, "useItemP25", "useItemP50", "useItemP75", null), 1, 1, 1, map)
            }
        }
        rootAnimation.update(deltaTime)
    }
}
