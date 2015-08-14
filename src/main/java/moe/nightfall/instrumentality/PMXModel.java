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
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
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

    private final LinkedList<FaceGroup>[] groups;

    private class FaceGroup {
        final HashSet<PMXFile.PMXBone> boneSet=new HashSet<PMXFile.PMXBone>();

        // Buffers for compiling
        final LinkedList<PMXFile.PMXVertex> vertexList=new LinkedList<PMXFile.PMXVertex>();

        // Buffers for rendering
        FloatBuffer vertexBuffer;
        FloatBuffer normalBuffer;
        FloatBuffer uvBuffer;

        // vec4, note that "1.0" is not a valid weight! This is compiled on load, and doesn't change.
        // For the pose buffer, look at the uniforms :)
        FloatBuffer bonesBuffer;
        // vec3
        FloatBuffer tangentBuffer;
    }

    public PMXModel(PMXFile pf, int maxGroupSize) {
        theFile = pf;
        groups=new LinkedList[pf.matData.length];

        int face = 0;

        for (int i = 0; i < theFile.matData.length; i++) {
            groups[i]=new LinkedList<FaceGroup>();
            PMXFile.PMXMaterial mat = theFile.matData[i];
            for (int ind = 0; ind < mat.faceCount; ind++) {
                PMXFile.PMXVertex vA=theFile.vertexData[theFile.faceData[face][0]];
                PMXFile.PMXVertex vB=theFile.vertexData[theFile.faceData[face][1]];
                PMXFile.PMXVertex vC=theFile.vertexData[theFile.faceData[face][2]];

                // Work out which bones are needed.

                int wA=weightVertices(vA.weightType);
                int wB=weightVertices(vB.weightType);
                int wC=weightVertices(vC.weightType);
                HashSet<PMXFile.PMXBone> usedBones=new HashSet<PMXFile.PMXBone>();
                for (int i2=0;i2<wA;i2++)
                    usedBones.add(theFile.boneData[vA.boneIndices[i2]]);
                for (int i2=0;i2<wB;i2++)
                    usedBones.add(theFile.boneData[vB.boneIndices[i2]]);
                for (int i2=0;i2<wC;i2++)
                    usedBones.add(theFile.boneData[vC.boneIndices[i2]]);

                // Ok, now for each group, check how many bones are missing (if none are missing we can break)

                FaceGroup target=null;
                FaceGroup bestBoneAdd=null;
                HashSet<PMXFile.PMXBone> bestBoneAddRequired=null;
                HashSet<PMXFile.PMXBone> bbarWork=new HashSet<PMXFile.PMXBone>();
                for (FaceGroup fg : groups[i]) {
                    // If the group can contain it, we don't need to do any more.
                    if (fg.boneSet.containsAll(usedBones)) {
                        target=fg;
                        break;
                    } else {
                        // Otherwise, check what is missing...
                        bbarWork.clear();
                        for (PMXFile.PMXBone pb : usedBones)
                            if (!fg.boneSet.contains(pb))
                                bbarWork.add(pb);
                        if (bbarWork.size()+fg.boneSet.size()>maxGroupSize)
                            continue; // If it won't fit, don't do it.
                        if (bestBoneAddRequired==null) {
                            bestBoneAdd=fg;
                            bestBoneAddRequired=bbarWork;
                            bbarWork=new HashSet<PMXFile.PMXBone>();
                        } else {
                            // If less is missing here than in what we're planning to put it in,
                            // this this is a better choice
                            if (bestBoneAddRequired.size()>bbarWork.size()) {
                                bestBoneAdd=fg;
                                bestBoneAddRequired=bbarWork;
                                bbarWork=new HashSet<PMXFile.PMXBone>();
                            }
                        }
                    }
                }
                if (target==null) {
                    if (bestBoneAdd == null) {
                        // there are no groups we can add to, create a new group
                        // We're not doing the "can it fit" check here, if it can't fit your model is broken/weird
                        if (usedBones.size()>maxGroupSize) {
                            System.err.println("WARNING! maxGroupSize of " + maxGroupSize + " is too small for a face which relies on " + usedBones.size() + " bones!");
                        } else {
                            target = new FaceGroup();
                            target.boneSet.addAll(usedBones);
                            groups[i].add(target);
                        }
                    } else {
                        // Well, this is simple :)
                        bestBoneAdd.boneSet.addAll(bestBoneAddRequired);
                        target=bestBoneAdd;
                    }
                }

                if (target!=null) {
                    // Add the vertices to wherever we want to put them
                    // (the facegroup needs to be complete before we start actually writing buffers)
                    target.vertexList.add(vA);
                    target.vertexList.add(vB);
                    target.vertexList.add(vC);
                } else {
                    System.err.println("Faces have been skipped!!! Display artifacting will result.");
                }
                face++;
            }
            System.out.println(groups[i].size()+" facegroups for shading on material "+i);
            // since this set is complete, we can set up the buffers now
            for (FaceGroup fg : groups[i]) {
                fg.bonesBuffer=BufferUtils.createFloatBuffer(fg.vertexList.size()*4);
                fg.vertexBuffer=BufferUtils.createFloatBuffer(fg.vertexList.size()*3);
                fg.normalBuffer=BufferUtils.createFloatBuffer(fg.vertexList.size()*3);
                fg.tangentBuffer=BufferUtils.createFloatBuffer(fg.vertexList.size()*3);
                fg.uvBuffer=BufferUtils.createFloatBuffer(fg.vertexList.size()*2);
                for (PMXFile.PMXVertex vt : fg.vertexList) {
                    fg.bonesBuffer.put(createBoneData(fg, vt));
                    fg.vertexBuffer.put(vt.posX);
                    fg.vertexBuffer.put(vt.posY);
                    fg.vertexBuffer.put(vt.posZ);
                    fg.normalBuffer.put(vt.normalX);
                    fg.normalBuffer.put(vt.normalY);
                    fg.normalBuffer.put(vt.normalZ);
                    fg.tangentBuffer.put(0);
                    fg.tangentBuffer.put(0);
                    fg.tangentBuffer.put(0);
                    fg.uvBuffer.put(vt.texU);
                    fg.uvBuffer.put(vt.texV);
                }
                fg.bonesBuffer.rewind();
                fg.vertexBuffer.rewind();
                fg.normalBuffer.rewind();
                fg.tangentBuffer.rewind();
                fg.uvBuffer.rewind();
            }
        }
    }

    private int weightVertices(int weightType) {
        if (weightType==0)
            return 1;
        if (weightType==1)
            return 2;
        if (weightType==2)
            return 4;
        return 0;
    }

    private float[] createBoneData(FaceGroup fg, PMXFile.PMXVertex v) {
        switch (v.weightType) {
            case 0:
                // we can't actually have "1" as a weight :)
                return new float[]{v.boneIndices[0]+0.5f,v.boneIndices[0]+0.5f,0,0};
            case 1:
                if (v.boneWeights[0]<0)
                    System.err.println("Weird (<) BDEF2 weight detected: "+v.boneWeights[0]);
                if (v.boneWeights[0]>1)
                    System.err.println("Weird (>) BDEF2 weight detected: "+v.boneWeights[0]);
                if (v.boneWeights[0]<=0)
                    return new float[]{v.boneIndices[1]+0.5f,v.boneIndices[1]+0.5f,0,0};
                if (v.boneWeights[0]>=1)
                    return new float[]{v.boneIndices[0]+0.5f,v.boneIndices[0]+0.5f,0,0};
                return new float[]{v.boneIndices[0]+v.boneWeights[0],v.boneIndices[1]+1.0f-v.boneWeights[0],0,0};
            case 2:
                return new float[]{v.boneIndices[0]+v.boneWeights[0],v.boneIndices[1]+v.boneWeights[1],v.boneIndices[2]+v.boneWeights[2],v.boneIndices[3]+v.boneWeights[3]};
            default:
                // Never fail silently... but considering this is a mod people will want to use, don't be a drama queen
                System.err.println("Unknown weight time " + v.weightType + " - assuming basic 1-bone");
                return new float[]{v.boneIndices[0]+0.5f,v.boneIndices[0]+0.5f,0,0};
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
     */
    public void render(IMaterialBinder textureBinder, Shader s) {
        int oldProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        for (int i=0;i<groups.length;i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            textureBinder.bindMaterial(mat);
            for (FaceGroup fg : groups[i]) {
                // Adjust uniforms
                // Then render this chunk of the model.
                GL11.glColor4d(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                GL11.glVertexPointer(3, 0, fg.vertexBuffer);
                GL11.glTexCoordPointer(2, 0, fg.uvBuffer);
                GL11.glNormalPointer(0, fg.normalBuffer);
                GL11.glDrawArrays(GL11.GL_TRIANGLES,0,fg.vertexList.size());
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            }
        }
    }

    public void update(double v) {
        if (anim != null)
            anim.update(v);
    }
}
