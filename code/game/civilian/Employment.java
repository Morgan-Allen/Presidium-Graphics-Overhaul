

package code.game.civilian ;
import code.game.actors.*;
import code.game.building.*;
import code.game.common.*;


public interface Employment extends Installation, Boardable {
  
  
  Behaviour jobFor(Actor actor) ;
  void addServices(Choice choice, Actor forActor) ;
  
  int numOpenings(Background b) ;
  Background[] careers() ;
  
  Base base() ;
  Personnel personnel() ;
}