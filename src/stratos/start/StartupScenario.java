

package stratos.start;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.politic.*;
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
    
    //public Background demesneTaken;  //TODO:  Configure this!
    public Background house;
    public Background gender;
    public List <Trait> chosenTraits = new List <Trait> ();
    public List <Skill> chosenSkills = new List <Skill> ();
    public List <Background> advisors = new List <Background> ();
    public Table <Background, Integer> numCrew = new Table();
    
    public int siteLevel, fundsLevel, titleLevel;
  }
  
  final Config config;
  
  
  
  public StartupScenario(Config config) {
    super();
    this.config = config;
  }
  
  
  public StartupScenario(Session s) throws Exception {
    super(s);
    this.config = new Config();
    config.house = (Background) s.loadObject();
    config.gender = (Background) s.loadObject();
    s.loadObjects(config.chosenTraits);
    s.loadObjects(config.chosenSkills);
    s.loadObjects(config.advisors);
    for (int i = s.loadInt(); i-- > 0;) {
      config.numCrew.put((Background) s.loadObject(), s.loadInt());
    }
    config.siteLevel  = s.loadInt();
    config.fundsLevel = s.loadInt();
    config.titleLevel = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(config.house);
    s.saveObject(config.gender);
    s.saveObjects(config.chosenTraits);
    s.saveObjects(config.chosenSkills);
    s.saveObjects(config.advisors);
    s.saveInt(config.numCrew.size());
    for (Background b : config.numCrew.keySet()) {
      s.saveObject(b);
      s.saveInt(config.numCrew.get(b));
    }
    s.saveInt(config.siteLevel);
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
      case(1) : meadow = 4; barrens = 2; desert  = 2; water = 3; break;
      case(2) : forest = 2; meadow  = 3; barrens = 2; water = 1; break;
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
    TG.setupMinerals(world, 0, 0, 0);
    TG.setupOutcrops(world);
    world.terrain().readyAllMeshes();
    
    Flora.populateFlora(world);
    
    //  TODO:  THIS NEEDS TO BE CONFIGURED EXTERNALLY!
    world.offworld.assignLocalSector(
      Sectors.SECTOR_ELYSIUM, Sectors.PLANET_DIAPSOR
    );
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    final Base base = Base.withName(world, "Player Base", Colour.LITE_BLUE);
    
    int funding = -1, interest = -1;
    switch (config.fundsLevel) {
      case(0) : funding = 7500 ; interest = 3; break;
      case(1) : funding = 10000; interest = 2; break;
      case(2) : funding = 12500; interest = 1; break;
    }
    base.finance.incCredits(funding, BaseFinance.SOURCE_CHARITY);
    base.finance.setInterestPaid(interest);
    base.commerce.assignHomeworld((Sector) config.house);
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
    //  Establish starting positions for the locals-
    establishLocals(world);
    //
    //  And finally, establish the position of the base site-
    final Bastion bastion = establishBastion(
      world, base, ruler, advisors, colonists
    );
    UI.assignBaseSetup(base, bastion.position(null));
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    String title = base.ruler().fullName();
    while (true) {
      File match = new File(Scenario.fullSavePath(title, null));
      if (! match.exists()) break;
      title = title+"I";
    }
    return title;
  }
  
  
  protected void afterCreation() {
    //saveProgress(false);
  }
  
  
  
  /**  Private helper methods-
    */
  protected Human ruler(Base base) {
    
    final int station = config.titleLevel;
    final float promoteChance = (25 - (station * 10)) / 100f;
    final Background vocation = Backgrounds.RULING_POSITIONS[station];
    final Background birth;
    if (Rand.num() < promoteChance) {
      if (Rand.num() < promoteChance) birth = Backgrounds.BORN_FREE;
      else birth = Backgrounds.BORN_GELDER;
    }
    else birth = Backgrounds.BORN_HIGH;
    
    final Background house = config.house;
    final Career rulerCareer = new Career(vocation, birth, house, config.gender);
    final Human ruler = new Human(rulerCareer, base);
    for (Skill s : house.skills()) ruler.traits.incLevel(s, 5);
    
    return ruler;
  }
  
  
  protected List <Human> advisors(Base base, Actor ruler) {
    final List <Human> advisors = new List <Human> ();
    final int numTries = 5;
    for (Background b : config.advisors) {
      Human picked = null;
      float bestRating = Float.NEGATIVE_INFINITY;
      
      for (int i = numTries; i-- > 0;) {
        final Human candidate = new Human(b, base);
        float rating = Career.ratePromotion(b, candidate);
        
        if (b == Backgrounds.FIRST_CONSORT) {
          rating += ruler.motives.attraction(candidate) * 1.0f;
          rating += candidate.motives.attraction(ruler) * 0.5f;
        }
        if (rating > bestRating) { picked = candidate; bestRating = rating; }
      }
      if (picked != null) advisors.add(picked);
    }
    
    return advisors;
  }
  
  
  protected List <Human> colonists(Base base) {
    final List <Human> colonists = new List <Human> ();
    final Background house = config.house;
    
    for (Background b : config.numCrew.keySet()) {
      final int num = config.numCrew.get(b);
      for (int n = num; n-- > 0;) {
        final Human c = new Human(b, base);
        for (Skill s : house.skills()) if (c.traits.traitLevel(s) > 0) {
          c.traits.incLevel(s, 5);
        }
        colonists.add(c);
      }
    }
    return colonists;
  }
  
  
  protected Bastion establishBastion(
    final Stage world, Base base,
    Human ruler, List <Human> advisors, List <Human> colonists
  ) {
    final Bastion bastion = new Bastion(base);
    advisors.add(ruler);
    base.assignRuler(ruler);
    
    base.setup.doPlacementsFor(bastion);
    //  TODO:  You may need to pass in some kind of rating for the bastion's
    //  location.
    
    if (! bastion.inWorld()) I.complain("NO LANDING SITE FOUND!");
    bastion.clearSurrounds();
    for (Actor a : advisors) {
      a.mind.setHome(bastion);
      a.mind.setWork(bastion);
      a.enterWorldAt(bastion, world);
      a.goAboard(bastion, world);
    }
    
    final Background careers[] = bastion.careers();
    for (Actor a : colonists) {
      a.assignBase(base);
      a.enterWorldAt(bastion, world);
      a.goAboard(bastion, world);
      if (Visit.arrayIncludes(careers, a.vocation())) {
        a.mind.setWork(bastion);
        a.mind.setHome(bastion);
      }
    }
    base.setup.establishRelations(bastion.staff.residents());
    
    //  TODO:  Vary this based on starting House-
    bastion.stocks.bumpItem(Economy.CARBS    , 10);
    bastion.stocks.bumpItem(Economy.PROTEIN  , 10);
    bastion.stocks.bumpItem(Economy.GREENS   , 10);
    bastion.stocks.bumpItem(Economy.SOMA     , 10);
    bastion.stocks.bumpItem(Economy.PARTS    , 20);
    bastion.stocks.bumpItem(Economy.PLASTICS , 10);
    //bastion.stocks.bumpItem(Economy.GENE_SEED, 15);
    //bastion.stocks.bumpItem(Economy.MEDICINE , 5 );
    //bastion.stocks.bumpItem(Economy.STIM_KITS, 10);
    //Placement.establishRelations(advisors, colonists);
    return bastion;
  }
  
  
  protected void establishLocals(Stage world) {
    
    //  TODO:  Allow for natives as well?
    int maxRuins = 0;
    Species wildSpecies[] = null;
    
    if (config.siteLevel == SITE_SETTLED) {
      wildSpecies = new Species[] { Species.QUDU, Species.HAREEN };
    }
    if (config.siteLevel == SITE_WILDERNESS) {
      maxRuins = world.size / (Stage.SECTOR_SIZE * 4);
      wildSpecies = Species.ANIMAL_SPECIES;
    }
    if (config.siteLevel == SITE_WASTELAND) {
      maxRuins = world.size / (Stage.SECTOR_SIZE * 2);
    }
    
    final Batch <Venue> ruins = Base.artilects(world).setup.doPlacementsFor(
      Ruins.VENUE_PROFILES[0], maxRuins
    );
    Base.artilects(world).setup.fillVacancies(ruins, true);
    
    if (wildSpecies != null) Nest.placeNests(world, wildSpecies);
  }
}










