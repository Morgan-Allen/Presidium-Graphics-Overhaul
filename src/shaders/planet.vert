
#version 120

attribute vec3 a_position;
attribute vec3 a_normal;
attribute float a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_boneWeight0;

uniform bool u_surfacePass;

uniform float u_globeRadius;
uniform mat4 u_rotation;
uniform mat4 u_camera;

varying vec2 v_texCoords0;
varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_screenPos;



void main() {
	v_texCoords0 = a_texCoord0;
	v_normal = normalize(a_position);
	
	if (u_surfacePass) {
	  v_position = v_normal * u_globeRadius;
	}
	else {
	  v_position = v_normal * u_globeRadius;
	}
	
	vec4 pos = vec4(v_position, 1.0);
  mat4 transform = u_rotation;
	
  v_normal = normalize((transform * vec4(v_normal, 0.0)).xyz);
	pos = transform * pos;
	pos = u_camera * pos;
	
	if (! u_surfacePass) {
    pos.xy += a_normal.xy;
	}
  
	gl_Position = pos;
	v_screenPos = pos.xy;
}


