/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



//  TODO:  Adapt this to collecting any promised special rewards from missions,
//  or from negotiations after contact.



public class Payday extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean verbose = false;
  
  
  final Property pays;
  
  
  public Payday(Actor actor, Property pays) {
    super(actor, pays, MOTIVE_JOB, NO_HARM);
    this.pays = pays;
  }
  
  
  public Payday(Session s) throws Exception {
    super(s);
    this.pays = (Property) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(pays);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    
    final Property work = actor.mind.work();
    if (work == null || work.staff().shiftFor(actor) == Venue.OFF_DUTY) {
      if (report) I.say("\nCannot collect pay- no employer or off-duty.");
      return -1;
    }
    
    final Profile p = pays.base().profiles.profileFor(actor);
    if (p.salary() == 0) return -1;
    
    final float wagesDue = p.wagesDue();
    final float payGap = p.daysSinceWageAssessed();
    if (payGap < 2 && wagesDue == 0) {
      if (report) I.say("\nPay gap is: "+payGap+" days, no payment available");
      return -1;
    }
    float impetus = actor.motives.greedPriority(wagesDue);
    float modifier = (Nums.clamp(payGap, 0, 2) - 1) * CASUAL / 2f;
    modifier = NO_MODIFIER + (modifier * ROUTINE);
    
    final float priority = impetus + modifier;
    if (report) {
      I.say("  Payment due: "+wagesDue+", pay gap: "+payGap+" days");
      I.say("  Greed impetus:  "+impetus );
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    
    //
    //  If you haven't been paid for ages, take matters into your own hands...
    //  TODO:  Consider making this an automatic crime?  Weight the priority,
    //  anyway...
    final Profile p = actor.base().profiles.profileFor(actor);
    if (p.wagesDue() == 0) {
      if (report) I.say("  Doing amateur audit.");
      return Audit.nextAmateurAudit(actor);
    }
    
    //
    //  Otherwise, proceed as standard.
    if (report) I.say("  Collecting pay.");
    final Action getPaid = new Action(
      actor, pays,
      this, "actionGetPaid",
      Action.TALK_LONG, "Getting Paid"
    );
    return getPaid;
  }
  
  
  public boolean actionGetPaid(Actor actor, Property venue) {
    final boolean report = verbose && I.talkAbout == actor;
    final Profile p = venue.base().profiles.profileFor(actor);
    if (report) I.say("Getting paid at "+venue);
    
    final float wages = p.wagesDue();
    if (report) I.say("Wages due "+wages);
    actor.gear.incCredits(wages);
    p.clearWages();
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Collecting wages at ");
    d.append(pays);
  }
}









