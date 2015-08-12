// Something to work on top of
// Going to port the three transforms of PMXTransformThread.transformVertex to this, 
// or maybe by using different shaders

attribute vec4 Bones; 
attribute vec3 Tangent; 
uniform mat4 Pose[32]; 
varying vec3 T,B,N; 
 
void main(void) { 
	mat4 mat = 0.0; 
 
	for (int i = 0; i < 4; i++) { 
		mat += Pose[int(Bones[i])] * fract(Bones[i]); 
	} 
 
	gl_Position = gl_ModelViewProjectionMatrix * (mat *gl_Vertex); 
 
	mat3 m3 = mat3(mat[0].xyz, mat[0].xyz, mat[0].xyz); // "mat3(mat)"
 
	N = gl_NormalMatrix * (m3 * gl_Normal); 
	T = gl_NormalMatrix * (m3 * Tangent); 
	B = cross(T, N); 
	
	gl_TexCoord[0] = gl_MultiTexCoord0; 
}