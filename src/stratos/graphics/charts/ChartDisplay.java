


package stratos.graphics.charts;
import stratos.graphics.common.*;
//import static stratos.graphics.common.GL.*;
import stratos.util.*;



public class ChartDisplay {
  
  
  final Rendering rendering;
  final public StarField starfield;
  final public PlanetDisplay planet;
  
  
  public ChartDisplay(Rendering rendering) {
    this.rendering = rendering;
    starfield = new StarField(this);
    planet = new PlanetDisplay(this);
  }
  
  
  public void renderWithin(Box2D area) {
    starfield.render();
    planet.render();
  }
}

