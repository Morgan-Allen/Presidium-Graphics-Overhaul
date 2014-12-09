

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;



//  TODO:  This should essentially work in a similar manner to an Audit, but
//  with more possibility of violence/intimidation (flat fees per day, rather
//  than a % of profits)?

//  Runners need to perform arrests here as well, though.  Maybe this shouldn't
//  be viewed as hostile?  Maybe runners/enforcers should be indifferent to
//  harmful actions by fellow gang/vocation members?  Not sure.


//  Well, either way, it should reset the 'tax done' count.  Just use the Audit
//  class?


public class Extortion extends Plan {
  
  
  /**  Constructors, data fields, and save/load/copy methods-
    */
  public Extortion(Actor actor, Target subject, boolean persistent,
      float harmFactor) {
    super(actor, subject, persistent, harmFactor);
  }
  
  
  public Extortion(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    return null;
  }
  
  
  protected float getPriority() {
    return 0;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
  }
}









