/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



/**  Retains a record of skills and techniques learned by the actor.
  */
public class ActorSkills {
  
  
  final static float
    MIN_FAIL_CHANCE    = 0.1f,
    MAX_SUCCEED_CHANCE = 0.9f;
  
  protected static boolean
    testsVerbose = false,
    techsVerbose = false;
  
  
  final Actor actor;
  private List <Technique> known = new List <Technique> ();
  private Technique active = null;
  
  
  public ActorSkills(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadObjects(known);
    active = (Technique) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(known);
    s.saveObject(active);
  }
  
  
  
  /**  Updates and modifications-
    */
  public void updateSkills(int numUpdates) {
    //
    //  See if we've learned any new techniques based on practice in source
    //  skills or item proficiency.
    if (actor.species().sapient()) for (Skill s : actor.traits.skillSet()) {
      final Series <Technique> learnt = Technique.learntFrom(s);
      if (learnt == null) continue;
      for (Technique t : learnt) if (t.canBeLearnt(actor, false)) {
        known.include(t);
      }
    }
    //
    //  Flush older/expired TDPs.
    
    //
    //  And decay any skills that haven't been used in a while.
  }
  
  
  public void addTechnique(Technique t) {
    known.include(t);
  }
  
  
  
  /**  Helper methods for technique selection and queries-
    */
  public Action bestTechniqueFor(Plan plan, Action taken) {
    final boolean report = I.talkAbout == actor && false;
    final Pick <Technique> pick = new Pick(0);
    this.active = null;
    
    final Target subject = taken.subject();
    float harmLevel = taken.actor.harmIntended(subject);
    if (subject == actor) harmLevel -= 0.5f;
    
    if (report) {
      I.say("\nGetting best technique for "+actor);
      I.say("  Fatigue:       "+actor.health.fatigueLevel());
      I.say("  Concentration: "+actor.health.concentration());
      I.say("");
    }
    
    for (Technique t : known) {
      if (! t.triggeredBy(actor, plan, taken, null, false)) continue;
      final float appeal = t.priorityFor(actor, subject, harmLevel);
      pick.compare(t, appeal);
      if (report) {
        I.say("  "+t+" (Fat "+t.fatigueCost+" Con "+t.concentrationCost+")");
        I.say("    Appeal is "+appeal);
      }
    }
    if (pick.empty()) return null;
    
    final Technique best = pick.result();
    this.active = best;
    if (report) I.say("  Technique chosen: "+best);
    return best.createActionFor(plan, actor, subject);
  }
  
  
  public float skillBonusFromTechniques(Skill skill, Action taken) {
    final Pick <Technique> pick = new Pick(0);
    this.active = null;
    
    final boolean acts      = taken != null;
    final Plan    current   = acts ? taken.parentPlan()                : null ;
    final Target  subject   = acts ? taken.subject()                   : actor;
    final float   harmLevel = acts ? taken.actor.harmIntended(subject) : -1   ;
    
    for (Technique t : known) {
      if (! t.triggeredBy(actor, current, taken, skill, true)) continue;
      final float appeal = t.priorityFor(actor, subject, harmLevel);
      pick.compare(t, appeal);
    }
    if (pick.empty()) return -1;
    
    final Technique bonus = pick.result();
    this.active = bonus;
    return bonus.passiveBonus(actor, skill, subject);
  }
  
  
  public Series <Technique> knownTechniques() {
    return known;
  }
  
  
  public Series <Power> knownPowers() {
    final Batch <Power> powers = new Batch <Power> ();
    for (Technique t : known) if (t.isPower()) powers.add((Power) t);
    return powers;
  }
  
  
  public boolean hasTechnique(Technique t) {
    return known.includes(t);
  }
  
  
  public Series <Traded> gearProficiencies() {
    final Batch <Traded> GP = new Batch();
    final Background b = actor.mind.vocation();
    if (b != null) for (Traded t : b.properGear()) {
      GP.add(t);
    }
    for (Technique t : known) {
      final Traded GT = t.allowsUse();
      if (GT != null) GP.add(GT);
    }
    return GP;
  }
  
  
  public boolean hasGearProficiency(Traded type) {
    return gearProficiencies().includes(type);
  }
  
  
  
  /**  Methods for performing actual skill tests against both static and active
    *  opposition-
    */
  public float chance(
    Skill checked,
    Actor b, Skill opposed,
    float bonus
  ) {
    if (checked == null) return 0;
    final float
      bonusA = actor.traits.usedLevel(checked) + Nums.max(0, bonus),
      bonusB = (b != null && opposed != null) ?
        (b.traits.usedLevel(opposed) - Nums.min(0, bonus)) :
        (0 - Nums.min(0, bonus));
    
    final float chance = (bonusA + 10 - bonusB) / 20;
    return Nums.clamp(chance, MIN_FAIL_CHANCE, MAX_SUCCEED_CHANCE);
  }
  
  
  public float chance(Skill checked, float DC) {
    return chance(checked, null, null, 0 - DC);
  }
  
  
  public float test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float duration, int range,
    Action action
  ) {
    if (checked == null) return 0;
    //  TODO:  Physical skills need to exact fatigue!
    //  TODO:  Sensitive skills must tie in with awareness/fog levels.
    //  TODO:  Cognitive skills should need study to advance.
    
    //
    //  Invoke any known techniques here that are registered to be triggered
    //  by a skill of this type, and get their associated bonus:
    final float boost = skillBonusFromTechniques(checked, action);
    if (boost > 0) bonus += boost;
    //
    //  Then get the baseline probability of success in the task.
    final float chance = chance(checked, b, opposed, bonus);
    float success = 0;
    //
    //  And calculate the overall degree of success weighted by random
    //  outcomes.
    if (range <= 0) success = chance;
    else for (int tried = range; tried-- > 0;) {
      if (Rand.num() < chance) success++;
    }
    //
    //  Then grant experience in the relevant skills (included those used by
    //  any competitor) and activate any special effects for used techniques-
    practice(checked, chance, duration);
    if (b != null && opposed != null) {
      b.skills.practice(opposed, chance, duration);
    }
    if (active != null) {
      final Target subject = action == null ? actor : action.subject();
      active.applyEffect(actor, success > 0, subject, true);
    }
    //
    //  Finally, return the result.
    return success;
  }
  
  
  public boolean test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float fullXP,
    Action action
  ) {
    return test(checked, b, opposed, bonus, fullXP, 1, action) > 0;
  }
  
  
  public float test(
    Skill checked, float difficulty, float duration, int range,
    Action action
  ) {
    return test(checked, null, null, 0 - difficulty, duration, range, action);
  }
  
  
  public boolean test(
    Skill checked, float difficulty, float duration,
    Action action
  ) {
    return test(checked, null, null, 0 - difficulty, duration, 1, action) > 0;
  }
  
  
  public void practiceAgainst(int DC, float duration, Skill skillType) {
    final float chance = chance(skillType, null, null, 0 - DC);
    practice(skillType, chance, duration);
  }
  
  
  private void practice(Skill skillType, float chance, float duration) {
    final float level = actor.traits.traitLevel(skillType);
    float practice = chance * (1 - chance) * 4;
    practice *= duration / (10f * (level + 1));
    actor.traits.incLevel(skillType, practice);
    
    if (skillType.parent != null) {
      practice(skillType.parent, chance, duration / 4);
    }
  }
}









