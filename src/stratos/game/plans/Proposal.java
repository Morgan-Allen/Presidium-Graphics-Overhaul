/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.util.*;



//  TODO:  Allow proposals by proxy on behalf of someone else (e.g, a base's
//         ruler.)

public class Proposal extends Dialogue {
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = true ;
  
  private Pledge offers, sought;
  
  
  public Proposal(Actor actor, Actor other) {
    super(actor, other, Dialogue.TYPE_CONTACT);
  }
  
  
  public Proposal(Session s) throws Exception {
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
  
  
  
  
  
  protected Session.Saveable selectTopic(boolean close) {
    if (close) return this;
    else return super.selectTopic(false);
  }
  
  //  TODO:  Also, make sure the other actor stands still long enough to
  //         receive the effects of the pledge...
  
  protected void discussTopic(Session.Saveable topic, boolean close) {
    if (topic != this) { super.discussTopic(topic, close); return; }
    if (offers == null || sought == null) I.complain("MUST SET TERMS FIRST!");
    //
    //  The likelihood of an offer being accepted depends on the degree of
    //  net benefit that the recipient expects to gain from the exchange, plus
    //  the current value of their relationship with the proposer.
    final boolean report = (
      I.talkAbout == actor || I.talkAbout == other
    ) && stepsVerbose;
    final float
      offersVal = offers.valueFor(other),
      soughtVal = sought.valueFor(other),
      cares     = other.relations.valueFor(actor) * Plan.PARAMOUNT,
      magnitude = Nums.abs(offersVal) + Nums.abs(soughtVal) + Nums.abs(cares),
      chance    = (offersVal + soughtVal + cares) / magnitude;
    //
    //  If the outcome is in any way uncertain, we use a skill-check to try and
    //  persuade the recipient of the deal.
    float talkResult = -1, opposeDC = -1;
    if      (chance >= 1) talkResult =  1;
    else if (chance <= 0) talkResult =  0;
    else {
      opposeDC = (1 - chance) * Qualities.STRENUOUS_DC;
      talkResult = DialogueUtils.talkResult(SUASION, opposeDC, actor, other);
    }
    if (report) {
      I.say("\nDiscussing proposal with "+other);
      final float skill = actor.traits.usedLevel(SUASION);
      I.say("  Offers value:  "+offersVal+" ("+offers+")");
      I.say("  Sought value:  "+soughtVal+" ("+sought+")");
      I.say("  Sway chance:   "+chance+" (Suasion "+skill+")");
      I.say("  Talk result:   "+talkResult+" (DC "+opposeDC+")");
    }
    
    if (talkResult >= 0.5f) {
      final Actor     GA = offers.makesPledge(), AA = sought.makesPledge();
      final Behaviour GB = offers.fulfillment(), AB = sought.fulfillment();
      if (GB != null) GA.mind.assignBehaviour(GB);
      if (AB != null) AA.mind.assignBehaviour(AB);
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
    if (topic() == this) {
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











