/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.sfx ;
import java.io.* ;

//import org.lwjgl.opengl.GL11 ;

import src.util.* ;
import src.graphics.common.* ;




public abstract class SFX extends Sprite {
  
  
  
  /**  Basic methods overrides.
    */
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
  }
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
  }
  
  /*
  final static int GL_DISABLES[] = {
    GL11.GL_CULL_FACE,
    GL11.GL_LIGHTING,
    GL11.GL_ALPHA_TEST
  } ;
  //  TODO:  use GL11.glDepthMask() here, in a dedicated rendering pass for all
  //  SFX objects.  Remove it from the renderTex() method below...
  
  public int[] GL_disables() { return GL_DISABLES ; }
  //*/
  
  public void setAnimation(String animName, float progress) {
  }
  
  
  
  /**  Intended as utility methods for performing actual rendering-
    */
  /*
  final protected static Vec3D verts[] = new Vec3D[] {
    new Vec3D(), new Vec3D(), new Vec3D(), new Vec3D()
  } ;
  
  protected void renderTex(Vec3D[] verts, Texture tex) {
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    tex.bindTex() ;
    Vec3D v ;
    GL11.glDepthMask(false) ;
    GL11.glBegin(GL11.GL_QUADS) ;
      GL11.glTexCoord2f(0, 0) ;
      v = verts[0] ;
      GL11.glVertex3f(v.x, v.y, v.z) ;
      GL11.glTexCoord2f(0, 1) ;
      v = verts[1] ;
      GL11.glVertex3f(v.x, v.y, v.z) ;
      GL11.glTexCoord2f(1, 1) ;
      v = verts[2] ;
      GL11.glVertex3f(v.x, v.y, v.z) ;
      GL11.glTexCoord2f(1, 0) ;
      v = verts[3] ;
      GL11.glVertex3f(v.x, v.y, v.z) ;
    GL11.glEnd() ;
    GL11.glDepthMask(true) ;
  }
  //*/
}