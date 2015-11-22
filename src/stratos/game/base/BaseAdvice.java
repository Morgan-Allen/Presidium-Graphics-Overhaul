/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.Text;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;


//  TODO:  There probably need to be some dedicated UI classes for this.

//  Notify the player when deaths occur, and for what reason.

//  TODO:  You should list the exact structures (or at least structure-types)
//         that are demanding particular goods!



public class BaseAdvice {
  
  
  final public static int
    LEVEL_FULL_AUTO      =  2,
    LEVEL_AUTO_ADVISE    =  1,
    LEVEL_AUTO_NO_ADVICE =  0,
    LEVEL_NO_AUTO        = -1;
  final static int
    WARNING_SLOW  = Stage.STANDARD_DAY_LENGTH,
    WARNING_FAST  = Stage.STANDARD_HOUR_LENGTH,
    NO_WARN_DELAY = 0;
  
  public static enum Topic {
    HUNGER_WARNING  ("No Food!"        , WARNING_SLOW ),
    HUNGER_SEVERE   ("Hunger"          , WARNING_FAST ),
    SICKNESS_WARNING("No Doctors"      , WARNING_SLOW ),
    SICKNESS_SEVERE ("Sickness"        , WARNING_FAST ),
    MORALE_WARNING  ("No Entertainment", WARNING_SLOW ),
    MORALE_SEVERE   ("Discontent"      , WARNING_FAST ),
    DANGER_WARNING  ("Poor Security"   , WARNING_SLOW ),
    DANGER_SEVERE   (""                , WARNING_FAST ),
    EXPORTS_WARNING ("No Exports!"     , WARNING_SLOW ),
    EXPENSE_SEVERE  (""                , WARNING_FAST ),
    NEEDS_WARNING   ("Shortages"       , WARNING_SLOW ),
    NEEDS_SEVERE    (""                , WARNING_FAST ),
    
    NEED_BUILDERS   ("No Technicians!" , WARNING_FAST ),
    NEED_ADMIN      ("No Auditors!"    , WARNING_FAST ),
    NEED_LANDING    ("No Landing Site!", WARNING_FAST ),
    
    BASE_ATTACK     (""                , NO_WARN_DELAY),
    CASUALTIES      (""                , NO_WARN_DELAY);
    
    final String label;
    final int warnDelay;
    
    Topic(String label, int warnDelay) {
      this.label = label;
      this.warnDelay = warnDelay;
    }
  };
  final static XML textXML = XML.load("media/Help/GameHelp.xml");
  
  final Base base;
  private int autonomy = LEVEL_AUTO_ADVISE;
  
  private float
    numNoFood = 0,
    numHungry = 0,
    numSick   = 0,
    numSad    = 0,
    numSecret = 0;
  private boolean
    needExports,
    needLanding,
    needSafety ,
    needsTechs ,
    needsAdmin ;
  
  private Batch <Traded> shortages = new Batch <Traded> ();
  private Table <Traded, Batch <Blueprint>> demanding = new Table();
  private Table <Topic, Float> topicDates = new Table <Topic, Float> ();
  
  
  
  
  public BaseAdvice(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    this.autonomy = s.loadInt();
    for (Topic t : Topic.values()) {
      final float date = s.loadFloat();
      if (date >= 0) topicDates.put(t, date);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(autonomy);
    for (Topic t : Topic.values()) {
      final Float date = topicDates.get(t);
      s.saveFloat(date == null ? -1 : date);
    }
  }
  
  
  public void setAutonomy(int level) {
    this.autonomy = level;
  }
  
  
  public int autonomy() {
    return autonomy;
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
    needLanding = false;
    shortages.clear();
    
    final Verse universe = base.world.offworld;
    final VerseLocation locale = universe.stageLocation();
    for (Dropship ship : universe.journeys.allDropships()) {
      if (! universe.journeys.dueToArrive(ship, locale)) continue;
      if (ship.dropPoint() == null) needLanding = true;
    }
    
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
      
      for (Condition c : Condition.ALL_CONDITIONS) {
        numSick += citizen.traits.relativeLevel(c);
      }
      
      boolean hasFood = false;
      if (home instanceof Venue) for (Traded t : Economy.ALL_FOOD_TYPES) {
        if (home.inventory().amountOf(t) > 0) hasFood = true;
      }
      if (! hasFood) numNoFood++;
      
      if (job == TECHNICIAN || job == ARTIFICER         ) needsTechs = false;
      if (job == AUDITOR || job == MINISTER_FOR_ACCOUNTS) needsAdmin = false;
      
      /*
      if (p.daysSincePsychEval(base.world) > Stage.DAYS_PER_WEEK) {
        numSecret += 1;
      }
      //*/
    }
    
    if (total == 0) return;
    numHungry /= total;
    numNoFood /= total;
    numSick   /= total;
    numSad    /= total;
    numSecret /= total;
  }
  
  
  public Series <Topic> adviceTopics() {
    //
    //  We first compile a list of everything one might complain about.
    final List <Topic> topics = new List <Topic> ();
    if (GameSettings.noAdvice || autonomy == LEVEL_AUTO_NO_ADVICE) {
      return topics;
    }
    
    if (numNoFood > 0.5f   ) topics.add(Topic.HUNGER_WARNING );
    if (numHungry > 0.5f   ) topics.add(Topic.HUNGER_SEVERE  );
    if (numSick   > 0.25f  ) topics.add(Topic.SICKNESS_SEVERE);
    if (numSad    > 0.75f  ) topics.add(Topic.MORALE_SEVERE  );
    
    if (needsTechs         ) topics.add(Topic.NEED_BUILDERS  );
    if (needsAdmin         ) topics.add(Topic.NEED_ADMIN     );
    if (needSafety         ) topics.add(Topic.DANGER_WARNING );
    
    if (needExports        ) topics.add(Topic.EXPORTS_WARNING);
    if (needLanding        ) topics.add(Topic.NEED_LANDING   );
    
    if (! shortages.empty()) topics.add(Topic.NEEDS_WARNING  );
    //
    //  We screen out any warnings that haven't been present longer than the
    //  warning-delay associated.
    final Batch <Topic> outDated = new Batch <Topic> ();
    for (Topic t : topicDates.keySet()) {
      if (! topics.includes(t)) outDated.add(t);
    }
    for (Topic t : outDated) topicDates.remove(t);
    //
    //  And record the dates of first appearance for any new problems-
    final float newDate = base.world.currentTime();
    for (Topic t : topics) {
      Float date = topicDates.get(t);
      if (date == null) topicDates.put(t, date = newDate);
      if (newDate - date < t.warnDelay) topics.remove(t);
    }
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
    if (topic == Topic.DANGER_WARNING ) assignXML(pane, about);

    if (topic == Topic.EXPORTS_WARNING) assignXML(pane, about);
    if (topic == Topic.NEED_LANDING   ) assignXML(pane, about);
    
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
          final MessagePane help = messageForNeed(o, UI, pane);
          UI.setMessagePane(help);
        }
      });
    }
  }
  
  
  //  TODO:- USE TOPICS FOR THESE!
  
  
  private MessagePane messageForNeed(
    final Traded t, final BaseUI UI, final MessagePane before
  ) {
    
    final String titleKey = "Need "+t;
    final MessagePane pane = new MessagePane(UI, null, titleKey, null, null);
    
    pane.header().setText("Shortage of "+t);
    
    final Description d = pane.detail();

    final String help = ((Traded) t).description;
    d.append(t+": ", Colour.LITE_GREY);
    if (help == null) d.append("(No description)", Colour.LITE_GREY);
    else d.append(help, Colour.LITE_GREY);
    d.append("\n");
    
    final float need = base.commerce.primaryShortage(t);
    final int percent = (int) (need * 100);
    d.append("Shortage: "+percent+"%\n");
    
    final Batch <Blueprint>
      canMake = new Batch <Blueprint> (),
      canUse  = new Batch <Blueprint> (),
      canImp  = new Batch <Blueprint> ();
    
    for (Blueprint b : base.setup.available()) {
      if (b.category == Target.TYPE_WIP) continue;
      else if (b.producing(t) != null) canMake.include(b);
      else if (b.consuming(t) != null) canUse .include(b);
    }
    if (Visit.arrayIncludes(StockExchange.ALL_STOCKED, t)) {
      canImp.add(StockExchange.BLUEPRINT);
    }
    if (Visit.arrayIncludes(SupplyDepot.ALL_STOCKED, t)) {
      canImp.add(SupplyDepot.BLUEPRINT);
    }
    
    if (canUse.size() > 0) {
      d.appendList("\nConsmed by:", canUse.toArray());
    }
    if (canMake.size() > 0) {
      d.appendList("\nProduced by:", canMake.toArray());
    }
    if (canImp.size() > 0) {
      d.appendList("\nCan import at:", canImp.toArray());
    }
    
    if (before != null) d.append(new Description.Link("\n\n  Back") {
      public void whenClicked() {
        UI.setMessagePane(before);
      }
    });
    
    return pane;
  }
  
  
  
  /**  Other miscellaneous topics-
    */
  final static MessageTopic TOPIC_CASUALTY = new MessageTopic(
    "topic_casualty", true, Mobile.class, String.class
  ) {
    protected void configMessage(final BaseUI UI, Text d, Object... args) {
      final Mobile killed = (Mobile) args[0];
      final String cause  = (String) args[1];
      d.appendAll(killed, " has died from "+cause+".");
    }
  };
  
  
  public void sendCasualtyMessageFromInjury(Actor a) {
    if (base != BaseUI.currentPlayed()) return;
    TOPIC_CASUALTY.dispatchMessage("Casualty: "+a, a, "injuries");
  }
  
  
  public void sendCasualtyMessageFromDisease(Actor a) {
    if (base != BaseUI.currentPlayed()) return;
    TOPIC_CASUALTY.dispatchMessage("Casualty: "+a, a, "disease");
  }
  
  
  public void sendCasualtyMessageFromStarvation(Actor a) {
    if (base != BaseUI.currentPlayed()) return;
    TOPIC_CASUALTY.dispatchMessage("Casualty: "+a, a, "starvation");
  }
  
  
  public void sendCasualtyMessageFromOldAge(Actor a) {
    if (base != BaseUI.currentPlayed()) return;
    TOPIC_CASUALTY.dispatchMessage("Casualty: "+a, a, "old age");
  }
  
  
  final static MessageTopic TOPIC_ARRIVALS = new MessageTopic(
    "topic_arrivals", false, Mobile.class, VerseLocation.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      final Mobile        arrived = (Mobile       ) args[0];
      final VerseLocation origin  = (VerseLocation) args[1];
      d.appendAll(arrived, " has arrived from ");
      if (origin == null) d.append("offworld.");
      else d.appendAll(origin, ".");
    }
  };
  
  
  public void sendArrivalMessage(Mobile arrived, VerseLocation from) {
    if (base != BaseUI.currentPlayed()) return;
    TOPIC_ARRIVALS.dispatchMessage("Arrival: "+arrived, arrived, from);
  }
}






