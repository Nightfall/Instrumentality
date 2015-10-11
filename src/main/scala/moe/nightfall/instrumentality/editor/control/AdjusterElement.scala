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

class AdjusterElement(valuePrefix: String, toAdjust: AdjusterElementData) extends EditElement {
    val adjustmentValue = 0.05d
    val valueDisplay = new LabelElement("?")

    val incButton = new ArrowButtonElement(0,
        toAdjust.value = correct(toAdjust.value + adjustmentValue)
    )
    val decButton = new ArrowButtonElement(180,
        toAdjust.value = correct(toAdjust.value - adjustmentValue)
    )

    subElements ++= Array(incButton, decButton, valueDisplay)

    /**
     * Keeps the value from succumbing to rounding errors
     *
     * @param v The value to correct
     * @return The value, rounded to the nearest adjustmentvalue
     */
    private def correct(v: Double): Double = ((v * (1 / adjustmentValue)).round) / (1 / adjustmentValue)

    override def draw(ox: Int, oy: Int, scrWidth: Int, scrHeight: Int) {
        valueDisplay.text = valuePrefix + toAdjust.value
        super.draw(ox, oy, scrWidth, scrHeight)
    }

    override def layout() {
        super.layout()
        val du = width / 8
        // dec/inc buttons are inset by half of the border's size.
        // Thus, where I write "du - borderWidth", that's because I *would* write borderWidth * 2,
        // but it's *half* of the border's size, so it's just borderWidth
        decButton.posX = borderWidth
        decButton.posY = borderWidth
        decButton.setSize(du, height - (borderWidth * 2))
        incButton.posX = width - (du + borderWidth)
        incButton.posY = borderWidth
        incButton.setSize(du, height - (borderWidth * 2))
        valueDisplay.posX = du
        valueDisplay.posY = borderWidth
        valueDisplay.setSize(width - (du * 2), height - (borderWidth * 2))
    }

}

trait AdjusterElementData {
    def value: Double

    def value_=(newValue: Double): Unit
}
