
package stratos.game.actors;
import stratos.game.common.*;
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
  final List <Technique> known = new List <Technique> ();
  
  
  public ActorSkills(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadObjects(known);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(known);
  }
  
  
  
  /**  Called every 20 seconds or so.
    */
  public void updateSkills(int numUpdates) {
    //
    //  See if we've learned any new techniques based on practice in source
    //  skills or item proficiency.
    for (Skill s : actor.traits.skillSet()) {
      final Series <Technique> learnt = Technique.learntFrom(s);
      if (learnt == null) continue;
      float level = actor.traits.traitLevel(s);
      level += actor.traits.bonusFrom(s.parent);
      for (Technique t : learnt) {
        if (level >= t.minLevel) known.include(t);
      }
    }
    //
    //  Flush older/expired TDPs.
    
    //
    //  And decay any skills that haven't been used in a while.
  }
  
  
  
  /**  Methods for performing actual skill tests against both static and active
    *  opposition-
    */
  public float chance(
    Skill checked,
    Actor b, Skill opposed,
    float bonus
  ) {
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
    float bonus, float duration, int range
  ) {
    //  TODO:  Physical skills need to exercise strength/vigour and exact
    //  fatigue!
    //  TODO:  Sensitive skills must exercise reflex/insight, and tie in with
    //  awareness/fog levels.
    //  TODO:  Cognitive skills need study to advance, and exercise will/
    //  intellect.
    
    //  Invoke any known techniques here that are registered to be triggered
    //  by a skill of this type, and get their associated bonus:
    final Target subject = actor.actionFocus();
    final Technique boost = pickSkillBonus(checked, subject);
    if (boost != null) bonus += boost.bonusFor(actor, checked, subject);
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
    practice(checked, chance, duration);
    if (b != null && opposed != null) {
      b.skills.practice(opposed, chance, duration);
    }
    //
    //  And return the result.
    if (boost != null) boost.applyEffect(actor, success > 0, subject);
    return success;
  }
  
  
  public boolean test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float fullXP
  ) {
    return test(checked, b, opposed, bonus, fullXP, 1) > 0;
  }
  
  
  public float test(
    Skill checked, float difficulty, float duration, int range
  ) {
    return test(checked, null, null, 0 - difficulty, duration, range);
  }
  
  
  public boolean test(Skill checked, float difficulty, float duration) {
    return test(checked, null, null, 0 - difficulty, duration, 1) > 0;
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
  
  
  
  /**  Technique-handling methods:
    *  TODO:  Move some of the decision-handling methods for Techniques over to
    *  here?
    */
  public Series <Technique> knownTechniques() {
    return known;
  }
  
  
  public Series <Power> knownPowers() {
    final Batch <Power> powers = new Batch <Power> ();
    for (Technique t : known) if (t.type == Technique.TYPE_SOVEREIGN_POWER) {
      powers.add((Power) t);
    }
    return powers;
  }
  
  
  public void addTechnique(Technique t) {
    known.include(t);
  }
  
  
  public Action pickIndependantAction(
    Target subject, Object trigger, Plan plan
  ) {
    Technique picked = pickBestKnown(subject, plan.harmFactor(), trigger);
    if (picked == null) return null;
    return picked.asActionFor(actor, subject);
  }
  
  //
  //  TODO:  Limit this to 'Passive Bonus' type techniques, and sum all such
  //  bonuses, not just the one.
  protected Technique pickSkillBonus(Skill s, Target subject) {
    final boolean report = techsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\n"+actor+" getting technique bonus for "+s+"...");
      I.say("  Fatigue: "+actor.health.fatigueLevel());
      I.say("  Concentration: "+actor.health.concentration());
    }
    final float harm = actor.harmIntended(subject);
    final Technique picked = pickBestKnown(subject, harm, s);
    
    if (report) I.say("  Technique picked: "+picked);
    return picked;
  }
  
  
  protected Technique pickBestKnown(
    Target subject, float harmLevel, Object trigger
  ) {
    final boolean report = techsVerbose && I.talkAbout == actor;
    Technique picked = null;
    float bestAppeal = 0;
    
    for (Technique t : known) if (t.trigger == trigger) {
      final float appeal = t.priorityFor(actor, subject, harmLevel);
      if (report) I.say("  "+t.name+" has appeal: "+appeal);
      if (appeal > bestAppeal) { bestAppeal = appeal; picked = t; }
    }
    return picked;
  }
}









