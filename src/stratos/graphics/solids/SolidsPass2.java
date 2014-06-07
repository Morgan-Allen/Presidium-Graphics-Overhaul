

package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;



//TODO:  Port over cel-shading related code here as well?


public class SolidsPass2 {
  
  
  final int MAX_BONES = 20;
  
  final Rendering rendering;
  final Batch <SolidSprite> inPass = new Batch <SolidSprite> ();
  private ShaderProgram shading;
  
  
  
  public SolidsPass2(Rendering rendering) {
    this.rendering = rendering;
    
    shading = new ShaderProgram(
      Gdx.files.internal("shaders/solids2.vert"),
      Gdx.files.internal("shaders/solids2.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog()) ;
    }
    else {
      I.say("Solids shader compilation log:");
      I.say(shading.getLog());
    }
  }
  
  
  public void dispose() {
    shading.dispose();
  }
  
  
  protected void register(SolidSprite sprite) {
    inPass.add(sprite);
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
  

  public void performPass() {
    final Table <Texture, Batch <Renderable>> allParts = new Table();
    
    for (SolidSprite sprite : inPass) {
      final Batch <Renderable> fromSprite = new Batch();
      sprite.addRenderables(fromSprite);
      
      for (Renderable r : fromSprite) {
        final TextureAttribute t;
        t = (TextureAttribute) r.material.get(TextureAttribute.Diffuse);
        if (t == null) continue;
        final Texture key = t.textureDescription.texture;
        
        Batch <Renderable> parts = allParts.get(key);
        if (parts == null) allParts.put(key, parts = new Batch());
        parts.add(r);
      }
    }
    
    shading.begin();
    enableTextures();
    //  The texture, tex colour, no. of overlays, and overlays themselves-
    shading.setUniformi("u_texture", 0);
    shading.setUniform4fv("u_texColor", new float[] {1, 1, 1, 1}, 0, 4);
    
    //  The ambient light, diffuse light, and diffuse light direction-
    /*
    final float
      ambA[]     = rendering.lighting.ambient.toFloatVals(),
      difA[]     = rendering.lighting.diffuse.toFloatVals();
    final Vec3D d = rendering.lighting.direction;
    final float lightDir[] = new float[] { d.x, d.y, d.z } ;
    shading.setUniform3fv("u_ambientLight"  , ambA, 0, 3);
    shading.setUniform3fv("u_diffuseLight"  , difA, 0, 3);
    shading.setUniform3fv("u_lightDirection", lightDir, 0, 3);
    //*/
    
    shading.setUniform3fv("u_ambientLight"  , new float[3], 0, 3);
    shading.setUniform3fv("u_diffuseLight"  , new float[3], 0, 3);
    shading.setUniform3fv("u_lightDirection", new float[3], 0, 3);
    shading.setUniformMatrix("u_camera", rendering.camera().combined);
    
    for (Texture key : allParts.keySet()) {
      key.bind(0);
      final Batch <Renderable> parts = allParts.get(key);
      for (Renderable r : parts) {
        //  TODO:  Having to bind overlays afresh sorta defeats the purpose of
        //  keying each pass off a texture.  Use multiple passes instead?
        bindOverlays(key, r);
        
        final float[] bones = new float[MAX_BONES * 16];
        for (int i = 0; i < r.bones.length * 16; i++) {
          bones[i] = r.bones[i / 16].val[i % 16];
        }
        
        shading.setUniformMatrix("u_worldTrans", r.worldTransform);
        shading.setUniformi("u_numBones", 0);
        //shading.setUniformi("u_numBones", r.bones.length);
        //shading.setUniformMatrix4fv("u_bones", bones, 0, bones.length);
        
        r.mesh.render(
          shading, r.primitiveType, r.meshPartOffset, r.meshPartSize
        );
      }
    }
    
    shading.end();
    
    inPass.clear();
    allParts.clear();
  }
  
  
  void enableTextures() {
    /*
    Gdx.gl.glEnable(GL11.GL_TEXTURE);
    Gdx.gl.glEnable(GL11.GL_TEXTURE0);
    Gdx.gl.glEnable(GL11.GL_TEXTURE1);
    Gdx.gl.glEnable(GL11.GL_TEXTURE2);
    Gdx.gl.glEnable(GL11.GL_TEXTURE3);
    Gdx.gl.glEnable(GL11.GL_TEXTURE4);
    Gdx.gl.glEnable(GL11.GL_TEXTURE5);
    Gdx.gl.glEnable(GL11.GL_TEXTURE6);
    Gdx.gl.glEnable(GL11.GL_TEXTURE7);
    Gdx.gl.glEnable(GL11.GL_ACTIVE_TEXTURE);
    Gdx.gl.glActiveTexture(0);
    //*/
  }
  
  
  
  /**  Helper method for binding multiple texture overlays-
    */
  final static int MAX_SKINS = 8;
  
  private void bindOverlays(Texture key, Renderable r) {
    final OverlayAttribute a;
    a = (OverlayAttribute) r.material.get(OverlayAttribute.Overlay);
    
    if (a == null || a.textures == null || a.textures.length < 1) {
      shading.setUniformi("u_overnum", 0);
      return;
    }
    
    final int overnum = a.textures.length;
    if (overnum > MAX_SKINS) I.complain("TOO MANY OVERLAYS!");
    shading.setUniformi("u_overnum", overnum);
    
    shading.setUniformi("u_skinTex", 1);
    a.textures[0].bind(1);
    
    if (overnum > 1) {
      shading.setUniformi("u_costume", 2);
      a.textures[1].bind(2);
    }
  }
  
  
}

