

package stratos.graphics.charts;
import stratos.graphics.common.*;



//  TODO:  Is this class even necessary?  There's a lot less data associated
//  than for starfield objects.

//  TODO:  Try unifying with the FieldObject class?  Maybe?

public class DisplaySector {
  
  
  final public String label;
  Colour colourKey;
  
  
  DisplaySector(String label) {
    this.label = label;
  }
  
  
  public Colour key() { return colourKey; }
}

