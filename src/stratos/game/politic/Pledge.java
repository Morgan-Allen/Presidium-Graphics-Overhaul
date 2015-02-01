


package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;



/*
 *  Reward types should include (in practice this should be used in foreign
 *  negotiations as well)-
 *  
 *  Cash Payment or Goods
 *  Personnel or Captives
 *  Support for Legislation
 *  Joining a Mission (Strike/Recover/Secure, Recon/Intel, Diplomacy)
 *  
 *  Permission to Marry
 *  Promotion and Demotion
 *  Pardon or Arrest
 *  Truce, Allegiance or Fealty
 */
public class Pledge implements Session.Saveable {
  
  
  private static boolean
    evalVerbose = false;
  
  public static enum Type {
    
    PAYMENT,
    PROMOTION,
    GIFT_ITEM,
    SENSE_OF_DUTY,
    
    GOOD_WILL,
    JOIN_MISSION,
    SWEAR_FEALTY,
    RELEASE_CAPTIVE
  }
  
  
  final Type type;
  final float amount;
  final Session.Saveable refers;
  final Accountable madeTo;

  

  public Pledge(Type type, Accountable madeTo) {
    this(type, -1, null, madeTo);
  }
  
  
  public Pledge(Type type, float amount, Accountable madeTo) {
    this(type, amount, null, madeTo);
  }
  

  public Pledge(Type type, Session.Saveable refers, Accountable madeTo) {
    this(type, -1, refers, madeTo);
  }
  
  
  protected Pledge(
    Type type, float amount, Session.Saveable refers, Accountable madeTo
  ) {
    this.type = type;
    this.amount = amount;
    this.refers = refers;
    this.madeTo = madeTo;
  }
  
  
  public Pledge(Session s) throws Exception {
    s.cacheInstance(this);
    this.type = (Type) s.loadEnum(Type.values());
    this.amount = s.loadFloat ();
    this.refers = s.loadObject();
    this.madeTo = (Accountable) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveEnum  (type  );
    s.saveFloat (amount);
    s.saveObject(refers);
    s.saveObject((Session.Saveable) madeTo);
  }
  
  
  
  //  The value of a given pledge to a given actor.
  //    Cash- base on greed.
  //    Gift item- base on value to actor.
  //    Promotion- base on career rating/ambition.
  //    Sense of duty- loyalty/military post.
  //    Good will- ask nothing.
  //    Mission- rating of mission.
  //    Swear fealty- degree of defection (Priority 5, 10, or 15.)
  //    Release captive- affection for subject (0-20.)
  
  //  Following through on your end (sealing the deal.)
  //    Cash- deliver.
  //    Gift item- deliver.
  //    Promotion- deliver.
  //    Sense of duty- nothing.
  //    Good will- nothing.
  //    Mission- deliver.
  //    Swear fealty- deliver.
  //    Release captive- deliver.
  
  //  Find the set of things you can pledge.
  //    Cash- vary amount.
  //    Gift item- anything stored in Bastion.
  //    Promotion- top 3 spots open and fit for.
  //    Sense of duty- nothing.
  //    Good will- nothing.
  //    Mission- anything declared.
  //    Swear fealty- alliance, spying, or capitulation.
  //    Release captive- anyone imprisoned they care for.
  
  //  You'll need a new UI for this.
  
  //  How does this work with your own subjects?
  //  Pick the top 3 things that the subject might value most, and are in
  //  reasonable proportion to the magnitude of service.  Then pick one?
  
  
  
  
  
  /**  UI and interface methods-
    */
}












