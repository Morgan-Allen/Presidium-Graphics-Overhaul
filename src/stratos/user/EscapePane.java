

package stratos.user;
import stratos.start.*;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.widgets.*;
import stratos.util.*;
import stratos.util.Description.Link;



//  This affords options to exit, restart, load from save, change difficulty,
//  enter debug mode, or skip ahead in time.


public class EscapePane extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    ESCAPE_BUTTON_TEX = ImageAsset.fromImage(
      EscapePane.class, "media/GUI/Panels/game_options_tab.png"
    ),
    ESCAPE_ICON_LIT = Button.CIRCLE_LIT,
    //BORDER_TEX = SelectionInfoPane.BORDER_TEX;
    //*
    BORDER_TEX = ImageAsset.fromImage(
      EscapePane.class, "media/GUI/Panel.png"
    );
  //*/
  

  final Scenario played;
  final Text text;
  final Bordering bordering;
  
  
  private EscapePane(BaseUI UI, Scenario played) {
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
  

  
  static Button createButton(final BaseUI UI, Scenario played) {
    final EscapePane pane = new EscapePane(UI, played);
    final Button button = new Button(
      UI, ESCAPE_BUTTON_TEX, ESCAPE_ICON_LIT, "Game Options"
    ) {
      protected void whenClicked() {
        ((BaseUI) UI).setInfoPanels(pane, null);
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
    
    
    //  TODO:  Allow the same interface here to be accessed directly from the
    //         Main Menu- allow with any associated Psi costs.
    
    //  TODO:  Make the file-path system more transparent and consistent!
    text.append("\n\nLoad Earlier Save:");
    
    final String prefix = played.savesPrefix();
    for (final String path : Scenario.savedFiles(prefix)) {
      if (path.endsWith("current.rep")) continue;
      
      final String titlePath = path.substring(
        prefix.length(), path.length() - ".rep".length()
      );
      text.append("\n  ");
      text.append(new Link(titlePath) { public void whenClicked() {
        Scenario.loadGame("saves/"+path, true);
      }});
    }
  }
  
}



//  What about pausing/unpausing?  There should be a button for that...  ...No,
//  there shouldn't be, excepting in debug mode.










