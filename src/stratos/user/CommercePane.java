

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
    CAT_BUDGET = "BUDGETING",
    CAT_DEMAND = "MARKETS",
    CATEGORIES[] = { CAT_BUDGET, CAT_DEMAND };
  
  
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
    //headerText.setText("Finance and Legislation");
    final Description d = detailText;
    if (category() == CAT_BUDGET) describeFinance(d);
    if (category() == CAT_DEMAND) describeDemands(d);
  }
  
  
  protected void describeFinance(Description d) {
    final Base base = UI.played();
    final BaseFinance BF = base.finance;
    
    d.append("FINANCE REPORT FOR "+base);
    int sumTI = 0, sumWI = 0, sumTO = 0, sumWO = 0, sumTB = 0, sumWB = 0;
    
    d.append("\n\n  Income sources: (week/total)");
    for (String key : BF.incomeSources()) {
      final int
        week  = (int) BF.weekIncome (key),
        total = (int) BF.totalIncome(key);
      d.append("\n    "+key+": "+week+"/"+total);
      sumWI += week ;
      sumTI += total;
    }
    d.append("\n    Total income: "+sumWI+"/"+sumTI);
    
    d.append("\n\n  Outlay sources: (week/total)");
    for (String key : BF.outlaySources()) {
      final int
        week  = 0 - (int) BF.weekOutlay (key),
        total = 0 - (int) BF.totalOutlay(key);
      d.append("\n    "+key+": "+week+"/"+total);
      sumWO += week ;
      sumTO += total;
    }
    d.append("\n    Total outlays: "+sumWO+"/"+sumTO);
    
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
      
      ((Text) d).insert(t.icon.asTexture(), 20, 20, true);
      d.append(" "+t+": "+supply+"/"+demand);
    }
    if (noLocal) d.append("\n  No local goods.");

    ((Text) d).cancelBullet();
    d.append("\n\nReserved For Trade: (import/export)");
    for (Traded t : Economy.ALL_MATERIALS) {
      final int
        demand = (int) BC.importDemand(t),
        supply = (int) BC.exportSupply(t);
      if (demand == 0 && supply == 0) continue;
      else noTrade = false;
      
      ((Text) d).insert(t.icon.asTexture(), 20, 20, true);
      d.append(" "+t+": "+demand+"/"+supply);
    }
    if (noTrade) d.append("\n  No trade goods.");
    
    ((Text) d).cancelBullet();
    d.append("\n\nOffworld Prices (Buy | Sell | Base)");
    for (Traded t : Economy.ALL_MATERIALS) {
      final String
        priceImp = I.shorten(BC.importPrice(t), 1),
        priceExp = I.shorten(BC.exportPrice(t), 1),
        baseCost = I.shorten(t.basePrice()    , 1);
      
      ((Text) d).insert(t.icon.asTexture(), 20, 20, true);
      d.append(" "+t+" (");
      d.append(priceImp+"", Colour.LITE_RED  );
      d.append(" | ");
      d.append(priceExp+"", Colour.LITE_GREEN);
      d.append(" | ");
      d.append(baseCost+"", Colour.LITE_BLUE );
      d.append(")");
    }
    
    ((Text) d).cancelBullet();
    d.append("\n\nTrading partners:");
    for (Sector partner : BC.partners()) {
      d.append("\n  ");
      d.append(partner);
      d.append(" (Makes: ");
      for (Traded t : partner.goodsMade) {
        if (t.form != Economy.FORM_MATERIAL) continue;
        ((Text) d).insert(t.icon.asTexture(), 20, 20, false);
      }
      d.append(")");
      d.append(" (Needs: ");
      for (Traded t : partner.goodsNeeded) {
        if (t.form != Economy.FORM_MATERIAL) continue;
        ((Text) d).insert(t.icon.asTexture(), 20, 20, false);
      }
      d.append(")");
    }
    if (BC.partners().size() == 0) d.append("\n    No partners.");
  }
}











