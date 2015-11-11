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
    final boolean report = I.talkAbout == actor && techsVerbose;
    boolean canLearn = actor.health.conscious() && actor.species().sapient();
    //
    //  See if we've learned any new techniques based on practice in source
    //  skills or item proficiency.
    if (canLearn) for (Skill s : actor.traits.skillSet()) {
      final Series <Technique> learnt = Technique.learntFrom(s);
      if (learnt != null) for (Technique t : learnt) {
        if (known.includes(t)) continue;
        if (! t.canBeLearnt(actor, false)) continue;
        
        if (report) {
          I.say("Learning: "+t);
        }
        
        known.add(t);
      }
    }
    //
    //  And apply passive techniques at all times.
    for (Technique t : availableTechniques()) if (t.isPassiveAlways()) {
      t.applyEffect(actor, true, actor, true);
    }
    //
    //  ...And decay any skills that haven't been used in a while?
  }
  
  
  public void addTechnique(Technique t) {
    known.include(t);
  }
  
  
  public void removeTechnique(Technique t) {
    known.remove(t);
  }
  
  
  
  /**  Helper methods for technique selection and queries-
    */
  public Action bestTechniqueFor(Plan plan, Action taken) {
    final boolean report = I.talkAbout == actor && techsVerbose;
    final Choice choice = new Choice(actor);
    this.active = null;
    
    if (report) {
      I.say("\nGetting best active technique for "+actor);
      I.say("  Fatigue:       "+actor.health.fatigueLevel ());
      I.say("  Concentration: "+actor.health.concentration());
      I.say("  Plan:          "+plan);
      I.say("");
    }
    
    for (Technique t : availableTechniques()) {
      if (t.targetsSelf() || t.targetsAny()) {
        rateActiveTechnique(t, plan, actor, choice, report);
      }
      if (t.targetsFocus()) {
        rateActiveTechnique(t, plan, taken.subject(), choice, report);
      }
      if (t.targetsAny()) for (Target seen : actor.senses.awareOf()) {
        rateActiveTechnique(t, plan, seen, choice, report);
      }
    }
    
    final Action action = (Action) choice.weightedPick();
    this.active = action == null ? null : (Technique) action.basis;
    if (report) I.say("  Technique chosen: "+active);
    return action;
  }
  
  
  protected float skillBonusFromTechniques(
    Skill skill, Plan current, Target subject, Action taken
  ) {
    final boolean report = I.talkAbout == actor && techsVerbose;
    
    if (report) {
      I.say("\nGetting best passive technique for "+actor);
      I.say("  Fatigue:       "+actor.health.fatigueLevel ());
      I.say("  Concentration: "+actor.health.concentration());
      I.say("  Skill used:    "+skill  );
      I.say("  Current plan:  "+current);
      I.say("  Subject:       "+subject);
    }
    
    final Pick <Technique> pick = new Pick(0);
    this.active = null;
    if (current == null && taken != null) current = taken.parentPlan();
    if (subject == null && taken != null) subject = taken.subject();
    if (current == null) current = actor.matchFor(null, true);
    if (subject == null) subject = actor;
    
    for (Technique t : availableTechniques()) if (t.isPassiveSkillFX()) {
      if (! t.triggersPassive(actor, current, skill, subject)) {
        if (report) I.say("  "+t+" is not applicable to "+subject);
        continue;
      }
      final float appeal = t.basePriority(actor, current, subject);
      
      if (report) {
        I.say("  "+t+" (Fat "+t.fatigueCost+" Con "+t.concentrationCost+")");
        I.say("    Appeal is: "+appeal);
        I.say("    Targeting: "+subject);
      }
      pick.compare(t, appeal);
    }
    
    final Technique bonus = pick.result();
    this.active = bonus;
    return bonus == null ? 0 : bonus.passiveBonus(actor, skill, subject);
  }
  
  
  private void rateActiveTechnique(
    Technique t, Plan plan, Target subject, Choice choice, boolean report
  ) {
    if (! t.triggersAction(actor, plan, subject)) {
      if (report) I.say("  "+t+" is not applicable to "+subject);
      return;
    }
    
    float appeal = -1  ;
    Action taken = null;
    final float radius = t.effectRadius();
    final boolean desc = t.effectDescriminates();
    
    if (radius > 0) {
      for (Actor affected : PlanUtils.subjectsInRange(subject, radius)) {
        final float priority = t.basePriority(actor, plan, affected);
        if (desc && priority <= 0) continue;
        appeal += priority;
      }
    }
    else {
      appeal = t.basePriority(actor, plan, subject);
    }
    if (appeal > 0) {
      taken = t.createActionFor(plan, actor, subject);
      if (taken != null) taken.setPriority(appeal * Plan.ROUTINE);
      choice.add(taken);
    }
    
    if (report) {
      I.say("  "+t+" (Fat "+t.fatigueCost+" Con "+t.concentrationCost+")");
      I.say("    Appeal is: "+appeal);
      I.say("    Targeting: "+subject);
    }
  }
  
  
  public Series <Technique> availableTechniques() {
    final Batch <Technique> all = new Batch(known.size() * 2);
    for (Technique t : known) all.add(t);
    for (Item i : actor.gear.usable()) {
      Visit.appendTo(all, i.type.techniques());
    }
    return all;
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
  
  
  public Series <Traded> getProficiencies() {
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
    final boolean opponent = b != null && opposed != null;
    //  TODO:  Physical skills need to exact fatigue!
    //  TODO:  Sensitive skills must tie in with awareness/fog levels.
    //  TODO:  Cognitive skills should need study to advance.
    //
    //  Invoke any known techniques here that are registered to be triggered
    //  by a skill of this type, and get their associated bonus:
    bonus += skillBonusFromTechniques(checked, null, null, action);
    if (opponent) {
      bonus -= b.skills.skillBonusFromTechniques(opposed, null, actor, null);
    }
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
    practice(checked, chance, duration, action);
    if (opponent) b.skills.practice(opposed, 1 - chance, duration, action);
    //
    //  Finally, return the result.
    return success;
  }
  
  
  private void practice(
    Skill skillType, float chance, float duration, Action after
  ) {
    final float level = actor.traits.traitLevel(skillType);
    float practice = chance * (1 - chance) * 4;
    practice *= duration / (10f * (level + 1));
    actor.traits.incLevel(skillType, practice);
    
    if (active != null) {
      active.afterSkillEffects(actor, chance, after);
      active = null;
    }
    if (skillType.parent != null) {
      practice(skillType.parent, chance, duration / 4, after);
    }
  }
  
  
  public void practiceAgainst(int DC, float duration, Skill skillType) {
    final float chance = chance(skillType, null, null, 0 - DC);
    practice(skillType, chance, duration, null);
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
}









