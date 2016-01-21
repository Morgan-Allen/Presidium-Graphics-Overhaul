/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;
import stratos.content.civic.ShieldWall;
import static stratos.game.actors.Qualities.*;


//
//  TODO:  Don't refer to the Shield Wall explicitly.  Use a public interface.

public class Patrolling extends Plan implements TileConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    TYPE_PROTECTION    = 0,
    TYPE_STREET_PATROL = 1,
    TYPE_SENTRY_DUTY   = 2;
  final static int
    WATCH_TIME = 10;
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final int type;
  final Element guarded;
  
  private Target onPoint;
  private float postTime = -1;
  private List <Target> patrolled;
  
  
  
  private Patrolling(
    Actor actor, Element guarded, List <Target> patrolled, int type
  ) {
    super(actor, guarded, MOTIVE_JOB, MILD_HELP);
    this.type      = type;
    this.guarded   = guarded;
    this.patrolled = patrolled;
    onPoint = (Target) patrolled.first();
  }
  
  
  public Patrolling(Session s) throws Exception {
    super(s);
    type     = s.loadInt();
    guarded  = (Element) s.loadObject();
    onPoint  = (Target) s.loadObject();
    postTime = s.loadFloat();
    s.loadObjects(patrolled = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type    );
    s.saveObject(guarded );
    s.saveObject(onPoint );
    s.saveFloat (postTime);
    s.saveObjects(patrolled);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Patrolling(other, guarded, patrolled, type);
  }
  
  
  
  /**  External factory methods and supplemental evaluation calls-
    */
  public static Patrolling protectionFor(
    Actor actor, Element guarded, float priority
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next perimeter patrol for "+actor);
    
    final Stage world = actor.world();
    final List <Target> patrolled = new List();
    
    if (guarded.isMobile()) {
      patrolled.add(guarded);
    }
    else {
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
    }
    
    Patrolling p = new Patrolling(actor, guarded, patrolled, TYPE_PROTECTION);
    return (Patrolling) p.addMotives(Plan.NO_PROPERTIES, priority);
  }
  
  
  public static Patrolling streetPatrol(
    Actor actor, Element init, Element dest, float priority
  ) {
    final Stage world = actor.world();
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
    
    Patrolling p = new Patrolling(actor, init, patrolled, TYPE_STREET_PATROL);
    return (Patrolling) p.addMotives(Plan.NO_PROPERTIES, priority);
  }
  
  
  public static Patrolling sentryDuty(
    Actor actor, ShieldWall start, Venue barracks, float priority
  ) {
    if (start == null || start.base() != barracks.base()) {
      return null;
    }
    
    final Vec3D between = Spacing.between(barracks, start);
    final Vec2D prefHeading = new Vec2D(between).perp();
    final float maxDist = Stage.ZONE_SIZE;
    
    final List <Target> patrolled = new List();
    final Batch <Target> flagged = new Batch();
    ShieldWall doors = null;
    ShieldWall next = start;
    float sumDist = 0; Vec3D temp = new Vec3D();
    
    while (next != null) {
      if (next.isTower()) patrolled.include(next);
      if (next.isGate() && doors == null) doors = next;
      sumDist += next.radius() * 2;
      
      if (sumDist > maxDist) break;
      next.flagWith(flagged);
      flagged.add(next);
      
      final Pick <Boarding> pick = new Pick();
      for (Boarding b : next.canBoard()) {
        if (! (b instanceof ShieldWall)) continue;
        if (b.flaggedWith() != null) continue;
        b.position(temp);
        pick.compare(b, prefHeading.dot(temp.x, temp.y));
      }
      next = (ShieldWall) pick.result();
    }
    
    for (Target t : flagged) t.flagWith(null);
    if (doors == null) {
      return null;
    }
    
    Patrolling p = new Patrolling(actor, doors, patrolled, TYPE_SENTRY_DUTY);
    return (Patrolling) p.addMotives(Plan.NO_PROPERTIES, priority);
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
    final float range = Stage.ZONE_SIZE / 2f;
    final Venue pick = (Venue) world.presences.randomMatchNear(
      base, origin, range
    );
    if (report) I.say("  Venue picked: "+pick);
    
    if (pick == null) return null;
    return Patrolling.streetPatrol(actor, pick, pick, priority);
  }
  
  
  public static void addFormalPatrols(
    Actor actor, Venue origin, Choice choice
  ) {
    ShieldWall wall = (ShieldWall) origin.world().presences.randomMatchNear(
      ShieldWall.class, origin, Stage.ZONE_SIZE
    );
    choice.add(Patrolling.sentryDuty(actor, wall, origin, Plan.ROUTINE));
    choice.add(Patrolling.nextGuardPatrol(actor, origin, Plan.CASUAL));
  }
  
  
  public static ShieldWall turretIsAboard(Target t) {
    if (! (t instanceof Mobile)) return null;
    final Boarding aboard = ((Mobile) t).aboard();
    if (aboard instanceof ShieldWall) return (ShieldWall) aboard;
    else return null;
  }
  
  
  public static float rateCompetence(
    Actor actor, Target guarded, int teamSize
  ) {
    //  TODO:  Include bonus from first aid or assembly skills, depending on the
    //  target and damage done?
    
    if (! PlanUtils.isArmed(actor)) return 0;
    final Tile under = actor.world().tileAt(guarded);
    return PlanUtils.combatWinChance(actor, under, teamSize);
  }
  
  
  
  /**  Obtaining and evaluating patrols targets-
    */
  final static Trait BASE_TRAITS[] = { FEARLESS, PATIENT, SOLITARY };
  
  protected float getPriority() {
    if (onPoint == null || patrolled.size() == 0) return 0;
    
    float urgency, avgDanger = 0, modifier;
    if (actor.base() != null) for (Target t : patrolled) {
      avgDanger += actor.base().dangerMap.sampleAround(t, Stage.ZONE_SIZE);
    }
    avgDanger = Nums.clamp(avgDanger / patrolled.size(), 0, 2);
    urgency   = avgDanger;
    modifier  = 0 - actor.senses.fearLevel();
    
    int teamSize = hasMotives(MOTIVE_MISSION) ? Mission.AVG_PARTY_LIMIT : 1;
    setCompetence(rateCompetence(actor, guarded, teamSize));
    
    toggleMotives(MOTIVE_EMERGENCY, PlanUtils.underAttack(guarded));
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, urgency + modifier, competence(),
      -1, Plan.REAL_FAIL_RISK * avgDanger, BASE_TRAITS
    );
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
    
    final Stage world = actor.world();
    Target stop = onPoint;
    
    //  First, check to see if there are any supplemental behaviours you could
    //  or should be performing (first aid, repairs, or defence.)
    
    final Plan old = (lastStep instanceof Plan) ? (Plan) lastStep : null;
    final Choice choice = new Choice(actor);
    final float range = actor.health.sightRange() + 1;
    choice.isVerbose = report;
    
    final Target target = CombatUtils.bestTarget(actor, guarded, false);
    if (target != null && Spacing.distance(target, guarded) < range) {
      choice.add(new Combat(actor, (Element) target).setMotivesFrom(this, 0));
    }
    if (guarded instanceof Actor) {
      choice.add(new FirstAid(actor, (Actor) guarded).setMotivesFrom(this, 0));
    }
    if (guarded instanceof Venue) {
      choice.add(new Repairs (actor, (Venue) guarded, isJob()));
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
      final Pathing p = ((Mobile) onPoint).pathing;
      final Target ahead = p == null ? onPoint : p.stepsAhead(2, true);
      
      Tile open = Spacing.pickRandomTile(ahead, 2, actor.world());
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
    
    //
    //  TODO:  Revisit this later...
    
    if (actor.senses.isEmergency()) {
      return MOTION_FAST;
    }
    
    if (guarded.isMobile()) {
      final Mobile m = (Mobile) guarded;
      final float dist = Spacing.distance(actor, m.aboard());
      if (dist >= actor.health.sightRange() / 2) return MOTION_FAST;
    }
    
    //  TODO:  Replace this with a general 'harm intended' clause?
    final Activities a = actor.world().activities;
    if (a.includesActivePlan(guarded, Combat.class)) return MOTION_FAST;
    
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
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_PROTECTION) {
      d.append("Guarding ");
      d.append(guarded);
    }
    if (type == TYPE_STREET_PATROL || type == TYPE_SENTRY_DUTY) {
      if (patrolled.size() == 1 || guarded == patrolled.last()) {
        d.appendAll("Patrolling around ", guarded);
      }
      else {
        d.append("Patrolling between ");
        d.append(guarded);
        d.append(" and ");
        d.append(patrolled.last());
      }
    }
  }
}












