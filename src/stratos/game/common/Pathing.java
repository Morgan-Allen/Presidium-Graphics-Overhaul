/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.PathSearch;
import stratos.user.*;
import stratos.util.*;
import stratos.game.actors.Choice;



public class Pathing {
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final public static int MAX_PATH_SCAN = 8;
  public static boolean
    verbose      = false,
    moveVerbose  = false,
    pathVerbose  = false,
    extraVerbose = false;
  
  final Mobile mobile;
  Target   moveTarget;
  Boarding pathTarget;
  
  private Boarding path[]    = null;
  private int      stepIndex = -1  ;
  
  
  public Pathing(Mobile a) {
    this.mobile = a;
  }
  
  
  void loadState(Session s) throws Exception {
    moveTarget = (Target) s.loadObject();
    path       = (Boarding[]) s.loadObjectArray(Boarding.class);
    pathTarget = (Boarding) s.loadObject();
    stepIndex  = s.loadInt();
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObject     (moveTarget);
    s.saveObjectArray(path      );
    s.saveObject     (pathTarget);
    s.saveInt        (stepIndex );
  }
  
  
  
  /**  Updating current heading-
    */
  public boolean inLocus(Boarding b) {
    if (b == null) return false;
    return Spacing.innerDistance(mobile, b) < 0.5f;
  }
  
  
  public Target target() {
    return moveTarget;
  }
  
  
  public Boarding lastStep() {
    return pathTarget;
  }
  
  
  public Boarding nextStep() {
    if (stepIndex == -1 || path == null) return null;
    if (! path[stepIndex].inWorld()) return null;
    
    if (inLocus(path[stepIndex])) {
      stepIndex = Nums.clamp(stepIndex + 1, path.length);
    }
    return path[stepIndex];
  }
  
  
  public Boarding stepsAhead(int ahead, boolean clamp) {
    if (stepIndex == -1 || path == null) {
      return clamp ? mobile.origin() : null;
    }
    ahead += stepIndex;
    if (ahead >= path.length) {
      if (clamp) return (Boarding) Visit.last(path);
      else return null;
    }
    else return path[ahead];
  }
  
  
  public void updateTarget(Target moveTarget) {
    final boolean report = verbose && I.talkAbout == mobile;
    if (report && extraVerbose) I.say("\nUpdating path target: "+moveTarget);
    
    final Target oldTarget = this.moveTarget;
    final boolean paths = moveTarget != null && moveTarget != mobile.aboard();
    
    if (paths && PathSearch.accessLocation(moveTarget, mobile) == null) {
      I.say("\nWARNING:  "+mobile+" CANNOT ACCESS: "+moveTarget);
      mobile.pathingAbort();
      moveTarget = null;
    }
    this.moveTarget = moveTarget;
    if (moveTarget != oldTarget || moveTarget == null) {
      if (moveTarget != null && report) {
        I.say("\nTARGET HAS CHANGED TO: "+moveTarget);
        I.say("  FROM: "+oldTarget);
      }
      path = null;
      stepIndex = -1;
      return;
    }
    else nextStep();
    if (path != null && extraVerbose && ! inLocus(path[stepIndex])) {
      I.say("\nNot in locus of: "+path[stepIndex]);
    }
  }
  
  
  public boolean checkPathingOkay() {
    final boolean report = I.talkAbout == mobile && verbose;
    if (moveTarget == null) {
      if (report) I.say("\nNo current path target!");
      return false;
    }
    if (path == null) {
      if (report) I.say("\nNo current path to target: "+moveTarget);
      return false;
    }
    
    final Boarding dest = PathSearch.accessLocation(moveTarget, mobile);
    boolean blocked = false, nearTarget = false, validPath = true;
    if (report) {
      I.say("\nChecking path okay for "+mobile);
      I.say("  True target: "+moveTarget+", dest: "+dest);
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
    if (report) I.say("  Path valid? "+validPath);
    return validPath;
  }
  
  
  public boolean refreshFullPath() {
    final boolean report = I.talkAbout == mobile && verbose;
    if (report) {
      I.say("REFRESHING PATH TO: "+moveTarget);
      I.say("  Current position: "+mobile.aboard());
      I.say("  Path was: "+I.list(path));
    }

    //
    //  Firstly, we perform some basic sanity checks on the start and end
    //  points of the prospective route.  Assuming those check out, we attempt
    //  pathing between them.
    final Boarding origin = PathSearch.accessLocation(mobile    , mobile);
    pathTarget            = PathSearch.accessLocation(moveTarget, mobile);
    
    if (origin == null || pathTarget == null) {
      if (report) I.say("  Start/end points blocked: "+mobile+"/"+moveTarget);
      path = null;
    }
    else {
      path = pathBetween(origin, pathTarget);
    }
    if (path == null) {
      if (report) I.say("  COULDN'T PATH TO: "+pathTarget);
      mobile.pathingAbort();
      stepIndex = -1;
      return false;
    }
    //
    //  If those pass inspection, we then select the next step in the currently
    //  approved path-
    else {
      if (report) {
        I.say("PATH IS: ");
        for (Boarding b : path) {
          if (b instanceof Tile) {
            final Tile t = (Tile) b;
            I.add("["+t.x+" "+t.y+"]");
          }
          else I.add(b+" ");
        }
      }
      //
      //  We start off with whatever step forward on the path is closest- which
      //  is either the origin tile or the one immediately after.
      int pickIndex  = Nums.clamp(Visit.indexOf(origin, path), path.length);
      int afterIndex = Nums.clamp(pickIndex + 1              , path.length);
      Boarding firstStep = path[pickIndex], stepAfter = path[afterIndex];
      //
      //  If the first step is already behind the actor (or, somehow, further
      //  away than the second,) then we use the second step.
      final Vec3D forward =     Spacing.between(mobile, stepAfter) ;
      float distF = forward.dot(Spacing.between(mobile, firstStep));
      if (distF <= 0 || distF > forward.length()) pickIndex = afterIndex;
      
      stepIndex = pickIndex;
      return true;
    }
  }
  
  
  protected Boarding[] pathBetween(Boarding initB, Boarding destB) {
    final boolean report = I.talkAbout == mobile && pathVerbose && extraVerbose;
    
    if (GameSettings.pathFree) {
      final PathSearch search = new PathSearch(initB, destB, false);
      if (report) search.verbosity = Search.VERBOSE;
      search.assignClient(mobile);
      search.doSearch();
      return search.fullPath(Boarding.class);
    }
    else {
      return mobile.world().pathingMap.getLocalPath(
        initB, destB, MAX_PATH_SCAN * 2, mobile, report
      );
    }
  }
  
  
  
  /**  Specialty methods for modifying the position/facing of actors-
    */
  private static Vec3D temp3 = new Vec3D();
  
  public Vec2D displacement(Target target) {
    final Vec3D p = target.position(temp3);
    final Vec2D disp = new Vec2D(
      p.x - mobile.position.x,
      p.y - mobile.position.y
    );
    return disp;
  }
  
  
  //  TODO:  Specify rotation-rate separately?
  public void headTowards(
    Target target, float speed, float inertia, boolean moves
  ) {
    if (target == null) return;
    final boolean report = I.talkAbout == mobile && verbose && extraVerbose;
    if (report) {
      I.say("\n"+mobile+" HEADING TOWARDS: "+target+" FROM: "+mobile.origin());
    }
    //
    //  Determine the appropriate offset and angle for this target-
    final Vec2D disp = displacement(target);
    float dist = disp.length();
    final boolean canTurn = dist > 0;
    float angle = canTurn ? disp.normalise().toAngle() : 0;
    float moveRate = moves ? (speed / Stage.UPDATES_PER_SECOND) : 0;
    //
    //  Determine how far one can move this update, including limits on
    //  maximum rotation-
    final float maxRotate = speed * 180 / (
      Stage.UPDATES_PER_SECOND * Nums.max(1, mobile.radius()) * inertia
    );
    final float
      angleDif = Vec2D.degreeDif(angle, mobile.rotation),
      absDif   = Nums.abs(angleDif);
    if (absDif > maxRotate) {
      angle = mobile.rotation + (maxRotate * (angleDif > 0 ? 1 : -1));
      angle = (angle + 360) % 360;
      moveRate *= (180 - absDif) / 180;
    }
    //
    //  To ensure non-collision with other objects, we subtract both our and
    //  their radius from the distance moved.
    if (target != mobile.aboard() && ! (target instanceof Tile)) {
      dist -= target.radius();
      dist -= mobile.radius();
      if (dist < 0) dist = 0;
    }
    if (report) {
      I.say("  Full dist: "+disp.length());
      I.say("  Move dist: "+dist);
      I.say("  Move rate: "+moveRate);
      I.say("  Angle dif: "+angleDif);
    }
    disp.scale(Nums.min(moveRate, dist));
    //
    //  Otherwise, apply the changes in heading-
    mobile.nextPosition.x = disp.x + mobile.position.x;
    mobile.nextPosition.y = disp.y + mobile.position.y;
    if (canTurn) mobile.nextRotation = angle;
  }
  
  
  public void applyMotionCollision(float moveRate, Target focus) {
    final Mobile m = mobile;
    if (m.indoors() || (! m.collides()) || (! m.isMoving())) return;
    final boolean report = I.talkAbout == mobile && moveVerbose;
    if (report) {
      I.say("\nUpdating motion-collision for "+m);
    }
    //
    //  TODO:  I am probably going to have to implement some kind of proper
    //  polygonal pathfinding here.  For the moment, it's just kind of
    //  distracting.
    
    final Vec2D sum = new Vec2D(), disp = new Vec2D();
    int numHits = 0;
    final float mMin = m.position.z, mMax = mMin + m.height();
    final float selfRadius = Nums.min(m.radius(), 0.5f);
    final Box2D area = m.area(null).expandBy(1);
    //
    //  After determining height range and ground-area affected, we iterate
    //  over every tile likely to be affected-
    for (Tile t : m.world.tilesIn(area, false)) if (t != null) {
      //
      //  Firstly, we check to avoid collision with nearby blocked tiles-
      if (t.blocked()) {
        final float tMax = t.above() == null ? 0 : t.above().height();
        if (mMin > tMax) continue;
        disp.set(m.position.x - t.x, m.position.y - t.y);
        final float dist = disp.length() - (m.radius() + t.radius());
        if (dist > 0 || disp.length() == 0) continue;
        sum.add(disp.normalise().scale(0 - dist));
        numHits++;
        if (report) I.say("  Blocked tile: "+t);
      }
      //
      //  Then, we check for collision with other mobiles within the same
      //  height range and any adjacent tiles.  (We effectively clip diameter
      //  to one tile-unit for this purpose.)
      else for (Mobile near : t.inside()) {
        if (near == m || near == focus || ! near.collides()) continue;
        final float
          nMin       = near.aboveGroundHeight(),
          nMax       = nMin + near.height(),
          nearRadius = Nums.min(near.radius(), 0.5f);
        if (nMin > mMax || mMin > nMax) continue;
        //
        //  Then, we establish the relative distance and displacement-
        disp.setFromAngle(Rand.num() * 360).scale(0.1f * selfRadius);
        disp.x += m.position.x - near.position.x;
        disp.y += m.position.y - near.position.y;
        final float dist = disp.length() - (selfRadius + nearRadius);
        if (dist > 0 || disp.length() == 0) continue;
        //
        //  If both check out, we increase the translation based on distance
        //  within the sum of radii, with a small random 'salt'-
        sum.add(disp.normalise().scale(0 - dist));
        numHits++;
        if (report) I.say("  Hit other mobile: "+near);
      }
    }
    if (numHits == 0 || sum.length() == 0) return;
    if (report) I.say("  Sum of collisions: "+sum);
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
    if (report) I.say("  After averaging/adjustment: "+sum);
    
    final int WS = m.world.size - 1;
    m.nextPosition.x = Nums.clamp(sum.x + m.nextPosition.x, 0, WS);
    m.nextPosition.y = Nums.clamp(sum.y + m.nextPosition.y, 0, WS);
  }
  
  
  public void applyStaticCollision() {
    final Boarding aboard = mobile.aboard();
    if (PathSearch.blockedBy(aboard, mobile) && mobile.collides()) {
      final Tile free = Spacing.nearestOpenTile(aboard, mobile);
      if (free != null) {
        mobile.nextPosition.x = free.x;
        mobile.nextPosition.y = free.y;
      }
      else if (I.logEvents() && I.used60Frames) {
        I.say("MOBILE IS TRAPPED! "+this+" at "+aboard);
      }
    }
  }
  
  
  public boolean facingTarget(Target target) {
    final boolean report = I.talkAbout == mobile && moveVerbose;
    if (target == null) return false;
    
    final Vec2D disp = displacement(target);
    if (disp.length() == 0) return true;
    final float angleDif = Nums.abs(Vec2D.degreeDif(
      disp.normalise().toAngle(), mobile.rotation
    ));
    if (report) I.say("Angle difference is: "+angleDif);
    return angleDif < 30;
  }
}






