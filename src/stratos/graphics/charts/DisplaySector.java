

package stratos.graphics.charts;
import stratos.graphics.common.*;
import stratos.util.*;



//  TODO:  Is this class even necessary?  There's a lot less data associated
//  than for starfield objects.

public class DisplaySector {
  
  
  final public String label;
  
  Colour colourKey;
  Vec3D coordinates = new Vec3D();
  
  
  DisplaySector(String label) {
    this.label = label;
  }
  
  
  public Colour key() { return colourKey; }
}


