/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



public class Payday extends Plan implements Economy {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final Employer pays ;
  
  
  public Payday(Actor actor, Employer pays) {
    super(actor, pays) ;
    this.pays = pays ;
  }
  
  
  public Payday(Session s) throws Exception {
    super(s) ;
    this.pays = (Employer) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(pays) ;
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  protected float getPriority() {
    if (! (pays instanceof Venue)) return 0 ;
    
    final Venue venue = (Venue) pays ;
    final Profile p = venue.base().profiles.profileFor(actor) ;
    float impetus = (p.daysSinceWageEval(venue.world()) - 1) * ROUTINE ;
    if (impetus < 0) impetus = 0 ;
    impetus += Plan.greedLevel(actor, (int) p.paymentDue()) * ROUTINE;
    return Visit.clamp(impetus, 0, URGENT) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final Profile p = actor.base().profiles.profileFor(actor) ;
    if (p.daysSinceWageEval(actor.world()) < 1) return null ;
    
    //  TODO:  Get rid of this.  The audit office is going to serve a different
    //  function, anyway.
    /*
    if (pays instanceof AuditOffice) {
      final AuditOffice office = (AuditOffice) pays ;
      if (office.assessRelief(actor, false) <= 0) return null ;
    }
    else
    //*/
    if (p.paymentDue() <= 0) return null ;
    
    final Action getPaid = new Action(
      actor, pays,
      this, "actionGetPaid",
      Action.TALK_LONG, "Getting Paid"
    ) ;
    return getPaid ;
  }
  
  
  public boolean actionGetPaid(Actor actor, Venue venue) {
    final Profile p = venue.base().profiles.profileFor(actor) ;
    I.sayAbout(actor, "Getting paid at "+venue) ;
    //Audit.auditEmployer(actor, venue) ;
    
    if (p.paymentDue() == 0) {
      /*
      if (venue instanceof AuditOffice) {
        I.sayAbout(actor, "Dispensing relief...") ;
        ((AuditOffice) venue).assessRelief(actor, true) ;
      }
      else {
      }
      //*/
      final float balance = Audit.auditForBalance(actor, venue) ;
      I.sayAbout(actor, "Getting balance: "+balance) ;
      venue.base().incCredits(balance) ;
    }
    
    final float wages = p.paymentDue() ;
    I.sayAbout(actor, "Wages due "+wages) ;
    venue.stocks.incCredits(0 - wages) ;
    actor.gear.incCredits(wages) ;
    p.clearWages(venue.world()) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Collecting wages at ") ;
    d.append(pays) ;
  }
}









