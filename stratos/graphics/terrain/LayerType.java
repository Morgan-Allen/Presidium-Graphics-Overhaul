

package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;
import com.badlogic.gdx.graphics.Texture;



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
  
  
  //  TODO:  This might not actually be needed, if image assets are disposed of
  //  automatically...
  protected void dispose() {
    for (Texture t : textures) t.dispose();
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
    final boolean central = layerID >= 0 ? (tileID >= layerID) : masked;
    
    if (central) {
      if (layerID < 0) {
        gridBatch.add(new Coord(tx, ty));
        textBatch.add(LayerPattern.OUTER_FRINGE_CENTRE);
      }
      else if (tileID == layerID) {
        gridBatch.add(new Coord(tx, ty));
        final int varID = terrain.varsIndices[tx][ty];
        textBatch.add(LayerPattern.extraFringeUV(varID, true)[0]);
      }
      return;
    }
    
    for (int n : N_INDEX) {
      final int x = tx + N_X[n], y = ty + N_Y[n];
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




