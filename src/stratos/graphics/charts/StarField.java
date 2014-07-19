


package stratos.graphics.charts;
import stratos.start.Disposal;
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



public class StarField extends Disposal {
  
  
  final Viewport view;
  final List <FieldObject> allObjects;
  
  private FieldObject selectObject;
  private FieldObject hoverFocus, selectFocus;
  private float hoverAlpha = 1, selectAlpha = 1;
  
  private Texture sectorsTex, axisTex;
  private float fieldSize, objectScale = 1f;
  private float rotation = 90, elevation = 0;
  
  private Stitching compiled;
  private ShaderProgram shading;
  
  
  
  public StarField() {
    super(true);
    view = new Viewport();
    
    allObjects = new List <FieldObject> () {
      protected float queuePriority(FieldObject r) {
        return 0 - r.depth;
      }
    };
  }
  
  
  protected void performAssetSetup() {
    //  NOTE:  The normal attribute here is actually used to store the offset
    //  of a corner from the given decal's coordinate centre (see below).
    compiled = new Stitching(
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
  }
  
  
  protected void performAssetDisposal() {
    if (shading == null) return;
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
  
  
  public FieldObject addFieldObject(Texture t, String label, Vec3D position) {
    return addFieldObject(
      t, label,
      1, 1, 0, 0,
      1, 0, 0,
      position
    );
  }
  
  
  public FieldObject addFieldObject(
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
    return object;
  }
  
  
  public void setSelectObject(
    Texture t, int gridW, int gridH, int gridX, int gridY
  ) {
    final FieldObject object = addFieldObject(
      t, null, gridW, gridH, gridX, gridY, 1, 0, 0, new Vec3D()
    );
    allObjects.remove(object);
    this.selectObject = object;
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
    
    if (pick != hoverFocus) hoverAlpha = 0;
    this.hoverFocus = pick;
    return pick;
  }
  
  
  public FieldObject objectLabelled(String label) {
    for (FieldObject o : allObjects) if (o.label != null) {
      if (o.label.equals(label)) return o;
    }
    return null;
  }
  
  
  public void setRotation(float rotation) {
    this.rotation = rotation;
  }
  
  
  public float rotation() {
    return this.rotation;
  }
  
  
  public void setElevation(float elevation) {
    this.elevation = elevation;
  }
  
  
  public float elevation() {
    return this.elevation;
  }
  
  
  public void setSelection(String sectorLabel) {
    final FieldObject OS = objectLabelled(sectorLabel);
    this.selectFocus = OS;
    selectAlpha = 0;
  }
  
  
  
  /**  Rendering methods-
    */
  public void renderWith(
    Rendering rendering, Box2D bounds, Alphabet forLabels
  ) {
    view.updateForWidget(bounds, fieldSize, rotation, elevation);
    
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
      
      final Texture t = object.texRegion.getTexture();
      if (t != lastTex) {
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        t.bind(0);
        lastTex = t;
      }
      if (compiled.meshFull() || t != lastTex) {
        compiled.renderWithShader(shading, true);
      }

      final Vec3D v = object.coordinates;
      c.set(v.x, v.y, v.z);
      renderObject(object, SW, SH, c, Colour.WHITE, piece);
    }
    compiled.renderWithShader(shading, true);
    
    //
    //  Finally, we render selection on top of this-
    selectObject.texRegion.getTexture().bind(0);
    final Colour fade = new Colour();
    final float alphaInc = 1f / Rendering.FRAMES_PER_SECOND;
    hoverAlpha  = Visit.clamp(hoverAlpha  + alphaInc, 0, 1);
    selectAlpha = Visit.clamp(selectAlpha + alphaInc, 0, 1);
    
    if (hoverFocus != null && hoverFocus != selectFocus) {
      final Vec3D hC = hoverFocus .coordinates;
      fade.set(1, 1, 1, hoverAlpha / 2);
      c.set(hC.x, hC.y, hC.z);
      renderObject(selectObject, SW, SH, c, fade, piece);
    }
    if (selectFocus != null) {
      final Vec3D sC = selectFocus.coordinates;
      fade.set(1, 1, 1, selectAlpha   );
      c.set(sC.x, sC.y, sC.z);
      renderObject(selectObject, SW, SH, c, fade, piece);
    }
    
    compiled.renderWithShader(shading, true);
    shading.end();
  }
  
  
  private void renderObject(
    FieldObject object, float SW, float SH,
    Vector3 c, Colour hue, float piece[]
  ) {
    
    final float
      x = object.offX / SW,
      y = object.offY / SH,
      w = object.fieldWide * objectScale / SW,
      h = object.fieldHigh * objectScale / SH;
    
    final TextureRegion r = object.texRegion;
    appendVertex(piece, c, x - w, y - h, hue, r.u , r.v2);
    appendVertex(piece, c, x - w, y + h, hue, r.u , r.v );
    appendVertex(piece, c, x + w, y - h, hue, r.u2, r.v2);
    appendVertex(piece, c, x + w, y + h, hue, r.u2, r.v );
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
    appendVertex(piece, new Vector3(-a, 0, -a), 0, 0, fade, 0, 1);
    appendVertex(piece, new Vector3(-a, 0,  a), 0, 0, fade, 0, 0);
    appendVertex(piece, new Vector3( a, 0, -a), 0, 0, fade, 1, 1);
    appendVertex(piece, new Vector3( a, 0,  a), 0, 0, fade, 1, 0);

    axisTex.bind(0);
    compiled.renderWithShader(shading, true);
  }
  
  
  private void renderLabels(Alphabet font) {
    //  NOTE:  The divide-by-2 is to allow for the OpenGL coordinate system.
    //  TODO:  get rid of the screen-width/height scaling.  Pass that as params
    //  to the shader once and have it do the math.
    final float
      SW = Gdx.graphics.getWidth()  / 2,
      SH = Gdx.graphics.getHeight() / 2;
    final float piece[] = new float[compiled.vertexSize];
    final Vector3 pos = new Vector3();
    font.texture().bind(0);
    
    for (FieldObject o : allObjects) if (o.label != null) {

      final Vec3D v = o.coordinates;
      pos.set(v.x, v.y, v.z);
      float
        x = Label.phraseWidth(o.label, font, 1.0f) * objectScale / (SW * -2),
        y = (0 - font.letterFor(' ').height * 2 * objectScale) / SH;
      
      for (char c : o.label.toCharArray()) {
        final Alphabet.Letter l = font.letterFor(c);
        if (l == null) continue;
        final float
          w = l.width  * objectScale / SW,
          h = l.height * objectScale / SH;
        
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


