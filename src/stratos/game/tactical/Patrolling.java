/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;


//
//  TODO:  You need to incorporate intercept behaviours here.


public class Patrolling extends Plan implements TileConstants, Qualities {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    TYPE_SECURITY      = 0,
    TYPE_STREET_PATROL = 1,
    TYPE_SENTRY_DUTY   = 2,
    TYPE_WANDERING     = 3,
    
    WATCH_TIME = 10 ;
  
  
  private static boolean verbose = false, evalVerbose = false;
  
  final int type ;
  final Element guarded ;
  
  private List <Target> patrolled ;
  private Boardable onPoint ;
  private float postTime = -1 ;
  
  
  
  private Patrolling(
    Actor actor, Element guarded, List <Target> patrolled, int type
  ) {
    super(actor, guarded) ;
    this.type = type ;
    this.guarded = guarded ;
    this.patrolled = patrolled ;
    onPoint = (Boardable) patrolled.first() ;
  }
  
  
  public Patrolling(Session s) throws Exception {
    super(s) ;
    type = s.loadInt() ;
    guarded = (Element) s.loadObject() ;
    s.loadTargets(patrolled = new List <Target> ()) ;
    onPoint = (Boardable) s.loadTarget() ;
    postTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(type) ;
    s.saveObject(guarded) ;
    s.saveTargets(patrolled) ;
    s.saveTarget(onPoint) ;
    s.saveFloat(postTime) ;
  }
  
  
  
  /**  Obtaining and evaluating patrols targets-
    */
  final static Trait BASE_TRAITS[] = { FEARLESS, SIMPLE, SOLITARY };
  final static Skill BASE_SKILLS[] = { SURVEILLANCE, HAND_TO_HAND };
  

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (onPoint == null) return 0;
    
    float urgency;
    if (type == TYPE_WANDERING) urgency = IDLE;
    else {
      //  Favour patrols through more (relatively) dangerous areas.
      float relDanger = 0 ;
      if (actor.base() != null) for (Target t : patrolled) {
        relDanger += Plan.dangerPenalty(t, actor) ;
      }
      relDanger /= patrolled.size() ;
      urgency = relDanger * ROUTINE * 1f / patrolled.size();
    }
    
    final float priority = priorityForActorWith(
      actor, onPoint, urgency,
      MILD_HELP, NO_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, MILD_DANGER,
      report
    );
    return priority;
  }
  
  
  
  /**  Behaviour execution-
    */
  public Behaviour getNextStep() {
    if (onPoint == null) return null ;
    final boolean report = verbose && I.talkAbout == actor;
    final World world = actor.world() ;
    Target stop = onPoint ;
    if (report) I.say("Goes: "+onPoint+", post time: "+postTime) ;
    //
    //  TODO:  You need to add an intercept/attack behaviour for enemies near
    //  the guarded target (if any?)
    
    if (type != TYPE_SENTRY_DUTY) {
      Tile open = world.tileAt(onPoint) ;
      open = Spacing.nearestOpenTile(open, actor) ;
      if (open == null) {
        onPoint = (Boardable) patrolled.atIndex(patrolled.indexOf(onPoint) + 1) ;
        if (onPoint == null) {
          abortBehaviour() ;
          return null ;
        }
      }
      else stop = open ;
    }
    else if (postTime != -1) {
      final float spent = world.currentTime() - postTime ;
      if (report) I.say("Time at post: "+spent) ;
      if (spent < WATCH_TIME) {
        final Action watch = new Action(
          actor, onPoint,
          this, "actionStandWatch",
          Action.LOOK, "Standing Watch"
        ) ;
        return watch ;
      }
    }
    
    if (report) I.say("Next stop: "+stop+" "+stop.hashCode()) ;
    final Action patrol = new Action(
      actor, stop,
      this, "actionPatrol",
      Action.LOOK, "Patrolling"
    ) ;
    return patrol ;
  }
  
  
  
  
  
  public boolean finished() {
    if (super.finished()) return true ;
    return onPoint == null ;
  }
  
  
  public int motionType(Actor actor) {
    //
    //  TODO:  You should be able to implement motion and chase behaviour here.
    return super.motionType(actor) ;
  }


  public boolean actionPatrol(Actor actor, Target spot) {
    //I.say("Patrolling to: "+spot) ;
    
    if (actor.base() != null) {
      final IntelMap map = actor.base().intelMap ;
      map.liftFogAround(spot, actor.health.sightRange() * 1.207f) ;
    }
    //
    //  If you're on sentry duty, check whether you need to stand watch.
    if (type == TYPE_SENTRY_DUTY) {
      if (postTime == -1) postTime = actor.world().currentTime() ;
      final float spent = actor.world().currentTime() - postTime ;
      if (spent < WATCH_TIME) return false ;
      else postTime = -1 ;
    }
    //
    //  If not, head on to the next stop on your patrol route-
    final int index = patrolled.indexOf(onPoint) + 1 ;
    if (index < patrolled.size()) {
      onPoint = (Boardable) patrolled.atIndex(index) ;
    }
    else {
      onPoint = null ;
    }
    return true ;
  }
  
  
  public boolean actionStandWatch(Actor actor, Target spot) {
    if (actor.base() != null) {
      final IntelMap map = actor.base().intelMap ;
      map.liftFogAround(spot, actor.health.sightRange() * 1.414f) ;
    }
    return true ;
  }
  
  
  
  /**  External factory methods-
    */
  public static Patrolling wandering(Actor actor) {
    final List<Target> patrolled = new List<Target>();
    final float range = actor.health.sightRange() + actor.aboard().radius();
    patrolled.add(Spacing.pickRandomTile(actor, range, actor.world()));
    return new Patrolling(actor, actor, patrolled, TYPE_WANDERING);
  }
  
  
  public static Patrolling aroundPerimeter(
    Actor actor, Element guarded, World world
  ) {
    final List <Target> patrolled = new List <Target> () ;
    final float range = Math.max(
      guarded.radius() * 2,
      actor.health.sightRange() / 2
    ) ;
    final Vec3D centre = guarded.position(null) ;
    for (int n : N_ADJACENT) {
      Tile point = world.tileAt(
        Visit.clamp(centre.x + (N_X[n] * range), 0, world.size - 1),
        Visit.clamp(centre.y + (N_Y[n] * range), 0, world.size - 1)
      ) ;
      if (point != null) patrolled.include(point) ;
    }
    return new Patrolling(actor, guarded, patrolled, TYPE_SECURITY) ;
  }
  
  
  public static Patrolling streetPatrol(
    Actor actor, Element init, Element dest, World world
  ) {
    final List <Target> patrolled = new List <Target> () ;
    final Tile
      initT = Spacing.nearestOpenTile((Element) init, dest, world),
      destT = Spacing.nearestOpenTile((Element) dest, init, world) ;
    
    PathSearch search = new PathSearch(initT, destT) ;
    search.doSearch() ;
    if (! search.success()) return null ;
    final Boardable path[] = search.fullPath(Boardable.class) ;
    float interval = World.PATCH_RESOLUTION ;
    
    for (int i = 0 ; i < path.length ; i += interval) {
      patrolled.include(path[i]) ;
    }
    patrolled.add(dest) ;
    return new Patrolling(actor, init, patrolled, TYPE_STREET_PATROL) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_WANDERING) {
      d.append("Wandering") ;
    }
    if (type == TYPE_SECURITY) {
      d.append("Securing perimeter for ") ;
      d.append(guarded) ;
    }
    if (type == TYPE_STREET_PATROL || type == TYPE_SENTRY_DUTY) {
      d.append("Patrolling between ") ;
      d.append(guarded) ; d.append(" and ") ;
      d.append(patrolled.last()) ;
    }
  }
}






//  TODO:  RESTORE THIS.
/*
public static Patrolling sentryDuty(
  Actor actor, ShieldWall start, int initDir
) {
  final Batch <Target> enRoute = new Batch <Target> () ;
  
  final float maxDist = World.SECTOR_SIZE * 1.5f ;
  final Vec3D p = start.position(null) ;
  final Tile ideal = actor.world().tileAt(
    p.x + (N_X[initDir] * 2),
    p.y + (N_Y[initDir] * 2)
  ) ;
  
  Boardable init = start, next = null ;
  float minDist = Float.POSITIVE_INFINITY ;
  for (Boardable b : init.canBoard(Spacing.tempB4)) {
    if (! (b instanceof ShieldWall)) continue ;
    final float dist = Spacing.distance(b, ideal) ;
    if (dist < minDist) { minDist = dist ; next = b ; }
  }
  if (next == null) return null ;
  
  init.flagWith(enRoute) ;
  next.flagWith(enRoute) ;
  enRoute.add(init) ;
  enRoute.add(next) ;
  
  while (true) {
    Boardable near = null ;
    for (Boardable b : next.canBoard(Spacing.tempB4)) {
      if (! (b instanceof ShieldWall)) continue ;
      if (b.flaggedWith() != null) continue ;
      near = b ;
      near.flagWith(enRoute) ;
      enRoute.add(near) ;
      break ;
    }
    if (near == null || enRoute.size() > maxDist / 2) break ;
    next = near ;
  }
  for (Target t : enRoute) t.flagWith(null) ;
  
  final List <Target> patrolled = new List <Target> () ;
  BlastDoors doors = null ;
  for (Target b : enRoute) {
    final ShieldWall s = (ShieldWall) b ;
    if (s.isTower()) patrolled.include(b) ;
    if (s.isGate()) {
      if (doors == null || (
        Spacing.distance(s, actor) < Spacing.distance(doors, actor)
      )) doors = (BlastDoors) s ;
    }
  }
  if (doors == null) return null ;
  return new Patrolling(actor, doors, patrolled, TYPE_SENTRY_DUTY) ;
}
//*/

