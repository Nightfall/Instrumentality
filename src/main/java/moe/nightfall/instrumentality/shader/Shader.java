package moe.nightfall.instrumentality.shader;

/**
 * A shader.
 * A holder for the GL program ID.
 */
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
