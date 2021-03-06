/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.game.wild.*;
import stratos.util.*;
import stratos.content.civic.*;
import stratos.content.wip.FormerBay;

import static stratos.game.actors.Backgrounds.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.verse.Faction.*;
import static stratos.game.wild.Habitat.*;


//  TODO:  Some of this info could ideally be loaded from XML?

/*
FULL LIST OF INTENDED SECTORS-
  
  16 Planets:
  Haliban
  Calivor + Presidium
  Asra Novi
  
  Axis Novena
  Parem V
  Solipsus Vier
  
  Norusei
  Hive Urym + Weirworld
  
  Albedo C97
  Ceta Rho
  The Homeworld
  
  Termagant
  Megiddo
  Xiridu
  
  Diapsor + The Gulf
//*/


public class StratosSetting extends Verse {
  
  
  final static String WORLDS_DIR = "media/Charts/worlds/";
  
  final public static Sector
    //
    //  TODO:  List common names and minor houses.
    //         Introduce Calivor, Nordsei, Solipsus, Urym Hive & The Outer
    //         Sphere. (aliens and freeholds later.)
    PLANET_ASRA_NOVI = new Sector(
      Verse.class, "Asra Novi", WORLDS_DIR+"asra_novi.png", FACTION_SUHAIL,
      "Asra Novi is a heavily-terraformed 'desert oasis' world noted for its "+
      "expertise in ecology and botanical science, together with polyamorous "+
      "traditions and luxury exports.",
      DESERT_BLOOD, MILD_GRAVITY, null, LIGHT_POPULATION,
      Sector.MAKES, REAGENTS, PLASTICS, DECOR, SPYCES,
      Sector.NEEDS, WATER, CIRCUITRY, POLYMER,
      
      BORN_FREE,
      NOVICE, CULTIVATION, CHEMISTRY,
      OFTEN, ECOLOGIST_CIRCLES, SOMETIMES, COURT_CIRCLES, AESTHETE_CIRCLES,
      
      EcologistRedoubt.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      StockExchange   .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, ServiceHatch.BLUEPRINT,
      
      BotanicalStation.BLUEPRINT,
      FormerBay.BLUEPRINT
    ),
    PLANET_PAREM_V = new Sector(
      Verse.class, "Parem V", WORLDS_DIR+"parem_v.png", FACTION_PROCYON,
      "Parem V was one of the first-settled systems in the known quadrant, "+
      "and though dour and repressive, remains host to numerous machine-"+
      "cults and revered arcane relics.",
      WASTES_BLOOD, NORMAL_GRAVITY, null, HIGH_POPULATION,
      Sector.MAKES, PARTS, ANTIMASS, CIRCUITRY,
      Sector.NEEDS, FUEL_RODS, PROTEIN,
      
      BORN_DREGS, BORN_PYON,
      NOVICE, ASSEMBLY, LOGIC,
      OFTEN, ARTIFICER_CIRCLES, SOMETIMES, COURT_CIRCLES,
      RARELY, ECOLOGIST_CIRCLES, AESTHETE_CIRCLES,
      
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      RunnerMarket    .BLUEPRINT,
      StockExchange   .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, ServiceHatch.BLUEPRINT,
      
      CultureVats.BLUEPRINT,
      Generator.BLUEPRINT,
      ExcavationSite.BLUEPRINT
    ),
    PLANET_HALIBAN = new Sector(
      Verse.class, "Haliban", WORLDS_DIR+"haliban.png", FACTION_ALTAIR,
      "Noted for it's spartan regimen and stern justice, Haliban's early "+
      "defection to the Calivor Republic have earned it several foes- and a "+
      "crucial role in quadrant defence strategy.",
      FOREST_BLOOD, STRONG_GRAVITY, null, MEDIUM_POPULATION,
      Sector.MAKES, CARBS, GREENS, MEDICINE,
      Sector.NEEDS, SERVICE_ARMAMENT, PARTS, REAGENTS,
      
      BORN_GELDER, BORN_FREE,
      NOVICE, MARKSMANSHIP, ANATOMY,
      OFTEN, MILITARY_CIRCLES, SOMETIMES, PHYSICIAN_CIRCLES,
      RARELY, VENDOR_CIRCLES,
      
      BotanicalStation.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      EcologistRedoubt.BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, ServiceHatch.BLUEPRINT,
      
      ShieldWall.BLUEPRINT,
      Airfield.BLUEPRINT,
      Bastion.LEVELS[1]
    ),
    PLANET_AXIS_NOVENA = new Sector(
      Verse.class, "Axis Novena", WORLDS_DIR+"axis_novena.png", FACTION_TAYGETA,
      "Aided by it's low gravity and thin atmosphere, Axis Novena became the "+
      "centre of a large shipping industry and trade network- along with "+
      "rampant smuggling and black-market tech research.",
      TUNDRA_BLOOD, MILD_GRAVITY, null, MEDIUM_POPULATION,
      Sector.MAKES, CIRCUITRY, MEDICINE, SERVICE_COMMERCE,
      Sector.NEEDS, GREENS, METALS, ANTIMASS,
      
      BORN_DREGS, BORN_GELDER,
      NOVICE, FIELD_THEORY, EVASION,
      OFTEN, VENDOR_CIRCLES, RUNNER_CIRCLES, SOMETIMES, ARTIFICER_CIRCLES,
      
      BotanicalStation.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      StockExchange   .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, ServiceHatch.BLUEPRINT,
      
      RunnerMarket.BLUEPRINT,
      Cantina.BLUEPRINT
    ),
    
    //
    //  TODO:  ...These need more detail.
    PLANET_SOLIPSUS_VIER = new Sector(
      Verse.class, "Solipsus Vier", null,
      new Faction("House Fomalhaut", null, FACTION_CIVILISED),
      "Notable for it's peculiar blend of pastoral tradition and caste "+
      "eugenics, Solipsus Vier is ruled by insular scientific elites fixated "+
      "on mental and physical purity.",
      WASTES_BLOOD, NORMAL_GRAVITY, null, MEDIUM_POPULATION,
      OFTEN, PHYSICIAN_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES, MILITARY_CIRCLES,
      RARELY, VENDOR_CIRCLES, NEVER, RUNNER_CIRCLES
    ),
    PLANET_NORUSEI = new Sector(
      Verse.class, "Norusei", null,
      new Faction("House Rana", null, FACTION_CIVILISED),
      "Once an idyllic tropical planet-resort, Norusei has enjoyed something "+
      "of a renaissance following the devastation of the Machine Wars, "+
      "boasting a rich tourist trade and export of celebrity cult-idols.",
      FOREST_BLOOD, NORMAL_GRAVITY, null, LIGHT_POPULATION,
      OFTEN, AESTHETE_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES,
      RARELY, ARTIFICER_CIRCLES, MILITARY_CIRCLES
    ),
    
    PLANET_URYM_HIVE = new Sector(
      Verse.class, "Urym Hive", null,
      new Faction("House Algol (Minor)", null, FACTION_CIVILISED),
      "Chief factory-world of the Empire in its prime, today inescapable "+
      "poverty, desperate squalor and seething unrest render Urym Hive's "+
      "uncounted billions governable in name only.",
      WASTES_BLOOD, INTENSE_GRAVITY, null, VAST_POPULATION
    ),
    PLANET_CALIVOR = new Sector(
      Verse.class, "Calivor", null,
      new Faction("House Regulus (Minor)", null, FACTION_CIVILISED),
      "Capital of the Republic whose meteoric rise to prominence saw a dozen "+
      "noble houses unseated in disgrace, to many Calivor remains a gleaming "+
      "beacon- and a looming threat.",
      TUNDRA_BLOOD, NORMAL_GRAVITY, null, HIGH_POPULATION
    ),
    PLANET_WEIRWORLD = new Sector(
      Verse.class, "Weirworld", null,
      new Faction("House Ophiuchus (Exiled)", null, FACTION_CIVILISED),
      "Shrouded by dense nebulae and dark rumour, the Weirworld is reputedly "+
      "a hollow organic Sphere, host to fleets of Strain vessels which raid "+
      "or colonise the quadrant periphery.",
      MUTATION, NOMINAL_GRAVITY, null, MEDIUM_POPULATION
    ),
    
    PLANET_DIAPSOR = new Sector(
      Verse.class, "Diapsor", null, FACTION_NATIVES,
      "Rendered all but uninhabitable after the Machine Wars, Diapsor was "+
      "placed under Imperial Quarantine until recent population pressures, "+
      "political reforms and ecologic recovery permitted re-settlement.",
      FOREST_BLOOD, NORMAL_GRAVITY, null, SPARSE_POPULATION
    ),
    PLANET_TERMAGANT = null,
    PLANET_HIVE_IDO  = null,
    PLANET_XIRIDU    = null,
    
    PLANET_THE_HOMEWORLD = new Sector(
      Verse.class, "The Homeworld", null,
      new Faction("No House (Jovian Protectorate)", null, FACTION_CIVILISED),
      "Surrounded by an impenetrable Null Barrier erected by the xenos "+
      "Jovians, the Homeworld is rumoured to be the birthplace of humanity, "+
      "transplanted by Jump Drives of colossal size.",
      DESERT_BLOOD, MILD_GRAVITY, null, LIGHT_POPULATION
    ),
    PLANET_ALBEDO_C97 = null,
    PLANET_CETA_RHO   = null,
    
    ALL_CAPITALS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN, PLANET_AXIS_NOVENA
    },
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN,
      PLANET_AXIS_NOVENA, PLANET_SOLIPSUS_VIER, PLANET_NORUSEI,
      PLANET_URYM_HIVE, PLANET_CALIVOR, PLANET_WEIRWORLD,
      PLANET_DIAPSOR,
      PLANET_THE_HOMEWORLD
    },
    DEFAULT_HOMEWORLD = PLANET_ASRA_NOVI;
  
  
  //  TODO:  You might consider moving these out into their individual class-
  //         files for the sake of completeness.
  
  final public static Sector
    SECTOR_ELYSIUM = new Sector(
      Verse.class, "Elysium Sector", null, FACTION_WILDLIFE,
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, SPARSE_POPULATION,
      0.15f, OCEAN,
      0.15f, SWAMPLANDS,
      0.35f, FOREST,
      0.35f, MEADOW,
      Qudu.SPECIES, Hareen.SPECIES,
      
      Sector.MAKES, CARBS, GREENS
    ) {
      public SectorScenario customScenario(Verse verse) {
        return new ScenarioElysium();
      }
    },
    
    SECTOR_PAVONIS = new Sector(
      Verse.class, "Pavonis Sector", null, FACTION_WILDLIFE,
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, SPARSE_POPULATION,
      0.25f, FOREST  ,
      0.35f, MEADOW  ,
      0.15f, SAVANNAH,
      0.10f, BARRENS ,
      Qudu.SPECIES, Hareen.SPECIES, Lictovore.SPECIES,
      
      Sector.MAKES, SPYCES, PROTEIN
    ) {
      public SectorScenario customScenario(Verse verse) {
        return new ScenarioPavonis();
      }
    },
    
    SECTOR_TERRA = new Sector(
      Verse.class, "Terra Sector", null, FACTION_WILDLIFE,
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, SPARSE_POPULATION,
      0.20f, SAVANNAH,
      0.25f, BARRENS,
      0.40f, DUNE,
      0.15f, CURSED_EARTH,
      Drone.SPECIES,
      
      Sector.MAKES, METALS, FUEL_RODS
    ) {
      public SectorScenario customScenario(Verse verse) {
        return new ScenarioTerra();
      }
    },
    
    ALL_DIAPSOR_SECTORS[] = {
      SECTOR_ELYSIUM, SECTOR_PAVONIS, SECTOR_TERRA
    },
    
    SECTOR_UNDERGROUND = new Sector(
      Verse.class, "Underground Sector", null, FACTION_VERMIN,
      "",
      MUTATION, NORMAL_GRAVITY, PLANET_DIAPSOR, SPARSE_POPULATION
    ),
    DEFAULT_START_LOCATION = SECTOR_ELYSIUM;
  
  
  final public static Sector
    ALL_SECTORS[] = (Sector[]) Visit.compose(
      Sector.class, ALL_PLANETS, ALL_DIAPSOR_SECTORS
    );
  
  
  public StratosSetting() {
    super(ALL_SECTORS);
  }
  
  
  public StratosSetting(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  protected void initSeparations() {
    setSeparation(SECTOR_ELYSIUM, SECTOR_PAVONIS, Sector.SEP_BORDERS, 1, true);
    setSeparation(SECTOR_ELYSIUM, SECTOR_TERRA  , Sector.SEP_BORDERS, 1, true);
    setSeparation(SECTOR_PAVONIS, SECTOR_TERRA  , Sector.SEP_BORDERS, 1, true);
    assignAsSiblings(true , ALL_SECTORS);
    assignAsSiblings(false, ALL_PLANETS);
    for (Sector s : ALL_PLANETS) {
      for (Sector n : ALL_PLANETS) if (n != s) {
        setSeparation(n, s, Sector.SEP_STELLAR, 2, false);
      }
      for (Sector n : ALL_DIAPSOR_SECTORS) {
        setSeparation(n, s, Sector.SEP_STELLAR, 2, true );
      }
    }
  }
  
  
  protected void initPolitics() {
    //  TODO:  LOAD ALL THIS FROM XML
    
    //  TODO:  Finish these up, and create some dedicated classes for the
    //  purpose.
    setRelations(FACTION_ARTILECTS, -1.0f, true, CIVIL_FACTIONS);
    setRelations(FACTION_VERMIN   , -0.5f, true, CIVIL_FACTIONS);
    setRelations(FACTION_NATIVES  ,  0.0f, true, CIVIL_FACTIONS);
    setRelations(FACTION_WILDLIFE ,  0.2f, true, CIVIL_FACTIONS);
    
    //  House Altair-
    //    Enemies:  Rigel-Procyon and Taygeta, Hive Urym
    //    Allies:  Fomalhaut and Calivor
    //    Bonus to Commoner relations, penalty to Noble and Native relations
    setRelations(FACTION_ALTAIR, false,
      FACTION_NATIVES     , -0.25f,
      FACTION_ARTILECTS   , -0.5f ,
      FACTION_WILDLIFE    ,  0.0f ,
      
      FACTION_TAYGETA     , -0.25f,
      FACTION_PROCYON     , -0.5f ,
      PLANET_URYM_HIVE    , -0.25f,
      PLANET_SOLIPSUS_VIER,  0.25f,
      PLANET_CALIVOR      ,  0.5f
    );
    
    //  House Suhail-
    //    Enemies:  Rigel-Procyon and Fomalhaut, Hive Urym
    //    Allies:  Ophiuchus-Rana
    //    Bonus to Native relations, penalty to Merchant relations
    setRelations(FACTION_SUHAIL, false,
      FACTION_NATIVES     ,  0.4f ,
      FACTION_ARTILECTS   , -0.75f,
      FACTION_WILDLIFE    ,  0.2f ,
      
      PLANET_SOLIPSUS_VIER, -0.25f,
      FACTION_PROCYON     , -0.5f ,
      PLANET_URYM_HIVE    , -0.25f,
      PLANET_NORUSEI      ,  0.5f
    );
    
    //  House Rigel-Procyon-
    //    Enemies:  Altair and Fomalhaut, Calivor
    //    Allies:  Hive Urym
    //    Bonus to Artilect and Noble relations, penalty to Commoner relations
    setRelations(FACTION_PROCYON, false,
      FACTION_NATIVES     ,  0.0f ,
      FACTION_ARTILECTS   ,  0.25f,
      FACTION_WILDLIFE    , -0.2f ,
      
      FACTION_ALTAIR      , -0.5f ,
      PLANET_SOLIPSUS_VIER, -0.25f,
      PLANET_URYM_HIVE    ,  0.65f
    );
  }
}




