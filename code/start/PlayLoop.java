/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package code.start;
import code.game.common.*;
import code.graphics.common.*;
import code.graphics.widgets.*;
import code.util.*;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.*;




//  *  Human models need skin overlays and device/outfit attachments.
//  *  Text should implement scrolling and scissor clipping. (this might need
//     some attention paid to general UI design, though.)



public final class PlayLoop {
  
  
  /**  Fields and constant definitions-
    */
  public final static int
    UPDATES_PER_SECOND = 10,
    FRAMES_PER_SECOND  = 60,
    
    DEFAULT_WIDTH  = 1200,
    DEFAULT_HEIGHT = 600,
    DEFAULT_HERTZ  = 60,
    
    MIN_SLEEP    = 10,
    SLEEP_MARGIN = 2 ;
  private static boolean verbose = false;
  
  
  private static Rendering rendering;
  private static Playable played, nextToPlay;
  
  private static long lastFrame, lastUpdate;
  private static float frameTime;
  private static long numStateUpdates = 0;
  private static float gameSpeed = 1.0f;
  
  private static boolean
    initDone   = false,
    shouldLoop = false,
    paused     = false,
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
  
  
  
  /**  The big static setup, run and exit methods-
    */
  public static void setupAndLoop(Playable scenario) {
    PlayLoop.played = scenario;
    numStateUpdates = 0;
    gameSpeed = 1.0f;
    
    if (! initDone) {
      initDone = true;
      
      final LwjglApplicationConfiguration
        config = new LwjglApplicationConfiguration();
      config.title = "Stratos";
      config.useGL20 = true;
      config.vSyncEnabled = true;
      config.width = DEFAULT_WIDTH;
      config.height = DEFAULT_HEIGHT;
      config.foregroundFPS = DEFAULT_HERTZ;
      config.backgroundFPS = DEFAULT_HERTZ;
      config.resizable = true;
      config.fullscreen = false;
      
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
          setPaused(true);
        }
        
        public void resume() {
          setPaused(false);
        }
        
        public void render() {
          if (! shouldLoop) return;
          final boolean okay = advanceLoop();
          if (! okay) exitLoop();
        }
      }, config);
    }
  }
  
  
  public static void gameStateWipe() {
    //  TODO:  Look into this more carefully.
    Spacing.wipeTempArrays();
    Composite.wipeCache();
    I.talkAbout = null;
    played = null;
    if (rendering != null) rendering.clearAll();
    //RuntimeUtil.gc();  //  TODO:  RESTORE THIS?
  }
  
  
  public static void exitLoop() {
    shouldLoop = false;
    Gdx.app.exit();
  }
  
  
  private static void initLoop() {
    Assets.compileAssetList("src");
    rendering = new Rendering();
  }
  
  
  private static void disposeLoop() {
    rendering.dispose();
    Assets.dispose();
  }
  
  
  private static boolean advanceLoop() {
    final long
      time      = timeMS(),
      frameGap  = time - lastFrame,
      updateGap = time - lastUpdate;
    final int FRAME_INTERVAL  = 1000 / FRAMES_PER_SECOND;
    final int UPDATE_INTERVAL = (int) (
      1000 / (UPDATES_PER_SECOND * gameSpeed)
    );
    
    if (paused) {
      frameTime = 1.0f;
      lastUpdate = lastFrame = time;
    }
    else {
      frameTime = (updateGap - 0) * 1.0f / UPDATE_INTERVAL;
      frameTime = Visit.clamp(frameTime, 0, 1);
    }
    final Playable current = played;
    float worldTime = (numStateUpdates + frameTime) / UPDATES_PER_SECOND;
    rendering.updateViews(worldTime, frameTime);
    //I.say("Advancing play loop...");
    
    if (Assets.loadProgress() < 1) {
      LoadingScreen.update("Loading Assets", Assets.loadProgress());
      Assets.advanceAssetLoading(FRAME_INTERVAL - (SLEEP_MARGIN * 2));
      rendering.renderDisplay(LoadingScreen.HUD);
      return true;
    }
    
    if (played != current) return true;
    if (played != null && played.loadProgress() < 1) {
      if (! played.isLoading()) played.beginGameSetup();
      LoadingScreen.update("Loading Scenario", played.loadProgress());
      rendering.renderDisplay(LoadingScreen.HUD);
      lastUpdate = lastFrame = time;
      //I.say("Content loading progress: "+played.loadProgress());
      return true;
    }
    
    //  TODO:  I'm updating graphics as fast as possible for the moment, since
    //  I get occasional flicker problems otherwise.  Still seems wasteful,
    //  mind...
    if (played != current) return true;
    if (frameGap >= FRAME_INTERVAL || true) {
      if (played != null) played.renderVisuals(rendering);
      rendering.renderDisplay(played.UI());
      lastFrame = time;
    }
    
    //  Now we essentially 'pretend' that updates were occurring once every
    //  UPDATE_INTERVAL milliseconds:
    if (played != null) {
      final int numUpdates = Math.min(
        (int) (updateGap / UPDATE_INTERVAL),
        (1 + (FRAME_INTERVAL / UPDATE_INTERVAL))
      );
      if (! paused) for (int n = numUpdates ; n-- > 0 ;) {
        if (played != current) return true;
        if (played.shouldExitLoop()) return false;
        ///I.say("UPDATING WORLD?");
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
    return frameTime ;
  }
  
  
  public static boolean paused() {
    return paused ;
  }
  
  
  public static float gameSpeed() {
    return gameSpeed ;
  }
  
  
  public static void setGameSpeed(float mult) {
    gameSpeed = Math.max(0, mult) ;
  }
  
  
  public static void setPaused(boolean p) {
    paused = p ;
  }
  
  
  public static void setNoInput(boolean n) {
    noInput = n ;
  }
}




