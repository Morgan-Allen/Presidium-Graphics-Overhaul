

package stratos.game.economic;
import stratos.game.actors.Backgrounds;
import stratos.util.*;



//  TODO:  Allow upgrades to define how many levels they allow, and their
//  precise type (level-upgrade, tech-upgrade, or policy-upgrade.)


public class Upgrade extends Index.Entry implements Backgrounds {
  
  
  //
  //  Upgrades tend to either expand employment, give a bonus to production
  //  of a particular item type, or enhance a particular kind of service.
  
  private static Table <Class, Batch <Upgrade>> byVenue = new Table();
  
  
  public static enum Type {
    VENUE_LEVEL,
    TECH_MODULE,
    SOC_POLICY
  };
  
  final public static int
    SINGLE_LEVEL = 1,
    TWO_LEVELS   = 2,
    THREE_LEVELS = 3;
  
  //  TODO:  Implement this as soon as possible.
  /*
  
  final Type type;
  final int maxLevel;
  final Upgrade required[];
  
  final String names[], description;
  
  //*/
  
  

  //*
  final public String name;
  final public String description;
  
  String levelNames[];
  
  final public Type type;
  final public int buildCost;
  final public Upgrade required[];
  final public int maxLevel;
  
  //  TODO:  ...either get rid of these, or make them more flexible.
  final public Object refers;
  final public int bonus;
  
  
  
  
  public Upgrade(
    String name, String desc,
    int buildCost, int maxLevel,
    Object refers, int bonus,
    Upgrade required, Class origin, Index index
  ) {
    super(index, name);
    this.name        = name;
    this.description = desc;
    this.type        = Type.TECH_MODULE;
    this.buildCost   = buildCost;
    this.refers      = refers;
    this.bonus       = bonus;
    this.required    = required == null ?
      new Upgrade[0] : new Upgrade[] {required}
    ;
    this.maxLevel    = maxLevel;
    
    Batch <Upgrade> VU = byVenue.get(origin);
    if (VU == null) byVenue.put(origin, VU = new Batch());
    VU.add(this);
  }
  
  
  public static Upgrade[] upgradesFor(Class venueType) {
    final Batch <Upgrade> VU = byVenue.get(venueType);
    return VU == null ? new Upgrade[0] : VU.toArray(Upgrade.class);
  }
  
  
  public String toString() {
    return name;// names[0];
  }
}








