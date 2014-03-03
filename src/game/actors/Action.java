/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;

import java.lang.reflect.* ;


//
//  TODO:  A lot of the methods here could more reasonably be moved to a
//  dedicated Motion class.
//  TODO:  Consider getting rid of separate Move Targets entirely.  It might
//  well be turning out as more hassle than it's worth.

//  TODO:  ...You need to arrange for actions to terminate if you wind up
//  staying in one place too long (which likely means you're stuck.)


public class Action implements Behaviour, AnimNames {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final public static int
    QUICK    = 1,
    CAREFUL  = 2,
    CARRIES  = 4,
    RANGED   = 8 ;
  
  private static boolean verbose = false ;
  
  
  final public Actor actor ;
  final public Session.Saveable basis ;
  final public Method toCall ;
  
  private float priority ;
  
  private int properties ;
  private byte inRange = -1 ;
  private Target actionTarget, moveTarget ;
  private float progress, oldProgress ;
  
  final String animName, description ;
  
  
  
  public Action(
    Actor actor, Target target,
    Session.Saveable basis, String methodName,
    String animName, String description
  ) {
    if (actor == null || target == null)
      I.complain("Null arguments for action!") ;
    this.actor = actor ;
    this.basis = basis ;
    this.toCall = namedMethodFor(basis, methodName) ;
    this.priority = ROUTINE ;
    this.actionTarget = this.moveTarget = target ;
    this.animName = animName ;
    this.description = description ;
  }
  
  
  public Action(Session s) throws Exception {
    s.cacheInstance(this) ;
    
    actor = (Actor) s.loadObject() ;
    basis = s.loadObject() ;
    toCall = namedMethodFor(basis, s.loadString()) ;
    priority = s.loadFloat() ;
    
    properties = s.loadInt() ;
    inRange = (byte) s.loadInt() ;
    actionTarget = s.loadTarget() ;
    moveTarget = s.loadTarget() ;
    
    progress = s.loadFloat() ;
    oldProgress = s.loadFloat() ;
    animName = s.loadString() ;
    description = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(actor) ;
    s.saveObject(basis) ;
    s.saveString(toCall.getName()) ;
    s.saveFloat(priority) ;
    
    s.saveInt(properties) ;
    s.saveInt(inRange) ;
    s.saveTarget(actionTarget) ;
    s.saveTarget(moveTarget) ;
    
    s.saveFloat(progress) ;
    s.saveFloat(oldProgress) ;
    s.saveString(animName) ;
    s.saveString(description) ;
  }
  
  
  
  public void setPriority(float p) {
    this.priority = p ;
  }
  
  
  public void setMoveTarget(Target t) {
    if (t == null) I.complain("MOVE TARGET MUST BE NON-NULL.") ;
    this.moveTarget = t ;
  }
  
  
  public void setProperties(int p) {
    this.properties = p ;
  }
  
  
  public Target target() {
    return actionTarget ;
  }
  
  
  public Target movesTo() {
    return moveTarget ;
  }
  
  
  public String methodName() {
    return toCall.getName() ;
  }
  
  
  public boolean ranged()  { return (properties & RANGED ) != 0 ; }
  public boolean careful() { return (properties & CAREFUL) != 0 ; }
  public boolean quick()   { return (properties & QUICK  ) != 0 ; }
  public boolean carries() { return (properties & CARRIES) != 0 ; }
  
  
  
  /**  Implementing the Behaviour contract-
    */
  public float priorityFor(Actor actor) {
    return priority ;
  }
  
  
  public boolean finished() {
    if (progress == -1) return true ;
    return (inRange == 1) && (progress >= 1) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null ;
    return this ;
  }
  
  
  public void abortBehaviour() {
    progress = -1 ;
    inRange = -1 ;
    actor.mind.cancelBehaviour(this) ;
    actor.assignAction(null) ;
  }
  
  
  public boolean valid() {
    return
      actor.inWorld() && moveTarget.inWorld() &&
      ! actionTarget.destroyed() ;
  }
  
  
  public boolean begun() {
    return actor.currentAction() == this ;
  }
  
  
  public Actor actor() {
    return actor ;
  }
  
  
  
  /**  Helper methods for dealing with motion-
    */
  public static float moveRate(Actor actor, boolean basic) {
    int motionType = MOTION_NORMAL ; for (Behaviour b : actor.mind.agenda) {
      final int MT = b.motionType(actor) ;
      if (MT != MOTION_ANY) { motionType = MT ; break ; }
    }
    
    final float luck = actor.motionWalks() ? moveLuck(actor) : 0.5f ;
    float rate = actor.health.baseSpeed() ;
    if (motionType == MOTION_SNEAK) rate *= (2 - luck) / 4 ;
    else if (motionType == MOTION_FAST) rate *= 2 * luck ;
    else rate += (luck - 0.5f) / 2 ;
    if (basic) return rate ;
    
    //  TODO:  Must also account for the effects of fatigue and encumbrance...
    final int pathType = actor.origin().pathType() ;
    switch (pathType) {
      case (Tile.PATH_HINDERS) : rate *= 0.8f ; break ;
      case (Tile.PATH_CLEAR  ) : rate *= 1.0f ; break ;
      case (Tile.PATH_ROAD   ) : rate *= 1.2f ; break ;
    }
    return rate ;
  }
  
  
  public static float moveLuck(Actor actor) {
    //
    //  This is employed during chases and stealth conflicts, so that the
    //  outcome is less (obviously) deterministic.  However, it must be
    //  constant at a given position and time.
    final Tile o = actor.origin() ;
    int var = o.world.terrain().varAt(o) ;
    var += (o.x * o.world.size) + o.y ;
    var -= o.world.currentTime() ;
    var ^= actor.hashCode() ;
    return (1 + (float) Math.sqrt(Math.abs(var % 10) / 4.5f)) / 2 ;
  }

  
  public int motionType(Actor actor) {
    if (quick()  ) return MOTION_FAST  ;
    if (careful()) return MOTION_SNEAK ;
    return MOTION_NORMAL ;
  }
  
  
  private void updateMotion(boolean active) {
    final boolean report = verbose && I.talkAbout == actor ;
    
    //  Firstly, we establish current displacements between actor and target,
    //  motion target & action target, how far the actor can see, and whether
    //  sticking to the full tile path is required-
    final boolean mustBoard = (
      (! ranged()) && (moveTarget instanceof Boardable)
    ) ;
    //final boolean blockage = ! actor.motion.checkEndPoint(moveTarget) ;
    final float
      sightRange = actor.health.sightRange(),
      motionDist = Spacing.distance(actor, moveTarget),
      actionDist = Spacing.distance(actor, actionTarget),
      separation = Spacing.distance(moveTarget, actionTarget) ;
    
    //  We also need to calculate an appropriate maximum distance in order for
    //  the action to progress-
    float maxDist = 0.01f ;
    if (ranged()) maxDist += sightRange * (inRange == 1 ? 2 : 1) ;
    else if (inRange == 1) maxDist += progress + 0.5f ;
    
    //  In order for the action to execute, the actor must both be close enough
    //  to the target and facing in the right direction.  If the target is
    //  close enough to see, you can consider ignoring tile-pathing in favour
    //  of closing directly on the subject.  If, on the other hand, the target
    //  should be visible, but isn't, then path towards it more directly.
    Target pathsTo = moveTarget ;
    boolean closed = false, approaching = false, facing = false ;
    final Target step = actor.motion.nextStep(), closeOn ;
    
    if (mustBoard) {
      approaching = actor.aboard() == moveTarget ;
      closed = approaching && (motionDist - maxDist < separation) ;
      closeOn = closed ? actionTarget : step ;
    }
    else {
      final boolean seen = MobileMotion.hasLineOfSight(
        actor, actionTarget, Math.max(maxDist, sightRange)
      ) ;
      if (Math.min(motionDist, actionDist) < maxDist && ! seen) {
        pathsTo = actionTarget ;
      }
      if (Pathing.blockedBy(pathsTo, actor)) {
        pathsTo = Spacing.nearestOpenTile(pathsTo, actor) ;
      }
      closed = seen && (actionDist <= maxDist) ;
      approaching = closed || (seen && (actionDist <= (maxDist + 1))) ;
      closeOn = approaching ? actionTarget : step ;
    }
    actor.motion.updateTarget(pathsTo) ;
    facing = actor.motion.facingTarget(closeOn) ;
    
    if (report) {
      I.say("Action target is: "+actionTarget) ;
      I.say("  Move target is: "+moveTarget) ;
      I.say("  Path target is: "+actor.motion.target()+", step: "+step) ;
      I.say("  Faced is: "+closeOn+", must board: "+mustBoard) ;
      I.say("  Current position: "+actor.aboard()) ;
      I.say("  Closed/facing: "+closed+"/"+facing+", doing update? "+active) ;
      I.say("  Is ranged? "+ranged()+", approaching? "+approaching) ;
      final float distance = Spacing.distance(actor, actor.motion.target()) ;
      I.say("  Distance: "+distance+", maximum: "+maxDist+"\n") ;
    }
    
    //  If both facing and proximity are satisfied, toggle the flag which
    //  allows action delivery to proceed.  (If delivery was already underway,
    //  cancel the action.)
    final byte oldRange = inRange ;
    inRange = (byte) ((closed && facing) ? 1 : 0) ;
    if (inRange != oldRange) {
      if (oldRange == 1) { abortBehaviour() ; return ; }
      else progress = oldProgress = 0 ;
    }
    
    //  If active updates to pathing & motion are called for, make them.
    if (active) {
      final float moveRate = moveRate(actor, false) ;
      actor.motion.headTowards(closeOn, moveRate, ! closed) ;
      if (! closed) actor.motion.applyCollision(moveRate) ;
    }
  }
  
  
  
  /**  Actual execution of associated behaviour-
    */
  private float actionDuration() {
    float duration = 1 ;
    if (quick()  ) duration /= 2 ;
    if (careful()) duration *= 2 ;
    return duration ;
  }
  
  
  protected void updateAction(boolean active) {
    if (verbose) I.sayAbout(actor, "Updating action: "+progress) ;
    if (finished()) {
      oldProgress = progress = 1 ;
      return ;
    }
    else {
      oldProgress = progress ;
      updateMotion(active) ;
    }
    if (inRange == 1) {
      progress += 1f / (actionDuration() * World.UPDATES_PER_SECOND) ;
      progress = Visit.clamp(progress, 0, 1) ;
      final float duration = actionDuration() ;
      final float contact = (duration - 0.25f) / duration ;
      if (oldProgress <= contact && progress > contact) applyEffect() ;
    }
    if (inRange == 0) progress += (
      moveRate(actor, false) *
      actor.moveAnimStride() /
      World.UPDATES_PER_SECOND
    ) ;
  }
  
  
  public void applyEffect() {
    try { toCall.invoke(basis, actor, actionTarget) ; }
    catch (Exception e) {
      I.say("PROBLEM WITH ACTION: "+toCall.getName()) ;
      I.report(e) ;
    }
  }
  
  
  
  /**  Caching methods for later execution-
    */
  final private static Table <
    Class <? extends Object>,
    Table <String, Method>
  > actionMethods = new Table <
    Class <? extends Object>,
    Table <String, Method>
  > (1000) ;
  
  
  private static Method namedMethodFor(Object plans, String methodName) {
    if (plans == null || methodName == null) return null ;
    Table <String, Method> aM = actionMethods.get(plans.getClass()) ;
    if (aM == null) {
      aM = new Table <String, Method> (20) ;
      for (Method method : plans.getClass().getMethods()) {
        aM.put(method.getName(), method) ;
      }
      actionMethods.put(plans.getClass(), aM) ;
    }
    final Method method = aM.get(methodName) ;
    if (method == null) I.complain(
      "NO SUCH METHOD! "+methodName+" FOR CLASS: "+plans
    ) ;
    if (! method.isAccessible()) {
      final Class <? extends Object> params[] = method.getParameterTypes() ;
      if (
        params.length != 2 ||
        ! Actor.class.isAssignableFrom(params[0]) ||
        ! Target.class.isAssignableFrom(params[1])
      ) I.complain("METHOD HAS BAD ARGUMENT SET!") ;
      method.setAccessible(true) ;
    }
    return method ;
  }
  
  
  
  /**  Methods to support rendering-
    */
  protected void configSprite(Sprite sprite, Rendering rendering) {
    //
    //  In the case of a pushing animation, you actually need to set different
    //  animations for the upper and lower body.
    ///I.sayAbout(actor, "anim progress: "+animProgress());
    sprite.setAnimation(animName(), animProgress(rendering));
  }
  
  
  protected float animProgress(Rendering rendering) {
    final float alpha = Rendering.frameTime() ;
    final float AP = ((progress * alpha) + (oldProgress * (1 - alpha))) ;
    if (AP > 1) return AP % 1 ;
    return AP ;
  }
  
  
  protected String animName() {
    if (inRange == 1) {
      return animName ;
    }
    else {
      if (quick()  ) return MOVE_FAST  ;
      if (careful()) return MOVE_SNEAK ;
      return MOVE ;
    }
  }
  
  
  public String toString() {
    return description ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append(description) ;
  }
}


