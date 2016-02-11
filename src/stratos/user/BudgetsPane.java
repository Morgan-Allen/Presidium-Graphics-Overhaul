/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class BudgetsPane extends SelectionPane {
  
  
  final static ImageAsset
    BUDGETS_ICON = ImageAsset.fromImage(
      BudgetsPane.class, "media/GUI/Panels/edicts_tab.png"  //  TODO:  CHANGE
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
    
    d.append("DEMAND REPORT FOR "+base);
    
    Text.cancelBullet(d);
    d.append("\n\nOffworld Prices (Buy | Sell | Base)");
    for (Traded t : Economy.ALL_MATERIALS) {
      
      final String
        priceImp = I.shorten(BD.importPrice(t), 1),
        priceExp = I.shorten(BD.exportPrice(t), 1),
        baseCost = I.shorten(t.defaultPrice() , 1);
      
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append(" ");
      d.append(t);
      d.append("\n");
      d.append(" (");
      d.append(priceImp+"", Colour.LITE_RED  );
      d.append(" | ");
      d.append(priceExp+"", Colour.LITE_GREEN);
      d.append(" | ");
      d.append(baseCost+"", Colour.LITE_BLUE );
      d.append(")");
    }
    
    Text.cancelBullet(d);
    
    //  TODO:  This could be moved out to panes for individual Sectors!
    
    for (SectorBase partner : BV.partners()) {
      d.append("\n\n");
      d.append(partner);
      if (partner.location == BV.homeworld()) d.append("  (Homeworld)");
      else d.append(" (Trading Partner)");
      
      final Vehicle nextShip = universe.journeys.nextTraderBetween(
        partner.location, locale, base, true
      );
      if (nextShip != null) {
        float ETA = universe.journeys.arrivalETA(nextShip, base);
        ETA /= Stage.STANDARD_HOUR_LENGTH;
        d.append("\n  Dropship ETA: "+Nums.round(ETA, 1, true)+" hours");
      }
      
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
    }
    if (BV.partners().size() == 0) d.append("\n\nNo Trade Partners.");

    boolean noLocal = true, noTrade = true;

    Text.cancelBullet(d);
    d.append("\n\nLocal Goods: (supply/demand)");
    for (Traded t : Economy.ALL_MATERIALS) {
      final int
        demand = (int) BD.primaryDemand(t),
        supply = (int) BD.primarySupply(t);
      if (demand == 0 && supply == 0) continue;
      else noLocal = false;
      
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append(" ");
      d.append(t);
      d.append(" ("+supply+"/"+demand+")");
    }
    if (noLocal) d.append("\n  (No local goods)");
    
    Text.cancelBullet(d);
    d.append("\n\nReserved For Trade: (import/export)");
    for (Traded t : Economy.ALL_MATERIALS) {
      final int
        demand = (int) BD.importDemand(t),
        supply = (int) BD.exportSupply(t);
      if (demand == 0 && supply == 0) continue;
      else noTrade = false;
      
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append(" ");
      d.append(t);
      d.append(" ("+supply+"/"+demand+")");
    }
    if (noTrade) d.append("\n  (No trade goods)");
  }
}











