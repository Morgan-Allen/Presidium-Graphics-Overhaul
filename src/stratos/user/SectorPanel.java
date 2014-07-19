

package stratos.user;
import stratos.game.campaign.*;
import stratos.graphics.widgets.*;


//  TODO:  Try merging this with much of the code from StarsPanel or
//  PlanetPanel?

public class SectorPanel extends UIGroup {
  
  
  final Bordering border;
  Image portrait;
  final public Text header;
  final public Text detail;
  
  
  public SectorPanel(HUD UI) {
    super(UI);
    
    //final int
      //TM = 20, BM = 20,  //top and bottom margins
      //LM = 20, RM = 20;  //left and right margins
    
    this.border = new Bordering(UI, SelectionInfoPane.BORDER_TEX);
    border.left   = 20;
    border.right  = 20;
    border.bottom = 20;
    border.top    = 20;
    
    
    border.relBound.set(0, 0, 1, 1);
    border.attachTo(this);
    //portrait = new Image();
    
    header = new Text(UI, BaseUI.INFO_FONT);
    header.scale = 1.25f;
    header.relBound.set(0, 1, 1, 0);
    header.absBound.set(0, -50, 0, 50);
    header.attachTo(border.inside);
    
    detail = new Text(UI, BaseUI.INFO_FONT);
    detail.scale = 0.75f;
    detail.relBound.set(0, 0, 1, 1);
    detail.absBound.set(0, 0, 0, -50);
    detail.attachTo(border.inside);
  }
  
  
  
}




