

package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL11;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.shaders.*;
import com.badlogic.gdx.graphics.g3d.utils.*;



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

    final RenderContext context = spriteBatch.getRenderContext();
    context.setDepthTest(GL20.GL_LEQUAL);
    context.setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    Gdx.gl.glDisable(GL11.GL_LIGHTING);
    
    spriteBatch.begin(rendering.camera());
    for (SolidSprite sprite : inPass) {
      spriteBatch.render(sprite, rendering.environment());
    }
    spriteBatch.end();
    clearAll();
    Gdx.gl.glEnable(GL11.GL_LIGHTING);
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
}




















//TODO:  Use this?
//shading.setVertexAttribute(name, size, type, normalize, stride, offset);


//TODO:  Basic mesh-rendering code is here.  (Consider discarding LibGDX
//entirely?)  Hmm.  I still need the image composition stuff.

/*
public void render (ShaderProgram shader, int primitiveType, int offset, int count, boolean autoBind) {
    if (count == 0) return;

    if (autoBind) bind(shader);

    if (isVertexArray) {
        if (indices.getNumIndices() > 0) {
            ShortBuffer buffer = indices.getBuffer();
            int oldPosition = buffer.position();
            int oldLimit = buffer.limit();
            buffer.position(offset);
            buffer.limit(offset + count);
            Gdx.gl20.glDrawElements(primitiveType, count, GL20.GL_UNSIGNED_SHORT, buffer);
            buffer.position(oldPosition);
            buffer.limit(oldLimit);
        } else {
            Gdx.gl20.glDrawArrays(primitiveType, offset, count);
        }
    } else {
        if (indices.getNumIndices() > 0)
            Gdx.gl20.glDrawElements(primitiveType, count, GL20.GL_UNSIGNED_SHORT, offset * 2);
        else
            Gdx.gl20.glDrawArrays(primitiveType, offset, count);
    }

    if (autoBind) unbind(shader);
}
//*/



