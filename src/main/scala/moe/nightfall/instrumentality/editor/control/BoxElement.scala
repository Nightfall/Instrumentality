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
 * See: GTK+ v3's horizontal Box for a general idea of what this is supposed to be used for.
 * It's just less flexible.
 * Created on 01/09/15.
 */
class BoxElement(val vertical: Boolean) extends EditElement {
    private var barPieces = scala.collection.mutable.ListBuffer[EditElement]()

    def +=(editElement: EditElement) {
        barPieces += editElement
        subElements += editElement
    }

    def -=(editElement: EditElement) {
        if (barPieces.contains(editElement))
            barPieces.remove(barPieces.indexOf(editElement))
        if (subElements.contains(editElement)) {
            subElements.remove(subElements.indexOf(editElement))
            editElement.cleanup()
        }
    }

    override def layout() {
        if (barPieces.nonEmpty) {
            val div = (if (vertical) height else width) / barPieces.size
            val leftover = (if (vertical) height else width) - (div * barPieces.size)

            var first = true
            var pos = 0
            for (button <- barPieces) {
                var ds = div
                if (first) {
                    ds += leftover
                    first = false
                }

                button.posX = if (vertical) 0 else pos
                button.posY = if (vertical) pos else 0

                pos += ds
                button.setSize(if (vertical) width else ds, if (vertical) ds else height)
            }
        }
    }
}
