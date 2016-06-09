
#version 120

#define M_PI 3.1415926535897932384626433832795

uniform float u_screenX;
uniform float u_screenY;
uniform float u_screenWide;
uniform float u_screenHigh;
uniform float u_portalRadius;

uniform bool u_surfacePass;
uniform float u_globeRadius;

uniform sampler2D u_surfaceTex;
uniform sampler2D u_sectorsMap;
uniform vec4 u_hoverKey;
uniform vec4 u_selectKey;

uniform sampler2D u_labelsTex;
uniform vec3 u_lightDirection;

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords0;
varying vec2 v_screenPos;




void main() {
  
  vec2 dist = vec2(
    (v_screenPos.x * u_screenWide) - u_screenX,
    (v_screenPos.y * u_screenHigh) - u_screenY
  );
  if (length(dist) > u_portalRadius) discard;
  vec4 color = vec4(0), over = vec4(1, 1, 1, 1);
  
  if (u_surfacePass) {
    vec4 key = texture2D(u_sectorsMap, v_texCoords0);
    color += texture2D(u_surfaceTex, v_texCoords0);
    
    vec3 lightVal = vec3(1, 1, 1);
    float dotVal = dot(-u_lightDirection, v_normal);
    if (dotVal <= 0) dotVal = 0.25f;
    else dotVal = clamp((dotVal * 2) + 0.25f, 0.0, 1.0);
    
    lightVal *= dotVal;
    color.rgb *= lightVal;
    
    if (key.rgb == u_hoverKey.rgb) {
      color.rgb += over.rgb * 0.25f * u_hoverKey.a ;
    }
    if (key.rgb == u_selectKey.rgb) {
      color.rgb += over.rgb * 0.25f * u_selectKey.a;
    }
  }
  else {
    color += texture2D(u_labelsTex, v_texCoords0);
  }
	
  gl_FragColor = color;
  if (gl_FragColor.a <= 0.001) discard;
}













    /*
    float angle = asin(v_position.x / length(v_position.xz));
    if (v_position.x < 0) angle += M_PI;
    vec2 texCoord = vec2(
      mod(angle / M_PI, 1),
      (1 + (v_position.y / u_globeRadius)) / 2
    );
    color += texture2D(u_sectorsTex, texCoord);
    color.a = (color.r + color.g + color.b) * 0.5f / 3;
    color.rgb = vec3(1, 1, 1);
    //*/


