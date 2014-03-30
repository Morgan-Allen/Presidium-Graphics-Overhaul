/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user ;
import com.badlogic.gdx.*;

import java.nio.*;

import stratos.game.actors.ActorHealth;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.Minimap;
import stratos.graphics.widgets.*;
import stratos.start.PlayLoop;
import stratos.util.*;




public class BaseUI extends HUD implements UIConstants {
  
  
  
  /**  Core field definitions, constructors, and save/load methods-
    */
  private World world ;
  private Base played ;
  
  private UITask currentTask ;
  final public Selection selection = new Selection(this) ;
  
  final Rendering rendering ;
  final public ViewTracking viewTracking ;
  
  UIGroup helpText ;
  MapsPanel mapsPanel;
  Text readout ;
  UIGroup panelArea, infoArea ;
  Quickbar quickbar ;
  
  private UIGroup currentPanel, newPanel;
  private TargetInfo currentInfo, newInfo;
  private boolean capturePanel = false;
  
  
  
  public BaseUI(World world, Rendering rendering) {
    this.world = world ;
    this.rendering = rendering ;
    this.viewTracking = new ViewTracking(this, rendering.view) ;
    configLayout() ;
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
    mapsPanel.relBound.setTo(MINI_BOUNDS) ;
    mapsPanel.absBound.setTo(MINI_INSETS) ;
    mapsPanel.attachTo(this) ;
    
    this.readout = new Text(this, INFO_FONT) ;
    readout.relBound.set(0, 1, 1, 0) ;
    readout.absBound.set(200, -50, -300, 40) ;
    readout.attachTo(this) ;
    
    this.panelArea = new UIGroup(this) ;
    panelArea.relBound.setTo(INFO_BOUNDS) ;
    panelArea.absBound.setTo(INFO_INSETS) ;
    panelArea.attachTo(this) ;
    this.infoArea = new UIGroup(this);
    infoArea.relBound.set(0, 0, 1, 1);
    infoArea.attachTo(this);
    currentPanel = newPanel = null ;
    currentInfo = newInfo = null;
    
    this.quickbar = new Quickbar(this) ;
    //quickbar.absBound.set(20, 20, -40, 0) ;
    quickbar.relBound.set(0, 0, 1, 0) ;
    quickbar.attachTo(this) ;
    //quickbar.setupMissionButtons() ;  //  Not needed any more, I think...
    quickbar.setupPowersButtons() ;
    quickbar.setupInstallButtons() ;
    
    this.helpText = new Tooltips(this);
    helpText.attachTo(this) ;
  }
  
  
  
  /**  Modifying the interface layout-
    */
  protected void setInfoPanels(UIGroup infoPanel, TargetInfo targetInfo) {
    if (infoPanel != currentPanel) {
      beginPanelFade();
      newPanel = infoPanel;
    }
    if (targetInfo != currentInfo) {
      if (currentInfo != null) currentInfo.active = false;
      newInfo = targetInfo;
    }
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateInput() {
    super.updateInput();
    selection.updateSelection(world, rendering.view, panelArea);
  }
  
  
  protected void updateState() {
    super.updateState() ;
    updateReadout() ;
  }
  
  
  protected void updateReadout() {
    //
    //  TODO:  Move these functions to a separate class.
    if (readout == null) return ;
    readout.setText("") ;
    //
    //  Credits first-
    final int credits = played.credits() ;
    if (credits >= 0) readout.append(credits+" Credits", Colour.WHITE) ;
    else readout.append((0 - credits)+" In Debt", Colour.YELLOW) ;
    readout.append("   ") ;
    //
    //  Then time and date-
    final float
      time = world.currentTime() / World.STANDARD_DAY_LENGTH ;
    final int
      days  = (int) time,
      hours = (int) ((time - days) * 24) ;
    String hS = hours+"00" ;
    while (hS.length() < 4) hS = "0"+hS ;
    String dS = "Day "+days+" "+hS+" Hours" ;
    readout.append(dS) ;
    //
    //  And finally current psy points-
    final boolean ruled = played.ruler() != null ;
    final ActorHealth RH = ruled ? played.ruler().health : null ;
    final int PS = ruled ? 2 * (int) RH.maxPsy() : 0 ;
    float psyPoints = 0 ;
    if (played.ruler() != null) {
      psyPoints += played.ruler().health.psyPoints() ;
      psyPoints *= PS / RH.maxPsy() ;
    }
    if (PS > 0 && psyPoints > 0) {
      readout.append("   Psy Points: ") ;
      float a = psyPoints / PS ;
      Colour tone = new Colour().set((1 - a) / 2, a, (1 - a), 1) ;
      while (--psyPoints > 0) {
        readout.append("|", tone) ;
        a = psyPoints / PS ;
        tone = new Colour().set((1 - a) / 2, a, (1 - a), 1) ;
        tone.setValue(1) ;
      }
      if ((psyPoints + 1) > 0) {
        tone.a = psyPoints + 1 ;
        readout.append("|", tone) ;
      }
    }
  }
  
  
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
  
  
  
  /**  Handling task execution (Outsource this to the HUD class?)-
    */
  public UITask currentTask() {
    return currentTask ;
  }
  
  
  public UIGroup currentPanel() {
    return this.currentPanel ;
  }
  
  
  public void beginTask(UITask task) {
    currentTask = task ;
  }
  
  
  public void endCurrentTask() {
    currentTask = null ;
  }
  
  
  public static boolean isPicked(Object o) {
    final HUD hud = PlayLoop.currentUI() ;
    if (! (hud instanceof BaseUI)) return false ;
    return (o == null) || ((BaseUI) hud).selection.selected() == o ;
  }
}

