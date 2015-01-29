/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
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
    Tier tierType = Tier.PRODUCER;
    private float demandAmount;
    private float demandBonus;
    private float pricePaid;
    private boolean fixed;
  }
  
  
  final Property basis;
  final Table <Traded, Demand> demands = new Table <Traded, Demand> ();
  final List <Manufacture> specialOrders = new List <Manufacture> ();
  final List <Delivery> reservations = new List <Delivery> ();
  
  
  public Stocks(Property v) {
    super(v);
    this.basis = v;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
    s.loadObjects(specialOrders);
    s.loadObjects(reservations );
    
    int numD = s.loadInt();
    while (numD-- > 0) {
      final Demand d = new Demand();
      d.type         = (Traded) s.loadObject();
      d.demandAmount = s.loadFloat();
      d.demandBonus  = s.loadFloat();
      d.tierType     = (Tier) s.loadEnum(Tier.values());
      d.pricePaid    = s.loadFloat();
      d.fixed        = s.loadBool ();
      demands.put(d.type, d);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(specialOrders);
    s.saveObjects(reservations );
    
    s.saveInt(demands.size());
    for (Demand d : demands.values()) {
      s.saveObject(d.type        );
      s.saveFloat (d.demandAmount);
      s.saveFloat (d.demandBonus );
      s.saveEnum  (d.tierType    );
      s.saveFloat (d.pricePaid   );
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
    final int oldC = (int) credits();
    super.incCredits(inc);
    final int newC = (int) credits();
    if (! basis.inWorld() || oldC == newC) return;
    String phrase = inc >= 0 ? "+" : "-";
    phrase+=" "+(int) Nums.abs(inc)+" credits";
    basis.chat().addPhrase(phrase);
  }
  
  
  
  /**  Assigning and producing jobs-
    */
  public void addSpecialOrder(Manufacture newOrder) {
    specialOrders.add(newOrder);
  }
  
  
  public boolean hasOrderFor(Item made) {
    for (Manufacture m : specialOrders) {
      if (m.made().matchKind(made)) return true;
    }
    return false;
  }
  
  
  public Traded[] demanded() {
    final Batch <Traded> batch = new Batch <Traded> ();
    for (Demand d : demands.values()) batch.add(d.type);
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
  
  
  //  TODO:  Get rid of this?  Or fold into 'special orders?'
  public Manufacture nextManufacture(Actor actor, Conversion c) {
    final float shortage = shortageOf(c.out.type);
    if (shortage <= 0) return null;
    
    final Manufacture m = new Manufacture(
      actor, basis, c,
      Item.withAmount(c.out, shortage + 5)
    );
    return m;
  }
  
  
  public List <Manufacture> specialOrders() {
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
  
  
  public Tier demandTier(Traded type) {
    final Demand d = demands.get(type);
    if (d == null) return Tier.PRODUCER;
    return d.tierType;
  }
  
  
  public boolean hasEnough(Traded type) {
    return amountOf(type) < (demandFor(type) / 2);
  }
  
  
  public float shortagePenalty(Traded type) {
    if (GameSettings.needsFree && type.form == FORM_PROVISION) return 0;
    final float
      amount = amountOf(type),
      demand = demandFor(type),
      shortage = demand - amount;
    return Nums.clamp(
      (shortage - ((demand + 1f) / 2)) / (amount + 1),
      0, 1
    );
  }
  
  
  public float shortageOf(Traded type) {
    return demandFor(type) - amountOf(type);
  }
  
  
  public float surplusOf(Traded type) {
    return amountOf(type) - demandFor(type);
  }
  
  
  public float priceFor(Traded type) {
    final Demand d = demands.get(type);
    if (d == null) return type.basePrice();
    return (d.pricePaid + type.basePrice()) / 2f;
  }
  
  
  
  /**  Utility methods for setting and propagating various types of demand-
    */
  private Demand demandRecord(Traded t) {
    final Demand d = demands.get(t);
    if (d != null) return d;
    Demand made = new Demand();
    made.type = t;
    made.pricePaid = t.basePrice();
    demands.put(t, made);
    return made;
  }
  

  protected void onWorldEntry() {
    //  TODO:  Use this to register service presences?
  }
  
  
  protected void onWorldExit() {
    clearDemands();
  }
  
  
  public void forceDemand(Traded type, float amount, Tier tier) {
    if (amount < 0) amount = 0;
    final Demand d = demandRecord(type);
    d.demandAmount = amount;
    d.fixed        = true  ;
    if (tier != Tier.ANY) d.tierType = tier;
  }
  
  
  public void incDemand(
    Traded type, float amount, Tier tier, int period
  ) {
    final Demand d = demandRecord(type);
    if (d.fixed) return;
    if (tier != Tier.ANY) d.tierType = tier;
    d.demandBonus += amount * period;
  }
  
  
  public void setDefaultTier(Traded types[], Tier tier) {
    for (Traded type : types) {
      final Demand d = demandRecord(type);
      final boolean fixed =
        d.tierType == Tier.TRADER   ||
        d.tierType == Tier.IMPORTER ||
        d.tierType == Tier.EXPORTER;
      if (! fixed) d.tierType = tier;
    }
  }
  
  
  public void translateDemands(Conversion cons, int period) {
    final boolean report = extraVerbose && I.talkAbout == basis;
    final float demand = cons.out == null ? 1 : shortageOf(cons.out.type);
    if (report) {
      I.say("\nTranslating demand for "+cons+" at "+basis);
      I.say("  Base demand is: "+demand);
    }
    if (demand <= 0) return;
    
    for (Item raw : cons.raw) {
      final float needed = raw.amount * demand / cons.out.amount;
      if (report) I.say("  Need "+needed+" "+raw.type+" as raw materials");
      incDemand(raw.type, needed, Tier.CONSUMER, period);
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
    
    final Traded services[] = basis.services();
    if (services != null) for (Traded t : services) {
      if (demands.get(t) == null) incDemand(t, 0, Tier.PRODUCER, period);
    }
    
    for (Manufacture m : specialOrders) {
      translateDemands(m.conversion, period);
    }
    
    for (Delivery d : reservations) {
      if (! d.isActive()) setReservation(d, false);
    }
    
    for (Demand d : demands.values()) {
      final Tier tier = d.tierType;
      final Traded type = d.type;
      final boolean trades = tier == Tier.TRADER, fixed = d.fixed;
      final float amount = amountOf(type);
      
      final boolean shouldGive = (
        tier == Tier.PRODUCER || tier == Tier.IMPORTER || trades
      );
      final boolean shouldTake = (
        tier == Tier.CONSUMER || tier == Tier.EXPORTER || trades
      );
      if (! fixed) d.demandAmount = d.demandBonus / period;
      d.demandBonus = 0;
      if (report) {
        I.say("  Updating channel for "+d.type);
        I.say("    Gives/takes:   "+shouldGive+"/"+shouldTake);
      }
      
      if (shouldGive) {
        final float
          demandEst = BD.demandAround (basis, type, -1),
          shortage  = BD.localShortage(basis, type    );
        
        if (! fixed) {
          float minSupply = Nums.ceil(demandEst + shortage);
          d.demandAmount += Nums.min (minSupply, maxSupply);
        }
        final float supplyEst = (d.demandAmount + amount) / 2;
        if (report) {
          I.say("    Local demand:     "+demandEst);
          I.say("    Local shortage:   "+shortage );
          I.say("    Impinging supply: "+supplyEst);
        }
        BD.impingeSupply(type, supplyEst, period, basis);
      }
      
      final boolean
        gives = shouldGive && amount         > 0,
        takes = shouldTake && d.demandAmount > 0;
      if (takes) {
        if (report) I.say("    Impinging demand: "+d.demandAmount);
        BD.impingeDemand(type, d.demandAmount, period, basis);
      }
      BP.togglePresence(basis, at, gives, type.supplyKey);
      BP.togglePresence(basis, at, takes, type.demandKey);
    }
  }
  
  
  public void updateOrders() {
    for (Manufacture m : specialOrders) {
      if (m.commission == null || m.finished()) specialOrders.remove(m);
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
      d.tierType = Tier.PRODUCER;
      presences.togglePresence(basis, at, false, d.type.demandKey);
    }
  }
}



