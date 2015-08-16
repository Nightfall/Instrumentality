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
package moe.nightfall.instrumentality;

import moe.nightfall.instrumentality.animations.IAnimation;
import moe.nightfall.instrumentality.shader.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Allows animating a model file, and rendering using LWJGL.
 * Note that changing the bone data (specifically the bone data) while a PMXModel is attached to it is not recommended without replacing the object.
 * The assumption is that PMXFile will never be used for editing.
 *
 * @author gamemanj
 *         Created on 24/07/15.
 */
public class PMXModel {

    public final PMXFile theFile;

    /**
     * Animation. Can be changed at any time.
     */
    public IAnimation anim;

    /**
     * Height of the model, determinated by the highest vertex
     */
    public final float height;

    private final LinkedList<FaceGroup>[] groups;
    private final Matrix4f[] boneCache;

    private static final int VBO_DATASIZE = 15;


    private class FaceGroup {
        // Used while creating the FaceGroup, this is discarded after the group is compiled
        final HashSet<PMXFile.PMXBone> boneSet = new HashSet<PMXFile.PMXBone>();

        // Buffers for compiling
        final LinkedList<PMXFile.PMXVertex> vertexList = new LinkedList<PMXFile.PMXVertex>();
        // If this is 0, the VBO hasn't been compiled yet
        int vboIndex = 0;

        // Bone mappings (these are finalized after the material containing this group is compiled)
        int[] boneMappingFileGroup;
        int[] boneMappingGroupFile;

        public float get(int boneIndice) {
            int res = boneMappingFileGroup[boneIndice];
            if (res == -1)
                throw new RuntimeException("Bone is being relied on that does not exist within this group.");
            return res;
        }
    }

    public PMXModel(PMXFile pf, int maxGroupSize) {
        theFile = pf;
        groups = new LinkedList[pf.matData.length];
        boneCache = new Matrix4f[pf.boneData.length];

        int face = 0;
        float height = 0;

        for (int i = 0; i < theFile.matData.length; i++) {
            groups[i] = new LinkedList<FaceGroup>();
            PMXFile.PMXMaterial mat = theFile.matData[i];
            for (int ind = 0; ind < mat.faceCount; ind++) {
                PMXFile.PMXVertex vA = theFile.vertexData[theFile.faceData[face][0]];
                PMXFile.PMXVertex vB = theFile.vertexData[theFile.faceData[face][1]];
                PMXFile.PMXVertex vC = theFile.vertexData[theFile.faceData[face][2]];
                height = vA.posY > height ? vA.posY : height;
                height = vB.posY > height ? vB.posY : height;
                height = vC.posY > height ? vC.posY : height;

                // Work out which bones are needed.

                int wA = weightVertices(vA.weightType);
                int wB = weightVertices(vB.weightType);
                int wC = weightVertices(vC.weightType);
                HashSet<PMXFile.PMXBone> usedBones = new HashSet<PMXFile.PMXBone>();
                for (int i2 = 0; i2 < wA; i2++)
                    usedBones.add(theFile.boneData[vA.boneIndices[i2]]);
                for (int i2 = 0; i2 < wB; i2++)
                    usedBones.add(theFile.boneData[vB.boneIndices[i2]]);
                for (int i2 = 0; i2 < wC; i2++)
                    usedBones.add(theFile.boneData[vC.boneIndices[i2]]);

                // Ok, now for each group, check how many bones are missing (if none are missing we can break)

                FaceGroup target = null;
                FaceGroup bestBoneAdd = null;
                HashSet<PMXFile.PMXBone> bestBoneAddRequired = null;
                HashSet<PMXFile.PMXBone> bbarWork = new HashSet<PMXFile.PMXBone>();
                for (FaceGroup fg : groups[i]) {
                    // If the group can contain it, we don't need to do any more.
                    if (fg.boneSet.containsAll(usedBones)) {
                        target = fg;
                        break;
                    } else {
                        // Otherwise, check what is missing...
                        bbarWork.clear();
                        for (PMXFile.PMXBone pb : usedBones)
                            if (!fg.boneSet.contains(pb))
                                bbarWork.add(pb);
                        if (bbarWork.size() + fg.boneSet.size() > maxGroupSize)
                            continue; // If it won't fit, don't do it.
                        if (bestBoneAddRequired == null) {
                            bestBoneAdd = fg;
                            bestBoneAddRequired = bbarWork;
                            bbarWork = new HashSet<PMXFile.PMXBone>();
                        } else {
                            // If less is missing here than in what we're planning to put it in,
                            // this this is a better choice
                            if (bestBoneAddRequired.size() > bbarWork.size()) {
                                bestBoneAdd = fg;
                                bestBoneAddRequired = bbarWork;
                                bbarWork = new HashSet<PMXFile.PMXBone>();
                            }
                        }
                    }
                }
                if (target == null) {
                    if (bestBoneAdd == null) {
                        // there are no groups we can add to, create a new group
                        // We're not doing the "can it fit" check here, if it can't fit your model is broken/weird
                        if (usedBones.size() > maxGroupSize) {
                            System.err.println("WARNING! maxGroupSize of " + maxGroupSize + " is too small for a face which relies on " + usedBones.size() + " bones!");
                        } else {
                            target = new FaceGroup();
                            target.boneSet.addAll(usedBones);
                            groups[i].add(target);
                        }
                    } else {
                        // Well, this is simple :)
                        bestBoneAdd.boneSet.addAll(bestBoneAddRequired);
                        target = bestBoneAdd;
                    }
                }

                if (target != null) {
                    // Add the vertices to wherever we want to put them
                    // (the facegroup needs to be complete before we start actually writing buffers)
                    target.vertexList.add(vC);
                    target.vertexList.add(vB);
                    target.vertexList.add(vA);
                } else {
                    System.err.println("Faces have been skipped!!! Display artifacting will result.");
                }
                face++;
            }
            // Pass 2 : clean up the mess
            for (FaceGroup fg : groups[i]) {
                fg.boneMappingFileGroup = new int[theFile.boneData.length];
                fg.boneMappingGroupFile = new int[fg.boneSet.size()];
                Iterator<PMXFile.PMXBone> it = fg.boneSet.iterator();
                for (int j = 0; j < fg.boneMappingFileGroup.length; j++)
                    fg.boneMappingFileGroup[j] = -1;
                for (int j = 0; j < fg.boneMappingGroupFile.length; j++)
                    fg.boneMappingFileGroup[fg.boneMappingGroupFile[j] = it.next().boneId] = j;
            }
            System.out.println(groups[i].size() + " facegroups for shading on material " + i);
        }
        this.height = height;
    }

    private int weightVertices(int weightType) {
        if (weightType == 0)
            return 1;
        if (weightType == 1)
            return 2;
        if (weightType == 2)
            return 4;
        return 0;
    }

    private float[] createBoneData(FaceGroup fg, PMXFile.PMXVertex v) {
        switch (v.weightType) {
            case 0:
                // we can't actually have "1" as a weight :)
                return new float[]{fg.get(v.boneIndices[0]) + 0.5f, fg.get(v.boneIndices[0]) + 0.5f, 0, 0};
            case 1:
                if (v.boneWeights[0] <= 0) {
                    System.err.println("Weird (<) BDEF2 weight detected: " + v.boneWeights[0]);
                    return new float[]{fg.get(v.boneIndices[1]) + 0.5f, fg.get(v.boneIndices[1]) + 0.5f, 0, 0};
                }
                if (v.boneWeights[0] >= 1) {
                    System.err.println("Weird (>) BDEF2 weight detected: " + v.boneWeights[0]);
                    return new float[]{fg.get(v.boneIndices[0]) + 0.5f, fg.get(v.boneIndices[0]) + 0.5f, 0, 0};
                }
                return new float[]{fg.get(v.boneIndices[0]) + v.boneWeights[0], fg.get(v.boneIndices[1]) + (1.0f - v.boneWeights[0]), 0, 0};
            case 2:
                return new float[]{fg.get(v.boneIndices[0]) + v.boneWeights[0], fg.get(v.boneIndices[1]) + v.boneWeights[1], fg.get(v.boneIndices[2]) + v.boneWeights[2], fg.get(v.boneIndices[3]) + v.boneWeights[3]};
            default:
                // Never fail silently... but considering this is a mod people will want to use, don't be a drama queen
                System.err.println("Unknown weight time " + v.weightType + " - assuming basic 1-bone");
                return new float[]{fg.get(v.boneIndices[0]) + 0.5f, fg.get(v.boneIndices[0]) + 0.5f, 0, 0};
        }
    }

    /**
     *
     * @param intoBoneSpace The matrix to apply to
     * @param bone          The bone to get the IBS of
     */
    public void createIBS(Matrix4f intoBoneSpace, PMXFile.PMXBone bone, boolean inverse) {

        // work out what we're supposed to be connected to
        float dX = bone.connectionPosOfsX;
        float dY = bone.connectionPosOfsY;
        float dZ = bone.connectionPosOfsZ;
        // We're connected to another bone?
        if (bone.flagConnection) {
            // If it's -1, assume some reasonable defaults
            if (bone.connectionIndex == -1) {
                dX = 0;
                dY = 1;
                dZ = 0;
            } else {
                dX = theFile.boneData[bone.connectionIndex].posX - bone.posX;
                dY = theFile.boneData[bone.connectionIndex].posY - bone.posY;
                dZ = theFile.boneData[bone.connectionIndex].posZ - bone.posZ;
            }
        }
        // now work out how far that is so the later maths works correctly
        double magnitude = Math.sqrt((dX * dX) + (dY * dY) + (dZ * dZ));
        // work out our direction...
        // Note: There was a forum post. It contained the maths that I translated to this code.
        // I have no idea how it would handle dX==0. It could involve explosions.
        // *looks at the IK bones that aren't around*
        // Oh. Wait. Those did dX==0, didn't they...
        float t = (float) Math.atan(dY / dX);
        float p = (float) Math.acos(dZ / magnitude);

        if (inverse) {
            // translate by the inverse position
            intoBoneSpace.translate(new Vector3f(bone.posX, bone.posY, bone.posZ));

            intoBoneSpace.rotate(-p, new Vector3f(1, 0, 0));
            intoBoneSpace.rotate(-t, new Vector3f(0, 0, 1));
        } else {
            // Attempt to rotate us into the bone's "space"
            intoBoneSpace.rotate(t, new Vector3f(0, 0, 1));
            intoBoneSpace.rotate(p, new Vector3f(1, 0, 0));

            // translate by the inverse position
            intoBoneSpace.translate(new Vector3f(-(bone.posX), -(bone.posY), -(bone.posZ)));
        }
    }

    /**
     * Do not modify the matrix that leaves.
     *
     * @param bone The bone to get the matrix of
     * @return A matrix4f that you should not modify.
     */
    public Matrix4f getBoneMatrix(PMXFile.PMXBone bone) {
        if (boneCache[bone.boneId] != null)
            return boneCache[bone.boneId];
        // Simple enough: get what the bone wants us to transform it by...
        PoseBoneTransform boneTransform = anim.getBoneTransform(compatibilityCheck(bone.globalName));
        Matrix4f i = new Matrix4f();
        if (boneTransform != null) {
            // Go into bone-space, apply the transform, then leave.
            createIBS(i, bone, true);
            boneTransform.apply(i);
            createIBS(i, bone, false);
        }
        // If there's a parent, run through this again with that...
        if (bone.parentBoneIndex != -1)
            Matrix4f.mul(getBoneMatrix(theFile.boneData[bone.parentBoneIndex]), i, i);
        return boneCache[bone.boneId]=i;
    }

    /**
     * Some models use different names for what are essentially the same bones.
     * Put checks here to find these.
     * <p/>
     * Note that if animations don't work correctly with the new name of a bone,
     * it is better to put the compatibility in the affected animations
     * (so the values can be adjusted to compensate)
     *
     * @param globalName The original globalName of the bone.
     * @return The translated bone name, or the original if it is not in the compatibility table.
     */
    private String compatibilityCheck(String globalName) {

        // Kagamine Rin Legs

        if (globalName.equalsIgnoreCase("L_leg"))
            return "leg_L";
        if (globalName.equalsIgnoreCase("L_knee"))
            return "knee_L";
        if (globalName.equalsIgnoreCase("L_foot"))
            return "ankle_L";

        if (globalName.equalsIgnoreCase("R_leg"))
            return "leg_R";
        if (globalName.equalsIgnoreCase("R_knee"))
            return "knee_R";
        if (globalName.equalsIgnoreCase("R_foot"))
            return "ankle_R";

        return globalName;
    }

    /**
     * Renders this model, with a given set of textures.
     * Make sure to enable GL_TEXTURE_2D before calling.
     *
     * @param textureBinder Binds a texture.
     */
    public void render(IMaterialBinder textureBinder, Shader s) {
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(s.getProgram());
        for (int i = 0; i < theFile.boneData.length; i++)
            boneCache[i] = null;
        for (int i = 0; i < groups.length; i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            textureBinder.bindMaterial(mat);
            for (FaceGroup fg : groups[i]) {
                if (fg.vboIndex == 0) {
                    // since this set is complete, we can set up the buffers now
                    FloatBuffer vboData = BufferUtils.createFloatBuffer(fg.vertexList.size() * VBO_DATASIZE);
                    fg.vboIndex = GL15.glGenBuffers();
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fg.vboIndex);
                    for (PMXFile.PMXVertex vt : fg.vertexList) {
                        int in = vboData.position();
                        vboData.put(vt.posX);
                        vboData.put(vt.posY);
                        vboData.put(vt.posZ);
                        vboData.put(vt.texU);
                        vboData.put(vt.texV);
                        vboData.put(vt.normalX);
                        vboData.put(vt.normalY);
                        vboData.put(vt.normalZ);

                        float[] res = createBoneData(fg, vt);
                        if (res.length != 4)
                            throw new RuntimeException("FIX CREATEBONEDATA");
                        vboData.put(res);
                        vboData.put(0);
                        vboData.put(0);
                        vboData.put(0);
                        if (vboData.position() - in != VBO_DATASIZE)
                            throw new RuntimeException("VBO_DATASIZE incorrect");
                    }
                    vboData.rewind();
                    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboData, GL15.GL_STATIC_DRAW);
                }
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fg.vboIndex);
                // Adjust uniforms
                // Then render this chunk of the model.
                GL11.glColor4d(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                GL11.glVertexPointer(3, GL11.GL_FLOAT, VBO_DATASIZE * 4, 0 * 4);
                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VBO_DATASIZE * 4, 3 * 4);
                GL11.glNormalPointer(GL11.GL_FLOAT, VBO_DATASIZE * 4, 5 * 4);
                int bonesAttrib = GL20.glGetAttribLocation(s.getProgram(), "Bones");
                GL20.glEnableVertexAttribArray(bonesAttrib);
                GL20.glVertexAttribPointer(bonesAttrib, 4, GL11.GL_FLOAT, false, VBO_DATASIZE * 4, 8 * 4);
                //GL20.glVertexAttribPointer(tangentsAttrib, 3, GL11.GL_FLOAT, false, VBO_DATASIZE - 4, 12);
                for (int bInd = 0; bInd < fg.boneMappingGroupFile.length; bInd++) {
                    Matrix4f m = getBoneMatrix(theFile.boneData[fg.boneMappingGroupFile[bInd]]);
                    m.store(matrix);
                    matrix.rewind();
                    int poseUniform = GL20.glGetUniformLocation(s.getProgram(), "Pose[" + bInd + "]");
                    GL20.glUniformMatrix4(poseUniform, false, matrix);
                }
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, fg.vertexList.size());
                GL20.glDisableVertexAttribArray(bonesAttrib);
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
        }
        GL20.glUseProgram(oldProgram);
    }

    public void update(double v) {
        if (anim != null)
            anim.update(v);
    }
}
