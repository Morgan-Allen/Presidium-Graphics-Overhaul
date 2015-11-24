/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.user.*;
import stratos.graphics.common.Colour;
import stratos.graphics.widgets.*;
import stratos.start.PlayLoop;
import stratos.start.TutorialScenario;
import stratos.util.*;




public class MainMenu2 extends MenuPane implements UIConstants {
  
  
  
  
  public MainMenu2(HUD UI) {
    super(UI, MainScreen.MENU_INIT);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    
    final Text newGame = new Text(UI, INFO_FONT);
    Call.add("  New Game", this, "enterNewGameFlow", newGame);
    newGame.setToLineSize();
    listing.add(newGame);
    
    final Text tutorial = new Text(UI, INFO_FONT);
    Call.add("  Tutorial", this, "enterTutorial", tutorial);
    tutorial.setToLineSize();
    listing.add(tutorial);
    
    final Text contGame = new Text(UI, INFO_FONT);
    Call.add("  Continue Game", this, "enterSavesList", contGame);
    contGame.setToLineSize();
    listing.add(contGame);
    
    final Text credits = new Text(UI, INFO_FONT);
    Call.add("  Info & Credits", this, "enterCredits", credits);
    credits.setToLineSize();
    listing.add(credits);
    
    final Text quit = new Text(UI, INFO_FONT);
    Call.add("  Quit", this, "enterQuitFlow", quit);
    quit.setToLineSize();
    listing.add(quit);
  }
  
  
  public void enterNewGameFlow(Object args[]) {
    final NewGamePane nextPane = new NewGamePane(UI);
    navigateForward(nextPane, true);
  }
  
  
  public void enterTutorial(Object args[]) {
    final TutorialScenario tutorial = new TutorialScenario("tutorial_quick");
    PlayLoop.setupAndLoop(tutorial);
  }
  
  
  public void enterSavesList(Object args[]) {
    
  }
  
  
  public void enterCredits(Object args[]) {
    
  }
  
  
  public void enterQuitFlow(Object args[]) {
    
    final MenuPane confirmPane = new MenuPane(UI, MainScreen.MENU_QUIT) {
      
      protected void fillListing(List <UINode> listing) {
        final Text question = new Text(UI, INFO_FONT);
        question.append("\nAre you sure you want to quit?\n", Colour.LITE_GREY);
        question.scale = 0.75f;
        question.setToPreferredSize(1000);
        listing.add(question);
        
        final Text yes = new Text(UI, INFO_FONT);
        Call.add("  Just Quit Already", this, "quitConfirmed", yes);
        yes.setToLineSize();
        listing.add(yes);
        
        final Text no = new Text(UI, INFO_FONT);
        Call.add("  Back", this, "quitCancelled", no);
        no.setToLineSize();
        listing.add(no);
      }
      
      public void quitConfirmed(Object args[]) {
        PlayLoop.exitLoop();
      }
      
      public void quitCancelled(Object args[]) {
        this.navigateBack();
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
  
  
  public void configToQuit(Object args[]) {
    text.setText("\nAre you sure you want to quit?\n");
    Call.add("\n  Just Quit Already", this, "quitConfirmed", text);
    Call.add("\n  Back", this, "configMainText", text);
  }
  
  
  public void quitConfirmed(Object args[]) {
    PlayLoop.exitLoop();
  }
  
  //*/
}











