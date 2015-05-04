

package stratos.game.base;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;


public class BaseAdvice {
  
  
  
  public static MessagePane configNeedsSummary(
    final MessagePane pane, final Base played, final BaseUI UI
  ) {
    pane.header().setText("Shortages!");
    pane.detail().setText(
      "Your base is short of the following goods or services.  Click on the "+
      "links below for tips on how to fill the demand.\n"
    );
    final Description d = pane.detail();
    
    for (final Object o : played.setup.needSatisfaction()) {
      d.append("\n  ");
      d.append(new Description.Link(o.toString()) {
        public void whenClicked() {
          final MessagePane help = BaseAdvice.messageForNeed(o, played, UI);
          //
          //  We include a 'back' link for returning to the main panel.
          //  TODO:  MAKE THIS A BASIC FUNCTION OF INFO-PANES
          help.detail().append(new Description.Link("\n  Back") {
            public void whenClicked() { UI.setInfoPanels(pane, null); }
          });
          UI.setInfoPanels(help, null);
        }
      });
    }
    return pane;
  }
  
  
  public static MessagePane messageForNeed(Object t, Base base, BaseUI UI) {
    
    final String titleKey = "Need "+t;
    final MessagePane pane = new MessagePane(
      UI, null, titleKey, null, null
    );
    
    final Batch <Blueprint> canMatch = new Batch <Blueprint> ();
    for (Blueprint blueprint : base.setup.canPlace) {
      if (blueprint.category == UIConstants.TYPE_HIDDEN) continue;
      if (blueprint.producing(t) != null) canMatch.include(blueprint);
    }
    
    pane.header().setText("Shortage of "+t);
    final Description d = pane.detail();
    
    if (t instanceof Traded) {
      final String help = ((Traded) t).description;
      d.append(t+": ", Colour.LITE_GREY);
      if (help == null) d.append("(No description)", Colour.LITE_GREY);
      else d.append(help, Colour.LITE_GREY);
      d.append("\n\n");
    }
    
    for (Blueprint match : canMatch) {
      final Conversion s = match.producing(t);
      
      if (s.raw.length > 0) {
        d.append("Consider building a "+match.name+", which converts ");
        for (Item i : s.raw) d.append(i.type+" ");
        d.append("to "+s.out.type+".");
      }
      else {
        d.append("Consider building a "+match.name+", which provides "+t+".");
      }
      
      final String category = InstallationPane.categoryFor(match);
      if (category != null) {
        d.append("\n  Category: "+category+" Structures", Colour.LITE_GREY);
      }
      
      if (match.required.length > 0) for (Blueprint req : match.required) {
        if (base.listInstalled(req, true).size() > 0) continue;
        d.append("\n  Requires: "+req.name, Colour.LITE_GREY);
      }
      d.append("\n\n");
    }
    
    if (Visit.arrayIncludes(Economy.ALL_MATERIALS, t)) {
      
      if (canMatch.size() > 0) {
        d.append("Alternatively, you could import this good at a ");
      }
      else d.append("You could import this good at a ");
      
      if (Visit.arrayIncludes(StockExchange.ALL_STOCKED, t)) {
        d.append("Supply Depot or Stock Exchange");
      }
      else d.append("Supply Depot");
      
      d.append("\n\n");
    }
    
    return pane;
  }
}





