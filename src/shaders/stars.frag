


#version 120

uniform sampler2D u_texture;

uniform float u_screenX;
uniform float u_screenY;
uniform float u_screenWide;
uniform float u_screenHigh;
uniform float u_portalRadius;

varying vec4 v_color;
varying vec2 v_texCoords0;
varying vec2 v_screenPos;



void main() {
  vec2 dist = vec2(
    (v_screenPos.x * u_screenWide) - u_screenX,
    (v_screenPos.y * u_screenHigh) - u_screenY
  );
  if (length(dist) > u_portalRadius) discard;
  
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

