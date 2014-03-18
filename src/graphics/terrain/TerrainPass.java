


package src.graphics.terrain;
import src.graphics.common.*;
import static src.graphics.common.GL.*;
import src.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;



public class TerrainPass {
  
  
  final Rendering rendering;
  final ShaderProgram shader;
  private Batch <TerrainChunk> chunks = new Batch <TerrainChunk> ();
  private FogOverlay fogApplied = null;
  
  
  public TerrainPass(Rendering rendering) {
    this.rendering = rendering;
    this.shader = new ShaderProgram(
      Gdx.files.internal("shaders/terrain.vert"),
      Gdx.files.internal("shaders/terrain.frag")
    );
    if (! shader.isCompiled()) {
      throw new GdxRuntimeException("\n"+shader.getLog()) ;
    }
  }
  
  
  public void dispose() {
    shader.dispose();
  }
  
  
  protected void register(TerrainChunk chunk) {
    chunks.add(chunk);
  }
  
  
  protected void applyFog(FogOverlay fog) {
    fogApplied = fog;
  }
  
  
  public void performPass() {
    if (chunks.size() == 0) return;
    
    shader.begin();
    shader.setUniformMatrix("u_camera", rendering.camera().combined);
    shader.setUniformi("u_texture", 0);
    //shader.setUniformi("u_animTex", 1);
    
    final float lightSum[] = rendering.lighting.lightSum();
    shader.setUniform4fv("u_lighting", lightSum, 0, 4);
    
    final TerrainSet set = chunks.first().belongs;
    if (fogApplied != null) {
      fogApplied.applyToShader(shader);
      shader.setUniformi("u_fogFlag", GL_TRUE);
    }
    else shader.setUniformi("u_fogFlag", GL_FALSE);
    
    //  TODO:  What about customised terrain splats?  ...If the ID is -1,
    //  render them last, but in order of presentation.
    for (LayerType type : set.layers) renderChunks(chunks, type);
    shader.end();
    
    clearAll();
  }
  
  
  protected void renderChunks(Batch <TerrainChunk> chunks, LayerType layer) {
    final Texture tex[] = layer.textures;
    final float time = (Rendering.activeTime() % 1) * tex.length;
    final int index = (int) time, animIndex = (index + 1) % tex.length;
    
    for (int i : new int[] { index, animIndex }) {
      final float opacity = (i == index) ? 1 : (time % 1);
      tex[i].bind(0);
      
      for (TerrainChunk chunk : chunks) {
        if (chunk.layer.layerID != layer.layerID) continue;
        
        //  In the event that an earlier terrain chunk is being faded out,
        //  render the predecessor semi-transparently-
        if (chunk.fadeOut != null) {
          final float alpha = (chunk.fadeIncept + 1) - Rendering.activeTime();
          
          if (alpha > 0) {
            final float outAlpha = Visit.clamp(alpha * 2, 0, 1);
            shader.setUniformf("u_opacity", opacity * outAlpha);
            chunk.fadeOut.mesh.render(shader, GL20.GL_TRIANGLES);
          }
          else {
            chunk.fadeOut.dispose();
            chunk.fadeIncept = -1;
            chunk.fadeOut = null;
          }
          
          final float inAlpha = Visit.clamp((1 - alpha) * 2, 0, 1);
          shader.setUniformf("u_opacity", opacity * inAlpha);
          chunk.mesh.render(shader, GL20.GL_TRIANGLES);
        }
        
        //  Otherwise just render directly-
        else {
          shader.setUniformf("u_opacity", opacity);
          chunk.mesh.render(shader, GL20.GL_TRIANGLES);
        }
        
        chunk.renderFlag = false;
      }
      if (tex.length == 1) break;
    }
  }
  
  
  public void clearAll() {
    chunks.clear();
    fogApplied = null;
  }
}



//  TODO:  ...Do I need this?
/*
if (chunk.belongs != set) I.complain(
  "ALL RENDERED CHUNKS MUST BELONG TO SAME TERRAIN SET!"
) ;
//*/