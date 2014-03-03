

package src.graphics.cutout;
import src.graphics.common.*;
import src.util.*;
import static src.graphics.cutout.CutoutModel.*;

import com.badlogic.gdx.graphics.* ;
//import com.badlogic.gdx.graphics.g2d.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;
import com.badlogic.gdx.graphics.g3d.decals.* ;




public class CutoutSprite extends Sprite {
  
  
  final CutoutModel model ;
  
  
  public CutoutSprite(CutoutModel model) {
    this.model = model ;
  }
  
  
  public ModelAsset model() {
    return model ;
  }
  
  public void setAnimation(String animName, float progress) {
    
  }
  
  public void registerFor(Rendering rendering) {
    rendering.cutoutsPass.register(this);
  }
}




/*
protected void setColor(Color tint) {
  //int intBits = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
  //float color = NumberUtils.intToFloatColor(intBits);
  float color = tint.toFloatBits();
}
//*/




