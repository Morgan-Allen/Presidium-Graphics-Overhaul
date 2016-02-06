

package stratos.user;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.graphics.widgets.*;
import stratos.util.*;


//
//  An options pane for pause, fast-forward, slow-motion, save and load options.


public class ProgressOptions extends UIGroup implements UIConstants {
  
  
  final BaseUI BUI;
  
  
  ProgressOptions(BaseUI UI) {
    super(UI);
    this.BUI = UI;
    setup();
  }
  
  

  private void setup() {
    //  TODO:  Just get some dedicated buttons in for this instead- these
    //  should be available regardless of the ruler being present!
    
    //  ...That should arguably be true for Powers in general, though- at least
    //  for debug purposes.
    
    final Power PROGRESS_POWERS[] = {
      Power.FORESIGHT    ,
      Power.REMEMBRANCE  ,
      Power.TIME_DILATION,
    };
    Batch <UIGroup> options = new Batch();
    final Actor ruler = null;// BUI.played().ruler();
    
    for (Power power : PROGRESS_POWERS) {
      UIGroup option = PowersPane.createButtonGroup(BUI, power, ruler, ruler);
      options.add(option);
    }
    
    final int sizeB = OPT_BUTTON_SIZE, spaceB = sizeB + OPT_MARGIN;
    int across = 100;
    for (UINode option : options) {
      option.alignToArea(across, 0, sizeB, sizeB);
      option.attachTo(this);
      across += spaceB;
    }
  }
  
  
}










