/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;
import com.badlogic.gdx.graphics.*;
import java.util.Iterator;




public class TerrainChunk implements TileConstants {
  
  
  private static boolean verbose = false;
  
  final int width, height, gridX, gridY;
  final LayerType layer;
  final TerrainSet belongs;
  
  private float[] vertices;
  private short[] indices;
  private Mesh mesh;
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
    if (mesh != null) {
      mesh.dispose();
      mesh = null;
    }
  }
  
  
  protected Mesh mesh() {
    if (mesh == null) {
      if (verbose && vertices.length > 0) {
        I.say("Setting up mesh object for "+hashCode());
        I.say("Vertex/index length: "+vertices.length+"/"+indices.length);
        I.say("Layer ID: "+layer.layerID);
      }
      mesh = new Mesh(
        true, vertices.length, indices.length,
        VertexAttribute.Position (),
        VertexAttribute.Normal   (),
        VertexAttribute.TexCoords(0)
      );
      mesh.setVertices(vertices);
      mesh.setIndices(indices);
    }
    return mesh;
  }
  
  
  public void generateMeshData() {
    if (verbose) I.say("Generating mesh data for "+this.hashCode());
    //
    //  First of all, compile a list of all occupied tiles and their
    //  associated UV fringing, based on the position of any adjacent tiles
    //  with the same layer assignment.
    final Batch<Coord> gridBatch = new Batch<Coord>();
    final Batch<float[]> textBatch = new Batch<float[]>();

    for (Coord c : Visit.grid(gridX, gridY, width, height, 1)) try {
      layer.addFringes(c.x, c.y, belongs, gridBatch, textBatch);
    } catch (Exception e) {}
    //
    //  We have 4 vertices and 2 faces per tile.  Each vertex requires 3 floats
    //  for geometry, 3 for normals, and 2 for tex coords.  And each face
    //  requires 3 vertex indices.
    final int numTiles = gridBatch.size();
    vertices = new float[numTiles * 4 * (3 + 3 + 2)];
    indices  = new short[numTiles * 2 * 3          ];
    final Iterator iterV = gridBatch.iterator(), iterT = textBatch.iterator();
    final Vec3D norm = new Vec3D();
    //
    //  Then we just fill up the arrays with the appropriate concatenation
    //  of data-
    for (int n = 0, pointV = 0, pointI = 0; n < numTiles; n++) {
      
      final Coord coord = (Coord) iterV.next();
      final float VP[] = LayerPattern.VERT_PATTERN;
      final float[] UV = (float[]) iterT.next();
      
      for (int c = 0, p = 0, t = 0; c < 4; c++) {
        final int
          xoff = (int) VP[p + 0],
          yoff = (int) VP[p + 1],
          zoff = (int) VP[p + 2];
        p += 3;
        putCornerSlope(coord.x + xoff, coord.y + zoff, norm);
        final float high = belongs.heightVals[coord.x + xoff][coord.y + zoff];
        
        vertices[pointV++] = xoff + coord.x - 0.5f;
        vertices[pointV++] = yoff + (high / 4);
        vertices[pointV++] = zoff + coord.y - 0.5f;
        
        vertices[pointV++] = norm.x;
        vertices[pointV++] = norm.z;
        vertices[pointV++] = norm.y;
        
        vertices[pointV++] = UV[t++];
        vertices[pointV++] = UV[t++];
      }
      for (float i : LayerPattern.INDEX_PATTERN) {
        indices[pointI++] = (short) ((n * 4) + i);
      }
    }
    refreshFlag = false;
  }
  
  
  Vec3D putCornerSlope(int x, int y, Vec3D norm) {
    return norm.set(
      0 - (slope(x, y, true ) + slope(x - 1, y, true )),
      0 - (slope(x, y, false) + slope(x, y - 1, false)),
      2
    ).normalise();
  }
  
  
  float slope(int x, int y, boolean across) {
    final byte HV[][] = belongs.heightVals;
    try { return across ?
      HV[x + 1][y] - HV[x][y] :
      HV[x][y + 1] - HV[x][y] ;
    }
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
    if (renderFlag || (vertices.length == 0 && fadeOut == null)) return;
    renderFlag = true;
    mesh();
    rendering.terrainPass.register(this);
  }
}








