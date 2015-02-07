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



public class Proposal extends Dialogue {
  
  
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
    this.offers = offers;
    this.sought = sought;
  }
  
  
  
  
  
  protected Session.Saveable selectTopic() {
    return this;
  }
  
  
  //  TODO:  Save this for the close of the conversation- and pass that data
  //  along to the method.  ...But get it working first.
  
  protected void discussTopic(Session.Saveable topic) {
    if (topic != this) { super.discussTopic(topic); return; }
    
    float opposeDC = Qualities.MODERATE_DC;
    if (offers != null) opposeDC -= offers.valueFor(other);
    if (sought != null) opposeDC -= sought.valueFor(other);
    float result = DialogueUtils.talkResult(SUASION, opposeDC, actor, other);
    
    //  TODO:  MODIFY RELATIONS TOO!
    if (result > 0.5f) {
      final Actor     GA = offers.makesPledge(), AA = sought.makesPledge();
      final Behaviour GB = offers.fulfillment(), AB = sought.fulfillment();
      if (GB != null) GA.mind.assignBehaviour(GB);
      if (AB != null) AA.mind.assignBehaviour(AB);
    }
    else {
      
    }
  }
  
}









