

package src.graphics.terrain;
import src.graphics.common.*;
import src.util.*;

import static src.graphics.common.GL.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;



public abstract class LayerType implements TileConstants {
  
  
  
  /**  Data, constants, constructors, setup and cleanup-
    */
  final Texture textures[];
  final int layerID;
  final boolean innerFringe;
  final private static boolean near[] = new boolean[8];
  
  
  public LayerType(ImageAsset image, boolean innerFringe, int layerID) {
    this.innerFringe = innerFringe;
    this.layerID = layerID;
    this.textures = new Texture[] { image.asTexture() };
  }
  
  
  public LayerType(ImageAsset tex[], boolean innerFringe, int layerID) {
    this.innerFringe = innerFringe;
    this.layerID = layerID;
    this.textures = new Texture[tex.length];
    for (int i = tex.length ; i-- > 0;) {
      this.textures[i] = tex[i].asTexture();
    }
  }
  
  
  public LayerType(String texName, boolean innerFringe, int layerID) {
    this.innerFringe = innerFringe;
    this.layerID = layerID;
    this.textures = new Texture[] { ImageAsset.getTexture(texName) };
  }
  
  
  protected void dispose() {
    for (Texture t : textures) t.dispose();
  }
  
  
  
  /**  Rendering methods-
    */
  protected void renderChunks(
    ShaderProgram shader, Batch <TerrainChunk> chunks
  ) {
    final float time = (Rendering.activeTime() % 1) * textures.length;
    final int index = (int) time, animIndex = (index + 1) % textures.length;
    
    for (int i : new int[] { index, animIndex }) {
      final float opacity = (i == index) ? 1 : (time % 1);
      textures[i].bind(0);
      shader.setUniformf("u_opacity", opacity);
      
      for (TerrainChunk chunk : chunks) {
        if (chunk.layer.layerID != this.layerID) continue;
        chunk.mesh.render(shader, GL20.GL_TRIANGLES);
        chunk.renderFlag = false;
      }
      if (textures.length == 1) break;
    }
  }
  
  
  
  /**  Geometry generation-
    */
  protected abstract boolean maskedAt(int tx, int ty, TerrainSet terrain);
  protected abstract int variantAt(int tx, int ty, TerrainSet terrain);
  

  protected void addFringes(
      int tx, int ty, TerrainSet terrain,
      Batch<Coord> gridBatch, Batch<float[]> textBatch
  ) {
    final boolean masked = maskedAt(tx, ty, terrain);
    if (innerFringe && ! masked) return;
    final int tileID = terrain.layerIndices[tx][ty];
    
    if (tileID >= layerID && ! innerFringe) {
      if (tileID == layerID) {
        // TODO: Use some variation here...
        
        gridBatch.add(new Coord(tx, ty));
        textBatch.add(LayerPattern.OUTER_FRINGE_CENTRE);
      }
      return;
    }
    
    for (int n : N_INDEX) {
      final int x = tx + N_X[n], y = ty + N_Y[n];
      /*
      if (
        Visit.clamp(x, terrain.size) != x ||
        Visit.clamp(y, terrain.size) != y
      ) near[n] = false;
      //*/
      try { near[n] = maskedAt(x, y, terrain); }
      catch (ArrayIndexOutOfBoundsException e) { near[n] = false ; }
    }
    final float fringes[][] = innerFringe ?
      LayerPattern.innerFringeUV(near) :
      LayerPattern.outerFringeUV(near);
    
    if (fringes != null) for (float UV[] : fringes) if (UV != null) {
      gridBatch.add(new Coord(tx, ty));
      textBatch.add(UV);
    }
  }
}




