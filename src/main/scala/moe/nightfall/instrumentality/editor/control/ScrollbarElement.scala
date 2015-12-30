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

abstract class ScrollbarElement(var scrollStep: Int) extends EditElement {    
    protected var _barSize, _size, _pos = 0
    
    def size = _size
    def barSize = _barSize
    def barPosition = _pos
    
    def setSize(size: Int) = {
        val ratio = (_pos / _size.toFloat)
        _size = size
        _pos = (ratio * size).toInt
        updateKnobPosition()
    }
    
    override def setSize(width: Int, height: Int) {
        super.setSize(width, height)
        updateKnobPosition()
    }
    
    def setBarSize(size: Int) = _barSize = size
    
    def setBarPosition(pos: Int) = {
        _pos = pos
        _pos = math.min(_pos, size)
        _pos = math.max(_pos, 0)
        updateKnobPosition()
        onScroll()
    }
    
    override def draw() = drawSubelements()
    
    protected var isHover = false
    
    var onScroll: () => Unit = _
    
    // TODO Keep scrolling when the cursor moves out of the element, mouseMove is only called
    // when the cursor is inside the boundaries
    protected val knob = new ButtonElement() {
        override def mouseMove(x: Int, y: Int, buttons: Array[Boolean]) {
            if (buttons(0)) {
                move(Mouse.getDX, Mouse.getDY)
            }
        }
    }
    
    protected def move(x: Int, y: Int)
    protected def updateKnobPosition()
    protected def updatePosition()
}

object ScrollbarElement {

    class Vertical(scrollStep: Int) extends ScrollbarElement(scrollStep) {
        
        private val leftButton = new ArrowButtonElement(180, {
            setBarPosition(_pos - scrollStep)
        })
        private val rightButton = new ArrowButtonElement(0, {
            setBarPosition(_pos + scrollStep)
        })
        
        private def knobMax = width - barSize - height
        
        override def updateKnobPosition() {
            knob.posX = ((_pos / size.toFloat) * (knobMax - height) + height).toInt
        }
        override def updatePosition() {
            _pos = ((knob.posX - height) / (knobMax - height).toFloat * size).toInt
        }
        
        override def layout() {
            subElements.clear()
            leftButton.setSize(height, height)
            rightButton.setSize(height, height)
            rightButton.posX = width - height
            knob.posX = height
            subElements += leftButton += rightButton += knob
        }

        override def setBarSize(size: Int) {
            super.setBarSize(size)
            knob.setSize(barSize, height)
            updateKnobPosition()
        }
        
        override def move(x: Int, y: Int) {
            if (x == 0) return
            knob.posX += x
            knob.posX = math.min(knob.posX, knobMax)
            knob.posX = math.max(knob.posX, height)
            updatePosition()
            
            onScroll()
        }
        
        layout()
    }
    
    class Horizontal(scrollStep: Int) extends ScrollbarElement(scrollStep) {

        private val upButton = new ArrowButtonElement(-90, {
            setBarPosition(_pos - scrollStep)
        })
        private val downButton = new ArrowButtonElement(+90, {
            setBarPosition(_pos + scrollStep)
        })
        
        private def knobMax = height - barSize - width
        
        override def updateKnobPosition() {
            knob.posY = ((_pos / size.toFloat) * (knobMax - width) + width).toInt
        }
        override def updatePosition() {
            _pos = ((knob.posY - width) / (knobMax - width).toFloat * size).toInt
        }
        
        override def layout() {
            upButton.setSize(width, width)
            downButton.setSize(width, width)
            downButton.posY = height - width
            subElements += upButton += downButton += knob
        }
        
        override def setBarSize(size: Int) {
            super.setBarSize(size)
            knob.setSize(width, barSize)
            updateKnobPosition()
        }
        
        override def move(x: Int, y: Int) {
            if (y == 0) return
            knob.posY -= y
            knob.posY = math.min(knob.posY, knobMax)
            knob.posY = math.max(knob.posY, width)
            updatePosition()
            
            onScroll()
        }
        
        layout()
    }
}