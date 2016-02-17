/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.verse.*;
import stratos.util.*;
import stratos.game.verse.EntryPoints.*;



public class PilotUtils {
  
  
  private static boolean
    landVerbose = false,
    flyVerbose  = false;
  
  
  /**  Utility methods for handling takeoff and landing:
    */
  static boolean isBoarding(Mobile m, Vehicle ship) {
    if (m.aboard() != ship) return false;
    if (Journey.activityFor(m) != null) return true;
    return false;
  }
  
  
  static void offloadPassengers(Vehicle ship, boolean landing) {
    final Stage world = ship.world();
    for (Mobile m : ship.inside()) {
      if (landing && ! m.inWorld()) {
        m.enterWorldAt(ship, world);
      }
      if ((! landing) && ! isBoarding(m, ship)) {
        m.goAboard(ship.dropPoint(), world);
      }
    }
  }
  
  
  static Boarding performLanding(
    Vehicle ship, Stage world, float entryFace
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
    
    if (dropPoint instanceof Docking) {
      //
      //  Rely on the docking functions of the landing site...
      ((Docking) dropPoint).setAsDocked(ship, true);
      return dropPoint;
    }
    else {
      //
      //  Claim any tiles underneath as owned, and either crush or evacuate any
      //  occupants-
      site.expandBy(-1);
      final Batch <Mobile> under = new Batch <Mobile> ();
      for (Tile t : Spacing.perimeter(site, world)) {
        t.clearUnlessOwned();
      }
      for (Tile t : world.tilesIn(site, false)) {
        if (report) I.say("    Claiming tile: "+t);
        if (t.above() != null) t.above().setAsDestroyed(false);
        Visit.appendTo(under, t.inside());
        t.setAbove(ship, true);
      }
      for (Mobile m : under) if (m != ship) {
        final Tile e = Spacing.nearestOpenTile(m.origin(), m);
        if (e == null) {
          I.say("\nWARNING, COULD NOT FIND ESCAPE POINT FOR DROP: "+ship);
          m.goAboard(ship, world);
        }
        else m.setPosition(e.x, e.y, world);
      }
      //
      //  Determine the position of the entrance tile-
      final int size = (int) site.xdim();
      final int EC[] = SiteUtils.entranceCoords(size, size, entryFace);
      final Tile exit = world.tileAt(site.xpos() + EC[0], site.ypos() + EC[1]);
      if (exit == null) I.complain("NO EXIT FOUND FOR "+ship);
      if (report) {
        I.say("  Area size:     "+size);
        I.say("  Entrance x/y:  "+EC[0]+"/"+EC[1]);
        I.say("  New exit:      "+exit+", blocked? "+exit.blocked());
      }
      //
      //  And just make sure the exit is clear-
      if (exit.above() != null) exit.above().setAsDestroyed(false);
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
  
  
  static void performTakeoff(Stage world, Vehicle ship, Vec3D exitPoint) {
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
      if (dropPoint instanceof Docking) {
        if (report) I.say("  Taking off from hangar...");
        ((Docking) dropPoint).setAsDocked(ship, false);
      }
      else {
        if (report) I.say("  Taking off from ground...");
        site.expandBy(-1);
        for (Tile t : world.tilesIn(site, false)) t.setAbove(null, true);
      }
    }
    
    if (report) I.say("  Exit point: "+exitPoint);
    ship.assignLandPoint(exitPoint, null);
  }
  
  
  public static boolean performTakeoffCheck(
    Vehicle ship, int maxStayDuration
  ) {
    if (! ship.structure.intact()) return false;
    final Stage world = ship.world();
    final Journey j = ship.journey();
    if (j == null || j.complete() || ! ship.landed()) return false;
    
    if (j.destination() != world.localSector()) {
      if (allAboard(ship)) { ship.beginTakeoff(); return true; }
      else if (! ship.boarding()) ship.beginBoarding();
    }
    return false;
  }
  
  
  static boolean allAboard(Vehicle ship) {
    //  TODO:  Include a specific provision for associated missions here!
    
    for (Mobile m : ship.staff.lodgers()) {
      if (! isBoarding(m, ship)) return false;
    }
    for (Mobile m : ship.staff.workers()) {
      if (! isBoarding(m, ship)) return false;
    }
    return true;
  }
  
  
  public static void completeTakeoff(Stage world, Vehicle ship) {
    final Verse verse = world.offworld;
    final Sector
      from = world.localSector(),
      goes = verse.journeys.destinationFor(ship);
    
    final Journey j = Journey.configForTrader(ship, from, goes, world);
    if (j != null) j.beginJourney();
    ship.assignLandPoint(null, null);
  }
  
  
  
  /**  Dealing with motion during flight:
    */
  static void adjustFlight(
    Vehicle ship, Vec3D aimPos, float aimRot, float height, float topSpeed
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
      I.say("  Current position: "+position);
      I.say("  Current rotation: "+rotation);
      I.say("  Target offset:    "+disp    );
      I.say("  heading:          "+heading );
    }
    //
    //  Calculate rate of lateral speed and descent-
    final float UPS = 1f / Stage.UPDATES_PER_SECOND;
    final float speed  = topSpeed * height * UPS;
    float       ascent = topSpeed * UPS / 4;
    ascent = Nums.min(ascent, Nums.abs(position.z - aimPos.z));
    if (ship.flightState() == Vehicle.STATE_LANDING) ascent *= -1;
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
  
}




