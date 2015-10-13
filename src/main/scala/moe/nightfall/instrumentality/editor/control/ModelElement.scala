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

import moe.nightfall.instrumentality.{Loader, ModelCache, PMXInstance, PMXModel}
import moe.nightfall.instrumentality.animations.NewPCAAnimation
import moe.nightfall.instrumentality.editor.UIUtils
import org.lwjgl.opengl.GL11

class ModelElement(ib: Boolean) extends View3DElement {
    var workModel: PMXInstance = null
    var workModelName: String = null

    var isButton: Boolean = ib
    var isHover: Boolean = false

    if (!isButton) {
        setModel(Loader.currentFile)
        Loader.currentFileListeners += (() => {
            setModel(Loader.currentFile)
        })
    }

    def setModel(modelName: String) {
        if (workModel != null) {
            workModel.cleanupGL()
            workModel = null
        }
        workModelName = modelName
        if (modelName == null) return
        val b: PMXModel = ModelCache.getLocal(modelName)
        if (b == null) return
        workModel = ModelElement.makeTestInstance(b)
    }

    override def draw() {
        if (isButton) {
            if (workModelName == null) {
                colourStrength = 0.6f
                colourStrength *= (if (isHover) 1.4f else 1f)
            }
            else {
                colourStrength = 0.9f
                if (workModelName.equalsIgnoreCase(Loader.currentFile)) colourStrength = 0.8f
                colourStrength *= (if (isHover) 1.1f else 1f)
            }
        }
        super.draw()
        if (isButton) {
            var text: String = "<null>"
            if (workModelName != null) text = workModelName
            UIUtils.drawBoundedText(text, width, height, borderWidth * 2)
        }
    }

    protected def draw3D() {
        if (workModel != null) {
            val sFactor: Float = 1.0f / workModel.theModel.height
            GL11.glTranslated(0, -0.5f, 0)
            GL11.glScaled(sFactor, sFactor, sFactor)
            workModel.render(Loader.shaderBoneTransform, 1, 1, 1, workModel.theModel.height + 1.0f)
        } else {
            GL11.glTranslated(-0.25, 0, 0)
            GL11.glScaled(0.01, -0.01, 0.01)
            var text = "Load Fail"
            if (workModelName == null)
                text = "Use Steve"
            UIUtils.drawText(text)
        }
    }

    override def mouseStateChange(x: Int, y: Int, isDown: Boolean, button: Int) {
        super.mouseStateChange(x, y, isDown, button)
        if (isButton)
            if (isDown)
                if (button == 0)
                    Loader.setCurrentFile(workModelName)
    }

    override def update(dTime: Double) {
        super.update(dTime)
        if (workModel != null) workModel.update(dTime)
    }

    override def mouseEnterLeave(isInside: Boolean) {
        super.mouseEnterLeave(isInside)
        isHover = isInside
    }

    override def cleanup() {
        super.cleanup()
        if (workModel != null) {
            workModel.cleanupGL()
            workModel = null
        }
    }
}

object ModelElement {
    def makeTestInstance(pm: PMXModel): PMXInstance = {
        val pi: PMXInstance = new PMXInstance(pm)
        val npca = new NewPCAAnimation(pm.anims)
        npca.walkStrength = 1
        npca.walkSpeed = 1
        pi.anim = npca
        /*return*/ pi
    }
}
