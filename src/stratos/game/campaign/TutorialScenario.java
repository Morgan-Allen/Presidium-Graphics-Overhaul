
package stratos.game.campaign;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class TutorialScenario extends StartupScenario {
  
  
  private static boolean verbose = false;
  
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
    
    config.siteLevel  = SITE_WILDERNESS;
    config.titleLevel = TITLE_KNIGHTED;
    config.fundsLevel = FUNDING_GENEROUS;
    
    //  TODO:  Don't need this if hiring is free...
    /*
    config.numCrew.put(Backgrounds.VETERAN   , 2);
    config.numCrew.put(Backgrounds.TECHNICIAN, 2);
    config.numCrew.put(Backgrounds.AUDITOR   , 1);
    config.numCrew.put(Backgrounds.CULTIVATOR, 3);
    //*/
    return config;
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.hireFree = true;
    super.configureScenario(world, base, UI);
  }
  
  
  protected Bastion establishBastion(
    Stage world, Base base, Human ruler,
    List<Human> advisors, List<Human> colonists
  ) {
    bastion = super.establishBastion(world, base, ruler, advisors, colonists);
    return bastion;
  }
  
  
  protected void establishLocals(Stage world) {
    ruins = Ruins.placeRuins(world, 1);
    huts = NativeHut.establishSites(NativeHut.TRIBE_FOREST, world);
  }
  
  
  
  /**  Checking objectives and message display-
    */
  public void updateGameState() {
    super.updateGameState();
    
    /*
    final Base natives = Base.baseWithName(world(), Base.KEY_NATIVES, true);
    I.say("\nGetting base relations-");
    I.say("  Player -> natives: "+base().relations.relationWith(natives));
    I.say("  Natives -> player: "+natives.relations.relationWith(base()));
    //*/
    
    if (showMessages()) {
      pushMessage(TITLE_WELCOME);
    }
    
    int numObjectives = 0;
    if (checkSecurityObjective()) {
      pushMessage(TITLE_SECURITY_DONE);
      numObjectives++;
    }
    if (checkContactObjective()) {
      pushMessage(TITLE_CONTACT_DONE);
      numObjectives++;
    }
    if (checkEconomicObjective()) {
      pushMessage(TITLE_ECONOMY_DONE);
      numObjectives++;
    }
    if (numObjectives >= 2) {
      pushMessage(TITLE_CONGRATULATIONS);
    }
    
    ///I.say("Objectives complete: "+numObjectives);
  }
  
  
  private boolean checkSecurityObjective() {
    if (ruins.size() == 0) return false;
    for (Ruins ruin : ruins) {
      if (ruin.destroyed());
      else return false;
    }
    return true;
  }
  
  
  private boolean checkContactObjective() {
    if (huts.size() == 0) return false;
    for (NativeHut hut : huts) {
      if (hut.destroyed() || hut.base() == base());
      else return false;
    }
    return true;
  }
  
  
  private boolean checkEconomicObjective() {
    boolean hasHolding = false;
    final Tile t = world().tileAt(0, 0);
    for (Object o : world().presences.matchesNear(Holding.class, t, -1)) {
      final Holding h = (Holding) o;
      hasHolding = true;
      if (h.upgradeLevel() < HoldingUpgrades.LEVEL_FREEBORN) return false;
    }
    if (base().finance.credits() < 0 || ! hasHolding) return false;
    return true;
  }
  
  
  
  /**  Monitoring and updates-
    */
  final String
    TITLE_WELCOME    = "Welcome",
    TITLE_OBJECTIVES = "Order of Business",
    TITLE_SECURITY   = "Objective 1: Security",
    TITLE_CONTACT    = "Objective 2: Contact",
    TITLE_ECONOMY    = "Objective 3: Economy Basics",
    TITLE_NAVIGATION = "Navigation Basics",
    
    TITLE_EXPLAIN_EXPAND    = "Expanding your base",
    TITLE_EXPLAIN_DEFEND    = "Defending your base",
    TITLE_EXPLAIN_CONTACT   = "Diplomacy missions",
    TITLE_EXPLAIN_INTERVIEW = "Interviewing citizens",
    TITLE_EXPLAIN_SUPPLY    = "Getting Supplies",
    TITLE_EXPLAIN_INDUSTRY  = "Housing and Industry",
    
    TITLE_SECURITY_DONE   = "Security objective complete",
    TITLE_CONTACT_DONE    = "Contact objective complete",
    TITLE_ECONOMY_DONE    = "Economy objective complete",
    TITLE_CONGRATULATIONS = "Tutorial complete!";
  
  protected boolean showMessages() { return true; }
  
  
  private void pushMessage(String title) {
    if (UI().selection.selected() != null) return;
    final CommsPanel comms = UI().commsPanel();
    if (! comms.hasMessage(title)) {
      if (verbose) I.say("PUSHING NEW MESSAGE: "+title);
      UI().setInfoPanels(messageFor(TITLE_WELCOME), null);
    }
  }
  
  
  private Text.Clickable linkFor(String linkText, final String title) {
    final CommsPanel comms = UI().commsPanel();
    return new Description.Link(linkText) {
      public void whenTextClicked() {
        DialoguePanel message = comms.messageWith(title);
        if (message == null) message = messageFor(title);
        UI().setInfoPanels(message, null);
      }
    };
  }
  
  
  private DialoguePanel messageFor(String title) {
    final CommsPanel comms = UI().commsPanel();
    if (comms.hasMessage(title)) return comms.messageWith(title);
    
    if (title == TITLE_WELCOME) {
      return comms.addMessage(
        TITLE_WELCOME, null,
        "Hello, and welcome to Stratos: The Sci-Fi Settlement Sim!  This "+
        "tutorial will walk you through the essentials of setting up a "+
        "settlement, exploring the surrounds, dealing with potential threats, "+
        "and establishing a sound economy.",
        linkFor("Proceed", TITLE_OBJECTIVES)
      );
    }
    
    if (title == TITLE_OBJECTIVES) {
      return comms.addMessage(
        TITLE_OBJECTIVES, null,
        "For the moment, we'll specify three basic objectives for you to "+
        "tackle.  Complete two of the three, and we will proceed to the next "+
        "stage of this tutorial.  What you tackle first is up to you.",
        linkFor("Tell me about the security objective.", TITLE_SECURITY),
        linkFor("Tell me about the contact objective.", TITLE_CONTACT),
        linkFor("Tell me about the economic objective.", TITLE_ECONOMY),
        linkFor("Wait a second.  How do I navigate?", TITLE_NAVIGATION)
      );
    }
    
    if (title == TITLE_NAVIGATION) {
      return comms.addMessage(
        TITLE_NAVIGATION, null,
        "To save and load, hit S and L, or click on the 'foresight' and "+
        "'remembrance' buttons in the bottom left.\n"+
        "To move your viewpoint, either click on a selectable object (such as "+
        "a structure, person, or point of terrain,) or click on the minimap. "+
        "You can also use the arrow keys.  Clicking also displays information "+
        "about most objects.\n"+
        "Construction options are available from the Guild buttons in the "+
        "bottom right, along with information about each structure type.\n"+
        "Finally, these messages will be stored in the Comms Panel, opened "+
        "by clicking just above and to the right of the top-left minimap.",
        linkFor("Okay, that helps.", TITLE_OBJECTIVES)
      );
    }
    
    if (title == TITLE_SECURITY) {
      return comms.addMessage(
        TITLE_SECURITY, null,
        "Somewhere on this map, there is an ancient ruin, inhabited by "+
        "artilect guardians who may come to threaten your settlement.  You "+
        "will need to find this site and destroy it, by first exploring the "+
        "map to find the site, and then declaring a strike mission.",
        linkFor("How do I deal with enemies?", TITLE_EXPLAIN_EXPAND),
        linkFor("How do I protect my subjects?", TITLE_EXPLAIN_DEFEND),
        linkFor("Tell me about the contact objective.", TITLE_CONTACT),
        linkFor("Tell me about the economic objective.", TITLE_ECONOMY)
      );
    }
    
    if (title == TITLE_EXPLAIN_EXPAND) {
      return comms.addMessage(
        TITLE_EXPLAIN_EXPAND, null,
        "To deal with threats to your base, you will need to find them first. "+
        "Click on a piece of hidden terrain, then on the green icon, to begin "+
        "a Reconnaissance mission.  Leave it public for now, and increase the "+
        "payment to attract applicants.\n"+
        "Once you find a threat, select it, and then click on the red icon to "+
        "begin a Strike mission.  (You may wish to build a Garrison or two "+
        "first, just to get numbers on your side.)  With luck, your soldiers "+
        "will polish off the interlopers in short order.",
        linkFor("How do I protect my subjects?", TITLE_EXPLAIN_DEFEND),
        linkFor("Go back to the security objective.", TITLE_SECURITY)
      );
    }
    
    if (title == TITLE_EXPLAIN_DEFEND) {
      return comms.addMessage(
        TITLE_EXPLAIN_DEFEND, null,
        "If you fear attacks on vulnerable portions of your settlement, you "+
        "can also declare temporary Security missions (the orange icon) for "+
        "up to 48 hours on selected persons or structures.  In the case of "+
        "buildings, repairs will be carried out by qualified applicants, "+
        "while unconscious citizens in dangerous areas can be recovered and "+
        "treated using the same method.",
        linkFor("How do I deal with enemies?", TITLE_EXPLAIN_EXPAND),
        linkFor("Go back to the security objective.", TITLE_SECURITY)
      );
    }
    //  Explain use of recon to explore, and strike to destroy.
    //  Explain use of defence flags, also for first aid and repairs.
    
    if (title == TITLE_CONTACT) {
      return comms.addMessage(
        TITLE_CONTACT, null,
        "Somewhere on this map, there is a camp of native tribal humans, who "+
        "may present either a threat or an asset, depending on how they are "+
        "handled.  Either declare a contact mission to bring them within the "+
        "fold of your settlement, or drive them out with a strike team.",
        linkFor("How do I make first contact?", TITLE_EXPLAIN_CONTACT),
        linkFor("What are the outcomes of contact?", TITLE_EXPLAIN_INTERVIEW),
        linkFor("Tell me about the security objective.", TITLE_SECURITY),
        linkFor("Tell me about the economic objective.", TITLE_ECONOMY)
      );
    }
    
    if (title == TITLE_EXPLAIN_CONTACT) {
      return comms.addMessage(
        TITLE_EXPLAIN_CONTACT, null,
        "Once you have explored the environs of your base and located the "+
        "native camp, you have a couple of options for handling them.  Here "+
        "we cover the diplomatic option- selecting the camp and declaring a "+
        "Contact mission (click the blue icon.)\n"+
        "Before you determine the payment offered, you may wish to change the "+
        "mission type from public to 'screened' or 'covert'- diplomacy is a "+
        "delicate matter, and you may want some finer control over who "+
        "applies for the job.",
        linkFor("What are the outcomes of contact?", TITLE_EXPLAIN_INTERVIEW),
        linkFor("Go back to the contact objective.", TITLE_CONTACT)
      );
    }
    
    if (title == TITLE_EXPLAIN_INTERVIEW) {
      return comms.addMessage(
        TITLE_EXPLAIN_INTERVIEW, null,
        "Contact objectives can be changed, to either 'demand submission', "+
        "'request audience' or 'offer friendship'.  The first option will "+
        "simply intimidate the native(s) into joining your base, which is "+
        "the surest method in the short term but risks later defection and "+
        "malcontent.  The second will request their presence at the Bastion, "+
        "where they can be interviewed, and the third merely offers gifts and "+
        "good will.  Given time, sufficiently respected natives might join "+
        "your base spontaneously.\n"+
        "Any of your own subjects may be summoned for interview at will, so "+
        "they can be recruited for secret missions, or just asked for advice "+
        "and opinions.",
        linkFor("How do I make first contact?", TITLE_EXPLAIN_CONTACT),
        linkFor("Go back to the contact objective.", TITLE_CONTACT)
      );
    }
    //  Mention that negotiations are delicate- use screened/covert.
    //  If negotiations are successful, they'll visit you.
    
    if (title == TITLE_ECONOMY) {
      return comms.addMessage(
        TITLE_ECONOMY, null,
        "In order for your settlement to provide a viable power base for "+
        "later expansion, you will need to establish exports and gather tax "+
        "from your citizens.  Try to put all your citizens in pyon housing or "+
        "better without running short of money.",
        linkFor("How do I get money and supplies?", TITLE_EXPLAIN_SUPPLY),
        linkFor("How do I improve my housing?", TITLE_EXPLAIN_INDUSTRY),
        linkFor("Tell me about the security objective.", TITLE_SECURITY),
        linkFor("Tell me about the contact objective.", TITLE_CONTACT)
      );
    }

    if (title == TITLE_EXPLAIN_SUPPLY) {
      return comms.addMessage(
        TITLE_EXPLAIN_SUPPLY, null,
        "To secure a solid early cash flow, a fair strategy is to build a "+
        "Supply Depot, followed by either a Botanical Station (on fertile "+
        "terrain) or an Excavation Site (on rocky areas.)  The Supply Depot "+
        "will allow spacecraft to dock conveniently at your base, and allows "+
        "you to set broad import/export levels for various types of goods.\n"+
        "The other structures, meanwhile, take advantage of local resources "+
        "to produce food and minerals for export or local consumption.  (Both "+
        "the Botanical Station and Excavation Site need a good deal of room, "+
        "so don't place them too close to your main base.)",
        linkFor("How do I improve my housing?", TITLE_EXPLAIN_INDUSTRY),
        linkFor("Go back to the economic objective.", TITLE_ECONOMY)
      );
    }
    
    if (title == TITLE_EXPLAIN_INDUSTRY) {
      return comms.addMessage(
        TITLE_EXPLAIN_INDUSTRY, null,
        "In order to improve your housing- and export finished goods- you may "+
        "wish to place an Artificer or Fabricator to take metal ore or carbs "+
        "and process them into parts and plastics, along with weapons or "+
        "outfits for your citizens.  A skilled worker with enough raw "+
        "materials can make perhaps 5 units per day, but you may wish to "+
        "install upgrades (like Assembly Line or Polymer Loom) to speed the "+
        "process.\n"+
        "Holdings should display the goods and services they require to "+
        "upgrade in their status description, and will do so after being "+
        "satisfied for a day or so.",
        linkFor("How do I get money and supplies?", TITLE_EXPLAIN_SUPPLY),
        linkFor("Go back to the economic objective.", TITLE_ECONOMY)
      );
    }
    
    //  Mention artificer and fabricator.  Upgrades to increase production.
    //  Mention supply depot and botanical station, for trade and food.
    
    if (title == TITLE_SECURITY_DONE) {
      return comms.addMessage(
        TITLE_SECURITY_DONE, null,
        "Congratulations!  Each of the ruins has now been destroyed."
      );
    }
    
    if (title == TITLE_CONTACT_DONE) {
      return comms.addMessage(
        TITLE_CONTACT_DONE, null,
        "Congratulations!  The native camps no longer pose a threat to your "+
        "base."
      );
    }
    
    if (title == TITLE_ECONOMY_DONE) {
      return comms.addMessage(
        TITLE_ECONOMY_DONE, null,
        "Congratulations!  Your future economic prospects are bright."
      );
    }
    
    if (title == TITLE_CONGRATULATIONS) {
      return comms.addMessage(
        TITLE_CONGRATULATIONS, null,
        "This tutorial is now complete.  Feel free to explore the mechanics "+
        "of construction and mission-settings some more, but when you are "+
        "ready, select 'Complete Tutorial' to advance to a new mission.\n\n"+
        "Complete Tutorial <UNDER CONSTRUCTION!>"
      );
    }
    
    return messageFor(TITLE_WELCOME);
  }
}









