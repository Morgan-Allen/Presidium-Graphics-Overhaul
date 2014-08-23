
package stratos.game.actors;
import stratos.game.common.Session;
import stratos.game.common.World;
import stratos.util.*;



/**  Retains a record of skills and techniques learned by the actor.
  */


public class ActorSkills {
  

  final static float
    MIN_FAIL_CHANCE    = 0.1f,
    MAX_SUCCEED_CHANCE = 0.9f;
  
  final Actor actor;

  //  This class is used to store cooldowns, previous targets, etc. specific
  //  to a particular technique.  Stands for  Technique.  Data.  Property.
  private static class TDP {
    String key;
    float expiry;
    Session.Saveable ref;
    float val;
  }

  final List <Technique> known = new List <Technique> ();
  final Table <String, TDP> techniqueData = new Table();
  
  
  ActorSkills(Actor actor) {
    this.actor = actor;
  }
  
  
  void loadState(Session s) throws Exception {
    
    s.loadObjects(known);
    for (int n = s.loadInt(); n-- > 0;) {
      final TDP datum = new TDP();
      datum.key    = s.loadString();
      datum.expiry = s.loadFloat ();
      datum.ref    = s.loadObject();
      datum.val    = s.loadFloat ();
      techniqueData.put(datum.key, datum);
    }
  }
  
  
  void saveState(Session s) throws Exception {
    
    s.saveObjects(known);
    s.saveInt(techniqueData.size());
    for (TDP datum : techniqueData.values()) {
      s.saveString(datum.key   );
      s.saveFloat (datum.expiry);
      s.saveObject(datum.ref   );
      s.saveFloat (datum.val   );
    }
  }

  
  /**  Called every 20 seconds or so.
    */
  protected void updateSkills(int numUpdates) {
    //
    //  See if we've learned any new techniques based on practice in source
    //  skills or item proficiency.
    for (Technique t : Technique.ALL_TECHNIQUES()) {
      if (t.source instanceof Skill) {
        final float level = actor.traits.traitLevel((Skill) t.source);
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
    float bonusA = actor.traits.useLevel(checked) + Math.max(0, bonus);
    float bonusB = 0 - Math.min(0, bonus);
    if (b != null && opposed != null) bonusB += b.traits.useLevel(opposed);
    final float chance = Visit.clamp(bonusA + 10 - bonusB, 0, 20) / 20;
    return Visit.clamp(chance, MIN_FAIL_CHANCE, MAX_SUCCEED_CHANCE);
  }
  
  
  public float chance(Skill checked, float DC) {
    return chance(checked, null, null, 0 - DC);
  }
  
  
  public float test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float duration,
    int range
  ) {
    //  TODO:  Physical skills need to exercise strength/vigour and exact
    //  fatigue!
    //  TODO:  Sensitive skills must exercise reflex/insight, and tie in with
    //  awareness/FoW.
    //  TODO:  Cognitive skills need study to advance, and exercise will/
    //  intellect.
    
    //
    //  Invoke any known techniques here that are registered to be triggered
    //  by a skill of this type, and get their associated bonuses:
    final Behaviour plan = actor.mind.rootBehaviour();
    final Action action = actor.currentAction();
    
    for (Technique t : techniquesKnownFor(checked)) {
      final float tBonus = t.applyBonus(actor, plan, action);
      if (tBonus > 0) bonus += tBonus;
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
    practice(checked, (1 - chance) * duration / 10);
    if (b != null) b.skills.practice(opposed, chance * duration / 10);
    //
    //  And return the result-
    return success;
  }
  
  
  public boolean test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float fullXP
  ) {
    return test(checked, b, opposed, bonus, fullXP, 1) > 0;
  }
  
  
  public boolean test(Skill checked, float difficulty, float duration) {
    return test(checked, null, null, 0 - difficulty, duration, 1) > 0;
  }
  
  
  public void practice(Skill skillType, float practice) {
    final float level = actor.traits.traitLevel(skillType);
    actor.traits.incLevel(skillType, practice / (level + 1));
    if (skillType.parent != null) practice(skillType.parent, practice / 5);
  }
  
  
  public void practiceAgainst(int DC, float duration, Skill skillType) {
    final float chance = chance(skillType, null, null, 0 - DC);
    practice(skillType, chance * duration / 10);
  }
  
  
  
  /**  Handling Techniques:
    */
  //  TODO:  Use a more efficient caching system for this.
  public Batch <Technique> techniquesKnownFor(Object trigger) {
    final Batch <Technique> matched = new Batch();
    for (Technique t : known) if (Visit.arrayIncludes(t.triggers, trigger)) {
      matched.include(t);
    }
    return matched;
  }
  
  
  protected void storeDatumFor(
    Technique t, String key, Session.Saveable ref, float val
  ) {
    key = t.name+key;
    TDP match = techniqueData.get(key);
    if (match == null) techniqueData.put(key, match = new TDP());
    
    match.key = key;
    match.expiry = actor.world().currentTime() + World.STANDARD_HOUR_LENGTH;
    match.ref = ref;
    match.val = val;
  }
  
  
  protected float datumValFor(Technique t, String key) {
    key = t.name+key;
    final TDP match = techniqueData.get(key);
    if (match != null) return match.val;
    else return 0;
  }
  
  
  protected Session.Saveable datumRefFor(Technique t, String key) {
    key = t.name+key;
    final TDP match = techniqueData.get(key);
    return match == null ? null : match.ref;
  }
}









