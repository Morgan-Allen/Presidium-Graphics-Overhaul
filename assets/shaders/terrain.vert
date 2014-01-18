attribute vec3 a_position;
attribute vec2 a_texCoord0;
attribute vec2 a_texCoord1;
attribute vec2 a_texCoord2;
attribute vec2 a_texCoord3;

uniform mat4 u_camera;

varying out vec2 v_texCoords0;
varying out vec2 v_texCoords1;
varying out vec2 v_texCoords2;
varying out vec2 v_texCoords3;
varying out vec3 v_position;

void main() {
	v_texCoords0 = a_texCoord0;
	v_texCoords1 = a_texCoord1;
	v_texCoords2 = a_texCoord2;
	v_texCoords3 = a_texCoord3;
	v_position = a_position;
	gl_Position = u_camera * vec4(a_position, 1.0);

}

