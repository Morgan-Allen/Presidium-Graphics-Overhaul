/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.game.verse.*;
import stratos.start.*;
import stratos.user.*;
import stratos.content.hooks.*;
import static stratos.user.ChartUtils.*;



public class MainScreen extends HUD {
  
  
  /**  Data fields, construction and setup methods-
    */
  final static int
    MENU_PANEL_WIDE = UIConstants.INFO_PANEL_WIDE,
    MARGIN          = UIConstants.DEFAULT_MARGIN * 2,
    CAROUSEL_HIGH   = 80,
    HELP_FIELD_HIGH = 80;
  final static int
    MENU_INIT          = 0,
    MENU_SAVES_LIST    = 1,
    MENU_CREDITS       = 2,
    MENU_QUIT          = 3,
    MENU_NEW_GAME_SITE = 4,
    MENU_NEW_GAME_CREW = 5;
  
  
  MenuPane menuView;
  int menuState = MENU_INIT;
  Verse verse = null;
  
  UIGroup displayArea;
  PlanetDisplay display;
  Carousel worldsDisplay;
  CrewDisplay crewDisplay;
  
  UIGroup infoArea;
  private UIGroup currentInfo;
  
  
  
  public MainScreen(Rendering rendering) {
    super(rendering);
    
    //  TODO:  Constrain this better!
    this.infoArea = new UIGroup(this);
    infoArea.alignVertical  (0, 0);
    infoArea.alignHorizontal(0, 0);
    infoArea.attachTo(this);
    
    menuView = new MainMenu(this);
    menuView.alignVertical(MARGIN * 2, MARGIN * 2);
    menuView.alignLeft(MARGIN, MENU_PANEL_WIDE);
    menuView.attachTo(this);
    
    final int
      dispInX = MENU_PANEL_WIDE + (MARGIN * 2),
      dispTop = CAROUSEL_HIGH   + (MARGIN * 2),
      dispBot = HELP_FIELD_HIGH + (MARGIN * 2);
    display = createPlanetDisplay(LOAD_PATH, PLANET_LOAD_FILE);
    display.showLabels = false;
    
    displayArea = new UIGroup(this) {
      public void render(WidgetsPass pass) {
        ChartUtils.renderPlanet(display, this, pass);
        super.render(pass);
      }
    };
    displayArea.alignVertical  (dispBot, dispTop);
    displayArea.alignHorizontal(dispInX, dispInX);
    displayArea.attachTo(this);
    
    worldsDisplay = createWorldsCarousel(this, StratosSetting.ALL_CAPITALS);
    worldsDisplay.alignTop(MARGIN, CAROUSEL_HIGH);
    worldsDisplay.alignHorizontal(dispInX, dispInX);
    worldsDisplay.attachTo(this);
    
    crewDisplay = new CrewDisplay(this);
    crewDisplay.alignToMatch(worldsDisplay);
    crewDisplay.attachTo(this);
  }
  
  
  
  /**  Regular queries and update methods-
    */
  public static MainScreen current() {
    final HUD current = PlayLoop.currentUI();
    if (current instanceof MainScreen) return (MainScreen) current;
    return null;
  }
  
  
  public static Verse currentVerse() {
    final MainScreen screen = current();
    if (screen == null) return null;
    return screen.verse;
  }
  
  
  protected void updateState() {
    PlayLoop.rendering().backColour = Colour.BLACK;
    
    for (UINode kid : kids()) if (kid instanceof MenuPane) {
      menuState = ((MenuPane) kid).stateID;
    }
    
    if (menuState < MENU_NEW_GAME_SITE) {
      display.spinAtRate(9, 0);
      display.setSelection(null, false);
      display.showWeather  = true ;
      display.showLabels   = false;
      worldsDisplay.hidden = true ;
      crewDisplay.hidden   = true ;
      verse                = null ;
    }
    else if (verse == null) {
      verse = new StratosSetting();
    }
    
    super.updateState();
  }
  
  
  
  /**  Handling any necessary help-information:
    */
  public void setInfoPane(UIGroup info) {
    if (currentInfo != null) currentInfo.detach();
    currentInfo = info;
    if (currentInfo != null) currentInfo.attachTo(infoArea);
  }
  
  
  public void clearInfoPane() {
    setInfoPane(null);
  }
  
  
  public UIGroup currentInfoPane() {
    return currentInfo;
  }
}










