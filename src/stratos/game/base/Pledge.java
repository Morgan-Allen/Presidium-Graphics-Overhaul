/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
import stratos.util.*;





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
/*
public static enum Type {
  
  PAYMENT,
  PROMOTION,
  GIFT_ITEM,
  SENSE_OF_DUTY,
  
  GOOD_WILL,
  JOIN_MISSION,
  SWEAR_FEALTY,
  RELEASE_CAPTIVE,
  
  GRANT_AUDIENCE,
  SUPPORT_RESEARCH
  HAND_IN_MARRIAGE
  BECOME_SPY
}
//*/



//  TODO:  Make this into a persistent plan, so that if the pledge cannot be
//  fulfilled immediately, the actor will attempt it later when possible.


public class Pledge implements Session.Saveable {
  
  
  private static boolean
    evalVerbose = false;
  
  final static int
    ACCEPT_INIT  = -1,
    ACCEPT_FALSE =  0,
    ACCEPT_TRUE  =  1,
    ACCEPT_LIES  =  2;
  
  
  final public static Index <Type> TYPE_INDEX = new Index <Type> ();
  
  final public Type type;
  final float amount;
  final Session.Saveable refers;
  final Actor makes;
  //private boolean deceive = false;//, accepted = false;
  private int acceptState = ACCEPT_INIT;
  
  

  public Pledge(Type type, Actor makes) {
    this(type, -1, null, makes);
  }
  
  
  public Pledge(Type type, float amount, Actor makes) {
    this(type, amount, null, makes);
  }
  

  public Pledge(Type type, Session.Saveable refers, Actor makes) {
    this(type, -1, refers, makes);
  }
  
  
  protected Pledge(
    Type type, float amount, Session.Saveable refers, Actor makes
  ) {
    this.type   = type  ;
    this.amount = amount;
    this.refers = refers;
    this.makes  = makes ;
  }
  
  
  public Pledge(Session s) throws Exception {
    s.cacheInstance(this);
    this.type        = TYPE_INDEX.loadEntry(s.input());
    this.amount      = s.loadFloat ();
    this.refers      = s.loadObject();
    this.makes       = (Actor) s.loadObject();
    this.acceptState = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    TYPE_INDEX.saveEntry(type, s.output());
    s.saveFloat (amount     );
    s.saveObject(refers     );
    s.saveObject(makes      );
    s.saveInt   (acceptState);
  }
  
  
  public float amount() {
    return amount;
  }
  
  
  public Session.Saveable refers() {
    return refers;
  }
  
  
  public float valueFor(Actor actor) {
    return type.valueOf(this, actor);
  }
  
  
  public Behaviour fulfillment(Pledge reward) {
    return type.fulfillment(this, reward);
  }
  
  
  public Actor makesPledge() {
    return makes;
  }
  
  
  public boolean accepted() {
    return acceptState == ACCEPT_TRUE;
  }
  
  
  public boolean refused() {
    return acceptState == ACCEPT_FALSE;
  }
  
  
  public String description() {
    return type.description(this);
  }
  
  
  public String toString() {
    return description();
  }
  
  
  public void setAcceptance(boolean accepted, boolean sincere) {
    if (! accepted) this.acceptState = ACCEPT_FALSE;
    else this.acceptState = sincere ? ACCEPT_TRUE : ACCEPT_LIES;
  }
  
  
  
  
  /**  Definitions for the various specific pledge types follow.
    */
  //  TODO:  Consider giving types an optional 'preparation' behaviour as well-
  //  e.g, buying/collecting a gift prior to handing off.  Or a 'stage'
  //  variable?
  
  
  public abstract static class Type extends Index.Entry {
    
    final public String name;
    public Type(String name) { super(TYPE_INDEX, name); this.name = name; }
    public abstract Pledge[] variantsFor(Actor makes, Actor makesTo);
    
    abstract String description(Pledge p);
    abstract float valueOf(Pledge p, Actor a);
    abstract Behaviour fulfillment(Pledge p, Pledge reward);
  }
  
  
  
  final public static Type TYPE_PAYMENT = new Type("Payment") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      return new Pledge[] {
          new Pledge(this, 50  , makesTo, makes),
          new Pledge(this, 100 , makesTo, makes),
          new Pledge(this, 250 , makesTo, makes),
          new Pledge(this, 500 , makesTo, makes),
          new Pledge(this, 1000, makesTo, makes),
      };
    }
    
    
    String description(Pledge p) {
      return ((int) p.amount)+" Credits Payment";
    }
    
    
    float valueOf(Pledge p, Actor a) {
      if (a != p.refers) return 0;
      return a.motives.greedPriority(p.amount);
    }
    
    
    Behaviour fulfillment(Pledge p, Pledge reward) {
      if (p.makes == p.makes.base().ruler()) {
        final BaseFinance.Source source = BaseFinance.SOURCE_REWARDS;
        p.makes.base().finance.incCredits(0 - p.amount, source);
      }
      else {
        p.makes.gear.incCredits(0 - p.amount);
      }
      
      final Actor paid = (Actor) p.refers;
      paid.gear.incCredits(p.amount);
      return null;
    }
  };
  
  public static Pledge paymentPledge(float amount, Actor from, Actor to) {
    return new Pledge(TYPE_PAYMENT, amount, to, from);
  }
  
  
  
  final public static Type TYPE_GIFT_ITEM = new Type("Gift Item") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      final Property from = makes.mind.home();
      if (from == null) return new Pledge[0];
      
      final Batch <Pledge> pledges = new Batch <Pledge> ();
      for (Item i : from.inventory().allItems()) {
        float amount = Nums.min(i.amount, 10);
        //  TODO:  The Gifting behaviour probably has this covered already.
        //         Try to use that.
        Delivery d = new Delivery(Item.withAmount(i, amount), from, makesTo);
        pledges.add(new Pledge(this, d, makes));
      }
      return pledges.toArray(Pledge.class);
    }
    
    
    String description(Pledge p) {
      final Item gift = ((Delivery) p.refers).allDelivered()[0];
      return "Gift of "+gift;
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final Item gift = ((Delivery) p.refers).allDelivered()[0];
      return a.motives.rateDesire(gift, null, a);
    }
    
    
    Behaviour fulfillment(Pledge p, Pledge reward) {
      //  TODO:  If the subjects are adjacent, just hand over the gift!
      
      
      return (Delivery) p.refers;
    }
  };
  
  public static Pledge giftPledge(
    Item item, Owner depot, Actor from, Actor to
  ) {
    final Delivery d = new Delivery(item, depot, to);
    return new Pledge(TYPE_GIFT_ITEM, d, from);
  }
  
  
  
  final public static Type TYPE_GOOD_WILL = new Type("Good Will") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      return new Pledge[] { new Pledge(this, makesTo, makes) };
    }
    
    
    public String description(Pledge p) {
      return "Good Will";
    }
    
    
    float valueOf(Pledge p, Actor a) {
      return 0;
    }
    
    
    Behaviour fulfillment(Pledge p, Pledge reward) {
      return null;
    }
  };
  
  public static Pledge goodWillPledge(Actor from, Actor to) {
    return new Pledge(TYPE_GOOD_WILL, from, to);
  }
  
  
  
  final public static Type TYPE_JOIN_MISSION = new Type("Join Mission") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      final Series <Mission> all = makesTo.base().tactics.allMissions();
      final Batch <Pledge> p = new Batch <Pledge> ();
      for (Mission m : all) {
        if (m.subject == makes || m.isApproved(makes)) continue;
        p.add(new Pledge(this, m, makes));
      }
      return p.toArray(Pledge.class);
    }
    
    
    public String description(Pledge p) {
      final Mission m = (Mission) p.refers;
      return m.toString();
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final Mission m = (Mission) p.refers;
      final Behaviour step = m.nextStepFor(a, true);
      return step == null ? 0 : step.priorityFor(a);
    }
    
    
    Behaviour fulfillment(Pledge p, Pledge reward) {
      final Mission m = (Mission) p.refers;
      p.makes.mind.assignMission(m);
      m.setApprovalFor(p.makes, true);
      m.setSpecialRewardFor(p.makes, reward);
      return null;
    }
  };
  
  public static Pledge missionPledge(Mission m, Actor from) {
    return new Pledge(TYPE_JOIN_MISSION, m, from);
  }
  
  
  
  final public static Type TYPE_AUDIENCE = new Type("Audience") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      return new Pledge[] { new Pledge(this, makesTo, makes) };
    }
    
    
    String description(Pledge p) {
      return "Audience with "+p.refers;
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final Actor host = (Actor) p.refers;
      return a.relations.valueFor(host) * Plan.ROUTINE;
    }
    
    
    Behaviour fulfillment(Pledge p, Pledge reward) {
      return Summons.officialSummons(p.makes, (Actor) p.refers);
    }
  };
  
  public static Pledge audiencePledge(Actor from, Actor to) {
    return new Pledge(TYPE_AUDIENCE, to, from);
  }
  
  
  
  //  TODO:  FIND A MORE EFFICIENT WAY TO HANDLE THIS!
  /*
  final public static Type TYPE_PROMOTION = new Type("Promotion") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      final Batch <Pledge> p = new Batch <Pledge> ();
      final BaseCommerce BC = makes.base().commerce;
      
      for (Background b : BC.jobDemand.keys()) {
        
        float shortage = BC.jobDemand.valueFor(b) - BC.jobSupply.valueFor(b);
        if (shortage <= 0) continue;
        
        //  TODO:  See if there's a more efficient way to handle this.
        final FindWork applies = FindWork.attemptFor(makesTo, b, makes.base());
        if (applies == null) continue;
        
        p.add(new Pledge(this, applies, makes));
      }
      return p.toArray(Pledge.class);
    }
    
    
    String description(Pledge p) {
      final FindWork work = (FindWork) p.refers;
      return "Work as "+work.position();
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final FindWork work = (FindWork) p.refers;
      return work.priorityFor(a);
    }
    
    
    Behaviour fulfillment(Pledge p, Pledge reward) {
      final FindWork work = (FindWork) p.refers;
      work.employer().staff().confirmApplication(work);
      return null;
    }
  };
  
  public static Pledge promotionPledge(FindWork work, Actor from) {
    return new Pledge(TYPE_PROMOTION, work, from);
  }
  //*/
  
  
  
}









