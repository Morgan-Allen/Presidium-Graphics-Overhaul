


package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.util.*;



public class Dining extends Plan {
  
  
  /**  Data fields, setup and save/load methods-
    */
  final static int
    TYPE_COOKING = 0,
    TYPE_MIXING  = 1,
    TYPE_EATING  = 2;
  
  final int type;
  
  
  private Dining(Actor cook, Venue served, int type) {
    super(cook, served, MOTIVE_JOB, NO_HARM);
    this.type = type;
  }
  
  
  public Dining(Session s) throws Exception {
    super(s);
    type = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Dining(other, (Venue) subject, type);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    return null;
  }
  
  
  protected float getPriority() {
    return 0;
  }
  
  
  
  /**  Meal definition for item storage-
    */
  final static String MEAL_NAMES[] = {
    "Soylent Beige",
    "Hiver's Milk",
    "Enriched Kelp Smoothie",
    "Sonic-Fried Carbs",
    "Grub!(tm)",
    "Grebulan Lily Salad",
    "Oni Rice Pastries",
    "Teluvian Spyce Wings",
    "Ciambotta Agar Deluxe"
  };
  
  final static String COCKTAIL_NAMES[] = {
    "Stir-Mixed Downer",
    "Red Pill Daquiri",
    "9-Dimensional Gargle Blaster"
  };
  
  final static String GARNISH_NAMES[] = {
    "Vacuum Wraps",
    "Grilled Roaches",
    "Vita-Paste",
    "Gamma Puffs",
    "Qudu Nut Soup",
    "Cerulean Coleslaw"
  };
  
  public static class Meal implements Session.Saveable {
    
    int quality;
    boolean cocktail;
    int nameA, nameB, nameC;  //  Refer to name-strings above...
    
    
    public static Meal loadConstant(Session s) throws Exception {
      final Meal m = new Meal();
      m.quality  = s.loadInt ();
      m.cocktail = s.loadBool();
      m.nameA    = s.loadInt ();
      m.nameB    = s.loadInt ();
      m.nameC    = s.loadInt ();
      return m;
    }
    
    
    public void saveState(Session s) throws Exception {
      s.saveInt (quality );
      s.saveBool(cocktail);
      s.saveInt (nameA   );
      s.saveInt (nameB   );
      s.saveInt (nameC   );
    }
    
    
    public String toString() {
      String a = "", b = "", c = "";
      if (nameA != -1) a = MEAL_NAMES[nameA];
      if (nameB != -1) b = COCKTAIL_NAMES[nameB];
      if (nameC != -1) c = " with "+GARNISH_NAMES[nameC];
      if (nameA != -1 && nameB != -1) b = " and a "+b;
      return a+b+c;
    }
  }
  
  
  
  /**  UI and debug methods-
    */
  public void describeBehaviour(Description d) {
  }
}















