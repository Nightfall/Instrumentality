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
package moe.nightfall.instrumentality.shader;

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.LinkedList
import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern;
import scala.collection.mutable.MutableList

/**
 * See http://lwjgl.org/wiki/index.php?title=GLSL_Shaders_with_LWJGL
 */
object ShaderManager {

    private var shaders = new MutableList[Shader]()

    def loadShaders() {
        println("Loading shaders, GLSL version supported: " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION))
        for (shader <- shaders) {
            shader.program = compileProgram(shader)
            if (shader.vertexShader != null) {
                println("Loaded vertex shader: \"" + shader.vertexShader + "\"")
            }
            if (shader.fragmentShader != null) {
                println("Loaded fragment shader: \"" + shader.fragmentShader +
                    "\"")
            }
        }
    }

    def createProgram(vertexShader: String, fragmentShader: String): Shader = {
        val shader = new Shader(vertexShader, fragmentShader)
        shaders += shader
        return shader
    }

    private def compileProgram(shader: Shader): Int = {
        var vertShader = 0
        var fragShader = 0
        if (shader.vertexShader != null) vertShader = createShader(shader.variables, shader.vertexShader,
            GL20.GL_VERTEX_SHADER)
        if (shader.fragmentShader != null) fragShader = createShader(shader.variables, shader.fragmentShader,
            GL20.GL_FRAGMENT_SHADER)
        val program = GL20.glCreateProgram()
        if (program == 0) return 0
        if (vertShader != 0) GL20.glAttachShader(program, vertShader)
        if (fragShader != 0) GL20.glAttachShader(program, fragShader)
        GL20.glLinkProgram(program)
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            sys.error(GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH)))
            return 0
        }
        GL20.glValidateProgram(program)
        if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) ==
            GL11.GL_FALSE) {
            sys.error(GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH)))
            return 0
        }
        return program
    }

    private def createShader(variables: collection.mutable.Map[String, Any], filename: String, shaderType: Int): Int = {
        var shader = 0
        try {
            shader = GL20.glCreateShader(shaderType)
            if (shader == 0) return 0
            val in = getClass.getResourceAsStream(filename)
            val data = Array.ofDim[Byte](in.available())
            in.read(data)
            in.close()
            var program = new String(data)
            val matcher = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*\\$\\{").matcher(program)
            while (matcher.find()) {
                val start = matcher.start()
                val end = program.indexOf("}", matcher.end())
                if (start == -1 || end == -1) throw new Exception("Invalid string replacement sequence found in shader \"" +
                    filename +
                    "\"")
                val variable = program.substring(start + 2, end)
                if (!variables.contains(variable)) throw new Exception("Coundn't find replacement for variable \"" + variable +
                    "\" in shader \"" +
                    filename +
                    "\"")
                program = program.substring(0, start) + variables.get(variable).get +
                    program.substring(end + 1, program.length)
            }
            program = program.replaceAll("\\$\\{", "\\${")
            print(program)
            GL20.glShaderSource(shader, program)
            GL20.glCompileShader(shader)
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) throw new RuntimeException("Error creating shader: " +
                GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH)))
            return shader
        } catch {
            case exc: Exception => {
                GL20.glDeleteShader(shader)
                throw exc
            }
        }
    }

    def printErrorLog(shader: Shader) {
        val intBuffer = BufferUtils.createIntBuffer(1)
        GL20.glGetProgram(shader.program, GL20.GL_INFO_LOG_LENGTH, intBuffer)
        val length = intBuffer.get
        if (length > 1) {
            val infoLog = BufferUtils.createByteBuffer(length)
            intBuffer.flip()
            GL20.glGetProgramInfoLog(shader.program, intBuffer, infoLog)
            val infoBytes = Array.ofDim[Byte](length)
            infoLog.get(infoBytes)
            val out = new String(infoBytes)
            sys.error("Shader info log:\n" + out)
        }
    }
}
