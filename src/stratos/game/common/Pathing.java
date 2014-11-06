/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import org.apache.commons.math3.util.FastMath;

import stratos.game.actors.Choice;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



public class Pathing {
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final public static int MAX_PATH_SCAN = 8;
  private static boolean
    pathVerbose = false,
    verbose     = false;
  
  final Mobile mobile;
  Target trueTarget;
  Boarding pathTarget;
  
  Boarding path[] = null;
  int stepIndex = -1;
  
  
  public Pathing(Mobile a) {
    this.mobile = a;
  }
  
  
  void loadState(Session s) throws Exception {
    trueTarget = s.loadTarget();
    path = (Boarding[]) s.loadTargetArray(Boarding.class);
    pathTarget = (Boarding) s.loadTarget();
    stepIndex = s.loadInt();
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveTarget(trueTarget);
    s.saveTargetArray(path);
    s.saveTarget(pathTarget);
    s.saveInt(stepIndex);
  }
  
  
  
  /**  Updating current heading-
    */
  private boolean inLocus(Boarding b) {
    if (b == null) return false;
    return Spacing.innerDistance(mobile, b) < 0.25f;
  }
  
  
  public Target target() {
    return trueTarget;
  }
  
  
  public Boarding lastStep() {
    return pathTarget;
  }
  
  
  public Boarding nextStep() {
    if (stepIndex == -1 || path == null) return null;
    if (! path[stepIndex].inWorld()) return null;
    return path[stepIndex];
  }
  
  
  protected Boarding location(Target t) {
    if (t instanceof Boarding && t != mobile) return (Boarding) t;
    if (t instanceof Mobile) {
      final Mobile a = (Mobile) t;
      if (a.aboard() != null) return a.aboard();
      return a.origin();
    }
    if (t instanceof Element) {
      return mobile.world.tileAt(t);
    }
    I.complain("CANNOT GET LOCATION FOR: "+t);
    return null;
  }
  
  
  public void updateTarget(Target moveTarget) {
    final boolean report = verbose && I.talkAbout == mobile;
    if (report) I.say("\nUpdating path target: "+moveTarget);
    
    final Target oldTarget = trueTarget;
    this.trueTarget = moveTarget;
    if (trueTarget != oldTarget) {
      if (report) I.say("...TARGET HAS CHANGED: "+trueTarget);
      path = null; stepIndex = -1; return;
    }
    else if (inLocus(nextStep())) {
      stepIndex = Visit.clamp(stepIndex + 1, path.length);
    }
    else if (report) I.say("Not in locus of: "+nextStep());
  }
  
  
  public boolean checkPathingOkay() {
    final boolean report = pathVerbose && I.talkAbout == mobile;
    if (trueTarget == null) {
      if (report) I.say("\nNo current path target!");
      return false;
    }
    if (path == null) {
      if (report) I.say("\nNo current path to target: "+trueTarget);
      return false;
    }
    
    final Boarding dest = location(trueTarget);
    boolean blocked = false, nearTarget = false, validPath = true;
    if (report) {
      I.say("\nChecking path okay for "+mobile);
      I.say("  True target: "+trueTarget+", dest: "+dest);
    }
    //
    //  Check to ensure that subsequent steps along this path are not blocked,
    //  and that the path target has not changed.
    validPath = nextStep() != null && pathTarget == dest;
    
    if (validPath) for (int i = 0; i < MAX_PATH_SCAN; i++) {
      final int index = stepIndex + i;
      if (index >= path.length) break;
      final Boarding t = path[index];
      
      if (PathSearch.blockedBy(t, mobile)) blocked = true;
      else if (! t.inWorld()) blocked = true;
      if (t == dest) nearTarget = true;
      
      if (report) I.say("  Blockage at "+t+"? "+blocked);
    }
    if (blocked) {
      if (report) I.say("  PATH IS BLOCKED");
      validPath = false;
    }
    //
    //  In the case that the path we're following is only partial, update once
    //  we're approaching the terminus-
    if (validPath && (! nearTarget) && (Visit.last(path) != dest)) {
      final int dist = path.length - (stepIndex + 1);
      if (dist < Stage.PATCH_RESOLUTION / 2) {
        if (report) I.say("  NEAR END OF PATH");
        validPath = false;
      }
    }
    return validPath;
  }
  
  
  public boolean refreshFullPath() {
    final boolean report = verbose && I.talkAbout == mobile;
    if (report) I.say("REFRESHING PATH TO: "+trueTarget);
    
    final Boarding origin = location(mobile);
    if (trueTarget == null) path = null;
    else {
      //  Firstly, we perform some basic sanity checks on the start and end
      //  points of the prospective route.
      pathTarget = location(trueTarget);
      if (report) I.say("BETWEEN: "+origin+" AND "+pathTarget);
      if (
        PathSearch.blockedBy(origin, mobile) ||
        PathSearch.blockedBy(pathTarget, mobile)
      ) path = null;
      else path = pathBetween(origin, pathTarget);
    }
    if (path == null) {
      if (report || (I.talkAbout == mobile && Choice.verbose)) {
        I.say("COULDN'T PATH TO: "+pathTarget);
      }
      mobile.pathingAbort();
      stepIndex = -1;
      return false;
    }
    
    //  If those pass inspection, we then select the next step in the currently
    //  approved path-
    else {
      if (report) {
        I.say("PATH IS: ");
        for (Boarding b : path) {
          I.add(b+" "+PathSearch.blockedBy(b, mobile)+" ");
        }
      }
      int index = 0;
      while (index < path.length) if (path[index++] == origin) break;
      stepIndex = Visit.clamp(index, path.length);
      return true;
    }
  }
  
  
  protected Boarding[] pathBetween(Boarding initB, Boarding destB) {
    if (GameSettings.pathFree) {
      final PathSearch search = new PathSearch(initB, destB, -1);
      if (verbose && I.talkAbout == mobile) search.verbose = true;
      search.client = mobile;
      search.doSearch();
      return search.fullPath(Boarding.class);
    }
    else {
      return mobile.world().pathingCache.getLocalPath(
        initB, destB, MAX_PATH_SCAN * 2,
        mobile, (verbose && I.talkAbout == mobile)
      );
    }
  }
  
  
  
  /**  Specialty methods for modifying the position/facing of actors-
    */
  private static Vec3D temp3 = new Vec3D();
  private static Vec2D temp2 = new Vec2D();
  
  private Vec2D displacement(Target target) {
    final Vec3D p = target.position(temp3);
    final Vec2D disp = new Vec2D(
      p.x - mobile.position.x,
      p.y - mobile.position.y
    );
    return disp;
  }
  
  
  public void headTowards(
    Target target, float speed, boolean moves
  ) {
    final boolean report = I.talkAbout == mobile && verbose;
    if (report) {
      I.say("\n"+mobile+" HEADING TOWARDS: "+target+" FROM: "+mobile.origin());
    }
    
    //  Don't move if something ahead is blocking entrance-
    if (target instanceof Tile) {
      final Series <Mobile> inside = ((Tile) target).inside();
      if (inside.size() > 0) for (Mobile m : inside) if (deferTo(m)) {
        speed /= 2;
      }
    }
    //
    //  Determine the appropriate offset and angle for this target-
    if (target == null) return;
    final Vec2D disp = displacement(target);
    final float dist = disp.length();
    float angle = dist == 0 ? 0 : disp.normalise().toAngle();
    float moveRate = moves ? (speed / Stage.UPDATES_PER_SECOND) : 0;
    if (report) I.say("  MOVE DIST: "+dist);
    //
    //  Determine how far one can move this update, including limits on
    //  maximum rotation-
    final float maxRotate = speed * 180 / (
      Stage.UPDATES_PER_SECOND * GameSettings.actorScale
    );
    final float
      angleDif = Vec2D.degreeDif(angle, mobile.rotation),
      absDif   = Math.abs(angleDif);
    if (absDif > maxRotate) {
      angle = mobile.rotation + (maxRotate * (angleDif > 0 ? 1 : -1));
      angle = (angle + 360) % 360;
      moveRate *= (180 - absDif) / 180;
    }
    disp.scale(Math.min(moveRate, dist));
    
    //  Otherwise, apply the changes in heading-
    mobile.nextPosition.x = disp.x + mobile.position.x;
    mobile.nextPosition.y = disp.y + mobile.position.y;
    if (dist > 0) mobile.nextRotation = angle;
    if (
      Float.isNaN(mobile.nextPosition.x) ||
      Float.isNaN(mobile.nextPosition.y)
    ) {
      I.say("ILLEGAL POSITION");
      new Exception().printStackTrace();
    }
  }
  
  
  private boolean deferTo(Mobile other) {
    //  TODO:  See below.
    if (true) return false;
    final Vec2D
      heading = temp2.setFromAngle(mobile.rotation),
      disp    = displacement(other);
    if (heading.dot(disp) < 0) return false;
    
    return other.hashCode() > mobile.hashCode();
  }
  
  
  public void applyCollision(float moveRate, Target focus) {
    final boolean report = I.talkAbout == mobile && verbose;
    //  TODO:  I am probably going to have to implement some kind of proper
    //  polygonal pathfinding here.  For the moment, it's just kind of
    //  distracting.
    if (true) {
      
      //  If your current location is blocked, you need to escape to a free tile-
      if (PathSearch.blockedBy(mobile.aboard(), mobile)) {
        final Tile blocked = mobile.origin();
        final Tile free = Spacing.nearestOpenTile(blocked, mobile);
        if (free == null) I.complain("NO FREE TILE AVAILABLE!");
        if (report) I.say("Escaping to free tile: "+free);
        mobile.setPosition(free.x, free.y, mobile.world());
        mobile.onMotionBlock(blocked);
        return;
      }
      return;
    }
    
    final Mobile m = mobile;
    if (m.indoors() || ! m.collides()) return;
    final Vec2D sum = new Vec2D(), disp = new Vec2D();
    int numHits = 0;
    final float mMin = m.position.z, mMax = mMin + m.height();
    final Box2D area = m.area(null).expandBy(1);
    //
    //  After determining height range and ground-area affected, we iterate
    //  over every tile likely to be affected-
    for (Tile t : m.world.tilesIn(area, true)) {
      //
      //  Firstly, we check to avoid collision with nearby blocked tiles-
      if (t.blocked()) {
        final float tMax = t.onTop() == null ? 0 : t.onTop().height();
        if (mMin > tMax) continue;
        disp.set(m.position.x - t.x, m.position.y - t.y);
        final float dist = disp.length() - (m.radius() + t.radius());
        if (dist > 0 || disp.length() == 0) continue;
        sum.add(disp.normalise().scale(0 - dist));
        numHits++;
      }
      //
      //  Then, we check for collision with other mobiles within the same
      //  height range-
      //  TODO:  This isn't actually guaranteed to catch all nearby mobiles
      //  unless you register them in multiple tiles.  See about that.
      else for (Mobile near : t.inside()) {
        if (near == m || near == focus || ! near.collides()) continue;
        final float
          nMin = near.aboveGroundHeight(),
          nMax = nMin + near.height();
        if (nMin > mMax || mMin > nMax) continue;
        //
        //  Then, we establish the relative distance and displacement-
        disp.setFromAngle(Rand.num() * 360).scale(0.1f * m.radius());
        disp.x += m.position.x - near.position.x;
        disp.y += m.position.y - near.position.y;
        final float dist = disp.length() - (m.radius() + near.radius());
        if (dist > 0 || disp.length() == 0) continue;
        //
        //  If both check out, we increase the translation based on distance
        //  within the sum of radii, with a small random 'salt'-
        sum.add(disp.normalise().scale(0 - dist));
        numHits++;
      }
    }
    if (numHits == 0 || sum.length() == 0) return;
    sum.scale(0.5f / numHits);
    
    //  Ensure that the effects of displacement never entirely cancel basic
    //  movement rate-
    final Vec2D facing = new Vec2D().setFromAngle(mobile.rotation);
    final float SL = sum.length(), push = sum.dot(facing) * -1f / SL;
    if (push > 0 && SL > 0) {
      final float minDisp = moveRate * 0.5f / Stage.UPDATES_PER_SECOND;
      facing.scale((SL - minDisp) * push);
      sum.add(facing).normalise().scale((minDisp + SL) / 2);
    }
    
    final int WS = m.world.size - 1;
    m.nextPosition.x = Visit.clamp(sum.x + m.nextPosition.x, 0, WS);
    m.nextPosition.y = Visit.clamp(sum.y + m.nextPosition.y, 0, WS);
    
    //  If your current location is blocked, you need to escape to a free tile-
    if (PathSearch.blockedBy(mobile.aboard(), mobile)) {
      final Tile blocked = mobile.origin();
      final Tile free = Spacing.nearestOpenTile(blocked, mobile);
      if (free == null) I.complain("NO FREE TILE AVAILABLE!");
      if (report) I.say("Escaping to free tile: "+free);
      mobile.setPosition(free.x, free.y, mobile.world());
      mobile.onMotionBlock(blocked);
      return;
    }
  }
  
  
  //
  //  TODO:  Consider removing this to the Action or Motion classes
  public boolean facingTarget(Target target) {
    final boolean report = I.talkAbout == mobile && verbose;
    if (target == null) return false;
    
    final Vec2D disp = displacement(target);
    if (disp.length() == 0) return true;
    final float angleDif = FastMath.abs(Vec2D.degreeDif(
      disp.normalise().toAngle(), mobile.rotation
    ));
    if (report) I.say("Angle difference is: "+angleDif);
    return angleDif < 30;
  }
}






