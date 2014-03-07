

package src.graphics.sfx;
import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.* ;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.* ;



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
  } ;
  
  
  final Rendering rendering;
  final Batch <SFX> inPass = new Batch <SFX> ();
  
  private Mesh compiled ;
  private float vertComp[] ;
  private short compIndex[] ;
  
  private int total = 0 ;
  private Texture lastTex = null ;
  private ShaderProgram shading ;
  private static Vector3 temp = new Vector3(), temp2 = new Vector3();
  
  
  
  public SFXPass(Rendering rendering) {
    this.rendering = rendering;
    
    compiled = new Mesh(
      Mesh.VertexDataType.VertexArray,
      false,
      MAX_QUADS * 4, MAX_QUADS * 6,
      
      VertexAttribute.Position(),
      VertexAttribute.Color(),
      VertexAttribute.TexCoords(0)
    );
    vertComp = new float[COMPILE_LIMIT] ;
    compIndex = new short[MAX_QUADS * 6] ;
    
    for (int i = 0, v = 0 ; i < compIndex.length ; v += 4) {
      compIndex[i++] = (short) (v + 0) ;
      compIndex[i++] = (short) (v + 2) ;
      compIndex[i++] = (short) (v + 1) ;
      compIndex[i++] = (short) (v + 1) ;
      compIndex[i++] = (short) (v + 2) ;
      compIndex[i++] = (short) (v + 3) ;
    }
    compiled.setIndices(compIndex) ;
    
    shading = new ShaderProgram(
        Gdx.files.internal("shaders/sfx.vert"),
        Gdx.files.internal("shaders/sfx.frag")
    ) ;
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog()) ;
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
    for (SFX s : inPass) s.renderInPass(this);
    compileAndRender(rendering.camera());
    clearAll();
  }
  
  
  public void clearAll() {
    inPass.clear();
  }
  
  
  protected void compileQuad(
    Texture tex, Colour colour, Vec3D verts[],
    float umin, float vmin, float umax, float vmax
  ) {
    if (tex != lastTex || total > COMPILE_LIMIT) {
      compileAndRender(rendering.camera());
    }
    lastTex = tex;
    
    for (int i = 0 ; i < 4 ; i++) {
      final int offset = total + (i * VERT_SIZE);
      rendering.view.worldToGL(verts[i], temp);
      vertComp[X0 + offset] = temp.x;
      vertComp[Y0 + offset] = temp.y;
      vertComp[Z0 + offset] = temp.z;
      
      vertComp[C0 + offset] = colour == null ?
        Sprite.WHITE_BITS : colour.bitValue;
      
      final float u = QUAD_UV[i * 2], v = QUAD_UV[(i * 2) + 1];
      vertComp[U0 + offset] = ((1 - u) * umin) + (u * umax);
      vertComp[V0 + offset] = ((1 - v) * vmin) + (v * vmax);
    }
    
    total += QUAD_SIZE;
  }
  
  
  protected void compileQuad(
    Texture tex, Colour colour,
    float x, float y, float wide, float high,
    float umin, float vmin, float umax, float vmax,
    float zpos, boolean fromScreen
  ) {
    int i = 0 ; for (Vec3D v : SFX.verts) {
      v.set(
        x + (QUAD_VERTS[i++] * wide),
        y + ((1 - QUAD_VERTS[i++]) * high),
        zpos + QUAD_VERTS[i++]
      );
      //  TODO:  This still needs working on.  z coords in particular need to
      //  be preserved?
      if (fromScreen) {
        rendering.view.translateFromScreen(v);
        //v.scale(1f / rendering.view.screenScale());
      }
    }
    compileQuad(tex, colour, SFX.verts, umin, vmin, umax, vmax);
  }
  
  
  private void compileAndRender(Camera camera) {
    if (total == 0 || lastTex == null) return;
    compiled.setVertices(vertComp, 0, total);
    
    shading.begin();
    shading.setUniformMatrix("u_camera", camera.combined);
    shading.setUniformi("u_texture", 0);
    lastTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    lastTex.bind(0);
    compiled.render(shading, GL10.GL_TRIANGLES, 0, total / 4);
    shading.end();

    total = 0 ;
  }
}






//  TODO:  Try using an ImmediateModeRenderer for the sake of simplicity-
//  http:// ...com/badlogic/gdx/graphics/glutils/ImmediateModeRenderer.html
//  http://www.java-gaming.org/index.php?topic=30430.0

/*
ImmediateModeRenderer r;

... create()
   if (Gdx.graphics.isGL20Available()) {
       //normals=false, colors=true, numTexCoords=none 
       r = new ImmediateModeRenderer20(false, true, 0); 
   } else {
       r = new ImmediateModeRenderer10();
   }


... render()
   //enable srcOver blending
   Gdx.gl.glEnable(GL10.GL_BLEND);
   Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

   //for example, this can use your OrthographicCamera
   r.begin(camera.combined, GL20.GL_TRIANGLE_FAN);

   ...

   //push our vertex data here...

   //.. similar to glColor4f
   r.color(0f, 0f, 0f, intensity);
   
   //.. similar to glVertex3f
   r.vertex(center.x, center.y, depth);
   
   ...

   //flush the renderer
   r.end();
//*/









