/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.politic.Power;
import stratos.util.*;



public interface Qualities {
  
  
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
  
  final static int
    FORM_NATURAL   = 0,
    FORM_PHYSICAL  = 1,
    FORM_SENSITIVE = 2,
    FORM_COGNITIVE = 3,
    FORM_PSYONIC   = 4,
    FORM_INSTINCT  = 5;
  
  
  final public static Skill
    IMMUNE    = new Skill("Immune"   , FORM_NATURAL, null),
    MUSCULAR  = new Skill("Muscular" , FORM_NATURAL, null),
    MOTOR     = new Skill("Motor"    , FORM_NATURAL, null),
    PERCEPT   = new Skill("Percept"  , FORM_NATURAL, null),
    COGNITION = new Skill("Cognition", FORM_NATURAL, null),
    NERVE     = new Skill("Nerve"    , FORM_NATURAL, null),
    ATTRIBUTES[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  
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
    
    INSTINCT_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill
    //
    //  Artifice-related skills:
    ASSEMBLY       = new Skill("Assembly"      , FORM_COGNITIVE, COGNITION),
    CHEMISTRY      = new Skill("Chemistry"     , FORM_COGNITIVE, COGNITION),
    INSCRIPTION    = new Skill("Inscription"   , FORM_COGNITIVE, COGNITION),
    FIELD_THEORY   = new Skill("Field Theory"  , FORM_COGNITIVE, COGNITION),
    ASTROGATION    = new Skill("Astrogation"   , FORM_COGNITIVE, COGNITION),
    SIMULACRA      = new Skill("Simulacra"     , FORM_COGNITIVE, COGNITION),
    ARTIFICER_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Ecology-related skills:
    XENOZOOLOGY    = new Skill("Xenozoology"   , FORM_COGNITIVE, COGNITION),
    CULTIVATION    = new Skill("Cultivation"   , FORM_COGNITIVE, COGNITION),
    GEOPHYSICS     = new Skill("Geophysics"    , FORM_COGNITIVE, COGNITION),
    CETANI_ECOLOGY = new Skill("Cetani Ecology", FORM_COGNITIVE, COGNITION),
    ALBEDO_ECOLOGY = new Skill("Albedo Ecology", FORM_COGNITIVE, COGNITION),
    SILICO_ECOLOGY = new Skill("Silico Ecology", FORM_COGNITIVE, COGNITION),
    ECOLOGIST_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Physician-related skills:
    PHARMACY       = new Skill("Pharmacy"      , FORM_COGNITIVE, COGNITION),
    GENE_CULTURE   = new Skill("Gene Culture"  , FORM_COGNITIVE, COGNITION),
    ANATOMY        = new Skill("Anatomy"       , FORM_COGNITIVE, COGNITION),
    PSYCHOANALYSIS = new Skill("Psychoanalysis", FORM_COGNITIVE, COGNITION),
    FORENSICS      = new Skill("Forensics"     , FORM_COGNITIVE, COGNITION),
    SOCIAL_HISTORY = new Skill("Social History", FORM_COGNITIVE, COGNITION),
    PHYSICIAN_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    //
    //  Research and governance:
    BATTLE_TACTICS = new Skill("Battle Tactics", FORM_COGNITIVE, COGNITION),
    ACCOUNTING     = new Skill("Accounting"    , FORM_COGNITIVE, COGNITION),
    ANCIENT_LORE   = new Skill("Ancient Lore"  , FORM_COGNITIVE, COGNITION),
    LEGISLATION    = new Skill("Legislation"   , FORM_COGNITIVE, COGNITION),
    ADMIN_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class),
    
    COGNITIVE_SKILLS[] = (Skill[]) Visit.compose(
      Skill.class,
      ARTIFICER_SKILLS, ECOLOGIST_SKILLS, PHYSICIAN_SKILLS, ADMIN_SKILLS
    );
  
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
    
    SENSITIVE_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
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
    
    PHYSICAL_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill
    SUGGESTION   = new Skill("Suggestion"  , FORM_PSYONIC, NERVE),
    SYNESTHESIA  = new Skill("Synesthesia" , FORM_PSYONIC, NERVE),
    METABOLISM   = new Skill("Metabolism"  , FORM_PSYONIC, NERVE),
    TRANSDUCTION = new Skill("Transduction", FORM_PSYONIC, NERVE),
    PROJECTION   = new Skill("Projection"  , FORM_PSYONIC, NERVE),
    PREMONITION  = new Skill("Premonition" , FORM_PSYONIC, NERVE),
    
    PSYONIC_SKILLS[] = (Skill[]) Trait.TRAIT_INDEX.soFar(Skill.class);
  
  final public static Skill ALL_SKILLS[] = (Skill[]) Visit.compose(Skill.class,
    ATTRIBUTES, INSTINCT_SKILLS, PSYONIC_SKILLS,
    COGNITIVE_SKILLS, SENSITIVE_SKILLS, PHYSICAL_SKILLS
  );
  
  
  //  TODO:  I think only about half of these are needed.
  public static Trait
    DEFENSIVE  = new Trait("Defensive", PERSONALITY, "Defensive"),
    CRITICAL   = new Trait("Critical", PERSONALITY, "Critical"),
    NERVOUS    = new Trait("Nervous", PERSONALITY, "Nervous"),
    
    CALM       = new Trait("Calm", PERSONALITY, "Calm"),
    POSITIVE   = new Trait("Positive", PERSONALITY, "Positive"),
    FEARLESS   = new Trait("Fearless", PERSONALITY, "Fearless"),
    
    CRUEL       = new Trait("Cruel", PERSONALITY, "Cruel"),
    DISHONEST   = new Trait("Dishonest", PERSONALITY, "Dishonest"),
    ACQUISITIVE = new Trait("Acquisitive", PERSONALITY, "Acquisitive"),
    
    EMPATHIC    = new Trait("Empathic", PERSONALITY, "Empathic"),
    ETHICAL     = new Trait("Ethical", PERSONALITY, "Ethical"),
    GENEROUS    = new Trait("Generous", PERSONALITY, "Generous"),
    
    SUBVERSIVE  = new Trait("Subversive", PERSONALITY, "Subversive"),
    NATURALIST  = new Trait("Naturalist", PERSONALITY, "Naturalist"),
    INDULGENT   = new Trait("Indulgent", PERSONALITY, "Indulgent"),
    
    DUTIFUL     = new Trait("Dutiful", PERSONALITY, "Dutiful"),
    URBANE      = new Trait("Urbane", PERSONALITY, "Urbane"),
    ABSTINENT   = new Trait("Abstinent", PERSONALITY, "Abstinent"),
    
    CREATIVE    = new Trait("Creative", PERSONALITY, "Creative"),
    CURIOUS     = new Trait("Curious", PERSONALITY, "Curious"),
    IMPULSIVE   = new Trait("Impulsive", PERSONALITY, "Impulsive"),
    
    TRADITIONAL = new Trait("Traditional", PERSONALITY, "Traditional"),
    IGNORANT    = new Trait("Ignorant", PERSONALITY, "Ignorant"),
    STUBBORN    = new Trait("Stubborn", PERSONALITY, "Stubborn"),
    
    AMBITIOUS   = new Trait("Ambitious", PERSONALITY, "Ambitious"),
    ENERGETIC   = new Trait("Energetic", PERSONALITY, "Energetic"),
    OUTGOING    = new Trait("Outgoing", PERSONALITY, "Outgoing"),
    
    HUMBLE      = new Trait("Humble", PERSONALITY, "Humble"),
    RELAXED     = new Trait("Relaxed", PERSONALITY, "Relaxed"),
    SOLITARY    = new Trait("Solitary", PERSONALITY, "Solitary"),
    
    EXCITABLE   = new Trait("Excitable", PERSONALITY, "Excitable"),
    IMPASSIVE   = new Trait("Impassive", PERSONALITY, "Impassive"),
    
    PERSONALITY_TRAITS[] = Personality.setupRelations(
      Trait.TRAIT_INDEX.soFar(Trait.class)
    ),
    
    //
    //  These are the listings for physical traits.  Physical traits are
    //  determined at birth and cannot be modified (except perhaps surgically),
    //  but do wax and wane based on aging, in a fashion similar to basic
    //  attributes.  TODO:  Implement that.
    
    FEMININE = new Trait("Sex Traits", PHYSICAL,
      "Busty",
      "Curvy",
      "Gamine",
      null,
      "Boyish",
      "Bearded",
      "Hirsute"
    ),
    HANDSOME = new Trait("Appearance", PHYSICAL,
      "Stunning",
      "Beautiful",
      "Handsome",
      null,
      "Plain",
      "Ugly",
      "Hideous"
    ),
    TALL = new Trait("Height", PHYSICAL,
      "Towering",
      "Big",
      "Tall",
      null,
      "Short",
      "Small",
      "Diminutive"
    ),
    STOUT = new Trait("Stoutness", PHYSICAL,
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
    ORIENTATION = new Trait("Orientation", CATEGORIC,
      "Heterosexual",
      "Bisexual",
      "Homosexual",
      null
    ),
    GENDER_MALE   = new Trait("Male"  , CATEGORIC, "Male"  ),
    GENDER_FEMALE = new Trait("Female", CATEGORIC, "Female"),
    
    DESERT_BLOOD = new Trait("Desert Blood", CATEGORIC,
      "Desert Blood", // "Desertborn", "Dark"
      null
    ),
    TUNDRA_BLOOD = new Trait("Tundra Blood", CATEGORIC,
      "Tundra Blood", // "Tundraborn", "Sallow"
      null
    ),
    FOREST_BLOOD = new Trait("Forest Blood", CATEGORIC,
      "Forest Blood", //  "Forestborn", "Tan"
      null
    ),
    WASTES_BLOOD = new Trait("Wastes Blood", CATEGORIC,
      "Wastes Blood", //  "Wastesborn", "Pale"
      null
    ),
    MUTATION = new Trait("Mutation", CATEGORIC,
      "Major Mutation",
      "Minor Mutation",
      "Nominal Mutation",
      null
    ),
    PSYONIC = new Trait("Psyonic", CATEGORIC,
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
  
  
  //  TODO:  Move these to the individual Powers classes, (or subsume into the
  //  Technique interfaces.)
  
  final public static Trait
    KINESTHESIA_EFFECT = new Condition(
      "Kinesthesia Effect", Table.make(
        MOTOR, 10, HAND_TO_HAND, 10, MARKSMANSHIP, 10, ATHLETICS, 10
      ),
      "Kinesthesia", "Kinesthesia", "Kinesthesia", null
    ) {
      public void affect(Actor a) {
        super.affect(a);
        a.world().ephemera.updateGhost(a, 1, Power.KINESTHESIA_FX_MODEL, 1);
      }
    },
    SUSPENSION_EFFECT = new Condition(
      "Suspension Effect", Table.make(),
      "Suspension", "Suspension", "Suspension", null
    ) {
      public void affect(Actor a) {
        super.affect(a);
        if (a.traits.usedLevel(this) <= 0) {
          a.health.setState(ActorHealth.STATE_RESTING);
        }
        else {
          a.health.liftInjury(0.1f);
          a.world().ephemera.updateGhost(a, 1, Power.SUSPENSION_FX_MODEL, 1);
        }
      }
    },
    SPICE_VISION_EFFECT = new Condition(
      "Spice Vision Effect", Table.make(
        IMMUNE, 10, COGNITION, 5, PERCEPT, 5, NERVE, 5
      ),
      "Spice Vision", "Spice Vision", "Spice Vision", null
    ) {
      public void affect(Actor a) {
        super.affect(a);
        if (a.traits.traitLevel(Conditions.SPICE_ADDICTION) <= 0) {
          a.traits.incLevel(Conditions.SPICE_ADDICTION, Rand.num() / 10f);
        }
      }
    },
    EFFECTS[] = Trait.TRAIT_INDEX.soFar(Trait.class);
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
  


