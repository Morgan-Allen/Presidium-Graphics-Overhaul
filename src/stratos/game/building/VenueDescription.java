

package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.Manufacture;
import static stratos.game.building.Venue.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  NOTE:  I'm moving these methods here essentially for the sake of reducing
//  clutter/space demands within the main Venue class.

public class VenueDescription {
  
  
  final String categories[];
  final Venue v;
  private static Upgrade lastCU;  //last clicked upgrade.
 
  
  protected VenueDescription(Venue v, String... categories) {
    this.categories = categories;
    this.v = v;
  }
  
  
  public static SelectionInfoPane configPanelWith(
    Venue venue, SelectionInfoPane panel, BaseUI UI, String... categories
  ) {
    final VenueDescription VD = new VenueDescription(
      venue, categories
    );
    return VD.configPanel(panel, UI);
  }
  
  
  public static SelectionInfoPane configStandardPanel(
    Venue venue, SelectionInfoPane panel, BaseUI UI
  ) {
    final VenueDescription VD = new VenueDescription(
      venue, CAT_UPGRADES, CAT_STATUS, CAT_STAFF, CAT_STOCK
    );
    return VD.configPanel(panel, UI);
  }
  
  
  public static SelectionInfoPane configSimplePanel(
    Installation venue, SelectionInfoPane panel, BaseUI UI, String statusMessage
  ) {
    if (panel == null) panel = new SelectionInfoPane(UI, venue, venue.portrait(UI));
    final Description d = panel.detail();

    final VenueDescription VD = new VenueDescription(null);
    VD.describeCondition(venue, d, UI, false);
    
    if (statusMessage != null) {
      d.append("\n");
      d.append(statusMessage);
    }
    return panel;
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    if (panel == null) panel = new SelectionInfoPane(
      UI, v, v.portrait(UI), categories
    );
    final String category = panel.category();
    final Description d = panel.detail();
    describeCategory(d, UI, category);
    return panel;
  }
  
  
  protected void describeCategory(Description d, BaseUI UI, String catID) {
    if (catID == CAT_UPGRADES) describeUpgrades(d, UI);
    if (catID == CAT_STATUS  ) describeCondition(v, d, UI, true);
    if (catID == CAT_STAFF   ) describePersonnel(d, UI);
    if (catID == CAT_STOCK   ) describeStocks(d, UI);
  }
  
  
  
  private void describeCondition(
    Installation v,
    Description d, BaseUI UI, boolean detail
  ) {
    
    final Stage world = v.world();
    d.append("Condition and Repair:");
    d.append("\n  Integrity: ");
    d.append(v.structure().repair()+" / "+v.structure().maxIntegrity());
    
    if (detail) {
      final String CUD = v.structure().currentUpgradeDesc();
      if (CUD != null) d.append("\n  "+CUD);
      d.append("\n  Materials Needed: "+"None");
      
      if (v instanceof Inventory.Owner) {
        final Inventory i = ((Inventory.Owner) v).inventory();
        d.append("\n  Untaxed Credits: "+(int) i.credits());
      }
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
    d.append("Stocks and Orders:");
    boolean empty = true;
    
    final Sorting <Item> listing = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a.equals(b)) return 0;
        if (a.type.typeID > b.type.typeID) return  1;
        if (a.type.typeID < b.type.typeID) return -1;
        if (a.refers != null && b.refers != null) {
          if (a.refers.hashCode() > b.refers.hashCode()) return  1;
          if (a.refers.hashCode() < b.refers.hashCode()) return -1;
        }
        if (a.quality > b.quality) return  1;
        if (a.quality < b.quality) return -1;
        return 0;
      }
    };
    for (Item item : v.stocks.allItems()) listing.add(item);
    for (Traded type : Economy.ALL_ITEM_TYPES) {
      if (v.stocks.demandFor(type) > 0 && v.stocks.amountOf(type) == 0) {
        listing.add(Item.withAmount(type, 0));
      }
    }
    if (listing.size() > 0) empty = false;
    for (Item item : listing) describeStocks(item, d);
    
    for (Manufacture m : v.stocks.specialOrders()) {
      d.append("\n  "); m.describeBehaviour(d); empty = false;
    }
    if (empty) d.append("\n  No stocks or orders.");
  }
  
  
  protected boolean describeStocks(Item item, Description d) {
    final Traded type = item.type;
    final float needed = v.stocks.demandFor(type);
    final float amount = v.stocks.amountOf(type);
    if (needed == 0 && amount == 0)
      return false;

    final String nS = I.shorten(needed, 1);
    d.append("\n  ");
    item.describeTo(d);
    
    final int price = (int) Math.ceil(v.priceFor(type));
    d.append(" /"+nS+" (Price "+price+")");
    return true;
  }
  
  
  
  private void describePersonnel(Description d, BaseUI UI) {
    final Background c[] = v.careers();
    if (c != null && c.length > 0) {
      d.append("\nCareers and Openings:");
      for (Background b : c) {
        final int
          hired = v.personnel.numHired(b),
          total = hired + v.numOpenings(b);
        d.append("\n  "+hired+"/"+total+" "+b.name);
      }
      d.append("\n");
    }
    
    d.append("\nPersonnel:");
    final Batch <Mobile> considered = new Batch <Mobile> ();
    for (Actor m : v.personnel.residents()) considered.include(m);
    for (Actor m : v.personnel.workers()) considered.include(m);
    for (Mobile m : v.inside()) considered.include(m);
    
    for (Mobile m : considered) {
      d.append("\n  ");
      if (d instanceof Text && m instanceof Human) {
        final Composite p = ((Human) m).portrait(UI);
        ((Text) d).insert(p.texture(), 40);
      }
      d.append(m);
      if (m instanceof Actor) {
        d.append("\n  ");
        d.append(descDuty((Actor) m));
      }
      d.append("\n  ");
      m.describeStatus(d);
    }

    for (final Application a : v.personnel.applications) {
      final Actor p = a.applies;
      d.append("\n\n");
      
      ((Text) d).insert(p.portrait(UI).texture(), 40);
      d.append(p);
      d.append(p.inWorld() ? " (" : " (Offworld ");
      d.append(p.vocation().name+")");
      
      final Series <Trait>
        TD = HumanDescription.sortTraits(p.traits.personality(), p),
        SD = HumanDescription.sortTraits(p.traits.skillSet()   , p);
      
      int numS = 0;
      for (Trait s : SD) {
        if (a.position.skillLevel((Skill) s) <= 0) continue;
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
        public void whenTextClicked() { v.personnel.confirmApplication(a); }
      });
    }
  }
  
  
  private String descDuty(Actor a) {
    final Background b = a.vocation();
    final String VN = b == null ? a.species().toString() : b.nameFor(a);
    if (a.mind.work() == v) {
      final String duty = v.personnel.onShift(a) ? "On-Duty" : "Off-Duty";
      return "("+duty+" "+VN+")";
    }
    if (a.mind.home() == v) return "(Resident "+VN+")";
    return "(Visiting "+VN+")";
  }
  
  
  private void describeUpgrades(Description d, BaseUI UI) {
    final Base played = BaseUI.current().played();
    
    if (played == v.base() && ! v.privateProperty()) {
      d.append("Orders: ");
      if (v.structure.needsSalvage()) {
        d.append(new Description.Link("\n  Cancel Salvage") {
          public void whenTextClicked() {
            v.structure.cancelSalvage();
          }
        });
      }
      else {
        d.append(new Description.Link("\n  Begin Salvage") {
          public void whenTextClicked() {
            v.structure.beginSalvage();
          }
        });
      }
      d.append("\n\n");
    }
    
    final int
      numU = v.structure.numUpgrades(),
      maxU = v.structure.maxUpgrades();
    if (maxU > 0) {
      final Batch <String> DU = v.structure.descOngoingUpgrades();
      d.append("Upgrade slots ("+numU+"/"+maxU+")");
      for (String s : DU) d.append("\n  "+s);
      d.append("\n\nUpgrades available: ");
      
      final Index <Upgrade> upgrades = v.allUpgrades();
      if (upgrades != null && upgrades.members().length > 0) {
        for (final Upgrade upgrade : upgrades) {
          d.append("\n  ");
          d.append(new Description.Link(upgrade.name) {
            public void whenTextClicked() { lastCU = upgrade; }
          });
          d.append(" (x"+v.structure.upgradeLevel(upgrade)+")");
        }
        if (lastCU != null) {
          d.append("\n\n");
          d.append(lastCU.description);
          d.append("\n  Cost: "+lastCU.buildCost+"   ");
          if (lastCU.required != null) {
            d.append("\n  Requires: "+lastCU.required.name);
          }
          if (v.structure.upgradePossible(lastCU)) {
            d.append(new Description.Link("\n\n  BEGIN UPGRADE") {
              public void whenTextClicked() {
                v.structure.beginUpgrade(lastCU, false);
              }
            });
          }
        }
      }
      else d.append("\n  No upgrades.");
    }
  }
}


