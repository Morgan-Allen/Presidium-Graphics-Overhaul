/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.glutils.*;
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
    final Batch <float[]> textBatch = new Batch();
    
    for (Coord c : Visit.grid(gridX, gridY, width, height, 1)) try {
      layer.addFringes(c.x, c.y, belongs, offsBatch, textBatch);
    } catch (Exception e) {}
    //
    //  We then create a new Stitching (with the default sequence of position/
    //  normal/tex-UV data) to compile the data.
    if (stitching != null) stitching.dispose();
    final int numTiles = offsBatch.size();
    stitching = new Stitching(true, numTiles);
    
    final Iterator
      iterO = offsBatch.iterator(),
      iterG = textBatch.iterator();
    Vec3D pos = new Vec3D(), norm = new Vec3D();
    float texU, texV;
    
    for (int n = 0; n < numTiles; n++) {
      final Vec3D coord  = (Vec3D  ) iterO.next();
      final float geom[] = (float[]) iterG.next();
      final float VP[]   = LayerPattern.VERT_PATTERN;
      
      for (int c = 0, p = 0, t = 0; c < 4; c++) {
        final int
          xoff = (int) VP[p + 0],
          yoff = (int) VP[p + 1],
          hX   = (int) (coord.x + xoff),
          hY   = (int) (coord.y + yoff),
          high;
        //
        //  In the case of normal tiles, we compose normals based on slope
        //  between adjoining tiles and sample within the same tile.
        pos.set(xoff - 0.5f, yoff - 0.5f, 0);
        putCornerNormal(hX, hY, norm);
        high = belongs.heightVals[hX][hY];
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
    //  If the corner is perfectly adjoined across the border, we average the
    //  measured slope over the adjacent tile (with greater weight given to
    //  the origin.)
    float sX = 0, sY = 0;
    final float mixWeight = 0.4f;
    sX += diff(x, y,  1,  0) * mixWeight;
    sX += diff(x, y, -1,  0) * mixWeight;
    sY += diff(x, y,  0,  1) * mixWeight;
    sY += diff(x, y,  0, -1) * mixWeight;
    sX /= 1 + (mixWeight * 2);
    sY /= 1 + (mixWeight * 2);
    //
    //  Then set the normal at 90 degrees to the slope, normalise and return.
    return norm.set(0 - sX, 0 - sY, 1).normalise();
  }
  
  
  private int diff(int x, int y, int offX, int offY) {
    byte high = belongs.heightVals[x][y];
    try { return high - belongs.heightVals[x + offX][y + offY]; }
    catch (ArrayIndexOutOfBoundsException e) { return 0; }
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
  
  
  protected void renderWithShader(ShaderProgram shading) {
    if (stitching == null) return;
    stitching.renderWithShader(shading, false);
  }
}











