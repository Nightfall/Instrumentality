#version 110

uniform sampler2D tex_sampler;
varying float originalY, alpha;
varying vec4 color;
varying vec3 normal;
uniform float fadeIn,fadeInDiscard,textured;
void main() {
    vec4 col = color;
    col = mat4(
        1.0,0.0,0.0,0.0,
        0.0,1.0,0.0,0.0,
        0.0,0.0,1.0,0.0,
        0.0,0.0,0.0,alpha
    ) * col;

    if (alpha < 0.05)
        discard;

    if (textured > 0.5)
        col *= texture2D(tex_sampler, gl_TexCoord[0].st);

    if (normal.y < 0.0) {
        float sv = atan(-normal.y) / 6.282;
        float m = 1.0 - sv;
        col = mat4(
        m,0.0,0.0,0.0,
        0.0,m,0.0,0.0,
        0.0,0.0,m,0.0,
        0.0,0.0,0.0,1.0
        ) * col;
    }

    if (originalY > fadeIn) {
    	if (originalY > fadeInDiscard)
    		discard;
    	float dist=fadeInDiscard-fadeIn;
    	float pos=(originalY-fadeIn)/dist;
        gl_FragColor = (vec4(0.0,0.5,1.0,0.0)*pos)+(col*(1.0-pos));
	} else {       
    	gl_FragColor = col;
    }
}
