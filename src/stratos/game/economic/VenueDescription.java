

package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Venue.*;



//  NOTE:  I'm moving these methods here essentially for the sake of reducing
//  clutter/space demands within the main Venue class.

public class VenueDescription {
  
  
  final String categories[];
  final Venue v;  //  TODO:  Apply to Properties, like, e.g, vehicles?
  private static Upgrade lastCU;  //last clicked upgrade.
 
  
  protected VenueDescription(Venue v, String... categories) {
    this.categories = categories;
    this.v = v;
  }
  
  
  public static SelectionInfoPane configStandardPanel(
    Venue venue, SelectionInfoPane panel, BaseUI UI, String... extraCategories
  ) {
    final String categories[] = (String[]) Visit.compose(
      String.class, extraCategories,
      new String[] { CAT_UPGRADES, CAT_STOCK, CAT_STAFF }
    );
    final VenueDescription VD = new VenueDescription(venue, categories);
    if (panel == null) panel = new SelectionInfoPane(
      UI, venue, venue.portrait(UI), true, categories
    );
    final String category = panel.category();
    final Description d = panel.detail(), l = panel.listing();
    
    VD.describeCondition(d, UI);
    if (category == CAT_UPGRADES) VD.describeUpgrades (l, UI);
    if (category == CAT_STOCK   ) VD.describeStocks   (l, UI);
    if (category == CAT_STAFF   ) VD.describeStaff(l, UI);
    return panel;
  }
  
  
  public static SelectionInfoPane configSimplePanel(
    Venue venue, SelectionInfoPane panel,
    BaseUI UI, String statusMessage
  ) {
    if (panel == null) panel = new SelectionInfoPane(
      UI, venue, venue.portrait(UI), true, CAT_STOCK, CAT_STAFF
    );
    
    final String category = panel.category();
    final Description d = panel.detail(), l = panel.listing();
    final VenueDescription VD = new VenueDescription(venue);
    
    VD.describeCondition(d, UI);
    if (statusMessage != null) {
      d.append("\n");
      d.append(statusMessage);
    }

    if (category == CAT_STOCK) VD.describeStocks   (l, UI);
    if (category == CAT_STAFF) VD.describeStaff(l, UI);
    return panel;
  }
  
  
  
  private void describeCondition(
    Description d, BaseUI UI
  ) {
    
    final Stage world = v.world();
    d.append("Condition and Repair:");
    d.append("\n  Integrity: ");
    d.append(v.structure().repair()+" / "+v.structure().maxIntegrity());
    
    final String CUD = v.structure().currentUpgradeDesc();
    if (CUD != null) d.append("\n  "+CUD);
    
    if (v instanceof Inventory.Owner) {
      final Inventory i = ((Inventory.Owner) v).inventory();
      d.append("\n  Untaxed Credits: "+(int) i.credits());
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
    
    final Tile t = world.tileAt(v);
    final float danger = v.base().dangerMap.sampleAt(t.x, t.y);
    if (danger > 0) {
      final String DN = " ("+I.shorten(danger, 1)+")";
      d.append("\n  "+Ambience.dangerDesc(danger)+" Hazards"+DN);
    }
    else {
      final String SN = " ("+I.shorten(0 - danger, 1)+")";
      d.append("\n  "+Ambience.dangerDesc(danger)+" Safety"+SN);
    }
    
    d.append("\n\n");
    
    d.append(v.helpInfo(), Colour.LIGHT_GREY);
  }
  
  
  protected void describeStocks(Description d, BaseUI UI) {
    d.append("Stocks:");
    boolean empty = true;
    
    final Sorting <Item> sortedItems = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a.equals(b)) return 0;
        if (a.type.basePrice() > b.type.basePrice()) return  1;
        if (a.type.basePrice() < b.type.basePrice()) return -1;
        /*
        if (a.refers != null && b.refers != null) {
          if (a.refers == b.refers) return 0;
          if (a.refers.hashCode() > b.refers.hashCode()) return  1;
          if (a.refers.hashCode() < b.refers.hashCode()) return -1;
        }
        //*/
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
        //float progress = v.stocks.amountOf(i) / 1f;
        //d.append(" ("+((int) (progress * 100))+"%)");
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
  
  
  
  private void describeStaff(Description d, BaseUI UI) {
    d.append("Staff:");
    final Background c[] = v.careers();
    if (c != null && c.length > 0) {
      for (Background b : c) {
        final int
          hired = v.staff.numHired(b),
          total = hired + v.numOpenings(b);
        d.append("\n  "+hired+"/"+total+" "+b.name);
      }
      //d.append("\n");
    }
    
    final Batch <Mobile> considered = new Batch <Mobile> ();
    for (Actor m : v.staff.residents()) considered.include(m);
    for (Actor m : v.staff.workers()) considered.include(m);
    for (Mobile m : v.inside()) considered.include(m);
    
    for (Mobile m : considered) {
      if (d instanceof Text && m instanceof Human) {
        final Composite p = ((Human) m).portrait(UI);
        ((Text) d).insert(p.texture(), 40, true);
      }
      else d.append("\n\n  ");
      d.append(m);
      if (m instanceof Actor) {
        d.append("\n  ");
        d.append(descDuty((Actor) m));
      }
      d.append("\n  ");
      m.describeStatus(d);
    }

    for (final FindWork a : v.staff.applications) {
      if (a.employer() != v) continue;
      final Actor p = a.actor();
      d.append("\n\n");
      
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
      
      d.append(new Description.Link("\n  Hire for "+a.hiringFee()+" credits") {
        public void whenClicked() { v.staff.confirmApplication(a); }
      });
    }
  }
  
  
  private String descDuty(Actor a) {
    final Background b = a.vocation();
    final String VN = b == null ? a.species().toString() : b.nameFor(a);
    if (a.mind.work() == v) {
      final String duty = v.staff.onShift(a) ? "On-Duty" : "Off-Duty";
      return "("+duty+" "+VN+")";
    }
    if (a.mind.home() == v) return "(Resident "+VN+")";
    return "(Visiting "+VN+")";
  }
  
  
  private void describeUpgrades(Description d, BaseUI UI) {
    final Base played = BaseUI.current().played();

    final Upgrade UA[] = Upgrade.upgradesFor(v.getClass());
    if (UA == null || UA.length == 0) {
      d.append("No upgrades available.");
      return;
    }
    
    final int
      numU = v.structure.numUpgrades(),
      maxU = v.structure.maxUpgrades();
    
    final Batch <String> DU = v.structure.descOngoingUpgrades();
    if (DU != null && DU.size() > 0) {
      d.append("Upgrades in queue:");
      for (String s : DU) d.append("\n  "+s);
      d.append("\n\n");
    }
    
    d.append("Upgrades available: ("+numU+"/"+maxU+" used)");
    
    if (UA.length > 0) for (final Upgrade upgrade : UA) {
      d.append("\n  ");
      final boolean possible = v.structure.upgradePossible(upgrade);
      final int level = v.structure.upgradeLevel(upgrade);
      
      d.append(new Description.Link(upgrade.name) {
        public void whenClicked() { lastCU = upgrade; }
      }, possible ? Text.LINK_COLOUR : Colour.LIGHT_GREY);

      final Colour linkC = possible ? Colour.WHITE : Colour.LIGHT_GREY;
      d.append("  (Cost "+upgrade.buildCost+")", linkC);
      if (level > 0) d.append(" (x"+level+")", linkC);
    }
    if (! Visit.arrayIncludes(UA, lastCU)) lastCU = UA[0];
    
    if (lastCU != null) {
      d.append("\n\n");
      d.append(lastCU.description, Colour.LIGHT_GREY);
      if (lastCU.required != null) {
        d.append("\n  Requires: "+lastCU.required.name);
      }
      if (v.structure.upgradePossible(lastCU)) {
        d.append(new Description.Link("\n\n  BEGIN UPGRADE") {
          public void whenClicked() {
            v.structure.beginUpgrade(lastCU, false);
          }
        });
      }
    }
    
    if (played == v.base() && ! v.privateProperty()) {
      d.append("\n\nOther Orders: ");
      if (v.structure.needsSalvage()) {
        d.append(new Description.Link("\n  Cancel Salvage") {
          public void whenClicked() {
            v.structure.cancelSalvage();
          }
        });
      }
      else {
        d.append(new Description.Link("\n  Begin Salvage") {
          public void whenClicked() {
            v.structure.beginSalvage();
          }
        });
      }
      d.append("\n\n");
    }
  }
}


