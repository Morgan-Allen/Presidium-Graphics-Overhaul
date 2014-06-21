


#version 120

uniform sampler2D u_texture;

varying vec4 v_color;
varying vec2 v_texCoords0;



void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  color *= v_color;
  
  float value = 0;
  if (color.r > value) value = color.r;
  if (color.g > value) value = color.g;
  if (color.b > value) value = color.b;
  
  if (value > 0) color.rgb /= value;
  color.a *= value;
  
  gl_FragColor = color;
}
