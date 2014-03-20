


package stratos.graphics.cutout ;
import static stratos.graphics.common.GL.*;
import static stratos.graphics.cutout.CutoutModel.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.SFX;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.glutils.*;



public class CutoutsPass {
	
  
  final static int
    MAX_SPRITES   = 1000,
    COMPILE_LIMIT = MAX_SPRITES * SIZE ;
  
  private static Vector3 temp = new Vector3(), temp2 = new Vector3();
  final static float GLOW_LIGHTS[] = { 1, 1, 1, 1 };
  
  
  final Rendering rendering;
  final Batch <CutoutSprite> inPass = new Batch <CutoutSprite> ();
  
  private Mesh compiled ;
  private float vertComp[] ;
  private short compIndex[] ;
  
  private int total = 0 ;
  private Texture lastTex = null ;
  private boolean wasLit = false ;
  private ShaderProgram shading ;
  
  
  
  public CutoutsPass(Rendering rendering) {
    this.rendering = rendering;
    
    compiled = new Mesh(
      Mesh.VertexDataType.VertexArray,
      false,
      MAX_SPRITES * 4, MAX_SPRITES * 6,
      VertexAttribute.Position(),
      VertexAttribute.Color(),
      VertexAttribute.TexCoords(0)
    );
    vertComp = new float[COMPILE_LIMIT];
    compIndex = new short[MAX_SPRITES * 6];
    
    for (int i = 0; i < compIndex.length ; i++) {
      compIndex[i] = (short) (((i / 6) * 4) + VERT_INDICES[i % 6]);
    }
    compiled.setIndices(compIndex) ;
    
    shading = new ShaderProgram(
      Gdx.files.internal("shaders/cutouts.vert"),
      Gdx.files.internal("shaders/cutouts.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog()) ;
    }
  }
  
  
  public void dispose() {
    compiled.dispose();
    shading.dispose();
  }
  
  
  
  /**  Rendering methods-
    */
  protected void register(CutoutSprite sprite) {
    inPass.add(sprite);
  }
  
  
  public void performPass() {
    final Table <ModelAsset, Batch <CutoutSprite>> subPasses = new Table();
    
    for (CutoutSprite s : inPass) {
      Batch <CutoutSprite> batch = subPasses.get(s.model());
      if (batch == null) subPasses.put(s.model(), batch = new Batch());
      batch.add(s);
    }
    
    for (Batch <CutoutSprite> subPass : subPasses.values()) {
      //  TODO:  Try using multi-texturing here instead.  Ought to be more
      //  efficient, and probably less bug-prone.
      for (CutoutSprite s : subPass) {
        compileSprite(s, rendering.camera(), false);
      }
      compileAndRender(rendering.camera());
      for (CutoutSprite s : subPass) {
        compileSprite(s, rendering.camera(), true);
      }
      compileAndRender(rendering.camera());
    }
    clearAll();
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
  
  
  private void compileSprite(
    CutoutSprite s, Camera camera, boolean lightPass
  ) {
    final Texture keyTex = lightPass ? s.model.lightSkin : s.model.texture;
    if (keyTex == null) return;
    if (keyTex != lastTex || lightPass != wasLit || total >= COMPILE_LIMIT) {
      compileAndRender(camera);
    }
    
    for (int off = 0 ; off < SIZE ; off += VERTEX_SIZE) {
      final int offset = total + off;
      temp.set(
        s.model.vertices[X0 + off],
        s.model.vertices[Y0 + off],
        s.model.vertices[Z0 + off]
      );
      temp.scl(s.scale);
      rendering.view.worldToGL(s.position, temp2);
      temp.add(temp2);
      vertComp[X0 + offset] = temp.x;
      vertComp[Y0 + offset] = temp.y;
      vertComp[Z0 + offset] = temp.z;
      
      final Colour fog = Colour.greyscale(s.fog);
      final float colourBits;
      if (s.colour == null) colourBits = fog.bitValue;
      else if (! s.colour.blank()) colourBits = s.colour.bitValue;
      else colourBits = Colour.combineAlphaBits(fog, s.colour);
      
      vertComp[C0 + offset] = colourBits;
      vertComp[U0 + offset] = s.model.vertices[U0 + off];
      vertComp[V0 + offset] = s.model.vertices[V0 + off];
    }
    
    total += SIZE;
    lastTex = keyTex;
    wasLit = lightPass;
  }
  
  
  private void compileAndRender(Camera camera) {
    if (total == 0 || lastTex == null) return;
    compiled.setVertices(vertComp, 0, total);
    
    shading.begin();
    shading.setUniformMatrix("u_camera", camera.combined);
    shading.setUniformi("u_texture", 0);
    
    if (wasLit) {
      shading.setUniform4fv("u_lighting", GLOW_LIGHTS, 0, 4);
    }
    else {
      final float lightSum[] = rendering.lighting.lightSum();
      shading.setUniform4fv("u_lighting", lightSum, 0, 4);
    }
    
    lastTex.bind(0);
    compiled.render(shading, GL10.GL_TRIANGLES, 0, (total * 6) / SIZE);
    shading.end();

    total = 0 ;
  }
}



