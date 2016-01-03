/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.wild.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.verse.Faction.*;
import static stratos.game.wild.Habitat.*;
import stratos.content.civic.*;



//  TODO:  Merge this with your work on sector-states.

//  TODO:  Most (or all) of this info should be loaded from an XML file.
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


//
//  HERE WE ARE BORN TO BE KINGS

public class Verse {
  

  final public static int
    INTENSE_GRAVITY   = -2,
    STRONG_GRAVITY    = -1,
    NORMAL_GRAVITY    =  0,
    MILD_GRAVITY      =  1,
    NOMINAL_GRAVITY   =  2,
    
    SPARSE_POPULATION =  0,
    LIGHT_POPULATION  =  1,
    MEDIUM_POPULATION =  2,
    HIGH_POPULATION   =  3,
    VAST_POPULATION   =  4;
  
  final static String GRAVITY_DESC[] = {
    "Intense", "Strong", "Normal", "Mild", "Nominal"
  };
  final static String POPULATION_DESC[] = {
    "Sparse", "Light", "Medium", "High", "Vast"
  };
  
  
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
      
      EcologistStation.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      StockExchange   .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, ServiceHatch.BLUEPRINT,
      
      EcologistRedoubt.BLUEPRINT,
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
      NOVICE, ASSEMBLY, ANCIENT_LORE,
      OFTEN, ARTIFICER_CIRCLES, SOMETIMES, COURT_CIRCLES,
      RARELY, ECOLOGIST_CIRCLES, AESTHETE_CIRCLES,

      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
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
      
      EcologistStation.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
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
      NOVICE, FIELD_THEORY, STEALTH_AND_COVER,
      OFTEN, VENDOR_CIRCLES, RUNNER_CIRCLES, SOMETIMES, ARTIFICER_CIRCLES,
      
      EcologistStation.BLUEPRINT,
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
  
  
  final public static Sector
    SECTOR_ELYSIUM = new Sector(
      Verse.class, "Elysium Sector", null, FACTION_WILDLIFE,
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, SPARSE_POPULATION,
      0.15f, OCEAN,
      0.35f, SWAMPLANDS,
      0.25f, FOREST,
      0.25f, MEADOW,
      Qudu.SPECIES, Hareen.SPECIES
    ),
    SECTOR_PAVONIS = new Sector(
      Verse.class, "Pavonis Sector", null, FACTION_WILDLIFE,
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, SPARSE_POPULATION,
      0.25f, FOREST,
      0.35f, MEADOW,
      0.15f, SAVANNAH,
      0.10f, BARRENS,
      Qudu.SPECIES, Hareen.SPECIES, Lictovore.SPECIES
    ),
    SECTOR_TERRA = new Sector(
      Verse.class, "Terra Sector", null, FACTION_WILDLIFE,
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, SPARSE_POPULATION,
      0.20f, SAVANNAH,
      0.25f, BARRENS,
      0.40f, DUNE,
      0.15f, CURSED_EARTH,
      Drone.SPECIES
    ),
    
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
  
  
  private void initSeparations() {
    SECTOR_ELYSIUM.setSeparation(SECTOR_PAVONIS, Sector.SEP_BORDERS, 1, true);
    SECTOR_ELYSIUM.setSeparation(SECTOR_TERRA  , Sector.SEP_BORDERS, 1, true);
    SECTOR_PAVONIS.setSeparation(SECTOR_TERRA  , Sector.SEP_BORDERS, 1, true);
    for (Sector s : ALL_SECTORS) s.calculateRemainingSeparations(ALL_SECTORS);
    for (Sector s : ALL_PLANETS) {
      for (Sector n : ALL_PLANETS) if (n != s) {
        n.setSeparation(s, Sector.SEP_STELLAR, 2, false);
      }
      for (Sector n : ALL_DIAPSOR_SECTORS) {
        n.setSeparation(s, Sector.SEP_STELLAR, 2, true);
      }
    }
  }
  
  
  private void initPolitics() {
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
  
  
  
  /**  Setup, data fields and save/load methods-
    */
  final public Stage world;
  private Sector stageLocation = DEFAULT_START_LOCATION;
  
  final public VerseJourneys journeys = new VerseJourneys(this);
  final List <SectorBase> bases = new List <SectorBase> ();
  final Table <Relation, Relation> relations = new Table();
  
  
  public Verse(Stage stage) {
    this.world = stage;
  }
  
  
  public void initialVerse() {
    this.initSeparations();
    this.initPolitics();
    for (Sector s : ALL_SECTORS) {
      final SectorBase base = new SectorBase(this, s);
      bases.add(base);
      world.schedule.scheduleForUpdates(base);
    }
    for (Faction f : CIVIL_FACTIONS) if (f.startSite() != null) {
      final SectorBase base = baseForSector(f.startSite());
      base.assignFaction(f);
    }
  }
  
  
  public void loadState(Session s) throws Exception {
    stageLocation = (Sector) s.loadObject();
    journeys.loadState(s);
    s.loadObjects(bases);
    
    relations.clear();
    for (int n = s.loadInt(); n-- > 0;) {
      final Relation r = Relation.loadFrom(s);
      relations.put(r, r);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(stageLocation);
    journeys.saveState(s);
    s.saveObjects(bases);
    
    s.saveInt(relations.size());
    for (Relation r : relations.values()) {
      Relation.saveTo(s, r);
    }
  }
  
  
  public void assignStageLocation(Sector location) {
    this.stageLocation = location;
  }
  
  
  public Sector stageLocation() {
    return stageLocation;
  }
  
  
  public Sector localSector() {
    Sector l = stageLocation;
    while (l.belongs != null) l = l.belongs;
    return l;
  }
  
  
  
  /**  Regular updates-
    */
  public void updateVerse(float time) {
    journeys.updateJourneys((int) time);
  }
  
  
  
  /**  Political setup and query methods-
    */
  void setRelation(Faction a, Faction b, float value, boolean symmetric) {
    final Relation key = new Relation(a, b, value, 0);
    Relation match = relations.get(key);
    if (match == null) relations.put(key, match = key);
    match.setValue(value, 0);
    if (symmetric) setRelation(b, a, value, false);
  }
  
  
  void setRelations(Faction a, boolean symmetric, Object... tableVals) {
    final Table vals = Table.make(tableVals);
    for (Object k : vals.keySet()) {
      final Object v = vals.get(k);
      if (k instanceof Sector) {
        k = ((Sector) k).startingOwner;
      }
      if (k instanceof Faction && v instanceof Float) {
        setRelation(a, (Faction) k, (Float) v, symmetric);
      }
      else I.complain("ILLEGAL RELATION TYPE: "+v+" FOR "+k);
    }
  }
  
  
  void setRelations(
    Faction a, float value, boolean symmetric, Faction... others
  ) {
    for (Faction k : others) {
      setRelation(a, k, value, symmetric);
    }
  }
  
  
  
  /**  Physical demographics and travel methods-
    */
  public Series <SectorBase> sectorBases() {
    return bases;
  }
  
  
  public SectorBase baseForSector(Sector s) {
    //
    //  TODO:  You may want the ability to include multiple bases per sector in
    //  future...
    for (SectorBase b : bases) {
      if (b.location == s) return b;
    }
    return null;
  }
  
  
  public Sector currentSector(Object object) {
    if (object instanceof Sector) {
      return (Sector) object;
    }
    if (object instanceof Mobile) {
      final Mobile mobile = (Mobile) object;
      if (mobile.inWorld() && mobile.base().isResident(mobile)) {
        return stageLocation();
      }
      for (SectorBase base : bases) {
        if (base.isResident(mobile)) return base.location;
      }
      if (mobile instanceof Human) {
        return (Sector) ((Human) mobile).career().homeworld();
      }
    }
    if (object instanceof Target) {
      final Target target = (Target) object;
      if (target.inWorld()) return stageLocation();
    }
    return null;
  }
  
  
  public static boolean isWorldExit(
    Target point, Actor actor, Sector goes
  ) {
    //
    //  Returns whether the given point can be used to escape off-stage to a
    //  given adjacent sector-
    if (! (point instanceof EntryPoints.Portal)) return false;
    final EntryPoints.Portal exit = (EntryPoints.Portal) point;
    if (goes != null && exit.leadsTo() != goes) return false;
    return exit.allowsEntry(actor) && exit.allowsStageExit(actor);
  }
  
  
  public static boolean isWorldExit(Target point, Actor actor) {
    //
    //  Returns whether the given point can be used to escape off-stage at all.
    return isWorldExit(point, actor, null);
  }
}













