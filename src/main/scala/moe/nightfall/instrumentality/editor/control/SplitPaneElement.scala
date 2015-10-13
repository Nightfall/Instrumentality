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

import moe.nightfall.instrumentality.editor.EditElement
import org.lwjgl.opengl.GL11

/**
 * Created on 05/10/15.
 */
class SplitPaneElement(val panelA: EditElement, val panelB: EditElement, val horizontal: Boolean, var resizeWeight: Double) extends EditElement {
    subElements += panelA += panelB
    var splitPix = 0
    // Used for tracking resizes
    var lastAxisLen = 1
    var firstResize = true
    // This is used to ensure border manipulation only manipulates the border
    var movingBorder = false
    // Makes manipulating multi-split environments easier :)
    var elementLock: Option[EditElement] = None

    def axisLen = if (horizontal) width else height

    override def draw() {
        drawSubelements()
        GL11.glLineWidth(1)
        GL11.glLineStipple(1, 0xF0F0.toShort)
        GL11.glEnable(GL11.GL_LINE_STIPPLE)
        GL11.glBegin(GL11.GL_LINES)
        GL11.glColor3d(1, 0, 0)
        if (horizontal) {
            GL11.glVertex2d(splitPix, 0)
            GL11.glVertex2d(splitPix, height)
        } else {
            GL11.glVertex2d(0, splitPix)
            GL11.glVertex2d(width, splitPix)
        }
        GL11.glEnd()
        GL11.glDisable(GL11.GL_LINE_STIPPLE)
    }

    override def layout() {
        panelA.posX = 0
        panelA.posY = 0
        if (axisLen != lastAxisLen) {
            val newAxisLen = axisLen
            if (firstResize) {
                firstResize = false
                splitPix = width / 2
            } else {
                splitPix += ((newAxisLen - lastAxisLen) * resizeWeight).toInt
            }
            lastAxisLen = newAxisLen
        }
        if (horizontal) {
            panelA.setSize(splitPix, height)
            panelB.posX = splitPix
            panelB.posY = 0
            panelB.setSize(width - splitPix, height)
        } else {
            panelA.setSize(width, splitPix)
            panelB.posX = 0
            panelB.posY = splitPix
            panelB.setSize(width, height - splitPix)
        }
    }

    override def mouseMove(x: Int, y: Int, buttons: Array[Boolean]): Unit = {
        if (elementLock.isEmpty) {
            if (!movingBorder) {
                super.mouseMove(x, y, buttons)
            } else {
                if (!buttons(0)) {
                    movingBorder = false
                } else {
                    splitPix = if (horizontal) x else y
                    layout()
                }
            }
        } else {
            elementLock.get.mouseMove(x - elementLock.get.posX, y - elementLock.get.posY, buttons)
        }
    }

    override def mouseStateChange(x: Int, y: Int, isDown: Boolean, button: Int): Unit = {
        if (elementLock.isEmpty) {
            if (button == 0)
                if (isDown) {
                    val axis = if (horizontal) x else y
                    if (axis > (splitPix - borderWidth))
                        if (axis < (splitPix + borderWidth))
                            movingBorder = true
                } else {
                    movingBorder = false
                }
            if (!movingBorder) {
                super.mouseStateChange(x, y, isDown, button)
                elementLock = selectedSubelement
            }
        } else {
            elementLock.get.mouseStateChange(x - elementLock.get.posX, y - elementLock.get.posY, isDown, button)
            if (!isDown)
                elementLock = None
        }
    }
}
