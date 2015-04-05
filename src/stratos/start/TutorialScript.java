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



/*

*  Use a linear sequence with much more detail on specific, simple steps.  You
   want to cover-
   * Basic Navigation and UI Features (access to game inputs and readouts.)
   * Building & Upgrades (space management.)
   * Recruitment & Missions (indirect control, motivation.)
   * Inital objectives (security, basic housing, turning a profit.)
   
   Just don't railroad the player if they want to wander off and explore the
   game's mechanics themselves.

*  More feedback on needs and settlement demands.  (Notifications or an RCI
   indicator of some sort are needed.)  You need feedback for-
     *  Citizen needs   / no food source (imports or grown.)
     *  Low funding     / no income source (exports or taxes.)
     *  Citizen housing / no holdings built.
     *  Specific goods  / no factory source built.
     *  Citizen health  / no medical team (sickbay and archives.)
     *  Danger          / no security force (walls and military.)
   Emphasise the emotive component here?
//*/


public class TutorialScript {
  
  final static String
    SCRIPT_FILE = "media/Help/TutorialScript.xml";
  
  final TutorialScenario tutorial;
  
  private class Topic {
    XML sourceNode;
    String title;
    DialoguePane message;
    
    boolean urgent  = false;
    Method trigger  = null ;
    boolean didPush = false;
  }
  
  final Table <String, Topic> allTopics = new Table <String, Topic> ();
  
  
  protected TutorialScript(TutorialScenario tutorial) {
    this.tutorial = tutorial;
    final XML xml = XML.load(SCRIPT_FILE);

    I.say("ALL METHODS ARE: ");
    for (Method m : getClass().getDeclaredMethods()) {
      I.say("  "+m);
    }
    
    
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
    //  presented to the player, and don't always need to be included..
    final String triggerName = topicNode.value("trigger");
    if (triggerName == null) topic.trigger = null;
    else try { topic.trigger = getClass().getDeclaredMethod(triggerName); }
    catch (Exception e) {
      I.say("\nWARNING:  No matching trigger method: "+triggerName);
      topic.trigger = null;
    }
    //
    //  We construct the message-panes themselves on demand, since the BaseUI
    //  might not have been initiated yet (see below.)
    return topic;
  }
  
  
  protected DialoguePane messageFor(String title, final BaseUI UI) {
  
    final Topic topic = allTopics.get(title);
    if (topic == null) return null;
    if (topic.message != null) return topic.message;
    
    //
    //  TODO:  ALLOW INCLUSION OF IMAGES/SCREENSHOTS WITHIN THE MESSAGE!
    
    final String content = topic.sourceNode.child("content").content();
    final Batch <Clickable> links = new Batch <Clickable> ();
    
    for (XML linkNode : topic.sourceNode.allChildrenMatching("link")) {
      final String linkKey  = linkNode.value("name");
      final String linkName = linkNode.content();
      
      links.add(new Clickable() {
        
        public String fullName() {
          return linkName;
        }
        
        public void whenClicked() {
          final DialoguePane message = messageFor(linkKey, UI);
          if (message != null) {
            UI.setInfoPanels(message, null);
          }
          else I.say("\nNO TOPIC MATCHING: "+linkKey);
        }
      });
    }
    return topic.message = new DialoguePane(
      UI, null, topic.title, content, null, links
    );
  }
  
  
  
  /**  Update methods for progress-conditions.
    */
  protected void checkForFlags() {
    //
    //  (We use an array here to allow deletions mid-iteration if something
    //  goes wrong, which java would not otherwise allow.)
    final Topic topics[] = new Topic[allTopics.size()];
    for (Topic topic : allTopics.values().toArray(topics)) {
      if (topic.trigger == null || topic.didPush) continue;
      
      boolean didTrigger = false;
      try {
        final Object triggerVal = topic.trigger.invoke(this);
        didTrigger = Boolean.TRUE.equals(triggerVal);
      }
      catch (Exception e) {
        I.report(e);
        allTopics.remove(topic.title);
      }
      
      if (didTrigger && pushMessage(topic.title, topic.urgent, true)) {
        topic.didPush = true;
      }
    }
  }
  
  
  private boolean pushMessage(
    String eventKey, boolean urgent, boolean viewNow
  ) {
    final ReminderListing reminders = UI().reminders();
    if (! reminders.hasMessageEntry(eventKey)) {
      final DialoguePane message = messageFor(eventKey, UI());
      if (message == null) return false;
      if (viewNow) UI().setInfoPanels(message, null);
      reminders.addMessageEntry(message, urgent);
      return true;
    }
    return false;
  }
  
  
  private boolean isViewing(String messageKey) {
    return messageKey.equals(UI().reminders().onScreenMessageKey());
  }
  
  
  
  
  /**  Trigger-methods that determine when to display certain topics (specified
    *  in the XML.)
    */
  private TrooperLodge barracksBuilt = null;
  private EngineerStation foundryBuilt = null;
  private SupplyDepot depotBuilt = null;
  private float startingBalance = -1;
  
  private Tile startAt = null;
  private ReconMission reconSent = null;
  private Drone droneAttacks = null;
  
  
  //  TODO:  You also need setup methods for the various stages.
  
  //  Create the assaulting drone once trade is complete.
  //  Assign upgrades and job applicants once the drone is dead.
  
  
  protected boolean checkShowWelcome() {
    return true;
  }
  
  
  protected boolean checkMotionDone() {
    final Vec3D lookPoint = UI().rendering.view.lookedAt;
    final Tile  lookTile  = tutorial.world().tileAt(lookPoint.x, lookPoint.y);
    if (lookTile == null) return false;
    
    if (startAt == null) startAt = lookTile;
    if (Spacing.distance(lookTile, startAt) < 4) return false;
    
    onMotionDone();
    return true;
  }
  
  
  protected void onMotionDone() {
    tutorial.base().intelMap.liftFogAround(tutorial.bastion, 12);
    UI().tracking.lockOn(tutorial.bastion);
  }
  
  
  protected boolean checkBuiltBarracks() {
    barracksBuilt = (TrooperLodge) firstBaseVenue(TrooperLodge.class);
    if (barracksBuilt == null) return false;
    
    onBuiltBarracks();
    return true;
  }
  
  
  protected void onBuiltBarracks() {
    barracksBuilt.structure.setState(Structure.STATE_INTACT, 1);
    tutorial.base().setup.fillVacancies(barracksBuilt, true);
  }
  
  
  protected boolean checkExploreBegun() {
    ReconMission match = null;
    for (Mission m : tutorial.base().tactics.allMissions()) {
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
    tutorial.base().setup.fillVacancies(tutorial.bastion, true);
    
    for (Actor a : tutorial.bastion.staff.workers()) {
      if (a.vocation() == Backgrounds.TECHNICIAN) {
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
    tutorial.base().setup.fillVacancies(depotBuilt  , true);
    tutorial.base().setup.fillVacancies(foundryBuilt, true);
  }
  
  
  protected boolean checkTradeSetup() {
    if (depotBuilt == null) return false;
    final Stocks DS = depotBuilt.stocks;
    final Traded imp = Economy.ORES, exp = Economy.PARTS;
    if (DS.demandFor(imp) == 0 || DS.producer(imp) == true ) return false;
    if (DS.demandFor(exp) == 0 || DS.producer(exp) == false) return false;
    
    onTradeSetup();
    return true;
  }
  
  
  protected void onTradeSetup() {
    final Base artilects = Base.artilects(world());
    droneAttacks = (Drone) Drone.SPECIES.sampleFor(artilects);
    
    Tile entry = Spacing.pickRandomTile(barracksBuilt, 6, world());
    entry = Spacing.nearestOpenTile(entry, entry);
    droneAttacks.enterWorldAt(entry, world());
    
    final Combat assault = new Combat(droneAttacks, barracksBuilt);
    assault.addMotives(Plan.MOTIVE_EMERGENCY, 100);
    droneAttacks.mind.assignBehaviour(assault);
    UI().tracking.lockOn(barracksBuilt);
    
    barracksBuilt.structure.addUpgrade(TrooperLodge.VOLUNTEER_STATION);
    barracksBuilt.structure.addUpgrade(TrooperLodge.MARKSMAN_TRAINING);
    barracksBuilt.structure.addUpgrade(TrooperLodge.MARKSMAN_TRAINING);
    barracksBuilt.structure.addUpgrade(TrooperLodge.TROOPER_STATION  );
    barracksBuilt.structure.addUpgrade(TrooperLodge.TROOPER_STATION  );
    barracksBuilt.structure.addUpgrade(TrooperLodge.TROOPER_STATION  );
    tutorial.base().setup.fillVacancies(barracksBuilt, false);
  }
  
  
  protected boolean checkDroneDestroyed() {
    if (droneAttacks == null) return false;
    if (droneAttacks.health.conscious()) return false;
    return true;
  }
  
  
  protected boolean checkRuinsDestroyed() {
    for (Venue ruin : tutorial.ruins) {
      if (ruin.structure.intact()) return false;
    }
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
    final float balance = tutorial.base().finance.credits();
    if (balance < startingBalance + 1000) return false;
    return true;
  }
  
  
  
  /**  Other helper methods-
    */
  private Venue firstBaseVenue(Class venueClass) {
    for (Object o : tutorial.world().presences.matchesNear(
      venueClass, null, -1
    )) {
      final Venue found = (Venue) o;
      if (found.base() == tutorial.base()) return found;
    }
    return null;
  }
  
  
  private Batch <Holding> allBaseHoldings() {
    final Batch <Holding> all = new Batch <Holding> ();
    for (Object o : tutorial.world().presences.matchesNear(
      Holding.class, null, -1
    )) {
      final Holding h = (Holding) o;
      if (h.base() == tutorial.base()) all.add(h);
    }
    return all;
  }
  
  
  private Stage world() {
    return tutorial.world();
  }
  
  
  private BaseUI UI() {
    return tutorial.UI();
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