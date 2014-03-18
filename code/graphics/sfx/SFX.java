/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.graphics.sfx ;
import java.io.* ;

import code.graphics.common.*;
import code.util.*;




public abstract class SFX extends Sprite {
  
  
  
  /**  Basic methods overrides.
    */
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
  }
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
  }
  
  
  public void setAnimation(String animName, float progress) {
  }
  
  
  protected abstract void renderInPass(SFXPass pass);
  
  
  public void readyFor(Rendering rendering) {
    rendering.sfxPass.register(this);
  }
  
  
  
  /**  Intended as utility methods for performing actual rendering-
    */
  final protected static Vec3D verts[] = new Vec3D[] {
    new Vec3D(), new Vec3D(), new Vec3D(), new Vec3D()
  };
}







