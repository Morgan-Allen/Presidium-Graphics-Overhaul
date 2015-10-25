/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.util.Rand;




//  I need to modify the AI of Fauna so that they can be 'domesticated' by a
//  particular handler.  Then they can follow-and-support said handler, and
//  behave as mounts.

//  Basically, dialogue needs to do what it does for any other actor- raise
//  relations with a probability of conversion.  (And try to bring some gifts
//  beforehand.)



public class EcologistTechniques {
  
  
  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = EcologistTechniques.class;
  
  final static PlaneFX.Model
    TRANQ_BURST_FX = PlaneFX.imageModel(
      "tranq_burst_fx", BASE_CLASS,
      FX_DIR+"tranquilliser_burst.png",
      0.66f, 0, 0.33f, true, false
    );
  
  final static int
    TRANQ_HIT_INIT = 2,
    TRANQ_HIT_FULL = 5,
    TRANQ_HIT_TIME = 5;
  
  final static float
    CALL_MIN_NOVELTY   = 0.5f,
    CALL_MAX_HOSTILITY = 0.5f;

  
  
  final public static Technique TRANQUILLISE = new Technique(
    "Tranquillise", UI_DIR+"tranquillise.png",
    "Deals bonus nonlethal damage to living targets over a short time.",
    BASE_CLASS, "tranquillise",
    MINOR_POWER         ,
    MILD_HARM           ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_FOCUS_TARGETING, MARKSMANSHIP, 10,
    Action.FIRE, Action.QUICK | Action.RANGED
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (! actor.gear.hasDeviceProperty(Devices.STUN)) {
        return false;
      }
      if (current instanceof Combat && subject instanceof Actor) {
        final Actor shot = (Actor) subject;
        if (! shot.health.organic()) return false;
        return true;
      }
      return false;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      final boolean hits = Combat.performGeneralStrike(
        actor, (Actor) subject, Combat.OBJECT_SUBDUE, actor.currentAction()
      );
      return hits && super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (success) {
        final Actor shot = (Actor) subject;
        shot.health.takeFatigue(TRANQ_HIT_INIT);
        shot.traits.incLevel(asCondition, 1);
        ActionFX.applyBurstFX(TRANQ_BURST_FX, shot, 0.5f, 1);
      }
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      float fatigue = TRANQ_HIT_FULL - TRANQ_HIT_INIT;
      fatigue /= conditionDuration();
      affected.health.takeFatigue(fatigue);
    }
    
    
    protected float conditionDuration() {
      return TRANQ_HIT_TIME;
    }
  };
  
  
  final public static Technique PATTERN_CAMO = new Technique(
    "Pattern Camo", UI_DIR+"pattern_camo.png",
    "Allows the subject to blend in with background vegetation, affording "+
    "protection from prying eyes and a better chance to ambush enemies.",
    BASE_CLASS, "pattern_camo",
    MINOR_POWER         ,
    REAL_HELP           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, null, 0,
    Action.MOVE_SNEAK, Action.NORMAL
  ) {
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject
    ) {
      if (actor.traits.hasTrait(asCondition)) {
        return false;
      }
      return actor.senses.isEmergency();
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (actor.indoors()) return false;
      
      final Tile location = actor.origin();
      final Species flora[] = location.habitat().floraSpecies;
      if (flora == null) return false;
      
      return super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      if (affected.isMoving()) {
        affected.traits.remove(asCondition);
      }
      else {
        affected.traits.incBonus(STEALTH_AND_COVER, 10);
        affected.traits.setLevel(asCondition, 0.99f);
      }
    }
    
    
    protected void onConditionStart(Actor affected) {
      super.onConditionStart(affected);
      
      final Tile location = affected.origin();
      final Species flora[] = location.habitat().floraSpecies;
      if (flora == null) return;
      final ModelAsset model = flora[0].modelSequence[0];
      affected.attachDisguise(model.makeSprite());
      
      applyAsCondition(affected);
      SenseUtils.breaksPursuit(affected, null);
    }
    
    
    protected void onConditionEnd(Actor affected) {
      super.onConditionEnd(affected);
      affected.detachDisguise();
    }
  };
  
  
  final public static Technique XENO_CALL = new Technique(
    "Xeno Call", UI_DIR+"xeno_call.png",
    "Aids communication with animal subjects and attempts at taming or "+
    "domestication.",
    BASE_CLASS, "xeno_call",
    MINOR_POWER         ,
    MILD_HELP           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_ANY_TARGETING, XENOZOOLOGY, 5,
    Action.TALK, Action.QUICK | Action.RANGED
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (! (subject instanceof Fauna)) {
        return false;
      }
      final Fauna calls = (Fauna) subject;
      if (actor.relations.noveltyFor(calls) <= CALL_MIN_NOVELTY) {
        return false;
      }
      if (current instanceof Dialogue && current.subject() == subject) {
        return true;
      }
      if (calls.planFocus(Combat.class, true) == actor) {
        return true;
      }
      return false;
    }
    
    
    public float basePriority(Actor actor, Plan current, Target subject) {
      return PlanUtils.dialoguePriority(actor, (Fauna) subject, 0, 1);
    }


    protected boolean checkActionSuccess(Actor actor, Target subject) {
      final boolean talks = DialogueUtils.talkResult(
        XENOZOOLOGY, MODERATE_DC, actor, (Fauna) subject
      ) > 0;
      return talks && super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      
      final Fauna calls = (Fauna) subject;
      using.relations.incRelation(calls, 0, 0, -1);
      
      if (success) {
        calls.relations.incRelation(using, 0.5f, 0.5f, 0);
        
        if (calls.harmIntended(using) > 0) {
          Behaviour root = calls.mind.rootBehaviour();
          calls.mind.cancelBehaviour(root, "Xeno Call Used!");
        }
        
        float relation = calls.relations.valueFor(using);
        if (Rand.num() < relation) {
          calls.assignBase(using.base());
          calls.mind.setHome(using.mind.home());
        }
      }
    }
  };
  
  
  final public static Traded MOUNT_HARNESS = new Traded(
    BASE_CLASS, "Mount Harness", null, Economy.FORM_SPECIAL, 0,
    "Allows riding of animal mounts."
  );
  
  final public static Technique MOUNT_TRAINING = new Technique(
    "Mount Training", UI_DIR+"mount_training.png",
    "Permits certain tamed animals to be used as personal mounts.",
    BASE_CLASS      , "mount_training",
    MAJOR_POWER     ,
    NO_HARM         ,
    NO_FATIGUE      ,
    NO_CONCENTRATION,
    IS_GEAR_PROFICIENCY | IS_TRAINED_ONLY, XENOZOOLOGY, 15,
    MOUNT_HARNESS
  ) {};
  
  
  final public static Technique ECOLOGIST_TECHNIQUES[] = {
    TRANQUILLISE, PATTERN_CAMO, XENO_CALL, MOUNT_TRAINING
  };
}








