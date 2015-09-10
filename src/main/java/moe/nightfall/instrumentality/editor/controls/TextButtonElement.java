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
package moe.nightfall.instrumentality.editor.controls;

import moe.nightfall.instrumentality.Loader;
import moe.nightfall.instrumentality.editor.UIUtils;

import org.lwjgl.opengl.GL11;

public class TextButtonElement extends ButtonElement {
    public String theText;

    public TextButtonElement(String txt, Runnable r) {
        super(r);
        theText = txt;
    }

    @Override
    public void draw(int scrWidth, int scrHeight) {
        super.draw(scrWidth, scrHeight);
        GL11.glPushMatrix();
        double scale = (getHeight() - borderWidth) / 8d;
        if (scale < 1.7) {
            scale = getHeight() / 8d;
        } else {
            GL11.glTranslated(borderWidth/2, borderWidth/2, 0);
        }
        GL11.glScaled(scale, scale, 1);
        UIUtils.drawText(theText, 2);
        GL11.glPopMatrix();
    }
}
