#version 110

uniform sampler2D tex_sampler;
varying float originalY;
varying vec4 color;
uniform float fadeIn,fadeInDiscard,textured;
void main() {
    vec4 col = color;
    if (textured > 0.5f)
        col *= texture2D(tex_sampler, gl_TexCoord[0].st);
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
