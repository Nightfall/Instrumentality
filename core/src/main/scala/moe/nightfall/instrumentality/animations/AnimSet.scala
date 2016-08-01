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

import java.io.{ByteArrayOutputStream, DataInputStream, DataOutputStream, IOException}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import moe.nightfall.instrumentality.{Main, PoseBoneTransform}

import scala.collection.mutable
import scala.collection.mutable.HashMap

/**
 * A set of poses that can be parented to each other & such
 * Created on 11/09/15.
 */
class AnimSet {
    // Basic pose data
    val allPoses = new HashMap[String, KeyframeAnimationData]
    val poseParents = new HashMap[String, String]

    // NOTE: The parent of a keyframe animation is considered in the editor by it's last frame.
    //       This is so things like bowPull make sense. :)

    allPoses.put("idle", new KeyframeAnimationData)
    allPoses.put("lookLR", new KeyframeAnimationData)
    poseParents.put("lookLR", "idle")
    allPoses.put("lookUD", new KeyframeAnimationData)
    poseParents.put("lookUD", "idle")
    allPoses.put("holdItem", new KeyframeAnimationData)
    poseParents.put("holdItem", "idle")
    allPoses.put("useItem", new KeyframeAnimationData)
    poseParents.put("useItem", "holdItem")
    allPoses.put("stabItem", new KeyframeAnimationData)
    poseParents.put("stabItem", "holdItem")
    allPoses.put("bowPull", new KeyframeAnimationData)
    poseParents.put("bowPull", "holdItem")
    allPoses.put("bowPullWaiting", new KeyframeAnimationData)
    poseParents.put("bowPullWaiting", "bowPull")

    allPoses.put("walk", new KeyframeAnimationData)
    poseParents.put("walk", "idle")

    allPoses.put("falling", new KeyframeAnimationData)
    poseParents.put("falling", "idle")

    allPoses.put("sneaking", new KeyframeAnimationData)
    poseParents.put("sneaking", "idle")

    allPoses.put("hairLR", new KeyframeAnimationData)
    poseParents.put("hairLR", "idle")

    allPoses.put("firstPerson", new KeyframeAnimationData)
    // Technically violates the purpose of pose parenting, but in most cases this is what you have to design for
    poseParents.put("firstPerson", "holdItem")

    // Note: The editor needs the PoseAnimation separately as something to read the interpolated values from.
    //       The editor will check if there's a keyframe at the point it's editing, and if there is,
    //       then it'll assume the output of this is that PoseAnimation.
    //       Makes sure the editor (generally) edits the values it's supposed to be editing,
    //       though I worry that Vic will wave the immutability wand over it and screw everything up.

    //       TODO : Vic, if you're making PoseAnimation or PoseBoneTransform immutable,
    //              make sure to fix the editor, which uses silly assumptions,
    //              like that when it edits something... it actually edits something, and not just makes a memory-using copy.
    //              <sarcasm>You know, that's SUCH a terrible assumption to make...</sarcasm>
    //              NOTE: It's less severe than it used to be, but, still.

    def createEditAnimation(name: String, point: Double): (PoseAnimation, Animation) = {
        val targ = allPoses.get(name).get
        val f = ((targ.lenFrames - 1) * point).toInt
        // This instance is being passed to the editor.
        val interpol = targ.doInterpolate(f)
        if (poseParents.get(name).isDefined) {
            (interpol, new OverlayAnimation(mutable.MutableList[Animation](interpol, createEditAnimation(poseParents.get(name).get, 1)._2)))
        } else {
            (interpol, interpol)
        }
    }

    @throws(classOf[IOException])
    def save(os: DataOutputStream) {
        os.writeUTF("Lelouch")
        // GZIP does better if we throw in all the data at once. Also, we need finish()
        val baos = new ByteArrayOutputStream()
        val zos = new DataOutputStream(baos)
        allPoses foreach { case (poseKey, animValue) =>
            // if we ever have the unfortunate situation of having multiple versions, here's how to check
            // VERSION NAMES: Lelouch Kallen Yukki Yuno Tomoya(?) Nagisa
            //                ^ CURRENT
            // (Note: Reset because of Keyframe Animation)
            // (Version names should alternate between genders. We're an equal opportunity name-grabber.)
            zos.write(1)
            zos.writeUTF(poseKey)
            zos.writeInt(animValue.lenFrames)
            animValue.frameMap foreach { case (frame, poseValue) => {
                zos.write(1)
                zos.writeInt(frame)
                poseValue.hashMap.filter(_._2.isNotZero).foreach { case (pbtKey, pbtValue) =>
                    zos.write(1)
                    zos.writeUTF(pbtKey)
                    zos.writeDouble(pbtValue.X0)
                    zos.writeDouble(pbtValue.X1)
                    zos.writeDouble(pbtValue.X2)
                    zos.writeDouble(pbtValue.Y0)
                    zos.writeDouble(pbtValue.Y1)
                    zos.writeDouble(pbtValue.Z0)
                    zos.writeDouble(pbtValue.TX0)
                    zos.writeDouble(pbtValue.TY0)
                    zos.writeDouble(pbtValue.TZ0)
                    zos.writeDouble(pbtValue.alphaMul)
                }
                zos.write(0)
            }
            }
            zos.write(0)
        }
        zos.write(0)
        zos.flush()
        val gzos = new GZIPOutputStream(os)
        gzos.write(baos.toByteArray)
        gzos.finish()
    }

    @throws(classOf[IOException])
    def load(dis: DataInputStream) {
        val ver = dis.readUTF()
        if (ver == "Lelouch") {
            // main pose loader is generally very similar
            loadMain(0, new DataInputStream(new GZIPInputStream(dis)))
        } else {
            throw new IOException("Not posedata!")
        }
    }

    def loadForHash(pmxHash: String) = {
        try {
            val stream = classOf[Main].getClassLoader.getResourceAsStream("assets/instrumentality/posesbuiltin/" + pmxHash + ".dat")
            if (stream != null) {
                load(new DataInputStream(stream))
                stream.close()
            }
        } catch {
            case _: IOException =>
        }
    }

    private def loadMain(ver: Int, dis: DataInputStream) = {
        while (dis.read() != 0) {
            val poseName = dis.readUTF()
            val pa = new KeyframeAnimationData
            pa.lenFrames = dis.readInt()
            allPoses.put(poseName, pa)
            while (dis.read() != 0) {
                val kf = new PoseAnimation()
                val frame = dis.readInt()
                pa.frameMap = pa.frameMap + (frame -> kf)
                while (dis.read() != 0) {
                    val pbt = new PoseBoneTransform
                    kf.hashMap.put(dis.readUTF(), pbt)
                    pbt.X0 = dis.readDouble()
                    pbt.X1 = dis.readDouble()
                    pbt.X2 = dis.readDouble()
                    pbt.Y0 = dis.readDouble()
                    pbt.Y1 = dis.readDouble()
                    pbt.Z0 = dis.readDouble()
                    pbt.TX0 = dis.readDouble()
                    pbt.TY0 = dis.readDouble()
                    pbt.TZ0 = dis.readDouble()
                    pbt.alphaMul = dis.readDouble()
                }
            }
        }
    }
}
