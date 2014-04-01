/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.building.*;
import stratos.game.campaign.System;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.util.*;


//
//  TODO:  Backgrounds need to include their own descriptions.


//TODO:  I'm going to limit this to a few basic headings now.
/*
VASSALS & AGENTS

Trooper        Logician
Explorer       Priest/ess
Runner         Spacer
Seer           Changer
               Glaive Knight
Jovian         Collective
Changeling     
Krech          Noble

Enforcer       Performer
Auditor        Minder
Labourer/Pyon  Trader
Physician      
Engineer       Brave
Ecologist      Gatherer
//*/




public class Background implements Economy, Session.Saveable {
  
  
  
  final public static Float
    ALWAYS    =  1.0f,
    OFTEN     =  0.6f,
    SOMETIMES =  0.3f,
    RARELY    = -0.7f,
    NEVER     = -1.0f ;
  //*
  final public static Integer
    LEARNING  = 0,
    NOVICE    = 5,
    PRACTICED = 10,
    EXPERT    = 15,
    MASTER    = 20 ;
  //*/
  
  final public static int
    SLAVE_CLASS  =  0,
    LOWER_CLASS  =  1,
    MIDDLE_CLASS =  2,
    UPPER_CLASS  =  3,
    RULER_CLASS  =  4 ;
  final public static int
    GUILD_MILITANT  =  0,
    GUILD_MERCHANT  =  1,
    GUILD_AESTHETE  =  2,
    GUILD_ARTIFICER =  3,
    GUILD_ECOLOGIST =  4,
    GUILD_PHYSICIAN =  5,
    GUILD_RUNNER    =  6,
    GUILD_NATIVE    =  7,
    GUILD_COURT     =  8,
    NOT_A_GUILD     = -1 ;

  final public static float
    NUM_DAYS_PAY = 20,
    PAY_INTERVAL = World.STANDARD_DAY_LENGTH * 20;
  final public static int HIRE_COSTS[] = {
    150, 250, 500, 1000, 2500  //Represents PAY_INTERVAL days' salary.
  } ;
  
  
  final protected static Object
    MAKES = new Object(),
    NEEDS = new Object() ;
  
  private static int nextID = 0 ;
  final public int ID = nextID++ ;
  private static Batch <Background> all = new Batch() ;
  
  
  //*
  final public static Background
    MALE_BIRTH = new Background(
      "Born Male", null, null, -1, NOT_A_GUILD,
      3, MUSCULAR, 1, MOTOR, SOMETIMES, TALL, NEVER, FEMININE
    ),
    FEMALE_BIRTH = new Background(
      "Born Female", null, null, -1, NOT_A_GUILD,
      2, IMMUNE, 2, PERCEPT, RARELY, STOUT, ALWAYS, FEMININE
    ),
    //
    //  Natives can only be recruited locally, not from offworld.
    NATIVE_BIRTH = new Background(
      "Born Native", "native_skin.gif", null, -1, NOT_A_GUILD,
      NOVICE, NATIVE_TABOO, LEARNING, HANDICRAFTS, MARKSMANSHIP, XENOZOOLOGY
    ),
    //
    //  The following are available to most actors as part of their careers-
    DREGS_BIRTH = new Background(
      "Born Dreg", "artificer_skin.gif", null, SLAVE_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, NATIVE_TABOO, HAND_TO_HAND, CHEMISTRY
    ),
    PYON_BIRTH = new Background(
      "Born Pyon", "pyon_skin.gif", null, LOWER_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, HARD_LABOUR, DOMESTICS, ASSEMBLY
    ),
    FREE_BIRTH = new Background(
      "Born Free", "citizen_skin.gif", null, MIDDLE_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, SUASION, MARKSMANSHIP, PILOTING
    ),
    GELDER_BIRTH = new Background(
      "Born Gelder", "vendor_skin.gif", null, UPPER_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, NOBLE_ETIQUETTE, ACCOUNTING, COUNSEL
    ),
    //
    //  Highborn are not available as normally-generated citizens, only as
    //  visiting NPCs or members of your household.
    LANDER_BIRTH = new Background(
      "Born Lander", "highborn_male_skin.gif", null, RULER_CLASS,
      NOVICE, NOBLE_ETIQUETTE, LEARNING, COMMON_CUSTOM, HAND_TO_HAND, ACCOUNTING
    ),
    HIGH_BIRTH = new Background(
      "Born High", "highborn_male_skin.gif", null, RULER_CLASS,
      NOVICE, NOBLE_ETIQUETTE, LEARNING, COMMAND, HAND_TO_HAND, ANCIENT_LORE
    ),
    
    OPEN_CLASSES[] = { DREGS_BIRTH, PYON_BIRTH, FREE_BIRTH, GELDER_BIRTH } ;
  
  
  
  
  final public static Background
    
    EXCAVATOR = new Background(
      "Excavator", "pyon_skin.gif", null,
      LOWER_CLASS, GUILD_ARTIFICER,
      EXPERT, HARD_LABOUR, NOVICE, GEOPHYSICS, ASSEMBLY, LEARNING, ANCIENT_LORE,
      OFTEN, STUBBORN, RARELY, NERVOUS, HANDSOME,
      OVERALLS
    ),
    
    TECHNICIAN = new Background(
      "Technician", "artificer_skin.gif", "artificer_portrait.png",
      LOWER_CLASS, GUILD_ARTIFICER,
      PRACTICED, ASSEMBLY, HARD_LABOUR, NOVICE, FIELD_THEORY, CHEMISTRY,
      SOMETIMES, DUTIFUL, RARELY, RELAXED,
      OVERALLS
    ),
    
    CORE_TECHNICIAN = new Background(
      "Core Technician", "citizen_skin.gif", "artificer_portrait.png",
      MIDDLE_CLASS, GUILD_ARTIFICER,
      EXPERT, FIELD_THEORY, PRACTICED, CHEMISTRY, ASSEMBLY,
      NOVICE, SHIELD_AND_ARMOUR,
      OFTEN, DUTIFUL, SOMETIMES, NERVOUS,
      SEALSUIT
    ),
    
    ARTIFICER = new Background(
      "Artificer", "artificer_skin.gif", "artificer_portrait.png",
      UPPER_CLASS, GUILD_ARTIFICER,
      EXPERT, ASSEMBLY, PRACTICED, FIELD_THEORY, SHIELD_AND_ARMOUR,
      NOVICE, ANCIENT_LORE, CHEMISTRY,
      SOMETIMES, CURIOUS, RARELY, NATURALIST,
      OVERALLS
    ),
    
    ARTIFICER_CIRCLES[] = { EXCAVATOR, TECHNICIAN, CORE_TECHNICIAN, ARTIFICER }
  ;

  final public static Background
    
    MEDIC = new Background(
      "Medic", "citizen_skin.gif", null,
      LOWER_CLASS, GUILD_PHYSICIAN,
      PRACTICED, DOMESTICS, SUASION, NOVICE, ANATOMY, PHARMACY, COUNSEL,
      OFTEN, EMPATHIC, SOMETIMES, STUBBORN,
      OVERALLS
    ),
    
    BIOCHEMIST = new Background(
      "Biochemist", "citizen_skin.gif", null,
      MIDDLE_CLASS, GUILD_PHYSICIAN,
      PRACTICED, GENE_CULTURE, PHARMACY, CHEMISTRY,
      RARELY, INDULGENT, RELAXED,
      OVERALLS
    ),
    
    SAVANT = new Background(
      "Savant", "citizen_skin.gif", null,
      MIDDLE_CLASS, GUILD_PHYSICIAN,
      EXPERT, ACCOUNTING, INSCRIPTION, PRACTICED, COUNSEL, ASSEMBLY,
      NOVICE, ANCIENT_LORE, LEGISLATION,
      ALWAYS, CURIOUS, SOMETIMES, NERVOUS, IMPASSIVE,
      OVERALLS
    ),
    
    PHYSICIAN = new Background(
      "Physician", "physician_skin.gif", "physician_portrait.png",
      UPPER_CLASS, GUILD_PHYSICIAN,
      EXPERT, ANATOMY, PHARMACY,
      PRACTICED, GENE_CULTURE, PSYCHOANALYSIS, COUNSEL, SUASION,
      OFTEN, CURIOUS, SOMETIMES, METICULOUS, IMPASSIVE, RARELY, INDULGENT,
      OVERALLS
    ),
    
    PHYSICIAN_CIRCLES[] = { MEDIC, BIOCHEMIST, SAVANT, PHYSICIAN }
  ;
  
  final public static Background
    
    CULTIVATOR = new Background(
      "Cultivator", "pyon_skin.gif", null,
      LOWER_CLASS, GUILD_ECOLOGIST,
      PRACTICED, CULTIVATION, HARD_LABOUR, NOVICE, DOMESTICS,
      OFTEN, OUTGOING, SOMETIMES, NATURALIST, RARELY, AMBITIOUS,
      OVERALLS
    ),
    
    ARCOLOGY_ENGINEER = new Background(
      "Arcology Engineer", "ecologist_skin.gif", null,
      MIDDLE_CLASS, GUILD_ECOLOGIST,
      PRACTICED, GEOPHYSICS, ASSEMBLY, HARD_LABOUR,
      NOVICE, GENE_CULTURE, SURVEILLANCE,
      RARELY, OUTGOING, OFTEN, POSITIVE,
      OVERALLS
    ),
    
    SURVEY_SCOUT = new Background(
      "Survey Scout", "ecologist_skin.gif", "ecologist_portrait.png",
      MIDDLE_CLASS, GUILD_ECOLOGIST,
      EXPERT, XENOZOOLOGY, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, NATIVE_TABOO,
      NOVICE, BATTLE_TACTICS, HAND_TO_HAND,
      RARELY, NERVOUS, RELAXED, OFTEN, NATURALIST,
      STUN_PISTOL, CAMOUFLAGE
    ),
    
    ECOLOGIST = new Background(
      "Ecologist", "ecologist_skin.gif", "ecologist_portrait.png",
      UPPER_CLASS, GUILD_ECOLOGIST,
      EXPERT, CULTIVATION, PRACTICED, GENE_CULTURE, XENOZOOLOGY,
      PRACTICED, GEOPHYSICS, CHEMISTRY,
      ALWAYS, NATURALIST, SOMETIMES, EMPATHIC, CURIOUS,
      SEALSUIT
    ),
    
    ECOLOGIST_CIRCLES[] = {
      CULTIVATOR, ARCOLOGY_ENGINEER, SURVEY_SCOUT, ECOLOGIST
    }
  ;
  
  final public static Background
    
    SUPPLY_CORPS = new Background(
      "Supply Corps", "pyon_skin.gif", null,
      LOWER_CLASS, GUILD_MERCHANT,
      NOVICE, PILOTING, HARD_LABOUR,
      OFTEN, RELAXED, RARELY, AMBITIOUS,
      OVERALLS
    ),
    
    SOMA_VENDOR = new Background(
      "Soma Vendor", "vendor_skin.gif", null,
      MIDDLE_CLASS, GUILD_MERCHANT,
      PRACTICED, COUNSEL, SUASION, NOVICE, DOMESTICS, CHEMISTRY,
      ACCOUNTING,
      SOMETIMES, ACQUISITIVE,
      OVERALLS
    ),
    
    STOCK_VENDOR = new Background(
      "Stock Vendor", "vendor_skin.gif", "vendor_portrait.png",
      MIDDLE_CLASS, GUILD_MERCHANT,
      PRACTICED, ACCOUNTING, DOMESTICS, NOVICE, SUASION, HARD_LABOUR,
      OVERALLS
    ),
    
    AUDITOR = new Background(
      "Auditor", "vendor_skin.gif", "vendor_portrait.png",
      UPPER_CLASS, GUILD_MERCHANT,
      EXPERT, COUNSEL, ACCOUNTING, PRACTICED, COMMAND, ANCIENT_LORE,
      ALWAYS, STUBBORN, OFTEN, DUTIFUL,
      SOMETIMES, AMBITIOUS, IMPASSIVE, RARELY, INDULGENT,
      OVERALLS
    ),
    
    VENDOR_CIRCLES[] = { SUPPLY_CORPS, SOMA_VENDOR, STOCK_VENDOR, AUDITOR }
  ;
  
  final public static Background
    
    VOLUNTEER = new Background(
      "Volunteer", "militant_skin.gif", "militant_portrait.png",
      LOWER_CLASS, GUILD_MILITANT,
      PRACTICED, HAND_TO_HAND, MARKSMANSHIP,
      NOVICE, SURVEILLANCE, ASSEMBLY, HARD_LABOUR, SHIELD_AND_ARMOUR,
      SOMETIMES, DUTIFUL, DEFENSIVE, RARELY, NERVOUS, FEMININE,
      SHOCK_STAFF, BLASTER, PARTIAL_ARMOUR
    ),
    
    TECH_RESERVE = new Background(
      "Tech Reserve", "artificer_skin.gif", "militant_portrait.png",
      MIDDLE_CLASS, GUILD_MILITANT,
      PRACTICED, HARD_LABOUR, ASSEMBLY,
      NOVICE, ANATOMY, PHARMACY, MARKSMANSHIP,
      RARELY, OUTGOING, SOMETIMES, METICULOUS,
      STUN_PISTOL, BELT_AND_BRACER
    ),
    
    SCOUT = new Background(
      "Scout", "ecologist_skin.gif", "militant_portrait.png",
      MIDDLE_CLASS, GUILD_MILITANT,
      PRACTICED, MARKSMANSHIP, SURVEILLANCE, STEALTH_AND_COVER,
      NOVICE, XENOZOOLOGY, ATHLETICS, HAND_TO_HAND,
      RARELY, NERVOUS, SOMETIMES, CURIOUS,
      BLASTER, CAMOUFLAGE
    ),
    
    VETERAN = new Background(
      "Veteran", "militant_skin.gif", "militant_portrait.png",
      UPPER_CLASS, GUILD_MILITANT,
      EXPERT, HAND_TO_HAND, MARKSMANSHIP, PRACTICED, SURVEILLANCE,
      FORMATION_COMBAT, COMMAND, SHIELD_AND_ARMOUR, BATTLE_TACTICS,
      OFTEN, DUTIFUL, SOMETIMES, STUBBORN, AMBITIOUS, NEVER, NERVOUS,
      SHOCK_STAFF, BLASTER, BODY_ARMOUR
    ),
    
    MILITARY_CIRCLES[] = { VOLUNTEER, TECH_RESERVE, SCOUT, VETERAN }
  ;
  
  final public static Background
    
    PERFORMER = new Background(
      "Performer", "aesthete_female_skin.gif", "aesthete_portrait.png",
      LOWER_CLASS, GUILD_AESTHETE,
      PRACTICED, MUSIC_AND_SONG, NOVICE, EROTICS, MASQUERADE,
      OFTEN, HANDSOME, RARELY, STOUT, SOMETIMES, EMPATHIC, INDULGENT,
      FINERY
    ) {
      final ImageAsset male_skin = costumeFor("aesthete_male_skin.gif") ;
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin ;
      }
    },
    
    FABRICATOR = new Background(
      "Fabricator", "citizen_skin.gif", null,
      LOWER_CLASS, GUILD_AESTHETE,
      PRACTICED, CHEMISTRY, HARD_LABOUR, NOVICE, GRAPHIC_DESIGN, HANDICRAFTS,
      SOMETIMES, STUBBORN, NERVOUS,
      OVERALLS
    ),
    
    ADVERTISER = new Background(
      "Advertiser", "citizen_skin.gif", null,
      MIDDLE_CLASS, GUILD_AESTHETE,
      EXPERT, GRAPHIC_DESIGN, SUASION,
      PRACTICED, COUNSEL, SOCIAL_HISTORY,
      NOVICE, ACCOUNTING, MUSIC_AND_SONG,
      RARELY, METICULOUS, STUBBORN, SOMETIMES, AMBITIOUS,
      OVERALLS
    ),
    
    AESTHETE = new Background(
      "Aesthete", "aesthete_male_skin.gif", null,
      UPPER_CLASS, GUILD_AESTHETE,
      EXPERT, GRAPHIC_DESIGN, PRACTICED, HANDICRAFTS, NOVICE, ANATOMY,
      RARELY, STUBBORN, IMPASSIVE, OFTEN, INDULGENT,
      FINERY
    ),
    
    AESTHETE_CIRCLES[] = { PERFORMER, FABRICATOR, ADVERTISER, AESTHETE }
  ;
  
  
  final public static Background
    //
    //  Scavengers represent the unemployed/homeless/penniless who want to
    //  leave your settlement, but can't.  Free Traders peddle small goods.
    SCAVENGER = new Background(
      "Scavenger", "native_skin.gif", null,
      SLAVE_CLASS, NOT_A_GUILD,
      NOVICE, STEALTH_AND_COVER, LEARNING, HANDICRAFTS,
      OFTEN, NERVOUS, ACQUISITIVE, RARELY, RELAXED
    ),
    FREE_TRADER = new Background(
      "Free Trader", "pyon_skin.gif", null,
      LOWER_CLASS, NOT_A_GUILD,
      PRACTICED, SUASION, NOVICE, HANDICRAFTS, ACCOUNTING, DOMESTICS,
      NATIVE_TABOO, COMMON_CUSTOM,
      SOMETIMES, OUTGOING, POSITIVE, RARELY, NERVOUS, AMBITIOUS
    ),
    //
    //  Mechanics and captains keep your dropships in working order.
    SHIP_MECHANIC = new Background(
      "Ship Mechanic", null, null,
      LOWER_CLASS, NOT_A_GUILD,
      PRACTICED, ASSEMBLY, HARD_LABOUR,
      NOVICE, FIELD_THEORY, SHIELD_AND_ARMOUR
    ),
    SHIP_CAPTAIN = new Background(
      "Ship Captain", null, null,
      MIDDLE_CLASS, NOT_A_GUILD,
      EXPERT, PILOTING, MARKSMANSHIP, PRACTICED, COMMAND, SUASION,
      NOVICE, ASTROGATION, BATTLE_TACTICS, COMMON_CUSTOM,
      STUN_PISTOL, PARTIAL_ARMOUR
    ),
    //
    //  These classes won't generally stay put, but might visit your settlement
    //  if the place needs their services.
    RUNNER = new Background(
      "Runner", "runner_skin.gif", "vendor_portrait.png",
      MIDDLE_CLASS, NOT_A_GUILD,
      EXPERT, PILOTING, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, SUASION, SURVEILLANCE, MASQUERADE,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, RARELY, METICULOUS,
      BLASTER, CAMOUFLAGE
    ),
    COMPANION = new Background(
      "Companion", "aesthete_female_skin.gif", "aesthete_portrait.png",
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, EROTICS, COUNSEL, SUASION, MASQUERADE, NOBLE_ETIQUETTE,
      PRACTICED, DOMESTICS, MUSIC_AND_SONG, COMMAND, HAND_TO_HAND,
      ALWAYS, HANDSOME, OFTEN, FEMININE, EMPATHIC, TALL, RARELY, STOUT,
      FINERY
    ) {
    final ImageAsset male_skin = costumeFor("aesthete_male_skin.gif") ;
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin ;
      }
    },
    
    OUTLAW_CIRCLES[] = {
        SCAVENGER, FREE_TRADER, SHIP_MECHANIC,
        SHIP_CAPTAIN, RUNNER, COMPANION
    } ;
  
  
  final public static Background
    //
    //  You'll always get a few of these in a given native village-
    GATHERER = new Background(
      "Gatherer", "native_skin.gif", null,
      LOWER_CLASS, NOT_A_GUILD,
      EXPERT, HANDICRAFTS, PRACTICED, DOMESTICS, CULTIVATION, HARD_LABOUR,
      NATIVE_TABOO, NOVICE, MASQUERADE,
      RARELY, RELAXED, OFTEN, OUTGOING
    ),
    HUNTER = new Background(
      "Hunter", "native_skin.gif", null,
      LOWER_CLASS, NOT_A_GUILD,
      EXPERT, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, XENOZOOLOGY, ATHLETICS,
      NOVICE, HAND_TO_HAND, HANDICRAFTS, MASQUERADE,
      SOMETIMES, NATURALIST,
      TOOTH_KNIFE, JAVELIN
    ),
    SHAMAN = new Background(
      "Shaman", "native_skin.gif", null,
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COUNSEL, PRACTICED, CULTIVATION,
      NOVICE, PHARMACY, ANATOMY, ANCIENT_LORE, MUSIC_AND_SONG,
      ALWAYS, TRADITIONAL, OFTEN, DUTIFUL, NATURALIST
    ),
    CHIEFTAIN = new Background(
      "Chieftain", "native_skin.gif", null,
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COMMAND, SUASION, MARKSMANSHIP,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS,
      RARELY, NERVOUS, OFTEN, TRADITIONAL,
      TOOTH_KNIFE, JAVELIN, SCRAP_GEAR
    ),
    
    NATIVE_CIRCLES[] = {
      GATHERER, HUNTER, SHAMAN, CHIEFTAIN
    },
    NATIVE_MALE_JOBS[]   = { HUNTER, CHIEFTAIN },
    NATIVE_FEMALE_JOBS[] = { GATHERER, SHAMAN  } ;
  
  
  final public static Background
    //
    //  Aristocratic titles are for the benefit of the player-character:
    KNIGHTED = new Background(
      "Knighted", "highborn_male_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS, SHIELD_AND_ARMOUR, COMMAND,
      NOBLE_ETIQUETTE, NOVICE, ACCOUNTING, ANCIENT_LORE, COMMON_CUSTOM,
      LEARNING, SUGGESTION, PREMONITION, PROJECTION,
      SOMETIMES, PSYONIC, OFTEN, TRADITIONAL, RARELY, NERVOUS
    ) {
      final ImageAsset female_skin = costumeFor("highborn_male_skin.gif") ;
      public String nameFor(Actor actor) {
        return actor.traits.male() ? "Knighted Lord" : "Knighted Lady" ;
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.male() ? costume : female_skin ;
      }
    },
    BARON = new Background(
      "Baron", "highborn_male_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      EXPERT, HAND_TO_HAND, BATTLE_TACTICS, SHIELD_AND_ARMOUR, COMMAND,
      NOBLE_ETIQUETTE, PRACTICED, ACCOUNTING, ANCIENT_LORE, COMMON_CUSTOM,
      NOVICE, SUGGESTION, PREMONITION, PROJECTION,
      OFTEN, PSYONIC, OFTEN, TRADITIONAL, RARELY, NERVOUS
    ) {
      final ImageAsset female_skin = costumeFor("highborn_male_skin.gif") ;
      public String nameFor(Actor actor) {
        return actor.traits.male() ? "Baron" : "Baroness" ;
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.male() ? costume : female_skin ;
      }
    },
    COUNT = new Background(
      "Count", "highborn_male_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      MASTER, HAND_TO_HAND, BATTLE_TACTICS, SHIELD_AND_ARMOUR, COMMAND,
      NOBLE_ETIQUETTE, EXPERT, ACCOUNTING, ANCIENT_LORE, COMMON_CUSTOM,
      PRACTICED, SUGGESTION, PREMONITION, PROJECTION,
      ALWAYS, PSYONIC, OFTEN, TRADITIONAL, RARELY, NERVOUS
    ) {
      final ImageAsset female_skin = costumeFor("highborn_male_skin.gif") ;
      public String nameFor(Actor actor) {
        return actor.traits.male() ? "Count" : "Countess" ;
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.male() ? costume : female_skin ;
      }
    },
    //  TODO:  Duke may also be called Prince in some systems.  Lord Solar
    //  applies to any ruler of an entire system, or Emperor/Empress to a ruler
    //  of the setting as a whole (a largely ceremonial position.)
    DUKE = null,
    RULING_POSITIONS[] = { KNIGHTED, COUNT, BARON, DUKE },
    //
    //  Your family, servants, bodyguards and captives-
    FIRST_CONSORT = new Background(
      "First Consort", "highborn_female_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      PRACTICED, COMMAND, SUASION, NOVICE, EROTICS, MASQUERADE, DOMESTICS,
      RARELY, IMPASSIVE, STUBBORN, OFTEN, AMBITIOUS, ACQUISITIVE,
      SOMETIMES, POSITIVE
    ) {
      final ImageAsset male_skin = costumeFor("highborn_female_skin.gif") ;
      public String nameFor(Actor actor) {
        final boolean male = actor.traits.male() ;
        final Background rank = actor.base().ruler().vocation() ;
        if (rank == KNIGHTED) return male ? "Lord Consort" : "Lady Consort" ;
        if (rank == COUNT) return male ? "Count Consort" : "Countess Consort" ;
        if (rank == BARON) return male ? "Baron Consort" : "Baroness Consort" ;
        if (rank == DUKE ) return male ? "Duke Consort"  : "Duchess Consort"  ;
        return name ;
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin ;
      }
    },
    MINISTER_FOR_ACCOUNTS = new Background(
      "Minister for Accounts", "vendor_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      EXPERT, ACCOUNTING, PRACTICED, SOCIAL_HISTORY, COUNSEL, SUASION
    ),
    WAR_MASTER = new Background(
      "War Master", "highborn_male_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      EXPERT, HAND_TO_HAND, SHIELD_AND_ARMOUR, BATTLE_TACTICS, PRACTICED,
      SURVEILLANCE, MARKSMANSHIP
    ),
    
    
    STEWARD = new Background(
      "Steward", "citizen_skin.gif", null,
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, DOMESTICS, PRACTICED, PHARMACY, ANATOMY, COUNSEL,
      GENE_CULTURE, NOVICE, NOBLE_ETIQUETTE,
      ALWAYS, DUTIFUL, OFTEN, TRADITIONAL, NEVER, DEFENSIVE
    ),

    //
    //  These positions are for the benefit of citizens elected at the Counsel
    //  Chamber, and politically have a similar degree of influence (Consuls,
    //  in principle, are on an equal footing with the Emperor/Empress.)
    PREFECT = new Background(
      "Prefect", "physician_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      PRACTICED, COUNSEL, SUASION, ACCOUNTING, COMMON_CUSTOM,
      NOVICE, NOBLE_ETIQUETTE, SOCIAL_HISTORY, BATTLE_TACTICS, COMMAND,
      OFTEN, OUTGOING, AMBITIOUS, SOMETIMES, ACQUISITIVE
    ),
    GOVERNOR = null,
    SENATOR  = null,
    CONSUL   = null,
    ELECTED_POSITIONS[] = { PREFECT, GOVERNOR, SENATOR, CONSUL },
    
    COURT_CIRCLES[] = {},
    HONOUR_GUARD = null,
    HOSTAGE      = null,
    //
    //  Ministers confer the benefits of a portion of their skills on the
    //  planet as a whole (including stuff off the main map.)
    MASTER_OF_ASSASSINS     = null,
    //WAR_MASTER              = null,
    CHIEF_ARTIFICER         = null,
    PLANETOLOGIST           = null,
    MINISTER_FOR_HEALTH     = null,
    //MINISTER_FOR_ACCOUNTS   = null,
    MINISTER_FOR_PROPAGANDA = null,
    //FIRST_CONSORT           = null,
    
    GOVERNMENT_CIRCLES[] = {}
    //
    //  TODO:  Do you need positions for leadership within the Strains?
  ;
  
  
  final public static int
    INTENSE_GRAVITY = -2,
    STRONG_GRAVITY  = -1,
    NORMAL_GRAVITY  =  0,
    MILD_GRAVITY    =  1,
    NOMINAL_GRAVITY =  2 ;
  
  final public static System
    //
    //  TODO:  List common names and minor houses.
    //         Introduce Calivor, Nordsei, Solipsus, Urym Hive & The Outer
    //         Sphere. (aliens and freeholds later.)
    PLANET_ASRA_NOVI = new System(
      "Asra Novi", "House Suhail",
      "Asra Novi is a heavily-terraformed 'desert oasis' world noted for it's "+
      "expertise in ecology and botanical science, together with polyamorous "+
      "traditions and luxury exports.",
      null, 0, 1, DESERT_BLOOD, MILD_GRAVITY,
      MAKES, SOMA, PLASTICS, FIXTURES, TRUE_SPICE,
      NEEDS, WATER, SERVICE_CONSORTS, DATALINKS,
      FREE_BIRTH,
      LEARNING, CULTIVATION, CHEMISTRY,
      OFTEN, ECOLOGIST_CIRCLES, SOMETIMES, COURT_CIRCLES, AESTHETE_CIRCLES
    ),
    PLANET_PAREM_V = new System(
      "Parem V", "House Procyon",
      "Parem V was one of the first-settled systems in the known quadrant, "+
      "and though dour and repressive, remains host to numerous machine-"+
      "cults and revered arcane relics.",
      null, 1, 1, WASTES_BLOOD, NORMAL_GRAVITY,
      MAKES, PARTS, DATALINKS, REPLICANTS,
      NEEDS, ATOMICS, FIXTURES, ARTIFACTS,
      DREGS_BIRTH, PYON_BIRTH,
      LEARNING, ASSEMBLY, ANCIENT_LORE,
      OFTEN, ARTIFICER_CIRCLES, SOMETIMES, COURT_CIRCLES,
      RARELY, ECOLOGIST_CIRCLES, AESTHETE_CIRCLES
    ),
    PLANET_HALIBAN = new System(
      "Haliban", "House Altair",
      "Noted for it's spartan regimen and stern justice, Haliban's early "+
      "defection to the Calivor Republic have earned it several foes- and a "+
      "crucial role in quadrant defence strategy.",
      null, 0, 0, FOREST_BLOOD, STRONG_GRAVITY,
      MAKES, CARBS, GREENS, ATOMICS,
      NEEDS, SERVICE_ARMAMENT, PARTS, MEDICINE,
      GELDER_BIRTH, FREE_BIRTH,
      LEARNING, MARKSMANSHIP, ANATOMY,
      OFTEN, MILITARY_CIRCLES, SOMETIMES, PHYSICIAN_CIRCLES,
      RARELY, VENDOR_CIRCLES
    ),
    PLANET_AXIS_NOVENA = new System(
      "Axis Novena", "House Taygeta",
      "Aided by it's low gravity and thin atmosphere, Axis Novena became the "+
      "centre of a large shipping industry and trade network- along with "+
      "rampant smuggling and black market research.",
      null, 1, 0, TUNDRA_BLOOD, NOMINAL_GRAVITY,
      MAKES, CIRCUITRY, MEDICINE, SERVICE_SHIPPING,
      NEEDS, GREENS, METALS, FUEL_RODS,
      DREGS_BIRTH, GELDER_BIRTH,
      LEARNING, FIELD_THEORY, STEALTH_AND_COVER,
      OFTEN, VENDOR_CIRCLES, OUTLAW_CIRCLES, SOMETIMES, ARTIFICER_CIRCLES
    ),
    //
    //  TODO:  ...These need more detail.
    PLANET_SOLIPSUS_VIER = new System(
      "Solipsus Vier", "House Fomalhaut",
      "Notable for it's peculiar blend of pastoral tradition and caste "+
      "eugenics, Solipsus Vier is ruled by insular scientific elites fixated "+
      "on mental and physical purity.",
      null, 2, 1, WASTES_BLOOD, NORMAL_GRAVITY,
      OFTEN, PHYSICIAN_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES, MILITARY_CIRCLES,
      RARELY, VENDOR_CIRCLES, NEVER, OUTLAW_CIRCLES
    ),
    PLANET_NORUSEI = new System(
      "Norusei", "House Rana",
      "Once an idyllic tropical planet-resort, Norusei has enjoyed something "+
      "of a renaissance following the devastation of the Machine Wars, "+
      "boasting a rich tourist trade and export of celebrity cult-idols.",
      null, 2, 0, FOREST_BLOOD, NORMAL_GRAVITY,
      OFTEN, AESTHETE_CIRCLES, SOMETIMES, ECOLOGIST_CIRCLES,
      RARELY, ARTIFICER_CIRCLES, MILITARY_CIRCLES
    ),
    
    PLANET_URYM_HIVE = new System(
      "Urym Hive", "House Algol (Minor)",
      "Chief factory-world of the Empire in its prime, today inescapable "+
      "poverty, desperate squalor and seething unrest render Urym Hive's "+
      "unnumbered billions governable in name only.",
      null, 0, 2, WASTES_BLOOD, INTENSE_GRAVITY
    ),
    PLANET_CALIVOR = new System(
      "Calivor", "House Regulus (Minor)",
      "Capital of the Republic whose meteoric rise to prominence saw a dozen "+
      "noble houses unseated in disgrace, to many Calivor remains a gleaming "+
      "beacon of opportunity.",
      null, 1, 2, TUNDRA_BLOOD, NORMAL_GRAVITY
    ),
    PLANET_THE_WEIRWORLD = new System(
      "The Weirworld", "House Ophiuchus (Exiled)",
      "Shrouded by dense nebulae and dark rumour, the Weirworld is reputedly "+
      "a hollow organic world, host to fleets of Strain vessels which raid "+
      "or colonise the quadrant periphery.",
      null, 2, 2, MUTATION, MILD_GRAVITY
    ),
    
    PLANET_DIAPSOR = new System(
      "Diapsor (Bloodstone/Presidium)", "No House (Freehold)",
      "Rendered virtually uninhabitable after the Machine Wars, the world "+
      "once known as Presidium was placed under Imperial Interdict for "+
      "seven centuries- a controversial edict, but ruthlessly enforced.  "+
      "Known today as Diapsor, the Bloodstone of the Gulf, intense "+
      "population pressures on several of the Homeworlds, together with "+
      "ecological improvement, have opened the doors to re-settlement.",
      null, -1, -1, MUTATION, NORMAL_GRAVITY
    ),
    
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN,
      PLANET_AXIS_NOVENA,// PLANET_SOLIPSUS_VIER, PLANET_NORUSEI,
      //PLANET_URYM_HIVE, PLANET_CALIVOR, PLANET_THE_OUTER_SPHERE
    } ;
  
  
  final public static Background
    ALL_BACKGROUNDS[] = (Background[]) all.toArray(Background.class) ;
  
  
  
  
  
  final public String name ;
  final protected ImageAsset costume, portrait ;
  
  final public int standing ;
  final public int guild ;
  final Table <Skill, Integer> baseSkills = new Table <Skill, Integer> () ;
  final Table <Trait, Float> traitChances = new Table <Trait, Float> () ;
  final List <Service> gear = new List <Service> () ;
  
  
  
  protected Background(
    String name, String costumeTex, String portraitTex,
    int standing, int guild, Object... args
  ) {
    this.name = name ;
    
    if (costumeTex == null) this.costume = null ;
    else this.costume = costumeFor(costumeTex);
    
    if (portraitTex == null) this.portrait = null ;
    else this.portrait = portraitFor(portraitTex);
    
    this.standing = standing ;
    this.guild = guild ;
    
    int level = 10 ;
    float chance = 0.5f ;
    for (int i = 0 ; i < args.length ; i++) {
      final Object o = args[i] ;
      if      (o instanceof Integer) { level  = (Integer) o ; }
      else if (o instanceof Float  ) { chance = (Float)   o ; }
      else if (o instanceof Skill) {
        baseSkills.put((Skill) o, level) ;
      }
      else if (o instanceof Trait) {
        traitChances.put((Trait) o, chance) ;
      }
      else if (o instanceof Service) {
        gear.add((Service) o) ;
      }
    }
    all.add(this) ;
  }
  
  
  public static Background loadConstant(Session s) throws Exception {
    return ALL_BACKGROUNDS[s.loadInt()] ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(ID) ;
  }
  
  
  
  /**  Data access-
    */
  public int skillLevel(Skill s) {
    final Integer level = baseSkills.get(s) ;
    return level == null ? 0 : (int) level ;
  }
  
  
  public List <Skill> skills() {
    final List <Skill> b = new List <Skill> () {
      protected float queuePriority(Skill r) { return r.traitID ; }
    } ;
    for (Skill s : baseSkills.keySet()) b.queueAdd(s) ;
    return b ;
  }
  
  
  public float traitChance(Trait t) {
    final Float chance = traitChances.get(t) ;
    return chance == null ? 0 : (float) chance ;
  }
  
  
  public List <Trait> traits() {
    final List <Trait> b = new List <Trait> () {
      protected float queuePriority(Trait r) { return r.traitID ; }
    } ;
    for (Trait t : traitChances.keySet()) b.queueAdd(t) ;
    return b ;
  }
  
  
  
  /**  Rendering and interface helper methods-
    */
  final static String COSTUME_DIR = "media/Actors/human/" ;
  
  
  private static ImageAsset costumeFor(String texName) {
    return ImageAsset.fromImage(COSTUME_DIR+texName, Background.class) ;
  }

  private static ImageAsset portraitFor(String texName) {
    return ImageAsset.fromImage(COSTUME_DIR+texName, Background.class) ;
  }
  
  
  public String toString() {
    return name ;
  }
  
  
  public String nameFor(Actor actor) {
    return name ;
  }
  
  
  public ImageAsset costumeFor(Actor actor) {
    return costume ;
  }
  
  
  public ImageAsset portraitFor(Actor actor) {
    return portrait ;
  }
}








