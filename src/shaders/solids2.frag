

#define maxOverlays 8

varying vec3 v_normal;
varying vec4 v_color;
varying vec2 v_texCoords0;

uniform int u_overnum;
uniform sampler2D u_texture;
uniform sampler2D u_skinTex;
uniform sampler2D u_costume;
uniform vec4 u_texColor;

uniform vec3 u_ambientLight;
uniform vec3 u_diffuseLight;
uniform vec3 u_lightDirection;



void main() {
  
  //  First, get the sum of relevant light values.
  vec3 lightVal = u_ambientLight;
  float dotVal = clamp(dot(-u_lightDirection, v_normal), 0.0, 1.0);
  lightVal += u_diffuseLight * dotVal;
  
  //  Then obtain the texture-based colour value.
  vec4 diffuse = texture2D(u_texture, v_texCoords0);
  if (u_overnum > 0) {
    vec4 over = texture2D(u_skinTex, v_texCoords0);
    //diffuse = mix(diffuse, over, over.a);
  }
  if (u_overnum > 1) {
    vec4 over = texture2D(u_costume, v_texCoords0);
    //diffuse = mix(diffuse, over, over.a);
  }
  diffuse *= u_texColor;
  
  //  Finally, assign the value and check for sufficient alpha.
  gl_FragColor.rgba = diffuse.rgba;
  //gl_FragColor.rgb *= lightVal;
  //if (gl_FragColor.a <= 0.001) discard;
  
  gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
}




//  NOTE:  This is probably redundant now, since I'm having difficulty getting
//  sampler arrays to work properly, and I can't reliably iterate over them in
//  any case.

/*
vec4 mixOverlays()
{
  vec4 color = texture2D(u_texture, v_texCoords0);
  
  //  NOTE:  Unfortunately, there's a fatal bug on OSX 10.6.8 which requires
  //  unrolling the inner loop here.  See a similar report here and fix here-
  //  http://www.opengl.org/discussion_boards/archive/index.php/t-166799.html
  //  https://github.com/gaborpapp/apps/blob/master/DepthMerge/resources/DepthMergeFrag.glsl
  
  int limit = u_overnum;
  if (limit > maxOverlays) limit = maxOverlays;
  
  if (limit > 0) {
    vec4 over = texture2D(u_overlays[0], v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 1) {
    vec4 over = texture2D(u_overlays[1], v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 2) {
    vec4 over = texture2D(u_overlays[2], v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 3) {
    vec4 over = texture2D(u_overlays[3], v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 4) {
    vec4 over = texture2D(u_overlays[4], v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 5) {
    vec4 over = texture2D(u_overlays[5], v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 6) {
    vec4 over = texture2D(u_overlays[6], v_texCoords0);
    color = mix(color, over, over.a);
  }
  else return color;
  
  if (limit > 7) {
    vec4 over = texture2D(u_overlays[7], v_texCoords0);
    color = mix(color, over, over.a);
  }
  return color;
}
//*/

