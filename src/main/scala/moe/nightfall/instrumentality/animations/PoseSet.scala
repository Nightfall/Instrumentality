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

import scala.collection.mutable.{HashMap, Map}

/**
 * A set of poses that can be parented to each other & such
 * Created on 11/09/15.
 */
class PoseSet {
    // Basic pose data, added in version Lelouch (first version)
    val allPoses = new HashMap[String, PoseAnimation]
    val poseParents = new HashMap[String, String]
    // Model ZIP download URL & folder containing PMX within ZIP, added in version Kallen
    // A blank downloadURL means that this is not usable for suggestion.
    // A blank downloadBaseFolder means that the URL *cannot* be automatically followed for some reason,
    // and the URL should instead be given to the web browser on the system.
    // (This mechanism allows creating a listing of recommended models for the user to download,
    //  which should make the mod a LOT easier to use. This method isn't flexible, but it's easy for the user.
    //  Also, this being bundled with the posedata ensure that whenever we suggest a model,
    //  it's a one-click process to use it, as the posedata is where we got the URL from.)
    var downloadURL = ""
    // Note: The value "." indicates that the PMX file is in the base folder.
    var downloadBaseFolder = ""

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
            while (n.isDefined) {
                val d = hashMap get n.get
                if (d.isEmpty || d.get < tVal)
                    hashMap.put(n.get, tVal)
                n = poseParents get n.get
                if (tVal > 1.0d)
                    tVal = 1.0d
            }
        }
        hashMap foreach { case (key, value) =>
            val optVal = allPoses get key
            if (optVal.isDefined) {
                rootOA.subAnimations += new StrengthMultiplyAnimation(optVal.get, value.toFloat)
            } else {
                System.err.println("Warning: Missing pose " + key)
            }
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
        os.writeUTF("Kallen")
        os.writeUTF(downloadURL)
        os.writeUTF(downloadBaseFolder)
        // GZIP does better if we throw in all the data at once. Also, we need finish()
        val baos = new ByteArrayOutputStream()
        val zos = new DataOutputStream(baos)
        allPoses foreach { case (poseKey, poseValue) =>
            // if we ever have the unfortunate situation of having multiple versions, here's how to check
            // VERSION NAMES: Lelouch Kallen Yukki Yuno Tomoya(?) Nagisa
            //                        ^ CURRENT
            // (Version names should alternate between genders. We're an equal opportunity name-grabber.)
            zos.write(1)
            zos.writeUTF(poseKey)
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
            downloadURL = "Unknown";
            loadMain(0, dis)
        } else if (ver == "Kallen") {
            downloadURL = dis.readUTF()
            downloadBaseFolder = dis.readUTF()
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

    private def loadMain(ver: Int, dis: DataInputStream): Unit = {
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
    }
}
