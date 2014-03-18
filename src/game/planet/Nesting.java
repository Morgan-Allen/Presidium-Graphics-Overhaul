


package src.game.planet ;
import src.game.actors.* ;
import src.game.common.* ;
import src.user.* ;
import src.util.*;



public class Nesting extends Plan {
  
  
  public Nesting(Fauna actor, Nest nest) {
    super(actor, nest) ;
  }
  
  
  public Nesting(Session s) throws Exception {
    super(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  

  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    return null ;
  }
  
  
  public void describeBehaviour(Description d) {
  }
}





