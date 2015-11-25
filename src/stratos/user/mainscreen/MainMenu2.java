/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.user.*;
import stratos.graphics.widgets.*;
import stratos.util.Description.*;
import stratos.start.*;
import stratos.util.*;




public class MainMenu2 extends MenuPane implements UIConstants {
  
  
  
  
  public MainMenu2(HUD UI) {
    super(UI, MainScreen.MENU_INIT);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    listing.add(createTextButton("  New Game", 1, new Link() {
      public void whenClicked() { enterNewGameFlow(); }
    }));
    
    listing.add(createTextButton("  Tutorial", 1, new Link() {
      public void whenClicked() { enterTutorial(); }
    }));
    
    listing.add(createTextButton("  Continue Game", 1, new Link() {
      public void whenClicked() { enterSavesList(); }
    }));
    
    listing.add(createTextButton("  Info & Credits", 1, new Link() {
      public void whenClicked() { enterCredits(); }
    }));
    
    listing.add(createTextButton("  Quit", 1, new Link() {
      public void whenClicked() { enterQuitFlow(); }
    }));
  }
  
  
  public void enterNewGameFlow() {
    final NewGamePane nextPane = new NewGamePane(UI);
    navigateForward(nextPane, true);
  }
  
  
  public void enterTutorial() {
    final TutorialScenario tutorial = new TutorialScenario("tutorial_quick");
    PlayLoop.setupAndLoop(tutorial);
  }
  
  
  public void enterSavesList() {
    
  }
  
  
  public void enterCredits() {
    
  }
  
  
  public void enterQuitFlow() {
    
    final MenuPane confirmPane = new MenuPane(UI, MainScreen.MENU_QUIT) {
      
      protected void fillListing(List <UINode> listing) {
        listing.add(createTextItem(
          "Are you sure you want to quit?", 1.2f, null
        ));
        listing.add(createTextButton("  Just quit already", 1, new Link() {
          public void whenClicked() {
            PlayLoop.exitLoop();
          }
        }));
        listing.add(createTextButton("  Maybe not", 1, new Link() {
          public void whenClicked() {
            navigateBack();
          }
        }));
      }
    };
    navigateForward(confirmPane, true);
  }
  
  
  //  TODO:  Include these!
  /*
  public void configToContinue(Object args[]) {
    text.setText("");
    text.append("\nSaved Games:");
    GameOptionsPane.appendLoadOptions(text, null);
    Call.add("\n\nBack", this, "configMainText", text);
  }
  
  
  public void configForSettings(Object args[]) {
    text.setText("\nChange Settings\n");
    Call.add("\n  Back", this, "configMainText", text);
  }
  
  
  public void configInfo(Object args[]) {
    text.setText("");
    Call.add("\n\nBack", this, "configMainText", text);
    
    if (gameCredits == null) gameCredits = XML.load(
      "media/Help/GameCredits.xml"
    ).matchChildValue("name", "Credits").child("content");
    
    help.setText("");
    help.append(gameCredits.content(), Colour.LITE_GREY);
  }
  //*/
}











