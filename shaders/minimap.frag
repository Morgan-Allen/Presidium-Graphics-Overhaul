#version 120


uniform sampler2D u_texture;
uniform sampler2D u_fog_old;
uniform sampler2D u_fog_new;

uniform vec2 u_fogSize;
uniform bool u_fogFlag;
uniform float u_fogTime;

varying MED vec2 v_texCoords0;
varying MED vec3 v_position;



void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  if(u_fogFlag) {
    vec2 sampled = vec2(
      (v_position.x + 0.5f) / u_fogSize.x,
      (v_position.z + 0.5f) / u_fogSize.y
    );
    vec4 fogOld = texture2D(u_fog_old, sampled);
    vec4 fogNew = texture2D(u_fog_new, sampled);
    vec4 fog = mix(fogOld.rgba, fogNew.rgba, u_fogTime);
    color.rgb = mix(color.rgb, fog.rgb, 1 - fog.r);
  }
  gl_FragColor = color;
}