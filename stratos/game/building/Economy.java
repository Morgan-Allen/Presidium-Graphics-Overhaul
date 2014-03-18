/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.graphics.common.*;



/*
SPYCE_A = new Item.Type(C, COMMODITY, "Spyce A (Tinerazine)", 400),
SPYCE_B = new Item.Type(C, COMMODITY, "Spyce B (Halebdynum)", 400),
SPYCE_C = new Item.Type(C, COMMODITY, "Spyce C (Natrizoral)", 400),
//*/
//  TODO:  Each commodity type should have a description attached.




public interface Economy extends Abilities {
  

  final static Class BC = Economy.class ;
  final public static int
    FORM_COMMODITY      = 0, FC = 0,
    FORM_PROVISION      = 1, FP = 1,
    FORM_UNIQUE         = 2, FU = 2,
    FORM_DEVICE         = 3, FD = 3,
    FORM_OUTFIT         = 4, FO = 4,
    FORM_SERVICE        = 5, FS = 5 ;
  
  final public static Service
    //
    //  Food types-
    CARBS       = new Service(BC, "Carbs"     , "carbs.gif"      , FC, 10 ),
    GREENS      = new Service(BC, "Greens"    , "greens.gif"     , FC, 25 ),
    PROTEIN     = new Service(BC, "Protein"   , "protein.gif"    , FC, 40 ),
    POLYMERS    = new Service(BC, "Polymers"  , "polymers.gif"   , FC, 60 ),
    SPICE_OIL   = new Service(BC, "Spice Oil" , "spice oil.gif"  , FC, 100),
    SPICE_GALL  = new Service(BC, "Spice Gall", "spice gall.gif" , FC, 200),
    //
    //  Mineral wealth-
    //  Metal Ore
    //  Isotopes
    METALS      = new Service(BC, "Metals"    , "ores.gif"       , FC, 20 ),
    FUEL_RODS   = new Service(BC, "Fuel Rod"  , "fuel rods.gif"  , FC, 55 ),
    ANTIMASS    = new Service(BC, "Antimass"  , "antimass.gif"   , FC, 120),
    SPICE_SALT  = new Service(BC, "Spice Salt", "spice salt.gif" , FC, 150),
    //
    //  Building materials-
    PLASTICS    = new Service(BC, "Plastics"  , "plastics.gif"   , FC, 35 ),
    PRESSFEED   = new Service(BC, "Pressfeed" , "pressfeed.gif"  , FC, 60 ),
    FIXTURES    = new Service(BC, "Fixtures"  , "decor.gif"      , FU, 160),
    PARTS       = new Service(BC, "Parts"     , "parts.gif"      , FC, 50 ),
    CIRCUITRY   = new Service(BC, "Circuitry" , "inscription.gif", FC, 140),
    DATALINKS   = new Service(BC, "Datalinks" , "datalinks.png"  , FC, 200),
    //
    //  Medical supplies-
    STIM_KITS   = new Service(BC, "Stim Kit"  , "stimkit.gif"    , FC, 40 ),
    RATION_KIT  = new Service(BC, "Ration Kit", "rations.gif"    , FC, 55 ),
    SOMA        = new Service(BC, "Soma"      , "soma.gif"       , FC, 70 ),
    MEDICINE    = new Service(BC, "Medicine"  , "medicines.gif"  , FC, 200),
    TRUE_SPICE  = new Service(BC, "True Spice", "spices.gif"     , FC, 500),
    
    
    //  Replicants.  Gene seed.  Ghost line.  Trophies.
    //  Samples.  Raw Credits.  Atomics.  Artifact.  Captives.
    
    ALL_FOOD_TYPES[] = { CARBS, PROTEIN, GREENS, RATION_KIT },
    ALL_COMMODITIES[] = Service.typesSoFar();
  
  
  //  TODO:  Either get rid of these, or move them back above!
  //*
  final public static Service
    
    SAMPLES     = new Service(BC, "Samples"   , "crates_big.gif", FU,  -1),
    CREDITS     = new Service(BC, "Credits"   , null            , FU,  -1),
    
    TROPHIES    = new Service(BC, FORM_UNIQUE , "Trophy"        ,     100),
    ARTIFACTS   = new Service(BC, FORM_UNIQUE , "Artifact"      ,     100),
    
    GENE_SEED   = new Service(BC, "Gene Seed" , "gene_seed.gif" , FU, 200),
    REPLICANTS  = new Service(BC, "Replicants", "replicant.gif" , FU, 200),
    GHOSTLINE   = new Service(BC, FORM_UNIQUE , "Ghostline"     ,     200),
    
    ATOMICS     = new Service(BC, FORM_UNIQUE , "Atomic"        ,    1000),
    
    ALL_UNIQUE_ITEMS[] = Service.typesSoFar();
  //*/
  
  final public static Service
    WATER        = new Service(BC, "Water"       , "water.png"    , FP, 10),
    LIFE_SUPPORT = new Service(BC, "Life Support", "life_S.png"   , FP, 10),
    POWER        = new Service(BC, "Power"       , "power.png"    , FP, 10),
    
    ALL_PROVISIONS[] = Service.typesSoFar() ;
  
  final public static Service
    SERVICE_ADMIN    = new Service(BC, FORM_SERVICE, "Admin"      , 0),
    SERVICE_TREAT    = new Service(BC, FORM_SERVICE, "Treatment"  , 0),
    SERVICE_PERFORM  = new Service(BC, FORM_SERVICE, "Performance", 0),
    SERVICE_DEPOT    = new Service(BC, FORM_SERVICE, "Depot"      , 0),
    SERVICE_REFUGE   = new Service(BC, FORM_SERVICE, "Refuge"     , 0),
    SERVICE_ARMAMENT = new Service(BC, FORM_SERVICE, "Armament"   , 0),
    SERVICE_SHIPPING = new Service(BC, FORM_SERVICE, "Shipping"   , 0),
    SERVICE_CAPTIVES = new Service(BC, FORM_SERVICE, "Captives"   , 0),
    SERVICE_CONSORTS = new Service(BC, FORM_SERVICE, "Consorts"   , 0),
    
    ALL_ABSTRACT_SERVICES[] = Service.typesSoFar();
  
  
  
  
  final public static int
    NONE     = 0,
    //
    //  These are properties of equipped weapons-
    MELEE    = 1 << 0,
    RANGED   = 1 << 1,
    ENERGY   = 1 << 2,
    PHYSICAL = 1 << 3,
    STUN     = 1 << 4,
    POISON   = 1 << 5,
    HOMING   = 1 << 6,
    PLASMA   = 1 << 7,
    //
    //  These are properties of natural weapons or armour-
    GRAPPLE      = 1 << 8,
    CAUSTIC      = 1 << 9,
    TRANSMORPHIC = 1 << 10,
    ENERGY_DRAIN = 1 << 12 ;
  
  
  final public static DeviceType
    
    MANIPLES = new DeviceType(BC, "Maniples",
      2, GRAPPLE | MELEE | PHYSICAL, 10,
      new Conversion(3, PARTS, "Foundry", 5, ASSEMBLY),
      "maniples", AnimNames.BUILD
    ),
    LASER_DRILL = new DeviceType(BC, "Laser Drill",
      5, RANGED | ENERGY, 10,
      new Conversion(2, PARTS, "Foundry", 5, ASSEMBLY),
      "laser drill", AnimNames.BUILD
    ),
    MODUS_LUTE = new DeviceType(BC, "Modus Lute",
      0, NONE, 40,
      new Conversion(1, PARTS, "Foundry", 10, ASSEMBLY),
      "modus lute", AnimNames.TALK_LONG
    ),
    BIOCORDER = new DeviceType(BC, "Biocorder",
      0, NONE, 55,
      new Conversion(2, PARTS, "Foundry", 15, ASSEMBLY),
      "biocorder", AnimNames.LOOK
    ),
    
    STUN_PISTOL = new DeviceType(BC, "Stun Pistol",
      10, RANGED | PHYSICAL | STUN | HOMING, 35,
      new Conversion(1, PARTS, "Foundry", 10, ASSEMBLY),
      "pistol", AnimNames.FIRE
    ),
    BLASTER = new DeviceType(BC, "Blaster",
      15, RANGED | ENERGY | PLASMA, 25,
      new Conversion(1, PARTS, "Foundry", 10, ASSEMBLY),
      "pistol", AnimNames.FIRE
    ),
    MISSILE_PACK = new DeviceType(BC, "Missile Pack",
      15, RANGED | PHYSICAL | HOMING, 30,
      new Conversion(2, PARTS, "Foundry", 15, ASSEMBLY),
      "shoulder", AnimNames.FIRE
    ),
    MICROWAVE_BEAM = new DeviceType(BC, "Microwave Beam",
      20, RANGED | ENERGY | STUN, 45,
      new Conversion(3, PARTS, "Foundry", 15, ASSEMBLY),
      "shoulder", AnimNames.FIRE
    ),
    RAIL_CANNON = new DeviceType(BC, "Rail Cannon",
      25, RANGED | PHYSICAL, 60,
      new Conversion(3, PARTS, "Foundry", 20, ASSEMBLY),
      "cannon", AnimNames.FIRE
    ),
    DISINTEGRATOR = new DeviceType(BC, "Disintegrator",
      30, RANGED | ENERGY, 120,
      new Conversion(5, PARTS, "Foundry", 25, ASSEMBLY),
      "cannon", AnimNames.FIRE
    ),
    
    JAVELIN = new DeviceType(BC, "Javelin",
      10, RANGED | PHYSICAL, 5,
      new Conversion(5, HANDICRAFTS),
      "spear", AnimNames.STRIKE//"staff"
    ),
    TOOTH_KNIFE = new DeviceType(BC, "Tooth Knife",
      5, MELEE | PHYSICAL, 5,
      new Conversion(5, HANDICRAFTS),
      "light blade", AnimNames.STRIKE
    ),
    
    SHOCK_STAFF = new DeviceType(BC, "Shock Staff",
      15, MELEE | PHYSICAL | STUN, 40,
      new Conversion(2, PARTS, "Foundry", 10, ASSEMBLY),
      "staff", AnimNames.STRIKE
    ),
    ARC_SABRE = new DeviceType(BC, "Arc Sabre",
      25, MELEE | ENERGY, 100,
      new Conversion(3, PARTS, "Foundry", 15, ASSEMBLY),
      "staff", AnimNames.STRIKE
    ),
    FIST_SHIV = new DeviceType(BC, "Fist Shiv",
      10, MELEE | PHYSICAL, 10,
      new Conversion(1, PARTS, "Foundry", 0, ASSEMBLY),
      "light blade", AnimNames.STRIKE
    ),
    KONOCHE = new DeviceType(BC, "Konoche",
      20, MELEE | PHYSICAL, 45,
      new Conversion(2, PARTS, "Foundry", 5, ASSEMBLY),
      "heavy blade", AnimNames.STRIKE_BIG
    ),
    
    INTRINSIC_MELEE_WEAPON = new DeviceType(
      BC, "Intrinsic Melee Weapon", 0, MELEE | PHYSICAL, 0,
      null, null, AnimNames.STRIKE
    ),
    INTRINSIC_ENERGY_WEAPON = new DeviceType(
      BC, "Intrinsic Energy Weapon", 0, RANGED | ENERGY, 0,
      null, null, AnimNames.FIRE
    ) ;
  final public static Service
    ALL_IMPLEMENTS[] = Service.typesSoFar() ;
  
  
  //
  //  TODO:  You should have skins associated with some of these.
  final public static OutfitType
    
    OVERALLS       = new OutfitType(
      BC, "Overalls"      , 1, 0, 50,
      new Conversion(1, PLASTICS, "Fabricator", 5, ASSEMBLY)
    ),
    FINERY         = new OutfitType(
      BC, "Finery"        , 1, 0 , 400,
      new Conversion(2, PLASTICS, "Fabricator", 15, GRAPHIC_DESIGN)
    ),
    SCRAP_GEAR = new OutfitType(
      BC, "Scrap Gear", 3, 0, 5,
      new Conversion(0, HANDICRAFTS)
    ),
    
    CAMOUFLAGE     = new OutfitType(
      BC, "Camouflage"    , 3, 0, 70,
      new Conversion(1, PLASTICS, "Fabricator", 5, HANDICRAFTS)
    ),
    SEALSUIT       = new OutfitType(
      BC, "Sealsuit"      , 4, 1, 150,
      new Conversion(1, PLASTICS, 1, PARTS, "Fabricator", 10, HANDICRAFTS)
    ),
    STEALTH_SUIT   = new OutfitType(
      BC, "Stealth Suit"  , 8, 5, 250,
      new Conversion(1, PLASTICS, 2, PARTS, "Fabricator", 15, HANDICRAFTS)
    ),
    
    BELT_AND_BRACER = new OutfitType(
      BC, "Belt and Bracer"   , 5, 10, 50,
      new Conversion(1, PARTS, "Foundry", 5, ASSEMBLY)
    ),
    PARTIAL_ARMOUR = new OutfitType(
      BC, "Partial Armour", 10, 10, 100,
      new Conversion(2, PARTS, "Foundry", 10, ASSEMBLY)
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 15, 10, 150,
      new Conversion(5, PARTS, "Foundry", 15, ASSEMBLY)
    ),
    POWER_ARMOUR   = new OutfitType(
      BC, "Power Armour"  , 20, 10, 275,
      new Conversion(8, PARTS, "Foundry", 20, ASSEMBLY)
    ),
    GOLEM_FRAME = new OutfitType(
      BC, "Golem Frame"  , 25, 10, 500,
      new Conversion(12, PARTS, "Foundry", 25, ASSEMBLY)
    ),
    //  Myrmidone armour, Suspensor, Inhibitor Collar.
    
    NATURAL_ARMOUR = new OutfitType(
      BC, "Natural Armour", 0, 0, 0, null
    ),
    ARTILECT_ARMOUR = new OutfitType(
      BC, "Artilect Armour", 0, 10, 0, null
    ) ;
  final public static Service
    ALL_OUTFITS[] = Service.typesSoFar() ;
  
  final public static Service
    ALL_ITEM_TYPES[] = Service.allTypes() ;
  
  
  
  
  final public static Object TO = new Object() ;
  
  final public static Conversion
    //
    //  Artificer conversions-
    METALS_TO_PARTS = new Conversion(
      1, METALS, TO, 2, PARTS,
      "Foundry",
      MODERATE_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    
    PARTS_TO_CIRCUITRY = new Conversion(
      1, PARTS, TO, 5, CIRCUITRY,
      "Foundry",
      DIFFICULT_DC, ASSEMBLY, ROUTINE_DC, INSCRIPTION, SIMPLE_DC, FIELD_THEORY
    ),
    
    CARBS_TO_PLASTICS = new Conversion(
      1, CARBS, TO, 2, PLASTICS,
      "Fabricator",
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, HANDICRAFTS
    ),
    
    PLASTICS_TO_DECOR = new Conversion(
      1, PLASTICS, TO, 2, FIXTURES,
      "Fabricator",
      STRENUOUS_DC, GRAPHIC_DESIGN, MODERATE_DC, HANDICRAFTS
    ),
    
    //
    //  Audit Office conversions-
    PLASTICS_TO_CREDITS = new Conversion(
      1, PLASTICS, TO, 500, CREDITS,
      "AuditOffice",
      MODERATE_DC, ACCOUNTING, MODERATE_DC, GRAPHIC_DESIGN
    ),
    
    PLASTICS_TO_PRESSFEED = new Conversion(
      1, PLASTICS, TO, 10, PRESSFEED,
      "AuditOffice",
      SIMPLE_DC, ACCOUNTING, DIFFICULT_DC, GRAPHIC_DESIGN
    ),
    
    //
    //  Archives conversions-
    CIRCUITRY_TO_DATALINKS = new Conversion(
      1, CIRCUITRY, TO, 5, DATALINKS,
      "Archives",
      MODERATE_DC, INSCRIPTION, SIMPLE_DC, ASSEMBLY, ACCOUNTING
    ),
    
    //
    //  Reactor conversions-
    METALS_TO_FUEL = new Conversion(
      10, METALS, TO, 1, FUEL_RODS,
      "Generator",
      MODERATE_DC, CHEMISTRY, MODERATE_DC, FIELD_THEORY
    ),
    
    //
    //  Culture Vats/Sickbay conversions-
    POWER_TO_CARBS = new Conversion(
      1, POWER, TO, 1, CARBS,
      "CultureVats",
      SIMPLE_DC, CHEMISTRY
    ),
    
    POWER_TO_PROTEIN = new Conversion(
      2, POWER, TO, 1, PROTEIN,
      "CultureVats",
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GENE_CULTURE
    ),
    
    GREENS_TO_SOMA = new Conversion(
      2, POWER, 1, GREENS, TO, 10, SOMA,
      "CultureVats",
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ),
    
    SPICE_TO_MEDICINE = new Conversion(
      5, POWER, 1, TRUE_SPICE, TO, 5, MEDICINE,
      "CultureVats",
      MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ),
    
    MEDICINE_TO_STIM_KITS = new Conversion(
      1, MEDICINE, TO, 10, STIM_KITS,
      "Sickbay",
      ROUTINE_DC, PHARMACY, ROUTINE_DC, CHEMISTRY
    ),
    
    PROTEIN_TO_REPLICANTS = new Conversion(
      5, PROTEIN, 5, POWER, TO, 1, REPLICANTS,
      "CultureVats",
      MODERATE_DC, GENE_CULTURE, ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ) ;
}






