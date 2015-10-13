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
package moe.nightfall.instrumentality.editor.control

import org.lwjgl.opengl.GL11


class ArrowButtonElement(arrowAngle: Double, toRun: => Unit) extends ButtonElement(toRun) {

    override def draw() {
        super.draw()
        val w: Double = 1 / 8.0
        val h: Double = 1 / 8.0
        val wu = 3
        val hu = 3

        GL11.glPushMatrix()
        GL11.glTranslated(width * w * 4, height * h * 4, 0)
        GL11.glScaled(width, height, 1)
        GL11.glRotated(arrowAngle, 0, 0, 1)
        GL11.glColor3d(0, 0, 0)
        GL11.glBegin(GL11.GL_TRIANGLES)
        GL11.glVertex2d(-(w * wu), -(h * hu))
        GL11.glVertex2d(w * (wu - 1), 0)
        GL11.glVertex2d(w * wu, 0)
        GL11.glVertex2d(-(w * wu), h * hu)
        GL11.glVertex2d(w * wu, 0)
        GL11.glVertex2d(w * (wu - 1), 0)
        GL11.glEnd()
        GL11.glPopMatrix()
    }
}
