/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.BaseFinance;
import stratos.game.politic.Profile;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;





//  Visit the dwelling in question.  Audit it (extend the class,) and lay some
//  smack down if the target doesn't behave.

//  But... how do you define 'not behaving'?  I think you need to have a notion
//  of 'tax evasion' before you can have a notion of correcting offences.

//  Assault.  Theft.  Desertion.  Corruption.

//  So, how do you evade tax?  ...By electing not to go and pay.  It is a sin
//  of ommission.  So it is the citizen's responsibility to pay their tax (or
//  obtain relief) at the nearest admin point (the bastion, enforcer bloc or
//  runner lodge.)


//  The auditor (analyst or runner) simply drops in to... remind the subject of
//  their obligations.  The muscle (enforcer or runner) will bring them in for
//  sentencing (or just dish out a beating.)

//  So... I don't actually need to create this class.  I need to refactor the
//  Payday and Auditing classes to more accurately reflect the duties of both
//  citizens and government.



public class Payday extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean verbose = false;
  
  
  final Property pays;
  
  
  public Payday(Actor actor, Property pays) {
    super(actor, pays, true, NO_HARM);
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
    
    final Profile p = pays.base().profiles.profileFor(actor);
    if (p.salary() == 0) return -1;
    
    final float payGap = p.daysSincePayAssess(pays.world());
    if (payGap < 2) {
      if (report) I.say("\nPay gap is: "+payGap+" days");
      return 0;
    }
    float modifier = Nums.clamp(payGap, 0, 2) - 1.5f;
    modifier = NO_MODIFIER + (modifier * ROUTINE);
    
    final float priority = priorityForActorWith(
      actor, pays, ROUTINE,
      modifier, NO_HARM,
      NO_COMPETITION, NO_FAIL_RISK,
      NO_SKILLS, NO_TRAITS, PARTIAL_DISTANCE_CHECK, report
    );
    if (report) {
      I.say("  Payment due: "+p.paymentDue()+", pay gap: "+payGap+" days");
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
    if (p.paymentDue() == 0) {
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

    final float wages = p.paymentDue();
    if (report) I.say("Wages due "+wages);
    actor.gear.incCredits(wages);
    p.clearWages(venue.world());
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Collecting wages at ");
    d.append(pays);
  }
}









