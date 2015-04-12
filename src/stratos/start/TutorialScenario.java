/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.base.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;



public class TutorialScenario extends StartupScenario implements
  MessagePane.MessageSource
{
  
  private static boolean
    verbose          = false,
    objectiveVerbose = false;
  
  Bastion bastion;
  Ruins ruins;
  private HelpScript script;
  
  
  public TutorialScenario(String prefix) {
    super(config(), prefix);
  }
  
  
  public TutorialScenario(Session s) throws Exception {
    super(s);
    bastion = (Bastion) s.loadObject();
    ruins   = (Ruins  ) s.loadObject();
    script().loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(bastion);
    s.saveObject(ruins  );
    script().saveState(s);
  }
  
  
  protected HelpScript script() {
    //
    //  NOTE:  We may have to initialise the script here, as save/load calls
    //  for messages in the ReminderListing can be invoked before the class-
    //  constructor has finished.
    if (script == null) script = new HelpScript(this);
    return script;
  }
  
  
  public MessagePane configMessage(String title, BaseUI UI) {
    return script().messageFor(title);
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
    return config;
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
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
    final VenueProfile RP[] = Ruins.VENUE_PROFILES;
    ruins = (Ruins) AS.doPlacementsFor(RP[0], 1).first();
    AS.fillVacancies(ruins, true);
  }
  
  
  
  /**  Checking objectives and message display-
    */
  public void updateGameState() {
    super.updateGameState();
    script().checkForFlags();
  }
}




//  TODO:  Save security and contact missions, plus handling natives, for an
//  intermediate/advanced tutorial where you move on to another map.

//  TODO:  Include psychic powers and one of the Schools (Shapers?) as well.
/*
ruler.skills.addTechnique(Power.REMOTE_VIEWING);
ruler.skills.addTechnique(Power.SUSPENSION    );
ruler.skills.addTechnique(Power.FORCEFIELD    );
ruler.skills.addTechnique(Power.TELEKINESIS   );
//*/

/*
final int tribeID = NativeHut.TRIBE_FOREST;
final BaseSetup NS = Base.natives(world, tribeID).setup;
huts = new Batch <NativeHut> ();
final VenueProfile NP[] = NativeHut.VENUE_PROFILES[tribeID];
Visit.appendTo(huts, NS.doPlacementsFor(NP[0], 2));
Visit.appendTo(huts, NS.doPlacementsFor(NP[1], 3));
NS.fillVacancies(huts, true);
for (NativeHut hut : huts) NS.establishRelationsAt(hut);
//*/


/*
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
//*/



