#version 120


uniform sampler2D u_texture;
uniform sampler2D u_fog_old;
uniform sampler2D u_fog_new;

uniform bool u_fogFlag;
uniform float u_fogTime;

uniform vec2 u_box_lower_corner;
uniform vec2 u_box_upper_corner;

varying vec2 v_texCoords0;
varying vec3 v_position;


bool inCameraBox(int x, int y) {
  int
    minX = int(u_box_lower_corner.x),
    minY = int(u_box_lower_corner.y),
    maxX = int(u_box_upper_corner.x),
    maxY = int(u_box_upper_corner.y);
  if ((x == minX || x == maxX) && y > minY && y < maxY) return true;
  if ((y == minY || y == maxY) && x > minX && x < maxX) return true;
  return false;
}


void main() {
  vec4 color = texture2D(u_texture, v_texCoords0);
  
  if(u_fogFlag) {
    vec4 fogOld = texture2D(u_fog_old, v_texCoords0);
    vec4 fogNew = texture2D(u_fog_new, v_texCoords0);
    vec4 fog = mix(fogOld.rgba, fogNew.rgba, u_fogTime);
    float darken = 0.0 + (fog.r * 1.0);
    color.r *= darken;
    color.g *= darken;
    color.b *= darken;
  }
  
  if (inCameraBox(int(v_position.x), int(v_position.y))) {
    color = vec4(1, 1, 1, 1);
  }
  gl_FragColor = color;
}


