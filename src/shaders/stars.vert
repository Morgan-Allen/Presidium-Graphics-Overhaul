
#version 120

attribute vec3 a_position;
attribute vec3 a_normal;
attribute float a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_rotation;
uniform mat4 u_camera;

varying vec2 v_texCoords0;
//varying vec4 v_color;



void main() {
	v_texCoords0 = a_texCoord0;
	//v_color = a_color;
	
  vec4 pos = vec4(a_position, 1.0);
  pos = u_rotation * pos;
  pos = u_camera * pos;
  pos.xy += a_normal.xy;
  
  gl_Position = pos;
}