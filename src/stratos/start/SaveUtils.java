


package stratos.start;
import stratos.game.common.Session;
import stratos.game.common.Stage;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.HUD;
import stratos.util.*;

import java.io.File;



public class SaveUtils {
  
  
  final static String
    SAVES_DIR    = "saves/",
    EXT          = ".rep" ,
    PAD_NAME     = "I"    ,
    DIVIDER      = ": "   ;
  
  
  public static String fullSavePath(String prefix, String suffix) {
    if (suffix == null || prefix == null) {
      I.complain("MUST HAVE BOTH SUFFIX AND PREFIX");
    }
    return SAVES_DIR+prefix+suffix+EXT;
  }
  
  
  public static String suffixFor(String fullPath) {
    final int split = fullPath.indexOf(DIVIDER);
    int start = split + DIVIDER.length();
    int end = fullPath.length() - EXT.length();
    return fullPath.substring(start, end);
  }
  
  
  public static String prefixFor(String fullPath) {
    final int split = fullPath.indexOf(DIVIDER);
    return fullPath.substring(0, split);
  }
  
  
  public static boolean saveExists(String saveFile) {
    if (saveFile == null) return false;
    final File file = new File(saveFile);
    if (! file.exists()) return false;
    else return true;
  }
  
  
  public static String uniqueVariant(String prefix) {
    while (latestSave(prefix) != null) prefix = prefix+PAD_NAME;
    return prefix;
  }

  
  public static List <String> savedFiles(String prefix) {
    final List <String> allSaved = new List <String> ();
    final File savesDir = new File(SAVES_DIR);
    
    for (File saved : savesDir.listFiles()) {
      final String name = saved.getName();
      if (! name.endsWith(EXT)) continue;
      if (! name.startsWith(prefix)) continue;
      allSaved.add(name);
    }
    
    return allSaved;
  }
  
  
  public static List <String> latestSaves() {
    final File savesDir = new File(SAVES_DIR);
    final Table <String, String> allPrefixes = new Table <String, String> ();
    final List <String> latest = new List <String> ();
    
    for (File saved : savesDir.listFiles()) {
      final String name = saved.getName();
      if (! name.endsWith(EXT)) continue;
      final String prefix = prefixFor(name);
      if (allPrefixes.get(prefix) != null) continue;
      latest.add(latestSave(prefix));
      allPrefixes.put(prefix, prefix);
    }
    return latest;
  }
  
  
  public static String latestSave(String prefix) {
    //
    //  We use the default alphabetical sorting here, as the only difference
    //  between saves of a given prefix should be the timestamp, and that'll be
    //  picked up in the ASCII codes for hours, minutes, etc (see below.)
    final Sorting <String> sorting = new Sorting <String> () {
      public int compare(String a, String b) {
        return a.compareTo(b);
      }
    };
    for (String s : savedFiles(prefix)) sorting.add(s);
    if (sorting.size() == 0) return null;
    return sorting.greatest();
  }
  
  
  public static String timeStamp(Stage world) {
    final float time = world.currentTime() / Stage.STANDARD_DAY_LENGTH;
    String
      day    = "Day "+(int) time,
      hour   = ""+(int)   (24 * (time % 1)),
      minute = ""+(int) (((24 * (time % 1)) % 1) * 60);
    while (hour  .length() < 2) hour   = "0"+hour  ;
    while (minute.length() < 2) minute = "0"+minute;
    final String newStamp = DIVIDER+day+", "+hour+minute+" Hours";
    return newStamp;
  }
  
  
  
  //  TODO:  This method should *definitely* only be called from a specific
  //  point in the overall play-loop sequence.  Fix that.
  public static void loadGame(
    final String saveFile, final boolean fromMenu
  ) {
    PlayLoop.sessionStateWipe();
    I.say("Should be loading game from: "+saveFile);
    final Playable loading = new Playable() {
      
      private boolean begun = false, done = false;
      private Scenario loaded = null;

      public HUD UI() { return null; }
      public void updateGameState() {}
      public void renderVisuals(Rendering rendering) {}
      
      public void beginGameSetup() {
        final Thread loadThread = new Thread() {
          public void run() {
            I.say("Beginning loading...");
            try {
              final Session s = Session.loadSession(saveFile);
              try { Thread.sleep(250); }
              catch (Exception e) {}
              loaded = s.scenario();
              I.say("  Loaded scenario is: "+loaded);
              done = true;
            }
            catch (Exception e) { I.report(e); }
          }
        };
        loadThread.start();
        begun = true;
      }
      
      
      public boolean shouldExitLoop() {
        if (loaded == null) return false;
        I.say("Loading complete...");
        PlayLoop.setupAndLoop(loaded);
        return false;
      }
      
      
      public boolean isLoading() {
        return begun;
      }
      
      public float loadProgress() {
        //  TODO:  Implement some kind of progress readout here.
        return done ? 1.0f : 0;//Session.loadProgress();
      }
    };
    PlayLoop.setupAndLoop(loading);
  }
  
}







