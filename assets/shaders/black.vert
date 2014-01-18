
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;
attribute vec2 a_boneWeight0;

uniform mat4 u_bones[20];
uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;
uniform bool u_skinningFlag;




void main() {

	
	mat4 skinning = mat4(1.0);
	
	if(u_skinningFlag)
		skinning = (a_boneWeight0.y) * u_bones[int(a_boneWeight0.x)];

	vec4 pos = u_worldTrans * skinning * vec4(a_position, 1.0);
	
	gl_Position = u_projViewTrans * pos;
	//gl_Position.z += 0.001;
}