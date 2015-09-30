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
import org.lwjgl.opengl.GL11
import moe.nightfall.instrumentality.editor.control.ArrowButtonElement

class ScrollAreaElement(val child : EditElement, var scrollStepX : Int, var scrollStepY : Int) extends EditElement {
    
    def this(child : EditElement, scrollStep : Int) = this(child, scrollStep, scrollStep)
    def this(child : EditElement) = this(child, 10, 10)
    
    var scrollX, scrollY = true
    private var scroll = 0
    
    private val upButton = new ArrowButtonElement(-90, scroll += scrollStepX)
    private val downButton = new ArrowButtonElement(+90, scroll -= scrollStepY)
    
    subElements += upButton += downButton += child
    
    override def draw(srcWidth : Int, srcHeight : Int) {
        if (srcWidth != width || srcHeight != height) {
            setSize(srcWidth, srcHeight)
            layout()
        }
        super.draw(srcWidth, srcHeight)
        GL11.glScissor(0, 0, srcWidth - upButton.width, srcHeight)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        child.draw(child.width - upButton.width, child.height)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    override def layout() {
        child.layout()
        child.setSize(
           if (scrollX) child.width else width - upButton.width,
           if (scrollY) child.height else height
        )
    }
}