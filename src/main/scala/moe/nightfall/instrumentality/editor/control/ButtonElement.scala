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
import org.lwjgl.input.Mouse

class ButtonElement(toRun : => Unit) extends EditElement {

    // So this can be changed...
    var onClick = () => toRun

    var isHover = false
    var baseStrength = 0.9f

    override def mouseStateChange(x: Int, y: Int, isDown: Boolean, button: Int) {
        super.mouseStateChange(x, y, isDown, button)
        if ((button == 0) && (!isDown) && (onClick != null))
            onClick()
    }

    override def draw() {
        if (isHover && Mouse.isButtonDown(0))
            colourStrength = baseStrength - (baseStrength / 8)
        else
            colourStrength = baseStrength * (if (isHover) 1.1f else 1.0f)

        super.draw()
    }

    override def mouseEnterLeave(isIn: Boolean) {
        isHover = isIn
    }
}
