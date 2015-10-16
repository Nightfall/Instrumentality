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
package moe.nightfall.instrumentality.editor.gui

import moe.nightfall.instrumentality.animations.PoseAnimation
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.{LabelElement, CheckboxElement, TextButtonElement}
import org.lwjgl.util.vector.Vector4f

/**
 * Created on 12/10/15.
 */
class PoseEditTimelineElement(pe: PoseEditElement) extends EditElement {
    colourStrength = 0

    var hiddenClipboard = new PoseAnimation()

    val deleteButton = new TextButtonElement("Delete", {
        pe.getEditAnim.frameMap -= pe.editingFrame
        pe.resetFrame
    })

    val copyButton = new TextButtonElement("Copy", {
        hiddenClipboard = new PoseAnimation(pe.editingData._1)
    })

    val pasteButton = new TextButtonElement("Paste", {
        pe.getEditAnim.frameMap += pe.editingFrame -> hiddenClipboard
        pe.resetFrame
    })

    val playCheckbox = new CheckboxElement()
    val playLabel = new LabelElement("Play (don't edit when on!)")

    subElements += deleteButton
    subElements += copyButton
    subElements += pasteButton
    subElements += playCheckbox
    subElements += playLabel

    def getMultiplier = (width - (borderWidth * 2)) / pe.getEditAnim.lenFrames.toDouble

    def getTimelineHeight = height / 2

    var time = 0.0d

    override def draw() {
        super.draw()
        // NOTE: The borderwidth is so the timeline is confined within the border,
        //       which looks nicer since it has a shadow.
        val multiplier = getMultiplier
        val timelineHeight = getTimelineHeight
        for (i <- 0 until pe.getEditAnim.lenFrames) {
            val selectedOfs = if (pe.editingFrame == i) 0.5f else 0.0f
            val ncol = new Vector4f(0.1f, 0.1f, 0.1f + selectedOfs, 1)
            var col = new Vector4f(0.2f, 0.2f, 0.2f + selectedOfs, 1)
            if (pe.getEditAnim.frameMap.get(i).isDefined)
                col = new Vector4f(0.5f, 0.5f, 0.25f + selectedOfs, 1)
            val start = Math.floor(i * multiplier).toInt
            val end = Math.ceil((i + 1) * multiplier).toInt
            val wid = end - start
            drawQRect(start + borderWidth, borderWidth, wid / 2, timelineHeight, ncol, ncol, col, col)
            drawQRect(start + (wid / 2) + borderWidth, borderWidth, wid / 2, timelineHeight, col, col, ncol, ncol)
        }
    }

    override def update(dT: Double) {
        time += dT
        if (time > 0.05d) {
            time -= 0.05d
            if (playCheckbox.checked) {
                pe.editingFrame += 1
                if (pe.editingFrame == pe.getEditAnim.lenFrames)
                    pe.editingFrame = 0
                pe.resetFrame
            }
        }
    }

    override def mouseStateChange(x: Int, y: Int, isDown: Boolean, button: Int) {
        if (y >= (getTimelineHeight + borderWidth)) {
            super.mouseStateChange(x, y, isDown, button)
            return
        }
        if (isDown) {
            val frame = Math.floor((x - borderWidth) / getMultiplier).toInt
            if (frame < 0)
                return
            if (frame >= pe.getEditAnim.lenFrames)
                return
            if (button == 0) {
                pe.editingFrame = frame
                pe.resetFrame
            }
        }
    }

    override def mouseMove(x: Int, y: Int, buttons: Array[Boolean]) {
        if (y >= (getTimelineHeight + borderWidth)) {
            super.mouseMove(x, y, buttons)
            return
        } else {
            super.mouseMove(x, y, Array(false, false))
        }
        val frame = Math.floor((x - borderWidth) / getMultiplier).toInt
        if (frame < 0)
            return
        if (frame >= pe.getEditAnim.lenFrames)
            return
        if (buttons(0)) {
            pe.editingFrame = frame
            pe.resetFrame
        }
    }

    override def layout() {
        super.layout()
        val quarter = (width - (borderWidth * 2)) / 4
        val buttonbarY = getTimelineHeight + borderWidth
        val remainingHeight = height - (buttonbarY + borderWidth)
        val buttonbarY2 = buttonbarY + (remainingHeight / 2)
        deleteButton.posX = borderWidth
        deleteButton.posY = buttonbarY
        deleteButton.setSize(quarter, remainingHeight / 2)

        copyButton.posX = borderWidth + quarter
        copyButton.posY = buttonbarY
        copyButton.setSize(quarter, remainingHeight / 2)

        pasteButton.posX = borderWidth + (quarter * 2)
        pasteButton.posY = buttonbarY
        pasteButton.setSize(quarter, remainingHeight / 2)

        playCheckbox.posX = borderWidth
        playCheckbox.posY = buttonbarY2
        playCheckbox.setSize(quarter / 2, remainingHeight / 2)

        playLabel.posX = borderWidth + (quarter / 2)
        playLabel.posY = buttonbarY2
        playLabel.setSize(quarter + (quarter / 2), remainingHeight / 2)
    }
}
