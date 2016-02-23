/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.start.SaveUtils.*;
import java.io.*;



public abstract class Scenario implements Session.Saveable, Playable {
  
  
  final static int
    DO_PLAY      = -1,
    DO_SAVE      =  0,
    DO_SAVE_EXIT =  1,
    DO_LOAD      =  2,
    DO_RESTART   =  3;
  
  
  private int nextOp = DO_PLAY;
  
  private Stage world;
  private Base base;
  final boolean isDebug;
  private float loadProgress = -1;
  
  private BaseUI UI = null;
  private String savesPrefix;
  private boolean skipNextLoad = false;
  private float lastSaveTime = -1;
  
  
  
  protected Scenario(String saveFile, boolean isDebug) {
    this.savesPrefix = saveFile;
    this.isDebug     = isDebug ;
  }
  
  
  protected Scenario(boolean isDebug) {
    this("<no_prefix>", isDebug);
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this);
    world        = (Stage) s.loadObject();
    base         = (Base) s.loadObject();
    savesPrefix  = s.loadString();
    skipNextLoad = s.loadBool();
    lastSaveTime = s.loadFloat();
    isDebug      = s.loadBool();
    
    loadProgress = 1;
    if (s.loadBool()) {
      UI = createUI(base, PlayLoop.rendering());
      UI.loadState(s);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(world       );
    s.saveObject(base        );
    s.saveString(savesPrefix );
    s.saveBool  (skipNextLoad);
    s.saveFloat (lastSaveTime);
    s.saveBool  (isDebug     );
    
    if (UI == null) s.saveBool(false);
    else { s.saveBool(true); UI.saveState(s); }
  }
  
  
  public Stage  world() { return world; }
  public Base   base () { return base ; }
  public BaseUI UI   () { return UI   ; }
  
  
  public void skipLoading() {
    this.skipNextLoad = true;
  }
  
  
  public String savesPrefix() {
    return savesPrefix;
  }
  
  
  protected void setSavesPrefix(String prefix) {
    this.savesPrefix = prefix;
  }
  
  
  public static Scenario current() {
    final Playable p = PlayLoop.played();
    if (p instanceof Scenario) return (Scenario) p;
    return null;
  }
  
  
  public static boolean isCurrentScenarioDebug() {
    Scenario current = current();
    return current != null && current.isDebug;
  }
  
  
  
  /**  Default methods for creating a new world, base, and user interface.
    */
  public void beginGameSetup() {
    initScenario(savesPrefix);
  }
  
  
  protected void initScenario(String prefix) {
    final String savePath = prefix == null ? null : latestSave(prefix);
    I.say("\nSave path is: "+savePath);
    
    if (SaveUtils.saveExists(savePath) && ! skipNextLoad) {
      I.say("\n\nLoading scenario from save file...");
      loadGame(savePath, false);
      return;
    }
    
    I.say("\n\nBeginning scenario setup from scratch...");
    SaveUtils.deleteAllSavesWithPrefix(savesPrefix);
    loadProgress = 0;
    final Thread loadingThread = new Thread() {
      public void run() {
        world = createWorld();
        loadProgress = 0.2f;
        I.say("\n  World created...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        base = createBase(world);
        loadProgress = 0.4f;
        I.say("\n  Base created...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        UI = createUI(base, PlayLoop.rendering());
        loadProgress = 0.6f;
        I.say("\n  UI setup done...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        configureScenario(world, base, UI);
        loadProgress = 0.8f;
        I.say("\n  Configured scenario...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        afterCreation();
        loadProgress = 1.0f;
        I.say("\n  Setup complete...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
      }
    };
    loadingThread.start();
  }
  
  
  public boolean isLoading() {
    return loadProgress < 1 && loadProgress != -1;
  }
  
  
  public float loadProgress() {
    return loadProgress;
  }
  
  
  public boolean wipeAssetsOnExit() {
    return true;
  }
  
  
  protected BaseUI createUI(Base base, Rendering rendering) {
    BaseUI UI = new BaseUI(base.world, this, rendering);
    UI.assignBaseSetup(base, new Vec3D(8, 8, 0));
    return UI;
  }
  
  
  protected abstract Stage createWorld();
  protected abstract Base createBase(Stage world);
  protected abstract void configureScenario(Stage world, Base base, BaseUI UI);
  protected abstract void afterCreation();
  
  
  
  /**  Methods for keeping track of saved and loaded state-
    */
  public void scheduleSave() {
    this.nextOp = DO_SAVE;
  }
  
  
  public void scheduleSaveAndExit() {
    this.nextOp = DO_SAVE_EXIT;
  }
  
  
  public void scheduleReset() {
    this.nextOp = DO_RESTART;
  }
  
  
  public void scheduleReload() {
    this.nextOp = DO_LOAD;
  }
  
  
  private void saveGame(final String saveFile) {
    try {
      loadProgress = 0;
      lastSaveTime = world.currentTime();
      Session.saveSession(saveFile, this);
      afterSaving();
      loadProgress = 1;
      
      try { Thread.sleep(250); }
      catch (Exception e) {}
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  private void resetScenario() {
    loadProgress = -1;
    this.world = null;
    this.base  = null;
    this.UI    = null;
    PlayLoop.sessionStateWipe();
    initScenario(null);
    PlayLoop.setupAndLoop(this);
  }
  
  
  
  /**  Methods for override by subclasses-
    */
  public boolean shouldExitLoop() {
    //
    //  NOTE:  These operations *have* to be called from this point in the loop
    //         in order to ensure that the old scenario has the chance to exit
    //         the loop (if applicable.)  You can get nasty bugs otherwise.
    if (nextOp == DO_SAVE_EXIT) {
      saveGame(fullSavePath(savesPrefix, timeSuffix(world)));
      return true;
    }
    if (nextOp == DO_SAVE) {
      saveGame(fullSavePath(savesPrefix, timeSuffix(world)));
    }
    if (nextOp == DO_RESTART) {
      resetScenario();
    }
    if (nextOp == DO_LOAD) {
      final String lastSave = latestSave(savesPrefix);
      if (lastSave != null) loadGame(lastSave, true);
    }
    nextOp = DO_PLAY;
    return false;
  }
  
  
  public void renderVisuals(Rendering rendering) {
    if (world == null) return;
    
    if ((! isDebug) && PlayLoop.gameSpeed() != 1) {
      final Colour blur = new Colour().set(0.5f, 0.5f, 0.1f, 0.4f);
      world.ephemera.applyFadeColour(blur);
    }
    
    final Base player = UI.played();
    world.renderFor(rendering, player);
    Base.renderMissions(world, rendering, player);
    
    if (GameSettings.showPaths) DebugPlacing.showZonePathing();
  }
  
  
  public void updateGameState() {
    if (world == null) return;
    /*
    if ((! isDebug) && PlayLoop.gameSpeed() < 1) {
      Power.applyTimeDilation(PlayLoop.gameSpeed(), this);
    }
    if ((! isDebug) && PlayLoop.gameSpeed() > 1) {
      Power.applyResting(PlayLoop.gameSpeed(), this);
    }
    //*/
    world.updateWorld();
  }
  
  
  public void afterSaving() {
    //world.ephemera.applyFadeColour(Colour.GREY);
    //if (! isDebug) Power.applyWalkPath(this);
  }
  
  
  public void afterLoading(boolean fromMenu) {
    //world.ephemera.applyFadeColour(Colour.BLACK);
    //if ((! isDebug) && (! fromMenu)) Power.applyDenyVision(this);
  }
  
  
  
  /**  Helper/Utility methods-
    */
  public Behaviour taskFor(Actor actor) {
    return null;
  }
}



