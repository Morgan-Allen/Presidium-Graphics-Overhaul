

package code.game.building ;
import code.util.*;



public class Upgrade extends Index.Member {
  
  
  //
  //  Upgrades tend to either expand employment, give a bonus to production
  //  of a particular item type, or enhance a particular kind of service.
  
  final String name ;
  final String description ;
  
  final int buildCost ;
  final Object refers ;
  final int bonus ;
  final Upgrade required ;
  
  
  public Upgrade(
    String name, String desc,
    int buildCost,
    Object refers, int bonus,
    Upgrade required, Index index
  ) {
    super(index) ;
    this.name = name ;
    this.description = desc ;
    this.buildCost = buildCost ;
    this.refers = refers ;
    this.bonus = bonus ;
    this.required = required ;
  }
  
  
  public String toString() {
    return name ;
  }
}