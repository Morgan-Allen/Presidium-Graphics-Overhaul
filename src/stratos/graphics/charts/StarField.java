


package stratos.graphics.charts;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;  //  TODO:  REPLACE
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.Texture.*;

import org.apache.commons.math3.util.FastMath;
import java.util.Random;



public class StarField {
  
  
  final Viewport view;
  final List <FieldObject> allObjects;
  
  private Texture sectorsTex, axisTex;
  private float fieldSize;
  
  private MeshCompile compiled;
  private ShaderProgram shading;
  
  
  
  public StarField() {
    view = new Viewport();
    
    //  NOTE:  The normal attribute here is actually used to store the offset
    //  of a corner from the given decal's coordinate centre (see below).
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
    
    allObjects = new List <FieldObject> () {
      protected float queuePriority(FieldObject r) {
        return 0 - r.depth;
      }
    };
  }
  
  
  public void dispose() {
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
  
  
  public void addFieldObject(Texture t, String label, Vec3D position) {
    addFieldObject(
      t, label,
      1, 1, 0, 0,
      1, 0, 0,
      position
    );
  }
  
  
  public void addFieldObject(
    Texture t, String label,
    int gridW, int gridH, int gridX, int gridY,
    float imgScale, float offX, float offY,
    Vec3D position
  ) {
    final FieldObject object = new FieldObject(label);
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
  
  
  public void addRandomScatter(
    Texture t, int gridW, int gridH, int[][] starTypes,
    int maxCompanions, int randomSeed
  ) {
    final Random rand = new Random(randomSeed);
    //  Note:  The array cast is needed to prevent infinite regression as more
    //  objects are added to the list!
    for (FieldObject object : allObjects.toArray(FieldObject.class)) {
      if (object.label == null) continue;
      
      for (int i = rand.nextInt(maxCompanions); i-- > 0;) {
        Vec3D coords = new Vec3D(
          rand.nextFloat() - 0.5f,
          rand.nextFloat() - 0.5f,
          rand.nextFloat() - 0.5f
        ).scale(2);
        if (rand.nextBoolean()) coords.add(object.coordinates);
        else coords.scale(fieldSize / 2);
        
        final int type[] = starTypes[rand.nextInt(starTypes.length)];
        float mag = (0.5f + rand.nextFloat()) / 2;
        if (coords.distance(object.coordinates) < 0.25f) continue;
        
        addFieldObject(
          t, null,  5, 5,  type[0], type[1],
          mag * mag,  0, 0,  coords
        );
      }
    }
  }
  
  
  
  /**  Selection and feedback methods-
    */
  public Vec3D screenPosition(FieldObject object, Vec3D put) {
    if (put == null) put = new Vec3D();
    view.translateGLToScreen(put.setTo(object.coordinates));
    return put;
  }
  
  
  public FieldObject selectedAt(Vector2 mousePos) {
    FieldObject pick = null;
    float minDist = Float.POSITIVE_INFINITY;
    
    final Vec3D v = new Vec3D();
    for (FieldObject o : allObjects) if (o.label != null) {
      screenPosition(o, v);
      final float
        dX = FastMath.abs(v.x - mousePos.x),
        dY = FastMath.abs(v.y - mousePos.y),
        dist = FastMath.max(dX, dY);
      if (dX < (o.fieldWide / 2) && dY < (o.fieldHigh / 2) && dist < minDist) {
        pick = o;
        minDist = dist;
      }
    }
    
    return pick;
  }
  
  
  public FieldObject objectLabelled(String label) {
    for (FieldObject o : allObjects) if (o.label != null) {
      if (o.label.equals(label)) return o;
    }
    return null;
  }
  
  
  
  /**  Rendering methods-
    */
  public void renderWith(
    Rendering rendering, Box2D bounds, Alphabet forLabels
  ) {
    final float time = Rendering.activeTime();
    view.updateForWidget(bounds, fieldSize, (90 + (time * 15)) % 360, 0);
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    glDepthMask(false);
    
    shading.begin();
    shading.setUniformi("u_texture", 0);
    shading.setUniformMatrix("u_rotation", new Matrix4().idt());
    shading.setUniformMatrix("u_camera", view.camera.combined);

    final float SW = Gdx.graphics.getWidth(), SH = Gdx.graphics.getHeight();
    final float portalSize = FastMath.min(bounds.xdim(), bounds.ydim());
    final Vec2D centre = bounds.centre();
    shading.setUniformf("u_portalRadius", portalSize / 2);
    shading.setUniformf("u_screenX", centre.x - (SW / 2));
    shading.setUniformf("u_screenY", centre.y - (SH / 2));
    shading.setUniformf("u_screenWide", SW / 2);
    shading.setUniformf("u_screenHigh", SH / 2);
    
    renderLabels(forLabels);
    renderSectorsAndAxes();

    Texture lastTex = null;
    float piece[] = new float[compiled.vertexSize];
    final Vector3 c = new Vector3();
    
    for (FieldObject object : allObjects) {
      
      final Vec3D v = object.coordinates;
      c.set(v.x, v.y, v.z);
      
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
    
    Colour fade = Colour.transparency(0.5f);
    appendVertex(piece, new Vector3(-a, -a, 0), 0, 0, fade, 0, 1);
    appendVertex(piece, new Vector3(-a,  a, 0), 0, 0, fade, 0, 0);
    appendVertex(piece, new Vector3( a, -a, 0), 0, 0, fade, 1, 1);
    appendVertex(piece, new Vector3( a,  a, 0), 0, 0, fade, 1, 0);
    
    sectorsTex.bind(0);
    compiled.renderWithShader(shading, true);
    
    fade = Colour.transparency(0.2f);
    appendVertex(piece, new Vector3(0, -a, -a), 0, 0, fade, 0, 1);
    appendVertex(piece, new Vector3(0, -a,  a), 0, 0, fade, 0, 0);
    appendVertex(piece, new Vector3(0,  a, -a), 0, 0, fade, 1, 1);
    appendVertex(piece, new Vector3(0,  a,  a), 0, 0, fade, 1, 0);

    axisTex.bind(0);
    compiled.renderWithShader(shading, true);
  }
  
  
  private void renderLabels(Alphabet font) {

    final float SW = Gdx.graphics.getWidth(), SH = Gdx.graphics.getHeight();
    final float piece[] = new float[compiled.vertexSize];
    final Vector3 pos = new Vector3();
    font.texture().bind(0);
    
    //  TODO:  Why is the scale off by default?   ...Well, maybe it isn't.  It's
    //  the default OpenGL screen coordinate system, isn't it.
    //  TODO:  get rid of the screen-width/height scaling.  Pass that as params
    //  to the shader once and have it do the math.
    
    for (FieldObject o : allObjects) if (o.label != null) {

      final Vec3D v = o.coordinates;
      pos.set(v.x, v.y, v.z);
      float
        x = 2 * Label.phraseWidth(o.label, font, 1.0f) / (SW * -2),
        y = 2 * (0 - font.letterFor(' ').height * 2) / SH;
      
      for (char c : o.label.toCharArray()) {
        final Alphabet.Letter l = font.letterFor(c);
        if (l == null) continue;
        final float w = 2 * l.width / SW, h = 2 * l.height / SH;
        
        appendVertex(piece, pos, x    , y    , Colour.WHITE, l.umin, l.vmax);
        appendVertex(piece, pos, x    , y + h, Colour.WHITE, l.umin, l.vmin);
        appendVertex(piece, pos, x + w, y    , Colour.WHITE, l.umax, l.vmax);
        appendVertex(piece, pos, x + w, y + h, Colour.WHITE, l.umax, l.vmin);
        x += w;
      }
      
      if (compiled.meshFull()) compiled.renderWithShader(shading, true);
    }
    compiled.renderWithShader(shading, true);
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


