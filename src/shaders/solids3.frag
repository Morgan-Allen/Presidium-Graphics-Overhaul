#version 120


uniform sampler2D u_texture;
//uniform vec4 u_lighting;
//uniform sampler2D u_fog_old;
//uniform sampler2D u_fog_new;
//uniform float u_opacity;
//uniform bool u_fogFlag;
//uniform vec2 u_fogSize;
//uniform float u_fogTime;

varying vec2 v_texCoords0;
varying vec3 v_position;


void main() {
	vec4 color = texture2D(u_texture, v_texCoords0);
  gl_FragColor = color;
	
	/*
	if(u_fogFlag) {
		vec2 sampled = vec2(
			(v_position.x + 0.5f) / u_fogSize.x,
			(v_position.z + 0.5f) / u_fogSize.y
		);
		vec4 fogOld = texture2D(u_fog_old, sampled);
		vec4 fogNew = texture2D(u_fog_new, sampled);
		vec4 fog = mix(fogOld.rgba, fogNew.rgba, u_fogTime);
		float darken = fog.r;
		color.r *= darken;
		color.g *= darken;
		color.b *= darken;
	}
	
	//  In the case of a 'glow' texture, ignore current lighting.
	if (u_opacity < 0) {
    color.a *= 0 - u_opacity;
    gl_FragColor = color;
	}
	else {
    color.a *= u_opacity;
    gl_FragColor = color * u_lighting;
	}
	//*/
}




