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

import moe.nightfall.instrumentality.PMXFile.{PMXBone, PMXVertex}
import moe.nightfall.instrumentality.PMXInstance.VBO_DATASIZE
import moe.nightfall.instrumentality.PMXModel.FaceGroup
import moe.nightfall.instrumentality.animations.Animation
import moe.nightfall.instrumentality.shader.Shader
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.{GL11, GL15, GL20}
import org.lwjgl.util.vector.{Matrix4f, Vector3f, Vector4f}


/**
 * Allows animating a model file, and rendering using LWJGL.
 * Note that changing the bone data (specifically the bone data) while a PMXInstance is attached to it is not recommended without replacing the object.
 * The assumption is that PMXFile will never be used for editing.
 *
 * @author gamemanj
 *         Created on 24/07/15.
 */
object PMXInstance {
    val VBO_DATASIZE = 12
}

class PMXInstance(val theModel: PMXModel) {

    val theFile = theModel.theFile
    /**
     * although theFile is part of theModel, most code uses theFile
     */

    /**
     * Animation. Can be changed at any time.
     */
    var anim: Animation = _

    var vboList = new Array[Array[Int]](theModel.groups.length)
    var boneCache = new Array[Matrix4f](theFile.boneData.length)

    for (i <- 0 until vboList.length)
        vboList(i) = new Array[Int](theModel.groups(i).length)

    private def createBoneData(fg: FaceGroup, v: PMXVertex): Array[Float] = {
        return v.weightType match {
            case 0 => Array(fg.get(v.boneIndices(0)) + 0.5f, fg.get(v.boneIndices(0)) + 0.5f, 0, 0)
            case 1 =>
                if (v.boneWeights(0) <= 0) {
                    sys.error("Weird (<) BDEF2 weight detected: " + v.boneWeights(0))
                    Array(fg.get(v.boneIndices(1)) + 0.5f, fg.get(v.boneIndices(1)) + 0.5f, 0, 0)
                } else if (v.boneWeights(0) >= 1) {
                    sys.error("Weird (>) BDEF2 weight detected: " + v.boneWeights(0))
                    Array(fg.get(v.boneIndices(0)) + 0.5f, fg.get(v.boneIndices(0)) + 0.5f, 0, 0)
                } else Array(fg.get(v.boneIndices(0)) + v.boneWeights(0), fg.get(v.boneIndices(1)) + (1.0f - v.boneWeights(0)), 0, 0)
            case 2 => Array(fg.get(v.boneIndices(0)) + v.boneWeights(0), fg.get(v.boneIndices(1)) + v.boneWeights(1), fg.get(v.boneIndices(2)) + v.boneWeights(2), fg.get(v.boneIndices(3)) + v.boneWeights(3))
            case _ =>
                // Never fail silently... but considering this is a mod people will want to use, don't be a drama queen
                sys.error("Unknown weight type " + v.weightType + " - assuming basic 1-bone")
                Array(fg.get(v.boneIndices(0)) + 0.5f, fg.get(v.boneIndices(0)) + 0.5f, 0, 0)
        }
    }

    def abssqr(d: Float) = {
        val a = math.abs(d)
        a * a
    }

    /**
     * @param intoBoneSpace The matrix to apply to
     * @param bone          The bone to get the IBS of
     */
    def createIBS(intoBoneSpace: Matrix4f, bone: PMXFile.PMXBone, inverse: Boolean) {

        // work out what we're supposed to be connected to
        var dX = bone.connectionPosOfsX
        var dY = bone.connectionPosOfsY
        var dZ = bone.connectionPosOfsZ
        // We're connected to another bone?
        if (bone.flagConnection) {
            // If it's -1, assume some reasonable defaults
            if (bone.connectionIndex == -1) {
                dX = 0
                dY = 1
                dZ = 0
            } else {
                dX = theFile.boneData(bone.connectionIndex).posX - bone.posX
                dY = theFile.boneData(bone.connectionIndex).posY - bone.posY
                dZ = theFile.boneData(bone.connectionIndex).posZ - bone.posZ
            }
        }

        // What is causing this madness??? FFS!!!
        // There are multiple ways for this to occur, I have NO idea why, but they all have a common symptom.
        if ((dX == 0) && (dY == 0) && (dZ == 0)) {
            if (bone.parentBoneIndex == -1) {
                dX = 0
                dY = 1
                dZ = 0
            } else {
                dX = theFile.boneData(bone.parentBoneIndex).posX - bone.posX
                dY = theFile.boneData(bone.parentBoneIndex).posY - bone.posY
                dZ = theFile.boneData(bone.parentBoneIndex).posZ - bone.posZ
            }
        }

        // now work out how far that is so the later maths works correctly
        val magnitude = math.sqrt(abssqr(dX) + abssqr(dY) + abssqr(dZ))

        // work out our direction...
        val t = Math.atan2(dY, dX).toFloat
        val p = Math.acos(dZ / magnitude).toFloat

        if (inverse) {
            // translate by the inverse position
            intoBoneSpace.translate(new Vector3f(bone.posX, bone.posY, bone.posZ))

            intoBoneSpace.rotate(-p, new Vector3f(1, 0, 0))
            intoBoneSpace.rotate(-t, new Vector3f(0, 0, 1))
        } else {
            // Attempt to rotate us into the bone's "space"
            intoBoneSpace.rotate(t, new Vector3f(0, 0, 1))
            intoBoneSpace.rotate(p, new Vector3f(1, 0, 0))

            // translate by the inverse position
            intoBoneSpace.translate(new Vector3f(-bone.posX, -bone.posY, -bone.posZ))
        }
    }

    /**
     * Do not modify the matrix that leaves.
     *
     * @param bone The bone to get the matrix of
     * @return A matrix4f that you should not modify.
     */
    def getBoneMatrix(bone: PMXBone): Matrix4f = {
        if (boneCache(bone.boneId) != null)
            return boneCache(bone.boneId)
        // Simple enough: get what the bone wants us to transform it by...
        val boneTransform = anim.getBoneTransform(bone.sensibleName)
        val i = new Matrix4f()
        boneTransform foreach { tr =>
            // Go into bone-space, apply the transform, then leave.
            createIBS(i, bone, true)
            tr.apply(i)
            createIBS(i, bone, false)
        }
        // If there's a parent, run through this again with that...
        if (bone.parentBoneIndex != -1)
            Matrix4f.mul(getBoneMatrix(theFile.boneData(bone.parentBoneIndex)), i, i)
        boneCache(bone.boneId) = i
        i
    }

    /**
     * Clears the bone cache. Use when you believe a change has been made (and preferably only under this case)
     */
    def clearBoneCache() {
        for (i <- 0 until theFile.boneData.length) boneCache(i) = null
    }

    /**
     * Renders this model, with a given set of textures.
     * Uses glPushAttrib and glPopAttrib to avoid disturbing any glEnables.
     *
     * @param s The animation shader.
     */
    def render(s: Shader, red: Double, green: Double, blue: Double, clippingSize: Float) {
        // Makes things simpler
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        val matrix = BufferUtils.createFloatBuffer(16)
        val oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
        GL20.glUseProgram(s.program)
        if (theModel.materials == null)
            theModel.setupMaterials()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL20.glUniform1f(GL20.glGetUniformLocation(s.program, "fadeIn"), clippingSize * theModel.height)
        GL20.glUniform1f(GL20.glGetUniformLocation(s.program, "fadeInDiscard"), (clippingSize + 0.1f) * theModel.height)
        for (i <- 0 until theModel.groups.length) {
            val mat = theFile.matData(i)
            val usingTex = mat.texTex != null
            if (usingTex) {
                val str = mat.texTex.toLowerCase.replace('\\', '/')
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, theModel.materials.get(str).get)
                GL11.glColor4d(red, green, blue, 1)
            } else {
                GL11.glColor4d(mat.diffR, mat.diffG, mat.diffB, 1)
            }
            for (j <- 0 until vboList(i).length) {
                val fg = theModel.groups(i).get(j).get
                if (vboList(i)(j) == 0) {
                    // since this set is complete, we can set up the buffers now
                    val vboData = BufferUtils.createFloatBuffer(fg.vertexList.size * VBO_DATASIZE)
                    vboList(i)(j) = GL15.glGenBuffers()
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboList(i)(j))
                    fg.vertexList foreach { vt =>
                        val in = vboData.position()
                        vboData.put(vt.posX)
                        vboData.put(vt.posY)
                        vboData.put(vt.posZ)
                        vboData.put(vt.texU)
                        vboData.put(vt.texV)
                        vboData.put(vt.normalX)
                        vboData.put(vt.normalY)
                        vboData.put(vt.normalZ)

                        val res = createBoneData(fg, vt)
                        if (res.length != 4)
                            throw new RuntimeException("FIX CREATEBONEDATA")
                        vboData.put(res)
                        if (vboData.position() - in != VBO_DATASIZE)
                            throw new RuntimeException("VBO_DATASIZE incorrect")
                    }
                    vboData.rewind()
                    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboData, GL15.GL_STATIC_DRAW)
                }
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboList(i)(j))
                // Adjust uniforms
                // Then render this chunk of the model.
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
                if (usingTex)
                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY)
                GL11.glVertexPointer(3, GL11.GL_FLOAT, VBO_DATASIZE * 4, 0 * 4)
                if (usingTex)
                    GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VBO_DATASIZE * 4, 3 * 4)
                GL11.glNormalPointer(GL11.GL_FLOAT, VBO_DATASIZE * 4, 5 * 4)
                val bonesAttrib = GL20.glGetAttribLocation(s.program, "Bones")
                GL20.glEnableVertexAttribArray(bonesAttrib)
                GL20.glVertexAttribPointer(bonesAttrib, 4, GL11.GL_FLOAT, false, VBO_DATASIZE * 4, 8 * 4)
                for (bInd <- 0 until fg.boneMappingGroupFile.length) {
                    val m = getBoneMatrix(theFile.boneData(fg.boneMappingGroupFile(bInd)))
                    m.store(matrix)
                    matrix.rewind()
                    val poseUniform = GL20.glGetUniformLocation(s.program, "Pose[" + bInd + "]")
                    GL20.glUniformMatrix4(poseUniform, false, matrix)
                }
                GL20.glUniform1f(GL20.glGetUniformLocation(s.program, "textured"), if (usingTex) 1 else 0)
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, fg.vertexList.size)
                GL20.glDisableVertexAttribArray(bonesAttrib)
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY)
                if (usingTex)
                    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY)
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
            }
        }
        // In case the above code enabled GL_TEXTURE_2D
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL20.glUseProgram(oldProgram)
        GL11.glPopAttrib()
    }

    def cleanupGL() {
        for (i <- 0 until theModel.groups.length) {
            for (j <- 0 until vboList(i).length) {
                if (vboList(i)(j) != 0) {
                    GL15.glDeleteBuffers(vboList(i)(j))
                    vboList(i)(j) = 0
                }
            }
        }
    }

    def update(v: Double) {
        if (anim != null) anim.update(v)
        // naturally implies a change
        clearBoneCache()
    }

    def renderDebug(selected: Int) {
        theFile.boneData foreach { bone =>
            GL11.glPointSize(2)
            val v3f = doTransform(bone, new Vector3f(bone.posX, bone.posY, bone.posZ))
            if (bone.parentBoneIndex != -1) {
                GL11.glLineWidth(2)
                GL11.glBegin(GL11.GL_LINES)
                val parentSelected = selected == bone.parentBoneIndex
                if (parentSelected) {
                    GL11.glColor3d(1, 1, 1)
                } else {
                    GL11.glColor3d(1, 0, 0)
                }
                GL11.glVertex3d(v3f.x, v3f.y, v3f.z)
                if (parentSelected) {
                    GL11.glColor3d(1, 1, 1)
                } else {
                    GL11.glColor3d(0, 1, 0)
                }
                val bd = theFile.boneData(bone.parentBoneIndex)
                val v3f2 = doTransform(bd, new Vector3f(bd.posX, bd.posY, bd.posZ))
                GL11.glVertex3d(v3f2.x, v3f2.y, v3f2.z)
                GL11.glEnd()
            }
            GL11.glPointSize(4)
            GL11.glBegin(GL11.GL_POINTS)
            GL11.glColor3d(if (selected == bone.boneId) 1 else 0, 0, 1)
            GL11.glVertex3d(v3f.x, v3f.y, v3f.z)
            GL11.glEnd()
        }
    }

    private def doTransform(pmxBone: PMXBone, iv: Vector3f): Vector3f = {
        val v = new Vector4f(iv.x, iv.y, iv.z, 1)
        val mat = getBoneMatrix(pmxBone)
        Matrix4f.transform(mat, v, v)
        new Vector3f(v.x, v.y, v.z)
    }

    override def finalize() {
        for (i <- 0 until theModel.groups.length) {
            for (j <- 0 until vboList(i).length) {
                if (vboList(i)(j) != 0) {
                    System.err.println("WARNING: Finalize without cleanup of a PMXInstance!");
                    return
                }
            }
        }
    }
}
