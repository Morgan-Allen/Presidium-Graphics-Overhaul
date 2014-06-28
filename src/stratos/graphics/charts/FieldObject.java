

package stratos.graphics.charts;
import stratos.util.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.apache.commons.math3.util.FastMath;



public class FieldObject {
  
  
  final public String label;
  
  TextureRegion texRegion;
  float fieldWide, fieldHigh, offX, offY;
  
  Vec3D coordinates;
  protected float depth;
  
  
  FieldObject(String label) {
    this.label = label;
  }
  
  
  public Vec3D coordinates() {
    return coordinates;
  }
  
  
  public float radius() {
    return FastMath.min(fieldWide, fieldHigh);
  }
  
  
  public String toString() { return ""+depth; }
}