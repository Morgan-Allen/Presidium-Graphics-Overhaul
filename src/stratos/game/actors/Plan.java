/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import java.lang.reflect.* ;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;
import org.apache.commons.math3.util.FastMath;



//
//  TODO:  Include a copyPlan(Actor other) method here, for the sake of asking
//  other actors to do favours, et cetera.

public abstract class Plan implements Saveable, Behaviour {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MOTIVE_INIT      = -1,
    MOTIVE_LEISURE   =  0,
    MOTIVE_DUTY      =  1,
    MOTIVE_EMERGENCY =  2;
  final static float
    NULL_PRIORITY = -100;
  
  private static boolean verbose = false, evalVerbose = false;
  
  final Target subject;
  protected Actor actor;
  
  protected float
    lastEvalTime = -1,
    priorityEval = NULL_PRIORITY;
  protected Behaviour
    nextStep = null,
    lastStep = null;
  
  private int motiveType = MOTIVE_INIT;
  private float motiveBonus = 0;
  
  
  
  protected Plan(Actor actor, Target subject) {
    this.actor = actor;
    this.subject = subject;
  }
  
  
  public Plan(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.actor = (Actor) s.loadObject() ;
    this.subject = s.loadTarget();
    
    this.lastEvalTime = s.loadFloat();
    this.priorityEval = s.loadFloat();
    this.nextStep = (Behaviour) s.loadObject();
    this.lastStep = (Behaviour) s.loadObject();
    this.motiveType = s.loadInt();
    this.motiveBonus = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(actor) ;
    s.saveTarget(subject);
    
    s.saveFloat(lastEvalTime);
    s.saveFloat(priorityEval);
    s.saveObject(nextStep);
    s.saveObject(lastStep);
    s.saveInt(motiveType);
    s.saveFloat(motiveBonus);
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (p == null || p.getClass() != this.getClass()) return false ;
    return this.subject == p.subject;
  }
  
  
  public Target subject() {
    return subject;
  }
  
  
  
  /**  Default implementations of Behaviour methods-
    */
  public int motionType(Actor actor) {
    return MOTION_ANY ;
  }
  
  
  public boolean valid() {
    if (! subject.inWorld()) return false;
    if (actor != null && ! actor.inWorld()) return false ;
    return true ;
  }
  
  
  public void abortBehaviour() {
    if (! hasBegun()) return ;
    if (verbose && I.talkAbout == actor) {
      I.say("\n"+actor+" Aborting plan! "+this+" "+this.hashCode()) ;
      new Exception().printStackTrace() ;
    }
    nextStep = null ;
    actor.mind.cancelBehaviour(this) ;
  }
  
  
  public float priorityFor(Actor actor) {
    if (this.actor != actor) {
      if (this.actor != null) ;  //TODO:  Give some kind of message here?
      this.actor = actor ;
      priorityEval = NULL_PRIORITY;
    }
    final float time = subject.world().currentTime();
    if (priorityEval != NULL_PRIORITY && time - lastEvalTime < 1) {
      return priorityEval;
    }
    priorityEval = getPriority();
    lastEvalTime = time;
    return priorityEval;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (this.actor != actor) {
      if (this.actor != null) ;  //TODO:  Give some kind of message here?
      this.actor = actor ;
      nextStep = null ;
    }
    if (! valid()) { onceInvalid() ; return nextStep = null ; }
    
    //  We do not cache steps for dormant or 'under consideration' plans, since
    //  that can screw up proper sequence of evaluation/execution.  Start from
    //  scratch instead.
    if (! actor.mind.agenda().includes(this)) {
      nextStep = lastStep = null ;
      return getNextStep() ;
    }
    else if (nextStep == null || nextStep.finished()) {
      nextStep = getNextStep() ;
      if (nextStep != null) lastStep = nextStep ;
      priorityEval = NULL_PRIORITY;
    }
    return nextStep ;
  }
  
  
  public boolean finished() {
    if (actor == null) return false ;
    if (this == actor.mind.rootBehaviour()) {
      if (priorityFor(actor) <= 0) {
        if (verbose) I.sayAbout(actor, "NO PRIORITY: "+this+" "+hashCode()) ;
        return true ;
      }
    }
    if (nextStepFor(actor) == null) {
      if (verbose) I.sayAbout(actor, "NO NEXT STEP: "+this+" "+hashCode()) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean hasBegun() {
    return actor != null && nextStep != null ;
  }
  
  
  public Actor actor() {
    return actor ;
  }
  
  
  
  /**  Assorted utility evaluation methods-
    */
  public Plan setMotive(int type, float bonus) {
    this.motiveType = type;
    this.motiveBonus = bonus;
    return this;
  }
  
  
  final protected static float
    NO_DANGER      = 0.0f,
    MILD_DANGER    = 0.5f,
    REAL_DANGER    = 1.0f,
    EXTREME_DANGER = 2.0f,
    
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
    
    NO_MODIFIER = 0;
  final protected static Skill NO_SKILLS[] = null;
  final protected static Trait NO_TRAITS[] = null;
  
  
  //  This probably needs to be reworked.
  protected float priorityForActorWith(
    Actor actor,
    Target subject,
    float defaultPriority,
    float subjectHarm,
    float peersCompete,
    Skill baseSkills[],
    Trait baseTraits[],
    float specialModifier,
    float distanceCheck,
    float dangerFactor,
    boolean report
  ) {
    if (subject == null) I.complain("NO SUBJECT SPECIFIED");
    float
      priority = ROUTINE + specialModifier,
      skillBonus = 0, traitBonus = 0,
      harmBonus = 0, competeBonus = 0;
    
    if (motiveType != MOTIVE_INIT) {
      if (defaultPriority == FROM_MOTIVE) defaultPriority = motiveBonus;
      else defaultPriority = (motiveBonus + defaultPriority) / 2f;
    }
    else if (defaultPriority == FROM_MOTIVE) {
      I.complain("NO MOTIVATION!");
      return -1;
    }
    
    if (report) {
      I.say("\nEvaluating priority for "+this);
      I.say("  Initialised at: "+priority+", default: "+defaultPriority);
    }
    
    if (baseSkills != null) for (Skill skill : baseSkills) {
      final float level = actor.traits.traitLevel(skill);
      skillBonus += CASUAL * (level - 5) / (15 * baseSkills.length);
    }
    
    if (baseTraits != null) for (Trait trait : baseTraits) {
      final float level = actor.traits.relativeLevel(trait);
      traitBonus += level * CASUAL / baseTraits.length;
    }
    
    if (subjectHarm != 0) {
      final float relation = actor.memories.relationValue(subject);
      harmBonus = 0 - relation * subjectHarm * PARAMOUNT;
    }
    
    if (peersCompete != 0 && (peersCompete < 0 || ! hasBegun())) {
      final float competition = competition(this, subject, actor);
      competeBonus = 0 - competition * peersCompete * CASUAL / 2;
    }
    
    priority += skillBonus;
    priority += traitBonus;
    priority += harmBonus;
    priority += competeBonus;
    if (report) {
      I.say("  Skill/traits bonus: "+skillBonus+"/"+traitBonus);
      I.say("  Harm/compete bonus: "+harmBonus+"/"+competeBonus);
      I.say("  Priority before clamp/scale is: "+priority);
    }
    
    priority *= defaultPriority * 1f / ROUTINE;
    final float
      min = defaultPriority * 0.5f,
      max = defaultPriority * 2.0f;
    if (priority < min) return priority - min;
    priority = Visit.clamp(priority, min, max);
    if (report) I.say("  Priority after clamp/scale: "+priority);
    
    float chancePenalty = 0, rangePenalty = 0, dangerPenalty = 0;
    
    if (dangerFactor > 0) {
      final float chance = successChance();
      priority *= chance;
      chancePenalty = (1 - chance) * dangerFactor * PARAMOUNT;
    }
    
    if (distanceCheck != 0) {
      //  TODO:  Estimate distance from actor's home, or nearest point of
      //  refuge?  Or make that an aspect of Retreat, maybe?
      rangePenalty = rangePenalty(actor, subject) * distanceCheck;
      final float danger = dangerPenalty(subject, actor);
      dangerPenalty = danger * (rangePenalty + 2) / 2f;
    }
    
    priority -= chancePenalty;
    priority -= rangePenalty;
    priority -= dangerPenalty;
    if (report) {
      I.say("  Chance penalty is: "+chancePenalty);
      I.say("  Range/Danger penalty is: "+rangePenalty+"/"+dangerPenalty);
      I.say("  Priority after clamp/scale, dist/danger: "+priority);
    }
    return priority;
  }
  
  
  //  TODO:  Consider making these methods abstract too?
  protected float successChance() {
    return 1 ;
  }
  
  protected void onceInvalid() {}
  
  protected abstract Behaviour getNextStep();
  protected abstract float getPriority();
  
  
  
  public static float rangePenalty(Target a, Target b) {
    if (a == null || b == null) return 0 ;
    final float SS = World.SECTOR_SIZE ;
    final float dist = Spacing.distance(a, b) / SS ;
    if (dist <= 1) return 0 ;
    return ((float) FastMath.log(2, dist)) - 1 ;
  }
  
  
  public static float dangerPenalty(Target t, Actor actor) {
    //
    //  TODO:  Incorporate estimate of dangers along entire route using
    //  path-caching.
    if (actor.base() == null) return 0 ;  //  TODO:  REMOVE THIS
    final Tile at = actor.world().tileAt(t) ;
    float danger = actor.base().dangerMap.sampleAt(at.x, at.y) ;
    if (danger < 0) return 0 ;
    danger *= 1 + actor.traits.relativeLevel(Qualities.NERVOUS) ;
    final float strength = CombatUtils.combatStrength(actor, null);
    
    if (evalVerbose && I.talkAbout == actor) {
      I.say("  Combat strength: "+strength);
      I.say("  Danger sample: "+danger);
    }
    return danger * 0.1f / (1 + strength) ;
  }
  
  
  public static float competition(Class planClass, Target t, Actor actor) {
    float competition = 0 ;
    final World world = actor.world() ;
    for (Behaviour b : world.activities.targeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b ;
        if (plan.getClass() != planClass) continue ;
        if (plan.actor() == actor) continue ;
        competition += plan.successChance() ;
      }
    }
    return competition ;
  }
  
  
  public static float competition(Plan match, Target t, Actor actor) {
    float competition = 0 ;
    final World world = actor.world() ;
    for (Behaviour b : world.activities.targeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b ;
        if (plan.actor() == actor) continue ;
        if (! plan.matchesPlan(match)) continue ;
        competition += plan.successChance() ;
      }
    }
    return competition ;
  }
  
  
  //  TODO:  Implement this.
  /*
  public static float victimBonus(Actor actor, float harmMult) {
    
  }
  //*/
  
  
  public static int upgradeBonus(Target location, Object refers) {
    if (! (location instanceof Venue)) return 0 ;
    final Venue v = (Venue) location ;
    if (refers instanceof Upgrade) {
      return v.structure.upgradeLevel((Upgrade) refers) ;
    }
    else return v.structure.upgradeBonus(refers) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String toString() {
    final StringDescription desc = new StringDescription() ;
    describeBehaviour(desc) ;
    return desc.toString() ;
  }
  
  
  protected boolean describedByStep(Description d) {
    if (lastStep != null) {
      lastStep.describeBehaviour(d) ;
      return true ;
    }
    return false ;
  }
  
  
  protected Target lastStepTarget() {
    if (lastStep == null) return null ;
    if (lastStep instanceof Action) return ((Action) lastStep).subject() ;
    if (lastStep instanceof Plan) return ((Plan) lastStep).lastStepTarget() ;
    return null ;
  }
  
  
  
  /**  Validation methods, intended to ensure that Plans can be stored
    *  compactly as memories-
    */
  private static Table <Class, Boolean> validations = new Table(100) ;
  
  
  private static boolean validatePlanClass(Class planClass) {
    final Boolean valid = validations.get(planClass) ;
    if (valid != null) return valid ;
    
    final String name = planClass.getSimpleName() ;
    boolean okay = true ;
    int dataSize = 0 ;
    
    for (Field field : planClass.getFields()) {
      final Class type = field.getType() ;
      if (type.isPrimitive()) dataSize += 4 ;
      else if (Saveable.class.isAssignableFrom(type)) dataSize += 4 ;
      else {
        I.complain(name+" contains non-saveable data: "+field.getName()) ;
        okay = false ;
      }
    }
    if (dataSize > 40) {
      I.complain(name+" has too many data fields.") ;
      okay = false ;
    }
    
    validations.put(planClass, okay) ;
    return okay ;
  }
}







