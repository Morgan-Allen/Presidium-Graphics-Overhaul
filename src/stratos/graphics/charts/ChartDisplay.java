


package stratos.graphics.charts;
import stratos.graphics.common.*;
import static stratos.graphics.common.GL.*;
import stratos.util.*;


//  TODO:  Try breaking this into separate displays.  And you'll need the
//  ability to key selectables off particular regions/field objects- plus an
//  info panel by the side.


public class ChartDisplay {
  
  
  final Rendering rendering;
  final public PlanetDisplay planet;
  final public StarField starfield;
  
  
  public ChartDisplay(Rendering rendering) {
    this.rendering = rendering;
    planet = new PlanetDisplay(this);
    starfield = new StarField(this);
  }
  
  
  public void dispose() {
    planet.dispose();
    starfield.dispose();
  }
  
  
  public void renderWithin(Box2D area) {
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    //planet.render();
    starfield.render();
  }
}






