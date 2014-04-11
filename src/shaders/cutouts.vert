
#version 120


attribute vec3 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

uniform mat4 u_camera;

varying vec2 v_texCoords0;
varying vec3 v_position;
varying vec4 v_color;


void main() {
  v_color = a_color;
  v_texCoords0 = a_texCoord0;
  v_position = a_position;
  gl_Position = u_camera * vec4(a_position, 1.0);
}