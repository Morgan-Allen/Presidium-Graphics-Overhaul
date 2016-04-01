/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.game.wild.*;
import stratos.start.PlayLoop;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;
import stratos.content.civic.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.user.UIConstants.*;




public class TutorialScenario extends SectorScenario {
  
  private static boolean
    verbose          = false,
    objectiveVerbose = false;
  
  
  private Bastion bastion = null;
  private Ruins ruinsNear = null, ruinsFar = null;
  
  private TrooperLodge     barracksBuilt = null;
  private EngineerStation  foundryBuilt  = null;
  private StockExchange    marketBuilt   = null;
  private BotanicalStation botanistBuilt = null;
  private boolean          auditorSeen   = false;
  
  private Tile            startAt      = null;
  private MissionRecon    reconSent    = null;
  private MissionStrike   strikeSent   = null;
  private MissionSecurity secureSent   = null;
  private Venue           ruinsTarget  = null;
  private Batch <Drone>   dronesAttack = new Batch();
  
  private Table <String, Target> allTargets = new Table();
  
  
  public TutorialScenario(String prefix) {
    super((Sector) null, "src/stratos/content/hooks/TutorialScript.xml");
    setSavesPrefix(prefix);
  }
  
  
  public TutorialScenario(Session s) throws Exception {
    super(s);
    
    bastion   = (Bastion) s.loadObject();
    ruinsNear = (Ruins  ) s.loadObject();
    ruinsFar  = (Ruins  ) s.loadObject();
    
    barracksBuilt = (TrooperLodge    ) s.loadObject();
    foundryBuilt  = (EngineerStation ) s.loadObject();
    marketBuilt   = (StockExchange   ) s.loadObject();
    botanistBuilt = (BotanicalStation) s.loadObject();
    auditorSeen   = s.loadBool();
    
    startAt      = (Tile           ) s.loadObject();
    reconSent    = (MissionRecon   ) s.loadObject();
    strikeSent   = (MissionStrike  ) s.loadObject();
    secureSent   = (MissionSecurity) s.loadObject();
    ruinsTarget  = (Venue          ) s.loadObject();
    s.loadObjects(dronesAttack);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(bastion  );
    s.saveObject(ruinsNear);
    s.saveObject(ruinsFar );

    s.saveObject(barracksBuilt);
    s.saveObject(foundryBuilt );
    s.saveObject(marketBuilt  );
    s.saveObject(botanistBuilt);
    s.saveBool  (auditorSeen  );
    
    s.saveObject (startAt     );
    s.saveObject (reconSent   );
    s.saveObject (strikeSent  );
    s.saveObject (secureSent  );
    s.saveObject (ruinsTarget );
    s.saveObjects(dronesAttack);
  }
  
  
  protected void clearAllFlags() {
    //
    //  TODO:  Should a script have some generalised methods for handling this?
    bastion       = null;
    ruinsNear     = null;
    ruinsFar      = null;
    
    barracksBuilt = null;
    foundryBuilt  = null;
    marketBuilt   = null;
    botanistBuilt = null;
    auditorSeen   = false;
    
    startAt      = null;
    reconSent    = null;
    strikeSent   = null;
    secureSent   = null;
    ruinsTarget  = null;
    dronesAttack.clear();
  }
  
  
  
  /**  Initial setup-
    */
  private static Expedition createExpedition() {
    final Faction backing = StratosSetting.PLANET_PAREM_V.startingOwner;
    final Expedition e = new Expedition().configFrom(
      StratosSetting.PLANET_PAREM_V, StratosSetting.SECTOR_TERRA, backing,
      Expedition.TITLE_COUNT, 2000, 0, new Batch()
    );
    e.assignLeader(Backgrounds.KNIGHTED.sampleFor(backing));
    return e;
  }
  
  
  protected void initScenario(String prefix) {
    clearAllFlags();
    setupScenario(createExpedition(), new StratosSetting(), prefix);
    super.initScenario(prefix);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    GameSettings.noAdvice = true;
    GameSettings.noShips  = true;
    GameSettings.noSpawn  = true;
    GameSettings.cashFree = true;
    base.advice.setAutonomy(BaseAdvice.LEVEL_AUTO_NO_ADVICE);
    base.finance.setInitialFunding(2500, 0);
  }
  
  
  protected Bastion establishBastion(
    Stage world, Base base, Human ruler,
    List <Human> advisors, List <Human> colonists
  ) {
    bastion = super.establishBastion(world, base, ruler, advisors, colonists);
    for (Actor a : bastion.staff.workers()) if (a != ruler) a.exitWorld();
    return bastion;
  }
  
  
  protected void establishLocals(Stage world) {
    //
    //  I need two ruins here.  One close to the bastion, for demo purposes,
    //  and one far away, as an objective.  (The closer ruins are 'softened up'
    //  a bit as well.)
    final Base artilects = Base.artilects(world);
    final Blueprint RB = Ruins.VENUE_BLUEPRINTS[0];
    ruinsNear = (Ruins) RB.createVenue(artilects);
    ruinsFar  = (Ruins) RB.createVenue(artilects);
    ruinsNear.structure.adjustStats(200, 5, 0, 0, 0, RB.properties);
    
    final SitingPass nearPass = new SitingPass(artilects, ruinsNear) {
      protected float ratePlacing(Target point, boolean exact) {
        float rating = super.ratePlacing(point, exact);
        return rating / (1 + Spacing.zoneDistance(point, bastion));
      }
    };
    final SitingPass farPass = new SitingPass(artilects, ruinsFar) {
      protected float ratePlacing(Target point, boolean exact) {
        float rating = super.ratePlacing(point, exact);
        return rating * (1 + Spacing.zoneDistance(point, bastion));
      }
    };
    farPass.placeState = nearPass.placeState = SitingPass.PLACE_INTACT;
    nearPass.performFullPass();
    farPass .performFullPass();
    
    final Actor boss = Tripod.SPECIES.sampleFor(artilects);
    boss.mind.setHome(ruinsFar);
    boss.enterWorldAt(ruinsFar, world);
  }
  
  
  
  /**  Initial introduction-
    */
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
  
  
  protected void whenMotionDone() {
    ScreenPing.addPingFor("Navigation Done");
  }
  
  
  protected void whenBastionTopicOpen() {
    base().intelMap.liftFogAround(bastion, 12);
    followIfNew(bastion, "bastion_seen");
  }
  
  
  
  /**  First round of security topics-
    */
  protected void whenPlaceBarracksRequestOpen() {
    addPingsLeadingTo(TrooperLodge.BLUEPRINT);
  }
  
  
  protected boolean checkBarracksPlaced() {
    barracksBuilt = (TrooperLodge) firstBaseVenue(TrooperLodge.class);
    if (barracksBuilt == null) return false;
    return true;
  }
  
  
  protected void onBarracksPlaced() {
    barracksBuilt.structure.setState(Structure.STATE_INTACT, 1);
    barracksBuilt.onCompletion();
    base().setup.fillVacancies(barracksBuilt, true);
    base().setup.fillVacancies(bastion      , true);
    followIfNew(barracksBuilt, "barracks_built");
  }
  
  
  protected void whenExploreRequestOpen() {
    if (ruinsNear == null) return;
    followIfNew(ruinsNear, "explore_ruins");
    ScreenPing.addPingFor(UIConstants.RECON_BUTTON_ID);
  }
  
  
  protected boolean checkExploreBegun() {
    if (reconSent == null) {
      final Mission match = firstBaseMission(MissionRecon.class);
      if (match == null) return false;
      reconSent = (MissionRecon) match;
    }
    if (reconSent.applicants().size() == 0) return false;
    if (! reconSent.hasBegun()) return false;
    return true;
  }
  
  
  protected void onExploreBegun() {
    for (Actor a : barracksBuilt.staff.workers()) {
      a.mind.assignMission(reconSent);
      break;
    }
  }
  
  
  
  /**  First round of economic topics-
    */
  protected void whenPlaceFacilitiesRequestOpen() {
    followIfNew(bastion, "placing_facilities");
    if (foundryBuilt == null) addPingsLeadingTo(EngineerStation.BLUEPRINT);
    if (marketBuilt  == null) addPingsLeadingTo(StockExchange  .BLUEPRINT);
  }
  
  
  protected boolean checkFacilitiesPlaced() {
    foundryBuilt = (EngineerStation) firstBaseVenue(EngineerStation.class);
    marketBuilt  = (StockExchange  ) firstBaseVenue(StockExchange  .class);
    if (foundryBuilt == null || marketBuilt == null) return false;
    return true;
  }
  
  
  protected void onFacilitiesPlaced() {
    bastion.structure.setUpgradeLevel(Bastion.LOGISTIC_SUPPORT, 2);
    base().setup.fillVacancies(bastion, true);
    for (Actor a : bastion.staff.workers()) if (! a.inWorld()) {
      a.enterWorldAt(bastion, world());
    }
    
    int num = 0;
    for (Actor a : bastion.staff.workers()) {
      if (a.mind.vocation() == Backgrounds.TECHNICIAN) {
        Venue built = (num++ % 2 == 0) ? marketBuilt : foundryBuilt;
        final Repairs build = new Repairs(a, built, true);
        build.addMotives(Plan.MOTIVE_JOB, Plan.PARAMOUNT);
        a.mind.assignBehaviour(build);
      }
    }
    
    marketBuilt.stocks.clearDemands();
  }
  
  
  protected boolean checkFacilitiesReady() {
    if (marketBuilt == null || foundryBuilt == null) return false;
    if (! marketBuilt .structure.intact()) return false;
    if (! foundryBuilt.structure.intact()) return false;
    return true;
  }
  
  
  protected void onFacilitiesReady() {
    base().setup.fillVacancies(marketBuilt , true);
    base().setup.fillVacancies(foundryBuilt, true);
    PlayLoop.setGameSpeed(1);
  }
  
  
  protected boolean checkFoundryUpgradesReady() {
    if (foundryBuilt == null) return false;
    return hasUpgrade(foundryBuilt, EngineerStation.ASSEMBLY_LINE, 2, false);
  }
  
  
  protected boolean checkMarketUpgradesReady() {
    if (marketBuilt == null) return false;
    return
      hasUpgrade(marketBuilt, StockExchange.RATIONS_VENDING, 1, false) &&
      hasUpgrade(marketBuilt, StockExchange.HARDWARE_STORE , 1, false);
  }
  
  
  protected void onMarketUpgradesReady() {
    if (marketBuilt == null || foundryBuilt == null) return;
    foundryBuilt.structure.advanceUpgrade(1);
    marketBuilt.structure.advanceUpgrade(1);
    marketBuilt.stocks.bumpItem(Economy.PARTS, 10);
    GameSettings.noShips = false;
    world().offworld.journeys.scheduleLocalDrop(base(), 5);
    base().visits.updateVisits(0);
  }
  
  
  protected void whenImportsSetupOpen() {
    ScreenPing.addPingFor(UIConstants.BUDGETS_BUTTON_ID);
  }
  
  
  protected boolean checkImportsSetup() {
    return
      base().demands.allowsImport(Economy.CARBS  ) &&
      base().demands.allowsImport(Economy.PROTEIN);
  }
  
  
  
  
  /**  Second round of security topics-
    */
  protected void whenNearRuinsTopicOpen() {
    base().intelMap.liftFogAround(ruinsNear, 12);
    followIfNew(ruinsNear, "ruins_strike");
    if (UI().selection.selected() == ruinsNear) {
      ScreenPing.addPingFor(UIConstants.STRIKE_BUTTON_ID);
    }
  }
  
  
  protected boolean checkAttackMissionSetup() {
    if (strikeSent == null) {
      final Mission match = firstBaseMission(MissionStrike.class);
      if (match == null) return false;
      else strikeSent = (MissionStrike) match;
    }
    if (strikeSent.assignedPriority() < Mission.PRIORITY_NOMINAL) return false;
    if (strikeSent.applicants().empty()) return false;
    return true;
  }
  
  
  protected void onAttackMissionSetup() {
    for (int n = 2; n-- > 0;) {
      Actor drone = Drone.SPECIES.sampleFor(ruinsNear.base());
      drone.enterWorldAt(ruinsNear, world());
      drone.mind.setHome(ruinsNear);
      if (n == 0) followIfNew(drone, "ruins_under_attack");
    }
  }
  
  
  protected boolean checkNearRuinsDronesDestroyed() {
    if (checkNearRuinsDestroyed()) return true;
    if (ruinsNear == null) return false;
    boolean anyDrones = false;
    for (Actor a : ruinsNear.staff.lodgers()) {
      if (a.health.alive()) return false;
      else anyDrones = true;
    }
    return anyDrones;
  }
  
  
  protected boolean checkNearRuinsDestroyed() {
    if (ruinsNear == null || ! ruinsNear.inWorld()) return true;
    return ruinsNear.destroyed();
  }
  
  
  
  /**  Second round of economic topics-
    */
  protected void whenHiringBasicsTopicOpen() {
    if (barracksBuilt == null || ! barracksBuilt.structure.intact()) return;
    if (foundryBuilt  == null || ! foundryBuilt .structure.intact()) return;
    
    final Tile between = Spacing.bestMidpoint(barracksBuilt, foundryBuilt);
    followIfNew(between, "hiring_begun");
    
    foundryBuilt .structure.setMainUpgradeLevel(2);
    barracksBuilt.structure.setMainUpgradeLevel(2);
    barracksBuilt.structure.setUpgradeLevel(TrooperLodge.FIRING_RANGE, 2);
    
    final Base base = base();
    while (
      barracksBuilt.staff.numOpenings(TROOPER) > 0 &&
      base.visits.numApplicants(TROOPER) < 2
    ) {
      final Actor applies = TROOPER.sampleFor(base);
      base.visits.addCandidate(applies, barracksBuilt, TROOPER);
    }
    while (
      foundryBuilt.staff.numOpenings(ENGINEER) > 0 &&
      base.visits.numApplicants(ENGINEER) < 1
    ) {
      final Actor applies = ENGINEER.sampleFor(base);
      base.visits.addCandidate(applies, foundryBuilt, ENGINEER);
    }
    
    ScreenPing.addPingFor(UIConstants.ROSTER_BUTTON_ID);
  }
  
  
  protected boolean checkHiringDone() {
    if (barracksBuilt == null || foundryBuilt == null) return false;
    if (barracksBuilt.structure.mainUpgradeLevel() < 2) return false;
    if (foundryBuilt .structure.mainUpgradeLevel() < 2) return false;
    return
      barracksBuilt.staff.numOpenings(TROOPER ) == 0 &&
      foundryBuilt .staff.numOpenings(ENGINEER) == 0;
  }
  
  
  
  protected void whenBudgetsTopicOpen() {
    ScreenPing.addPingFor(UIConstants.BUDGETS_BUTTON_ID);
  }
  
  
  protected boolean checkBudgetsPaneOpened() {
    if (! script().topicTriggered("Profits and Loss")) return false;
    if (! (UI().currentInfoPane() instanceof BudgetsPane)) return false;
    final BudgetsPane pane = (BudgetsPane) UI().currentInfoPane();
    if (pane.category() != BudgetsPane.CAT_BUDGET) return false;
    return true;
  }
  
  
  protected void whenExpandingIndustryTopicOpen() {
    if (! hasInstalled(2, EngineerStation.BLUEPRINT, false)) {
      addPingsLeadingTo(EngineerStation.BLUEPRINT);
    }
    if (! hasInstalled(1, ExcavationSite.BLUEPRINT, false)) {
      addPingsLeadingTo(ExcavationSite .BLUEPRINT);
    }
  }
  
  
  protected boolean checkExtraIndustryPlaced() {
    if (! hasInstalled(2, EngineerStation.BLUEPRINT, false)) {
      return false;
    }
    if (! hasInstalled(1, ExcavationSite.BLUEPRINT, false)) {
      return false;
    }
    return true;
  }
  
  
  protected void whenPersonalHousingTopicOpen() {
    addPingsLeadingTo(Holding.BLUEPRINT);
  }
  
  
  protected boolean checkHousingPlaced() {
    if (! checkExtraIndustryPlaced()) return false;
    return hasInstalled(2, Holding.BLUEPRINT, false);
  }
  
  
  protected void whenSupplyDepotTopicOpen() {
    addPingsLeadingTo(SupplyDepot.BLUEPRINT);
  }
  
  
  protected boolean checkSupplyDepotPlaced() {
    if (! checkExtraIndustryPlaced()) return false;
    return hasInstalled(1, SupplyDepot.BLUEPRINT, false);
  }
  
  
  protected boolean checkAuditorSeen() {
    if (! script().topicTriggered("Watch and Learn")) return false;
    if (this.auditorSeen) return true;
    Selectable subject = UI().selection.selected();
    if (subject instanceof Actor) {
      final Actor actor = (Actor) subject;
      if (actor.mind.vocation() == AUDITOR) {
        this.auditorSeen = true;
        onAuditorSeen(actor);
      }
    }
    return false;
  }
  
  
  protected void onAuditorSeen(Actor actor) {
    marketBuilt .stocks.incCredits(75 + Rand.index(50));
    foundryBuilt.stocks.incCredits(25 + Rand.index(50));
    
    if (actor.health.asleep()) {
      actor.health.liftFatigue(100);
      actor.health.setState(ActorHealth.STATE_ACTIVE);
    }
    final Audit audit = Audit.nextOfficialAudit(
      actor, marketBuilt, foundryBuilt
    );
    audit.addMotives(Plan.MOTIVE_JOB, Plan.PARAMOUNT);
    actor.mind.assignBehaviour(audit);
  }
  
  
  protected boolean checkExtraBuildingFinished() {
    boolean done = true;
    done &= hasInstalled(2, EngineerStation.BLUEPRINT, true);
    done &= hasInstalled(1, ExcavationSite .BLUEPRINT, true);
    done &= hasInstalled(2, Holding        .BLUEPRINT, true);
    done &= hasInstalled(1, SupplyDepot    .BLUEPRINT, true);
    done &= checkAuditorSeen();
    return done;
  }
  
  
  protected void onExtraBuildingFinished() {
    GameSettings.cashFree = false;
    for (Object o : world().presences.allMatches(base())) {
      final Venue v = (Venue) o;
      base().setup.fillVacancies(v, false);
    }
    world().offworld.journeys.scheduleLocalDrop(base(), 5);
    base().visits.updateVisits(0);
    base().finance.setInitialFunding(3000, 0);
  }
  
  
  
  /**  Third round of security topics-
    */
  protected void whenBaseAttackTopicOpen() {
    if (dronesAttack.empty()) {
      
      final Pick <Venue> closest = new Pick <Venue> ();
      for (Object o : world().presences.allMatches(base())) {
        final Venue v = (Venue) o;
        closest.compare(v, 0 - Spacing.distance(v, ruinsFar));
      }
      this.ruinsTarget = closest.result();
      
      final Base artilects = Base.artilects(world());
      artilects.setup.fillVacancies(ruinsFar, true);
      final Tile entry = Spacing.bestMidpoint(ruinsTarget, ruinsFar);
      
      if (entry != null) while (dronesAttack.size() < 2) {
        final Drone attacks = (Drone) Drone.SPECIES.sampleFor(artilects);
        attacks.enterWorldAt(entry, world());
        final Combat strike = new Combat(attacks, ruinsTarget);
        strike.addMotives(Plan.MOTIVE_EMERGENCY, Plan.PARAMOUNT);
        attacks.mind.assignBehaviour(strike);
        dronesAttack.add(attacks);
      }
      
      followIfNew(dronesAttack.first(), "base_being_attacked");
    }
    
    base().intelMap.liftFogAround(dronesAttack.first(), 9);
  }
  
  
  protected boolean checkDroneAssaultDestroyed() {
    if (dronesAttack.size() == 0) return false;
    for (Drone drone : dronesAttack) {
      if (drone.health.alive()) return false;
    }
    return true;
  }
  
  
  protected boolean checkFarRuinsFound() {
    if (ruinsFar == null) return false;
    return ruinsFar.visibleTo(base());
  }
  
  
  protected void onFarRuinsFound() {
    if (ruinsFar == null) return;
    followIfNew(ruinsFar, "far_ruins_found");
  }
  
  
  protected boolean checkFarRuinsDestroyed() {
    if (ruinsFar == null) return true;
    if (! ruinsFar.destroyed()) return false;
    for (Actor a : ruinsFar.staff.lodgers()) if (! a.destroyed()) return false;
    return true;
  }
  
  
  protected void onFarRuinsDestroyed() {
  }
  
  
  
  /**  Third round of economic topics-
    */
  protected boolean checkPositiveCashFlow() {
    return base().finance.recentBalance() > 0;
  }
  
  protected boolean haveHoldingUpgrade() {
    for (Venue v : base().listInstalled(Holding.BLUEPRINT, true)) {
      if (((Holding) v).upgradeLevel() > 0) return true;
    }
    return false;
  }
  
  protected boolean checkTutorialComplete() {
    if (! checkFarRuinsDestroyed()) return false;
    if (! checkPositiveCashFlow ()) return false;
    if (! haveHoldingUpgrade    ()) return false;
    return true;
  }
  
  
  
  /**  Other helper methods-
    */
  private void addPingsLeadingTo(Blueprint blueprint) {
    if (! ScreenPing.checkWidgetActive(INSTALL_PANE_ID)) {
      ScreenPing.addPingFor(INSTALL_BUTTON_ID);
    }
    else if (! PlacingTask.isBeingPlaced(blueprint)) {
      ScreenPing.addPingFor(blueprint.keyID);
    }
  }
  
  
  private void followIfNew(Target t, String key) {
    if (allTargets.get(key) != null) return;
    allTargets.put(key, t);
    Selection.pushSelection(null, null);
    UI().tracking.lockOn(t);
  }
  
  
  private boolean hasInstalled(int minBuilt, Blueprint type, boolean intact) {
    return base().listInstalled(type, intact).size() >= minBuilt;
  }
  
  
  private boolean hasUpgrade(Venue v, Upgrade u, int level, boolean intact) {
    final int state = intact ? Structure.STATE_INTACT : Structure.STATE_NONE;
    return v.structure.upgradeLevel(u, state) >= level;
  }
  
  
  private Venue firstBaseVenue(Class venueClass) {
    for (Object o : world().presences.matchesNear(
      venueClass, null, -1
    )) {
      final Venue found = (Venue) o;
      if (found.base() == base()) return found;
    }
    return null;
  }
  
  
  private Mission firstBaseMission(Class missionClass) {
    for (Mission m : base().tactics.allMissions()) {
      if (m.getClass() == missionClass) return m;
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


