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
package moe.nightfall.instrumentality.editor.gui;

import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.UIUtils;
import moe.nightfall.instrumentality.editor.controls.ModelElement;
import org.lwjgl.opengl.GL11;

/**
 * Created on 01/09/15.
 */
class BenchmarkElement(val myModel : PMXModel) extends EditElement {
    var time : Double = _
    var avgTime : Double = _

    override def update(dTime : Double) {
        super.update(dTime)
        avgTime = ((avgTime * 99) + dTime) / 100
        time += dTime
        if (time > 2.0d) {
            val me = new ModelElement(false)
            me.setSize(200, 320)
            subElements += me
            layout()
            time -= 2.0d
        }
    }

    override def layout() {
        var i = 0
        for (me <- subElements) {
            me.posX = i * 50
            me.posY = (me.posX / width) * 50
            me.posX %= width
            me.posY %= height
            i += 1
        }
    }

    override def draw(scrWidth : Int, scrHeight : Int) {
        super.draw(scrWidth, scrHeight)
        GL11.glPushMatrix()
        GL11.glTranslated(1, 1, 0)
        GL11.glScaled(2, 2, 2)
        GL11.glColor3d(1, 1, 1)

        val str = subElements.size + " elems/" + ((((1 / avgTime) * 10).toInt) / 10d) + " FPS"
        UIUtils.drawText(str, 3)

        GL11.glPopMatrix()
    }
}
