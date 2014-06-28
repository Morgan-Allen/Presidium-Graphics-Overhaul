


package stratos.graphics.charts;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.apache.commons.math3.util.FastMath;



public class PlanetDisplay {
  
  
  private static boolean verbose = true;
  
  final ShaderProgram shading;
  final Viewport view;
  
  Matrix4 rotation;
  SolidModel globeModel;
  NodePart surfacePart, sectorsPart;
  Texture surfaceTex, sectorsTex;
  
  ImageAsset sectorsKeyTex;
  final List <DisplaySector> sectors = new List <DisplaySector> ();
  
  private static class FaceData {
    Vec3D c1, c2, c3, e1, e2, e3, n1, n2, n3;  //corners, edges, edge-normals
    Vec2D t1, t2, t3;  //texture coordinates
  }
  private FaceData faceData[];
  private float matchU, matchV;  //search results.
  
  
  
  public PlanetDisplay() {
    this.shading = new ShaderProgram(
      Gdx.files.internal("shaders/planet.vert"),
      Gdx.files.internal("shaders/planet.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog()) ;
    }
    
    this.view = new Viewport();
    view.elevation = 0;
    view.rotation  = 0;
    view.update();
  }
  
  
  public void dispose() {
    //  TODO:  THIS MUST BE SCHEDULED
    shading.dispose();
  }
  
  
  
  /**  Additional setup methods-
    */
  public void attachModel(
    SolidModel model, Texture surfaceTex,
    Texture sectorsTex, ImageAsset sectorsKeyTex
  ) {
    this.rotation = new Matrix4();
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
  }
  
  
  
  /**  Selection, feedback and highlighting-
    */
  private void cacheFaceData() {
    //
    //  Firstly, determine how many faces are on the globe surface, and how the
    //  data is partitioned for each.
    final MeshPart part = surfacePart.meshPart;
    final int
      partFaces = part.numVertices / 3,
      meshFaces = part.mesh.getNumIndices() / 3,
      vertSize = 3 + 3 + 2 + 2,  //position, normal, tex coord and bone weight.
      offset   = part.indexOffset / vertSize;
    I.say("PART FACES: "+partFaces+", MESH FACES: "+meshFaces);
    I.say("Vertex Size: "+vertSize+", index offset: "+offset);
    //
    //  Secondly, retrieve the data, and set up structures for receipt after
    //  processing.
    float vertData[] = new float[partFaces * 3 * vertSize];
    part.mesh.getVertices(part.indexOffset, vertData.length, vertData);
    final short indices[] = new short[meshFaces * 3];
    part.mesh.getIndices(indices);
    Vec3D tempV[] = new Vec3D[3];
    Vec2D tempT[] = new Vec2D[3];
    this.faceData = new FaceData[partFaces];
    //
    //  Finally, extract the vertex and tex-coord data, calculate edge and
    //  edge-normal vectors, and cache them for later reference-
    for (int n = 0; n < partFaces; n++) {
      for (int i = 3; i-- > 0;) {
        final int off = (indices[offset + (n * 3) + i] - offset) * vertSize;
        //
        final Vec3D c = tempV[i] = new Vec3D();
        c.set(vertData[off + 0], vertData[off + 1], vertData[off + 2]);
        final Vec2D t = tempT[i] = new Vec2D();
        t.set(vertData[off + 6], vertData[off + 7]);
      }
      //
      final FaceData f = faceData[n] = new FaceData();
      f.c1 = tempV[0]; f.c2 = tempV[1]; f.c3 = tempV[2];
      f.t1 = tempT[0]; f.t2 = tempT[1]; f.t3 = tempT[2];
      
      if (f.c1.length() == 0 && f.c2.length() == 0 && f.c3.length() == 0) {
        I.say("WARNING: Blank face geometry at index "+n);
      }
      
      f.n1 = f.c2.cross(f.c1, null);
      f.n2 = f.c3.cross(f.c2, null);
      f.n3 = f.c1.cross(f.c3, null);
      f.e1 = f.c2.sub(f.c1, null);
      f.e2 = f.c3.sub(f.c2, null);
      f.e3 = f.c1.sub(f.c3, null);
    }
  }
  
  
  public Vec3D surfacePosition(Vector2 mousePos) {
    
    Vec3D centre = new Vec3D(0, 0, 0);  //Correct?
    float radius = 3.0f;  //  Question mark, TODO:  ESTABLISH?
    float rotation = 0f;  //  Also, this?
    
    final Vec3D screenPos = new Vec3D(centre);
    view.translateGLToScreen(screenPos);
    final float
      distY = (mousePos.y - screenPos.y) / view.screenScale(),
      distX = (mousePos.x - screenPos.x) / view.screenScale();
    if (FastMath.abs(distY) > radius) return null;
    
    final float latitudeRadius = (float) FastMath.sqrt(
      (radius * radius) - (distY * distY)
    );
    if (FastMath.abs(distX) > latitudeRadius) return null;
    
    final float
      angle = (float) FastMath.asin(distX / latitudeRadius),
      longitude = angle + (float) FastMath.toRadians(rotation);
    
    final Vec3D onSurface = new Vec3D(
      latitudeRadius * (float) FastMath.sin(longitude),
      distY,
      latitudeRadius * (float) FastMath.cos(longitude)
    );
    onSurface.add(centre);
    return onSurface;
  }
  
  
  private boolean checkIntersection(
    Vec3D point, int faceIndex
  ) {
    final FaceData f = faceData[faceIndex];
    final float
      d1 = f.n1.dot(f.c1.sub(point, null)) / f.n1.dot(f.e3),
      d2 = f.n2.dot(f.c2.sub(point, null)) / f.n2.dot(f.e1),
      d3 = f.n3.dot(f.c3.sub(point, null)) / f.n3.dot(f.e2);
    if (d1 < 0 || d2 < 0 || d3 < 0) return false;
    
    float u = 0, v = 0;
    float w1 = d1 / (d1 + d2), w2 = d2 / (d1 + d2), w3 = 1 - d3;
    
    u = (w1 * f.t1.x) + (w2 * f.t2.x);
    u = (w3 * f.t3.x) + ((1 - w3) * u);
    
    v = (w1 * f.t1.y) + (w2 * f.t2.y);
    v = (w3 * f.t3.y) + ((1 - w3) * v);
    
    this.matchU = u;
    this.matchV = v;
    
    return true;
  }
  
  
  public int colourSelectedAt(Vector2 mousePos) {
    final int nullVal = 0;
    final Vec3D onSurface = surfacePosition(mousePos);
    if (onSurface == null) return nullVal;
    
    boolean matchFound = false;
    for (int n = faceData.length; n-- > 0;) {
      if (checkIntersection(onSurface, n)) { matchFound = true; break; }
    }
    if (! matchFound) return nullVal;
    
    final Pixmap keyTex = sectorsKeyTex.asPixels();
    final int colourVal = keyTex.getPixel(
      (int) (matchU * keyTex.getWidth()),
      (int) (matchV * keyTex.getHeight())
    );
    return colourVal;
  }
  
  
  public DisplaySector selectedAt(Vector2 mousePos) {
    final int colourVal = colourSelectedAt(mousePos);
    if (colourVal == 0) return null;
    
    final Colour match = new Colour();
    match.setFromRGBA(colourVal);
    
    //  Now, you need to get the closest match from all sectors...
    DisplaySector pick = null;
    float minDiff = 10;
    
    for (DisplaySector sector : sectors) {
      final Colour c = sector.colourKey;
      float diff = 0;
      diff += FastMath.abs(match.r - c.r);
      diff += FastMath.abs(match.g - c.g);
      diff += FastMath.abs(match.b - c.b);
      if ((diff / 3) > 0.1f) continue;
      
      if (diff < minDiff) { pick = sector; minDiff = diff; }
    }
    return pick;
  }
  
  
  
  /**  Render methods and helper functions-
    */
  public void renderWith(Rendering rendering, Box2D bounds, Alphabet font) {
    
    //final float r = (float) FastMath.toRadians(Rendering.activeTime() * 30);
    rotation.idt();
    rotation.scl(0.2f);  //TODO:  FIX THE RADIUS INSTEAD
    //rotation.rot(Vector3.Y, r);
    ///view.rotation = rendering.view.rotation;
    view.rotation = 90;
    view.update();
    
    
    final Vec3D l = new Vec3D().set(-1, -1, -1).normalise();
    final float lightVals[] = new float[] { l.x, l.y, l.z };
    MeshPart p;
    
    shading.begin();
    shading.setUniformMatrix("u_rotation", rotation);
    shading.setUniformMatrix("u_camera", view.camera.combined);
    
    shading.setUniformi("u_surfaceTex", 0);
    shading.setUniformi("u_sectorsTex", 1);
    shading.setUniform3fv("u_lightDirection", lightVals, 0, 3);
    
    p = surfacePart.meshPart;
    surfaceTex.bind(0);
    shading.setUniformi("u_surfacePass", GL11.GL_TRUE );
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    
    p = surfacePart.meshPart;
    sectorsKeyTex.asTexture().bind(0);
    shading.setUniformi("u_surfacePass", GL11.GL_TRUE );
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    
    p = sectorsPart.meshPart;
    sectorsTex.bind(1);
    shading.setUniformi("u_surfacePass", GL11.GL_FALSE);
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    
    shading.end();
  }
  
}








