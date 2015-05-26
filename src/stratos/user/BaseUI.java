/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.user.notify.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;



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
  
  private ReminderListing reminders;
  private Button optionsButton;
  
  private Button installButton;
  private UINode rosterButton ;
  private Button budgetButton ;
  private Button sectorsButton;
  
  
  private UIGroup infoArea, optionsArea, messageArea;
  private BorderedLabel popup;
  private Quickbar quickbar;
  
  private UIGroup currentInfo, newInfo;  //  TODO:  Insist on selection-panes?
  private SelectionOptions currentOptions, newOptions;
  private MessagePane currentMessage, newMessage;
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
    tracking .loadState(s);
    selection.loadState(s);
    reminders.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(played);
    tracking .saveState(s);
    selection.saveState(s);
    reminders.saveState(s);
  }
  
  
  public Base played() { return played; }
  public Stage world() { return world ; }
  
  public ReminderListing reminders() { return reminders; }
  
  
  public static BaseUI current() {
    final HUD UI = PlayLoop.currentUI();
    if (UI instanceof BaseUI) return (BaseUI) UI;
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
    
    this.messageArea = new UIGroup(this);
    messageArea.alignHorizontal(0.5f, MESSAGE_PANE_WIDE, 0);
    messageArea.alignTop(READOUT_HIGH, MESSAGE_PANE_HIGH);
    messageArea.attachTo(this);

    //  TODO:  Constrain this better.
    this.optionsArea = new UIGroup(this);
    optionsArea.alignVertical  (0, 0);
    optionsArea.alignHorizontal(0, 0);
    optionsArea.attachTo(this);
    
    //  TODO:  Constrain this better.
    this.infoArea = new UIGroup(this);
    infoArea.alignVertical  (0, 0);
    infoArea.alignHorizontal(0, 0);
    infoArea.attachTo(this);
    
    this.popup = new BorderedLabel(this);
    popup.alignHorizontal(0.5f, 0, 0);
    popup.alignBottom(QUICKBAR_HIGH, 0);
    popup.attachTo(this);
    
    currentInfo = newInfo = null;
    currentOptions  = newOptions  = null;
    
    this.quickbar = new Quickbar(this);
    quickbar.alignAcross(0, 1);
    quickbar.alignBottom(0, 0);
    quickbar.attachTo(this);
  }
  
  
  private void configPanels() {
    
    final int
      PTS = PANEL_TAB_SIZE,
      HTS = DEFAULT_MARGIN,// + (PTS / 2),
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
    
    this.installButton = InstallPane.createButton(this);
    installButton.stretch = false;
    installButton.alignTop(0, PTH);
    installButton.alignRight((PTS * 0) + HTS, PTS);
    installButton.attachTo(this);
    
    this.rosterButton = RosterPane.createButton(this);
    rosterButton.stretch = false;
    rosterButton.alignTop(0, PTH);
    rosterButton.alignRight((PTS * 1) + HTS, PTS);
    rosterButton.attachTo(this);
    
    this.budgetButton = BudgetsPane.createButton(this);
    budgetButton.stretch = false;
    budgetButton.alignTop(0, PTH);
    budgetButton.alignRight((PTS * 2) + HTS, PTS);
    budgetButton.attachTo(this);
    
    this.sectorsButton = SectorsPane.createButton(this);
    sectorsButton.stretch = false;
    sectorsButton.alignTop(0, PTH);
    sectorsButton.alignRight((PTS * 3) + HTS, PTS);
    sectorsButton.attachTo(this);
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
    return ((BaseUI) hud).currentInfoPane() == panel;
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateInput() {
    super.updateInput();
    selection.updateSelection(world, rendering.view, infoArea);
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
    if (capturePanel && currentInfo != null) {
      final Box2D b = new Box2D().setTo(currentInfo.trueBounds());
      rendering.fading.applyFadeWithin(b, "panel_fade");
      capturePanel = false;
    }
    if (currentInfo != newInfo) {
      if (currentInfo != null) currentInfo.detach();
      if (newInfo     != null) newInfo.attachTo(infoArea);
      currentInfo = newInfo;
    }
    
    //  Only attach the new information panel once the old one is done fading
    //  out.  TODO:  Use a similar trick above...
    if (
      (currentOptions != newOptions) &&
      (currentOptions == null || ! currentOptions.attached())
    ) {
      if (newOptions != null) newOptions.attachTo(optionsArea);
      currentOptions = newOptions;
    }
    
    //  TODO:  ALLOW FOR FADE-IN/FADE-OUT HERE AS WELL
    if (currentMessage != newMessage) {
      if (currentMessage != null) currentMessage.detach();
      if (newMessage     != null) newMessage.attachTo(messageArea);
      currentMessage = newMessage;
    }
    
    if (KeyInput.wasTyped(Keys.ESCAPE) && currentTask != null) {
      currentTask.cancelTask();
    }
  }
  
  
  
  /**  Updating the central information panel and target options-
    */
  public void setInfoPane(UIGroup info) {
    if (info != currentInfo) {
      beginPanelFade();
      newInfo = info;
    }
  }
  
  
  public void setOptionsList(SelectionOptions options) {
    if (options != currentOptions) {
      if (currentOptions != null) currentOptions.active = false;
      newOptions = options;
    }
  }
  
  
  public void setMessagePane(MessagePane message) {
    if (message != currentMessage) {
      newMessage = message;
    }
  }
  
  
  public void clearInfoPane() {
    setInfoPane(null);
  }
  
  
  public void clearOptionsList() {
    setOptionsList(null);
  }
  
  
  public void clearMessagePane() {
    setMessagePane(null);
  }
  
  
  public UIGroup currentInfoPane() {
    return newInfo;
  }
  
  
  public SelectionOptions currentOptions() {
    return newOptions;
  }
  
  
  public MessagePane currentMessage() {
    return newMessage;
  }
  
  
  //  TODO:  get rid of this once render-to-texture is working...
  public void beginPanelFade() {
    capturePanel = true;
  }
  
  
  public static boolean paneOpenFor(Object o) {
    final BaseUI UI = current();
    if (UI == null || ! (UI.currentInfoPane() instanceof SelectionPane)) {
      return false;
    }
    return ((SelectionPane) UI.currentInfoPane()).selected == o;
  }
  
  
  public static boolean hasMessageFocus(Target subject) {
    final BaseUI UI = BaseUI.current();
    if (UI == null || UI.currentMessage() == null) return false;
    return UI.currentMessage().focus == subject;
  }
}








