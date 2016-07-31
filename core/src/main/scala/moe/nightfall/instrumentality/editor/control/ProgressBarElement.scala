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

/**
 * Created on 11/10/15.
 */
class ProgressBarElement extends EditElement {
    var progressValue = 0.0d
    var interpolatedProgressValue = 0.0d
    colourStrength = 0.0f

    override def update(dt: Double) {
        super.update(dt)
        if (progressValue > interpolatedProgressValue) {
            interpolatedProgressValue += dt
            if (interpolatedProgressValue > progressValue)
                interpolatedProgressValue = progressValue
        } else {
            interpolatedProgressValue -= dt
            if (interpolatedProgressValue < progressValue)
                interpolatedProgressValue = progressValue
        }
    }

    override def draw() {
        super.draw()
        val areaWidth = width - (borderWidth * 2)
        val progressPoint = (areaWidth * interpolatedProgressValue).toInt
        drawSkinnedRect(borderWidth, borderWidth, progressPoint, height - (borderWidth * 2), 1)
        drawSkinnedRect(borderWidth + progressPoint, borderWidth, areaWidth - progressPoint, height - (borderWidth * 2), 0.5f)
    }
}
