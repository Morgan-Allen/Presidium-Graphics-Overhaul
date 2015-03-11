#version 120


uniform sampler2D u_texture;
uniform vec4 u_lighting;
uniform bool u_glowFlag;

varying vec2 v_texCoords0;
varying vec3 v_position;
varying vec4 v_color;


//  TODO:  You should be performing texture combinations at this stage.

void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  if (u_glowFlag) {
    color = color * v_color * u_lighting;
  }
  else {
    color = color * v_color * u_lighting;
  }
  
  if (color.a < 0.1) discard;
  else gl_FragDepth = gl_FragCoord.z;
  
  gl_FragColor = color;
}