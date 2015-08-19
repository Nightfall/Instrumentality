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
package moe.nightfall.instrumentality.editor;

import moe.nightfall.instrumentality.Main;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/**
 * Created on 18/08/15.
 */
public class EditElement {
    // Note that posX and posY may be ignored if this is the root
    private int sizeWidth, sizeHeight;
    public int posX, posY;

    protected void drawRect(int x, int y, int w, int h, double r, double g, double b) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4d(r, g, b, 1);
        GL11.glVertex3d(x, y, 0);
        GL11.glVertex3d(x, y + h, 0);
        GL11.glVertex3d(x + w, y + h, 0);
        GL11.glVertex3d(x + w, y, 0);
        GL11.glEnd();
    }

    protected void drawSkinnedRect(int x, int y, int w, int h, double strength) {
        double str = 1.0f;
        double step = (1.0f - strength) / 4;
        for (int i = 0; i < 4; i++) {
            drawRect(x + i, y + i, w - (i * 2), h - (i * 2), 0, str * 0.5f, str);
            str -= step;
        }
    }

    public void draw() {
        drawSkinnedRect(posX, posY, sizeWidth, sizeHeight, 0.5f);
    }

    public void setSize(int width, int height) {
        sizeWidth = width;
        sizeHeight = height;
    }

    public int getWidth() {
        return sizeWidth;
    }

    public int getHeight() {
        return sizeHeight;
    }

    public void cleanup() {

    }
}
