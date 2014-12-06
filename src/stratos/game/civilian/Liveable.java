

package stratos.game.civilian;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;



//  TODO:  Merge this with Installation?

public interface Liveable extends Installation, Inventory.Owner, Boarding {
  
  Behaviour jobFor(Actor actor);  //  TODO:  At least use the same signature...
  void addServices(Choice choice, Actor forActor);
  
  int numOpenings(Background b);
  Background[] careers();
  
  float homeCrowding(Actor actor);
  float visitCrowding(Actor actor);
  
  Traded[] services();  //  TODO:  Use Conversions instead.
  Personnel personnel();
}