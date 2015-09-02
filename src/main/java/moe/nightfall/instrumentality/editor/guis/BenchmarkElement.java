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
import moe.nightfall.instrumentality.editor.UIFont;
import moe.nightfall.instrumentality.editor.controls.ModelElement;
import org.lwjgl.opengl.GL11;

/**
 * Created on 01/09/15.
 */
public class BenchmarkElement extends EditElement {
    public final PMXModel myModel;
    public double time;

    public BenchmarkElement(PMXModel mdl) {
        myModel = mdl;
    }

    @Override
    public void update(double dTime) {
        super.update(dTime);
        time += dTime;
        if (time > 2.0d) {
            ModelElement me = new ModelElement(false);
            me.setSize(200, 320);
            subElements.add(me);
            layout();
            time -= 2.0d;
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

    @Override
    public void draw(int scrWidth, int scrHeight) {
        super.draw(scrWidth, scrHeight);
        GL11.glPushMatrix();
        GL11.glTranslated(1, 1, 0);
        GL11.glScaled(2, 2, 2);
        GL11.glColor3d(1, 1, 1);
        GL11.glPushMatrix();

        String str = "If you've done 6 impossible things today,\nwhy not top it off with a meal at Milliways,\nthe cafe at the end of the universe.\n1234567890\nthe quick brown fox jumped over the lazy dog\nTHE QUICK BROWN FOX JUMPED OVER THE LAZY DOG";
        for (char c : str.toCharArray()) {
            if (c == 10) {
                GL11.glPopMatrix();
                GL11.glTranslated(0, 10, 0);
                GL11.glPushMatrix();
            } else {
                UIFont.drawChar(c);
                GL11.glTranslated(7, 0, 0);
            }
        }

        GL11.glPopMatrix();
        GL11.glPopMatrix();
    }
}
