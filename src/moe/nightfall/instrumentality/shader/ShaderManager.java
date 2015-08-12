package moe.nightfall.instrumentality.shader;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** See http://lwjgl.org/wiki/index.php?title=GLSL_Shaders_with_LWJGL **/
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
				vertShader = createShader(shader.vertexShader, ARBVertexShader.GL_VERTEX_SHADER_ARB);
			if (shader.fragmentShader != null)
				fragShader = createShader(shader.fragmentShader, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
		} catch (Exception exc) {
			exc.printStackTrace();
			return 0;
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

	private static int createShader(String filename, int shaderType) throws Exception {
		int shader = 0;
		try {
			shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
			if (shader == 0)
				return 0;

			FileInputStream fin = new FileInputStream(filename);
			byte[] data = new byte[fin.available()];
			fin.read(data);
			fin.close();

			ARBShaderObjects.glShaderSourceARB(shader, new String(data));
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