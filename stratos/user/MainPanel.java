/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user ;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;


//  TODO:  This isn't actually being used at the moment.  Get rid of it.


public class MainPanel extends InfoPanel implements UIConstants {
  
  
  /**  Field definitions, constructors and initial setup-
    */
  final static int
    TAB_MISSIONS = 0,
    TAB_ADMIN    = 1,
    TAB_POWERS   = 2,
    TAB_COMM_LOG = 3,
    TAB_GUILDS   = 4 ;
  
  final public ImageAsset BORDER_TEX = ImageAsset.fromImage(
    "media/GUI/main_pane_background.png", MainPanel.class
  ) ;
  
  
  final static int
    PANEL_WIDE = GUILDS_PANEL_WIDE - 36,
    
    GUILDS_MARGIN = 0,
    GUILDS_TOP = 0,
    GUILDS_WIDE = PANEL_WIDE,
    GUILD_B_WIDE = GUILDS_WIDE / 3,
    GUILD_B_HIGH = (int) (GUILD_B_WIDE / 2.5f),
    GUILDS_HIGH = (GUILD_B_HIGH * 2),
    GUILDS_BOTTOM = GUILDS_TOP + GUILDS_HIGH,
    
    HEADER_TOP = 5,
    HEADER_HIGH = 20,
    TEXT_TOP = GUILDS_BOTTOM + 10 ;
  
  
  final BaseUI UI ;
  Button guildButtons[] ;
  InfoPanel currentTab = null ;
  
  
  
  public MainPanel(BaseUI UI) {
    super(UI, null, GUILDS_BOTTOM) ;
    this.UI = UI ;
    InstallTab.setupTypes() ;
    setupLayout() ;
  }
  
  
  void setupLayout() {
    createGuildButton(
      "militant_category_button", "Militant Structures",
      TAB_GUILDS + 0, 0, 0
    ) ;
    createGuildButton(
      "merchant_category_button", "Merchant Structures",
      TAB_GUILDS + 1, 1, 0
    ) ;
    createGuildButton(
      "aesthete_category_button", "Aesthete Structures",
      TAB_GUILDS + 2, 2, 0
    ) ;
    createGuildButton(
      "artificer_category_button", "Artificer Structures",
      TAB_GUILDS + 3, 0, 1
    ) ;
    createGuildButton(
      "ecologist_category_button", "Ecologist Structures",
      TAB_GUILDS + 4, 1, 1
    ) ;
    createGuildButton(
      "physician_category_button", "Physician Structures",
      TAB_GUILDS + 5, 2, 1
    ) ;
  }
  
  
  private Button createGuildButton(
    String img, String help, final int buttonID, float a, float d
  ) {
    final Button button = new Button(
      UI, Quickbar.GUILD_IMG_ASSETS.get(img), help
    ) {
      protected void whenClicked() {
        switchToPane(buttonID) ;
      }
    } ;
    button.relBound.set(0, 1, 0, 0) ;
    button.absBound.set(
      GUILDS_MARGIN + (a * GUILD_B_WIDE),
      0 - (((d + 1) * GUILD_B_HIGH) + GUILDS_TOP),
      GUILD_B_WIDE, GUILD_B_HIGH
    ) ;
    button.attachTo(this) ;
    button.stretch = true ;
    return button ;
  }
  
  
  
  /**  Subsequent UI responses and content production-
    */
  private void switchToPane(int buttonID) {
    UI.beginPanelFade() ;
    if (buttonID == TAB_MISSIONS) {
      currentTab = new MissionsTab(UI) ;
    }
    if (buttonID >= TAB_GUILDS) {
      final String catName = INSTALL_CATEGORIES[buttonID - TAB_GUILDS] ;
      //currentTab = new InstallTab(this, catName) ;
    }
  }
  
  
  protected void updateState() {
    if (currentTab != null) {
      currentTab.updateText(UI, headerText, detailText) ;
    }
    super.updateState() ;
  }
}





/*
PANES_TOP = PORTRAIT_DOWN ,
PANE_B_WIDE = PANEL_WIDE / 2,
PANE_B_HIGH = PANE_B_WIDE / 2,
PANE_INSET_PERCENT = 10,
PANES_HIGH = (PANE_B_HIGH * 2),
PANES_BOTTOM = PANES_TOP + PANES_HIGH,
//*/

///protected void whenHovered() { I.say("Hovering over pane "+buttonID) ; }

/*
missionsButton = createPaneButton(
  TABS_PATH+"missions_tab.png",
  "Open the missions tab",
  TAB_MISSIONS, 0, 0
) ;
missionsButton.setHighlight(Button.TRIANGLE_LIT_TEX) ;
adminButton = createPaneButton(
  TABS_PATH+"install_tab.png",  //  CHANGE THIS
  "Open the administration tab",
  TAB_ADMIN, 0.5f, 0
) ;
adminButton.setHighlight(Button.TRI_INV_LIT_TEX) ;
powersButton = createPaneButton(
  TABS_PATH+"powers_tab.png",
  "Open the powers tab",
  TAB_POWERS, 1, 0
) ;
powersButton.setHighlight(Button.TRIANGLE_LIT_TEX) ;
commsButton = createPaneButton(
  TABS_PATH+"comms_tab.png",
  "Open the communications tab",
  TAB_COMM_LOG, 0.5f, 1
) ;
commsButton.setHighlight(Button.TRIANGLE_LIT_TEX) ;
//*/

/*
private Button createPaneButton(
  String img, String help, final int buttonID, float a, float d
) {
  final Button button = new Button(UI, img, help) {
    protected void whenClicked() { switchToPane(buttonID) ; }
  } ;
  button.stretch = true ;
  button.selectMode = Button.MODE_ALPHA ;
  button.relBound.set(0, 1, 0, 0) ;

  final float
    m = PANE_INSET_PERCENT / 100f,
    xM = PANE_B_WIDE * m,
    yM = PANE_B_HIGH * m ;
  button.absBound.set(
    xM + (a * PANE_B_WIDE),
    yM - (((d + 1) * PANE_B_HIGH) + PANES_TOP),
    PANE_B_WIDE - (xM * 2), PANE_B_HIGH - (yM * 2)
  ) ;
  button.attachTo(this) ;
  return button ;
}
//*/





