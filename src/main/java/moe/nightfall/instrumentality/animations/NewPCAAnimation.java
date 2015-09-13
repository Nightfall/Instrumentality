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

import java.util.HashMap;

/**
 * Created on 13/09/15.
 */
public class NewPCAAnimation implements IAnimation {
    public PoseSet poseSet;
    private double walkCycleTime;
    public double walkStrength, walkSpeed, lookLR, lookUD, fallStrength;

    public NewPCAAnimation(PoseSet ps) {
        poseSet = ps;
    }

    public IAnimation poseSetResult = new IAnimation() {
        @Override
        public PoseBoneTransform getBoneTransform(String boneName) {
            return null;
        }

        @Override
        public void update(double deltaTime) {
        }
    };

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        return poseSetResult.getBoneTransform(boneName);
    }

    public double getInterpolate(double time, double sine) {
        double interpolResult = time * (1 - sine);
        interpolResult += ((1 - Math.cos(time * Math.PI)) / 2) * sine;
        return interpolResult;
    }

    public void setupCycle(double time2, String[] subAnims, double sine, double strength, HashMap<String, Double> hashMap) {
        while (time2 < 0)
            time2 += 1;
        while (time2 > 1)
            time2 -= 1;
        double segment = 1.0d / (subAnims.length - 1);
        int currentStart = (int) Math.floor(time2 / segment);
        int currentEnd = currentStart + 1;
        double strengthEnd = getInterpolate((time2 - (currentStart * segment)) / segment, sine);
        currentStart %= subAnims.length;
        currentEnd %= subAnims.length;
        double strengthStart = 1.0d - strengthEnd;
        strengthStart *= strength;
        strengthEnd *= strength;
        hashMap.put(subAnims[currentStart], strengthStart);
        hashMap.put(subAnims[currentEnd], strengthEnd);
    }

    @Override
    public void update(double deltaTime) {
        walkCycleTime += deltaTime * walkSpeed;
        HashMap<String, Double> hashMap = new HashMap<String, Double>();
        hashMap.put("idle", 1d);
        if (lookLR < 0) {
            hashMap.put("lookR", -lookLR);
        } else if (lookLR > 0) {
            hashMap.put("lookL", lookLR);
        }
        if (lookUD < 0) {
            hashMap.put("lookD", -lookUD);
        } else if (lookUD > 0) {
            hashMap.put("lookU", lookUD);
        }
        setupCycle(walkCycleTime, new String[]{"walkLFHit", "walkRFMidpoint", "walkRFHit", "walkLFMidpoint", "walkLFHit"}, 0.5d, walkStrength, hashMap);
        hashMap.put("falling", fallStrength);
        poseSetResult = poseSet.createAnimation(hashMap);
    }
}
