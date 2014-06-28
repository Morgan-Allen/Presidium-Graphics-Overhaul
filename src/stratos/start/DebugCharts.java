

package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;



public class DebugCharts extends VisualDebug {
  
  
  private HUD UI;
  private ChartsPanel chartsPanel;
  
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCharts());
  }
  
  
  protected void loadVisuals() {
    UI = new HUD(PlayLoop.rendering());
    chartsPanel = new ChartsPanel(UI);
    chartsPanel.relBound.set(0, 0, 1, 1);
    chartsPanel.attachTo(UI);
    
    chartsPanel.loadStarfield("media/Charts/", "coordinates.xml");
    chartsPanel.loadPlanet("media/Charts/", "sectors.xml");
  }
  
  
  public HUD UI() { return UI; }
  protected void onRendering(Sprite sprite) {}
}

