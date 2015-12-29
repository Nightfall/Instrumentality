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
 * Base element of the UI framework.
 * EditElements are allowed to assume they will only have one parent per layout (NOTE: layout is called by setSize)
 * This is because, in some extreme cases, an EditElement subclass may actually remove parts not on screen until the next layout(),
 * which is called by setSize().
 * However, last time I tried adding a "parent" field to this thing, the result was incredibly messy,
 * and there were quite a few cases where reference loops did not help things.
 * There are also cases where a "decorator" pattern is useful.
 * Thus, there is no explicit "parent" field.
 * Created on 18/08/15.
 */
abstract class EditElement {
    // Note that posX and posY may be ignored if this is the root

    // No! Do not direct access to sizeWidth and sizeHeight!
    private var sizeWidth, sizeHeight: Int = _
    var posX, posY: Int = _

    val subElements = ListBuffer[EditElement]()
    var borderWidth = 6
    var shadowWidth = 4

    var colourR = 0.9f
    var colourG = 0.9f
    var colourB = 1f
    var colourStrength = 0.9f

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
        val shadow = 0.3f

        val outerColour = new Vector4f(shadow, shadow, shadow, 0)
        val innerColour = new Vector4f(shadow, shadow, shadow, 0.5f)
        val bkgInnerColour = new Vector4f((str - step) * colourR, (str - step) * colourG, (str - step) * colourB, 0.85f)
        val bkgOuterColour = new Vector4f(str * colourR, str * colourG, str * colourB, 0.85f)

        // Main panels

        val w2 = (w / 2) - shadowWidth
        val h2 = (h / 2) - shadowWidth

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        // UL
        drawRect(x + shadowWidth, y + shadowWidth, w2, h2, bkgOuterColour, bkgOuterColour, bkgInnerColour, bkgOuterColour)
        // LL
        drawRect(x + shadowWidth, y + (h / 2), w2, h2, bkgOuterColour, bkgOuterColour, bkgOuterColour, bkgInnerColour)
        // UR
        drawRect(x + (w / 2), y + shadowWidth, w2, h2, bkgOuterColour, bkgInnerColour, bkgOuterColour, bkgOuterColour)
        // LR
        drawRect(x + (w / 2), y + (h / 2), w2, h2, bkgInnerColour, bkgOuterColour, bkgOuterColour, bkgOuterColour)

        // Edges

        // top
        drawQRect(x + shadowWidth, y, w - (shadowWidth * 2), shadowWidth, outerColour, innerColour, innerColour, outerColour)

        // left
        drawQRect(x, y + shadowWidth, shadowWidth, h - (shadowWidth * 2), outerColour, outerColour, innerColour, innerColour)

        // bottom
        drawQRect(x + shadowWidth, (y + h) - shadowWidth, w - (shadowWidth * 2), shadowWidth, innerColour, outerColour, outerColour, innerColour)

        // right
        drawQRect((x + w) - shadowWidth, y + shadowWidth, shadowWidth, h - (shadowWidth * 2), innerColour, innerColour, outerColour, outerColour)

        // UL
        drawQRect(x, y, shadowWidth, shadowWidth, outerColour, outerColour, innerColour, outerColour)
        // LL
        drawQRect(x, (y + h) - shadowWidth, shadowWidth, shadowWidth, outerColour, outerColour, outerColour, innerColour)
        // UR
        drawQRect((x + w) - shadowWidth, y, shadowWidth, shadowWidth, outerColour, innerColour, outerColour, outerColour)
        // LR
        drawQRect((x + w) - shadowWidth, (y + h) - shadowWidth, shadowWidth, shadowWidth, innerColour, outerColour, outerColour, outerColour)
        GL11.glDisable(GL11.GL_BLEND)
    }

    def drawWillCull(ee: EditElement) = {
        // TODO Tuple-ception, what about using at least *some* classes somewhere?
        val rrect = UIUtils.clipRectByClippingBounds(UIUtils.widgetX + ee.posX, UIUtils.widgetY + ee.posY, ee.width, ee.height)
        ((rrect._3 <= 0) || (rrect._4 <= 0), rrect)
    }

    def drawSubelements() {
        subElements foreach { ee =>
            // MAGIC: detect if we're being clipped and don't draw if completely clipped
            val rrect = drawWillCull(ee)
            if (!rrect._1) {
                GL11.glPushMatrix()
                GL11.glTranslated(ee.posX, ee.posY, 0)
                val bounds = UIUtils.setClippingBounds(rrect._2)
                val oldWX = UIUtils.widgetX
                val oldWY = UIUtils.widgetY
                UIUtils.widgetX += ee.posX
                UIUtils.widgetY += ee.posY
                ee.draw()
                UIUtils.widgetX = oldWX
                UIUtils.widgetY = oldWY
                UIUtils.setClippingBounds(bounds)
                GL11.glPopMatrix()
            }
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
     */
    def draw() {
        drawSkinnedRect(0, 0, sizeWidth, sizeHeight, colourStrength)
        drawSubelements()
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
