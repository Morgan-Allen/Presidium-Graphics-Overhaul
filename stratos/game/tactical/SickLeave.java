


package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;


//  TODO:  This should be moved to the stratos.game.base package.
public class SickLeave extends Plan {
  
  
  private static boolean verbose = false ;
  
  final Sickbay sickbay ;
  private Treatment needed ;
  
  
  
  public SickLeave(Actor actor, Sickbay sickbay) {
    super(actor) ;
    this.sickbay = sickbay ;
    needed = new Treatment(null, actor, sickbay) ;
  }
  
  
  public SickLeave(Session s) throws Exception {
    super(s) ;
    sickbay = (Sickbay) s.loadObject() ;
    needed = (Treatment) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(sickbay) ;
    s.saveObject(needed) ;
  }
  
  
  public float priorityFor(Actor actor) {
    if (sickbay == null || needed == null) return 0 ;
    if (needed.baseUrgency() == 0) return 0 ;
    
    final Item treatResult = needed.treatResult() ;
    if (treatResult != null) {
      if (hasBegun()) { if (treatResult.amount >= 1) return 0 ; }
      else { if (treatResult.amount > 0) return 0 ; }
    }
    
    final float crowding = hasBegun() ? 0 :
      Plan.competition(SickLeave.class, sickbay, actor) ;
    //
    //  Modify for Psych Eval, since it's only needed in cases of severe bad
    //  morale or for key personnel.
    float impetus = needed.baseUrgency() - crowding ;
    if (needed.type == Treatment.TYPE_PSYCH_EVAL) {
      final Background v = actor.vocation() ;
      if (v.guild == Background.GUILD_MILITANT) return impetus ;
      if (v.standing >= Background.UPPER_CLASS) return impetus ;
      impetus *= (0.5f - actor.health.moraleLevel()) ;
    }
    
    if (verbose) I.sayAbout(actor, "Sick leave impetus: "+impetus) ;
    return Visit.clamp(impetus, 0, CRITICAL) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (priorityFor(actor) <= 0) return null ;
    final Action leave = new Action(
      actor, sickbay,
      this, "actionLeave",
      Action.FALL, "Taking Sick Leave"
    ) ;
    return leave ;
  }
  
  
  public boolean actionLeave(Actor actor, Sickbay sickbay) {
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    if (needed == null) {
      d.append("Seeking Treatment at ") ;
      d.append(sickbay) ;
    }
    else needed.descForPatient(d) ;
  }
}
//*/













