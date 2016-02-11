/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.base.*;
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
    float consumption;
    float production;
    boolean fixed;
  }
  
  
  final Property basis;
  final Table <Traded, Demand> demands = new Table <Traded, Demand> ();
  
  final List <Item> specialOrders = new List <Item> ();
  final List <Bringing> reservations = new List <Bringing> ();
  
  
  public Stocks(Property v) {
    super(v);
    this.basis = v;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
    
    for (int n = s.loadInt(); n-- > 0;) specialOrders.add(Item.loadFrom(s));
    s.loadObjects(reservations);
    
    int numD = s.loadInt();
    while (numD-- > 0) {
      final Demand d = new Demand();
      d.type         = (Traded) s.loadObject();
      d.consumption  = s.loadFloat();
      d.production   = s.loadFloat();
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
      s.saveFloat (d.consumption );
      s.saveFloat (d.production  );
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
    if ((! basis.inWorld()) || oldC == newC) return;
    
    inc = newC - oldC;
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
  
  
  public Manufacture nextManufacture(Actor actor, Conversion c) {
    final float shortage = relativeShortage(c.out.type, true);
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
  

  public void setReservation(Bringing d, boolean is) {
    if (is) reservations.include(d);
    else    reservations.remove (d);
  }
  
  
  public Series <Bringing> reservations() {
    return reservations;
  }
  
  
  /**  Returns the shortage of a given good-type for this vendor...
    *  NOTE:  The amount returned here can vary from +1 to -1 (or even beyond)
    *  depending on whether current stocks are lower than consumption-levels
    *  (positive) or higher (negative, scaled against production-level.)
    */
  public float relativeShortage(Traded type, boolean production) {
    final Demand d = demandRecord(type);
    final float amount = amountOf(type), base = d.consumption;
    
    if (production) {
      return Nums.clamp(1 - (amount / (base + d.production)), 0, 1);
    }
    else if (amount >= base) {
      if (d.production == 0) return -1;
      else return Nums.clamp(0 - (amount - base) / d.production, -1, 0);
    }
    else {
      return 1 - (amount / base);
    }
  }
  
  
  public float absoluteShortage(Traded type, boolean production) {
    final Demand d = demandRecord(type);
    final float amount = amountOf(type), base = d.consumption;
    
    if (production) return base + d.production - amount;
    else return base - amount;
  }
  
  
  
  /**  Supplementary supply/demand-related methods-
    */
  public float totalDemand(Traded type) {
    final Demand d = demands.get(type);
    return d == null ? 0 : d.production + d.consumption;
  }
  

  public float consumption(Traded type) {
    final Demand d = demands.get(type);
    return d == null ? 0 : d.consumption;
  }
  
  
  public float production(Traded type) {
    final Demand d = demands.get(type);
    return d == null ? 0 : d.production;
  }
  
  
  public float absoluteSurplus(Traded type, boolean production) {
    return 0 - absoluteShortage(type, production);
  }
  
  
  public Traded[] demanded() {
    final Batch <Traded> batch = new Batch <Traded> ();
    for (Demand d : demands.values()) if (d.consumption + d.production > 0) {
      batch.add(d.type);
    }
    return batch.toArray(Traded.class);
  }
  
  
  public Traded[] shortageTypes(boolean production) {
    final Batch <Traded> batch = new Batch <Traded> ();
    for (Demand d : demands.values()) {
      if (relativeShortage(d.type, production) > 0) batch.add(d.type);
    }
    return batch.toArray(Traded.class);
  }
  
  
  public Batch <Item> shortages() {
    final Batch <Item> batch = new Batch <Item> ();
    for (Demand d : demands.values()) {
      float amount = absoluteShortage(d.type, false);
      if (amount <= 0) continue;
      batch.add(Item.withAmount(d.type, amount));
    }
    return batch;
  }
  
  
  public boolean isDemandFixed(Traded type) {
    final Demand d = demands.get(type);
    return d != null && d.fixed;
  }
  
  
  public boolean canDemand(Traded type) {
    return demands.get(type) != null;
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
    final Traded services[] = basis.services();
    if (services != null) for (Traded t : services) setConsumption(t, 0);
  }
  
  
  protected void onWorldExit() {
    clearDemands();
  }
  
  
  public void forceDemand(
    Traded type, float consumption, float production
  ) {
    final Demand d = demandRecord(type);
    d.consumption = Nums.max(0, consumption);
    d.production  = Nums.max(0, production );
    d.fixed       = true;
  }
  
  
  public void setFreeTrade(Traded type) {
    final Demand d = demandRecord(type);
    d.fixed = false;
  }
  
  
  public void setConsumption(Traded type, float consumption) {
    final Demand d = demandRecord(type);
    d.consumption = Nums.max(0, consumption);
  }
  
  
  public void updateStockDemands(
    int period, Traded services[], Conversion... cons
  ) {
    final BaseDemands BD = basis.base().demands;
    final Presences   BP = basis.world().presences;
    final Tile        at = basis.world().tileAt(basis);
    final boolean isTrader = basis.owningTier() == Owner.TIER_TRADER;
    
    //  TODO:  Scrub demand for anything not listed in services (or as a raw
    //  material.)
    
    for (Item i : specialOrders) if (hasItem(i)) {
      deleteSpecialOrder(i);
    }
    for (Bringing d : reservations) {
      if (! d.isActive()) setReservation(d, false);
    }
    //
    //  Translate any demand for conversion-processes:
    for (Conversion c : cons) for (Item r : c.raw) setConsumption(r.type, 0);
    for (Conversion c : cons) translateRawDemands(c, period);
    //
    //  Update production-demand based on sampling of ambient consumption-
    //  levels:
    if (isTrader) {
      for (Traded t : services) if (t.common()) {
        final Demand d = demandRecord(t);
        
        boolean buys  = amountOf(d.type) > 0 && d.production > 0;
        boolean sells = d.consumption > 0 && ! buys;
        BP.togglePresence(basis, at, sells, d.type.supplyKey);
        BP.togglePresence(basis, at, buys , d.type.demandKey);
        
        if (d.fixed) {
          final float need = (d.consumption + d.production) / 2;
          BD.impingeDemand(d.type, need, period, basis);
        }
        else {
          d.consumption = BD.demandSampleFor(basis, t, period);
          d.production  = BD.supplySampleFor(basis, t, period);
        }
      }
    }
    else {
      for (Traded t : services) {
        final Demand d = demandRecord(t);
        if (d.fixed || ! t.common()) continue;
        d.production = BD.demandSampleFor(basis, t, period) + 1;
      }
      for (Demand d : demands.values()) {
        boolean sells = amountOf(d.type) > 0 && d.production > 0;
        boolean buys  = d.consumption > 0 && ! sells;
        BP.togglePresence(basis, at, sells, d.type.supplyKey);
        BP.togglePresence(basis, at, buys , d.type.demandKey);
        
        if (d.fixed || d.production > 0) continue;
        BD.impingeDemand(d.type, d.consumption, period, basis);
      }
    }
  }
  
  
  private void translateRawDemands(Conversion cons, int period) {
    final boolean report = extraVerbose && I.talkAbout == basis;
    final float demand = cons.out == null ? 1 : totalDemand(cons.out.type);
    if (report) {
      I.say("\nTranslating demand for "+cons+" at "+basis);
      I.say("  Base demand is: "+demand);
    }
    if (demand <= 0) return;
    
    for (Item raw : cons.raw) {
      final float needed = raw.amount * demand / cons.out.amount;
      if (report) I.say("  Need "+needed+" "+raw.type+" as raw materials");
      final Demand d = demandRecord(raw.type);
      d.consumption += needed;
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
      d.production = d.consumption = 0;
      presences.togglePresence(basis, at, false, d.type.demandKey);
      presences.togglePresence(basis, at, false, d.type.supplyKey);
    }
  }
}



