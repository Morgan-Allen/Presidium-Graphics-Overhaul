

package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;


public interface Employment extends Installation, Boardable {
  
  
  Behaviour jobFor(Actor actor) ;
  void addServices(Choice choice, Actor forActor) ;
  
  int numOpenings(Background b) ;
  Background[] careers() ;
  
  Base base() ;
  Personnel personnel() ;
}