/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.graphics.common.*;



public interface Economy extends Qualities {
  
  
  final static float
    NUM_WEAR_DAYS = Backgrounds.NUM_DAYS_PAY;
  
  final static Class BC = Economy.class;
  final public static int
    FORM_COMMODITY      = 0, FC = 0,
    FORM_PROVISION      = 1, FP = 1,
    FORM_UNIQUE         = 2, FU = 2,
    FORM_DEVICE         = 3, FD = 3,
    FORM_OUTFIT         = 4, FO = 4,
    FORM_SERVICE        = 5, FS = 5;
  
  final public static Service
    //
    //  Food types-
    CARBS       = new Service(BC, "Carbs"     , "carbs.gif"      , FC, 10 ),
    GREENS      = new Service(BC, "Greens"    , "greens.gif"     , FC, 25 ),
    PROTEIN     = new Service(BC, "Protein"   , "protein.gif"    , FC, 40 ),
    POLYMER     = new Service(BC, "Organics"  , "polymers.gif"   , FC, 60 ),
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
  
  
  //  TODO:  Housing as Service?
  //  Insolation and Land Area.
  //  Moisture and Sea Area.
  //  Minerals and Rock Area.
  
  
  final public static Service
    WATER        = new Service(BC, "Water"       , "water.png"    , FP, 10),
    LIFE_SUPPORT = new Service(BC, "Life Support", "life_S.png"   , FP, 10),
    POWER        = new Service(BC, "Power"       , "power.png"    , FP, 10),
    
    ALL_PROVISIONS[] = Service.typesSoFar();
  
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
    ENERGY_DRAIN = 1 << 12;
  
  
  //  TODO:  Replace most of these with a shorter set of categories.
  //  Halberd Guns.  Blasters.  Stunners.  Burners.
  //  Frames & Bracers.  Shivs.  Artillery.  Arc Blades.
  
  
  final public static DeviceType
    
    MANIPLES = new DeviceType(
      BC, "Maniples",
      "maniples", AnimNames.BUILD,
      2, GRAPPLE | MELEE | PHYSICAL, 10,
      Artificer.class, 3, PARTS, 5, ASSEMBLY
    ),
    LASER_DRILL = new DeviceType(
      BC, "Laser Drill",
      "laser drill", AnimNames.BUILD,
      5, RANGED | ENERGY, 10,
      Artificer.class, 2, PARTS, 5, ASSEMBLY
    ),
    MODUS_LUTE = new DeviceType(
      BC, "Modus Lute",
      "modus lute", AnimNames.TALK_LONG,
      0, NONE, 40,
      Artificer.class, 1, PARTS, 10, ASSEMBLY
    ),
    BIOCORDER = new DeviceType(
      BC, "Biocorder",
      "biocorder", AnimNames.LOOK,
      0, NONE, 55,
      Artificer.class, 2, PARTS, 15, ASSEMBLY
    ),
    
    STUN_PISTOL = new DeviceType(
      BC, "Stun Pistol",
      "pistol", AnimNames.FIRE,
      10, RANGED | PHYSICAL | STUN | HOMING, 35,
      Artificer.class, 1, PARTS, 10, ASSEMBLY
    ),
    BLASTER = new DeviceType(
      BC, "Blaster",
      "pistol", AnimNames.FIRE,
      15, RANGED | ENERGY | PLASMA, 25,
      Artificer.class, 1, PARTS, 10, ASSEMBLY
    ),
    ARC_SABRE = new DeviceType(
      BC, "Arc Sabre",
      "staff", AnimNames.STRIKE,
      25, MELEE | ENERGY, 100,
      Artificer.class, 3, PARTS, 15, ASSEMBLY
    ),
    
    JAVELIN = new DeviceType(
      BC, "Javelin",
      "spear", AnimNames.STRIKE,//"staff"
      10, RANGED | PHYSICAL, 5,
      null, 5, HANDICRAFTS
    ),
    TOOTH_SHIV = new DeviceType(
      BC, "Tooth Shiv",
      "light blade", AnimNames.STRIKE,
      5, MELEE | PHYSICAL, 5,
      null, 5, HANDICRAFTS
    ),
    
    INTRINSIC_MELEE_WEAPON = new DeviceType(
      BC, "Intrinsic Melee Weapon",
      null, AnimNames.STRIKE,
      0, MELEE | PHYSICAL, 0,
      null
    ),
    INTRINSIC_ENERGY_WEAPON = new DeviceType(
      BC, "Intrinsic Energy Weapon",
      null, AnimNames.FIRE,
      0, RANGED | ENERGY, 0,
      null
    );
  final public static Service
    ALL_IMPLEMENTS[] = Service.typesSoFar();
  
  
  //
  //  TODO:  You should have skins associated with some of these.
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
    
    CAMOUFLAGE     = new OutfitType(
      BC, "Camouflage"    , 3, 0, 70,
      FRSD.class, 1, PLASTICS, 5, HANDICRAFTS
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
      Artificer.class, 1, PARTS, 5, ASSEMBLY
    ),
    PARTIAL_ARMOUR = new OutfitType(
      BC, "Partial Armour", 10, 10, 100,
      Artificer.class, 2, PARTS, 10, ASSEMBLY
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 15, 10, 150,
      Artificer.class, 5, PARTS, 15, ASSEMBLY
    ),
    POWER_ARMOUR   = new OutfitType(
      BC, "Power Armour"  , 20, 10, 275,
      Artificer.class, 8, PARTS, 20, ASSEMBLY
    ),
    GOLEM_FRAME = new OutfitType(
      BC, "Golem Frame"  , 25, 10, 500,
      Artificer.class, 12, PARTS, 25, ASSEMBLY
    ),
    
    NATURAL_ARMOUR = new OutfitType(
      BC, "Natural Armour", 0, 0, 0,
      null
    ),
    ARTILECT_ARMOUR = new OutfitType(
      BC, "Artilect Armour", 0, 10, 0,
      null
    );
  final public static Service
    ALL_OUTFITS[] = Service.typesSoFar();
  
  final public static Service
    ALL_ITEM_TYPES[] = Service.allTypes();
  
  
  
  
  final public static Object TO = new Object();
  
  final public static Conversion
    //
    //  Artificer conversions-
    METALS_TO_PARTS = new Conversion(
      Artificer.class, 1, METALS, TO, 2, PARTS,
      MODERATE_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    
    PARTS_TO_CIRCUITRY = new Conversion(
      Artificer.class, 1, PARTS, TO, 5, CIRCUITRY,
      DIFFICULT_DC, ASSEMBLY, ROUTINE_DC, INSCRIPTION, SIMPLE_DC, FIELD_THEORY
    ),
    
    WASTE_TO_PLASTICS = new Conversion(
      FRSD.class, TO, 1, PLASTICS,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, ASSEMBLY
    ),
    
    PLASTICS_TO_DECOR = new Conversion(
      FRSD.class, 1, PLASTICS, TO, 2, FIXTURES,
      STRENUOUS_DC, GRAPHIC_DESIGN, MODERATE_DC, HANDICRAFTS
    ),
    
    /*
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
    //*/
    
    //
    //  Archives conversions-
    CIRCUITRY_TO_DATALINKS = new Conversion(
      Archives.class, 1, CIRCUITRY, TO, 5, DATALINKS,
      MODERATE_DC, INSCRIPTION, SIMPLE_DC, ASSEMBLY, ACCOUNTING
    ),
    
    //
    //  Reactor conversions-
    METALS_TO_FUEL = new Conversion(
      Reactor.class, 10, METALS, TO, 1, FUEL_RODS,
      MODERATE_DC, CHEMISTRY, MODERATE_DC, FIELD_THEORY
    ),
    
    //
    //  Culture Vats/Sickbay conversions-
    WASTE_TO_CARBS = new Conversion(
      CultureVats.class, TO, 1, CARBS,
      SIMPLE_DC, CHEMISTRY
    ),
    
    CARBS_TO_PROTEIN = new Conversion(
      CultureVats.class, 2, CARBS, TO, 1, PROTEIN,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GENE_CULTURE
    ),
    
    CARBS_TO_SOMA = new Conversion(
      CultureVats.class, 2, CARBS, TO, 1, SOMA,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ),
    
    SPICE_TO_MEDICINE = new Conversion(
      CultureVats.class, 1, TRUE_SPICE, TO, 5, MEDICINE,
      MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ),
    
    MEDICINE_TO_STIM_KITS = new Conversion(
      Sickbay.class, 1, MEDICINE, TO, 10, STIM_KITS,
      ROUTINE_DC, PHARMACY, ROUTINE_DC, CHEMISTRY
    ),
    
    PROTEIN_TO_REPLICANTS = new Conversion(
      CultureVats.class, 5, PROTEIN, 5, POWER, TO, 1, REPLICANTS,
      MODERATE_DC, GENE_CULTURE, ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    );
}






