/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;



public class Conversion implements Economy, Session.Saveable {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public Class <? extends Venue> facility;
  final public Item raw[], out;
  final public Skill skills[];
  final public float skillDCs[];

  final private String ID;
  final static Table <String, Conversion>
    allConversions = new Table <String, Conversion> ();
  
  
  public Conversion(Class <? extends Venue> facility, Object... args) {
    this(facility, null, args);
  }
  

  public Conversion(
    Class <? extends Venue> facility,
    String customID,
    Object... args
  ) {
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
      else if (o instanceof Service) {
        if (recRaw) { rawB.add(o); rawN.add(num); }
        else { out = Item.withAmount((Service) o, num); }
      }
    }
    //
    //  Then assign the final tallies-
    int i;
    raw = new Item[rawB.size()];
    for (i = 0; i < rawB.size(); i++) {
      raw[i] = Item.withAmount(
        (Service) rawB.atIndex(i),
        (Float  ) rawN.atIndex(i)
      );
    }
    this.out = out;
    skills = (Skill[]) skillB.toArray(Skill.class);
    skillDCs = Visit.fromFloats(skillN.toArray());
    //
    //  And compute an ID for save/load purposes-
    this.facility = facility;
    final String outID = customID == null ? out.type.name : customID;
    if (facility == null) this.ID = "<none> "+outID;
    else this.ID = facility.getSimpleName()+" "+outID;
    allConversions.put(ID, this);
    
    //I.say("ADDING CONVERSION: "+this+" TOTAL: "+allConversions.size());
  }
  
  
  public static Conversion[] parse(
    Class <? extends Venue> facility,
    Object args[][]
  ) {
    Conversion c[] = new Conversion[args.length];
    for (int i = c.length; i-- > 0;) c[i] = new Conversion(facility, args[i]);
    return c;
  }
  
  
  public static Conversion loadConstant(Session s) throws Exception {
    s.loadClass();
    return allConversions.get(s.loadString());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveClass(facility);
    s.saveString(ID);
  }
  
  
  
  /**  Economic helper methods-
    */
  protected int rawPriceValue() {
    int sum = 0; for (Item i : raw) sum += i.type.basePrice;
    return sum;
  }
  
  
  public static Conversion[] allConversions() {
    final Conversion array[] = new Conversion[allConversions.size()];
    return allConversions.values().toArray(array);
  }
  
  
  
  /**  Rendering and interface-
    */
  public String toString() { return ID; }
}







