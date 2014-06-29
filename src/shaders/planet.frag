
#version 120


#define maxOverlays 8


varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords0;

uniform bool u_surfacePass;
uniform sampler2D u_surfaceTex;
uniform sampler2D u_sectorsTex;
uniform sampler2D u_sectorsMap;
uniform vec4 u_sectorKey;

uniform vec3 u_lightDirection;




void main() {
  vec4 color = vec4(0), over = vec4(1, 1, 1, 1);
  if (u_surfacePass) {
    vec4 key = texture2D(u_sectorsMap, v_texCoords0);
    color += texture2D(u_surfaceTex, v_texCoords0);
    if (key == u_sectorKey) color = mix(color, over, 0.5);
    
    vec3 lightVal = vec3(1, 1, 1);
    float dotVal = dot(-u_lightDirection, v_normal);
    if (dotVal <= 0) dotVal = 0.25f;
    else dotVal = clamp((dotVal * 2) + 0.25f, 0.0, 1.0);
    
    lightVal *= dotVal;
    color.rgb *= lightVal;
  }
  else {
    color += texture2D(u_sectorsTex, v_texCoords0);
    color.a = (color.r + color.g + color.b) / 3;
    color = mix(color, vec4(1, 1, 1, 0), 0.5);
  }
	
  gl_FragColor = color;
  if (gl_FragColor.a <= 0.001) discard;
}





