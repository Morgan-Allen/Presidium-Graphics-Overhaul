/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.common.*;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.HUD;
import stratos.util.*;
import java.io.File;



public class SaveUtils {
  
  
  final static String
    SAVES_DIR    = "saves/",
    EXT          = ".rep" ,
    PAD_NAME     = "I"    ,
    DIVIDER      = ": "   ,
    DAYS_SEP     = ", "   ,
    DAY_LABEL    = "Day"  ,
    HOURS_LABEL  = "Hours";
  
  
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
    final File file = new File(SAVES_DIR+saveFile);
    if (! file.exists()) return false;
    else return true;
  }
  
  
  public static String uniqueVariant(String prefix) {
    while (latestSave(prefix) != null) prefix = prefix+PAD_NAME;
    return prefix;
  }
  
  
  public static String[] latestSaves() {
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
    return latest.toArray(String.class);
  }
  
  
  public static String latestSave(String prefix) {
    final String saves[] = savedFiles(prefix);
    if (saves.length == 0) return null;
    return saves[saves.length - 1];
  }
  
  
  public static String[] savedFiles(final String prefix) {
    //
    //  In essence, we filter out all numeric digits in the string and use that
    //  to establish a sorting order in time.
    final Sorting <String> sorting = new Sorting <String> () {
      public int compare(String a, String b) {
        final int
          timeA = getTimeDigits(a, prefix),
          timeB = getTimeDigits(b, prefix);
        if (timeA > timeB) return  1;
        if (timeB > timeA) return -1;
        return 0;
      }
    };
    final File savesDir = new File(SAVES_DIR);
    
    for (File saved : savesDir.listFiles()) {
      final String name = saved.getName();
      if (! name.endsWith(EXT)) continue;
      if (! name.startsWith(prefix)) continue;
      sorting.add(name);
    }
    return sorting.toArray(String.class);
  }
  
  
  public static int getTimeDigits(String fileName, String prefix) {
    final StringBuffer s = new StringBuffer();
    for (char c : fileName.substring(prefix.length()).toCharArray()) {
      if (c < '0' || c > '9') continue;
      else s.append(c);
    }
    return Integer.parseInt(s.toString());
  }
  
  
  public static String timeStamp(Stage world) {
    final float time = world.currentTime() / Stage.STANDARD_DAY_LENGTH;
    String
      day    = DAY_LABEL+" "+(int) time,
      hour   = ""+(int)   (24 * (time % 1)),
      minute = ""+(int) (((24 * (time % 1)) % 1) * 60);
    while (hour  .length() < 2) hour   = "0"+hour  ;
    while (minute.length() < 2) minute = "0"+minute;
    final String newStamp = DIVIDER+day+", "+hour+minute+" "+HOURS_LABEL;
    return newStamp;
  }
  
  
  public static void deleteAllLaterSaves(String saveFile) {
    final String prefix = prefixFor(saveFile);
    boolean matchFound = false;
    for (String fileName : savedFiles(prefix)) {
      if (matchFound) new File(SAVES_DIR+fileName).delete();
      if (fileName.equals(saveFile)) matchFound = true;
    }
  }
  
  
  public static void deleteAllSavesWithPrefix(String prefix) {
    if (prefix == null) return;
    for (String fileName : savedFiles(prefix)) {
      new File(SAVES_DIR+fileName).delete();
    }
  }
  
  
  
  //  TODO:  This method should *definitely* only be called from a specific
  //  point in the overall play-loop sequence.  Fix that.
  
  //  NOTE:  The argument is assumed to be the name of the file *within* the
  //         saves directory, not the entire system path.
  public static void loadGame(
    final String saveFile, final boolean fromMenu
  ) {
    PlayLoop.sessionStateWipe();
    deleteAllLaterSaves(saveFile);
    
    final String fullPath = SAVES_DIR+saveFile;
    I.say("Should be loading game from: "+fullPath);
    
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
              final Session s = Session.loadSession(fullPath);
              try { Thread.sleep(250); }
              catch (Exception e) {}
              loaded = s.scenario();
              I.say("  Loaded scenario is: "+loaded);
              done = true;
            }
            catch (Exception e) { e.printStackTrace(); }
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







