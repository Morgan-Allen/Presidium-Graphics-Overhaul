/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
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
  
  
  final public static Index <Type> TYPE_INDEX = new Index <Type> ();
  
  public abstract static class Type extends Index.Entry {
    
    final public String name;
    public Type(String name) { super(TYPE_INDEX, name); this.name = name; }
    public abstract Pledge[] variantsFor(Actor makes, Actor makesTo);
    
    //abstract void describe(Pledge p, Description d);
    abstract String description(Pledge p);
    abstract float valueOf(Pledge p, Actor a);
    abstract Behaviour fulfillment(Pledge p);
  }
  
  
  final public Type type;
  final float amount;
  final Session.Saveable refers;
  final Actor makes;
  
  

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
    this.type = TYPE_INDEX.loadFromEntry(s.input());
    this.amount = s.loadFloat ();
    this.refers = s.loadObject();
    this.makes  = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    TYPE_INDEX.saveEntry(type, s.output());
    s.saveFloat (amount);
    s.saveObject(refers);
    s.saveObject(makes );
  }
  
  
  public float valueFor(Actor actor) {
    return type.valueOf(this, actor);
  }
  
  
  public Behaviour fulfillment() {
    return type.fulfillment(this);
  }
  
  
  public Actor makesPledge() {
    return makes;
  }
  
  
  public String description() {
    return type.description(this);
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
  
  /*
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
  //*/
  
  
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
    
    
    Behaviour fulfillment(Pledge p) {
      //  TODO:  Should it be the actor making the pledge, or an actor you
      //  refer to?  Who is paying, exactly?
      
      if (p.makes == p.makes.base().ruler()) {
        final String source = BaseFinance.SOURCE_REWARDS;
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
    
    
    Behaviour fulfillment(Pledge p) {
      return (Delivery) p.refers;
    }
  };
  
  
  
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
    
    
    Behaviour fulfillment(Pledge p) {
      return null;
    }
  };
  
  
  final public static Type TYPE_JOIN_MISSION = new Type("Join Mission") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      final Series <Mission> all = makesTo.base().tactics.allMissions();
      final Pledge p[] = new Pledge[all.size()];
      int i = 0;
      for (Mission m : all) p[i++] = new Pledge(this, m, makes);
      return p;
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
    
    
    Behaviour fulfillment(Pledge p) {
      final Mission m = (Mission) p.refers;
      p.makes.mind.assignMission(m);
      m.setApprovalFor(p.makes, true);
      return null;
    }
  };
  
  
  
  
  
  
  
  
  /**  UI and interface methods-
    */
}









