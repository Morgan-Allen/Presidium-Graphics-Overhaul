#version 120


uniform sampler2D u_texture;

varying vec2 v_texCoords0;
varying vec3 v_position;
varying vec4 v_color;


void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  color = color * v_color;
  
  if (color.a < 0.5) gl_FragDepth = -10;
  else gl_FragDepth = gl_FragCoord.z;
  
  gl_FragColor = color;
}