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

import org.lwjgl.opengl.GL11;

public class ArrowButtonElement extends ButtonElement {
    public double arrowAngle=0;
    public ArrowButtonElement(double ang,Runnable r) {
        super(r);
        arrowAngle=ang;
    }
    @Override
    public void draw(int scrWidth, int scrHeight) {
        super.draw(scrWidth, scrHeight);
        double w=getWidth()/8.0;
        double h=getHeight()/8.0;
        int wu=3;
        int hu=3;
        GL11.glPushMatrix();
        GL11.glTranslated(w*4, h*4, 0);
        GL11.glRotated(arrowAngle, 0, 0, 1);
        GL11.glColor3d(0, 0, 0);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2d(-(w*wu), -(h*hu));
        GL11.glVertex2d((w*(wu-1)), 0);
        GL11.glVertex2d((w*wu), 0);
        GL11.glVertex2d(-(w*wu), (h*hu));
        GL11.glVertex2d((w*wu), 0);
        GL11.glVertex2d((w*(wu-1)), 0);
        GL11.glEnd();
        GL11.glPopMatrix();
    }
}
