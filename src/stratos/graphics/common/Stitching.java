/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.common;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.*;



public class Stitching {
  
  
  /**  Data fields, constructors and disposal methods-
    */
  final public static int
    //  Two triangles stretching between the diagonal and opposite corners-
    DEFAULT_QUAD_INDEX_ORDER[] = {
      0, 2, 1, 1, 2, 3
    },
    //  Position, normal, and UV data.
    DEFAULT_VERTEX_SIZE = 3 + 3 + 2,
    BONED_VERTEX_SIZE   = 3 + 3 + 2 + 2;
  
  final public int
    vertexSize,
    pieceSize,
    maxPieces,
    maxSize;
  final public boolean
    useQuads;
  
  final Mesh compiled;
  final float verts[];
  final short indices[];
  
  private int marker = 0;
  private float tempVB[], tempPB[];
  
  
  public Stitching(
    int vertexSize, boolean useQuads, int maxPieces,
    int quadIndexOrder[],
    VertexAttribute... attributes
  ) {
    this.vertexSize = vertexSize;
    this.pieceSize  = useQuads ? (vertexSize * 4) : (vertexSize * 3);
    this.maxPieces  = maxPieces;
    this.maxSize    = pieceSize * maxPieces;
    this.useQuads   = useQuads;
    
    verts    = new float[maxSize];
    indices  = new short[maxPieces * (useQuads ? 6 : 3)];
    compiled = new Mesh(
      Mesh.VertexDataType.VertexArray,
      false,
      verts.length / vertexSize, indices.length,
      attributes
    );
    this.tempVB = new float[vertexSize];
    this.tempPB = new float[pieceSize ];
    
    //  Next, we need to fill the index array.
    if (useQuads) {
      if (quadIndexOrder == null) quadIndexOrder = DEFAULT_QUAD_INDEX_ORDER;
      for (int p = 0, i = 0; p < (maxPieces * 4); p += 4) {
        indices[i++] = (short) (p + quadIndexOrder[0]);
        indices[i++] = (short) (p + quadIndexOrder[1]);
        indices[i++] = (short) (p + quadIndexOrder[2]);
        indices[i++] = (short) (p + quadIndexOrder[3]);
        indices[i++] = (short) (p + quadIndexOrder[4]);
        indices[i++] = (short) (p + quadIndexOrder[5]);
      }
    }
    else for (int i = 0; i < indices.length; i++) {
      indices[i] = (short) i;
    }
  }
  
  
  public Stitching(boolean useQuads, int maxPieces) {
    this(
      DEFAULT_VERTEX_SIZE, useQuads, maxPieces, DEFAULT_QUAD_INDEX_ORDER,
      VertexAttribute.Position(),
      VertexAttribute.Normal(),
      VertexAttribute.TexCoords(0)
    );
  }
  
  
  public void dispose() {
    compiled.dispose();
  }
  
  
  
  /**  Methods for appending data to the mesh-buffer:
    */
  public void appendVertex(float buffer[]) {
    if (buffer == null || buffer.length != vertexSize) {
      I.complain("Incorrect buffer size!");
    }
    System.arraycopy(buffer, 0, verts, marker, vertexSize);
    marker += vertexSize;
  }
  
  
  public void appendPiece(float buffer[]) {
    if (buffer == null || buffer.length != pieceSize) {
      I.complain("Incorrect buffer size!");
    }
    System.arraycopy(buffer, 0, verts, marker, pieceSize);
    marker += pieceSize;
  }
  
  
  public void appendDefaultVertex(
    Vec3D position, Vec3D normal, float texU, float texV,
    boolean flipZ
  ) {
    if (vertexSize != DEFAULT_VERTEX_SIZE) {
      I.complain("Incorrect buffer size!");
    }
    tempVB[0] = position.x;
    tempVB[1] = flipZ ? position.z : position.y;
    tempVB[2] = flipZ ? position.y : position.z;
    tempVB[3] = normal.x;
    tempVB[4] = flipZ ? normal.z : normal.y;
    tempVB[5] = flipZ ? normal.y : normal.z;
    tempVB[6] = texU;
    tempVB[7] = texV;
    appendVertex(tempVB);
  }
  
  
  public boolean meshFull() {
    return marker >= maxSize;
  }
  
  
  
  /**  Methods for performing actual render-pass execution.
    */
  public boolean reset() {
    if (marker == 0) return false;
    //  NOTE:  Not needed any more, but included as a reminder if needed...
    //for (int i = marker; i-- > 0;) verts[i] = 0;
    marker = 0;
    return true;
  }
  
  
  public void renderWithShader(ShaderProgram shading, boolean reset) {
    if (marker <= 0) return;
    compiled.setVertices(verts);
    compiled.setIndices(indices);
    
    final int numIndices = (marker / pieceSize) * (useQuads ? 6 : 3);
    compiled.render(shading, GL11.GL_TRIANGLES, 0, numIndices);
    
    if (reset) reset();
  }
}





