


package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.game.plans.FindWork;
import stratos.game.plans.Gifting;
import stratos.game.base.*;
import stratos.game.tactical.*;
import stratos.game.civilian.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;




public class DebugPlans extends Scenario {
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugPlans());
  }
  
  
  private DebugPlans() {
    super();
  }
  
  
  public DebugPlans(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_plans");
  }
  
  
  protected String saveFilePrefix(World world, Base base) {
    return "debug_plans";
  }
  
  
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      Habitat.ESTUARY     , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final World world = new World(TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    world.terrain().readyAllMeshes();
    //Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(World world) {
    return Base.baseWithName(world, "Player Base", false);
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    //configMedicalScenario(world, base, UI);
    //configHuntingScenario(world, base, UI);
    //configCombatScenario(world, base, UI);
    configDialogueScenario(world, base, UI);
    //configPurchaseScenario(world, base, UI);
    //configRaidScenario(world, base, UI);
    //configArtilectScenario(world, base, UI);
    //configContactScenario(world, base, UI);
    //configWildScenario(world, base, UI);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
    if (base().credits() < 0) base().incCredits(100);
  }
  
  
  private void configMedicalScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    
    final Actor treats = new Human(Backgrounds.PHYSICIAN, base);
    Placement.establishVenue(
      new Sickbay(base), 6, 6, true, world,
      treats,
      new Human(Backgrounds.MINDER, base),
      new Human(Backgrounds.MINDER, base)
    );
    
    final Actor patient = new Human(Backgrounds.VETERAN, base);
    patient.enterWorldAt(world.tileAt(10, 10), world);
    patient.health.takeInjury(patient.health.maxHealth());
    
    UI.selection.pushSelection(patient, true);
  }
  
  
  private void configHuntingScenario(World world, Base base, BaseUI UI) {
    //GameSettings.fogFree = true;
    GameSettings.buildFree = true;
    //GameSettings.noBlood = true;
    
    final Actor hunts = new Human(Backgrounds.EXPLORER, base);
    final Venue station = new SurveyStation(base);
    Placement.establishVenue(
      station, 6, 6, true, world,
      new Human(Backgrounds.EXPLORER, base),
      new Human(Backgrounds.EXPLORER, base),
      hunts
    );
    
    final Base wildlife = Base.baseWithName(world, Base.KEY_WILDLIFE, true);
    final Actor prey = new Vareen(wildlife);
    prey.enterWorldAt(world.tileAt(9, 9), world);
    
    //prey.health.takeFatigue(prey.health.maxHealth());
    //hunts.mind.assignBehaviour(Hunting.asHarvest(hunts, prey, station));
    UI.selection.pushSelection(hunts, true);
    
    Nest.placeNests(world, Species.HAREEN, Species.QUD);
  }
  
  
  private void configCombatScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    //GameSettings.noBlood = true;
    
    Actor soldier = null;
    for (int n = 1; n-- > 0;) {
      soldier = new Human(Backgrounds.KNIGHTED, base);
      soldier.enterWorldAt(world.tileAt(4, 4), world);
    }
    
    final Actor civilian = new Human(Backgrounds.STOCK_VENDOR, base);
    civilian.enterWorldAt(world.tileAt(5, 4), world);
    civilian.health.takeInjury(civilian.health.maxHealth() * 2);
    
    final Base artilects = Base.baseWithName(world, Base.KEY_ARTILECTS, true);
    final Actor threat = new Tripod(artilects);
    threat.enterWorldAt(world.tileAt(8, 6), world);
    
    UI.selection.pushSelection(threat, true);
  }
  
  
  private void configDialogueScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    GameSettings.noBlood = true;
    GameSettings.hireFree = true;
    
    final Artificer venue = new Artificer(base);
    Placement.establishVenue(venue, world.tileAt(4, 4), true, world);
    venue.stocks.bumpItem(Economy.PARTS, 10);
    
    Actor citizen = null, other = null;
    for (int n = 2; n-- > 0;) {
      citizen = new Human(Backgrounds.CULTIVATOR, base);
      citizen.enterWorldAt(venue, world);
      if (other == null) other = citizen;
    }
    
    UI.selection.pushSelection(citizen, true);
    
    world.presences.togglePresence(
      venue, venue.origin(), true, Economy.PARTS.supplyKey
    );
    final Delivery getting = DeliveryUtils.bestCollectionFor(
      citizen, Economy.PARTS, 1, null, 5, false
    );
    getting.shouldPay = null;
    
    final Item gift = Item.withAmount(Economy.PARTS, 1);
    final Gifting gifting = new Gifting(
      citizen, other, gift, getting
    );
    citizen.mind.assignBehaviour(gifting);
  }
  
  
  private void configPurchaseScenario(World world, Base base, BaseUI UI) {
    GameSettings.needsFree = true;
    
    Actor citizen = null;
    for (int n = 2; n-- > 0;) {
      citizen = new Human(Backgrounds.RUNNER, base);
      citizen.enterWorldAt(world.tileAt(10 + n, 10 + n), world);
      citizen.gear.incCredits(1000);
    }
    UI.selection.pushSelection(citizen, true);
    final Venue foundry = new Artificer(base);
    Placement.establishVenue(
      foundry, 6, 6, true, world,
      new Human(Backgrounds.TECHNICIAN, base),
      new Human(Backgrounds.TECHNICIAN, base),
      new Human(Backgrounds.ARTIFICER, base)
    );
    foundry.stocks.bumpItem(Economy.METALS, 40);
    foundry.stocks.bumpItem(Economy.PARTS, 20);
  }
  
  
  private void configRaidScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = false;
    //GameSettings.hireFree = true;
    
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    Placement.establishVenue(bastion, 11, 11, true, world);
    FindWork.fillVacancies(bastion, true);
    
    //  And introduce ruins, with a complement of artilects.
    final Base artilects = Base.baseWithName(world, Base.KEY_ARTILECTS, true);
    final Ruins ruins = new Ruins(artilects);
    Placement.establishVenue(ruins, 44, 44, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    Ruins.populateArtilects(world, ruins, true);
    
    UI.selection.pushSelection(bastion, true);
  }
  
  
  private void configArtilectScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;

    final Base artilects = Base.baseWithName(world, Base.KEY_ARTILECTS, true);
    final Ruins ruins = new Ruins(artilects);
    Placement.establishVenue(ruins, 20, 20, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    Artilect lives = null;
    
    Ruins.populateArtilects(
      world, ruins, true,
      new Tripod(artilects),
      lives = new Cranial(artilects)
    );
    
    final Human subject = new Human(Backgrounds.AESTHETE, base);
    subject.enterWorldAt(ruins, world);
    subject.health.takeInjury(subject.health.maxHealth() + 1);
    subject.health.setState(ActorHealth.STATE_STRICKEN);
    
    UI.selection.pushSelection(lives, true);
  }
  
  
  private void configContactScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    GameSettings.hireFree = true;
    GameSettings.noBlood = true;
    
    //  TODO:  Problem- at the moment, default hostilities between the bases
    //  are enough to trigger immediate combat behaviours (at least among the
    //  more aggressively inclined.)
    
    //  You need to take the same 'default empathy' method from the First Aid
    //  behaviour and apply it here in reverse.  Most sympathy for same base
    //  and species.
    
    //  That might not be enough, though.
    
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    bastion.stocks.bumpItem(Economy.PROTEIN, 20);
    bastion.stocks.bumpItem(Economy.PLASTICS, 10);
    bastion.stocks.bumpItem(Economy.TRUE_SPICE, 5);
    final Actor ruler = new Human(Backgrounds.KNIGHTED, base);
    Placement.establishVenue(
      bastion, 11, 11, true, world,
      ruler, new Human(Backgrounds.FIRST_CONSORT, base)
    );
    base.assignRuler(ruler);
    
    final Garrison garrison = new Garrison(base);
    Placement.establishVenue(garrison, world.tileAt(3, 15), true, world);
    
    //  And introduce a native camp.
    final Base natives = Base.baseWithName(world, Base.KEY_NATIVES, true);
    final NativeHut hut = NativeHut.newHall(NativeHut.TRIBE_FOREST, natives);
    Placement.establishVenue(hut, 21, 21, true, world);
    final Batch <Actor> lives = NativeHut.populateHut(hut, null);
    //UI.selection.pushSelection(lives.first(), true);
    
    final Mission peaceMission = new ContactMission(base, hut);
    peaceMission.assignPriority(Mission.PRIORITY_ROUTINE);
    peaceMission.setObjective(ContactMission.OBJECT_AUDIENCE);
    base.addMission(peaceMission);
    peaceMission.beginMission();
    
    UI.selection.pushSelection(peaceMission, true);
  }
  
  
  private void configWildScenario(World world, Base base, BaseUI UI) {
    
    //  This is the last scenario to test.  Just introduce some animals, and
    //  see how they react to (A) eachother and (B) the nearby base.
  }
  
  
  protected void afterCreation() {
  }
}



