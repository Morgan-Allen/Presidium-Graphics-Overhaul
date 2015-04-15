/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.civic.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;

import stratos.graphics.widgets.Text.Clickable;
import java.lang.reflect.*;



//  TODO:  Allow inclusion of images in the messages.
//  TODO:  Disable shipping until the player is ready.

//  TODO:  Require messages be actively dismissed before going to the 'old
//         messages' queue (and possibly before new messages can be presented?)
//  TODO:  Have a general flags/triggers table to clarify saving/loading.


//  TODO:  UNIFY TOPICS WITH REMINDER-ENTRIES AND/OR WITH MESSAGE-PANES

/*
*  More feedback on needs and settlement demands.  (Notifications or an RCI
   indicator of some sort are needed.)  You need feedback for-
     *  Citizen needs   / no food source (imports or grown.)
     *  Low funding     / no income source (exports or taxes.)
     *  Citizen housing / no holdings built.
     *  Specific goods  / no factory source built.
     *  Citizen health  / no medical team (sickbay and archives.)
     *  Danger          / no security force (walls and military.)
//*/


public class HelpScript {
  
  final static String
    SCRIPT_FILE = "media/Help/TutorialScript.xml";
  
  final TutorialScenario scenario;
  
  private class Topic {
    XML sourceNode;
    String title;
    MessagePane message;
    
    boolean urgent    = false;
    boolean triggered = false;
    Method trigger  = null;
    Method onOpen   = null;
  }
  
  final Table <String, Topic> allTopics = new Table <String, Topic> ();
  
  
  protected HelpScript(TutorialScenario tutorial) {
    this.scenario = tutorial;
    final XML xml = XML.load(SCRIPT_FILE);
    
    for (XML topicNode : xml.allChildrenMatching("topic")) {
      final Topic topic = loadTopic(topicNode);
      allTopics.put(topic.title, topic);
      I.say("\nADDED BASE TOPIC: "+topic.title);
    }
  }
  
  
  private Topic loadTopic(XML topicNode) {
    final Topic topic = new Topic();
    topic.sourceNode = topicNode;
    topic.title      = topicNode.value("name");
    topic.urgent     = topicNode.getBool("urgent");
    //
    //  Trigger-methods are used to decide when the topic in question should be
    //  presented to the player, and don't always need to be included.
    final String triggerName = topicNode.value("trigger");
    if (triggerName == null) topic.trigger = null;
    else try { topic.trigger = getClass().getDeclaredMethod(triggerName); }
    catch (Exception e) {
      I.say("\nWARNING:  No matching trigger method: "+triggerName);
      topic.trigger = null;
    }
    //
    //  On-open methods, as the name suggests, are called when a message is
    //  first viewed (and not before.)
    final String onOpenName = topicNode.value("onOpen");
    if (onOpenName == null) topic.onOpen = null;
    else try { topic.onOpen = getClass().getDeclaredMethod(onOpenName); }
    catch (Exception e) {
      I.say("\nWARNING:  No matching on-open method: "+onOpenName);
      topic.onOpen = null;
    }
    //
    //  We construct the message-panes themselves on demand, since the BaseUI
    //  might not have been initiated yet (see below.)
    return topic;
  }
  
  
  protected void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final String key = s.loadString();
      final Topic topic = allTopics.get(key);
      topic.triggered = s.loadBool();
    }
    this.loadAllFlags(s);
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveInt(allTopics.size());
    for (Topic t : allTopics.values()) {
      s.saveString(t.title);
      s.saveBool(t.triggered);
    }
    this.saveAllFlags(s);
  }
  
  
  protected MessagePane messageFor(String title) {
    
    final Topic topic = allTopics.get(title);
    if (topic == null) return null;
    if (topic.message != null) return topic.message;
    
    final String content = topic.sourceNode.child("content").content();
    final Batch <Clickable> links = new Batch <Clickable> ();

    
    //  I also want links to dismiss a message, and links to view all messages.
    //  But... those need to be added to all message-panels.
    
    //  Well, not dialogue panels.  Which should probably be a different class
    //  entirely?
    
    //  ...Yeah.  Dialogue panels need to be a different class.
    
    //  Also, image insertion!
    
    for (XML node : topic.sourceNode.children()) {
      if (node.tag().equals("content")) {
        
      }
      if (node.tag().equals("image")) {
        
      }
      if (node.tag().equals("link")) {
        final String linkKey  = node.value("name");
        final String linkName = node.content();
        final Topic  linked   = allTopics.get(linkKey);
        if (linked == null) {
          I.say("\n  WARNING: NO TOPIC MATCHING "+linkKey);
        }
        else links.add(new Description.Link(linkName) {
          public void whenClicked() {
            UI().reminders().retireMessage(topic.message);
            pushTopicMessage(linked, true);
          }
        });
      }
    }
    
    //  TODO:  I need a subclass of message-pane for this purpose.  One that
    //  will show time and date.
    
    //  TODO:  UNIFY TOPICS WITH MESSAGES.
    
    links.add(new Description.Link("Dismiss") {
      public void whenClicked() {
        UI().reminders().retireMessage(topic.message);
        UI().setInfoPanels(null, null);
      }
    });
    
    topic.message = new MessagePane(
      UI(), null, topic.title, null, scenario
    );
    topic.message.assignContent(content, links);
    return topic.message;
  }
  
  
  
  /**  Update methods for progress-conditions.
    */
  protected void checkForFlags() {
    //
    //  (We use an array here to allow deletions mid-iteration if something
    //  goes wrong, which java would not otherwise allow.)
    final Topic topics[] = new Topic[allTopics.size()];
    for (Topic topic : allTopics.values().toArray(topics)) {
      if (topic.trigger == null || topic.triggered) continue;
      
      boolean didTrigger = false;
      try {
        final Object triggerVal = topic.trigger.invoke(this);
        didTrigger = Boolean.TRUE.equals(triggerVal);
      }
      catch (Exception e) {
        I.report(e);
        allTopics.remove(topic.title);
      }
      
      if (didTrigger) {
        pushTopicMessage(topic, true);
      }
    }
  }
  
  
  private void pushTopicMessage(Topic topic, boolean viewNow) {
    final ReminderListing reminders = UI().reminders();
    final MessagePane message = messageFor(topic.title);
    if (message == null) return;
    topic.triggered = true;
    
    if (! reminders.hasMessageEntry(topic.title)) {
      reminders.addMessageEntry(message, topic.urgent);
      
      if (topic.onOpen != null) try { topic.onOpen.invoke(this); }
      catch (Exception e) { I.report(e); }
    }
    if (viewNow) UI().setInfoPanels(message, null);
  }
  
  
  
  /**  Trigger-methods that determine when to display certain topics (specified
    *  in the XML.)
    */
  private TrooperLodge    barracksBuilt = null;
  private EngineerStation foundryBuilt  = null;
  private SupplyDepot     depotBuilt    = null;
  private float startingBalance = -1;
  
  private Tile         startAt      = null;
  private ReconMission reconSent    = null;
  private Drone        droneAttacks = null;
  
  
  protected void loadAllFlags(Session s) throws Exception {
    barracksBuilt   = (TrooperLodge   ) s.loadObject();
    foundryBuilt    = (EngineerStation) s.loadObject();
    depotBuilt      = (SupplyDepot    ) s.loadObject();
    startingBalance = s.loadFloat();
    
    startAt      = (Tile        ) s.loadObject();
    reconSent    = (ReconMission) s.loadObject();
    droneAttacks = (Drone       ) s.loadObject();
  }
  
  
  protected void saveAllFlags(Session s) throws Exception {
    s.saveObject(barracksBuilt  );
    s.saveObject(foundryBuilt   );
    s.saveObject(depotBuilt     );
    s.saveFloat (startingBalance);
    
    s.saveObject(startAt     );
    s.saveObject(reconSent   );
    s.saveObject(droneAttacks);
  }
  
  
  protected boolean checkShowWelcome() {
    return true;
  }
  
  
  protected boolean checkMotionDone() {
    final Vec3D lookPoint = UI().rendering.view.lookedAt;
    final Tile  lookTile  = scenario.world().tileAt(lookPoint.x, lookPoint.y);
    if (lookTile == null) return false;
    
    if (startAt == null) startAt = lookTile;
    if (Spacing.distance(lookTile, startAt) < 4) return false;
    return true;
  }
  
  
  protected void onMotionDone() {
    scenario.base().intelMap.liftFogAround(scenario.bastion, 12);
    UI().tracking.lockOn(scenario.bastion);
  }
  
  
  protected boolean checkBuiltBarracks() {
    barracksBuilt = (TrooperLodge) firstBaseVenue(TrooperLodge.class);
    if (barracksBuilt == null) return false;
    
    onBuiltBarracks();
    return true;
  }
  
  
  protected void onBuiltBarracks() {
    barracksBuilt.structure.setState(Structure.STATE_INTACT, 1);
    scenario.base().setup.fillVacancies(barracksBuilt   , true);
    scenario.base().setup.fillVacancies(scenario.bastion, true);
    UI().tracking.lockOn(barracksBuilt);
  }
  
  
  protected boolean checkExploreBegun() {
    ReconMission match = null;
    for (Mission m : scenario.base().tactics.allMissions()) {
      if (m instanceof ReconMission) match = (ReconMission) m;
    }
    if (match == null) return false;
    
    reconSent = match;
    onExploreBegun();
    return true;
  }
  
  
  protected void onExploreBegun() {
    reconSent.assignPriority(Mission.PRIORITY_ROUTINE);
    for (Actor a : barracksBuilt.staff.workers()) {
      a.mind.assignMission(reconSent);
      break;
    }
  }
  
  
  protected boolean checkFacilitiesPlaced() {
    foundryBuilt = (EngineerStation) firstBaseVenue(EngineerStation.class);
    depotBuilt = (SupplyDepot) firstBaseVenue(SupplyDepot.class);
    if (foundryBuilt == null || depotBuilt == null) return false;
    
    onFacilitiesPlaced();
    return true;
  }
  
  
  protected void onFacilitiesPlaced() {
    depotBuilt  .structure.setState(Structure.STATE_INSTALL, 0.5f);
    foundryBuilt.structure.setState(Structure.STATE_INSTALL, 0.5f);
    
    for (Actor a : scenario.bastion.staff.workers()) {
      if (a.mind.vocation() == Backgrounds.TECHNICIAN) {
        final Repairs build = new Repairs(a, depotBuilt);
        build.addMotives(Plan.MOTIVE_JOB, Plan.PARAMOUNT);
        a.mind.assignBehaviour(build);
      }
    }
  }
  
  
  protected boolean checkFacilitiesReady() {
    if (depotBuilt == null || foundryBuilt == null) return false;
    if (! depotBuilt.structure.intact()) return false;
    if (! foundryBuilt.structure.intact()) return false;
    
    onFacilitiesReady();
    return true;
  }
  
  
  protected void onFacilitiesReady() {
    scenario.base().setup.fillVacancies(depotBuilt  , true);
    scenario.base().setup.fillVacancies(foundryBuilt, true);
  }
  
  
  protected boolean checkTradeSetup() {
    if (depotBuilt == null) return false;
    final Stocks DS = depotBuilt.stocks;
    final Traded imp = Economy.ORES, exp = Economy.PARTS;
    if (DS.demandFor(imp) == 0 || DS.producer(imp) == true ) return false;
    if (DS.demandFor(exp) == 0 || DS.producer(exp) == false) return false;
    
    return true;
  }
  
  
  protected void onBaseAttackOpen() {
    final Base artilects = Base.artilects(world());
    droneAttacks = (Drone) Drone.SPECIES.sampleFor(artilects);
    
    Tile entry = Spacing.pickRandomTile(barracksBuilt, 6, world());
    entry = Spacing.nearestOpenTile(entry, entry);
    droneAttacks.enterWorldAt(entry, world());
    
    final Combat assault = new Combat(droneAttacks, barracksBuilt);
    assault.addMotives(Plan.MOTIVE_EMERGENCY, 100);
    droneAttacks.mind.assignBehaviour(assault);
    UI().selection.pushSelection(droneAttacks);
    
    barracksBuilt.structure.addUpgrade(TrooperLodge.VOLUNTEER_STATION);
    barracksBuilt.structure.addUpgrade(TrooperLodge.MARKSMAN_TRAINING);
    barracksBuilt.structure.addUpgrade(TrooperLodge.TROOPER_STATION  );
    
    final Base base = scenario.base();
    for (int n = 3; n-- > 0;) {
      final Actor applies = Backgrounds.TROOPER.sampleFor(base);
      base.commerce.addCandidate(applies, barracksBuilt, Backgrounds.TROOPER);
    }
  }
  
  
  protected boolean checkDroneDestroyed() {
    if (droneAttacks == null) return false;
    if (droneAttacks.health.conscious()) return false;
    return true;
  }
  
  
  protected boolean checkRuinsDestroyed() {
    if (scenario.ruins.structure.intact()) return false;
    return true;
  }
  
  
  protected boolean checkHousingAndVenueUpgrade() {
    if (foundryBuilt == null) return false;
    if (! foundryBuilt.structure.hasUpgrade(EngineerStation.ASSEMBLY_LINE)) {
      return false;
    }
    
    boolean anyHU = false;
    for (Holding h : allBaseHoldings()) {
      if (h.upgradeLevel() > 0) anyHU = true;
    }
    if (! anyHU) return false;
    return true;
  }
  
  
  protected boolean checkPositiveCashFlow() {
    if (startingBalance == -1) return false;
    final float balance = scenario.base().finance.credits();
    if (balance < startingBalance + 1000) return false;
    return true;
  }
  
  
  
  /**  Other helper methods-
    */
  private Venue firstBaseVenue(Class venueClass) {
    for (Object o : scenario.world().presences.matchesNear(
      venueClass, null, -1
    )) {
      final Venue found = (Venue) o;
      if (found.base() == scenario.base()) return found;
    }
    return null;
  }
  
  
  private Batch <Holding> allBaseHoldings() {
    final Batch <Holding> all = new Batch <Holding> ();
    for (Object o : scenario.world().presences.matchesNear(
      Holding.class, null, -1
    )) {
      final Holding h = (Holding) o;
      if (h.base() == scenario.base()) all.add(h);
    }
    return all;
  }
  
  
  private Stage world() {
    return scenario.world();
  }
  
  
  private BaseUI UI() {
    return scenario.UI();
  }
}


    
    /*
    for (String key : BIG_KEYS) if (allTopics.get(key) == null) I.say(
      "\nWARNING: No matching topic found for "+key+" in "+SCRIPT_FILE
    );
    //*/

  
  /*
  final static List <String> BIG_KEYS = new List <String> ();
  
  
  final static String
    EVENT_WELCOME         = BIG_KEYS.pass("Welcome!"                   ),
    EVENT_SECURITY_DONE   = BIG_KEYS.pass("Security Objective Complete"),
    EVENT_CONTACT_DONE    = BIG_KEYS.pass("Contact Objective Complete" ),
    EVENT_ECONOMY_DONE    = BIG_KEYS.pass("Economy Objective Complete" ),
    EVENT_CONGRATULATIONS = BIG_KEYS.pass("Tutorial Complete!"         );
  //*/



    
    /*
    UI = tutorial.UI();
    pushMessage(EVENT_WELCOME, true, true);
    
    checkMotion();
    if (motionDone && pushMessage("Navigation Done", true, true)) {
    }
    
    if (isViewing("The Bastion")) {
      tutorial.base().intelMap.liftFogAround(tutorial.bastion, 12);
      UI.tracking.lockOn(tutorial.bastion);
      pushMessage("Defences", true, false);
    }
    
    checkBuildBarracks();
    if (barracksDone && pushMessage("Trooper Lodge Built", true, true)) {
      barracksBuilt.structure.setState(Structure.STATE_INTACT, 1);
      tutorial.base().setup.fillVacancies(barracksBuilt, true);
      pushMessage("Finding Threats", true, false);
    }
    
    checkExploreDone();
    if (exploreDone && pushMessage("Recon Mission Begun", true, true)) {
      reconSent.assignPriority(Mission.PRIORITY_ROUTINE);
      for (Actor a : barracksBuilt.staff.workers()) {
        a.mind.assignMission(reconSent);
      }
    }
    
    //
    //  Next:
    //    *  Strike Missions and recruiting.
    //    *  Build a stock exchange and engineer station.  Hiring and Upgrades.
    //    *  Housing, trade revenue and taxation.
    
    //
    //  Secondary tutorial:
    //    *  Security and Contact missions.
    //    *  Other guilds and schools.
    //    *  Offworld missions.
    //*/




/*
private boolean isViewing(String messageKey) {
  return messageKey.equals(UI().reminders().onScreenMessageKey());
}
//*/



/*
protected boolean checkFood() {
  return false;
}


protected boolean checkFunding() {
  return false;
}


protected boolean checkSafety() {
  return false;
}


protected boolean checkHealth() {
  return false;
}


protected boolean checkHousing(int houseLevel) {
  return false;
}


protected boolean checkSupply(Traded traded) {
  return false;
}
//*/