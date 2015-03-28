/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.economic;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.Inventory.Owner;
import stratos.game.maps.*;
import stratos.game.politic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class ShipUtils {
  
  
  private static boolean
    landVerbose = false,
    flyVerbose  = false,
    siteVerbose = false;
  
  
  /**  Utility methods for handling takeoff and landing:
    */
  static boolean isBoarding(Mobile m, Dropship ship) {
    if (m.aboard() != ship) return false;
    if (Offworld.activityFor(m) != null) return true;
    return false;
  }
  
  
  static void offloadPassengers(Dropship ship, boolean landing) {
    final Stage world = ship.world();
    
    for (Mobile m : ship.inside()) {
      if (landing && ! m.inWorld()) {
        m.enterWorldAt(ship, world);
        m.goAboard(ship, world);
      }
      if ((! landing) && ! isBoarding(m, ship)) {
        m.goAboard(ship.dropPoint(), world);
      }
    }
  }
  
  
  static Boarding performLanding(
    Dropship ship, Stage world, float entryFace
  ) {
    final boolean report = landVerbose && I.talkAbout == ship;
    final Boarding dropPoint = ship.dropPoint();
    final Box2D site = new Box2D(ship.landArea());
    
    if (report) {
      I.say("\n"+ship+" performing landing!");
      I.say("  Landing site:  "+site     );
      I.say("  Entrance face: "+entryFace);
      I.say("  Drop point:    "+dropPoint);
    }
    
    if (dropPoint instanceof Venue) {
      //
      //  Rely on the docking functions of the landing site...
      ((Airfield) dropPoint).setToDock(ship);
      return dropPoint;
    }
    else {
      //
      //  Claim any tiles underneath as owned, and either crush or evacuate any
      //  occupants-
      site.expandBy(-1);
      final Batch <Mobile> under = new Batch <Mobile> ();
      for (Tile t : Spacing.perimeter(site, world)) {
        if (t.onTop() != null && t.owningTier() <= Owner.TIER_NATURAL) {
          t.onTop().setAsDestroyed();
        }
      }
      for (Tile t : world.tilesIn(site, false)) {
        if (report) I.say("    Claiming tile: "+t);
        if (t.onTop() != null) t.onTop().setAsDestroyed();
        Visit.appendTo(under, t.inside());
        t.setOnTop(ship);
      }
      for (Mobile m : under) if (m != ship) {
        final Tile e = Spacing.nearestOpenTile(m.origin(), m);
        m.setPosition(e.x, e.y, world);
      }
      //
      //  Determine the position of the entrance tile-
      final int size = (int) site.xdim();
      final int EC[] = Placement.entranceCoords(size, size, entryFace);
      final Tile exit = world.tileAt(site.xpos() + EC[0], site.ypos() + EC[1]);
      if (exit == null) I.complain("NO EXIT FOUND FOR "+ship);
      if (report) {
        I.say("  Area size:     "+size);
        I.say("  Entrance x/y:  "+EC[0]+"/"+EC[1]);
        I.say("  New exit:      "+exit+", blocked? "+exit.blocked());
      }
      //
      //  And just make sure the exit is clear-
      if (exit.onTop() != null) exit.onTop().setAsDestroyed();
      ship.assignLandPoint(ship.aiming(), exit);
      exit.refreshAdjacent();
      if (report) for (Boarding b : exit.canBoard()) {
        I.say("    Leads to: "+b);
      }
      if (! Visit.arrayIncludes(exit.canBoard(), ship)) {
        I.complain("SHIP'S DROP POINT DOES NOT LEAD BACK TO SHIP: "+ship);
      }
      return exit;
    }
  }
  
  
  static void performTakeoff(Stage world, Dropship ship) {
    final boolean report = landVerbose && I.talkAbout == ship;
    if (report) I.say("\n"+ship+" performing takeoff!");
    
    final Boarding dropPoint = ship.dropPoint();
    Box2D site = ship.landArea();
    if (site == null) {
      if (report) I.say("  Ship had no landing site- will assign at source.");
      ship.assignLandPoint(ship.position(null), dropPoint);
      site = ship.landArea();
    }
    
    if (ship.landed()) {
      if (dropPoint instanceof Airfield) {
        if (report) I.say("  Taking off from hangar...");
        ((Airfield) dropPoint).setToDock(null);
      }
      else {
        if (report) I.say("  Taking off from ground...");
        site.expandBy(-1);
        for (Tile t : world.tilesIn(site, false)) t.setOnTop(null);
      }
    }
    final Tile exits = Spacing.pickRandomTile(
      ship.origin(), Dropship.INIT_DIST, world
    );
    final Vec3D exitPoint = new Vec3D(exits.x, exits.y, Dropship.INIT_HIGH);
    
    if (report) I.say("  Exit point: "+exitPoint);
    ship.assignLandPoint(exitPoint, null);
  }
  
  
  public static boolean allAboard(Dropship ship) {
    for (Mobile m : ship.staff.workers()) {
      if (! isBoarding(m, ship)) return false;
    }
    return true;
  }
  
  
  /**  Dealing with motion during flight:
    */
  static void adjustFlight(
    Dropship ship, Vec3D aimPos, float aimRot, float height
  ) {
    final boolean report = flyVerbose && I.talkAbout == ship;
    if (report) I.say("\nAdjusting flight heading for "+ship);
    //
    //  Firstly, determine what your current position is relative to the aim
    //  point-
    final Stage world = ship.world();
    final Vec3D
      position     = ship.position(null),
      nextPosition = new Vec3D(position);
    float
      rotation     = ship.rotation(),
      nextRotation = rotation;
    final Vec3D disp = aimPos.sub(position, null);
    final Vec2D heading = new Vec2D().setTo(disp).scale(-1);
    if (report) {
      I.say("  Current position: "+position    );
      I.say("  Current rotation: "+rotation    );
      I.say("  Target offset:    "+disp        );
      I.say("  heading:          "+heading     );
    }
    //
    //  Calculate rate of lateral speed and descent-
    final float UPS = 1f / Stage.UPDATES_PER_SECOND;
    final float speed = Dropship.TOP_SPEED * height * UPS;
    float ascent = Dropship.TOP_SPEED * UPS / 4;
    ascent = Nums.min(ascent, Nums.abs(position.z - aimPos.z));
    if (ship.flightStage() == Dropship.STAGE_DESCENT) ascent *= -1;
    //
    //  Then head toward the aim point (for non-zero displacement)-
    if (disp.length() > 0) {
      if (disp.length() > speed) disp.scale(speed / disp.length());
      disp.z = 0;
      nextPosition.setTo(position).add(disp);
      nextPosition.z = position.z + ascent;
    }
    //
    //  And adjust rotation (for non-zero headings)-
    if (heading.length() > 0) {
      float angle = (heading.toAngle() * height) + (aimRot * (1 - height));
      final float
        angleDif  = Vec2D.degreeDif(angle, rotation),
        absDif    = Nums.abs(angleDif),
        maxRotate = 90 * UPS;
      if (height < 0.5f && absDif > maxRotate) {
        angle = rotation + (maxRotate * (angleDif > 0 ? 1 : -1));
        angle = (angle + 360) % 360;
      }
      nextRotation = angle;
    }
    //
    //  Then apply these changes to the vessel itself:
    if (report) {
      I.say("  Speed:            "+speed       );
      I.say("  Ascent rate:      "+ascent      );
      I.say("  Next position:    "+nextPosition);
      I.say("  Next rotation:    "+nextRotation);
    }
    ship.setHeading(nextPosition, nextRotation, false, world);
  }
  
  
  
  
  /**  Finally, utility methods for finding a suitable landing point-
    */
  //  TODO:  Treat landing sites as a type of venue and generate them using
  //  the PlacementGrid-scanning methods.  (Like you need to do for holdings?)
  //
  //  TODO:  Alternatively, use DeliveryUtils to try rating placement sites?
  //
  static boolean checkLandingArea(Dropship ship, Stage world, Box2D area) {
    final Boarding dropPoint = ship.dropPoint();
    if (dropPoint instanceof Venue) {
      if (! dropPoint.inWorld()) return false;
      final Airfield strip = (Airfield) dropPoint;
      final Vec3D aimPos = strip.dockLocation(ship);
      ship.assignLandPoint(aimPos, strip);
      return true;
    }
    else for (Tile t : world.tilesIn(area, false)) {
      if (t == null) return false;
      if (t.onTop() == ship) continue;
      if (PavingMap.pavingReserved(t, true) || ! t.buildable()) return false;
    }
    return true;
  }
  
  
  public static boolean findLandingSite(final Dropship ship, final Base base) {
    final boolean report = siteVerbose && BaseUI.current().played() == base;
    
    ship.assignBase(base);
    final Stage world = base.world;
    //
    //  If your current landing site is still valid, then keep it.
    final Box2D landArea = ship.landArea();
    if (landArea != null && checkLandingArea(ship, world, landArea)) {
      if (report) {
        I.say("\nCurrent landing site valid for "+ship+":");
        I.say("  "+ship.landArea());
      }
      return true;
    }
    //
    //  If that fails, check to see if landing at a supply depot/launch hangar
    //  is possible:
    final Pick <Airfield> pick = new Pick <Airfield> ();
    
    for (Object o : world.presences.matchesNear(Airfield.class, ship, -1)) {
      final Airfield strip = (Airfield) o;
      if (strip.docking() != null || ! strip.structure.intact()) continue;
      float rating = 0;
      for (Traded good : Economy.ALL_MATERIALS) {
        rating += Nums.max(0, strip.stocks.shortageOf(good));
        rating += Nums.max(0, strip.stocks.surplusOf (good));
      }
      rating /= 2 * ALL_MATERIALS.length;
      pick.compare(strip, rating);
    }
    final Airfield strip = pick.result();
    if (strip != null) {
      final Vec3D aimPos = strip.dockLocation(ship);
      ship.assignLandPoint(aimPos, strip);
      strip.setToDock(ship);
      I.say("Landing at depot: "+strip);
      return true;
    }
    //
    //  Otherwise, search for a suitable landing site on the bare ground near
    //  likely customers:
    final Tile midTile = world.tileAt(world.size / 2, world.size / 2);
    final Presences p = world.presences;
    Target nearest = null;
    nearest = p.randomMatchNear(SERVICE_COMMERCE, midTile, -1);
    if (findLandingSite(ship, nearest, base)) return true;
    nearest = p.randomMatchNear(base, midTile, -1);
    if (findLandingSite(ship, nearest, base)) return true;
    nearest = p.nearestMatch(base, midTile, -1);
    if (findLandingSite(ship, nearest, base)) return true;
    nearest = midTile;
    if (findLandingSite(ship, nearest, base)) return true;
    return false;
  }

  
  private static boolean findLandingSite(
    final Dropship ship, Target from, final Base base
  ) {
    if (from == null) return false;
    final Tile init = Spacing.nearestOpenTile(base.world.tileAt(from), from);
    if (init == null) return false;
    final boolean report = siteVerbose && BaseUI.current().played() == base;
    
    //
    //  Then, spread out to try and find a decent landing site-
    final Box2D area = ship.area(null);
    final int maxDist = Stage.SECTOR_SIZE * 2;
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, init) > maxDist) return false;
        return ! t.blocked();
      }
      protected boolean canPlaceAt(Tile t) {
        area.xpos(t.x - 0.5f);
        area.ypos(t.y - 0.5f);
        return checkLandingArea(ship, base.world, area);
      }
    };
    spread.doSearch();
    
    //
    //  
    if (spread.success()) {
      final Vec3D aimPos = new Vec3D(
        area.xpos() + (area.xdim() / 2f),
        area.ypos() + (area.ydim() / 2f),
        0
      );
      aimPos.z = base.world.terrain().trueHeight(aimPos.x, aimPos.y);
      ship.assignLandPoint(aimPos, null);
      
      if (report) I.say("\n"+ship+" found landing at point: "+aimPos);
      return true;
    }
    
    else {
      ship.assignLandPoint(null, null);
      if (report) I.say("No landing site found for "+ship+".");
      return false;
    }
  }
}


