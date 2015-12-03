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

import java.nio.FloatBuffer

import moe.nightfall.instrumentality.editor.{UIUtils, EditElement}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.GLU

abstract class View3DElement extends EditElement {
    var rotYaw: Double = 0
    var rotPitch: Double = 0

    protected def draw3D(): Unit

    var translateY = 0.0d
    var scale = 3.0d

    private var dragX = 0
    private var dragY = 0
    private var ignoreFirstDrag = false

    override def draw() {
        super.draw()
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()

        // Virtual viewport
        GL11.glTranslated(-1, -1, 0)
        GL11.glTranslated(UIUtils.widgetX / (UIUtils.scrWidth.toDouble / 2), (UIUtils.scrHeight - (UIUtils.widgetY + height)) / (UIUtils.scrHeight.toDouble / 2), 0)
        GL11.glScaled(width / UIUtils.scrWidth.toDouble, height / UIUtils.scrHeight.toDouble, 1)
        GL11.glTranslated(1, 1, 0)

        // Use this code to debug the virtual viewport code.
        /*
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glColor3d(0, 1, 0)
        GL11.glVertex2d(1, -1)
        GL11.glVertex2d(1, 1)
        GL11.glVertex2d(-1, 1)
        GL11.glVertex2d(-1, -1)
        GL11.glEnd()
        GL11.glBegin(GL11.GL_LINES)
        GL11.glColor3d(1, 0, 1)
        GL11.glVertex2d(-1, -1)
        GL11.glVertex2d(1, 1)
        GL11.glVertex2d(-1, 1)
        GL11.glVertex2d(1, -1)
        GL11.glEnd()
        */
        val asp = width / height.toFloat
        GLU.gluPerspective(45, asp, 0.1f, 100)

        // Now transfer into modelview and do stuff.
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glTranslated(0, 0, -5)

        GL11.glScaled(scale, scale, scale)

        GL11.glRotated(rotPitch, 1, 0, 0)
        GL11.glRotated(rotYaw, 0, 1, 0)

        GL11.glTranslated(0, translateY, 0)

        GL11.glDisable(GL11.GL_CULL_FACE)
        draw3D()
        GL11.glEnable(GL11.GL_CULL_FACE)
        // Cleanup.
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPopMatrix()
    }

    private def dumpProject(fb: FloatBuffer) {
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, fb)
        println("x y z w (Transposed : each row is the multiplier from sV)")
        for (i <- 1 to 4) {
            for (i <- 1 to 4) {
                print(fb.get() + " ")
            }
            println()
        }
        fb.rewind()
        println("-")
    }

    override def mouseMove(x: Int, y: Int, buttons: Array[Boolean]) {
        if (buttons(0)) {
            rotYaw += x - dragX
            rotPitch += y - dragY
        }
        if (buttons(1)) {
            translateY -= (y - dragY) / (20.0d * scale)
            scale += (x - dragX) / 20.0d
            if (scale < 0.05d)
                scale = 0.05d
        }
        ignoreFirstDrag = false
        dragX = x
        dragY = y
    }

    override def mouseStateChange(x: Int, y: Int, isDown: Boolean, button: Int) {
        super.mouseStateChange(x, y, isDown, button)
        if (isDown && (button == 0)) {
            ignoreFirstDrag = true
        }
    }

    override def mouseEnterLeave(isInside: Boolean) {
        super.mouseEnterLeave(isInside)
        ignoreFirstDrag = true
    }
}
