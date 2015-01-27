


package stratos.user;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;


//  TODO:  The list of installations may have to be moved elsewhere, like a
//  dedicated installations pane.

public class Quickbar extends UIGroup implements UIConstants {
  
  
  final static int
    BUTTON_SIZE = 40,
    SPACING     = 2 ;
  
  final static int
    NUM_GUILDS = 6,
    MAX_SLOTS  = 9;
  
  
  final BaseUI UI;
  final Button inSlots[] = new Button[MAX_SLOTS];
  final UIGroup slotsGroup, guildsGroup;
  private UIGroup optionList;
  
  
  
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
    if (slotID >= MAX_SLOTS) I.complain("INVALID QUICKBAR SLOT: "+slotID);
    final Button prior = inSlots[slotID];
    if (prior != null) prior.detach();
    
    button.alignBottom(0                               , BUTTON_SIZE);
    button.alignLeft  (slotID * (BUTTON_SIZE + SPACING), BUTTON_SIZE);
    button.attachTo(slotsGroup);
    inSlots[slotID] = button;
  }
  
  
  protected void updateState() {
    super.updateState();
    if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
      if (optionList != null) optionList.detach();
    }
  }
  
  
  
  /**  
    */
  class PowerTask implements UITask {
    
    final Power power;
    final String option;
    final Actor caster;
    final Image preview;
    
    final float PS = BUTTON_SIZE * 0.75f, HPS = PS / 2;
    
    
    PowerTask(Quickbar bar, Power p, String o, Actor c) {
      power = p;
      option = o;
      caster = c;
      //  TODO:  CREATE A DEDICATED CURSOR CLASS.
      preview = new Image(UI, power.buttonImage);
      preview.blocksSelect = false;
      preview.attachTo(UI);
      preview.relAlpha = 0.5f;
    }
    
    
    public void doTask() {
      final boolean clicked = UI.mouseClicked();
      Object hovered = UI.selection.hovered();
      if (hovered == null) hovered = UI.selection.pickedTile();
      
      preview.alignToArea(
        (int) (UI.mouseX() - HPS),
        (int) (UI.mouseY() - HPS),
        (int) PS, (int) PS
      );

      if (! (hovered instanceof Target)) hovered = null;
      final Target picked = (Target) hovered;
      if (power.finishedWith(caster, option, picked, clicked)) {
        cancelTask();
      }
      else if (clicked) cancelTask();
    }
    
    
    public void cancelTask() {
      UI.endCurrentTask();
      preview.detach();
    }
    
    //  TODO:  RESTORE THIS
    public ImageAsset cursorImage() { return null; }// power.buttonImage; }
  }
  
  
  //
  //  Ideally, you'll want a nicer way to present these- give them a little
  //  background, similar to text bubbles, and stretch to accommodate the
  //  longest string.
  //  TODO:  Better yet, just have single powers for each.
  
  private UIGroup constructOptionList(final Power power, String options[]) {
    final UIGroup list = new UIGroup(UI);
    final Quickbar bar = this;
    
    int i = 0; for (final String option : options) {
      final Text text = new Text(UI, UIConstants.INFO_FONT);
      text.append(new Description.Link(option) {
        public void whenClicked() {
          final Actor caster = UI.played().ruler();
          final PowerTask task = new PowerTask(bar, power, option, caster);
          UI.beginTask(task);
          optionList.detach();
        }
      }, Colour.GREY);
      text.alignBottom(i++ * 20, 16 );
      text.alignLeft  (0       , 300);
      text.attachTo(list);
    }
    optionList = list;
    return list;
  }
  
  
  protected void setupPowersButtons() {
    final Quickbar bar = this;
    int index = 0;
    
    for (final Power power : Power.BASIC_POWERS) {
      
      final Button button = new Button(
        UI, power.buttonImage,
        power.name.toUpperCase()+"\n  "+power.helpInfo
      ) {
        
        protected void whenClicked() {
          if (! enabled) return;
          final Actor caster = BaseUI.current().played().ruler();
          if (optionList != null) optionList.detach();
          //
          //  If there are options, display them instead.
          final String options[] = power.options();
          if (options != null) {
            constructOptionList(power, options);
            optionList.alignToMatch(this);
            optionList.alignBottom(BUTTON_SIZE + 2, 0);
            optionList.attachTo(bar);
            return;
          }
          else if (power.finishedWith(caster, null, null, true)) {
            return;
          }
          else {
            final PowerTask task = new PowerTask(bar, power, null, caster);
            BaseUI.current().beginTask(task);
          }
        }
        
        protected void updateState() {
          this.enabled = true;
          //  TODO:  Restore this dependancy (except for remembrance and
          //  foresight?)
          ///this.enabled = BaseUI.currentPlayed().ruler() != null;
          super.updateState();
        }

        protected String disableInfo() {
          return "  (Unavailable: No governor)";
        }
      };
      addToSlot(button, index++);
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
      button.alignBottom(0, BUTTON_SIZE);
      button.alignAcross(place, place + (1f / maxB));
      
      button.attachTo(guildsGroup);
      
    }
  }
}




