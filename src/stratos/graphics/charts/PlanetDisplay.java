


package stratos.graphics.charts;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.util.*;
//import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.GdxRuntimeException;

import org.apache.commons.math3.util.FastMath;



public class PlanetDisplay {
  
  
  //final ChartDisplay display;
  final ShaderProgram shading;
  
  Matrix4 rotation;
  SolidModel globeModel;
  NodePart surfacePart, sectorsPart;
  Texture surfaceTex, sectorsTex;
  
  
  public PlanetDisplay() {
    this.shading = new ShaderProgram(
      Gdx.files.internal("shaders/planet.vert"),
      Gdx.files.internal("shaders/planet.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog()) ;
    }
  }
  
  
  void dispose() {
    //  TODO:  THIS MUST BE SCHEDULED
    shading.dispose();
  }
  
  
  public void attachModel(
    SolidModel model, Texture surfaceTex, Texture sectorsTex
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
  }
  
  
  public void renderWith(Rendering rendering, Box2D bounds) {
    
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
    
    //glDepthMask(false);
    
    p = sectorsPart.meshPart;
    sectorsTex.bind(1);
    shading.setUniformi("u_surfacePass", GL11.GL_FALSE);
    p.mesh.render(shading, p.primitiveType, p.indexOffset, p.numVertices);
    
    shading.end();
  }
  
}


