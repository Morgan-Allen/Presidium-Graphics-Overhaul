/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.start;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import stratos.game.actors.Backgrounds;
import stratos.game.wild.Species;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.graphics.*;

import java.awt.Dimension;
import java.awt.Toolkit;



public final class PlayLoop {
  
  
  /**  Fields and constant definitions-
    */
  final public static Class DEFAULT_INIT_CLASSES[] = {
    Backgrounds.class,
    Species.class
  };
  final public static String DEFAULT_INIT_PACKAGE = "stratos";
  
  public final static int
    UPDATES_PER_SECOND = 10,
    FRAMES_PER_SECOND  = 60,
    
    DEFAULT_WIDTH  = 1200,
    DEFAULT_HEIGHT = 720,
    DEFAULT_HERTZ  = 60,
    
    MIN_SLEEP    = 10,
    SLEEP_MARGIN = 2;
  private static boolean
    verbose = false;
  
  
  private static String initPackage  = DEFAULT_INIT_PACKAGE;
  private static Class[] initClasses = DEFAULT_INIT_CLASSES;
  
  private static Rendering rendering;
  private static Playable played;
  private static Thread gdxThread;
  private static boolean loopChanged = false;
  
  private static long lastFrame, lastUpdate;
  private static float frameTime;
  private static long numStateUpdates = 0, numFrameUpdates = 0;
  private static float gameSpeed = 1.0f;
  
  private static boolean
    initDone   = false,
    shouldLoop = false,
    paused     = false,
    background = false,
    noInput    = false;
  
  
  
  /**  Returns the components of the current game state-
    */
  public static HUD currentUI() {
    return played.UI();
  }
  
  public static Rendering rendering() {
    return rendering;
  }
  
  public static Playable played() {
    return played;
  }
  
  public static boolean onMainThread() {
    return Thread.currentThread() == gdxThread;
  }
  
  public static boolean mainThreadBegun() {
    return gdxThread != null;
  }
  
  
  
  
  /**  The big static setup, run and exit methods-
    */
  private static LwjglApplicationConfiguration getConfig() {
    final Dimension SS = Toolkit.getDefaultToolkit().getScreenSize();
    final boolean report = true;
    
    final LwjglApplicationConfiguration
      config = new LwjglApplicationConfiguration()
    ;
    config.title = "Stratos";
    config.useGL20      = true;
    config.vSyncEnabled = true;
    config.width  = Nums.min(DEFAULT_WIDTH , SS.width  - 100);
    config.height = Nums.min(DEFAULT_HEIGHT, SS.height - 100);
    config.foregroundFPS = DEFAULT_HERTZ;
    config.backgroundFPS = DEFAULT_HERTZ;
    config.resizable  = false;
    config.fullscreen = false;
    
    if (report) {
      I.say("\nSetting up screen configuration...");
      I.say("  Default width/height: "+DEFAULT_WIDTH+"/"+DEFAULT_HEIGHT);
      I.say("  Screen  width/height: "+SS    .width +"/"+SS    .height );
      I.say("  Window  width/height: "+config.width +"/"+config.height );
      I.say("");
    }
    return config;
  }
  
  
  public static void setupAndLoop(Playable scenario) {
    setupAndLoop(scenario, DEFAULT_INIT_PACKAGE, DEFAULT_INIT_CLASSES);
  }
  
  
  public static void setupAndLoop(
    Playable scenario, String initPackage, Class... initClasses
  ) {
    
    PlayLoop.initPackage = initPackage;
    PlayLoop.initClasses = initClasses;
    
    PlayLoop.loopChanged     = true;
    PlayLoop.played          = scenario;
    PlayLoop.numStateUpdates = 0;
    PlayLoop.numFrameUpdates = 0;
    PlayLoop.gameSpeed       = 1.0f;
    
    if (verbose) {
      I.say("ASSIGNED NEW PLAYABLE: "+scenario);
      I.reportStackTrace();
    }
    
    if (! initDone) {
      initDone = true;
      
      new LwjglApplication(new ApplicationListener() {
        public void create() {
          shouldLoop = true;
          initLoop();
        }
        
        public void resize(int width, int height) {
        }
        
        public void dispose() {
          disposeLoop();
        }
        
        public void pause() {
          background = true;
        }
        
        public void resume() {
          background = false;
        }
        
        public void render() {
          gdxThread = Thread.currentThread();
          if (! shouldLoop) {
            if (verbose) I.say("should not be looping...");
            return;
          }
          
          final boolean okay = advanceLoop();
          
          if (! okay) {
            if (verbose) I.say("Loop does not want to advance!");
            exitLoop();
          }
        }
      }, getConfig());
    }
  }
  
  
  public static void sessionStateWipe() {
    I.talkAbout = null;
    played = null;
    Assets.disposeSessionAssets();
    
    if (rendering != null) rendering.clearAll();
  }
  
  
  public static void exitLoop() {
    if (verbose) I.say("EXITING PLAY LOOP");
    shouldLoop = false;
    Gdx.app.exit();
  }
  
  
  private static void initLoop() {
    Assets.compileAssetList(
      initPackage, initClasses
    );
    for (String name : Assets.classesToLoad()) {
      Session.checkSaveable(name);
    }
    rendering = new Rendering();
  }
  
  
  private static void disposeLoop() {
    rendering.dispose();
    Assets.disposeGameAssets();
  }
  
  
  private static boolean advanceLoop() {
    final long time = timeMS(), frameGap = time - lastFrame, updateGap;
    final int FRAME_INTERVAL  = 1000 / FRAMES_PER_SECOND;
    final int UPDATE_INTERVAL = (int) (
      1000 / (UPDATES_PER_SECOND * gameSpeed)
    );
    final boolean freeze = paused || background;
    
    if (freeze || (time - lastUpdate) > UPDATE_INTERVAL * 10) {
      lastUpdate = time;
      updateGap = 0;
    }
    else {
      updateGap = time - lastUpdate;
      frameTime = (updateGap - 0) * 1.0f / UPDATE_INTERVAL;
      frameTime = Nums.clamp(frameTime, 0, 1);
    }
    
    loopChanged = false;
    float worldTime = (numStateUpdates + frameTime) / UPDATES_PER_SECOND;
    rendering.updateViews(worldTime, frameTime);
    
    if (verbose) {
      I.say("\nAdvancing play loop, time: "+time);
      I.say("  Last frame/last update: "+lastFrame+"/"+lastUpdate);
      I.say("  Frame/update gap: "+frameGap+"/"+updateGap);
      I.say("  FRAME/UPDATE INTERVAL: "+FRAME_INTERVAL+"/"+UPDATE_INTERVAL);
    }
    
    if (Assets.loadProgress() < 1) {
      if (verbose) {
        I.say("  Loading assets!");
        I.say("  Loading progress: "+Assets.loadProgress());
      }
      
      LoadingScreen.update("Loading Assets", Assets.loadProgress());
      Assets.advanceAssetLoading(FRAME_INTERVAL - (SLEEP_MARGIN * 2));
      
      rendering.renderDisplay();
      rendering.renderUI(LoadingScreen.HUD(rendering));
      return true;
    }
    
    if (loopChanged) {
      if (verbose) I.say("  Loop changed!  Will return");
      return true;
    }
    if (played != null && played.loadProgress() < 1) {
      if (verbose) {
        I.say("  Loading simulation: "+played);
        I.say("  Is loading?         "+played.isLoading());
        I.say("  Loading progress:   "+played.loadProgress());
      }
      
      if (! played.isLoading()) {
        if (verbose) I.say("  Beginning simulation setup...");
        played.beginGameSetup();
      }
      LoadingScreen.update("Loading Simulation", played.loadProgress());
      
      rendering.renderDisplay();
      rendering.renderUI(LoadingScreen.HUD(rendering));
      lastUpdate = lastFrame = time;
      return true;
    }
    
    //  TODO:  I'm updating graphics as fast as possible for the moment, since
    //  I get occasional flicker problems otherwise.  Still seems wasteful,
    //  mind...
    if (loopChanged) {
      if (verbose) I.say("  Loop changed!  Will return");
      return true;
    }
    if (frameGap >= FRAME_INTERVAL || true) {
      if (verbose) I.say("  Rendering graphics.");
      
      if (played != null) {
        played.renderVisuals(rendering);
      }
      final HUD UI = played.UI();
      if (UI != null) {
        UI.updateInput();
        UI.renderWorldFX();
      }
      rendering.renderDisplay();
      rendering.renderUI(UI);
      KeyInput.updateInputs();
      lastFrame = time;
      numFrameUpdates++;
    }
    
    //  Now we essentially 'pretend' that updates were occurring once every
    //  UPDATE_INTERVAL milliseconds:
    if (played != null) {
      final int numUpdates = Nums.min(
        (int) (updateGap / UPDATE_INTERVAL),
        (1 + (FRAME_INTERVAL / UPDATE_INTERVAL))
      );
      if (played.shouldExitLoop()) {
        if (verbose) I.say("  Exiting loop!  Will return");
        return false;
      }
      
      if (verbose) I.say("  No. of updates: "+numUpdates);
      if (! freeze) for (int n = numUpdates; n-- > 0;) {
        
        if (loopChanged) {
          if (verbose) I.say("  Loop changed!  Will return");
          return true;
        }
        if (verbose) I.say("  Updating simulation.");
        played.updateGameState();
        numStateUpdates++;
        lastUpdate += UPDATE_INTERVAL;
      }
    }
    return true;
  }
  
  
  private static long timeMS() {
    return java.lang.System.nanoTime() / 1000000;
  }
  
  
  
  /**  Pausing the loop, exiting the loop, and setting simulation speed and
    *  frame rate.
    */
  public static float frameTime() {
    return frameTime;
  }
  
  
  public static long frameUpdates() {
    return numFrameUpdates;
  }
  
  
  public static boolean isFrameIncrement(int unit) {
    return (numFrameUpdates % unit) == 0;
  }
  
  
  public static boolean paused() {
    return paused;
  }
  
  
  public static float gameSpeed() {
    return gameSpeed;
  }
  
  
  public static void setGameSpeed(float mult) {
    gameSpeed = Nums.max(0, mult);
  }
  
  
  public static void setPaused(boolean p) {
    paused = p;
  }
  
  
  public static void setNoInput(boolean n) {
    noInput = n;
  }
}




