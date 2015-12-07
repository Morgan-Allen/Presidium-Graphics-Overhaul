/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import stratos.game.verse.*;
import stratos.util.*;



public class SelectSitePane extends MenuPane {
  
  
  final Expedition expedition;
  
  
  public SelectSitePane(HUD UI) {
    super(UI, MainScreen.MENU_NEW_GAME_SITE);
    expedition = new Expedition();
  }
  
  
  protected void fillListing(List <UINode> listing) {
    //
    //  Pick a homeworld first.
    listing.add(createTextItem("Homeworld:", 1.2f, null, 1));
    
    final VerseLocation homeworlds[] = Verse.ALL_CAPITALS;
    for (final VerseLocation homeworld : homeworlds) {
      listing.add(new TextButton(UI, "  "+homeworld.name, 1) {
        protected void whenClicked() { selectHomeworld(homeworld); }
        protected boolean toggled() { return hasHomeworld(homeworld); }
      });
    }
    listing.add(createTextItem(
      "Your homeworld will determine the initial colonists and finance "+
      "available to your settlement, along with technical expertise and "+
      "trade revenue.", 0.75f, Colour.LITE_GREY, 3
    ));
    //
    //  Then pick a sector.
    listing.add(createTextItem("Landing Site:", 1.2f, null, 1));

    final VerseLocation landings[] = Verse.ALL_DIAPSOR_SECTORS;
    for (final VerseLocation landing : landings) {
      listing.add(new TextButton(UI, "  "+landing.name, 1) {
        public void whenClicked() { selectLanding(landing); }
        protected boolean toggled() { return hasLanding(landing); }
      });
    }
    listing.add(createTextItem(
      "Your landing site will determine the type of resources initially "+
      "available to your settlement, along with local species and other "+
      "threats.", 0.75f, Colour.LITE_GREY, 3
    ));
    
    //
    //  And include an option to proceed further...
    listing.add(new TextButton(UI, "  Continue", 1) {
      protected void whenClicked() { pushNextPane(); }
      protected boolean enabled() { return canProgress(); }
    });
  }
  
  
  protected void updateState() {
    super.updateState();
    
    final MainScreen screen = MainScreen.current();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = false;
    screen.crewDisplay.hidden   = true ;
    if (expedition.destination() == null) screen.display.spinAtRate(9, 0);
  }
  
  
  

  /**  Handling homeworld selection-
    */
  private void selectHomeworld(VerseLocation homeworld) {
    final MainScreen screen = MainScreen.current();
    screen.worldsDisplay.setSelection(homeworld);
    expedition.setOrigin(homeworld, homeworld.startingOwner);
    homeworld.whenClicked(null);
  }
  
  
  private boolean hasHomeworld(VerseLocation world) {
    return expedition.origin() == world;
  }
  
  

  /**  Handling landing selection-
    */
  private void selectLanding(VerseLocation landing) {
    final MainScreen screen = MainScreen.current();
    screen.display.setSelection(landing.name, true);
    expedition.setDestination(landing);
    landing.whenClicked(null);
  }
  
  
  private boolean hasLanding(VerseLocation landing) {
    return expedition.destination() == landing;
  }
  
  
  
  /**  Other navigation tasks.
    */
  private boolean canProgress() {
    if (expedition.origin     () == null) return false;
    if (expedition.destination() == null) return false;
    return true;
  }
  
  
  private void pushNextPane() {
    //MainScreen.current().clearInfoPane();
    expedition.backing().configStartingExpedition(expedition);
    navigateForward(new SelectTraitsPane(UI, expedition), true);
  }
  
  
  protected void navigateBack() {
    MainScreen.current().clearInfoPane();
    super.navigateBack();
  }
}




