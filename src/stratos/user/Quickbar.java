/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;


//  TODO:  The list of installations may have to be moved elsewhere, like a
//  dedicated installations pane.

//  In an impressions-style game, I generally go to a buildings-panel so I can
//  construct an entire neighbourhood with many different structure-types.  So
//  I want to be able to access them all easily at the same time.

//  If I were making gradual, incremental adjustments, then having the tools
//  for diagnosis and options to construct in the same pane would make sense.
//  But due to the front-loaded nature of planning, the latter rarely happens.




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
    guildsGroup.alignRight (0, GUILDS_WIDE);
    guildsGroup.alignBottom(0, 0          );
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
  
  

  /**  Meanwhile, on the other side of the bar, we have the installation
    *  buttons:
    */
  final static ImageAsset GUILD_IMAGE_ASSETS[] = ImageAsset.fromImages(
    Quickbar.class, BUTTONS_PATH,
    "militant_category_button.png",
    "merchant_category_button.png",
    "aesthete_category_button.png",
    "artificer_category_button.png",
    "ecologist_category_button.png",
    "physician_category_button.png"
  );
  //  TODO:  GIVE MORE EXTENSIVE HELP-INFO HERE!
  /*
  final static String GUILD_INFO[] = {
    "Militant Structures",
    "Merchant Structures",
    "Aesthete Structures",
    "Artificer Structures",
    "Ecologist Structures",
    "Physician Structures",
  };
  //*/
  
  
  protected void setupInstallButtons() {
    for (int i = 0; i < NUM_GUILDS; i++) {
      final String
        help    = INSTALL_CATEGORIES[i]+" Structures",
        catName = INSTALL_CATEGORIES[i];
      
      final InstallTab newTab = new InstallTab(UI, catName);
      final Button button = new Button(UI, GUILD_IMAGE_ASSETS[i], help) {
        
        protected void whenClicked() {
          ///I.say("\nWas clicked: "+catName);
          
          final BaseUI UI = BaseUI.current();
          UI.beginPanelFade();
          
          if (UI.currentPane() == newTab) {
            ///I.say("  Same panel?");
            UI.setInfoPanels(null, null);
          }
          else {
            ///I.say("  New panel...");
            UI.setInfoPanels(newTab, null);
          }
        }
        
      };
      button.stretch = true;
      
      final int maxB = INSTALL_CATEGORIES.length;
      final float place = i * 1f / maxB;
      button.alignBottom(0, BAR_BUTTON_SIZE);
      button.alignAcross(place, place + (1f / maxB));
      
      button.attachTo(guildsGroup);
      
    }
  }
}




