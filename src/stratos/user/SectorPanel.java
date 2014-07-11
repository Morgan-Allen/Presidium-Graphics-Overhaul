

package stratos.user;
import stratos.graphics.widgets.*;


//  TODO:  Have this extend InfoPanel?

public class SectorPanel extends UIGroup {
  
  
  Bordering border;
  Image portrait;
  final public Text header;
  final public Text detail;
  
  
  public SectorPanel(HUD UI) {
    super(UI);
    
    //border = new Bordering();
    //portrait = new Image();
    
    header = new Text(UI, BaseUI.INFO_FONT);
    header.scale = 1.25f;
    header.relBound.set(0, 1, 1, 0);
    header.absBound.set(0, -50, 0, 50);
    header.attachTo(this);
    
    detail = new Text(UI, BaseUI.INFO_FONT);
    detail.scale = 0.75f;
    detail.relBound.set(0, 0, 1, 1);
    detail.absBound.set(0, 0, 0, -50);
    detail.attachTo(this);
  }
  
  
  
}





