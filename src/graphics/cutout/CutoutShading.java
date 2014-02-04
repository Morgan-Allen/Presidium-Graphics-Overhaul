

package graphics.cutout ;
import static graphics.common.GL.GL_FALSE;
import static graphics.common.GL.GL_TRUE;
import graphics.terrain.LayerType;
import graphics.terrain.TerrainChunk;
import util.Coord;
import util.I;
import util.Visit;

import com.badlogic.gdx.* ;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g2d.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;
import com.badlogic.gdx.graphics.glutils.* ;
import com.badlogic.gdx.graphics.g3d.* ;
import com.badlogic.gdx.graphics.g3d.shaders.* ;
import com.badlogic.gdx.graphics.g3d.utils.* ;



public class CutoutShading {
	
  
  final static int
    MAX_SPRITES   = 1000,
    COMPILE_LIMIT = MAX_SPRITES * Decal.SIZE ;
  
  private Mesh compiled ;
  private float vertComp[] ;
  private short compIndex[] ;
  
  private int total = 0 ;
  private Texture lastTex = null ;
  private ShaderProgram shading ;
  
  
  
  public CutoutShading() {
    compiled = new Mesh(
      false, MAX_SPRITES * 4, MAX_SPRITES * 6,
      VertexAttribute.Position(),
      VertexAttribute.Color(),
      VertexAttribute.TexCoords(0)
    ) ;
    vertComp = new float[COMPILE_LIMIT] ;
    compIndex = new short[MAX_SPRITES * 6] ;
    
    for (int i = 0, v = 0 ; i < compIndex.length ; v += 4) {
      compIndex[i++] = (short) (v + 0) ;
      compIndex[i++] = (short) (v + 2) ;
      compIndex[i++] = (short) (v + 1) ;
      compIndex[i++] = (short) (v + 1) ;
      compIndex[i++] = (short) (v + 2) ;
      compIndex[i++] = (short) (v + 3) ;
    }
    
    setupShading() ;
  }
  
  
  
  /**  Rendering methods-
    */
  private void setupShading() {
    shading = new ShaderProgram(
        Gdx.files.internal("shaders/cutouts.vert"),
        Gdx.files.internal("shaders/cutouts.frag")
    ) ;
    if (! shading.isCompiled()) {
      throw new GdxRuntimeException("\n"+shading.getLog()) ;
    }
  }
  
  
  public void compileSprite(Decal s, Camera camera) {
    if (s.model.texture != lastTex || total >= COMPILE_LIMIT) {
      compileAndRender(camera) ;
    }
    final int inc = s.vertices.length ;
    System.arraycopy(s.vertices, 0, vertComp, total, inc) ;
    total += inc ;
    lastTex = s.model.texture ;
  }
  
  
  public void compileAndRender(Camera camera) {
    if (total == 0 || lastTex == null) return ;
    compiled.setVertices(vertComp, 0, total) ;
    compiled.setIndices(compIndex) ;
    total = 0 ;
    
    shading.begin();
    
    shading.setUniformMatrix("u_camera", camera.combined);
    lastTex.bind(0) ;
    shading.setUniformi("u_texture", 0) ;
    
    compiled.render(shading, GL10.GL_TRIANGLES) ;
    shading.end() ;
  }
}



/*
  public void render(Camera camera, ShaderProgram shader, float time) {
    Gdx.gl.glEnable(GL10.GL_DEPTH_TEST);
    Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
    Gdx.gl.glEnable(GL10.GL_BLEND);
    Gdx.gl.glDepthMask(true);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    
    shader.begin();
    shader.setUniformMatrix("u_camera", camera.combined);
    
    if (fog != null) {
      fog.applyToShader(shader) ;
      shader.setUniformi("u_fogFlag", GL_TRUE);
    }
    else shader.setUniformi("u_fogFlag", GL_FALSE);
    
    for (LayerType type : layers) {
      layers[type.layerID].texture.bind(0) ;
      for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
        final TerrainChunk chunk = chunks[c.x][c.y][type.layerID];
        chunk.mesh.render(shader, GL20.GL_TRIANGLES);
      }
    }
    
    shader.end();
    if (fog != null) fog.checkBufferSwap(time);
  }
//*/


