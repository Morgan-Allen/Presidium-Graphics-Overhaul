

package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.shaders.*;
import com.badlogic.gdx.graphics.g3d.utils.*;



//  TODO:  Port over cel-shading related code here as well?
//  TODO:  Look up the SpriteBatch code- it's not terribly complicated.


public class SolidsPass {
  
  
  final public static int
    MAX_BONES = 20 ;
  
  final Rendering rendering;
  final ModelBatch spriteBatch;
  final Batch <SolidSprite> inPass = new Batch <SolidSprite> ();
  
  
  public SolidsPass(Rendering rendering) {
    this.rendering = rendering;
    
    final DefaultShaderProvider provider = new DefaultShaderProvider() {
      protected Shader createShader(Renderable renderable) {
        DefaultShader shad = new GDXShader(renderable, config, null);
        return shad;
      }
    };
    provider.config.numBones = 20;
    provider.config.fragmentShader = Gdx.files.internal(
      "shaders/solids_new.frag"
    ).readString();
    provider.config.vertexShader = Gdx.files.internal(
      "shaders/solids_new.vert"
    ).readString();
    this.spriteBatch = new ModelBatch(provider);
  }
  
  
  public void dispose() {
    spriteBatch.dispose();
  }
  
  
  protected void register(SolidSprite sprite) {
    inPass.add(sprite);
  }
  
  
  public void performPass() {
    
    spriteBatch.begin(rendering.camera());
    for (SolidSprite sprite : inPass) {
      spriteBatch.render(sprite, rendering.environment());
    }
    spriteBatch.end();
    clearAll();
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
}

