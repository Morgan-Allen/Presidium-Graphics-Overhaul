

package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.Inventory.Owner;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Venue.*;
import static stratos.game.economic.Economy.*;



//  NOTE:  I'm moving these methods here essentially for the sake of reducing
//  clutter/space demands within the main Venue class.

public class VenueDescription {
  
  
  final public static String
    CAT_UPGRADES = "UPGRADES",
    CAT_STOCK    = "STOCK"   ,
    CAT_STAFFING = "STAFFING",
    CAT_VISITORS = "VISITORS";
  
  final String categories[];
  final Venue v;  //  TODO:  Apply to Properties, like, e.g, vehicles?
  private Upgrade lastCU = null;  //last clicked upgrade.
 
  
  protected VenueDescription(Venue v, String... categories) {
    this.categories = categories;
    this.v = v;
  }
  
  
  public static SelectionInfoPane configStandardPanel(
    Venue venue, SelectionInfoPane panel, BaseUI UI, boolean stocksOrders
  ) {
    final String categories[] = {
      CAT_UPGRADES, CAT_STOCK, CAT_STAFFING, CAT_VISITORS
    };
    final VenueDescription VD = new VenueDescription(venue, categories);
    if (panel == null) panel = new SelectionInfoPane(
      UI, venue, venue.portrait(UI), true, categories
    );
    final String category = panel.category();
    final Description d = panel.detail(), l = panel.listing();
    
    VD.describeCondition(d, UI);
    if (category == CAT_UPGRADES) VD.describeUpgrades(l, UI);
    if (category == CAT_STOCK   ) {
      if (stocksOrders) VD.describeStockOrders(l, UI);
      else              VD.describeStocks     (l, UI);
    }
    if (category == CAT_STAFFING) VD.describeStaffing(l, UI);
    if (category == CAT_VISITORS) VD.describeVisitors(l, UI);
    return panel;
  }
  
  
  public static SelectionInfoPane configSimplePanel(
    Venue venue, SelectionInfoPane panel,
    BaseUI UI, String statusMessage
  ) {
    if (panel == null) panel = new SelectionInfoPane(
      UI, venue, venue.portrait(UI), true, CAT_STOCK, CAT_STAFFING
    );
    
    final String category = panel.category();
    final Description d = panel.detail(), l = panel.listing();
    final VenueDescription VD = new VenueDescription(venue);
    
    VD.describeCondition(d, UI);
    
    if (statusMessage != null) {
      d.append("\n\n");
      d.append(statusMessage);
    }
    
    if (category == CAT_STOCK   ) VD.describeStocks  (l, UI);
    if (category == CAT_STAFFING) VD.describeStaffing(l, UI);
    //if (category == CAT_VISITORS) VD.describeVisitors(l, UI);
    return panel;
  }

  
  
  //  TODO:  At this point, you might as well write some custom widgets.
  //final static int MIN_TRADE = 5, MAX_TRADE = 20;
  
  
  private void describeStockOrders(Description d, BaseUI UI) {
    final Traded types[] = v.services();
    d.append("Orders:");
    
    if (! v.structure.intact()) {
      d.append("  Venue under construction!  Cannot set orders until done.");
      return;
    }
    
    for (int i = 0 ; i < types.length; i++) {
      final Traded t = types[i];
      if (t.form != FORM_MATERIAL) continue;
      
      final Tier tier = v.stocks.demandTier(t);
      final int level = (int) Nums.ceil(v.stocks.demandFor(t));
      d.append("\n  ");
      
      if (tier == Tier.IMPORTER) d.append(new Description.Link("IMPORT") {
        public void whenClicked() {
          v.stocks.forceDemand(t, 0, Tier.EXPORTER);
        }
      }, Colour.GREEN);
      if (tier == Tier.TRADER) d.append(new Description.Link("TRADE") {
        public void whenClicked() {
          v.stocks.forceDemand(t, 0, Tier.IMPORTER);
        }
      }, Colour.BLUE);
      if (tier == Tier.EXPORTER) d.append(new Description.Link("EXPORT") {
        public void whenClicked() {
          v.stocks.forceDemand(t, 0, Tier.TRADER);
        }
      }, Colour.MAGENTA);
      
      final float amount = v.stocks.amountOf(t);
      d.append(" "+I.shorten(amount, 1)+"/");
      
      final float maxTrade = v.spaceFor(t);
      d.append(new Description.Link(I.lengthen(level, 4)) {
        public void whenClicked() {
          v.stocks.forceDemand(t, (level >= maxTrade) ? 0 : (level + 5), tier);
        }
      });
      d.append(" ");
      d.append(t);
    }
  }
  
  
  
  private void describeCondition(Description d, BaseUI UI) {
    final Stage world = v.world();
    d.append("Condition and Repair:");
    d.append("\n  Integrity: ");
    d.append(v.structure().repair()+" / "+v.structure().maxIntegrity());
    
    if (v instanceof Inventory.Owner) {
      final Inventory i = ((Inventory.Owner) v).inventory();
      d.append("\n  Credits: "+(int) i.credits());
      d.append(" ("+(int) i.unTaxed()+" Untaxed)");
    }
    
    final float squalor = 0 - world.ecology().ambience.valueAt(v);
    if (squalor > 0) {
      final String SN = " ("+I.shorten(squalor, 1)+")";
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Squalor"+SN);
    }
    else {
      final String AN = " ("+I.shorten(0 - squalor, 1)+")";
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Ambience"+AN);
    }
    
    final float danger = v.base().dangerMap.sampleAround(v, Stage.SECTOR_SIZE);
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
  
  
  protected void describeStocks(Description d, BaseUI UI) {
    d.append("Stocks and Provisions:");
    boolean empty = true;
    //
    //  Describe supply and demand for power, life support, etc:
    for (Traded t : Economy.ALL_PROVISIONS) {
      final float output = v.structure.outputOf(t);
      if (output > 0) {
        d.append("\n  "+t+" Output: "+I.shorten(output, 1));
        empty = false;
        continue;
      }
      final float demand = v.stocks.demandFor(t);
      if (demand <= 0) continue;
      final float supply = v.stocks.amountOf(t);
      d.append("\n  "+I.shorten(supply, 1)+"/"+I.shorten(demand, 1)+" "+t);
      empty = false;
    }
    //
    //  Then describe conventional items:
    final Sorting <Item> sortedItems = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a.equals(b)) return 0;
        if (a.type.basePrice() > b.type.basePrice()) return  1;
        if (a.type.basePrice() < b.type.basePrice()) return -1;
        if (a.quality > b.quality) return  1;
        if (a.quality < b.quality) return -1;
        return 0;
      }
    };
    for (Item item : v.stocks.allItems()) {
      sortedItems.add(item);
    }
    for (Traded type : v.stocks.demanded()) if (v.stocks.amountOf(type) == 0) {
      sortedItems.add(Item.withAmount(type, 0));
    }
    if (sortedItems.size() > 0) empty = false;
    for (Item item : sortedItems) describeStocks(item, d);
    //
    //  And finally, list any special orders:
    final Sorting <Item> sortedOrders = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a.equals(b)) return 0;
        return v.stocks.amountOf(a) < v.stocks.amountOf(b) ? 1 : -1;
      }
    };
    for (Manufacture m : v.stocks.specialOrders()) {
      sortedOrders.add(m.made());
    }
    for (Item i : v.stocks.allItems()) {
      if (i.type.form == Economy.FORM_MATERIAL ) continue;
      if (i.type.form == Economy.FORM_PROVISION) continue;
      if (sortedOrders.includes(i)) continue;
      sortedOrders.add(i);
    }
    if (sortedOrders.size() > 0) {
      empty = false;
      d.append("\n\nSpecial Orders:");
      for (Item i : sortedOrders) {
        d.append("\n  ");
        i.describeTo(d);
        if (v.stocks.hasOrderFor(i)) {
          final float progress = v.stocks.amountOf(i) / 1f;
          d.append(" ("+((int) (progress * 100))+"%)");
        }
      }
    }
    
    if (empty) d.append("\n  No stocks or orders.");
  }
  
  
  protected boolean describeStocks(Item item, Description d) {
    final Traded type = item.type;
    if (type.form != Economy.FORM_MATERIAL) return false;
    
    final float needed = v.stocks.demandFor(type);
    final float amount = v.stocks.amountOf(type);
    if (needed == 0 && amount == 0) return false;

    final String nS = I.shorten(needed, 1);
    d.append("\n  ");
    item.describeTo(d);
    
    final int price = (int) Nums.ceil(v.priceFor(type));
    d.append(" /"+nS+" (Price "+price+")");
    return true;
  }
  
  
  
  private void describeStaffing(Description d, BaseUI UI) {
    final Background c[] = v.careers();
    if (c != null && c.length > 0) {
      for (Background b : c) {
        final int
          hired = v.staff.numHired(b),
          total = v.numOpenings(b);
        if (total == 0 && hired == 0) continue;
        
        ((Text) d).cancelBullet();
        d.append(b.name+": ("+hired+"/"+total+")");
        
        for (final FindWork a : v.staff.applications) {
          if (a.employer() != v || a.position() != b) continue;
          final Actor p = a.actor();
          ((Text) d).insert(p.portrait(UI).texture(), 40, true);
          d.append(p);
          d.append(p.inWorld() ? " (" : " (Offworld ");
          d.append(p.vocation().name+")");
          
          final Series <Trait>
            TD = HumanDescription.sortTraits(p.traits.personality(), p),
            SD = HumanDescription.sortTraits(p.traits.skillSet()   , p);
          
          int numS = 0;
          for (Trait s : SD) {
            if (a.position().skillLevel((Skill) s) <= 0) continue;
            if (++numS > 3) break;
            d.append("\n  "+s+" ("+((int) p.traits.traitLevel(s))+") ");
          }
          d.append("\n  ");
          int numT = 0;
          for (Trait t : TD) {
            if (++numT > 3) break;
            d.append(t+" ");
          }
          
          d.append("\n  ");
          final String hireDesc = "Hire for "+a.hiringFee()+" credits";
          d.append(new Description.Link(hireDesc) {
            public void whenClicked() { v.staff.confirmApplication(a); }
          });
        }
        
        for (Actor a : v.staff.workers()) if (a.vocation() == b) {
          descActor(a, d, UI);
        }
        d.append("\n");
      }
    }
    
    //  TODO:  Make residency a kind of 'opening' for these purposes?
    ((Text) d).cancelBullet();
    d.append("Residents: ");
    if (v.staff.residents().size() == 0) d.append("\n  No residents.");
    for (Actor a : v.staff.residents()) descActor(a, d, UI);
  }
  
  
  private void describeVisitors(Description d, BaseUI UI) {
    d.append("Visitors:");
    for (Mobile m : v.inside()) descActor(m, d, UI);
  }
  
  
  private void descActor(Mobile m, Description d, BaseUI UI) {
    if (d instanceof Text && m instanceof Human) {
      final Composite p = ((Human) m).portrait(UI);
      ((Text) d).insert(p.texture(), 40, true);
    }
    else d.append("\n\n  ");
    d.append(m);
    d.append("\n  ");
    m.describeStatus(d);
    if (m instanceof Actor) {
      d.append("\n  ");
      d.append(descDuty((Actor) m));
    }
  }
  
  
  private String descDuty(Actor a) {
    if (a.mind.work() == v) return v.staff.onShift(a) ?
      "(On-Duty)" : "(Off-Duty)"
    ;
    if (a.mind.home() == v) return "(Resident)";
    return "(Visiting)";
  }
  
  
  private void describeUpgrades(Description d, BaseUI UI) {
    //final Base played = BaseUI.current().played();
    
    if (! v.structure().intact()) {
      d.append("Upgrades unavailable while under construction.");
      return;
    }
    
    //  TODO:  Try to revise this, and include some explanatory text for why
    //  they haven't started just yet.
    //  TODO:  Also- don't allow upgrades until the structure is finished
    //  building!  (Conversely, DO allow hiring before then.)
    final Upgrade UA[] = Upgrade.upgradesFor(v.getClass());
    if (UA == null || UA.length == 0) {
      d.append("No upgrades available.");
      return;
    }
    
    final Colour grey = Colour.LITE_GREY;
    
    if (lastCU != null) {
      if (! Visit.arrayIncludes(UA, lastCU)) lastCU = UA[0];
      d.append("\n");
      d.append(lastCU.description, Colour.LITE_GREY);
      for (Upgrade u : lastCU.required) {
        d.append("\n  Requires: "+u.name);
      }
      if (! v.structure.upgradePossible(lastCU)) {
        d.append("\n\n");
        d.append(v.structure.upgradeError(lastCU));
      }
      d.append("\n\n");
      d.append(new Description.Link("BACK") {
        public void whenClicked() { lastCU = null; }
      });
    }
    
    else for (final Upgrade upgrade : UA) {
      final int cost = upgrade.buildCost;
      final boolean possible =
        v.structure.upgradePossible(upgrade) &&
        cost <= v.base().finance.credits()
      ;
      final int
        level  = v.structure.upgradeLevel(upgrade, Structure.STATE_INTACT ),
        queued = v.structure.upgradeLevel(upgrade, Structure.STATE_INSTALL);
      if (level + queued == 0 && ! possible) continue;
      
      d.append("\n"+upgrade.name+" x"+level+"  ("+cost+" Credits)");
      
      d.append("\n  ");
      final String desc = "INSTALL";
      if (possible) d.append(new Description.Link(desc) {
        public void whenClicked() {
          v.structure.beginUpgrade(upgrade, false);
        }
      });
      else d.append(desc, grey);
      
      d.append("  ");
      d.append(new Description.Link("INFO") {
        public void whenClicked() { lastCU = upgrade; }
      });
    }
    
    final Batch <String> OA = v.structure.descOngoingUpgrades();
    if (OA.size() > 0) {
      d.append("\n\nUpgrades in progress: ");
      for (String u : OA) d.append("\n  "+u);
      d.append("\n");
    }
  }
  
  
  private void describeOrders(Description d) {
    d.append("\n");
    d.append("\nOrders:");
    
    final Batch <Description.Link> orders = new Batch();
    addOrdersTo(orders);
    for (Description.Link link : orders) {
      d.append(" ");
      d.append(link);
    }
  }
  
  
  protected void addOrdersTo(Series <Description.Link> orderList) {
    final Base played = BaseUI.currentPlayed();

    if (played == v.base()) {
      if (v.structure.needsSalvage()) {
        orderList.add(new Description.Link("Cancel Salvage") {
          public void whenClicked() {
            v.structure.cancelSalvage();
          }
        });
      }
      else {
        orderList.add(new Description.Link("Salvage") {
          public void whenClicked() {
            v.structure.beginSalvage();
          }
        });
      }
    }
  }
  
  
}









