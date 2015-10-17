/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.content.civic.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;




/*
  Solo Run         (stealth 5.)
  First Shot       (stealth 10, marksmanship 5.)
  
  Tracer Slug      (marksmanship 10, stealth 5, sniper kit/training.)
  Alias            (masquerade 10, camo suit/training.)
  Overload         (inscription 5, jak terminal/training.)
  
  Slow Burn     & Fast Toxin     (IV Punks)
  Nip/Tuck      & Gene Roulette  (Ladder Snakes)
  Cyberkinetics & AI Assist      (Jak Blacks)
//*/

public class RunnerTechniques {
  
  
  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = RunnerTechniques.class;
  
  
  //  Slow burn.  Fast Toxin.  
  //  Tracer Bead.  Overload.
  //  Solo run.  First shot.  (general abilities.)
  //
  //  Alias and Nip/Tuck are going to require undercover ops, which I'm not
  //  ready for yet.
  
  
  //  TODO:  Actual usage!
  
  final public static Technique OVERLOAD = new Technique(
    "Sniper Kit", UI_DIR+"sniper_kit.png",
    "Deals devastating damage when granted the benefit of surprise.",
    BASE_CLASS, "sniper kit",
    MINOR_POWER         ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MAJOR_CONCENTRATION ,
    IS_INDEPENDANT_ACTION | IS_TRAINED_ONLY, null, 0,
    Action.FIRE, Action.RANGED
  ) {
  };
  
  final public static Technique SNIPER_KIT = new Technique(
    "Sniper Kit", UI_DIR+"sniper_kit.png",
    "Deals devastating damage when granted the benefit of surprise.",
    BASE_CLASS, "sniper kit",
    MEDIUM_POWER        ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MAJOR_CONCENTRATION ,
    IS_INDEPENDANT_ACTION | IS_TRAINED_ONLY, null, 0,
    SURVEILLANCE
  ) {
  };
  
  
  
  final public static Technique FAST_TOXIN = new Technique(
    "Fast Toxin", UI_DIR+"fast_toxin.png",
    "A fast-acting poison suitable for application to melee or kinetic "+
    "weaponry.",
    BASE_CLASS, "fast_toxin",
    MEDIUM_POWER        ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_INDEPENDANT_ACTION | IS_GAINED_FROM_ITEM, null, 0,
    Action.FIRE, Action.RANGED
  ) {
    
  };
  
  final public static UsedItemType FAST_TOXIN_ITEM = new UsedItemType(
    BASE_CLASS, "Fast Toxin", null,
    40, FAST_TOXIN.description,
    FAST_TOXIN, RunnerMarket.class,
    1, REAGENTS, MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
  ) {
    
    public float useRating(Actor actor) {
      if (! PlanUtils.isArmed(actor)) return -1;
      return 1.5f - actor.traits.relativeLevel(ETHICAL);
    }
    
    public int normalCarry(Actor actor) {
      return 1;
    }
  };
  
  
  final public static Technique SLOW_BURN = new Technique(
    "Slow Burn", UI_DIR+"slow_burn.png",
    "Slows the user's perception of time, allowing for faster reactions and "+
    "effective concentration.",
    BASE_CLASS, "slow_burn",
    MEDIUM_POWER        ,
    REAL_HELP           ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_INDEPENDANT_ACTION | IS_GAINED_FROM_ITEM, null, 0,
    Action.STRIKE_BIG, Action.NORMAL
  ) {
    
  };
  
  final public static UsedItemType SLOW_BURN_ITEM = new UsedItemType(
    BASE_CLASS, "Slow Burn", null,
    40, SLOW_BURN.description,
    SLOW_BURN, RunnerMarket.class,
    1, REAGENTS, MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
  ) {
    
    public float useRating(Actor actor) {
      if (! PlanUtils.isArmed(actor)) return -1;
      return 1.5f - actor.traits.relativeLevel(ETHICAL);
    }
    
    public int normalCarry(Actor actor) {
      return 1;
    }
  };
}




//  TODO:  Restore some of these:

/*
final static Traded
  NEURAL_IMPLANT = new Traded(
    RunnerMarket.class, "Neural Implant", null, Economy.FORM_USED_ITEM, 100,
    "Neural implants boost cognitive ability and may allow hacking of "+
    "simple drones and cybrids."
  ),
  KINETIC_IMPLANT = new Traded(
    RunnerMarket.class, "Kinetic Implant", null, Economy.FORM_USED_ITEM, 120,
    "Kinetic implants boost strength and dexterity, together with a degree "+
    "of natural armour."
  ),
  SIMSTIMS = new Traded(
    RunnerMarket.class, "Simstims", null, Economy.FORM_SPECIAL, 45,
    "Simstims provide voyeuristic virtual entertainment to the masses."
  ),
  
  FAST_TOXIN = new Traded(
    RunnerMarket.class, "Fast Toxin", null, Economy.FORM_USED_ITEM, 85,
    "A fast-acting poison suitable for application to melee or kinetic "+
    "weaponry."
  ),
  SLOW_BURN = new Traded(
    RunnerMarket.class, "Slow Burn", null, Economy.FORM_USED_ITEM, 55,
    "An addictive narcotic that greatly accelerates reaction times and "+
    "perception."
  ),
  
  //  Disguise, beauty, and cognitive/sensitive/physical DNA treatments are
  //  reserved for the Hudzin Baru.
  
  //  TODO:  Maybe these should be special abilities for the runner class?
  //  Yes.  Work that out.
  
  SNIPER_KIT = new Traded(
    RunnerMarket.class, "Sniper Kit", null, Economy.FORM_USED_ITEM, 90,
    "Allows ranged attacks at far greater distances, particularly if the "+
    "target is surprised."
  ),
  STICKY_BOMB = new Traded(
    RunnerMarket.class, "Sticky Bomb", null, Economy.FORM_USED_ITEM, 35,
    "Deals heavy damage to vehicles and buildings, if attached at point-"+
    "blank range."
  ),
  NIGHT_OPTICS = new Traded(
    RunnerMarket.class, "Night Optics", null, Economy.FORM_USED_ITEM, 25,
    "Allows extended sight range in space or nocturnal conditions."
  ),
  GHOST_CAMO = new Traded(
    RunnerMarket.class, "Ghost Camo", null, Economy.FORM_USED_ITEM, 40,
    "Improves stealth and cover in daytime or outdoor environments."
  );
//*/


    
    //  TODO:  Restore later?
    /*
    RUNNER_SILVERFISH = new Background(
      Backgrounds.class,
      "Runner (Silverfish)", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, SUASION, SURVEILLANCE, MASQUERADE,
      LEARNING, HAND_TO_HAND,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, METICULOUS,
      BLASTER, STEALTH_SUIT
    ),
    JACK_ARTIST = new Background(
      Backgrounds.class,
      "Jack Artist", "", "artificer_skin.gif", "artificer_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, INSCRIPTION, SIMULACRA,
      PRACTICED, ASSEMBLY, ACCOUNTING,
      LEARNING, SUASION, MASQUERADE,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, METICULOUS,
      OVERALLS
    ),
    ASSASSIN = null,
    //  TODO:  Assassin.
    
    RUNNER_IV_PUNKS = new Background(
      Backgrounds.class,
      "Runner (IV Punks)", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, HAND_TO_HAND, SURVEILLANCE, BATTLE_TACTICS,
      LEARNING, COMMAND, MASQUERADE, ANATOMY,
      OFTEN, ACQUISITIVE, SOMETIMES, DEFENSIVE, DISHONEST,
      BLASTER, STEALTH_SUIT
    ),
    STREET_COOK = new Background(
      Backgrounds.class,
      "Street Cook", "", "physician_skin.gif", "physician_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, CHEMISTRY, PHARMACY, PRACTICED, FORENSICS,
      LEARNING, ANATOMY, COUNSEL, TRUTH_SENSE,
      OVERALLS
    ),
    BRUISER = null,
    //  TODO:  Bruiser.
    
    RUNNER_HUDZENA = new Background(
      Backgrounds.class,
      "Runner (Hudzeena)", "", "runner_skin.gif", "runner_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, MASQUERADE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, SURVEILLANCE, SUASION,
      LEARNING, NATIVE_TABOO, XENOZOOLOGY,
      OFTEN, ACQUISITIVE, DISHONEST, SOMETIMES, NERVOUS, NATURALIST,
      BLASTER, STEALTH_SUIT
    ),
    FACE_FIXER = new Background(
      Backgrounds.class,
      "Face Fixer", "", "physician_skin.gif", "physician_portrait.png",
      CLASS_AGENT, NOT_A_GUILD,
      EXPERT, GENE_CULTURE, ANATOMY, PRACTICED, SUASION,
      LEARNING, GRAPHIC_DESIGN, HANDICRAFTS,
      OVERALLS
    ),
    ANONYMOUS = null,
    //  TODO:  Anonymous.
    //*/


