#version 120
#define maxBones 50


attribute vec3 a_position;
attribute vec3 a_normal;
attribute float a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_boneWeight0;
attribute vec2 a_boneWeight1;
attribute vec2 a_boneWeight2;

uniform int u_numBones;
uniform mat4 u_bones[maxBones];

uniform mat4 u_worldTrans;
uniform mat4 u_camera;

uniform vec4 u_ambientLight;
uniform vec4 u_diffuseLight;
uniform vec3 u_lightDirection;

varying vec2 v_texCoords0;
varying vec3 v_position;
varying vec3 v_normal;
varying vec3 v_lightDiffuse;



void main() {
  v_texCoords0 = a_texCoord0;
  v_position = a_position;
  
  vec4 pos = vec4(v_position, 1.0);
  mat4 transform = u_worldTrans;
  
  if (u_numBones > 0) {
    mat4 boneTrans = mat4(0.0);
    boneTrans += (a_boneWeight0.y) * u_bones[int(a_boneWeight0.x)];
    boneTrans += (a_boneWeight1.y) * u_bones[int(a_boneWeight1.x)];
    boneTrans += (a_boneWeight2.y) * u_bones[int(a_boneWeight2.x)];
    transform = transform * boneTrans;
  }
  v_normal = normalize((transform * vec4(a_normal, 0.0)).xyz);
  
  {
    float dota = dot(v_normal, -u_lightDirection);
    vec3 ambient = u_ambientLight.rgb;
    vec3 diffuse = u_diffuseLight.rgb;
    
    // diffuse light from the front and more ambient from back
    if (dota < 0)
      v_lightDiffuse = ambient * (0.5f - dota);
    else
      v_lightDiffuse = ambient + (diffuse * dota);
    
    // standard directional light
    //v_lightDiffuse = ambient + diffuse * clamp(dota, 0.0, 1.0);
  }
  
  pos = transform * pos;
  gl_Position = u_camera * pos;
}



