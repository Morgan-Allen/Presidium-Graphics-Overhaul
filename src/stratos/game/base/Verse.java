/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;
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


12 sectors:
  
  Elysium Sector
  Pavonis Sector
  Olympus Sector
  
  Terra Sector
  Tharsis Sector
  Acheron Sector
  
  Ascreus Sector
  Planitia Sector
  Thaumasia Sector
  
  The Dolmen Sea
  The Ocean Verdi
  The Archipelago
  
  +4 Away Missions/Mini-sites
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
    
    NO_POPULATION     =  0,
    LIGHT_POPULATION  =  1,
    MEDIUM_POPULATION =  2,
    HIGH_POPULATION   =  3,
    VAST_POPULATION   =  4;
  
  final public static VerseLocation
    //
    //  TODO:  List common names and minor houses.
    //         Introduce Calivor, Nordsei, Solipsus, Urym Hive & The Outer
    //         Sphere. (aliens and freeholds later.)
    PLANET_ASRA_NOVI = new VerseLocation(
      Verse.class, "Asra Novi", "House Suhail",
      "Asra Novi is a heavily-terraformed 'desert oasis' world noted for its "+
      "expertise in ecology and botanical science, together with polyamorous "+
      "traditions and luxury exports.",
      DESERT_BLOOD, MILD_GRAVITY, null, MEDIUM_POPULATION,
      VerseLocation.MAKES, REAGENTS, PLASTICS, DECOR, SPYCES,
      VerseLocation.NEEDS, WATER, CIRCUITRY, POLYMER,
      BORN_FREE,
      NOVICE, CULTIVATION, CHEMISTRY,
      OFTEN, ECOLOGIST_CIRCLES, SOMETIMES, COURT_CIRCLES, AESTHETE_CIRCLES,
      
      BotanicalStation.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      StockExchange   .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, Heighway.BLUEPRINT,
      
      EcologistRedoubt.BLUEPRINT,
      FormerBay.BLUEPRINT
    ),
    PLANET_PAREM_V = new VerseLocation(
      Verse.class, "Parem V", "House Procyon",
      "Parem V was one of the first-settled systems in the known quadrant, "+
      "and though dour and repressive, remains host to numerous machine-"+
      "cults and revered arcane relics.",
      WASTES_BLOOD, NORMAL_GRAVITY, null, MEDIUM_POPULATION,
      VerseLocation.MAKES, PARTS, ANTIMASS, CIRCUITRY,
      VerseLocation.NEEDS, FUEL_RODS, PROTEIN,
      BORN_DREGS, BORN_PYON,
      NOVICE, ASSEMBLY, ANCIENT_LORE,
      OFTEN, ARTIFICER_CIRCLES, SOMETIMES, COURT_CIRCLES,
      RARELY, ECOLOGIST_CIRCLES, AESTHETE_CIRCLES,

      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      StockExchange   .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, Heighway.BLUEPRINT,
      
      CultureVats.BLUEPRINT,
      Generator.BLUEPRINT,
      ExcavationSite.BLUEPRINT
    ),
    PLANET_HALIBAN = new VerseLocation(
      Verse.class, "Haliban", "House Altair",
      "Noted for it's spartan regimen and stern justice, Haliban's early "+
      "defection to the Calivor Republic have earned it several foes- and a "+
      "crucial role in quadrant defence strategy.",
      FOREST_BLOOD, STRONG_GRAVITY, null, MEDIUM_POPULATION,
      VerseLocation.MAKES, CARBS, GREENS, MEDICINE,
      VerseLocation.NEEDS, SERVICE_ARMAMENT, PARTS, REAGENTS,
      BORN_GELDER, BORN_FREE,
      NOVICE, MARKSMANSHIP, ANATOMY,
      OFTEN, MILITARY_CIRCLES, SOMETIMES, PHYSICIAN_CIRCLES,
      RARELY, VENDOR_CIRCLES,
      
      BotanicalStation.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, Heighway.BLUEPRINT,
      
      ShieldWall.BLUEPRINT,
      Airfield.BLUEPRINT,
      Bastion.LEVELS[1]
    ),
    PLANET_AXIS_NOVENA = new VerseLocation(
      Verse.class, "Axis Novena", "House Taygeta",
      "Aided by it's low gravity and thin atmosphere, Axis Novena became the "+
      "centre of a large shipping industry and trade network- along with "+
      "rampant smuggling and black-market tech research.",
      TUNDRA_BLOOD, MILD_GRAVITY, null, HIGH_POPULATION,
      VerseLocation.MAKES, CIRCUITRY, MEDICINE, SERVICE_COMMERCE,
      VerseLocation.NEEDS, GREENS, METALS, ANTIMASS,
      BORN_DREGS, BORN_GELDER,
      NOVICE, FIELD_THEORY, STEALTH_AND_COVER,
      OFTEN, VENDOR_CIRCLES, RUNNER_CIRCLES, SOMETIMES, ARTIFICER_CIRCLES,
      
      BotanicalStation.BLUEPRINT,
      EngineerStation .BLUEPRINT,
      PhysicianStation.BLUEPRINT,
      TrooperLodge    .BLUEPRINT,
      StockExchange   .BLUEPRINT,
      SupplyDepot     .BLUEPRINT,
      Bastion.BLUEPRINT, Holding.BLUEPRINT, Heighway.BLUEPRINT,
      
      RunnerMarket.BLUEPRINT,
      Cantina.BLUEPRINT
    ),
    
    //
    //  TODO:  ...These need more detail.
    PLANET_SOLIPSUS_VIER = new VerseLocation(
      Verse.class, "Solipsus Vier", "House Fomalhaut",
      "Notable for it's peculiar blend of pastoral tradition and caste "+
      "eugenics, Solipsus Vier is ruled by insular scientific elites fixated "+
      "on mental and physical purity.",
      WASTES_BLOOD, NORMAL_GRAVITY, null, MEDIUM_POPULATION,
      OFTEN, PHYSICIAN_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES, MILITARY_CIRCLES,
      RARELY, VENDOR_CIRCLES, NEVER, RUNNER_CIRCLES
    ),
    PLANET_NORUSEI = new VerseLocation(
      Verse.class, "Norusei", "House Rana",
      "Once an idyllic tropical planet-resort, Norusei has enjoyed something "+
      "of a renaissance following the devastation of the Machine Wars, "+
      "boasting a rich tourist trade and export of celebrity cult-idols.",
      FOREST_BLOOD, NORMAL_GRAVITY, null, LIGHT_POPULATION,
      OFTEN, AESTHETE_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES,
      RARELY, ARTIFICER_CIRCLES, MILITARY_CIRCLES
    ),
    
    PLANET_URYM_HIVE = new VerseLocation(
      Verse.class, "Urym Hive", "House Algol (Minor)",
      "Chief factory-world of the Empire in its prime, today inescapable "+
      "poverty, desperate squalor and seething unrest render Urym Hive's "+
      "unnumbered billions governable in name only.",
      WASTES_BLOOD, INTENSE_GRAVITY, null, VAST_POPULATION
    ),
    PLANET_CALIVOR = new VerseLocation(
      Verse.class, "Calivor", "House Regulus (Minor)",
      "Capital of the Republic whose meteoric rise to prominence saw a dozen "+
      "noble houses unseated in disgrace, to many Calivor remains a gleaming "+
      "beacon of opportunity.",
      TUNDRA_BLOOD, NORMAL_GRAVITY, null, HIGH_POPULATION
    ),
    PLANET_WEIRWORLD = new VerseLocation(
      Verse.class, "Weirworld", "House Ophiuchus (Exiled)",
      "Shrouded by dense nebulae and dark rumour, the Weirworld is reputedly "+
      "a hollow organic Sphere, host to fleets of Strain vessels which raid "+
      "or colonise the quadrant periphery.",
      MUTATION, NOMINAL_GRAVITY, null, MEDIUM_POPULATION
    ),
    
    PLANET_DIAPSOR = new VerseLocation(
      Verse.class, "Diapsor", "No House (Freehold)",
      "Rendered all but uninhabitable after the Machine Wars, Diapsor was "+
      "placed under Imperial Quarantine until recent population pressures, "+
      "political reforms and ecologic recovery permitted re-settlement.",
      FOREST_BLOOD, NORMAL_GRAVITY, null, NO_POPULATION
    ),
    PLANET_TERMAGANT = null,
    PLANET_HIVE_IDO  = null,
    PLANET_XIRIDU    = null,
    
    PLANET_THE_HOMEWORLD = new VerseLocation(
      Verse.class, "The Homeworld", "No House (Jovian Protectorate)",
      "Surrounded by an impenetrable Null Barrier erected by the xenos "+
      "Jovians, the Homeworld is rumoured to be the birthplace of humanity, "+
      "transplanted by Jump Drives of colossal size.",
      DESERT_BLOOD, MILD_GRAVITY, null, LIGHT_POPULATION
    ),
    PLANET_ALBEDO_C97 = null,
    PLANET_CETA_RHO   = null,
    
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN,
      PLANET_AXIS_NOVENA, PLANET_SOLIPSUS_VIER, PLANET_NORUSEI,
      PLANET_URYM_HIVE, PLANET_CALIVOR, PLANET_WEIRWORLD,
      PLANET_DIAPSOR,
      PLANET_THE_HOMEWORLD
    },
    DEFAULT_HOMEWORLD = PLANET_ASRA_NOVI;
  
  
  final public static VerseLocation
    SECTOR_ELYSIUM = new VerseLocation(
      Verse.class, "Elysium Sector", "No House (Freehold)",
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, NO_POPULATION
    ),
    SECTOR_PAVONIS = new VerseLocation(
      Verse.class, "Pavonis Sector", "No House (Freehold)",
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, NO_POPULATION
    ),
    SECTOR_TERRA = new VerseLocation(
      Verse.class, "Terra Sector", "No House (Freehold)",
      "",
      WASTES_BLOOD, NORMAL_GRAVITY, PLANET_DIAPSOR, NO_POPULATION
    ),
    
    ALL_DIAPSOR_SECTORS[] = {
      SECTOR_ELYSIUM, SECTOR_PAVONIS, SECTOR_TERRA
    },
    
    SECTOR_UNDERGROUND = new VerseLocation(
      Verse.class, "Underground Sector", "N/A",
      "",
      MUTATION, NORMAL_GRAVITY, PLANET_DIAPSOR, NO_POPULATION
    ),
    
    DEFAULT_START_LOCATION = SECTOR_ELYSIUM;
  
  
  final public static VerseLocation
    ALL_SECTORS[] = (VerseLocation[]) Visit.compose(
      VerseLocation.class, ALL_PLANETS, ALL_DIAPSOR_SECTORS
    );
  
  
  private void initPolitics() {
    //  TODO:  LOAD ALL THIS FROM XML
    
    //  TODO:  Finish these up, and create some dedicated classes for the
    //  purpose.
    setRelations(Base.KEY_ARTILECTS, -1.0f, true, (Object[]) ALL_PLANETS);
    setRelations(Base.KEY_VERMIN   , -0.5f, true, (Object[]) ALL_PLANETS);
    setRelations(Base.KEY_NATIVES  ,  0.0f, true, (Object[]) ALL_PLANETS);
    setRelations(Base.KEY_WILDLIFE ,  0.2f, true, (Object[]) ALL_PLANETS);
    
    //  House Altair-
    //    Enemies:  Rigel-Procyon and Taygeta, Hive Urym
    //    Allies:  Fomalhaut and Calivor
    //    Bonus to Commoner relations, penalty to Noble and Native relations
    setRelations(PLANET_HALIBAN, false,
      Base.KEY_NATIVES  , -0.25f,
      Base.KEY_ARTILECTS, -0.5f ,
      Base.KEY_WILDLIFE ,  0.0f ,
      
      PLANET_AXIS_NOVENA  , -0.25f,
      PLANET_PAREM_V      , -0.5f ,
      PLANET_URYM_HIVE    , -0.25f,
      PLANET_SOLIPSUS_VIER,  0.25f,
      PLANET_CALIVOR      ,  0.5f
    );
    
    //  House Suhail-
    //    Enemies:  Rigel-Procyon and Fomalhaut, Hive Urym
    //    Allies:  Ophiuchus-Rana
    //    Bonus to Native relations, penalty to Merchant relations
    setRelations(PLANET_ASRA_NOVI, false,
      Base.KEY_NATIVES  ,  0.4f ,
      Base.KEY_ARTILECTS, -0.75f,
      Base.KEY_WILDLIFE ,  0.2f ,
      
      PLANET_SOLIPSUS_VIER, -0.25f,
      PLANET_PAREM_V      , -0.5f ,
      PLANET_URYM_HIVE    , -0.25f,
      PLANET_NORUSEI      ,  0.5f
    );
    
    //  House Rigel-Procyon-
    //    Enemies:  Altair and Fomalhaut, Calivor
    //    Allies:  Hive Urym
    //    Bonus to Artilect and Noble relations, penalty to Commoner relations
    setRelations(PLANET_PAREM_V, false,
      Base.KEY_NATIVES  ,  0.0f ,
      Base.KEY_ARTILECTS,  0.25f,
      Base.KEY_WILDLIFE , -0.2f ,
      
      PLANET_HALIBAN      , -0.5f ,
      PLANET_SOLIPSUS_VIER, -0.25f,
      PLANET_URYM_HIVE    ,  0.65f
    );
  }
  
  
  private static Object keyFor(Base base) {
    if (base.isNative()) return Base.KEY_NATIVES;
    if (base.primal) return base.title();
    final Object home = base.commerce.homeworld();
    if (home != null) return home;
    return Base.KEY_FREEHOLD;
  }
  
  
  
  /**  Setup, data fields and save/load methods-
    */
  final Stage world;
  private VerseLocation stageLocation = DEFAULT_START_LOCATION;
  
  final public VerseJourneys journeys = new VerseJourneys(this);
  final List <VerseBase> bases = new List <VerseBase> ();
  
  //  TODO:  THIS WILL HAVE TO BE SAVED AND LOADED TOO
  final Table <Object, Table <Object, Float>> relations = new Table();
  
  
  public Verse(Stage stage) {
    this.world = stage;
    this.initPolitics();
  }
  
  
  public void loadState(Session s) throws Exception {
    stageLocation = (VerseLocation) s.loadObject();
    journeys.loadState(s);
    s.loadObjects(bases);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(stageLocation);
    journeys.saveState(s);
    s.saveObjects(bases);
  }
  
  
  public void assignStageLocation(VerseLocation location) {
    this.stageLocation = location;
  }
  
  
  public VerseLocation stageLocation() {
    return stageLocation;
  }
  
  
  public VerseLocation localWorld() {
    VerseLocation l = stageLocation;
    while (l.belongs != null) l = l.belongs;
    return l;
  }
  
  
  
  /**  Regular updates-
    */
  public void updateVerse(float time) {
    journeys.updateJourneys((int) time);
    for (VerseBase base : bases) base.updateBase();
  }
  
  
  
  /**  Political setup and query methods-
    */
  public float defaultRelations(Base base, Base other) {
    final Object BK = keyFor(base), OK = keyFor(other);
    final Table BR = relations.get(BK);
    if (BR == null) return 0;
    final Float val = (Float) BR.get(OK);
    return val != null ? val : 0;
  }
  
  
  void setRelation(Object a, Object b, float value, boolean symmetric) {
    Table <Object, Float> AR = relations.get(a);
    if (AR == null) relations.put(a, AR = new Table());
    AR.put(b, value);
    if (symmetric) setRelation(b, a, value, false);
  }
  
  
  void setRelations(Object a, boolean symmetric, Object... tableVals) {
    final Table vals = Table.make(tableVals);
    for (Object k : vals.keySet()) {
      final Object v = vals.get(k);
      if (v instanceof Float) {
        setRelation(a, k, (Float) v, symmetric);
      }
      else I.complain("ILLEGAL RELATION TYPE: "+v+" FOR "+k);
    }
  }
  
  
  void setRelations(
    Object a, float value, boolean symmetric, Object... others
  ) {
    for (Object k : others) {
      setRelation(a, k, value, symmetric);
    }
  }
  
  
  public static VerseBase baseForLocation(
    VerseLocation location, Verse universe
  ) {
    if (location == null) return null;
    for (VerseBase base : universe.bases) {
      if (base.location == location) return base;
    }
    final VerseBase base = new VerseBase(universe, location);
    universe.bases.add(base);
    return base;
  }
  
  
  public static VerseLocation currentLocation(Mobile mobile, Verse universe) {
    if (mobile.inWorld()) {
      return universe.stageLocation();
    }
    for (VerseBase base : universe.bases) if (base.isResident(mobile)) {
      return base.location;
    }
    if (mobile instanceof Human) {
      return (VerseLocation) ((Human) mobile).career().homeworld();
    }
    return null;
  }
  
  
  public static boolean isWorldExit(
    Target point, Actor actor, VerseLocation goes
  ) {
    //
    //  Returns whether the given point can be used to escape off-stage to a
    //  given adjacent sector-
    if (! (point instanceof StageExit)) return false;
    final StageExit exit = (StageExit) point;
    if (goes != null && exit.leadsTo() != goes) return false;
    return exit.allowsEntry(actor) && exit.allowsStageExit(actor);
  }
  
  
  public static boolean isWorldExit(Target point, Actor actor) {
    //
    //  Returns whether the given point can be used to escape off-stage at all.
    return isWorldExit(point, actor, null);
  }
}













