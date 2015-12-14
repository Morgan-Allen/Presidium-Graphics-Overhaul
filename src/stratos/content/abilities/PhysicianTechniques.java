/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.sfx.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;



public class PhysicianTechniques {
  
  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = PhysicianTechniques.class;
  
  final static PlaneFX.Model
    HYPO_FX_MODEL = PlaneFX.animatedModel(
      "hypo_spray_fx", BASE_CLASS,
      FX_DIR+"hypo_spray_fx.png",
      4, 1, 4, 1, 0.25f
    ),
    BOOST_FX_MODEL = PlaneFX.animatedModel(
      "booster_shot_fx", BASE_CLASS,
      FX_DIR+"booster_shot_fx.png",
      4, 1, 4, 1, 0.25f
    ),
    PAX_GAS_BURST_MODEL = PlaneFX.imageModel(
      "pax_gas_burst_fx", BASE_CLASS,
      FX_DIR+"pax_gas_burst.png",
      1, 0, 1, true, false
    );
  
  final static int
    MEDICINE_CHARGES    =  10,
    SPRAY_HEAL_INIT_MIN =  2,
    SPRAY_HEAL_INIT_MAX =  5,
    SPRAY_DURATION      =  Stage.STANDARD_SHIFT_LENGTH,
    SPRAY_DURATION_HEAL =  2,
    BOOST_STAT_BONUS    =  5,
    BOOST_FATIGUE_LIFT  =  5,
    GAS_STAT_PENALTY    = -5,
    GAS_FATIGUE_DAMAGE  =  5,
    GAS_DURATION        =  10,
    PAX_SOCIAL_BONUS    =  2,
    PAX_COMMAND_BONUS   =  5;
  
  
  final public static Technique HYPO_SPRAY = new Technique(
    "Hypo Spray", UI_DIR+"hypo_spray.png",
    "Stems bleeding and allows more rapid short-term treatment of injury.",
    BASE_CLASS, "hypo_spray",
    MINOR_POWER         ,
    REAL_HELP           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, PHARMACY, 5,
    ANATOMY
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (! (current instanceof Treatment)  ) return false;
      if (actor.gear.amountOf(MEDICINE) <= 0) return false;
      final Actor treats = (Actor) subject;
      if (treats.traits.hasTrait(asCondition)) return false;
      return super.triggersPassive(actor, current, used, subject, reactive);
    }
    
    
    public Traded itemNeeded() {
      return MEDICINE;
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return 5;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      if (! (subject instanceof Actor)) return;
      
      using.gear.bumpItem(MEDICINE, -1f / MEDICINE_CHARGES);
      final Actor treats = (Actor) subject;
      treats.traits.setLevel(asCondition, 1);
      treats.health.setBleeding(false);
      treats.health.liftInjury(roll(SPRAY_HEAL_INIT_MIN, SPRAY_HEAL_INIT_MAX));
      
      ActionFX.applyBurstFX(HYPO_FX_MODEL, using, 1.5f, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      float regen = SPRAY_DURATION_HEAL / SPRAY_DURATION;
      affected.health.liftInjury(regen);
    }
    
    
    protected float conditionDuration() {
      return SPRAY_DURATION;
    }
  };
  
  
  final public static Technique BOOSTER_SHOT = new Technique(
    "Booster Shot", UI_DIR+"booster_shot.png",
    "Temporarily increases the subject's "+IMMUNE+", "+MOTOR+" and "+
    COGNITION+" while lifting fatigue.",
    BASE_CLASS, "booster_shot",
    MEDIUM_POWER       ,
    REAL_HELP          ,
    MINOR_FATIGUE      ,
    MAJOR_CONCENTRATION,
    IS_ANY_TARGETING | IS_TRAINED_ONLY, PHARMACY, 10,
    Action.FIRE, Action.QUICK
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (actor.gear.amountOf(MEDICINE) <= 0) return false;
      if (current instanceof Treatment) return subject == current.subject();
      else return actor.senses.isEmergency();
    }
    
    
    public Traded itemNeeded() {
      return MEDICINE;
    }


    public float basePriority(Actor actor, Plan current, Target subject) {
      if (current instanceof Treatment) {
        return super.basePriority(actor, current, subject);
      }
      else {
        final Actor a = (Actor) subject;
        if (! a.isDoing(Combat.class, null)) return -1;
        if (a.traits.hasTrait(asCondition) ) return -1;
        if (a.senses.underAttack()         ) return -1;
        final float rating = 0 - PlanUtils.combatPriority(
          actor, subject, 0, 1, false, Plan.REAL_HARM
        );
        return rating;
      }
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      using.gear.bumpItem(MEDICINE, -1f / MEDICINE_CHARGES);
      final Actor affects = (Actor) subject;
      affects.traits.setLevel(asCondition, 1);
      
      ActionFX.applyBurstFX(BOOST_FX_MODEL, using, 1.5f, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      float statUp = Nums.min(1, affected.traits.traitLevel(asCondition));
      affected.traits.incBonus(IMMUNE   , BOOST_STAT_BONUS * statUp);
      affected.traits.incBonus(MOTOR    , BOOST_STAT_BONUS * statUp);
      affected.traits.incBonus(COGNITION, BOOST_STAT_BONUS * statUp);
      
      float fatInc = 1f / conditionDuration();
      affected.health.liftFatigue(BOOST_FATIGUE_LIFT * fatInc);
    }
  };
  
  
  final public static Technique PAX_9 = new Technique(
    "Pax 9", UI_DIR+"pax_9.png",
    "Temporarily stuns and dazes a hostile enemy.",
    BASE_CLASS, "pax_9",
    MAJOR_POWER        ,
    MILD_HARM          ,
    MINOR_FATIGUE      ,
    MAJOR_CONCENTRATION,
    IS_ANY_TARGETING | IS_TRAINED_ONLY, PHARMACY, 15,
    Action.FIRE, Action.QUICK | Action.RANGED
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (actor.gear.amountOf(MEDICINE) <= 0) return false;
      if (! (subject instanceof Actor)      ) return false;
      final Actor a = (Actor) subject;
      if (! a.isDoing(Combat.class, null)) return false;
      if (! a.health.organic()           ) return false;
      if (a.traits.hasTrait(asCondition) ) return false;
      return true;
    }
    
    
    public Traded itemNeeded() {
      return MEDICINE;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      final Actor affects = (Actor) subject;
      final Action action = Technique.currentTechniqueBy(actor);
      return actor.skills.test(PHARMACY, affects, NERVE, 0, 10, action);
    }


    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      using.gear.bumpItem(MEDICINE, -1f / MEDICINE_CHARGES);
      if (! success) return;
      
      final Actor affects = (Actor) subject;
      affects.traits.setLevel(asCondition, 1);
      ActionFX.applyBurstFX(PAX_GAS_BURST_MODEL, subject, 0, 1);
    }
    
    
    protected float conditionDuration() {
      return GAS_DURATION;
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      affected.traits.incBonus(HAND_TO_HAND, GAS_STAT_PENALTY);
      affected.traits.incBonus(MARKSMANSHIP, GAS_STAT_PENALTY);
      affected.traits.incBonus(ATHLETICS   , GAS_STAT_PENALTY);
      
      float fatigue = GAS_FATIGUE_DAMAGE / conditionDuration();
      affected.health.takeFatigue(fatigue);
    }
  };
  
  
  
  //  TODO:  Replace with Pax Conditioning, which inhibits harmful actions but
  //  also provides a bonus to most persuasion/plea attempts.
  
  final public static Technique PAX_CONDITIONING = new Technique(
    "Pax Conditioning", UI_DIR+"pax_conditioning.png",
    "Conditions the physician against hostile or unethical behaviour, helping "+
    "to instil greater trust in both friend and foe.",
    BASE_CLASS, "pax_conditioning",
    MINOR_POWER     ,
    NO_HARM         ,
    NO_FATIGUE      ,
    NO_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_PASSIVE_ALWAYS | IS_TRAINED_ONLY, null, -1, null
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (! (current instanceof Dialogue)) return false;
      return used == COMMAND || used == SUASION || used == COUNSEL;
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      if (skill == COMMAND) return PAX_COMMAND_BONUS;
      else                  return PAX_SOCIAL_BONUS ;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      affected.traits.incBonus(EMPATHIC , EMPATHIC .maxVal / 2f);
      affected.traits.incBonus(ETHICAL  , ETHICAL  .maxVal / 2f);
      affected.traits.incBonus(DEFENSIVE, DEFENSIVE.minVal / 2f);
      super.applyAsCondition(affected);
    }
  };
  
  
  final public static Technique PHYSICIAN_TECHNIQUES[] = {
    HYPO_SPRAY, BOOSTER_SHOT, PAX_9, PAX_CONDITIONING
  };
}






