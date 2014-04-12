


package stratos.game.campaign ;
import stratos.game.common.*;
import stratos.game.actors.Background;
import stratos.game.actors.Human;
import stratos.game.base.*;
import stratos.game.planet.*;
import stratos.game.wild.*;
import stratos.graphics.widgets.Text;
import stratos.user.*;
import stratos.util.*;



//
//  TODO:  Guide the player through the basics of setting up a settlement,
//  using missions to explore, defend, and either attack or contact natives
//  and other local threats.


//  Objectives:
//    Destroy the artilects and raze their lair.
//    Make peace with, or drive out, all native camps.
//    Turn a profit for 6 consecutive days.
//    Attain a population of 60 citizens.



public class TutorialScenario extends StartupScenario {
  
  
  Bastion bastion;
  
  Batch <Ruins> ruins;
  Batch <NativeHut> huts;
  
  
  private static Config config() {
    final Config config = new Config();
    config.house = Background.PLANET_HALIBAN;
    
    config.male = Rand.yes();
    
    config.siteLevel = SITE_WILDERNESS;
    config.titleLevel = TITLE_KNIGHTED;
    config.fundsLevel = FUNDING_STANDARD;
    return config;
  }
  
  
  public TutorialScenario() {
    super(config());
  }
  
  
  public TutorialScenario(Session s) throws Exception {
    super(s);
    s.loadObjects(ruins = new Batch <Ruins> ());
    s.loadObjects(huts = new Batch <NativeHut> ());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(ruins);
    s.saveObjects(huts);
  }
  
  
  
  /**  Initial setup-
    */
  protected Bastion establishBastion(
    World world, Base base, Human ruler,
    List<Human> advisors, List<Human> colonists
  ) {
    bastion = super.establishBastion(world, base, ruler, advisors, colonists);
    return bastion;
  }
  
  
  protected void establishLocals(World world) {
    ruins = Ruins.placeRuins(world, 1);
    huts = NativeHut.establishSites(NativeHut.TRIBE_FOREST, world);
  }
  
  
  
  /**  Monitoring and updates-
    */
  final String
    TITLE_WELCOME = "Welcome",
    TITLE_OBJECTIVES = "Order of Business",
    TITLE_SECURITY = "Objective 1: Security",  //  Recon and Strike!
    TITLE_CONTACT = "Objective 2: Contact",
    TITLE_ECONOMY = "Objective 3: Economy Basics";  //
  
  //private String stage = TITLE_WE
  //private int stage = 0;
  
  
  
  public void updateGameState() {
    super.updateGameState();
    
    pushMessage(TITLE_WELCOME);
    
  }
  
  
  private void pushMessage(String title) {
    final CommsPanel comms = UI().commsPanel();
    if (! comms.hasMessage(title)) {
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
        "What you do first is up to you, but for the moment, we'll specify "+
        "three basic objectives for you to tackle.  Complete two of the "+
        "three, and we will proceed to the next stage of this tutorial.",
        linkFor("Tell me about the security objective.", TITLE_SECURITY),
        linkFor("Tell me about the contact objective.", TITLE_CONTACT),
        linkFor("Tell me about the economic objective.", TITLE_ECONOMY)
      );
    }
    
    if (title == TITLE_SECURITY) {
      return comms.addMessage(
        TITLE_SECURITY, null,
        "Somewhere on this map, there is an ancient ruin, inhabited by "+
        "artilect guardians who may come to threaten your settlement.  You "+
        "will need to find this site and destroy it, by first exploring the "+
        "map to find the site, and then declaring a strike mission.",
        linkFor("Go back", TITLE_OBJECTIVES),
        linkFor("Tell me about the contact objective.", TITLE_CONTACT),
        linkFor("Tell me about the economic objective.", TITLE_ECONOMY)
      );
    }
    
    if (title == TITLE_CONTACT) {
      return comms.addMessage(
        TITLE_CONTACT, null,
        "Somewhere on this map, there is a camp of primitive humanoids, who "+
        "may present either a threat or an asset, depending on how they are "+
        "handled.  Either declare a contact mission to bring them within the "+
        "fold of your settlement, or drive them out entirely.",
        linkFor("Go back", TITLE_OBJECTIVES),
        linkFor("Tell me about the security objective.", TITLE_SECURITY),
        linkFor("Tell me about the economic objective.", TITLE_ECONOMY)
      );
    }
    
    if (title == TITLE_ECONOMY) {
      return comms.addMessage(
        TITLE_ECONOMY, null,
        "In order for your settlement to provide a viable power base for "+
        "later expansion, you will need to establish exports and gather tax "+
        "from your citizens.  Try to put all your citizens in pyon housing or "+
        "better, and turn a profit for six consecutive days.",
        linkFor("Go back", TITLE_OBJECTIVES),
        linkFor("Tell me about the security objective.", TITLE_SECURITY),
        linkFor("Tell me about the contact objective.", TITLE_CONTACT)
      );
    }
    
    return messageFor(TITLE_WELCOME);
  }
}




