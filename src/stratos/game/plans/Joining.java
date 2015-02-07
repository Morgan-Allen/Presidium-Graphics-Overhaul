

package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  See if this can be adapted for use during Missions?  Certainly
//  useful for pack-hunters, like the Lictovore.


public class Joining extends Plan {
  
  
  final Plan basis;
  final Actor joined;
  
  
  public Joining(Actor actor, Plan basis, Actor joined) {
    super(actor, basis.subject, basis.motiveType(), MILD_HELP);
    this.basis = (basis.actor() == actor) ? basis : basis.copyFor(actor);
    this.joined = joined;
  }
  
  
  public Joining(Session s) throws Exception {
    super(s);
    basis = (Plan) s.loadObject();
    joined = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(basis);
    s.saveObject(joined);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public boolean valid() {
    if (! basis.valid()) return false;
    if (joined == null || ! joined.inWorld()) return false;
    return true;
  }
  
  
  protected float getPriority() {
    if (! joined.isDoing(basis.getClass(), basis.subject())) return -1;
    setMotiveFrom(basis, 0);
    return basis.priorityFor(actor);
  }
  
  
  public float harmFactor() {
    return basis.harmFactor();
  }
  
  
  public float competence() {
    return basis.competence();
  }
  
  
  protected Behaviour getNextStep() {
    if (basis.finished()) return null;
    return basis;
  }
  
  
  public void describeBehaviour(Description d) {
    basis.describeBehaviour(d);
    d.append(" with ");
    d.append(joined);
  }
}








