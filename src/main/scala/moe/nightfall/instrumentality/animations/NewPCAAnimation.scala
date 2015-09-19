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
import scala.collection.mutable.Map
import scala.collection.mutable.HashMap

/**
 * Created on 13/09/15.
 */
class NewPCAAnimation(var poseSet: PoseSet) extends Animation {
    private var walkCycleTime: Double = _
    var walkStrength, walkSpeed, lookLR, lookUD, fallStrength: Double = _

    var poseSetResult = new Animation {
        override def getBoneTransform(boneName: String) = null
        override def update(deltaTime: Double) {}
    }

    override def getBoneTransform(boneName: String) = poseSetResult getBoneTransform boneName

    def getInterpolate(time: Double, sine: Double) =
        time*(1 - sine) + ((1 - Math.cos(time * Math.PI)) / 2)*sine

    def setupCycle(time: Double, subAnims: Array[String], sine: Double, strength: Double, map: Map[String, Double]) {
        val time2 =
            if (time >= 0)
                time % 1.0
            else
                1 + (time % 1.0)
        val segment = 1.0d / (subAnims.length - 1)
        var currentStart = Math.floor(time2 / segment).toInt
        var currentEnd = currentStart + 1
        var strengthEnd = getInterpolate((time2 - (currentStart * segment)) / segment, sine)
        currentStart %= subAnims.length
        currentEnd %= subAnims.length
        var strengthStart = 1.0d - strengthEnd
        strengthStart *= strength
        strengthEnd *= strength
        map.put(subAnims(currentStart), strengthStart)
        map.put(subAnims(currentEnd), strengthEnd)
    }

    override def update(deltaTime: Double) {
        walkCycleTime += deltaTime * walkSpeed
        val map = new HashMap[String, Double]
        map.put("idle", 1d)
        if (lookLR < 0)
            map.put("lookR", -lookLR)
        else if (lookLR > 0)
            map.put("lookL", lookLR)
        
        if (lookUD < 0)
            map.put("lookD", -lookUD)
        else if (lookUD > 0)
            map.put("lookU", lookUD)
        
        setupCycle(walkCycleTime, Array("walkLFHit", "walkRFMidpoint", "walkRFHit", "walkLFMidpoint", "walkLFHit"), 0.5, walkStrength, map)
        map.put("falling", fallStrength)
        poseSetResult = poseSet.createAnimation(map)
    }
}