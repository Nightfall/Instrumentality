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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * It would be nice if we could move the FaceGroup generator into this file.
 * Created on 19/08/15.
 */
public class PMXModel {
    public PMXFile theFile;
    public HashMap<String, Integer> materials = new HashMap<String, Integer>();

    /**
     * Height of the model, determinated by the highest vertex
     */
    public final float height;

    public final LinkedList<FaceGroup>[] groups;

    public class FaceGroup {
        // Used while creating the FaceGroup, this is discarded after the group is compiled
        final HashSet<PMXFile.PMXBone> boneSet = new HashSet<PMXFile.PMXBone>();

        // Buffers for compiling
        final LinkedList<PMXFile.PMXVertex> vertexList = new LinkedList<PMXFile.PMXVertex>();

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
        groups = new LinkedList[theFile.matData.length];

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
}
