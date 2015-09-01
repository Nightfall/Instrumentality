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

import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.controls.ModelElement;

import java.util.LinkedList;

/**
 * Created on 01/09/15.
 */
public class BenchmarkElement extends EditElement {
    public final PMXModel myModel;
    public LinkedList<ModelElement> flows;
    public double time;

    public BenchmarkElement(PMXModel mdl) {
        myModel = mdl;
    }

    @Override
    public void update(double dTime) {
        super.update(dTime);
        time += dTime;
        if (time > 1.0d) {
            ModelElement me = new ModelElement(false);
            me.setSize(200, 320);
            subElements.add(me);
            layout();
            time -= 1.0d;
        }
    }

    @Override
    public void layout() {
        int i = 0;
        for (EditElement me : subElements) {
            me.posX = i * 50;
            me.posY = (me.posX / getWidth()) * 50;
            me.posX %= getWidth();
            me.posY %= getHeight();
            i++;
        }
    }
}
