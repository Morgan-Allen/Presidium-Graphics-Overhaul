

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



public class StarField {
  
  /*
  final ImageAsset
    STARFIELD_TEX = ImageAsset.fromImage(
      "media/Charts/stellar_objects.png", StarField.class
    ),
    AXIS_TEX = ImageAsset.fromImage(
      "media/Charts/sky_axis.png", StarField.class
    );
  //*/
  
  final ChartDisplay display;
  
  static class FieldObject {
    TextureRegion texRegion;
    float fieldWide, fieldHigh;
    
    Vec3D coordinates;
  }
  
  final List <FieldObject> allObjects = new List <FieldObject> ();
  
  private MeshCompile compiled;
  private ShaderProgram shading;
  
  
  
  StarField(ChartDisplay display) {
    this.display = display;
    
    //  NOTE:  The normal attribute here is actually used to store the offset
    //  of a corner from the given decal's coordinate centre (see below),
    //  rather than re-computing vertex positions each frame.
    compiled = new MeshCompile(
      3 + 3 + 1 + 2,                   //number of floats per vertex.
      true, 100,                       //is a quad, max. total quads
      new int[] { 0, 1, 2, 1, 2, 3 },  //indices for quad vertices
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
  }
  
  
  public void addFieldObject(Texture t, Vec3D position) {
    addFieldObject(t, 1, 1, 0, 0, position);
  }
  
  
  public void addFieldObject(
    Texture t, int gridW, int gridH, int offX, int offY,
    Vec3D position
  ) {
    final FieldObject object = new FieldObject();
    allObjects.add(object);
    
    final float w = 1f / gridW, h = 1f / gridH;
    object.texRegion = new TextureRegion(t);
    object.texRegion.setRegion(
      offX * w, offY * h,
      (offX + 1) * w, (offY + 1) * h
    );
    object.fieldWide = t.getWidth()  * w;
    object.fieldHigh = t.getHeight() * h;
    
    object.coordinates = position;
  }
  
  
  
  private Mesh testMesh = null;
  
  
  void testRender() {
    
    if (testMesh == null) {
      testMesh = new Mesh(
        Mesh.VertexDataType.VertexArray,
        false,
        4, 6,
        VertexAttribute.Position(),
        VertexAttribute.Normal(),
        VertexAttribute.Color(),
        VertexAttribute.TexCoords(0)
      );
      
      testMesh.setVertices(new float[] {
        1, 1, 0,   -5, -5, 0,   0,   0, 0,
        1, 1, 0,   -5,  5, 0,   0,   0, 1,
        1, 1, 0,    5, -5, 0,   0,   1, 0,
        1, 1, 0,    5,  5, 0,   0,   1, 1,
      });
      testMesh.setIndices(new short[] {0, 1, 2, 1, 2, 3});
    }
    
    final float time = Rendering.activeTime();
    final Matrix4 rotation = new Matrix4().idt();
    rotation.rot(Vector3.Y, (float) FastMath.toRadians(30 * time));

    shading.begin();
    shading.setUniformi("u_texture", 0);
    shading.setUniformMatrix("u_rotation", rotation);
    shading.setUniformMatrix("u_camera", display.rendering.camera().combined);
    
    Texture tex = allObjects.first().texRegion.getTexture();
    tex.bind(0);
    
    testMesh.render(shading, GL11.GL_TRIANGLES, 0, 6);
    
    shading.end();
  }
  
  
  
  void render() {
    if (true) { testRender(); return; }
    
    //  TODO:  All you need to bind here is the texture, the camera, and the
    //  field transform.  (Plus the index of the selected decal(?))
    
    //  TODO:  Set blending here to GL_ADD/GL_ONE, so you get a cumulative
    //  'glow' effect...
    
    final float time = Rendering.activeTime();
    final Matrix4 rotation = new Matrix4().idt();
    //rotation.rot(Vector3.Y, (float) FastMath.toRadians(30 * time));
    
    //glDisable(GL10.GL_NORMALIZE);
    shading.begin();
    shading.setUniformi("u_texture", 0);
    shading.setUniformMatrix("u_rotation", rotation);
    shading.setUniformMatrix("u_camera", new Matrix4().idt());
    
    
    Texture lastTex = null;
    float piece[] = new float[compiled.pieceSize];
    
    //I.say("\nPerforming rendering-");
    
    for (FieldObject object : allObjects) {
      final Vec3D c = object.coordinates;
      final TextureRegion r = object.texRegion;
      final Texture t = r.getTexture();
      final float w = object.fieldWide / 2f, h = object.fieldHigh / 2f;
      
      ///I.say("  Rendering field object at "+c+", w/h: "+w+"/"+h);
      
      if (t != lastTex) { t.bind(0); lastTex = t; }
      
      if (compiled.meshFull() || t != lastTex) {
        compiled.renderWithShader(shading, true);
      }
      
      for (int n = 0, v = 0; n < 4; n++) {
        final boolean up = (n % 2) > 0, right = n > 1;
        //  Position-
        piece[v++] = c.x;// + (w * (right ? -1 : 1));
        piece[v++] = c.y;// + (h * (up    ? -1 : 1));
        piece[v++] = c.z;
        //  Corner offset-
        piece[v++] = w * (right ? -1 : 1);
        piece[v++] = h * (up    ? -1 : 1);
        piece[v++] = 0;
        //  Color and texture coordinates-
        piece[v++] = Colour.WHITE.bitValue;
        piece[v++] = right ? r.u : r.u2;
        piece[v++] = up ? r.v : r.v2;
      }
      compiled.appendPiece(piece);
    }
    compiled.renderWithShader(shading, true);
    
    shading.end();
    //glEnable(GL10.GL_NORMALIZE);
  }
}




