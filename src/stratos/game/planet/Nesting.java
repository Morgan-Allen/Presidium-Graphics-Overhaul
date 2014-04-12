


package stratos.game.planet ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



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





