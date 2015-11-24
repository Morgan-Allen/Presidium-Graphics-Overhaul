/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.user.ChartUtils.*;



public class MainScreen extends HUD {
  
  
  final static int
    MENU_PANEL_WIDE = UIConstants.INFO_PANEL_WIDE,
    MARGIN          = UIConstants.DEFAULT_MARGIN * 2,
    CAROUSEL_HIGH   = 80;
  final static int
    MENU_INIT          = 0,
    MENU_NEW_GAME_SITE = 1,
    MENU_NEW_GAME_CREW = 2,
    MENU_SAVES_LIST    = 3,
    MENU_CREDITS       = 4,
    MENU_QUIT          = 5;
  
  
  MenuPane menuView;
  int menuState = MENU_INIT;
  
  UIGroup displayArea;
  PlanetDisplay display;
  Carousel worldsDisplay;
  
  
  
  public MainScreen(Rendering rendering) {
    super(rendering);
    
    menuView = new MainMenu2(this);
    menuView.alignVertical(MARGIN * 2, MARGIN * 2);
    menuView.alignLeft(MARGIN, MENU_PANEL_WIDE);
    menuView.attachTo(this);
    
    final int
      dispInX = MENU_PANEL_WIDE + (MARGIN * 2),
      dispInY = CAROUSEL_HIGH   + (MARGIN * 2);
    display = createPlanetDisplay(LOAD_PATH, PLANET_LOAD_FILE);
    display.showLabels = false;
    
    displayArea = new UIGroup(this) {
      public void render(WidgetsPass pass) {
        ChartUtils.renderPlanet(display, this, pass);
        super.render(pass);
      }
    };
    displayArea.alignVertical  (dispInY, dispInY);
    displayArea.alignHorizontal(dispInX, dispInX);
    displayArea.attachTo(this);
    
    worldsDisplay = createWorldsCarousel(this);
    worldsDisplay.alignTop(MARGIN, CAROUSEL_HIGH);
    worldsDisplay.alignHorizontal(dispInX, dispInX);
    worldsDisplay.attachTo(this);
  }
  
  
  public static MainScreen current() {
    final HUD current = PlayLoop.currentUI();
    if (current instanceof MainScreen) return (MainScreen) current;
    return null;
  }
  
  
  protected void updateState() {
    
    for (UINode kid : kids()) if (kid instanceof MenuPane) {
      menuState = ((MenuPane) kid).stateID;
    }
    
    if (menuState == MENU_INIT) {
      float rotInc = 90f / (10 * Rendering.FRAMES_PER_SECOND);
      display.setRotation(display.rotation() + rotInc);
      display.showWeather = true;
      worldsDisplay.hidden = true;
    }
    
    super.updateState();
  }
  
}














