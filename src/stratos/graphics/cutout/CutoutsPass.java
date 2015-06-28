/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.cutout;
import static stratos.graphics.common.GL.*;
import static stratos.graphics.cutout.CutoutModel.*;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.glutils.*;



//  TODO:  See if you can unify this with other types of rendering pass?

public class CutoutsPass {
	
  
  final static int
    MAX_SPRITES   = 1000,
    COMPILE_LIMIT = MAX_SPRITES * SIZE;
  
  private static Vector3 temp = new Vector3(), temp2 = new Vector3();
  private static Colour tempC = new Colour();
  final static float GLOW_LIGHTS[] = { 1, 1, 1, 1 };
  
  
  final Rendering rendering;
  final Batch <CutoutSprite>
    passSplat   = new Batch <CutoutSprite> (),
    passNormal  = new Batch <CutoutSprite> (),
    passPreview = new Batch <CutoutSprite> ();
  
  private Mesh compiled;
  private float vertComp[];
  private short compIndex[];
  
  private int total = 0;
  private Texture lastTex = null;
  private boolean wasLit = false;
  private ShaderProgram shading;
  
  
  
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
    
    for (int i = 0; i < compIndex.length; i++) {
      compIndex[i] = (short) (((i / 6) * 4) + VERT_INDICES[i % 6]);
    }
    compiled.setIndices(compIndex);
    
    shading = new ShaderProgram(
      Gdx.files.internal("shaders/cutouts.vert"),
      Gdx.files.internal("shaders/cutouts.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog());
    }
  }
  
  
  public void dispose() {
    compiled.dispose();
    shading.dispose();
  }
  
  
  
  /**  Rendering methods-
    */
  protected void register(CutoutSprite sprite) {
    switch (sprite.passType) {
      case (CutoutSprite.PASS_SPLAT  ): passSplat  .add(sprite); break;
      case (CutoutSprite.PASS_NORMAL ): passNormal .add(sprite); break;
      case (CutoutSprite.PASS_PREVIEW): passPreview.add(sprite); break;
    }
  }
  
  
  public void performSplatPass() {
    performPass(passSplat);
  }
  
  
  public void performNormalPass() {
    performPass(passNormal);
  }
  
  
  public void performPreviewPass() {
    ///I.say("total previews... "+passPreview.size());
    performPass(passPreview);
  }
  
  
  private void performPass(Batch <CutoutSprite> inPass) {
    if (inPass.size() == 0) return;
    final boolean report = false;
    if (report) {
      I.say("\nPerforming cutouts pass, total sprites: "+inPass.size());
    }
    
    final Table <ModelAsset, List <CutoutSprite>> subPasses = new Table();
    final Batch <CutoutSprite> ghosts = new Batch <CutoutSprite> ();
    final Stack <ModelAsset> sequence = new Stack <ModelAsset> ();
    
    for (CutoutSprite s : inPass) {
      if (s.colour != null && s.colour.transparent()) {
        ghosts.add(s);
        continue;
      }
      List <CutoutSprite> batch = subPasses.get(s.model());
      if (batch == null) {
        batch = new List <CutoutSprite> () {
          protected float queuePriority(CutoutSprite r) {
            return 0 - r.depth;
          }
        };
        subPasses.put(s.model(), batch);
        sequence.add(s.model());
      }
      s.depth = rendering.view.screenDepth(s.position);
      batch.add(s);
    }
    
    for (ModelAsset modelKey : sequence) {
      final List <CutoutSprite> subPass = subPasses.get(modelKey);
      subPass.queueSort();
      if (report) {
        I.say("  Rendering pass for "+modelKey);
        I.say("  Total sprites in pass: "+subPass.size());
      }
      //  TODO:  Try using multi-texturing here instead.  Ought to be more
      //         efficient, and probably less bug-prone.
      for (CutoutSprite s : subPass) {
        final boolean glow = s.colour != null && s.colour.glows();
        compileSprite(s, rendering.camera(), glow, s.model.texture);
      }
      compileAndRender(rendering.camera());
      for (CutoutSprite s : subPass) {
        compileSprite(s, rendering.camera(), true, s.model.lightSkin);
      }
      compileAndRender(rendering.camera());
    }
    
    for (CutoutSprite s : ghosts) {
      compileSprite(s, rendering.camera(), false, s.model.texture);
    }
    compileAndRender(rendering.camera());
    inPass.clear();
  }
  
  
  public void clearAll() {
    passSplat.clear();
    passNormal.clear();
    passPreview.clear();
  }
  
  
  private void compileSprite(
    CutoutSprite s, Camera camera, boolean lightPass, Texture keyTex
  ) {
    if (keyTex == null) return;
    
    final int faceIndex = Nums.clamp(s.faceIndex, s.model.allFaces.length);
    final float spriteVerts[] = s.model.allFaces[faceIndex];
    final int sizeS = SIZE * (spriteVerts.length / SIZE);
    if (
      keyTex    != lastTex ||
      lightPass != wasLit  ||
      (total + sizeS) >= COMPILE_LIMIT
    ) {
      compileAndRender(camera);
    }

    final Colour fog = Colour.greyscale(s.fog);
    float colourBits = 0;
    if      (  s.colour == null) colourBits = fog.floatBits;
    else if (  s.colour.glows()) colourBits = s.colour.floatBits;
    else if (! s.colour.blank()) colourBits = s.colour.floatBits;
    else colourBits = Colour.combineAlphaBits(fog, s.colour);
    
    for (int off = 0; off < sizeS; off += VERTEX_SIZE) {
      final int offset = total + off;
      temp.set(
        spriteVerts[X0 + off],
        spriteVerts[Y0 + off],
        spriteVerts[Z0 + off]
      );
      temp.scl(s.scale);
      Viewport.worldToGL(s.position, temp2);
      temp.add(temp2);
      vertComp[X0 + offset] = temp.x;
      vertComp[Y0 + offset] = temp.y;
      vertComp[Z0 + offset] = temp.z;
      vertComp[C0 + offset] = colourBits;
      vertComp[U0 + offset] = spriteVerts[U0 + off];
      vertComp[V0 + offset] = spriteVerts[V0 + off];
    }
    
    total   += sizeS;
    lastTex =  keyTex;
    wasLit  =  lightPass;
  }
  
  
  private void compileAndRender(Camera camera) {
    if (total == 0 || lastTex == null) return;
    compiled.setVertices(vertComp, 0, total);
    
    shading.begin();
    shading.setUniformMatrix("u_camera", camera.combined);
    shading.setUniformi("u_texture", 0);
    
    if (wasLit) {
      shading.setUniform4fv("u_lighting", GLOW_LIGHTS, 0, 4);
      shading.setUniformi("u_glowFlag", GL_TRUE);
    }
    else {
      final float lightSum[] = rendering.lighting.lightSum;
      shading.setUniform4fv("u_lighting", lightSum, 0, 4);
      shading.setUniformi("u_glowFlag", GL_FALSE);
    }
    
    lastTex.bind(0);
    compiled.render(shading, GL10.GL_TRIANGLES, 0, (total * 6) / SIZE);
    shading.end();
    total = 0;
  }
}




