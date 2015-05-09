

package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;


//  TODO:  There probably need to be some dedicated UI classes for this.

//  Provide a hyperlink system for help on different topics.

//  Notify the player when deaths occur, and for what reason.

//  TODO:  You should list the exact structures (or at least structure-types)
//         that are demanding particular goods.



public class BaseAdvice {

  
  final public static int
    LEVEL_TOTAL   =  2,
    LEVEL_ADVISOR =  1,
    LEVEL_NONE    =  0;
  
  public static enum Topic {
    HUNGER_WARNING  ("No Food!"),
    HUNGER_SEVERE   ("Hunger"),
    SICKNESS_WARNING("No Doctors"),
    SICKNESS_SEVERE ("Sickness"),
    MORALE_WARNING  ("No Entertainment"),
    MORALE_SEVERE   ("Discontent"),
    DANGER_WARNING  ("Poor Security"),
    DANGER_SEVERE   (""),
    EXPENSE_WARNING ("No Exports!"),
    EXPENSE_SEVERE  (""),
    NEEDS_WARNING   ("Shortages"),
    NEEDS_SEVERE    (""),
    
    NEED_BUILDERS   ("No Technicians!"),
    NEED_ADMIN      ("No Auditors!"),
    
    BASE_ATTACK     (""),
    CASUALTIES      ("");
    
    final String label;
    Topic(String label) { this.label = label; }
  };
  final static XML textXML = XML.load("media/Help/GameHelp.xml");
  
  final Base base;
  private int controlLevel = LEVEL_ADVISOR;
  
  private float
    numNoFood = 0,
    numHungry = 0,
    numSick   = 0,
    numSad    = 0,
    numSecret = 0;
  private boolean
    needExports,
    needSafety ,
    needsTechs ,
    needsAdmin ;
  private Batch <Traded> shortages = new Batch <Traded> ();
  
  
  
  
  public BaseAdvice(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    this.controlLevel = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(controlLevel);
  }
  
  
  public void setControlLevel(int level) {
    this.controlLevel = level;
  }
  
  
  public void updateAdvice(int numUpdates) {
    if (numUpdates % 10 != 0) return;
    
    summariseDefenceNeeds();
    summariseEconomyNeeds();
    summariseCitizenNeeds();
  }
  
  
  private void summariseDefenceNeeds() {
    needSafety = false;
    
    if (base.tactics.forceStrength() < 25) {
      needSafety = true;
    }
  }
  
  
  private void summariseEconomyNeeds() {
    needExports = false;
    shortages.clear();
    
    if (base.commerce.exportSupply.size() == 0) {
      needExports = true;
    }
    for (Traded t : Economy.ALL_MATERIALS) {
      if (Visit.arrayIncludes(Economy.ALL_FOOD_TYPES, t)) continue;
      if (base.commerce.primaryShortage(t) < 0.5f) continue;
      if (base.commerce.primaryDemand  (t) < 5   ) continue;
      shortages.add(t);
    }
    for (Traded t : Economy.ALL_PROVISIONS) {
      if (base.commerce.primaryShortage(t) < 0.5f) continue;
      if (base.commerce.primaryDemand  (t) < 5   ) continue;
      shortages.add(t);
    }
  }
  
  
  private void summariseCitizenNeeds() {
    int total = 0;
    needsTechs = true;
    needsAdmin = true;
    numNoFood = 0;
    numHungry = 0;
    numSick   = 0;
    numSad    = 0;
    numSecret = 0;
    
    for (Profile p : base.profiles.allProfiles.values()) {
      final Actor      citizen = p.actor;
      final Background job     = citizen.mind.vocation();
      final Property   home    = citizen.mind.home();
      total++;
      numHungry += citizen.health.hungerLevel();
      numHungry += citizen.health.nutritionLevel() - 1;
      numSad    += 1 - citizen.health.moraleLevel();
      
      for (Condition c : Conditions.ALL_CONDITIONS) {
        numSick += citizen.traits.relativeLevel(c);
      }
      
      boolean hasFood = false;
      if (home instanceof Venue) for (Traded t : Economy.ALL_FOOD_TYPES) {
        if (home.inventory().amountOf(t) > 0) hasFood = true;
      }
      if (! hasFood) numNoFood++;
      
      if (job == TECHNICIAN || job == ARTIFICER         ) needsTechs = false;
      if (job == AUDITOR || job == MINISTER_FOR_ACCOUNTS) needsAdmin = false;
      
      if (p.daysSincePsychEval(base.world) > Stage.DAYS_PER_WEEK) {
        numSecret += 1;
      }
    }
    
    if (total == 0) return;
    numHungry /= total;
    numNoFood /= total;
    numSick   /= total;
    numSad    /= total;
    numSecret /= total;
  }
  
  
  public Batch <Topic> adviceTopics() {
    final Batch <Topic> topics = new Batch <Topic> ();
    
    if (numNoFood > 0.5f   ) topics.add(Topic.HUNGER_WARNING );
    if (numHungry > 0.5f   ) topics.add(Topic.HUNGER_SEVERE  );
    if (numSick   > 0.25f  ) topics.add(Topic.SICKNESS_SEVERE);
    if (numSad    > 0.75f  ) topics.add(Topic.MORALE_SEVERE  );
    
    if (needsTechs         ) topics.add(Topic.NEED_BUILDERS  );
    if (needsAdmin         ) topics.add(Topic.NEED_ADMIN     );
    if (needExports        ) topics.add(Topic.EXPENSE_WARNING);
    if (needSafety         ) topics.add(Topic.DANGER_WARNING );
    
    if (! shortages.empty()) topics.add(Topic.NEEDS_WARNING  );
    
    return topics;
  }
  
  
  public MessagePane configAdvicePanel(
    MessagePane pane, Object topic, BaseUI UI
  ) {
    if (! (topic instanceof Topic)) return null;
    
    final Topic about = (Topic) topic;
    final String title = about.label;
    if (pane == null) pane = new MessagePane(UI, null, title, null, null);
    
    if (topic == Topic.HUNGER_WARNING ) configNeedsSummary(
      pane, UI, about, Economy.ALL_FOOD_TYPES
    );
    if (topic == Topic.HUNGER_SEVERE  ) configNeedsSummary(
      pane, UI, about, Economy.ALL_FOOD_TYPES
    );
    if (topic == Topic.MORALE_SEVERE  ) assignXML(pane, about);
    if (topic == Topic.SICKNESS_SEVERE) assignXML(pane, about);
    
    if (topic == Topic.NEED_BUILDERS  ) assignXML(pane, about);
    if (topic == Topic.NEED_ADMIN     ) assignXML(pane, about);
    if (topic == Topic.EXPENSE_WARNING) assignXML(pane, about);
    if (topic == Topic.DANGER_WARNING ) assignXML(pane, about);
    
    if (topic == Topic.NEEDS_WARNING  ) configNeedsSummary(
      pane, UI, about, shortages.toArray(Traded.class)
    );
    return pane;
  }
  
  
  private void assignXML(MessagePane pane, Topic topic) {
    final XML topicXML = textXML.matchChildValue("name", topic.label);
    String content = topicXML.child("content").content();
    pane.detail().setText(content);
  }
  
  
  private void configNeedsSummary(
    final MessagePane pane, final BaseUI UI,
    Topic topic, Traded... shortages
  ) {
    assignXML(pane, topic);
    final Description d = pane.detail();
    d.append("\n");
    for (final Traded o : shortages) {
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
  }
  
  
  private MessagePane messageForNeed(Traded t, BaseUI UI) {
    
    final String titleKey = "Need "+t;
    final MessagePane pane = new MessagePane(
      UI, null, titleKey, null, null
    );
    
    pane.header().setText("Shortage of "+t);
    
    final Description d = pane.detail();
    if (t instanceof Traded) {
      final String help = ((Traded) t).description;
      d.append(t+": ", Colour.LITE_GREY);
      if (help == null) d.append("(No description)", Colour.LITE_GREY);
      else d.append(help, Colour.LITE_GREY);
      d.append("\n\n");
    }
    
    final float need = base.commerce.primaryShortage(t);
    final int percent = (int) (need * 100);
    d.append("Shortage: "+percent+"%\n\n");
    
    final Batch <Blueprint>
      canMake = new Batch <Blueprint> (),
      canUse  = new Batch <Blueprint> ();
    for (Blueprint b : base.setup.canPlace) {
      if (b.category == UIConstants.TYPE_HIDDEN) continue;
      else if (b.producing(t) != null) canMake.include(b);
      else if (b.consuming(t) != null) canUse .include(b);
    }
    
    if (canUse.size() > 0) {
      d.append("This commodity is used by the following facilities:");
      for (Blueprint b : canUse) d.append("\n  "+b.name);
      d.append("\n\n");
    }
    
    for (Blueprint match : canMake) {
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
      
      if (canMake.size() > 0) {
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



