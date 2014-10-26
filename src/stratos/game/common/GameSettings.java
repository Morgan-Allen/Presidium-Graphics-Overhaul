/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.util.*;
import java.lang.reflect.*;



public class GameSettings {
  
  
  /**  Global setting fields which can be freely accessed from anywhere within
    *  the program (though they would typically only be changed during scenario
    *  setup.)
    */
  //  TODO:  It's possible that these should be base or scenario-specific,
  //  rather than global?
  final public static float
    SMALL_SPRITE_SCALE = 0.55f,
    BIG_SPRITE_SCALE   = 0.85f;
  
  public static boolean
    
    buildFree = false,
    paveFree  = false,
    hireFree  = false,
    psyFree   = false,
    fogFree   = false,
    pathFree  = false,
    needsFree = false,
    noBlood   = false,
    
    hardCore  = false;
  
  public static int
    freeHousingLevel = 0;
  
  public static float
    actorScale = SMALL_SPRITE_SCALE;
  
  
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
    for (Field f : settings) try {
      final Type t = f.getType();
      if      (t == Boolean.TYPE) s.saveBool (f.getBoolean(null));
      else if (t == Float  .TYPE) s.saveFloat(f.getFloat  (null));
      else if (t == Integer.TYPE) s.saveInt  (f.getInt    (null));
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  protected static void loadSettings(Session s) throws Exception {

    for (Field f : settings) try {
      final Type t = f.getType();
      if      (t == Boolean.TYPE) f.setBoolean(null, s.loadBool ());
      else if (t == Float  .TYPE) f.setFloat  (null, s.loadFloat());
      else if (t == Integer.TYPE) f.setInt    (null, s.loadInt  ());
    }
    catch (Exception e) { I.report(e); }
  }
}





