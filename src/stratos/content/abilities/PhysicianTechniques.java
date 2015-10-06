/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.sfx.*;
import stratos.graphics.sfx.PlaneFX.Model;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.content.civic.PhysicianStation;



public class PhysicianTechniques {
  
  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = PhysicianTechniques.class;
  
  final public static PlaneFX.Model
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
    MEDICINE_USE = 0.1f;
  
  
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
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      return super.canBeLearnt(learns, trained);
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (! (current instanceof Treatment)  ) return false;
      if (actor.gear.amountOf(MEDICINE) <= 0) return false;
      final Actor treats = (Actor) action.subject();
      if (treats.traits.traitLevel(asCondition) > 0) return false;
      return super.triggeredBy(actor, current, action, used, passive);
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
      treats.health.liftInjury(2 + Rand.index(4));
      
      ActionFX.applyBurstFX(HYPO_FX_MODEL, using, 1.5f, 1);
    }
    
    
    protected float conditionDuration() {
      return Stage.STANDARD_SHIFT_LENGTH;
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
    IS_INDEPENDANT_ACTION | IS_TRAINED_ONLY, PHARMACY, 10,
    Action.FIRE, Action.QUICK
  ) {
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      return super.canBeLearnt(learns, trained);
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (passive || actor.gear.amountOf(MEDICINE) <= 0) return false;
      if (current instanceof Treatment) return true;
      if (actor.senses.isEmergency()  ) return true;
      return false;
    }
    
    
    protected Action createActionFor(Plan parent, Actor actor, Target subject) {
      final Pick <Actor> pick = new Pick(0);
      
      if (parent instanceof Treatment) {
        final float rating = parent.priorityFor(actor);
        pick.compare((Actor) parent.subject, rating);
      }
      else for (Target t : actor.senses.awareOf()) if (t instanceof Actor) {
        final Actor a = (Actor) t;
        if (! a.isDoing(Combat.class, null)) continue;
        if (a.traits.hasTrait(asCondition) ) continue;
        if (a.senses.underAttack()         ) continue;
        final float rating = 0 - PlanUtils.combatPriority(
          actor, t, 0, 1, false, Plan.REAL_HARM
        );
        pick.compare(a, rating);
      }
      if (pick.empty()) return null;
      
      final Action action = super.createActionFor(parent, actor, pick.result());
      action.setPriority(pick.bestRating());
      return action;
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
    IS_INDEPENDANT_ACTION | IS_TRAINED_ONLY, PHARMACY, 15,
    Action.FIRE, Action.QUICK | Action.RANGED
  ) {
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      return super.canBeLearnt(learns, trained);
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (passive || actor.gear.amountOf(MEDICINE) <= 0) return false;
      if (actor.senses.isEmergency()) return true;
      return false;
    }
    
    
    public float basePriority(Actor actor, Target subject, float harmWanted) {
      return super.basePriority(actor, subject, Plan.MILD_HARM);
    }
    
    
    protected Action createActionFor(Plan parent, Actor actor, Target subject) {
      final Pick <Actor> pick = new Pick(0);
      
      for (Target t : actor.senses.awareOf()) if (t instanceof Actor) {
        final Actor a = (Actor) t;
        if (! a.isDoing(Combat.class, null)) continue;
        if (! a.health.organic()           ) continue;
        if (a.traits.hasTrait(asCondition) ) continue;
        final float rating = PlanUtils.combatPriority(
          actor, t, 0, 1, false, Plan.MILD_HARM
        );
        pick.compare(a, rating);
      }
      if (pick.empty()) return null;
      
      final Action action = super.createActionFor(parent, actor, pick.result());
      action.setPriority(pick.bestRating());
      return action;
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
  
  
  //
  //  TODO:  Fill in FX here (and descriptions for others!)
  
  //  Prevents all directly harmful actions by self, but confers near-immunity
  //  to suggestion or intimidation-based effects.
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
}














