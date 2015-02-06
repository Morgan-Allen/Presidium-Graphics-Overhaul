/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.actors.Backgrounds;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



//
//  TODO:  Try merging this with the TileSpread class, or the PlacementGrid
//  class.  Placement2 can probably be got rid off completely.

public class Placement implements TileConstants {
  
  
  private static boolean verbose = false, cacheVerbose = false;
  
  final private static Coord footprintCache[][][] = new Coord[100][100][];
  //
  //  NOTE:  This method is intended to generate a sequence of coordinates to
  //  check that should eliminate unfit placement-sites for buildings faster
  //  than a naive sequential iteration.  It splits the x and y axes into
  //  successively finer quadrants, closing a 'net' on any potential obstacles
  //  within the area:
  //
  //  PASS 1:           PASS 2:           PASS 3:          ETC.
  //    *-----------*     *-----*-----*     *--*--*--*--*
  //    |           |     |     |     |     *--*--*--*--*
  //    |           |     *-----*-----*     *--*--*--*--*
  //    |           |     |     |     |     *--*--*--*--*
  //    *-----------*     *-----*-----*     *--*--*--*--*
  //
  //
  private static Coord[] footprintFor(int sizeX, int sizeY) {
    //
    //  Return the cached version if available, initialise otherwise-
    Coord offsets[] = footprintCache[sizeX][sizeY];
    if (offsets != null) return offsets;
    else offsets = footprintCache[sizeX][sizeY] = new Coord[sizeX * sizeY];
    //
    //  Find the initial 'step' size for the grid.
    int stepX = 1, stepY = 1, i = 0;
    while (stepX <= sizeX * 2) stepX *= 2;
    while (stepY <= sizeY * 2) stepY *= 2;
    final int maxX = sizeX - 1, maxY = sizeY - 1;
    final boolean mask[][] = new boolean[sizeX][sizeY];
    //
    //  Shrink the grid with every step, skipping over any previously-used
    //  coordinates, and return the sequence of coordinates compiled-
    while (stepX > 1 || stepY > 1) {
      if (stepX > 1) stepX /= 2;
      if (stepY > 1) stepY /= 2;
      for (int x = 0;;) {
        for (int y = 0;;) {
          if (! mask[x][y]) {
            if (cacheVerbose) I.say("X/Y: "+x+"/"+y);
            mask[x][y] = true;
            offsets[i++] = new Coord(x, y);
          }
          if (y == maxY) break;
          y += stepY;
          if (y >= sizeY) y = maxY;
        }
        if (x == maxX) break;
        x += stepX;
        if (x >= sizeX) x = maxX;
      }
    }
    if (cacheVerbose) I.say("SIZE X/Y: "+sizeX+"/"+sizeY);
    if (cacheVerbose) I.say("OFFSETS GENERATED: "+i);
    return offsets;
  }
  
  
  public static boolean checkAreaClear(
    Tile origin, int sizeX, int sizeY//, int owningPriority
  ) {
    if (origin == null || sizeX <= 0 || sizeY <= 0) return false;
    for (Coord c : footprintFor(sizeX, sizeY)) {
      final Tile t = origin.world.tileAt(origin.x + c.x, origin.y + c.y);
      if (t == null || t.reserved()) return false;
    }
    return true;
  }
  
  
  //
  //  NOTE:  This method assumes that the fixtures in question will occupy a
  //  contiguous 'strip' or bloc for placement purposes.
  public static boolean checkPlacement(
    Structure.Basis fixtures[], Stage world
  ) {
    Box2D limits = null;
    for (Structure.Basis f : fixtures) {
      if (limits == null) limits = new Box2D(f.footprint());
      else limits.include(f.footprint());
    }
    if (! checkAreaClear(
      world.tileAt(limits.xpos() + 0.5f, limits.ypos() + 0.5f),
      (int) limits.xdim(),
      (int) limits.ydim()
    )) return false;
    
    for (Structure.Basis f : fixtures) if (! f.canPlace()) return false;
    return true;
  }
  
  
  public static boolean findClearanceFor(
    final Venue v, final Target near, final Stage world
  ) {
    final float maxDist = Stage.SECTOR_SIZE / 2;
    Tile init = world.tileAt(near);
    init = Spacing.nearestOpenTile(init, init);
    if (init == null) return false;
    
    final TileSpread search = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, near) > maxDist) return false;
        return ! t.blocked();
      }
      protected boolean canPlaceAt(Tile t) {
        v.setPosition(t.x, t.y, world);
        return checkPlacement(v.structure.asGroup(), world);
      }
    };
    search.doSearch();
    return search.success();
  }
  
  
  public static Venue establishVenue(
    final Venue v, final Target near, boolean intact, Stage world
  ) {
    if (! findClearanceFor(v, near, world)) return null;
    
    for (Structure.Basis i : v.structure.asGroup()) {
      i.doPlacement();
      if (intact || GameSettings.buildFree) {
        i.structure().setState(Structure.STATE_INTACT, 1.0f);
      }
      else {
        i.structure().setState(Structure.STATE_INSTALL, 0.0f);
      }
      ((Element) i).setAsEstablished(true);
    }
    return v;
  }
  
  
  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final Stage world,
    Actor... employed
  ) {
    if (establishVenue(v, world.tileAt(atX, atY), intact, world) == null) {
      return null;
    }
    for (Actor a : employed) {
      if (! a.inWorld()) {
        a.assignBase(v.base());
        a.enterWorldAt(v, world);
        a.goAboard(v, world);
      }
      a.mind.setWork(v);
      if (v.crowdRating(a, Backgrounds.AS_RESIDENT) < 1) {
        a.mind.setHome(v);
      }
    }
    if (GameSettings.hireFree) v.base().setup.fillVacancies(v, intact);
    return v;
  }
  
  
  
  /**  Other utility methods intended to help ensure free and easy pathing-
    */
  private static Box2D tA = new Box2D();

  
  public static int[] entranceCoords(int xdim, int ydim, float face) {
    if (face == Venue.ENTRANCE_NONE) return new int[] { 0, 0 };
    face = (face + 0.5f) % Venue.NUM_SIDES;
    float edgeVal = face % 1;
    
    int enterX = 1, enterY = -1;
    if (face < Venue.ENTRANCE_EAST) {
      //  This is the north edge.
      enterX = xdim;
      enterY = (int) (ydim * edgeVal);
    }
    else if (face < Venue.ENTRANCE_SOUTH) {
      //  This is the east edge.
      enterX = (int) (ydim * (1 - edgeVal));
      enterY = xdim;
    }
    else if (face < Venue.ENTRANCE_WEST) {
      //  This is the south edge.
      enterX = -1;
      enterY = (int) (ydim * (1 - edgeVal));
    }
    else {
      //  This is the west edge.
      enterX = (int) (ydim * edgeVal);
      enterY = -1;
    }
    return new int[] { enterX, enterY };
  }
  
  
  //  TODO:  Return a set of venues/fixtures/elements that conflict with the
  //  one being placed instead!
  
  //
  //  This method checks whether the placement of the given element in this
  //  location would create secondary 'gaps' along it's perimeter that might
  //  lead to the existence of inaccessible 'pockets' of terrain- that would
  //  cause pathing problems.
  public static boolean perimeterFits(Element element) {
    final Stage world = element.origin().world;
    final Box2D area = element.area(tA);
    final Tile perim[] = Spacing.perimeter(area, world);
    final int tier = element.owningTier();
    //
    //  Here, we check the first perimeter.  First, determine where the first
    //  taken (reserved) tile after a contiguous gap is-
    boolean inClearSpace = false;
    int index = perim.length - 1;
    while (index >= 0) {
      final Tile t = perim[index];
      if (t == null || t.owningTier() >= tier) {
        if (inClearSpace) break;
      }
      else { inClearSpace = true; }
      index--;
    }
    //
    //  Then, starting from that point, and scanning in the other direction,
    //  ensure there's no more than a single contiguous clear space-
    final int
      firstTaken = (index + perim.length) % perim.length,
      firstClear = (firstTaken + 1) % perim.length;
    inClearSpace = false;
    int numSpaces = 0;
    for (index = firstClear; index != firstTaken;) {
      final Tile t = perim[index];
      if (t == null || t.owningTier() >= tier) {
        inClearSpace = false;
      }
      else if (! inClearSpace) { inClearSpace = true; numSpaces++; }
      index = (index + 1) % perim.length;
    }
    if (numSpaces > 1) return false;
    //
    //  Finally, try to prevent clustering among structures that belong to
    //  different portions of the map:
    area.expandBy(1);
    final Tile outerPerim[] = Spacing.perimeter(area, world);
    if (! checkClustering(world, element, outerPerim)) return false;
    //
    //  If you run the gauntlet, return true-
    return true;
  }
  
  
  //  TODO:  Reserve this strictly for venues?  (Better yet, just use the
  //  claims-system if possible.)
  
  public static boolean checkClustering(
    Stage world, Element f, Tile outerPerim[]
  ) {
    final boolean report = false;
    final int tier = f.owningTier();
    
    final Tile at = f.origin();
    final StageSection belongs = world.sections.sectionAt(at.x, at.y);
    if (report) {
      I.say("\nChecking for clustering by "+f+" at "+at);
      I.say("  Belongs to section: "+belongs);
    }
    
    for (Tile t : outerPerim) {
      if (t == null || t.owningTier() < tier) continue;
      
      //  TODO:  You can ignore any structures whose claim you could normally
      //  override!
      
      final Tile o = t.onTop().origin();
      final StageSection s = world.sections.sectionAt(o.x, o.y);
      if (report) I.say("  "+t.onTop()+" belongs to "+s);
      if (s != belongs) return false;
    }
    return true;
  }
}














