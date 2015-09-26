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

import java.awt.image.BufferedImage
import java.io.{PrintStream, FileOutputStream}

import moe.nightfall.instrumentality.PMXModel._
import moe.nightfall.instrumentality.animations.PoseSet
import org.lwjgl.opengl.GL11

import scala.collection.mutable.MutableList

/**
 * OLD: It would be nice if we could move the FaceGroup generator into this file.
 * NEW: It would be nice if we could move the VBO generation into this file.
 * NEW2: Actually, the resource cost would be higher, then - and since these get cached forever, that's a Bad Thing
 * This is a PMXModel. It's meant to contain everything not specific to a given instance of the model.
 * Created on 19/08/15.
 */

object PMXModel {

    class FaceGroup {
        // Used while creating the FaceGroup, this is discarded after the group is compiled
        val boneSet = collection.mutable.Set[PMXFile.PMXBone]()

        // Buffers for compiling
        val vertexList = MutableList[PMXFile.PMXVertex]()

        // Bone mappings (these are finalized after the material containing this group is compiled)
        var boneMappingFileGroup: Array[Int] = _
        var boneMappingGroupFile: Array[Int] = _

        def get(boneIndice: Int): Int = {
            val res = boneMappingFileGroup(boneIndice)
            if (res == -1) throw new RuntimeException("Bone is being relied on that does not exist within this group.")
            return res;
        }
    }

}

class PMXModel private {
    var theFile: PMXFile = _
    val poses = new PoseSet()

    // This is null'd once setupMaterials is called. Don't modify after this goes into the ModelCache.
    var materialData = collection.mutable.Map[String, BufferedImage]()
    // This is the OpenGL materials hashmap, don't access outside the OpenGL owning thread.
    var materials: Map[String, Int] = _

    /**
     * Height of the model, determinated by the highest vertex
     */
    var height: Float = _

    var groups: Array[MutableList[FaceGroup]] = _

    def this(pf: PMXFile, maxGroupSize: Int) {
        this()
        theFile = pf
        groups = new Array(theFile.matData.length)

        var face = 0
        var height = 0f

        for (i <- 0 until theFile.matData.length) {
            groups(i) = MutableList()
            val mat = theFile.matData(i)
            for (ind <- 0 until mat.faceCount) {
                val vA = theFile.vertexData(theFile.faceData(face)(0))
                val vB = theFile.vertexData(theFile.faceData(face)(1))
                val vC = theFile.vertexData(theFile.faceData(face)(2))
                height = if (vA.posY > height) vA.posY else height
                height = if (vB.posY > height) vB.posY else height
                height = if (vC.posY > height) vC.posY else height

                // Work out which bones are needed.

                val wA = weightVertices(vA.weightType)
                val wB = weightVertices(vB.weightType)
                val wC = weightVertices(vC.weightType)

                val usedBones = collection.mutable.Set[PMXFile.PMXBone]()
                for (i2 <- 0 until wA)
                    usedBones += theFile.boneData(vA.boneIndices(i2))
                for (i2 <- 0 until wB)
                    usedBones += theFile.boneData(vB.boneIndices(i2))
                for (i2 <- 0 until wC)
                    usedBones += theFile.boneData(vC.boneIndices(i2))

                // Ok, now for each group, check how many bones are missing (if none are missing we can break)

                var target: FaceGroup = null
                var bestBoneAdd: FaceGroup = null
                var bestBoneAddRequired = collection.mutable.Set[PMXFile.PMXBone]()
                var bbarWork = collection.mutable.Set[PMXFile.PMXBone]()

                groups(i) takeWhile { fg =>
                    // If the group can contain it, we don't need to do any more.
                    if (usedBones.subsetOf(fg.boneSet)) {
                        target = fg
                        false
                    } else {
                        // Otherwise, check what is missing...
                        bbarWork.clear()
                        usedBones foreach { pb =>
                            if (!fg.boneSet.contains(pb)) {
                                bbarWork += pb
                            }
                        }

                        if (bbarWork.size + fg.boneSet.size <= maxGroupSize) {
                            if (bestBoneAddRequired == null) {
                                bestBoneAdd = fg
                                bestBoneAddRequired = bbarWork
                                bbarWork = collection.mutable.Set[PMXFile.PMXBone]()
                            } else {
                                // If less is missing here than in what we're planning to put it in,
                                // this this is a better choice
                                if (bestBoneAddRequired.size > bbarWork.size) {
                                    bestBoneAdd = fg
                                    bestBoneAddRequired = bbarWork;
                                    bbarWork = collection.mutable.Set[PMXFile.PMXBone]()
                                }
                            }
                        }
                        true
                    }
                }
                if (target == null) {
                    if (bestBoneAdd == null) {
                        // there are no groups we can add to, create a new group
                        // We're not doing the "can it fit" check here, if it can't fit your model is broken/weird
                        if (usedBones.size > maxGroupSize) {
                            System.err.println("WARNING! maxGroupSize of " + maxGroupSize + " is too small for a face which relies on " + usedBones.size + " bones!")
                        } else {
                            target = new FaceGroup()
                            target.boneSet ++= usedBones
                            groups(i) += target
                        }
                    } else {
                        // Well, this is simple :)
                        bestBoneAdd.boneSet ++= bestBoneAddRequired
                        target = bestBoneAdd
                    }
                }

                if (target != null) {
                    // Add the vertices to wherever we want to put them
                    // (the facegroup needs to be complete before we start actually writing buffers)
                    target.vertexList += vC += vB += vA
                } else {
                    sys.error("Faces have been skipped!!! Display artifacting will result.")
                }
                face += 1
            }
            // Pass 2 : clean up the mess
            groups(i) foreach { fg =>
                fg.boneMappingFileGroup = Array.fill(theFile.boneData.length)(-1)
                fg.boneMappingGroupFile = new Array(fg.boneSet.size)
                val it = fg.boneSet.iterator
                for (j <- 0 until fg.boneMappingGroupFile.length) {
                    val k = it.next.boneId
                    fg.boneMappingGroupFile(j) = k
                    fg.boneMappingFileGroup(k) = j
                }
            }
            println(groups(i).size + " facegroups for shading on material " + i)
        }
        this.height = height
        //debugWriteObj()
    }

    private def weightVertices(weightType: Int): Int = {
        return weightType match {
            case 0 => 1
            case 1 => 2
            case 2 => 4
            case _ => 0
        }
    }

    /**
     * Sets up the OpenGL materials, and (hopefully) deallocs the ByteBuffers that used to hold the data.
     * (material setup is done this way to avoid problems with multithreading - on that note,
     * only call from the render thread)
     */
    def setupMaterials() {
        // Sometimes images are reused. The ModelCache stage will ensure that the reused images use the same BufferedImage.
        var imgmap = Map[BufferedImage, Int]()
        val map = for ((k, v) <- materialData) yield {
            if (imgmap.contains(v)) {
                (k, imgmap(v))
            } else {
                val bTex = GL11.glGenTextures()
                imgmap += v -> bTex
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, bTex)
                Loader.writeGLTexImg(v, GL11.GL_NEAREST)
                (k, bTex)
            }
        }

        // Convert to immutable map
        materials = map.toMap
    }

    /**
     * Cleanup all GL objects.
     * Only call from the render thread.
     */
    def cleanupGL() {
        if (materials != null)
            materials.values.foreach(GL11.glDeleteTextures(_))
        materials = null
    }

    override def finalize() {
        if (materials != null)
            System.err.println("WARNING: Finalize without cleanup of a PMXModel!")
    }

    /**
     * For debugging purposes, writes out an OBJ file.
     * I'm hoping that this'll help me figure out what's wrong.
     * (Well, it didn't, but now we know this works.
     */
    def debugWriteObj() {
        System.err.println("DEBUG WRITE OBJ")
        val tgt = new PrintStream(new FileOutputStream("debug.obj"))
        tgt.println("# FACEGROUP DUMP : EXPORTED BY DEBUG CODE, NOT TEX'D, DO NOT USE UNLESS DEBUGGING")
        tgt.println("# Height: " + height)
        theFile.vertexData.foreach(vert => {
            tgt.println("v " + vert.posX + " " + vert.posY + " " + vert.posZ)
        })
        for (i <- 0 until groups.length) {
            for (j <- 0 until groups(i).length) {
                tgt.println("g fGroup." + i + ":" + theFile.matData(i).texTex + "." + j)
                val g = groups(i)(j)
                for (vi <- 0 until g.vertexList.length by 3)
                    tgt.println("f " + (g.vertexList(vi).vxId + 1) + " " + (g.vertexList(vi + 1).vxId + 1) + " " + (g.vertexList(vi + 2).vxId + 1))
            }
        }
        tgt.close()
    }

}
