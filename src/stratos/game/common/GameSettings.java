/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.start.*;
import stratos.util.*;
import java.lang.reflect.*;



public class GameSettings {
  
  
  /**  Global debugging flags.
    */
  //  TODO:  Insert these more widely.
  public static boolean
    initVerbose   = false,
    updateVerbose = false,
    extraVerbose  = false,
    
    mindVerbose   = false,
    senseVerbose  = false,
    evalVerbose   = false,
    stepsVerbose  = false,
    
    healthVerbose = false,
    equipVerbose  = false,
    skillsVerbose = false,
    powerVerbose  = false,
    relateVerbose = false,
    
    fightVerbose  = false,
    patrolVerbose = false,
    talkVerbose   = false,
    looksVerbose  = false,
    treatVerbose  = false,
    buildVerbose  = false,
    makingVerbose = false,
    studyVerbose  = false,
    adminVerbose  = false,
    buysVerbose   = false,
    sellsVerbose  = false,
    homeVerbose   = false,
    workVerbose   = false,
    
    shipsVerbose  = false,
    roadsVerbose  = false,
    questVerbose  = false,
    lawsVerbose   = false;
    
  
  
  /**  Global setting fields which can be freely accessed from anywhere within
    *  the program (though they would typically only be changed during scenario
    *  setup.)
    */
  //  TODO:  It's possible that these should be base or scenario-specific,
  //  rather than global?
  
  
  //
  //  
  final public static float
    SMALL_HUMAN_SCALE = 0.550f,
    BIG_HUMAN_SCALE   = 0.875f;
  
  //
  //  I'm including a number of vital economic constants here, since they have
  //  a pretty significant impact on gameplay.
  final public static float
    SPENDING_MULT  = 1.0f,
    ITEM_WEAR_DAYS = 10  ;
  
  //  Gameplay effects-
  public static boolean
    
    buildFree = false,
    paveFree  = false,
    hireFree  = false,
    cashFree  = false,
    psyFree   = false,
    fogFree   = false,
    pathFree  = true ,
    needsFree = false,
    techsFree = false,
    
    noPsy     = false,
    noBlood   = false,
    noChat    = false,
    noShips   = false,
    noSpawn   = false,
    noAdvice  = false,
    
    fastTrips = false,
    fastRaids = false,
    
    hardCore  = false;
  public static int
    freeHousingLevel = 0;
  
  //  Graphical effects-
  public static boolean
    bigHumans = true ,
    showPaths = false;
  
  
  public static float peopleScale() {
    return bigHumans ? BIG_HUMAN_SCALE : SMALL_HUMAN_SCALE;
  }
  
  
  
  /**  I've decided to switch to reflection methods for saving/loading rather
    *  than using manual calls, since I'm probably going to forget something
    *  eventually otherwise.
    */
  private static Field  settings   [];
  private static Object defaultVals[];
  
  static {
    final Batch <Field> valid = new Batch <Field> ();
    final Batch <Object> vals = new Batch <Object> ();
    
    for (Field f : GameSettings.class.getFields()) try {
      
      final int mods = f.getModifiers();
      if (
        Modifier.isPrivate(mods) ||
        Modifier.isFinal  (mods) ||
        ! f.getType().isPrimitive()
      ) continue;
      
      valid.add(f);
      vals.add(f.get(null));
    }
    catch (Exception e) { I.report(e); }
    
    settings    = valid.toArray(Field.class );
    defaultVals = vals .toArray(Object.class);
  }
  
  
  
  /**  Useful for toggling settings within the main UI for debug purposes:
    */
  public static String[] publishSimpleOptions() {
    final Batch <String> names = new Batch <String> ();
    for (Field setting : settings) {
      final String name = setting.getName();
      if (name.contains("Verbose")) continue;
      if (setting.getType() != boolean.class) continue;
      names.add(name);
    }
    return names.toArray(String.class);
  }
  
  
  public static Object valueForOption(String optionName) {
    try {
      final Field field = GameSettings.class.getField(optionName);
      return field.get(null);
    }
    catch (Exception e) { return null; }
  }
  
  
  public static void assignOptionValue(Object value, String optionName) {
    if (I.logEvents()) {
      I.say("\nOPTION "+optionName+" ASSIGNED VALUE "+value);
    }
    try {
      final Field field = GameSettings.class.getField(optionName);
      field.set(null, value);
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  
  /**  Public access methods for saving, loading, and resets to default values-
    */
  public static void setDefaults() {
    int n = 0;
    for (Field f : settings) try {
      f.set(null, defaultVals[n++]);
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  protected static void saveSettings(Session s) throws Exception {
    for (Field f : settings) s.saveWithType(f.get(null), f.getType());
    s.saveBool(PlayLoop.paused());
  }
  
  
  protected static void loadSettings(Session s) throws Exception {
    for (Field f : settings) f.set(null, s.loadWithType(f.getType()));
    PlayLoop.setPaused(s.loadBool());
  }
}





