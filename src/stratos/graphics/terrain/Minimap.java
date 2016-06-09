/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;
import stratos.start.*;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.graphics.Texture.TextureFilter;



public class Minimap extends Assets.Loadable {
  
  
  private Texture mapImage;
  private Mesh mapMesh;
  private ShaderProgram shading;
  private Box2D cameraBox = new Box2D();
  
  
  public Minimap() {
    super("MINIMAP", Minimap.class, true);
  }
  
  
  public void updateTexture(int texSize, int RGBA[][]) {
    if (! stateLoaded()) return;
    
    final Pixmap drawnTo = new Pixmap(
      texSize, texSize, Pixmap.Format.RGBA8888
    );
    Pixmap.setBlending(Pixmap.Blending.None);
    for (Coord c : Visit.grid(0, 0, texSize, texSize, 1)) {
      drawnTo.drawPixel(c.x, c.y, RGBA[c.x][c.y]);
    }
    if (mapImage == null) {
      mapImage = new Texture(drawnTo);
      mapImage.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    }
    else mapImage.load(new PixmapTextureData(
      drawnTo, Pixmap.Format.RGBA8888, false, false
    ));
    drawnTo.dispose();
  }
  
  
  public void updateCameraBox(Box2D box, int screenX, int screenY) {
    this.cameraBox = box;
  }
  
  
  public void updateGeometry(Box2D bound) {
    if (! stateLoaded()) return;
    
    //  Initialise the mesh if required-
    if (mapMesh == null) {
      mapMesh = new Mesh(
        Mesh.VertexDataType.VertexArray,
        false, 4, 6,
        VertexAttribute.Position(),
        VertexAttribute.ColorPacked(),
        VertexAttribute.TexCoords(0)
      );
      mapMesh.setIndices(new short[] {0, 1, 2, 2, 3, 0 });
      shading = new ShaderProgram(
        Gdx.files.internal("stratos/graphics/shaders/minimap.vert"),
        Gdx.files.internal("stratos/graphics/shaders/minimap.frag")
      );
      if (! shading.isCompiled()) {
        throw new GdxRuntimeException("\n"+shading.getLog());
      }
    }
    if (bound == null) return;
    
    //  You draw a diamond-shaped area around the four points-
    final float
      w = bound.xdim(), h = bound.ydim(),
      x = bound.xpos(), y = bound.ypos();
    final float mapGeom[] = new float[] {
      //  left corner-
      x, y + (h / 2), 0,
      Colour.WHITE.floatBits, 0, 0,
      //  top corner-
      x + (w / 2), y + h, 0,
      Colour.WHITE.floatBits, 1, 0,
      //  right corner-
      x + w, y + (h / 2), 0,
      Colour.WHITE.floatBits, 1, 1,
      //  bottom corner-
      x + (w / 2), y, 0,
      Colour.WHITE.floatBits, 0, 1
    };
    mapMesh.setVertices(mapGeom);
  }
  
  
  protected State loadAsset() {
    updateGeometry(null);
    return state = State.LOADED;
  }
  
  
  protected State disposeAsset() {
    if (! stateLoaded()) return state = State.ERROR;
    if (mapImage != null) mapImage.dispose();
    if (mapMesh  != null) mapMesh .dispose();
    if (shading  != null) shading .dispose();
    return state = State.DISPOSED;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Coord getMapPosition(final Vector2 pos, Box2D bound, int mapSize) {
    //  TODO:  try putting together a Matrix transform for this instead!
    //
    //  Returns the position on the physical map from a given screen point.
    final float
      origX = bound.xpos(),
      origY = bound.ypos() + (bound.ydim() * 0.5f),
      cX = (pos.x - origX) / bound.xdim(),
      cY = (origY - pos.y) / bound.ydim();
    return new Coord(
      (int) ((cX - cY) * mapSize),
      (int) ((cY + cX) * mapSize)
    );
  }
  
  
  public Coord getScreenPosition(int x, int y, int mapSize, Box2D bound) {
    //
    //  Returns the on-screen position of a given physical world-point.
    float cX = x * 1f / mapSize, cY = y * 1f / mapSize;
    float sX = 0, sY = 0.5f;
    sX += (cY * 0.5f) + (cX * 0.5f);
    sY += (cX * 0.5f) - (cY * 0.5f);
    
    sX = bound.xpos() + (sX * bound.xdim());
    sY = bound.ypos() + (sY * bound.ydim());
    return new Coord((int) sX, (int) sY);
  }
  
  
  public void renderWith(FogOverlay fogApplied) {
    if (! stateLoaded()) {
      ///I.say("RENDERING CALL WHEN DISPOSED");
      return;
    }
    
    final Matrix4 screenMat = new Matrix4();
    screenMat.setToOrtho2D(
      0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()
    );
    
    shading.begin();
    shading.setUniformMatrix("u_ortho", screenMat);
    shading.setUniformi("u_texture", 0);
    
    final Box2D CB = cameraBox;
    shading.setUniformf("u_box_lower_corner", CB.xpos(), CB.ypos());
    shading.setUniformf("u_box_upper_corner", CB.xmax(), CB.ymax());
    
    if (fogApplied != null) {
      fogApplied.applyToMinimap(shading);
      shading.setUniformi("u_fogFlag", GL20.GL_TRUE);
    }
    else {
      shading.setUniformi("u_fogFlag", GL20.GL_FALSE);
    }
    
    mapImage.bind(0);
    mapMesh.render(shading, GL20.GL_TRIANGLES);
    shading.end();
  }
}








