

package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;



public class DebugUI extends VisualDebug {
  
  
  private HUD UI;
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugUI());
  }
  
  
  protected void loadVisuals() {
    UI = new HUD(PlayLoop.rendering());
    
    final Text text = new Text(UI, UIConstants.INFO_FONT);
    
    text.alignHorizontal(0, 200, 110);
    text.alignDown      (0, 1);
    text.attachTo(UI);
    text.setText("");
    
    text.insert(ImageAsset.WHITE_TEX(), 25, 25, true);
    text.append(
      "QUICK BROWN FOX JUMPED OVER LAZY DOG"+
      "\n\nLorum Ipsum Dolor etc. etc. etc.  Make up whatever you like, really."
    );
    for (int n = 40; n-- > 0;) text.append("\nTT");
    text.append("\nFinished!");
    
    text.insert(ImageAsset.WHITE_TEX(), 40, 40, true);
    for (int n = 2; n-- > 0;) {
      text.append(" ");
      text.insert(ImageAsset.WHITE_TEX(), 25, 25, false);
    }
    
    final Scrollbar bar = text.makeScrollBar(SelectionPane.SCROLL_TEX);
    bar.alignHorizontal(0, 20, 210);
    bar.alignDown(0, 1);
    bar.attachTo(UI);
    
    
    /*
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








