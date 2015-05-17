

package stratos.user;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



//  NOTE:  I'm moving these methods here essentially for the sake of reducing
//  clutter/space demands within the main Venue class.

//  TODO:  At this point, you might as well write some custom widgets.

public class VenuePane extends SelectionPane {
  
  
  final public static String
    CAT_UPGRADES = "UPGRADE" ,
    CAT_STOCK    = "STOCK"   ,
    CAT_STAFFING = "STAFFING",
    DEFAULT_CATS[] = { CAT_UPGRADES, CAT_STOCK, CAT_STAFFING };
  
  final Venue v;  //  TODO:  Apply to Properties, like, e.g, vehicles?
  private Upgrade lastCU = null;
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
    if (category == CAT_STOCK) {
      if (setStock != null && setStock.length > 0) {
        VD.describeStockOrders(l, setStock, UI);
      }
      else VD.describeStocks(l, UI);
    }
    if (category == CAT_STAFFING) VD.describeStaffing(l, UI);
    return panel;
  }
  
  
  public static SelectionPane configSimplePanel(
    Venue venue, SelectionPane panel,
    BaseUI UI, String statusMessage
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
    
    VD.describeStocks(l, UI);
    l.append("\n");
    VD.describeStaffing(l, UI);
    return panel;
  }

  
  
  private void describeStockOrders(Description d, Traded types[], BaseUI UI) {
    d.append("Orders:");
    
    if (! v.structure.intact()) {
      d.append("  Venue under construction!  Cannot set orders until done.");
      return;
    }
    
    for (int i = 0 ; i < types.length; i++) {
      final Traded t = types[i];
      if (t.form != FORM_MATERIAL) continue;
      
      final boolean
        demands = v.stocks.demandFor(t) > 0,
        exports = demands && v.stocks.producer(t);
      
      final int level = (int) Nums.ceil(v.stocks.demandFor(t));
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append("  ");
      
      final float maxTrade = v.spaceFor(t), minTrade = Nums.min(5, maxTrade);
      //
      //  The options here are cyclic...
      if (exports == true) d.append(new Description.Link("EXPORTS") {
        public void whenClicked() {
          if (I.logEvents()) I.say("\n"+t+" IS BEING IMPORTED AT "+v);
          v.stocks.forceDemand(t, minTrade, false);
        }
      }, Colour.LITE_GREEN);
      
      else if (demands == true) d.append(new Description.Link("IMPORTS") {
        public void whenClicked() {
          if (I.logEvents()) I.say("\n"+t+" IS NOT BEING TRADED AT "+v);
          v.stocks.forceDemand(t, 0, false);
        }
      }, Colour.LITE_RED  );
      
      else d.append(new Description.Link("NO TRADE") {
        public void whenClicked() {
          if (I.logEvents()) I.say("\n"+t+" IS BEING EXPORTED AT "+v);
          v.stocks.forceDemand(t, minTrade, true);
        }
      }, Colour.LITE_BLUE );
      
      final float amount = v.stocks.amountOf(t);
      d.append(" "+I.shorten(amount, 1)+"/");
      
      d.append(new Description.Link(I.lengthen(level, 4, false)) {
        public void whenClicked() {
          final int newLevel = (level >= maxTrade) ? 0 : (level + 5);
          v.stocks.forceDemand(t, newLevel, exports);
          if (I.logEvents()) I.say("\n"+t+" LEVEL AT "+v+" IS "+newLevel);
        }
      });
      d.append(" ");
      d.append(t);
      final int price = Nums.round(v.priceFor(t, exports), 1, true);
      d.append(" ("+price+"c)");
    }
  }
  
  
  
  private void describeCondition(Description d, BaseUI UI) {
    final Stage world = v.world();
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
  
  
  protected void describeStocks(Description d, BaseUI UI) {
    d.append("Stocks and Provisions:");
    
    //  TODO:  Just use stock-demands for both these things?
    for (Traded t : Economy.ALL_PROVISIONS) {
      final float output = v.structure.outputOf(t);
      final float demand = v.stocks.demandFor(t);
      if (output <= 0 && demand <= 0) continue;
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append("  ");
      
      if (output > 0) {
        d.append(t+" Output: "+I.shorten(output, 1));
      }
      else if (demand > 0) {
        final float supply = v.stocks.amountOf(t);
        d.append(I.shorten(supply, 1)+"/"+I.shorten(demand, 1)+" "+t);
      }
    }
    for (Traded type : ALL_MATERIALS) {
      describeStocks(type, d);
    }
    
    final Batch <Item> special = new Batch();
    for (Item i : v.stocks.allItems()) if (! i.isCommon()) special.add(i);
    if (! special.empty()) {
      Text.cancelBullet(d);
      d.append("\nOther Items:");
      for (Item i : special) {
        d.append("\n  ");
        i.describeTo(d);
      }
    }
    
    if (v.stocks.specialOrders().size() > 0) {
      Text.cancelBullet(d);
      d.append("\nSpecial Orders:");
      for (Item i : v.stocks.specialOrders()) {
        d.append("\n  ");
        i.describeTo(d);
      }
    }
  }
  
  
  protected boolean describeStocks(Traded type, Description d) {
    if (type.form != FORM_MATERIAL) return false;
    
    final float needed = v.stocks.demandFor(type);
    final float amount = v.stocks.amountOf(type);
    if (needed == 0 && amount == 0) return false;
    
    Text.insert(type.icon.asTexture(), 20, 20, true, d);
    d.append("  "+I.shorten(amount, 1)+" "+type.name);
    
    final String nS = I.shorten(needed, 1);
    d.append(" /"+nS);
    if (v.stocks.producer(type)) d.append(" (producer)");
    else d.append(" (consumer)");
    return true;
  }
  
  
  
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
          hired = v.staff.numHired(b),
          total = v.staff.numOpenings(b) + hired,
          apps  = v.staff.numApplied(b);
        if (total == 0 && hired == 0) continue;
        
        Text.cancelBullet(d);
        d.append(b.name+": ("+hired+"/"+total+")");
        if (apps > 0) d.append("\n  Total applied: "+apps);
        
        for (FindWork a : v.staff.applications()) if (a.position() == b) {
          mentioned.include(a.actor());
          descApplicant(a.actor(), a, d, UI);
        }
        for (final Actor a : v.staff.workers()) if (a.mind.vocation() == b) {
          descActor(a, d, UI);
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
      descActor(a, d, UI);
      anyLives = true;
    }
    if (! anyLives) d.append("None.");
    d.append("\n");
    
    Text.cancelBullet(d);
    d.append("Visitors: ");
    boolean anyVisit = false;
    for (Mobile m : v.inside()) {
      if (Staff.doesBelong(m, v)) continue;
      descActor(m, d, UI);
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
  
  
  private void describeUpgrades(Description d, BaseUI UI) {
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
      d.append("\n");
      d.append(lastCU.description, Colour.LITE_GREY);
      for (Upgrade u : lastCU.required) {
        d.append("\n  Requires: "+u.baseName);
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
      
      final String name = upgrade.nameAt(v, -1, null);
      d.append("\n"+name);
      if (possible) d.append("  ("+cost+" Credits)");
      
      d.append("\n  ");
      final String desc = "INSTALL";
      if (possible) d.append(new Description.Link(desc) {
        public void whenClicked() {
          v.structure.beginUpgrade(upgrade, false);
          if (I.logEvents()) I.say("\nBEGAN UPGRADE: "+upgrade+" AT "+v);
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
    d.append("\n");
    d.append("\nOrders:");
    
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
  
  
  public static void descActor(Mobile m, Description d, BaseUI UI) {
    if (d instanceof Text && m instanceof Actor) {
      final Composite p = ((Actor) m).portrait(UI);
      if (p != null) Text.insert(p.delayedImage(UI), 40, 40, true, d);
      else d.append("\n");
    }
    else d.append("\n\n  ");
    d.append(m);
    d.append("\n  ");
    m.describeStatus(d);
  }
}









