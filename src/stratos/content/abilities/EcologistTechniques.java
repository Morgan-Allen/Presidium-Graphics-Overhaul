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
import stratos.util.I;
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
      0.5f, 0, 0.15f, true, false
    );
  
  final static int
    TRANQ_HIT_INIT = 2,
    TRANQ_HIT_FULL = 5,
    TRANQ_HIT_TIME = 5,
    
    CALL_AFFECTION_MAX         = 50,
    CALL_AFFECTION_NUDGE       = 10,
    CALL_AFFECTION_MIN_CONVERT = 30,
    CALL_AFFECTION_MAX_NEEDED  = 65,
    CALL_MAX_RANGE             = Stage.ZONE_SIZE * 2,
    CALL_NOVELTY_LOSS          = 10;

  
  
  final public static Technique TRANQUILLISE = new Technique(
    "Tranquillise", UI_DIR+"tranquillise.png",
    "Deals bonus nonlethal damage to living targets over a short time.",
    BASE_CLASS, "tranquillise",
    MINOR_POWER         ,
    MILD_HARM           ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_FOCUS_TARGETING | IS_TRAINED_ONLY, MARKSMANSHIP, 10,
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
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
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
    IS_PASSIVE_ALWAYS | IS_TRAINED_ONLY, null, 0, null
  ) {
    
    private boolean canHide(Actor using) {
      if (! using.health.conscious()) return false;
      if (using.isMoving() || ! using.senses.isEmergency()) return false;
      return true;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      if (using.traits.hasTrait(asCondition) || ! canHide(using)) return;
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (actor.indoors()) return false;
      
      final Tile location = actor.origin();
      final Species flora[] = location.habitat().floraSpecies;
      if (flora == null) return false;
      
      return super.checkActionSuccess(actor, subject);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      if (! canHide(affected)) {
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
    IS_ANY_TARGETING | IS_TRAINED_ONLY, XENOZOOLOGY, 5,
    Action.LOOK, Action.RANGED
  ) {
    
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (
        (subject == actor) && (! actor.senses.isEmergency()) &&
        actor.world().claims.inWilds(actor) &&
        actor.health.concentrationLevel() >= 1 &&
        animalCompanion(actor) == null
      ) {
        return true;
      }
      if (! (subject instanceof Fauna)) {
        return false;
      }
      final Fauna f = (Fauna) subject;
      if (f.relations.noveltyFor(actor) <= 0) return false;
      return ! f.domesticated();
    }
    
    
    public float basePriority(Actor actor, Plan current, Target subject) {
      if (super.basePriority(actor, current, subject) <= 0) return -1;
      if (subject == actor) return Plan.CASUAL;
      return PlanUtils.dialoguePriority(actor, (Fauna) subject, false, 0, 1);
    }


    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (subject == actor) return true;
      
      final boolean talks = DialogueUtils.talkResult(
        XENOZOOLOGY, MODERATE_DC, actor, (Fauna) subject
      ) > 0;
      return talks && super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      //
      //  In the case of shots-in-the-dark, pick a random nearby creature that
      //  might respond, and check against that.
      if (subject == using) {
        subject = using.world().presences.randomMatchNear(
          Fauna.class, using, CALL_MAX_RANGE
        );
        if (! (subject instanceof Fauna)) return;
        success &= checkActionSuccess(using, subject);
      }
      final Fauna calls = (Fauna) subject;
      super.applyEffect(using, subject, success, passive);
      //
      //  If successful, nudge relations with the creature upward, and attempt
      //  to initiate a conversation (if not already started.)
      if (success) {
        float affectMax   = CALL_AFFECTION_MAX   / 100f;
        float affectNudge = CALL_AFFECTION_NUDGE / 100f;
        calls.relations.incRelation(using, affectMax, affectNudge, 0);
        
        final Dialogue response = Dialogue.responseFor(
          calls, using, null, Plan.ROUTINE * Rand.num()
        );
        if (! calls.mind.mustIgnore(response)) {
          calls.mind.assignBehaviour(response);
        }
        //
        //  If you don't already have an animal companion, you can attempt to
        //  'convert' the creature-
        if (animalCompanion(using) != null) return;
        final float convertRoll = roll(
          CALL_AFFECTION_MIN_CONVERT, CALL_AFFECTION_MAX_NEEDED
        ) / 100;
        if (convertRoll < calls.relations.valueFor(using)) {
          calls.setAsDomesticated(using);
        }
      }
      //
      //  If the call fails entirely, reduce novelty so that attempts can't be
      //  made indefinitely:
      else {
        final float loss = CALL_NOVELTY_LOSS / -100f;
        using.relations.incRelation(calls, 0, 0, loss);
        calls.relations.incRelation(using, 0, 0, loss);
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
    IS_SELF_TARGETING | IS_PASSIVE_ALWAYS | IS_TRAINED_ONLY, XENOZOOLOGY, 15,
    MOUNT_HARNESS
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      final Actor comp = animalCompanion(actor);
      if (actor.currentMount() == comp) return false;
      return shouldMount(actor, comp);
    }
    
    
    protected Action createActionFor(Plan parent, Actor actor, Target subject) {
      final Action a = super.createActionFor(parent, actor, subject);
      Fauna moves = animalCompanion(actor);
      if (moves == null) return null;
      a.setMoveTarget(moves);
      return a;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      if (subject == using) subject = animalCompanion(using);
      final Fauna companion = (Fauna) subject;
      
      if (shouldMount(using, companion)) {
        using.bindToMount(companion);
      }
      else {
        using.releaseFromMount();
      }
    }
  };
  
  
  private static boolean shouldMount(Actor using, Actor companion) {
    final Action action = using.currentAction();
    
    if (companion == null || action == null) return false;
    if (companion.health.baseBulk() < using.health.baseBulk()) return false;
    
    final Target focus = action.subject();
    if (focus == companion || action.movesTo() == companion) return true;

    final float seeRange = using.health.sightRange();
    final float distance = Spacing.distance(using, action.subject());
    if (
      distance < seeRange && (! action.ranged()) &&
      (focus != companion.actionFocus() || focus.indoors())
    ) {
      return false;
    }
    return true;
  }
  
  
  private static Fauna animalCompanion(Actor using) {
    for (Actor a : using.relations.servants()) if (a instanceof Fauna) {
      return (Fauna) a;
    }
    return null;
  }
  
  
  final public static Technique ECOLOGIST_TECHNIQUES[] = {
    TRANQUILLISE, PATTERN_CAMO, XENO_CALL, MOUNT_TRAINING
  };
}












