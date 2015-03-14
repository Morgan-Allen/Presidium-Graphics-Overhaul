/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import com.badlogic.gdx.Input.Keys;
import stratos.start.*;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.widgets.*;
import stratos.util.*;
import stratos.util.Description.Link;



//  This affords options to exit, restart, load from save, change difficulty,
//  enter debug mode, or skip ahead in time.

public class GameOptionsPane extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    OPTIONS_ICON_TEX = ImageAsset.fromImage(
      GameOptionsPane.class, "media/GUI/Panels/game_options_tab.png"
    ),
    OPTIONS_ICON_LIT = Button.CIRCLE_LIT,
    
    BORDER_TEX = ImageAsset.fromImage(
      GameOptionsPane.class, "media/GUI/Panel.png"
    );
  

  final Scenario played;
  final Text text;
  final Bordering bordering;
  
  
  private GameOptionsPane(BaseUI UI, Scenario played) {
    super(UI);
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
  }
  
  
  static Button createButton(final BaseUI baseUI, Scenario played) {
    final GameOptionsPane pane = new GameOptionsPane(baseUI, played);
    
    final Button button = new Button(
      baseUI, OPTIONS_ICON_TEX, OPTIONS_ICON_LIT, "Game Options"
    ) {
      
      protected void whenClicked() {
        if (baseUI.currentPane() == pane) {
          baseUI.setInfoPanels(null, null);
        }
        else {
          baseUI.setInfoPanels(pane, null);
        }
      }
      
      protected void updateState() {
        super.updateState();
        if (KeyInput.wasTyped(Keys.ESCAPE) && baseUI.currentTask() == null) {
          whenClicked();
        }
        final boolean paneOpen = baseUI.currentPane() == pane;
        PlayLoop.setPaused(paneOpen);
      }
    };
    return button;
  }
  
  
  protected void updateState() {
    super.updateState();
    text.setText("");
    
    text.append("\nScenario Options:");
    
    text.append("\n  ");
    text.append(new Link("Save and Exit") { public void whenClicked() {
      I.say("EXITING GAME...");
      played.scheduleSaveAndExit();
    }});
    
    text.append("\n  ");
    text.append(new Link("Save and Resume") { public void whenClicked() {
      I.say("SAVING GAME...");
      played.scheduleSave();
      ((BaseUI) UI).setInfoPanels(null, null);
    }});
    
    text.append("\n  ");
    text.append(new Link("Restart") { public void whenClicked() {
      I.say("WILL RESTART...");
      played.scheduleReset();
    }});
    
    //  TODO:  Make the file-path system more transparent and consistent!
    text.append("\n\nLoad Earlier Save:");
    appendLoadOptions(text, played.savesPrefix());
  }
  
  
  public static void appendLoadOptions(Text text, String prefix) {
    
    final int
      prefLength =  prefix == null ? 0 : prefix.length(),
      extLength  = (prefix == null ? "-current.rep" : ".rep").length();
    
    //  TODO:  List any associated Psi costs here (for non-current saves.)
    for (final String path : Scenario.savedFiles(prefix)) {
      final boolean current = path.endsWith("-current.rep");
      //if (prefix != null &&   current) continue;
      if (prefix == null && ! current) continue;
      
      String titlePath = path.substring(
        prefLength, path.length() - extLength
      );
      if (prefix != null && current) {
        titlePath = "Last Save";
      }
      
      text.append("\n  ");
      text.append(new Link(titlePath) { public void whenClicked() {
        Scenario.loadGame("saves/"+path, true);
      }});
    }
  }
}





