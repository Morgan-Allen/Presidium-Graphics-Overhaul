/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.graphics.common.*;
import stratos.game.verse.*;
import stratos.user.*;
import stratos.util.*;
import stratos.util.Description.*;
import static stratos.user.ChartUtils.*;



//  TODO:  Just use a Text for now.  Don't bother with interior listings, or
//         headers and footers.


public class SelectSitePane extends MenuPane {
  
  final static int
    STAGE_PICK_SECTORS   = 0,
    STAGE_CONFIG_LEADER  = 1,
    STAGE_PICK_HOUSEHOLD = 2,
    STAGE_PICK_COLONISTS = 3;
  
  private int stage = STAGE_PICK_SECTORS;
  
  
  private UINode forwardButton;
  
  private VerseLocation homeworld;
  private VerseLocation landing;
  
  private Actor leader;
  private List <Actor> advisors  = new List();
  private List <Actor> colonists = new List();
  
  
  
  public SelectSitePane(HUD UI) {
    super(UI, MainScreen.MENU_NEW_GAME_SITE);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    final SelectSitePane pane = this;
    //
    //  Pick a homeworld first.
    listing.add(createTextItem("Homeworld:", 1.2f, null));
    
    final VerseLocation homeworlds[] = Verse.ALL_CAPITALS;
    for (final VerseLocation homeworld : homeworlds) {
      listing.add(new TextButton(UI, "  "+homeworld.name, 1) {
        protected void whenClicked() { selectHomeworld(homeworld); }
        protected boolean toggled() { return pane.homeworld == homeworld; }
      });
    }
    listing.add(createTextItem(
      "Your homeworld will determine the initial colonists and finance "+
      "available to your settlement, along with technical expertise and "+
      "trade revenue.", 0.75f, Colour.LITE_GREY
    ));
    //
    //  Then pick a sector.
    listing.add(createTextItem("Landing Site:", 1.2f, null));

    final VerseLocation landings[] = Verse.ALL_DIAPSOR_SECTORS;
    for (final VerseLocation landing : landings) {
      listing.add(new TextButton(UI, "  "+landing.name, 1) {
        public void whenClicked() { selectLanding(landing); }
        protected boolean toggled() { return pane.landing == landing; }
      });
    }
    listing.add(createTextItem(
      "Your landing site will determine the type of resources initially "+
      "available to your settlement, along with local species and other "+
      "threats.", 0.75f, Colour.LITE_GREY
    ));
    
    //
    //  And include an option to proceed further...
    listing.add(forwardButton = new TextButton(UI, "Continue", 1) {
      public void whenClicked() {
        
      }
    });
  }
  
  
  protected void updateState() {
    final MainScreen screen = MainScreen.current();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = false;
    screen.helpField.hidden     = false;
    if (landing == null) screen.display.spinAtRate(9, 0);
    
    forwardButton.hidden = landing == null || homeworld == null;
    super.updateState();
  }
  
  
  private void selectHomeworld(VerseLocation homeworld) {
    final MainScreen screen = MainScreen.current();
    screen.worldsDisplay.setSelection(homeworld);
    screen.helpField.setText("");
    screen.helpField.append(homeworld.info, Colour.LITE_GREY);
    this.homeworld = homeworld;
  }
  
  
  private void selectLanding(VerseLocation landing) {
    final MainScreen screen = MainScreen.current();
    screen.display.setSelection(landing.name, true);
    screen.helpField.setText("");
    screen.helpField.append(landing.info, Colour.LITE_GREY);
    this.landing = landing;
  }
}



  /*
  public static class Config {
    //  TODO:  Just pick House, Province, Options.  And a few perks.
    
    public Background house ;
    public Background gender;
    public List <Trait    > chosenTraits = new List <Trait    > ();
    public List <Skill    > chosenSkills = new List <Skill    > ();
    public List <Technique> chosenTechs  = new List <Technique> ();
    
    public List  <Background> advisors = new List  <Background> ();
    public Tally <Background> crew     = new Tally <Background> ();
    public Tally <Blueprint > built    = new Tally <Blueprint > ();
    public VerseLocation demesne;
    public int siteLevel, fundsLevel, titleLevel;
  }
  
  final Config config;
  //*/
  
  
  
  