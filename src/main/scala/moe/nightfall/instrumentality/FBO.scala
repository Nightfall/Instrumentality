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
// Inspired by http://wiki.lwjgl.org/wiki/Using_Frame_Buffer_Objects_(FBO)
package moe.nightfall.instrumentality

import org.lwjgl.opengl.GLContext
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.lwjgl.opengl.GL11
import scala.collection.mutable.Stack
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.util.glu.GLU

object FBO {
    lazy val supported = GLContext.getCapabilities.OpenGL30
    
    def ensure() = {
        if(!supported) throw new UnsupportedOperationException("FBO unsupported!")
    }
    
    def create(width: Int, height: Int): FBO = {
        ensure()
        new FBO(width, height)
    }
    
    private val stack = Stack[Int]()
    
    def bind(fbo: FBO): Unit = bind(fbo.id)
    
    def bind(fbo: Int) {
        ensure()
        val current = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)
        if (current != 0) stack.push(current)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
    }
    
    def unbind() {
        ensure()
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, if (stack.size > 0) stack.pop() else 0)
    }
}

class FBO private (private var _width: Int, private var _height: Int) {
    val id = GL30.glGenFramebuffers()
    private var _texture = 0
    
    bind()
    genTexture()
    checkComplete()
    unbind()
    
    def texture = _texture
    
    def bind() = FBO.bind(id)
    def unbind() = FBO.unbind()
    
    def width = _width
    def height = _height
    
    def genTexture() {
        if (this.texture != 0) GL11.glDeleteTextures(this.texture)
        val texture = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null.asInstanceOf[ByteBuffer])
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)        
        setTexture(texture)
    }
    
    def genTexture(width: Int, height: Int) {
        _width = width
        _height = height
        genTexture()
    }
    
    def setTexture(texture: Int) {
        _texture = texture
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0)
    }
    
    def dispose() {
        GL30.glDeleteFramebuffers(id)
        GL11.glDeleteTextures(texture)
    }
    
    def bindTexture() = GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
    def unbindTexture() = GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    
    def checkComplete() = {
        val framebuffer = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
        framebuffer match {
            case GL30.GL_FRAMEBUFFER_COMPLETE =>
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT =>
                throw new RuntimeException(s"FrameBuffer $id, has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT exception")
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT =>
                throw new RuntimeException(s"FrameBuffer $id has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT exception")
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER =>
                throw new RuntimeException(s"FrameBuffer $id has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER exception")
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER =>
                throw new RuntimeException(s"FrameBuffer $id has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER exception" );
            case _ => throw new RuntimeException(s"Unexpected reply from glCheckFramebufferStatusEXT: $id");
        }
    }
}