

package src.graphics.cutout ;
import src.graphics.common.*;
import src.graphics.common.Sprite;
import src.util.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g2d.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;



public class CutoutModel extends ModelAsset {
  
  
  public static final int
    VERTEX_SIZE = 3 + 1 + 2,  //  (position, colour and texture coords.)
    SIZE = 4 * VERTEX_SIZE,   //  (4 vertices, 1 per corner.)
    X0 = 0, Y0 = 1, Z0 = 2,
    C0 = 3, U0 = 4, V0 = 5;
  
  
  final static float VERT_PATTERN[] = {
    0, 1, 0,
    1, 1, 0,
    0, 0, 0,
    1, 0, 0
  } ;
  
  private String fileName;
  private Box2D window;
  private float size;
  private boolean loaded = false;
  
  Texture texture ;
  TextureRegion region ;
  Vector2 offset, dimension ;

  final float vertices[] = new float[SIZE];
  
  
  
  private CutoutModel(
    String fileName, Class modelClass, Box2D window, float size
  ) {
    super(fileName+""+window, modelClass);
    this.fileName = fileName;
    this.window = window;
    this.size = size;
    Assets.registerForLoading(this);
  }
  
  
  protected void loadAsset() {
    texture = Assets.getTexture(fileName);
    region = new TextureRegion(
      texture,
      window.xpos(), window.ypos(),
      window.xmax(), window.ymax()
    );
    final Texture t = texture;
    setupDimensions(size, t.getHeight() * 1f / t.getWidth());
    setupVertices();
    loaded = true;
  }
  
  
  public boolean isLoaded() {
    return loaded;
  }
  
  
  protected void disposeAsset() {
    //  TODO:  The texture might not be unique to this model!  Check if already
    //  disposed of!
    texture.dispose();
  }
  
  
  public Sprite makeSprite() {
    if (! loaded) I.complain("CANNOT CREATE SPRITE UNTIL LOADED!") ;
    return new CutoutSprite(this);
  }
  
  
  public static CutoutModel fromImage(
    String fileName, Class sourceClass, float size, float height
  ) {
    final Box2D window = new Box2D().set(0, 0, 1, 1);
    return new CutoutModel(fileName, sourceClass, window, size);
  }
  
  
  public static CutoutModel[] fromImages(
    String path, Class sourceClass, float size, float height,
    String... files
  ) {
    final CutoutModel models[] = new CutoutModel[files.length];
    for (int i = 0 ; i < files.length ; i++) {
      models[i] = fromImage(path+files[i], sourceClass, size, height);
    }
    return models;
  }
  
  
  public static CutoutModel[][] fromImageGrid(
    String fileName, Class sourceClass,
    int gridX, int gridY, float size, float height
  ) {
    final CutoutModel grid[][] = new CutoutModel[gridX][gridY] ;
    final float stepX = 1f / gridX, stepY = 1f / gridY ;
    for (Coord c : Visit.grid(0, 0, gridX, gridY, 1)) {
      final float gx = c.x * stepX, gy = c.y * stepY;
      final Box2D window = new Box2D().set(gx, gy, stepX, stepY);
      
      grid[c.x][gridY - (c.y + 1)] = new CutoutModel(
        fileName, sourceClass, window, size
      );
    }
    return grid ;
  }
  
  
  private void setupDimensions(float size, float relHigh) {
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
      
      vertices[C0 + i] = Sprite.WHITE_BITS ;
      vertices[U0 + i] = (region.getU() * (1 - x)) + (region.getU2() * x) ;
      vertices[V0 + i] = (region.getV() * y) + (region.getV2() * (1 - y)) ;
    }
  }
  
}



