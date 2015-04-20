/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.base.BaseDemands;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Stocks extends Inventory {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  private static boolean
    verbose      = false,
    extraVerbose = false;
  
  final static int
    SUPPLY_PER_WORKER = 5;
  
  static class Demand {
    Traded type;
    private float demandAmount;
    private float demandBonus;
    private boolean producer;
    private boolean fixed;
  }
  
  
  final Property basis;
  final Table <Traded, Demand> demands = new Table <Traded, Demand> ();
  final List <Item> specialOrders = new List <Item> ();
  final List <Delivery> reservations = new List <Delivery> ();
  
  
  public Stocks(Property v) {
    super(v);
    this.basis = v;
    final Traded services[] = v.services();
    if (services != null) for (Traded t : services) {
      incDemand(t, 0, 0, true);
    }
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
    
    for (int n = s.loadInt(); n-- > 0;) specialOrders.add(Item.loadFrom(s));
    s.loadObjects(reservations);
    
    int numD = s.loadInt();
    while (numD-- > 0) {
      final Demand d = new Demand();
      d.type         = (Traded) s.loadObject();
      d.demandAmount = s.loadFloat();
      d.demandBonus  = s.loadFloat();
      d.producer     = s.loadBool ();
      d.fixed        = s.loadBool ();
      demands.put(d.type, d);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveInt(specialOrders.size());
    for (Item i : specialOrders) Item.saveTo(s, i);
    s.saveObjects(reservations);
    
    s.saveInt(demands.size());
    for (Demand d : demands.values()) {
      s.saveObject(d.type        );
      s.saveFloat (d.demandAmount);
      s.saveFloat (d.demandBonus );
      s.saveBool  (d.producer    );
      s.saveBool  (d.fixed       );
    }
  }
  
  
  
  
  /**  Overrides of standard inventory methods-
    */
  //  TODO:  Move these to the afterTransaction methods instead?
  
  public boolean addItem(Item item) {
    final int oldAmount = (int) amountOf(item);
    if (super.addItem(item)) {
      final int inc = ((int) amountOf(item)) - oldAmount;
      if (basis.inWorld() && inc != 0 && item.type.form != FORM_PROVISION) {
        String phrase = inc >= 0 ? "+" : "-";
        phrase+=" "+inc+" "+item.type.name;
        basis.chat().addPhrase(phrase);
      }
      return true;
    }
    return false;
  }
  
  
  public void incCredits(float inc) {
    if (Float.isNaN(inc)) I.complain("INC IS NOT-A-NUMBER!");
    if (Float.isNaN(credits)) credits = 0;
    if (inc == 0) return;
    final int oldC = (int) allCredits();
    super.incCredits(inc);
    final int newC = (int) allCredits();
    if (! basis.inWorld() || oldC == newC) return;
    String phrase = inc >= 0 ? "+" : "-";
    phrase+=" "+(int) Nums.abs(inc)+" credits";
    basis.chat().addPhrase(phrase);
  }
  
  
  
  /**  Assigning and producing jobs-
    */
  public void addSpecialOrder(Item ordered) {
    specialOrders.add(ordered);
  }
  
  
  public boolean deleteSpecialOrder(Item ordered) {
    for (Item i : specialOrders) {
      if (i.matchKind(ordered)) { specialOrders.remove(i); return true; }
    }
    return false;
  }
  
  
  public boolean hasOrderFor(Item ordered) {
    for (Item i : specialOrders) {
      if (i.matchKind(ordered)) return true;
    }
    return false;
  }
  
  
  public Traded[] demanded() {
    final Batch <Traded> batch = new Batch <Traded> ();
    for (Demand d : demands.values()) if (d.demandAmount > 0) {
      batch.add(d.type);
    }
    return batch.toArray(Traded.class);
  }
  
  
  public Traded[] shortageTypes() {
    final Batch <Traded> batch = new Batch <Traded> ();
    for (Demand d : demands.values()) if (shortageOf(d.type) > 0) {
      batch.add(d.type);
    }
    return batch.toArray(Traded.class);
  }
  
  
  public Traded[] surplusTypes() {
    final Batch <Traded> batch = new Batch <Traded> ();
    for (Demand d : demands.values()) if (shortageOf(d.type) < 0) {
      batch.add(d.type);
    }
    return batch.toArray(Traded.class);
  }
  
  
  public Batch <Item> shortages() {
    final Batch <Item> batch = new Batch <Item> ();
    for (Demand d : demands.values()) {
      float amount = shortageOf(d.type);
      if (amount <= 0) continue;
      batch.add(Item.withAmount(d.type, amount));
    }
    return batch;
  }
  
  
  public Manufacture nextManufacture(Actor actor, Conversion c) {
    final float shortage = shortageOf(c.out.type);
    if (shortage <= 0) return null;
    
    final Manufacture m = new Manufacture(
      actor, basis, c,
      Item.withAmount(c.out, shortage + 5), false
    );
    return m;
  }
  
  
  public Series <Item> specialOrders() {
    return specialOrders;
  }
  

  public void setReservation(Delivery d, boolean is) {
    if (is) reservations.include(d);
    else    reservations.remove (d);
  }
  
  
  public Series <Delivery> reservations() {
    return reservations;
  }
  
  
  
  /**  Public accessor methods-
    */
  public float demandFor(Traded type) {
    final Demand d = demands.get(type);
    if (d == null) return 0;
    return d.demandAmount;
  }
  
  
  public boolean producer(Traded type) {
    final Demand d = demands.get(type);
    if (d == null) return false;
    return d.producer;
  }
  
  
  public boolean canDemand(Traded type) {
    return demands.get(type) != null;
  }
  
  
  public boolean hasEnough(Traded type) {
    return amountOf(type) > (demandFor(type) / 2);
  }
  
  
  public float relativeShortage(Traded type) {
    final float demand = demandFor(type);
    if (demand == 0) return 0;
    return (demand - amountOf(type)) / demand;
  }
  
  
  public float shortageOf(Traded type) {
    return demandFor(type) - amountOf(type);
  }
  
  
  public float surplusOf(Traded type) {
    return amountOf(type) - demandFor(type);
  }
  
  
  
  /**  Utility methods for setting and propagating various types of demand-
    */
  private Demand demandRecord(Traded t) {
    final Demand d = demands.get(t);
    if (d != null) return d;
    Demand made = new Demand();
    made.type = t;
    demands.put(t, made);
    return made;
  }
  

  protected void onWorldEntry() {
    //  TODO:  Use this to register service presences?
  }
  
  
  protected void onWorldExit() {
    clearDemands();
  }
  
  
  public void forceDemand(
    Traded type, float amount, boolean producer
  ) {
    if (amount < 0) amount = 0;
    final Demand d = demandRecord(type);
    d.demandAmount = amount  ;
    d.fixed        = true    ;
    d.producer     = producer;
  }
  
  
  //  TODO:  You can probably get rid of this now.  It's not really used a
  //         great deal any more- the demand increments are internalised from
  //         the demand-map.
  //*
  public void incDemand(
    Traded type, float amount, int period, boolean producer
  ) {
    //final boolean fresh = demands.get(type) == null;
    //  TODO:  Give a warning if you change the produce/consume type of a non-
    //         fresh demand at a standard facility.
    final Demand d = demandRecord(type);
    if (d.fixed) return;
    
    d.producer = producer;
    d.demandBonus += amount * period;
  }
  //*/
  
  
  public void translateDemands(Conversion cons, int period) {
    final boolean report = extraVerbose && I.talkAbout == basis;
    final float demand = cons.out == null ? 1 : demandFor(cons.out.type);
    if (report) {
      I.say("\nTranslating demand for "+cons+" at "+basis);
      I.say("  Base demand is: "+demand);
    }
    if (demand <= 0) return;
    
    for (Item raw : cons.raw) {
      final float needed = raw.amount * demand / cons.out.amount;
      if (report) I.say("  Need "+needed+" "+raw.type+" as raw materials");
      incDemand(raw.type, needed, period, producer(raw.type));
    }
  }
  
  
  public void updateDemands(int period) {
    final boolean report = I.talkAbout == basis && verbose;
    if (report) {
      I.say("\nUpdating stock demands for "+basis);
      I.say("  Base period: "+period);
    }
    
    final Presences   BP = basis.world().presences;
    final BaseDemands BD = basis.base ().demands;
    final Tile        at = basis.world().tileAt(basis);
    final int maxSupply  = basis.staff().workforce() * SUPPLY_PER_WORKER;
    
    for (Item i : specialOrders) if (i.type.materials() != null) {
      translateDemands(i.type.materials(), period);
    }
    else if (hasItem(i)) {
      deleteSpecialOrder(i);
    }
    
    for (Delivery d : reservations) {
      if (! d.isActive()) setReservation(d, false);
    }
    
    for (Demand d : demands.values()) {
      final Traded type = d.type;
      final boolean available = DeliveryUtils.canTradeBetween(
        owner.owningTier() , d.producer,
        Owner.TIER_FACILITY, false     , false
      );
      final float amount = amountOf(type);
      if (report) {
        I.say("  Updating channel for "+d.type+" (producer "+d.producer+")");
        I.say("    Current amount:    "+amount);
        I.say("    Open for delivery: "+available);
      }

      if (! d.fixed) d.demandAmount = d.demandBonus / period;
      d.demandBonus = 0;
      
      if (available) {
        final float
          demandEst = BD.demandAround (basis, type, -1),
          shortage  = BD.localShortage(basis, type    );
        
        if (! d.fixed) {
          float minSupply = Nums.ceil(demandEst + shortage);
          d.demandAmount += Nums.min (minSupply, maxSupply);
        }
        final float supplyEst = (d.demandAmount + amount) / 2;
        if (report) {
          I.say("    Local demand:      "+demandEst);
          I.say("    Local shortage:    "+shortage );
          I.say("    Impinging supply:  "+supplyEst);
        }
        BD.impingeSupply(type, supplyEst, period, basis);
      }
      
      final boolean
        gives =    available  && amount         > 0,
        takes = (! available) && d.demandAmount > 0;
      if (takes) {
        if (report) I.say("    Impinging demand:  "+d.demandAmount);
        BD.impingeDemand(type, d.demandAmount, period, basis);
      }
      BP.togglePresence(basis, at, gives, type.supplyKey);
      BP.togglePresence(basis, at, takes, type.demandKey);
    }
  }
  
  
  public void updateOrders() {
    for (Item i : specialOrders) {
      if (hasItem(i)) specialOrders.remove(i);
    }
  }
  
  
  public void clearDemands() {
    final Presences presences = basis.world().presences;
    final Tile at = basis.world().tileAt(basis);

    final Traded services[] = basis.services();
    if (services != null) for (Traded s : services) {
      presences.togglePresence(basis, at, false, s.supplyKey);
    }
    
    for (Demand d : demands.values()) {
      d.demandAmount = 0;
      d.producer = false;
      presences.togglePresence(basis, at, false, d.type.demandKey);
      presences.togglePresence(basis, at, false, d.type.supplyKey);
    }
  }
}



