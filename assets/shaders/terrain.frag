#version 120

#ifdef GL_ES 
#define MED mediump
precision mediump float;
#else
#define MED
#endif



uniform sampler2D u_texture;
uniform sampler2D u_fog;
uniform vec2 u_fogSize;
uniform bool u_fogFlag;

varying MED vec2 v_texCoords0;
varying MED vec3 v_position;


void main() {
	vec4 color = texture2D(u_texture, v_texCoords0);
	
	if(u_fogFlag) {
		vec4 fog = texture2D(u_fog, vec2(v_position.x / u_fogSize.x, v_position.z/ u_fogSize.y));
		color.rgb = mix(color.rgb, fog.rgb, fog.a);
	}
	gl_FragColor = color;
}
