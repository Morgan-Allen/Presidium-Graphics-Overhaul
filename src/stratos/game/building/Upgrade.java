

package stratos.game.building;
import stratos.game.actors.Backgrounds;
import stratos.util.*;



public class Upgrade extends Index.Member implements Backgrounds {
  
  
  //
  //  Upgrades tend to either expand employment, give a bonus to production
  //  of a particular item type, or enhance a particular kind of service.
  
  final public String name;
  final public String description;
  
  final public int buildCost;
  final public Object refers;
  final public int bonus;
  final public Upgrade required;
  
  
  public Upgrade(
    String name, String desc,
    int buildCost,
    Object refers, int bonus,
    Upgrade required, Index index
  ) {
    super(index);
    this.name = name;
    this.description = desc;
    this.buildCost = buildCost;
    this.refers = refers;
    this.bonus = bonus;
    this.required = required;
    /*
    if (name.endsWith("Station")) {
      I.say(name+" upgrade refers to: "+refers);
    }
    //*/
  }
  
  
  public String toString() {
    return name;
  }
}

