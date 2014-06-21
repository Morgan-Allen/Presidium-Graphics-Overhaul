

package stratos.graphics.charts;
import stratos.graphics.common.*;
import stratos.graphics.sfx.Label;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.GdxRuntimeException;

import org.apache.commons.math3.util.FastMath;



//  TODO:  ADD TEXT LABELS
//  TODO:  Figure out a way to model all this in a 'soft' manner.  Preferably
//         with xml.  And you need to be able to click on field objects to
//         gather information, assign missions, et cetera.

//  TODO:  Ideally, you should have a dedicated Viewport for perspective.


public class StarField {
  
  
  final ChartDisplay display;
  
  static class FieldObject {
    TextureRegion texRegion;
    float fieldWide, fieldHigh, offX, offY;
    
    Vec3D coordinates;
    float depth;
    
    public String toString() { return ""+depth; }
  }
  
  
  final List <FieldObject> allObjects;
  private Texture sectorsTex, axisTex;
  private float fieldSize;
  
  private MeshCompile compiled;
  private ShaderProgram shading;
  
  
  
  StarField(ChartDisplay display) {
    this.display = display;
    
    //  NOTE:  The normal attribute here is actually used to store the offset
    //  of a corner from the given decal's coordinate centre (see below),
    //  rather than re-computing vertex positions each frame.
    compiled = new MeshCompile(
      3 + 3 + 1 + 2,                 //number of floats per vertex.
      true, 100,                     //is a quad, max. total quads
      new int[] {0, 1, 2, 1, 2, 3},  //indices for quad vertices
      VertexAttribute.Position(),
      VertexAttribute.Normal(),
      VertexAttribute.Color(),
      VertexAttribute.TexCoords(0)
    );
    
    shading = new ShaderProgram(
      Gdx.files.internal("shaders/stars.vert"),
      Gdx.files.internal("shaders/stars.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog());
    }
    
    final Viewport v = display.rendering.view;
    allObjects = new List <FieldObject> () {
      protected float queuePriority(FieldObject r) {
        return r.depth;
      }
    };
  }
  
  
  void dispose() {
    //  TODO:  THIS MUST BE SCHEDULED
    shading.dispose();
    compiled.dispose();
  }
  
  
  
  /**  Additional setup methods-
    */
  public void setupWith(
    Texture sectorsTex,
    Texture axisTex,
    float fieldSize
  ) {
    this.sectorsTex = sectorsTex;
    this.axisTex = axisTex;
    this.fieldSize = fieldSize;
  }
  
  
  public void addFieldObject(Texture t, Vec3D position) {
    addFieldObject(
      t, 1, 1, 0, 0,
      1, 0, 0,
      position
    );
  }
  
  
  public void addFieldObject(
    Texture t, int gridW, int gridH, int gridX, int gridY,
    float imgScale, float offX, float offY,
    Vec3D position
  ) {
    final FieldObject object = new FieldObject();
    allObjects.add(object);
    
    final float w = 1f / gridW, h = 1f / gridH;
    object.texRegion = new TextureRegion(t);
    object.texRegion.setRegion(
      gridX * w, gridY * h,
      (gridX + 1) * w, (gridY + 1) * h
    );
    object.fieldWide = t.getWidth()  * w * imgScale;
    object.fieldHigh = t.getHeight() * h * imgScale;
    object.offX = offX;
    object.offY = offY;
    
    object.coordinates = position;
  }
  
  
  //  TODO:  Also, A background image- distant stars, a nebula, et cetera.
  
  
  private void renderLabels() {
    //  TODO:  You should really use the Label class functions here.  There
    //  must be some way to adapt 'em?
    /*
    Label.renderPhrase(
      phrase, font, fontScale, colour, screenX, screenY, screenZ, pass, vivid
    );
    //*/
  }
  
  
  
  void render() {
    
    //  TODO:  This transform needs to be more general, so that it can co-exist
    //  with the planet display.
    //  TODO:  The rotation vector will have to be applied to the coordinates,
    //  or the depth-sorting won't work correctly!
    
    //final float time = Rendering.activeTime();
    final Matrix4 rotation = new Matrix4().idt();
    //rotation.rot(Vector3.Y, (float) FastMath.toRadians(30 * time));

    shading.begin();
    shading.setUniformi("u_texture", 0);
    shading.setUniformMatrix("u_rotation", rotation);
    shading.setUniformMatrix("u_camera", display.rendering.camera().combined);
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    glDepthMask(false);
    renderSectorsAndAxes();
    //glDepthMask(true);
    
    
    for (FieldObject o : allObjects) {
      o.depth = 0 - display.rendering.view.screenDepth(o.coordinates);
    }
    allObjects.queueSort();

    
    Texture lastTex = null;
    float piece[] = new float[compiled.vertexSize];
    final float SW = Gdx.graphics.getWidth(), SH = Gdx.graphics.getHeight();
    final Vector3 c = new Vector3();
    
    for (FieldObject object : allObjects) {
      
      Viewport.worldToGL(object.coordinates, c);
      //if (c.y > 0) { renderSectorsAndAxes(); lastTex = null; }
      
      final TextureRegion r = object.texRegion;
      final Texture t = r.getTexture();
      
      if (t != lastTex) {
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        t.bind(0);
        lastTex = t;
      }
      
      if (compiled.meshFull() || t != lastTex) {
        compiled.renderWithShader(shading, true);
      }
      
      final float
        x = object.offX / SW,
        y = object.offY / SH,
        w = object.fieldWide / SW,
        h = object.fieldHigh / SH;
      
      final Colour fade = Colour.WHITE;
      appendVertex(piece, c, x - w, y - h, fade, r.u , r.v2);
      appendVertex(piece, c, x - w, y + h, fade, r.u , r.v );
      appendVertex(piece, c, x + w, y - h, fade, r.u2, r.v2);
      appendVertex(piece, c, x + w, y + h, fade, r.u2, r.v );
    }
    compiled.renderWithShader(shading, true);
    
    shading.end();
  }
  

  
  private void renderSectorsAndAxes() {
    
    float piece[] = new float[compiled.vertexSize];
    float a = fieldSize / 2;
    final Colour fade = Colour.transparency(0.33f);
    
    /*
    final Vector3 c = new Vector3();
    appendVertex(piece, c, -1, -1, fade, 0, 1);
    appendVertex(piece, c, -1,  1, fade, 0, 0);
    appendVertex(piece, c,  1, -1, fade, 1, 1);
    appendVertex(piece, c,  1,  1, fade, 1, 0);
    //*/

    appendVertex(piece, new Vector3(-a, 0, -a), 0, 0, fade, 0, 1);
    appendVertex(piece, new Vector3(-a, 0,  a), 0, 0, fade, 0, 0);
    appendVertex(piece, new Vector3( a, 0, -a), 0, 0, fade, 1, 1);
    appendVertex(piece, new Vector3( a, 0,  a), 0, 0, fade, 1, 0);
    
    sectorsTex.bind(0);
    compiled.renderWithShader(shading, true);
    
    /*
    a *= 2;
    
    appendVertex(piece, new Vector3(-a, -a, 0), 0, 0, fade, 0, 1);
    appendVertex(piece, new Vector3(-a,  a, 0), 0, 0, fade, 0, 0);
    appendVertex(piece, new Vector3( a, -a, 0), 0, 0, fade, 1, 1);
    appendVertex(piece, new Vector3( a,  a, 0), 0, 0, fade, 1, 0);
    
    appendVertex(piece, new Vector3(0, -a, -a), 0, 0, fade, 0, 1);
    appendVertex(piece, new Vector3(0, -a,  a), 0, 0, fade, 0, 0);
    appendVertex(piece, new Vector3(0,  a, -a), 0, 0, fade, 1, 1);
    appendVertex(piece, new Vector3(0,  a,  a), 0, 0, fade, 1, 0);
    
    axisTex.bind(0);
    compiled.renderWithShader(shading, true);
    //*/
  }
  
  
  
  private void appendVertex(
    float piece[],
    Vector3 pos, float offX, float offY,
    Colour c, float tu, float tv
  ) {
    int v = 0;
    piece[v++] = pos.x;
    piece[v++] = pos.y;
    piece[v++] = pos.z;
    //  Corner offset-
    piece[v++] = offX;
    piece[v++] = offY;
    piece[v++] = 0;
    //  Color and texture coordinates-
    piece[v++] = c.bitValue;
    piece[v++] = tu;
    piece[v++] = tv;
    compiled.appendVertex(piece);
  }
}






