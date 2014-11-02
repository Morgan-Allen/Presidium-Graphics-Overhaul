

package stratos.game.building;
import stratos.game.actors.Backgrounds;
import stratos.util.*;



//  TODO:  Allow upgrades to define how many levels they allow, and their
//  precise type (level-upgrade, tech-upgrade, or policy-upgrade.)


public class Upgrade extends Index.Entry implements Backgrounds {
  
  
  //
  //  Upgrades tend to either expand employment, give a bonus to production
  //  of a particular item type, or enhance a particular kind of service.
  
  public static enum Type { VENUE_LEVEL, TECH_MODULE, POLICY };
  
  final public String name;
  final public String description;
  String levelNames[];
  
  Type type;
  final public int buildCost;
  final public Upgrade required;
  int numLevels;
  
  //  TODO:  ...either get rid of these, or make them more flexible.
  final public Object refers;
  final public int bonus;
  
  
  private static Table <Class, Batch <Upgrade>> byVenue = new Table();
  
  
  public Upgrade(
    String name, String desc,
    int buildCost,
    Object refers, int bonus,
    Upgrade required,
    Class origin, Index index
  ) {
    super(index, name);
    this.name = name;
    this.description = desc;
    this.buildCost = buildCost;
    this.refers = refers;
    this.bonus = bonus;
    this.required = required;
    
    Batch <Upgrade> VU = byVenue.get(origin);
    if (VU == null) byVenue.put(origin, VU = new Batch());
    VU.add(this);
  }
  
  
  public static Upgrade[] upgradesFor(Class venueType) {
    final Batch <Upgrade> VU = byVenue.get(venueType);
    return VU == null ? new Upgrade[0] : VU.toArray(Upgrade.class);
  }
  
  
  public String toString() {
    return name;
  }
}








