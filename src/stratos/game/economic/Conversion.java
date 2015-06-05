/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Conversion extends Index.Entry implements Session.Saveable {
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Index <Conversion> INDEX = new Index <Conversion> ();
  
  final public Item raw[], out;
  final public Skill skills[];
  final public float skillDCs[];
  
  //
  //  NOTE:  We have to use this indirect loading system to avoid certain types
  //  of reference loop during class-initialisation, which can trigger a crash.
  private Class facilityClass;
  private Blueprint facility;
  private String specialName;
  
  
  public Conversion(
    Blueprint facility,
    String customID,
    Object... args
  ) {
    this(facility, null, customID, args);
    if (facility == null) I.say("\nWARNING: NULL FACILITY FOR "+customID);
  }
  
  
  protected Conversion(
    Class <? extends Venue> facilityClass,
    String customID,
    Object... args
  ) {
    this(null, facilityClass, customID, args);
    if (facilityClass == null) I.say("\nWARNING: NULL FACILITY FOR "+customID);
  }
  
  
  private Conversion(
    Blueprint facility,
    Class <? extends Venue> facilityClass,
    String customID,
    Object... args
  ) {
    super(INDEX, customID);
    //
    //  Initially, we record raw materials first, and assume a default
    //  quantity of 1 each (this is also the default used for skill DCs.)
    float num = 1;
    boolean recRaw = true;
    //
    //  Set up temporary storage variables.
    Item out = null;
    Batch rawB = new Batch(), skillB = new Batch();
    Batch rawN = new Batch(), skillN = new Batch();
    //
    //  Iterate over all arguments-
    for (Object o : args) {
      if      (o instanceof Integer) num = (Integer) o;
      else if (o instanceof Float  ) num = (Float) o;
      else if (o instanceof Skill  ) {
        skillB.add(o  );
        skillN.add(num);
      }
      else if (o == TO             ) recRaw = false;
      else if (o instanceof Traded) {
        if (recRaw) { rawB.add(o); rawN.add(num); }
        else { out = Item.withAmount((Traded) o, num); }
      }
    }
    //
    //  Then assign the final tallies-
    int i;
    raw = new Item[rawB.size()];
    for (i = 0; i < rawB.size(); i++) {
      raw[i] = Item.withAmount(
        (Traded) rawB.atIndex(i),
        (Float  ) rawN.atIndex(i)
      );
    }
    this.out = out;
    skills = (Skill[]) skillB.toArray(Skill.class);
    skillDCs = Visit.fromFloats(skillN.toArray());
    //
    //  And register with the parent facility (if possible)-
    this.facility      = facility;
    this.facilityClass = facilityClass;
    if (facility != null) facility.addProduction(this);
  }
  
  
  public static Conversion loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public boolean producesAt(Venue venue) {
    if (facility == venue.blueprint) return true;
    if (facilityClass == venue.getClass()) return true;
    return false;
  }
  
  
  
  /**  Economic helper methods-
    */
  protected int rawPriceValue() {
    int sum = 0; for (Item i : raw) sum += i.type.basePrice();
    return sum;
  }
  
  
  public float testChance(Actor actor, float checkMod) {
    if (skills.length == 0) return 1;
    float chance = 0;
    for (int i = skills.length; i-- > 0;) chance += actor.skills.chance(
      skills[i], skillDCs[i] - checkMod
    );
    return chance / skills.length;
  }
  
  
  public float performTest(Actor actor, float checkMod, float duration) {
    if (skills.length == 0) return 1;
    float result = 0;
    for (int i = skills.length; i-- > 0;) result += actor.skills.test(
      skills[i], skillDCs[i] - checkMod, duration, 1
    );
    return result / skills.length;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String toString() {
    return entryKey();
  }
  
  
  public Conversion attachName(String name) {
    this.specialName = name;
    return this;
  }
  
  
  public String specialName() {
    return specialName;
  }
}












