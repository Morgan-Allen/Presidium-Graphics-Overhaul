

package graphics.cutout ;
import util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g2d.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;



public class CutoutModel {
  
  
  public static final int
    VERTEX_SIZE = 3 + 1 + 2,  //  (position, colour and texture coords.)
    SIZE = 4 * VERTEX_SIZE,   //  (4 vertices, 1 per corner.)
    X0 = 0, Y0 = 1, Z0 = 2,
    C0 = 3, U0 = 4, V0 = 5 ;
  
  final static float
    WHITE_BITS = Color.WHITE.toFloatBits() ;
  
  final static float VERT_PATTERN[] = {
    0, 1, 0,
    1, 1, 0,
    0, 0, 0,
    1, 0, 0
  } ;
  
  
  Texture texture ;
  TextureRegion region ;
  Vector2 offset, dimension ;

  final float vertices[] = new float[SIZE] ;
  
  
  
  public static CutoutModel fromImage(String fileName, int size, int height) {
    
    final CutoutModel model = new CutoutModel() ;
    model.texture = new Texture(Gdx.files.internal(fileName)) ;
    model.region = new TextureRegion(model.texture, 0, 0, 1.0f, 1.0f) ;
    
    final Texture t = model.texture ;
    model.setupDimensions(size, t.getHeight() * 1f / t.getWidth()) ;
    model.setupVertices() ;
    return model ;
  }
  
  
  public static CutoutModel[][] fromImageGrid(
    String fileName, int gridX, int gridY, int size, int height
  ) {
    final CutoutModel grid[][] = new CutoutModel[gridX][gridY] ;
    final Texture texture = new Texture(Gdx.files.internal(fileName)) ;
    final float stepX = 1f / gridX, stepY = 1f / gridY ;
    
    for (Coord c : Visit.grid(0, 0, gridX, gridY, 1)) {
      final CutoutModel model = new CutoutModel() ;
      final float gx = c.x * stepX, gy = c.y * stepY ;
      model.texture = texture ;
      model.region = new TextureRegion(
        texture, gx, gy, gx + stepX, gy + stepY
      ) ;
      model.setupDimensions(size, 1) ;
      model.setupVertices() ;
      grid[c.x][c.y] = model ;
    }
    
    return grid ;
  }
  
  
  private void setupDimensions(int size, float relHigh) {
    //  TODO:  This will need to be based on more precise measurements of the
    //  default camera angle.
    
    final float wide = size * (float) Math.sqrt(2), high = wide * relHigh ;
    dimension = new Vector2(wide, high) ;
    //  1 : 2 ratio for camera elevation, so. Tan-1 (1/2) is the angle you need.
    //  Hell, you can just use pythagoras, in that case:
    //  sqrt((1 * 1) + (2 * 2)).
    float idealBase = wide * 0.5f / ((float) Math.sqrt(5)) ;
    offset = new Vector2(0, (high / 2) - idealBase) ;
  }
  
  
  private void setupVertices() {
    final Quaternion
      rotation = new Quaternion(0, 0, 0, 0),
      onAxis = new Quaternion() ;
    final Vector3 temp = new Vector3() ;
    
    rotation.set(Vector3.Z, 0) ;
    onAxis.set(Vector3.Y, -45) ;
    rotation.mul(onAxis) ;
    onAxis.set(Vector3.X, -30) ;
    rotation.mul(onAxis) ;
    
    final float
      left   = offset.x - (dimension.x / 2f),
      bottom = offset.y - (dimension.y / 2f) ;
    for (int i = 0, p = 0 ; i < vertices.length ; i += VERTEX_SIZE) {
      final float
        x = VERT_PATTERN[p++],
        y = VERT_PATTERN[p++],
        z = VERT_PATTERN[p++] ;
      
      temp.set(
        left   + (dimension.x * x),
        bottom + (dimension.y * y),
        z
      ) ;
      temp.mul(rotation) ;
      vertices[X0 + i] = temp.x ;
      vertices[Y0 + i] = temp.y ;
      vertices[Z0 + i] = temp.z ;
      
      vertices[C0 + i] = WHITE_BITS ;
      vertices[U0 + i] = (region.getU() * (1 - x)) + (region.getU2() * x) ;
      vertices[V0 + i] = (region.getV() * y) + (region.getV2() * (1 - y)) ;
    }
  }
  
}



