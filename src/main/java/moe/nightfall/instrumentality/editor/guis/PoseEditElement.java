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
package moe.nightfall.instrumentality.editor.guis;

import moe.nightfall.instrumentality.*;
import moe.nightfall.instrumentality.PMXFile.PMXBone;
import moe.nightfall.instrumentality.animations.PoseAnimation;
import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.controls.TreeviewElement;
import moe.nightfall.instrumentality.editor.controls.View3DElement;
import org.lwjgl.opengl.GL11;

import java.util.LinkedList;

public class PoseEditElement extends EditElement {
    public PoseAnimation editedPose;
    public PMXInstance pmxInst;
    public View3DElement model;
    public TreeviewElement<PMXFile.PMXBone> tView;
    public PoseEditParamsElement params;
    public int selectedBoneId = 0;

    public PoseEditElement(PoseAnimation ep, PMXModel pm) {
        params = new PoseEditParamsElement(this);
        pmxInst = new PMXInstance(pm);
        pmxInst.anim = ep;
        editedPose = ep;
        model = new View3DElement() {
            @Override
            protected void draw3d() {
                GL11.glPushMatrix();
                GL11.glTranslated(0, -0.5, 0);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                GL11.glColor3d(0.5, 0.5, 0.5);
                GL11.glVertex3d(-0.5, 0, -0.5);
                GL11.glVertex3d(0.5, 0, -0.5);
                GL11.glVertex3d(0.5, 0, 0.5);
                GL11.glVertex3d(-0.5, 0, 0.5);
                GL11.glVertex3d(-0.5, 0, -0.5);
                GL11.glEnd();
                GL11.glBegin(GL11.GL_LINES);
                GL11.glColor3d(0.5, 0.5, 0.5);
                GL11.glVertex3d(0, 0, -0.5);
                GL11.glVertex3d(0, 0, 0.5);
                GL11.glVertex3d(-0.5, 0, 0);
                GL11.glVertex3d(0.5, 0, 0);
                GL11.glEnd();
                double s = 1 / pmxInst.theModel.height;
                GL11.glScaled(s, s, s);
                if (params.showModel.getChecked())
                    pmxInst.render(Loader.shaderBoneTransform, 1, 1, 1, 2);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                if (params.showDebug.getChecked())
                    pmxInst.renderDebug(selectedBoneId);
                GL11.glPopMatrix();
            }
        };
        subElements.add(model);
        tView = new TreeviewElement<PMXFile.PMXBone>(new TreeviewElement.INodeStructurer<PMXFile.PMXBone>() {

            @Override
            public String getNodeName(PMXFile.PMXBone n) {
                return n.globalName;
            }

            @Override
            public Iterable<PMXFile.PMXBone> getChildNodes(PMXFile.PMXBone n) {
                int parId = -1;
                if (n != null)
                    parId = n.boneId;
                LinkedList<PMXFile.PMXBone> ll = new LinkedList<PMXFile.PMXBone>();
                for (PMXFile.PMXBone pb : pmxInst.theFile.boneData)
                    if (pb.parentBoneIndex == parId)
                        ll.add(pb);
                return ll;
            }

            @Override
            public void onNodeClick(PMXBone n) {
                selectedBoneId = n.boneId;
            }
        });
        subElements.add(tView);
        subElements.add(params);
    }

    @Override
    public void layout() {
        model.posX = 0;
        model.posY = 0;
        int hSplit = (int) (getWidth() * 0.40d);
        int vSplit = (int) (getHeight() * 0.8d);
        model.setSize(hSplit, getHeight());
        tView.posX = hSplit;
        tView.posY = 0;
        tView.setSize(getWidth() - hSplit, vSplit);
        params.posX = hSplit;
        params.posY = vSplit;
        params.setSize(getWidth() - hSplit, getHeight() - vSplit);
    }

    public PoseBoneTransform getEditPBT() {
        String mid = pmxInst.theFile.boneData[selectedBoneId].globalName.toLowerCase();
        PoseBoneTransform pbt = editedPose.hashMap.get(mid);
        if (pbt == null) {
            pbt = new PoseBoneTransform();
            editedPose.hashMap.put(mid, pbt);
        }
        return pbt;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        pmxInst.cleanupGL();
    }
}
