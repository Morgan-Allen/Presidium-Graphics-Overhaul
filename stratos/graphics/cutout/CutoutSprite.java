

package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.util.*;




public class CutoutSprite extends Sprite {
  
  
  final public static int
    PASS_SPLAT   = 0,
    PASS_NORMAL  = 1,
    PASS_PREVIEW = 2;
  
  protected CutoutModel model ;
  public int passType ;
  
  
  
  public CutoutSprite(CutoutModel model) {
    this.model = model ;
    this.passType = model.splat ? PASS_SPLAT : PASS_NORMAL;
  }
  
  
  public ModelAsset model() {
    return model ;
  }
  
  
  public void setModel(CutoutModel model) {
    this.model = model;
  }
  
  
  public void setAnimation(String animName, float progress) {}
  
  
  public void readyFor(Rendering rendering) {
    rendering.cutoutsPass.register(this);
  }
}

