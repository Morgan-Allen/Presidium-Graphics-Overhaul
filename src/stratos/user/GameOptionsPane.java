/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import com.badlogic.gdx.Input.Keys;

import stratos.start.*;
import stratos.game.common.GameSettings;
import stratos.graphics.common.Colour;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.widgets.*;
import stratos.util.*;
import stratos.util.Description.Link;



//  This affords options to exit, restart, load from save, change difficulty,
//  enter debug mode, or skip ahead in time.

public class GameOptionsPane extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    OPTIONS_ICON_TEX = ImageAsset.fromImage(
      GameOptionsPane.class, "options_pane_icon",
      "media/GUI/Panels/game_options_tab.png"
    ),
    OPTIONS_ICON_LIT = Button.CIRCLE_LIT,
    
    BORDER_TEX = ImageAsset.fromImage(
      GameOptionsPane.class, "options_pane_icon_border",
      "media/GUI/Front/Panel.png"
    );
  

  final Scenario played;
  final Text text;
  final Bordering bordering;
  final Scrollbar scrolling;
  
  
  private GameOptionsPane(BaseUI UI, Scenario played) {
    super(UI);
    setWidgetID(OPTIONS_PANE_ID);
    
    this.played = played;
    this.text = new Text(UI, INFO_FONT);
    this.bordering = new Bordering(UI, BORDER_TEX);
    
    this.alignHorizontal(0.5f, 400, 0);
    this.alignVertical  (0.5f, 550, 0);
    
    bordering.attachTo(this);
    text.alignAcross(0, 1);
    text.alignDown  (0, 1);
    text.attachTo   (this);
    bordering.surround(text);
    
    this.scrolling = text.makeScrollBar(SelectionPane.SCROLL_TEX);
    scrolling.alignToMatch(text);
    scrolling.alignRight(0 - SCROLLBAR_WIDE, SCROLLBAR_WIDE);
    scrolling.attachTo(bordering.inside);
  }
  
  
  static Button createButton(final BaseUI baseUI, Scenario played) {
    final GameOptionsPane pane = new GameOptionsPane(baseUI, played);
    
    final Button button = new Button(
      baseUI, OPTIONS_BUTTON_ID,
      OPTIONS_ICON_TEX, OPTIONS_ICON_LIT, "Game Options"
    ) {
      
      protected void whenClicked() {
        if (baseUI.currentInfoPane() == pane) {
          baseUI.clearInfoPane();
        }
        else {
          baseUI.setInfoPane(pane);
        }
      }
      
      protected void updateState() {
        super.updateState();
        this.hidden = false;// ! Scenario.isCurrentScenarioDebug();
        if (hidden) return;
        
        if (KeyInput.wasTyped(Keys.ESCAPE) && baseUI.currentTask() == null) {
          whenClicked();
        }
      }
    };
    return button;
  }
  
  
  protected void updateState() {
    super.updateState();
    text.setText("");
    final BaseUI baseUI = (BaseUI) UI;
    
    text.append("\nScenario Options:");
    
    text.append("\n  ");
    text.append(new Link("Save and Exit") { public void whenClicked(Object context) {
      I.say("EXITING GAME...");
      PlayLoop.setPaused(false);
      played.scheduleSaveAndExit();
    }});
    
    text.append("\n  ");
    text.append(new Link("Save and Resume") { public void whenClicked(Object context) {
      I.say("SAVING GAME...");
      PlayLoop.setPaused(false);
      played.scheduleSave();
      baseUI.clearInfoPane();
    }});
    
    text.append("\n  ");
    text.append(new Link("Restart") { public void whenClicked(Object context) {
      I.say("WILL RESTART...");
      PlayLoop.setPaused(false);
      played.scheduleReset();
    }});
    
    text.append("\n\nLoad Earlier Save:");
    appendLoadOptions(text, played.savesPrefix());
    
    text.append(
      "\n\n  (Note: Load or Restart will erase all later saves)",
      Colour.LITE_GREY
    );
    
    //
    //  TODO:  You will need to include a separate panel for these...
    text.append("\n\nDebug Options:");
    appendDebugOptions(text);
  }
  
  
  protected void appendDebugOptions(Text text) {
    for (final String option : GameSettings.publishSimpleOptions()) {
      final Object value = GameSettings.valueForOption(option);
      if (value == null) continue;
      
      text.append("\n  "+option+":  ", Colour.LITE_GREY);
      final boolean toggled = Boolean.TRUE.equals(value);
      
      text.append(new Description.Link(""+value) {
        public void whenClicked(Object context) {
          GameSettings.assignOptionValue(! toggled, option);
        }
      });
    }
  }
  
  
  public static void appendLoadOptions(Text text, String prefix) {
    //final boolean report = prefix == null || PlayLoop.isFrameIncrement(100);
    //
    //  TODO:  List any associated Psi costs here, based on time-stamp.
    final String saves[] = prefix == null ?
      SaveUtils.latestSaves()     :
      SaveUtils.savedFiles(prefix);
    
    for (final String path : saves) {
      final String titlePath;
      if (prefix == null) titlePath = SaveUtils.prefixFor(path);
      else                titlePath = SaveUtils.suffixFor(path);
      
      text.append("\n  ");
      text.append(new Link(titlePath) { public void whenClicked(Object context) {
        SaveUtils.loadGame(path, true);
      }});
    }
  }
}











