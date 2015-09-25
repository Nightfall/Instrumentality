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
package moe.nightfall.instrumentality.editor

import org.lwjgl.opengl.GL11

import scala.collection.mutable.ListBuffer

/**
 * Base element of the UI framework
 * Created on 18/08/15.
 */
abstract class EditElement {
    // Note that posX and posY may be ignored if this is the root
    private var sizeWidth, sizeHeight: Int = _
    var posX, posY: Int = _

    val subElements = ListBuffer[EditElement]()
    var colourStrength = 0.25f
    var borderWidth = 8

    private var lastHoverTarget: Option[EditElement] = None
    var selectedSubelement: Option[EditElement] = None

    protected def drawRect(x: Int, y: Int, w: Int, h: Int, r: Double, g: Double, b: Double) {
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glColor4d(r, g, b, 1)
        GL11.glVertex3d(x, y, 0)
        GL11.glVertex3d(x, y + h, 0)
        GL11.glVertex3d(x + w, y + h, 0)
        GL11.glVertex3d(x + w, y, 0)
        GL11.glEnd()
    }

    protected def drawSkinnedRect(x: Int, y: Int, w: Int, h: Int, strength: Double) {
        val sz = borderWidth;

        var str = 1.0d
        val step = (1.0f - strength) / (sz * 2)
        for (i <- 0 until sz) {
            drawRect(x + i, y + i, w - (i * 2), h - (i * 2), 0, str * 0.5f, str)
            str -= step
        }
    }

    def drawSubelements(scrWidth: Int, scrHeight: Int) {
        subElements foreach { ee =>
            GL11.glPushMatrix();
            GL11.glTranslated(ee.posX, ee.posY, 0);
            ee.draw(scrWidth, scrHeight);
            GL11.glPopMatrix();
        }
    }

    def setSize(width: Int, height: Int) {
        sizeWidth = width
        sizeHeight = height
        layout()
    }

    def width = sizeWidth

    def height = sizeHeight

    def findElementAt(x: Int, y: Int): Option[EditElement] = {
        return subElements find { ee =>
            (x >= ee.posX) && (x < ee.posX + ee.width) &&
                (y >= ee.posY) && (y < ee.posY + ee.height)
        }
    }

    def mouseMoveSubelements(x: Int, y: Int, buttons: Array[Boolean]) = {
        val targetElement = findElementAt(x, y)
        if (targetElement != lastHoverTarget) {
            lastHoverTarget.map(_.mouseEnterLeave(false))
            targetElement.map(_.mouseEnterLeave(true))
            lastHoverTarget = targetElement
        }
        targetElement map { el =>
            el.mouseMove(x - el.posX, y - el.posY, buttons)
        }
    }

    def updateSubelements(dTime: Double) {
        subElements foreach (_.update(dTime))
    }

    // Functions meant for overriding

    def layout() = ()

    def draw(scrWidth: Int, scrHeight: Int) {
        drawSkinnedRect(0, 0, sizeWidth, sizeHeight, colourStrength)
        drawSubelements(scrWidth, scrHeight);
    }

    // RANDOM NOTE II : buttons[] is used for detecting dragging,
    // without embedding the mouse state into every element // TODO Wouldn't do that, its ugly

    def mouseMove(x: Int, y: Int, buttons: Array[Boolean]) {
        mouseMoveSubelements(x, y, buttons)
    }

    // RANDOM NOTE: We assume people only have 2 buttons, others must be ignored.
    // This is because not all people have middle-mouse-buttons, it's unfair to assume.

    def mouseStateChange(x: Int, y: Int, isDown: Boolean, isRight: Boolean) {
        val targetElement = findElementAt(x, y)
        if (isDown)
            selectedSubelement = targetElement
        targetElement.map { el =>
            el.mouseStateChange(x - el.posX, y - el.posY, isDown, isRight)
        }
    }

    def mouseEnterLeave(isInside: Boolean) {
        if (!isInside) {
            lastHoverTarget.map(_.mouseEnterLeave(false))
            lastHoverTarget = None
        }
    }

    // RANDOM NOTE III : Always call super if overriding this function.

    def cleanup(): Unit = subElements.foreach(_.cleanup())

    def update(dTime: Double) {
        updateSubelements(dTime)
    }

}
