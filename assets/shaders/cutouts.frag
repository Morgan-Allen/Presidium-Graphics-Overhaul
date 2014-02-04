#version 120


uniform sampler2D u_texture;

varying vec2 v_texCoords0;
varying vec3 v_position;


void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  gl_FragColor = color;
}

