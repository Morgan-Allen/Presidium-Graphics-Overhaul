#version 120

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_camera;

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords0;


void main() {
	v_position   = a_position ;
	v_normal     = a_normal   ;
	v_texCoords0 = a_texCoord0;
	gl_Position = u_camera * vec4(a_position, 1.0);
}