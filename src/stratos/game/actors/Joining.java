

package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;



public class Joining extends Plan {
  
  
  final Plan basis;
  final Actor joined;
  
  
  public Joining(Actor actor, Plan basis, Actor joined) {
    super(actor, basis.subject);
    this.basis = (basis.actor == actor) ? basis : basis.copyFor(actor);
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
    return new Joining(other, basis.copyFor(other), joined);
  }
  
  
  public boolean valid() {
    if (! basis.valid()) return false;
    if (joined == null || ! joined.inWorld()) return false;
    return true;
  }
  
  
  
  protected float getPriority() {
    this.setMotiveFrom(basis, 0);
    return basis.getPriority();
  }
  
  
  public float harmFactor()    { return basis.harmFactor()   ; }
  public float competeFactor() { return basis.competeFactor(); }
  
  
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





