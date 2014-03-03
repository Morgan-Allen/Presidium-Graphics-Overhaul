

package src.graphics.terrain;
import src.graphics.common.*;
import src.util.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture;



public abstract class LayerType implements TileConstants {
  
  
  final Texture texture;
  final int layerID;
  final boolean innerFringe;
  final private static boolean near[] = new boolean[8];
  
  
  public LayerType(ImageAsset image, boolean innerFringe, int layerID) {
    this.innerFringe = innerFringe;
    this.layerID = layerID;
    this.texture = image.asTexture();
  }
  
  
  public LayerType(String texName, boolean innerFringe, int layerID) {
    this.innerFringe = innerFringe;
    this.layerID = layerID;
    this.texture = Assets.getTexture(texName);
  }
  
  
  protected void dispose() {
    texture.dispose();
  }
  
  
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
      near[n] = maskedAt(x, y, terrain);
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




