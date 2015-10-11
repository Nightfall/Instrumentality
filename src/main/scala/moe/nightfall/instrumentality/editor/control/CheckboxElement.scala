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

// TODO: Is there a better way to do this?
class CheckboxElement(toRun: => Unit) extends ButtonElement {

    // so this can be changed...
    var checked = false

    onClick = () => {
        checked = !checked
        toRun
    }

    override def draw(ox: Int, oy: Int, scrWidth: Int, scrHeight: Int) {
        super.draw(ox, oy, scrWidth, scrHeight)
        GL11.glColor3d(0, 0, 0)
        val xL = width / 4
        val xM = width / 2
        val xR = (width / 4) * 3

        val yU = height / 4
        val yM = height / 2
        val yL = (height / 4) * 3
        if (checked) {
            GL11.glBegin(GL11.GL_LINE_STRIP)
            GL11.glVertex2d(xL, yM)
            GL11.glVertex2d(xM, yL)
            GL11.glVertex2d(xR, yU)
            GL11.glEnd()
        } else {
            GL11.glBegin(GL11.GL_LINES)
            GL11.glVertex2d(xL, yU)
            GL11.glVertex2d(xR, yL)
            GL11.glVertex2d(xR, yU)
            GL11.glVertex2d(xL, yL)
            GL11.glEnd()
        }
    }
}
