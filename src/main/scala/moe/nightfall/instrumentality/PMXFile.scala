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
package moe.nightfall.instrumentality

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import PMXFile._

/**
 * A loaded PMX file.
 * Note that this is separate from a model playing an animation, so to reduce memory usage, create one of these and attach models to it.
 *
 * @author gamemanj
 *         Created on 24/07/15.
 */

object PMXFile {
     class PMXVertex(val vxId : Int) {
        var posX, posY, posZ : Float = _
        var normalX, normalY, normalZ : Float = _
        var texU, texV : Float = _
        var edgeScale : Float = _
        var weightType : Int = _
        var boneIndices = new Array[Int](4)
        var boneWeights = new Array[Float](4)
        var sdefCX, sdefCY, sdefCZ : Float = _
        var sdefR0X, sdefR0Y, sdefR0Z : Float = _
        var sdefR1X, sdefR1Y, sdefR1Z : Float = _
    }

    class PMXMaterial(val matId : Int) {
        var localName, globalName : String = _
        var diffR, diffG, diffB, diffA : Float = _
        var specR, specG, specB : Float = _
        var specStrength : Float = _
        var ambR, ambG, ambB : Float = _
        var drawMode : Int = _
        var edgeR, edgeG, edgeB, edgeA : Float = _
        var edgeSize : Float = _
        var texTex, texEnv, texToon : String = _ // the texture pool isn't needed
        var envMode : Int = _
        // toonFlag uses toonIndex if 1, texToon if 0
        var toonFlag, toonIndex : Int = _
        var memo : String = _
        var faceCount : Int = _ // Note that this is divided by 3 to match with faceData
    }

    class PMXBone(val boneId : Int) {
        var localName, globalName : String = _
        var posX, posY, posZ : Float = _
        var parentBoneIndex : Int = _
        var transformLevel : Int = _
        var flagConnection, flagRotatable, 
          flagMovable, flagDisplay, flagCanOperate, flagIK, 
          flagAddLocalDeform, flagAddRotation, flagAddMovement, flagFixedAxis, 
          flagLocalAxis, flagPhysicalTransform, flagExternalParentTransform 
        : Boolean = _
        
        // Connection | Set
        var connectionIndex : Int = _
        // Connection | Unset
        var connectionPosOfsX, connectionPosOfsY, connectionPosOfsZ : Float = _
        // Add Rotation | Set
        var addRotationParentIndex : Int = _
        var addRotationRate : Float = _
        // Fixed Axis | Set
        var fixedAxisX, fixedAxisY, fixedAxisZ : Float = _
        // Local Axis | Set
        var localAxisXX, localAxisXY, localAxisXZ : Float = _
        var localAxisZX, localAxisZY, localAxisZZ : Float = _
        // External Parent Transform | Set
        var externalParentTransformKeyValue : Int = _
        // IK Data not included
    }
}

class PMXFile private {
    var localCharname, globalCharname : String = _
    var localComment, globalComment : String = _
    var vertexData : Array[PMXVertex] = _
    var faceData : Array[Array[Int]] = _
    var matData : Array[PMXMaterial] = _
    var boneData : Array[PMXBone] = _

    def this(pmxFile : Array[Byte]) = { this()
      val bb = ByteBuffer.wrap(pmxFile)
      bb.order(ByteOrder.LITTLE_ENDIAN)
      if (bb.get() != 'P' || bb.get() != 'M' || bb.get() != 'X' || bb.get() != ' ')
          throw new IOException("Not PMX file")
      if (bb.getFloat() != 2.0f)
          throw new IOException("Sorry, only V2.0 PMX files supported");
      if (bb.get() != 8)
          throw new IOException("Data Count should be 8");
      val textEncoding = bb.get()
      if (bb.get() != 0)
          throw new IOException("We don't have any way to handle extended UVs, thus denying them");
      val vertexIS = bb.get()
      val textureIS = bb.get()
      val materialIS = bb.get()
      val boneIS = bb.get()
      val morphIS = bb.get()
      val rigidIS = bb.get()
      
      localCharname = getText(bb, textEncoding)
      globalCharname = getText(bb, textEncoding)
      localComment = getText(bb, textEncoding)
      globalComment = getText(bb, textEncoding)
      vertexData = new Array(bb.getInt())
      
      for (i <- 0 until vertexData.length) {
          vertexData(i) = readVertex(i, bb, boneIS)
      }
      
      val faceVData = bb.getInt()
      if (faceVData % 3 != 0)
          throw new IOException("Invalid facecount, must be multiple of 3")
      faceData = Array.ofDim(faceVData / 3, 3)
      for (i <- 0 until faceVData by 3) {
          faceData(i / 3)(0) = getIndex(bb, vertexIS)
          faceData(i / 3)(1) = getIndex(bb, vertexIS)
          faceData(i / 3)(2) = getIndex(bb, vertexIS)
      }
      
      val texData = new Array[String](bb.getInt())
      for (i <- 0 until texData.length)
          texData(i) = getText(bb, textEncoding)
      
      matData = new Array[PMXMaterial](bb.getInt())
      for (i <- 0 until matData.length)
          matData(i) = readMaterial(i, bb, textEncoding, textureIS, texData)
      
      boneData = new Array[PMXBone](bb.getInt())
      for (i <- 0 until boneData.length)
          boneData(i) = readBone(i, bb, textEncoding, boneIS)
    }

    private def readBone(id : Int, bb : ByteBuffer, textEncoding : Int, boneIS : Int) : PMXBone = {
        val pb = new PMXBone(id)
        pb.localName = getText(bb, textEncoding)
        pb.globalName = getText(bb, textEncoding)
        System.out.println(pb.globalName)
        pb.posX = -bb.getFloat()
        pb.posY = bb.getFloat()
        pb.posZ = bb.getFloat()
        pb.parentBoneIndex = getIndex(bb, boneIS)
        pb.transformLevel = bb.getInt()
        val i = bb.getShort()
        pb.flagConnection = (i & 0x0001) != 0
        pb.flagRotatable = (i & 0x0002) != 0
        pb.flagMovable = (i & 0x0004) != 0
        pb.flagDisplay = (i & 0x0008) != 0
        pb.flagCanOperate = (i & 0x0010) != 0
        pb.flagIK = (i & 0x0020) != 0
        pb.flagAddLocalDeform = (i & 0x0080) != 0
        pb.flagAddRotation = (i & 0x0100) != 0
        pb.flagAddMovement = (i & 0x0200) != 0
        pb.flagFixedAxis = (i & 0x0400) != 0
        pb.flagLocalAxis = (i & 0x0800) != 0
        pb.flagPhysicalTransform = (i & 0x1000) != 0
        pb.flagExternalParentTransform = (i & 0x2000) != 0
        if (pb.flagConnection) {
            pb.connectionIndex = getIndex(bb, boneIS)
        } else {
            pb.connectionPosOfsX = -bb.getFloat()
            pb.connectionPosOfsY = bb.getFloat()
            pb.connectionPosOfsZ = bb.getFloat()
        }
        if (pb.flagAddRotation) {
            pb.addRotationParentIndex = getIndex(bb, boneIS)
            pb.addRotationRate = bb.getFloat()
        }
        if (pb.flagFixedAxis) {
            pb.fixedAxisX = -bb.getFloat()
            pb.fixedAxisY = bb.getFloat()
            pb.fixedAxisZ = bb.getFloat()
        }
        if (pb.flagLocalAxis) {
            // TODO: What are these values, and do they need to be X-flipped
            pb.localAxisXX = bb.getFloat()
            pb.localAxisXY = bb.getFloat()
            pb.localAxisXZ = bb.getFloat()
            pb.localAxisZX = bb.getFloat()
            pb.localAxisZY = bb.getFloat()
            pb.localAxisZZ = bb.getFloat()
        }
        if (pb.flagExternalParentTransform)
            pb.externalParentTransformKeyValue = bb.getInt()
        if (pb.flagIK) {
            getIndex(bb, boneIS)
            bb.getInt()
            bb.getFloat()
            val lc = bb.getInt()
            for (i2 <- 0 until lc) {
                getIndex(bb, boneIS)
                if (bb.get() != 0) {
                    bb.getFloat()
                    bb.getFloat()
                    bb.getFloat()
                    bb.getFloat()
                    bb.getFloat()
                    bb.getFloat()
                }
            }
        }
        return pb;
    }

    private def readMaterial(id : Int, bb : ByteBuffer, textEncoding : Int, textureIS : Int, tex : Array[String]) : PMXMaterial = {
        val pm = new PMXMaterial(id);
        pm.localName = getText(bb, textEncoding)
        pm.globalName = getText(bb, textEncoding)
        pm.diffR = bb.getFloat()
        pm.diffG = bb.getFloat()
        pm.diffB = bb.getFloat()
        pm.diffA = bb.getFloat()
        pm.specR = bb.getFloat()
        pm.specG = bb.getFloat()
        pm.specB = bb.getFloat()
        pm.specStrength = bb.getFloat()
        pm.ambR = bb.getFloat()
        pm.ambG = bb.getFloat()
        pm.ambB = bb.getFloat()
        pm.drawMode = bb.get()
        pm.edgeR = bb.getFloat()
        pm.edgeG = bb.getFloat()
        pm.edgeB = bb.getFloat()
        pm.edgeA = bb.getFloat()
        pm.edgeSize = bb.getFloat()
        var ind = getIndex(bb, textureIS)
        pm.texTex = if (ind < 0) null else tex(ind)
        ind = getIndex(bb, textureIS)
        pm.texEnv = if (ind < 0) null else tex(ind)
        pm.envMode = bb.get()
        pm.toonFlag = bb.get()
        pm.toonFlag match {
            case 0 =>
                ind = getIndex(bb, textureIS)
                pm.texToon = if (ind < 0) null else tex(ind)
            case 1 =>
                pm.toonIndex = bb.get()
            case _ => throw new IOException("Unknown ToonFlag " + pm.toonFlag)
        }
        pm.memo = getText(bb, textEncoding)
        pm.faceCount = bb.getInt()
        if ((pm.faceCount % 3) != 0)
            throw new IOException("Material facecount % 3 != 0")
        pm.faceCount /= 3
        return pm
    }

    private def readVertex(id : Int, bb : ByteBuffer, boneIS : Int) : PMXVertex = {
        val pmxVertex = new PMXVertex(id)
        pmxVertex.posX = -bb.getFloat()
        pmxVertex.posY = bb.getFloat()
        pmxVertex.posZ = bb.getFloat()
        pmxVertex.normalX = -bb.getFloat()
        pmxVertex.normalY = bb.getFloat()
        pmxVertex.normalZ = bb.getFloat()
        pmxVertex.texU = bb.getFloat()
        pmxVertex.texV = bb.getFloat()
        pmxVertex.weightType = bb.get()
        pmxVertex.weightType match {
            case 0 => pmxVertex.boneIndices(0) = getIndex(bb, boneIS)
            case 1 =>
                pmxVertex.boneIndices(0) = getIndex(bb, boneIS)
                pmxVertex.boneIndices(1) = getIndex(bb, boneIS)
                pmxVertex.boneWeights(0) = bb.getFloat()
            case 2 =>
                pmxVertex.boneIndices(0) = getIndex(bb, boneIS)
                pmxVertex.boneIndices(1) = getIndex(bb, boneIS)
                pmxVertex.boneIndices(2) = getIndex(bb, boneIS)
                pmxVertex.boneIndices(3) = getIndex(bb, boneIS)
                pmxVertex.boneWeights(0) = bb.getFloat()
                pmxVertex.boneWeights(1) = bb.getFloat()
                pmxVertex.boneWeights(2) = bb.getFloat()
                pmxVertex.boneWeights(3) = bb.getFloat()
            case 3 =>
                pmxVertex.boneIndices(0) = getIndex(bb, boneIS)
                pmxVertex.boneIndices(1) = getIndex(bb, boneIS)
                pmxVertex.boneWeights(0) = bb.getFloat()
                pmxVertex.sdefCX = -bb.getFloat()
                pmxVertex.sdefCY = bb.getFloat()
                pmxVertex.sdefCZ = bb.getFloat()
                pmxVertex.sdefR0X = -bb.getFloat()
                pmxVertex.sdefR0Y = bb.getFloat()
                pmxVertex.sdefR0Z = bb.getFloat()
                pmxVertex.sdefR1X = -bb.getFloat()
                pmxVertex.sdefR1Y = bb.getFloat()
                pmxVertex.sdefR1Z = bb.getFloat()
            case _ => throw new IOException("unknown weight def " + pmxVertex.weightType)
        }
        pmxVertex.edgeScale = bb.getFloat()
        return pmxVertex
    }


    private def getText(bb : ByteBuffer, textEncoding : Int) : String = {
        val data = new Array[Byte](bb.getInt())
        bb.get(data)
        try {
            return new String(data, if (textEncoding == 1) "UTF-8" else "UTF-16LE")
        } catch {
            case e : UnsupportedEncodingException =>
              e.printStackTrace()
              return "~ unsupported encoding ~"
        }
    }

    private def getIndex(bb : ByteBuffer, tpe : Int) : Int = {
        return tpe match {
            case 1 => bb.get()
            case 2 => bb.getShort()
            case 4 => bb.getInt()
            case _ => throw new IOException("unknown index type " + tpe)
        }
    }
}
