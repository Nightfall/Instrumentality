#version 110

uniform sampler2D tex_sampler;
varying float originalY;
uniform float fadeIn,fadeInDiscard;
void main() {
    if (originalY > fadeIn) {
    	if (originalY > fadeInDiscard)
    		discard;
    	float dist=fadeInDiscard-fadeIn;
    	float pos=(originalY-fadeIn)/dist;
        gl_FragColor = (vec4(0.0,0.5,1.0,0.0)*pos)+(texture2D(tex_sampler, gl_TexCoord[0].st)*(1.0-pos));
	} else {       
    	gl_FragColor = texture2D(tex_sampler, gl_TexCoord[0].st);
    }
}
