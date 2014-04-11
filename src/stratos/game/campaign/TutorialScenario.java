


package stratos.game.campaign ;
import stratos.game.common.*;
import stratos.game.actors.Background;
import stratos.game.actors.Human;
import stratos.game.base.*;
import stratos.game.planet.*;
import stratos.game.wild.*;
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
  public void updateGameState() {
    super.updateGameState();
    
    final CommsPanel comms = UI().commsPanel();
    
    if (! comms.hasMessage("Welcome")) comms.pushMessage(
      "Welcome", null,
      "Hello, and welcome to Stratos: The Sci-Fi Settlement Sim!  This "+
      "tutorial will walk you through the essentials of setting up a "+
      "settlement, exploring the surrounds, dealing with potential threats, "+
      "and establishing a sound economy."
    );
    
    /*
    postMessage(
      "Order of Business",
      "What you do first is up to you.  But as a general rule, your first "+
      "order of business"
    );
    //*/
  }
}




/*
postMessage(
  "In order for your settlement to become a viable long-term power-base, "+
  "you will need to get your finances in order.  Your citizens need "+
  "basic foodstuffs and supplies in order to survive and construct new "+
  "facilities."
);

//  TODO:  What about the combat guilds?  Trooper, Runner, or Explorer.

postMessage(
  "  Objective:  Turn a profit for 6 consecutive days."
);

postMessage(
  "Many sectors have resident tribes of human primitives or mutant "+
  "strains, which may present either a threat or an opportunity, "+
  "depending on how they are handled.  A diplomatic touch may allow them "+
  "to be recruited as allies, but hot-blooded arguments on either side "+
  "can spark violent retaliation.\n"+
  "  Objective:  Make peace with, or drive out, all Native camps."
);

postMessage(
  "As your settlement grows in size, it will begin to attract attention "+
  "from your neighbours- in this case, hostile artilects from the nearby "+
  "ruins.  This threat will need to be neutralised in order for your base "+
  "to grow.\n"+
  "  Objective:  Destroy the Ruins"
);
//*/