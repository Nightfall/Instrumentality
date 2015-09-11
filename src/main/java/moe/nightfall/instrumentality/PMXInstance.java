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

import moe.nightfall.instrumentality.PMXFile.PMXBone;
import moe.nightfall.instrumentality.animations.IAnimation;
import moe.nightfall.instrumentality.shader.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;

/**
 * Allows animating a model file, and rendering using LWJGL.
 * Note that changing the bone data (specifically the bone data) while a PMXInstance is attached to it is not recommended without replacing the object.
 * The assumption is that PMXFile will never be used for editing.
 *
 * @author gamemanj
 *         Created on 24/07/15.
 */
public class PMXInstance {

    public final PMXFile theFile;
    /**
     * although theFile is part of theModel, most code uses theFile
     */
    public final PMXModel theModel;

    /**
     * Animation. Can be changed at any time.
     */
    public IAnimation anim;

    private final int[][] vboList;
    private final Matrix4f[] boneCache;

    private static final int VBO_DATASIZE = 15;

    public PMXInstance(PMXModel pm) {
        theFile = pm.theFile;
        vboList = new int[pm.groups.length][];
        boneCache = new Matrix4f[theFile.boneData.length];
        for (int i = 0; i < vboList.length; i++)
            vboList[i] = new int[pm.groups[i].size()];
        theModel = pm;
    }

    private float[] createBoneData(PMXModel.FaceGroup fg, PMXFile.PMXVertex v) {
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
        return boneCache[bone.boneId] = i;
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

        // Kagamine Rin

        // .stuff

        if (globalName.equalsIgnoreCase("center"))
            return "root";

        if (globalName.equalsIgnoreCase("head_o"))
            return "head";

        if (globalName.equalsIgnoreCase("LEyeCtrl"))
            return "L_eye_ctrl";
        if (globalName.equalsIgnoreCase("REyeCtrl"))
            return "R_eye_ctrl";

        // .arms

        if (globalName.equalsIgnoreCase("L_Arm"))
            return "L_shouler";
        if (globalName.equalsIgnoreCase("R_arm"))
            return "R_shouler";
        if (globalName.equalsIgnoreCase("L_elbow"))
            return "L_ellbow";
        if (globalName.equalsIgnoreCase("R_elbow"))
            return "R_ellbow";
        if (globalName.equalsIgnoreCase("L_wrist"))
            return "L_hand";
        if (globalName.equalsIgnoreCase("R_wrist"))
            return "R_hand";

        // .legs

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
     * @param s The animation shader.
     */
    public void render(Shader s, double red, double green, double blue, float clippingSize) {
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(s.getProgram());
        for (int i = 0; i < theFile.boneData.length; i++)
            boneCache[i] = null;
        if (theModel.materials == null)
            theModel.setupMaterials();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glUniform1f(GL20.glGetUniformLocation(s.getProgram(), "fadeIn"), clippingSize * theModel.height);
        GL20.glUniform1f(GL20.glGetUniformLocation(s.getProgram(), "fadeInDiscard"), (clippingSize + 0.5f) * theModel.height);
        for (int i = 0; i < theModel.groups.length; i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, theModel.materials.get(mat.texTex.toLowerCase()));
            for (int j = 0; j < vboList[i].length; j++) {
                PMXModel.FaceGroup fg = theModel.groups[i].get(j);
                if (vboList[i][j] == 0) {
                    // since this set is complete, we can set up the buffers now
                    FloatBuffer vboData = BufferUtils.createFloatBuffer(fg.vertexList.size() * VBO_DATASIZE);
                    vboList[i][j] = GL15.glGenBuffers();
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboList[i][j]);
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
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboList[i][j]);
                // Adjust uniforms
                // Then render this chunk of the model.
                GL11.glColor4d(red, green, blue, 1.0f);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                GL11.glVertexPointer(3, GL11.GL_FLOAT, VBO_DATASIZE * 4, 0 * 4);
                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VBO_DATASIZE * 4, 3 * 4);
                GL11.glNormalPointer(GL11.GL_FLOAT, VBO_DATASIZE * 4, 5 * 4);
                int bonesAttrib = GL20.glGetAttribLocation(s.getProgram(), "Bones");
                GL20.glEnableVertexAttribArray(bonesAttrib);
                GL20.glVertexAttribPointer(bonesAttrib, 4, GL11.GL_FLOAT, false, VBO_DATASIZE * 4, 8 * 4);
                //GL20.glVertexAttribPointer(tangentsAttrib, 3, GL11.GL_FLOAT, false, VBO_DATASIZE * 4, 12 * 4);
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
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(oldProgram);
    }

    public void cleanupGL() {
        for (int i = 0; i < theModel.groups.length; i++)
            for (int j = 0; j < vboList[i].length; j++)
                if (vboList[i][j] != 0) {
                    GL15.glDeleteBuffers(vboList[i][j]);
                    vboList[i][j] = 0;
                }
    }

    public void update(double v) {
        if (anim != null)
            anim.update(v);
    }

    public void renderDebug(int selected) {
        for (PMXFile.PMXBone bone : theFile.boneData) {
            GL11.glPointSize(2);
            Vector3f v3f = doTransform(bone, new Vector3f(bone.posX, bone.posY, bone.posZ));
            if (bone.parentBoneIndex != -1) {
                GL11.glLineWidth(2);
                GL11.glBegin(GL11.GL_LINES);
                boolean parentSelected = selected == bone.parentBoneIndex;
                if (parentSelected) {
                    GL11.glColor3d(1, 1, 1);
                } else {
                    GL11.glColor3d(1, 0, 0);
                }
                GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
                if (parentSelected) {
                    GL11.glColor3d(1, 1, 1);
                } else {
                    GL11.glColor3d(0, 1, 0);
                }
                Vector3f v3f2 = doTransform(theFile.boneData[bone.parentBoneIndex], new Vector3f(theFile.boneData[bone.parentBoneIndex].posX, theFile.boneData[bone.parentBoneIndex].posY, theFile.boneData[bone.parentBoneIndex].posZ));
                GL11.glVertex3d(v3f2.x, v3f2.y, v3f2.z);
                GL11.glEnd();
            }
            GL11.glPointSize(4);
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glColor3d(selected == bone.boneId ? 1 : 0, 0, 1);
            GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
            GL11.glEnd();
        }
    }

    private Vector3f doTransform(PMXBone pmxBone, Vector3f iv) {
        Vector4f v = new Vector4f(iv.x, iv.y, iv.z, 1);
        Matrix4f.transform(getBoneMatrix(pmxBone), v, v);
        return new Vector3f(v.x, v.y, v.z);
    }

    @Override
    public void finalize() {
        for (int i = 0; i < theModel.groups.length; i++)
            for (int j = 0; j < vboList[i].length; j++)
                if (vboList[i][j] != 0) {
                    System.err.println("WARNING: Finalize without cleanup of a PMXInstance!");
                    return;
                }
    }
}
