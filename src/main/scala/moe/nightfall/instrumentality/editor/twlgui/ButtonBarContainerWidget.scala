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
package moe.nightfall.instrumentality.editor.twlgui

import de.matthiasmann.twl.{Alignment, Container, BoxLayout, Widget}

/**
 * Created on 16/10/15.
 */
class ButtonBarContainerWidget(sizeRatio: Double) extends Container {
    val barCore: BoxLayout = new BoxLayout()
    barCore.setAlignment(Alignment.FILL)
    add(barCore)
    private var underPanel: Widget = null
    // Sometimes this needs to be overridden.
    var noCleanupOnChange: Boolean = false

    def setUnderPanel(editElement: Widget, noCleanup: Boolean) {
        if (underPanel != null) {
            if (!noCleanupOnChange)
                underPanel.destroy()
            removeChild(underPanel)
        }
        noCleanupOnChange = noCleanup
        underPanel = editElement
        add(underPanel)
        layout()
    }

    override def layout() = {
        val point = (getInnerHeight * sizeRatio).toInt
        barCore.setPosition(getInnerX, getInnerY)
        barCore.setSize(getInnerWidth, point)
        if (underPanel != null) {
            underPanel.setPosition(getInnerX, point + getInnerY)
            underPanel.setSize(getInnerWidth, getInnerHeight - point)
            underPanel.setVisible(true)
        }
    }
}
