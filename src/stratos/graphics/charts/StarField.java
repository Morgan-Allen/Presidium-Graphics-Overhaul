

package stratos.graphics.charts;
import stratos.graphics.common.*;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.apache.commons.math3.util.FastMath;



//  TODO:  Restore axis display.
//  TODO:  Figure out a way to model all this in a 'soft' manner.  Preferably
//         with xml.  And you need to be able to click on field objects to
//         gather information, assign missions, et cetera.

/*
final ImageAsset
  STARFIELD_TEX = ImageAsset.fromImage(
    "media/Charts/stellar_objects.png", StarField.class
  ),
  AXIS_TEX = ImageAsset.fromImage(
    "media/Charts/sky_axis.png", StarField.class
  );
//*/




public class StarField {
  
  
  final ChartDisplay display;
  
  static class FieldObject {
    TextureRegion texRegion;
    float fieldWide, fieldHigh, offX, offY;
    
    Vec3D coordinates;
  }
  
  final List <FieldObject> allObjects;// = new List <FieldObject> ();
  
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
        return v.screenDepth(r.coordinates);
      }
    };
  }
  
  
  void dispose() {
    //  TODO:  THIS MUST BE SCHEDULED
    shading.dispose();
    compiled.dispose();
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
  
  
  
  
  void render() {
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    glDepthMask(false);
    allObjects.queueSort();
    
    //  TODO:  This transform needs to be more general, so that it can co-exist
    //  with the planet display.
    //  TODO:  The rotation vector will have to be applied to the coordinates,
    //  or the depth-sorting won't work correctly!
    
    final float time = Rendering.activeTime();
    final Matrix4 rotation = new Matrix4().idt();
    //rotation.rot(Vector3.Y, (float) FastMath.toRadians(30 * time));

    shading.begin();
    shading.setUniformi("u_texture", 0);
    shading.setUniformMatrix("u_rotation", rotation);
    shading.setUniformMatrix("u_camera", display.rendering.camera().combined);
    
    Texture lastTex = null;
    float piece[] = new float[compiled.pieceSize];
    final float SW = Gdx.graphics.getWidth(), SH = Gdx.graphics.getHeight();
    final Vector3 c = new Vector3();
    
    for (FieldObject object : allObjects) {
      
      Viewport.worldToGL(object.coordinates, c);
      final TextureRegion r = object.texRegion;
      final Texture t = r.getTexture();
      final float w = object.fieldWide, h = object.fieldHigh;
      
      if (t != lastTex) { t.bind(0); lastTex = t; }
      
      if (compiled.meshFull() || t != lastTex) {
        compiled.renderWithShader(shading, true);
      }
      
      for (int n = 0, v = 0; n < 4; n++) {
        final boolean up = (n % 2) > 0, right = n > 1;
        //  Position-
        piece[v++] = c.x;
        piece[v++] = c.y;
        piece[v++] = c.z;
        //  Corner offset-
        piece[v++] = (object.offX + (w * (right ? -1 : 1))) / SW;
        piece[v++] = (object.offY + (h * (up    ? -1 : 1))) / SH;
        piece[v++] = 0;
        //  Color and texture coordinates-
        piece[v++] = Colour.WHITE.bitValue;
        piece[v++] = right ? r.u : r.u2;
        piece[v++] = up    ? r.v2 : r.v;
      }
      compiled.appendPiece(piece);
    }
    compiled.renderWithShader(shading, true);
    
    shading.end();
  }
}




