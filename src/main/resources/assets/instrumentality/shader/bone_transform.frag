#version 110

uniform sampler2D tex_sampler;
void main() {
    gl_FragColor = texture2D(tex_sampler, gl_TexCoord[0].st);
}