  /**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.civic.*;
import stratos.graphics.common.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  ...Organics.  That's the name for carbons.

public final class Economy {
  
  
  private Economy() {}
  final static Class <Economy> BC = Economy.class;
  
  final public static int
    FORM_RESOURCE  = 0,
    FORM_MATERIAL  = 1,
    FORM_PROVISION = 2,
    FORM_SERVICE   = 3,
    FORM_DEVICE    = 4,
    FORM_OUTFIT    = 5,
    FORM_USABLE    = 6,
    FORM_SPECIAL   = 7;
  final public static float
    DEFAULT_SALES_MARGIN   = 0.2f,
    DEFAULT_IMPORT_MARGIN  = 0.5f,
    DEFAULT_EXPORT_MARGIN  = 0.5f,
    DEFAULT_SMUGGLE_MARGIN = 1.0f;
  final public static float
    ITEM_WEAR_DURATION = 100;

  
  final static Table <Traded, Integer> CATEGORY_TABLE, CT;
  static { CATEGORY_TABLE = CT = new Table <Traded, Integer> (); }
  final public static int
    CATEGORY_OTHER    = -1,
    CATEGORY_FOOD     =  0,
    CATEGORY_SPYCE    =  1,
    CATEGORY_DRUG     =  2,
    CATEGORY_MINERAL  =  3,
    CATEGORY_WARES    =  4,
    CATEGORY_SECURED  =  5;  //  TODO:  Merge with the Forms above?
  private static Traded[] label(int ID, Traded... members) {
    for (Traded t : members) CT.put(t, ID);
    return members;
  }
  public static int categoryFor(Traded service) {
    final Integer i = CT.get(service);
    return i == null ? CATEGORY_OTHER : i;
  }
  
  
  final public static Traded
    MINERALS   = new Traded(BC, "Minerals"  , null, FORM_RESOURCE, 0),
    MOISTURE   = new Traded(BC, "Moisture"  , null, FORM_RESOURCE, 0),
    INSOLATION = new Traded(BC, "Insolation", null, FORM_RESOURCE, 0),
    LAND_AREA  = new Traded(BC, "Land Area" , null, FORM_RESOURCE, 0);
  final public static Traded
    ALL_RESOURCES[] = Traded.INDEX.soFar(Traded.class);
  
  
  final public static Traded
    CARBS = new Traded(
      BC, "Carbs"    , "carbs.gif"    , FORM_MATERIAL, 10,
      "Carbohydrate-based and oily foodstuffs"
    ),
    GREENS = new Traded(
      BC, "Greens"   , "greens.gif"   , FORM_MATERIAL, 55,
      "Fresh vegetable and fibrous foodstuffs"
    ),
    PROTEIN = new Traded(
      BC, "Protein"  , "protein.gif"  , FORM_MATERIAL, 25,
      "Game meat and cultured yeast foodstuffs"
    ),
    //  Rations
    ALL_FOOD_TYPES[] = label(CATEGORY_FOOD, CARBS, GREENS, PROTEIN),
    
    SOMA = new Traded(
      BC, "Soma"     , "soma.gif"     , FORM_MATERIAL, 40,
      "A mild recreational narcotic with minimal side-effects"
    ),
    CATALYST = new Traded(
      BC, "Catalyst" , "catalyst.gif"  , FORM_MATERIAL, 60,
      "Common stimulants and metabolic tracers used in lab work"
    ),
    MEDICINE = new Traded(
      BC, "Medicine" , "medicine.gif", FORM_MATERIAL, 120,
      "Drugs and supplements tailored to treat common diseases"
    ),
    ALL_DRUG_TYPES[] = label(CATEGORY_DRUG, SOMA, CATALYST, MEDICINE),
    
    POLYMER = new Traded(
      BC, "Polymer"  , "carbons.gif"  , FORM_MATERIAL, 5,
      "Long-chain hydrocarbons, used in plastics production"
    ),
    METALS = new Traded(
      BC, "Ores"     , "ores.gif"     , FORM_MATERIAL, 10,
      "Common metal ores, used in construction and industry"
    ),
    ISOTOPES = new Traded(
      BC, "Isotopes" , "fuel rods.gif" , FORM_MATERIAL, 35,
      "Heavy or radioactive elements used in nuclear synthesis"
    ),
    ALL_MINERAL_TYPES[] = label(CATEGORY_MINERAL, POLYMER, METALS, ISOTOPES),
    
    PLASTICS = new Traded(
      BC, "Plastics" , "plastics.gif" , FORM_MATERIAL, 25,
      "Flexible and light-weight, used for cloth and home fittings"
    ),
    PARTS = new Traded(
      BC, "Parts"    , "parts.gif"    , FORM_MATERIAL, 40,
      "Durable and heat-resistant, needed for heavy engineering"
    ),
    CIRCUITRY = new Traded(
      BC, "Circuitry", "inscription.gif", FORM_MATERIAL, 80,
      "Used to manufacture terminals and other advanced devices"
    ),
    ALL_WARES_TYPES[] = label(CATEGORY_WARES, PLASTICS, PARTS, CIRCUITRY);
  
  final public static Traded
    ALL_MATERIALS [] = Traded.INDEX.soFar(Traded.class);
  
  final public static Traded
    SAMPLES = new Traded(
      BC, "Samples"    , null, FORM_SPECIAL, 0
    ),
    SLAG = new Traded(
      BC, "Slag"       , null, FORM_SPECIAL, 0
    ),
    
    SPYCE_T = new Traded(
      BC, "Spyce T", "spyce.gif"  , FORM_SPECIAL, 200,
      "Tinerazine, a spyce compound found in animal galls"
    ),
    SPYCE_H = new Traded(
      BC, "Spyce H", "spyce.gif"  , FORM_SPECIAL, 200,
      "Halebdynum, a spyce compound found as a dessicated salt"
    ),
    
    ANTIMASS = new Traded(
      BC, "Antimass", "fuel_rods.gif" , FORM_SPECIAL, 85,
      "A potent energy source needed for atomics and space travel"
    ),
    PRESSFEED = new Traded(
      BC, "Pressfeed", "pressfeed.gif", FORM_SPECIAL, 50,
      "Disposable propaganda used to raise morale"
    ),
    DATALINKS = new Traded(
      BC, "Datalinks", "datalinks.gif", FORM_SPECIAL, 125,
      "Encrypted information relays suited to advanced study"
    ),
    DECOR = new Traded(
      BC, "Decor"    , "decor.gif"    , FORM_SPECIAL, 250,
      "Interior decoration for the homes of the upper crust"
    ),
    
    ARTIFACTS = new Traded(
      BC, "Artifacts"  , null, FORM_SPECIAL, 0
    ),
    
    TREATMENT = new Traded(
      BC, "Treatment"  , null, FORM_SPECIAL, 0
    ),
    GENE_SEED = new Traded(
      BC, "Gene Seed"  , null, FORM_SPECIAL, 0
    ),
    PSYCH_SCAN = new Traded(
      BC, "Psych Scan" , null, FORM_SPECIAL, 0
    ),
    REPLICANTS = new Traded(
      BC, "Replicants" , null, FORM_SPECIAL, 0
    );
  final public static Traded
    ALL_SPECIAL_ITEMS[] = Traded.INDEX.soFar(Traded.class);
  
  
  final public static Traded
    ATMO  = new Traded(
      BC, "Atmo" , "life_S.png", FORM_PROVISION, 5
    ),
    POWER = new Traded(
      BC, "Power", "power.png" , FORM_PROVISION, 10
    ),
    WATER = new Traded(
      BC, "Water", "water.png" , FORM_PROVISION, 20
    ),
    COMMS = new Traded(
      BC, "Comms", "admin.png" , FORM_PROVISION, 15
    );
  final public static Traded
    ALL_PROVISIONS[] = Traded.INDEX.soFar(Traded.class);
  
  
  final public static Traded
    SERVICE_ENTERTAIN  = new Traded(
      BC, "Entertainment", null, FORM_SERVICE, 0
    ),
    SERVICE_HEALTHCARE = new Traded(
      BC, "Healthcare"   , null, FORM_SERVICE, 0
    ),
    SERVICE_HOUSING    = new Traded(
      BC, "Housing"      , null, FORM_SERVICE, 0
    ),
    SERVICE_REFUGE     = new Traded(
      BC, "Refuge"       , null, FORM_SERVICE, 0
    ),
    SERVICE_SECURITY   = new Traded(
      BC, "Security"     , null, FORM_SERVICE, 0
    ),
    SERVICE_ADMIN      = new Traded(
      BC, "Admin"        , null, FORM_SERVICE, 0
    ),
    SERVICE_COMMERCE   = new Traded(
      BC, "Commerce"     , null, FORM_SERVICE, 0
    ),
    SERVICE_REPAIRS    = new Traded(
      BC, "Repairs"      , null, FORM_SERVICE, 0
    ),
    SERVICE_ARMAMENT   = new Traded(
      BC, "Armament"     , null, FORM_SERVICE, 0
    );
  final public static Traded
    ALL_SERVICES[] = Traded.INDEX.soFar(Traded.class);
  
  

  final public static int
    NONE     = 0,
    //
    //  These are properties of equipped weapons-
    MELEE    = 1 << 0,
    RANGED   = 1 << 1,
    ENERGY   = 1 << 2,
    KINETIC  = 1 << 3,
    STUN     = 1 << 4,
    POISON   = 1 << 5,
    HOMING   = 1 << 6,
    BURNER   = 1 << 7,
    //
    //  These are properties of natural weapons or armour-
    GRAPPLE      = 1 << 8,
    CAUSTIC      = 1 << 9,
    TRANSMORPHIC = 1 << 10,
    ENERGY_DRAIN = 1 << 12;
  
  final public static Object
    TO = new Object();
  
  
  
  //  TODO:  Move the various armour/device types to their own catalogues.
  //  (Which you can do now, thanks to the revamped index system.)
  
  final public static DeviceType
    STUN_WAND = new DeviceType(
      BC, "Stun Wand",
      "pistol", AnimNames.FIRE,
      6, RANGED | ENERGY | STUN | HOMING, 35,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    BLASTER = new DeviceType(
      BC, "Blaster",
      "pistol", AnimNames.FIRE,
      10, RANGED | ENERGY | BURNER, 25,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    HALBERD_GUN = new DeviceType(
      BC, "Halberd Gun",
      "", AnimNames.FIRE_SIDE,
      13, RANGED | MELEE | KINETIC, 40,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    
    SHIV = new DeviceType(
      BC, "Shiv",
      "light blade", AnimNames.STRIKE,
      5, MELEE | KINETIC, 5,
      EngineerStation.class, 1, PARTS, 0, ASSEMBLY
    ),
    ZWEIHANDER = new DeviceType(
      BC, "Zweihander",
      "heavy blade", AnimNames.STRIKE_BIG,
      15, MELEE | KINETIC, 25,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    ARC_SABRE = new DeviceType(
      BC, "Arc Sabre",
      "sabre", AnimNames.STRIKE,
      25, MELEE | ENERGY, 100,
      EngineerStation.class, 3, PARTS, 15, ASSEMBLY
    ),
    
    HUNTING_LANCE = new DeviceType(
      BC, "Hunting Lance",
      "spear", AnimNames.STRIKE,
      10, RANGED | KINETIC, 5,
      null, 5, HANDICRAFTS
    ),
    LIMB_AND_MAW = new DeviceType(
      BC, "Limb and Maw",
      null, AnimNames.STRIKE,
      0, MELEE | KINETIC, 0,
      null
    ),
    INTRINSIC_BEAM = new DeviceType(
      BC, "Intrinsic Beam",
      null, AnimNames.FIRE,
      0, RANGED | ENERGY, 0,
      null
    ),
    
    LASDRILL_FRAME = new DeviceType(
      BC, "Laser Drill",
      "laser drill", AnimNames.BUILD,
      5, MELEE | KINETIC | ENERGY, 10,
      EngineerStation.class, 2, PARTS, 5, ASSEMBLY
    ),
    MODUS_LUTE = new DeviceType(
      BC, "Modus Lute",
      "modus lute", AnimNames.TALK_LONG,
      0, NONE, 40,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    BIOCORDER = new DeviceType(
      BC, "Biocorder",
      "biocorder", AnimNames.LOOK,
      0, NONE, 55,
      EngineerStation.class, 2, PARTS, 15, ASSEMBLY
    );
  final public static Traded
    ALL_DEVICES[] = Traded.INDEX.soFar(Traded.class);
  
  
  //  TODO:  You should have skins associated with some of these...
  final public static OutfitType
    
    OVERALLS       = new OutfitType(
      BC, "Overalls"      , 1, 0, 50,
      Fabricator.class, 1, PLASTICS, 5, ASSEMBLY
    ),
    FINERY         = new OutfitType(
      BC, "Finery"        , 1, 0 , 400,
      Fabricator.class, 2, PLASTICS, 15, GRAPHIC_DESIGN
    ),
    SCRAP_GEAR = new OutfitType(
      BC, "Scrap Gear", 3, 0, 5,
      null, 0, HANDICRAFTS
    ),
    INTRINSIC_ARMOUR = new OutfitType(
      BC, "Natural Armour", 0, 0, 0,
      null
    ),
    
    SEALSUIT       = new OutfitType(
      BC, "Sealsuit"      , 4, 1, 150,
      Fabricator.class, 1, PLASTICS, 1, PARTS, 10, HANDICRAFTS
    ),
    STEALTH_SUIT   = new OutfitType(
      BC, "Stealth Suit"  , 8, 5, 250,
      Fabricator.class, 1, PLASTICS, 2, PARTS, 15, HANDICRAFTS
    ),
    
    BELT_AND_BRACER = new OutfitType(
      BC, "Belt and Bracer"   , 5, 10, 50,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 15, 10, 150,
      EngineerStation.class, 5, PARTS, 15, ASSEMBLY
    ),
    POWER_ARMOUR   = new OutfitType(
      BC, "Power Armour"  , 20, 10, 275,
      EngineerStation.class, 8, PARTS, 20, ASSEMBLY
    ),
    GOLEM_FRAME = new OutfitType(
      BC, "Golem Frame"  , 25, 10, 500,
      EngineerStation.class, 12, PARTS, 25, ASSEMBLY
    );
  final public static Traded
    ALL_OUTFITS[] = Traded.INDEX.soFar(Traded.class);
  
}





