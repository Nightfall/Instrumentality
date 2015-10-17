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
package moe.nightfall.instrumentality.editor.twlgui

import java.nio.FloatBuffer

import de.matthiasmann.twl.{GUI, Widget}
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.GLU

/**
 * NOTE: For this to work properly, the GUI must fill the whole screen.
 * Created on 16/10/15.
 */
abstract class View3DWidget extends Widget {
    var rotYaw: Double = 0
    var rotPitch: Double = 0

    protected def draw3D(): Unit

    private var dragX = 0
    private var dragY = 0
    private var translateY = 0.0d
    private var scale = 3.0d
    private var ignoreFirstDrag = false

    override def paint(gui: GUI) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthFunc(GL11.GL_LEQUAL)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()

        // Virtual viewport
        GL11.glTranslated(-1, -1, 0)
        GL11.glTranslated(getInnerX / (gui.getWidth.toDouble / 2), (gui.getHeight - (getInnerY + getInnerHeight)) / (gui.getHeight.toDouble / 2), 0)
        GL11.glScaled(getInnerWidth / gui.getWidth.toDouble, getInnerHeight / gui.getHeight.toDouble, 1)
        GL11.glTranslated(1, 1, 0)

        // Use this code to debug the virtual viewport code.


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

        val asp = getInnerWidth / getInnerHeight.toFloat
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
        GL11.glPopAttrib()
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

    /*
        override def mouseMove(x: Int, y: Int, buttons: Array[Boolean]) {
            if (buttons(0)) {
                rotYaw += x - dragX
                rotPitch += y - dragY
            }
            if (buttons(1)) {
                translateY -= (y - dragY) / (20.0d * scale)
                scale += (x - dragX) / 20.0d
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
        */
}
