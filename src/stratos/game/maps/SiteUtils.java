/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.Image;
import stratos.start.PlayLoop;
import stratos.user.*;
import stratos.util.*;



public class SiteUtils implements TileConstants {
  
  public static boolean
    showPockets  = false;
  private static boolean
    verbose      = false,
    cacheVerbose = false;
  
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
    Tile origin, int sizeX, int sizeY
  ) {
    //  TODO:  Use owningTier()?
    if (origin == null || sizeX <= 0 || sizeY <= 0) return false;
    for (Coord c : footprintFor(sizeX, sizeY)) {
      final Tile t = origin.world.tileAt(origin.x + c.x, origin.y + c.y);
      if (t == null || ! t.buildable()) return false;
    }
    return true;
  }
  
  
  public static boolean checkAreaClear(Box2D area, Stage world) {
    final Tile origin = world.tileAt(area.xpos(), area.ypos());
    return checkAreaClear(origin, (int) area.xdim(), (int) area.ydim());
  }
  
  
  public static int minSpacing(Venue a, Venue b) {
    if (a.blueprint.isZoned() || b.blueprint.isZoned()) {
      return Stage.UNIT_GRID_SIZE;
    }
    else return 0;
  }
  
  
  public static float worldOverlap(Target point, Stage world, int claimSize) {
    final Vec3D at = point.position(null);
    Box2D area = new Box2D(at.x, at.y, 0, 0);
    area.expandBy(claimSize);
    
    float fullArea = area.area();
    area.cropBy(world.area());
    return area.area() / fullArea;
  }
  

  public static Venue establishVenue(
    final Venue v, final Target near, boolean intact, Stage world,
    Actor... employed
  ) {
    final SitingPass pass = new SitingPass(v.base(), null, v) {
      protected float ratePlacing(Target point, boolean exact) {
        float rating = 1 / (1 + Spacing.zoneDistance(point, near));
        return rating;
      }
    };
    if (intact) pass.placeState = SitingPass.PLACE_INTACT;
    pass.performFullPass();
    
    if (! v.inWorld()) return null;
    for (Actor a : employed) v.base().setup.addWorkerTo(v, a, intact);
    return v;
  }
  

  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final Stage world,
    Actor... employed
  ) {
    final Tile at = world.tileAt(atX, atY);
    return establishVenue(v, at, intact, world, employed);
  }
  
  
  
  /**  Checks whether or not one owner trumps another-
    */
  public static boolean trumpsSiting(Placeable placed, Placeable other) {
    if (placed.base() == other.base()) return false;
    if (placed.owningTier() >= Owner.TIER_FACILITY) {
      return other.owningTier() < Owner.TIER_FACILITY;
    }
    else return other.owningTier() < placed.owningTier();
  }
  
  
  
  /**  Utility methods for placement of sequential structures-
    */
  public static Object setupSegment(
    Venue fixture, Tile position, Box2D area, Coord points[],
    Object piecesX[], Object piecesY[]
  ) {
    final boolean across = area.xdim() >= area.ydim();
    final Object pieces[] = across ? piecesX : piecesY;
    if (pieces == null || pieces.length < 3) {
      I.complain("PIECES MUST HAVE 3 ENTRIES!");
    }
    
    Coord min = points[0], max = min;
    for (Coord p : points) {
      int c = across ? p.x : p.y;
      if (c > (across ? max.x : max.y)) max = p;
      if (c < (across ? min.x : min.y)) min = p;
    }

    final Box2D print = fixture.footprint();
    final boolean
      atMin = print.contains(min),
      atMax = print.contains(max);

    fixture.setFacing(across ? X_AXIS : Y_AXIS);
    if (atMin) return pieces[0];
    if (atMax) return pieces[2];
    else       return pieces[1];
  }
  
  
  public static Object setupMergingSegment(
    Venue fixture, Tile position, Box2D area, Coord otherPoints[],
    Object piecesX[], Object piecesY[], Object hub,
    Class <? extends Venue> ofType
  ) {
    final boolean report = verbose && PlacingTask.isBeingPlaced(fixture);
    if (report) {
      I.say("\nSetting up segment: "+fixture);
      I.say("  Position:   "+position);
      I.say("  Area:       "+area);
      I.say("  New points: ");
      for (Coord c : otherPoints) I.add(c+" ");
    }
    final Stage world = position.world;
    final int s = fixture.size;
    boolean near[] = new boolean[8], adj, allMatch;
    int numN = 0;
    //
    //  In essence, we compile an adjacency-record by checking in each
    //  direction for either (A) an existing structure of the same type, or (B)
    //  another structure *intended* for placement as part of the same batch.
    for (int dir : T_ADJACENT) {
      final int
        dX = position.x + (T_X[dir] * s),
        dY = position.y + (T_Y[dir] * s);
      
      adj = false;
      allMatch = true;
      
      for (int x = s; x-- > 0;) for (int y = s; y-- > 0;) {
        final Tile n = world.tileAt(dX + x, dY + y);
        final Element under = n == null ? null : n.reserves();
        if (under == null || ! ofType.isAssignableFrom(under.getClass())) {
          allMatch = false;
        }
      }
      if (allMatch) {
        adj = true;
      }
      else for (Coord p : otherPoints) {
        if (dX == p.x && dY == p.y) adj = true;
      }
      
      near[dir] = adj;
      if (report) I.say("  "+dX+"|"+dY+" ("+DIR_NAMES[dir]+"): "+adj);
      if (adj) numN++;
    }
    //
    //  Then we return either start, middle or end points for lines along the x
    //  or y axis, or a hub for junctions, corners and isolates.
    if (numN > 2) return hub;
    if (near[E] & near[W]) return piecesX[1];
    if (near[N] & near[S]) return piecesY[1];
    if (numN == 2) return hub;
    if (near[E]) return piecesX[0];
    if (near[W]) return piecesX[2];
    if (near[N]) return piecesY[0];
    if (near[S]) return piecesY[2];
    return hub;
  }
  
  
  public static Venue[] placeAlongLine(
    Blueprint type, int initX, int initY, int length, boolean across,
    Base base, boolean intact
  ) {
    final Batch <Venue> placed = new Batch();
    final Batch <Coord> coords = new Batch();
    final Box2D area = new Box2D(initX, initY, 0, 0);
    
    for (int l = 0; l < length; l += Stage.UNIT_GRID_SIZE) {
      final int x = initX + (across ? 0 : l), y = initY + (across ? l : 0);
      coords.add(new Coord(x, y));
      area.include(x, y, 0.5f);
    }
    final Coord coordA[] = coords.toArray(Coord.class);
    
    for (Coord c : coords) {
      final Venue v = type.createVenue(base);
      final Tile t = base.world.tileAt(c.x, c.y);
      v.setupWith(t, area, coordA);
      if (! v.canPlace()) continue;
      v.doPlacement(intact);
      placed.add(v);
    }
    
    return placed.toArray(Venue.class);
  }
  
  
  public static Venue[] placeAroundPerimeter(
    Blueprint type, Box2D around, Base base, boolean intact
  ) {
    final Stage world = base.world;
    final int grid = type.size;
    around = new Box2D(around);
    around.expandToUnit(grid);
    intact |= GameSettings.buildFree;
    
    final Tile perim[] = Spacing.perimeter(around, world);
    final int side = perim.length / 4;
    final Batch <Tile> scanned = new Batch <Tile> ();
    final Batch <Venue> placed = new Batch <Venue> ();
    //
    //  We then visit each side of the perimeter and place the appropriate
    //  fixture-type in a line along that edge:
    for (int dir = 4; dir-- > 0;) {
      Batch <Coord> points = new Batch <Coord> ();
      Box2D area = null;
      
      for (int i = side; i-- > 0;) {
        final Tile p = world.tileAtGrid(perim[i + (dir * side)], grid);
        if (p == null || p.flaggedWith() != null) continue;
        if (area == null) area = new Box2D(p.x, p.y, 0, 0);
        area.include(p.x, p.y, 0.5f);
        points.add(new Coord(p.x, p.y));
        p.flagWith(scanned);
        scanned.add(p);
      }
      final Coord pointsA[] = points.toArray(Coord.class);
      for (Coord c : points) {
        final Venue segment = type.createVenue(base);
        segment.setupWith(world.tileAt(c.x, c.y), area, pointsA);
        if (segment.canPlace()) {
          placed.add(segment);
        }
        else if (verbose) {
          final Account reasons = new Account();
          segment.canPlace(reasons);
          I.say("\nCOULD NOT PLACE AT: "+c+", because:");
          for (String r : reasons.failReasons()) I.say("  "+r);
        }
      }
    }
    
    for (Venue segment : placed) segment.doPlacement(intact);
    for (Tile t : scanned) t.flagWith(null);
    //
    //  As a final touch, clear anything in the middle that doesn't belong-
    if (intact) for (Tile t : world.tilesIn(around, true)) {
      t.clearUnlessOwned(true);
    }
    return placed.toArray(Venue.class);
  }
  
  
  
  /**  Other utility methods intended to help ensure free and easy pathing-
    */
  public static boolean pathingOkayAround(
    Target subject, Box2D footprint, int tier, Stage world
  ) {
    final boolean shows = tier >= Owner.TIER_PRIVATE && showPockets;
    final Tile perimeter[] = Spacing.perimeter(footprint, world);
    //
    //  To try and ensure that pathing-routes aren't interrupted, we find all
    //  adjacent 'pockets' of pathable terrain that can be traced from the
    //  perimeter.  (The trace-length is limited to reduce computation-burden,
    //  and also because very long detours are bad for pathing.)
    final int maxTrace = perimeter.length + (Stage.ZONE_SIZE / 2);
    final Batch <Batch <Tile>> pocketsFound = new Batch();
    for (Tile t : perimeter) {
      //  TODO:  DO NOT BORDER ON ANY CLAIMED AREAS EITHER!
      
      final Batch <Tile> pocket = findPocket(t, footprint, maxTrace, tier);
      if (pocket != null) pocketsFound.add(pocket);
    }
    //
    //  You need at least one pocket to ensure access to the item being
    //  introduced, but anything beyond that indicates that a contiguous, empty
    //  area is being partitioned- i.e, might create areas impossible to reach.
    //  So valid placement needs exactly *one* pocket...
    int numRealPockets = 0;
    for (Batch <Tile> pocket : pocketsFound) if (pocket.size() > 0) {
      for (Tile t : pocket) t.flagWith(null);
      numRealPockets++;
      //
      //  (NOTE: Visual overlay creation may be toggled on for debug purposes.)
      if (shows) {
        final TerrainChunk overlay = world.terrain().createOverlay(
          world, pocket.toArray(Tile.class),
          true, Image.TRANSLUCENT_WHITE,
          true
        );
        final int index = pocketsFound.indexOf(pocket);
        final Colour tones[] = Colour.PRIMARIES;
        overlay.colour    = tones[Nums.clamp(index, tones.length)];
        overlay.throwAway = true;
        overlay.readyFor(PlayLoop.rendering());
      }
    }
    if (numRealPockets != 1) return false;
    else return true;
  }
  
  
  private static Batch <Tile> findPocket(
    Tile origin, Box2D area, int maxTrace, int tier
  ) {
    //
    //  Basic sanity-checks and variable-setup comes first-
    if (! singleTileClear(origin, area, tier)) return null;
    if (origin.flaggedWith() != null         ) return null;
    
    final Batch <Tile> pocket = new Batch();
    final Stack <Tile> agenda = new Stack();
    final Batch <Batch <Tile>> pocketsMet = new Batch();
    final Tile tempA[] = new Tile[4], tempB[] = new Tile[8];
    
    agenda.add(origin);
    pocket.add(origin);
    origin.flagWith(pocket);
    //
    //  Then, starting with the first tile encountered (along the area's
    //  perimeter) we try 'hugging the wall' to find the lining of the current
    //  pocket.
    while (agenda.size() > 0 && pocket.size() <= maxTrace) {
      final Tile best = agenda.removeFirst();
      for (Tile t : best.edgeAdjacent(tempA)) {
        if (! singleTileClear(t, area, tier)) continue;
        //
        //  Any other pockets we encounter are flagged for merging (see below.)
        if (t.flaggedWith() != null) {
          final Batch <Tile> other = (Batch <Tile>) t.flaggedWith();
          if (other != pocket) pocketsMet.include(other);
          continue;
        }
        //
        //  Otherwise, we go across all directly-adjacent, non-blocked tiles,
        //  and see if they share a contiguous 'wall' with the last tile.
        boolean borders = false;
        for (Tile n : t.allAdjacent(tempB)) if (n != null) {
          if (singleTileClear(n, area, tier)  ) continue;
          if (Spacing.maxAxisDist(n, best) > 1) continue;
          borders = true; break;
        }
        if (borders) {
          agenda.addLast(t);
          pocket.add(t);
          t.flagWith(pocket);
        }
      }
    }
    //
    //  Having defined the current pocket, we merge with any bordering pockets
    //  as well-
    for (Batch <Tile> other : pocketsMet) {
      for (Tile t : other) { t.flagWith(pocket); pocket.add(t); }
      other.clear();
    }
    return pocket;
  }
  
  
  public static boolean pathingOkayAround(Element e, Stage world) {
    return pathingOkayAround(e, e.area(null), e.owningTier(), world);
  }
  
  
  public static boolean pathingOkayAround(Tile t, int tier) {
    return pathingOkayAround(t, t.area(null), tier, t.world);
  }
  
  
  public static boolean checkAroundClaim(Venue v, Box2D claimed, Stage world) {
    final int tier = v.owningTier();
    for (Tile t : Spacing.perimeter(claimed, world)) if (t != null) {
      if (! singleTileClear(t, claimed, tier)) return false;
    }
    return true;
  }
  
  
  private static boolean singleTileClear(Tile t, Box2D footprint, int tier) {
    if (t == null || ! t.habitat().pathClear) return false;
    if (footprint.contains(t.x, t.y)) return false;
    final int minTier = Nums.min(tier, Owner.TIER_PRIVATE);
    return t.owningTier() < minTier || t.pathType() < Tile.PATH_HINDERS;
  }
  
  
  
  /**  And finally, utility methods for handling entrances and facing-
    */
  public static int[] entranceCoords(int xdim, int ydim, float face) {
    if (face == Venue.FACE_NONE) return new int[] { 0, 0 };
    face = (face + 0.5f) % Venue.NUM_FACES;
    float edgeVal = face % 1;
    int enterX = 1, enterY = -1;
    
    if (face < Venue.FACE_EAST) {
      //  This is the north edge.
      enterX = xdim;
      enterY = (int) (ydim * edgeVal);
    }
    else if (face < Venue.FACE_SOUTH) {
      //  This is the east edge.
      enterX = (int) (xdim * (1 - edgeVal));
      enterY = ydim;
    }
    else if (face < Venue.FACE_WEST) {
      //  This is the south edge.
      enterX = -1;
      enterY = (int) (ydim * (1 - edgeVal));
    }
    else {
      //  This is the west edge.
      enterX = (int) (xdim * edgeVal);
      enterY = -1;
    }
    return new int[] { enterX, enterY };
  }
  
  
  public static int pickBestEntranceFace(Venue v) {
    
    final Tile o = v == null ? null : v.origin();
    int bestFace = Venue.FACE_INIT;
    if (o == null) return bestFace;
    
    final Tile batch[] = new Tile[8];
    float bestRating = -1;
    
    for (int face : Venue.ALL_FACES) {
      final int coords[] = entranceCoords(v.xdim(), v.ydim(), face);
      final Tile t = o.world.tileAt(o.x + coords[0], o.y + coords[1]);
      if (! isViableEntrance(v, t)) continue;
      
      float rating = 1;
      for (Tile n : t.allAdjacent(batch)) {
        if (PavingMap.pavingReserved(n, false)) rating++;
      }
      if (rating > bestRating) { bestRating = rating; bestFace = face; }
    }
    return bestFace;
  }
  
  
  public static boolean isViableEntrance(Venue v, Tile e) {
    //  TODO:  Unify this with singleTileClear()?
    if (e == null || ! e.habitat().pathClear) return false;
    if (e.reserves() == null) return true;
    
    final int maxTier = Nums.min(v.owningTier(), Owner.TIER_PRIVATE);
    final Element under = e.reserves();
    
    return under == null || (
      under.owningTier() < maxTier ||
      under.pathType  () <= Tile.PATH_CLEAR
    );
  }
}




