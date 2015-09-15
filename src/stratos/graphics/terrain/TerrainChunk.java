/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;
import java.util.Iterator;



public class TerrainChunk implements TileConstants {
  
  
  private static boolean verbose = false;
  
  final int width, height, gridX, gridY;
  final LayerType layer;
  final TerrainSet belongs;
  
  private Stitching stitching = null;
  private boolean renderFlag = false, refreshFlag = true;
  
  protected TerrainChunk fadeOut = null;
  protected float fadeIncept = -1;
  
  public Colour colour = Colour.WHITE;
  public boolean throwAway = false;
  
  
  public TerrainChunk(int width, int height, int gridX, int gridY,
    LayerType layer, TerrainSet belongs
  ) {
    this.gridX   = gridX  ;
    this.gridY   = gridY  ;
    this.width   = width  ;
    this.height  = height ;
    this.layer   = layer  ;
    this.belongs = belongs;
  }
  
  
  public void dispose() {
    if (stitching != null) {
      stitching.dispose();
      stitching = null;
    }
  }
  
  
  public void generateMeshData() {
    if (verbose) I.say("Generating mesh data for "+this.hashCode());
    //
    //  First of all, compile a list of all occupied tiles and their
    //  associated UV fringing, based on the position of any adjacent tiles
    //  with the same layer assignment.
    final Batch <Vec3D  > offsBatch = new Batch();
    final Batch <Integer> faceBatch = new Batch();
    final Batch <float[]> textBatch = new Batch();
    
    for (Coord c : Visit.grid(gridX, gridY, width, height, 1)) try {
      layer.addFringes(c.x, c.y, belongs, offsBatch, faceBatch, textBatch);
    } catch (Exception e) {}
    //
    //  We then create a new Stitching (with the default sequence of position/
    //  normal/tex-UV data) to compile the data.
    if (stitching != null) stitching.dispose();
    final int numTiles = offsBatch.size();
    stitching = new Stitching(true, numTiles);
    
    final Iterator
      iterO = offsBatch.iterator(),
      iterF = faceBatch.iterator(),
      iterG = textBatch.iterator();
    Vec3D pos = new Vec3D(), norm = new Vec3D();
    float texU, texV;
    
    for (int n = 0; n < numTiles; n++) {
      final Vec3D coord  = (Vec3D  ) iterO.next();
      final int   facing = (Integer) iterF.next();
      final float geom[] = (float[]) iterG.next();
      final float VP[]   = LayerPattern.VERT_PATTERN;
      
      for (int c = 0, p = 0, t = 0; c < 4; c++) {
        final int
          xoff = (int) VP[p + 0],
          yoff = (int) VP[p + 1],
          hX   = (int) (coord.x * 2),
          hY   = (int) (coord.y * 2),
          high;
        //
        //  In the case of cliff-segments, we flip the section on it's side,
        //  rotate to face an adjacent tile as required, and sample height from
        //  the edges of said tile.
        if (facing != -1) {
          pos.set(xoff - 0.5f, yoff - 0.5f, - 0.5f);
          norm.set(0, 0, 1);
          ROTATE_Y.trans(pos );
          ROTATE_Y.trans(norm);
          Z_ROTATIONS[facing].trans(pos );
          Z_ROTATIONS[facing].trans(norm);
          final int
            faceX = (pos.x > 0 ? 1 : 0) + T_X[facing * 2],
            faceY = (pos.y > 0 ? 1 : 0) + T_Y[facing * 2];
          high = belongs.heightVals[hX + faceX][hY + faceY];
          pos.z -= 0.5f;
        }
        //
        //  In the case of normal tiles, we compose normals based on slope
        //  between adjoining tiles and sample within the same tile.
        else {
          pos.set(xoff - 0.5f, yoff - 0.5f, 0);
          putCornerNormal(hX + xoff, hY + yoff, norm);
          high = belongs.heightVals[hX + xoff][hY + yoff];
        }
        //
        //  Then stitch the results together for later rendering...
        pos.add(coord);
        pos.z = (pos.z + high) / 4f;
        texU = geom[t++];
        texV = geom[t++];
        p += 3;
        stitching.appendDefaultVertex(pos, norm, texU, texV, true);
      }
    }
    refreshFlag = false;
  }
  
  
  private Vec3D putCornerNormal(int x, int y, Vec3D norm) {
    //
    //  We determine the grid-coordinates of the tile and the relative offset
    //  of this corner first (the height-map has a 2x2:1x1 resolution)
    final boolean
      xUp = x % 2 == 1,
      yUp = y % 2 == 1;
    final int
      tX = (x / 2) * 2,
      tY = (y / 2) * 2;
    //
    //  We measure the slope across the x and y axis, and determine if the
    //  edges are flush with adjacent tiles.
    float sX = diff(tX, y, 1, 0);
    float sY = diff(x, tY, 0, 1);
    final boolean
      joinX = diff(x, y, xUp ? 1 : -1, 0) == 0,
      joinY = diff(x, y, 0, yUp ? 1 : -1) == 0;
    //
    //  If the corner is perfectly adjoined across the border, we average the
    //  measured slope over the adjacent tile (with greater weight given to
    //  the origin.)
    final float mixWeight = 0.4f;
    if (joinX) {
      sX += diff(tX + (xUp ? 2 : -2), y, 1, 0) * mixWeight;
      sX /= 1 + mixWeight;
    }
    if (joinY) {
      sY += diff(x, tY + (yUp ? 2 : -2), 0, 1) * mixWeight;
      sY /= 1 + mixWeight;
    }
    //
    //  Then set the normal at 90 degrees to the slope, normalise and return.
    return norm.set(0 - sX, 0 - sY, 1).normalise();
  }
  
  
  private int diff(int x, int y, int offX, int offY) {
    return TerrainSet.heightDiff(belongs, x, y, offX, offY);
  }
  
  
  protected void flagRefresh() {
    refreshFlag = true;
  }
  
  
  protected boolean needsRefresh() {
    return refreshFlag;
  }
  
  
  protected void resetRenderFlag() {
    renderFlag = false;
  }
  
  
  public void readyFor(Rendering rendering) {
    if (renderFlag || (stitching == null && fadeOut == null)) return;
    renderFlag = true;
    rendering.terrainPass.register(this);
  }
  
  
  protected Stitching stitching() {
    return stitching;
  }
}








