


package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Adapt this specifically for use by animals and vermin.

public class Nesting extends Resting {
  
  
  public Nesting(Fauna actor, Nest nest) {
    super(actor, nest);
  }
  
  
  public Nesting(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  
  
  /*
  protected float getPriority() {
    return ROUTINE;
  }
  
  
  protected Behaviour getNextStep() {
    return null;
  }
  
  
  public void describeBehaviour(Description d) {
  }
  //*/
}








