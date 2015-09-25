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
package moe.nightfall.instrumentality

import java.awt.image.BufferedImage

import moe.nightfall.instrumentality.shader.{Shader, ShaderManager}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

import scala.collection.mutable.ListBuffer

object Loader {

    // TODO Move
    val groupSize = 12

    // TODO This is not convenient
    // This is the current model name for the player.
    // To be notified when this changes, put yourself on the list below :)
    var currentFile: String = _
    var currentFileListeners = ListBuffer[() => Unit]()

    var shaderBoneTransform: Shader = _

    def setup() {
        loadModel()
        loadShaders()
    }

    def loadShaders() = ShaderManager.loadShaders()

    def loadModel() {
        shaderBoneTransform = ShaderManager.createProgram("/assets/instrumentality/shader/bone_transform.vert",
            "/assets/instrumentality/shader/bone_transform.frag").set("groupSize", groupSize)
    }

    def setCurrentFile(workModelName: String) {
        currentFile = workModelName
        currentFileListeners.foreach(_())
    }

    // No better idea where to put this
    def writeGLTexImg(bi: BufferedImage, filter: Int) {
        val ib = new Array[Int](bi.getWidth() * bi.getHeight())
        bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), ib, 0, bi.getWidth())
        val inb = BufferUtils.createByteBuffer(bi.getWidth() * bi.getHeight() * 4)
        for (i <- 0 until (bi.getWidth() * bi.getHeight())) {
            val c = ib(i)
            inb put ((c & 0xFF0000) >> 16).toByte
            inb put ((c & 0xFF00) >> 8).toByte
            inb put (c & 0xFF).toByte
            inb put ((c & 0xFF000000) >> 24).toByte
        }
        inb.rewind()
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, bi.getWidth(), bi.getHeight(), 0, GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE, inb)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter)
    }
}
