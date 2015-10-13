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

import moe.nightfall.instrumentality.editor.EditElement
import org.lwjgl.util.vector.Vector4f

/**
 * Created on 12/10/15.
 */
class PoseEditTimelineElement(pe: PoseEditElement) extends EditElement {
    colourStrength = 0

    def getMultiplier = (width - (borderWidth * 2)) / pe.getEditAnim.lenFrames.toDouble

    override def draw() {
        super.draw()
        // NOTE: The borderwidth is so the timeline is confined within the border,
        //       which looks nicer since it has a shadow.
        val multiplier = getMultiplier
        for (i <- 0 until pe.getEditAnim.lenFrames) {
            val selectedOfs = if (pe.editingData._1 == i) 0.5f else 0.0f
            val ncol = new Vector4f(0.1f, 0.1f, 0.1f + selectedOfs, 1)
            var col = new Vector4f(0.2f, 0.2f, 0.2f + selectedOfs, 1)
            if (pe.getEditAnim.frameMap.get(i).isDefined)
                col = new Vector4f(0.5f, 0.5f, 0.25f + selectedOfs, 1)
            val start = Math.floor(i * multiplier).toInt
            val end = Math.ceil((i + 1) * multiplier).toInt
            val wid = end - start
            drawQRect(start + borderWidth, borderWidth, wid / 2, height - (borderWidth * 2), ncol, ncol, col, col)
            drawQRect(start + (wid / 2) + borderWidth, borderWidth, wid / 2, height - (borderWidth * 2), col, col, ncol, ncol)
        }
    }

    override def mouseStateChange(x: Int, y: Int, isDown: Boolean, button: Int): Unit = {
        if (isDown) {

        }
    }
}
