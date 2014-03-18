


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;


//
//  TODO:  Allow customised descriptions and basic training as standard.
//  TODO:  Allow actors to perform minor dialogue without distraction.
//  TODO:  HAVE THIS PLAN RESPONSIBLE FOR AUDITING IF NOBODY ELSE DOES IT FIRST


public class Supervision extends Plan implements Economy {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final float WAIT_TIME = 20f ;
  
  final Venue venue ;
  private float beginTime = -1 ;
  
  
  public Supervision(Actor actor, Venue supervised) {
    super(actor, supervised) ;
    this.venue = supervised ;
  }
  
  
  public Supervision(Session s) throws Exception {
    super(s) ;
    this.venue = (Venue) s.loadObject() ;
    this.beginTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(venue) ;
    s.saveFloat(beginTime) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    if (Plan.competition(Supervision.class, venue, actor) > 0) return 0 ;
    if (! actor.mind.work().personnel().onShift(actor)) return 0 ;
    return CASUAL + priorityMod ;
  }
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (beginTime == -1) beginTime = actor.world().currentTime() ;
    final float elapsed = actor.world().currentTime() - beginTime ;
    if (elapsed > WAIT_TIME / 2) {
      final Behaviour nextJob = venue.jobFor(actor) ;
      if (elapsed > WAIT_TIME || ! (nextJob instanceof Supervision)) {
        abortBehaviour() ;
        return null ;
      }
    }
    
    final Action supervise = new Action(
      actor, venue,
      this, "actionSupervise",
      Action.LOOK, "Supervising"
    ) ;
    return supervise ;
  }
  
  
  public boolean actionSupervise(Actor actor, Venue venue) {
    
    //
    //  If you have any items demanded by the venue, put them away-
    for (Item i : actor.gear.allItems()) {
      if (i.refers != null || i.type.form != FORM_COMMODITY) continue ;
      if (venue.stocks.demandFor(i.type) > 0) actor.gear.transfer(i, venue) ;
    }
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Supervising ") ;
    d.append(venue) ;
  }
}













