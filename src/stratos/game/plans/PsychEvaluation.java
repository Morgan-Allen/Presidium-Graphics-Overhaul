

package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;



public class PsychEvaluation extends Treatment {
  
  
  private static boolean
    evalVerbose  = true ,
    eventVerbose = true ;
  
  
  protected PsychEvaluation(
    Actor treats, Actor patient, Condition treated, Boarding sickbay
  ) {
    super(treats, patient, Conditions.POOR_MORALE, sickbay);
  }
  
  
  public PsychEvaluation(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public Plan copyFor(Actor other) {
    return new PsychEvaluation(other, patient, sickness, sickbay);
  }
  
  
  
  /**  Target and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { EMPATHIC, CURIOUS };
  final static Skill BASE_SKILLS[] = { SUASION, COUNSEL, PSYCHOANALYSIS };
  
  
  protected float getPriority() {
    return 0;
  }
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
  }
}


/*
public class Treatment extends Plan implements Economy {
  private void configFor(Actor patient, Venue theatre, boolean forLeave) {
    if (
      actor != patient && ofType(TYPE_PSYCH_EVAL, null) &&
      theatre.structure.upgradeLevel(Sickbay.NEURAL_SCANNING) > 0
    ) {
      final Item psyResult = treatResult(OT, TYPE_PSYCH_EVAL, null);
      final Profile profile = theatre.base().profiles.profileFor(patient);
      
      if (
        profile.daysSincePsychEval(theatre.world()) > 10 &&
        (psyResult == null || psyResult.amount < 1)
      ) {
        treatDC = PSYCH_EVAL_DC;
        treatDC += (1 - patient.health.moraleLevel()) * 5;
        majorSkill = PSYCHOANALYSIS;
        minorSkill = PHARMACY;
        accessory  = Item.withAmount(STIM_KITS, 1);
        type = TYPE_PSYCH_EVAL;
        if (psyResult != null) result = psyResult;
        return;
      }
    }
/*
    
    if (type == TYPE_PSYCH_EVAL) {
      //
      //  Combat poor morale and update engram records-
      if (actor.gear.amountOf(result) == 1) {
        final Profile p = actor.base().profiles.profileFor(patient);
        p.setPsychEvalTime(actor.world().currentTime());
      }
      patient.health.adjustMorale((1 + effect) / (2 * MEDIUM_DURATION));
      depleteResult(patient, MEDIUM_DURATION);
      return true;
    }
    
    return true;
  }
  
  //
  //  TODO:  This should really require a separate Psychoanalysis check, stored
  //  as a separate treatment item...
  private void reconstructFaculties(Actor patient) {
    final World world = patient.world();
    final Profile p = theatre.base().profiles.profileFor(patient);
    float memoryLoss = p.daysSincePsychEval(world) / (MEDIUM_DURATION * 2);
    memoryLoss = Visit.clamp(memoryLoss, 0.1f, 0.9f);
    
    for (Relation r : patient.memories.relations()) {
      float level = r.value();
      level *= 1 - (memoryLoss * Rand.avgNums(2) * (1 - Math.abs(level)));
      patient.mind.initRelation(r.subject, level, Rand.num());
    }
    
    for (Skill s : patient.traits.skillSet()) {
      float level = patient.traits.traitLevel(s);
      level *= 1 - (memoryLoss * Rand.avgNums(2) / 2f);
      patient.traits.setLevel(s, level);
    }
  }
//*/





