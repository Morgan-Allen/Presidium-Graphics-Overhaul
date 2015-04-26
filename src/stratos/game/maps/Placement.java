/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.civic.ShieldWall;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.Backgrounds;
import stratos.graphics.widgets.KeyInput;
import stratos.user.PlacingTask;
import stratos.util.*;



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
    
    for (Structure.Basis f : fixtures) {
      if (! f.canPlace()) return false;
    }
    return true;
  }
  
  
  public static Tile findClearSpot(
    final Target near, final Stage world, final int margin
  ) {
    final Tile init = world.tileAt(near);
    final float maxDist = near.radius() + 0.5f + (Stage.SECTOR_SIZE / 2);
    
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
    final int s = fixture.size, hS = s / 2;
    boolean near[] = new boolean[8], adj;
    int numN = 0;
    //
    //  In essence, we compile an adjacency-record by checking in each
    //  direction for either (A) an existing structure of the same type, or (B)
    //  another structure *intended* for placement as part of the same batch.
    for (int t : T_ADJACENT) {
      final Tile n = world.tileAt(
        position.x + (T_X[t] * s),
        position.y + (T_Y[t] * s)
      );
      if (n == null) continue; else adj = false;
      final Element under = n.onTop();
      if (
        under != null && under.origin() == n &&
        ofType.isAssignableFrom(under.getClass())
      ) {
        adj = true;
      }
      else for (Coord p : otherPoints) {
        if (n.x + hS == p.x && n.y + hS == p.y) adj = true;
      }
      near[t] = adj;
      if (report) I.say("  "+n+" ("+DIR_NAMES[t]+"): "+adj);
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
    final int grid = type.size, hS = grid / 2;
    if (hS > 0) {
      around = new Box2D(around);
      around.incX(0 - hS);
      around.incY(0 - hS);
      around.incWide(hS);
      around.incHigh(hS);
    }
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
        points.add(new Coord(p.x + hS, p.y + hS));
        p.flagWith(scanned);
        scanned.add(p);
      }
      
      final Coord pointsA[] = points.toArray(Coord.class);
      final Batch <Venue> group = new Batch <Venue> ();
      
      for (Coord c : points) {
        final Venue s = type.createVenue(base);
        s.setPosition(c.x - hS, c.y - hS, world);
        s.setupWith(s.origin(), area, pointsA);
        if (s.canPlace()) group.add(s);
      }
      
      final Venue groupA[] = group.toArray(Venue.class);
      
      for (Venue s : group) {
        s.doPlacement();
        if (intact) s.structure.setState(Structure.STATE_INTACT, 1);
        s.structure.assignGroup(groupA);
        placed.add(s);
      }
    }
    
    for (Tile t : scanned) t.flagWith(null);
    return placed.toArray(Venue.class);
  }
  
  
  
  /**  Other utility methods intended to help ensure free and easy pathing-
    */
  private static Box2D tA = new Box2D();
  
  
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
  
  
  public static boolean perimeterFits(Element e, Stage world) {
    boolean report = verbose && e.owningTier() > Owner.TIER_PRIVATE;
    if (report) {
      I.say("\nCHECKING PERIMETER FOR "+I.tagHash(e));
    }
    return perimeterFits(e, e.area(tA), e.owningTier(), 2, world);
  }
  
  
  public static boolean perimeterFits(Tile t, int tier) {
    return perimeterFits(t, t.area(tA), tier, 2, t.world);
  }
  
  
  //
  //  This method checks whether the placement of the given element in this
  //  location would create secondary 'gaps' along it's perimeter that might
  //  lead to the existence of inaccessible 'pockets' of terrain- that would
  //  cause pathing problems.
  public static boolean perimeterFits(
    Target subject, Box2D footprint, int tier, int spaceNeed, Stage world
  ) {
    final boolean report = verbose && PlacingTask.isBeingPlaced(subject);
    //
    //  We check recursively for pockets at a finer resolution:
    if (spaceNeed > 1) {
      final boolean fits;
      fits = perimeterFits(subject, footprint, tier, spaceNeed -1, world);
      if (! fits) return false;
    }
    //
    //  If more than one tile of space is required, pull the footprint down by
    //  the appropriate amount (so that the clearance-check has space to
    //  operate.)
    if (spaceNeed > 1) {
      final int shift = spaceNeed - 1;
      footprint = new Box2D(footprint);
      footprint.incX   (-shift);
      footprint.incY   (-shift);
      footprint.incHigh( shift);
      footprint.incWide( shift);
    }
    //
    //  In essence, we scan along the perimeter and note how often the tiles
    //  switch between being blocked and unblocked.
    final Tile perim[] = Spacing.perimeter(footprint, world);
    final boolean minor = tier < Owner.TIER_PRIVATE;
    int inBlock = -1, numGaps = 0;
    if (report) {
      I.say("  Checking tiles along perim, area: "+footprint);
      I.say("    ");
    }
    for (Tile t : perim) {
      if (minor && t != null && t.owningTier() >= Owner.TIER_PRIVATE) {
        //
        //  We also forbid direct placement of natural items next to artificial
        //  items-
        return false;
      }
      int block = checkClear(t, spaceNeed, tier, report) ? 0 : 1;
      if (report) I.add(" "+block);
      if (block != inBlock) { inBlock = block; if (block == 0) numGaps++; }
    }
    //
    //  There's a potential fail case if a gap lies across the first and last
    //  tiles checked, so we decrement the count in that case-
    final boolean tailGap =
      checkClear(perim[0               ], spaceNeed, tier, report) &&
      checkClear(perim[perim.length - 1], spaceNeed, tier, report);
    if (tailGap) numGaps--;
    
    if (report) {
      I.say("  Footprint was: "+footprint);
      I.say("  Gap count is: "+numGaps+" (spacing "+spaceNeed+")");
    }
    return numGaps < 2;
  }
  
  
  private static boolean checkClear(
    Tile t, int space, int tier, boolean report
  ) {
    if (t == null) return false;
    final int maxTier = Nums.min(tier, Owner.TIER_PRIVATE);
    if (space == 1) {
      return singleTileClear(t, maxTier);
    }
    else for (int x = space; x-- > 0;) for (int y = space; y-- > 0;) {
      final Tile u = t.world.tileAt(x + t.x, y + t.y);
      if (u == null || ! singleTileClear(u, maxTier)) return false;
    }
    return true;
  }
  
  
  private static boolean singleTileClear(Tile t, int tier) {
    if (! t.habitat().pathClear) return false;
    return t.owningTier() < tier || t.pathType() < Tile.PATH_HINDERS;
  }
  
  
  //  TODO:  You need a public boolean method to establish 'dominance' between
  //  two different tiers for placement purposes.
  
  
  public static boolean isViableEntrance(Venue v, Tile e) {
    //  TODO:  Unify this with singleTileClear()?
    if (e == null || ! e.habitat().pathClear) return false;
    if (e.onTop() == null) return true;
    final int maxTier = Nums.min(v.owningTier(), Owner.TIER_PRIVATE);
    final Element under = e.onTop();
    return under == null || (
      under.owningTier() < maxTier ||
      under.pathType  () <= Tile.PATH_CLEAR
    );
  }
}








