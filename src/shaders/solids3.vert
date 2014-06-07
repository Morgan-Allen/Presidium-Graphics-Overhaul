
#version 120


#define maxBones 20


attribute vec3 a_position;
attribute vec3 a_normal;
attribute float a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_boneWeight0;

uniform int u_numBones;
uniform mat4 u_bones[maxBones];

uniform mat4 u_worldTrans;
uniform mat4 u_camera;

varying vec2 v_texCoords0;
varying vec3 v_position;



void main() {
	v_texCoords0 = a_texCoord0;
	v_position = a_position;
	
	
	vec4 pos = vec4(v_position, 1.0);
  mat4 transform = u_worldTrans;
  
  if (u_numBones > 0) {
    mat4 skinning = mat4(0.0);
    skinning += (a_boneWeight0.y) * u_bones[int(a_boneWeight0.x)];
    transform = transform * skinning;
    //pos = skinning * pos;
  }
	
	pos = transform * pos;
	gl_Position = u_camera * pos;
}

