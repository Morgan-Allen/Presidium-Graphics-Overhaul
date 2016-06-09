

package stratos.graphics.sfx;
import stratos.util.*;

//import static stratos.graphics.common.GL.*;
import static stratos.graphics.cutout.CutoutModel.VERT_INDICES;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;

import stratos.graphics.common.*;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.*;



public class SFXPass {
  
  
  //  TODO:  This is replicating a good deal of function from the CutoutsPass
  //  class.  Create a common superclass instead.
  final static int
    MAX_QUADS     = 1000,
    VERT_SIZE     = 3 + 1 + 2,  //  (position, colour and texture.)
    QUAD_SIZE     = VERT_SIZE * 4,
    COMPILE_LIMIT = MAX_QUADS * QUAD_SIZE,
    X0 = 0, Y0 = 1, Z0 = 2,
    C0 = 3, U0 = 4, V0 = 5;
  final static float QUAD_UV[] = {
    0, 1,
    1, 1,
    0, 0,
    1, 0
  };
  final static float QUAD_VERTS[] = {
    0, 1, 0,
    1, 1, 0,
    0, 0, 0,
    1, 0, 0
  };
  
  
  final Rendering rendering;
  final Batch <SFX> inPass = new Batch <SFX> ();
  
  //  TODO:  Key registered sprites to their textures, and render in batches
  //  based on those.
  
  
  private Mesh compiled;
  private float vertComp[];
  private short compIndex[];
  
  private int total = 0;
  private Texture lastTex = null;
  private ShaderProgram shading;

  private boolean vividMode = false;
  private static Vector3 temp = new Vector3();
  
  
  
  public SFXPass(Rendering rendering) {
    this.rendering = rendering;
    
    compiled = new Mesh(
      Mesh.VertexDataType.VertexArray,
      false,
      MAX_QUADS * 4, MAX_QUADS * 6,
      
      VertexAttribute.Position(),
      VertexAttribute.ColorPacked(),
      VertexAttribute.TexCoords(0)
    );
    vertComp  = new float[COMPILE_LIMIT];
    compIndex = new short[MAX_QUADS * 6];

    for (int i = 0; i < compIndex.length; i++) {
      compIndex[i] = (short) (((i / 6) * 4) + VERT_INDICES[i % 6]);
    }
    compiled.setIndices(compIndex);
    
    shading = new ShaderProgram(
        Gdx.files.internal("stratos/graphics/shaders/sfx.vert"),
        Gdx.files.internal("stratos/graphics/shaders/sfx.frag")
    );
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog());
    }
  }
  
  
  public void dispose() {
    compiled.dispose();
    shading.dispose();
  }
  
  
  
  
  protected void register(SFX sprite) {
    inPass.add(sprite);
  }
  
  
  public void performPass() {
    //  TODO:  Something similar should definitely be employed in the cutouts
    //  phase?  ModelBatch uses something similar, actually.
    final Table <ModelAsset, Batch <SFX>> subPasses = new Table();
    
    for (SFX s : inPass) {
      Batch <SFX> batch = subPasses.get(s.model());
      if (batch == null) subPasses.put(s.model(), batch = new Batch());
      batch.add(s);
    }
    for (int priority : SFX.ALL_PRIORITIES) {
      for (Batch <SFX> subPass : subPasses.values()) {
        if (subPass.first().priorityKey != priority) continue;
        for (SFX s : subPass) {
          s.renderInPass(this);
        }
        compileAndRender(rendering.camera());
      }
    }
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    clearAll();
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
  
  
  protected void compileQuad(
    Texture tex, Colour colour, boolean vivid, Vec3D verts[], Box2D frameUV
  ) {
    compileQuad(
      tex, colour, vivid, verts,
      frameUV.xpos(), frameUV.ypos(), frameUV.xmax(), frameUV.ymax()
    );
  }
  
  
  protected void compileQuad(
    Texture tex, Colour colour, boolean vivid,
    Vec3D verts[], float umin, float vmin, float umax, float vmax
  ) {
    if (tex != lastTex || vivid != vividMode || total >= COMPILE_LIMIT) {
      compileAndRender(rendering.camera());
    }
    if (vivid != vividMode) {
      if (vivid) {
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
      }
      else {
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
      }
      vividMode = vivid;
    }
    lastTex = tex;
    
    for (int i = 0; i < 4; i++) {
      final int offset = total + (i * VERT_SIZE);
      Viewport.worldToGL(verts[i], temp);
      vertComp[X0 + offset] = temp.x;
      vertComp[Y0 + offset] = temp.y;
      vertComp[Z0 + offset] = temp.z;
      
      vertComp[C0 + offset] = colour == null ?
        Sprite.WHITE_BITS : colour.floatBits;
      
      final float u = QUAD_UV[i * 2], v = QUAD_UV[(i * 2) + 1];
      vertComp[U0 + offset] = ((1 - u) * umin) + (u * umax);
      vertComp[V0 + offset] = ((1 - v) * vmin) + (v * vmax);
    }
    
    total += QUAD_SIZE;
  }
  
  
  protected void compileQuad(
    Texture tex, Colour colour, boolean vivid,
    float x, float y, float wide, float high,
    float umin, float vmin, float umax, float vmax,
    float zpos, boolean fromScreen
  ) {
    int i = 0; for (Vec3D v : SFX.verts) {
      v.set(
        x + (QUAD_VERTS[i++] * wide),
        y + ((1 - QUAD_VERTS[i++]) * high),
        zpos + QUAD_VERTS[i++]
      );
      //  TODO:  This still needs working on.  z coords in particular need to
      //  be preserved?
      if (fromScreen) rendering.view.translateFromScreen(v);
    }
    compileQuad(tex, colour, vivid, SFX.verts, umin, vmin, umax, vmax);
  }
  
  
  private void compileAndRender(Camera camera) {
    if (total == 0 || lastTex == null) return;
    compiled.setVertices(vertComp, 0, total);
    
    shading.begin();
    shading.setUniformMatrix("u_camera", camera.combined);
    shading.setUniformi("u_texture", 0);
    lastTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    lastTex.bind(0);
    compiled.render(shading, GL20.GL_TRIANGLES, 0, total / 4);
    shading.end();

    total = 0;
  }
}




