/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;




public class TerrainPass {
  
  
  final Rendering rendering;
  final ShaderProgram shader;
  private Batch <TerrainChunk>
    chunks = new Batch <TerrainChunk> (),
    overlays = new Batch <TerrainChunk> ();
  private FogOverlay fogApplied = null;
  
  
  public TerrainPass(Rendering rendering) {
    this.rendering = rendering;
    this.shader = new ShaderProgram(
      Gdx.files.internal("stratos/graphics/shaders/terrain.vert"),
      Gdx.files.internal("stratos/graphics/shaders/terrain.frag")
    );
    if (! shader.isCompiled()) {
      throw new GdxRuntimeException("\n"+shader.getLog());
    }
  }
  
  
  public void dispose() {
    shader.dispose();
  }
  
  
  protected void register(TerrainChunk chunk) {
    if (chunk.layer.layerID < 0) overlays.add(chunk);
    else chunks.add(chunk);
  }
  
  
  protected void applyFog(FogOverlay fog) {
    fogApplied = fog;
  }
  
  
  public void performPass() {
    if (chunks.size() == 0) return;
    beginShader();
    final TerrainSet set = chunks.first().belongs;
    //  We first compile the sets of terrain chunks in each layer, along with
    //  any customised/impromptu overlays which are rendered last, in order of
    //  presentation-
    final Batch <TerrainChunk>
      layerBatches[] = new Batch[set.layers.length];
    for (LayerType type : set.layers) {
      layerBatches[type.layerID] = new Batch <TerrainChunk> ();
    }
    for (TerrainChunk chunk : chunks) {
      final LayerType type = chunk.layer;
      layerBatches[type.layerID].add(chunk);
    }
    for (LayerType type : set.layers) {
      renderChunks(layerBatches[type.layerID], type);
    }
    shader.end();
    chunks.clear();
    fogApplied = null;
  }
  
  
  public void performOverlayPass() {
    if (overlays.size() == 0) return;
    beginShader();
    final Batch <TerrainChunk> single = new Batch <TerrainChunk> ();
    
    for (TerrainChunk chunk : overlays) {
      ///I.say("Rendering chunk, verts: "+chunk.vertices.length);
      single.clear();
      single.add(chunk);
      renderChunks(single, chunk.layer);
    }
    shader.end();
    overlays.clear();
  }
  
  
  private void beginShader() {
    //  Firstly, we set up shader parameters, include any fog of war-
    shader.begin();
    shader.setUniformMatrix("u_camera", rendering.camera().combined);
    shader.setUniformi("u_texture", 0);
    final float lightSum[] = rendering.lighting.lightSum;
    final float lightDir[] = rendering.lighting.lightDir;
    
    shader.setUniform4fv("u_lighting"      , lightSum, 0, 4);
    shader.setUniform3fv("u_lightDirection", lightDir, 0, 3);
    
    if (fogApplied != null) {
      fogApplied.applyToTerrain(shader);
      shader.setUniformi("u_fogFlag", GL20.GL_TRUE);
    }
    else shader.setUniformi("u_fogFlag", GL20.GL_FALSE);
  }
  
  
  public void clearAll() {
    chunks.clear();
    fogApplied = null;
  }
  
  
  
  protected void renderChunks(Batch <TerrainChunk> chunks, LayerType layer) {
    
    //  In the case of animated textures, we have to determine the current and
    //  next texture frames to fade between-
    final ImageAsset tex[] = layer.layerFrames;
    final float time = (Rendering.activeTime() % 1) * tex.length;
    final int index = (int) time, animIndex = (index + 1) % tex.length;
    
    for (int i : new int[] { index, animIndex }) {
      //
      //  We vary opacity in order to 'blur' between different frames for an
      //  animated terrain-patch-
      final float opacity = (i == index) ? 1 : (time % 1);
      tex[i].asTexture().bind(0);
      
      for (TerrainChunk chunk : chunks) {
        if (chunk.layer.layerID != layer.layerID) I.complain("WRONG LAYER!");
        final Colour c = chunk.colour == null ? Colour.WHITE : chunk.colour;
        //
        //  In the event that an earlier terrain chunk is being faded out, we
        //  render the predecessor semi-transparently (and dispose once gone.)
        //  Otherwise, we just default to full opacity for the primary texture.
        float inAlpha = 1, outAlpha = 0;
        if (chunk.fadeOut != null) {
          final float alpha = (chunk.fadeIncept + 1) - Rendering.activeTime();
          outAlpha = Nums.clamp(     alpha  * 2, 0, 1);
          inAlpha  = Nums.clamp((1 - alpha) * 2, 0, 1);
        }
        
        shader.setUniformf("u_texColor", c.r, c.g, c.b, 1);
        shader.setUniformf("u_opacity", opacity * c.a * inAlpha);
        chunk.renderWithShader(shader);
        
        if (outAlpha > 0) {
          shader.setUniformf("u_opacity", opacity * c.a * outAlpha);
          chunk.fadeOut.renderWithShader(shader);
        }
        else if (chunk.fadeOut != null) {
          chunk.fadeOut.dispose();
          chunk.fadeIncept = -1;
          chunk.fadeOut = null;
        }
        
        if (chunk.throwAway) chunk.dispose();
        else chunk.resetRenderFlag();
      }
      if (tex.length == 1) break;
    }
  }
}

