/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;



public abstract class Plan implements Session.Saveable, Behaviour {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MOTIVE_LEISURE   = 0,
    MOTIVE_JOB       = 1,
    MOTIVE_AMBITION  = 2,
    MOTIVE_EMERGENCY = 3,
    MOTIVE_MISSION   = 4,
    MOTIVE_CANCELLED = 5;
  
  final static float
    NULL_PRIORITY = -100;
  
  private static boolean
    verbose      = false,
    extraVerbose = false,
    evalVerbose  = false,
    doesVerbose  = false;
  private static Class
    verboseClass = null;
  
  final public Target subject;
  protected Actor actor;  //  TODO:  MAKE THIS FINAL
  
  protected float
    lastEvalTime = -1,
    priorityEval = NULL_PRIORITY;
  protected Behaviour
    nextStep = null,
    lastStep = null;
  
  private int   motiveType  = -1;
  private float motiveBonus =  0;
  private float harmFactor, competence;
  private boolean begun;  //  TODO:  Have a general 'stage' counter.
  
  
  
  protected Plan(
    Actor actor, Target subject, int motiveType, float harmFactor
  ) {
    this.actor      = actor     ;
    this.subject    = subject   ;
    this.motiveType = motiveType;
    this.harmFactor = harmFactor;
    if (subject == null) I.complain("NULL PLAN SUBJECT");
  }
  
  
  public Plan(Session s) throws Exception {
    s.cacheInstance(this);
    this.actor   = (Actor) s.loadObject();
    this.subject = s.loadTarget();
    
    this.lastEvalTime = s.loadFloat();
    this.priorityEval = s.loadFloat();
    this.nextStep     = (Behaviour) s.loadObject();
    this.lastStep     = (Behaviour) s.loadObject();
    this.motiveType   = s.loadInt();
    this.motiveBonus  = s.loadFloat();
    
    this.harmFactor = s.loadFloat();
    this.competence = s.loadFloat();
    this.begun      = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(actor  );
    s.saveTarget(subject);
    
    s.saveFloat (lastEvalTime);
    s.saveFloat (priorityEval);
    s.saveObject(nextStep    );
    s.saveObject(lastStep    );
    s.saveInt   (motiveType  );
    s.saveFloat (motiveBonus );
    
    s.saveFloat(harmFactor);
    s.saveFloat(competence);
    s.saveBool (begun     );
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (p == null || p.getClass() != this.getClass()) return false;
    return this.subject == ((Plan) p).subject;
  }
  
  
  public Target subject() {
    return subject;
  }
  
  
  
  /**  Default implementations of Behaviour methods-
    */
  public int motionType(Actor actor) {
    return MOTION_ANY;
  }
  
  
  public boolean valid() {
    if (! subject.inWorld()) return false;
    if (actor != null && ! actor.inWorld()) return false;
    return true;
  }
  
  
  public boolean persistent() {
    return motiveType == MOTIVE_JOB || motiveType == MOTIVE_AMBITION;
  }
  
  
  public boolean isEmergency() {
    return motiveType == MOTIVE_EMERGENCY;
  }
  
  
  public void toggleActive(boolean is) {
    if (actor == null) return;
    actor.world().activities.registerFocus(this, is);
  }
  
  
  public void interrupt(String cause) {
    if (! hasBegun()) return;
    if (verbose && I.talkAbout == actor) {
      I.say("\n"+actor+" Aborting plan! "+I.tagHash(this));
      I.say("  Cause: "+cause);
      I.reportStackTrace();
    }
    nextStep = lastStep = null;
    priorityEval = NULL_PRIORITY;
    actor.mind.cancelBehaviour(this, cause);
    setMotive(MOTIVE_CANCELLED, 0);
  }
  
  
  private boolean checkRefreshDue(Actor actor, boolean report) {
    if (report) {
      I.say("\nChecking if refreshment due for "+this);
      I.say("  Priority:  "+priorityEval);
      I.say("  Next step: "+nextStep);
    }
    
    if (this.actor != actor) {
      if (report) I.say("SWITCHING TO NEW ACTOR!");
      clearEval(actor);
      begun = false;
      return true;
    }
    
    if (! valid()) {
      if (report) I.say("\nNEXT STEP IS NULL: NOT VALID");
      onceInvalid();
      this.interrupt(INTERRUPT_NOT_VALID);
      return false;
    }
    
    final boolean oldDone = lastStep != null && (
      lastStep.finished() ||
      lastStep.nextStepFor(actor) == null
    );
    if (oldDone) {
      if (report) I.say("OLD STEP FINISHED: "+lastStep);
      clearEval(actor);
      lastStep = null;
      return true;
    }
    
    if (priorityEval == NULL_PRIORITY || nextStep == null) {
      if (report) I.say("ALREADY FLAGGED FOR EVALUATION!");
      return true;
    }
    
    if (actor.actionInProgress()) {
      if (report) I.say("ACTOR IS TOO BUSY!");
      return false;
    }
    final float
      timeGone = actor.world().currentTime() - lastEvalTime,
      interval = evaluationInterval();
    if (report) {
      I.say("\nChecking for fresh evaluation: "+this);
      I.say("  Time gone: "+timeGone+"/"+interval);
    }
    if (timeGone >= interval) { clearEval(actor); return true; }
    
    return false;
  }
  
  
  protected void clearEval(Actor actor) {
    this.actor   = actor;
    priorityEval = NULL_PRIORITY;
    nextStep     = null;
  }
  
  
  protected int evaluationInterval() {
    return actor.senses.isEmergency() ? 1 : 10;
  }
  
  
  public float priorityFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor && hasBegun() && (
      verboseClass == null || verboseClass == this.getClass()
    );
    if (motiveType == MOTIVE_CANCELLED) return -1;
    if (report && extraVerbose) {
      I.say("\nCurrent priority for "+this+" is: "+priorityEval);
    }
    
    if (checkRefreshDue(actor, report && extraVerbose)) {
      final float time = actor.world().currentTime();
      if (report) I.say("\nGetting fresh priority... "+I.tagHash(this));
      priorityEval = 0;  //  Note: This avoids certain types of infinite loop.
      priorityEval = getPriority();
      if (report) I.say("\nNew priority: "+priorityEval);
      if (nextStep != null) {
        begun        = true;
        lastEvalTime = time;
      }
    }
    return priorityEval;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor && hasBegun() && (
      verboseClass == null || verboseClass == this.getClass()
    );
    if (motiveType == MOTIVE_CANCELLED) return null;
    
    if (report && extraVerbose) {
      I.say("\nCurrent plan step for "+this+" is: "+I.tagHash(nextStep));
    }
    
    if (checkRefreshDue(actor, report && extraVerbose)) {
      if (report) I.say("\nPlan step for "+this+" was: "+I.tagHash(lastStep));
      final float time = actor.world().currentTime();
      final boolean subStep = this != actor.mind.rootBehaviour();
      
      nextStep = getNextStep();
      if (lastStep != null && lastStep.matchesPlan(nextStep)) {
        if (report) I.say("  NEXT STEP THE SAME AS OLD STEP: "+nextStep);
        nextStep = lastStep;
      }
      else if (nextStep != null) {
        if (report) I.say("  New plan step: "+I.tagHash(nextStep));
        lastStep = nextStep;
      }
      //
      //  We may have to set priority manually here, because sub-steps never
      //  have their priority queried outside- but this is needed to prevent
      //  being flagged as 'due for refresh'.
      if (subStep && priorityEval == NULL_PRIORITY) {
        priorityEval = IDLE;
      }
      if (priorityEval != NULL_PRIORITY) {
        begun        = true;
        lastEvalTime = time;
      }
    }
    return nextStep;
  }
  
  
  public boolean finished() {
    final boolean report = verbose && hasBegun() && I.talkAbout == actor;
    if (motiveType == MOTIVE_CANCELLED) return true;
    
    //  TODO:  Some of the reasoning here is a little opaque.  Rewrite this to
    //  have a single 'update' method, then read off everything else passively.
    /*
    if (! hasBegun()) return false;
    if (this == actor.mind.rootBehaviour()) {
      if (priorityEval != NULL_PRIORITY && priorityEval <= 0) {
        if (report) I.say("\nNO PRIORITY: "+I.tagHash(this));
        return true;
      }
    }
    if (lastStep != null && nextStep == null) {
      if (report) I.say("\nNO NEXT STEP: "+I.tagHash(this));
      return true;
    }
    return false;
    //*/
    //*
    if (actor == null) return false;
    
    if (this == actor.mind.rootBehaviour()) {
      if (priorityFor(actor) <= 0) {
        if (report) I.say("\nNO PRIORITY: "+I.tagHash(this));
        return true;
      }
    }
    if (nextStepFor(actor) == null) {
      if (report) I.say("\nNO NEXT STEP: "+I.tagHash(this));
      return true;
    }
    return false;
    //*/
  }
  
  
  public boolean hasBegun() {
    return actor != null && begun;
  }
  
  
  public boolean isActive() {
    return actor != null && actor.mind.agenda.includes(this);
  }
  
  
  public Actor actor() {
    return actor;
  }
  
  
  public static boolean canFollow(Actor a, Behaviour b) {
    if (b == null || ! b.valid()) return false;
    if (b.finished() || b.nextStepFor(a) == null) return false;
    return true;
  }
  
  
  public static boolean canPersist(Behaviour b) {
    if (b == null || ! b.valid()) return false;
    if (b.finished() || ! b.persistent()) return false;
    return true;
  }
  
  
  public static void reportPlanDetails(Behaviour b, Actor a) {
    if (b == null) { I.say("  IS NULL"); return; }
    I.say("    Plan class:    "+b.getClass().getSimpleName());
    I.say("    Valid:         "+b.valid());
    I.say("    Finished:      "+b.finished());
    I.say("    Next step:     "+b.nextStepFor(a));
    I.say("    Persists?      "+b.persistent());
    I.say("    Target is:     "+b.subject());
    I.say("    Target exists? "+b.subject().inWorld());
    I.say("    Target intact? "+(! b.subject().destroyed()));
  }
  
  
  
  /**  Assorted utility evaluation methods-
    */
  public Plan setMotive(int type, float bonus) {
    this.motiveType  = type ;
    this.motiveBonus = bonus;
    this.lastEvalTime = -1;
    return this;
  }
  
  
  public Plan setMotiveFrom(Plan parent, float bonus) {
    return setMotive(parent.motiveType, parent.motiveBonus + bonus);
  }
  
  
  public boolean hasMotiveType(int type) {
    return this.motiveType == type;
  }
  
  
  public float motiveBonus() {
    return this.motiveBonus;
  }
  
  
  public int motiveType() {
    return motiveType;
  }
  
  
  final public static float
    
    NO_FAIL_RISK      = 0.0f,
    MILD_FAIL_RISK    = 0.5f,
    REAL_FAIL_RISK    = 1.0f,
    EXTREME_FAIL_RISK = 2.0f,
    
    NO_COMPETITION   =  0.0f,
    MILD_COMPETITION =  0.5f,
    FULL_COMPETITION =  1.0f,
    MILD_COOPERATION = -0.5f,
    FULL_COOPERATION = -1.0f,
    
    NO_HARM      =  0.0f,
    MILD_HARM    =  0.5f,
    REAL_HARM    =  1.0f,
    EXTREME_HARM =  1.5f,
    MILD_HELP    = -0.5f,
    REAL_HELP    = -1.0f,
    EXTREME_HELP = -1.5f,
    
    NO_DISTANCE_CHECK      = 0.0f,
    PARTIAL_DISTANCE_CHECK = 0.5f,
    NORMAL_DISTANCE_CHECK  = 1.0f,
    HEAVY_DISTANCE_CHECK   = 2.0f,
    
    NO_MODIFIER = 0,
    
    PRIORITY_NEVER = 0 - PARAMOUNT;
  final protected static Skill NO_SKILLS[] = null;
  final protected static Trait NO_TRAITS[] = null;
  
  
  /**  Baseline priority-calculation method intended for use by subclasses.
    *  TODO:  DOCUMENT PROPERLY
    * @param actor
    * @param subject
    * @param defaultRange
    * @param flatBonus
    * @param subjectHarm
    * @param peersCompete
    * @param failRisk
    * @param baseSkills
    * @param baseTraits
    * @param distanceCheck
    * @param report
    * @return
    */
  protected float priorityForActorWith(
    
    Actor actor,
    Target subject,
    
    float defaultRange,
    float flatBonus,
    
    float subjectHarm,
    float peersCompete,
    float failRisk,
    
    Skill baseSkills[],
    Trait baseTraits[],
    float distanceCheck,
    
    boolean report
  ) {
    if (motiveType == MOTIVE_CANCELLED) return PRIORITY_NEVER;
    if (doesVerbose && hasBegun() && I.talkAbout == actor) report = true;
    
    this.harmFactor    = subjectHarm ;
    //this.competeFactor = peersCompete;
    final boolean mission = motiveType == MOTIVE_MISSION;
    float priority = PARAMOUNT, relation = 0;
    
    //  Firstly, we calculate the effect of internal and external motivations,
    //  such as how much we like the subject versus how much harm will be done
    //  to them.
    if (subjectHarm != 0) {
      relation = actor.relations.valueFor(subject);
      final float
        mulWeight = Nums.abs(subjectHarm),
        mulSign   = subjectHarm > 0 ? -1 : 1,
        mulLevel  = priority * relation * mulSign;
      priority = (priority * (1 - mulWeight)) + (mulLevel * mulWeight);
    }

    if (report) {
      I.say("\nEvaluating priority for "+this);
      I.say("  Actor/Subject:               "+actor+"/"+subject);
      I.say("  Harm factor/relation:        "+subjectHarm+"/"+relation);
      I.say("  After relation effects:      "+priority+"/"+PARAMOUNT);
    }
    
    final float extraPriority = flatBonus + motiveBonus;
    priority *= defaultRange * 1f / PARAMOUNT;
    priority += (extraPriority) / 2;
    priority = Nums.clamp(priority, 0, defaultRange + ROUTINE);
    
    if (report) {
      I.say("  Default priority range:      "+defaultRange);
      I.say("  Motive type/bonus:           "+motiveType+"/"+motiveBonus);
      I.say("  After motive effects:        "+priority+"/"+defaultRange);
    }
    
    //  We also inject the effects of competition/cooperation from peers.
    if (peersCompete != 0 && (peersCompete < 0 || ! hasBegun())) {
      float competeSum = competition(this, subject, actor) * peersCompete;
      priority *= Nums.clamp(1 - competeSum, 0, 1.5f);
      if (report) I.say("  After competition effects:   "+priority);
    }
    
    //  Okay.  You want to have 10 in at *least* one relevant skill in order for
    //  the activity to be at full strength.
    if (baseSkills != null && baseSkills.length > 0 && priority > 0) {
      if (report) I.say("  Getting skill effects...");
      float maxSkill = 0, avgSkill = 0;
      
      for (Skill s : baseSkills) {
        final float level = actor.traits.usedLevel(s);
        if (report) I.say("    "+s+" "+level);
        maxSkill = Nums.max(maxSkill, level / 10);
        avgSkill += level / 10;
      }
      
      avgSkill /= baseSkills.length;
      maxSkill = Nums.clamp((maxSkill + avgSkill) / 2, 0, 2);
      this.competence = maxSkill;
      priority *= Nums.sqrt(maxSkill);
      if (report) {
        I.say("  After skill adjustments:     "+priority);
      }
    }
    
    //  You also want to have at least one relevant trait in the *positive*
    //  range, in order for the activity to be at full strength.  Traits in the
    //  negative range make it less likely.
    if (baseTraits != null && baseTraits.length > 0 && priority > 0) {
      if (report) I.say("  Getting trait effects...");
      float maxTrait = -1, avgTrait = 0;
      
      for (Trait t : baseTraits) {
        final float level = actor.traits.relativeLevel(t);
        if (report) I.say("    "+t+" "+level);
        maxTrait = Nums.max(maxTrait, level);
        if (level >= 0.5f) avgTrait += (level - 0.5f) / 0.5f;
        else avgTrait += (level - 0.5f) / 1.5f;
      }
      
      avgTrait /= baseTraits.length;
      maxTrait = Nums.clamp((maxTrait + avgTrait) / 2, -1, 1);
      priority *= Nums.sqrt(1 + maxTrait);
      
      if (report) {
        I.say("  After trait adjustments:     "+priority);
      }
    }
    
    //  And finally, we include the off-putting effects of distance, danger,
    //  the potential costs of failure, and any flat modifier included.
    float
      chancePenalty = 0,
      rangePenalty  = 0,
      dangerPenalty = 0;
    
    if (failRisk > 0) {
      final float chance = successChanceFor(actor);
      chancePenalty = (1 - chance) * failRisk * PARAMOUNT;
    }
    
    if (distanceCheck != 0) {
      final float range = rangePenalty(actor.base(), actor, subject);
      rangePenalty = range * distanceCheck;
      final float danger = dangerPenalty(subject, actor) * (1f + failRisk);
      dangerPenalty = danger * (1 + range) / 2f;
    }
    
    priority += extraPriority / 2;
    priority -= chancePenalty;
    priority -= rangePenalty ;
    priority -= dangerPenalty;
    priority = Nums.clamp(priority, 0, defaultRange + extraPriority + ROUTINE);
    
    if (report) {
      I.say("  Distance is:                 "+Spacing.distance(actor, subject));
      I.say("  Chance penalty is:           "+chancePenalty);
      I.say("  Compete factor is:           "+peersCompete);
      I.say("  Range/Danger penalty is:     "+rangePenalty+"/"+dangerPenalty);
      I.say("  Flat modifier:               "+flatBonus);
      I.say("  Final priority:              "+priority);
    }
    return priority;
  }
  
  //  TODO:  This should be used to replace the skills handed over.
  //  TODO:  Consider making this abstract?
  public float successChanceFor(Actor actor) { return 1; }
  
  //  TODO:  Pass in a conversion instead (or allow for that.)
  
  protected float successForActorWith(
    Actor actor, Skill baseSkills[], float DC, boolean realTest
  ) {
    if (realTest) {
      float success = 0;
      for (Skill s : baseSkills) {
        success += actor.skills.test(s, DC, 1) ? 1 : 0;
      }
      return success / baseSkills.length;
    }
    else {
      float chance = 0;
      for (Skill s : baseSkills) {
        chance += actor.skills.chance(s, DC);
      }
      return chance / baseSkills.length;
    }
  }
  
  
  public float harmFactor() { return harmFactor; }
  public float competence() { return competence; }
  
  
  protected void onceInvalid() {}
  
  protected abstract Behaviour getNextStep();
  protected abstract float getPriority();
  public abstract Plan copyFor(Actor other);
  
  
  
  /**  Various utility methods for modifying plan priority (primarily invoked
    *  above.)
    */
  //
  //  TODO:  Incorporate estimate of dangers along entire route using
  //  path-caching.
  public static float rangePenalty(Base base, Target from, Target to) {
    if (from == null || to == null) return 0;
    final float SS   = Stage.SECTOR_SIZE;  //  TODO:  Modify by move speed!
    final float dist = Spacing.distance(from, to) / SS;
    
    float baseMult = 2;
    if (base != null) {
      final Tile at = base.world.tileAt(to);
      final Base owns = base.world.claims.baseClaiming(at);
      if (owns != null) baseMult -= owns.relations.relationWith(base);
    }
    
    if (dist <= 1) return (dist / 2) * baseMult;
    return (Nums.log(2, dist) + 0.5f) * baseMult;
  }
  
  
  public static float dangerPenalty(Target t, Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final Tile at = actor.origin();
    float danger = actor.base().dangerMap.sampleAround(
      at.x, at.y, Stage.SECTOR_SIZE
    );
    if (danger < 0) return 0;
    //danger *= 1 + actor.traits.relativeLevel(Qualities.NERVOUS);
    final float strength = actor.senses.powerLevel();
    
    float penalty = danger / (1 + strength);
    if (report) {
      I.say("\nGetting danger penalty for "+actor);
      I.say("  Combat strength: "+strength);
      I.say("  Danger sample:   "+danger  );
      I.say("  Final penalty:   "+penalty );
    }
    return penalty;
  }
  
  
  public static float competition(Class planClass, Target t, Actor actor) {
    float competition = 0;
    final Stage world = actor.world();
    for (Behaviour b : world.activities.allTargeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b;
        if (plan.getClass() != planClass) continue;
        if (plan.actor() == null || plan.actor() == actor) continue;
        competition += plan.successChanceFor(plan.actor());
      }
    }
    return competition;
  }
  
  
  public static float competition(Plan match, Target t, Actor actor) {
    float competition = 0;
    final Stage world = actor.world();
    for (Behaviour b : world.activities.allTargeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b;
        if (plan.actor() == actor) continue;
        if (! plan.matchesPlan(match)) continue;
        competition += plan.successChanceFor(plan.actor());
      }
    }
    return competition;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String toString() {
    final StringDescription desc = new StringDescription();
    describeBehaviour(desc);
    return desc.toString();
  }
  
  
  protected boolean lastStepIs(String methodName) {
    if (! (lastStep instanceof Action)) return false;
    return ((Action) lastStep).methodName().equals(methodName);
  }
  
  
  protected boolean needsSuffix(Description d, String defaultPrefix) {
    if (lastStep == null) {
      d.append(defaultPrefix);
      return true;
    }
    if (lastStep instanceof Action) {
      lastStep.describeBehaviour(d);
      return true;
    }
    else {
      lastStep.describeBehaviour(d);
      return false;
    }
  }
  
  
  protected Target lastStepTarget() {
    if (lastStep == null) return null;
    if (lastStep instanceof Action) return ((Action) lastStep).subject();
    if (lastStep instanceof Plan  ) return ((Plan  ) lastStep).lastStepTarget();
    return null;
  }
  
  
  public static String priorityDescription(float priority) {
    final int maxIndex = PRIORITY_DESCRIPTIONS.length;
    final float index = (priority / PARAMOUNT) * (maxIndex - 1);
    return PRIORITY_DESCRIPTIONS[Nums.clamp((int) index, maxIndex)];
  }
}




/*
//  We do not cache steps for dormant or 'under consideration' plans, since
//  that can screw up proper sequence of evaluation/execution.  Start from
//  scratch instead.
if (! isActive()) {
  if (report) I.say("\nNEXT STEP GOT WHILE INACTIVE.");
  clearEval(actor);
  return getNextStep();
}
//*/



