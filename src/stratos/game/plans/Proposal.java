

package stratos.game.plans;
import stratos.game.actors.Behaviour;
import stratos.game.actors.Qualities;
import stratos.game.common.Actor;
import stratos.game.common.Session;
import stratos.game.politic.Pledge;




//  No, not marriage.  In the sense of a 'business proposal'.  You suggest
//  something that the other actor might wish to do.

public class Proposal extends Dialogue {
  
  
  Pledge given;
  Pledge asked;
  
  
  private Proposal(Actor actor, Actor other, int type) {
    super(actor, other, type);
  }
  
  
  public Proposal(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  protected Session.Saveable selectTopic() {
    return this;
  }
  
  
  protected void discussTopic(Session.Saveable topic) {
    //  TODO:  Implement this as a subclass of Dialogue!
    
    //if (topic != this) { super.discussTopic(topic); return; }
    
    float opposeDC = Qualities.MODERATE_DC;
    opposeDC -= given.valueFor(other);
    opposeDC -= asked.valueFor(other);
    float result = DialogueUtils.talkResult(SUASION, opposeDC, actor, other);
    
    if (result > 0.5f) {
      final Actor     GA = given.makesPledge(), AA = asked.makesPledge();
      final Behaviour GB = given.fulfillment(), AB = asked.fulfillment();
      if (GB != null) GA.mind.assignBehaviour(GB);
      if (AB != null) AA.mind.assignBehaviour(AB);
    }
  }
  
  
  
  
}











