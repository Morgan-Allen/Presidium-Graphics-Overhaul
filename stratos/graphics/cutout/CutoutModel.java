

package stratos.graphics.cutout ;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.* ;



public class CutoutModel extends ModelAsset {
  
  
  public static final int
    VERTEX_SIZE = 3 + 1 + 2,  //  (position, colour and texture coords.)
    SIZE = 4 * VERTEX_SIZE,   //  (4 vertices, 1 per corner.)
    X0 = 0, Y0 = 1, Z0 = 2,
    C0 = 3, U0 = 4, V0 = 5;
  
  
  final public static float VERT_PATTERN[] = {
    0, 1, 0,
    1, 1, 0,
    0, 0, 0,
    1, 0, 0
  } ;
  final public static short VERT_INDICES[] = {
    0, 2, 1, 1, 2, 3
  };
  
  private String fileName;
  private Box2D window;
  private float size;
  private boolean loaded = false;
  
  Texture texture;
  Texture lightSkin;//  TODO:  USE THIS
  
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
  }
  
  
  protected void loadAsset() {
    texture = ImageAsset.getTexture(fileName);
    region = new TextureRegion(
      texture,
      window.xpos(), window.ypos(),
      window.xmax(), window.ymax()
    );
    final Texture t = texture;
    setupDimensions(size, t.getHeight() * 1f / t.getWidth());
    setupVertices();
    
    String litName = fileName.substring(0, fileName.length() - 4);
    litName+="_lights.png";
    if (Assets.exists(litName)) {
      lightSkin = ImageAsset.getTexture(litName);
    }
    
    loaded = true;
  }
  
  
  public boolean isLoaded() {
    return loaded;
  }
  
  
  protected void disposeAsset() {
    texture.dispose();
    if (lightSkin != null) lightSkin.dispose();
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
    final float
      angle = (float) Math.toRadians(Viewport.DEFAULT_ELEVATE),
      incidence = (float) Math.sin(angle);
    
    final float wide = size * (float) Math.sqrt(2), high = wide * relHigh;
    dimension = new Vector2(wide, high);
    final float idealBase = wide * 0.5f * incidence;
    offset = new Vector2(0, (high / 2) - idealBase);
  }
  
  
  private void setupVertices() {
    final Quaternion
      rotation = new Quaternion(0, 0, 0, 0),
      onAxis   = new Quaternion();
    final Vector3 temp = new Vector3();
    
    rotation.set(Vector3.Z, 0);
    onAxis.set(Vector3.Y, -45);
    rotation.mul(onAxis);
    onAxis.set(Vector3.X, 0 - Viewport.DEFAULT_ELEVATE);
    rotation.mul(onAxis);
    
    final float
      left   = offset.x - (dimension.x / 2f),
      bottom = offset.y - (dimension.y / 2f);
    for (int i = 0, p = 0; i < vertices.length; i += VERTEX_SIZE) {
      final float
        x = VERT_PATTERN[p++],
        y = VERT_PATTERN[p++],
        z = VERT_PATTERN[p++];
      temp.set(left + (dimension.x * x), bottom + (dimension.y * y), z);
      temp.mul(rotation);
      vertices[X0 + i] = temp.x;
      vertices[Y0 + i] = temp.y;
      vertices[Z0 + i] = temp.z;

      vertices[C0 + i] = Sprite.WHITE_BITS;
      vertices[U0 + i] = (region.getU() * (1 - x)) + (region.getU2() * x);
      vertices[V0 + i] = (region.getV() * y) + (region.getV2() * (1 - y));
    }
  }
  
}



