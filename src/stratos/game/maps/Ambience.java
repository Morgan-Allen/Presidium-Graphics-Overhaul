


package stratos.game.maps;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public class Ambience {
  
  
  
  /**  Data fields, constructors, and save/load functionality-
    */
  final public static String SQUALOR_DESC[] = {
    "Mild",
    "Moderate",
    "Serious",
    "Terrible",
    "Toxic"
  };
  final public static String AMBIENCE_DESC[] = {
    "Fair",
    "Good",
    "Excellent",
    "Beautiful",
    "Paradise"
  };
  final public static String HAZARD_DESC[] = {
    "Negligible",
    "Threatening",
    "Elevated",
    "Hostile",
    "Mortal"
  };
  final public static String SAFETY_DESC[] = {
    "Nominal",
    "Acceptable",
    "Good",
    "Strong",
    "Absolute"
  };
  final static float
    MIP_BLEND = 2.0f,
    RATE_MULT = 4.0f,
    MAX_LEVEL = 10.0f;
  
  final Stage world;
  final MipMap mapValues;
  
  
  public Ambience(Stage world) {
    this.world = world;
    this.mapValues = new MipMap(world.size);
  }
  
  
  public void loadState(Session s) throws Exception {
    mapValues.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    mapValues.saveTo(s.output());
  }
  
  
  
  /**  UI assistance-
    */
  private static String descFrom(String s[], float level) {
    return s[Visit.clamp((int) (level * s.length), s.length)];
  }
  
  
  public static String squalorDesc(float rating) {
    if (rating <= 0) return descFrom(AMBIENCE_DESC, 0 - rating / MAX_LEVEL);
    return descFrom(SQUALOR_DESC, rating / MAX_LEVEL);
  }
  
  
  public static String dangerDesc(float rating) {
    if (rating <= 0) return descFrom(SAFETY_DESC, 0 - rating / MAX_LEVEL);
    return descFrom(HAZARD_DESC, rating / MAX_LEVEL);
  }
  
  
  
  /**  Queries and value updates-
    */
  public void updateAt(Tile tile) {
    int value = 0;
    final Element owner = tile.onTop();
    
    if (owner instanceof Installation) {
      value = ((Installation) owner).structure().ambienceVal();
    }
    if (owner instanceof Flora) {
      value = ((Flora) owner).growStage();
    }
    
    for (Mobile m : tile.inside()) if (m instanceof Actor) {
      final Actor a = (Actor) m;
      value -= 5 * a.health.stressPenalty();
      final Item outfit = a.gear.outfitEquipped();
      if (outfit != null) value += (outfit.quality - 1) / 2f;
    }
    
    mapValues.set((byte) value, tile.x, tile.y);
  }
  
  
  public float valueAt(Tile t) {
    return mapValues.blendValAt(t.x, t.y, MIP_BLEND) * RATE_MULT;
  }
  
  
  public float valueAt(Target t) {
    final Vec3D v = t.position(null);
    return mapValues.blendValAt(v.x, v.y, MIP_BLEND) * RATE_MULT;
  }
}



