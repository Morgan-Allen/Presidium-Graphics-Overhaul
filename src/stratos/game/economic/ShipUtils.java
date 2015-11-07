/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.content.civic.Airfield;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class ShipUtils {
  
  
  private static boolean
    landVerbose = false,
    flyVerbose  = false,
    siteVerbose = true ;
  
  
  /**  Utility methods for handling takeoff and landing:
    */
  static boolean isBoarding(Mobile m, Dropship ship) {
    if (m.aboard() != ship) return false;
    if (VerseJourneys.activityFor(m) != null) return true;
    return false;
  }
  
  
  static void offloadPassengers(Dropship ship, boolean landing) {
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
        t.clearUnlessOwned();
      }
      for (Tile t : world.tilesIn(site, false)) {
        if (report) I.say("    Claiming tile: "+t);
        if (t.above() != null) t.above().setAsDestroyed();
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
      if (exit.above() != null) exit.above().setAsDestroyed();
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
        for (Tile t : world.tilesIn(site, false)) t.setAbove(null, true);
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
  
  
  public static void completeTakeoff(Stage world, Dropship ship) {
    world.offworld.journeys.handleEmmigrants(ship);
    ship.assignLandPoint(null, null);
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
  //
  //  TODO:  Treat landing sites as a type of venue and generate them using
  //  the PlacementGrid-scanning methods.  (Like you need to do for holdings?)
  //
  //  TODO:  Alternatively, use DeliveryUtils to try rating placement sites?
  
  static boolean checkLandingArea(Dropship ship, Stage world, Box2D area) {
    final Boarding dropPoint = ship.dropPoint();
    
    if (dropPoint instanceof Airfield) {
      final Airfield strip = (Airfield) dropPoint;
      if (rateAirfield(strip, ship) <= 0) return false;
      return true;
    }
    
    if (area != null) {
      if (SiteUtils.checkAreaClear(area, world)) return true;
      for (Tile t : world.tilesIn(area, false)) {
        if (t == null) return false;
        if (t.above() == ship) continue;
        if (PavingMap.pavingReserved(t, true) || ! t.buildable()) return false;
      }
      return true;
    }
    
    return false;
  }
  
  
  public static boolean findLandingSite(Dropship ship, boolean useCache) {
    //
    //  Basic variable setup and sanity checks-
    final Base  base  = ship.base();
    final Stage world = base.world ;
    final boolean report = siteVerbose && BaseUI.currentPlayed() == base;
    if (ship.landed()) return true;
    //
    //  If your current landing site is still valid, then keep it.
    if (useCache && checkLandingArea(ship, world, ship.landArea())) {
      if (report) {
        I.say("\nCurrent landing site valid for "+ship);
        I.say("  Point: "+ship.dropPoint());
        I.say("  Area:  "+ship.landArea ());
      }
      return true;
    }
    //
    //  If that fails, check to see if landing at a supply depot/launch hangar
    //  is possible:
    final Airfield strip = findAirfield(ship, world);
    if (strip != null) {
      final Vec3D aimPos = strip.dockLocation(ship);
      ship.assignLandPoint(aimPos, strip);
      strip.setToDock(ship);
      if (report) I.say("Landing at airfield: "+strip);
      return true;
    }
    //
    //  Otherwise, search for a suitable landing site on the bare ground near
    //  likely customers:
    return findLandingArea(ship, base);
  }
  
  
  private static float rateAirfield(Airfield strip, Dropship ship) {
    if ((! strip.inWorld()) || ! strip.structure.intact()) return -1;
    if (strip.docking() != null && strip.docking() != ship) return -1;

    //  TODO:  SEE IF YOU CAN USE THE BRING-UTILS CLASS FOR THIS (see below.)
    //  In principle, the findLandingArea class is already searching for
    //  commerce-venues/trade-venues, so if one of those is an Airfield, that
    //  should work fine...
    float rating = 1;
    for (Traded good : Economy.ALL_MATERIALS) {
      rating += Nums.max(0, strip.stocks.shortageOf(good));
      rating += Nums.max(0, strip.stocks.surplusOf (good));
    }
    rating /= 2 * ALL_MATERIALS.length;
    return rating;
  }
  
  
  private static Airfield findAirfield(Dropship ship, Stage world) {
    final Pick <Airfield> pick = new Pick <Airfield> (null, 0);
    for (Object o : world.presences.matchesNear(Airfield.class, ship, -1)) {
      final Airfield strip = (Airfield) o;
      pick.compare(strip, rateAirfield(strip, ship));
    }
    return pick.result();
  }

  
  private static boolean findLandingArea(
    final Dropship ship, final Base base
  ) {
    final boolean report = siteVerbose && BaseUI.current().played() == base;
    
    final Stage world = base.world;
    final Presences p = world.presences;
    final int ZS = Stage.ZONE_SIZE;
    final Box2D area = ship.area(null);
    final Traded goods[] = ship.cargo.demanded();
    //
    //  If these ship doesn't have a position yet, we provisionally assign one,
    //  and then see what might be interested in trading with it:
    final Tile randTile = world.tileAt(
      Rand.index(world.size),
      Rand.index(world.size)
    );
    if (ship.landArea() == null || ship.dropPoint() == null) {
      ship.setPosition(randTile.x, randTile.y, world);
    }
    final Bringing collects = BringUtils.bestBulkCollectionFor(
      ship, goods, 2, 10, 5
    );
    final Bringing delivers = BringUtils.bestBulkDeliveryFrom(
      ship, goods, 2, 10, 5
    );
    //
    //  We then perform a general siting-pass favouring points close to these
    //  preferred collection points (and commerce-venues in general.)
    final SitingPass spread = new SitingPass(base, null, null) {
      
      protected float ratePlacing(Target point, boolean exact) {
        Target nearest = p.nearestMatch(SERVICE_COMMERCE, point, -1);
        if (nearest == null) nearest = p.nearestMatch(base, point, -1);
        if (nearest == null) nearest = randTile;
        float rating = 1;
        
        if (collects != null) {
          rating *= ZS / (ZS + Spacing.distance(point, collects.origin));
        }
        if (delivers != null) {
          rating *= ZS / (ZS + Spacing.distance(point, delivers.destination));
        }
        if (nearest != null) {
          rating *= ZS / (ZS + Spacing.distance(point, nearest));
          return rating;
        }
        else return -1;
      }
      
      protected boolean canPlaceAt(Tile t, Account reasons) {
        area.xpos(t.x - 0.5f);
        area.ypos(t.y - 0.5f);
        if (checkLandingArea(ship, base.world, area)) {
          return reasons.setSuccess();
        }
        else return reasons.setFailure("No landing possible.");
      }
    };
    spread.performFullPass();
    //
    //  If successful, assign the landing pont in question, otherwise report
    //  the failure.
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




