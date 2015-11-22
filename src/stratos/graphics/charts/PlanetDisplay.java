/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.charts;
import stratos.graphics.common.*;
import stratos.graphics.sfx.Label;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.start.Assets;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.GdxRuntimeException;




public class PlanetDisplay extends Assets.Loadable {
  
  
  final static int   DEFAULT_RADIUS = 10;
  final static float KEY_TOLERANCE  = 0.02f;
  private static boolean setupVerbose = false;
  
  
  private float
    rotation  = 0,
    elevation = 0,
    radius    = DEFAULT_RADIUS;
  private Mat3D
    rotMatrix = new Mat3D().setIdentity();
  
  private Viewport view;
  private ShaderProgram shading;
  
  private SolidModel globeModel;
  private NodePart surfacePart, sectorsPart;
  private Texture surfaceTex, sectorsTex;
  
  private ImageAsset sectorsKeyTex;
  private List <DisplaySector> sectors = new List <DisplaySector> ();
  
  private Colour hoverKey, selectKey;
  private float hoverAlpha = 0, selectAlpha = 0;
  private Stitching labelling;
  
  //private boolean loaded = false, disposed = false;
  
  
  
  
  public PlanetDisplay() {
    super("PLANET_DISPLAY", PlanetDisplay.class, true);
    this.view = new Viewport();
  }
  
  
  protected State loadAsset() {
    if (state == State.LOADED) return State.ERROR;
    
    this.shading = new ShaderProgram(
      Gdx.files.internal("shaders/planet.vert"),
      Gdx.files.internal("shaders/planet.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog());
    }
    
    this.labelling = new Stitching(
      Stitching.BONED_VERTEX_SIZE,   //number of floats per vertex.
      true, 100,                     //is a quad, max. total quads
      new int[] {0, 1, 2, 1, 2, 3},  //indices for quad vertices
      VertexAttribute.Position  ( ),
      VertexAttribute.Normal    ( ),
      VertexAttribute.TexCoords (0),
      VertexAttribute.BoneWeight(0)
    );
    
    return state = State.LOADED;
  }
  
  
  protected State disposeAsset() {
    if (state == State.DISPOSED) return State.ERROR;
    shading.dispose();
    labelling.dispose();
    return state = State.DISPOSED;
  }
  
  
  
  /**  Additional setup methods-
    */
  public void attachModel(
    SolidModel model, Texture surfaceTex,
    Texture sectorsTex, ImageAsset sectorsKeyTex
  ) {
    this.rotation = 0;
    this.globeModel = model;
    
    final String partNames[] = globeModel.partNames();
    this.surfacePart = globeModel.partWithName(partNames[0]);
    this.sectorsPart = globeModel.partWithName(partNames[1]);
    this.surfaceTex = surfaceTex;
    this.sectorsTex = sectorsTex;
    surfaceTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    sectorsTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    
    this.sectorsKeyTex = sectorsKeyTex;
    cacheFaceData();
  }
  
  
  public void attachSector(
    String label, Colour key
  ) {
    final DisplaySector sector = new DisplaySector(label);
    sector.colourKey = key;
    sectors.add(sector);
    cacheFaceData();
    calcCoordinates(sector);
  }
  
  
  
  /**  Helper method for storing corners, edges, edge-normals, texture
    *  coordinates, and corner distances- later used to perform selection.
    */
  private static class FaceData {
    int ID;
    Colour key;
    Vec3D midpoint;
    
    Vec3D c1, c2, c3, e21, e32, e13, n21, n32, n13;
    Vec2D t1, t2, t3;
    float d1, d2, d3;
  }
  private FaceData faceData[];  //Cached for convenience...
  private float matchU, matchV;  //search results.
  private Vec3D temp = new Vec3D();
  
  
  private void cacheFaceData() {
    if (faceData != null) return;
    //
    //  Firstly, determine how many faces are on the globe surface, and how the
    //  data is partitioned for each.
    final MeshPart part = surfacePart.meshPart;
    final Pixmap keyTex = sectorsKeyTex.asPixels();
    final int
      partFaces = part.numVertices / 3,
      meshFaces = part.mesh.getNumIndices() / 3,
      vertSize  = 3 + 3 + 2 + 6,  //  vert, normal, tex coord and bone weights.
      offset    = part.indexOffset / vertSize;
    
    //  TODO:  THESE MAGIC CONSTANTS NEED TO BE IN A SINGLE CENTRAL INTERFACE.
    
    if (setupVerbose) {
      I.say("PART FACES: "+partFaces+", MESH FACES: "+meshFaces);
      I.say("Vertex Size: "+vertSize+", index offset: "+offset);
    }
    //
    //  Secondly, retrieve the data, and set up structures for receipt after
    //  processing.
    final float vertData[] = new float[meshFaces * 3 * vertSize];
    final short indices [] = new short[meshFaces * 3];
    part.mesh.getVertices(vertData);
    part.mesh.getIndices(indices);
    Vec3D tempV[] = new Vec3D[3];
    Vec2D tempT[] = new Vec2D[3];
    this.faceData = new FaceData[partFaces];
    //
    //  Finally, extract the vertex and tex-coord data, calculate edge and
    //  edge-normal vectors, and cache them for later reference-
    for (int n = 0; n < partFaces; n++) {
      if (setupVerbose) I.say("\nINITING NEXT FACE: "+n);
      
      for (int i = 0; i < 3; i++) {
        final int index = indices[offset + (n * 3) + i];
        final int off = index * vertSize;
        //
        final Vec3D c = tempV[i] = new Vec3D();
        c.set(vertData[off + 0], vertData[off + 1], vertData[off + 2]);
        final Vec2D t = tempT[i] = new Vec2D();
        t.set(vertData[off + 6], vertData[off + 7]);
        if (setupVerbose) {
          I.say("  Corner: "+i+", index is: "+index);
          I.say("    Vertex:  "+c);
          I.say("    Texture: "+t);
        }
      }
      //
      final FaceData f = faceData[n] = new FaceData();
      f.ID = n;
      //
      //  Having obtained geometry data, calculate and store edges & normals.
      f.c1 = tempV[0]; f.c2 = tempV[1]; f.c3 = tempV[2];
      f.t1 = tempT[0]; f.t2 = tempT[1]; f.t3 = tempT[2];
      f.e21 = f.c2.sub(f.c1, null);
      f.e32 = f.c3.sub(f.c2, null);
      f.e13 = f.c1.sub(f.c3, null);
      f.n21 = f.c2.cross(f.c1, null).normalise();
      f.n32 = f.c3.cross(f.c2, null).normalise();
      f.n13 = f.c1.cross(f.c3, null).normalise();
      f.d1 = f.n32.dot(f.e21);
      f.d2 = f.n13.dot(f.e32);
      f.d3 = f.n21.dot(f.e13);
      
      if (setupVerbose) {
        I.say("  Edge 2-1: "+f.e21);
        I.say("  Edge 3-2: "+f.e32);
        I.say("  Edge 1-3: "+f.e13);
        I.say("  Norm 2-1: "+f.n21);
        I.say("  Norm 3-2: "+f.n32);
        I.say("  Norm 1-3: "+f.n13);
        I.say("  Dist to corner 1: "+f.d1);
        I.say("  Dist to corner 2: "+f.d2);
        I.say("  Dist to corner 3: "+f.d3);
      }
      //
      //  Finally, obtain a sample of the colour key and midpoints-
      f.midpoint = new Vec3D().add(f.c1).add(f.c2).add(f.c3).scale(1f / 3);
      final float
        u = (f.t1.x + f.t2.x + f.t3.x) / 3,
        v = (f.t1.y + f.t2.y + f.t3.y) / 3;
      final int colourVal = keyTex.getPixel(
        (int) (u * keyTex.getWidth()),
        (int) (v * keyTex.getHeight())
      );
      f.key = new Colour();
      f.key.setFromRGBA(colourVal);
      
      if (setupVerbose) {
        I.say("    Midpoint: "+f.midpoint);
        I.say("    Colour sample: "+f.key);
      }
    }
  }
  
  
  private boolean checkIntersection(
    Vec3D point, FaceData f
  ) {
    final float
      w1 = f.n32.dot(f.c2.sub(point, temp)) / f.d1,
      w2 = f.n13.dot(f.c3.sub(point, temp)) / f.d2,
      w3 = f.n21.dot(f.c1.sub(point, temp)) / f.d3;
    
    if (w1 < 0 || w2 < 0 || w3 < 0) return false;
    float u = 0, v = 0, sum = w1 + w2 + w3;
    
    u += f.t1.x * w1 / sum;
    u += f.t2.x * w2 / sum;
    u += f.t3.x * w3 / sum;
    
    v += f.t1.y * w1 / sum;
    v += f.t2.y * w2 / sum;
    v += f.t3.y * w3 / sum;
    
    this.matchU = u;
    this.matchV = v;
    
    return true;
  }
  
  
  
  /**  Selection, feedback and highlighting-
    */
  public Vec3D surfacePosition(Vector2 mousePos) {
    
    final Vec3D
      origin    = new Vec3D(0, 0, 0),
      screenPos = new Vec3D(mousePos.x, mousePos.y, 0);
    
    view.translateGLToScreen(origin);
    origin.z = 0;
    view.translateGLFromScreen(origin);
    view.translateGLFromScreen(screenPos);
    screenPos.sub(origin);
    
    final float len = screenPos.length();
    if (len > radius) return null;
    
    final float offset = Nums.sqrt(
      (radius * radius) - (len * len)
    );
    final Vec3D depth = new Vec3D(0, 0, -1);
    view.translateGLFromScreen(depth);
    origin.set(0, 0, 0);
    view.translateGLFromScreen(origin);
    depth.sub(origin);
    depth.normalise().scale(offset);
    
    screenPos.add(depth);
    final Mat3D invRot = rotMatrix.inverse(null);
    invRot.trans(screenPos);
    return screenPos;
  }
  
  
  public int colourSelectedAt(Vector2 mousePos) {
    final Vec3D onSurface = surfacePosition(mousePos);
    if (onSurface == null) return 0;
    return colourOnSurface(onSurface);
  }
  
  
  private int colourOnSurface(Vec3D onSurface) {
    
    if (faceData == null) return 0;
    boolean matchFound = false;
    for (FaceData f : faceData) {
      if (checkIntersection(onSurface, f)) { matchFound = true; break; }
    }
    if (! matchFound) return 0;
    
    final Pixmap keyTex = sectorsKeyTex.asPixels();
    final int colourVal = keyTex.getPixel(
      (int) (matchU * keyTex.getWidth()),
      (int) (matchV * keyTex.getHeight())
    );
    return colourVal;
  }
  
  
  public DisplaySector selectedAt(Vector2 mousePos) {
    
    final int colourVal = colourSelectedAt(mousePos);
    if (colourVal == 0) {
      this.hoverKey = null;
      return null;
    }
    
    final Colour match = new Colour();
    match.setFromRGBA(colourVal);
    if (match.difference(hoverKey) >= KEY_TOLERANCE) {
      this.hoverAlpha = 0;
      this.hoverKey = match;
    }
    
    for (DisplaySector sector : sectors) {
      final float diff = match.difference(sector.colourKey);
      if (diff < KEY_TOLERANCE) return sector;
    }
    
    return null;
  }
  
  
  private void calcCoordinates(DisplaySector sector) {
    sector.coordinates.set(0, 0, 0);
    for (FaceData f : faceData) {
      final float diff = sector.colourKey.difference(f.key);
      if (diff > KEY_TOLERANCE) continue;
      sector.coordinates.add(f.midpoint);
    }
    sector.coordinates.normalise().scale(radius);
  }
  
  
  public DisplaySector sectorLabelled(String label) {
    for (DisplaySector sector : sectors) if (sector.label != null) {
      if (sector.label.equals(label)) return sector;
    }
    return null;
  }
  
  
  public Vec3D screenPosition(DisplaySector sector, Vec3D put) {
    if (put == null) put = new Vec3D();
    put.setTo(sector.coordinates);
    rotMatrix.trans(put);
    view.translateGLToScreen(put);
    return put;
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
    final DisplaySector DS = sectorLabelled(sectorLabel);
    final int key = DS == null ? 0 : colourOnSurface(DS.coordinates);
    if (key == 0) selectKey = null;
    else (selectKey = new Colour()).setFromRGBA(key);
    selectAlpha = 0;
  }
  
  
  
  /**  Render methods and helper functions-
    */
  public void renderWith(Rendering rendering, Box2D bounds, Alphabet font) {
    
    //
    //  First of all, we configure viewing perspective, aperture size, rotation
    //  and offset:
    rotMatrix.setIdentity();
    rotMatrix.rotateY(Nums.toRadians(0 - rotation));
    view.updateForWidget(bounds, (radius * 2) + 0, 90, elevation);
    
    final Matrix4 trans = new Matrix4().idt();
    trans.rotate(Vector3.Y, 0 - rotation);

    final float SW = Gdx.graphics.getWidth(), SH = Gdx.graphics.getHeight();
    final float portalSize = Nums.min(bounds.xdim(), bounds.ydim());
    final Vec2D centre = bounds.centre();
    shading.begin();
    shading.setUniformf("u_globeRadius", radius);
    shading.setUniformMatrix("u_rotation", trans);
    shading.setUniformMatrix("u_camera", view.camera.combined);
    shading.setUniformf("u_portalRadius", portalSize / 2);
    shading.setUniformf("u_screenX", centre.x - (SW / 2));
    shading.setUniformf("u_screenY", centre.y - (SH / 2));
    shading.setUniformf("u_screenWide", SW / 2);
    shading.setUniformf("u_screenHigh", SH / 2);
    
    //
    //  Then, we configure parameters for selection/hover/highlight FX.
    final float alphaInc = 1f / Rendering.FRAMES_PER_SECOND;
    hoverAlpha  = Nums.clamp(hoverAlpha  + alphaInc, 0, 1);
    selectAlpha = Nums.clamp(selectAlpha + alphaInc, 0, 1);
    final Colour h, s;
    if (hoverKey != null && hoverKey.difference(selectKey) > 0) {
      h = hoverKey;
    }
    else h = Colour.HIDE;
    s = selectKey == null ? Colour.HIDE : selectKey;
    shading.setUniformf("u_hoverKey" , h.r, h.g, h.b, hoverAlpha / 2);
    shading.setUniformf("u_selectKey", s.r, s.g, s.b, selectAlpha   );
    
    //
    //  One these are prepared, we can set up lighting and textures for the
    //  initial surface pass.
    final Vec3D l = new Vec3D().set(-1, -1, -1).normalise();
    final float lightVals[] = new float[] { l.x, l.y, l.z };
    MeshPart p;
    shading.setUniformi("u_surfaceTex", 0);
    shading.setUniformi("u_labelsTex" , 1);
    shading.setUniformi("u_sectorsMap", 2);
    shading.setUniform3fv("u_lightDirection", lightVals, 0, 3);
    
    p = surfacePart.meshPart;
    surfaceTex.bind(0);
    sectorsKeyTex.asTexture().bind(2);
    shading.setUniformi("u_surfacePass", GL11.GL_TRUE );
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    //  TODO:  Render sector outlines here too...
    /*
    p = sectorsPart.meshPart;
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    //*/
    
    //
    //  And on top of all these, the labels for each sector.
    Gdx.gl.glDisable(GL11.GL_DEPTH_TEST);
    Gdx.gl.glDepthMask(false);
    font.texture().bind(1);
    shading.setUniformi("u_surfacePass", GL11.GL_FALSE);
    renderLabels(font);
    
    shading.end();
  }
  
  
  private void renderLabels(Alphabet font) {
    //
    //  NOTE:  The divide-by-2 is to allow for the OpenGL coordinate system.
    //  TODO:  get rid of the screen-width/height scaling.  Pass that as params
    //  to the shader once and have it do the math.
    final float
      SW = Gdx.graphics.getWidth()  / 2,
      SH = Gdx.graphics.getHeight() / 2;
    final float piece[] = new float[labelling.vertexSize];
    final Vector3 pos = new Vector3();
    final Vec3D onScreen = new Vec3D(), origin = new Vec3D(0, 0, 0);
    font.texture().bind(0);
    view.translateGLToScreen(origin);
    //
    //  Having performed initial setup, iterate across each labelled sector and
    //  compile text geometry, with appropriate offsets to allow for global
    //  rotation.
    for (DisplaySector s : sectors) if (s.label != null) {
      //
      final Vec3D v = s.coordinates;
      pos.set(v.x, v.y, v.z);
      rotMatrix.trans(v, onScreen);
      view.translateGLToScreen(onScreen);
      if (onScreen.z > origin.z) continue;
      //
      float
        a = (onScreen.x - origin.x) / (radius * view.screenScale()),
        x = Label.phraseWidth(s.label, font, 1.0f) / SW,
        y = (0 - font.letterFor(' ').height * 2  ) / SH;
      a *= Nums.abs(a);
      x *= (1 - a) / -2;
      //
      //  NOTE:  Texture-v is flipped due to differences in pixel order in
      //  images vs. on-screen.
      for (char c : s.label.toCharArray()) {
        final Alphabet.Letter l = font.letterFor(c);
        if (l == null) continue;
        final float w = l.width / SW, h = l.height / SH;
        //
        appendVertex(piece, pos, x    , y    , l.umin, l.vmax);
        appendVertex(piece, pos, x    , y + h, l.umin, l.vmin);
        appendVertex(piece, pos, x + w, y    , l.umax, l.vmax);
        appendVertex(piece, pos, x + w, y + h, l.umax, l.vmin);
        x += w;
      }
      if (labelling.meshFull()) labelling.renderWithShader(shading, true);
    }
    labelling.renderWithShader(shading, true);
  }
  
  
  private void appendVertex(
    float piece[],
    Vector3 pos, float offX, float offY,
    float tu, float tv
  ) {
    int v = 0;
    piece[v++] = pos.x;
    piece[v++] = pos.y;
    piece[v++] = pos.z;
    //  Corner offset-
    piece[v++] = offX;
    piece[v++] = offY;
    piece[v++] = 0;
    //  Texture coordinates-
    piece[v++] = tu;
    piece[v++] = tv;
    //  Bone weights-
    piece[v++] = -1;
    piece[v++] =  0;
    labelling.appendVertex(piece);
  }
}







