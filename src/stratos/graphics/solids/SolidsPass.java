/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;
import stratos.graphics.solids.SolidSprite.Part;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.math.*;



public class SolidsPass {

  final static int
    MAX_SKINS = 8,
    MAX_BONES = 50;
  
  final static String OVER_NAMES[] = {
    "u_over0", "u_over1", "u_over2", "u_over3",
    "u_over4", "u_over5", "u_over6", "u_over7",
  };
  
  
  final Rendering rendering;
  final Batch <SolidSprite> inPass = new Batch <SolidSprite> ();
  private ShaderProgram shading;
  
  
  
  public SolidsPass(Rendering rendering) {
    this.rendering = rendering;
    
    shading = new ShaderProgram(
      Gdx.files.internal("shaders/solids.vert"),
      Gdx.files.internal("shaders/solids.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog());
    }
  }
  
  
  public void dispose() {
    shading.dispose();
  }
  
  
  protected void register(SolidSprite sprite) {
    inPass.add(sprite);
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
  
  
  public void performPass() {
    
    shading.begin();
    shading.setUniformi("u_texture", 0);
    for (int i = 0; i < MAX_SKINS; i++) {
      shading.setUniformi(OVER_NAMES[i], i + 1);
    }
    
    //  The ambient light, diffuse light, and light direction-
    final float
      ambA    [] = rendering.lighting.ambient.toFloatVals(),
      difA    [] = rendering.lighting.diffuse.toFloatVals(),
      lightDir[] = rendering.lighting.lightDir;
    shading.setUniform4fv("u_ambientLight"  , ambA    , 0, 4);
    shading.setUniform4fv("u_diffuseLight"  , difA    , 0, 4);
    shading.setUniform3fv("u_lightDirection", lightDir, 0, 3);
    shading.setUniformMatrix("u_camera", rendering.camera().combined);
    
    final Sorting <SolidSprite.Part> allParts = new Sorting <SolidSprite.Part> () {
      public int compare(Part a, Part b) {
        //  TODO:  First, sort transparency.  Then by level.  Then by texture.
        return 0;
      }
    };
    for (SolidSprite sprite : inPass) sprite.addPartsTo(allParts);
    
    //  TODO:  Don't substitute new bone information unless it's a different
    //  base sprite.
    //  TODO:  In the case of texture overlays, consider using a single big
    //  texture atlas for efficiency?
    
    for (SolidSprite.Part part : allParts) {
      if (part.texture == null) continue;
      part.texture.bind(0);
      part.texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);  //TODO:  Make more general
      bindOverlays(part);
      final Colour c = part.colour;
      shading.setUniformf("u_texColor", c.r, c.g, c.b, c.a);
      
      final Matrix4 partBones[] = part.meshBones;
      final float[] bones = new float[MAX_BONES * 16];
      final int maxIter = Nums.min(MAX_BONES, partBones.length);
      
      for (int i = 0; i < maxIter * 16; i++) {
        bones[i] = partBones[i / 16].val[i % 16];
      }
      shading.setUniformMatrix("u_worldTrans", part.belongs.transform);
      shading.setUniformi("u_numBones", partBones.length);
      shading.setUniformMatrix4fv("u_bones", bones, 0, bones.length);
      
      part.mesh.render(
        shading, part.meshType, part.meshIndex, part.meshVerts
      );
    }
    
    shading.end();
    inPass.clear();
  }
  
  
  
  /**  Helper method for binding multiple texture overlays-
    */
  private void bindOverlays(SolidSprite.Part part) {
    if (part == null || part.overlays == null || part.overlays.length < 1) {
      shading.setUniformi("u_numOverlays", 0);
      return;
    }
    final int numOver = part.overlays.length;
    if (numOver > MAX_SKINS) I.complain("TOO MANY OVERLAYS!");
    
    shading.setUniformi("u_numOverlays", numOver);
    for (int i = 0; i < numOver; i++) part.overlays[i].bind(i + 1);
  }
}




