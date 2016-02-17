/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.test;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.start.PlayLoop;
import stratos.start.Scenario;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.content.civic.*;
import stratos.content.hooks.*;
import static stratos.content.hooks.StratosSetting.*;
import stratos.user.*;
import stratos.util.*;




public class DebugMissions extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugMissions());
  }
  
  
  private DebugMissions() {
    super("debug_missions", true);
  }
  
  
  public DebugMissions(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_missions");
  }
  
  
  protected Stage createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      Habitat.FOREST      , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final Verse verse = new StratosSetting();
    final Sector at = SECTOR_ELYSIUM;
    final Stage world = Stage.createNewWorld(verse, at, TG.generateTerrain());
    Flora.populateFlora(world);
    world.readyAfterPopulation();
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    Base base = Base.settlement(world, "Player Base", Faction.FACTION_ALTAIR);
    base.research.initKnowledgeFrom(PLANET_HALIBAN);
    base.finance.setInitialFunding(11000, 0);
    return base;
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = false;
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    GameSettings.fogFree   = true;
    
    if (true ) offworldClaimScenario  (world, base, UI);
    if (false) offworldRaidingScenario(world, base, UI);
    if (false) offworldReconScenario  (world, base, UI);
    
    if (false) strikeScenario  (world, base, UI);
    if (false) securityScenario(world, base, UI);
    if (false) contactScenario (world, base, UI);
    if (false) reconScenario   (world, base, UI);
  }
  
  
  private void offworldClaimScenario(Stage world, Base base, BaseUI UI) {
    final Venue HQ = new Bastion(base);
    SiteUtils.establishVenue(HQ, 5, 5, true, world);
    base.setup.fillVacancies(HQ, true);
    base.assignRuler(HQ.staff.workers().first());
    
    MissionClaiming m = MissionClaiming.claimFor(SECTOR_PAVONIS, base);
    m.assignPriority(Mission.PRIORITY_ROUTINE);
    base.tactics.addMission(m);
    
    //  TODO:
    //  This case should be satisfied when the applicants for the mission are
    //  offworld, the journey completes, and SECTOR_PAVONIS now belongs to the
    //  same faction.
  }
  
  
  private void offworldRaidingScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue HQ = new Bastion(base);
    SiteUtils.establishVenue(HQ, 5, 5, true, world);
    base.setup.fillVacancies(HQ, true);
    base.assignRuler(HQ.staff.workers().first());
    
    final Verse verse = world.offworld;
    CivicBase procyon = Base.settlement(world, null, Faction.FACTION_PROCYON);
    verse.baseForSector(PLANET_PAREM_V).updateAsScheduled(0, false);
    
    procyon.visits.attemptRaidingVisit(
      CivicBase.AVG_SECTOR_POWER, 10,
      PLANET_PAREM_V, new Airship(procyon), CivicBase.RAID_CLASSES
    );
    
    //  TODO:
    //  This case should be satisfied when raiders arrive from offworld and
    //  successfully destroy at least one base structure.
  }
  
  
  private void offworldReconScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fastTrips = true;
    
    final Venue HQ = new Bastion(base);
    SiteUtils.establishVenue(HQ, 5, 5, true, world);
    base.setup.fillVacancies(HQ, true);
    base.assignRuler(HQ.staff.workers().first());
    
    final Venue home = new TrooperLodge(base);
    SiteUtils.establishVenue(home, 8, 5, true, world);
    base.setup.fillVacancies(home, true);
    
    final Mission recon = MissionRecon.reconFor(SECTOR_PAVONIS, base);
    recon.assignPriority(Mission.PRIORITY_ROUTINE);
    recon.setMissionType(Mission.TYPE_SCREENED   );
    base.tactics.addMission(recon);
    
    for (Actor a : home.staff.workers()) {
      a.mind.assignMission(recon);
      recon.setApprovalFor(a, true);
    }
    recon.beginMission();
    
    //  TODO:
    //  This case should be satisfied when the recon-mission completes, the
    //  foreign sector is explored and the applicants return home from off-map.
  }
  
  
  private void contactScenario(Stage world, Base base, BaseUI UI) {
    final Base natives = Base.natives(world, NativeHut.TRIBE_FOREST);
    Faction.setMutualFactionRelations(natives, base, -0.25f);
    
    final NativeHut hut = NativeHut.newHall(1, natives);
    SiteUtils.establishVenue(hut, 4, 4, true, world);
    
    for (int i = 3; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.GATHERER, natives);
      actor.mind.setHome(hut);
      actor.mind.setWork(hut);
      actor.enterWorldAt(hut, world);
    }
    
    final Mission contact = new MissionContact(base, hut);
    contact.setMissionType(Mission.TYPE_SCREENED  );
    contact.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 1; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      actor.enterWorldAt(2, 2, world);
      actor.mind.assignMission(contact);
      contact.setApprovalFor(actor, true);
      Selection.pushSelection(actor, null);
    }
    
    base.tactics.addMission(contact);
    contact.beginMission();
    
    //  TODO:
    //  This case should be satisfied when the mission completes, and the
    //  native in question agrees to come visit the applicant.  (If the mission
    //  completes without that agreement, log as a failure.)
  }
  
  
  private void securityScenario(Stage world, Base base, BaseUI UI) {
    final Base artilects = Base.artilects(world);
    
    final Actor robot = Tripod.SPECIES.sampleFor(artilects);
    robot.enterWorldAt(32, 32, world);
    
    final Actor vet = new Human(Backgrounds.TROOPER, base);
    vet.enterWorldAt(4, 4, world);
    
    final Combat combat = new Combat(robot, vet);
    combat.addMotives(Plan.MOTIVE_EMERGENCY, 100);
    robot.mind.assignBehaviour(combat);
    
    final Mission security = MissionSecurity.securityFor(vet, base);
    security.setMissionType(Mission.TYPE_SCREENED  );
    security.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 5; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      final Tile entry = Spacing.pickRandomTile(vet, 4, world);
      actor.enterWorldAt(entry.x, entry.y, world);
      actor.mind.assignMission(security);
      security.setApprovalFor(actor, true);
      Selection.pushSelection(actor, null);
    }
    
    base.tactics.addMission(security);
    security.beginMission();
    
    //  TODO:
    //  This case should be satisfied when the robot is destroyed.
  }
  
  
  private void strikeScenario(Stage world, Base base, BaseUI UI) {
    final Base artilects = Base.artilects(world);
    Faction.setMutualFactionRelations(artilects, base, -1);
    
    final Ruins ruin = new Ruins(artilects);
    SiteUtils.establishVenue(ruin, 12, 12, true, world);
    
    final Mission strike = MissionStrike.strikeFor(ruin, base);
    strike.setMissionType(Mission.TYPE_SCREENED  );
    strike.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 4; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      final Tile entry = Spacing.pickRandomTile(world.tileAt(4, 4), 4, world);
      actor.enterWorldAt(entry.x, entry.y, world);
      actor.mind.assignMission(strike);
      strike.setApprovalFor(actor, true);
      Selection.pushSelection(actor, null);
    }
    
    base.tactics.addMission(strike);
    strike.beginMission();
    
    //  TODO:
    //  This case should be satisfied when the ruins and all it's inhabitants
    //  are destroyed.
  }
  
  
  private void reconScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = false;
    
    final Mission recon = MissionRecon.reconFor(world.tileAt(20, 20), base);
    recon.setMissionType(Mission.TYPE_SCREENED  );
    recon.assignPriority(Mission.PRIORITY_URGENT);
    
    for (int i = 3; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      final Tile entry = Spacing.pickRandomTile(world.tileAt(4, 4), 4, world);
      actor.enterWorldAt(entry.x, entry.y, world);
      actor.mind.assignMission(recon);
      recon.setApprovalFor(actor, true);
      Selection.pushSelection(actor, null);
    }
    
    base.tactics.addMission(recon);
    recon.beginMission();
    
    //  TODO:
    //  This case should be satisfied when the terrain around the target tile
    //  is fully revealed.
  }
  
  
  protected void afterCreation() {
  }
}










