/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.util.*;

import java.lang.reflect.*;
import static stratos.game.actors.Qualities.*;


//  TODO:  ...You need to arrange for actions to terminate if you wind up
//  staying in one place too long (which likely means you're stuck.)

public class Action implements Behaviour, AnimNames {
  
  
  /**  Field definitions, constants and constructors-
    */
  final public static int
    QUICK    = 1,   //  Done while running.
    CAREFUL  = 2,   //  Done in stealth mode.
    TRACKS   = 4,   //  Should track facing with the target.
    RANGED   = 8,   //  Don't need to be adjacent to the target.
    NO_LOOP  = 16,  //  Don't loop the animation.
    PHYS_FX  = 32;  //  An involuntary effect, such as being stunned or thrown.
  final static byte
    STATE_INIT   = -1,
    STATE_CLOSED =  0,
    STATE_MOVE   =  1,
    STATE_SNEAK  =  2,
    STATE_RUN    =  3;
  
  private static boolean
    verbose     = false,
    verboseMove = false,
    verboseAnim = false;
  
  
  final public Actor actor;
  final public Session.Saveable basis;
  final public Method toCall;
  
  private float priority;
  private int properties;
  private byte moveState = -1;
  private Target actionTarget, moveTarget;
  private float progress, oldProgress;
  
  final String animName, description;
  
  
  
  public Action(
    Actor actor, Target target,
    Session.Saveable basis, String methodName,
    String animName, String description
  ) {
    if (actor == null || target == null) {
      I.complain("Null arguments for action!");
    }
    this.actor = actor;
    this.basis = basis;
    this.toCall = namedMethodFor(basis, methodName);
    this.priority = ROUTINE;
    this.actionTarget = this.moveTarget = target;
    this.animName = animName;
    this.description = description;
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (p == null || p.getClass() != this.getClass()) return false;
    final Action oldAction = (Action) p;
    if (oldAction == null || oldAction.subject() != subject()) return false;
    if (basis != oldAction.basis || toCall != oldAction.toCall) return false;
    if (properties != oldAction.properties) return false;
    return true;
  }
  
  
  public Action(Session s) throws Exception {
    s.cacheInstance(this);
    
    actor = (Actor) s.loadObject();
    basis = s.loadObject();
    toCall = namedMethodFor(basis, s.loadString());
    priority = s.loadFloat();
    
    properties = s.loadInt();
    moveState = (byte) s.loadInt();
    actionTarget = s.loadTarget();
    moveTarget = s.loadTarget();
    
    progress = s.loadFloat();
    oldProgress = s.loadFloat();
    animName = s.loadString();
    description = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(actor);
    s.saveObject(basis);
    s.saveString(toCall.getName());
    s.saveFloat(priority);
    
    s.saveInt(properties);
    s.saveInt(moveState);
    s.saveTarget(actionTarget);
    s.saveTarget(moveTarget);
    
    s.saveFloat(progress);
    s.saveFloat(oldProgress);
    s.saveString(animName);
    s.saveString(description);
  }
  
  
  
  public void setPriority(float p) {
    this.priority = p;
  }
  
  
  public void setMoveTarget(Target t) {
    if (t == null) I.complain("MOVE TARGET MUST BE NON-NULL.");
    this.moveTarget = t;
  }
  
  
  public void setProperties(int p) {
    this.properties = p;
  }
  
  
  public Target subject() {
    return actionTarget;
  }
  
  
  public Target movesTo() {
    return moveTarget;
  }
  
  
  public String methodName() {
    return toCall.getName();
  }
  
  
  public boolean ranged()  { return (properties & RANGED ) != 0; }
  public boolean careful() { return (properties & CAREFUL) != 0; }
  public boolean quick()   { return (properties & QUICK  ) != 0; }
  public boolean tracks()  { return (properties & TRACKS ) != 0; }
  public boolean physFX()  { return (properties & PHYS_FX) != 0; }
  
  
  
  /**  Implementing the Behaviour contract-
    */
  public float priorityFor(Actor actor) {
    return priority;
  }
  
  
  public boolean finished() {
    if (progress == -1) return true;
    return (moveState == STATE_CLOSED) && (progress >= 1);
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null;
    return this;
  }
  
  
  public void interrupt(String cause) {
    final boolean report = verbose && I.talkAbout == actor;
    progress = -1;
    moveState = STATE_INIT;
    //actor.mind.cancelBehaviour(this);
    actor.assignAction(null);
    if (report) {
      I.say("\nCancelling action: "+this);
      I.say("  Cause: "+cause);
    }
  }
  
  
  public boolean valid() {
    return
      actor.inWorld() && moveTarget.inWorld() &&
      ! actionTarget.destroyed();
  }
  
  
  public boolean hasBegun() {
    return this == actor.currentAction();
  }
  
  
  public boolean persistent() {
    return false;
  }
  
  
  public Actor actor() {
    return actor;
  }
  
  
  public boolean isMoving() {
    return moveState != STATE_INIT && moveState != STATE_CLOSED;
  }
  
  
  public boolean isClosed() {
    return moveState == STATE_CLOSED;
  }
  
  
  
  /**  Helper methods for dealing with motion-
    */
  //  TODO:  Move these functions out to a 'MotionUtils' utility class.
  
  public int motionType(Actor actor) {
    int motionType = MOTION_NORMAL;
    if (quick()  ) motionType = MOTION_FAST ;
    if (careful()) motionType = MOTION_SNEAK;
    
    if (motionType == MOTION_NORMAL) for (Behaviour b : actor.mind.agenda) {
      if (b == this) continue;
      final int MT = b.motionType(actor);
      if (MT != MOTION_ANY) { motionType = MT; break; }
    }
    return motionType;
  }
  
  
  public static float speedMultiple(Actor actor, boolean basic) {
    final Action a = actor.currentAction();
    if (a == null) return 0;
    
    float rate = 1;
    if (a.moveState == STATE_SNEAK) {
      rate /= 2;
      if (! basic) rate *= (1 + actor.skills.chance(STEALTH_AND_COVER, 15));
    }
    else if (a.moveState == STATE_RUN) {
      rate *= 2;
      if (! basic) rate *= (1 + actor.skills.chance(ATHLETICS, 15));
    }
    
    if (basic) return rate;
    //  TODO:  Must also account for the effects of fatigue and encumbrance.
    
    final int pathType = actor.origin().pathType();
    switch (pathType) {
      case (Tile.PATH_HINDERS) : rate *= 0.8f; break;
      case (Tile.PATH_CLEAR  ) : rate *= 1.0f; break;
      case (Tile.PATH_ROAD   ) : rate *= 1.2f; break;
    }
    return rate;
  }
  
  
  private float updateMotion(boolean active, int motionType) {
    final boolean report = verboseMove && I.talkAbout == actor;
    
    //  Firstly, we establish current displacements between actor and target,
    //  motion target & action target, how far the actor can see, and whether
    //  sticking to the full tile path is required-
    final boolean mustBoard = (
      (! ranged()) && (moveTarget instanceof Boarding)
    );
    //final boolean blockage = ! actor.motion.checkEndPoint(moveTarget);
    final float
      sightRange = actor.health.sightRange(),
      motionDist = Spacing.distance(actor, moveTarget),
      actionDist = Spacing.distance(actor, actionTarget),
      separation = Spacing.distance(moveTarget, actionTarget);
    
    //  We also need to calculate an appropriate maximum distance in order for
    //  the action to progress-
    float maxDist = 0.01f;
    if (ranged()) maxDist += sightRange * (moveState == STATE_CLOSED ? 2 : 1);
    else if (moveState == STATE_CLOSED) maxDist += progress + 0.5f;
    
    //  In order for the action to execute, the actor must both be close enough
    //  to the target and facing in the right direction.  If the target is
    //  close enough to see, you can consider ignoring tile-pathing in favour
    //  of closing directly on the subject.  If, on the other hand, the target
    //  should be visible, but isn't, then path towards it more directly.
    Target pathsTo = moveTarget;
    boolean closed = false, approaching = false, facing = false;
    final Target step = actor.pathing.nextStep(), closeOn;
    
    if (report) I.say("\nAction is: "+methodName()+" "+hashCode());
    
    if (contactMade() && ! tracks()) {
      if (report) I.say("  Have closed on target.");
      pathsTo = actor.aboard();
      closeOn = actor;
      closed = approaching = facing = true;
    }
    else if (mustBoard) {
      if (report) I.say("  Must board target.");
      approaching = actor.aboard() == moveTarget;
      closed = approaching && (motionDist - maxDist < separation);
      closeOn = closed ? actionTarget : step;
      facing = actor.pathing.facingTarget(closeOn);
    }
    else {
      if (report) I.say("  Must have facing and line of sight.");
      //  TODO:  Build line-of-sight considerations into the actor's reaction
      //  algorithms instead?
      final boolean seen = Senses.hasLineOfSight(
        actor, actionTarget, Nums.max(maxDist, sightRange)
      );
      
      if (Nums.min(motionDist, actionDist) < maxDist && ! seen) {
        pathsTo = actionTarget;
      }
      if (PathSearch.blockedBy(pathsTo, actor)) {
        pathsTo = Spacing.nearestOpenTile(pathsTo, actor);
      }
      
      closed = seen && (actionDist <= maxDist);
      approaching = closed || (seen && (actionDist <= (maxDist + 1)));
      closeOn = approaching ? actionTarget : step;
      facing = actor.pathing.facingTarget(closeOn);
    }
    actor.pathing.updateTarget(pathsTo);
    
    if (report) {
      I.say("  Action target is: "+actionTarget);
      I.say("  Move target is: "+moveTarget);
      I.say("  Closing on: "+closeOn+", must board: "+mustBoard);
      
      final boolean blocked = PathSearch.blockedBy(actor.aboard(), actor);
      I.say("  Currently aboard: "+actor.aboard()+", blocked? "+blocked);
      I.say("  Closed/facing: "+closed+"/"+facing+", doing update? "+active);
      I.say("  Is ranged? "+ranged()+", approaching? "+approaching);
      
      final Target PT = actor.pathing.target();
      I.say("  Path target is: "+PT+", step: "+step);
      if (PT != null) {
        final float distance = Spacing.distance(actor, PT);
        I.say("  Distance: "+distance+", maximum: "+maxDist+"\n");
      }
      else {
        final boolean MB = PathSearch.blockedBy(moveTarget, actor);
        I.say("  Move target blocked? "+MB);
      }
    }
    
    //  If both facing and proximity are satisfied, toggle the flag which
    //  allows action delivery to proceed.  (If delivery was already underway,
    //  cancel the action.)
    final byte oldState = moveState;
    if (closed && facing) moveState = STATE_CLOSED;
    else if (motionType == MOTION_SNEAK) moveState = STATE_SNEAK;
    else if (motionType == MOTION_FAST ) moveState = STATE_RUN  ;
    else moveState = STATE_MOVE;
    
    if (moveState != oldState) {
      if (oldState == STATE_CLOSED) { interrupt(INTERRUPT_CANCEL); return 0; }
      else progress = oldProgress = 0;
    }
    
    //  If active updates to pathing & motion are called for, make them.
    float moveRate = speedMultiple(actor, false) * actor.health.baseSpeed();
    if (active) {
      if (report) I.say("Move rate: "+moveRate);
      actor.pathing.headTowards(closeOn, moveRate, ! closed);
      if (! closed) actor.pathing.applyCollision(moveRate, actionTarget);
      return moveRate;
    }
    else return 0;
  }
  
  
  
  /**  Actual execution of associated behaviour-
    */
  private float actionDuration() {
    float duration = 1;
    if (quick()  ) duration /= 2;
    if (careful()) duration *= 2;
    return duration;
  }
  
  
  private float contactTime() {
    final float duration = actionDuration();
    final float contact = (duration - 0.25f) / duration;
    return contact;
  }
  
  
  private boolean contactMade() {
    return moveState == STATE_CLOSED && progress >= contactTime();
  }
  
  
  protected void updateAction(boolean active) {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) {
      I.say("\nUpdating action for: "+actor+"  Target: "+actionTarget);
      I.say("  Method is: "+methodName()+", basis "+basis.getClass());
    }
    
    if (finished()) {
      if (report) I.say("  Finished!");
      oldProgress = progress = 1;
      return;
    }
    
    final int motionType = motionType(actor);
    oldProgress = progress;
    final float moveRate = updateMotion(active, motionType);
    
    if (moveState == STATE_CLOSED) {
      final float contact = contactTime();
      progress += 1f / (actionDuration() * Stage.UPDATES_PER_SECOND);
      progress = Nums.clamp(progress, 0, 1);
      if (report) {
        I.say("  Have closed on target, contact at "+contact);
        I.say("  Old/new progress: "+oldProgress+"/"+progress);
      }
      if (oldProgress <= contact && progress > contact) {
        if (report) I.say("  Applying effect...");
        applyEffect();
      }
    }
    
    else if (moveState != STATE_INIT) {
      float speedUp = moveRate / actor.health.baseSpeed();
      speedUp = (1 + speedUp) / (2 * Stage.UPDATES_PER_SECOND);
      progress += actor.moveAnimStride() * speedUp;
      if (report) {
        I.say("  Moving into position...");
        I.say("  Old/new progress: "+oldProgress+"/"+progress);
      }
    }
  }
  
  
  public void applyEffect() {
    try { toCall.invoke(basis, actor, actionTarget); }
    catch (Exception e) {
      I.say(
        "\nPROBLEM WITH ACTION: "+toCall.getName()+
        "("+actor+", "+actionTarget+")"
      );
      e.printStackTrace();
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
  > (1000);
  
  
  private static Method namedMethodFor(Object plans, String methodName) {
    if (plans == null || methodName == null) return null;
    Table <String, Method> aM = actionMethods.get(plans.getClass());
    if (aM == null) {
      aM = new Table <String, Method> (20);
      for (Method method : plans.getClass().getMethods()) {
        aM.put(method.getName(), method);
      }
      actionMethods.put(plans.getClass(), aM);
    }
    final Method method = aM.get(methodName);
    if (method == null) I.complain(
      "NO SUCH METHOD! "+methodName+" FOR CLASS: "+plans
    );
    if (! method.isAccessible()) {
      final Class <? extends Object> params[] = method.getParameterTypes();
      if (
        params.length != 2 ||
        ! Actor.class.isAssignableFrom(params[0]) ||
        ! Target.class.isAssignableFrom(params[1])
      ) I.complain("METHOD HAS BAD ARGUMENT SET!");
      method.setAccessible(true);
    }
    return method;
  }
  
  
  
  /**  Methods to support rendering-
    */
  protected void configSprite(Sprite s, Rendering rendering) {
    final SolidSprite sprite = (SolidSprite) s;
    
    final boolean report = verboseAnim && I.talkAbout == actor;
    
    final String animName;
    if      (moveState == STATE_CLOSED) animName = this.animName;
    else if (moveState == STATE_SNEAK ) animName = MOVE_SNEAK;
    else if (moveState == STATE_RUN   ) animName = MOVE_FAST;
    else animName = MOVE;
    boolean loop = animName != this.animName || (properties & NO_LOOP) == 0;
    
    final float alpha = Rendering.frameAlpha();
    final float AP = ((progress * alpha) + (oldProgress * (1 - alpha)));
    
    if (report) {
      I.say("\nAction configuring animation for "+actor);
      I.say("  Range name: "+animName);
      I.say("  Progress:   "+AP);
    }
    sprite.setAnimation(animName, (AP > 1) ? (AP % 1) : AP, loop);
  }
  
  
  public String toString() {
    return description;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append(description);
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
  final Tile o = actor.origin();
  int var = o.world.terrain().varAt(o);
  var += (o.x * o.world.size) + o.y;
  var -= o.world.currentTime();
  var ^= actor.hashCode();
  return (1 + (float) Nums.sqrt(Nums.abs(var % 10) / 4.5f)) / 2;
}
//*/