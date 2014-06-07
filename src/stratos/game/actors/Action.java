/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import java.lang.reflect.* ;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  ...You need to arrange for actions to terminate if you wind up
//  staying in one place too long (which likely means you're stuck.)
public class Action implements Behaviour, AnimNames {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final public static int
    QUICK    = 1,
    CAREFUL  = 2,
    TRACKS   = 4,
    RANGED   = 8,
    NO_LOOP  = 16;
  final static byte
    STATE_INIT   = -1,
    STATE_CLOSED =  0,
    STATE_MOVE   =  1,
    STATE_SNEAK  =  2,
    STATE_RUN    =  3;
  
  private static boolean verbose = false;
  
  
  final public Actor actor ;
  final public Session.Saveable basis ;
  final public Method toCall ;
  
  private float priority ;
  private int properties ;
  private byte moveState = -1;
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
    moveState = (byte) s.loadInt() ;
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
    s.saveInt(moveState) ;
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
  
  
  public Target subject() {
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
  public boolean tracks()  { return (properties & TRACKS ) != 0 ; }
  
  
  
  /**  Implementing the Behaviour contract-
    */
  public float priorityFor(Actor actor) {
    return priority ;
  }
  
  
  public boolean finished() {
    if (progress == -1) return true ;
    return (moveState == STATE_CLOSED) && (progress >= 1) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null ;
    return this ;
  }
  
  
  public void abortBehaviour() {
    progress = -1;
    moveState = STATE_INIT;
    actor.mind.cancelBehaviour(this);
    actor.assignAction(null);
  }
  
  
  public boolean valid() {
    return
      actor.inWorld() && moveTarget.inWorld() &&
      ! actionTarget.destroyed();
  }
  
  
  public boolean hasBegun() {
    return actor.currentAction() == this ;
  }
  
  
  public Actor actor() {
    return actor ;
  }
  
  
  
  /**  Helper methods for dealing with motion-
    */
  //*
  public static float moveRate(
    Actor actor, boolean basic
  ) {
    final boolean report = false && verbose && I.talkAbout == actor;
    if (report) I.say("Deciding on motion rate:");
    int motionType = MOTION_NORMAL ; for (Behaviour b : actor.mind.agenda) {
      final int MT = b.motionType(actor);
      if (report) I.say("  Type is: "+MT);
      if (MT != MOTION_ANY) { motionType = MT ; break ; }
    }
    if (report) I.say("  Result:  "+motionType);

    float rate = actor.health.baseSpeed() ;
    if (motionType == MOTION_SNEAK) rate /= 2 ;
    else if (motionType == MOTION_FAST) rate *= 2 ;
    
    if (basic) return rate;
    
    //  TODO:  Must also account for the effects of fatigue and encumbrance.
    
    final int pathType = actor.origin().pathType() ;
    switch (pathType) {
      case (Tile.PATH_HINDERS) : rate *= 0.8f ; break ;
      case (Tile.PATH_CLEAR  ) : rate *= 1.0f ; break ;
      case (Tile.PATH_ROAD   ) : rate *= 1.2f ; break ;
    }
    
    return rate ;
  }
  //*/

  
  public int motionType(Actor actor) {
    if (quick()  ) return MOTION_FAST  ;
    if (careful()) return MOTION_SNEAK ;
    return MOTION_ANY ;
  }
  
  
  private void updateMotion(boolean active, float moveRate) {
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
    if (ranged()) maxDist += sightRange * (moveState == STATE_CLOSED ? 2 : 1) ;
    else if (moveState == STATE_CLOSED) maxDist += progress + 0.5f ;
    
    //  In order for the action to execute, the actor must both be close enough
    //  to the target and facing in the right direction.  If the target is
    //  close enough to see, you can consider ignoring tile-pathing in favour
    //  of closing directly on the subject.  If, on the other hand, the target
    //  should be visible, but isn't, then path towards it more directly.
    Target pathsTo = moveTarget ;
    boolean closed = false, approaching = false, facing = false ;
    final Target step = actor.pathing.nextStep(), closeOn ;
    
    if (contactMade() && ! tracks()) {
      pathsTo = actor.aboard();
      closeOn = actor;
      closed = approaching = facing = true;
    }
    else if (mustBoard) {
      approaching = actor.aboard() == moveTarget ;
      closed = approaching && (motionDist - maxDist < separation) ;
      closeOn = closed ? actionTarget : step ;
      facing = actor.pathing.facingTarget(closeOn);
    }
    else {
      //  TODO:  Build line-of-sight considerations into the actor's reaction
      //  algorithms instead?
      final boolean seen = Senses.hasLineOfSight(
        actor, actionTarget, Math.max(maxDist, sightRange)
      );
      
      if (Math.min(motionDist, actionDist) < maxDist && ! seen) {
        pathsTo = actionTarget ;
      }
      if (PathSearch.blockedBy(pathsTo, actor)) {
        pathsTo = Spacing.nearestOpenTile(pathsTo, actor) ;
      }
      closed = seen && (actionDist <= maxDist) ;
      approaching = closed || (seen && (actionDist <= (maxDist + 1))) ;
      closeOn = approaching ? actionTarget : step ;
      facing = actor.pathing.facingTarget(closeOn);
    }
    actor.pathing.updateTarget(pathsTo);
    
    if (report) {
      I.say("Action is: "+methodName()+" "+hashCode());
      I.say("  Action target is: "+actionTarget) ;
      I.say("  Move target is: "+moveTarget) ;
      I.say("  Path target is: "+actor.pathing.target()+", step: "+step) ;
      I.say("  Closing on: "+closeOn+", must board: "+mustBoard) ;
      final boolean blocked = PathSearch.blockedBy(actor.aboard(), actor);
      I.say("  Currently aboard: "+actor.aboard()+", blocked? "+blocked) ;
      I.say("  Closed/facing: "+closed+"/"+facing+", doing update? "+active) ;
      I.say("  Is ranged? "+ranged()+", approaching? "+approaching) ;
      final float distance = Spacing.distance(actor, actor.pathing.target()) ;
      I.say("  Distance: "+distance+", maximum: "+maxDist+"\n") ;
    }
    
    //  If both facing and proximity are satisfied, toggle the flag which
    //  allows action delivery to proceed.  (If delivery was already underway,
    //  cancel the action.)
    final byte oldState = moveState;
    final float animRate = moveRate / actor.health.baseSpeed();
    
    if (closed && facing) moveState = STATE_CLOSED;
    else if (animRate < 0.75f) moveState = STATE_SNEAK;
    else if (animRate > 1.5f ) moveState = STATE_RUN;
    else moveState = STATE_MOVE;
    
    if (moveState != oldState) {
      if (oldState == STATE_CLOSED) { abortBehaviour() ; return ; }
      else progress = oldProgress = 0 ;
    }
    
    //  If active updates to pathing & motion are called for, make them.
    if (active) {
      if (report) I.say("Move rate: "+moveRate);
      actor.pathing.headTowards(closeOn, moveRate, ! closed);
      if (! closed) actor.pathing.applyCollision(moveRate, actionTarget);
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
  
  
  private float contactTime() {
    final float duration = actionDuration() ;
    final float contact = (duration - 0.25f) / duration ;
    return contact;
  }
  
  
  private boolean contactMade() {
    return moveState == STATE_CLOSED && progress >= contactTime();
  }
  
  
  protected void updateAction(boolean active) {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("Updating action: "+progress+", target: "+actionTarget);
    if (finished()) {
      if (report) I.say("Finished!");
      oldProgress = progress = 1 ;
      return ;
    }
    
    final float moveRate = moveRate(actor, false);
    oldProgress = progress ;
    updateMotion(active, moveRate) ;
    
    if (moveState == STATE_CLOSED) {
      progress += 1f / (actionDuration() * World.UPDATES_PER_SECOND) ;
      progress = Visit.clamp(progress, 0, 1) ;
      final float contact = contactTime();
      if (oldProgress <= contact && progress > contact) applyEffect() ;
    }
    else if (moveState != STATE_INIT) {
      float speedUp = moveRate / actor.health.baseSpeed();
      speedUp = (1 + speedUp) / (2 * World.UPDATES_PER_SECOND);
      progress += actor.moveAnimStride() * speedUp;
    }
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
  protected void configSprite(Sprite s, Rendering rendering) {
    final SolidSprite sprite = (SolidSprite) s;
    //
    //  In the case of a pushing animation, you actually need to set different
    //  animations for the upper and lower body.  TODO:  THAT
    
    final String animName;
    if (moveState == STATE_CLOSED) animName = this.animName;
    else if (moveState == STATE_SNEAK) animName = MOVE_SNEAK;
    else if (moveState == STATE_RUN) animName = MOVE_FAST;
    else animName = MOVE;
    boolean loop = animName != this.animName || (properties & NO_LOOP) == 0;
    
    final float alpha = Rendering.frameAlpha();
    final float AP = ((progress * alpha) + (oldProgress * (1 - alpha)));
    sprite.setAnimation(animName, (AP > 1) ? (AP % 1) : AP, loop);
  }
  
  
  public String toString() {
    return description ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append(description) ;
  }
}





//  TODO:  MOVE STUFF LIKE THIS TO ANOTHER CLASS.

//  ...There has got to be some way to simplify this junk.
/*
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
//*/