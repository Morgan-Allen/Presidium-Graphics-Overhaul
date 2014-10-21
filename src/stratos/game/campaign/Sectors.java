

package stratos.game.campaign;
import stratos.game.actors.*;
import stratos.util.Visit;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;

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


public interface Sectors extends Backgrounds {
  
  final public static int
    INTENSE_GRAVITY = -2,
    STRONG_GRAVITY  = -1,
    NORMAL_GRAVITY  =  0,
    MILD_GRAVITY    =  1,
    NOMINAL_GRAVITY =  2;
  
  final public static Sector
    //
    //  TODO:  List common names and minor houses.
    //         Introduce Calivor, Nordsei, Solipsus, Urym Hive & The Outer
    //         Sphere. (aliens and freeholds later.)
    PLANET_ASRA_NOVI = new Sector(
      Sectors.class, "Asra Novi", "House Suhail",
      "Asra Novi is a heavily-terraformed 'desert oasis' world noted for it's "+
      "expertise in ecology and botanical science, together with polyamorous "+
      "traditions and luxury exports.",
      null, 0, 1, DESERT_BLOOD, MILD_GRAVITY,
      Sector.MAKES, SOMA, PLASTICS, ARTWORKS, SPYCE,
      Sector.NEEDS, OPEN_WATER, SERVICE_ENTERTAIN, DATALINKS,
      FREE_BIRTH,
      LEARNING, CULTIVATION, CHEMISTRY,
      OFTEN, ECOLOGIST_CIRCLES, SOMETIMES, COURT_CIRCLES, AESTHETE_CIRCLES
    ),
    PLANET_PAREM_V = new Sector(
      Sectors.class, "Parem V", "House Procyon",
      "Parem V was one of the first-settled systems in the known quadrant, "+
      "and though dour and repressive, remains host to numerous machine-"+
      "cults and revered arcane relics.",
      null, 1, 1, WASTES_BLOOD, NORMAL_GRAVITY,
      Sector.MAKES, PARTS, DATALINKS, ORGANS,
      Sector.NEEDS, ARTWORKS, SERVICE_REPAIRS,
      DREGS_BIRTH, PYON_BIRTH,
      LEARNING, ASSEMBLY, ANCIENT_LORE,
      OFTEN, ARTIFICER_CIRCLES, SOMETIMES, COURT_CIRCLES,
      RARELY, ECOLOGIST_CIRCLES, AESTHETE_CIRCLES
    ),
    PLANET_HALIBAN = new Sector(
      Sectors.class, "Haliban", "House Altair",
      "Noted for it's spartan regimen and stern justice, Haliban's early "+
      "defection to the Calivor Republic have earned it several foes- and a "+
      "crucial role in quadrant defence strategy.",
      null, 0, 0, FOREST_BLOOD, STRONG_GRAVITY,
      Sector.MAKES, CARBS, GREENS, MEDICINE,
      Sector.NEEDS, SERVICE_ARMAMENT, PARTS,
      GELDER_BIRTH, FREE_BIRTH,
      LEARNING, MARKSMANSHIP, ANATOMY,
      OFTEN, MILITARY_CIRCLES, SOMETIMES, PHYSICIAN_CIRCLES,
      RARELY, VENDOR_CIRCLES
    ),
    PLANET_AXIS_NOVENA = new Sector(
      Sectors.class, "Axis Novena", "House Taygeta",
      "Aided by it's low gravity and thin atmosphere, Axis Novena became the "+
      "centre of a large shipping industry and trade network- along with "+
      "rampant smuggling and black market research.",
      null, 1, 0, TUNDRA_BLOOD, NOMINAL_GRAVITY,
      Sector.MAKES, DATALINKS, MEDICINE, SERVICE_COMMERCE,
      Sector.NEEDS, GREENS, ORES, FUEL_RODS,
      DREGS_BIRTH, GELDER_BIRTH,
      LEARNING, FIELD_THEORY, STEALTH_AND_COVER,
      OFTEN, VENDOR_CIRCLES, OUTLAW_CIRCLES, SOMETIMES, ARTIFICER_CIRCLES
    ),
    //
    //  TODO:  ...These need more detail.
    PLANET_SOLIPSUS_VIER = new Sector(
      Sectors.class, "Solipsus Vier", "House Fomalhaut",
      "Notable for it's peculiar blend of pastoral tradition and caste "+
      "eugenics, Solipsus Vier is ruled by insular scientific elites fixated "+
      "on mental and physical purity.",
      null, 2, 1, WASTES_BLOOD, NORMAL_GRAVITY,
      OFTEN, PHYSICIAN_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES, MILITARY_CIRCLES,
      RARELY, VENDOR_CIRCLES, NEVER, OUTLAW_CIRCLES
    ),
    PLANET_NORUSEI = new Sector(
      Sectors.class, "Norusei", "House Rana",
      "Once an idyllic tropical planet-resort, Norusei has enjoyed something "+
      "of a renaissance following the devastation of the Machine Wars, "+
      "boasting a rich tourist trade and export of celebrity cult-idols.",
      null, 2, 0, FOREST_BLOOD, NORMAL_GRAVITY,
      OFTEN, AESTHETE_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES,
      RARELY, ARTIFICER_CIRCLES, MILITARY_CIRCLES
    ),
    
    PLANET_URYM_HIVE = new Sector(
      Sectors.class, "Urym Hive", "House Algol (Minor)",
      "Chief factory-world of the Empire in its prime, today inescapable "+
      "poverty, desperate squalor and seething unrest render Urym Hive's "+
      "unnumbered billions governable in name only.",
      null, 0, 2, WASTES_BLOOD, INTENSE_GRAVITY
    ),
    PLANET_CALIVOR = new Sector(
      Sectors.class, "Calivor", "House Regulus (Minor)",
      "Capital of the Republic whose meteoric rise to prominence saw a dozen "+
      "noble houses unseated in disgrace, to many Calivor remains a gleaming "+
      "beacon of opportunity.",
      null, 1, 2, TUNDRA_BLOOD, NORMAL_GRAVITY
    ),
    PLANET_THE_WEIRWORLD = new Sector(
      Sectors.class, "The Weirworld", "House Ophiuchus (Exiled)",
      "Shrouded by dense nebulae and dark rumour, the Weirworld is reputedly "+
      "a hollow organic Sphere, host to fleets of Strain vessels which raid "+
      "or colonise the quadrant periphery.",
      null, 2, 2, MUTATION, MILD_GRAVITY
    ),
    
    PLANET_DIAPSOR = new Sector(
      Sectors.class, "Diapsor", "No House (Freehold)",
      "Rendered all but uninhabitable after the Machine Wars, Diapsor was "+
      "placed under Imperial Quarantine until recent population pressures, "+
      "political change and ecological recovery permitted re-settlement.",
      null, -1, -1, MUTATION, NORMAL_GRAVITY
    ),
    
    PLANET_THE_HOMEWORLD = new Sector(
      Sectors.class, "The Homeworld", "No House (Jovian Protectorate)",
      "Surrounded by an impenetrable Null Barrier erected by the xenos "+
      "Jovians, the Homeworld is rumoured to be the birthplace of humanity, "+
      "transplanted by Jump Drives of unfathomable size.",
      null, -1, -1, DESERT_BLOOD, MILD_GRAVITY
    ),
    
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN,
      PLANET_AXIS_NOVENA, PLANET_SOLIPSUS_VIER, PLANET_NORUSEI,
      PLANET_URYM_HIVE, PLANET_CALIVOR, PLANET_THE_WEIRWORLD,
      PLANET_DIAPSOR, PLANET_THE_HOMEWORLD
    };
  
  
  //  TODO:  These should all, properly, be considered sub-sectors of Planet
  //         Diapsor.
  final public static Sector
    SECTOR_ELYSIUM = new Sector(
      Sectors.class, "Elysium Sector", "No House (Freehold)",
      "",
      null, -1, -1, WASTES_BLOOD, NORMAL_GRAVITY
    ),
    SECTOR_PAVONIS = new Sector(
      Sectors.class, "Pavonis Sector", "No House (Freehold)",
      "",
      null, -1, -1, WASTES_BLOOD, NORMAL_GRAVITY
    ),
    SECTOR_TERRA = new Sector(
      Sectors.class, "Terra Sector", "No House (Freehold)",
      "",
      null, -1, -1, WASTES_BLOOD, NORMAL_GRAVITY
    ),
    
    ALL_DIAPSOR_SECTORS[] = {
      SECTOR_ELYSIUM, SECTOR_PAVONIS, SECTOR_TERRA
    };
  
  
  final public static Sector
    ALL_SECTORS[] = (Sector[]) Visit.compose(
      Sector.class, ALL_PLANETS, ALL_DIAPSOR_SECTORS
    );
}




