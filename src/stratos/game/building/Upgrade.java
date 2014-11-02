

package stratos.game.building;
import stratos.game.actors.Backgrounds;
import stratos.util.*;



public class Upgrade extends Index.Entry implements Backgrounds {
  
  
  //
  //  Upgrades tend to either expand employment, give a bonus to production
  //  of a particular item type, or enhance a particular kind of service.
  
  final public String name;
  final public String description;
  
  final public int buildCost;
  final public Object refers;
  final public int bonus;
  final public Upgrade required;
  
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








