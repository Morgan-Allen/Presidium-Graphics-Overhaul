

#define maxBones 20

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_boneWeight0;
attribute vec2 a_boneWeight1;

varying vec3 v_normal;
varying vec4 v_color;
varying vec2 v_texCoords0;

varying vec3 v_lightDiffuse;
uniform vec3 u_ambientLight;

uniform mat4 u_camera;
uniform mat4 u_worldTrans;

uniform int u_numBones;
uniform mat4 u_bones[maxBones];



void main() {
  v_color = a_color;
  v_texCoords0 = a_texCoord0;
  v_normal = a_normal;
  
  mat4 transform = u_worldTrans;
  vec4 pos = vec4(a_position, 1.0);
  
  if (u_numBones > 0) {
    mat4 skinning = mat4(0.0);
    skinning += (a_boneWeight0.y) * u_bones[int(a_boneWeight0.x)];
    transform = transform * skinning;
  }
  
  //pos = transform * pos;
  //v_normal = normalize((transform * vec4(a_normal, 0.0)).xyz);
  gl_Position = u_camera * pos;
  
  gl_Position = vec4(a_position, 1.0);
}






