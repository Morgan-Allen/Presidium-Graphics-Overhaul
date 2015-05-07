

package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;


//  TODO:  There probably need to be some dedicated UI classes for this.
//         Return multiple panes for different kinds of shortage.

public class BaseAdvice {

  
  final public static int
    LEVEL_TOTAL   =  2,
    LEVEL_ADVISOR =  1,
    LEVEL_NONE    =  0;
  
  final Base base;
  private int controlLevel = LEVEL_ADVISOR;
  
  
  public BaseAdvice(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    this.controlLevel = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(controlLevel);
  }
  
  
  
  /**  Basic access and modifier methods-
    */
  public void setControlLevel(int level) {
    this.controlLevel = level;
  }
  
  
  
  /**  Methods for handling shortages of certain other goods and services,
    *  based on citizen desires-
    */
  private void summariseCitizenNeeds() {
    
    float numHungry = 0, numSick = 0, numLackPsych = 0;
    
    for (Profile p : base.profiles.allProfiles.values()) {
      final Actor citizen = p.actor;
      numHungry += citizen.health.hungerLevel();
      numHungry += citizen.health.nutritionLevel() - 1;
      
      for (Condition c : Conditions.ALL_CONDITIONS) {
        numSick += citizen.traits.traitLevel(c);
      }
      
      //  TODO:  Integrate this later...
      if (p.daysSincePsychEval(base.world) > Stage.DAYS_PER_WEEK) {
        numLackPsych += 1;
      }
    }
    
    
  }
  
  
  
  
  
  
  /**  Methods for handling shortages of different types of good:
    */
  public Object[] venueGoodNeeds() {
    final List <Object> needs = new List <Object> () {
      protected float queuePriority(Object r) { return needRating(r); }
    };
    if (controlLevel == LEVEL_NONE || GameSettings.noAdvice) {
      return needs.toArray();
    }
    
    for (Traded t : Economy.ALL_PROVISIONS) {
      if (base.commerce.primaryShortage(t) < 0.5f) continue;
      if (base.commerce.primaryDemand  (t) < 5   ) continue;
      needs.add(t);
    }
    for (Traded t : Economy.ALL_MATERIALS) {
      if (base.commerce.primaryShortage(t) < 0.5f) continue;
      if (base.commerce.primaryDemand  (t) < 5   ) continue;
      needs.add(t);
    }
    for (Traded t : Economy.ALL_SERVICES) {
      if (base.demands.globalShortage(t) < 0.5f) continue;
      if (base.demands.globalDemand  (t) < 5   ) continue;
      needs.add(t);
    }
    return needs.toArray();
  }
  
  
  public float needRating(Object need) {
    if (controlLevel == LEVEL_NONE || GameSettings.noAdvice) {
      return 0;
    }
    if (need instanceof Traded) {
      return base.commerce.primaryShortage((Traded) need);
    }
    return base.demands.globalShortage(need);
  }
  
  
  public MessagePane configNeedsSummary(
    final MessagePane pane, final BaseUI UI
  ) {
    pane.header().setText("Shortages!");
    pane.detail().setText(
      "Your base is short of the following goods or services.  Click on the "+
      "links below for tips on how to fill the demand.\n"
    );
    final Description d = pane.detail();
    
    for (final Object o : venueGoodNeeds()) {
      d.append("\n  ");
      d.append(new Description.Link(o.toString()) {
        public void whenClicked() {
          final MessagePane help = messageForNeed(o, UI);
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
  
  
  public MessagePane messageForNeed(Object t, BaseUI UI) {
    
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









