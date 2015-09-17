/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;



public abstract class LayerType implements TileConstants {
  
  
  /**  Data, constants, constructors, setup and cleanup-
    */
  final public String layerName;
  final public int layerID;
  final public ImageAsset layerFrames[];
  final public boolean innerFringe;
  
  final private static boolean near[] = new boolean[8];
  
  
  public LayerType(
    ImageAsset image, boolean innerFringe, int layerID, String name
  ) {
    this(new ImageAsset[] { image }, innerFringe, layerID, name);
  }
  
  
  public LayerType(
    ImageAsset images[], boolean innerFringe, int layerID, String name
  ) {
    this.layerFrames = images     ;
    this.innerFringe = innerFringe;
    this.layerID     = layerID    ;
    this.layerName   = name       ;
  }
  
  
  
  /**  Geometry generation-
    */
  protected abstract boolean maskedAt(int tx, int ty, TerrainSet terrain);
  protected abstract int variantAt(int tx, int ty, TerrainSet terrain);
  
  //
  //  TODO:  Consider returning arbitrary geometry within a Fragment class, so
  //  that position/normal/UV data is neatly wrapped and identified in transit.
  
  protected void addFringes(
    int tx, int ty, TerrainSet terrain,
    Batch <Vec3D  > offsBatch,
    Batch <Integer> faceBatch,
    Batch <float[]> textBatch
  ) {
    final boolean masked = maskedAt(tx, ty, terrain);
    if (innerFringe && ! masked) return;
    
    final int tileID = terrain.layerIndices[tx][ty];
    final int varID = variantAt(tx, ty, terrain);
    final boolean central = layerID >= 0 ? (tileID >= layerID) : masked;
    
    if (central) {
      //
      //  For central tiles, we include a texture-variant  occupying a full
      //  square:
      if (layerID < 0) {
        offsBatch.add(new Vec3D(tx, ty, 0));
        faceBatch.add(-1);
        textBatch.add(innerFringe ?
          LayerPattern.INNER_FRINGE_CENTRE :
          LayerPattern.OUTER_FRINGE_CENTRE
        );
      }
      else if (tileID == layerID) {
        offsBatch.add(new Vec3D(tx, ty, 0));
        faceBatch.add(-1);
        textBatch.add(LayerPattern.extraFringeUV(varID)[0]);
      }
      //
      //  We also include geometry for any cliffs along the facing edges-
      if (layerID == tileID && ! innerFringe) for (int n : T_ADJACENT) {
        final int faceID = n / 2;
        final float cornerCoords[] = LayerPattern.FACING_CORNER_PATTERN[faceID];
        final int
          nX = T_X[n],
          nY = T_Y[n],
          xA = (int) cornerCoords[0],
          yA = (int) cornerCoords[1],
          xB = (int) cornerCoords[2],
          yB = (int) cornerCoords[3],
          hX = tx * 2,
          hY = ty * 2,
          sideA = TerrainSet.heightDiff(
            terrain, hX + xA, hY + yA, nX, nY
          ),
          sideB = TerrainSet.heightDiff(
            terrain, hX + xB, hY + yB, nX, nY
          ),
          cliff = Nums.max(sideA, sideB);
        
        for (int h = cliff; h-- > 0;) {
          offsBatch.add(new Vec3D(tx, ty, 0 - h));
          faceBatch.add(faceID);
          textBatch.add(LayerPattern.CLIFF_EXTRAS_UV[faceID]);
        }
      }
      return;
    }
    else {
      if (layerID < 0 && varID == -1) return;
    }
    //
    //  NOTE:  Due to some dumb legacy requirements of mine, I've had to swap
    //  the X and Y coordinates here to maintain the usefulness of some
    //  layer-patterns from an older version of the engine.  Remove later...
    //  if I ever get time.
    for (int n : T_INDEX) {
      final int x = tx + T_Y[n], y = ty + T_X[n];
      try { near[n] = maskedAt(x, y, terrain); }
      catch (ArrayIndexOutOfBoundsException e) { near[n] = false; }
    }
    final float fringes[][] = innerFringe ?
      LayerPattern.innerFringeUV(near, varID % 2) :
      LayerPattern.outerFringeUV(near);
    if (fringes != null) for (float UV[] : fringes) if (UV != null) {
      offsBatch.add(new Vec3D(tx, ty, 0));
      faceBatch.add(-1);
      textBatch.add(UV);
    }
  }
}









