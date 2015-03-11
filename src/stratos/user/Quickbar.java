/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//  TODO:  Get rid of, or heavily amend, this system.

public class Quickbar extends UIGroup implements UIConstants {
  
  
  final BaseUI UI;
  final Button inSlots[] = new Button[BAR_MAX_SLOTS];
  final UIGroup slotsGroup, guildsGroup;
  
  
  
  public Quickbar(BaseUI UI) {
    super(UI);
    this.UI = UI;
    
    slotsGroup = new UIGroup(UI);
    slotsGroup.alignLeft  (0, 0);
    slotsGroup.alignBottom(0, 0);
    slotsGroup.attachTo(this);
    
    guildsGroup = new UIGroup(UI);
    guildsGroup.alignRight (0, INFO_PANEL_WIDE);
    guildsGroup.alignBottom(0, 0              );
    guildsGroup.attachTo(this);
  }
  
  
  public void addToSlot(Button button, int slotID) {
    if (slotID >= BAR_MAX_SLOTS) I.complain("INVALID QUICKBAR SLOT: "+slotID);
    final Button prior = inSlots[slotID];
    if (prior != null) prior.detach();
    
    final int slotWide = BAR_BUTTON_SIZE + BAR_SPACING;
    button.alignBottom(0                , BAR_BUTTON_SIZE);
    button.alignLeft  (slotID * slotWide, BAR_BUTTON_SIZE);
    button.attachTo(slotsGroup);
    inSlots[slotID] = button;
  }
  
  
  protected void updateState() {
    super.updateState();
    
    for (int i = 0; i < Nums.min(9, BAR_MAX_SLOTS); i++) {
      final char key = (char) ('0' + i + 1);
      final Button b = inSlots[i];
      if ((b == null) || (! KeyInput.wasTyped(key))) continue;
      b.performAction();
    }
  }
}




