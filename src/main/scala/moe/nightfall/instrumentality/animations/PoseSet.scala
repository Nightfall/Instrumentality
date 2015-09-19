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
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map

/**
 * A set of poses that can be parented to each other & such
 * Created on 11/09/15.
 */
class PoseSet {
    val allPoses = new HashMap[String, PoseAnimation]
    val poseParents = new HashMap[String, String]
    
    allPoses.put("idle", new PoseAnimation)
    allPoses.put("lookL", new PoseAnimation)
    poseParents.put("lookL", "idle")
    allPoses.put("lookR", new PoseAnimation)
    poseParents.put("lookR", "idle")
    allPoses.put("lookU", new PoseAnimation)
    poseParents.put("lookU", "idle")
    allPoses.put("lookD", new PoseAnimation)
    poseParents.put("lookD", "idle")
    allPoses.put("holdItem", new PoseAnimation)
    poseParents.put("holdItem", "idle")
    allPoses.put("useItemP25", new PoseAnimation)
    poseParents.put("useItemP25", "holdItem")
    allPoses.put("useItemP50", new PoseAnimation)
    poseParents.put("useItemP50", "useItemP25")
    allPoses.put("useItemP75", new PoseAnimation)
    poseParents.put("useItemP75", "useItemP50")
    allPoses.put("stabItem", new PoseAnimation)
    poseParents.put("stabItem", "holdItem")
    allPoses.put("bowPullSItem", new PoseAnimation)
    poseParents.put("bowPullSItem", "holdItem")
    allPoses.put("bowPullEItem", new PoseAnimation)
    poseParents.put("bowPullEItem", "bowPullSItem")
    
    allPoses.put("walkStart", new PoseAnimation)
    poseParents.put("walkStart", "idle")
    
    // when left foot hits ground, R ankle should be lifted but toes not actually off ground at this point
    allPoses.put("walkLFHit", new PoseAnimation)
    poseParents.put("walkLFHit", "idle")
    
    // Left and right feet are both central from a top-down perspective,
    // from side, right is raised
    allPoses.put("walkLFMidpoint", new PoseAnimation)
    poseParents.put("walkLFMidpoint", "idle")
    
    // right foot hits ground, (see walkLFHit)
    allPoses.put("walkRFHit", new PoseAnimation)
    poseParents.put("walkRFHit", "idle")
    
    // Right foot reaches midpoint, see walkLFMidpoint but flip which feet are involved
    allPoses.put("walkRFMidpoint", new PoseAnimation)
    poseParents.put("walkRFMidpoint", "idle")
    
    allPoses.put("falling", new PoseAnimation)
    poseParents.put("falling", "idle")
    
    allPoses.put("sneaking", new PoseAnimation)
    poseParents.put("sneaking", "idle")
    
    allPoses.put("hairL", new PoseAnimation)
    poseParents.put("hairL", "idle")
    
    allPoses.put("hairR", new PoseAnimation)
    poseParents.put("hairR", "idle")

    def createAnimation(poseStrengths: Map[String, Double]): Animation = {
        val rootOA = new OverlayAnimation
        val hashMap = new HashMap[String, Double]
        poseStrengths foreach { entry =>
            var n = Option(entry._1)
            var tVal = entry._2
            while (n != None) {
                val d = hashMap get (n.get)
                if(d == None || d.get < tVal)
                    hashMap.put(n.get, tVal)
                n = poseParents get (n.get)
                if (tVal > 1.0d)
                    tVal = 1.0d
            }
        }
        hashMap foreach { case (key, value) =>
            rootOA.subAnimations.add(new StrengthMultiplyAnimation((allPoses get key).get, value.toFloat))
        }
        
        rootOA
    }

    def createEditAnimation(name: String) = {
        val hashMap = new HashMap[String, Double]
        var n = Option(name)
        while (n != None) {
            hashMap.put(n.get, 1.0)
            n = poseParents get (n.get)
        }
        createAnimation(hashMap)
    }

    @throws(classOf[IOException])
    def save(os: DataOutputStream) {
        os.writeUTF("Lelouch")
        allPoses foreach { case (poseKey, poseValue) =>
            // if we ever have the unfortunate situation of having multiple versions, here's how to check
            // VERSION NAMES: Lelouch Kallen Yukki Yuno
            // (Version names should alternate between genders. We're an equal opportunity name-grabber.)
            os.write(1)
            os.writeUTF(poseKey)
            poseValue.hashMap forEach { case (pbtKey, pbtValue) =>
                os.write(1)
                os.writeUTF(pbtKey)
                os.writeDouble(pbtValue.X0)
                os.writeDouble(pbtValue.X1)
                os.writeDouble(pbtValue.X2)
                os.writeDouble(pbtValue.Y0)
                os.writeDouble(pbtValue.Y1)
                os.writeDouble(pbtValue.Z0)
                os.writeDouble(pbtValue.TX0)
                os.writeDouble(pbtValue.TY0)
                os.writeDouble(pbtValue.TZ0)
            }
            os.write(0)
        }
        os.write(0)
    }

    @throws(classOf[IOException])
    def load(dis: DataInputStream) {
        val ver = dis.readUTF()
        if (ver == "Lelouch") {
            while (dis.read() != 0) {
                val poseName = dis.readUTF()
                val pa = new PoseAnimation
                allPoses.put(poseName, pa)
                while (dis.read() != 0) {
                    val pbt = new PoseBoneTransform
                    pa.hashMap.put(dis.readUTF(), pbt)
                    pbt.X0 = dis.readDouble()
                    pbt.X1 = dis.readDouble()
                    pbt.X2 = dis.readDouble()
                    pbt.Y0 = dis.readDouble()
                    pbt.Y1 = dis.readDouble()
                    pbt.Z0 = dis.readDouble()
                    pbt.TX0 = dis.readDouble()
                    pbt.TY0 = dis.readDouble()
                    pbt.TZ0 = dis.readDouble()
                }
            }
        } else {
            throw new IOException("Not posedata!")
        }
    }
}
