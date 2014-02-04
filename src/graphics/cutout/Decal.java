

package graphics.cutout ;
import util.I;

import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g2d.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;
import com.badlogic.gdx.graphics.g3d.decals.* ;





public class Decal {
  
  // 3(x,y,z) + 1(color) + 2(u,v)
  public static final int
    VERTEX_SIZE = 3 + 1 + 2,
    SIZE = 4 * VERTEX_SIZE;
  
  final private static Vector3
    tempA = new Vector3(), tempB = new Vector3();
  
  // meaning of the floats in the vertices array
  public static final int X1 = 0;
  public static final int Y1 = 1;
  public static final int Z1 = 2;
  public static final int C1 = 3;
  public static final int U1 = 4;
  public static final int V1 = 5;
  public static final int X2 = 6;
  public static final int Y2 = 7;
  public static final int Z2 = 8;
  public static final int C2 = 9;
  public static final int U2 = 10;
  public static final int V2 = 11;
  public static final int X3 = 12;
  public static final int Y3 = 13;
  public static final int Z3 = 14;
  public static final int C3 = 15;
  public static final int U3 = 16;
  public static final int V3 = 17;
  public static final int X4 = 18;
  public static final int Y4 = 19;
  public static final int Z4 = 20;
  public static final int C4 = 21;
  public static final int U4 = 22;
  public static final int V4 = 23;
  
  protected static Quaternion rotator = new Quaternion(0, 0, 0, 0);
  protected static final Vector3 X_AXIS = new Vector3(1, 0, 0);
  protected static final Vector3 Y_AXIS = new Vector3(0, 1, 0);
  protected static final Vector3 Z_AXIS = new Vector3(0, 0, 1);
  
  
  final CutoutModel model ;
  protected float[] vertices = new float[SIZE];
  protected Quaternion rotation = new Quaternion();
  protected boolean updated = false;
  
  public Vector3 position = new Vector3();
  public float scale = 1.0f ;
  
  
  
  public Decal(CutoutModel model) {
    this.model = model ;
    this.updateUVs() ;
    this.setColor(Color.WHITE) ;
  }
  
  
  
  /**  Methods for ensuring correct rotation-
    */
  public void setRotationX(float angle){
    rotation.set(X_AXIS, angle);
    updated = false;
  }
  
  public void setRotationY(float angle){
    rotation.set(Y_AXIS, angle);
    updated = false;
  }
  
  public void setRotationZ(float angle){
    rotation.set(Z_AXIS, angle);
    updated = false;
  }
  
  public void rotateX(float angle) {
    rotator.set(X_AXIS, angle);
    rotation.mul(rotator);
    updated = false;
  }
  
  public void rotateY(float angle) {
    rotator.set(Y_AXIS, angle);
    rotation.mul(rotator);
    updated = false;
  }
  
  public void rotateZ(float angle) {
    rotator.set(Z_AXIS, angle);
    rotation.mul(rotator);
    updated = false;
  }
  
  

  /**  Sets the rotation of this decal based on the (normalized) direction and
    *  up vector.
    */
  public void setRotation(Vector3 dir, Vector3 up) {
    tempA.set(up).crs(dir).nor();
    tempB.set(dir).crs(tempA).nor();
    rotation.setFromAxes(
        tempA.x, tempB.x, dir.x,
        tempA.y, tempB.y, dir.y,
        tempA.z, tempB.z, dir.z
    );
    updated = false;
  }
  
  
  public void lookAt(Vector3 position, Vector3 up) {
    tempB.set(position).sub(this.position).nor();
    setRotation(tempB, up);
  }
  
  
  public void update () {
    if (! updated) {
      resetVertices();
      transformVertices();
    }
  }
  
  
  
  /**  Transforms the position component of the vertices using properties such
    *  as position, scale, etc.
    */
  protected void transformVertices() {
    I.say("\nTransforming vertices:") ;
    for (int off = 0 ; off < SIZE ; off += VERTEX_SIZE) {
      tempA.set(
          vertices[X1 + off] * scale,
          vertices[Y1 + off] * scale,
          vertices[Z1 + off]
      );
      rotation.transform(tempA);
      tempA.add(position);
      vertices[X1 + off] = tempA.x;
      vertices[Y1 + off] = tempA.y;
      vertices[Z1 + off] = tempA.z;
      
      I.say("  "+tempA) ;
    }
    updated = true;
  }
  
  
  
  /**  Resets the position components of the vertices array based on the
    *  dimensions (preparation for transformation.)
    */
  protected void resetVertices() {
    float left = 0 - model.dimension.x / 2f;
    float right = left + model.dimension.x;
    float top = model.dimension.y / 2f;
    float bottom = top - model.dimension.y;
    
    // left top
    vertices[X1] = left;
    vertices[Y1] = top;
    vertices[Z1] = 0;
    // right top
    vertices[X2] = right;
    vertices[Y2] = top;
    vertices[Z2] = 0;
    // left bot
    vertices[X3] = left;
    vertices[Y3] = bottom;
    vertices[Z3] = 0;
    // right bot
    vertices[X4] = right;
    vertices[Y4] = bottom;
    vertices[Z4] = 0;
    
    updated = false;
  }
  
  
  
  /**  Re-applies the uv coordinates from the material's texture region to the
    *  uv components of the vertices array.
    */
  protected void updateUVs() {
    TextureRegion tr = model.region;
    // left top
    vertices[U1] = tr.getU();
    vertices[V1] = tr.getV();
    // right top
    vertices[U2] = tr.getU2();
    vertices[V2] = tr.getV();
    // left bot
    vertices[U3] = tr.getU();
    vertices[V3] = tr.getV2();
    // right bot
    vertices[U4] = tr.getU2();
    vertices[V4] = tr.getV2();
  }
  
  
  protected void setColor(Color tint) {
    float color = tint.toFloatBits();
    vertices[C1] = color;
    vertices[C2] = color;
    vertices[C3] = color;
    vertices[C4] = color;
  }
}






/*
public void setColor (float r, float g, float b, float a) {
  int intBits = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
  float color = NumberUtils.intToFloatColor(intBits);
  vertices[C1] = color;
  vertices[C2] = color;
  vertices[C3] = color;
  vertices[C4] = color;
}
//*/
