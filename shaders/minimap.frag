#version 120


uniform sampler2D u_texture;
uniform sampler2D u_fog_old;
uniform sampler2D u_fog_new;

uniform bool u_fogFlag;
uniform float u_fogTime;

varying vec2 v_texCoords0;
varying vec3 v_position;



void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  if(u_fogFlag) {
    vec4 fogOld = texture2D(u_fog_old, v_texCoords0);
    vec4 fogNew = texture2D(u_fog_new, v_texCoords0);
    vec4 fog = mix(fogOld.rgba, fogNew.rgba, u_fogTime);
    float darken = (1 + fog.r) / 2;
    color.r *= darken;
    color.g *= darken;
    color.b *= darken;
  }
  gl_FragColor = color;
}