


package stratos.game.campaign;
import java.io.*;

import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.HUD;
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;



public abstract class Scenario implements Session.Saveable, Playable {
  
  
  private World world ;
  private Base base ;
  final boolean isDebug ;
  private float loadProgress = -1;
  
  private BaseUI UI ;
  private List <String> timeStamps = new List <String> () ;
  private String savesPrefix ;
  private float lastSaveTime = -1;
  
  
  
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
    
    loadProgress = 1;
    UI = createUI(base, PlayLoop.rendering());
    UI.loadState(s) ;
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
    
    I.say("Beginning scenario setup...");
    loadProgress = 0;
    final Thread loadingThread = new Thread() {
      public void run() {
        world = createWorld();
        loadProgress = 0.2f;
        I.say("World created...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        base = createBase(world);
        loadProgress = 0.4f;
        I.say("Base created...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        UI = createUI(base, PlayLoop.rendering());
        loadProgress = 0.6f;
        I.say("UI setup done...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        configureScenario(world, base, UI);
        savesPrefix = saveFilePrefix(world, base);
        loadProgress = 0.8f;
        I.say("Configuring scenario...");
        
        try { Thread.sleep(100); }
        catch (Exception e) {}
        
        afterCreation();
        loadProgress = 1.0f;
        I.say("Setup complete...");
        
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
  
  
  protected BaseUI createUI(Base base, Rendering rendering) {
    BaseUI UI = new BaseUI(base.world, rendering) ;
    UI.assignBaseSetup(base, new Vec3D(8, 8, 0)) ;
    return UI ;
  }
  
  
  protected void resetScenario() {
    loadProgress = -1;
    this.world = null;
    this.base = null;
    this.UI = null;
    PlayLoop.gameStateWipe();
    PlayLoop.setupAndLoop(this);
  }
  
  
  protected abstract World createWorld();
  protected abstract Base createBase(World world);
  protected abstract void configureScenario(World world, Base base, BaseUI UI);
  protected abstract String saveFilePrefix(World world, Base base);
  protected abstract void afterCreation();
  
  
  
  
  /**  Saving and loading methods-
    */
  //
  //  TODO:  Saving and Loading functions need to be performed on a background
  //  thread.  In fact... there should be some method of packing them into a
  //  Scenario object's beginLoadingContent() method.
  
  //  No, just overwrite the loadProgress() method (even for saves.)
  
  
  public static void saveGame(final String saveFile) {
    final Scenario scenario = current();
    if (scenario == null) return;
    scenario.loadProgress = 0;
    //  TODO:  Implement some kind of progress readout here...
    
    final Thread saveThread = new Thread() {
      public void run() {
        try {
          scenario.lastSaveTime = scenario.world.currentTime();
          Session.saveSession(scenario.world(), scenario, saveFile);
          scenario.afterSaving();
          scenario.loadProgress = 1;
          
          try { Thread.sleep(250); }
          catch (Exception e) {}
        }
        catch (Exception e) { I.report(e); }
      }
    };
    saveThread.start();
  }
  
  
  public static void loadGame(final String saveFile, final boolean fromMenu) {
    PlayLoop.gameStateWipe();

    PlayLoop.setupAndLoop(new Playable() {
      private boolean begun = false, done = false;
      
      public void updateGameState() {}
      public void renderVisuals(Rendering rendering) {}
      public HUD UI() { return null; }
      public boolean shouldExitLoop() { return false; }
      
      public void beginGameSetup() {
        final Thread loadThread = new Thread() {
          public void run() {
            try {
              final Session s = Session.loadSession(saveFile);
              done = true;
              try { Thread.sleep(250); }
              catch (Exception e) {}
              
              final Scenario scenario = s.scenario();
              scenario.afterLoading(fromMenu);
              PlayLoop.setupAndLoop(scenario);
            }
            catch (Exception e) { I.report(e); }
          }
        };
        loadThread.start();
        begun = true;
      }
      
      public boolean isLoading() {
        return begun;
      }
      
      public float loadProgress() {
        //  TODO:  Implement some kind of progress readout here.
        return done ? 0.999f : 0;//Session.loadProgress();
      }
    });
  }
  
  
  public static float timeSinceLastSave() {
    final Scenario scenario = current();
    if (scenario == null || scenario.lastSaveTime == -1) return -1;
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
    //  TODO:  These should only be available in debug situations...
    if (isDebug || true) {
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
    if (world == null) return;
    if ((! isDebug) && PlayLoop.gameSpeed() != 1) {
      final Colour blur = new Colour().set(0.5f, 0.5f, 0.1f, 0.4f) ;
      world.ephemera.applyFadeColour(blur) ;
    }
    world.renderFor(rendering, base) ;
    base.renderFor(rendering) ;
    UI.renderWorldFX();
  }
  
  
  public void updateGameState() {
    if (world == null) return;
    if ((! isDebug) && PlayLoop.gameSpeed() < 1) {
      Power.applyTimeDilation(PlayLoop.gameSpeed(), this);
    }
    if ((! isDebug) && PlayLoop.gameSpeed() > 1) {
      Power.applyResting(PlayLoop.gameSpeed(), this);
    }
    world.updateWorld();
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








