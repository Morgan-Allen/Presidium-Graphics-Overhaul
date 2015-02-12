/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.game.base.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.common.Colour;



public class DebugSocial extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugSocial());
  }
  
  
  private DebugSocial() {
    super();
  }
  
  
  public DebugSocial(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_social");
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_social";
  }
  
  
  protected Stage createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      Habitat.ESTUARY     , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final Stage world = new Stage(TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    world.terrain().readyAllMeshes();
    Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.withName(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    world.offworld.assignLocalSector(
      Sectors.SECTOR_PAVONIS,
      Sectors.PLANET_DIAPSOR
    );
    
    if (false) testCareers(base);
    if (false) configDialogueScenario(world, base, UI);
    if (false) configArtilectScenario(world, base, UI);
    if (true ) configContactScenario (world, base, UI);
    if (false) configWildScenario    (world, base, UI);
    if (false) applyJobScenario      (world, base, UI);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
    if (base().finance.credits() < 0) base().finance.incCredits(100, "CHARITY");
  }
  
  
  private void configDialogueScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.noBlood  = true;
    GameSettings.hireFree = true;
    GameSettings.fogFree  = true;
    
    Actor a1 = null, a2 = null;
    for (int n = 2; n-- > 0;) {
      a1 = new Human(Backgrounds.CULTIVATOR, base);
      a1.enterWorldAt(2 + n, 3 + n, world);
      if (a2 == null) a2 = a1;
    }
    
    a1.motives.setSolitude(1);
    a2.motives.setSolitude(1);
    
    final Dialogue d = new Dialogue(a1, a2);
    a1.mind.assignBehaviour(d);
    UI.selection.pushSelection(a1);
    
    
    Actor a3 = new Human(Backgrounds.TECHNICIAN, base);
    Actor a4 = new Human(Backgrounds.TECHNICIAN, base);
    a3.enterWorldAt(7 , 7, world);
    a4.enterWorldAt(10, 5, world);
    
    final Item gift = Item.withAmount(Economy.GREENS, 4);
    a3.gear.addItem(gift);
    
    final Proposal d2 = new Proposal(a3, a4);
    d2.setMotive(Plan.MOTIVE_LEISURE, Plan.ROUTINE);
    d2.setTerms(Pledge.giftPledge(gift, a3, a3, a4), null);
    a3.mind.assignBehaviour(d2);
    
    UI.selection.pushSelection(a3);
    
    //  TODO:  RE-TEST THE ENTIRE GIFT-GETTING ROUTINE, INCLUDING PURCHASES AND
    //  COMMISSIONS.
  }
  
  
  private void configArtilectScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;

    final Base artilects = Base.artilects(world);
    
    final Ruins ruins = new Ruins(artilects);
    Placement.establishVenue(ruins, 20, 20, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    
    UI.assignBaseSetup(artilects, ruins.position(null));
    artilects.setup.fillVacancies(ruins, true);
    
    final Career career = new Career(
      Backgrounds.COMPANION,
      Backgrounds.BORN_GELDER,
      Sectors.PLANET_AXIS_NOVENA,
      null
    );
    final Human subject = new Human(career, base);
    
    I.say("Subject race is: "+Human.raceFor(subject));
    I.say("Subject gender is: "+subject.traits.genderDescription());
    
    subject.enterWorldAt(22, 29, world);
    subject.health.takeInjury(subject.health.maxHealth() + 1, true);
    subject.health.setState(ActorHealth.STATE_DYING);
    UI.selection.pushSelection(subject);
  }
  
  
  private void configContactScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree  = true;
    GameSettings.hireFree = true;
    GameSettings.noBlood  = true;
    //
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    final Actor
      ruler   = new Human(Backgrounds.KNIGHTED     , base),
      consort = new Human(Backgrounds.FIRST_CONSORT, base);
    Placement.establishVenue(
      bastion, 11, 11, true, world,
      ruler, consort
    );
    base.assignRuler(ruler);
    bastion.updateAsScheduled(0, false);
    for (Item i : bastion.stocks.shortages()) bastion.stocks.addItem(i);
    
    final TrooperLodge garrison = new TrooperLodge(base);
    Placement.establishVenue(garrison, world.tileAt(3, 15), true, world);
    
    //
    //  Introduce some natives to contact, some distance away-
    final Base natives = Base.natives(world, NativeHut.TRIBE_FOREST);
    final Actor talks = new Human(Backgrounds.GATHERER, natives);
    talks.enterWorldAt(18, 18, world);
    
    //
    //  Then configure a contact mission asking to secure audience with the
    //  natives.
    final ContactMission peaceMission = new ContactMission(base, talks);
    peaceMission.assignPriority(Mission.PRIORITY_ROUTINE);
    peaceMission.setMissionType(Mission.TYPE_SCREENED);
    final Item gift = Item.withAmount(Economy.PROTEIN, 5);
    peaceMission.setTerms(
      Pledge.giftPledge(gift, bastion, ruler, talks),
      Pledge.audiencePledge(talks, ruler)
    );
    base.tactics.addMission(peaceMission);
    consort.mind.assignMission(peaceMission);
    peaceMission.setApprovalFor(consort, true);
    peaceMission.beginMission();
    
    UI.selection.pushSelection(peaceMission);
  }
  
  
  private void applyJobScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree   = true;
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    
    final Venue applyAt = new EngineerStation(base);
    Placement.establishVenue(applyAt, 4, 4, true, world);
    
    final Venue secondary = new Cantina(base);
    Placement.establishVenue(secondary, 4, 9, true, world);
    base.setup.fillVacancies(secondary, true);
    
    final Venue applyFrom = new EngineerStation(base);
    Placement.establishVenue(applyFrom, 9, 9, true, world,
      new Human(Backgrounds.TECHNICIAN, base)
    );
    
    final Venue powers = new SolarBank(base);
    Placement.establishVenue(powers, 9, 4, true, world);
    
    final Actor applies = applyFrom.staff.workers().first();
    FindWork.assignAmbition(applies, Backgrounds.ARTIFICER, applyAt, 2);
    UI.selection.pushSelection(applies);
  }
  
  
  private void configWildScenario(Stage world, Base base, BaseUI UI) {
    
    //  This is the last scenario to test.  Just introduce some animals, and
    //  see how they react to (A) eachother and (B) the nearby base.
  }
  
  
  private void testCareers(Base base) {
    //
    //  Just checking on the probability distribution in the careers system-
    I.say("\nGenerating random companions:");
    final int runs = 125;
    int numM = 0, numF = 0;
    final Tally <Trait> frequencies = new Tally <Trait> ();
    
    for (int n = runs; n-- > 0;) {
      I.say("\nGENERATING NEW ACTOR...");
      final Human comp = new Human(Backgrounds.COMPANION, base);
      I.say("  Gender for "+comp+" is "+comp.traits.genderDescription());
      if (comp.traits.female()) numF++;
      if (comp.traits.male  ()) numM++;
      frequencies.add(1, Human.raceFor(comp));
    }
    I.say("\nFinal results: ("+runs+" total)");
    I.say("  "+numF+" female");
    I.say("  "+numM+" male"  );
    for (Trait t : frequencies.keysToArray(Trait.class)) {
      I.say("  "+frequencies.valueFor(t)+" "+t);
    }
  }
  
  
  protected void afterCreation() {
  }
}



