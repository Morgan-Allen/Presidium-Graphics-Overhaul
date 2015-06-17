/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.util.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;



public class CutoutModel extends ModelAsset {
  
  private static boolean
    verbose = false;
  private static String
    verboseModel = "NONE";
  
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
  };
  final public static short VERT_INDICES[] = {
    0, 2, 1, 1, 2, 3
  };
  final static Vector3 FLAT_VERTS[] = {
    new Vector3(0, 1, 0),
    new Vector3(1, 1, 0),
    new Vector3(0, 0, 0),
    new Vector3(1, 0, 0),
  };
  final static int
    BOX_X = 0, BOX_Y = 1, BOX_Z = 1;
  final static Vector3 BOX_VERTS[] = {
    
    //  Top face (z at 1)-
    new Vector3(0, 1, BOX_Z),
    new Vector3(1, 1, BOX_Z),
    new Vector3(0, 0, BOX_Z),
    new Vector3(1, 0, BOX_Z),
    
    //  South face (x at 0)-
    new Vector3(BOX_X, 0, 1),
    new Vector3(BOX_X, 1, 1),
    new Vector3(BOX_X, 0, 0),
    new Vector3(BOX_X, 1, 0),
    
    //  East face (y at 1)-
    new Vector3(0, BOX_Y, 1),
    new Vector3(1, BOX_Y, 1),
    new Vector3(0, BOX_Y, 0),
    new Vector3(1, BOX_Y, 0)
  };
  
  
  final String fileName;
  final Box2D window;
  final float size, high;
  final boolean splat;
  
  protected Texture texture;
  protected Texture lightSkin;
  
  private TextureRegion region;
  private float maxScreenWide, maxScreenHigh, minScreenHigh, imgScreenHigh;
  
  final float vertices[] = new float[SIZE];
  protected float allFaces[][];
  final Table <String, Integer> faceLookup = new Table();
  
  
  
  private CutoutModel(
    String fileName, Class modelClass, Box2D window,
    float size, float high, boolean splat
  ) {
    super(fileName+"_"+window, modelClass);
    this.fileName = fileName;
    this.window   = window  ;
    this.size     = size    ;
    this.high     = high    ;
    this.splat    = splat   ;
  }
  
  
  protected State loadAsset() {
    texture = ImageAsset.getTexture(fileName);
    region = new TextureRegion(
      texture,
      window.xpos(), window.ypos(),
      window.xmax(), window.ymax()
    );
    final Texture t = texture;
    final float relHeight =
      (t.getHeight() * window.ydim()) /
      (t.getWidth () * window.xdim());
    setupDimensions(size, relHeight);
    setupVertices();
    
    String litName = fileName.substring(0, fileName.length() - 4);
    litName+="_lights.png";
    if (assetExists(litName)) lightSkin = ImageAsset.getTexture(litName);
    return state = State.LOADED;
  }
  
  
  protected State disposeAsset() {
    texture.dispose();
    if (lightSkin != null) lightSkin.dispose();
    return state = State.DISPOSED;
  }
  
  
  public CutoutSprite makeSprite() {
    if (! stateLoaded()) {
      I.complain("CANNOT CREATE SPRITE UNTIL LOADED: "+fileName);
    }
    return new CutoutSprite(this, 0);
  }
  
  
  public static CutoutModel fromImage(
    Class sourceClass, String fileName, float size, float height
  ) {
    final Box2D window = new Box2D().set(0, 0, 1, 1);
    return new CutoutModel(fileName, sourceClass, window, size, height, false);
  }
  
  
  public static CutoutModel fromSplatImage(
    Class sourceClass, String fileName, float size
  ) {
    final Box2D window = new Box2D().set(0, 0, 1, 1);
    return new CutoutModel(fileName, sourceClass, window, size, 0, true);
  }
  
  
  public static CutoutModel[] fromImages(
    Class sourceClass, String path, float size, float height, boolean splat,
    String... files
  ) {
    final CutoutModel models[] = new CutoutModel[files.length];
    for (int i = 0; i < files.length; i++) {
      final String fileName = path+files[i];
      final Box2D window = new Box2D().set(0, 0, 1, 1);
      models[i] = new CutoutModel(
        fileName, sourceClass, window, size, height, splat
      );
    }
    return models;
  }
  
  
  public static CutoutModel[][] fromImageGrid(
    Class sourceClass, String fileName,
    int gridX, int gridY, float size, float height, boolean splat
  ) {
    final CutoutModel grid[][] = new CutoutModel[gridX][gridY];
    final float stepX = 1f / gridX, stepY = 1f / gridY;
    for (Coord c : Visit.grid(0, 0, gridX, gridY, 1)) {
      final float gx = c.x * stepX, gy = c.y * stepY;
      final Box2D window = new Box2D().set(gx, gy, stepX, stepY);
      grid[c.x][gridY - (c.y + 1)] = new CutoutModel(
        fileName, sourceClass, window, size, height, splat
      );
    }
    return grid;
  }
  
  
  
  /**  Vertex-manufacture methods during initial setup-
    */
  //      A
  //     /-\---/-\
  //   C/ D \ /   \E
  //    \   / \   /
  //     \-/---\-/
  //      B
  //  This might or might not be hugely helpful, but if you tilt your head to
  //  the right so that X/Y axes are flipped and imagine this as an isometric
  //  box framing the image contents, then:
  //
  //    maxScreenWide   = A to B
  //    maxScreenHigh   = D to E
  //    minScreenHigh   = D to C, and
  //    imageScreenHigh = actual height of image
  //  -all measured in world-units, *but* relative to screen coordinates.  See
  //   below.
  
  private void setupDimensions(float size, float relHigh) {
    final float
      viewAngle = Nums.toRadians(Viewport.DEFAULT_ELEVATE),
      wide      = size * Nums.ROOT2;
    maxScreenWide   =  wide;
    imgScreenHigh =  wide * relHigh;
    maxScreenHigh   = (wide * Nums.sin(viewAngle)) / 2;
    minScreenHigh   = 0 - maxScreenHigh;
    maxScreenHigh   += high * Nums.cos(viewAngle);
  }
  
  
  private void setupVertices() {
    //
    //  For the default-sprite geometry, we do a naive translation of some
    //  rectangular vertex-points, keeping the straightforward UV but
    //  translating the screen-coordinates into world-space.  The effect is
    //  something like a 2D cardboard-cutout, propped up at an angle on-stage.
    //
    final Vector3 temp = new Vector3();
    final Batch <float[]> faces = new Batch();
    
    for (int i = 0, p = 0; i < vertices.length; i += VERTEX_SIZE) {
      final float
        x = VERT_PATTERN[p++],
        y = VERT_PATTERN[p++],
        z = VERT_PATTERN[p++];
      temp.set(
        maxScreenWide * (x - 0.5f), (imgScreenHigh * y) + minScreenHigh, z
      );
      Viewport.isometricInverted(temp, temp);
      vertices[X0 + i] = temp.x;
      vertices[Y0 + i] = temp.y;
      vertices[Z0 + i] = temp.z;
      vertices[C0 + i] = Sprite.WHITE_BITS;
      vertices[U0 + i] = (region.getU() * (1 - x)) + (region.getU2() * x);
      vertices[V0 + i] = (region.getV() * y) + (region.getV2() * (1 - y));
    }
    faces.add(vertices);
    //
    //  As a method of facilitating certain construction-animations, we also
    //  'dice up' the cutout, something like the apparent facets of a Rubik's
    //  Cube.
    //
    //  In this case, we preserve the simple geometry, but use the inverse
    //  transform to get the UV to line up with the isometric viewpoint.
    //
    final boolean topOnly = (int) high == 0;
    for (Coord c : Visit.grid(0, 0, (int) size, (int) size, 1)) {
      if (topOnly) {
        addFace(c.x, c.y, 0, FLAT_VERTS, faces);
      }
      else for (int h = (int) high; h-- > 0;) {
        addFace(c.x, c.y, h, BOX_VERTS, faces);
      }
    }
    this.allFaces = faces.toArray(float[].class);
  }
  
  
  private void addFace(
    int x, int y, int z, Vector3 baseVerts[], Batch <float[]> faces
  ) {
    final float vertices[] = new float[baseVerts.length * VERTEX_SIZE];
    final Vector3 temp = new Vector3();
    float maxHigh = maxScreenHigh - minScreenHigh;
    maxHigh *= imgScreenHigh / maxHigh;
    //
    //  And finally, we translate each of the interior points accordingly-
    int i = 0;
    for (Vector3 v : baseVerts) {
      vertices[X0 + i] = temp.x = (x + v.x - (size / 2));
      vertices[Y0 + i] = temp.y = (z + v.z);
      vertices[Z0 + i] = temp.z = (y + v.y - (size / 2));
      Viewport.isometricRotation(temp, temp);
      vertices[C0 + i] = Sprite.WHITE_BITS;
      vertices[U0 + i] = 0 + ((temp.x / maxScreenWide) + 0.5f   );
      vertices[V0 + i] = 1 - ((temp.y - minScreenHigh) / maxHigh);
      i += VERTEX_SIZE;
    }
    //
    //  We then cache the face with a unique key for easy access (see below.)
    final String key = x+"_"+y+"_"+z;
    
    final boolean report = verbose && fileName.endsWith(verboseModel);
    if (report) {
      I.say("\nAdding face: "+key);
      I.say("  Vertices length: "+vertices.length);
    }
    faceLookup.put(key, faces.size());
    faces.add(vertices);
  }
  
  
  public CutoutSprite facingSprite(int x, int y, int z) {
    final String key = x+"_"+y+"_"+Nums.max(0, z);
    final Integer index = faceLookup.get(key);
    if (index == null || index < 1) return null;
    return new CutoutSprite(this, index);
  }
  
  
  public String fileName() {
    return this.fileName;
  }
}









