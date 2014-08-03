

package stratos.game.civilian;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;


public interface Employer extends Installation, Inventory.Owner, Boarding {
  
  
  Behaviour jobFor(Actor actor);  //  TODO:  At least use the same signature...
  void addServices(Choice choice, Actor forActor);
  
  int numOpenings(Background b);
  Background[] careers();
  
  float homeCrowding(Actor actor);
  float visitCrowding(Actor actor);
  
  //  TODO:  This is the right thing to do.  USE THIS INSTEAD
  /*
  public static enum Use { WORK, HOME, VISIT }
  
  void addBehavioursFor(Actor actor, Choice choice, Use use);
  int openingsFor(Actor a, Use use);
  //*/
  
  TradeType[] services();
  Base base();
  Personnel personnel();
}