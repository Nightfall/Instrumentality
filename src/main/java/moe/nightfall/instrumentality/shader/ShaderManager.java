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

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * See http://lwjgl.org/wiki/index.php?title=GLSL_Shaders_with_LWJGL
 **/
public class ShaderManager {

    private static List<Shader> shaders = new LinkedList<Shader>();

    public static void loadShaders() {
        System.out.println("Loading shaders, GLSL version supported: " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        for (Shader shader : shaders) {
            shader.program = compileProgram(shader);
            if (shader.vertexShader != null) {
                System.out.println("Loaded vertex shader: \"" + shader.vertexShader + "\"");
            }
            if (shader.fragmentShader != null) {
                System.out.println("Loaded fragment shader: \"" + shader.fragmentShader + "\"");
            }
        }
    }

    public static Shader createProgram(String vertexShader, String fragmentShader) {
        Shader shader = new Shader(vertexShader, fragmentShader);
        shaders.add(shader);
        return shader;
    }

    private static int compileProgram(Shader shader) {
        int vertShader = 0, fragShader = 0;
        try {
            if (shader.vertexShader != null)
                vertShader = createShader(shader.variables, shader.vertexShader, ARBVertexShader.GL_VERTEX_SHADER_ARB);
            if (shader.fragmentShader != null)
                fragShader = createShader(shader.variables, shader.fragmentShader, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        int program = ARBShaderObjects.glCreateProgramObjectARB();
        if (program == 0)
            return 0;

        if (vertShader != 0)
            ARBShaderObjects.glAttachObjectARB(program, vertShader);
        if (fragShader != 0)
            ARBShaderObjects.glAttachObjectARB(program, fragShader);

        ARBShaderObjects.glLinkProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            System.err.println(ARBShaderObjects.glGetInfoLogARB(program, ARBShaderObjects.glGetObjectParameteriARB(program,
                    ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB)));
            return 0;
        }

        ARBShaderObjects.glValidateProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            System.err.println(ARBShaderObjects.glGetInfoLogARB(program, ARBShaderObjects.glGetObjectParameteriARB(program,
                    ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB)));
            return 0;
        }

        return program;
    }

    private static int createShader(Map<String, Object> variables, String filename, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
            if (shader == 0)
                return 0;

            FileInputStream fin = new FileInputStream(filename);
            byte[] data = new byte[fin.available()];
            fin.read(data);
            fin.close();
            
            String program = new String(data);
            Matcher matcher = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*\\$\\{").matcher(program);
            while (matcher.find()) {
            	int start = matcher.start();
            	int end = program.indexOf("}", matcher.end());
            	if (start == -1 || end == -1) 
            		throw new Exception("Invalid string replacement sequence found in shader \"" + filename + "\"");
            	String variable = program.substring(start + 2, end);
            	if (!variables.containsKey(variable)) 
            		throw new Exception("Coundn't find replacement for variable \"" + variable + "\" in shader \"" + filename + "\"");
            	program = program.substring(0, start) + variables.get(variable) + program.substring(end + 1, program.length());
            }
            // TODO Proper escape sequences?
            program = program.replaceAll("\\$\\{", "\\${");

            ARBShaderObjects.glShaderSourceARB(shader, program);
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: "
                        + ARBShaderObjects.glGetInfoLogARB(shader, ARBShaderObjects.glGetObjectParameteriARB(shader,
                        ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB)));

            return shader;

        } catch (Exception exc) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw exc;
        }
    }

    public static void printErrorLog(Shader shader) {
        IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(shader.getProgram(), ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, intBuffer);

        int length = intBuffer.get();
        if (length > 1) {
            ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
            intBuffer.flip();
            ARBShaderObjects.glGetInfoLogARB(shader.getProgram(), intBuffer, infoLog);
            byte[] infoBytes = new byte[length];
            infoLog.get(infoBytes);
            String out = new String(infoBytes);
            System.err.println("Shader info log:\n" + out);
        }
    }

    public static void bindShader(Shader shader) {
        ARBShaderObjects.glUseProgramObjectARB(shader.getProgram());
    }

    public static void releaseShader() {
        ARBShaderObjects.glUseProgramObjectARB(0);
    }
}
