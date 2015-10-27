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

class PowerlineContainerElement(sizeRatio: Double, getHome: (PowerlineContainerElement) => EditElement) extends EditElement {

    val barCore: BoxElement = new BoxElement(false)
    subElements += barCore
    private var underPanel: EditElement = null
    // Sometimes this needs to be overridden.
    var noCleanupOnChange: Boolean = false
    private val powerlineBobs = scala.collection.mutable.ListBuffer[(EditElement, () => Unit)]()

    def addThing(button: EditElement, onDelete: () => Unit) {
        val pl = new PowerlineArrowElement(button)
        barCore += pl
        // o...kay? (These parenthesis are not useless!)
        powerlineBobs += ((pl, onDelete))
    }

    def addAndGo(s: String, l: EditElement) {
        val ind = powerlineBobs.length + 1
        addThing(new TextButtonElement(s, {
            removeFromIndex(ind)
            setUnderPanel(l, true)
        }), () => {
            l.cleanup()
        })
        setUnderPanel(l, true)
    }

    def setUnderPanel(editElement: EditElement, noCleanup: Boolean) {
        if (underPanel != null) {
            if (!noCleanupOnChange)
                underPanel.cleanup()
            subElements -= underPanel
        }
        noCleanupOnChange = noCleanup
        underPanel = editElement
        subElements += underPanel

        layout()
    }

    def removeFromIndex(i: Int) {
        while (powerlineBobs.length > i) {
            barCore -= powerlineBobs(i)._1
            powerlineBobs(i)._2()
            powerlineBobs.remove(i)
        }
    }

    barCore += new TextButtonElement("Home", {
        removeFromIndex(0)
        setUnderPanel(getHome(this), false)
    })
    setUnderPanel(getHome(this), false)

    override def layout() {
        val size: Int = (height * sizeRatio).asInstanceOf[Int]
        if (underPanel != null) {
            underPanel.posX = 0
            underPanel.posY = size
            underPanel.setSize(width, height - size)
        }
        barCore.setSize(width, size)
        barCore.posX = 0
        barCore.posY = 0
    }

    class PowerlineArrowElement(val b: EditElement) extends EditElement {
        subElements += b

        override def draw() {
            drawSubelements()
            GL11.glLineWidth(1)
            GL11.glColor3f(0, 0, 0)
            GL11.glBegin(GL11.GL_LINE_STRIP)
            // It doesn't look right if we respect the border
            GL11.glVertex2d(0, shadowWidth)
            GL11.glVertex2d(height / 4, height / 2)
            GL11.glVertex2d(0, height - shadowWidth)
            GL11.glEnd()
        }

        override def layout() {
            b.posX = height / 4
            b.posY = 0
            b.setSize(width - (height / 4), height)
        }
    }

}
