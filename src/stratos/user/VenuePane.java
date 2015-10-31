/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.base.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



//  TODO:  At this point, you might as well write some custom widgets here...

public class VenuePane extends SelectionPane {
  
  
  final public static String
    CAT_UPGRADES = "UPGRADE",
    CAT_STOCK    = "STOCK"  ,
    CAT_STAFFING = "STAFF"  ,
    DEFAULT_CATS[] = { CAT_UPGRADES, CAT_STOCK, CAT_STAFFING };
  
  final public Venue v;  //  TODO:  Apply to Properties, like, e.g, vehicles?
  private Actor dismissing = null;
 
  
  protected VenuePane(BaseUI UI, Venue v, String... categories) {
    super(UI, v, v.portrait(UI), true, categories);
    this.v = v;
  }
  
  
  public static SelectionPane configStandardPanel(
    Venue venue, SelectionPane panel, BaseUI UI,
    Traded setStock[], String... cats
  ) {
    if (cats == null || cats.length == 0) cats = DEFAULT_CATS;
    if (panel == null) panel = new VenuePane(UI, venue, cats);
    final VenuePane VD = (VenuePane) panel;
    
    final String category = panel.category();
    final Description d = panel.detail(), l = panel.listing();
    
    VD.describeCondition(d, UI);
    if (category == CAT_UPGRADES) VD.describeUpgrades(l, UI);
    if (category == CAT_STOCK) VD.describeStocks(l, UI, setStock);
    /*
    {
      if (setStock != null && setStock.length > 0) {
        VD.describeStockOrders(l, setStock, UI);
      }
      else VD.describeStocks(l, UI, setStock);
    }
    //*/
    if (category == CAT_STAFFING) VD.describeStaffing(l, UI);
    return panel;
  }
  
  
  public static SelectionPane configSimplePanel(
    Venue venue, SelectionPane panel,
    BaseUI UI, Traded setStock[], String statusMessage
  ) {
    if (panel == null) panel = new VenuePane(
      UI, venue
    );
    final VenuePane VD = (VenuePane) panel;
    final Description d = panel.detail(), l = panel.listing();
    VD.describeCondition(d, UI);
    
    if (statusMessage != null) {
      d.append("\n\n");
      d.append(statusMessage);
    }
    
    VD.describeStocks(l, UI, setStock);
    l.append("\n");
    VD.describeStaffing(l, UI);
    return panel;
  }
  
  
  private void describeCondition(Description d, BaseUI UI) {
    final Stage world = v.origin().world();
    d.append("Condition and Repair:");
    
    final int repair = Nums.round(v.structure.repair(), 1, true);
    d.append("\n  Integrity: ");
    d.append(repair+" / "+v.structure().maxIntegrity());
    
    final Inventory i = v.inventory();
    d.append("\n  Credits: "+(int) i.allCredits());
    d.append(" ("+(int) i.unTaxed()+" Untaxed)");
    
    final float squalor = 0 - world.ecology().ambience.valueAt(v);
    if (squalor > 0) {
      final String SN = " ("+I.shorten(squalor, 1)+")";
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Squalor"+SN);
    }
    else {
      final String AN = " ("+I.shorten(0 - squalor, 1)+")";
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Ambience"+AN);
    }
    
    final float danger = v.base().dangerMap.sampleAround(v, Stage.ZONE_SIZE);
    if (danger > 0) {
      final String DN = " ("+I.shorten(danger, 1)+")";
      d.append("\n  "+Ambience.dangerDesc(danger)+" Hazards"+DN);
    }
    else {
      final String SN = " ("+I.shorten(0 - danger, 1)+")";
      d.append("\n  "+Ambience.dangerDesc(danger)+" Safety"+SN);
    }
    
    describeOrders(d);
    
    d.append("\n\n");
    d.append(v.helpInfo(), Colour.LITE_GREY);
  }
  
  
  
  /**  Listing demands, inventory and special items-
    */
  final public static Traded ITEM_LIST_ORDER[] = (Traded[]) Visit.compose(
    Traded.class, ALL_PROVISIONS, ALL_MATERIALS, ALL_SPECIAL_ITEMS
  );
  
  protected void describeStocks(Description d, BaseUI UI, Traded setStock[]) {
    d.append("Stocks and Provisions:");
    
    final Traded[] demands = setStock == null ? v.stocks.demanded() : setStock;
    final Batch <Traded> special = new Batch();
    
    for (Traded t : ITEM_LIST_ORDER) {
      final float needed = v.stocks.demandFor(t);
      final float amount = v.stocks.amountOf (t);
      
      if (Visit.arrayIncludes(demands, t) && t.common()) {
        describeStocks(t, d, setStock == demands);
      }
      else if (needed > 0 || amount > 0) special.add(t);
    }
    for (Item i : v.stocks.allItems()) {
      if (Visit.arrayIncludes(ITEM_LIST_ORDER, i.type)) continue;
      if (v.stocks.hasOrderFor(i)) continue;
      special.include(i.type);
    }
    
    if (! special.empty()) {
      Text.cancelBullet(d);
      d.append("\nOther Items:");
      for (Traded t : special) {
        if (t.common()) describeStocks(t, d, false);
        else for (Item i : v.stocks.matches(t)) {
          d.append("\n  ");
          i.describeTo(d);
        }
      }
    }
    if (v.stocks.specialOrders().size() > 0) {
      Text.cancelBullet(d);
      d.append("\nSpecial Orders:");
      for (Item i : v.stocks.specialOrders()) {
        d.append("\n  ");
        i.describeTo(d);
        final float progress = v.stocks.amountOf(i);
        d.append(" ("+(int) (progress * 100)+"%)");
      }
    }
  }
  
  
  protected boolean describeStocks(
    final Traded type, Description d, boolean set
  ) {
    final float needed = v.stocks.demandFor(type);
    final float amount = v.stocks.amountOf (type);
    
    Text.insert(type.icon.asTexture(), 20, 20, true, d);
    d.append("  "+I.shorten(amount, 1)+" ");
    d.append(type);

    if (set) {
      final String MODES[] = { "Trading", "No Trade", "Imports", "Exports" };
      final boolean
        consumer  =   v.stocks.consumer     (type),
        producer  =   v.stocks.producer     (type),
        freeTrade = ! v.stocks.isDemandFixed(type),
        noTrade   =   needed == 0 && ! freeTrade,
        trader    =   v.owningTier() == Owner.TIER_TRADER;
      final int
        numModes = MODES.length,
        setUnit  = Nums.round(needed, 5, false),
        limit    = v.spaceFor(type),
        mode     = noTrade ? 1 : (freeTrade ? 0 : (producer ? 3 : 2));
      
      if (mode != 1) {
        final String nS = I.shorten(needed, 1);
        d.append(" /"+nS);
      }
      d.append(" (");
      
      String modeDesc = MODES[mode];
      //if (mode == 0 && producer) modeDesc = "Made";
      //if (mode == 0 && consumer) modeDesc = "Used";
      
      d.append(new Description.Link(modeDesc) {
        public void whenClicked() {
          int nextMode = (mode + 1) % numModes;
          if (consumer && nextMode == 3 && ! trader) nextMode = 0;
          if (producer && nextMode == 2 && ! trader) nextMode = 3;
          
          if (nextMode == 0) v.stocks.setFreeTrade(type);
          if (nextMode == 1) v.stocks.forceDemand(type, 0, producer);
          if (nextMode == 2) v.stocks.forceDemand(type, 5, false);
          if (nextMode == 3) v.stocks.forceDemand(type, 5, true );
        }
      });
      
      if (mode > 1 && setUnit < limit) d.append(new Description.Link(" More") {
        public void whenClicked() {
          if (mode == 2) v.stocks.forceDemand(type, setUnit + 5, false);
          if (mode == 3) v.stocks.forceDemand(type, setUnit + 5, true );
        }
      });
      d.append(")");
      //else d.append("(MORE)", Colour.GREY);
    }
    else {
      if (needed != amount) {
        final String nS = I.shorten(needed, 1);
        d.append(" /"+nS);
      }
      if (v.stocks.producer(type)) d.append(" (Made)");
      if (v.stocks.consumer(type)) d.append(" (Used)");
      //if (v.stocks.producer(type)) d.append(" (producer)");
      //else 
    }
    
    return true;
  }
  
  
  
  /**  Describing personnel, visitors and residents-
    */
  private void describeStaffing(Description d, BaseUI UI) {
    final Background c[] = v.careers();
    Batch <Actor> mentioned = new Batch <Actor> ();
    
    if (dismissing != null) {
      d.append("\nAre you certain you want to dismiss ");
      d.append(dismissing);
      d.append("?\n  ");
      d.append(new Description.Link("Confirm") {
        public void whenClicked() {
          final Actor works = dismissing;
          if (! works.inWorld()) v.base().commerce.removeCandidate(works);
          works.mind.setWork(null);
          dismissing = null;
        }
      });
      d.append(new Description.Link("  Cancel") {
        public void whenClicked() { dismissing = null; }
      });
      return;
    }
    
    if (c != null && c.length > 0) {
      
      for (Background b : c) {
        final int
          hired = v.staff.numHired    (b),
          total = v.staff.numPositions(b),
          apps  = v.staff.numApplied  (b);
        if (total == 0 && hired == 0) continue;
        
        Text.cancelBullet(d);
        d.append(b.name+": ("+hired+"/"+total+")");
        if (apps > 0) d.append("\n  Total applied: "+apps);
        
        for (FindWork a : v.staff.applications()) if (a.position() == b) {
          mentioned.include(a.actor());
          descApplicant(a.actor(), a, d, UI);
        }
        for (final Actor a : v.staff.workers()) if (a.mind.vocation() == b) {
          descActor(a, d, UI, v);
          d.append("\n  ");
          d.append(descDuty(a));
          mentioned.include(a);
          d.append(new Description.Link("  Dismiss") {
            public void whenClicked() { dismissing = a; }
          });
        }
        d.append("\n");
      }
    }
    
    Text.cancelBullet(d);
    d.append("Residents: ");
    boolean anyLives = false;
    for (Actor a : v.staff.lodgers()) {
      if (mentioned.includes(a)) continue;
      descActor(a, d, UI, v);
      anyLives = true;
    }
    if (! anyLives) d.append("None.");
    d.append("\n");
    
    Text.cancelBullet(d);
    d.append("Visitors: ");
    boolean anyVisit = false;
    for (Mobile m : v.inside()) {
      if (Staff.doesBelong(m, v)) continue;
      descActor(m, d, UI, v);
      anyVisit = true;
    }
    if (! anyVisit) d.append("None.");
    d.append("\n");
  }
  
  
  private String descDuty(Actor a) {
    if (a.mind.work() == v) return v.staff.onShift(a) ?
      "(On-Duty)" : "(Off-Duty)"
    ;
    if (a.mind.home() == v) return "(Resident)";
    return "(Visiting)";
  }
  
  
  private void describeUpgrades(Description d, final BaseUI UI) {
    if (! v.structure().intact()) {
      d.append("Upgrades unavailable while under construction.");
      return;
    }
    
    //  TODO:  Allow both hiring and queuing of upgrades before a structure is
    //  completed.
    
    final Series <Upgrade> UA = Upgrade.upgradesAvailableFor(v);
    if (UA == null || UA.size() == 0) {
      d.append("No upgrades available.");
      return;
    }
    
    final Base base = v.base();
    int numU = v.structure.numUpgrades(), maxU = v.structure.maxUpgrades();
    if (maxU > 0) d.append("\nUpgrades Installed: "+numU+"/"+maxU);
    
    for (final Upgrade upgrade : UA) {
      d.append("\n");
      upgrade.appendVenueOrders(d, v, base);
    }
    
    final Batch <String> OA = v.structure.descOngoingUpgrades();
    if (OA.size() > 0) {
      Text.cancelBullet(d);
      d.append("\n\nUpgrades in progress: ");
      for (String u : OA) d.append("\n  "+u);
      d.append("\n");
      
      if (v.structure.upgradeProgress() == 0) d.append(
        "\nUpgrades will be installed once your engineering staff arrive "+
        "on-site.", Colour.LITE_GREY
      );
      else d.append(
        "\nUpgrades are currently being installed.", Colour.LITE_GREY
      );
    }
  }
  
  
  private void describeOrders(Description d) {
    d.append("\n  Orders:");
    
    final Batch <Description.Link> orders = new Batch();
    addOrdersTo(orders);
    for (Description.Link link : orders) {
      d.append(" ");
      d.append(link);
    }
    
    //  TODO:  INCLUDE A RE-LOCATE OPTION TOO!
  }
  
  
  protected void addOrdersTo(Series <Description.Link> orderList) {
    final Base played = BaseUI.currentPlayed();

    if (played == v.base()) {
      if (v.structure.needsSalvage()) {
        orderList.add(new Description.Link("Cancel Salvage") {
          public void whenClicked() {
            v.structure.cancelSalvage();
            if (I.logEvents()) I.say("\nCANCELLED SALVAGE: "+v);
          }
        });
      }
      else {
        orderList.add(new Description.Link("Salvage") {
          public void whenClicked() {
            v.structure.beginSalvage();
            if (I.logEvents()) I.say("\nBEGAN SALVAGE: "+v);
          }
        });
      }
    }
  }
  
  
  
  /**  Utility methods for actor-description that tend to get re-used
    *  elsewhere...
    */
  public static void descApplicant(
    Actor a, final FindWork sought, Description d, BaseUI UI
  ) {
    final Composite comp = a.portrait(UI);
    if (comp != null) Text.insert(comp.texture(), 40, 40, true, d);
    else d.append("\n");
    
    d.append(a);
    d.append(a.inWorld() ? " (" : " (Offworld ");
    d.append(a.mind.vocation().name+")");
    if (sought.wasHired()) return;
    
    final Series <Trait>
      TD = ActorDescription.sortTraits(a.traits.personality(), a),
      SD = ActorDescription.sortTraits(a.traits.skillSet()   , a);
    
    int numS = 0;
    for (Trait s : SD) {
      if (sought.position().skillLevel((Skill) s) <= 0) continue;
      if (++numS > 3) break;
      d.append("\n  "+s+" ("+((int) a.traits.traitLevel(s))+") ");
    }
    d.append("\n  ");
    int numT = 0;
    for (Trait t : TD) {
      if (++numT > 3) break;
      d.append(t+" ");
    }
    
    d.append("\n  ");
    final String hireDesc = "Hire for "+sought.hiringFee()+" credits";
    d.append(new Description.Link(hireDesc) {
      public void whenClicked() { sought.confirmApplication(); }
    });
    d.append(new Description.Link("  Dismiss") {
      public void whenClicked() { sought.cancelApplication(); }
    });
  }
  
  
  public static void descActor(Mobile m, Description d, BaseUI UI, Venue v) {
    if (d instanceof Text && m instanceof Actor) {
      final Composite p = ((Actor) m).portrait(UI);
      final String ID = ""+m.hashCode();
      if (p != null) Text.insert(p.delayedImage(UI, ID), 40, 40, true, d);
      else d.append("\n");
    }
    else d.append("\n\n  ");
    d.append(m);
    d.append("\n  ");
    m.describeStatus(d, v);
  }
}









