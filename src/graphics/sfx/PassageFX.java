


package src.graphics.sfx ;
import org.lwjgl.opengl.GL11;

import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.util.* ;



/**  This class is intended to accomodate rendering of strip-mined terrain and
  *  underground or hidden passages.
  */
public class PassageFX extends SFX implements TileConstants {

  final static ModelAsset
    PASSAGE_MODEL = new ModelAsset("passage_model", PassageFX.class) {
      public boolean isLoaded() { return true ; }
      protected void loadAsset() {}
      protected void disposeAsset() {}
      public Sprite makeSprite() { return new PassageFX() ; }
    } ;
  public ModelAsset model() { return PASSAGE_MODEL ; }
  
  
  /*
  Texture tex ;
  MeshBuffer mesh ;
  
  
  public void setupWithArea(Box2D area, TileMask mask, Texture tex) {
    MeshBuffer.beginRecord() ;
    for (Coord c : Visit.grid(area)) {
      genTileFor(c.x, c.y, mask) ;
    }
    this.mesh = new MeshBuffer(MeshBuffer.compileRecord()) ;
    this.tex = tex ;
  }
  //*/

  
  protected void renderInPass(SFXPass pass) {
    //tex.bindTex() ;
    //mesh.renderTo(rendering) ;
  }
  
  
  
  /**  Geometry compilation helper methods/constants-
    */
  //
  //  Okay.  Depending on tile adjacency, you need to position the walls and
  //  floor to meet up with surrounding terrain.
  //  You have 8 polygons- 2 for each corner of the tile, meeting in the centre
  //  like a paper flower.
  //
  //   7  0  1
  //   6  -  2
  //   5  4  3
  
  final static Vec3D
    TILE_TEMPLATE[] = constructTileTemplate(),
    FLAT_NORMAL = new Vec3D(0, 0, 1),
    WALL_NORMALS[] = {
      new Vec3D( 0, -2, 1).normalise(),
      new Vec3D(-2,  0, 1).normalise(),
      new Vec3D( 0,  2, 1).normalise(),
      new Vec3D( 2,  0, 1).normalise()
    },
    FLOOR_UV_OFF = new Vec3D(1, 1, 0),
    WALLS_UV_OFF[] = {
      new Vec3D(1, 2, 0),
      new Vec3D(2, 1, 0),
      new Vec3D(1, 0, 0),
      new Vec3D(0, 1, 0),
    } ;
  
  
  private static Vec3D vertAtIndex(int n) {
    return new Vec3D(N_X[n] / 2f, N_Y[n] / 2f, 0) ;
  }
  
  
  private static Vec3D[] constructTileTemplate() {
    final Vec3D temp[] = new Vec3D[8 * 3] ;
    int i = 0 ; for (int n : N_INDEX) {
      temp[i++] = vertAtIndex((n + 1) % 8) ;
      temp[i++] = vertAtIndex(n) ;
      temp[i++] = new Vec3D(0, 0, 0) ;
    }
    return temp ;
  }
  
  
  /*
  private void genTileFor(int x, int y, TileMask mask) {
    
    if (! mask.maskAt(x, y)) return ;
    
    final int numV = TILE_TEMPLATE.length ;
    final float
      verts[] = new float[numV * 3],
      norms[] = new float[numV * 3],
      texts[] = new float[numV * 2] ;
    
    final boolean near[][] = new boolean[3][3] ;
    for (int n : N_INDEX) {
      near[N_X[n] + 1][N_Y[n] + 1] = mask.maskAt(x + N_X[n], y + N_Y[n]) ;
    }
    near[1][1] = true ;

    final float U = -0.25f ;
    int iV = 0, iN = 0, iT = 0 ;
    boolean mX, mY, mD, isFlat ;
    
    //
    //  Firstly, determine the position of vertices-
    for (int i = 0 ; i < TILE_TEMPLATE.length ; i++) {
      final Vec3D v = TILE_TEMPLATE[i] ;
      verts[iV++] = v.x + x ;
      verts[iV++] = v.y + y ;
      
      mD = near[1 + (int) (v.x * 2)][1 + (int) (v.y * 2)];
      mX = mY = true ;
      //*
      if (v.x < 0) mX &= near[0][1] ;
      if (v.x > 0) mX &= near[2][1] ;
      if (v.y < 0) mY &= near[1][0] ;
      if (v.y > 0) mY &= near[1][2] ;
      verts[iV++] = (mX && mY && mD) ? U : 0 ;
    }
    
    //
    //  Secondly, determine the proper normals/tex coords-
    for (int i = 0, wall = 0 ; i < TILE_TEMPLATE.length ; i++) {
      final int n = 3 * (i / 3) ;
      final boolean
        firstLow = verts[(n * 3) + 2] == U,
        otherLow = verts[(n * 3) + 5] == U ;
      isFlat = firstLow && otherLow ;
      //
      //  We vary the normals and tex coords depending on type...
      if (! isFlat) {
        wall = (i / 3) + 1 ;
        if (firstLow) wall-- ;
        if (otherLow) wall++ ;
        wall = (wall % 8) / 2 ;
      }
      final Vec3D
        v = TILE_TEMPLATE[i],
        norm  = isFlat ? FLAT_NORMAL : WALL_NORMALS[wall],
        offUV = isFlat ? FLOOR_UV_OFF : WALLS_UV_OFF[wall] ;
      
      norms[iN++] = norm.x ;
      norms[iN++] = norm.y ;
      norms[iN++] = norm.z ;
      texts[iT++] = Visit.clamp((v.x + offUV.x + 0.5f) / 3f, 0.01f, 0.99f) ;
      texts[iT++] = Visit.clamp((v.y + offUV.y + 0.5f) / 3f, 0.01f, 0.99f) ;
    }
    
    //  TODO:  Restore this functionality.
    //MeshBuffer.recordGeom(verts, norms, texts) ;
  }
  //*/
  
}






