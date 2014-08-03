/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.terrain;
//import stratos.start.Disposal;
import stratos.graphics.common.*;
import stratos.start.Assets;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.graphics.Texture.TextureFilter;



//  TODO:  Have the minimap refresh itself every second or so, and simply fade
//  in the new version on top of the old?  Something like that.  If you wanted,
//  you could do some kind of fancy burn-in or flicker transition-effect.
//
//  TODO:  You need to be able to present the ambience map here.

//  TODO:  THIS HAS TO BE DISPOSED OF AFTER THE WORLD IS QUIT FROM



public class Minimap extends Assets.Loadable {
  
  
  private Texture mapImage;
  private Mesh mapMesh;
  private ShaderProgram shading;
  
  
  public Minimap() {
    super("MINIMAP", Minimap.class, true);
  }
  
  
  public void updateTexture(int texSize, int RGBA[][]) {
    final Pixmap drawnTo = new Pixmap(texSize, texSize, Pixmap.Format.RGBA8888);
    Pixmap.setBlending(Pixmap.Blending.None);
    for (Coord c : Visit.grid(0, 0, texSize, texSize, 1)) {
      drawnTo.drawPixel(c.x, c.y, RGBA[c.x][c.y]);
      //drawnTo.drawPixel(c.x, c.y, 0xffffffff);
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
  
  
  public void updateGeometry(Box2D bound) {
    
    //  Initialise the mesh if required-
    if (mapMesh == null) {
      mapMesh = new Mesh(
        Mesh.VertexDataType.VertexArray,
        false, 4, 6,
        VertexAttribute.Position(),
        VertexAttribute.Color(),
        VertexAttribute.TexCoords(0)
      );
      mapMesh.setIndices(new short[] {0, 1, 2, 2, 3, 0 });
      shading = new ShaderProgram(
        Gdx.files.internal("shaders/minimap.vert"),
        Gdx.files.internal("shaders/minimap.frag")
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
      Colour.WHITE.bitValue, 0, 0,
      //  top corner-
      x + (w / 2), y + h, 0,
      Colour.WHITE.bitValue, 1, 0,
      //  right corner-
      x + w, y + (h / 2), 0,
      Colour.WHITE.bitValue, 1, 1,
      //  bottom corner-
      x + (w / 2), y, 0,
      Colour.WHITE.bitValue, 0, 1
    };
    mapMesh.setVertices(mapGeom);
  }
  
  
  protected void loadAsset() {
    updateGeometry(null);
  }
  
  
  public boolean isLoaded() {
    return mapMesh != null;
  }
  
  
  protected void disposeAsset() {
    mapImage.dispose();
    mapMesh.dispose();
    shading.dispose();
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Coord getMapPosition(final Vector2 pos, Box2D bound, int mapSize) {
    final float
      cX = (pos.x -  bound.xpos()) / bound.xdim(),
      cY = ((bound.ypos() + (bound.ydim() * 0.5f)) - pos.y) / bound.ydim();
    return new Coord(
      (int) ((cX - cY) * mapSize),
      (int) ((cY + cX) * mapSize)
    );
  }
  
  
  public void renderWith(FogOverlay fogApplied) {
    
    final Matrix4 screenMat = new Matrix4();
    screenMat.setToOrtho2D(
      0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()
    );
    
    shading.begin();
    shading.setUniformMatrix("u_ortho", screenMat);
    shading.setUniformi("u_texture", 0);
    
    if (fogApplied != null) {
      fogApplied.applyToMinimap(shading);
      shading.setUniformi("u_fogFlag", GL_TRUE);
    }
    else shading.setUniformi("u_fogFlag", GL_FALSE);
    
    mapImage.bind(0);
    mapMesh.render(shading, GL20.GL_TRIANGLES);
    shading.end();
  }
}








