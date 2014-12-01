

package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Joining extends Plan {
  
  
  final Plan basis;
  final Actor joined;
  
  
  public Joining(Actor actor, Plan basis, Actor joined) {
    super(actor, basis.subject, basis.persistent());
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
  
  
  
  public static boolean checkInvitation(
    Actor actor, Actor asked, Dialogue origin, Behaviour invitation
  ) {
    if (! (invitation instanceof Plan)) return false;
    if (actor.mind.hasToDo(Joining.class)) return false;
    
    final boolean report = false;
    
    final Plan basis = (Plan) invitation;
    if (basis.hasMotiveType(Plan.MOTIVE_DUTY)) return false;
    
    final Plan copy = basis.copyFor(asked);
    if (copy == null) return false;
    
    final float motiveBonus = DialogueUtils.talkResult(
      SUASION, ROUTINE_DC, actor, asked
    ) * CASUAL;
    basis.setMotiveFrom(origin, 0);
    copy.setMotive(Plan.MOTIVE_LEISURE, motiveBonus);
    
    final Behaviour intended = asked.mind.nextBehaviour();
    if (Choice.wouldSwitch(asked, copy, intended, true, report)) return false;
    return true;
  }
  
  
  protected float getPriority() {
    if (! joined.isDoing(basis.getClass(), basis.subject())) return -1;
    setMotiveFrom(basis, 0);
    return basis.priorityFor(actor);
  }
  
  
  public float harmFactor()    { return basis.harmFactor()  ; }
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





