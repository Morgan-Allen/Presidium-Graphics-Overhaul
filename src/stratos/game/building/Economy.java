/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.graphics.common.*;
import static stratos.game.actors.Qualities.*;



public final class Economy {
  
  
  private Economy() {}
  
  final public static float
    ITEM_WEAR_DURATION = 100;
  
  final static Class BC = Economy.class;
  
  final public static int
    FORM_RESOURCE  = 0,
    FORM_MATERIAL  = 1,
    FORM_PROVISION = 2,
    FORM_SERVICE   = 3,
    FORM_DEVICE    = 4,
    FORM_OUTFIT    = 5,
    FORM_USABLE    = 6,
    FORM_SPECIAL   = 7;
  
  
  final public static TradeType
    MINERALS   = new TradeType(BC, "Minerals"  , null, FORM_RESOURCE, 0),
    MOISTURE   = new TradeType(BC, "Moisture"  , null, FORM_RESOURCE, 0),
    INSOLATION = new TradeType(BC, "Insolation", null, FORM_RESOURCE, 0),
    LAND_AREA  = new TradeType(BC, "Land Area" , null, FORM_RESOURCE, 0);
  final public static TradeType
    ALL_RESOURCES[] = TradeType.typesSoFar();
  
  
  final public static TradeType
    CARBS = new TradeType(
      BC, "Carbs"    , "carbs.gif"    , FORM_MATERIAL, 10
    ),
    GREENS = new TradeType(
      BC, "Greens"   , "greens.gif"   , FORM_MATERIAL, 20
    ),
    PROTEIN = new TradeType(
      BC, "Protein"  , "protein.gif"  , FORM_MATERIAL, 50
    ),
    SPYCE = new TradeType(
      BC, "Spyce"    , "spyce.gif"    , FORM_MATERIAL, 100
    ),
    
    SOMA = new TradeType(
      BC, "Soma"     , "soma.gif"     , FORM_MATERIAL, 40
    ),
    STIM_KITS = new TradeType(
      BC, "Stim Kits", "stimkit.gif"  , FORM_MATERIAL, 60
    ),
    MEDICINES = new TradeType(
      BC, "Medicines", "medicines.gif", FORM_MATERIAL, 120
    ),
    ORGANS = new TradeType(
      BC, "Organs"   , "organs.gif"   , FORM_MATERIAL, 250
    ),
    
    ORES = new TradeType(
      BC, "Ores"     , "ores.gif"     , FORM_MATERIAL, 25
    ),
    ISOTOPES = new TradeType(
      BC, "Isotopes" , "isotopes.gif" , FORM_MATERIAL, 55
    ),
    PARTS = new TradeType(
      BC, "Parts"    , "parts.gif"    , FORM_MATERIAL, 40
    ),
    FUEL_RODS = new TradeType(
      BC, "Fuel Rods", "fuel_rods.gif", FORM_MATERIAL, 85
    ),
    
    PLASTICS = new TradeType(
      BC, "Plastics" , "plastics.gif" , FORM_MATERIAL, 25
    ),
    PRESSFEED = new TradeType(
      BC, "Pressfeed", "pressfeed.gif", FORM_MATERIAL, 50
    ),
    ARTWORKS = new TradeType(
      BC, "Artworks" , "decor.gif"    , FORM_MATERIAL, 100
    ),
    DATALINKS = new TradeType(
      BC, "Datalinks", "datalinks.gif", FORM_MATERIAL, 125
    );
  
  final public static TradeType
    ALL_FOOD_TYPES[] = { CARBS, GREENS, PROTEIN },
    ALL_MATERIALS[] = TradeType.typesSoFar();
  
  final public static TradeType
    SAMPLES = new TradeType(
      BC, "Samples"    , null, FORM_SPECIAL, 0
    ),
    CREDITS = new TradeType(
      BC, "Credits"    , null, FORM_SPECIAL, 0
    ),
    DECOR_ITEMS = new TradeType(
      BC, "Decor Items", null, FORM_SPECIAL, 0
    ),
    TREATMENT = new TradeType(
      BC, "Treatment"  , null, FORM_SPECIAL, 0
    ),
    GENE_SEED = new TradeType(
      BC, "Gene Seed"  , null, FORM_SPECIAL, 0
    ),
    PSYCH_SCAN = new TradeType(
      BC, "Psych Scan" , null, FORM_SPECIAL, 0
    );
  final public static TradeType
    ALL_SPECIAL_ITEMS[] = TradeType.typesSoFar();
  
  
  final public static TradeType
    LIFE_SUPPORT = new TradeType(
      BC, "Life Support", "life_S.png", FORM_PROVISION, 5
    ),
    POWER = new TradeType(
      BC, "Power"       , "power.png" , FORM_PROVISION, 10
    ),
    ADMIN = new TradeType(
      BC, "Admin"       , "admin.png" , FORM_PROVISION, 15
    ),
    OPEN_WATER = new TradeType(
      BC, "Open Water"  , "water.png" , FORM_PROVISION, 20
    );
  final public static TradeType
    ALL_PROVISIONS[] = TradeType.typesSoFar();
  
  
  final public static TradeType
    SERVICE_ENTERTAIN = new TradeType(
      BC, "Entertainment", null, FORM_SERVICE, 0
    ),
    SERVICE_HEALTHCARE = new TradeType(
      BC, "Healthcare"   , null, FORM_SERVICE, 0
    ),
    SERVICE_HOUSING = new TradeType(
      BC, "Housing"      , null, FORM_SERVICE, 0
    ),
    SERVICE_REFUGE = new TradeType(
      BC, "Refuge"       , null, FORM_SERVICE, 0
    ),
    SERVICE_SECURITY = new TradeType(
      BC, "Security"     , null, FORM_SERVICE, 0
    ),
    SERVICE_ADMIN = new TradeType(
      BC, "Admin"        , null, FORM_SERVICE, 0
    ),
    SERVICE_COMMERCE = new TradeType(
      BC, "Commerce"     , null, FORM_SERVICE, 0
    ),
    SERVICE_REPAIRS = new TradeType(
      BC, "Repairs"      , null, FORM_SERVICE, 0
    ),
    SERVICE_ARMAMENT = new TradeType(
      BC, "Armament"     , null, FORM_SERVICE, 0
    );
  final public static TradeType
    ALL_SERVICES[] = TradeType.typesSoFar();
  
  

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
  final public static TradeType
    ALL_DEVICES[] = TradeType.typesSoFar();
  
  
  //  TODO:  You should have skins associated with some of these...
  final public static OutfitType
    
    OVERALLS       = new OutfitType(
      BC, "Overalls"      , 1, 0, 50,
      FRSD.class, 1, PLASTICS, 5, ASSEMBLY
    ),
    FINERY         = new OutfitType(
      BC, "Finery"        , 1, 0 , 400,
      FRSD.class, 2, PLASTICS, 15, GRAPHIC_DESIGN
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
      FRSD.class, 1, PLASTICS, 1, PARTS, 10, HANDICRAFTS
    ),
    STEALTH_SUIT   = new OutfitType(
      BC, "Stealth Suit"  , 8, 5, 250,
      FRSD.class, 1, PLASTICS, 2, PARTS, 15, HANDICRAFTS
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
  final public static TradeType
    ALL_OUTFITS[] = TradeType.typesSoFar();
  
  
  final public static TradeType
    ALL_ITEM_TYPES[] = TradeType.allTypes();
  
  
  final public static Conversion
    
    METALS_TO_PARTS = new Conversion(
      EngineerStation.class, 1, ORES, TO, 2, PARTS,
      MODERATE_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    
    WASTE_TO_PLASTICS = new Conversion(
      FRSD.class, TO, 1, PLASTICS,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, ASSEMBLY
    ),
    
    PLASTICS_TO_DECOR = new Conversion(
      FRSD.class, 1, PLASTICS, TO, 2, ARTWORKS,
      STRENUOUS_DC, GRAPHIC_DESIGN, MODERATE_DC, HANDICRAFTS
    ),
    
    PLASTICS_TO_PRESSFEED = new Conversion(
      Bastion.class, 1, PLASTICS, TO, 10, PRESSFEED,
      SIMPLE_DC, ACCOUNTING, DIFFICULT_DC, GRAPHIC_DESIGN
    ),
    
    PARTS_TO_DATALINKS = new Conversion(
      Archives.class, 1, PARTS, TO, 5, DATALINKS,
      MODERATE_DC, INSCRIPTION, SIMPLE_DC, ASSEMBLY, ACCOUNTING
    ),
    
    METALS_TO_FUEL = new Conversion(
      Reactor.class, 10, ORES, TO, 1, FUEL_RODS,
      MODERATE_DC, CHEMISTRY, MODERATE_DC, FIELD_THEORY
    ),
    
    WASTE_TO_CARBS = new Conversion(
      CultureLab.class, TO, 1, CARBS,
      SIMPLE_DC, CHEMISTRY
    ),
    
    CARBS_TO_PROTEIN = new Conversion(
      CultureLab.class, 2, CARBS, TO, 1, PROTEIN,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GENE_CULTURE
    ),
    
    CARBS_TO_SOMA = new Conversion(
      CultureLab.class, 2, CARBS, TO, 1, SOMA,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ),
    
    PROTEIN_TO_STIM_KITS = new Conversion(
      CultureLab.class, 1, PROTEIN, TO, 10, STIM_KITS,
      ROUTINE_DC, PHARMACY, ROUTINE_DC, CHEMISTRY
    ),
    PROTEIN_TO_ORGANS = new Conversion(
      CultureLab.class, 5, PROTEIN, 5, POWER, TO, 1, ORGANS,
      MODERATE_DC, GENE_CULTURE, ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ),
    
    STIM_KITS_TO_MEDICINE = new Conversion(
      PhysicianStation.class, 1, STIM_KITS, TO, 5, MEDICINES,
      MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    );
  
}





