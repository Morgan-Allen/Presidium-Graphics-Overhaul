

package src.game.civilian ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;


public interface Employment extends Installation, Boardable {
  
  
  Behaviour jobFor(Actor actor) ;
  void addServices(Choice choice, Actor forActor) ;
  
  int numOpenings(Background b) ;
  Background[] careers() ;
  
  Base base() ;
  Personnel personnel() ;
}