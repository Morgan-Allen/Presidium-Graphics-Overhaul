/**  
  *  Written by Morgan Allen and Jaroslaw Jedynak.
  *  
  *  Feel free to poke around for non-commercial purposes.
  */
package stratos.test;
import stratos.content.civic.*;
import stratos.content.hooks.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;
import stratos.content.abilities.*;



public class DebugCombat extends AutomatedScenario {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  Batch <Actor> ourSoldiers   = new Batch();
  Batch <Actor> enemySoldiers = new Batch();

  Background usesTechniques;
  Technique toUse[];
  Batch <Batch <Technique>> techsUsed = new Batch();
  
  
  private DebugCombat(TestCase testCase) {
    super("debug_combat", testCase);
  }
  
  
  public DebugCombat(Session s) throws Exception {
    super(s);
    s.loadObjects(ourSoldiers  );
    s.loadObjects(enemySoldiers);
    toUse = (Technique[]) s.loadObjectArray(Technique.class);
    for (int n = ourSoldiers.size(); n-- > 0;) {
      techsUsed.add((Batch <Technique>) s.loadObjects(new Batch()));
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(ourSoldiers  );
    s.saveObjects(enemySoldiers);
    s.saveObjectArray(toUse);
    for (Batch <Technique> used : techsUsed) s.saveObjects(used);
  }
  
  
  
  /**  Initial setup methods for before/after construction-
    */
  public static void main(String args[]) {
    AutomatedTestRunner.setupAutomatedTest(getTestCases(), "debug_combat_log.txt");
  }
  
   
  public void beginGameSetup() {
    super.initScenario("debug_combat");
  }
  
  
  protected void afterCreation() {
    return;
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
    final Sector at = StratosSetting.SECTOR_ELYSIUM;
    final Stage world = Stage.createNewWorld(verse, at, TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    world.readyAfterPopulation();
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.settlement(world, "Player Base", Faction.FACTION_ALTAIR);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    GameSettings.noChat    = true;
    super.configureScenario(world, base, UI);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
    ///if (PlayLoop.stateUpdates() % 10 == 0) PathingMap.reportObs();
  }
  

  
  /**  Test evaluation and duration, plus actual scenario definitions-
    */
  protected TestResult getCurrentResult() {
    boolean
      ourSoldiersBeat   = everyoneIsBeaten(ourSoldiers),
      enemySoldiersBeat = everyoneIsBeaten(enemySoldiers),
      allTechniquesUsed = everyoneUsedTechniques(ourSoldiers);
    
    if (ourSoldiersBeat || enemySoldiersBeat) {
      if (allTechniquesUsed) {
        return TestResult.PASSED;
      }
      else {
        return TestResult.FAILED;
      }
    }
    // still waiting
    return TestResult.UNKNOWN;
  }
  
  
  boolean everyoneIsBeaten(Batch <Actor> actors) {
    if (actors.empty()) return false;
    for (Actor actor : actors) {
      if (actor.isDoing(Retreat.class, null)) continue;
      if (! actor.health.conscious()) continue;
      return false;
    }
    return true;
  }
  
  
  boolean everyoneUsedTechniques(Batch <Actor> actors) {
    boolean allKnownTechniquesUsed = true;
    for (Actor actor : actors) if (actor.mind.vocation() == usesTechniques) {
      if (! techniquesWereUsedBy(actor, toUse)) {
        allKnownTechniquesUsed = false;
      }
    }
    return allKnownTechniquesUsed;
  }
  
  
  boolean techniquesWereUsedBy(Actor using, Technique known[]) {
    final int index = ourSoldiers.indexOf(using);
    if (index < 0 || known == null) return false;
    
    final Batch <Technique> used = techsUsed.atIndex(index);
    for (Technique t : known) if (Technique.isDoingAction(using, t)) {
      used.include(t);
    }
    for (Technique t : known) if (! used.includes(t)) return false;
    return true;
  }
  

  abstract static class CombatTestCase extends TestCase {
    DebugCombat scenario;
    
    CombatTestCase(String name) {
      super(name);
    }

    AutomatedScenario initScenario() {
      scenario = new DebugCombat(this);
      return scenario;
    }

    TestResult currentResult() {
      return scenario.getCurrentResult();
    }
    
    protected long getMaxTestDuration() {
      return 60 * 3;
    }
  }


  public static Stack <TestCase> getTestCases() {
    Stack<TestCase> result = new Stack<TestCase>();

    result.add(new CombatTestCase("Trooper vs. Yamagur") {
      void setupScenario(Stage world, Base base, BaseUI UI) {
        scenario.setupCombatScenario(
                world, base, UI,
                new Background[] { TROOPER, TROOPER },
                TrooperTechniques.TROOPER_TECHNIQUES,
                new Species[] { Yamagur.SPECIES },
                Base.wildlife(world), true
        );
      }
    });

    result.add(new CombatTestCase("Ecologist vs. Yamagur") {
      void setupScenario(Stage world, Base base, BaseUI UI) {
        scenario.setupCombatScenario(
                world, base, UI,
                new Background[] { ECOLOGIST },
                EcologistTechniques.ECOLOGIST_TECHNIQUES,
                new Species[] { Yamagur.SPECIES },
                Base.wildlife(world), true
        );
      }
    });

    result.add(new CombatTestCase("Runner vs. Tripod") {
      void setupScenario(Stage world, Base base, BaseUI UI) {
        scenario.setupCombatScenario(
                world, base, UI,
                new Background[] { RUNNER, RUNNER },
                RunnerTechniques.RUNNER_TECHNIQUES,
                new Species[] { Tripod.SPECIES },
                Base.artilects(world), true
        );
      }
    });

    result.add(new CombatTestCase("Physician + Trooper vs. Runner") {
      void setupScenario(Stage world, Base base, BaseUI UI) {
        scenario.setupCombatScenario(
                world, base, UI,
                new Background[] { PHYSICIAN, TROOPER, TROOPER, EXCAVATOR },
                PhysicianTechniques.PHYSICIAN_TECHNIQUES,
                new Background[] { RUNNER, RUNNER },
                Base.artilects(world), true
        );
      }
    });

    result.add(new CombatTestCase("Artilect raid vs. Troopers") {
      void setupScenario(Stage world, Base base, BaseUI UI) {
        scenario.raidingScenario(world, base, UI);
      }
    });

    return result;
  }
  
  
  
  /**  Helper methods for scenario setup-
    */
  private void raidingScenario(Stage world, Base base, BaseUI UI) {
    //
    //  Introduce a bastion, with standard personnel.
    Flora.populateFlora(world);
    GameSettings.fastRaids = true;
    
    final Sector homeworld = StratosSetting.PLANET_PAREM_V;
    base.research.initKnowledgeFrom(homeworld);
    base.visits.assignHomeworld  (homeworld);
    base.finance.setInitialFunding(10000, 0);
    
    final Bastion bastion = new Bastion(base);
    SiteUtils.establishVenue(bastion, 11, 11, true, world);
    base.setup.fillVacancies(bastion, true);
    
    final Box2D perim = bastion.area(new Box2D()).expandBy(8);
    SiteUtils.placeAroundPerimeter(ShieldWall.BLUEPRINT, perim, base, true);
    
    for (int n = 2; n-- > 0;) {
      final TrooperLodge barracks = new TrooperLodge(base);
      SiteUtils.establishVenue(barracks, barracks, true, world);
      base.setup.fillVacancies(barracks, true);
      Visit.appendTo(ourSoldiers, barracks.staff.workers());
    }
    
    
    //  And introduce ruins, with a complement of artilects.
    final ArtilectBase artilects = Base.artilects(world);
    Faction.setMutualFactionRelations(artilects, base, -0.5f);
    artilects.setOnlineLevel(0.25f);
    
    final Blueprint ruinsType = Ruins.VENUE_BLUEPRINTS[0];
    final Ruins ruins = (Ruins) ruinsType.createVenue(artilects);
    SiteUtils.establishVenue(ruins, 44, 44, true, world);
    
    final Batch <Actor> lurking = new Batch();
    for (Species s : Species.ARTILECT_SPECIES) for (int n = 2; n-- > 0;) {
      Actor a = s.sampleFor(artilects);
      lurking.add(a);
    }
    artilects.setup.fillVacancies(ruins, true, lurking.toArray(Actor.class));
    Visit.appendTo(enemySoldiers, lurking);
    
    Selection.pushSelection(ruins.staff.workers().first(), null);
  }

  
  private void setupCombatScenario(
    Stage world, Base base, BaseUI UI,
    Background selfTypes[], Technique techniques[],
    Background otherTypes[], Base otherBase, boolean otherFights
  ) {
    //
    //  First, create the set of soldiers on 'our' side-
    Batch <Actor> soldiers = new Batch();
    Background mainType = selfTypes[0];
    Venue lair = null;
    
    for (Background b : selfTypes) {
      Actor soldier = b.sampleFor(base);
      soldier.enterWorldAt(4 + Rand.index(2), 4 + Rand.index(2), world);
      soldiers.add(soldier);
      
      if (mainType == b && techniques != null) for (Technique t : techniques) {
        if (t.isItemDerived()) {
          soldier.gear.bumpItem(t.itemNeeded(), 1);
        }
        else {
          if (t.itemNeeded() != null) soldier.gear.bumpItem(t.itemNeeded(), 1);
          soldier.skills.addTechnique(t);
        }
      }
      
      base.intelMap.liftFogAround(soldier, 9);
      Selection.pushSelection(soldier, null);
    }
    //
    //  Save the list of soldiers generated (and create a blank record of the
    //  Techniques used by each.)
    this.ourSoldiers = soldiers;
    this.toUse = techniques;
    this.usesTechniques = mainType;
    this.techsUsed.clear();
    for (Actor a : soldiers) techsUsed.add(new Batch());
    
    //  Next, generate the enemy soldiers-
    Batch <Actor> enemySoldiers = new Batch();
    if (otherTypes != null) for (Background b : otherTypes) {
      Actor other = b.sampleFor(otherBase);
      other.enterWorldAt(9 + Rand.index(3), 9 + Rand.index(3), world);
      other.health.setMaturity(0.8f);
      enemySoldiers.add(other);
      
      if (lair == null) {
        lair = NestUtils.createNestFor(other);
        if (lair != null) {
          lair.structure.setState(Structure.STATE_INTACT, 0.5f);
          SiteUtils.establishVenue(lair, 50, 50, true, world);
        }
      }
      if (lair != null) {
        other.mind.setHome(lair);
      }

      final Actor enemy = (Actor) Rand.pickFrom(soldiers);
      if (otherFights) {
        final Combat c = new Combat(other, enemy);
        c.addMotives(Plan.NO_PROPERTIES, Plan.PARAMOUNT);
        other.mind.assignBehaviour(c);
        Selection.pushSelection(enemy, null);
      }
      else {
        final Combat c = new Combat(enemy, other);
        c.addMotives(Plan.NO_PROPERTIES, Plan.PARAMOUNT);
        enemy.mind.assignBehaviour(c);
        final Retreat r = new Retreat(other, lair);
        r.addMotives(Plan.NO_PROPERTIES, Plan.PARAMOUNT);
        other.mind.assignBehaviour(r);
        Selection.pushSelection(other, null);
      }
    }
    this.enemySoldiers = enemySoldiers;
  }
}









