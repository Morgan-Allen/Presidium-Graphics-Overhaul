

package stratos.user;
import stratos.game.base.*;
import stratos.graphics.widgets.*;



//  TODO:  Try merging this with much of the code from StarsPanel or
//  PlanetPanel?
public class SectorPanel extends UIGroup {
  
  
  final Bordering border;
  //Image portrait;
  final public Text header;
  final public Text detail;
  
  
  public SectorPanel(HUD UI) {
    super(UI);
    
    this.border = new Bordering(UI, SelectionPane.BORDER_TEX);
    border.setInsets(20, 20, 30, 30);
    border.setUV(0.2f, 0.2f, 0.3f, 0.3f);
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    //portrait = new Image();
    
    header = new Text(UI, BaseUI.INFO_FONT);
    header.scale = 1.25f;
    header.alignTop   (0, 50);
    header.alignAcross(0, 1 );
    header.attachTo(border.inside);
    
    detail = new Text(UI, BaseUI.INFO_FONT);
    detail.scale = 0.75f;
    detail.alignVertical(0, 50);
    detail.alignAcross  (0, 1 );
    detail.attachTo(border.inside);
  }
  
}




