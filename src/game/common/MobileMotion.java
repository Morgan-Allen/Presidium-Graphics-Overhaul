/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.common.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class MobileMotion {
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final public static int MAX_PATH_SCAN = 8 ;
  private static boolean verbose = false ;
  
  final Mobile mobile ;
  Target trueTarget ;
  Boardable pathTarget ;
  
  Boardable path[] = null ;
  int stepIndex = -1 ;
  
  
  public MobileMotion(Mobile a) {
    this.mobile = a ;
  }
  
  
  void loadState(Session s) throws Exception {
    trueTarget = s.loadTarget() ;
    path = (Boardable[]) s.loadTargetArray(Boardable.class) ;
    pathTarget = (Boardable) s.loadTarget() ;
    stepIndex = s.loadInt() ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveTarget(trueTarget) ;
    s.saveTargetArray(path) ;
    s.saveTarget(pathTarget) ;
    s.saveInt(stepIndex) ;
  }
  
  
  
  /**  Updating current heading-
    */
  private boolean inLocus(Boardable b) {
    if (b == null) return false ;
    return Spacing.innerDistance(mobile, b) < 0.5f ;
  }
  
  
  public Target target() {
    return trueTarget ;
  }
  
  
  public Boardable lastStep() {
    return pathTarget ;
  }
  
  
  public Boardable nextStep() {
    if (stepIndex == -1 || path == null) return null ;
    if (! path[stepIndex].inWorld()) return null ;
    return path[stepIndex] ;
  }
  
  
  protected Boardable location(Target t) {
    if (t instanceof Boardable && t != mobile) return (Boardable) t ;
    if (t instanceof Mobile) {
      final Mobile a = (Mobile) t ;
      if (a.aboard() != null) return a.aboard() ;
      return a.origin() ;
    }
    if (t instanceof Element) {
      return Spacing.nearestOpenTile((Element) t, mobile, mobile.world()) ;
    }
    I.complain("CANNOT GET LOCATION FOR: "+t) ;
    return null ;
  }
  
  
  public void updateTarget(Target moveTarget) {
    final Target oldTarget = trueTarget ;
    this.trueTarget = moveTarget ;
    if (trueTarget != oldTarget) {
      if (verbose) I.sayAbout(mobile, "...TARGET HAS CHANGED: "+trueTarget) ;
      path = null ; stepIndex = -1 ; return ;
    }
    else if (inLocus(nextStep())) {
      stepIndex = Visit.clamp(stepIndex + 1, path.length) ;
    }
    else if (verbose) I.sayAbout(mobile, "Not in locus of: "+nextStep()) ;
  }
  
  
  public boolean checkPathingOkay() {
    if (trueTarget == null) return true ;
    if (path == null) return false ;
    final Boardable dest = location(trueTarget) ;
    boolean blocked = false, nearTarget = false, validPath = true ;
    //
    //  Check to ensure that subsequent steps along this path are not blocked,
    //  and that the path target has not changed.
    validPath = nextStep() != null && pathTarget == dest ;
    
    if (validPath) for (int i = 0 ; i < MAX_PATH_SCAN ; i++) {
      final int index = stepIndex + i ;
      if (index >= path.length) break ;
      final Boardable t = path[index] ;
      if (Pathing.blockedBy(t, mobile)) blocked = true ;
      else if (! t.inWorld()) blocked = true ;
      if (t == dest) nearTarget = true ;
    }
    if (blocked) {
      if (verbose) I.sayAbout(mobile, "PATH IS BLOCKED") ;
      validPath = false ;
    }
    //
    //  In the case that the path we're following is only partial, update once
    //  we're approaching the terminus-
    if (validPath && (! nearTarget) && (Visit.last(path) != dest)) {
      final int dist = path.length - (stepIndex + 1) ;
      if (dist < World.PATCH_RESOLUTION / 2) {
        if (verbose) I.sayAbout(mobile, "NEAR END OF PATH") ;
        validPath = false ;
      }
    }
    return validPath ;
  }
  
  
  public boolean refreshFullPath() {
    if (verbose) I.sayAbout(mobile, "REFRESHING PATH TO: "+trueTarget) ;
    
    final Boardable origin = location(mobile) ;
    if (trueTarget == null) path = null ;
    else {
      
      //  Firstly, we perform some basic sanity checks on the start and end
      //  points of the prospective route.
      pathTarget = location(trueTarget) ;
      if (verbose) I.sayAbout(mobile, "BETWEEN: "+origin+" AND "+pathTarget) ;
      if (
        Pathing.blockedBy(origin, mobile) ||
        Pathing.blockedBy(pathTarget, mobile)
      ) path = null ;
      else path = pathBetween(origin, pathTarget) ;
    }
    if (path == null) {
      if (verbose) I.sayAbout(mobile, "COULDN'T PATH TO: "+pathTarget) ;
      mobile.pathingAbort() ;
      stepIndex = -1 ;
      return false ;
    }
    
    //  If those pass inspection, we then select the next step in the currently
    //  approved path-
    else {
      if (verbose && I.talkAbout == mobile) {
        I.say("PATH IS: ") ;
        for (Boardable b : path) I.add(b+" "+Pathing.blockedBy(b, mobile)+" ") ;
      }
      int index = 0 ;
      while (index < path.length) if (path[index++] == origin) break ;
      stepIndex = Visit.clamp(index, path.length) ;
      return true ;
    }
  }
  
  
  protected Boardable[] pathBetween(Boardable initB, Boardable destB) {
    if (GameSettings.pathFree) {
      final Pathing search = new Pathing(initB, destB, -1) ;
      if (verbose && I.talkAbout == mobile) search.verbose = true ;
      search.client = mobile ;
      search.doSearch() ;
      return search.fullPath(Boardable.class) ;
    }
    else {
      return mobile.world().pathingCache.getLocalPath(
        initB, destB, MAX_PATH_SCAN * 2,
        mobile, (verbose && I.talkAbout == mobile)
      ) ;
    }
  }
  
  
  
  /**  Specialty methods for modifying the position/facing of actors-
    */
  private Vec2D displacement(Target target) {
    final Vec3D p = target.position(null) ;
    final Vec2D disp = new Vec2D(
      p.x - mobile.position.x,
      p.y - mobile.position.y
    ) ;
    return disp ;
  }
  
  
  public void headTowards(
    Target target, float speed, boolean moves
  ) {
    //
    //  Determine the appropriate offset and angle for this target-
    if (target == null) return ;
    final Vec2D disp = displacement(target) ;
    final float dist = disp.length() ;
    float angle = dist == 0 ? 0 : disp.normalise().toAngle() ;
    float moveRate = moves ? (speed / World.UPDATES_PER_SECOND) : 0 ;
    //
    //  Determine how far one can move this update, including limits on
    //  maximum rotation-
    final float maxRotate = speed * 180 / World.UPDATES_PER_SECOND ;
    final float
      angleDif = Vec2D.degreeDif(angle, mobile.rotation),
      absDif   = Math.abs(angleDif) ;
    if (absDif > maxRotate) {
      angle = mobile.rotation + (maxRotate * (angleDif > 0 ? 1 : -1)) ;
      angle = (angle + 360) % 360 ;
      moveRate *= (180 - absDif) / 180 ;
    }
    disp.scale(Math.min(moveRate, dist)) ;
    //
    //  Then apply the changes in heading-
    mobile.nextPosition.x = disp.x + mobile.position.x ;
    mobile.nextPosition.y = disp.y + mobile.position.y ;
    mobile.nextRotation = angle ;
    if (
      Float.isNaN(mobile.nextPosition.x) ||
      Float.isNaN(mobile.nextPosition.y)
    ) {
      I.say("ILLEGAL POSITION") ;
      new Exception().printStackTrace() ;
    }
  }
  
  
  //
  //  TODO:  Move this to a Senses class.
  public static boolean hasLineOfSight(
    Target origin, Target target, float maxRange
  ) {
    if (origin == null || target == null) return false ;
    if (maxRange > 0 && Spacing.distance(origin, target) > maxRange) {
      return false ;
    }
    final boolean reports = verbose && I.talkAbout == origin ;
    //
    //  Firstly, we determine the start and end points for the line segment,
    //  and the vector connecting the two-
    final World world = origin.world() ;
    final Vec2D
      orig = new Vec2D().setTo(origin.position(null)),
      dest = new Vec2D().setTo(target.position(null)),
      line = new Vec2D().setTo(dest).sub(orig) ;
    final float fullLength = line.length() ;
    final Batch <Tile> considered = new Batch <Tile> () ;
    final Vec2D toCheck = new Vec2D() ;
    //
    //  Next, we assemble a list of each tile that might plausibly intersect
    //  the line segment-
    for (int i = 0 ; i < fullLength ; i++) {
      toCheck.setTo(line).normalise().scale(i).add(orig) ;
      final Tile t = world.tileAt(toCheck.x, toCheck.y) ;
      if (t == null || t.flaggedWith() != null) continue ;
      considered.add(t) ;
      t.flagWith(considered) ;
      for (Tile n : t.edgeAdjacent(Spacing.tempT4)) {
        if (n == null || n.flaggedWith() != null) continue ;
        considered.add(n) ;
        n.flagWith(considered) ;
      }
    }
    for (Tile t : considered) t.flagWith(null) ;
    //
    //  Then, we check to see if any such tiles are actually blocked, and
    //  perform a more exacting intersection test-
    if (reports) {
      I.say("\nCHECKING LINE OF SIGHT") ;
      I.say("  Mobile origin: "+orig) ;
      I.say("  Target position: "+dest) ;
    }
    boolean blocked = false ;
    boolean onRight, onLeft ;
    for (Tile t : considered) if (t.blocked()) {
      if (t == target || t.owner() == target) continue ;
      //
      //  We first check whether the centre of the tile in question falls
      //  between the start and end points of the line segment-
      toCheck.set(t.x, t.y).sub(orig).normalise() ;
      final float dot = line.dot(toCheck) ;
      if (dot < 0 || dot > fullLength) continue ;
      onRight = onLeft = false ;
      //
      //  Then, we check to see if corners of this tile lie to both the left
      //  and right of the line segment-
      for (int d : Tile.N_DIAGONAL) {
        toCheck.set(
          t.x - (Tile.N_X[d] / 2f),
          t.y - (Tile.N_Y[d] / 2f)
        ).sub(orig) ;
        final float side = line.side(toCheck) ;
        if (side < 0) onLeft = true ;
        if (side > 0) onRight  = true ;
      }
      //
      //  If both checks are positive, we consider the tile blocked, and return
      //  the result-
      if (reports) {
        I.say("  Might be blocked at: "+t) ;
        I.say("  On right/left? "+onRight+"/"+onLeft) ;
      }
      if (onRight && onLeft) { blocked = true ; break ; }
    }
    if (reports) I.say(blocked ? "L_O_S BLOCKED!" : "L_O_S OKAY...") ;
    return ! blocked ;
  }
  
  
  public void applyCollision(float moveRate) {
    final Mobile m = mobile ;
    if (m.indoors()) return ;
    final Vec2D sum = new Vec2D(), disp = new Vec2D() ;
    int numHits = 0 ;
    final float mMin = m.position.z, mMax = mMin + m.height() ;
    final Box2D area = m.area(null).expandBy(1) ;
    //
    //  After determining height range and ground-area affected, we iterate
    //  over every tile likely to be affected-
    for (Tile t : m.world.tilesIn(area, true)) {
      //
      //  Firstly, we check to avoid collision with nearby blocked tiles-
      if (t.blocked()) {
        final float tMax = t.owner() == null ? 0 : t.owner().height() ;
        if (mMin > tMax) continue ;
        disp.set(m.position.x - t.x, m.position.y - t.y) ;
        final float dist = disp.length() - (m.radius() + t.radius()) ;
        if (dist > 0 || disp.length() == 0) continue ;
        sum.add(disp.normalise().scale(0 - dist)) ;
        numHits++ ;
      }
      //
      //  Then, we check for collision with other mobiles within the same
      //  height range-
      //  TODO:  This isn't actually guaranteed to catch all nearby mobiles
      //  unless you register them in multiple tiles.  See about that.
      else for (Mobile near : t.inside()) {
        if (near == m) continue ;
        final float
          nMin = near.aboveGroundHeight(),
          nMax = nMin + near.height() ;
        if (nMin > mMax || mMin > nMax) continue ;
        //
        //  Then, we establish the relative distance and displacement-
        disp.setFromAngle(Rand.num() * 360).scale(0.1f * m.radius()) ;
        disp.x += m.position.x - near.position.x ;
        disp.y += m.position.y - near.position.y ;
        final float dist = disp.length() - (m.radius() + near.radius()) ;
        if (dist > 0 || disp.length() == 0) continue ;
        //
        //  If both check out, we increase the translation based on distance
        //  within the sum of radii, with a small random 'salt'-
        sum.add(disp.normalise().scale(0 - dist)) ;
        numHits++ ;
      }
    }
    if (numHits == 0 || sum.length() == 0) return ;
    sum.scale(0.5f / numHits) ;
    
    //  Ensure that the effects of displacement never entirely cancel basic
    //  movement rate-
    final Vec2D facing = new Vec2D().setFromAngle(mobile.rotation) ;
    final float SL = sum.length(), push = sum.dot(facing) * -1f / SL ;
    if (push > 0 && SL > 0) {
      final float minDisp = moveRate * 0.5f / World.UPDATES_PER_SECOND ;
      facing.scale((SL - minDisp) * push) ;
      sum.add(facing).normalise().scale((minDisp + SL) / 2) ;
    }
    
    final int WS = m.world.size - 1 ;
    m.nextPosition.x = Visit.clamp(sum.x + m.nextPosition.x, 0, WS) ;
    m.nextPosition.y = Visit.clamp(sum.y + m.nextPosition.y, 0, WS) ;
    
    //  If your current location is blocked, you need to escape to a free tile-
    if (Pathing.blockedBy(mobile.aboard(), mobile)) {
      final Tile blocked = mobile.origin() ;
      final Tile free = Spacing.nearestOpenTile(blocked, mobile) ;
      if (free == null) I.complain("NO FREE TILE AVAILABLE!") ;
      if (verbose) I.sayAbout(mobile, "Escaping to free tile: "+free) ;
      mobile.setPosition(free.x, free.y, mobile.world()) ;
      mobile.onMotionBlock(blocked) ;
      return ;
    }
  }
  
  
  //
  //  TODO:  Consider removing this to the Action or Motion classes
  public boolean facingTarget(Target target) {
    if (target == null) return false ;
    final Vec2D disp = displacement(target) ;
    if (disp.length() == 0) return true ;
    final float angleDif = Math.abs(Vec2D.degreeDif(
      disp.normalise().toAngle(), mobile.rotation
    )) ;
    return angleDif < 30 ;
  }
}


