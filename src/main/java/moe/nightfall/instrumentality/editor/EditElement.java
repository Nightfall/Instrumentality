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

import org.lwjgl.opengl.GL11;

import java.util.LinkedList;

/**
 * Base element of the UI framework
 * Created on 18/08/15.
 */
public class EditElement {
    // Note that posX and posY may be ignored if this is the root
    private int sizeWidth, sizeHeight;
    public int posX, posY;

    public LinkedList<EditElement> subElements = new LinkedList<EditElement>();
    public float colourStrength = 0.25f;
    public int borderWidth = 8;

    private EditElement lastHoverTarget;

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
        int sz = borderWidth;

        double str = 1.0f;
        double step = (1.0f - strength) / (sz * 2);
        for (int i = 0; i < sz; i++) {
            drawRect(x + i, y + i, w - (i * 2), h - (i * 2), 0, str * 0.5f, str);
            str -= step;
        }
    }

    public void drawSubelements(int scrWidth, int scrHeight) {
        for (EditElement ee : subElements) {
            GL11.glPushMatrix();
            GL11.glTranslated(ee.posX, ee.posY, 0);
            ee.draw(scrWidth, scrHeight);
            GL11.glPopMatrix();
        }
    }

    public void setSize(int width, int height) {
        sizeWidth = width;
        sizeHeight = height;
        layout();
    }

    public int getWidth() {
        return sizeWidth;
    }

    public int getHeight() {
        return sizeHeight;
    }

    public EditElement findElementAt(int x, int y) {
        for (EditElement ee : subElements) {
            if ((x >= ee.posX) && (x < ee.posX + ee.getWidth()))
                if ((y >= ee.posY) && (y < ee.posY + ee.getHeight()))
                    return ee;
        }
        return null;
    }

    public void mouseMoveSubelements(int x, int y, boolean[] buttons) {
        EditElement targetElement = findElementAt(x, y);
        if (targetElement != lastHoverTarget) {
            if (lastHoverTarget != null)
                lastHoverTarget.mouseEnterLeave(false);
            if (targetElement != null)
                targetElement.mouseEnterLeave(true);
            lastHoverTarget = targetElement;
        }
        if (targetElement != null)
            targetElement.mouseMove(x - targetElement.posX, y - targetElement.posY, buttons);
    }

    public void updateSubelements(double dTime) {
        for (EditElement ee : subElements)
            ee.update(dTime);
    }

    // Functions meant for overriding

    public void layout() {
    }

    public void draw(int scrWidth, int scrHeight) {
        drawSkinnedRect(0, 0, sizeWidth, sizeHeight, colourStrength);
        drawSubelements(scrWidth, scrHeight);
    }

    // RANDOM NOTE II : buttons[] is used for detecting dragging,
    // without embedding the mouse state into every element

    public void mouseMove(int x, int y, boolean[] buttons) {
        mouseMoveSubelements(x, y, buttons);
    }

    // RANDOM NOTE: We assume people only have 2 buttons, others must be ignored.
    // This is because not all people have middle-mouse-buttons, it's unfair to assume.

    public void mouseStateChange(int x, int y, boolean isDown, boolean isRight) {
        EditElement targetElement = findElementAt(x, y);
        if (targetElement != null)
            targetElement.mouseStateChange(x - targetElement.posX, y - targetElement.posY, isDown, isRight);
    }

    public void mouseEnterLeave(boolean isInside) {
        if (!isInside) {
            if (lastHoverTarget != null) {
                lastHoverTarget.mouseEnterLeave(false);
                lastHoverTarget = null;
            }
        }
    }

    // RANDOM NOTE III : Always call super if overriding this function.

    public void cleanup() {
        for (EditElement ee : subElements)
            ee.cleanup();
    }

    public void update(double dTime) {
        updateSubelements(dTime);
    }

}
