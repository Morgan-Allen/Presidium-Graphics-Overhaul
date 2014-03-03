

package src.game.campaign ;
import src.start.*;
import src.game.building.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.graphics.widgets.* ;
import src.util.* ;

import java.io.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;




public abstract class Scenario implements Session.Saveable, Playable {
  
  
  private World world ;
  private Base base ;
  final boolean isDebug ;
  private float loadProgress = 0;
  //private boolean loaded = false;
  
  private BaseUI UI ;
  private List <String> timeStamps = new List <String> () ;
  private String savesPrefix ;
  private float lastSaveTime = -1;
  
  
  
  /*
  public Scenario(World world, Base base, String saveFile) {
    this.world = world ;
    this.base = base ;
    this.savesPrefix = saveFile ;
    this.isDebug = false ;
    //UI = createUI(base, PlayLoop.rendering()) ;
  }
  //*/
  
  
  public Scenario() {
    this(null, false);
  }
  
  
  protected Scenario(String saveFile, boolean isDebug) {
    this.savesPrefix = saveFile ;
    this.isDebug = isDebug ;
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.world() ;
    base = (Base) s.loadObject() ;
    savesPrefix = s.loadString() ;
    lastSaveTime = s.loadFloat();
    isDebug = s.loadBool() ;
    for (int i = s.loadInt() ; i-- > 0 ;) timeStamps.add(s.loadString()) ;
    
    //UI = createUI(base, PlayLoop.rendering()) ;
    //UI.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base) ;
    s.saveString(savesPrefix) ;
    s.saveFloat(lastSaveTime);
    s.saveBool(isDebug) ;
    s.saveInt(timeStamps.size()) ;
    for (String stamp : timeStamps) s.saveString(stamp) ;
    
    UI.saveState(s) ;
  }
  
  
  public World world() { return world ; }
  public Base base() { return base ; }
  public BaseUI UI() { return UI ; }
  
  
  public static Scenario current() {
    final Playable p = PlayLoop.played();
    if (p instanceof Scenario) return (Scenario) p;
    else I.complain("NO CURRENT SCENARIO BEING PLAYED.");
    return null;
  }
  
  
  
  /**  Default methods for creating a new world, base, and user interface.
    */
  public void beginGameSetup() {
    loadProgress = 0;
    final Thread loadingThread = new Thread() {
      public void run() {
        world = createWorld();
        loadProgress = 0.2f;
        
        base = createBase(world);
        loadProgress = 0.4f;
        
        UI = createUI(base, PlayLoop.rendering());
        loadProgress = 0.6f;
        
        configureScenario(world, base, UI);
        savesPrefix = saveFilePrefix(world, base);
        loadProgress = 0.8f;
        
        afterCreation();
        loadProgress = 1.0f;
      }
    };
    loadingThread.run();
  }
  
  
  public boolean isLoading() {
    return loadProgress >= 1;
  }
  
  
  public float loadProgress() {
    return loadProgress;
  }
  
  
  protected BaseUI createUI(Base base, Rendering rendering) {
    BaseUI UI = new BaseUI(base.world, rendering) ;
    UI.assignBaseSetup(base, new Vec3D(8, 8, 0)) ;
    return UI ;
  }
  
  
  protected abstract World createWorld();
  protected abstract Base createBase(World world);
  protected abstract void configureScenario(World world, Base base, BaseUI UI);
  protected abstract String saveFilePrefix(World world, Base base);
  protected abstract void afterCreation();
  
  
  protected void resetScenario() {
    this.world = null ;
    this.base = null ;
    this.UI = null ;
    gameStateWipe() ;
    //this.beginLoadingContent();
    //setupScenario() ;
    PlayLoop.setupAndLoop(this) ;
  }
  
  
  
  
  /**  Saving and loading methods-
    */
  //
  //  TODO:  Saving and Loading functions need to be performed on a background
  //  thread.  In fact... there should be some method of packing them into a
  //  Scenario object's beginLoadingContent() method.
  
  //  No, just overwrite the loadProgress() method (even for saves.)
  
  
  public static void saveGame(String saveFile) {
    final Scenario scenario = current();
    if (scenario == null) return;
    //KeyInput.clearInputs() ;
    try {
      scenario.lastSaveTime = scenario.world.currentTime();
      Session.saveSession(scenario.world(), scenario, saveFile);
      scenario.afterSaving();
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  public static void loadGame(String saveFile, boolean fromMenu) {
    try {
      gameStateWipe();
      final Session s = Session.loadSession(saveFile);
      final Scenario scenario = s.scenario();
      scenario.afterLoading(fromMenu);
      PlayLoop.setupAndLoop(scenario);
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  public static void gameStateWipe() {
    //  TODO:  Look into this more carefully.  Port back to the PlayLoop class?
    //KeyInput.clearInputs();
    Spacing.wipeTempArrays();
    I.talkAbout = null;
    /*
    scenario = null;
    UI       = null;
    if (rendering != null) rendering.clearAll();
    lastSaveTime = -1;
    //RuntimeUtil.gc();  //  TODO:  RESTORE THIS?
    //*/
  }
  
  
  public static float timeSinceLastSave() {
    final Scenario scenario = current();
    if (scenario.lastSaveTime == -1) return -1 ;
    float time = scenario.world.currentTime();
    time -= scenario.lastSaveTime;
    return time;
  }
  
  
  
  /**  Methods for keeping track of saved and loaded state-
    */
  //
  //  Saves must be tagged by ruler, day and time.  There can't be more than
  //  three for a given ruler at once- and if there would be, the least
  //  recently saved is deleted.  So you have the following array of files-
  //
  //  saves/<ruler>-current.rep     _ONLY THIS CAN BE LOADED FROM MENU_
  //  saves/<ruler><timestamp A>.rep
  //  saves/<ruler><timestamp B>.rep
  //  saves/<ruler><timestamp C>.rep
  //
  //  When and if you go back in the timeline, subsequent saves are deleted.
  //  So there's always a strictly ascending chronological order.
  
  final public static String
    CURRENT_SAVE = "-current" ;
  final public static int
    MAX_SAVES = 3 ;
  
  
  public String savesPrefix() {
    return savesPrefix ;
  }
  
  
  public void saveProgress(boolean overwrite) {
    //
    //  In the case of an overwrite, just save under the current file-
    if (overwrite) {
      saveGame(fullSavePath(savesPrefix, CURRENT_SAVE)) ;
      return ;
    }
    //
    //  If necessary, delete the least recent save.
    if (timeStamps.size() >= MAX_SAVES) {
      final String oldStamp = timeStamps.removeFirst() ;
      final File f = new File(fullSavePath(savesPrefix, oldStamp)) ;
      if (f.exists()) f.delete() ;
    }
    //
    //  Create a new save.
    final float time = world.currentTime() / World.STANDARD_DAY_LENGTH ;
    String
      day = "Day "+(int) time,
      hour = ""+(int) (24 * (time % 1)),
      minute = ""+(int) (((24 * (time % 1)) % 1) * 60) ;
    while (hour.length() < 2) hour = "0"+hour ;
    while (minute.length() < 2) minute = "0"+minute ;
    
    final String newStamp = day+", "+hour+minute+" Hours" ;
    timeStamps.addLast(newStamp) ;
    saveGame(fullSavePath(savesPrefix, newStamp)) ;
  }
  
  
  public String[] loadOptions() {
    //
    //  Strip away any missing entries-
    for (String stamp : timeStamps) {
      final File f = new File(fullSavePath(savesPrefix, stamp)) ;
      if (! f.exists()) timeStamps.remove(stamp) ;
    }
    return timeStamps.toArray(String.class) ;
  }
  
  
  public void wipeSavesAfter(String option) {
    //
    //  Delete any subsequent time-stamp entries.
    boolean matched = false ; for (String stamp : timeStamps) {
      if (matched) {
        timeStamps.remove(stamp) ;
        final File f = new File(fullSavePath(savesPrefix, stamp)) ;
        if (f.exists()) f.delete() ;
      }
      if (stamp.equals(option)) matched = true ;
    }
    if (! matched) I.complain("NO SUCH TIME STAMP!") ;
  }
  
  
  public static String fullSavePath(String prefix, String suffix) {
    if (suffix == null) return "saves"+prefix+".rep" ;
    return "saves/"+prefix+suffix+".rep" ;
  }
  
  
  public static List <String> savedFiles(String prefix) {
    final List <String> allSaved = new List <String> () ;
    final File savesDir = new File("saves/") ;
    
    for (File saved : savesDir.listFiles()) {
      final String name = saved.getName() ;
      if (! name.endsWith(".rep")) continue ;
      if (prefix == null) {
        if (! name.endsWith(CURRENT_SAVE+".rep")) continue ;
      }
      else if (! name.startsWith(prefix)) continue ;
      allSaved.add(name) ;
    }
    
    return allSaved ;
  }
  
  
  public static boolean loadedFrom(String prefix) {
    final String fullPath = fullSavePath(prefix, CURRENT_SAVE) ;
    final File file = new File(fullPath) ;
    if (! file.exists()) return false ;
    try {
      loadGame(fullPath, true) ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; }
    return false ;
  }
  
  
  
  
  /**  Methods for override by subclasses-
    */
  public boolean shouldExitLoop() {
    if (isDebug) {
      if (Gdx.input.isKeyPressed(Keys.R)) {
        I.say("RESET MISSION?") ;
        resetScenario() ;
        return false ;
      }
      if (Gdx.input.isKeyPressed(Keys.F)) {
        I.say("Paused? "+PlayLoop.paused()) ;
        PlayLoop.setPaused(! PlayLoop.paused()) ;
      }
      if (Gdx.input.isKeyPressed(Keys.S)) {
        I.say("SAVING GAME...") ;
        saveGame(fullSavePath(savesPrefix, CURRENT_SAVE)) ;
        return false ;
      }
      if (Gdx.input.isKeyPressed(Keys.L)) {
        I.say("LOADING GAME...") ;
        loadGame(fullSavePath(savesPrefix, CURRENT_SAVE), true) ;
        return true ;
      }
    }
    return false ;
  }
  
  
  public void renderVisuals(Rendering rendering) {
    if ((! isDebug) && PlayLoop.gameSpeed() != 1) {
      final Colour blur = new Colour().set(0.5f, 0.5f, 0.1f, 0.4f) ;
      world.ephemera.applyFadeColour(blur) ;
    }
    world.renderFor(rendering, base) ;
    base.renderFor(rendering) ;
    UI.renderWorldFX();
  }
  
  
  public void updateGameState() {
    if ((! isDebug) && PlayLoop.gameSpeed() < 1) {
      Power.applyTimeDilation(PlayLoop.gameSpeed(), this) ;
    }
    if ((! isDebug) && PlayLoop.gameSpeed() > 1) {
      Power.applyResting(PlayLoop.gameSpeed(), this) ;
    }
    world.updateWorld() ;
  }
  
  
  public void afterSaving() {
    world.ephemera.applyFadeColour(Colour.GREY) ;
    if (! isDebug) Power.applyWalkPath(this) ;
  }
  
  
  public void afterLoading(boolean fromMenu) {
    world.ephemera.applyFadeColour(Colour.BLACK) ;
    if ((! isDebug) && ! fromMenu) Power.applyDenyVision(this) ;
  }
  
  

  
  /**  Helper/Utility methods-
    */
  public Behaviour taskFor(Actor actor) {
    return null ;
  }
}








