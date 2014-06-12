
#version 120


#define maxOverlays 8


varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords0;

uniform bool u_surfacePass;
uniform sampler2D u_surfaceTex;
uniform sampler2D u_sectorsTex;

uniform vec3 u_lightDirection;




void main() {
  vec4 color = vec4(0);
  if (u_surfacePass) {
    color += texture2D(u_surfaceTex, v_texCoords0);
    
    vec3 lightVal = vec3(1, 1, 1);
    float dotVal = dot(-u_lightDirection, v_normal);
    lightVal *= dotVal;
    color.rgb *= lightVal;
  }
  else {
    color += texture2D(u_sectorsTex, v_texCoords0);
  }
	
  gl_FragColor = color;
  if (gl_FragColor.a <= 0.001) discard;
}







  /*
  vec3 lightVal = u_ambientLight.rgb;
  float dotVal = dot(-u_lightDirection, v_normal);
  
  if (dotVal > 1) dotVal = 1;
  else if (dotVal < 0) {
    lightVal *= (0.5f - dotVal);
  }
  else lightVal += u_diffuseLight.rgb * dotVal;
  color.rgb *= lightVal;
  //*/