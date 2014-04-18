/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors ;
import stratos.game.tactical.Power;
import stratos.util.*;



public interface Qualities {
  
  
  final public static int
    PERSONALITY = 0,
    PHYSICAL    = 1,
    CATEGORIC   = 2,
    ARTIFICIAL  = 3,
    SKILL       = 4,
    CONDITION   = 5 ;
  
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
  
  final static int
    FORM_NATURAL   = 0,
    FORM_PHYSICAL  = 1,
    FORM_SENSITIVE = 2,
    FORM_COGNITIVE = 3,
    FORM_PSYONIC   = 4,
    FORM_INSTINCT  = 5 ;
  
  
  final public static Skill
    IMMUNE    = new Skill("Immune"   , FORM_NATURAL, null),
    MUSCULAR  = new Skill("Muscular" , FORM_NATURAL, null),
    MOTOR     = new Skill("Motor"    , FORM_NATURAL, null),
    PERCEPT   = new Skill("Percept"  , FORM_NATURAL, null),
    COGNITION = new Skill("Cognition", FORM_NATURAL, null),
    NERVE     = new Skill("Nerve"    , FORM_NATURAL, null),
    ATTRIBUTES[] = Trait.skillsSoFar();
  
  
  //  TODO:  Move these down
  final public static Skill
    //
    //  For the benefit of animals and non-human species-
    SCENTING       = new Skill("Scenting"      , FORM_INSTINCT, PERCEPT  ),
    LIMB_AND_MAW   = new Skill("Limb and Maw"  , FORM_INSTINCT, MOTOR    ),
    NESTING        = new Skill("Nesting"       , FORM_INSTINCT, PERCEPT  ),
    MIMESIS        = new Skill("Mimesis"       , FORM_INSTINCT, MOTOR    ),
    PHEREMONIST    = new Skill("Pheremonist"   , FORM_INSTINCT, NERVE    ),
    IMMANENCE      = new Skill("Immanence"     , FORM_INSTINCT, COGNITION),
    
    INSTINCT_SKILLS[] = Trait.skillsSoFar();
  
  final public static Skill
    //
    //  Artifice-related skills:
    ASSEMBLY       = new Skill("Assembly"      , FORM_COGNITIVE, COGNITION),
    CHEMISTRY      = new Skill("Chemistry"     , FORM_COGNITIVE, COGNITION),
    INSCRIPTION    = new Skill("Inscription"   , FORM_COGNITIVE, COGNITION),
    FIELD_THEORY   = new Skill("Field Theory"  , FORM_COGNITIVE, COGNITION),
    ASTROGATION    = new Skill("Astrogation"   , FORM_COGNITIVE, COGNITION),
    SIMULACRA      = new Skill("Simulacra"     , FORM_COGNITIVE, COGNITION),
    ARTIFICER_SKILLS[] = Trait.skillsSoFar(),
    //
    //  Ecology-related skills:
    XENOZOOLOGY    = new Skill("Xenozoology"   , FORM_COGNITIVE, COGNITION),
    CULTIVATION    = new Skill("Cultivation"   , FORM_COGNITIVE, COGNITION),
    GEOPHYSICS     = new Skill("Geophysics"    , FORM_COGNITIVE, COGNITION),
    CETANI_ECOLOGY = new Skill("Cetani Ecology", FORM_COGNITIVE, COGNITION),
    ALBEDO_ECOLOGY = new Skill("Albedo Ecology", FORM_COGNITIVE, COGNITION),
    SILICO_ECOLOGY = new Skill("Silico Ecology", FORM_COGNITIVE, COGNITION),
    ECOLOGIST_SKILLS[] = Trait.skillsSoFar(),
    //
    //  Physician-related skills:
    PHARMACY       = new Skill("Pharmacy"      , FORM_COGNITIVE, COGNITION),
    GENE_CULTURE   = new Skill("Gene Culture"  , FORM_COGNITIVE, COGNITION),
    ANATOMY        = new Skill("Anatomy"       , FORM_COGNITIVE, COGNITION),
    PSYCHOANALYSIS = new Skill("Psychoanalysis", FORM_COGNITIVE, COGNITION),
    FORENSICS      = new Skill("Forensics"     , FORM_COGNITIVE, COGNITION),
    SOCIAL_HISTORY = new Skill("Social History", FORM_COGNITIVE, COGNITION),
    PHYSICIAN_SKILLS[] = Trait.skillsSoFar(),
    //
    //  Research and governance:
    BATTLE_TACTICS = new Skill("Battle Tactics", FORM_COGNITIVE, COGNITION),
    ACCOUNTING     = new Skill("Accounting"    , FORM_COGNITIVE, COGNITION),
    ANCIENT_LORE   = new Skill("Ancient Lore"  , FORM_COGNITIVE, COGNITION),
    LEGISLATION    = new Skill("Legislation"   , FORM_COGNITIVE, COGNITION),
    ADMIN_SKILLS[] = Trait.skillsSoFar(),
    
    COGNITIVE_SKILLS[] = (Skill[]) Visit.compose(
      Skill.class,
      ARTIFICER_SKILLS, ECOLOGIST_SKILLS, PHYSICIAN_SKILLS, ADMIN_SKILLS
    ) ;
  
  final public static Skill
    //
    //  Methods of persuasion:
    COMMAND           = new Skill("Command"        , FORM_SENSITIVE, PERCEPT),
    SUASION           = new Skill("Suasion"        , FORM_SENSITIVE, PERCEPT),
    COUNSEL           = new Skill("Counsel"        , FORM_SENSITIVE, PERCEPT),
    TRUTH_SENSE       = new Skill("Truth Sense"    , FORM_SENSITIVE, PERCEPT),
    //
    //  Knowing the language and culture:
    NATIVE_TABOO      = new Skill("Native Taboo"   , FORM_SENSITIVE, PERCEPT),
    COMMON_CUSTOM     = new Skill("Common Custom"  , FORM_SENSITIVE, PERCEPT),
    NOBLE_ETIQUETTE   = new Skill("Noble Etiquette", FORM_SENSITIVE, PERCEPT),
    OUTER_DIALECTS    = new Skill("Outer Dialects" , FORM_SENSITIVE, PERCEPT),
    REPUBLIC_LAWS     = new Skill("Republic Laws"  , FORM_SENSITIVE, PERCEPT),
    IMPERIAL_DOGMA    = new Skill("Imperial Dogma" , FORM_SENSITIVE, PERCEPT),
    //
    //  Forms of artistic expression/entertainment:
    EROTICS           = new Skill("Erotics"        , FORM_SENSITIVE, MOTOR  ),
    MASQUERADE        = new Skill("Masquerade"     , FORM_SENSITIVE, PERCEPT),
    MUSIC_AND_SONG    = new Skill("Music and Song" , FORM_SENSITIVE, MOTOR  ),
    GRAPHIC_DESIGN    = new Skill("Graphic Design" , FORM_SENSITIVE, PERCEPT),
    
    SENSITIVE_SKILLS[] = Trait.skillsSoFar() ;
  
  final public static Skill
    //
    //  Direct combat skills:
    FORMATION_COMBAT  = new Skill("Formation Combat" , FORM_PHYSICAL, NERVE),
    MARKSMANSHIP      = new Skill("Marksmanship"     , FORM_PHYSICAL, MOTOR),
    HAND_TO_HAND      = new Skill("Hand to Hand"     , FORM_PHYSICAL, MOTOR),
    SHIELD_AND_ARMOUR = new Skill("Shield and Armour", FORM_PHYSICAL, MOTOR),
    HEAVY_WEAPONS     = new Skill("Heavy Weapons"    , FORM_PHYSICAL, MOTOR),
    FIREARMS          = new Skill("Firearms"         , FORM_PHYSICAL, MOTOR),
    //
    //  Exploration and mobility:
    ATHLETICS         = new Skill("Athletics"        , FORM_PHYSICAL, NERVE),
    PILOTING          = new Skill("Piloting"         , FORM_PHYSICAL, MOTOR),
    SURVEILLANCE      = new Skill("Surveillance"     , FORM_PHYSICAL, MOTOR),
    STEALTH_AND_COVER = new Skill("Stealth and Cover", FORM_PHYSICAL, MOTOR),
    //
    //  General patience and elbow grease:
    HANDICRAFTS       = new Skill("Handicrafts"      , FORM_PHYSICAL, MOTOR),
    HARD_LABOUR       = new Skill("Hard Labour"      , FORM_PHYSICAL, NERVE),
    DOMESTICS         = new Skill("Domestics"        , FORM_PHYSICAL, MOTOR),
    BODY_MEDITATION   = new Skill("Body Meditation"  , FORM_PHYSICAL, NERVE),
    
    PHYSICAL_SKILLS[] = Trait.skillsSoFar();
  
  final public static Skill
    SUGGESTION   = new Skill("Suggestion"  , FORM_PSYONIC, NERVE),
    SYNESTHESIA  = new Skill("Synesthesia" , FORM_PSYONIC, NERVE),
    METABOLISM   = new Skill("Metabolism"  , FORM_PSYONIC, NERVE),
    TRANSDUCTION = new Skill("Transduction", FORM_PSYONIC, NERVE),
    PROJECTION   = new Skill("Projection"  , FORM_PSYONIC, NERVE),
    PREMONITION  = new Skill("Premonition" , FORM_PSYONIC, NERVE),
    
    PSYONIC_SKILLS[] = Trait.skillsSoFar();
  
  final public static Skill ALL_SKILLS[] = (Skill[]) Visit.compose(Skill.class,
    ATTRIBUTES, INSTINCT_SKILLS, PSYONIC_SKILLS,
    COGNITIVE_SKILLS, SENSITIVE_SKILLS, PHYSICAL_SKILLS
  ) ;
  
  
  public static Trait
    DEFENSIVE  = new Trait(PERSONALITY, "Defensive"),
    CRITICAL   = new Trait(PERSONALITY, "Critical"),
    NERVOUS    = new Trait(PERSONALITY, "Nervous"),
    
    CALM       = new Trait(PERSONALITY, "Calm"),
    POSITIVE   = new Trait(PERSONALITY, "Positive"),
    FEARLESS   = new Trait(PERSONALITY, "Fearless"),
    
    CRUEL       = new Trait(PERSONALITY, "Cruel"),
    DISHONEST   = new Trait(PERSONALITY, "Dishonest"),
    ACQUISITIVE = new Trait(PERSONALITY, "Acquisitive"),
    
    EMPATHIC    = new Trait(PERSONALITY, "Empathic"),
    TRUTHFUL    = new Trait(PERSONALITY, "Truthful"),
    GENEROUS    = new Trait(PERSONALITY, "Generous"),
    
    SUBVERSIVE  = new Trait(PERSONALITY, "Subversive"),
    NATURALIST  = new Trait(PERSONALITY, "Naturalist"),
    INDULGENT   = new Trait(PERSONALITY, "Indulgent"),
    
    DUTIFUL     = new Trait(PERSONALITY, "Dutiful"),
    URBANE      = new Trait(PERSONALITY, "Urbane"),
    ABSTINENT   = new Trait(PERSONALITY, "Abstinent"),
    
    CREATIVE    = new Trait(PERSONALITY, "Creative"),
    CURIOUS     = new Trait(PERSONALITY, "Curious"),
    IMPULSIVE   = new Trait(PERSONALITY, "Impulsive"),
    
    TRADITIONAL = new Trait(PERSONALITY, "Traditional"),
    IGNORANT    = new Trait(PERSONALITY, "Ignorant"),
    STUBBORN    = new Trait(PERSONALITY, "Stubborn"),
    
    AMBITIOUS   = new Trait(PERSONALITY, "Ambitious"),
    ENERGETIC   = new Trait(PERSONALITY, "Energetic"),
    OUTGOING    = new Trait(PERSONALITY, "Outgoing"),
    
    HUMBLE      = new Trait(PERSONALITY, "Humble"),
    RELAXED     = new Trait(PERSONALITY, "Relaxed"),
    SOLITARY    = new Trait(PERSONALITY, "Solitary"),
    
    EXCITABLE   = new Trait(PERSONALITY, "Excitable"),
    IMPASSIVE   = new Trait(PERSONALITY, "Impassive"),
    
    PERSONALITY_TRAITS[] = Personality.setupRelations(Trait.traitsSoFar()),
    //
    //  These are the listings for physical traits.  Physical traits are
    //  determined at birth and cannot be modified (except perhaps surgically),
    //  but do wax and wane based on aging, in a fashion similar to basic
    //  attributes.  TODO:  Implement that.
    
    FEMININE = new Trait(PHYSICAL,
      "Busty",
      "Curvy",
      "Gamine",
      null,
      "Boyish",
      "Bearded",
      "Hirsute"
    ),
    HANDSOME = new Trait(PHYSICAL,
      "Stunning",
      "Beautiful",
      "Handsome",
      null,
      "Plain",
      "Ugly",
      "Hideous"
    ),
    TALL = new Trait(PHYSICAL,
      "Towering",
      "Big",
      "Tall",
      null,
      "Short",
      "Small",
      "Diminutive"
    ),
    STOUT = new Trait(PHYSICAL,
      "Rotund",
      "Stout",
      "Sturdy",
      null,
      "Lithe",
      "Lean",
      "Gaunt"
    ),
    PHYSICAL_TRAITS[] = Trait.traitsSoFar(),
    
    //
    //  Categoric traits are qualitative physical traits unaffected by aging.
    ORIENTATION = new Trait(CATEGORIC,
      "Heterosexual",
      "Bisexual",
      "Homosexual",
      null
    ),
    GENDER = new Trait(CATEGORIC,
      "Female",
      null,
      "Male"
    ),
    DESERT_BLOOD = new Trait(CATEGORIC,
      "Desert Blood", // "Desertborn", "Dark"
      null
    ),
    TUNDRA_BLOOD = new Trait(CATEGORIC,
      "Tundra Blood", // "Tundraborn", "Sallow"
      null
    ),
    FOREST_BLOOD = new Trait(CATEGORIC,
      "Forest Blood", //  "Forestborn", "Tan"
      null
    ),
    WASTES_BLOOD = new Trait(CATEGORIC,
      "Wastes Blood", //  "Wastesborn", "Pale"
      null
    ),
    MUTATION = new Trait(CATEGORIC,
      "Major Mutation",
      "Minor Mutation",
      "Nominal Mutation",
      null
    ),
    PSYONIC = new Trait(CATEGORIC,
      "Psyon"
    ),
    BLOOD_TRAITS[] = {
      DESERT_BLOOD, TUNDRA_BLOOD, FOREST_BLOOD, WASTES_BLOOD
    },
    CATEGORIC_TRAITS[] = Trait.traitsSoFar() ;
  //
  //  TODO:  Create a list of freaky mutations to pick from, some good, some
  //  bad.  (Bad is more likely when acquired at random, good more likely as a
  //  result of natural selection/eugenics.)
  
  
  
  
  
  final static int
    NO_LATENCY     = 0,
    SHORT_LATENCY  = 1,
    MEDIUM_LATENCY = 10,
    LONG_LATENCY   = 100,
    
    NO_SPREAD    = 0,
    SLOW_SPREAD  = 2,
    RAPID_SPREAD = 5,
    
    NO_VIRULENCE        = 0,
    MINIMAL_VIRULENCE   = 5,
    LOW_VIRULENCE       = 10,
    AVERAGE_VIRULENCE   = 15,
    HIGH_VIRULENCE      = 20,
    EXTREME_VIRULENCE   = 25 ;
  
  final public static Condition
    //
    //  Finally, listings for various conditions that might beset the actor-
    INJURY = new Condition(
      "Critical Injury",
      "Serious Injury",
      "Modest Injury",
      "Slight Injury",
      null
    ),
    FATIGUE = new Condition(
      "Extreme Fatigue",
      "Heavy Fatigue",
      "Modest Fatigue",
      "Mild Fatigue",
      null
    ),
    POOR_MORALE = new Condition(
      "Broken Morale",
      "Poor Morale",
      null,
      "Good Morale",
      "Superb Morale"
    ),
    
    HUNGER = new Condition(
      "Near Starvation",
      "Hungry",
      "Peckish",
      null,
      "Full"
    ),
    MALNOURISHMENT = new Condition(
      "Malnourished",
      null
    ),
    //
    //  TODO:  Use this as a possible side-effect of incompetent foraging.
    POISONED = new Condition(
      SHORT_LATENCY, LOW_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -5, MUSCULAR, -5
      ),
      "Severe Poisoning",
      "Bad Poisoning",
      "Mild Poisoning",
      null,
      "Poison Immune"
    ),
    
    SOMA_HAZE = new Condition(
      NO_LATENCY, NO_VIRULENCE, NO_SPREAD, Table.make(
        MOTOR, -3, COGNITION, -1, PERCEPT, 1
      ),
      "Soma Haze",
      "Soma Haze",
      "Soma Haze",
      null,
      "Haze Immune"
    ),
    ILLNESS = new Condition(
      SHORT_LATENCY, MINIMAL_VIRULENCE, RAPID_SPREAD, Table.make(
        IMMUNE, -5, MUSCULAR, -5
      ),
      "Debilitating Illness",
      "Serious Illness",
      "Mild Illness",
       null,
       "Illness Immune"
    ),
    SPICE_ADDICTION = new Condition(
      LONG_LATENCY, LOW_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -10, PERCEPT, -5, NERVE, -5, COGNITION, -5
      ),
      "Complete Spice Addiction",
      "Heavy Spice Addiction",
      "Mild Spice Addiction",
      null,
      "Addiction Immune"
    ),
    CANCER = new Condition(
      LONG_LATENCY, AVERAGE_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -20, MUSCULAR, -10
      ),
      "Terminal Cancer",
      "Advanced Cancer",
      "Early Cancer",
      null,
      "Cancer Immune"
    ),
    RAGE_INFECTION = new Condition(
      SHORT_LATENCY, HIGH_VIRULENCE, RAPID_SPREAD, Table.make(
        IMMUNE, 5, MUSCULAR, 5, DEFENSIVE, 5, COGNITION, -15
      ),
      "Rage Frenzy",
      "Rage Fever",
      "Rage Onset",
      null,
      "Rage Immune"
    ),
    HIREX_PARASITE = new Condition(
      MEDIUM_LATENCY, HIGH_VIRULENCE, SLOW_SPREAD, Table.make(
        COGNITION, -5, MOTOR, -5, PERCEPT, -5, MUSCULAR, -5, HANDSOME, -5
      ),
      "Hirex Fruiting",
      "Hirex Infestation",
      "Hirex Gestation",
      null,
      "Hirex Immune"
    ),
    ALBEDAN_STRAIN = new Condition(
      MEDIUM_LATENCY, EXTREME_VIRULENCE, SLOW_SPREAD, Table.make(
        INDULGENT, 2, IMMUNE, 5, PERCEPT, 5, MOTOR, -5
      ),
      "Albedan Strain",
      "Albedan Strain",
      "Albedan Strain",
      null,
      "Strain Immune"
    ),
    SILVERQUICK = new Condition(
      SHORT_LATENCY, EXTREME_VIRULENCE, RAPID_SPREAD, Table.make(
        IMPASSIVE, 5, IMMUNE, -20, MUSCULAR, -20
      ),
      "Silverquick Rictus",
      "Silverquick Scale",
      "Silverquick Taint",
      null,
      "Silverquick Immune"
    ),
    MOEBIUS_PLAGUE = new Condition(
      LONG_LATENCY, EXTREME_VIRULENCE, NO_SPREAD, Table.make(
        MOTOR, -20, MUSCULAR, -20
      ),
      "Moebius Sublimation",
      "Moebius Displacement",
      "Moebius Plague",
      null,
      "Plague Immune"
    ),
    
    SPONTANEOUS_DISEASE[] = {
      ILLNESS, CANCER, HIREX_PARASITE
    },
    TREATABLE_CONDITIONS[] = {
      INJURY, POOR_MORALE, POISONED,
      ILLNESS, SOMA_HAZE, SPICE_ADDICTION,
      CANCER, RAGE_INFECTION, HIREX_PARASITE,
      ALBEDAN_STRAIN, SILVERQUICK, MOEBIUS_PLAGUE
    } ;
  final public static Trait CONDITIONS[] = Trait.traitsSoFar() ;
  
  
  final public static Trait
    KINESTHESIA_EFFECT = new Condition(
      NO_LATENCY, NO_VIRULENCE, NO_SPREAD, Table.make(
        MOTOR, 10, HAND_TO_HAND, 10, MARKSMANSHIP, 10, ATHLETICS, 10
      ),
      "Kinesthesia", "Kinesthesia", "Kinesthesia", null
    ) {
      public void affect(Actor a) {
        super.affect(a) ;
        a.world().ephemera.updateGhost(a, 1, Power.KINESTHESIA_FX_MODEL, 1) ;
      }
    },
    SUSPENSION_EFFECT = new Condition(
      NO_LATENCY, NO_VIRULENCE, NO_SPREAD, Table.make(),
      "Suspension", "Suspension", "Suspension", null
    ) {
      public void affect(Actor a) {
        super.affect(a) ;
        if (a.traits.useLevel(this) <= 0) {
          a.health.setState(ActorHealth.STATE_RESTING) ;
        }
        else {
          a.health.liftInjury(0.1f) ;
          a.world().ephemera.updateGhost(a, 1, Power.SUSPENSION_FX_MODEL, 1) ;
        }
      }
    },
    SPICE_VISION_EFFECT = new Condition(
     SHORT_LATENCY, NO_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, 10, COGNITION, 5, PERCEPT, 5, NERVE, 5
      ),
      "Spice Vision", "Spice Vision", "Spice Vision", null
    ) {
      public void affect(Actor a) {
        super.affect(a) ;
        if (a.traits.traitLevel(SPICE_ADDICTION) <= 0) {
          a.traits.incLevel(SPICE_ADDICTION, Rand.num() / 10f) ;
        }
      }
    },
    EFFECTS[] = Trait.traitsSoFar() ;
  
  
  final public static Trait
    ALL_TRAIT_TYPES[] = Trait.from(Trait.allTraits) ;
}


/*
//
//  TODO:  Put these in a separate class, so you can concisely describe their
//  effects.
final public static Trait
PSYONIC        = new Trait(PHYSICAL, "Psyonic"       ),
REGENERATIVE   = new Trait(PHYSICAL, "Regenerative"  ),
SUPERCOGNITIVE = new Trait(PHYSICAL, "Supercognitive"),
JUMPER         = new Trait(PHYSICAL, "Jumper"        ),
HYPERPHYSICAL  = new Trait(PHYSICAL, "Hyperphysical" ),
CHAMELEON      = new Trait(PHYSICAL, "Chameleon"     ),
ULTRASENSITIVE = new Trait(PHYSICAL, "Ultrasensitive"),
VENOMOUS       = new Trait(PHYSICAL, "Venomous"      ),
PHASE_SHIFTER  = new Trait(PHYSICAL, "Phase Shifter" ),
GILLED         = new Trait(PHYSICAL, "Gilled"        ),
FOUR_ARMED     = new Trait(PHYSICAL, "Four Armed"    ),
ODD_COLOUR     = new Trait(PHYSICAL, "Odd Colour"    ),
ECCENTRIC      = new Trait(PHYSICAL, "Eccentric"     ),
STERILE        = new Trait(PHYSICAL, "Sterile"       ),
FURRED         = new Trait(PHYSICAL, "Furred"        ),
SCALY          = new Trait(PHYSICAL, "Scaly"         ),
SICKLY         = new Trait(PHYSICAL, "Sickly"        ),
DISTURBED      = new Trait(PHYSICAL, "Disturbed"     ),
DEFORMED       = new Trait(PHYSICAL, "Deformed"      ),
LEPROUS        = new Trait(PHYSICAL, "Leprous"       ),
NULL_EMPATH    = new Trait(PHYSICAL, "Null Empath"   ),
ATAVIST        = new Trait(PHYSICAL, "Atavist"       ),
ABOMINATION    = new Trait(PHYSICAL, "Abomination"   ),
MUTANT_TRAITS[] = Trait.traitsSoFar() ;
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


    PRIME_DIRECTIVES    = new Trait("Prime Directives"   , Type.SUPERNORMAL),
    ARTILECT            = new Trait("Artilect"           , Type.SUPERNORMAL),
    SILICATE_METABOLISM = new Trait("Silicate Metabolism", Type.SUPERNORMAL),
    MINDLESS            = new Trait("Mindless"           , Type.SUPERNORMAL),
    ANCIENT             = new Trait("Ancient"            , Type.SUPERNORMAL),
    HUMANOID            = new Trait("Humanoid"           , Type.INNATE     ),
    INSECTILE           = new Trait("Insectile"          , Type.SUPERNORMAL),
    PLANT_METABOLISM    = new Trait("Plant Metabolism"   , Type.SUPERNORMAL),
    IMMOBILE            = new Trait("Immobile"           , Type.SUPERNORMAL),
    XENOMORPH           = new Trait("Xenomorph"          , Type.SUPERNORMAL),
    AMORPHOUS           = new Trait("Amorphous"          , Type.SUPERNORMAL),
    MELDED              = new Trait("Melded"             , Type.SUPERNORMAL),
    PART_CYBORG         = new Trait("Part Cyborg"        , Type.ACQUIRED   ),
    FULL_CYBORG         = new Trait("Full Cyborg"        , Type.ACQUIRED   ),
    FAST_METABOLISM     = new Trait("Fast Metabolism"    , Type.INNATE     ),
    LONG_LIVED          = new Trait("Long Lived"         , Type.INNATE     ),
    IMMORTAL            = new Trait("Immortal"           , Type.SUPERNORMAL),
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
  NERVOUS = new Trait(PERSONALITY,
    "Cowardly",
    "Nervous",
    "Cautious",
    null,
    "Assertive",
    "Fearless",
    "Reckless"
  ),
  AGGRESSIVE = new Trait(PERSONALITY,
    "Vengeful",
    "Aggressive",
    "Defensive",
    null,
    "Calm",
    "Gentle",
    "Pacifist"
  ),
  FRIENDLY = new Trait(PERSONALITY,
    "Fawning",
    "Complimentary",
    "Friendly",
    null,
    "Reserved",
    "Critical",
    "Caustic"
  ),
  OPTIMISTIC = new Trait(PERSONALITY,
    "Blithe",
    "Optimistic",
    "Cheerful",
    null,
    "Skeptical",
    "Pessimistic",
    "Morose"
  ),
  DEBAUCHED = new Trait(PERSONALITY,
    "Debauched",
    "Lusty",
    "Fun",
    null,
    "Temperate",
    "Abstinent",
    "Ascetic"
  ),
  APPETITE = new Trait(PERSONALITY,
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
  STUBBORN = new Trait(PERSONALITY,
    "Obstinate",
    "Stubborn",
    "Persistent",
    null,
    "Spontaneous",
    "Impulsive",
    "Fickle"
  ),
  INQUISITIVE = new Trait(PERSONALITY,
    "Insatiably Curious",
    "Inquisitive",
    "Curious",
    null,
    "Stolid",
    "Disinterested",
    "Dull"
  ),
  SOCIABLE = new Trait(PERSONALITY,
    "Gregarious",
    "Sociable",
    "Open",
    null,
    "Private",
    "Solitary",
    "Withdrawn"
  ),
  DUTIFUL = new Trait(PERSONALITY,
    "Obedient",
    "Dutiful",
    "Respectful of Betters",
    null,
    "Independant",
    "Rebellious",
    "Anarchic"
  ),
  IMPASSIVE = new Trait(PERSONALITY,
    "Emotionless",
    "Impassive",
    "Rational",
    null,
    "Passionate",
    "Excitable",
    "Manic"
  ),
  INDOLENT = new Trait(PERSONALITY,
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
  TRADITIONAL = new Trait(PERSONALITY,
    "Hidebound",
    "Traditional",
    "Old-fashioned",
    null,
    "Reformist",
    "Radical",
    "Subversive"
  ),
  NATURALIST = new Trait(PERSONALITY,
    "Gone Feral",
    "Ecophile",
    "Naturalist",
    null,
    "Urbanist",
    "Industrialist",
    "Antiseptic"
  ),
  ACQUISITIVE = new Trait(PERSONALITY,
    "Avaricious",
    "Acquisitive",
    "Thrifty",
    null,
    "Generous",
    "Extravagant",
    "Profligate"
  ),
  AMBITIOUS = new Trait(PERSONALITY,
    "Narcissist",
    "Ambitious",
    "Proud",
    null,
    "Modest",
    "Humble",
    "Complacent"
  ),
  HONOURABLE = new Trait(PERSONALITY,
    "Unimpeachable",
    "Honourable",
    "Trustworthy",
    null,
    "Sly",
    "Dishonest",
    "Manipulative"
  ),
  EMPATHIC = new Trait(PERSONALITY,
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
  


