/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Devices.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.craft.Outfits.*;


//  TODO:  Allow backgrounds to 'extend' or 'develop' eachother.

public interface Backgrounds {
  
  
  final public static Float
    ALWAYS    =  1.0f,
    OFTEN     =  0.6f,
    SOMETIMES =  0.3f,
    RARELY    = -0.7f,
    NEVER     = -1.0f;
  final public static Integer
    NOVICE    = 0 ,
    LEARNING  = 5 ,
    PRACTICED = 10,
    EXPERT    = 15,
    MASTER    = 20;
  final public static int
    CLASS_NATIVE    = -1,
    CLASS_SLAVE     =  0,
    CLASS_VASSAL    =  1,
    CLASS_AGENT     =  2,
    CLASS_STRATOI   =  3,
    NOT_A_CLASS     = -2;
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
    NUM_DAYS_PAY      = 20 / GameSettings.SPENDING_MULT,
    PAY_INTERVAL      = Stage.STANDARD_DAY_LENGTH * NUM_DAYS_PAY,
    MIN_DAILY_EXPENSE = 100 / NUM_DAYS_PAY,
    BIG_DAILY_EXPENSE = 400 / NUM_DAYS_PAY;
  final public static int
    HIRE_COSTS[] = {
      125, 250, 500, 1000 //Represents (NUM_DAYS_PAY) salary.
    },
    DEFAULT_TAX_PERCENTS[] = {
      45, 55, 65, 75
    },
    HOUSE_LEVEL_TAX_BONUS[]  = { 0, 25, 50, 75, 100 },
    DEFAULT_EMBEZZLE_PERCENT = 30  ,
    DEFAULT_SURPLUS_PERCENT  = 10  ,
    DEFAULT_RULER_STIPEND    = 2000;
  
  
  final public static Background
    BORN_MALE = new Background(
      Backgrounds.class,
      "Born Male", "", null, null, NOT_A_CLASS, NOT_A_GUILD,
      3, MUSCULAR, 1, MOTOR, SOMETIMES, TALL,
      RARELY, FEMININE, ALWAYS, GENDER_MALE, NEVER, GENDER_FEMALE
    ),
    BORN_FEMALE = new Background(
      Backgrounds.class,
      "Born Female", "", null, null, NOT_A_CLASS, NOT_A_GUILD,
      2, IMMUNE, 2, PERCEPT, RARELY, STOUT,
      OFTEN, FEMININE, NEVER, GENDER_MALE, ALWAYS, GENDER_FEMALE
    ),
    //
    //  Natives can only be recruited locally, not from offworld.
    BORN_NATIVE = new Background(
      Backgrounds.class,
      "Native Birth", "", "native_skin.gif", null, NOT_A_CLASS, NOT_A_GUILD,
      LEARNING, NATIVE_TABOO, NOVICE, HANDICRAFTS, MARKSMANSHIP, XENOZOOLOGY
    ),
    //
    //  The following are available to most actors as part of their careers-
    BORN_DREGS = new Background(
      Backgrounds.class,
      "Dreg Birth", "", "artificer_skin.gif", null, CLASS_SLAVE, NOT_A_GUILD,
      LEARNING, COMMON_CUSTOM, NOVICE, NATIVE_TABOO, HAND_TO_HAND, CHEMISTRY
    ),
    BORN_PYON = new Background(
      Backgrounds.class,
      "Pyon Birth", "", "pyon_skin.gif", null, CLASS_VASSAL, NOT_A_GUILD,
      LEARNING, COMMON_CUSTOM, NOVICE, HARD_LABOUR, ASSEMBLY
    ),
    BORN_FREE = new Background(
      Backgrounds.class,
      "Free Birth", "", "citizen_skin.gif", null, CLASS_AGENT, NOT_A_GUILD,
      LEARNING, COMMON_CUSTOM, NOVICE, SUASION, MARKSMANSHIP, PILOTING
    ),
    BORN_GELDER = new Background(
      Backgrounds.class,
      "Gelder Birth", "", "vendor_skin.gif", null, CLASS_AGENT, NOT_A_GUILD,
      LEARNING, COMMON_CUSTOM, NOVICE, ETIQUETTE, ACCOUNTING, COUNSEL
    ),
    //
    //  Highborn are not available as normally-generated citizens, only as
    //  visiting NPCs or members of your household.
    BORN_LANDER = new Background(
      Backgrounds.class,
      "Lander Birth", "", "highborn_male_skin.gif", null, CLASS_STRATOI,
      LEARNING, ETIQUETTE,
      NOVICE, COMMON_CUSTOM, HAND_TO_HAND, ACCOUNTING
    ),
    BORN_HIGH = new Background(
      Backgrounds.class,
      "High Birth", "", "highborn_male_skin.gif", null, CLASS_STRATOI,
      LEARNING, ETIQUETTE, NOVICE, COMMAND, HAND_TO_HAND, BATTLE_TACTICS
    ),
    
    OPEN_CLASSES [] = { BORN_DREGS, BORN_PYON, BORN_FREE, BORN_GELDER  },
    RULER_CLASSES[] = { BORN_FREE, BORN_GELDER, BORN_LANDER, BORN_HIGH };
  
  
  
  
  final public static Background
    
    EXCAVATOR = new Background(
      Backgrounds.class,
      "Excavator", "",
      "pyon_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_ARTIFICER,
      EXPERT, HARD_LABOUR, LEARNING, ASSEMBLY,
      OFTEN, PERSISTENT, RARELY, NERVOUS, HANDSOME,
      LIFTER_FRAME
    ),
    
    TECHNICIAN = new Background(
      Backgrounds.class,
      "Technician",
      "Technicians are trained to operate and perform routine maintenance on "+
      "base machinery and aid in construction projects.",
      "artificer_skin.gif", "artificer_portrait.png",
      CLASS_VASSAL, GUILD_ARTIFICER,
      PRACTICED, ASSEMBLY, HARD_LABOUR, LEARNING, FIELD_THEORY, CHEMISTRY,
      SOMETIMES, PERSISTENT, RARELY, RELAXED,
      LIFTER_FRAME
    ),
    
    CORE_TECHNICIAN = new Background(
      Backgrounds.class,
      "Core Technician", "", "citizen_skin.gif", "artificer_portrait.png",
      CLASS_AGENT, GUILD_ARTIFICER,
      EXPERT, FIELD_THEORY, PRACTICED, CHEMISTRY, ASSEMBLY,
      OFTEN, PERSISTENT, SOMETIMES, NERVOUS,
      LIFTER_FRAME, MANIPULATOR
    ),
    
    ENGINEER = new Background(
      Backgrounds.class,
      "Engineer",
      "Engineers are highly educated as physicists and artificers, and can "+
      "tackle commissions reliant on dangerous or arcane technologies.",
      "artificer_skin.gif", "artificer_portrait.png",
      CLASS_AGENT, GUILD_ARTIFICER,
      EXPERT, ASSEMBLY, PRACTICED, FIELD_THEORY,
      LEARNING, HAND_TO_HAND, CHEMISTRY,
      SOMETIMES, CURIOUS, RARELY, RUGGED,
      LIFTER_FRAME, MANIPULATOR
    ),
    
    ARTIFICER_CIRCLES[] = { EXCAVATOR, TECHNICIAN, CORE_TECHNICIAN, ENGINEER }
  ;

  final public static Background
    
    MINDER = new Background(
      Backgrounds.class,
      "Minder",
      "Minders are essential to tending the wounded and seeing to aspects of "+
      "diet and sanitary needs.",
      "citizen_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_PHYSICIAN,
      PRACTICED, HARD_LABOUR, SUASION, LEARNING, ANATOMY, BIOLOGY, COUNSEL,
      OFTEN, EMPATHIC, SOMETIMES, PERSISTENT,
      OVERALLS, MEDICINE
    ),
    
    VATS_BREEDER = new Background(
      Backgrounds.class,
      "Vats Breeder",
      "Vat Breeders supervise the cultivation and harvesting involved in "+
      "biochemical industries.",
      "citizen_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_PHYSICIAN,
      PRACTICED, BIOLOGY, CHEMISTRY,
      RARELY, RELAXED, RELAXED,
      SEALSUIT
    ),
    
    SAVANT = new Background(
      Backgrounds.class,
      "Savant", "", "citizen_skin.gif", null,
      CLASS_AGENT, GUILD_PHYSICIAN,
      EXPERT, ACCOUNTING, LOGIC, PRACTICED, COUNSEL, LEARNING, ASSEMBLY,
      ALWAYS, CURIOUS, SOMETIMES, NERVOUS, CALM,
      OVERALLS
    ),
    
    PHYSICIAN = new Background(
      Backgrounds.class,
      "Physician",
      "Physicians undergo extensive education in every aspect of human "+
      "metabolism and anatomy, and can tailor their treatments to any "+
      "patient's idiosyncracies.",
      "physician_skin.gif", "physician_portrait.png",
      CLASS_AGENT, GUILD_PHYSICIAN,
      EXPERT, ANATOMY, BIOLOGY,
      PRACTICED, LOGIC, COUNSEL, SUASION,
      OFTEN, CURIOUS, SOMETIMES, METICULOUS, CALM, RARELY, RELAXED,
      OVERALLS, MEDICINE, BIOCORDER
    ),
    
    PHYSICIAN_CIRCLES[] = { MINDER, VATS_BREEDER, SAVANT, PHYSICIAN }
 ;
  
  final public static Background
    
    CULTIVATOR = new Background(
      Backgrounds.class,
      "Cultivator",
      "Cultivators plant and reap harvests, maintain farm equipment, and "+
      "perform other agricultural chores.",
      "pyon_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_ECOLOGIST,
      PRACTICED, CULTIVATION, HARD_LABOUR, LEARNING, ASSEMBLY,
      OFTEN, RUGGED, SOMETIMES, EMPATHIC, RARELY, AMBITIOUS,
      OVERALLS
    ),
    
    ECOLOGIST = new Background(
      Backgrounds.class,
      "Ecologist", 
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification capable of adapting species to local climate conditions.",
      "ecologist_skin.gif", "ecologist_portrait.png",
      CLASS_AGENT, GUILD_ECOLOGIST,
      EXPERT, MARKSMANSHIP, XENOZOOLOGY,
      PRACTICED, BIOLOGY, EVASION, ATHLETICS,
      LEARNING, CHEMISTRY,
      ALWAYS, RUGGED, SOMETIMES, IMPULSIVE, CURIOUS,
      STUN_WAND, SEALSUIT
    ),
    
    ECOLOGIST_CIRCLES[] = {
      CULTIVATOR, ECOLOGIST
    }
 ;
  
  final public static Background
    
    SUPPLY_CORPS = new Background(
      Backgrounds.class,
      "Supply Corps",
      "Your Supply Corps are responsible for transport and deliveries within "+
      "your settlement, particularly of raw materials and bulk goods.",
      "pyon_skin.gif", "pyon_portrait.png",
      CLASS_VASSAL, GUILD_MERCHANT,
      LEARNING, CHEMISTRY, PILOTING, ASSEMBLY, HARD_LABOUR,
      OFTEN, RELAXED, RARELY, AMBITIOUS,
      OVERALLS
    ),
    
    SOMA_CHEF = new Background(
      Backgrounds.class,
      "Soma Chef", "", "vendor_skin.gif", "vendor_portrait.png",
      CLASS_VASSAL, GUILD_MERCHANT,
      PRACTICED, COUNSEL, SUASION, LEARNING, HARD_LABOUR, CHEMISTRY,
      ACCOUNTING,
      SOMETIMES, OUTGOING, RELAXED,
      OVERALLS
    ),
    
    STOCK_VENDOR = new Background(
      Backgrounds.class,
      "Stock Vendor", "", "vendor_skin.gif", "vendor_portrait.png",
      CLASS_VASSAL, GUILD_MERCHANT,
      PRACTICED, ACCOUNTING, TRUTH_SENSE, LEARNING, SUASION, HARD_LABOUR,
      OVERALLS
    ),
    
    AUDITOR = new Background(
      Backgrounds.class,
      "Auditor", "", "vendor_skin.gif", "vendor_portrait.png",
      CLASS_AGENT, GUILD_MERCHANT,
      EXPERT, COUNSEL, ACCOUNTING, PRACTICED, COMMAND, LOGIC,
      ALWAYS, PERSISTENT, SOMETIMES, LOYAL, AMBITIOUS, CALM,
      OVERALLS
    ),
    
    VENDOR_CIRCLES[] = { SUPPLY_CORPS, SOMA_CHEF, STOCK_VENDOR, AUDITOR }
 ;
  
  final public static Background
    
    VOLUNTEER = new Background(
      Backgrounds.class,
      "Volunteer",
      "Dedicated in defence of their homes, a volunteer militia makes up the "+
      "bulk of your domestic forces.",
      "militant_skin.gif", "militant_portrait.png",
      CLASS_VASSAL, GUILD_MILITANT,
      PRACTICED, HAND_TO_HAND, MARKSMANSHIP,
      LEARNING, SURVEILLANCE, ASSEMBLY, HARD_LABOUR,
      SOMETIMES, LOYAL, DEFENSIVE, RARELY, NERVOUS,
      BLASTER, BODY_ARMOUR
    ),
    
    TROOPER = new Background(
      Backgrounds.class,
      "Trooper",
      "Seasoned professional soldiers, Troopers provide the backbone of your "+
      "officer corps and command structure.",
      "militant_skin.gif", "militant_portrait.png",
      CLASS_AGENT, GUILD_MILITANT,
      EXPERT, HAND_TO_HAND, MARKSMANSHIP, PRACTICED, SURVEILLANCE,
      BATTLE_TACTICS, COMMAND,
      OFTEN, LOYAL, SOMETIMES, TRADITIONAL, AMBITIOUS, NEVER, NERVOUS,
      HALBERD_GUN, BODY_ARMOUR
    ),
    
    ENFORCER = new Background(
      Backgrounds.class,
      "Enforcer",
      "",
      "enforcer_skin.gif", "enforcer_portrait.png",
      CLASS_AGENT, GUILD_MILITANT,
      PRACTICED, MARKSMANSHIP, SUASION, COMMAND, EVASION, LEARNING,
      HAND_TO_HAND, ANATOMY, COUNSEL, BATTLE_TACTICS,
      OFTEN, PERSISTENT, SOMETIMES, FEARLESS, CURIOUS, AMBITIOUS,
      BODY_ARMOUR, STUN_WAND
    ),
    
    MILITARY_CIRCLES[] = {
      VOLUNTEER, TROOPER, ENFORCER,
    }
 ;
  
  final public static Background
    
    PERFORMER = new Background(
      Backgrounds.class,
      "Performer", "", "aesthete_female_skin.gif", "aesthete_portrait.png",
      CLASS_VASSAL, GUILD_AESTHETE,
      PRACTICED, MUSIC_AND_SONG, LEARNING, EROTICS, SUASION,
      OFTEN, HANDSOME, RARELY, STOUT, SOMETIMES, RELAXED, IMPULSIVE,
      FINERY
    ) {
      //  TODO:  Develop some specialised sub-method for this...
      final ImageAsset
        male_skin     = costumeFor ("aesthete_male_skin.gif"    ),
        male_portrait = portraitFor("aesthete_male_portrait.png");
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin;
      }
      public ImageAsset portraitFor(Actor actor) {
        return actor.traits.female() ? portrait : male_portrait;
      }
    },
    
    COMPANION = new Background(
      Backgrounds.class,
      "Companion", "", "aesthete_female_skin.gif", "aesthete_portrait.png",
      CLASS_AGENT, GUILD_AESTHETE,
      EXPERT, EROTICS, COUNSEL, SUASION, ETIQUETTE,
      PRACTICED, MUSIC_AND_SONG, COMMAND, HAND_TO_HAND,
      ALWAYS, HANDSOME, OFTEN, EMPATHIC, TALL, RARELY, STOUT,
      OFTEN, GENDER_FEMALE,
      FINERY
    ) {
      final ImageAsset
        male_skin     = costumeFor ("aesthete_male_skin.gif"    ),
        male_portrait = portraitFor("aesthete_male_portrait.png");
      public ImageAsset costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin;
      }
      public ImageAsset portraitFor(Actor actor) {
        //I.say(actor+" female? "+actor.traits.female());
        return actor.traits.female() ? portrait : male_portrait;
      }
    },
    
    AESTHETE_CIRCLES[] = { PERFORMER, COMPANION }
 ;
  
  
  final public static Background
    //
    //  Scavengers represent the unemployed/homeless/penniless who want to
    //  leave your settlement, but can't.  Free Traders peddle small goods.
    VAGRANT = new Background(
      Backgrounds.class,
      "Vagrant", "", "pyon_skin.gif", null,
      CLASS_VASSAL, NOT_A_GUILD,
      LEARNING, EVASION, NOVICE, HANDICRAFTS,
      OFTEN, NERVOUS, RARELY, RELAXED, HANDSOME
    ),
    DECK_HAND = new Background(
      Backgrounds.class,
      "Deck Hand", "",
      "air_corps_skin.gif", "air_corps_portrait.png",
      CLASS_VASSAL, NOT_A_GUILD,
      PRACTICED, ASSEMBLY, HARD_LABOUR,
      LEARNING, FIELD_THEORY, MARKSMANSHIP, COMMON_CUSTOM
    ),
    SHIP_CAPTAIN = new Background(
      Backgrounds.class,
      "Ship Captain", "",
      "air_corps_skin.gif", "air_corps_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, PILOTING, MARKSMANSHIP, PRACTICED, COMMAND, SUASION,
      LEARNING, BATTLE_TACTICS, COMMON_CUSTOM,
      BLASTER, BELT_AND_BRACER
    ),
    
    RUNNER = new Background(
      Backgrounds.class,
      "Runner", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, MARKSMANSHIP, EVASION,
      PRACTICED, SUASION, SURVEILLANCE,
      LEARNING, HAND_TO_HAND,
      OFTEN, SELFISH, SOMETIMES, NERVOUS, METICULOUS,
      CARBINE, STEALTH_SUIT
    ),
    FIXER = new Background(
      Backgrounds.class,
      "Fixer", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, ASSEMBLY, LOGIC, PRACTICED, ACCOUNTING, LEARNING, SUASION,
      OFTEN, SELFISH, SOMETIMES, NERVOUS, METICULOUS,
      OVERALLS
    ),
    
    RUNNER_CIRCLES[] = { SHIP_CAPTAIN, DECK_HAND, RUNNER, FIXER };
  
  
  final public static Background
    //
    //  You'll always get a few of these in a given native village-
    GATHERER = new Background(
      Backgrounds.class,
      "Gatherer", "", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, HANDICRAFTS, HARD_LABOUR,
      PRACTICED, CULTIVATION, NATIVE_TABOO,
      LEARNING, SUASION, SURVEILLANCE,
      RARELY, RELAXED, OFTEN, OUTGOING
    ),
    HUNTER = new Background(
      Backgrounds.class,
      "Hunter", "", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, SURVEILLANCE, EVASION,
      PRACTICED, MARKSMANSHIP, XENOZOOLOGY, ATHLETICS,
      LEARNING, HAND_TO_HAND, HANDICRAFTS,
      SOMETIMES, RUGGED,
      HUNTING_LANCE, SCRAP_GEAR
    ),
    SHAMAN = new Background(
      Backgrounds.class,
      "Shaman", "", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COUNSEL, PRACTICED, CULTIVATION,
      LEARNING, BIOLOGY, ANATOMY, MUSIC_AND_SONG,
      ALWAYS, TRADITIONAL, OFTEN, RUGGED
    ),
    CHIEFTAIN = new Background(
      Backgrounds.class,
      "Chieftain", "", "native_skin.gif", null,
      CLASS_NATIVE, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COMMAND, SUASION, MARKSMANSHIP,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS, SURVEILLANCE,
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
      "Knighted",
      "",
      "highborn_male_skin.gif", "highborn_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      MASTER, HAND_TO_HAND, EXPERT, BATTLE_TACTICS, ETIQUETTE,
      PRACTICED, ACCOUNTING, SUGGESTION, PREMONITION,
      SOMETIMES, GIFTED, SOLITARY, OFTEN, AMBITIOUS, RARELY, NERVOUS,
      SIDE_SABRE, BELT_AND_BRACER
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
      "Baron",
      "",
      "highborn_male_skin.gif", "highborn_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      MASTER, HAND_TO_HAND, EXPERT, BATTLE_TACTICS, ETIQUETTE,
      PRACTICED, ACCOUNTING, SUGGESTION, PREMONITION,
      SOMETIMES, GIFTED, SOLITARY, OFTEN, AMBITIOUS, RARELY, NERVOUS,
      SIDE_SABRE, BELT_AND_BRACER
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
      "Count",
      "",
      "highborn_male_skin.gif", "highborn_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      MASTER, HAND_TO_HAND, EXPERT, BATTLE_TACTICS, ETIQUETTE,
      PRACTICED, ACCOUNTING, SUGGESTION, PREMONITION,
      SOMETIMES, GIFTED, SOLITARY, OFTEN, AMBITIOUS, RARELY, NERVOUS,
      SIDE_SABRE, BELT_AND_BRACER
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
      "First Consort",
      "NO DESCRIPTION YET",
      "highborn_female_skin.gif", "highborn_consort_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      PRACTICED, COMMAND, SUASION, EROTICS,
      RARELY, ABSTINENT, OFTEN, AMBITIOUS, ALWAYS, HANDSOME,
      FINERY, MODUS_LUTE
    ) {
      final ImageAsset
        male_skin     = costumeFor ("highborn_male_skin.gif"),
        male_portrait = portraitFor("highborn_portrait.png" );
      public String nameFor(Actor actor) {
        final boolean male  = actor.traits.male();
        final Base    base  = actor.base();
        final Actor   ruler = base == null ? null : base.ruler();
        if (ruler == null) return male ? "Lord Consort" : "Lady Consort";
        final Background rank = ruler.mind.vocation();
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
        return actor.traits.female() ? portrait : male_portrait;
      }
    },
    MINISTER_FOR_ACCOUNTS = new Background(
      Backgrounds.class, "Minister for Accounts",
      "NO DESCRIPTION YET",
      "vendor_skin.gif", "vendor_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      EXPERT, ACCOUNTING, PRACTICED, LOGIC, COUNSEL, SUASION,
      FINERY
    ),
    WAR_MASTER = new Background(
      Backgrounds.class, "War Master",
      "NO DESCRIPTION YET",
      "highborn_male_skin.gif", "highborn_portrait.png",
      CLASS_STRATOI, NOT_A_GUILD,
      EXPERT, HAND_TO_HAND, COMMAND, BATTLE_TACTICS, PRACTICED,
      SURVEILLANCE, MARKSMANSHIP, EVASION,
      SIDE_SABRE, BLASTER, BODY_ARMOUR
    ),
    
    STEWARD = new Background(
      Backgrounds.class, "Steward", "", "citizen_skin.gif", null,
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, ETIQUETTE, PRACTICED, ANATOMY, COUNSEL, HARD_LABOUR,
      ALWAYS, LOYAL, OFTEN, TRADITIONAL, NEVER, DEFENSIVE
    ),
    
    HONOUR_GUARD = null,
    CAPTIVE      = null,
    COURT_CIRCLES[] = {},

    //
    //  These positions are for the benefit of citizens elected at the Counsel
    //  Chamber, and politically have a similar degree of influence (Consuls,
    //  in principle, are on an equal footing with the Emperor/Empress.)
    PREFECT = new Background(
      Backgrounds.class, "Prefect", "", "physician_skin.gif", null,
      CLASS_STRATOI, NOT_A_GUILD,
      PRACTICED, COUNSEL, SUASION, ACCOUNTING, COMMON_CUSTOM,
      LEARNING, ETIQUETTE, BATTLE_TACTICS, COMMAND,
      OFTEN, OUTGOING, AMBITIOUS, SOMETIMES, SELFISH
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
  
  final public static Background[]
    ALL_STANDARD_CIRCLES = (Background[]) Visit.compose(
      Background.class,
      ARTIFICER_CIRCLES, PHYSICIAN_CIRCLES, ECOLOGIST_CIRCLES,
      MILITARY_CIRCLES, AESTHETE_CIRCLES, VENDOR_CIRCLES
    );
  
  
  final public static Background
    AS_RESIDENT = new Background(
      Backgrounds.class, "Resident",
      "Placeholder argument for passing to crowding/job-getting methods.",
      null, null, NOT_A_CLASS, NOT_A_GUILD
    ),
    AS_VISITOR  = new Background(
      Backgrounds.class, "Visitor",
      "Placeholder argument for passing to crowding/job-getting methods.",
      null, null, NOT_A_CLASS, NOT_A_GUILD
    );
}





//  TODO:  COME BACK TO THESE LATER.
/*
DECK_HAND = new Background(
  Backgrounds.class,
  "Deckhand",
  "",
  "air_corps_skin.gif", "air_corps_portrait.png",
  CLASS_AGENT, GUILD_MILITANT,
  PRACTICED, HARD_LABOUR, ASSEMBLY,
  LEARNING, PILOTING, FIREARMS, SIMULACRA
  //  TODO:  Fill in personality...
),

WINGMAN = new Background(
  Backgrounds.class,
  "Wingman",
  "",
  "air_corps_skin.gif", "air_corps_portrait.png",
  CLASS_AGENT, GUILD_MILITANT,
  PRACTICED, MARKSMANSHIP, PILOTING, SURVEILLANCE, STEALTH_AND_COVER,
  LEARNING, HARD_LABOUR, ASSEMBLY,
  BLASTER, SEALSUIT
  //  TODO:  Fill in personality...
),

ACE = new Background(
  Backgrounds.class,
  "Ace",
  "",
  "air_corps_skin.gif", "air_corps_portrait.png",
  CLASS_STRATOI, GUILD_MILITANT
  //  TODO:  Fill in extra skills.
),
//*/



/*
NATIVE_AUXILIARY = new Background(
  Backgrounds.class,
  "Native Auxiliary",
  "",
  "native_skin.gif", null,
  CLASS_VASSAL, GUILD_ECOLOGIST,
  EXPERT, XENOZOOLOGY, SURVEILLANCE, STEALTH_AND_COVER,
  PRACTICED, MARKSMANSHIP, NATIVE_TABOO,
  LEARNING, BATTLE_TACTICS, HAND_TO_HAND,
  //  TODO:  Fill in personality
  HUNTING_LANCE
),

KOMMANDO = new Background(
  Backgrounds.class,
  "Kommando (Stormer)",
  "Kommandos are rugged guerilla combatants that utilise stealth and "+
  "savagery to hunt down their foes.",
  "kommando_skin.gif", "kommando_portrait.png",
  CLASS_AGENT, GUILD_ECOLOGIST,
  EXPERT, HAND_TO_HAND, SURVEILLANCE, STEALTH_AND_COVER,
  PRACTICED, NATIVE_TABOO, ATHLETICS,
  LEARNING, BATTLE_TACTICS, XENOZOOLOGY,
  RARELY, NERVOUS, RELAXED, EMPATHIC, SOMETIMES, NATURALIST,
  OFTEN, GENDER_MALE,
  ZWEIHANDER, STEALTH_SUIT
),

SLAYER = new Background(
  Backgrounds.class,
  "Kommando (Slayer)",
  "",
  "kommando_skin.gif", "kommando_portrait.png",
  CLASS_AGENT, GUILD_ECOLOGIST
  //  TODO:  Fill in skills and abilities plus personality
),
//*/






