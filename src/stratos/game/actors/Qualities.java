/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.util.*;



//  TODO:  I think I know how to clean this up and simplify.  Use 4-5 main
//  personality traits and under 20 main skills.  Everything else is a
//  technique or a trait-variant.


//  Endurance.  Percept.  Intellect.

//  Close Combat.      Medicine.
//  Marksmanship.      Engineering.
//  Surveillance.      Ecology.
//  Piloting.          Accounting.
//  Athletics.         Music & Song.
//  Diplomacy.         Handicrafts.

//  Psyonics:   Palatine Way <> Xenopath Way
//  Logician Training   <> Jil Baru Training
//  Collective Training <> Navigator Training
//  LENSr Training      <> Tek Priest Training



final public class Qualities {
  
  
  final public static int
    PERSONALITY = 0,
    PHYSICAL    = 1,
    CATEGORIC   = 2,
    ARTIFICIAL  = 3,
    SKILL       = 4,
    CONDITION   = 5;
  
  final public static int
    EFFORTLESS_DC  = -10,
    TRIVIAL_DC     = -5 ,
    SIMPLE_DC      =  0 ,
    ROUTINE_DC     =  5 ,
    MODERATE_DC    =  10,
    DIFFICULT_DC   =  15,
    STRENUOUS_DC   =  20,
    PUNISHING_DC   =  25,
    IMPOSSIBLE_DC  =  30;
  
  final public static int
    FORM_NATURAL   = 0,
    FORM_PHYSICAL  = 1, //Phys.
    FORM_SENSITIVE = 2, //Sens.
    FORM_COGNITIVE = 3, //Intel.
    FORM_PSYONIC   = 4, //Psy.
    FORM_INSTINCT  = 5;
  
  final static Class BC = Qualities.class;
  
  
  final public static Skill
    IMMUNE    = new Skill(BC, "Immune"   , FORM_NATURAL, null),
    MUSCULAR  = new Skill(BC, "Muscular" , FORM_NATURAL, null),
    MOTOR     = new Skill(BC, "Motor"    , FORM_NATURAL, null),
    PERCEPT   = new Skill(BC, "Percept"  , FORM_NATURAL, null),
    COGNITION = new Skill(BC, "Cognition", FORM_NATURAL, null),
    NERVE     = new Skill(BC, "Nerve"    , FORM_NATURAL, null),
    ATTRIBUTES[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  
  final public static Skill
    //
    //  Artifice-related skills:
    ASSEMBLY       = new Skill(BC, "Assembly"      , FORM_COGNITIVE, COGNITION),
    CHEMISTRY      = new Skill(BC, "Chemistry"     , FORM_COGNITIVE, COGNITION),
    FIELD_THEORY   = new Skill(BC, "Field Theory"  , FORM_COGNITIVE, COGNITION),
    LOGIC          = new Skill(BC, "Logic"         , FORM_COGNITIVE, COGNITION),
    ARTIFICER_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Ecology-related skills:
    XENOZOOLOGY    = new Skill(BC, "Xenozoology"   , FORM_COGNITIVE, COGNITION),
    CULTIVATION    = new Skill(BC, "Cultivation"   , FORM_COGNITIVE, COGNITION),
    ECOLOGIST_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Physician-related skills:
    BIOLOGY        = new Skill(BC, "Biology"       , FORM_COGNITIVE, COGNITION),
    ANATOMY        = new Skill(BC, "Anatomy"       , FORM_COGNITIVE, COGNITION),
    PHYSICIAN_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Research and governance:
    BATTLE_TACTICS = new Skill(BC, "Battle Tactics", FORM_COGNITIVE, COGNITION),
    ACCOUNTING     = new Skill(BC, "Accounting"    , FORM_COGNITIVE, COGNITION),
    ADMIN_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    
    COGNITIVE_SKILLS[] = (Skill[]) Visit.compose(
      Skill.class,
      ARTIFICER_SKILLS, ECOLOGIST_SKILLS, PHYSICIAN_SKILLS, ADMIN_SKILLS
    );
  
  final public static Skill
    //
    //  Methods of persuasion:
    COMMAND        = new Skill(BC, "Command"       , FORM_SENSITIVE, PERCEPT),
    SUASION        = new Skill(BC, "Suasion"       , FORM_SENSITIVE, PERCEPT),
    COUNSEL        = new Skill(BC, "Counsel"       , FORM_SENSITIVE, PERCEPT),
    TRUTH_SENSE    = new Skill(BC, "Truth Sense"   , FORM_SENSITIVE, PERCEPT),
    //
    //  Knowing the language and culture:
    NATIVE_TABOO   = new Skill(BC, "Native Taboo"  , FORM_SENSITIVE, PERCEPT),
    COMMON_CUSTOM  = new Skill(BC, "Common Custom" , FORM_SENSITIVE, PERCEPT),
    ETIQUETTE      = new Skill(BC, "Etiquette"     , FORM_SENSITIVE, PERCEPT),
    //
    //  Forms of artistic expression/entertainment:
    EROTICS        = new Skill(BC, "Erotics"       , FORM_SENSITIVE, MOTOR  ),
    MUSIC_AND_SONG = new Skill(BC, "Music"         , FORM_SENSITIVE, NERVE  ),
    HANDICRAFTS    = new Skill(BC, "Handicrafts"   , FORM_SENSITIVE, NERVE  ),
    
    SENSITIVE_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill
    //
    //  Direct combat skills:
    MARKSMANSHIP   = new Skill(BC, "Marksmanship"  , FORM_PHYSICAL, MOTOR),
    HAND_TO_HAND   = new Skill(BC, "Hand to Hand"  , FORM_PHYSICAL, MOTOR),
    //
    //  Exploration and mobility:
    ATHLETICS      = new Skill(BC, "Athletics"     , FORM_PHYSICAL, NERVE),
    PILOTING       = new Skill(BC, "Piloting"      , FORM_PHYSICAL, MOTOR),
    SURVEILLANCE   = new Skill(BC, "Surveillance"  , FORM_PHYSICAL, MOTOR),
    EVASION        = new Skill(BC, "Evasion"       , FORM_PHYSICAL, MOTOR),
    //
    //  General patience and elbow grease:
    HARD_LABOUR    = new Skill(BC, "Hard Labour"   , FORM_PHYSICAL, NERVE),
    
    PHYSICAL_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill
    SUGGESTION   = new Skill(BC, "Suggestion"  , FORM_PSYONIC, NERVE),
    METABOLISM   = new Skill(BC, "Metabolism"  , FORM_PSYONIC, NERVE),
    TRANSDUCTION = new Skill(BC, "Transduction", FORM_PSYONIC, NERVE),
    PREMONITION  = new Skill(BC, "Premonition" , FORM_PSYONIC, NERVE),
    PSYONIC_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  
  final public static Skill ALL_SKILLS[] = (Skill[]) Visit.compose(Skill.class,
    ATTRIBUTES,
    COGNITIVE_SKILLS, SENSITIVE_SKILLS, PHYSICAL_SKILLS, PSYONIC_SKILLS
  );
  
  
  final public static Trait
    
    DEFENSIVE  = new Trait(BC, "Defensive"   , PERSONALITY, "Defensive"),
    CALM       = new Trait(BC, "Calm"        , PERSONALITY, "Calm"),
    P1[] = Trait.correlate(DEFENSIVE, CALM, -1),
    
    NERVOUS    = new Trait(BC, "Nervous"     , PERSONALITY, "Nervous"),
    FEARLESS   = new Trait(BC, "Fearless"    , PERSONALITY, "Fearless"),
    P2[] = Trait.correlate(NERVOUS, FEARLESS, -1),
    
    SELFISH     = new Trait(BC, "Selfish"    , PERSONALITY, "Selfish"),
    EMPATHIC    = new Trait(BC, "Empathic"   , PERSONALITY, "Empathic"),
    P3[] = Trait.correlate(SELFISH, EMPATHIC, -1),
    
    LOYAL       = new Trait(BC, "Dutiful"    , PERSONALITY, "Dutiful"),
    FICKLE      = new Trait(BC, "Impulsive"  , PERSONALITY, "Impulsive"),
    P4[] = Trait.correlate(LOYAL, FICKLE, -1),
    
    PERSISTENT  = new Trait(BC, "Dutiful"    , PERSONALITY, "Dutiful"),
    IMPULSIVE   = new Trait(BC, "Impulsive"  , PERSONALITY, "Impulsive"),
    P5[] = Trait.correlate(PERSISTENT, IMPULSIVE, -1),
    
    OUTGOING    = new Trait(BC, "Outgoing"   , PERSONALITY, "Outgoing"),
    SOLITARY    = new Trait(BC, "Solitary"   , PERSONALITY, "Solitary"),
    P6[] = Trait.correlate(OUTGOING, SOLITARY, -1),
    
    CURIOUS     = new Trait(BC, "Curious"    , PERSONALITY, "Curious"),
    TRADITIONAL = new Trait(BC, "Traditional", PERSONALITY, "Traditional"),
    P7[] = Trait.correlate(CURIOUS, TRADITIONAL, -1),
    
    AMBITIOUS   = new Trait(BC, "Ambitious"  , PERSONALITY, "Ambitious"),
    HUMBLE      = new Trait(BC, "Humble"     , PERSONALITY, "Humble"),
    P8[] = Trait.correlate(AMBITIOUS, HUMBLE, -1),
    
    RELAXED     = new Trait(BC, "Indulgent"  , PERSONALITY, "Indulgent"),
    ABSTINENT   = new Trait(BC, "Abstinent"  , PERSONALITY, "Abstinent"),
    P9[] = Trait.correlate(RELAXED, ABSTINENT, -1),
    
    NATURALIST  = new Trait(BC, "Naturalist" , PERSONALITY, "Naturalist"),
    METICULOUS  = new Trait(BC, "Meticulous" , PERSONALITY, "Meticulous"),
    PX[] = Trait.correlate(NATURALIST, METICULOUS, -1),
    
    PERSONALITY_TRAITS[] = Trait.TRAIT_INDEX.soFar(Trait.class),
    
    //
    //  These are the listings for physical traits.  Physical traits are
    //  determined at birth and cannot be modified (except perhaps surgically),
    //  but do wax and wane based on aging, in a fashion similar to basic
    //  attributes.  TODO:  Implement that.
    
    FEMININE = new Trait(BC, "Sex Traits", PHYSICAL,
      "Busty",
      "Curvy",
      "Gamine",
      null,
      "Boyish",
      "Bearded",
      "Hirsute"
    ),
    HANDSOME = new Trait(BC, "Appearance", PHYSICAL,
      "Stunning",
      "Beautiful",
      "Handsome",
      null,
      "Plain",
      "Ugly",
      "Hideous"
    ),
    TALL = new Trait(BC, "Height", PHYSICAL,
      "Towering",
      "Big",
      "Tall",
      null,
      "Short",
      "Small",
      "Diminutive"
    ),
    STOUT = new Trait(BC, "Stoutness", PHYSICAL,
      "Obese",
      "Flabby",
      "Stout",
      null,
      "Lithe",
      "Lean",
      "Gaunt"
    ),
    PHYSICAL_TRAITS[] = Trait.TRAIT_INDEX.soFar(Trait.class),
    
    //
    //  Categoric traits are qualitative physical traits unaffected by aging.
    ORIENTATION = new Trait(BC, "Orientation", CATEGORIC,
      "Heterosexual",
      "Bisexual",
      "Homosexual",
      null
    ),
    GENDER_MALE   = new Trait(BC, "Male"  , CATEGORIC, "Male"  ),
    GENDER_FEMALE = new Trait(BC, "Female", CATEGORIC, "Female"),
    
    DESERT_BLOOD = new Trait(BC, "Desert Blood", CATEGORIC,
      "Desert Blood", // "Desertborn", "Dark"
      null
    ),
    TUNDRA_BLOOD = new Trait(BC, "Tundra Blood", CATEGORIC,
      "Tundra Blood", // "Tundraborn", "Sallow"
      null
    ),
    FOREST_BLOOD = new Trait(BC, "Forest Blood", CATEGORIC,
      "Forest Blood", //  "Forestborn", "Tan"
      null
    ),
    WASTES_BLOOD = new Trait(BC, "Wastes Blood", CATEGORIC,
      "Wastes Blood", //  "Wastesborn", "Pale"
      null
    ),
    MUTATION = new Trait(BC, "Mutation", CATEGORIC,
      "Major Mutation",
      "Minor Mutation",
      "Nominal Mutation",
      null
    ),
    GIFTED = new Trait(BC, "Gifted", CATEGORIC,
      "Gifted"
    ),
    RACIAL_TRAITS[] = {
      DESERT_BLOOD, FOREST_BLOOD, TUNDRA_BLOOD, WASTES_BLOOD
    },
    CATEGORIC_TRAITS[] = Trait.TRAIT_INDEX.soFar(Trait.class);
  //
  //  TODO:  Create a list of freaky mutations to pick from, some good, some
  //  bad.  (Bad is more likely when acquired at random, good more likely as a
  //  result of natural selection/eugenics.)
}


/*
//
//  TODO:  Put these in a separate class, so you can concisely describe their
//  effects.
final public static Trait
PSYONIC        = new Trait(BC, PHYSICAL, "Psyonic"       ),
REGENERATIVE   = new Trait(BC, PHYSICAL, "Regenerative"  ),
SUPERCOGNITIVE = new Trait(BC, PHYSICAL, "Supercognitive"),
JUMPER         = new Trait(BC, PHYSICAL, "Jumper"        ),
HYPERPHYSICAL  = new Trait(BC, PHYSICAL, "Hyperphysical" ),
CHAMELEON      = new Trait(BC, PHYSICAL, "Chameleon"     ),
ULTRASENSITIVE = new Trait(BC, PHYSICAL, "Ultrasensitive"),
VENOMOUS       = new Trait(BC, PHYSICAL, "Venomous"      ),
PHASE_SHIFTER  = new Trait(BC, PHYSICAL, "Phase Shifter" ),
GILLED         = new Trait(BC, PHYSICAL, "Gilled"        ),
FOUR_ARMED     = new Trait(BC, PHYSICAL, "Four Armed"    ),
ODD_COLOUR     = new Trait(BC, PHYSICAL, "Odd Colour"    ),
ECCENTRIC      = new Trait(BC, PHYSICAL, "Eccentric"     ),
STERILE        = new Trait(BC, PHYSICAL, "Sterile"       ),
FURRED         = new Trait(BC, PHYSICAL, "Furred"        ),
SCALY          = new Trait(BC, PHYSICAL, "Scaly"         ),
SICKLY         = new Trait(BC, PHYSICAL, "Sickly"        ),
DISTURBED      = new Trait(BC, PHYSICAL, "Disturbed"     ),
DEFORMED       = new Trait(BC, PHYSICAL, "Deformed"      ),
LEPROUS        = new Trait(BC, PHYSICAL, "Leprous"       ),
NULL_EMPATH    = new Trait(BC, PHYSICAL, "Null Empath"   ),
ATAVIST        = new Trait(BC, PHYSICAL, "Atavist"       ),
ABOMINATION    = new Trait(BC, PHYSICAL, "Abomination"   ),
MUTANT_TRAITS[] = Trait.traitsSoFar();
//*/

/*
  //  Logicians, Spacers, Initiates, Shapers, Collective and Symbiotes-
  //    Supercognitive, Primary/Secondary/Tertiary, Cyborg, Melded, Symbiote
  //  There are some extra traits lying around-
  //    Infected, Hypersensitive/Ultraphysical, Longevity.
  //  Each of the the major monster categories also has an identifying trait-
  //    Humanoid, Insectile, Silicate, Artilect, Browser and Predator.
  //  The three non-humanoid species also have a dedicated life-cycle-
  //    Sessile/Changeling/Blossom Node, Larva/Worker/Soldier/Queen, Jovian.


    PRIME_DIRECTIVES    = new Trait(BC, "Prime Directives"   , Type.SUPERNORMAL),
    ARTILECT            = new Trait(BC, "Artilect"           , Type.SUPERNORMAL),
    SILICATE_METABOLISM = new Trait(BC, "Silicate Metabolism", Type.SUPERNORMAL),
    MINDLESS            = new Trait(BC, "Mindless"           , Type.SUPERNORMAL),
    ANCIENT             = new Trait(BC, "Ancient"            , Type.SUPERNORMAL),
    HUMANOID            = new Trait(BC, "Humanoid"           , Type.INNATE     ),
    INSECTILE           = new Trait(BC, "Insectile"          , Type.SUPERNORMAL),
    PLANT_METABOLISM    = new Trait(BC, "Plant Metabolism"   , Type.SUPERNORMAL),
    IMMOBILE            = new Trait(BC, "Immobile"           , Type.SUPERNORMAL),
    XENOMORPH           = new Trait(BC, "Xenomorph"          , Type.SUPERNORMAL),
    AMORPHOUS           = new Trait(BC, "Amorphous"          , Type.SUPERNORMAL),
    MELDED              = new Trait(BC, "Melded"             , Type.SUPERNORMAL),
    PART_CYBORG         = new Trait(BC, "Part Cyborg"        , Type.ACQUIRED   ),
    FULL_CYBORG         = new Trait(BC, "Full Cyborg"        , Type.ACQUIRED   ),
    FAST_METABOLISM     = new Trait(BC, "Fast Metabolism"    , Type.INNATE     ),
    LONG_LIVED          = new Trait(BC, "Long Lived"         , Type.INNATE     ),
    IMMORTAL            = new Trait(BC, "Immortal"           , Type.SUPERNORMAL),
    SPECIES_TRAITS[] = Trait.traitsSoFar(),
    
    ALL_TRAITS[] = Trait.allTraits()
 ;
//*/

