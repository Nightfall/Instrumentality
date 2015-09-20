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
import moe.nightfall.instrumentality.editor.controls.ModelElement
import moe.nightfall.instrumentality.editor.control.ArrowButtonElement

/**
 * Created on 25/08/15, ported to Scala on 2015-09-20
 */
class ModelChooserElement(models: Seq[String]) extends EditElement {
	// TODO: Remove this when code is ported to Scala.
	implicit def funToRunnable(fun: () => Unit) = new Runnable() { def run() = fun() }
	
	private var group = Array.fill[ModelElement](3)(new ModelElement(true))
	subElements ++= group
	
	private var ptrStart: Int = 0
	private var buttonbar = Array[EditElement](
		new ArrowButtonElement(180, () => {
			ptrStart -= 1
			updatePosition()
		}),
		new ArrowButtonElement(0, () => {
			ptrStart += 1
			updatePosition()
		}),
		new ModelElement(false)
	)
	subElements ++= buttonbar
	updatePosition()
	
	private val availableModels = models.toArray
	colourStrength = 0.5f
	
	override def layout() = {
		val x = width / group.length
		val y = height / 4
		
		for((element, index) <- group.view.zipWithIndex) {
			element.posX = x * index
			element.posY = y
			element.setSize(x, y * 3)
		}
		
		for((button, index) <- group.view.zipWithIndex) {
			button.posX = y * index
			button.posY = 0
			button.setSize(y, y)
		}
	}
	
	def updatePosition() = {
		for((element, index) <- group.view.zipWithIndex) {
			element.setModel(availableModels((index + ptrStart) % availableModels.length))
		}
	}
}
