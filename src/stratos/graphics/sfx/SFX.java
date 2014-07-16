/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.graphics.sfx;
import java.io.*;

import stratos.graphics.common.*;
import stratos.util.*;




public abstract class SFX extends Sprite {
  
  
  
  /**  Basic methods overrides.
    */
  final static int
    PRIORITY_FIRST  = 0,
    PRIORITY_MIDDLE = 1,
    PRIORITY_LAST   = 2,
    ALL_PRIORITIES[] = {0, 1, 2};
  
  final int priorityKey;
  
  protected SFX(int priority) {
    this.priorityKey = priority;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
  }
  
  
  public void setAnimation(String animName, float progress, boolean loop) {
  }
  
  
  public void readyFor(Rendering rendering) {
    rendering.sfxPass.register(this);
  }
  
  
  protected abstract void renderInPass(SFXPass pass);
  
  
  /**  Intended as utility methods for performing actual rendering-
    */
  final protected static Vec3D verts[] = new Vec3D[] {
    new Vec3D(), new Vec3D(), new Vec3D(), new Vec3D()
  };
}







