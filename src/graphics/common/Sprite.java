

package src.graphics.common;
import java.io.*;

//import src.util.Vec3D;
import src.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



public abstract class Sprite {

  final public static float
    WHITE_BITS = Color.WHITE.toFloatBits(),
    GREEN_BITS = Color.GREEN.toFloatBits(),
    RED_BITS   = Color.RED  .toFloatBits(),
    BLUE_BITS  = Color.BLUE .toFloatBits(),
    BLACK_BITS = Color.BLACK.toFloatBits(),
    CLEAR_BITS = new Color(0, 0, 0, 0).toFloatBits();
  
  
  final public Vec3D position = new Vec3D();
  public float scale = 1, rotation = 0;
  public float depth;
  public float fog = 1;
  public Colour colour = null;
  //public float colourBits = WHITE_BITS;
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    
  }
  
  protected void loadFrom(DataInputStream in) throws Exception {
    
  }
  
  
  public void matchTo(Sprite s) {
    position.setTo(s.position) ;
    scale = s.scale ;
    fog = s.fog ;
    colour = s.colour;
    //colourBits = s.colourBits ;
  }
  
  public void update() {
  }
  
  
  public abstract ModelAsset model();
  public abstract void setAnimation(String animName, float progress);
  public abstract void registerFor(Rendering rendering);
}






