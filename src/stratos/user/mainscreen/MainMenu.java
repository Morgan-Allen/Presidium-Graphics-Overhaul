/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.user.*;
import stratos.graphics.common.Colour;
import stratos.graphics.widgets.*;
import stratos.util.Description.*;
import stratos.start.*;
import stratos.util.*;




public class MainMenu extends MenuPane implements UIConstants {
  
  
  public MainMenu(HUD UI) {
    super(UI, MainScreen.MENU_INIT);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    
    final Image banner = new Image(UI, "media/Help/start_banner.png");
    banner.stretch = false;
    banner.expandToTexSize(1, false);
    listing.add(banner);
    
    listing.add(createTextButton("  New Game", 1, new Link() {
      public void whenClicked(Object context) { enterNewGameFlow(); }
    }));
    
    listing.add(createTextButton("  Tutorial", 1, new Link() {
      public void whenClicked(Object context) { enterTutorial(); }
    }));
    
    listing.add(createTextButton("  Continue Game", 1, new Link() {
      public void whenClicked(Object context) { enterSavesList(); }
    }));
    
    listing.add(createTextButton("  Info & Credits", 1, new Link() {
      public void whenClicked(Object context) { enterCredits(); }
    }));
    
    listing.add(createTextButton("  Quit", 1, new Link() {
      public void whenClicked(Object context) { enterQuitFlow(); }
    }));
  }
  
  
  public void enterNewGameFlow() {
    final SelectSitePane sitePane = new SelectSitePane(UI);
    navigateForward(sitePane, true);
  }
  
  
  public void enterTutorial() {
    final TutorialScenario tutorial = new TutorialScenario("tutorial_quick");
    PlayLoop.setupAndLoop(tutorial);
  }
  
  
  public void enterSavesList() {
    final SavesListPane savesPane = new SavesListPane(UI);
    navigateForward(savesPane, true);
  }
  
  
  public void enterCredits() {
    final MenuPane creditsPane = new MenuPane(UI, MainScreen.MENU_CREDITS) {
      
      protected void fillListing(List <UINode> listing) {
        final String text = XML.load(
          "media/Help/GameCredits.xml"
        ).matchChildValue("name", "Credits").child("content").content();
        
        listing.add(createTextItem("Credits:", 1.2f, null, 1));
        listing.add(createTextItem(text, 0.75f, Colour.LITE_GREY, 0));
      }
    };
    navigateForward(creditsPane, true);
  }
  
  
  public void enterQuitFlow() {
    final MenuPane confirmPane = new MenuPane(UI, MainScreen.MENU_QUIT) {
      
      protected void fillListing(List <UINode> listing) {
        listing.add(createTextItem(
          "Are you sure you want to quit?", 1.0f, null, 1
        ));
        listing.add(createTextButton("  Just quit already", 1, new Link() {
          public void whenClicked(Object context) {
            PlayLoop.exitLoop();
          }
        }));
        listing.add(createTextButton("  Maybe not", 1, new Link() {
          public void whenClicked(Object context) {
            navigateBack();
          }
        }));
      }
    };
    navigateForward(confirmPane, true);
  }
}




