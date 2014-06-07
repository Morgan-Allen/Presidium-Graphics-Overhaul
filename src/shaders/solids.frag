
#version 120


#define maxOverlays 8


varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords0;

uniform sampler2D u_texture;
uniform int u_numOverlays;
uniform sampler2D u_over0;
uniform sampler2D u_over1;
uniform sampler2D u_over2;
uniform sampler2D u_over3;
uniform sampler2D u_over4;
uniform sampler2D u_over5;
uniform sampler2D u_over6;
uniform sampler2D u_over7;
uniform vec4 u_texColor;

uniform vec4 u_ambientLight;
uniform vec4 u_diffuseLight;
uniform vec3 u_lightDirection;


vec4 mixOverlays();



void main() {
	vec4 color = mixOverlays() * u_texColor;
	
  vec3 lightVal = u_ambientLight.rgb;
  float dotVal = dot(-u_lightDirection, v_normal);
  
  if (dotVal > 1) dotVal = 1;
  else if (dotVal < 0) {
    lightVal *= (0.5f - dotVal);
  }
  else lightVal += u_diffuseLight.rgb * dotVal;
  
  color.rgb *= lightVal;
	
  gl_FragColor = color;
  if (gl_FragColor.a <= 0.001) discard;
}



vec4 mixOverlays()
{
  vec4 color = texture2D(u_texture, v_texCoords0);
  
  //  NOTE:  There's a limitation of GLSL 1.2 or lower which requires unrolling
  //  the inner loop here.
  int limit = u_numOverlays;
  if (limit > maxOverlays) limit = maxOverlays;
  
  if (limit > 0) {
    vec4 over = texture2D(u_over0, v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 1) {
    vec4 over = texture2D(u_over1, v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 2) {
    vec4 over = texture2D(u_over2, v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 3) {
    vec4 over = texture2D(u_over3, v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 4) {
    vec4 over = texture2D(u_over4, v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 5) {
    vec4 over = texture2D(u_over5, v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 6) {
    vec4 over = texture2D(u_over6, v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 7) {
    vec4 over = texture2D(u_over7, v_texCoords0);
    color = mix(color, over, over.a);
  }
  return color;
}





