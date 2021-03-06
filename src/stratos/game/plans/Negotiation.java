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
import stratos.game.maps.PathSearch;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Include Gift-giving as an automatic sub-step?  (Pick up something
//  that the subject would value at the point of origin.)

public class Negotiation extends Dialogue {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = true ;
  
  Venue HQ;
  private Pledge offers, sought;
  private int numHails = 0;
  
  
  public Negotiation(Actor actor, Actor other) {
    super(actor, other, Dialogue.TYPE_CONTACT);
    addMotives(Plan.MOTIVE_JOB, 0);
  }
  
  
  public Negotiation(Session s) throws Exception {
    super(s);
    offers = (Pledge) s.loadObject();
    sought = (Pledge) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(offers);
    s.saveObject(sought);
  }
  
  
  public void setTerms(Pledge offers, Pledge sought) {
    if (offers == null) offers = Pledge.goodWillPledge(actor, other);
    if (sought == null) sought = Pledge.goodWillPledge(other, actor);
    this.offers = offers;
    this.sought = sought;
  }
  
  
  public Pledge termsOffered() {
    return offers;
  }
  
  
  public Pledge termsSought() {
    return sought;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    final Stage world = subject.world();
    if (world == null) return null;
    
    if (PathSearch.accessLocation(other, actor) == null) {
      if (numHails >= 3) return null;
      
      final Action hails = new Action(
        actor, other,
        this, "actionHail",
        Action.LOOK, "Waiting for "+subject
      );
      hails.setMoveTarget(Spacing.pickFreeTileAround(other.aboard(), actor));
      hails.setProperties(Action.RANGED | Action.NO_LOOP);
      return hails;
    }
    return super.getNextStep();
  }
  
  
  public boolean actionHail(Actor actor, Actor other) {
    super.actionHail(actor, other);
    numHails++;
    return true;
  }
  
  
  public boolean interrupt(String cause) {
    if (cause == Plan.INTERRUPT_LOSE_PATH && moveTarget() == other) {
      nextStep = null;
      actor.assignAction(null);
      return false;
    }
    else return super.interrupt(cause);
  }
  
  
  protected void discussTopic(Session.Saveable topic, boolean close) {
    final boolean report = (
      I.talkAbout == actor || I.talkAbout == other
    ) && stepsVerbose;
    if (report) {
      I.say("\nDiscussing: "+topic);
      I.say("  Offer accepted? "+offers.accepted());
      I.say("  Offer refused?  "+offers.refused ());
      I.say("  Time up?        "+close);
    }
    
    if (! close) { super.discussTopic(topic, close); return; }
    if (offers == null || sought == null) I.complain("MUST SET TERMS FIRST!");
    if (offers.accepted()) return;
    
    final boolean accepted = tryAcceptance(offers, sought, other, other);
    setAcceptance(offers, sought, actor, other, accepted);
  }
  
  
  public static float acceptChance(
    Pledge offers, Pledge sought, Actor talks, Actor with
  ) {
    return acceptTest(offers, sought, talks, with, true);
  }
  
  
  public static boolean tryAcceptance(
    Pledge offers, Pledge sought, Actor talks, Actor with
  ) {
    return acceptTest(offers, sought, talks, with, false) >= 0.5f;
  }
  
  
  private static float acceptTest(
    Pledge offers, Pledge sought, Actor actor, Actor other, boolean chanceOnly
  ) {
    //
    //  The likelihood of an offer being accepted depends on the degree of
    //  net benefit that the recipient expects to gain from the exchange, plus
    //  the current value of their relationship with the proposer.
    final float
      offersVal = offers == null ? 0 : offers.valueFor(other),
      soughtVal = sought == null ? 0 : sought.valueFor(other),
      cares     = other.relations.valueFor(actor) * Plan.PARAMOUNT,
      magnitude = Nums.abs(offersVal) + Nums.abs(soughtVal) + Nums.abs(cares),
      chance    = (offersVal + soughtVal + cares) / magnitude,
      opposeDC  = (1 - chance) * Qualities.STRENUOUS_DC;
    //
    //  If the outcome is in any way uncertain, we use a skill-check to try and
    //  persuade the recipient of the deal.
    if (chance >= 1) return 1;
    if (chance <= 0) return 0;
    if (chanceOnly) {
      return DialogueUtils.talkChance(SUASION, opposeDC, actor, other);
    }
    else {
      return DialogueUtils.talkResult(SUASION, opposeDC, actor, other);
    }
  }
  
  
  public static void setAcceptance(
    Pledge offers, Pledge sought, Actor actor, Actor other, boolean accepted
  ) {
    final boolean report = (
      I.talkAbout == actor || I.talkAbout == other
    ) && stepsVerbose;
    final float
      offersVal = offers.valueFor(other),
      soughtVal = sought.valueFor(other),
      cares     = other.relations.valueFor(actor) * Plan.PARAMOUNT,
      magnitude = Nums.abs(offersVal) + Nums.abs(soughtVal) + Nums.abs(cares);
    
    if (accepted) {
      if (report) I.say("  OFFER ACCEPTED");
      sought.setAcceptance(true, true);
      offers.setAcceptance(true, true);
      sought.performFulfillment(offers);
      offers.performFulfillment(sought);
      //
      //  If the offer is accepted, the recipient modifies their relation based
      //  on how much they benefit from the offer.  (A portion of this is
      //  shared with the proposer, in proportion to their existing relation.)
      final float
        impact = (offersVal + soughtVal) / Plan.PARAMOUNT,
        likes  = actor.relations.valueFor(other);
      other.relations.incRelation(actor, impact         / 2, 0.25f, 0);
      actor.relations.incRelation(other, impact * likes / 2, 0.25f, 0);
    }
    else {
      if (report) I.say("  OFFER REFUSED");
      sought.setAcceptance(false, true);
      offers.setAcceptance(false, true);
      //
      //  If the offer is refused, then relations for both parties are lowered,
      //  but more for the giver than the receiver.
      float impact = 0 - magnitude / (Plan.PARAMOUNT * 4);
      actor.relations.incRelation(other, impact    , 0.25f, 0);
      other.relations.incRelation(actor, impact / 2, 0.25f, 0);
    }
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (actor.isDoingAction("actionWait", other)) {
      d.appendAll("Waiting for ", other);
    }
    else if (topic() == this) {
      d.append("Making a proposal: ");
      d.append(offers.description());
      d.append(" for ");
      d.append(sought.description());
      d.append(" to ");
      d.append(other);
    }
    else super.describeBehaviour(d);
  }
}











