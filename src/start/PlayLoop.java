/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.start;
import src.util.*;
import src.graphics.widgets.*;
import src.graphics.common.*;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.*;




//  *  Text must implement scrolling and scissor clipping.
//  *  Human models need skin overlays and device/outfit attachments.
//  *  Building sprites need item-stock displays and light overlays?
//  *  The various special FX need to be re-implemented or else dummied out.
//  *  Minimap must be fixed.
//  *  Terrain rendering and BuildingSprites must be figured out.



public final class PlayLoop {
  
  
  /**  Fields and constant definitions-
    */
  public final static int
    UPDATES_PER_SECOND = 10,
    FRAMES_PER_SECOND  = 30,
    
    DEFAULT_WIDTH  = 1000,
    DEFAULT_HEIGHT = 600,
    DEFAULT_HERTZ  = 60,
    
    MIN_SLEEP    = 10,
    SLEEP_MARGIN = 2 ;

  private static boolean verbose = false;
  
  
  private static Rendering rendering ;
  private static HUD UI ;
  private static Playable played ;

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
    return UI ;
  }
  
  public static Rendering rendering() {
    return rendering ;
  }
  
  public static Playable played() {
    return played ;
  }
  
  
  
  /**  The big static setup, run and exit methods-
    */
  public static void setupAndLoop(Playable scenario) {
    PlayLoop.UI = null;
    PlayLoop.played = scenario;
    numStateUpdates = 0;
    gameSpeed = 1.0f;
    
    if (! initDone) {
      initDone = true;
      
      final LwjglApplicationConfiguration
        config = new LwjglApplicationConfiguration();
      config.title = "Presidium";
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
    
    if (paused) frameTime = 1.0f;
    else {
      frameTime = (updateGap - FRAME_INTERVAL) * 1.0f / UPDATE_INTERVAL;
      frameTime = Math.max(0, Math.min(1, frameTime));
    }
    
    ///I.say("Advancing play loop...");
    
    if (Assets.loadProgress() < 1) {
      LoadingScreen.update("Loading Assets", Assets.loadProgress());
      Assets.advanceAssetLoading(FRAME_INTERVAL - (SLEEP_MARGIN * 2));
      rendering.renderDisplay(0, 0, LoadingScreen.HUD);
      return true;
    }
    
    if (played != null && played.loadProgress() < 1) {
      if (! played.isLoading()) played.beginGameSetup();
      LoadingScreen.update("Loading Scenario", played.loadProgress());
      rendering.renderDisplay(0, 0, LoadingScreen.HUD);
      lastUpdate = lastFrame = timeMS();
      if (UI == null) UI = played.UI();
      ///I.say("Content loading progress: "+played.loadProgress());
      return true;
    }
    
    if (frameGap >= FRAME_INTERVAL) {
      if (played != null) played.renderVisuals(rendering);
      float worldTime = (numStateUpdates + frameTime) / UPDATES_PER_SECOND;
      rendering.renderDisplay(worldTime, frameTime, UI);
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
        if (played.shouldExitLoop()) return false;
        played.updateGameState() ;
        numStateUpdates++;
      }
      lastUpdate += numUpdates * UPDATE_INTERVAL ;
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




