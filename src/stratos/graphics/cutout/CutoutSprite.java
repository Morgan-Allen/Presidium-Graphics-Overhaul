

package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.util.*;




public class CutoutSprite extends Sprite {
  
  
  protected CutoutModel model;
  
  
  
  public CutoutSprite(CutoutModel model) {
    this.model = model;
    this.passType = model.splat ? PASS_SPLAT : PASS_NORMAL;
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







