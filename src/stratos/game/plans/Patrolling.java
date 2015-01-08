/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.base.*;
import stratos.game.maps.*;
import stratos.util.*;



//  TODO:  The list of patrol points needs to be more randomised, and needs
//  to keep up with moving targets more elegantly.


public class Patrolling extends Plan implements TileConstants, Qualities {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    TYPE_SECURITY      = 0,
    TYPE_STREET_PATROL = 1,
    TYPE_SENTRY_DUTY   = 2;
  final static int
    WATCH_TIME = 10;
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final int type;
  final Element guarded;
  
  private List <Target> patrolled;
  private Target onPoint;
  private float postTime = -1;
  
  
  
  private Patrolling(
    Actor actor, Element guarded, List <Target> patrolled, int type
  ) {
    super(actor, guarded, true, MILD_HELP);
    this.type = type;
    this.guarded = guarded;
    this.patrolled = patrolled;
    onPoint = (Target) patrolled.first();
  }
  
  
  public Patrolling(Session s) throws Exception {
    super(s);
    type = s.loadInt();
    guarded = (Element) s.loadObject();
    s.loadTargets(patrolled = new List <Target> ());
    onPoint = (Target) s.loadTarget();
    postTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
    s.saveObject(guarded);
    s.saveTargets(patrolled);
    s.saveTarget(onPoint);
    s.saveFloat(postTime);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Patrolling(other, guarded, patrolled, type);
  }
  
  
  
  /**  Obtaining and evaluating patrols targets-
    */
  final static Trait BASE_TRAITS[] = { FEARLESS, IGNORANT, SOLITARY };
  final static Skill BASE_SKILLS[] = { SURVEILLANCE, FORENSICS };
  
  //  TODO:  Include bonus from first aid or assembly skills, depending on the
  //  target?

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (onPoint == null || patrolled.size() == 0) return 0;
    
    float urgency, relDanger = 0;
    if (actor.base() != null) for (Target t : patrolled) {
      relDanger += actor.base().dangerMap.sampleAt(t);
    }
    relDanger /= patrolled.size();
    urgency = Nums.clamp(relDanger * ROUTINE, IDLE, ROUTINE);
    
    float modifier = 0 - actor.senses.fearLevel();
    if (actor.senses.isEmergency()) modifier = PARAMOUNT;
    
    final float priority = priorityForActorWith(
      actor, guarded,
      urgency, modifier,
      MILD_HELP, NO_COMPETITION, REAL_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("\nExtra patrolling factors:");
      I.say("  Relative danger: "+relDanger);
      I.say("  Base urgency:    "+urgency);
    }
    return priority;
  }
  
  
  
  /**  Behaviour execution-
    */
  public Behaviour getNextStep() {
    if (onPoint == null) return null;
    
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next patrol step for "+actor);
      I.say("  Going to: "+onPoint+", post time: "+postTime);
    }
    
    final Stage      world   = actor.world();
    final Activities actives = world.activities;
    Target stop = onPoint;
    
    //  First, check to see if there are any supplemental behaviours you could
    //  or should be performing (first aid, repairs, or defence.)
    
    final Plan old = (lastStep instanceof Plan) ? (Plan) lastStep : null;
    final Choice choice = new Choice(actor);
    final float range = actor.health.sightRange() + 1;
    choice.isVerbose = report;
    for (Target t : actor.senses.awareOf()) {
      if (! CombatUtils.isActiveHostile(actor, t)) continue;
      final float dist = Spacing.distance(t, guarded) / range;
      final float bonus = Plan.ROUTINE * (1 - dist);
      choice.add(new Combat(actor, (Element) t).setMotiveFrom(this, bonus));
    }
    if (guarded instanceof Actor) {
      choice.add(new FirstAid(actor, (Actor) guarded).setMotiveFrom(this, 0));
    }
    if (guarded instanceof Venue) {
      choice.add(new Repairs(actor, (Venue) guarded).setMotiveFrom(this, 0));
    }
    final Behaviour picked = choice.pickMostUrgent();
    if (Choice.wouldSwitch(actor, old, picked, true, report)) {
      if (report) I.say("  Performing sub-task: "+picked);
      return picked;
    }
    
    //  If you're on sentry duty, check to see if you've spent long enough at
    //  your post.
    if (type == TYPE_SENTRY_DUTY) {
      if (postTime != -1) {
        final float spent = world.currentTime() - postTime;
        if (report) I.say("Time at post: "+spent);
        if (spent < WATCH_TIME) {
          final Action watch = new Action(
            actor, onPoint,
            this, "actionStandWatch",
            Action.LOOK, "Standing Watch"
          );
          return watch;
        }
      }
    }
    
    //  Otherwise, find the nearest free point to stand around the next point
    //  to guard, and proceed there.
    else if (onPoint.isMobile()) {
      Tile open = Spacing.pickRandomTile(onPoint, range / 2, actor.world());
      open = Spacing.nearestOpenTile(open, actor);
      if (open == null) { interrupt(INTERRUPT_CANCEL); return null; }
      else stop = open;
    }
    else {
      Tile open = world.tileAt(onPoint);
      open = Spacing.nearestOpenTile(open, actor);
      if (open == null) { interrupt(INTERRUPT_CANCEL); return null; }
      else stop = open;
    }
    
    //  Either way, return a patrolling action-
    if (report) {
      I.say("  Next stop: "+I.tagHash(stop));
    }
    final Action patrol = new Action(
      actor, stop,
      this, "actionPatrol",
      Action.LOOK, "Patrolling"
    );
    return patrol;
  }
  
  
  public boolean finished() {
    if (onPoint == null) return true;
    return super.finished();
  }
  
  
  public int motionType(Actor actor) {
    if (
      actor.world().activities.includesActivePlan(guarded, Combat.class) &&
      Spacing.distance(actor, guarded) >= actor.health.sightRange()
    ) {
      return MOTION_FAST;
    }
    return super.motionType(actor);
  }


  public boolean actionPatrol(Actor actor, Target spot) {
    //I.say("Patrolling to: "+spot);
    
    if (actor.base() != null) {
      final IntelMap map = actor.base().intelMap;
      map.liftFogAround(spot, actor.health.sightRange() * 1.207f);
    }
    //
    //  If you're on sentry duty, check whether you need to stand watch.
    if (type == TYPE_SENTRY_DUTY) {
      if (postTime == -1) postTime = actor.world().currentTime();
      final float spent = actor.world().currentTime() - postTime;
      if (spent < WATCH_TIME) return false;
      else postTime = -1;
    }
    //
    //  If not, head on to the next stop on your patrol route-
    final int index = patrolled.indexOf(onPoint) + 1;
    if (index < patrolled.size()) {
      onPoint = (Boarding) patrolled.atIndex(index);
    }
    else {
      onPoint = null;
    }
    return true;
  }
  
  
  public boolean actionStandWatch(Actor actor, Target spot) {
    if (actor.base() != null) {
      final IntelMap map = actor.base().intelMap;
      map.liftFogAround(spot, actor.health.sightRange() * 1.414f);
    }
    return true;
  }
  
  
  
  /**  External factory methods-
    */
  public static Patrolling aroundPerimeter(
    Actor actor, Element guarded, Stage world
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next perimeter patrol for "+actor);
    
    final List <Target> patrolled = new List <Target> ();
    
    if (guarded.isMobile()) {
      patrolled.add(guarded);
      return new Patrolling(actor, guarded, patrolled, TYPE_SECURITY);
    }
    
    final float range = Nums.max(
      guarded.radius() * 2,
      actor.health.sightRange() / 2
    );
    final Vec3D centre = guarded.position(null);
    if (report) I.say("  Range is: "+range+", centre: "+centre);
    
    for (int n : T_ADJACENT) {
      Tile point = world.tileAt(
        Nums.clamp(centre.x + (T_X[n] * range), 0, world.size - 1),
        Nums.clamp(centre.y + (T_Y[n] * range), 0, world.size - 1)
      );
      if (point != null) {
        if (report) I.say("  Patrol point: "+point);
        patrolled.include(point);
      }
    }
    
    return new Patrolling(actor, guarded, patrolled, TYPE_SECURITY);
  }
  
  
  public static Patrolling streetPatrol(
    Actor actor, Element init, Element dest, Stage world
  ) {
    final List <Target> patrolled = new List <Target> ();
    final Tile
      initT = Spacing.nearestOpenTile((Element) init, dest, world),
      destT = Spacing.nearestOpenTile((Element) dest, init, world);
    
    final PathSearch search = new PathSearch(initT, destT, true);
    search.doSearch();
    if (! search.success()) return null;
    final Boarding path[] = search.fullPath(Boarding.class);
    float interval = Stage.PATCH_RESOLUTION;
    
    for (int i = 0; i < path.length; i += interval) {
      patrolled.include(path[i]);
    }
    patrolled.add(dest);
    return new Patrolling(actor, init, patrolled, TYPE_STREET_PATROL);
  }
  
  
  public static Patrolling sentryDuty(
    Actor actor, ShieldWall start, int initDir
  ) {
    final Batch<Target> enRoute = new Batch<Target>();

    final float maxDist = Stage.SECTOR_SIZE * 1.5f;
    final Vec3D p = start.position(null);
    Tile ideal = actor.world().tileAt(
      p.x + (T_X[initDir] * 2),
      p.y + (T_Y[initDir] * 2)
    );
    if (ideal == null) return null;
    
    Boarding init = start, next = null;
    float minDist = Float.POSITIVE_INFINITY;
    for (Boarding b : init.canBoard()) {
      if (! (b instanceof ShieldWall)) continue;
      final float dist = Spacing.distance(b, ideal);
      if (dist < minDist) {
        minDist = dist;
        next = b;
      }
    }
    if (next == null) return null;
    
    init.flagWith(enRoute);
    next.flagWith(enRoute);
    enRoute.add(init);
    enRoute.add(next);
    
    while (true) {
      Boarding near = null;
      for (Boarding b : next.canBoard()) {
        if (!(b instanceof ShieldWall))
          continue;
        if (b.flaggedWith() != null)
          continue;
        near = b;
        near.flagWith(enRoute);
        enRoute.add(near);
        break;
      }
      if (near == null || enRoute.size() > maxDist / 2)
        break;
      next = near;
    }
    for (Target t : enRoute) t.flagWith(null);
    
    final List<Target> patrolled = new List<Target>();
    BlastDoors doors = null;
    for (Target b : enRoute) {
      final ShieldWall s = (ShieldWall) b;
      if (s.isTower())
        patrolled.include(b);
      if (s.isGate()) {
        if (doors == null
            || (Spacing.distance(s, actor) < Spacing.distance(doors, actor)))
          doors = (BlastDoors) s;
      }
    }
    if (doors == null) return null;
    return new Patrolling(actor, doors, patrolled, TYPE_SENTRY_DUTY);
  }
  
  
  public static Patrolling nextGuardPatrol(
    Actor actor, Venue origin, float priority
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next guard patrol for "+actor);
      I.say("  Base: "+origin.base());
      final PresenceMap map = actor.world().presences.mapFor(origin.base());
      I.say("  Total targets: "+map.population());
    }

    //  Grab a random building nearby and patrol around it.
    final Stage world = actor.world();
    final Base base = origin.base();
    final float range = Stage.SECTOR_SIZE / 2f;
    final Venue pick = (Venue) world.presences.randomMatchNear(
      base, origin, range
    );
    if (report) I.say("  Venue picked: "+pick);
    if (pick != null) {
      final Patrolling p = Patrolling.aroundPerimeter(actor, pick, world);
      p.setMotive(Plan.MOTIVE_DUTY, priority);
      return p;
    }
    
    //  TODO:  Restore sentry duty along shield walls!
    
    /*
    final Venue
      init = (Venue) world.presences.randomMatchNear(base, origin, range),
      dest = (Venue) world.presences.randomMatchNear(base, origin, range);
    
    if (init instanceof ShieldWall || dest instanceof ShieldWall) {
      Target pick, other;
      if (Rand.yes()) { pick = init; other = dest; }
      else            { pick = dest; other = init; }
      if (! (pick instanceof ShieldWall)) pick = other;
      final Patrolling s = Patrolling.sentryDuty(
        actor, (ShieldWall) pick, Rand.index(8)
      );
      if (s != null) {
        s.setMotive(Plan.MOTIVE_DUTY, priority);
        return s;
      }
    }
    
    if (init != null && dest != null) {
      final Patrolling p = (init == dest) ?
        Patrolling.aroundPerimeter(actor, init, world) :
        Patrolling.streetPatrol(actor, init, dest, world);
      if (p != null) {
        p.setMotive(Plan.MOTIVE_DUTY, priority);
        return p;
      }
    }
    //*/
    
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_SECURITY) {
      d.append("Securing perimeter for ");
      d.append(guarded);
    }
    if (type == TYPE_STREET_PATROL || type == TYPE_SENTRY_DUTY) {
      d.append("Patrolling between ");
      d.append(guarded); d.append(" and ");
      d.append(patrolled.last());
    }
  }
}






//  TODO:  RESTORE THIS.
/*
//*/

