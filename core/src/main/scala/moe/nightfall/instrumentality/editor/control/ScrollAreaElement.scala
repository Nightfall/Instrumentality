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

import moe.nightfall.instrumentality.editor.{UIUtils, EditElement, SelfResizable}
import org.lwjgl.opengl.GL11

// TODO mouse wheel support
class ScrollAreaElement(val child: EditElement, var scrollStepX: Int, var scrollStepY: Int) extends EditElement {

    var scrollAvailableX = false
    var scrollAvailableY = false

    def this(child: EditElement, scrollStep: Int) = this(child, scrollStep, scrollStep)

    def this(child: EditElement) = this(child, 64, 64)

    colourStrength = 0.8f

    val buttonSize = 32
    var scrollX = 0
    var scrollY = 0
    
    val vertScrollBar = new ScrollbarElement.Vertical(scrollStepX)    
    vertScrollBar.onScroll = () => {
        scrollX = -vertScrollBar.barPosition
        layout()
    }
    
    val horScrollBar = new ScrollbarElement.Horizontal(scrollStepY)
    horScrollBar.onScroll = () => {
        scrollY = -horScrollBar.barPosition
        layout()
    }

    override def update(dT: Double) = {
        // TODO It looks like this doesn't always work
        super.update(dT)
        if (child.isInstanceOf[SelfResizable])
            if (child.asInstanceOf[SelfResizable].hasSuggestionChanged)
                layout()
    }

    override def layout() {
        super.layout()
        
        scrollAvailableX = child.width > width
        scrollAvailableY = child.height > height

        val removeX = if (scrollAvailableY) buttonSize else 0
        val removeY = if (scrollAvailableX) buttonSize else 0
       
        vertScrollBar.posY = height - buttonSize
        vertScrollBar.setSize(width - removeX, buttonSize)
        vertScrollBar.setSize(child.width - width)
        vertScrollBar.setBarSize(32)
        vertScrollBar.setBarSize(math.max((width / child.width.toFloat) * (width - buttonSize * 2 - removeX), 32).toInt)
        
        horScrollBar.posX = width - buttonSize
        horScrollBar.setSize(buttonSize, height - removeY)
        horScrollBar.setSize(child.height - height)
        horScrollBar.setBarSize(math.max((height / child.height.toFloat) * (height - buttonSize * 2 - removeY), 32).toInt)
        
        // Set child to our size so that the child has some knowledge of the container
        child.setSize(width - buttonSize, height - buttonSize)

        if (child.isInstanceOf[SelfResizable]) {
            val size = child.asInstanceOf[SelfResizable].getSuggestedSize
            child.setSize(
                size._1,
                size._2
            )
            child.asInstanceOf[SelfResizable].clearSuggestionChanged
        } else {
            child.setSize(
                width * 2,
                height * 2
            )
        }

        subElements.clear()
        subElements += child
        if (scrollAvailableX)
            subElements += vertScrollBar
        else
            scrollX = 0
        if (scrollAvailableY)
            subElements += horScrollBar
        else
            scrollY = 0

        child.posX = scrollX
        child.posY = scrollY
    }
    
    layout()
}