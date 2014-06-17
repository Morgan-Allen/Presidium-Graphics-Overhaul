
#version 120

attribute vec3 a_position;
attribute vec3 a_normal;
attribute float a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_boneWeight0;

uniform mat4 u_rotation;
uniform mat4 u_camera;

varying vec2 v_texCoords0;
varying vec3 v_position;
varying vec3 v_normal;



void main() {
	v_texCoords0 = a_texCoord0;
	v_position = a_position;
	//v_position = v_position * 25 / length(v_position);
	
	vec4 pos = vec4(v_position, 1.0);
  mat4 transform = u_rotation;
	
  v_normal = normalize((transform * vec4(a_normal, 0.0)).xyz);
	pos = transform * pos;
	gl_Position = u_camera * pos;
}

  
  