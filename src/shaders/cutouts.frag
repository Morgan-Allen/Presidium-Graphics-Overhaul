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
    //
    //  Here, in essence, you convert any darker areas over to the glow-tone.
    float alpha = color.a * v_color.a;
    ///float grey = (color.r + color.g + color.b) / 3;
    float grey = max(color.r, max(color.g, color.b));
    ///color = mix(color, vec4(0, 0, 0, 1), grey);
    color = mix(color, v_color, 1 - grey);
    color.a = alpha;
  }
  else {
    color = color * v_color * u_lighting;
  }
  
  if (color.a < 0.1) discard;
  else gl_FragDepth = gl_FragCoord.z;
  
  gl_FragColor = color;
}