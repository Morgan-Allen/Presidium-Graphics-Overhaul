/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class BudgetsPane extends SelectionPane {
  
  
  final static ImageAsset
    BUDGETS_ICON = ImageAsset.fromImage(
      BudgetsPane.class, "budgets_pane_icon", "media/GUI/Panels/edicts_tab.png"
      //  TODO:  Get a more specific icon!
    ),
    BUDGETS_ICON_LIT = Button.CROSSHAIRS_LIT;
  
  final public static String
    CAT_DEMAND   = "[MARKETS]",
    CAT_BUDGET   = "[BUDGETS]",
    CATEGORIES[] = { CAT_DEMAND, CAT_BUDGET };
  
  
  private int periodShown = -1;
  
  
  public BudgetsPane(BaseUI UI) {
    super(UI, null, null, false, false, 0, CATEGORIES);
    setWidgetID(BUDGETS_PANE_ID);
  }
  
  
  static Button createButton(final BaseUI baseUI) {
    return new PaneButton(
      new BudgetsPane(baseUI), baseUI,
      BUDGETS_BUTTON_ID, BUDGETS_ICON, BUDGETS_ICON_LIT, "Finance"
    );
  }
  
  
  protected void updateText(
    Text headerText, Text detailText, Text listingText
  ) {
    super.updateText(headerText, detailText, listingText);
    final Description d = detailText;
    if (category() == CAT_BUDGET) describeFinance(d);
    if (category() == CAT_DEMAND) describeDemands(d);
  }
  
  
  protected void describeFinance(Description d) {
    final Base base = BaseUI.currentPlayed();
    final BaseFinance BF = base.finance;
    
    d.append("FINANCE REPORT FOR "+base);
    int sumWI = 0, sumWO = 0, sumWB = 0;
    //
    //  First of all, we let the user determine what day-period to inspect:
    final Batch <Integer> periods = BF.periodIDs();
    
    d.append("\n\n  Show period:");
    for (final int period : periods) d.append(
      new Description.Link(" Day "+period+" ") {
        public void whenClicked(Object context) { periodShown = period; }
      },
      (period == periodShown ? Colour.GREEN : Text.LINK_COLOUR)
    );
    d.append(new Description.Link(" Total") {
      public void whenClicked(Object context) {
        periodShown = -1;
      }
    }, (periodShown == -1 ? Colour.GREEN : Text.LINK_COLOUR));
    //
    //  Then we list all income sources:
    d.append("\n\n  Income sources");
    
    for (BaseFinance.Source key : BaseFinance.ALL_SOURCES) {
      final int period = (int) BF.periodIncome(key, periodShown);
      if (period == 0) continue;
      d.append("\n    "+key+": "+period);
      sumWI += period ;
    }
    d.append("\n    Total income: "+sumWI);
    //
    //  And outlay sources-
    d.append("\n\n  Outlay sources");
    for (BaseFinance.Source key : BaseFinance.ALL_SOURCES) {
      final int period = 0 - (int) BF.periodOutlay(key, periodShown);
      if (period == 0) continue;
      d.append("\n    "+key+": "+period);
      sumWO += period ;
    }
    d.append("\n    Total outlays: "+sumWO);
    //
    //  And finally, total balance-
    sumWB = sumWI - sumWO;
    d.append("\n\n  Balance: "+sumWB);
    d.append("\n  Current credit: "+(int) BF.credits());

    final Tally <Property>
      balances = base.finance.venueBalances(periodShown);
    
    d.append("\n\n  Balances by venue");
    if (balances.size() == 0) {
      d.append("\n    No venues assessed.", Colour.LITE_GREY);
    }
    else for (Property venue : balances.keys()) {
      d.appendAll("\n    ", venue, " : ");
      final int total = (int) balances.valueFor(venue);
      final Colour toneT = total >= 0 ? Colour.GREEN : Colour.RED;
      d.append(""+total, toneT);
    }
  }
  
  
  protected void describeDemands(Description d) {
    final Base base = BaseUI.currentPlayed();
    final Verse universe = base.world.offworld;
    final Sector locale = universe.stageLocation();
    final BaseDemands BD = base.demands;
    final BaseVisits BV = base.visits;

    Text.cancelBullet(d);
    d.append("DEMAND REPORT FOR "+base);
    
    for (final Traded t : Economy.ALL_MATERIALS) {
      final String
        priceImp = I.shorten(BD.importPrice(t), 0),
        priceExp = I.shorten(BD.exportPrice(t), 0),
        demand   = I.shorten(BD.primaryDemand   (t), 0),
        supply   = I.shorten(BD.primarySupply   (t), 0),
        consPD   = I.shorten(BD.dailyConsumption(t), 1),
        prodPD   = I.shorten(BD.dailyProduction (t), 1);
      final boolean
        allowImp = BD.allowsImport(t),
        allowExp = BD.allowsExport(t);
      
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append(" ");
      d.append(t);
      
      d.append(" ("+supply+"/"+demand+")");
      d.append(" ("+prodPD+"/"+consPD+" per day)");
      
      d.append("\n");
      d.append(" Import for "+priceImp+": ");
      d.append(new Description.Link(allowImp ? "Yes" : "No ") {
        public void whenClicked(Object context) {
          BD.setImportsAllowed(t, ! allowImp);
        }
      });
      d.append("\n");
      d.append(" Export for "+priceExp+": ");
      d.append(new Description.Link(allowExp ? "Yes" : "No ") {
        public void whenClicked(Object context) {
          BD.setExportsAllowed(t, ! allowExp);
        }
      });
      d.append("\n");
    }
    
    Text.cancelBullet(d);
    Text.cancelBullet(d);
    
    for (SectorBase partner : BV.partners()) {
      final Vehicle nextShip = universe.journeys.nextTraderBetween(
        partner.location, locale, base, true
      );
      if (nextShip == null) continue;
      
      d.append(partner);
      if (partner.location == BV.homeworld()) d.append("  (Homeworld)");
      else d.append(" (Trading Partner)");

      float ETA = universe.journeys.arrivalETA(nextShip, base);
      ETA /= Stage.STANDARD_HOUR_LENGTH;
      d.append("\n  Dropship ETA: "+Nums.round(ETA, 1, true)+" hours");
      
      d.append("\n ");
      d.append(" (Makes: ");
      for (Traded t : partner.made()) {
        if (t.form != Economy.FORM_MATERIAL) continue;
        Text.insert(t.icon.asTexture(), 20, 20, false, d);
      }
      d.append(")");
      d.append(" (Needs: ");
      for (Traded t : partner.needed()) {
        if (t.form != Economy.FORM_MATERIAL) continue;
        Text.insert(t.icon.asTexture(), 20, 20, false, d);
      }
      d.append(")");
      d.append("\n\n");
    }
    if (BV.partners().size() == 0) d.append("\n\nNo Trade Partners.");
    
  }
}













