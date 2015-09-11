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

import moe.nightfall.instrumentality.PoseBoneTransform;
import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.controls.AdjusterElement;
import moe.nightfall.instrumentality.editor.controls.CheckboxElement;
import moe.nightfall.instrumentality.editor.controls.LabelElement;

/**
 * Created on 10/09/15.
 */
public class PoseEditParamsElement extends EditElement {
    public final PoseEditElement parentPE;
    public final CheckboxElement showModel, showDebug;
    public final LabelElement showModelText, showDebugText;
    public AdjusterElement[] allAdjusters = new AdjusterElement[3 * 4];

    public PoseEditParamsElement(PoseEditElement parent) {
        parentPE = parent;
        AdjusterElement.IAdjustable[] adjs = new AdjusterElement.IAdjustable[allAdjusters.length];
        final String[] names = {
                "X0", "Y0", "Z0",
                "X1", "Y1", null,
                "X2", null, null,
                "TX0", "TY0", "TZ0",
        };
        for (int i = 0; i < adjs.length; i++) {
            if (names[i] != null) {
                final String nn = names[i];
                adjs[i] = new AdjusterElement.IAdjustable() {
                    @Override
                    public double getValue() {
                        try {
                            return (Float) (PoseBoneTransform.class.getField(nn).get(parentPE.getEditPBT()));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                        return 0;
                    }

                    @Override
                    public void setValue(double v) {
                        try {
                            PoseBoneTransform.class.getField(nn).set(parentPE.getEditPBT(), (float) v);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                    }
                };
            }
        }
        for (int i = 0; i < adjs.length; i++) {
            if (adjs[i] != null) {
                AdjusterElement ae = new AdjusterElement(names[i] + ":");
                ae.adjustable = adjs[i];
                allAdjusters[i] = ae;
                subElements.add(ae);
            }
        }
        Runnable nullRunnable = new Runnable() {
            @Override
            public void run() {
            }
        };
        showModel = new CheckboxElement(nullRunnable);
        subElements.add(showModel);
        showDebug = new CheckboxElement(nullRunnable);
        showDebug.setChecked(true);
        subElements.add(showDebug);
        showModelText = new LabelElement("S.Mdl");
        subElements.add(showModelText);
        showDebugText = new LabelElement("S.Str");
        subElements.add(showDebugText);
    }

    @Override
    public void layout() {
        super.layout();
        int tW = getWidth() / 3;
        int tH = getHeight() / 5;
        for (int i = 0; i < allAdjusters.length; i++) {
            AdjusterElement ae = allAdjusters[i];
            if (ae != null) {
                ae.setSize(tW, tH);
                ae.posX = (i % 3) * tW;
                ae.posY = (i / 3) * tH;
            }
        }
        showModelText.posY = showModel.posY = showDebug.posY = showDebugText.posY = tH * 4;

        int labelSize = (getWidth() / 2) - tH;
        showModel.posX = 0;
        showModel.setSize(tH, tH);
        showModelText.posX = tH;
        showModelText.setSize(labelSize, tH);

        showDebug.posX = tH + labelSize;
        showDebug.setSize(tH, tH);
        showDebugText.posX = tH + labelSize + tH;
        showDebugText.setSize(labelSize, tH);
    }
}
