package moe.nightfall.instrumentality.shader;

public class Shader {
	
	int program;
	final String vertexShader;
	final String fragmentShader;
	
	Shader(String vertexShader, String fragmentShader) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
	}
	
	public int getProgram() {
		return program;
	}
}
