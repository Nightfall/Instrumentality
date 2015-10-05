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

import moe.nightfall.instrumentality.editor.{EditElement, SelfResizable}
import org.lwjgl.opengl.GL11

class ScrollAreaElement(val child: EditElement, var scrollStepX: Int, var scrollStepY: Int) extends EditElement {

    var scrollAvailableX = false
    var scrollAvailableY = false

    def this(child: EditElement, scrollStep: Int) = this(child, scrollStep, scrollStep)

    def this(child: EditElement) = this(child, 64, 64)

    colourStrength = 0.25f

    val buttonSize = 32
    private var scrollX = 0
    private var scrollY = 0

    private val upButton = new ArrowButtonElement(-90, {
        scrollY += scrollStepY;
        layout
    })
    private val downButton = new ArrowButtonElement(+90, {
        scrollY -= scrollStepY;
        layout
    })

    private val leftButton = new ArrowButtonElement(180, {
        scrollX += scrollStepX;
        layout
    })
    private val rightButton = new ArrowButtonElement(0, {
        scrollX -= scrollStepX;
        layout
    })

    // glScissor doesn't stack AFAIK,
    // and worse, it means the child has to be treated specially,
    // while still remaining in the subelements list to avoid reimplementing
    // the mouse functions.
    // In the end, it's not worth using it specifically on the child.

    override def draw(ox: Int, oy: Int, scrWidth: Int, scrHeight: Int) {
        GL11.glScissor(ox, scrHeight - (oy + height), width, height)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        super.draw(ox, oy, scrWidth, scrHeight)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    override def update(dT: Double) = {
        super.update(dT)
        if (child.isInstanceOf[SelfResizable])
            if (child.asInstanceOf[SelfResizable].hasSuggestionChanged)
                layout()
    }

    override def layout() {
        super.layout()

        val removeX = if (scrollAvailableY) buttonSize else 0
        val removeY = if (scrollAvailableX) buttonSize else 0

        upButton.posX = width - buttonSize
        upButton.posY = 0
        downButton.posX = width - buttonSize
        downButton.posY = height - (buttonSize + removeY)

        leftButton.posX = 0
        leftButton.posY = height - buttonSize
        rightButton.posX = width - (buttonSize + removeX)
        rightButton.posY = height - buttonSize

        upButton.setSize(buttonSize, buttonSize)
        downButton.setSize(buttonSize, buttonSize)
        leftButton.setSize(buttonSize, buttonSize)
        rightButton.setSize(buttonSize, buttonSize)

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

        scrollAvailableX = child.width > width
        scrollAvailableY = child.height > height

        subElements.clear()
        subElements += child
        if (scrollAvailableX)
            subElements += leftButton += rightButton
        else
            scrollX = 0
        if (scrollAvailableY)
            subElements += upButton += downButton
        else
            scrollY = 0

        child.posX = scrollX
        child.posY = scrollY
    }
}