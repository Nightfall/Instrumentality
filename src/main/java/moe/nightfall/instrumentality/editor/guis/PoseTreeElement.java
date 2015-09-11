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

import moe.nightfall.instrumentality.Loader;
import moe.nightfall.instrumentality.ModelCache;
import moe.nightfall.instrumentality.animations.PoseSet;
import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.controls.ButtonBarContainerElement;
import moe.nightfall.instrumentality.editor.controls.TextButtonElement;
import moe.nightfall.instrumentality.editor.controls.TreeviewElement;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created on 11/09/15.
 */
public class PoseTreeElement extends EditElement {
    public final TreeviewElement<String> treeviewElement;
    public final PoseSet targetSet;
    public final TextButtonElement saveButton;

    public PoseTreeElement(PoseSet ps, final ButtonBarContainerElement whereAmI) {
        targetSet = ps;
        treeviewElement = new TreeviewElement<String>(new TreeviewElement.INodeStructurer<String>() {
            @Override
            public String getNodeName(String n) {
                return n;
            }

            @Override
            public Iterable<String> getChildNodes(String n) {
                LinkedList<String> ll = new LinkedList<String>();
                for (String s : targetSet.allPoses.keySet())
                    if (n == null) {
                        if (!targetSet.poseParents.containsKey(s))
                            ll.add(s);
                    } else {
                        if (n.equals(targetSet.poseParents.get(s)))
                            ll.add(s);
                    }
                return ll;
            }

            @Override
            public void onNodeClick(String n) {
                whereAmI.setUnderPanel(new PoseEditElement(targetSet.allPoses.get(n), ModelCache.getLocal(Loader.currentFile), targetSet.createEditAnimation(n)), false);
            }
        });
        subElements.add(treeviewElement);
        saveButton = new TextButtonElement("Save", new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fos = new FileOutputStream(ModelCache.modelRepository + "/" + Loader.currentFile + "/mmcposes.dat");
                    DataOutputStream dos = new DataOutputStream(fos);
                    targetSet.save(dos);
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        subElements.add(saveButton);
    }

    @Override
    public void layout() {
        super.layout();
        treeviewElement.posX = 0;
        treeviewElement.posY = 0;
        int bbarHeight = (int) (getHeight() * 0.1d);
        treeviewElement.setSize(getWidth(), getHeight() - bbarHeight);
        saveButton.posX = 0;
        saveButton.posY = getHeight() - bbarHeight;
        saveButton.setSize(getWidth(), bbarHeight);
    }
}
