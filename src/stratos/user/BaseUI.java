/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.util.*;
import com.badlogic.gdx.*;




//  TODO:  The area dedicated to the current info-panel must be made more
//         flexible, to accomodate the stars & planet-charts.

public class BaseUI extends HUD implements UIConstants {
  
  
  
  /**  Core field definitions, constructors, and save/load methods-
    */
  private World world;
  private Base played;
  private UITask currentTask;
  
  final public Selection selection = new Selection(this);
  final public SelectionTracking tracking;
  
  private UIGroup helpText;
  private MapsPanel mapsPanel;
  private Readout readout;
  
  //  TODO:  Also a policies panel?  ...Yeah.  Why not.
  private CommsPanel  commsPanel;
  private PlanetPanel planetPanel;
  private StarsPanel  starsPanel;
  private Button
    commsButton ,
    planetButton,
    starsButton;
  
  
  private UIGroup panelArea, infoArea;
  private Quickbar quickbar;
  
  private UIGroup currentPanel, newPanel;
  private TargetOptions currentInfo, newInfo;
  private boolean capturePanel = false;
  
  
  
  public BaseUI(World world, Rendering rendering) {
    super(rendering);
    this.world = world;
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
    tracking.loadState(s);
    selection.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(played);
    tracking.saveState(s);
    selection.saveState(s);
  }
  
  
  public Base played() { return played; }
  public World world() { return world ; }
  
  
  public CommsPanel commsPanel() { return commsPanel; }
  
  
  public static BaseUI current() {
    final HUD UI = PlayLoop.currentUI();
    if (UI instanceof BaseUI) return (BaseUI) UI;
    else I.complain("NO BASE UI IN PLACE!");
    return null;
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
  
  
  
  /**  Construction of the default interface layout-
    */
  private void configLayout() {
    
    this.mapsPanel = new MapsPanel(this, world, null);
    mapsPanel.alignTop (0, MINIMAP_WIDE);
    mapsPanel.alignLeft(0, MINIMAP_WIDE);
    mapsPanel.attachTo(this);
    
    this.readout = new Readout(this);
    readout.alignHorizontal(MINIMAP_WIDE, GUILDS_WIDE);
    readout.alignTop(0, READOUT_HIGH);
    readout.attachTo(this);
    
    this.panelArea = new UIGroup(this);
    panelArea.alignVertical(QUICKBAR_HIGH, READOUT_HIGH);
    panelArea.alignHorizontal(0, 0);
    panelArea.attachTo(this);
    
    this.infoArea = new UIGroup(this);
    infoArea.alignVertical  (0, 0);
    infoArea.alignHorizontal(0, 0);
    infoArea.attachTo(this);
    
    currentPanel = newPanel = null;
    currentInfo = newInfo = null;
    
    this.quickbar = new Quickbar(this);
    quickbar.alignAcross(0, 1);
    quickbar.alignBottom(0, 0);
    quickbar.attachTo(this);
    quickbar.setupPowersButtons();
    quickbar.setupInstallButtons();
  }
  
  
  private void configPanels() {
    
    final int PTS = PANEL_TAB_SIZE;
    
    this.commsPanel = new CommsPanel(this);
    this.commsButton = new Button(
      this,
      CommsPanel.COMMS_ICON.asTexture(),
      CommsPanel.COMMS_ICON_LIT.asTexture(),
      "messages"
    ) {
      protected void whenClicked() {
        setInfoPanels(commsPanel, null);
      }
    };
    commsButton.alignTop(0, PTS);
    commsButton.alignHorizontal(0.5f, PTS, PTS * 0);
    commsButton.attachTo(this);
    
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
    planetButton.alignTop(0, PTS);
    planetButton.alignHorizontal(0.5f, PTS, PTS * 1);
    planetButton.attachTo(this);
    
    this.starsPanel = new StarsPanel(this);
    this.starsButton = new Button(
      this,
      StarsPanel.STARS_ICON.asTexture(),
      StarsPanel.STARS_ICON_LIT.asTexture(),
      "starcharts"
    ) {
      protected void whenClicked() {
        setInfoPanels(starsPanel, null);
      }
    };
    starsButton.alignTop(0, PTS);
    starsButton.alignHorizontal(0.5f, PTS, PTS * 2);
    starsButton.attachTo(this);
  }
  
  
  private void configHovers() {
    this.helpText = new Tooltips(this);
    helpText.attachTo(this);
  }
  
  
  
  /**  Tasks and target-selection helper methods-
    */
  protected UITask currentTask() {
    return currentTask;
  }
  
  
  protected UIGroup currentPane() {
    return currentPanel;
  }
  
  
  protected TargetOptions currentInfo() {
    return currentInfo;
  }
  
  
  protected void beginTask(UITask task) {
    currentTask = task;
  }
  
  
  protected void endCurrentTask() {
    currentTask = null;
  }
  
  
  public static boolean isPicked(Object o) {
    final HUD hud = PlayLoop.currentUI();
    if (! (hud instanceof BaseUI)) return false;
    return (o == null) || ((BaseUI) hud).selection.selected() == o;
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateInput() {
    super.updateInput();
    selection.updateSelection(world, rendering.view, panelArea);
  }
  
  public void renderWorldFX() {
    selection.renderWorldFX(rendering);
    if (currentTask != null) {
      if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) currentTask.cancelTask();
      else currentTask.doTask();
    }
  }
  
  
  public void renderHUD(Rendering rendering) {
    super.renderHUD(rendering);
    tracking.updateTracking();
    
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
    if (currentInfo != newInfo) {
      if (newInfo      != null) newInfo.attachTo(infoArea);
      currentInfo  = newInfo;
    }
  }
  
  
  
  /**  Updating the central information panel and target options-
    */
  public void setInfoPanels(UIGroup panel, TargetOptions options) {
    if (panel   != currentPanel) {
      beginPanelFade();
      newPanel = panel;
    }
    if (options != currentInfo ) {
      if (currentInfo != null) currentInfo.active = false;
      newInfo = options;
    }
  }
  
  protected void beginPanelFade() {
    capturePanel = true;
  }
}




