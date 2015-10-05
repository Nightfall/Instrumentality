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
import org.lwjgl.util.vector.Vector4f

import scala.collection.mutable.ListBuffer

/**
 * Base element of the UI framework
 * Created on 18/08/15.
 */
abstract class EditElement {
    // Note that posX and posY may be ignored if this is the root

    // No! Do not direct access to sizeWidth and sizeHeight!
    private var sizeWidth, sizeHeight: Int = _
    var posX, posY: Int = _

    val subElements = ListBuffer[EditElement]()
    var colourStrength = 0.9f
    var borderWidth = 6

    private var lastHoverTarget: Option[EditElement] = None
    var selectedSubelement: Option[EditElement] = None

    // Move those to utility object
    protected def drawQRect(x: Int, y: Int, w: Int, h: Int, c_tl: Vector4f, c_ll: Vector4f, c_lr: Vector4f, c_tr: Vector4f) {
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glColor4d(c_tl.x, c_tl.y, c_tl.z, c_tl.w)
        GL11.glVertex3d(x, y, 0)
        GL11.glColor4d(c_ll.x, c_ll.y, c_ll.z, c_ll.w)
        GL11.glVertex3d(x, y + h, 0)
        GL11.glColor4d(c_lr.x, c_lr.y, c_lr.z, c_lr.w)
        GL11.glVertex3d(x + w, y + h, 0)
        GL11.glColor4d(c_tr.x, c_tr.y, c_tr.z, c_tr.w)
        GL11.glVertex3d(x + w, y, 0)
        GL11.glEnd()
    }

    protected def drawRect(x: Int, y: Int, w: Int, h: Int, c_tl: Vector4f, c_ll: Vector4f, c_lr: Vector4f, c_tr: Vector4f) {
        // some GPUs render this as 2 tris, doesn't look good, try to define a saner way
        val central = Vector4f.add(Vector4f.add(c_tl, c_ll, null), Vector4f.add(c_lr, c_tr, null), null)

        GL11.glBegin(GL11.GL_TRIANGLE_FAN)

        GL11.glColor4d(central.x / 4, central.y / 4, central.z / 4, central.w / 4)
        GL11.glVertex3d(x + (w / 2), y + (h / 2), 0)

        GL11.glColor4d(c_tl.x, c_tl.y, c_tl.z, c_tl.w)
        GL11.glVertex3d(x, y, 0)
        GL11.glColor4d(c_ll.x, c_ll.y, c_ll.z, c_ll.w)
        GL11.glVertex3d(x, y + h, 0)

        GL11.glVertex3d(x, y + h, 0)
        GL11.glColor4d(c_lr.x, c_lr.y, c_lr.z, c_lr.w)
        GL11.glVertex3d(x + w, y + h, 0)

        GL11.glVertex3d(x + w, y + h, 0)
        GL11.glColor4d(c_tr.x, c_tr.y, c_tr.z, c_tr.w)
        GL11.glVertex3d(x + w, y, 0)

        GL11.glVertex3d(x + w, y, 0)
        GL11.glColor4d(c_tl.x, c_tl.y, c_tl.z, c_tl.w)
        GL11.glVertex3d(x, y, 0)

        GL11.glEnd()
    }

    protected def drawRect(x: Int, y: Int, w: Int, h: Int, r: Float, g: Float, b: Float, a: Float): Unit = {
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glColor4d(r, g, b, a)
        GL11.glVertex3d(x, y, 0)
        GL11.glVertex3d(x, y + h, 0)
        GL11.glVertex3d(x + w, y + h, 0)
        GL11.glVertex3d(x + w, y, 0)
        GL11.glEnd()
    }

    protected def drawSkinnedRect(x: Int, y: Int, w: Int, h: Int, strength: Float) {

        val str = strength
        val step = 0.3f
        val shadow = 0.4f
        val outerColour = new Vector4f(0, shadow * 0.5f, shadow, 0)
        val innerColour = new Vector4f(0, shadow * 0.5f, shadow, 1)
        val bkgInnerColour = new Vector4f(0, (str - step) * 0.5f, str - step, 1)
        val bkgOuterColour = new Vector4f(0, str * 0.5f, str, 1)

        // Main panels

        val w2 = (w / 2) - borderWidth
        val h2 = (h / 2) - borderWidth
        // UL
        drawRect(x + borderWidth, y + borderWidth, w2, h2, bkgOuterColour, bkgOuterColour, bkgInnerColour, bkgOuterColour)
        // LL
        drawRect(x + borderWidth, y + (h / 2), w2, h2, bkgOuterColour, bkgOuterColour, bkgOuterColour, bkgInnerColour)
        // UR
        drawRect(x + (w / 2), y + borderWidth, w2, h2, bkgOuterColour, bkgInnerColour, bkgOuterColour, bkgOuterColour)
        // LR
        drawRect(x + (w / 2), y + (h / 2), w2, h2, bkgInnerColour, bkgOuterColour, bkgOuterColour, bkgOuterColour)

        // Edges

        GL11.glEnable(GL11.GL_BLEND)
        // top
        drawQRect(x + borderWidth, y, w - (borderWidth * 2), borderWidth, outerColour, innerColour, innerColour, outerColour)

        // left
        drawQRect(x, y + borderWidth, borderWidth, h - (borderWidth * 2), outerColour, outerColour, innerColour, innerColour)

        // bottom
        drawQRect(x + borderWidth, (y + h) - borderWidth, w - (borderWidth * 2), borderWidth, innerColour, outerColour, outerColour, innerColour)

        // right
        drawQRect((x + w) - borderWidth, y + borderWidth, borderWidth, h - (borderWidth * 2), innerColour, innerColour, outerColour, outerColour)

        // UL
        drawQRect(x, y, borderWidth, borderWidth, outerColour, outerColour, innerColour, outerColour)
        // LL
        drawQRect(x, (y + h) - borderWidth, borderWidth, borderWidth, outerColour, outerColour, outerColour, innerColour)
        // UR
        drawQRect((x + w) - borderWidth, y, borderWidth, borderWidth, outerColour, innerColour, outerColour, outerColour)
        // LR
        drawQRect((x + w) - borderWidth, (y + h) - borderWidth, borderWidth, borderWidth, innerColour, outerColour, outerColour, outerColour)
        GL11.glDisable(GL11.GL_BLEND)
    }

    def drawSubelements(ox: Int, oy: Int, scrWidth: Int, scrHeight: Int) {
        subElements foreach { ee =>
            GL11.glPushMatrix();
            GL11.glTranslated(ee.posX, ee.posY, 0);
            ee.draw(ox + ee.posX, oy + ee.posY, scrWidth, scrHeight);
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
        return subElements.reverse find { ee =>
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

    /**
     * Layout the element.
     * This cannot change the element's size.
     */
    def layout() = ()

    /**
     * Draws the element.
     * Keep in mind that scrWidth and scrHeight are NOT the size of the element!!!
     * They are the size of the root display, used for calculating some special transforms.
     * @param ox The absolute top-left X position on the screen, in pixels.
     * @param oy The absolute top-left Y position on the screen, in pixels.
     * @param scrWidth The absolute X size of the screen, in pixels.
     * @param scrHeight The absolute Y size of the screen, in pixels.
     */
    def draw(ox: Int, oy: Int, scrWidth: Int, scrHeight: Int) {
        drawSkinnedRect(0, 0, sizeWidth, sizeHeight, colourStrength)
        drawSubelements(ox, oy, scrWidth, scrHeight)
    }

    // RANDOM NOTE II : buttons[] is used for detecting dragging,
    // without embedding the mouse state into every element // TODO Wouldn't do that, its ugly

    def mouseMove(x: Int, y: Int, buttons: Array[Boolean]) {
        mouseMoveSubelements(x, y, buttons)
    }

    // RANDOM NOTE: We assume people only have 2 buttons, others must be ignored.
    // This is because not all people have middle-mouse-buttons, it's unfair to assume.

    def mouseStateChange(x: Int, y: Int, isDown: Boolean, button: Int) {
        val targetElement = findElementAt(x, y)
        if (isDown)
            selectedSubelement = targetElement
        targetElement.map { el =>
            el.mouseStateChange(x - el.posX, y - el.posY, isDown, button)
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
