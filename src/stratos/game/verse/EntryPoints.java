/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
//import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;





//  TODO:  This doesn't actually do much of anything ATM, aside from the
//         static utility methods...


public class EntryPoints {
  
  
  private static boolean
    siteVerbose = false;
  
  final Stage world;
  
  
  
  public EntryPoints(Stage world) {
    this.world = world;
  }
  
  
  public void loadState(Session s) throws Exception {
    return;
  }
  
  
  public void saveState(Session s) throws Exception {
    return;
  }
  

  
  /**  Interface and utlity-methods for entry/exit directly from ground-level:
    */
  //  TODO:  I'm not sure I can have every tile along the border of the world
  //         implement the Portal interface.  Maybe Retreat should be using a
  //         different mechanism for bolt-holes...?
  
  public static interface Portal extends Boarding {
    
    final static int
      TYPE_BORDER    = 0,
      TYPE_BOLT_HOLE = 1,
      TYPE_WAY_GATE  = 2;
    
    int exitType();
    Sector leadsTo();
    boolean allowsStageExit(Mobile m);
  }
  
  
  public static Tile findBorderPoint(
    Boarding inWorld, Sector offWorld, Base client
  ) {
    final Stage  world = inWorld.world();
    final Sector local = world.localSector();
    if (! local.borders(offWorld)) return null;
    
    //
    //  TODO:  You're going to need some idea of sector coordinates to do this
    //  properly...
    
    final Box2D bordering = new Box2D().setTo(world.area()).expandBy(-1);
    final Pick <Tile> pick = new Pick();
    
    for (Tile t : Spacing.perimeter(bordering, world)) {
      if (! world.pathingMap.hasPathBetween(t, inWorld, client, false)) {
        continue;
      }
      pick.compare(t, 0 - Spacing.distance(t, inWorld));
    }
    return pick.result();
  }
  
  
  
  /**  Interfaces and utility methods for handling dock-points:
    */
  public static interface Docking extends Property {
    
    boolean allowsDocking(Vehicle docks);
    boolean isDocked(Vehicle docks);
    Series <Vehicle> docked();
    Vec3D dockLocation(Vehicle docks);
    void setAsDocked(Vehicle docks, boolean is);
  }
  
  
  private static float rateDocking(Docking dock, Vehicle ship) {
    if ((! dock.inWorld()) || ! dock.structure().intact()) return -1;
    if (! dock.allowsDocking(ship)) return -1;
    //
    //  TODO:  SEE IF YOU CAN USE THE BRING-UTILS CLASS FOR THIS (see below...)
    
    final Inventory stocks = dock.inventory();
    float rating = 1;
    for (Traded good : Economy.ALL_MATERIALS) {
      rating += Nums.abs(stocks.relativeShortage(good, true));
    }
    rating *= 2f / ALL_MATERIALS.length;
    return rating;
  }
  
  
  private static Docking findDocking(Vehicle ship, Stage world) {
    final Pick <Docking> pick = new Pick(null, 0);
    for (Object o : world.presences.matchesNear(SERVICE_DOCKING, ship, -1)) {
      final Docking strip = (Docking) o;
      pick.compare(strip, rateDocking(strip, ship));
    }
    return pick.result();
  }
  
  
  public static Vehicle findTransport(
    Boarding inWorld, Sector offWorld, Base client
  ) {
    final Stage world = inWorld.world();
    final Pick <Vehicle> pick = new Pick();
    
    for (Object o : world.presences.matchesNear(SERVICE_DOCKING, inWorld, -1)) {
      final Docking strip = (Docking) o;
      if (! world.pathingMap.hasPathBetween(strip, inWorld, client, false)) {
        continue;
      }
      for (Vehicle v : strip.docked()) {
        final float dist = Spacing.distance(inWorld, v);
        final Journey j = v.journey();
        if (j == null) pick.compare(v, 0 - dist);
      }
    }
    return pick.result();
  }
  
  
  
  /**  Finally, utility methods for finding a suitable landing point-
    */
  public static boolean checkLandingArea(
    Vehicle ship, Stage world, Box2D area
  ) {
    final Boarding dropPoint = ship.dropPoint();
    
    if (dropPoint instanceof Docking) {
      final Docking strip = (Docking) dropPoint;
      if (rateDocking(strip, ship) <= 0) return false;
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
  
  
  public static boolean findLandingSite(Vehicle trans, boolean useCache) {
    
    //  TODO:  YOU'LL NEED TO FIND AN ALTERNATE ENTRY-POINT FOR GROUND-BASED
    //  VEHICLES- along the borders of the map.
    
    //
    //  Basic variable setup and sanity checks-
    final Base  base  = trans.base();
    final Stage world = base.world ;
    final boolean report = siteVerbose && BaseUI.currentPlayed() == base;
    if (trans.landed()) return true;
    //
    //  If your current landing site is still valid, then keep it.
    if (useCache && checkLandingArea(trans, world, trans.landArea())) {
      if (report) {
        I.say("\nCurrent landing site valid for "+trans);
        I.say("  Point: "+trans.dropPoint());
        I.say("  Area:  "+trans.landArea ());
      }
      return true;
    }
    
    final Docking strip = findDocking(trans, world);
    if (strip != null) {
      final Vec3D aimPos = strip.dockLocation(trans);
      trans.assignLandPoint(aimPos, strip);
      if (report) I.say("Landing at airfield: "+strip);
      return true;
    }
    //
    //  Otherwise, search for a suitable landing site on the bare ground near
    //  likely customers:
    return findLandingArea(trans, base);
  }

  
  private static boolean findLandingArea(
    final Vehicle ship, final Base base
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
      ship, goods, 2, 10, 5, true
    );
    final Bringing delivers = BringUtils.bestBulkDeliveryFrom(
      ship, goods, 2, 10, 5, true
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




