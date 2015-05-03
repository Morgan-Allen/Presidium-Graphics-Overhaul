

package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class CommercePane extends SelectionPane {
  
  
  final static ImageAsset
    COMMERCE_ICON = ImageAsset.fromImage(
      CommercePane.class, "media/GUI/Panels/edicts_tab.png"  //  TODO:  CHANGE
    ),
    COMMERCE_ICON_LIT = Button.CIRCLE_LIT;
  
  final static String
    CAT_DEMAND   = "MARKETS",
    CAT_BUDGET   = "BUDGETING",
    CATEGORIES[] = { CAT_DEMAND, CAT_BUDGET };
  
  
  private int periodShown = -1;
  
  
  public CommercePane(BaseUI UI) {
    super(UI, null, false, false, 0, CATEGORIES);
  }
  
  
  static Button createButton(final BaseUI baseUI) {
    final CommercePane pane = new CommercePane(baseUI);
    final Button button = new Button(
      baseUI, COMMERCE_ICON, COMMERCE_ICON_LIT, "Finance and Legislation"
    ) {
      protected void whenClicked() {
        if (baseUI.currentPane() == pane) {
          baseUI.setInfoPanels(null, null);
        }
        else {
          baseUI.setInfoPanels(pane, null);
        }
      }
    };
    return button;
  }
  
  
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    super.updateText(UI, headerText, detailText, listingText);
    final Description d = detailText;
    if (category() == CAT_BUDGET) describeFinance(d);
    if (category() == CAT_DEMAND) describeDemands(d);
  }
  
  
  protected void describeFinance(Description d) {
    final Base base = UI.played();
    final BaseFinance BF = base.finance;
    
    d.append("FINANCE REPORT FOR "+base);
    int sumTI = 0, sumWI = 0, sumTO = 0, sumWO = 0, sumTB = 0, sumWB = 0;
    //
    //  First of all, we let the user determine what day-period to inspect:
    final Batch <Integer> periods = BF.periodIDs();
    if (! BF.hasPeriodRecord(periodShown)) periodShown = periods.last();

    final String showName = "Day "+periodShown;
    d.append("\nShow period: ");
    for (final int period : periods) d.append(
      new Description.Link("\n  Day "+period+" ") {
        public void whenClicked() { periodShown = period; }
      },
      (period == periodShown ? Colour.GREEN : Text.LINK_COLOUR)
    );
    //
    //  Then we list all income sources:
    d.append("\n\n  Income sources: ("+showName+"/total)");
    
    for (BaseFinance.Source key : BaseFinance.ALL_SOURCES) {
      final int
        period = (int) BF.periodIncome(key, periodShown),
        total  = (int) BF.totalIncome (key);
      if (period == 0 && total == 0) continue;
      d.append("\n    "+key+": "+period+"/"+total);
      sumWI += period ;
      sumTI += total;
    }
    d.append("\n    Total income: "+sumWI+"/"+sumTI);
    //
    //  And outlay sources-
    d.append("\n\n  Outlay sources: ("+showName+"/total)");
    for (BaseFinance.Source key : BaseFinance.ALL_SOURCES) {
      final int
        period = 0 - (int) BF.periodOutlay(key, periodShown),
        total  = 0 - (int) BF.totalOutlay (key);
      if (period == 0 && total == 0) continue;
      d.append("\n    "+key+": "+period+"/"+total);
      sumWO += period ;
      sumTO += total;
    }
    d.append("\n    Total outlays: "+sumWO+"/"+sumTO);
    //
    //  And finally, total balance-
    sumTB = sumTI - sumTO;
    sumWB = sumWI - sumWO;
    d.append("\n\n  Balance: "+sumWB+"/"+sumTB);
    d.append("\n  Current credit: "+(int) BF.credits());
    
  }
  
  
  protected void describeDemands(Description d) {
    final Base base = UI.played();
    final BaseCommerce BC = base.commerce;
    
    d.append("DEMAND REPORT FOR "+base);
    boolean noLocal = true, noTrade = true;
    
    d.append("\n\nLocal Goods: (supply/demand)");
    for (Traded t : Economy.ALL_MATERIALS) {
      final int
        demand = (int) BC.primaryDemand(t),
        supply = (int) BC.primarySupply(t);
      if (demand == 0 && supply == 0) continue;
      else noLocal = false;
      
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append(" "+t+": "+supply+"/"+demand);
    }
    if (noLocal) d.append("\n  No local goods.");

    Text.cancelBullet(d);
    d.append("\n\nReserved For Trade: (import/export)");
    for (Traded t : Economy.ALL_MATERIALS) {
      final int
        demand = (int) BC.importDemand(t),
        supply = (int) BC.exportSupply(t);
      if (demand == 0 && supply == 0) continue;
      else noTrade = false;
      
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append(" "+t+": "+demand+"/"+supply);
    }
    if (noTrade) d.append("\n  No trade goods.");
    
    Text.cancelBullet(d);
    d.append("\n\nOffworld Prices (Buy | Sell | Base)");
    for (Traded t : Economy.ALL_MATERIALS) {
      final String
        priceImp = I.shorten(BC.importPrice(t), 1),
        priceExp = I.shorten(BC.exportPrice(t), 1),
        baseCost = I.shorten(t.basePrice()    , 1);
      
      Text.insert(t.icon.asTexture(), 20, 20, true, d);
      d.append(" "+t+" (");
      d.append(priceImp+"", Colour.LITE_RED  );
      d.append(" | ");
      d.append(priceExp+"", Colour.LITE_GREEN);
      d.append(" | ");
      d.append(baseCost+"", Colour.LITE_BLUE );
      d.append(")");
    }
    
    Text.cancelBullet(d);
    d.append("\n\nTrading partners:");
    for (VerseLocation partner : BC.partners()) {
      d.append("\n  ");
      d.append(partner);
      d.append(" (Makes: ");
      for (Traded t : partner.goodsMade) {
        if (t.form != Economy.FORM_MATERIAL) continue;
        Text.insert(t.icon.asTexture(), 20, 20, false, d);
      }
      d.append(")");
      d.append(" (Needs: ");
      for (Traded t : partner.goodsNeeded) {
        if (t.form != Economy.FORM_MATERIAL) continue;
        Text.insert(t.icon.asTexture(), 20, 20, false, d);
      }
      d.append(")");
    }
    if (BC.partners().size() == 0) d.append("\n    No partners.");
  }
}











