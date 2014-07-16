

package stratos.user;
import stratos.game.campaign.*;
import stratos.graphics.widgets.*;


//  TODO:  Have this extend InfoPanel?

public class SectorPanel extends UIGroup {
  
  
  final Bordering border;
  Image portrait;
  final public Text header;
  final public Text detail;
  
  private Sector subject;
  
  
  public SectorPanel(HUD UI) {
    super(UI);
    
    final int
      TM = 40, BM = 40,  //top and bottom margins
      LM = 40, RM = 40;  //left and right margins
    
    this.border = new Bordering(UI, SelectionInfoPane.BORDER_TEX);
    border.left   = LM;
    border.right  = RM;
    border.bottom = BM;
    border.top    = TM;
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




