

package stratos.start;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.common.Colour;

import java.io.*;



//  Planitia Sector (robots + ruins, artifact.)
//  Pavonis Sector (mild settlement, pacified.)
//  Aeolis Sector (desert plus wildlife.)
//  Marineris Sector (neutral + water/islands.  Some wildlife.)

public class StartupScenario extends Scenario {
  
  
  final public static int
    MAX_SKILLS    = 3,
    MAX_TRAITS    = 2,
    MAX_POWERS    = 3,
    MAX_ADVISORS  = 2,
    MAX_COLONISTS = 9,
    MAX_PERKS     = 3;
  final public static Background ADVISOR_BACKGROUNDS[] = {
    Backgrounds.FIRST_CONSORT,
    Backgrounds.MINISTER_FOR_ACCOUNTS,
    Backgrounds.WAR_MASTER
  };
  final public static Background COLONIST_BACKGROUNDS[] = {
    Backgrounds.VOLUNTEER,
    Backgrounds.SUPPLY_CORPS,
    Backgrounds.FABRICATOR,
    Backgrounds.TECHNICIAN,
    Backgrounds.CULTIVATOR,
    Backgrounds.MINDER
  };
  final public static String
    SITE_DESC[] = {
      "Wasteland",
      "Wilderness",
      "Settled"
    },
    FUNDING_DESC[] = {
      "Minimal  (7500  Credits, 3% interest)",
      "Standard (10000 Credits, 2% interest)",
      "Generous (12500 Credits, 1% interest)"
    },
    TITLE_MALE[] = {
      "Knighted Lord (Small Estate)",
      "Count (Typical Estate)",
      "Baron (Large Estate)"
    },
    TITLE_FEMALE[] = {
      "Knighted Lady (Small Estate)",
      "Countess (Typical Estate)",
      "Baroness (Large Estate)"
    };
  final public static int
    MAP_SIZES[] = { 64, 128, 256 },
    
    SITE_WASTELAND  = 0,
    SITE_WILDERNESS = 1,
    SITE_SETTLED    = 2,
    
    FUNDING_MINIMAL  = 0,
    FUNDING_STANDARD = 1,
    FUNDING_GENEROUS = 2,
    
    TITLE_KNIGHTED = 0,
    TITLE_COUNT    = 1,
    TITLE_BARON    = 2;
  
  public static class Config {
    //  TODO:  Just pick House, Province, Options.  And a few perks.
    
    public Background house ;
    public Background gender;
    public List <Trait    > chosenTraits = new List <Trait    > ();
    public List <Skill    > chosenSkills = new List <Skill    > ();
    public List <Technique> chosenTechs  = new List <Technique> ();
    
    public List  <Background> advisors = new List  <Background> ();
    public Tally <Background> crew     = new Tally <Background> ();
    public Tally <Blueprint > built    = new Tally <Blueprint > ();
    public VerseLocation demesne;
    public int siteLevel, fundsLevel, titleLevel;
  }
  
  final Config config;
  
  
  
  public StartupScenario(Config config, String prefix) {
    super(prefix, false);
    this.config = config;
  }
  
  
  public StartupScenario(Session s) throws Exception {
    super(s);
    this.config = new Config();
    config.house  = (Background) s.loadObject();
    config.gender = (Background) s.loadObject();
    s.loadObjects(config.chosenTraits);
    s.loadObjects(config.chosenSkills);
    s.loadObjects(config.chosenTechs );
    
    s.loadObjects(config.advisors    );
    for (int i = s.loadInt(); i-- > 0;) {
      config.crew.set((Background) s.loadObject(), s.loadInt());
    }
    for (int i = s.loadInt(); i-- > 0;) {
      config.built.set((Blueprint) s.loadObject(), s.loadFloat());
    }
    config.demesne    = (VerseLocation) s.loadObject();
    config.siteLevel  = s.loadInt();
    config.fundsLevel = s.loadInt();
    config.titleLevel = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject (config.house       );
    s.saveObject (config.gender      );
    s.saveObjects(config.chosenTraits);
    s.saveObjects(config.chosenSkills);
    s.saveObjects(config.chosenTechs );
    s.saveObjects(config.advisors    );
    s.saveInt(config.crew.size());
    for (Background b : config.crew.keys()) {
      s.saveObject(b);
      s.saveInt((int) config.crew.valueFor(b));
    }
    s.saveInt(config.built.size());
    for (Blueprint b : config.built.keys()) {
      s.saveObject(b);
      s.saveFloat(config.built.valueFor(b));
    }
    s.saveObject(config.demesne);
    s.saveInt(config.siteLevel );
    s.saveInt(config.fundsLevel);
    s.saveInt(config.titleLevel);
  }
  
  
  
  /**  Required setup methods-
    */
  protected Stage createWorld() {
    final int station = config.titleLevel;
    float water = 2;
    float forest = 0, meadow = 0, barrens = 0, desert = 0, wastes = 0;
    
    switch (config.siteLevel) {
      case(0) : wastes = 3; desert  = 2; barrens = 4; water = 0; break;
      case(1) : meadow = 4; barrens = 2; desert  = 2; water = 1; break;
      case(2) : forest = 2; meadow  = 3; barrens = 2; water = 2; break;
    }
    
    //  TODO:  the terrain setup algorithm should not be directly interacting
    //  with the world, only passing data onto the constructor.  The
    //  'readyAllMeshes()' method should be called automatically then.
    final TerrainGen TG = new TerrainGen(
      MAP_SIZES[station], 0.2f,
      Habitat.OCEAN       , water  ,
      Habitat.ESTUARY     , forest ,
      Habitat.MEADOW      , meadow ,
      Habitat.BARRENS     , barrens,
      Habitat.DUNE        , desert ,
      Habitat.CURSED_EARTH, wastes
    );
    final Stage world = new Stage(TG.generateTerrain());
    TG.setupMinerals(world, 1, 0, 0.5f);
    TG.setupOutcrops(world);
    world.terrain().readyAllMeshes();
    
    Flora.populateFlora(world);
    
    //  TODO:  THIS NEEDS TO BE CONFIGURED EXTERNALLY!
    if (config.demesne == null) config.demesne = Verse.SECTOR_ELYSIUM;
    world.offworld.assignStageLocation(config.demesne);
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    final Base base = Base.settlement(world, "Player Base", Colour.LITE_BLUE);
    
    int funding = -1, interest = -1;
    switch (config.fundsLevel) {
      case(0) : funding = 7500 ; interest = 3; break;
      case(1) : funding = 10000; interest = 2; break;
      case(2) : funding = 12500; interest = 1; break;
    }
    base.finance.setInitialFunding(funding, interest);
    base.commerce.assignHomeworld((VerseLocation) config.house);
    return base;
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    //
    //  Determine relevant attributes for the ruler-
    final Human ruler = ruler(base);
    //
    //  Try to pick out some complementary advisors-
    final List <Human> advisors = advisors(base, ruler);
    //
    //  Pick out some random colonists-
    final List <Human> colonists = colonists(base);
    //
    //  Establish the position of the base site-
    final Bastion bastion = establishBastion(
      world, base, ruler, advisors, colonists
    );
    UI.assignBaseSetup(base, bastion.position(null));
    //
    //  Establish starting positions for the locals.  (We actually do this last
    //  largely for balance reasons, though strictly speaking the locals would
    //  of course be there first.)
    establishLocals(world);
  }
  
  
  protected void afterCreation() {
    //saveProgress(false);
  }
  
  
  
  /**  Private helper methods-
    */
  protected Human ruler(Base base) {
    //
    //  Firstly, we determine the ruler's current rank in the feudal hierarchy
    //  and their class of origin.
    final int station = config.titleLevel;
    final Background vocation = Backgrounds.RULING_POSITIONS[station];
    
    final float promoteChance = (25 - (station * 10)) / 100f;
    Background birth = Backgrounds.BORN_HIGH;
    
    while (Rand.num() < promoteChance) {
      int index = Visit.indexOf(birth, Backgrounds.RULER_CLASSES);
      if (index <= 0) break;
      else birth = Backgrounds.RULER_CLASSES[index - 1];
    }
    //
    //  Then we generate the ruler themselves along with any modifications
    //  chosen by the player.
    final Background house = config.house;
    final Career rulerCareer = new Career(
      vocation, birth, house, config.gender
    );
    final Human ruler = new Human(rulerCareer, base);
    for (Skill s : house.skills()) {
      ruler.traits.incLevel(s, 5 + Rand.index(5) - 2);
    }
    for (Trait t : config.chosenTraits) {
      ruler.traits.setLevel(t, t.maxVal * (Rand.num() + 1) / 1);
    }
    for (Skill s : config.chosenSkills) {
      ruler.traits.incLevel(s, 10 + Rand.index(5) - 2);
    }
    for (Technique t : config.chosenTechs) {
      ruler.skills.addTechnique(t);
    }
    
    return ruler;
  }
  
  
  protected List <Human> advisors(Base base, Actor ruler) {
    final List <Human> advisors = new List <Human> ();
    final Background homeworld = config.house;
    
    for (Background b : config.advisors) {
      final Pick <Human> pick = new Pick <Human> ();
      //
      //  We make several attempts to find the 'best' candidate possible for
      //  the job.
      for (int i = 5; i-- > 0;) {
        final Career c = new Career(b, null, homeworld, null);
        final Human candidate = new Human(c, base);
        float rating = 0;
        //
        //  ...Which in the case of marriage, involves attraction.
        if (b == Backgrounds.FIRST_CONSORT) {
          rating += ruler.motives.attraction(candidate) * 1.0f;
          rating += candidate.motives.attraction(ruler) * 0.5f;
          rating += Career.ratePromotion(b, candidate, false) ;
        }
        else rating += Career.ratePromotion(b, candidate, false);
        pick.compare(candidate, rating);
      }
      if (pick.empty()) continue;
      //
      //  Once that's determined, we further increment their skills based on
      //  those the ruler finds valuable:
      final Human advisor = pick.result();
      for (Skill s : config.chosenSkills) {
        if (advisor.traits.traitLevel(s) > 0) advisor.traits.incLevel(s, 5);
      }
      advisors.add(advisor);
    }
    return advisors;
  }
  
  
  protected List <Human> colonists(Base base) {
    final List <Human> colonists = new List <Human> ();
    final Background homeworld = config.house;
    
    for (Background b : config.crew.keys()) {
      final int num = (int) config.crew.valueFor(b);
      for (int n = num; n-- > 0;) {
        final Career c = new Career(b, null, homeworld, null);
        colonists.add(new Human(c, base));
      }
    }
    return colonists;
  }
  
  
  protected Bastion establishBastion(
    final Stage world, Base base,
    Human ruler, List <Human> advisors, List <Human> colonists
  ) {
    //
    //  First of all, we attempt to establish the Bastion itself and some
    //  peripheral defences-
    final Bastion bastion = new Bastion(base);
    advisors.add(ruler);
    base.assignRuler(ruler);
    base.setup.doPlacementsFor(bastion);
    base.setup.fillVacancies(bastion, true);
    if (! bastion.inWorld()) I.complain("BASTION COULD NOT ENTER WORLD!");
    //
    //  We clear away any structures that might have conflicted with the
    //  bastion, along with their inhabitants-
    for (Venue v : world.claims.venuesConflicting(bastion)) {
      for (Actor a : v.staff.lodgers()) a.exitWorld();
      for (Actor a : v.staff.workers()) a.exitWorld();
      v.exitWorld();
    }
    //
    //  Once that's done, we can draw a curtain wall:
    
    //  TODO:  INTRODUCE ESTABLISHMENT FOR OTHER STRUCTURES.  ...But walls
    //  should probably still go first?
    if (config.built.valueFor(ShieldWall.BLUEPRINT) > 0) {
      final Venue wall[] = Placement.placeAroundPerimeter(
        ShieldWall.BLUEPRINT, bastion.areaClaimed(), base, true
      );
      for (Venue v : wall) ((ShieldWall) v).updateFacing(true);
      final float fogBound = bastion.areaClaimed().maxSide() * Nums.ROOT2 / 2;
      base.intelMap.liftFogAround(bastion, fogBound);
    }
    //
    //  Then introduce personnel-
    for (Actor a : advisors) {
      a.mind.setHome(bastion);
      a.mind.setWork(bastion);
      a.enterWorldAt(bastion, world);
      a.goAboard(bastion, world);
    }
    for (Actor a : colonists) {
      a.assignBase(base);
      a.enterWorldAt(bastion, world);
      a.goAboard(bastion, world);
    }
    base.setup.establishRelations(bastion.staff.lodgers());
    //
    //  TODO:  Vary this based on starting House
    bastion.updateAsScheduled(0, false);
    for (Item i : bastion.stocks.shortages()) {
      bastion.stocks.addItem(i);
    }
    return bastion;
  }
  
  
  protected void establishLocals(Stage world) {
    
    //  TODO:  Allow for natives as well?
    int maxRuins = 0;
    Species nesting[] = null;
    
    if (config.siteLevel == SITE_SETTLED) {
      nesting = new Species[] { Qudu.SPECIES, Hareen.SPECIES };
    }
    if (config.siteLevel == SITE_WILDERNESS) {
      maxRuins = world.size / (Stage.ZONE_SIZE * 4);
      nesting = Species.ANIMAL_SPECIES;
    }
    if (config.siteLevel == SITE_WASTELAND) {
      maxRuins = world.size / (Stage.ZONE_SIZE * 2);
    }
    
    final Batch <Venue> ruins = Base.artilects(world).setup.doPlacementsFor(
      Ruins.VENUE_BLUEPRINTS[0], maxRuins
    );
    Base.artilects(world).setup.fillVacancies(ruins, true);
    if (nesting != null) Nest.populateFauna(world, nesting);
  }
}










