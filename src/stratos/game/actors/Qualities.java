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
    INSCRIPTION    = new Skill(BC, "Inscription"   , FORM_COGNITIVE, COGNITION),
    FIELD_THEORY   = new Skill(BC, "Field Theory"  , FORM_COGNITIVE, COGNITION),
    ASTROGATION    = new Skill(BC, "Astrogation"   , FORM_COGNITIVE, COGNITION),
    SIMULACRA      = new Skill(BC, "Simulacra"     , FORM_COGNITIVE, COGNITION),
    ARTIFICER_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Ecology-related skills:
    XENOZOOLOGY    = new Skill(BC, "Xenozoology"   , FORM_COGNITIVE, COGNITION),
    CULTIVATION    = new Skill(BC, "Cultivation"   , FORM_COGNITIVE, COGNITION),
    GEOPHYSICS     = new Skill(BC, "Geophysics"    , FORM_COGNITIVE, COGNITION),
    CETANI_ECOLOGY = new Skill(BC, "Cetani Ecology", FORM_COGNITIVE, COGNITION),
    ALBEDO_ECOLOGY = new Skill(BC, "Albedo Ecology", FORM_COGNITIVE, COGNITION),
    SILICO_ECOLOGY = new Skill(BC, "Silico Ecology", FORM_COGNITIVE, COGNITION),
    ECOLOGIST_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Physician-related skills:
    PHARMACY       = new Skill(BC, "Pharmacy"      , FORM_COGNITIVE, COGNITION),
    GENE_CULTURE   = new Skill(BC, "Gene Culture"  , FORM_COGNITIVE, COGNITION),
    ANATOMY        = new Skill(BC, "Anatomy"       , FORM_COGNITIVE, COGNITION),
    PSYCHOANALYSIS = new Skill(BC, "Psychoanalysis", FORM_COGNITIVE, COGNITION),
    FORENSICS      = new Skill(BC, "Forensics"     , FORM_COGNITIVE, COGNITION),
    SOCIAL_HISTORY = new Skill(BC, "Social History", FORM_COGNITIVE, COGNITION),
    PHYSICIAN_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Research and governance:
    BATTLE_TACTICS = new Skill(BC, "Battle Tactics", FORM_COGNITIVE, COGNITION),
    ACCOUNTING     = new Skill(BC, "Accounting"    , FORM_COGNITIVE, COGNITION),
    ANCIENT_LORE   = new Skill(BC, "Ancient Lore"  , FORM_COGNITIVE, COGNITION),
    LEGISLATION    = new Skill(BC, "Legislation"   , FORM_COGNITIVE, COGNITION),
    ADMIN_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    
    COGNITIVE_SKILLS[] = (Skill[]) Visit.compose(
      Skill.class,
      ARTIFICER_SKILLS, ECOLOGIST_SKILLS, PHYSICIAN_SKILLS, ADMIN_SKILLS
    );
  
  final public static Skill
    //
    //  Methods of persuasion:
    COMMAND           = new Skill(BC, "Command"        , FORM_SENSITIVE, PERCEPT),
    SUASION           = new Skill(BC, "Suasion"        , FORM_SENSITIVE, PERCEPT),
    COUNSEL           = new Skill(BC, "Counsel"        , FORM_SENSITIVE, PERCEPT),
    TRUTH_SENSE       = new Skill(BC, "Truth Sense"    , FORM_SENSITIVE, PERCEPT),
    //
    //  Knowing the language and culture:
    NATIVE_TABOO      = new Skill(BC, "Native Taboo"   , FORM_SENSITIVE, PERCEPT),
    COMMON_CUSTOM     = new Skill(BC, "Common Custom"  , FORM_SENSITIVE, PERCEPT),
    NOBLE_ETIQUETTE   = new Skill(BC, "Noble Etiquette", FORM_SENSITIVE, PERCEPT),
    OUTER_DIALECTS    = new Skill(BC, "Outer Dialects" , FORM_SENSITIVE, PERCEPT),
    REPUBLIC_LAWS     = new Skill(BC, "Republic Laws"  , FORM_SENSITIVE, PERCEPT),
    IMPERIAL_DOGMA    = new Skill(BC, "Imperial Dogma" , FORM_SENSITIVE, PERCEPT),
    //
    //  Forms of artistic expression/entertainment:
    EROTICS           = new Skill(BC, "Erotics"        , FORM_SENSITIVE, MOTOR  ),
    MASQUERADE        = new Skill(BC, "Masquerade"     , FORM_SENSITIVE, PERCEPT),
    MUSIC_AND_SONG    = new Skill(BC, "Music and Song" , FORM_SENSITIVE, MOTOR  ),
    GRAPHIC_DESIGN    = new Skill(BC, "Graphic Design" , FORM_SENSITIVE, PERCEPT),
    
    SENSITIVE_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill
    //
    //  Direct combat skills:
    FORMATION_COMBAT  = new Skill(BC, "Formation Combat" , FORM_PHYSICAL, NERVE),
    MARKSMANSHIP      = new Skill(BC, "Marksmanship"     , FORM_PHYSICAL, MOTOR),
    HAND_TO_HAND      = new Skill(BC, "Hand to Hand"     , FORM_PHYSICAL, MOTOR),
    SHIELD_AND_ARMOUR = new Skill(BC, "Shield and Armour", FORM_PHYSICAL, MOTOR),
    HEAVY_WEAPONS     = new Skill(BC, "Heavy Weapons"    , FORM_PHYSICAL, MOTOR),
    FIREARMS          = new Skill(BC, "Firearms"         , FORM_PHYSICAL, MOTOR),
    //
    //  Exploration and mobility:
    ATHLETICS         = new Skill(BC, "Athletics"        , FORM_PHYSICAL, NERVE),
    PILOTING          = new Skill(BC, "Piloting"         , FORM_PHYSICAL, MOTOR),
    SURVEILLANCE      = new Skill(BC, "Surveillance"     , FORM_PHYSICAL, MOTOR),
    STEALTH_AND_COVER = new Skill(BC, "Stealth and Cover", FORM_PHYSICAL, MOTOR),
    //
    //  General patience and elbow grease:
    HANDICRAFTS       = new Skill(BC, "Handicrafts"      , FORM_PHYSICAL, MOTOR),
    HARD_LABOUR       = new Skill(BC, "Hard Labour"      , FORM_PHYSICAL, NERVE),
    DOMESTICS         = new Skill(BC, "Domestics"        , FORM_PHYSICAL, MOTOR),
    BODY_MEDITATION   = new Skill(BC, "Body Meditation"  , FORM_PHYSICAL, NERVE),
    
    PHYSICAL_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill
    SUGGESTION   = new Skill(BC, "Suggestion"  , FORM_PSYONIC, NERVE),
    SYNESTHESIA  = new Skill(BC, "Synesthesia" , FORM_PSYONIC, NERVE),
    METABOLISM   = new Skill(BC, "Metabolism"  , FORM_PSYONIC, NERVE),
    TRANSDUCTION = new Skill(BC, "Transduction", FORM_PSYONIC, NERVE),
    PROJECTION   = new Skill(BC, "Projection"  , FORM_PSYONIC, NERVE),
    PREMONITION  = new Skill(BC, "Premonition" , FORM_PSYONIC, NERVE),
    
    PSYONIC_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill
    //
    //  For the benefit of animals and non-human species-
    MIMESIS        = new Skill(BC, "Mimesis"       , FORM_INSTINCT, MOTOR    ),
    PHEREMONIST    = new Skill(BC, "Pheremonist"   , FORM_INSTINCT, NERVE    ),
    IMMANENCE      = new Skill(BC, "Immanence"     , FORM_INSTINCT, COGNITION),
    
    INSTINCT_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill ALL_SKILLS[] = (Skill[]) Visit.compose(Skill.class,
    ATTRIBUTES,
    COGNITIVE_SKILLS, SENSITIVE_SKILLS, PHYSICAL_SKILLS,
    PSYONIC_SKILLS, INSTINCT_SKILLS
  );
  
  
  //  TODO:  I think only about half of these are needed.
  public static Trait
    DEFENSIVE  = new Trait(BC, "Defensive", PERSONALITY, "Defensive"),
    CRITICAL   = new Trait(BC, "Critical", PERSONALITY, "Critical"),
    NERVOUS    = new Trait(BC, "Nervous", PERSONALITY, "Nervous"),
    
    CALM       = new Trait(BC, "Calm", PERSONALITY, "Calm"),
    POSITIVE   = new Trait(BC, "Positive", PERSONALITY, "Positive"),
    FEARLESS   = new Trait(BC, "Fearless", PERSONALITY, "Fearless"),
    
    CRUEL       = new Trait(BC, "Cruel", PERSONALITY, "Cruel"),
    DISHONEST   = new Trait(BC, "Dishonest", PERSONALITY, "Dishonest"),
    ACQUISITIVE = new Trait(BC, "Acquisitive", PERSONALITY, "Acquisitive"),
    
    EMPATHIC    = new Trait(BC, "Empathic", PERSONALITY, "Empathic"),
    ETHICAL     = new Trait(BC, "Ethical", PERSONALITY, "Ethical"),
    GENEROUS    = new Trait(BC, "Generous", PERSONALITY, "Generous"),
    
    SUBVERSIVE  = new Trait(BC, "Subversive", PERSONALITY, "Subversive"),
    NATURALIST  = new Trait(BC, "Naturalist", PERSONALITY, "Naturalist"),
    INDULGENT   = new Trait(BC, "Indulgent", PERSONALITY, "Indulgent"),
    
    DUTIFUL     = new Trait(BC, "Dutiful", PERSONALITY, "Dutiful"),
    METICULOUS  = new Trait(BC, "Meticulous", PERSONALITY, "Urbane"),
    ABSTINENT   = new Trait(BC, "Abstinent", PERSONALITY, "Abstinent"),
    
    CREATIVE    = new Trait(BC, "Creative", PERSONALITY, "Creative"),
    CURIOUS     = new Trait(BC, "Curious", PERSONALITY, "Curious"),
    IMPULSIVE   = new Trait(BC, "Impulsive", PERSONALITY, "Impulsive"),
    
    TRADITIONAL = new Trait(BC, "Traditional", PERSONALITY, "Traditional"),
    PATIENT     = new Trait(BC, "Patient", PERSONALITY, "Patient"),
    STUBBORN    = new Trait(BC, "Stubborn", PERSONALITY, "Stubborn"),
    
    AMBITIOUS   = new Trait(BC, "Ambitious", PERSONALITY, "Ambitious"),
    ENERGETIC   = new Trait(BC, "Energetic", PERSONALITY, "Energetic"),
    OUTGOING    = new Trait(BC, "Outgoing", PERSONALITY, "Outgoing"),
    
    HUMBLE      = new Trait(BC, "Humble", PERSONALITY, "Humble"),
    RELAXED     = new Trait(BC, "Relaxed", PERSONALITY, "Relaxed"),
    SOLITARY    = new Trait(BC, "Solitary", PERSONALITY, "Solitary"),
    
    EXCITABLE   = new Trait(BC, "Excitable", PERSONALITY, "Excitable"),
    IMPASSIVE   = new Trait(BC, "Impassive", PERSONALITY, "Impassive"),
    
    PERSONALITY_TRAITS[] = Personality.setupRelations(
      Trait.TRAIT_INDEX.soFar(Trait.class)
    ),
    
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
      "Rotund",
      "Stout",
      "Sturdy",
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
    PSYONIC = new Trait(BC, "Psyonic", CATEGORIC,
      "Psyon"
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



/*
  //
  //  These are the listings of personality traits.  These can be modified
  //  over time based on experience, peer pressure or conditioning.  Genetic
  //  factors also influence their expression.  (TODO:  Implement that.)
  //
  //  I've divided these into 3 main categories-
  //    Basic Impulses (emotional drives or physical needs)
  //    Meta-Decisional (modify the general process of plan-selection)
  //    Cultural/Ethical (overall social values)
  
  //
  //  BASIC IMPULSES-
  NERVOUS = new Trait(BC, PERSONALITY,
    "Cowardly",
    "Nervous",
    "Cautious",
    null,
    "Assertive",
    "Fearless",
    "Reckless"
  ),
  AGGRESSIVE = new Trait(BC, PERSONALITY,
    "Vengeful",
    "Aggressive",
    "Defensive",
    null,
    "Calm",
    "Gentle",
    "Pacifist"
  ),
  FRIENDLY = new Trait(BC, PERSONALITY,
    "Fawning",
    "Complimentary",
    "Friendly",
    null,
    "Reserved",
    "Critical",
    "Caustic"
  ),
  OPTIMISTIC = new Trait(BC, PERSONALITY,
    "Blithe",
    "Optimistic",
    "Cheerful",
    null,
    "Skeptical",
    "Pessimistic",
    "Morose"
  ),
  DEBAUCHED = new Trait(BC, PERSONALITY,
    "Debauched",
    "Lusty",
    "Fun",
    null,
    "Temperate",
    "Abstinent",
    "Ascetic"
  ),
  APPETITE = new Trait(BC, PERSONALITY,
    "Gluttonous",
    "Big Appetite",
    "Gourmand",
    null,
    "Frugal",
    "Small Appetite",
    "No Appetite"
  ),
  
  //
  //  META-DECISIONAL-
  STUBBORN = new Trait(BC, PERSONALITY,
    "Obstinate",
    "Stubborn",
    "Persistent",
    null,
    "Spontaneous",
    "Impulsive",
    "Fickle"
  ),
  INQUISITIVE = new Trait(BC, PERSONALITY,
    "Insatiably Curious",
    "Inquisitive",
    "Curious",
    null,
    "Stolid",
    "Disinterested",
    "Dull"
  ),
  SOCIABLE = new Trait(BC, PERSONALITY,
    "Gregarious",
    "Sociable",
    "Open",
    null,
    "Private",
    "Solitary",
    "Withdrawn"
  ),
  DUTIFUL = new Trait(BC, PERSONALITY,
    "Obedient",
    "Dutiful",
    "Respectful of Betters",
    null,
    "Independant",
    "Rebellious",
    "Anarchic"
  ),
  IMPASSIVE = new Trait(BC, PERSONALITY,
    "Emotionless",
    "Impassive",
    "Rational",
    null,
    "Passionate",
    "Excitable",
    "Manic"
  ),
  INDOLENT = new Trait(BC, PERSONALITY,
    "Lethargic",
    "Indolent",
    "Relaxed",
    null,
    "Busy",
    "Restless",
    "Workaholic"
  ),
  
  //
  //  CULTURAL/ETHICAL-
  TRADITIONAL = new Trait(BC, PERSONALITY,
    "Hidebound",
    "Traditional",
    "Old-fashioned",
    null,
    "Reformist",
    "Radical",
    "Subversive"
  ),
  NATURALIST = new Trait(BC, PERSONALITY,
    "Gone Feral",
    "Ecophile",
    "Naturalist",
    null,
    "Urbanist",
    "Industrialist",
    "Antiseptic"
  ),
  ACQUISITIVE = new Trait(BC, PERSONALITY,
    "Avaricious",
    "Acquisitive",
    "Thrifty",
    null,
    "Generous",
    "Extravagant",
    "Profligate"
  ),
  AMBITIOUS = new Trait(BC, PERSONALITY,
    "Narcissist",
    "Ambitious",
    "Proud",
    null,
    "Modest",
    "Humble",
    "Complacent"
  ),
  HONOURABLE = new Trait(BC, PERSONALITY,
    "Unimpeachable",
    "Honourable",
    "Trustworthy",
    null,
    "Sly",
    "Dishonest",
    "Manipulative"
  ),
  EMPATHIC = new Trait(BC, PERSONALITY,
    "Martyr Complex",
    "Compassionate",
    "Sympathetic",
    null,
    "Hard",
    "Cruel",
    "Sadistic"
  ),
  PERSONALITY_TRAITS[] = Trait.traitsSoFar(),
//*/
  


