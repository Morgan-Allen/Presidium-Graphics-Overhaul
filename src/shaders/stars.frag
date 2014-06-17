


#version 120

uniform sampler2D u_texture;

//varying vec4 v_color;
varying vec2 v_texCoords0;



void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  //color *= v_color;
  
  gl_FragColor = color;
}
