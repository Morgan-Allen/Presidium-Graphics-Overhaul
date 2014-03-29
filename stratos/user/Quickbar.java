


package stratos.user ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;



public class Quickbar extends UIGroup implements UIConstants {
  
  
  final static int
    BUT_SIZE = 40,
    SPACING  = 2 ;

  final static int NUM_GUILDS = 6;
  final static String GUILD_IMG_NAMES[] = {
    "militant_category_button",
    "merchant_category_button",
    "aesthete_category_button",
    "artificer_category_button",
    "ecologist_category_button",
    "physician_category_button",
  };
  final static String GUILD_TITLES[] = {
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
        BUTTONS_PATH+name+".png", Quickbar.class
      );
      GUILD_IMG_ASSETS.put(name, asset);
    }
  }
  
  
  final BaseUI UI ;
  private UIGroup optionList ;
  
  
  
  public Quickbar(BaseUI UI) {
    super(UI) ;
    this.UI = UI ;
  }
  
  
  private void addToSlot(
    Button button, UIGroup parent, List <Button> allSlots
  ) {
    final Button last = allSlots.last() ;
    button.absBound.set(0, 0, BUT_SIZE, BUT_SIZE) ;
    if (last != null) button.absBound.xpos(last.absBound.xmax() + SPACING) ;
    allSlots.add(button) ;
    button.attachTo(parent) ;
  }
  
  
  private float lengthFor(List <Button> allSlots) {
    return (allSlots.size() * (BUT_SIZE + SPACING)) - SPACING ;
  }
  
  
  protected void updateState() {
    super.updateState() ;
    if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
      if (optionList != null) optionList.detach() ;
    }
  }
  
  
  
  /**  
    */
  class PowerTask implements UITask {
    
    final Power power ;
    final String option ;
    final Actor caster ;
    final Image preview ;
    
    final float PS = BUT_SIZE * 0.75f, HPS = PS / 2 ;
    
    
    PowerTask(Quickbar bar, Power p, String o, Actor c) {
      power = p ;
      option = o ;
      caster = c ;
      
      //  The preview image can't return a selection, or nothing beneath will
      //  be picked.  TODO:  CREATE A DEDICATED CURSOR CLASS
      preview = new Image(UI, power.buttonImage) {
        protected UINode selectionAt(Vector2 mousePos) {
          return null ;
        }
      } ;
      preview.attachTo(UI) ;
      preview.relAlpha = 0.5f ;
    }
    
    
    public void doTask() {
      final boolean clicked = UI.mouseClicked();
      Object hovered = UI.selection.hovered();
      if (hovered == null) hovered = UI.selection.pickedTile();
      preview.absBound.set(
        UI.mouseX() - HPS,
        UI.mouseY() - HPS,
        PS, PS
      ) ;

      if (! (hovered instanceof Target)) hovered = null ;
      final Target picked = (Target) hovered ;
      if (power.finishedWith(caster, option, picked, clicked)) {
        cancelTask() ;
      }
    }
    
    
    public void cancelTask() {
      UI.endCurrentTask() ;
      preview.detach() ;
    }
    
    //  TODO:  RESTORE THIS
    public ImageAsset cursorImage() { return null ; }// power.buttonImage ; }
  }
  
  
  //
  //  Ideally, you'll want a nicer way to present these- give them a little
  //  background, similar to text bubbles, and stretch to accommodate the
  //  longest string.
  //  TODO:  Also, the escape key needs to quit options-display.
  
  private UIGroup constructOptionList(final Power power, String options[]) {
    final UIGroup list = new UIGroup(UI) ;
    final Quickbar bar = this ;
    
    int i = 0 ; for (final String option : options) {
      final Text text = new Text(UI, UIConstants.INFO_FONT) ;
      text.append(new Description.Link(option) {
        public void whenClicked() {
          final Actor caster = UI.played().ruler() ;
          final PowerTask task = new PowerTask(bar, power, option, caster) ;
          UI.beginTask(task) ;
          optionList.detach() ;
        }
      }, Colour.GREY) ;
      text.absBound.set(0, i++ * 20, 300, 16) ;
      text.attachTo(list) ;
    }
    optionList = list ;
    return list ;
  }
  
  
  protected void setupPowersButtons() {
    final Quickbar bar = this ;
    final UIGroup powerGroup = new UIGroup(UI) ;
    final List <Button> powerSlots = new List <Button> () ;
    
    for (final Power power : Power.BASIC_POWERS) {
      
      final Button button = new Button(
        UI, power.buttonImage,
        power.name.toUpperCase()+"\n  "+power.helpInfo
      ) {
        protected void whenClicked() {
          ///I.say(power.name+" CLICKED") ;
          final Actor caster = BaseUI.current().played().ruler() ;
          if (optionList != null) optionList.detach() ;
          //
          //  If there are options, display them instead.
          final String options[] = power.options() ;
          if (options != null) {
            constructOptionList(power, options) ;
            optionList.absBound.setTo(this.absBound) ;
            optionList.absBound.ypos(BUT_SIZE + 2) ;
            optionList.attachTo(bar) ;
            return ;
          }
          else if (
            power.finishedWith(caster, null, null, true)
          ) return ;
          else {
            ///I.say("Power needs a task...") ;
            final PowerTask task = new PowerTask(bar, power, null, caster) ;
            BaseUI.current().beginTask(task) ;
          }
        }
      } ;
      addToSlot(button, powerGroup, powerSlots) ;
    }
    
    powerGroup.attachTo(this) ;
  }
  
  
  protected void setupMissionButtons() {
    final UIGroup missionGroup = new UIGroup(UI) ;
    final List <Button> missionSlots = new List <Button> () ;
    final Quickbar bar = this;
    
    final Button strikeMB = new Button(
      UI, MissionsTab.STRIKE_ICON,
      "Strike Mission\n  Destroy, capture or neutralise a chosen target"
    ) {
      public void whenClicked() { MissionsTab.initStrikeTask(bar.UI) ; }
    } ;
    addToSlot(strikeMB, missionGroup, missionSlots) ;
    
    final Button reconMB = new Button(
      UI, MissionsTab.RECON_ICON,
      "Recon Mission\n  Explore a given area or follow a chosen subject"
    ) {
      public void whenClicked() { MissionsTab.initReconTask(bar.UI) ; }
    } ;
    addToSlot(reconMB, missionGroup, missionSlots) ;
    
    final Button securityMB = new Button(
      UI, MissionsTab.SECURITY_ICON,
      "Security Mission\n  Protect a given area, structure or subject"
    ) {
      public void whenClicked() { MissionsTab.initSecurityTask(bar.UI) ; }
    } ;
    addToSlot(securityMB, missionGroup, missionSlots) ;
    
    final Button contactMB = new Button(
      UI, MissionsTab.CONTACT_ICON,
      "Contact Mission\n  Establish better relations with the subject"
    ) {
      public void whenClicked() { MissionsTab.initContactTask(bar.UI) ; }
    } ;
    addToSlot(contactMB, missionGroup, missionSlots) ;
    
    final float length = this.lengthFor(missionSlots) ;
    missionGroup.relBound.set(0.55f, 0, 0, 0) ;
    missionGroup.absBound.set(-length / 2, 0, 0, 0) ;
    missionGroup.attachTo(this) ;
  }
  
  
  protected void setupInstallButtons() {
    final UIGroup installGroup = new UIGroup(UI) ;
    final List <Button> installSlots = new List <Button> () ;
    
    for (int i = 0 ; i < NUM_GUILDS ; i++) {
      createGuildButton(
        GUILD_IMG_NAMES[i], GUILD_TITLES[i],
        i, installGroup, installSlots
      ) ;
    }
    
    installGroup.relBound.set(1, 0, 0, 0) ;
    installGroup.absBound.set(-INFO_AREA_WIDE, 0, 0, 0) ;
    installGroup.attachTo(this) ;
  }
  

  private void createGuildButton(
    String img, String help, final int buttonID,
    UIGroup installGroup, List <Button> installSlots
  ) {
    final String catName = INSTALL_CATEGORIES[buttonID] ;
    final InstallTab newTab = new InstallTab(UI, catName) ;
    final Button button = new Button(UI, GUILD_IMG_ASSETS.get(img), help) {
      protected void whenClicked() {
        final BaseUI UI = BaseUI.current();
        UI.beginPanelFade() ;
        if (UI.currentPanel() == newTab) {
          UI.setInfoPanels(null, null) ;
        }
        else UI.setInfoPanels(newTab, null) ;
      }
    } ;
    button.stretch = true ;
    
    button.absBound.set(0, 0, INFO_AREA_WIDE / 6f, BUT_SIZE) ;
    final Button last = installSlots.last() ;
    if (last != null) button.absBound.xpos(last.absBound.xmax()) ;
    installSlots.add(button) ;
    button.attachTo(installGroup) ;
  }
}





