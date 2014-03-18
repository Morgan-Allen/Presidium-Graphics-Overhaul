

package code.graphics.common;
import java.io.*;
import java.lang.reflect.Field;


//import src.util.Vec3D;

import code.util.*;

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
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    //  TODO:  FILL THESE IN
  }
  
  protected void loadFrom(DataInputStream in) throws Exception {
    //  TODO:  FILL THESE IN
  }
  
  
  public void matchTo(Sprite s) {
    position.setTo(s.position) ;
    scale = s.scale ;
    fog = s.fog ;
    colour = s.colour;
  }
  
  
  /**  Checking for valid animation names-
    */
  private static Table<String, String> validAnimNames = null;
  
  public static boolean isValidAnimName(String animName) {
    if (validAnimNames == null) {
      validAnimNames = new Table<String, String>(100);
      for (Field field : AnimNames.class.getFields())
        try {
          if (field.getType() != String.class)
            continue;
          final String value = (String) field.get(null);
          validAnimNames.put(value, value);
        } catch (Exception e) {
      }
    }
    return validAnimNames.get(animName) != null;
  }
  
  
  
  public abstract ModelAsset model();
  public abstract void setAnimation(String animName, float progress);
  public abstract void readyFor(Rendering rendering);
}






