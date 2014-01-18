#ifdef GL_ES 
#define MED mediump
precision mediump float;
#else
#define MED
#endif



uniform sampler2D u_texture;
uniform sampler2D u_fog;
uniform ivec2 u_fogSize;
uniform bool u_fogFlag;

varying MED vec2 v_texCoords0;
varying MED vec2 v_texCoords1;
varying MED vec2 v_texCoords2;
varying MED vec2 v_texCoords3;
varying MED vec3 v_position;


void main() {
	//gl_FragColor = texture2D(u_texture, v_texCoords0);
	vec4 t0 = texture2D(u_texture, v_texCoords0);
	vec4 t1 = texture2D(u_texture, v_texCoords1);
	vec4 t2 = texture2D(u_texture, v_texCoords2);
	vec4 t3 = texture2D(u_texture, v_texCoords3);
	
	vec4 color = t0;
	color = mix(color, t1, t1.a);
	color = mix(color, t2, t2.a);
	color = mix(color, t3, t3.a);
	
	if(u_fogFlag) {
		vec4 fog = texture2D(u_fog, vec2(v_position.x / u_fogSize.x, v_position.z/ u_fogSize.y));
		color.rgb = mix(color.rgb, fog.rgb, fog.a);
	}
	
	gl_FragColor = color;
	//gl_FragColor.a = 1.0;
}