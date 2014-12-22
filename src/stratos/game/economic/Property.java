
package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;



//  TODO:  Reconsider the naming system here, and possibly merge one or two
//  stages in the class hierarchy?  Venue and Property might swap names, and
//  Structural can probably be merged with either Fixture or <building class>.

//  Element -> Fixture -> Structural -> Venue
//          \> Mobile -> Actor
//                    \> Vehicle
//
//  Venue and Vehicle are both Properties (citizens can live and work there,
//  they can be constructed or repaired, and they have stock inventories.)


public interface Property extends Structure.Basis, Inventory.Owner, Boarding {
  
  Behaviour jobFor(Actor actor);  //  TODO:  At least use the same signature...
  void addServices(Choice choice, Actor forActor);
  
  int numOpenings(Background b);
  Background[] careers();
  
  float homeCrowding(Actor actor);
  float visitCrowding(Actor actor);
  
  Traded[] services();  //  TODO:  Use Conversions instead.
  Staff staff();
  boolean isManned();
}