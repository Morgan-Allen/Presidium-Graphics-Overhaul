

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


public class SolidsPass {

  final static int
    MAX_SKINS = 8,
    MAX_BONES = 20;
  
  final static String OVER_NAMES[] = {
    "u_over0", "u_over1", "u_over2", "u_over3",
    "u_over4", "u_over5", "u_over6", "u_over7",
  };
  
  
  final Rendering rendering;
  final Batch <SolidSprite> inPass = new Batch <SolidSprite> ();
  private ShaderProgram shading;
  
  
  
  public SolidsPass(Rendering rendering) {
    this.rendering = rendering;
    
    shading = new ShaderProgram(
      Gdx.files.internal("shaders/solids.vert"),
      Gdx.files.internal("shaders/solids.frag")
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
    
    //  TODO:  Go back to keying each pass off particular textures again.
    //final Table <Texture, Batch <Renderable>> allParts = new Table();
    
    shading.begin();
    shading.setUniformi("u_texture", 0);
    for (int i = 0 ; i < MAX_SKINS; i++) {
      shading.setUniformi(OVER_NAMES[i], i + 1);
    }
    
    
    //
    //  TODO:  Insert fog and selection FX here.
    //shading.setUniform4fv("u_texColor", new float[] {1, 1, 1, 1}, 0, 4);
    
    //  The ambient light, diffuse light, and diffuse light direction-
    final float
      ambA[]     = rendering.lighting.ambient.toFloatVals(),
      difA[]     = rendering.lighting.diffuse.toFloatVals();
    final Vec3D d = rendering.lighting.direction;
    final float lightDir[] = new float[] { d.x, d.y, d.z } ;
    shading.setUniform4fv("u_ambientLight"  , ambA, 0, 4);
    shading.setUniform4fv("u_diffuseLight"  , difA, 0, 4);
    shading.setUniform3fv("u_lightDirection", lightDir, 0, 3);
    shading.setUniformMatrix("u_camera", rendering.camera().combined);
    
    //I.say("Ambient: "+rendering.lighting.ambient);
    //I.say("Diffuse: "+rendering.lighting.diffuse);
    
    for (SolidSprite sprite : inPass) {
      final Batch <Renderable> fromSprite = new Batch();
      sprite.addRenderables(fromSprite);
      
      for (Renderable r : fromSprite) {
        final TextureAttribute t;
        t = (TextureAttribute) r.material.get(TextureAttribute.Diffuse);
        if (t == null) continue;
        final Texture key = t.textureDescription.texture;
        
        key.bind(0);
        bindOverlays(key, r);
        
        final float[] bones = new float[MAX_BONES * 16];
        for (int i = 0; i < r.bones.length * 16; i++) {
          bones[i] = r.bones[i / 16].val[i % 16];
        }
        
        shading.setUniformMatrix("u_worldTrans", r.worldTransform);
        shading.setUniformi("u_numBones", r.bones.length);
        shading.setUniformMatrix4fv("u_bones", bones, 0, bones.length);
        
        r.mesh.render(
          shading, r.primitiveType, r.meshPartOffset, r.meshPartSize
        );
      }
    }
    
    shading.end();
    inPass.clear();
  }
  
  
  
  /**  Helper method for binding multiple texture overlays-
    */
  private void bindOverlays(Texture key, Renderable r) {
    final OverlayAttribute a;
    a = (OverlayAttribute) r.material.get(OverlayAttribute.Overlay);
    
    if (a == null || a.textures == null || a.textures.length < 1) {
      shading.setUniformi("u_numOverlays", 0);
      return;
    }
    final int numOver = a.textures.length;
    if (numOver > MAX_SKINS) I.complain("TOO MANY OVERLAYS!");
    
    shading.setUniformi("u_numOverlays", numOver);
    for (int i = 0; i < numOver; i++) a.textures[i].bind(i + 1);
  }
  
  
}




