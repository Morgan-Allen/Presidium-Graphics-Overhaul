/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
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


public interface Property extends Structure.Basis, Owner, Boarding {
  
  void addTasks(Choice choice, Actor forActor, Background background);
  float crowdRating(Actor forActor, Background background);
  Background[] careers();
  Traded[] services();  //  TODO:  Use Conversions instead...
  
  Staff staff();
  boolean openFor(Actor actor);
  Boarding mainEntrance();
}


