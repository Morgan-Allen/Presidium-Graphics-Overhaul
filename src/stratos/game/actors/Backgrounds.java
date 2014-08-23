/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.building.*;
import stratos.game.campaign.Sector;
import stratos.graphics.common.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public interface Backgrounds {
  
  
  final public static Float
    ALWAYS    =  1.0f,
    OFTEN     =  0.6f,
    SOMETIMES =  0.3f,
    RARELY    = -0.7f,
    NEVER     = -1.0f;
  final public static Integer
    LEARNING  = 0 ,
    NOVICE    = 5 ,
    PRACTICED = 10,
    EXPERT    = 15,
    MASTER    = 20;
  
  final public static int
    CLASS_NATIVE   = -1,
    CLASS_SLAVE    =  0,
    CLASS_VASSAL   =  1,
    CLASS_FREEMEN  =  2,
    CLASS_STRATOI  =  3,
    NOT_A_CLASS    = -2;
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
    NOT_A_GUILD     = -1;
  
  final public static float
    NUM_DAYS_PAY = 20,
    PAY_INTERVAL = World.STANDARD_DAY_LENGTH * 20;
  final public static int
    HIRE_COSTS[] = {
      150, 250, 500, 1000 //Represents PAY_INTERVAL days' salary.
    },
    DEFAULT_RELIEF = 100,
    DEFAULT_TAX_PERCENT = 50,
    DEFAULT_SURPLUS_PERCENT = 10,
    DEFAULT_RULER_STIPEND   = 2000;
  
  
  final public static Background
    MALE_BIRTH = new Background(
      Backgrounds.class,
      "Born Male", null, null, NOT_A_CLASS, NOT_A_GUILD,
      3, MUSCULAR, 1, MOTOR, SOMETIMES, TALL, NEVER, FEMININE
    ),
    FEMALE_BIRTH = new Background(
      Backgrounds.class,
      "Born Female", null, null, NOT_A_CLASS, NOT_A_GUILD,
      2, IMMUNE, 2, PERCEPT, RARELY, STOUT, ALWAYS, FEMININE
    ),
    //
    //  Natives can only be recruited locally, not from offworld.
    NATIVE_BIRTH = new Background(
      Backgrounds.class,
      "Born Native", "native_skin.gif", null, NOT_A_CLASS, NOT_A_GUILD,
      NOVICE, NATIVE_TABOO, LEARNING, HANDICRAFTS, MARKSMANSHIP, XENOZOOLOGY
    ),
    //
    //  The following are available to most actors as part of their careers-
    DREGS_BIRTH = new Background(
      Backgrounds.class,
      "Born Dreg", "artificer_skin.gif", null, CLASS_SLAVE, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, NATIVE_TABOO, HAND_TO_HAND, CHEMISTRY
    ),
    PYON_BIRTH = new Background(
      Backgrounds.class,
      "Born Pyon", "pyon_skin.gif", null, CLASS_VASSAL, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, HARD_LABOUR, DOMESTICS, ASSEMBLY
    ),
    FREE_BIRTH = new Background(
      Backgrounds.class,
      "Born Free", "citizen_skin.gif", null, CLASS_FREEMEN, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, SUASION, MARKSMANSHIP, PILOTING
    ),
    GELDER_BIRTH = new Background(
      Backgrounds.class,
      "Born Gelder", "vendor_skin.gif", null, CLASS_FREEMEN, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, NOBLE_ETIQUETTE, ACCOUNTING, COUNSEL
    ),
    //
    //  Highborn are not available as normally-generated citizens, only as
    //  visiting NPCs or members of your household.
    LANDER_BIRTH = new Background(
      Backgrounds.class,
      "Born Lander", "highborn_male_skin.gif", null, CLASS_STRATOI,
      NOVICE, NOBLE_ETIQUETTE, LEARNING, COMMON_CUSTOM, HAND_TO_HAND, ACCOUNTING
    ),
    HIGH_BIRTH = new Background(
      Backgrounds.class,
      "Born High", "highborn_male_skin.gif", null, CLASS_STRATOI,
      NOVICE, NOBLE_ETIQUETTE, LEARNING, COMMAND, HAND_TO_HAND, ANCIENT_LORE
    ),
    
    OPEN_CLASSES[] = { DREGS_BIRTH, PYON_BIRTH, FREE_BIRTH, GELDER_BIRTH };
  
  
  
  
  final public static Background
    
    EXCAVATOR = new Background(
      Backgrounds.class,
      "Excavator", "pyon_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_ARTIFICER,
      EXPERT, HARD_LABOUR, NOVICE, GEOPHYSICS, ASSEMBLY, LEARNING, ANCIENT_LORE,
      OFTEN, STUBBORN, RARELY, NERVOUS, HANDSOME,
      OVERALLS
    ),
    
    TECHNICIAN = new Background(
      Backgrounds.class,
      "Technician", "artificer_skin.gif", "artificer_portrait.png",
      CLASS_VASSAL, GUILD_ARTIFICER,
      PRACTICED, ASSEMBLY, HARD_LABOUR, NOVICE, FIELD_THEORY, CHEMISTRY,
      SOMETIMES, DUTIFUL, RARELY, RELAXED,
      OVERALLS
    ),
    
    CORE_TECHNICIAN = new Background(
      Backgrounds.class,
      "Core Technician", "citizen_skin.gif", "artificer_portrait.png",
      CLASS_FREEMEN, GUILD_ARTIFICER,
      EXPERT, FIELD_THEORY, PRACTICED, CHEMISTRY, ASSEMBLY,
      NOVICE, SHIELD_AND_ARMOUR,
      OFTEN, DUTIFUL, SOMETIMES, NERVOUS,
      SEALSUIT
    ),
    
    ARTIFICER = new Background(
      Backgrounds.class,
      "Artificer", "artificer_skin.gif", "artificer_portrait.png",
      CLASS_FREEMEN, GUILD_ARTIFICER,
      EXPERT, ASSEMBLY, PRACTICED, FIELD_THEORY, SHIELD_AND_ARMOUR,
      NOVICE, ANCIENT_LORE, CHEMISTRY,
      SOMETIMES, CURIOUS, RARELY, NATURALIST,
      OVERALLS
    ),
    
    ARTIFICER_CIRCLES[] = { EXCAVATOR, TECHNICIAN, CORE_TECHNICIAN, ARTIFICER }
 ;

  final public static Background
    
    MINDER = new Background(
      Backgrounds.class,
      "Minder", "citizen_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_PHYSICIAN,
      PRACTICED, DOMESTICS, SUASION, NOVICE, ANATOMY, PHARMACY, COUNSEL,
      OFTEN, EMPATHIC, SOMETIMES, STUBBORN,
      OVERALLS
    ),
    
    VATS_BREEDER = new Background(
      Backgrounds.class,
      "Vats Breeder", "citizen_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_PHYSICIAN,
      PRACTICED, GENE_CULTURE, PHARMACY, CHEMISTRY,
      RARELY, INDULGENT, RELAXED,
      OVERALLS
    ),
    
    ENFORCER = new Background(
      Backgrounds.class,
      "Enforcer", "enforcer_skin.gif", "enforcer_portrait.png",
      CLASS_FREEMEN, GUILD_PHYSICIAN,
      PRACTICED, MARKSMANSHIP, SUASION, COUNSEL, STEALTH_AND_COVER, NOVICE,
      HAND_TO_HAND, ANATOMY, PHARMACY, COMMAND, BATTLE_TACTICS,
      OFTEN, FEARLESS, STUBBORN, SOMETIMES, DUTIFUL, CURIOUS, CRUEL,
      BODY_ARMOUR, STUN_WAND
    ),
    
    //*
    ARCHIVE_SAVANT = new Background(
      Backgrounds.class, "Archive Savant", "citizen_skin.gif", null,
      CLASS_FREEMEN, GUILD_PHYSICIAN,
      EXPERT, ACCOUNTING, INSCRIPTION, PRACTICED, COUNSEL, ASSEMBLY,
      NOVICE, ANCIENT_LORE, LEGISLATION,
      ALWAYS, CURIOUS, SOMETIMES, NERVOUS, IMPASSIVE,
      OVERALLS
    ),
    //*/  //TODO:  Also Psychoanalyst?
    
    PHYSICIAN = new Background(
      Backgrounds.class,
      "Physician", "physician_skin.gif", "physician_portrait.png",
      CLASS_FREEMEN, GUILD_PHYSICIAN,
      EXPERT, ANATOMY, PHARMACY,
      PRACTICED, GENE_CULTURE, PSYCHOANALYSIS, COUNSEL, SUASION,
      OFTEN, CURIOUS, SOMETIMES, URBANE, IMPASSIVE, RARELY, INDULGENT,
      OVERALLS
    ),
    
    PHYSICIAN_CIRCLES[] = { MINDER, VATS_BREEDER, ENFORCER, PHYSICIAN }
 ;
  
  final public static Background
    
    CULTIVATOR = new Background(
      Backgrounds.class,
      "Cultivator", "pyon_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_ECOLOGIST,
      PRACTICED, CULTIVATION, HARD_LABOUR, NOVICE, DOMESTICS, ASSEMBLY,
      OFTEN, OUTGOING, SOMETIMES, NATURALIST, RARELY, AMBITIOUS,
      OVERALLS
    ),
    
    FORMER_ENGINEER = new Background(
      Backgrounds.class,
      "Former Engineer", "ecologist_skin.gif", "ecologist_portrait.png",
      CLASS_FREEMEN, GUILD_ECOLOGIST,
      PRACTICED, GEOPHYSICS, ASSEMBLY, HARD_LABOUR,
      NOVICE, GENE_CULTURE, SURVEILLANCE,
      RARELY, OUTGOING, OFTEN, POSITIVE,
      OVERALLS
    ),
    
    //  TODO:  Replace with Kommando, or merge into Ecologist.
    EXPLORER = new Background(
      Backgrounds.class,
      "Explorer", "ecologist_skin.gif", "ecologist_portrait.png",
      CLASS_FREEMEN, GUILD_ECOLOGIST,
      EXPERT, XENOZOOLOGY, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, NATIVE_TABOO,
      NOVICE, BATTLE_TACTICS, HAND_TO_HAND,
      RARELY, NERVOUS, RELAXED, OFTEN, NATURALIST,
      STUN_WAND, STEALTH_SUIT
    ),
    
    ECOLOGIST = new Background(
      Backgrounds.class,
      "Ecologist", "ecologist_skin.gif", "ecologist_portrait.png",
      CLASS_FREEMEN, GUILD_ECOLOGIST,
      EXPERT, CULTIVATION, PRACTICED, GENE_CULTURE, XENOZOOLOGY,
      PRACTICED, GEOPHYSICS, CHEMISTRY,
      ALWAYS, NATURALIST, SOMETIMES, EMPATHIC, CURIOUS,
      SEALSUIT
    ),
    
    ECOLOGIST_CIRCLES[] = {
      CULTIVATOR, FORMER_ENGINEER, EXPLORER, ECOLOGIST
    }
 ;
  
  final public static Background
    
    FAB_WORKER = new Background(
      Backgrounds.class,
      "Fab Worker", "pyon_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_MERCHANT,
      NOVICE, CHEMISTRY, PILOTING, ASSEMBLY, HARD_LABOUR,
      OFTEN, RELAXED, RARELY, AMBITIOUS,
      OVERALLS
    ),
    
    SOMA_VENDOR = new Background(
      Backgrounds.class,
      "Soma Vendor", "vendor_skin.gif", "vendor_portrait.png",
      CLASS_VASSAL, GUILD_MERCHANT,
      PRACTICED, COUNSEL, SUASION, NOVICE, DOMESTICS, CHEMISTRY,
      ACCOUNTING,
      SOMETIMES, ACQUISITIVE,
      OVERALLS
    ),
    
    STOCK_VENDOR = new Background(
      Backgrounds.class,
      "Stock Vendor", "vendor_skin.gif", "vendor_portrait.png",
      CLASS_VASSAL, GUILD_MERCHANT,
      PRACTICED, ACCOUNTING, DOMESTICS, NOVICE, SUASION, HARD_LABOUR,
      OVERALLS
    ),
    
    AUDITOR = new Background(
      Backgrounds.class,
      "Auditor", "vendor_skin.gif", "vendor_portrait.png",
      CLASS_FREEMEN, GUILD_MERCHANT,
      EXPERT, COUNSEL, ACCOUNTING, PRACTICED, COMMAND, ANCIENT_LORE,
      ALWAYS, STUBBORN, OFTEN, DUTIFUL,
      SOMETIMES, AMBITIOUS, IMPASSIVE, RARELY, INDULGENT,
      OVERALLS
    ),
    
    VENDOR_CIRCLES[] = { FAB_WORKER, SOMA_VENDOR, STOCK_VENDOR, AUDITOR }
 ;
  
  final public static Background
    
    VOLUNTEER = new Background(
      Backgrounds.class,
      "Volunteer", "militant_skin.gif", "militant_portrait.png",
      CLASS_VASSAL, GUILD_MILITANT,
      PRACTICED, HAND_TO_HAND, MARKSMANSHIP,
      NOVICE, SURVEILLANCE, ASSEMBLY, HARD_LABOUR, SHIELD_AND_ARMOUR,
      SOMETIMES, DUTIFUL, DEFENSIVE, RARELY, NERVOUS, FEMININE,
      BLASTER, BODY_ARMOUR
    ),
    
    //  TODO:  Restore traits here.
    AIR_CORPS = new Background(
      Backgrounds.class,
      "Air Corps", "artificer_skin.gif", "militant_portrait.png",
      CLASS_FREEMEN, GUILD_MILITANT,
      PRACTICED, MARKSMANSHIP, PILOTING, SURVEILLANCE, STEALTH_AND_COVER,
      NOVICE, HARD_LABOUR, ASSEMBLY,
      BLASTER, SEALSUIT
    ),
    
    //  TODO:  Draw up a different set of art assets for this guy.
    MECH_LEGION = new Background(
      Backgrounds.class,
      "Mech Legion", "militant_skin.gif", "militant_portrait.png",
      CLASS_FREEMEN, GUILD_MILITANT,
      EXPERT, HAND_TO_HAND, FORMATION_COMBAT, SHIELD_AND_ARMOUR,
      PRACTICED, COMMAND, NOVICE, BATTLE_TACTICS, MARKSMANSHIP,
      BLASTER, POWER_ARMOUR
    ),
    
    VETERAN = new Background(
      Backgrounds.class,
      "Veteran", "militant_skin.gif", "militant_portrait.png",
      CLASS_FREEMEN, GUILD_MILITANT,
      EXPERT, HAND_TO_HAND, MARKSMANSHIP, PRACTICED, SURVEILLANCE,
      FORMATION_COMBAT, COMMAND, SHIELD_AND_ARMOUR, BATTLE_TACTICS,
      OFTEN, DUTIFUL, SOMETIMES, STUBBORN, AMBITIOUS, NEVER, NERVOUS,
      BLASTER, BODY_ARMOUR
    ),
    
    MILITARY_CIRCLES[] = { VOLUNTEER, AIR_CORPS, MECH_LEGION, VETERAN }
 ;
  
  final public static Background
    
    PERFORMER = new Background(
      Backgrounds.class,
      "Performer", "aesthete_female_skin.gif", "aesthete_portrait.png",
      CLASS_VASSAL, GUILD_AESTHETE,
      PRACTICED, MUSIC_AND_SONG, NOVICE, EROTICS, MASQUERADE,
      OFTEN, HANDSOME, RARELY, STOUT, SOMETIMES, EMPATHIC, INDULGENT,
      FINERY
    ) {
      final ImageAsset male_skin = costumeFor("aesthete_male_skin.gif");
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin;
      }
    },
    
    FABRICATOR = new Background(
      Backgrounds.class,
      "Fabricator", "citizen_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_AESTHETE,
      PRACTICED, CHEMISTRY, HARD_LABOUR, NOVICE, GRAPHIC_DESIGN, HANDICRAFTS,
      SOMETIMES, STUBBORN, NERVOUS,
      OVERALLS
    ),
    
    ADVERTISER = new Background(
      Backgrounds.class,
      "Advertiser", "citizen_skin.gif", "pyon_portrait.png",
      CLASS_FREEMEN, GUILD_AESTHETE,
      EXPERT, GRAPHIC_DESIGN, SUASION,
      PRACTICED, COUNSEL, SOCIAL_HISTORY,
      NOVICE, ACCOUNTING, MUSIC_AND_SONG,
      RARELY, URBANE, STUBBORN, SOMETIMES, AMBITIOUS,
      OVERALLS
    ),
    
    AESTHETE = new Background(
      Backgrounds.class,
      "Aesthete", "aesthete_male_skin.gif", "aesthete_portrait.png",
      CLASS_FREEMEN, GUILD_AESTHETE,
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
      Backgrounds.class,
      "Scavenger", "native_skin.gif", null,
      CLASS_VASSAL, NOT_A_GUILD,
      NOVICE, STEALTH_AND_COVER, LEARNING, HANDICRAFTS,
      OFTEN, NERVOUS, ACQUISITIVE, RARELY, RELAXED
    ),
    FREE_TRADER = new Background(
      Backgrounds.class,
      "Free Trader", "pyon_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, NOT_A_GUILD,
      PRACTICED, SUASION, NOVICE, HANDICRAFTS, ACCOUNTING, DOMESTICS,
      NATIVE_TABOO, COMMON_CUSTOM,
      SOMETIMES, OUTGOING, POSITIVE, RARELY, NERVOUS, AMBITIOUS
    ),
    //
    //  Mechanics and captains keep your dropships in working order.
    SHIP_MECHANIC = new Background(
      Backgrounds.class,
      "Ship Mechanic", "artificer_skin.gif", "artificer_portrait.png",
      CLASS_VASSAL, NOT_A_GUILD,
      PRACTICED, ASSEMBLY, HARD_LABOUR,
      NOVICE, FIELD_THEORY, SHIELD_AND_ARMOUR
    ),
    SHIP_CAPTAIN = new Background(
      Backgrounds.class,
      "Ship Captain", null, "pyon_portrait.png",
      CLASS_FREEMEN, NOT_A_GUILD,
      EXPERT, PILOTING, MARKSMANSHIP, PRACTICED, COMMAND, SUASION,
      NOVICE, ASTROGATION, BATTLE_TACTICS, COMMON_CUSTOM,
      BLASTER, BELT_AND_BRACER
    ),
    //
    //  These classes won't generally stay put, but might visit your settlement
    //  if the place needs their services.
    RUNNER = new Background(
      Backgrounds.class,
      "Runner", "runner_skin.gif", "vendor_portrait.png",
      CLASS_FREEMEN, NOT_A_GUILD,
      EXPERT, PILOTING, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, SUASION, SURVEILLANCE, MASQUERADE,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, RARELY, URBANE,
      BLASTER, STEALTH_SUIT
    ),
    COMPANION = new Background(
      Backgrounds.class,
      "Companion", "aesthete_female_skin.gif", "aesthete_portrait.png",
      CLASS_FREEMEN, NOT_A_GUILD,
      EXPERT, EROTICS, COUNSEL, SUASION, MASQUERADE, NOBLE_ETIQUETTE,
      PRACTICED, DOMESTICS, MUSIC_AND_SONG, COMMAND, HAND_TO_HAND,
      ALWAYS, HANDSOME, OFTEN, FEMININE, EMPATHIC, TALL, RARELY, STOUT,
      FINERY
    ) {
    final ImageAsset male_skin = costumeFor("aesthete_male_skin.gif");
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin;
      }
    },
    
    OUTLAW_CIRCLES[] = {
        SCAVENGER, FREE_TRADER, SHIP_MECHANIC,
        SHIP_CAPTAIN, RUNNER, COMPANION
    };
  
  
  final public static Background
    //
    //  You'll always get a few of these in a given native village-
    GATHERER = new Background(
      Backgrounds.class,
      "Gatherer", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, HANDICRAFTS, PRACTICED, DOMESTICS, CULTIVATION, HARD_LABOUR,
      NATIVE_TABOO, NOVICE, MASQUERADE,
      RARELY, RELAXED, OFTEN, OUTGOING
    ),
    HUNTER = new Background(
      Backgrounds.class,
      "Hunter", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, XENOZOOLOGY, ATHLETICS,
      NOVICE, HAND_TO_HAND, HANDICRAFTS, MASQUERADE,
      SOMETIMES, NATURALIST,
      HUNTING_LANCE, SCRAP_GEAR
    ),
    SHAMAN = new Background(
      Backgrounds.class,
      "Shaman", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COUNSEL, PRACTICED, CULTIVATION,
      NOVICE, PHARMACY, ANATOMY, ANCIENT_LORE, MUSIC_AND_SONG,
      ALWAYS, TRADITIONAL, OFTEN, DUTIFUL, NATURALIST
    ),
    CHIEFTAIN = new Background(
      Backgrounds.class,
      "Chieftain", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COMMAND, SUASION, MARKSMANSHIP,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS,
      RARELY, NERVOUS, OFTEN, TRADITIONAL,
      HUNTING_LANCE, SCRAP_GEAR
    ),
    //  TODO:  Restore Cargo Cultist and Mutant Pseer.
    CARGO_CULTIST = null,
    MUTANT_PSEER  = null,
    
    NATIVE_CIRCLES[] = {
      GATHERER, HUNTER, SHAMAN, CHIEFTAIN
    },
    NATIVE_MALE_JOBS[]   = { HUNTER, CHIEFTAIN },
    NATIVE_FEMALE_JOBS[] = { GATHERER, SHAMAN  };
  
  
  final public static Background
    //
    //  Aristocratic titles are for the benefit of the player-character:
    KNIGHTED = new Background(
      Backgrounds.class,
      "Knighted", "highborn_male_skin.gif", "highborn_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS, SHIELD_AND_ARMOUR, COMMAND,
      NOBLE_ETIQUETTE, NOVICE, ACCOUNTING, ANCIENT_LORE, COMMON_CUSTOM,
      LEARNING, SUGGESTION, PREMONITION, PROJECTION,
      SOMETIMES, PSYONIC, OFTEN, TRADITIONAL, RARELY, NERVOUS
    ) {
      final ImageAsset female_skin = costumeFor("highborn_male_skin.gif");
      public String nameFor(Actor actor) {
        return actor.traits.male() ? "Knighted" : "Knighted";
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.male() ? costume : female_skin;
      }
    },
    BARON = new Background(
      Backgrounds.class,
      "Baron", "highborn_male_skin.gif", "highborn_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      EXPERT, HAND_TO_HAND, BATTLE_TACTICS, SHIELD_AND_ARMOUR, COMMAND,
      NOBLE_ETIQUETTE, PRACTICED, ACCOUNTING, ANCIENT_LORE, COMMON_CUSTOM,
      NOVICE, SUGGESTION, PREMONITION, PROJECTION,
      OFTEN, PSYONIC, OFTEN, TRADITIONAL, RARELY, NERVOUS
    ) {
      final ImageAsset female_skin = costumeFor("highborn_male_skin.gif");
      public String nameFor(Actor actor) {
        return actor.traits.male() ? "Baron" : "Baroness";
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.male() ? costume : female_skin;
      }
    },
    COUNT = new Background(
      Backgrounds.class,
      "Count", "highborn_male_skin.gif", "highborn_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      MASTER, HAND_TO_HAND, BATTLE_TACTICS, SHIELD_AND_ARMOUR, COMMAND,
      NOBLE_ETIQUETTE, EXPERT, ACCOUNTING, ANCIENT_LORE, COMMON_CUSTOM,
      PRACTICED, SUGGESTION, PREMONITION, PROJECTION,
      ALWAYS, PSYONIC, OFTEN, TRADITIONAL, RARELY, NERVOUS
    ) {
      final ImageAsset female_skin = costumeFor("highborn_male_skin.gif");
      public String nameFor(Actor actor) {
        return actor.traits.male() ? "Count" : "Countess";
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.male() ? costume : female_skin;
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
      Backgrounds.class,
      "First Consort", "highborn_female_skin.gif", "aesthete_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      PRACTICED, COMMAND, SUASION, NOVICE, EROTICS, MASQUERADE, DOMESTICS,
      RARELY, IMPASSIVE, STUBBORN, OFTEN, AMBITIOUS, ACQUISITIVE,
      SOMETIMES, POSITIVE
    ) {
      final ImageAsset male_skin = costumeFor("highborn_female_skin.gif");
      public String nameFor(Actor actor) {
        final boolean male = actor.traits.male();
        final Background rank = actor.base().ruler().vocation();
        if (rank == KNIGHTED) return male ? "Lord Consort" : "Lady Consort";
        if (rank == COUNT) return male ? "Count Consort" : "Countess Consort";
        if (rank == BARON) return male ? "Baron Consort" : "Baroness Consort";
        if (rank == DUKE ) return male ? "Duke Consort"  : "Duchess Consort" ;
        return name;
      }
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin;
      }
      public ImageAsset portraitFor(Actor actor) {
        return actor.traits.female() ? portrait : null;
      }
    },
    MINISTER_FOR_ACCOUNTS = new Background(
      Backgrounds.class, "Minister for Accounts", "vendor_skin.gif", null,
      CLASS_STRATOI, NOT_A_GUILD,
      EXPERT, ACCOUNTING, PRACTICED, SOCIAL_HISTORY, COUNSEL, SUASION
    ),
    WAR_MASTER = new Background(
      Backgrounds.class, "War Master", "highborn_male_skin.gif", null,
      CLASS_STRATOI, NOT_A_GUILD,
      EXPERT, HAND_TO_HAND, SHIELD_AND_ARMOUR, BATTLE_TACTICS, PRACTICED,
      SURVEILLANCE, MARKSMANSHIP
    ),
    
    
    STEWARD = new Background(
      Backgrounds.class, "Steward", "citizen_skin.gif", null,
      CLASS_FREEMEN, NOT_A_GUILD,
      EXPERT, DOMESTICS, PRACTICED, PHARMACY, ANATOMY, COUNSEL,
      GENE_CULTURE, NOVICE, NOBLE_ETIQUETTE,
      ALWAYS, DUTIFUL, OFTEN, TRADITIONAL, NEVER, DEFENSIVE
    ),
    
    HONOUR_GUARD = null,
    CAPTIVE      = null,
    COURT_CIRCLES[] = {},

    //
    //  These positions are for the benefit of citizens elected at the Counsel
    //  Chamber, and politically have a similar degree of influence (Consuls,
    //  in principle, are on an equal footing with the Emperor/Empress.)
    PREFECT = new Background(
      Backgrounds.class, "Prefect", "physician_skin.gif", null,
      CLASS_STRATOI, NOT_A_GUILD,
      PRACTICED, COUNSEL, SUASION, ACCOUNTING, COMMON_CUSTOM,
      NOVICE, NOBLE_ETIQUETTE, SOCIAL_HISTORY, BATTLE_TACTICS, COMMAND,
      OFTEN, OUTGOING, AMBITIOUS, SOMETIMES, ACQUISITIVE
    ),
    GOVERNOR = null,
    SENATOR  = null,
    CONSUL   = null,
    ELECTED_POSITIONS[] = { PREFECT, GOVERNOR, SENATOR, CONSUL },
    //
    //  Ministers confer the benefits of a portion of their skills on the
    //  planet as a whole (including stuff off the main map.)
    MASTER_OF_ASSASSINS     = null,
    //WAR_MASTER            = null,
    CHIEF_ARTIFICER         = null,
    PLANETOLOGIST           = null,
    MINISTER_FOR_HEALTH     = null,
    //MINISTER_FOR_ACCOUNTS = null,
    MINISTER_FOR_PROPAGANDA = null,
    //FIRST_CONSORT         = null,
    
    GOVERNMENT_CIRCLES[] = {},
    //
    //  TODO:  These are appointed-representatives for each of the major
    //  metahuman strain populations.
    STRAIN_AGENT = null,
    MSSID_REPRESENTATIVE = null,  //metahuman-stable-strain-ID program
    
    STRAIN_CIRCLES[] = { STRAIN_AGENT, MSSID_REPRESENTATIVE }
  ;
}









