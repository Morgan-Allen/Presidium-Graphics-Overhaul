

package graphics.cutout ;
import util.* ;
import static graphics.cutout.CutoutModel.* ;

import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g2d.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;
import com.badlogic.gdx.graphics.g3d.decals.* ;




public class CutoutSprite {
  
  
  final CutoutModel model ;
  public Vector3 position = new Vector3();
  
  public float scale = 1.0f ;
  public float colour = WHITE_BITS ;
  
  
  
  public CutoutSprite(CutoutModel model) {
    this.model = model ;
  }
  
  
  public void update() {
    
  }
  /*
  protected void setColor(Color tint) {
    //int intBits = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
    //float color = NumberUtils.intToFloatColor(intBits);
    float color = tint.toFloatBits();
  }
  //*/
}





