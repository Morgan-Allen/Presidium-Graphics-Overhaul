/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
//import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.user.notify.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;




//  TODO:  The area dedicated to the current info-panel must be made more
//         flexible, to accomodate the stars & planet-charts.

public class BaseUI extends HUD implements UIConstants {
  
  
  
  /**  Core field definitions, constructors, and save/load methods-
    */
  private Stage world;
  private Scenario scenario;
  private Base played;
  private UITask currentTask;
  
  final public Selection selection = new Selection(this);
  final public SelectionTracking tracking;
  
  private UIGroup helpText;
  private MapsDisplay mapsPanel;
  private Readout readout;
  
  //private CommsPane commsPanel;
  private ReminderListing reminders;
  //private PlanetPanel planetPanel;
  //private StarsPanel  starsPanel ;  //Just use the homeworld.

  private Button optionsButton;
  private Button commsButton;  //  TODO:  GET RID OF THIS
  
  private Button buildButton;
  private UINode rosterButton;
  private Button edictsButton;
  
  
  private UIGroup panelArea, infoArea;
  private BorderedLabel popup;
  private Quickbar quickbar;
  
  private UIGroup currentPanel, newPanel;
  private TargetOptions currentInfo, newInfo;
  private boolean capturePanel = false;
  
  
  
  public BaseUI(Stage world, Scenario scenario, Rendering rendering) {
    super(rendering);
    this.world = world;
    this.scenario = scenario;
    this.tracking = new SelectionTracking(this, rendering.view);
    configLayout();
    configPanels();
    configHovers();
  }
  
  
  public void assignBaseSetup(Base played, Vec3D homePos) {
    this.played = played;
    if (homePos != null) rendering.view.lookedAt.setTo(homePos);
    mapsPanel.setBase(played);
  }
  
  
  public void loadState(Session s) throws Exception {
    final Base played = (Base) s.loadObject();
    assignBaseSetup(played, null);
    tracking  .loadState(s);
    selection .loadState(s);
    //commsPanel.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(played);
    tracking  .saveState(s);
    selection .saveState(s);
    //commsPanel.saveState(s);
  }
  
  
  public Base played() { return played; }
  public Stage world() { return world ; }
  
  public ReminderListing reminders() { return reminders; }
  //public CommsPane commsPanel() { return commsPanel; }
  
  
  public static BaseUI current() {
    final HUD UI = PlayLoop.currentUI();
    if (UI instanceof BaseUI) return (BaseUI) UI;
    //else I.complain("NO BASE UI IN PLACE!");
    return null;
  }
  
  
  public static Base currentPlayed() {
    final BaseUI UI = current();
    return UI == null ? null : UI.played();
  }
  
  
  public static boolean isSelected(Object o) {
    final BaseUI UI = current();
    return UI != null & UI.selection.selected() == o;
  }
  
  
  public static boolean isHovered(Object o) {
    final BaseUI UI = current();
    return UI != null & UI.selection.hovered() == o;
  }
  
  
  public static boolean isSelectedOrHovered(Object o) {
    return isSelected(o) || isHovered(o);
  }
  
  
  public static void setPopupMessage(String message) {
    final BaseUI UI = current();
    if (UI != null) UI.popup.setMessage(message, true, 0.5f);
  }
  
  
  
  /**  Construction of the default interface layout-
    */
  private void configLayout() {
    
    this.mapsPanel = new MapsDisplay(this, world, null);
    mapsPanel.alignTop (0, MINIMAP_HIGH);
    mapsPanel.alignLeft(0, MINIMAP_WIDE);
    mapsPanel.attachTo(this);
    
    this.readout = new Readout(this);
    readout.alignHorizontal(MINIMAP_WIDE + PANEL_TAB_SIZE, INFO_PANEL_WIDE);
    readout.alignTop(0, READOUT_HIGH);
    readout.attachTo(this);
    
    this.panelArea = new UIGroup(this);
    panelArea.alignVertical  (0, 0);
    panelArea.alignHorizontal(0, 0);
    panelArea.attachTo(this);
    
    this.infoArea = new UIGroup(this);
    infoArea.alignVertical  (0, 0);
    infoArea.alignHorizontal(0, 0);
    infoArea.attachTo(this);
    
    this.popup = new BorderedLabel(this);
    popup.alignHorizontal(0.5f, 0, 0);
    popup.alignBottom(QUICKBAR_HIGH, 0);
    popup.attachTo(this);
    
    currentPanel = newPanel = null;
    currentInfo  = newInfo  = null;
    
    this.quickbar = new Quickbar(this);
    quickbar.alignAcross(0, 1);
    quickbar.alignBottom(0, 0);
    quickbar.attachTo(this);
  }
  
  
  private void configPanels() {
    
    final int
      PTS = PANEL_TAB_SIZE,
      HTS = DEFAULT_MARGIN + (PTS / 2),
      PTH = PANEL_TABS_HIGH;
    
    this.optionsButton = GameOptionsPane.createButton(this, scenario);
    optionsButton.stretch = false;
    optionsButton.alignTop (0, PTH);
    optionsButton.alignLeft(0, PTS);
    optionsButton.attachTo(this);
    
    this.reminders = new ReminderListing(this);
    reminders.alignLeft(10, 100);
    reminders.alignVertical(QUICKBAR_HIGH, MINIMAP_HIGH + 40);
    reminders.attachTo(this);
    
    this.buildButton = InstallationPane.createButton(this);
    buildButton.stretch = false;
    buildButton.alignTop(0, PTH);
    buildButton.alignRight((PTS * 0) + HTS, PTS);
    buildButton.attachTo(this);
    
    this.rosterButton = RosterPane.createButton(this);
    rosterButton.stretch = false;
    rosterButton.alignTop(0, PTH);
    rosterButton.alignRight((PTS * 1) + HTS, PTS);
    rosterButton.attachTo(this);
    
    this.edictsButton = CommercePane.createButton(this);
    edictsButton.stretch = false;
    edictsButton.alignTop(0, PTH);
    edictsButton.alignRight((PTS * 2) + HTS, PTS);
    edictsButton.attachTo(this);
    
    /*
    this.planetPanel = new PlanetPanel(this);
    this.planetButton = new Button(
      this,
      PlanetPanel.PLANET_ICON.asTexture(),
      PlanetPanel.PLANET_ICON_LIT.asTexture(),
      "planet sectors"
    ) {
      protected void whenClicked() {
        setInfoPanels(planetPanel, null);
      }
    };
    planetButton.stretch = false;
    planetButton.alignTop(0, PTS);
    planetButton.alignLeft(MINIMAP_WIDE + HS - (PTS * 1), PTS);
    planetButton.attachTo(this);
    //*/
  }
  
  
  private void configHovers() {
    this.helpText = new Tooltips(this);
    helpText.attachTo(this);
  }
  
  
  
  /**  Tasks and target-selection helper methods-
    */
  public UITask currentTask() {
    return currentTask;
  }
  
  
  public UIGroup currentPane() {
    return newPanel;
  }
  
  
  public TargetOptions currentInfo() {
    return newInfo;
  }
  
  
  public void beginTask(UITask task) {
    currentTask = task;
    if (I.logEvents()) I.say("\nBEGAN UI TASK: "+task);
  }
  
  
  public void endCurrentTask() {
    currentTask = null;
  }
  
  
  public static boolean isPicked(Object o) {
    final HUD hud = PlayLoop.currentUI();
    if (! (hud instanceof BaseUI)) return false;
    return (o == null) || ((BaseUI) hud).selection.selected() == o;
  }
  
  
  public static boolean isOpen(UIGroup panel) {
    final HUD hud = PlayLoop.currentUI();
    if (! (hud instanceof BaseUI)) return false;
    return ((BaseUI) hud).currentPane() == panel;
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateInput() {
    super.updateInput();
    selection.updateSelection(world, rendering.view, panelArea);
  }
  
  
  public void renderWorldFX() {
    selection.renderWorldFX(rendering);
  }
  
  
  public void renderHUD(Rendering rendering) {
    if (currentTask != null) currentTask.doTask();
    
    super.renderHUD(rendering);
    tracking.updateTracking();
    
    //  In order to transition smoothly between the old and new information
    //  panes, we essentially capture a local screenshot of the old panel, then
    //  fade that out on top of the new panel.
    
    //  TODO:  This doesn't look terribly smooth if you inspect it closely-
    //  either use simple alpha-fadeouts or start rendering-to-texture instead.
    if (capturePanel && currentPanel != null) {
      final Box2D b = new Box2D().setTo(currentPanel.trueBounds());
      rendering.fading.applyFadeWithin(b, "panel_fade");
      capturePanel = false;
    }
    if (currentPanel != newPanel) {
      if (currentPanel != null) currentPanel.detach();
      if (newPanel     != null) newPanel.attachTo(panelArea);
      currentPanel = newPanel;
    }
    
    //  Only attach the new information panel once the old one is done fading
    //  out.  TODO:  Use a similar trick above...
    if (
      (currentInfo != newInfo) &&
      (currentInfo == null || ! currentInfo.attached())
    ) {
      if (newInfo != null) newInfo.attachTo(infoArea);
      currentInfo = newInfo;
    }
    
    if (KeyInput.wasTyped(Keys.ESCAPE) && currentTask != null) {
      currentTask.cancelTask();
    }
  }
  
  
  
  /**  Updating the central information panel and target options-
    */
  public void setInfoPanels(
    UIGroup panel, TargetOptions options
  ) {
    if (panel   != currentPanel) {
      beginPanelFade();
      newPanel = panel;
    }
    if (options != currentInfo ) {
      if (currentInfo != null) currentInfo.active = false;
      newInfo = options;
    }
  }
  
  
  public void setPanelsInstant(
    UIGroup panel, TargetOptions options
  ) {
    currentPanel = null; newPanel = panel  ;
    currentInfo  = null; newInfo  = options;
    capturePanel = false;
  }
  
  
  public void beginPanelFade() {
    capturePanel = true;
  }
}




