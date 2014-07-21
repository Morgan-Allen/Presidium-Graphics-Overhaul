

package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;



public class DebugCharts extends VisualDebug {
  
  
  private HUD UI;
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCharts());
  }
  
  
  protected void loadVisuals() {
    UI = new HUD(PlayLoop.rendering());
    //*
    StarsPanel starsPanel = new StarsPanel(UI);
    starsPanel.alignAcross(0, 0.5f);
    starsPanel.alignDown  (0, 1   );
    starsPanel.attachTo(UI);
    starsPanel.loadStarfield("media/Charts/", "coordinates.xml");
    //*/
    /*
    PlanetPanel planetPanel = new PlanetPanel(UI);
    planetPanel.relBound.set(0.0f, 0, 0.5f, 1);
    //planetPanel.absBound.set(50, 50, -100, -100);
    planetPanel.attachTo(UI);
    planetPanel.loadPlanet("media/Charts/", "sectors.xml");
    //*/
  }


  public HUD UI() { return UI; }
  protected void onRendering(Sprite sprite) {}
}








