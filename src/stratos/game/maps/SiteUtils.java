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
  
  
  //
  //  NOTE:  This method assumes that the fixtures in question will occupy a
  //  contiguous 'strip' or bloc for placement purposes.
  public static boolean checkPlacement(
    Placeable fixtures[], Stage world
  ) {
    Box2D limits = null;
    for (Placeable f : fixtures) {
      if (limits == null) limits = new Box2D(f.footprint());
      else limits.include(f.footprint());
    }
    if (! checkAreaClear(
      world.tileAt(limits.xpos() + 0.5f, limits.ypos() + 0.5f),
      (int) limits.xdim(),
      (int) limits.ydim()
    )) return false;
    
    for (Placeable f : fixtures) {
      if (! f.canPlace(Account.NONE)) return false;
    }
    return true;
  }
  
  
  public static boolean findClearanceFor(
    final Venue v, final Target near, final Stage world
  ) {
    //  TODO:  USE A PROPER SITING-CLASS FOR THIS
    
    final int maxDist = Stage.ZONE_SIZE / 2;
    final Tile init = world.tileAt(near);
    if (init == null) return false;
    
    //  For now, I'm just going to use the perimeter-methods to 'spiral out'
    //  from the target in question and try tiles until you find a placement-
    //  location.  (I ran into problems with tiles being simultaneously flagged
    //  by the TileSpread and the pathingOkayAround method before.)
    //  TODO:  Like it says, use a proper Siting class implementation
    
    v.setPosition(init.x, init.y, world);
    if (checkPlacement(v.structure.asGroup(), world)) return true;
    
    Box2D area = new Box2D();
    for (int m = 0; m < maxDist; m++) {
      init.area(area).expandBy(m);
      for (Tile t : Spacing.perimeter(area, world)) {
        v.setPosition(t.x, t.y, world);
        if (checkPlacement(v.structure.asGroup(), world)) return true;
      }
    }
    return false;
  }
  
  
  public static Venue establishVenue(
    final Venue v, final Target near, boolean intact, Stage world,
    Actor... employed
  ) {
    if (! findClearanceFor(v, near, world)) return null;
    if (! v.setupWith(v.origin(), null)) return null;
    
    for (Placeable i : v.structure.asGroup()) {
      i.doPlacement(intact);
      ((Element) i).setAsEstablished(true);
    }
    return v;
  }
  
  
  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final Stage world,
    Actor... employed
  ) {
    final Tile near = world.tileAt(atX, atY);
    if (establishVenue(v, near, intact, world, employed) == null) {
      return null;
    }
    if (! Visit.empty(employed)) {
      v.base().setup.fillVacancies(v, intact, employed);
    }
    if (GameSettings.hireFree) v.base().setup.fillVacancies(v, intact);
    return v;
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
    
    //  TODO:  Try to get rid of these 'hS'/half-size offsets, both here and in
    //         the PlacingTask class?  It's a tad confusing.
    
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
      final Batch <Venue> group = new Batch <Venue> ();
      
      for (Coord c : points) {
        final Venue s = type.createVenue(base);
        s.setPosition(c.x, c.y, world);
        s.setupWith(s.origin(), area, pointsA);
        if (s.canPlace()) group.add(s);
      }
      
      final Venue groupA[] = group.toArray(Venue.class);
      
      for (Venue s : group) {
        s.doPlacement(intact);
        s.structure.assignGroup(groupA);
        placed.add(s);
      }
    }
    
    for (Tile t : scanned) t.flagWith(null);
    return placed.toArray(Venue.class);
  }
  
  
  
  /**  Other utility methods intended to help ensure free and easy pathing-
    */
  public static boolean pathingOkayAround(
    Target subject, Box2D footprint, int tier, int margin, Stage world
  ) {
    final boolean shows = tier >= Owner.TIER_PRIVATE && showPockets;
    final Tile perimeter[] = Spacing.perimeter(footprint, world);
    //
    //  To try and ensure that pathing-routes aren't interrupted, we find all
    //  adjacent 'pockets' of pathable terrain that can be traced from the
    //  perimeter.  (The trace- length is limited to reduce computation-burden,
    //  and also because very long detours are bad for pathing.)
    final int maxTrace = perimeter.length + (Stage.ZONE_SIZE * margin) / 4;
    final Batch <Batch <Tile>> pocketsFound = new Batch();
    for (Tile t : perimeter) {
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
      if (shows && margin == 1) {
        final TerrainChunk overlay = world.terrain().createOverlay(
          world, pocket.toArray(Tile.class), true, Image.TRANSLUCENT_WHITE
        );
        final int index = pocketsFound.indexOf(pocket);
        final Colour tones[] = Colour.PRIMARIES;
        overlay.colour    = tones[Nums.clamp(index, tones.length)];
        overlay.throwAway = true;
        overlay.readyFor(PlayLoop.rendering());
      }
    }
    if (numRealPockets != 1) return false;
    //
    //  If an extra safety-margin is required, we expand the area and check
    //  again at that size.  Otherwise return.
    if (margin <= 1) return true;
    final Box2D expanded = new Box2D(footprint).expandBy(1);
    return pathingOkayAround(subject, expanded, tier, margin - 1, world);
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
  
  
  private static boolean hasBlockingOwner(Tile t, Box2D area, int tier) {
    if (singleTileClear(t, area, tier)) return false;
    return t != null && t.above() != null;
  }
  
  
  private static boolean singleTileClear(Tile t, Box2D footprint, int tier) {
    if (t == null || ! t.habitat().pathClear) return false;
    if (footprint.contains(t.x, t.y)) return false;
    return t.owningTier() < tier || t.pathType() < Tile.PATH_HINDERS;
  }
  
  
  public static boolean pathingOkayAround(Element e, Stage world) {
    return pathingOkayAround(e, e.area(null), e.owningTier(), 2, world);
  }
  
  
  public static boolean pathingOkayAround(Tile t, int tier) {
    return pathingOkayAround(t, t.area(null), tier, 2, t.world);
  }
  
  
  public static int[] entranceCoords(int xdim, int ydim, float face) {
    if (face == Venue.FACING_NONE) return new int[] { 0, 0 };
    face = (face + 0.5f) % Venue.NUM_FACES;
    float edgeVal = face % 1;
    
    int enterX = 1, enterY = -1;
    if (face < Venue.FACING_EAST) {
      //  This is the north edge.
      enterX = xdim;
      enterY = (int) (ydim * edgeVal);
    }
    else if (face < Venue.FACING_SOUTH) {
      //  This is the east edge.
      enterX = (int) (ydim * (1 - edgeVal));
      enterY = xdim;
    }
    else if (face < Venue.FACING_WEST) {
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



  
  /*
  public static Tile findClearSpot(
    final Target near, final Stage world, final int margin
  ) {
    final Tile init = world.tileAt(near);
    final float maxDist = near.radius() + 0.5f + (Stage.ZONE_SIZE / 2);
    
    final TileSpread search = new TileSpread(init) {
      
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, init) > maxDist) return false;
        return t.onTop() == near || ! t.blocked();
      }
      
      protected boolean canPlaceAt(Tile t) {
        final Tile c = world.tileAt(t.x - margin, t.y - margin);
        return checkAreaClear(c, margin * 2, margin * 2);
      }
    };
    search.doSearch();
    if (search.success()) return search.bestFound();
    else return null;
  }
  //*/
