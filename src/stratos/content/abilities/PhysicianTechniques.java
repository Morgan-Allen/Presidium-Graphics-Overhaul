/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.common.*;
import stratos.game.economic.Traded;
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
    PAX_FX_MODEL = PlaneFX.animatedModel(
      "pax_9_fx", BASE_CLASS,
      FX_DIR+"pax_9_fx.png",
      4, 1, 4, 1, 0.25f
    );
  
  final static float
    MEDICINE_USE = 0.1f,
    
    SPRAY_HEAL_INIT_MIN = 2,
    SPRAY_HEAL_INIT_MAX = 5,
    SPRAY_DURATION      = Stage.STANDARD_SHIFT_LENGTH,
    SPRAY_DURATION_HEAL = 2;
  
  
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
      Actor actor, Plan current, Skill used, Target subject
    ) {
      if (! (current instanceof Treatment)  ) return false;
      if (actor.gear.amountOf(MEDICINE) <= 0) return false;
      final Actor treats = (Actor) subject;
      if (treats.traits.hasTrait(asCondition)) return false;
      return super.triggersPassive(actor, current, used, subject);
    }
    
    
    public Traded itemNeeded() {
      return MEDICINE;
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return 5;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (! (subject instanceof Actor)) return;
      
      using.gear.bumpItem(MEDICINE, 0 - MEDICINE_USE);
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
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      
      using.gear.bumpItem(MEDICINE, 0 - MEDICINE_USE);
      final Actor affects = (Actor) subject;
      affects.traits.setLevel(asCondition, 1);
      
      ActionFX.applyBurstFX(BOOST_FX_MODEL, using, 1.5f, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      float statUp = Nums.min(1, affected.traits.traitLevel(asCondition));
      affected.traits.incBonus(IMMUNE   , 5 * statUp);
      affected.traits.incBonus(MOTOR    , 5 * statUp);
      affected.traits.incBonus(COGNITION, 5 * statUp);
      
      float fatInc = 1f / conditionDuration();
      affected.health.liftFatigue(5 * fatInc);
    }
  };
  
  
  //  Reduces hostilities and increases suggestibility.
  final public static Technique PAX_9 = new Technique(
    "Pax 9", UI_DIR+"pax_9.png",
    "",
    BASE_CLASS, "pax_9",
    MAJOR_POWER         ,
    MILD_HARM           ,
    MINOR_FATIGUE       ,
    MEDIUM_CONCENTRATION,
    IS_ANY_TARGETING | IS_TRAINED_ONLY, PHARMACY, 15,
    Action.FIRE, Action.QUICK | Action.RANGED
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (actor.gear.amountOf(MEDICINE) <= 0) return false;
      return actor.senses.isEmergency() && subject instanceof Actor;
    }
    
    
    public Traded itemNeeded() {
      return MEDICINE;
    }
    
    
    public float basePriority(Actor actor, Plan current, Target subject) {
      final Actor a = (Actor) subject;
      if (! a.isDoing(Combat.class, null)) return -1;
      if (! a.health.organic()           ) return -1;
      if (a.traits.hasTrait(asCondition) ) return -1;
      return super.basePriority(actor, current, subject);
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      final Actor affects = (Actor) subject;
      final Action action = Technique.currentTechniqueBy(actor);
      return actor.skills.test(PHARMACY, affects, NERVE, 0, 10, action);
    }


    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      using.gear.bumpItem(MEDICINE, 0 - MEDICINE_USE);
      if (! success) return;
      
      final Actor affects = (Actor) subject;
      affects.enterStateKO(Action.FALL);
      affects.mind.clearAgenda();
      affects.relations.incRelation(using, 0.5f, 0.1f, 0.2f);
      affects.traits.setLevel(asCondition, 1);
      SenseUtils.breaksPursuit(affects, null);
      
      ActionFX.applyBurstFX(PAX_FX_MODEL, using, 1.5f, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      affected.traits.incBonus(NERVE    , -5   );
      affected.traits.incBonus(PERCEPT  , -5   );
      affected.traits.incBonus(DEFENSIVE, -0.5f);
      affected.traits.incBonus(EMPATHIC ,  0.5f);
    }
  };
  
  
  //  Prevents all directly harmful actions by self, but confers near-immunity
  //  to suggestion or intimidation and a minor bonus to premonition and
  //  concentration.
  final public static Technique PSY_INHIBITION = new Technique(
    "Psy Inhibition", UI_DIR+"psy_inhibition.png",
    "",
    BASE_CLASS, "psy_inhibition",
    MAJOR_POWER         ,
    NO_HARM             ,
    NO_FATIGUE          ,
    NO_CONCENTRATION    ,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, null, -1, null
  ) {
  };
  
  
  final public static Technique PHYSICIAN_TECHNIQUES[] = {
    HYPO_SPRAY, BOOSTER_SHOT, PAX_9, PSY_INHIBITION
  };
}














