/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.util.*;
import com.badlogic.gdx.*;




public class BaseUI extends HUD implements UIConstants {
  
  
  
  /**  Core field definitions, constructors, and save/load methods-
    */
  private World world ;
  private Base played ;
  
  private UITask currentTask ;
  final public Selection selection = new Selection(this) ;
  
  //final Rendering rendering ;
  final public ViewTracking viewTracking ;
  
  UIGroup helpText ;
  MapsPanel mapsPanel;
  Readout readout;
  
  //  TODO:  Also a starcharts and policies panel.
  CommsPanel commsPanel;
  Button commsButton;
  
  UIGroup panelArea, infoArea ;
  Quickbar quickbar ;
  
  private InfoPanel currentPanel, newPanel;
  private TargetInfo currentInfo, newInfo;
  private boolean capturePanel = false;
  
  
  
  public BaseUI(World world, Rendering rendering) {
    super(rendering);
    this.world = world ;
    //this.rendering = rendering ;
    this.viewTracking = new ViewTracking(this, rendering.view) ;
    configLayout() ;
    configPanels() ;
    configHovers() ;
  }
  
  
  public void assignBaseSetup(Base played, Vec3D homePos) {
    this.played = played;
    if (homePos != null) rendering.view.lookedAt.setTo(homePos);
    mapsPanel.setBase(played);
  }
  
  
  public void loadState(Session s) throws Exception {
    final Base played = (Base) s.loadObject() ;
    assignBaseSetup(played, null) ;
    viewTracking.loadState(s) ;
    selection.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(played) ;
    viewTracking.saveState(s) ;
    selection.saveState(s) ;
  }
  
  
  public Base played() { return played ; }
  public World world() { return world  ; }
  
  
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
    
    this.mapsPanel = new MapsPanel(this, world, null) ;
    mapsPanel.relBound.set(0, 1, 0, 0);
    mapsPanel.absBound.set(0, -256, 256, 256);
    mapsPanel.attachTo(this) ;
    
    this.readout = new Readout(this) ;
    readout.relBound.set(0, 1, 1, 0) ;
    readout.absBound.set(200, -50, -300, READOUT_HIGH) ;
    readout.attachTo(this) ;
    
    this.panelArea = new UIGroup(this) ;
    panelArea.relBound.set(0.25f, 1, 0.5f, 0);
    panelArea.absBound.set(
      0, -(INFO_PANEL_HIGH + READOUT_HIGH),
      0, INFO_PANEL_HIGH
    );
    panelArea.attachTo(this) ;
    
    this.infoArea = new UIGroup(this);
    infoArea.relBound.set(0, 0, 1, 1);
    infoArea.attachTo(this);
    currentPanel = newPanel = null ;
    currentInfo = newInfo = null;
    
    this.quickbar = new Quickbar(this) ;
    quickbar.relBound.set(0, 0, 1, 0) ;
    quickbar.attachTo(this) ;
    quickbar.setupPowersButtons() ;
    quickbar.setupInstallButtons() ;
  }
  
  
  private void configPanels() {
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
    commsButton.relBound.set(0, 1, 0, 0);
    commsButton.absBound.set(256 - 60, 0 - 100, 80, 80);
    commsButton.attachTo(this);
  }
  
  
  private void configHovers() {
    this.helpText = new Tooltips(this);
    helpText.attachTo(this) ;
  }
  
  
  
  /**  Modifying the interface layout-
    */
  public void setInfoPanels(InfoPanel infoPanel, TargetInfo targetInfo) {
    if (infoPanel != currentPanel) {
      beginPanelFade();
      newPanel = infoPanel;
    }
    if (targetInfo != currentInfo) {
      if (currentInfo != null) currentInfo.active = false;
      newInfo = targetInfo;
    }
  }
  
  
  protected UITask currentTask() {
    return currentTask ;
  }
  
  
  protected InfoPanel currentPanel() {
    return currentPanel ;
  }
  
  
  protected TargetInfo currentInfo() {
    return currentInfo;
  }
  
  
  protected void beginTask(UITask task) {
    currentTask = task ;
  }
  
  
  protected void endCurrentTask() {
    currentTask = null ;
  }
  
  
  public static boolean isPicked(Object o) {
    final HUD hud = PlayLoop.currentUI() ;
    if (! (hud instanceof BaseUI)) return false ;
    return (o == null) || ((BaseUI) hud).selection.selected() == o ;
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateInput() {
    super.updateInput();
    selection.updateSelection(world, rendering.view, panelArea);
  }
  /*
  
  protected void updateState() {
    super.updateState() ;
    updateReadout() ;
  }
  //*/
  
  
  
  
  public void renderWorldFX() {
    selection.renderWorldFX(rendering) ;
    if (currentTask != null) {
      if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) currentTask.cancelTask() ;
      else currentTask.doTask() ;
    }
  }
  
  
  public void renderHUD(Rendering rendering) {
    super.renderHUD(rendering);
    viewTracking.updateTracking() ;
    
    if (currentPanel != newPanel) {
      if (currentPanel != null) currentPanel.detach();
      if (newPanel     != null) newPanel.attachTo(panelArea);
      currentPanel = newPanel;
    }
    if (currentInfo != newInfo) {
      if (newInfo      != null) newInfo.attachTo(infoArea);
      currentInfo  = newInfo;
    }
    
    if (capturePanel) {
      final Box2D b = new Box2D().setTo(panelArea.trueBounds());
      b.expandBy(0 - InfoPanel.MARGIN_WIDTH);
      rendering.fading.applyFadeWithin(b, "panel_fade");
      capturePanel = false ;
    }
  }
  
  
  protected void beginPanelFade() {
    capturePanel = true ;
  }
}

