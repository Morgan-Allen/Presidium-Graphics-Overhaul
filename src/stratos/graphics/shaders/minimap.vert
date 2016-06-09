#version 120


attribute vec3 a_position;
attribute vec2 a_texCoord0;
uniform mat4 u_ortho;
varying vec2 v_texCoords0;
varying vec3 v_position;


void main() {
  v_texCoords0 = a_texCoord0;
  v_position = a_position;
  gl_Position = u_ortho * vec4(a_position, 1.0);
}