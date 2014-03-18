/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.game.actors ;
import java.lang.reflect.* ;

import code.game.building.*;
import code.game.civilian.*;
import code.game.common.*;
import code.game.common.Session.Saveable;
import code.game.tactical.*;
import code.user.*;
import code.util.*;



//
//  TODO:  Include a copyPlan(Actor other) method here, for the sake of asking
//  other actors to do favours, et cetera.



public abstract class Plan implements Saveable, Behaviour {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean verbose = false ;
  
  final Saveable signature[] ;
  final int hash ;
  protected Actor actor ;
  protected Behaviour nextStep = null, lastStep = null ;
  
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
  
  
  public boolean valid() {
    for (Saveable o : signature) if (o instanceof Target) {
      final Target t = (Target) o ;
      if (! t.inWorld()) return false ;
    }
    if (actor != null && ! actor.inWorld()) return false ;
    return true ;
  }
  
  //public abstract Plan copyFor(Actor other) ;
  
  
  
  /**  Default implementations of Behaviour methods-
    */
  //
  //  TODO:  This needs to return the appropriate motion mode for the actor at
  //  the given time.
  public int motionType(Actor actor) {
    return MOTION_ANY ;
  }
  
  

  public void abortBehaviour() {
    if (! begun()) return ;
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
    //
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
  
  
  protected void onceInvalid() {}
  
  protected abstract Behaviour getNextStep() ;
  
  
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
  
  
  public boolean begun() {
    return actor != null && nextStep != null ;
  }
  
  
  public Actor actor() {
    return actor ;
  }
  
  
  
  /**  Assorted utility evaluation methods-
    */
  final private static float IL2 = 1 / (float) Math.log(2) ;
  
  
  public static float rangePenalty(Target a, Target b) {
    if (a == null || b == null) return 0 ;
    final float SS = World.SECTOR_SIZE ;
    final float dist = Spacing.distance(a, b) * 1.0f / SS ;
    if (dist <= 1) return dist ;
    return IL2 * (float) Math.log(dist) ;
  }
  
  
  public static float dangerPenalty(Target t, Actor actor) {
    //
    //  TODO:  Incorporate estimate of dangers along entire route using
    //  path-caching.
    if (actor.base() == null) return 0 ;  //  TODO:  REMOVE THIS
    final Tile at = actor.world().tileAt(t) ;
    float danger = actor.base().dangerMap.shortTermVal(at) ;
    if (danger < 0) return 0 ;
    danger *= actor.traits.scaleLevel(Abilities.NERVOUS) ;
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
  
  
  protected float successChance() {
    return 1 ;
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







