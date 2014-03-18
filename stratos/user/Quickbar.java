

package stratos.user ;
import org.lwjgl.input.Keyboard;

import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;



public class Quickbar extends UIGroup implements UIConstants {
  
  final static int
    BUT_SIZE = 40,
    SPACING  = 2 ;
  
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
  //
  //  TODO:  Export this to a dedicated PowersTab class.  Probably.
  
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
      preview = new Image(UI, power.buttonImage) {
        protected UINode selectionAt(Vec2D mousePos) {
          return null ;
        }
      } ;
      preview.attachTo(UI) ;
      preview.relAlpha = 0.5f ;
    }
    
    
    public void doTask() {
      final boolean clicked = UI.mouseClicked() ;
      Object hovered = UI.selection.hovered() ;
      if (hovered == null) hovered = UI.selection.pickedTile() ;
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
      UI, MissionsTab.STRIKE_ICON.asTexture(),
      "Strike Mission\n  Destroy, capture or neutralise a chosen target"
    ) {
      public void whenClicked() { MissionsTab.initStrikeTask(bar.UI) ; }
    } ;
    addToSlot(strikeMB, missionGroup, missionSlots) ;
    
    final Button reconMB = new Button(
      UI, MissionsTab.RECON_ICON.asTexture(),
      "Recon Mission\n  Explore a given area or follow a chosen subject"
    ) {
      public void whenClicked() { MissionsTab.initReconTask(bar.UI) ; }
    } ;
    addToSlot(reconMB, missionGroup, missionSlots) ;
    
    final Button securityMB = new Button(
      UI, MissionsTab.SECURITY_ICON.asTexture(),
      "Security Mission\n  Protect a given area, structure or subject"
    ) {
      public void whenClicked() { MissionsTab.initSecurityTask(bar.UI) ; }
    } ;
    addToSlot(securityMB, missionGroup, missionSlots) ;
    
    final Button contactMB = new Button(
      UI, MissionsTab.CONTACT_ICON.asTexture(),
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
    
    createGuildButton(
      "militant_category_button", "Militant Structures",
      0, installGroup, installSlots
    ) ;
    createGuildButton(
      "merchant_category_button", "Merchant Structures",
      1, installGroup, installSlots
    ) ;
    createGuildButton(
      "aesthete_category_button", "Aesthete Structures",
      2, installGroup, installSlots
    ) ;
    createGuildButton(
      "artificer_category_button", "Artificer Structures",
      3, installGroup, installSlots
    ) ;
    createGuildButton(
      "ecologist_category_button", "Ecologist Structures",
      4, installGroup, installSlots
    ) ;
    createGuildButton(
      "physician_category_button", "Physician Structures",
      5, installGroup, installSlots
    ) ;
    
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
    final Button button = new Button(
      UI, BUTTONS_PATH+img+".png", help
    ) {
      protected void whenClicked() {
        final BaseUI UI = BaseUI.current();
        UI.beginPanelFade() ;
        if (UI.currentPanel() == newTab) {
          UI.setInfoPanel(null, null) ;
        }
        else UI.setInfoPanel(newTab, null) ;
      }
    } ;
    button.stretch = true ;
    
    button.absBound.set(0, 0, INFO_AREA_WIDE / 6f, BUT_SIZE) ;
    final Button last = installSlots.last() ;
    if (last != null) button.absBound.xpos(last.absBound.xmax()) ;
    installSlots.add(button) ;
    button.attachTo(installGroup) ;
    //addToSlot(button, installGroup, installSlots) ;
  }
}












