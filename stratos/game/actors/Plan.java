/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import java.lang.reflect.* ;

import org.apache.commons.math3.util.FastMath;

import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;



//
//  TODO:  Include a copyPlan(Actor other) method here, for the sake of asking
//  other actors to do favours, et cetera.



public abstract class Plan implements Saveable, Behaviour {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean verbose = false, evalVerbose = false;
  
  final Saveable signature[] ;
  final int hash ;
  protected Actor actor ;
  protected Behaviour
    nextStep = null,
    lastStep = null ;
  public float priorityMod = 0 ;
  
  
  
  protected Plan(Actor actor, Saveable... signature) {
    this(actor, ROUTINE, null, signature) ;
  }
  
  
  protected Plan(
    Actor actor, float priority, String desc,
    Saveable... signature
  ) {
    this.actor = actor ;
    this.signature = signature ;
    this.hash = Table.hashFor((Object[]) signature) ;
  }
  
  
  public Plan(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.actor = (Actor) s.loadObject() ;
    final int numS = s.loadInt() ;
    this.signature = new Saveable[numS] ;
    for (int i = 0 ; i < numS ; i++) signature[i] = s.loadObject() ;
    this.hash = Table.hashFor((Object[]) signature) ;
    
    this.nextStep = (Behaviour) s.loadObject() ;
    this.lastStep = (Behaviour) s.loadObject() ;
    this.priorityMod = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(actor) ;
    s.saveInt(signature.length) ;
    for (Saveable o : signature) s.saveObject(o) ;
    s.saveObject(nextStep) ;
    s.saveObject(lastStep) ;
    s.saveFloat(priorityMod) ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (p == null || p.getClass() != this.getClass()) return false ;
    for (int i = 0 ; i < signature.length ; i++) {
      final Object s = signature[i], pS = p.signature[i] ;
      if (s == null && pS != null) return false ;
      if (! s.equals(pS)) return false ;
    }
    return true ;
  }
  
  
  public int planHash() {
    return hash ;
  }
  
  
  
  /**  Default implementations of Behaviour methods-
    */
  public int motionType(Actor actor) {
    return MOTION_ANY ;
  }
  
  
  public boolean valid() {
    for (Saveable o : signature) if (o instanceof Target) {
      final Target t = (Target) o ;
      if (! t.inWorld()) return false ;
    }
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
    }
    return nextStep ;
  }
  
  
  public Behaviour nextStep() {
    return nextStepFor(actor) ;
  }
  
  
  public boolean finished() {
    if (actor == null) return false ;
    if (this == actor.mind.rootBehaviour()) {
      if (priorityFor(actor) <= 0) return true ;
    }
    if (nextStep() == null) {
      if (verbose) I.sayAbout(actor, "NO NEXT STEP") ;
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
    float dangerFactor
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    float priority = ROUTINE + specialModifier;
    if (report) I.say("Evaluating priority for "+this);
    
    if (baseSkills != null) for (Skill skill : baseSkills) {
      final float level = actor.traits.traitLevel(skill);
      priority += (level - 5) / (5 * baseSkills.length);
    }
    
    if (baseTraits != null) for (Trait trait : baseTraits) {
      final float level = actor.traits.relativeLevel(trait);
      priority += level * CASUAL / baseTraits.length;
    }
    
    if (subjectHarm != 0) {
      final float relation = actor.mind.relationValue(subject);
      priority -= relation * subjectHarm * PARAMOUNT;
    }
    
    if (peersCompete != 0 && ! hasBegun()) {
      final float competition = competition(this, subject, actor);
      priority -= competition * peersCompete * CASUAL / 2;
    }
    
    if (report) I.say("Priority before clamp/scale is: "+priority);
    if (priority <= 0) return 0;
    
    priority = Visit.clamp(priority / ROUTINE, 0.5f, 2.5f);
    priority *= defaultPriority;
    
    if (dangerFactor > 0) {
      final float chance = successChance();
      priority -= (1 - chance) * dangerFactor * ROUTINE;
    }
    
    if (distanceCheck != 0) {
      final float range = rangePenalty(actor, subject) * distanceCheck;
      priority -= range;
      if (dangerFactor > 0) {
        final float danger = dangerPenalty(subject, actor);
        priority -= danger * (range + 2) / 2f;
      }
    }
    
    if (report) I.say("Priority after clamp/scale, dist/danger: "+priority);
    return priority;
  }
  
  
  //  TODO:  Consider making these methods abstract too?
  protected float successChance() {
    return 1 ;
  }
  
  protected void onceInvalid() {}
  
  protected abstract Behaviour getNextStep() ;
  
  
  
  
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
    danger *= actor.traits.scaleLevel(Qualities.NERVOUS) ;
    return danger * 0.1f / (1 + Combat.combatStrength(actor, null)) ;
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
    if (lastStep instanceof Action) return ((Action) lastStep).target() ;
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







