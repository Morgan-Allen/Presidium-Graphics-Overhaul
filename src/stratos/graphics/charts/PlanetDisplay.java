


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
    view.update();
  }
  
  
  void dispose() {
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
    final DisplaySector sector = new DisplaySector();
    sector.label = label;
    sector.colourKey = key.getRGBA();
    sectors.add(sector);
  }
  
  
  
  /**  Selection, feedback and highlighting-
    */
  private void cacheFaceData() {
    //  TODO:  Shoot, you have to use the surface mesh, not the sectors mesh.
    
    //
    //  Firstly, determine how many faces are on the globe surface, and how the
    //  data is partitioned for each.
    final int
      partFaces = sectorsPart.meshPart.numVertices / 3,
      meshFaces = sectorsPart.meshPart.mesh.getNumIndices() / 3,
      vertSize = 3 + 3 + 2 + 2,  //position, normal, tex coord and bone weight.
      offset = sectorsPart.meshPart.indexOffset / vertSize;
    I.say("PART FACES: "+partFaces+", MESH FACES: "+meshFaces);
    I.say("Vertex Size: "+vertSize+", index offset: "+offset);
    //
    //  Secondly, retrieve the data, and set up structures for receipt after
    //  processing.
    float vertData[] = new float[partFaces * 3 * vertSize];
    sectorsPart.meshPart.mesh.getVertices(
      sectorsPart.meshPart.indexOffset, partFaces * 3, vertData
    );
    final short indices[] = new short[meshFaces * 3];
    sectorsPart.meshPart.mesh.getIndices(indices);
    Vec3D tempV[] = new Vec3D[3];
    Vec2D tempT[] = new Vec2D[3];
    this.faceData = new FaceData[partFaces];
    //
    //  Finally, extract the vertex and tex-coord data, and calculated edge
    //  and edge-normal vectors.
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
      f.n1 = f.c2.cross(f.c1, null);
      f.n2 = f.c3.cross(f.c2, null);
      f.n3 = f.c1.cross(f.c3, null);
      f.e1 = f.c2.sub(f.c1, null);
      f.e2 = f.c3.sub(f.c2, null);
      f.e3 = f.c1.sub(f.c3, null);
    }
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
  
  
  public DisplaySector selectedAt(Vector2 mousePos) {
    //  First of all, get the distance and offset of the mouse coordinate from
    //  the centre of the globe.  That will then give you latitude/longitude,
    //  and/or UV, and that will allow you to pick a colour from an areas-skin
    //  and match that to a particular sector.
    Vec3D centre = new Vec3D(0, 0, 0);
    float radius = 4.0f;
    float rotation = 90f;
    //  TODO:  THE ABOVE NEED TO BE ESTABLISHED/PASSED TO SHADER!
    view.translateGLToScreen(centre);
    Vector2 screenPos = new Vector2(centre.x, centre.y);
    
    final float
      distY = screenPos.y - mousePos.y,
      distX = screenPos.x - mousePos.x,
      latitudeRadius = (float) FastMath.sqrt(
        (radius * radius) - (distY * distY)
      ),
      angle = (float) FastMath.asin(latitudeRadius / distX),
      //latitude = (float) FastMath.asin(radius / latitudeRadius),
      longitude = angle + (float) FastMath.toRadians(rotation);
    
    final Vec3D onSurface = new Vec3D(
      mousePos.x, mousePos.y,
      0 - (radius - (latitudeRadius * (float) FastMath.cos(longitude)))
    );
    view.translateFromScreen(onSurface);
    
    boolean matchFound = false;
    for (int n = faceData.length; n-- > 0;) {
      if (checkIntersection(onSurface, n)) { matchFound = true; break; }
    }
    if (! matchFound) return null;
    
    final Pixmap keyTex = sectorsKeyTex.asPixels();
    final int colourVal = keyTex.getPixel(
      (int) (matchU * keyTex.getWidth()),
      (int) (matchV * keyTex.getHeight())
    );
    
    for (DisplaySector sector : sectors) {
      if (sector.colourKey == colourVal) return sector;
    }
    return null;
  }
  
  
  
  /**  Render methods and helper functions-
    */
  public void renderWith(Rendering rendering, Box2D bounds, Alphabet font) {
    
    final Matrix4 screenMat = new Matrix4();
    final float
      wide = Gdx.graphics.getWidth()  / 10,
      high = Gdx.graphics.getHeight() / 10;
    screenMat.setToOrtho(-wide, wide, -high, high, -25, 25);
    
    final float r = (float) FastMath.toRadians(Rendering.activeTime() * 30);
    rotation.idt();
    rotation.scl(0.2f);
    rotation.rot(Vector3.Y, r);
    
    final Vec3D l = new Vec3D().set(-1, -1, -1).normalise();
    final float lightVals[] = new float[] { l.x, l.y, l.z };
    MeshPart p;
    
    shading.begin();
    shading.setUniformMatrix("u_rotation", rotation);
    shading.setUniformMatrix("u_camera", screenMat);
    shading.setUniformMatrix("u_camera", rendering.camera().combined);
    
    shading.setUniformi("u_surfaceTex", 0);
    shading.setUniformi("u_sectorsTex", 1);
    shading.setUniform3fv("u_lightDirection", lightVals, 0, 3);
    
    p = surfacePart.meshPart;
    surfaceTex.bind(0);
    shading.setUniformi("u_surfacePass", GL11.GL_TRUE );
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    
    p = sectorsPart.meshPart;
    sectorsTex.bind(1);
    shading.setUniformi("u_surfacePass", GL11.GL_FALSE);
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    
    shading.end();
  }
  
}


