/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
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

//  TODO:  Move the various types out to a separate class or classes.



public class Pledge implements Session.Saveable {
  
  
  private static boolean
    evalVerbose = false;
  
  final static int
    ACCEPT_INIT  = -1,
    ACCEPT_FALSE =  0,
    ACCEPT_TRUE  =  1,
    ACCEPT_LIES  =  2;
  final static Pledge
    NO_PLEDGE[] = new Pledge[0];
  
  static boolean isRuler(Actor actor) {
    if (actor.base() == null) return false;
    return actor == actor.base().ruler();
  }
  
  
  final public static Index <Type> TYPE_INDEX = new Index <Type> ();
  
  final public Type type;
  final float amount;
  final Session.Saveable refers;
  final Actor makes;
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
  
  
  public boolean isDeceit() {
    return false;
  }
  
  
  public Session.Saveable refers() {
    return refers;
  }
  
  
  public float valueFor(Actor actor) {
    return type.valueOf(this, actor);
  }
  
  
  public void performFullfillment() {
    type.performFulfillment(this, null);
  }
  
  
  public void performFulfillment(Pledge reward) {
    type.performFulfillment(this, reward);
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
  public abstract static class Type extends Index.Entry {
    
    final public String name;
    
    public Type(String name) {
      super(TYPE_INDEX, name);
      this.name = name;
    }
    
    public boolean canMakePledge(Actor makes, Actor makesTo) {
      return makes.inWorld() && makesTo.inWorld();
    }
    
    public abstract Pledge[] variantsFor(Actor makes, Actor makesTo);
    
    abstract float valueOf(Pledge p, Actor a);
    abstract void performFulfillment(Pledge p, Pledge reward);
    
    abstract String description(Pledge p);
  }
  
  
  
  final public static Type TYPE_PAYMENT = new Type("Payment") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      final Batch <Pledge> vars = new Batch();
      final int amounts[] = { 50, 100, 250, 500, 1000 };
      final boolean ruler = isRuler(makes);
      for (int amount : amounts) {
        if (ruler && amount > makes.base().finance.credits()) break;
        if ((! ruler) && amount > makes.gear.allCredits()   ) break;
        vars.add(new Pledge(this, amount, makesTo, makes));
      }
      return vars.toArray(Pledge.class);
    }
    
    
    String description(Pledge p) {
      return ((int) p.amount)+" Credits Payment";
    }
    
    
    float valueOf(Pledge p, Actor a) {
      return a.motives.greedPriority(p.amount) * (a == p.makes ? -1 : 1);
    }
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      if (isRuler(p.makes)) {
        final BaseFinance.Source source = BaseFinance.SOURCE_REWARDS;
        p.makes.base().finance.incCredits(0 - p.amount, source);
      }
      else {
        p.makes.gear.incCredits(0 - p.amount);
      }
      
      final Actor paid = (Actor) p.refers;
      if (isRuler(paid)) {
        final BaseFinance.Source source = BaseFinance.SOURCE_DONATION;
        p.makes.base().finance.incCredits(p.amount, source);
      }
      else {
        paid.gear.incCredits(p.amount);
      }
    }
  };
  
  public static Pledge paymentPledge(float amount, Actor from, Actor to) {
    return new Pledge(TYPE_PAYMENT, amount, to, from);
  }
  
  
  
  final public static Type TYPE_MILITARY_DUTY = new Type("Military Duty") {
    
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      return NO_PLEDGE;
    }
    
    
    String description(Pledge p) {
      return "Military Duty";
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final Object work = a.mind.work();
      if (work instanceof Conscription) {
        return ((Conscription) work).motiveBonus(a);
      }
      return Plan.ROUTINE;
    }
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      final Object work = p.makes.mind.work();
      if (work instanceof Conscription) {
        ((Conscription) work).beginDowntime(p.makes);
      }
    }
  };
  
  public static Pledge militaryDutyPledge(Actor from) {
    return new Pledge(TYPE_MILITARY_DUTY, from);
  }
  
  
  
  final public static Type TYPE_GIFT_ITEM = new Type("Gift Item") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      final Property from = makes.mind.home();
      if (! (from instanceof Venue)) return NO_PLEDGE;
      
      final Batch <Pledge> pledges = new Batch <Pledge> ();
      for (Item i : from.inventory().allItems()) {
        if (i.type.form == Economy.FORM_PROVISION) continue;
        int amount = Nums.min(Nums.floor(i.amount / 2), 10);
        if (amount <= 0) continue;
        
        Bringing d = new Bringing(Item.withAmount(i, amount), from, makesTo);
        pledges.add(new Pledge(this, d, makes));
      }
      return pledges.toArray(Pledge.class);
    }
    
    
    String description(Pledge p) {
      final Item gift = ((Bringing) p.refers).allDelivered()[0];
      return "Gift of "+gift;
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final Item gift = ((Bringing) p.refers).allDelivered()[0];
      return a.motives.rateValue(gift);
    }
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      final Bringing b = (Bringing) p.refers;
      final Item gift = b.allDelivered()[0];
      b.origin.inventory().removeItem(gift);
      b.destination.inventory().addItem(gift);
    }
  };
  
  public static Pledge giftPledge(
    Item item, Owner depot, Actor from, Actor to
  ) {
    final Bringing d = new Bringing(item, depot, to);
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
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      return;
    }
  };
  
  public static Pledge goodWillPledge(Actor from, Actor to) {
    return new Pledge(TYPE_GOOD_WILL, from, to);
  }
  
  
  
  final public static Type TYPE_JOIN_MISSION = new Type("Join Mission") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      if (isRuler(makes) || ! isRuler(makesTo)) return NO_PLEDGE;
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
      return step == null ? 0 : step.priority();
    }
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      final Mission m = (Mission) p.refers;
      p.makes.mind.assignMission(m);
      m.setApprovalFor(p.makes, true);
      m.setSpecialRewardFor(p.makes, reward);
    }
  };
  
  public static Pledge missionPledge(Mission m, Actor from) {
    return new Pledge(TYPE_JOIN_MISSION, m, from);
  }
  
  
  
  final public static Type TYPE_AUDIENCE = new Type("Audience") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      if (isRuler(makes)) return NO_PLEDGE;
      return new Pledge[] { new Pledge(this, makesTo, makes) };
    }
    
    
    String description(Pledge p) {
      return "Personal Audience";
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final Actor host = (Actor) p.refers;
      return a.relations.valueFor(host) * Plan.ROUTINE;
    }
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      final Actor host = (Actor) p.refers;
      final Faction f = host.base().faction();
      final Item pass = Item.withReference(Economy.ITEM_PASSCODE, f);
      p.makes.gear.addItem(pass);
      final Summons summons = Summons.officialSummons(p.makes, host);
      p.makes.mind.assignBehaviour(summons);
    }
  };
  
  public static Pledge audiencePledge(Actor from, Actor to) {
    return new Pledge(TYPE_AUDIENCE, to, from);
  }
  
  
  final public static Type TYPE_JOIN_BASE = new Type("Join Base") {
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      if (isRuler(makes)) return NO_PLEDGE;
      final Object home = makes.mind.home();
      if (! (home instanceof Venue)) return NO_PLEDGE;
      final Venue belongs = (Venue) home;
      if (belongs.base() == makesTo.base()) return NO_PLEDGE;
      return new Pledge[] { new Pledge(this, makesTo, makes) };
    }
    
    
    float valueOf(Pledge p, Actor a) {
      if (a == p.refers) {
        return Plan.ROUTINE;
      }
      
      final Base  joins   = ((Actor) p.refers).base();
      final Venue belongs = (Venue) p.makes.mind.home();
      final Base  local   = belongs.base();
      
      float overallSuccess = 0, numBelong = 0;
      for (Actor b : belongs.staff().workers()) {
        overallSuccess += b.relations.valueFor(joins.faction());
        numBelong++;
      }
      for (Actor b : belongs.staff().lodgers()) {
        overallSuccess += b.relations.valueFor(joins.faction());
        numBelong++;
      }
      if (numBelong > 0) overallSuccess /= numBelong;
      
      float localPower = 0, liegePower = 0;
      liegePower = 0 - joins.dangerMap.sampleAround(belongs, -1);
      localPower = 0 - local.dangerMap.sampleAround(belongs, -1);
      
      float powerSum = Nums.abs(liegePower) + Nums.abs(localPower);
      float overallPower = liegePower - localPower;
      overallPower /= Nums.max(1, powerSum);
      
      return (overallSuccess + overallPower - 1) * Plan.ROUTINE;
    }
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      final Venue belongs = (Venue) p.makes.mind.home();
      final Base  joins   = ((Actor) p.refers).base();
      
      ((Venue) belongs).assignBase(joins);
      for (Actor a : belongs.staff().workers()) {
        a.assignBase(joins);
      }
      for (Actor a : belongs.staff().lodgers()) {
        a.assignBase(joins);
      }
    }
    
    
    String description(Pledge p) {
      final Base joins = ((Actor) p.refers).base();
      return "Join "+joins;
    }
  };
  
  public static Pledge joinBasePledge(Actor joining, Base joins) {
    return new Pledge(TYPE_JOIN_BASE, joins.ruler(), joining);
  }
  
  
  
  final public static Type TYPE_OPEN_BORDERS = new Type("Open Borders") {
    
    
    public Pledge[] variantsFor(Actor makes, Actor makesTo) {
      if (! (isRuler(makes) && isRuler(makesTo))) return NO_PLEDGE;
      return new Pledge[] { new Pledge(this, makesTo.base(), makes) };
    }
    
    
    public boolean canMakePledge(Actor makes, Actor makesTo) {
      return makesTo.inWorld();
    }
    
    
    float valueOf(Pledge p, Actor a) {
      final Base       base  = (Base) p.refers;
      final Verse      verse = base.world.offworld;
      final Sector     with  = verse.currentSector(p.makes);
      final SectorBase SB    = verse.baseForSector(with);
      final float      mult  = Plan.PARAMOUNT;
      return Faction.relationValue(SB.faction(), base.faction(), verse) * mult;
    }
    
    
    void performFulfillment(Pledge p, Pledge reward) {
      final Base   base = (Base) p.refers;
      final Sector with = base.world.offworld.currentSector(p.makes);
      base.visits.togglePartner(with, true);
    }
    
    
    String description(Pledge p) {
      return "Peace Treaty with "+p.refers;
    }
  };
  
  public static Pledge openBordersPledge(Base base, SectorBase with) {
    return new Pledge(TYPE_OPEN_BORDERS, base, with.ruler());
  }
  
  
  //  TODO:  FIND A MORE EFFICIENT WAY TO HANDLE THIS!
  //         ...Just limit to the same circles.
  
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









