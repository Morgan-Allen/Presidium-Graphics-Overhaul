#version 120


uniform sampler2D u_texture;
uniform vec4 u_lighting;

varying vec2 v_texCoords0;
varying vec3 v_position;
varying vec4 v_color;


//  TODO:  Perform glow fx (ignore lighting) here?

void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  color = color * v_color * u_lighting;
  
  if (color.a < 0.1) discard;
  else gl_FragDepth = gl_FragCoord.z;
  
  gl_FragColor = color;
}