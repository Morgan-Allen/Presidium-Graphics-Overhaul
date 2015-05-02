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
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;




public class TutorialScenario extends StartupScenario {
  
  private static boolean
    verbose          = false,
    objectiveVerbose = false;
  
  final static String SCRIPT_XML_PATH = "media/Help/TutorialScript.xml";
  
  
  Bastion bastion;
  Ruins ruins;
  final MessageScript script;
  
  
  public TutorialScenario(String prefix) {
    super(config(), prefix);
    script = new MessageScript(this, SCRIPT_XML_PATH);
  }
  
  
  public TutorialScenario(Session s) throws Exception {
    super(s);
    bastion = (Bastion) s.loadObject();
    ruins   = (Ruins  ) s.loadObject();
    this.loadAllFlags(s);
    this.script = (MessageScript) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(bastion);
    s.saveObject(ruins  );
    this.saveAllFlags(s);
    s.saveObject(script);
  }
  
  
  
  /**  Initial setup-
    */
  private static Config config() {
    final Config config = new Config();
    config.house = Verse.PLANET_HALIBAN;
    config.gender = null;
    
    config.siteLevel  = SITE_WILDERNESS ;
    config.titleLevel = TITLE_KNIGHTED  ;
    config.fundsLevel = FUNDING_GENEROUS;
    return config;
  }
  
  
  protected void initScenario(String prefix) {
    clearAllFlags();
    super.initScenario(prefix);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    
    GameSettings.noAdvice = true;
    GameSettings.noShips  = true;
    base.setup.setControlLevel(BaseSetup.LEVEL_CUSTOM);
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
    final Blueprint RP[] = Ruins.VENUE_BLUEPRINTS;
    ruins = (Ruins) AS.doPlacementsFor(RP[0], 1).first();
    AS.fillVacancies(ruins, true);
  }
  
  
  
  /**  Checking objectives and message display-
    */
  public void updateGameState() {
    super.updateGameState();
    script.checkForEvents();
  }
  
  
  private TrooperLodge    barracksBuilt = null;
  private EngineerStation foundryBuilt  = null;
  private SupplyDepot     depotBuilt    = null;
  private float startingBalance = -1;
  
  private Tile         startAt      = null;
  private MissionRecon reconSent    = null;
  private Drone        droneAttacks = null;
  
  
  protected void loadAllFlags(Session s) throws Exception {
    barracksBuilt   = (TrooperLodge   ) s.loadObject();
    foundryBuilt    = (EngineerStation) s.loadObject();
    depotBuilt      = (SupplyDepot    ) s.loadObject();
    startingBalance = s.loadFloat();
    
    startAt      = (Tile        ) s.loadObject();
    reconSent    = (MissionRecon) s.loadObject();
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
  
  
  protected void clearAllFlags() {
    //  TODO:  The script should definitely have some generalised methods for
    //  handling this...
    
    script.clearScript();
    barracksBuilt = null;
    foundryBuilt  = null;
    depotBuilt    = null;
    startingBalance = -1;
    startAt      = null;
    reconSent    = null;
    droneAttacks = null;
  }
  
  
  protected boolean checkShowWelcome() {
    return true;
  }
  
  
  protected boolean checkMotionDone() {
    final Vec3D lookPoint = UI().rendering.view.lookedAt;
    final Tile  lookTile  = world().tileAt(lookPoint.x, lookPoint.y);
    if (lookTile == null) return false;
    
    if (startAt == null) startAt = lookTile;
    if (Spacing.distance(lookTile, startAt) < 4) return false;
    return true;
  }
  
  
  protected void onMotionDone() {
    base().intelMap.liftFogAround(bastion, 12);
    UI().tracking.lockOn(bastion);
  }
  
  
  protected boolean checkBuiltBarracks() {
    barracksBuilt = (TrooperLodge) firstBaseVenue(TrooperLodge.class);
    if (barracksBuilt == null) return false;
    
    onBuiltBarracks();
    return true;
  }
  
  
  protected void onBuiltBarracks() {
    barracksBuilt.structure.setState(Structure.STATE_INTACT, 1);
    base().setup.fillVacancies(barracksBuilt, true);
    base().setup.fillVacancies(bastion      , true);
    UI().tracking.lockOn(barracksBuilt);
  }
  
  
  protected boolean checkExploreBegun() {
    MissionRecon match = null;
    for (Mission m : base().tactics.allMissions()) {
      if (m instanceof MissionRecon) match = (MissionRecon) m;
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
    
    for (Actor a : bastion.staff.workers()) {
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
    base().setup.fillVacancies(depotBuilt  , true);
    base().setup.fillVacancies(foundryBuilt, true);
  }
  
  
  protected boolean checkTradeSetup() {
    if (depotBuilt == null) return false;
    final Stocks DS = depotBuilt.stocks;
    final Traded imp = Economy.METALS, exp = Economy.PARTS;
    if (DS.demandFor(imp) == 0 || DS.producer(imp) == true ) return false;
    if (DS.demandFor(exp) == 0 || DS.producer(exp) == false) return false;
    return true;
  }
  
  
  protected void onTradeSetup() {
    GameSettings.noShips = false;
    world().offworld.journeys.scheduleLocalDrop(base(), 5);
  }
  
  
  protected void onSecurityBasicsOpen() {
    UI().tracking.lockOn(barracksBuilt);
  }
  
  
  protected void onBaseAttackOpen() {

    final Base artilects = Base.artilects(world());
    droneAttacks = (Drone) Drone.SPECIES.sampleFor(artilects);
    
    Tile entry = Placement.findClearSpot(barracksBuilt, world(), 2);
    
    
    entry = Spacing.nearestOpenTile(entry, entry);
    droneAttacks.enterWorldAt(entry, world());
    
    final Combat assault = new Combat(droneAttacks, barracksBuilt);
    assault.addMotives(Plan.MOTIVE_EMERGENCY, 100);
    droneAttacks.mind.assignBehaviour(assault);
    
    UI().tracking.lockOn(droneAttacks);
  }
  
  
  protected boolean checkDroneDestroyed() {
    if (droneAttacks == null) return false;
    if (droneAttacks.health.conscious()) return false;
    return true;
  }
  
  
  protected void onHireSoldiersOpen() {
    barracksBuilt.structure.setUpgradeLevel(TrooperLodge.VOLUNTEER_STATION, 1);
    barracksBuilt.structure.setUpgradeLevel(TrooperLodge.TROOPER_STATION  , 1);
    barracksBuilt.structure.setUpgradeLevel(TrooperLodge.MARKSMAN_TRAINING, 2);
    
    final Base base = base();
    while (base.commerce.numCandidates(Backgrounds.TROOPER) < 3) {
      final Actor applies = Backgrounds.TROOPER.sampleFor(base);
      base.commerce.addCandidate(applies, barracksBuilt, Backgrounds.TROOPER);
    }
  }
  
  
  protected boolean checkRuinsDestroyed() {
    if (ruins.structure.intact()) return false;
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
    final float balance = base().finance.credits();
    if (balance < startingBalance + 1000) return false;
    return true;
  }
  
  
  protected void onTutorialDone() {
    GameSettings.noAdvice = false;
    base().setup.setControlLevel(BaseSetup.LEVEL_ADVISE);
  }
  
  
  
  
  /**  Other helper methods-
    */
  private Venue firstBaseVenue(Class venueClass) {
    for (Object o : world().presences.matchesNear(
      venueClass, null, -1
    )) {
      final Venue found = (Venue) o;
      if (found.base() == base()) return found;
    }
    return null;
  }
  
  
  private Batch <Holding> allBaseHoldings() {
    final Batch <Holding> all = new Batch <Holding> ();
    for (Object o : world().presences.matchesNear(
      Holding.class, null, -1
    )) {
      final Holding h = (Holding) o;
      if (h.base() == base()) all.add(h);
    }
    return all;
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
final VenueProfile NP[] = NativeHut.VENUE_BLUEPRINTS[tribeID];
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



