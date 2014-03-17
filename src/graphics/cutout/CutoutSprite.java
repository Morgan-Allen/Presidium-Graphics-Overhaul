

package src.graphics.cutout;
import src.graphics.common.*;
import src.util.*;




public class CutoutSprite extends Sprite {
  
  
  final CutoutModel model ;
  
  
  public CutoutSprite(CutoutModel model) {
    this.model = model ;
  }
  
  
  public ModelAsset model() {
    return model ;
  }
  
  
  public void update() {}
  public void setAnimation(String animName, float progress) {}
  
  
  public void registerFor(Rendering rendering) {
    rendering.cutoutsPass.register(this);
  }
}




