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

import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.controls.ArrowButtonElement;
import moe.nightfall.instrumentality.editor.controls.ModelElement;

import java.util.LinkedList;

/**
 * Created on 25/08/15.
 */
public class ModelChooserElement extends EditElement {
    private ModelElement[] group = new ModelElement[3];
    private EditElement[] buttonbar = new EditElement[3];
    private final String[] availableModels;
    private int ptrStart = 0;

    public ModelChooserElement(Iterable<String> models) {
        LinkedList<String> ls = new LinkedList<String>();
        ls.add(null);
        for (String s : models)
            ls.add(s);
        availableModels = ls.toArray(new String[0]);
        colourStrength = 0.5f;
        for (int i = 0; i < group.length; i++) {
            group[i] = new ModelElement(true);
            subElements.add(group[i]);
        }
        buttonbar[0] = new ArrowButtonElement(180, new Runnable() {
            @Override
            public void run() {
                ptrStart--;
                updatePosition();
            }
        });
        buttonbar[1] = new ArrowButtonElement(0, new Runnable() {
            @Override
            public void run() {
                ptrStart++;
                updatePosition();
            }
        });
        buttonbar[2] = new ModelElement(false);
        for (int j = 0; j < buttonbar.length; j++)
            subElements.add(buttonbar[j]);
        updatePosition();
    }

    @Override
    public void layout() {
        for (int i = 0; i < group.length; i++) {
            group[i].posX = i * (getWidth() / group.length);
            group[i].posY = getHeight() / 4;
            group[i].setSize(getWidth() / group.length, (getHeight() / 4) * 3);
        }
        int bbSize = (getHeight() / 4);
        for (int j = 0; j < buttonbar.length; j++) {
            buttonbar[j].posX = bbSize * j;
            buttonbar[j].posY = 0;
            buttonbar[j].setSize(bbSize, bbSize);
        }
    }

    public void updatePosition() {
        for (int i = 0; i < group.length; i++)
            group[i].setModel(availableModels[(i + ptrStart) % availableModels.length]);
    }
}
