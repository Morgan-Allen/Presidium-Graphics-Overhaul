


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
  //  TODO:  Also, the escape key needs to quit options-display.
  
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
          ///I.say(power.name+" CLICKED");
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
          else if (
            power.finishedWith(caster, null, null, true)
          ) return;
          else {
            ///I.say("Power needs a task...");
            final PowerTask task = new PowerTask(bar, power, null, caster);
            BaseUI.current().beginTask(task);
          }
        }
      };
      addToSlot(button, index++);
    }
  }
  
  

  /**  Meanwhile, on the other side of the bar, we have the installation
    *  buttons:
    */
  final static String GUILD_IMG_NAMES[] = {
    "militant_category_button",
    "merchant_category_button",
    "aesthete_category_button",
    "artificer_category_button",
    "ecologist_category_button",
    "physician_category_button",
  };
  final static String GUILD_INFO[] = {
    "Militant Structures",
    "Merchant Structures",
    "Aesthete Structures",
    "Artificer Structures",
    "Ecologist Structures",
    "Physician Structures",
  };
  final static Table <String, ImageAsset> GUILD_IMG_ASSETS;
  static {
    GUILD_IMG_ASSETS = new Table <String, ImageAsset> ();
    for (String name : GUILD_IMG_NAMES) {
      final ImageAsset asset = ImageAsset.fromImage(
        Quickbar.class, BUTTONS_PATH+name+".png"
      );
      GUILD_IMG_ASSETS.put(name, asset);
    }
  }
  
  
  protected void setupInstallButtons() {
    for (int i = 0; i < NUM_GUILDS; i++) {
      final String
        img     = GUILD_IMG_NAMES[i],
        help    = GUILD_INFO[i],
        catName = INSTALL_CATEGORIES[i];
      
      final InstallTab newTab = new InstallTab(UI, catName);
      final Button button = new Button(UI, GUILD_IMG_ASSETS.get(img), help) {
        protected void whenClicked() {
          final BaseUI UI = BaseUI.current();
          ///I.say("Guild button clicked...");
          UI.beginPanelFade();
          if (UI.currentPane() == newTab) {
            UI.setInfoPanels(null, null);
          }
          else UI.setInfoPanels(newTab, null);
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






/*
protected void setupMissionButtons() {
  final UIGroup missionGroup = new UIGroup(UI);
  final List <Button> missionSlots = new List <Button> ();
  final Quickbar bar = this;
  
  final Button strikeMB = new Button(
    UI, MissionsTab.STRIKE_ICON,
    "Strike Mission\n  Destroy, capture or neutralise a chosen target"
  ) {
    public void whenTextClicked() { MissionsTab.initStrikeTask(bar.UI); }
  };
  addToSlot(strikeMB, missionGroup, missionSlots);
  
  final Button reconMB = new Button(
    UI, MissionsTab.RECON_ICON,
    "Recon Mission\n  Explore a given area or follow a chosen subject"
  ) {
    public void whenTextClicked() { MissionsTab.initReconTask(bar.UI); }
  };
  addToSlot(reconMB, missionGroup, missionSlots);
  
  final Button securityMB = new Button(
    UI, MissionsTab.SECURITY_ICON,
    "Security Mission\n  Protect a given area, structure or subject"
  ) {
    public void whenTextClicked() { MissionsTab.initSecurityTask(bar.UI); }
  };
  addToSlot(securityMB, missionGroup, missionSlots);
  
  final Button contactMB = new Button(
    UI, MissionsTab.CONTACT_ICON,
    "Contact Mission\n  Establish better relations with the subject"
  ) {
    public void whenTextClicked() { MissionsTab.initContactTask(bar.UI); }
  };
  addToSlot(contactMB, missionGroup, missionSlots);
  
  final float length = this.lengthFor(missionSlots);
  missionGroup.relBound.set(0.55f, 0, 0, 0);
  missionGroup.absBound.set(-length / 2, 0, 0, 0);
  missionGroup.attachTo(this);
}
//*/
