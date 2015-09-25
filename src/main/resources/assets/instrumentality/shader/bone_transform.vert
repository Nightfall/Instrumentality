#version 110
/* Something to work on top of
 * Going to port the three transforms of PMXTransformThread.transformVertex to this, 
 * or maybe by using different shaders
 * (-- Vic, though Vic didn't sign it)
 * 
 * I made the bone transforms matrices to speed this up.
 * You just need to somehow get them into the shader... more difficult than it seems :)
 * Importantly, if there's a fixed set of posevalues, we need to allocate a fixed set of bones
 * that receive these posevalues (that is - the ones we're animating)
 *
 * BTW, I've added some comments to the code, and adjusted a small problem.
 * For future reference:
 * "Loading shaders, GLSL version supported: 1.30"
 * apparently I can USE GLSL 3.3...if I use the core profile...
 * _which isn't possible with MC modding_
 * DAMMIT MESA!
 * -- gamemanj
 */
attribute vec4 Bones; 
attribute vec3 Tangent;
// Note: The value would be something like 12 in practice, I want to make sure that if the regex fails we know about it
uniform mat4 Pose[/*${groupSize*/1/*}*/];
varying vec3 T,B,N;
varying float originalY;

void main(void) { 
    // Just "0.0" did not work on my system.
    // If this fails, try 16 0.0s separated by commas. Works here though.
    mat4 mat = mat4(0.0);
    // This supports *4* bone weights at once.
    // Each number is the number of a bone, with the fractional being a weight.
    mat += Pose[int(Bones[0])] * fract(Bones[0]);
    mat += Pose[int(Bones[1])] * fract(Bones[1]);
    mat += Pose[int(Bones[2])] * fract(Bones[2]);
    mat += Pose[int(Bones[3])] * fract(Bones[3]);

    gl_Position = gl_ModelViewProjectionMatrix * (mat * gl_Vertex);

    mat3 m3 = mat3(mat[0].xyz, mat[0].xyz, mat[0].xyz); // "mat3(mat)"

    N = gl_NormalMatrix * (m3 * gl_Normal);
    T = gl_NormalMatrix * (m3 * Tangent); 
    B = cross(T, N);
    originalY = gl_Vertex.y;

    gl_TexCoord[0] = gl_MultiTexCoord0; 
}
