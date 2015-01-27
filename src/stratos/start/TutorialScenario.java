
package stratos.start;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.start.TutorialScript.*;



//The raiding dynamics here need to be worked out, so that the natives and
//artilects can plausibly co-exist.
//Divide the map into regions, and arrange for *not* wandering outside of it.
//And have that show up on the minimap (regional claims.)

//Start out with 8K.
//End with +1 housing and at least 4K.
//Destroy the Artilect lair.
//Persuade the natives to open trade with your base.

//  ...This map is too small to allow for multiple factions, really.  More
//  useful as a theoretical exercise.



public class TutorialScenario extends StartupScenario implements
  CommsPanel.CommSource
{
  
  private static boolean
    verbose          = false,
    objectiveVerbose = false;
  
  Bastion bastion;
  Batch <Ruins> ruins;
  Batch <NativeHut> huts;
  
  
  public TutorialScenario() {
    super(config());
  }
  
  
  public TutorialScenario(Session s) throws Exception {
    super(s);
    s.loadObjects(ruins = new Batch <Ruins    > ());
    s.loadObjects(huts  = new Batch <NativeHut> ());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(ruins);
    s.saveObjects(huts );
  }
  
  
  
  /**  Initial setup-
    */
  private static Config config() {
    final Config config = new Config();
    config.house = Sectors.PLANET_HALIBAN;
    config.gender = null;
    
    config.siteLevel  = SITE_WILDERNESS ;
    config.titleLevel = TITLE_KNIGHTED  ;
    config.fundsLevel = FUNDING_GENEROUS;
    
    config.numCrew.put(Backgrounds.TROOPER   , 2);
    config.numCrew.put(Backgrounds.TECHNICIAN, 3);
    config.numCrew.put(Backgrounds.AUDITOR   , 1);
    
    config.advisors.add(Backgrounds.FIRST_CONSORT);
    return config;
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    
    if (showMessages()) registerAllTopics();
  }
  
  
  protected Bastion establishBastion(
    Stage world, Base base, Human ruler,
    List <Human> advisors, List <Human> colonists
  ) {
    bastion = super.establishBastion(world, base, ruler, advisors, colonists);
    return bastion;
  }
  
  
  protected void establishLocals(Stage world) {
    
    final BaseSetup AS = Base.artilects(world).setup;
    ruins = new Batch <Ruins> ();
    Visit.appendTo(ruins, AS.doPlacementsFor(Ruins.VENUE_PROFILES[0], 1));
    AS.fillVacancies(ruins, true);
    
    final int tribeID = NativeHut.TRIBE_FOREST;
    final BaseSetup NS = Base.natives(world, tribeID).setup;
    huts = new Batch <NativeHut> ();
    final VenueProfile NP[] = NativeHut.VENUE_PROFILES[tribeID];
    Visit.appendTo(huts, NS.doPlacementsFor(NP[0], 2));
    Visit.appendTo(huts, NS.doPlacementsFor(NP[1], 3));
    NS.fillVacancies(huts, true);
    for (NativeHut hut : huts) NS.establishRelationsAt(hut);
  }
  
  
  
  /**  Checking objectives and message display-
    */
  public void updateGameState() {
    super.updateGameState();

    if (showMessages()) {
      pushMessage(EVENT_WELCOME);

      int numObjectives = 0;
      if (checkSecurityObjective()) {
        pushMessage(EVENT_SECURITY_DONE);
        numObjectives++;
      }
      if (checkContactObjective()) {
        pushMessage(EVENT_CONTACT_DONE);
        numObjectives++;
      }
      if (checkEconomicObjective()) {
        pushMessage(EVENT_ECONOMY_DONE);
        numObjectives++;
      }
      if (numObjectives >= 2) {
        pushMessage(EVENT_CONGRATULATIONS);
      }
    }
  }
  
  
  private boolean checkSecurityObjective() {
    final boolean report = objectiveVerbose;
    int numRuins = 0, numRazed = 0;
    
    for (Ruins ruin : ruins) {
      numRuins++;
      if (ruin.destroyed()) numRazed++;
    }
    
    if (report) {
      I.say("\nChecking security objective:");
      I.say("  "+numRazed+"/"+numRuins+" destroyed.");
    }
    return numRazed == numRuins;
  }
  
  
  private boolean checkContactObjective() {
    final boolean report = objectiveVerbose;
    int numHuts = 0, numRazed = 0, numConverts = 0;
    
    for (NativeHut hut : huts) {
      numHuts++;
      if (hut.destroyed()) numRazed++;
      else if (hut.base() == base()) numConverts++;
    }
    
    if (report) {
      I.say("\nChecking contact objective:");
      I.say("  "+numHuts+" huts in total.");
      I.say("  "+numRazed+" razed, "+numConverts+" converted.");
    }
    return (numRazed + numConverts) == numHuts;
  }
  
  
  private boolean checkEconomicObjective() {
    final boolean report = objectiveVerbose;
    final int needLevel = HoldingUpgrades.LEVEL_PYON;
    int numHoldings = 0, totalLevel = 0;
    
    final Tile t = world().tileAt(0, 0);
    for (Object o : world().presences.matchesNear(Holding.class, t, -1)) {
      final Holding h = (Holding) o;
      if (h.base() != base()) continue;
      numHoldings++;
      totalLevel += h.upgradeLevel();
    }
    
    final int avgLevel = numHoldings == 0 ? 0 : (totalLevel / numHoldings);
    if (report) {
      I.say("\nChecking economic objective:");
      I.say("  "+numHoldings+" total holdings, total levels: "+totalLevel);
      I.say("  Average level: "+avgLevel+"/"+needLevel);
      I.say("  Current credits: "+base().finance.credits());
    }
    if (base().finance.credits() < 0 || numHoldings == 0) return false;
    return avgLevel >= needLevel;
  }
  
  
  
  /**  Monitoring and updates-
    */
  protected boolean showMessages() { return true; }
  
  
  private void registerAllTopics() {
    final CommsPanel comms = UI().commsPanel();
    for (String topicKey : ALL_TOPIC_TITLES) {
      final DialoguePanel panel = comms.messageWith(topicKey);
      if (panel != null) continue;
      comms.addMessage(this, topicKey, messageFor(topicKey, comms, false));
    }
  }

  
  private void pushMessage(String eventKey) {
    final CommsPanel comms = UI().commsPanel();
    
    if (! comms.hasMessage(eventKey)) {
      if (verbose || true) I.say("PUSHING NEW MESSAGE: "+eventKey);
      UI().setInfoPanels(messageFor(eventKey, comms, false), null);
    }
  }
  
  
  public DialoguePanel messageFor(
    String title, CommsPanel comms, boolean useCache
  ) {
    return new TutorialScript(this).messageFor(title, comms, useCache);
  }
}









