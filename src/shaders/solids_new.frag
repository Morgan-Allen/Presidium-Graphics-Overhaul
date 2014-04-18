

#ifdef GL_ES 
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#ifdef normalFlag
varying vec3 v_normal;
#endif //normalFlag

#if defined(colorFlag)
varying vec4 v_color;
#endif

#ifdef blendedFlag
varying float v_opacity;
#ifdef alphaTestFlag
varying float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
varying MED vec2 v_texCoords0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#ifdef specularFlag
varying vec3 v_lightSpecular;
#endif //specularFlag

#ifdef shadowMapFlag
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
varying vec3 v_shadowMapUv;
#define separateAmbientFlag

float getShadowness(vec2 offset)
{
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    return step(v_shadowMapUv.z, dot(texture2D(u_shadowTexture, v_shadowMapUv.xy + offset), bitShifts));//+(1.0/255.0));	
}

float getShadow() 
{
	return (//getShadowness(vec2(0,0)) + 
			getShadowness(vec2(u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(u_shadowPCFOffset, -u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, -u_shadowPCFOffset))) * 0.25;
}
#endif //shadowMapFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
varying float v_fog;
#endif // fogFlag




//  CUSTOM MODIFICATION STARTS HERE
uniform sampler2D u_overlays[8];
uniform int u_overnum;


vec4 mixOverlays()
{
  vec4 color = texture2D(u_diffuseTexture, v_texCoords0);
  
  //  NOTE:  Unfortunately, there's a fatal bug on OSX 10.6.8 which requires
  //  unrolling the inner loop here.  See a similar report here and fix here-
  //  http://www.opengl.org/discussion_boards/archive/index.php/t-166799.html
  //  https://github.com/gaborpapp/apps/blob/master/DepthMerge/resources/DepthMergeFrag.glsl
  
  int limit = u_overnum;
  if (limit > 8) limit = 8;
  
  if (limit > 0) {
    vec4 over = texture2D(u_overlays[0], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  else return color;
  
  if (limit > 1) {
    vec4 over = texture2D(u_overlays[1], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  else return color;
  
  if (limit > 2) {
    vec4 over = texture2D(u_overlays[2], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  else return color;
  
  if (limit > 3) {
    vec4 over = texture2D(u_overlays[3], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  else return color;
  
  if (limit > 4) {
    vec4 over = texture2D(u_overlays[4], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  else return color;
  
  if (limit > 5) {
    vec4 over = texture2D(u_overlays[5], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  else return color;
  
  if (limit > 6) {
    vec4 over = texture2D(u_overlays[6], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  else return color;
  
  if (limit > 7) {
    vec4 over = texture2D(u_overlays[7], v_texCoords0);
    color = mix(color, over, over.a);
    if (color.a < 0.001) discard;
  }
  return color;
}



//  NOTE:  This seems to work fine, but might throw up bugs on other hardware
//  due to not-referencing various flags.  Disabled for now.

/*
void main() {
  vec4 diffuse = mixOverlays();
  
  //gl_FragColor.rgb = diffuse.rgb;
  gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse);
  gl_FragColor.rgb = mix(diffuse.rgb, gl_FragColor.rgb, 0.5);
  
  #ifdef blendedFlag
    gl_FragColor.a = diffuse.a * v_opacity;
    #ifdef alphaTestFlag
      if (gl_FragColor.a <= v_alphaTest)
        discard;
    #endif
  #else
    gl_FragColor.a = 1.0;
  #endif
}
//*/



//  TODO:  90% of this stuff can be dispensed with.

//*
void main() {
	#if defined(normalFlag) 
		vec3 normal = v_normal;
	#endif // normalFlag
		
  #if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
    vec4 diffuse = mixOverlays() * u_diffuseColor * v_color;
  #elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
    vec4 diffuse = mixOverlays() * u_diffuseColor;
  #elif defined(diffuseTextureFlag) && defined(colorFlag)
    vec4 diffuse = mixOverlays() * v_color;
  #elif defined(diffuseTextureFlag)
    vec4 diffuse = mixOverlays();
  #elif defined(diffuseColorFlag) && defined(colorFlag)
    vec4 diffuse = u_diffuseColor * v_color;
  #elif defined(diffuseColorFlag)
    vec4 diffuse = u_diffuseColor;
  #elif defined(colorFlag)
    vec4 diffuse = v_color;
  #else
    vec4 diffuse = vec4(1.0);
  #endif

	#if (!defined(lightingFlag))  
		gl_FragColor.rgb = diffuse.rgb;
	#elif (!defined(specularFlag))
		#if defined(ambientFlag) && defined(separateAmbientFlag)
			#ifdef shadowMapFlag
				gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + getShadow() * v_lightDiffuse));
				//gl_FragColor.rgb = texture2D(u_shadowTexture, v_shadowMapUv.xy);
			#else
				gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + v_lightDiffuse));
			#endif //shadowMapFlag
		#else
			#ifdef shadowMapFlag
				gl_FragColor.rgb = getShadow() * (diffuse.rgb * v_lightDiffuse);
			#else
				gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse);
			#endif //shadowMapFlag
		#endif
	#else
		#if defined(specularTextureFlag) && defined(specularColorFlag)
			vec3 specular = texture2D(u_specularTexture, v_texCoords0).rgb * u_specularColor.rgb * v_lightSpecular;
		#elif defined(specularTextureFlag)
			vec3 specular = texture2D(u_specularTexture, v_texCoords0).rgb * v_lightSpecular;
		#elif defined(specularColorFlag)
			vec3 specular = u_specularColor.rgb * v_lightSpecular;
		#else
			vec3 specular = v_lightSpecular;
		#endif
			
		#if defined(ambientFlag) && defined(separateAmbientFlag)
			#ifdef shadowMapFlag
			gl_FragColor.rgb = (diffuse.rgb * (getShadow() * v_lightDiffuse + v_ambientLight)) + specular;
				//gl_FragColor.rgb = texture2D(u_shadowTexture, v_shadowMapUv.xy);
			#else
				gl_FragColor.rgb = (diffuse.rgb * (v_lightDiffuse + v_ambientLight)) + specular;
			#endif //shadowMapFlag
		#else
			#ifdef shadowMapFlag
				gl_FragColor.rgb = getShadow() * ((diffuse.rgb * v_lightDiffuse) + specular);
			#else
				gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse) + specular;
			#endif //shadowMapFlag
		#endif
	#endif //lightingFlag

	#ifdef fogFlag
		gl_FragColor.rgb = mix(gl_FragColor.rgb, u_fogColor.rgb, v_fog);
	#endif // end fogFlag

	#ifdef blendedFlag
		gl_FragColor.a = diffuse.a * v_opacity;
		#ifdef alphaTestFlag
			if (gl_FragColor.a <= v_alphaTest)
				discard;
		#endif
	#else
		gl_FragColor.a = 1.0;
	#endif

}
//*/

