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
import moe.nightfall.instrumentality.shader.ShaderManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

    private final IntBuffer[] indexBuffer;
    private final IntBuffer[] cobaltIndexBuffer;

    private final FloatBuffer buffer_v;
    private final FloatBuffer buffer_n;
    private final FloatBuffer buffer_t;

    public PMXModel(PMXFile pf) {
        theFile = pf;
        indexBuffer = new IntBuffer[pf.matData.length];
        cobaltIndexBuffer = new IntBuffer[pf.matData.length];

        buffer_v = BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_n = BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_t = BufferUtils.createFloatBuffer(theFile.vertexData.length * 2);

        int face = 0;

        for (int i = 0; i < theFile.matData.length; i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            indexBuffer[i] = BufferUtils.createIntBuffer(mat.faceCount * 3);
            cobaltIndexBuffer[i] = BufferUtils.createIntBuffer(mat.faceCount * 6);
            for (int ind = 0; ind < mat.faceCount; ind++) {
                cobaltIndexBuffer[i].put(theFile.faceData[face][0]);
                cobaltIndexBuffer[i].put(theFile.faceData[face][1]);

                cobaltIndexBuffer[i].put(theFile.faceData[face][1]);
                cobaltIndexBuffer[i].put(theFile.faceData[face][2]);

                cobaltIndexBuffer[i].put(theFile.faceData[face][2]);
                cobaltIndexBuffer[i].put(theFile.faceData[face][0]);

                indexBuffer[i].put(theFile.faceData[face][0]);
                indexBuffer[i].put(theFile.faceData[face][1]);
                indexBuffer[i].put(theFile.faceData[face][2]);
                face++;
            }
        }
        for (int vi = 0; vi < theFile.vertexData.length; vi++) {
            PMXFile.PMXVertex ver = theFile.vertexData[vi];
            buffer_v.put(new float[]{ver.posX, ver.posY, ver.posZ});
            buffer_n.put(new float[]{ver.normalX, ver.normalY, ver.normalZ});
            buffer_t.put(new float[]{ver.texU, ver.texV});
        }
    }

    /**
     * This used to be created in transformCore, then cached forever
     * now that's no longer needed, as the entire transform matrix is cached for the frame -
     * caching this is a waste of time now.
     *
     * @param bone        The bone to get the IBS of
     * @param translation Disable for normals
     * @return An IBS matrix
     */
    public Matrix4f createIBS(PMXFile.PMXBone bone, boolean translation) {

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
        Matrix4f intoBoneSpace = new Matrix4f();

        // Attempt to rotate us into the bone's "space"
        intoBoneSpace.rotate(t, new Vector3f(0, 0, 1));
        intoBoneSpace.rotate(p, new Vector3f(1, 0, 0));

        // translate by the inverse position
        if (translation)
            intoBoneSpace.translate(new Vector3f(-(bone.posX), -(bone.posY), -(bone.posZ)));
        return intoBoneSpace;
    }

    public Matrix4f getBoneMatrix(PMXFile.PMXBone bone, boolean translation) {
        // Simple enough: get what the bone wants us to transform it by...
        PoseBoneTransform boneTransform = anim.getBoneTransform(compatibilityCheck(bone.globalName));
        Matrix4f i = new Matrix4f();
        if (boneTransform != null) {
            // Go into bone-space, apply the transform, then leave.
            Matrix4f t = createIBS(bone, translation);
            Matrix4f.mul(t, i, i);

            Matrix4f bt = new Matrix4f();
            boneTransform.apply(bt, translation);
            Matrix4f.mul(bt, i, i);

            t.invert();
            Matrix4f.mul(t, i, i);
        }
        // If there's a parent, run through this again with that...
        if (bone.parentBoneIndex != -1)
            Matrix4f.mul(getBoneMatrix(theFile.boneData[bone.parentBoneIndex], translation), i, i);
        return i;
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
     * @param cobalt        undocumented feature
     */
    public void render(IMaterialBinder textureBinder, boolean cobalt) {

        buffer_v.rewind();
        buffer_n.rewind();
        buffer_t.rewind();
        for (int pass = 0; pass < (cobalt ? 5 : 1); pass++) {
            for (int i = 0; i < theFile.matData.length; i++) {
            	
            	ShaderManager.bindShader(Main.shaderBoneTransform);
            	
                cobaltIndexBuffer[i].rewind();
                indexBuffer[i].rewind();
                PMXFile.PMXMaterial mat = theFile.matData[i];
                textureBinder.bindMaterial(mat);
                if (cobalt) {
                    float mul = new float[]{
                            0.1f,
                            0.2f,
                            0.55f,
                            0.75f,
                            1.0f
                    }[pass];
                    GL11.glLineWidth(5 - pass);
                    //GL11.glClearColor(0.0f, 0.1f, 0.4f, 1.0f);
                    GL11.glColor4d(0.0f, 0.1f + (0.1f * mul), 0.4f + (0.6f * mul), 1.0f);
                } else {
                    GL11.glColor4d(1.0f, 1.0f, 1.0f, 1.0f);
                }
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                GL11.glVertexPointer(3, 0, buffer_v);
                GL11.glTexCoordPointer(2, 0, buffer_t);
                GL11.glNormalPointer(0, buffer_n);
                if (cobalt) {
                    GL11.glDrawElements(GL11.GL_LINES, cobaltIndexBuffer[i]);
                } else {
                    GL11.glDrawElements(GL11.GL_TRIANGLES, indexBuffer[i]);
                }
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                
                ShaderManager.releaseShader();
            }
        }
    }

    public void update(double v) {
        if (anim != null)
            anim.update(v);
    }

}
