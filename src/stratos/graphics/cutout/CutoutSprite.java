/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.util.*;
import java.io.*;



public class CutoutSprite extends Sprite {
  
  protected CutoutModel model;
  protected int faceIndex = 0;
  
  
  public CutoutSprite(CutoutModel model, int faceIndex) {
    this.model = model;
    this.passType = model.splat ? PASS_SPLAT : PASS_NORMAL;
    this.faceIndex = faceIndex;
  }
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.write(faceIndex);
  }
  
  
  protected void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    faceIndex = in.read();
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  public void setModel(CutoutModel model) {
    this.model = model;
  }
  
  
  public void setAnimation(String animName, float progress, boolean loop) {}
  
  
  public void readyFor(Rendering rendering) {
    if (passType == PASS_NORMAL && model.splat) passType = PASS_SPLAT;
    rendering.cutoutsPass.register(this);
  }
}







