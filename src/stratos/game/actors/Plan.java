/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.user.*;
import stratos.util.*;
import org.apache.commons.math3.util.FastMath;



//
//  TODO:  I think that the whole Plan structure needs to be polished.  Keep it
//  all recursive and internal, rather than keeping the stack within the
//  actor's mind?


public abstract class Plan implements Saveable, Behaviour {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MOTIVE_INIT      = -1,
    MOTIVE_LEISURE   =  0,
    MOTIVE_DUTY      =  1,
    MOTIVE_EMERGENCY =  2,
    MOTIVE_MISSION   =  3,
    MOTIVE_CANCELLED =  5;
  final static float
    NULL_PRIORITY = -100;
  
  private static boolean verbose = false, evalVerbose = false;
  
  final public Target subject;
  final boolean persistent;
  protected Actor actor;
  
  protected float
    lastEvalTime = -1,
    priorityEval = NULL_PRIORITY;
  protected Behaviour
    nextStep = null,
    lastStep = null;
  
  private int motiveType = MOTIVE_INIT;
  private float motiveBonus = 0;
  
  private float harmFactor, competeFactor;
  
  
  
  protected Plan(Actor actor, Target subject, boolean persistent) {
    this.actor = actor;
    this.subject = subject;
    this.persistent = persistent;
    if (subject == null) I.complain("NULL PLAN SUBJECT");
  }
  
  
  public Plan(Session s) throws Exception {
    s.cacheInstance(this);
    this.actor = (Actor) s.loadObject();
    this.subject = s.loadTarget();
    this.persistent = s.loadBool();
    
    this.lastEvalTime = s.loadFloat();
    this.priorityEval = s.loadFloat();
    this.nextStep = (Behaviour) s.loadObject();
    this.lastStep = (Behaviour) s.loadObject();
    this.motiveType = s.loadInt();
    this.motiveBonus = s.loadFloat();
    
    this.harmFactor = s.loadFloat();
    this.competeFactor = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(actor);
    s.saveTarget(subject);
    s.saveBool(persistent);
    
    s.saveFloat(lastEvalTime);
    s.saveFloat(priorityEval);
    s.saveObject(nextStep);
    s.saveObject(lastStep);
    s.saveInt(motiveType);
    s.saveFloat(motiveBonus);
    
    s.saveFloat(harmFactor);
    s.saveFloat(competeFactor);
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (p == null || p.getClass() != this.getClass()) return false;
    return this.subject == p.subject;
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
    return persistent;
  }
  
  
  public void abortBehaviour() {
    if (! hasBegun()) return;
    if (verbose && I.talkAbout == actor) {
      I.say("\n"+actor+" Aborting plan! "+this+" "+this.hashCode());
      new Exception().printStackTrace();
    }
    nextStep = lastStep = null;
    actor.mind.cancelBehaviour(this);
    setMotive(MOTIVE_CANCELLED, 0);
  }
  
  
  public float priorityFor(Actor actor) {
    if (this.actor != actor) {
      if (this.actor != null) {
        I.complain("PLAN CANNOT SWITCH ACTOR! "+actor+" vs. "+this.actor);
        return -1;
      }
      else {
        this.actor = actor;
        priorityEval = NULL_PRIORITY;
      }
    }
    final float time = actor.world().currentTime();
    if (priorityEval != NULL_PRIORITY && time - lastEvalTime < 1) {
      return priorityEval;
    }
    lastEvalTime = time;
    priorityEval = 0;  //Note: This helps avoid certain types of infinite loop.
    priorityEval = getPriority();
    return priorityEval;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor && hasBegun();
    if (motiveType == MOTIVE_CANCELLED) return null;
    if (report) I.say("\nFinding next step for "+this);
    
    if (this.actor != actor) {
      this.actor = actor;
      nextStep = null;
      if (report) I.say("NEXT STEP IS NULL: DIFFERENT ACTOR");
    }
    if (! valid()) {
      onceInvalid();
      if (report) I.say("NEXT STEP IS NULL: NOT VALID");
      return nextStep = null;
    }
    
    //  We do not cache steps for dormant or 'under consideration' plans, since
    //  that can screw up proper sequence of evaluation/execution.  Start from
    //  scratch instead.
    if (! actor.mind.doing(this)) {
      if (report) {
        I.say("NEXT STEP IS NULL: NOT ACTIVE");
        //new Exception().printStackTrace();
      }
      nextStep = null;
      return getNextStep();
    }
    else if (
      nextStep == null || nextStep.finished() ||
      nextStep.nextStepFor(actor) == null
    ) {
      nextStep = getNextStep();
      if (nextStep != null) lastStep = nextStep;
      else if (report) I.say("NEXT STEP IS NULL: WAS ACTIVE");
      priorityEval = NULL_PRIORITY;
    }
    return nextStep;
  }
  
  
  public boolean finished() {
    final boolean report = verbose && hasBegun() && I.talkAbout == actor;
    if (motiveType == MOTIVE_CANCELLED) return true;
    if (actor == null) return false;
    
    if (nextStepFor(actor) == null) {
      if (report) I.say("NO NEXT STEP: "+this+" "+hashCode());
      return true;
    }
    if (this == actor.mind.rootBehaviour()) {
      if (priorityFor(actor) <= 0) {
        if (report) I.say("NO PRIORITY: "+this+" "+hashCode());
        return true;
      }
    }
    return false;
  }
  
  
  public boolean hasBegun() {
    return actor != null && lastStep != null;
  }
  
  
  public Actor actor() {
    return actor;
  }
  
  
  
  /**  Assorted utility evaluation methods-
    */
  public Plan setMotive(int type, float bonus) {
    this.motiveType = type;
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
    * @param specialBonus
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
    float specialBonus,
    
    float subjectHarm,
    float peersCompete,
    float failRisk,
    
    Skill baseSkills[],
    Trait baseTraits[],
    float distanceCheck,
    
    boolean report
  ) {
    if (motiveType == MOTIVE_CANCELLED) return PRIORITY_NEVER;
    
    if (report) {
      I.say("\nEvaluating priority for "+this);
      I.say("  Default priority range:      "+defaultRange);
    }
    this.harmFactor    = subjectHarm ;
    this.competeFactor = peersCompete;
    float priority = PARAMOUNT, relation = 0;
    
    //  Firstly, we calculate the effect of internal and external motivations,
    //  such as how much we like the subject versus how much harm will be done
    //  to them.
    if (subjectHarm != 0) {
      relation = actor.relations.valueFor(subject);
      final float
        mulWeight = FastMath.abs(subjectHarm),
        mulSign   = subjectHarm > 0 ? -1 : 1,
        mulLevel  = priority * relation * mulSign;
      priority = (priority * (1 - mulWeight)) + (mulLevel * mulWeight);
    }
    if (specialBonus != 0) {
      defaultRange += specialBonus / 2f;
      priority     += specialBonus;
    }
    if (motiveType != MOTIVE_INIT) {
      defaultRange = (motiveBonus + defaultRange) / 2f;
      priority     = (motiveBonus + priority    ) / 2f;
    }
    if (report) {
      I.say("  Motive type/bonus:           "+motiveType+"/"+motiveBonus);
      I.say("  Harm factor/relation:        "+subjectHarm+"/"+relation);
      I.say("  Special modifier:            "+specialBonus);
      I.say("  After motive effects:        "+priority);
    }
    if (defaultRange <= 0 || priority <= 0) return PRIORITY_NEVER;
    
    //  Then we incorporate quantitative adjustments based on temperament and
    //  skill aptitudes, along with the effects of competition or co-operation:
    float
      skillAvg = 0, skillMax = -1, skillBonus = 0,
      traitAvg = 0, traitMax = -1, traitBonus = 0,
      peersCount = 0, competePenalty = 0;
    
    if (peersCompete != 0 && (peersCompete < 0 || ! hasBegun())) {
      peersCount = competition(this, subject, actor);
      competePenalty = peersCount * peersCompete * CASUAL;
    }
    
    if (baseSkills != null) for (Skill skill : baseSkills) {
      final float level = (actor.traits.traitLevel(skill) - 5) / 20;
      skillAvg += level / baseSkills.length;
      skillMax = FastMath.max(skillMax, level);
    }
    else skillMax = 0;
    skillBonus = ROUTINE * (skillAvg + skillMax) / 2;
    
    if (baseTraits != null) for (Trait trait : baseTraits) {
      final float level = actor.traits.relativeLevel(trait);
      traitAvg += level / baseTraits.length;
      traitMax = FastMath.max(traitMax, level);
    }
    else traitMax = 0;
    traitBonus = ROUTINE * (traitAvg + traitMax) / 2;
    
    priority += skillBonus;
    priority += traitBonus;
    priority -= competePenalty;
    if (report) {
      I.say("  Skill/traits effect:         "+skillBonus+"/"+traitBonus);
      I.say("  Peers/competition penalty:   "+peersCount+"/"+competePenalty);
      I.say("  Priority before clamp/scale: "+priority);
    }
    
    //  With all that done, we clamp the priority to within something
    //  resembling the default range.
    priority *= defaultRange * 1f / PARAMOUNT;
    final float
      min = defaultRange * 0.0f,
      max = defaultRange * 2.0f;
    priority = Visit.clamp(priority, min, max);
    if (report) {
      I.say("  Priority after clamp/scale:  "+priority);
    }
    
    //  And finally, we include the off-putting effects of distance, danger,
    //  and the potential costs of failure, together with any special bonuses
    //  thrown in at the start.
    float
      chancePenalty = 0,
      rangePenalty  = 0,
      dangerPenalty = 0;
    if (failRisk > 0) {
      final float chance = successChance();
      chancePenalty = (1 - chance) * failRisk * PARAMOUNT;
    }
    if (distanceCheck != 0) {
      final float range = rangePenalty(actor, subject);
      rangePenalty = range * distanceCheck;
      final float danger = dangerPenalty(subject, actor) * (1f + failRisk);
      dangerPenalty = danger * range / 2f;
    }
    priority -= chancePenalty;
    priority -= rangePenalty ;
    priority -= dangerPenalty;
    
    if (report) {
      I.say("  Distance is:                 "+Spacing.distance(actor, subject));
      I.say("  Chance penalty is:           "+chancePenalty);
      I.say("  Range/Danger penalty is:     "+rangePenalty+"/"+dangerPenalty);
      I.say("  Final priority:              "+priority);
    }
    return priority;
  }
  
  
  public float harmFactor()    { return harmFactor   ; }
  public float competeFactor() { return competeFactor; }
  
  protected float successChance() { return 1; }
  protected void onceInvalid() {}
  
  protected abstract Behaviour getNextStep();
  protected abstract float getPriority();
  
  public abstract Plan copyFor(Actor other);
  //public abstract Memory makeMemory();  //TODO:  IMPLEMENT
  
  
  
  /**  Various utility methods for modifying plan priority (primarily invoked
    *  above.)
    */
  public static float rangePenalty(Target a, Target b) {
    if (a == null || b == null) return 0;
    final float SS = Stage.SECTOR_SIZE;  //  TODO:  Modify by move speed!
    final float dist = Spacing.distance(a, b) / SS;
    if (dist <= 1) return dist / 2;
    return ((float) FastMath.log(2, dist)) + 0.5f;
  }
  
  
  public static float dangerPenalty(Target t, Actor actor) {
    //
    //  TODO:  Incorporate estimate of dangers along entire route using
    //  path-caching.
    //if (actor.base() == null) return 0;
    final Tile at = actor.world().tileAt(t);
    float danger = actor.base().dangerMap.sampleAt(at.x, at.y);
    if (danger < 0) return 0;
    danger *= 1 + actor.traits.relativeLevel(Qualities.NERVOUS);
    final float strength = actor.senses.powerLevel();
    
    if (evalVerbose && I.talkAbout == actor) {
      I.say("  Combat strength: "+strength);
      I.say("  Danger sample: "+danger);
    }
    return danger * 0.1f / (1 + strength);
  }
  
  
  public static float competition(Class planClass, Target t, Actor actor) {
    float competition = 0;
    final Stage world = actor.world();
    for (Behaviour b : world.activities.targeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b;
        if (plan.getClass() != planClass) continue;
        if (plan.actor() == null || plan.actor() == actor) continue;
        competition += plan.successChance();
      }
    }
    return competition;
  }
  
  
  public static float competition(Plan match, Target t, Actor actor) {
    float competition = 0;
    final Stage world = actor.world();
    for (Behaviour b : world.activities.targeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b;
        if (plan.actor() == actor) continue;
        if (! plan.matchesPlan(match)) continue;
        competition += plan.successChance();
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
    if (lastStep instanceof Plan) return ((Plan) lastStep).lastStepTarget();
    return null;
  }
  
  
  
  /**  Validation methods, intended to ensure that Plans can be stored
    *  compactly as memories-
    */
  //  TODO:  Implement this.
  /*
  private static Table <Class, Boolean> validations = new Table(100);
  
  
  private static boolean validatePlanClass(Class planClass) {
    final Boolean valid = validations.get(planClass);
    if (valid != null) return valid;
    
    final String name = planClass.getSimpleName();
    boolean okay = true;
    int dataSize = 0;
    
    for (Field field : planClass.getFields()) {
      final Class type = field.getType();
      if (type.isPrimitive()) dataSize += 4;
      else if (Saveable.class.isAssignableFrom(type)) dataSize += 4;
      else {
        I.complain(name+" contains non-saveable data: "+field.getName());
        okay = false;
      }
    }
    if (dataSize > 40) {
      I.complain(name+" has too many data fields.");
      okay = false;
    }
    
    validations.put(planClass, okay);
    return okay;
  }
  //*/
}







