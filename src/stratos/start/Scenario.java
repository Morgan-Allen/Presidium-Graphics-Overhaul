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
  
  private BaseUI UI;
  private String savesPrefix;
  private float lastSaveTime = -1;
  
  
  
  protected Scenario(String saveFile, boolean isDebug) {
    this.savesPrefix = saveFile;
    this.isDebug     = isDebug ;
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this);
    world = s.world();
    base  = (Base) s.loadObject();
    savesPrefix  = s.loadString();
    lastSaveTime = s.loadFloat();
    isDebug      = s.loadBool();
    
    loadProgress = 1;
    UI = createUI(base, PlayLoop.rendering());
    UI.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base             );
    s.saveString(savesPrefix      );
    s.saveFloat (lastSaveTime     );
    s.saveBool  (isDebug          );
    UI.saveState(s);
  }
  
  
  public Stage  world() { return world; }
  public Base   base () { return base ; }
  public BaseUI UI   () { return UI   ; }
  
  
  public static Scenario current() {
    final Playable p = PlayLoop.played();
    if (p instanceof Scenario) return (Scenario) p;
    else I.complain("NO CURRENT SCENARIO BEING PLAYED.");
    return null;
  }
  
  
  public String savesPrefix() {
    return savesPrefix;
  }
  
  
  protected void setSavesPrefix(String prefix) {
    this.savesPrefix = prefix;
  }
  
  
  
  /**  Default methods for creating a new world, base, and user interface.
    */
  public void beginGameSetup() {
    initScenario(savesPrefix);
  }
  
  
  protected void initScenario(String prefix) {
    final String savePath = prefix == null ? null : latestSave(prefix);
    I.say("\nSave path is: "+savePath);
    
    if (SaveUtils.saveExists(savePath)) {
      I.say("\n\nLoading scenario from save file...");
      loadGame(savePath, false);
      return;
    }
    
    I.say("\n\nBeginning scenario setup from scratch...");
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
        //savesPrefix = saveFilePrefix(world, base);
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
      Session.saveSession(world, this, saveFile);
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
      saveGame(fullSavePath(savesPrefix, timeStamp(world)));
      return true;
    }
    if (nextOp == DO_SAVE) {
      saveGame(fullSavePath(savesPrefix, timeStamp(world)));
    }
    if (nextOp == DO_RESTART) {
      resetScenario();
    }
    if (nextOp == DO_LOAD) {
      loadGame(latestSave(savesPrefix), true);
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


    /*
    if (isDebug || true) {
      if (KeyInput.wasTyped('r')) {
        I.say("RESET MISSION?");
        resetScenario();
        return false;
      }
      if (KeyInput.wasTyped('f')) {
        I.say("Paused? "+PlayLoop.paused());
        PlayLoop.setPaused(! PlayLoop.paused());
      }
      if (KeyInput.wasTyped('s')) {
        I.say("SAVING GAME...");
        saveGame(fullSavePath(savesPrefix, CURRENT_SAVE));
        return false;
      }
      if (KeyInput.wasTyped('l')) {
        I.say("LOADING GAME...");
        loadGame(fullSavePath(savesPrefix, CURRENT_SAVE), true);
        return false;
      }
    }
    //*/



  /*
  final public static String
    CURRENT_SAVE = "-current";
  final public static int
    MAX_SAVES = 3;
  
  
  public void saveProgress(final boolean overwrite, final boolean quit) {
    final Thread saveThread = new Thread() {
      public void run() {
        
        //  In the case of an overwrite, just save under the current file-
        if (overwrite) {
          saveGame(fullSavePath(savesPrefix, CURRENT_SAVE));
          if (quit) PlayLoop.exitLoop();
          return;
        }
        
        //  If necessary, delete the least recent save.
        if (timeStamps.size() >= MAX_SAVES) {
          final String oldStamp = timeStamps.removeFirst();
          final File f = new File(fullSavePath(savesPrefix, oldStamp));
          if (f.exists()) f.delete();
        }
        
        //  Create a new save.
        final float time = world.currentTime() / Stage.STANDARD_DAY_LENGTH;
        String
          day = "Day "+(int) time,
          hour = ""+(int) (24 * (time % 1)),
          minute = ""+(int) (((24 * (time % 1)) % 1) * 60);
        while (hour.length() < 2) hour = "0"+hour;
        while (minute.length() < 2) minute = "0"+minute;
        
        final String newStamp = day+", "+hour+minute+" Hours";
        timeStamps.addLast(newStamp);
        saveGame(fullSavePath(savesPrefix, newStamp));
        if (quit) PlayLoop.exitLoop();
      }
    };
    saveThread.start();
  }
  
  
  private void saveGame(final String saveFile) {
    try {
      loadProgress = 0;
      lastSaveTime = world.currentTime();
      Session.saveSession(world, this, saveFile);
      afterSaving();
      loadProgress = 1;
      
      try { Thread.sleep(250); }
      catch (Exception e) {}
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  public static void loadGame(
    final String saveFile, final boolean fromMenu
  ) {
    I.say("Should be loading game from: "+saveFile);
    
    PlayLoop.sessionStateWipe();
    final Playable loading = new Playable() {
      
      private boolean begun = false, done = false;
      
      public void updateGameState() {}
      public void renderVisuals(Rendering rendering) {}
      public HUD UI() { return null; }
      public boolean shouldExitLoop() { return false; }
      
      public void beginGameSetup() {
        final Thread loadThread = new Thread() {
          public void run() {
            I.say("Beginning loading...");
            try {
              final Session s = Session.loadSession(saveFile);
              done = true;
              try { Thread.sleep(250); }
              catch (Exception e) {}
              
              final Scenario scenario = s.scenario();
              scenario.afterLoading(fromMenu);
              PlayLoop.setupAndLoop(scenario);
              I.say("Loading complete...");
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
    };
    PlayLoop.setupAndLoop(loading);
  }
  
  
  public static boolean saveExists(String saveFile) {
    final File file = new File(saveFile);
    if (! file.exists()) return false;
    else return true;
  }
  
  
  public float timeSinceLastSave() {
    if (lastSaveTime == -1) return -1;
    return world.currentTime() - lastSaveTime;
  }
  
  
  public static String fullSavePath(String prefix, String suffix) {
    if (suffix == null) suffix = CURRENT_SAVE;
    return "saves/"+prefix+suffix+".rep";
  }
  
  
  public static List <String> savedFiles(String prefix) {
    final List <String> allSaved = new List <String> ();
    final File savesDir = new File("saves/");
    
    for (File saved : savesDir.listFiles()) {
      final String name = saved.getName();
      if (! name.endsWith(".rep")) continue;
      if (prefix == null) {
        if (! name.endsWith(CURRENT_SAVE+".rep")) continue;
      }
      else if (! name.startsWith(prefix)) continue;
      allSaved.add(name);
    }
    
    return allSaved;
  }
  
  
  public String[] loadOptions() {
    //
    //  Strip away any missing entries-
    for (String stamp : timeStamps) {
      final File f = new File(fullSavePath(savesPrefix, stamp));
      if (! f.exists()) timeStamps.remove(stamp);
    }
    return timeStamps.toArray(String.class);
  }
  
  
  public void revertTo(String option) {
    //  Delete any subsequent time-stamp entries.
    boolean matched = false; for (String stamp : timeStamps) {
      if (matched) timeStamps.remove(stamp);
      if (stamp.equals(option)) matched = true;
    }
    if (! matched) I.complain("NO SUCH TIME STAMP!");
    
    //  Delete any files with this prefix not contained within the time-stamps.
    for (String file : savedFiles(savesPrefix)) {
      boolean okay = false;
      for (String stamp : timeStamps) {
        final String match = fullSavePath(savesPrefix, stamp);
        if (file.equals(match)) okay = true;
      }
      if (! okay) {
        final File f = new File(fullSavePath(savesPrefix, file));
        if (f.exists()) f.delete();
      }
    }
    
    //  And finally, load the earlier game-
    Scenario.loadGame(Scenario.fullSavePath(savesPrefix, option), false);
  }
  //*/
  

