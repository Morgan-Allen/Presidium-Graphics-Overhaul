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
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.content.civic.PhysicianStation;



public class PhysicianTechniques {
  
  final static String DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = PhysicianTechniques.class;
  
  
  final public static Technique HYPO_SPRAY = new Technique(
    "Hypo Spray", DIR+"hypo_spray.png",
    "",
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
      if (! (current instanceof Treatment)     ) return false;
      if (actor.gear.amountOf(MEDICINE) <= 0   ) return false;
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
      if (subject instanceof Actor) {
        final Actor treats = (Actor) subject;
        treats.traits.setLevel(asCondition, 1);
        treats.health.liftInjury(1 + Rand.index(5));
      }
    }
    
    
    protected float conditionDuration() {
      return Stage.STANDARD_DAY_LENGTH;
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      float lift = affected.health.maxHealth() / 3;
      lift /= Stage.STANDARD_DAY_LENGTH;
      affected.health.liftInjury(lift);
    }
  };
  
  
  //
  //  Improves fatigue regen and boosts reflex/cognition/vigor.
  final public static Technique BOOSTER_SHOT = new Technique(
    "Booster Shot", DIR+"booster_shot.png",
    "",
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
      if (current instanceof Combat) return true;
      
      return super.triggeredBy(actor, current, action, used, passive);
    }
    
    
    protected Action createActionFor(Plan parent, Actor actor, Target subject) {
      final Pick <Actor> pick = new Pick(0);
      for (Target t : actor.senses.awareOf()) if (t instanceof Actor) {
        final Actor a = (Actor) t;
        if (a.traits.hasTrait(asCondition)) continue;
        final float rating = 0 - PlanUtils.combatPriority(
          actor, t, 0, 1, false, Plan.REAL_HARM
        );
        pick.compare(a, rating);
      }
      if (pick.empty()) return null;
      return super.createActionFor(parent, actor, pick.result());
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      
      final Actor affects = (Actor) subject;
      affects.traits.setLevel(asCondition, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      float amount = Nums.min(1, affected.traits.traitLevel(asCondition));
      
      affected.traits.incBonus(IMMUNE   , 5 * amount);
      affected.traits.incBonus(MOTOR    , 5 * amount);
      affected.traits.incBonus(COGNITION, 5 * amount);
      affected.health.liftFatigue(0.2f * amount);
    }
  };
  
  
  //  Reduces hostilities and increases suggestibility.
  final public static Technique PAX_9 = null;
  
  //  Prevents all harmful actions by self, but confers near-immunity to
  //  suggestion or intimidation.
  final public static Technique PSY_INHIBITION = null;
}










