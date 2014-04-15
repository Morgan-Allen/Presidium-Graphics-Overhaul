

package stratos.game.building ;
import org.apache.commons.math3.util.FastMath;

import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.util.*;



//
//  TODO:  Try merging this with the TileSpread class.


public class Placement implements TileConstants {
  
  
  private static boolean verbose = false, cacheVerbose = false ;
  
  final private static Coord footprintCache[][][] = new Coord[100][100][] ;
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
    Coord offsets[] = footprintCache[sizeX][sizeY] ;
    if (offsets != null) return offsets ;
    else offsets = footprintCache[sizeX][sizeY] = new Coord[sizeX * sizeY] ;
    //
    //  Find the initial 'step' size for the grid.
    int stepX = 1, stepY = 1, i = 0 ;
    while (stepX <= sizeX * 2) stepX *= 2 ;
    while (stepY <= sizeY * 2) stepY *= 2 ;
    final int maxX = sizeX - 1, maxY = sizeY - 1 ;
    final boolean mask[][] = new boolean[sizeX][sizeY] ;
    //
    //  Shrink the grid with every step, skipping over any previously-used
    //  coordinates, and return the sequence of coordinates compiled-
    while (stepX > 1 || stepY > 1) {
      if (stepX > 1) stepX /= 2 ;
      if (stepY > 1) stepY /= 2 ;
      for (int x = 0 ;;) {
        for (int y = 0 ;;) {
          if (! mask[x][y]) {
            if (cacheVerbose) I.say("X/Y: "+x+"/"+y) ;
            mask[x][y] = true ;
            offsets[i++] = new Coord(x, y) ;
          }
          if (y == maxY) break ;
          y += stepY ;
          if (y >= sizeY) y = maxY ;
        }
        if (x == maxX) break ;
        x += stepX ;
        if (x >= sizeX) x = maxX ;
      }
    }
    if (cacheVerbose) I.say("SIZE X/Y: "+sizeX+"/"+sizeY) ;
    if (cacheVerbose) I.say("OFFSETS GENERATED: "+i) ;
    return offsets ;
  }
  
  
  public static boolean checkAreaClear(
    Tile origin, int sizeX, int sizeY, int owningPriority
  ) {
    for (Coord c : footprintFor(sizeX, sizeY)) {
      final Tile t = origin.world.tileAt(origin.x + c.x, origin.y + c.y) ;
      if (t == null || t.owningType() >= owningPriority) return false ;
    }
    return true ;
  }
  
  
  //
  //  NOTE:  This method assumes that the fixtures in question will occupy a
  //  contiguous 'strip' or bloc for placement purposes.
  public static boolean checkPlacement(
    Fixture fixtures[], World world
  ) {
    Box2D limits = null ;
    for (Fixture f : fixtures) {
      if (limits == null) f.area(limits = new Box2D()) ;
      else limits.include(f.area()) ;
    }
    if (! checkAreaClear(
      world.tileAt(limits.xpos() + 0.5f, limits.ypos() + 0.5f),
      (int) limits.xdim(),
      (int) limits.ydim(),
      fixtures[0].owningType()
    )) return false ;
    
    for (Fixture f : fixtures) if (! f.canPlace()) return false ;
    return true ;
  }
  
  
  public static boolean findClearanceFor(
    final Venue v, final Target near, final World world
  ) {
    final float maxDist = World.SECTOR_SIZE / 2 ;
    Tile init = world.tileAt(near) ;
    init = Spacing.nearestOpenTile(init, init) ;
    if (init == null) return false ;
    
    final TileSpread search = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, near) > maxDist) return false ;
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        v.setPosition(t.x, t.y, world) ;
        if (! checkAreaClear(t, v.size, v.size, v.owningType())) return false ;
        return v.canPlace() ;
      }
    } ;
    search.doSearch() ;
    return search.success() ;
  }
  
  
  public static Venue establishVenue(
    final Venue v, final Target near, boolean intact, World world
  ) {
    if (! findClearanceFor(v, near, world)) return null ;
    v.placeFromOrigin() ;
    if (intact || GameSettings.buildFree) {
      v.structure.setState(Structure.STATE_INTACT, 1.0f) ;
      ///v.onCompletion() ;
    }
    else {
      v.structure.setState(Structure.STATE_INSTALL, 0.0f) ;
    }
    v.setAsEstablished(true) ;
    return v ;
  }
  
  
  public static Venue[] establishVenueStrip(
    final Venue strip[], final Target near, boolean intact, final World world
  ) {
    final Venue v = strip[0];
    final int deep = v.size;
    final Tile init = world.tileAt(near);
    final int maxDist = World.SECTOR_SIZE / 2;
    
    final TileSpread search = new TileSpread(init) {
      int minX, minY;
      
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, near) > maxDist) return false ;
        if (t.owner() == near) return true;
        return ! t.blocked() ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        if (verbose) I.say("  Trying "+t);
        
        dirLoop: for (int dir : N_ADJACENT) {
          int xdim = N_X[dir] * deep, ydim = N_Y[dir] * deep;
          if (xdim == 0) xdim = ydim * strip.length;
          if (ydim == 0) ydim = xdim * strip.length;
          if (xdim >= 0) minX = t.x;
          else { xdim *= -1; minX = t.x - xdim; }
          if (ydim >= 0) minY = t.y;
          else { ydim *= -1; minY = t.y - ydim; }
          
          final Tile c = world.tileAt(minX, minY);
          if (! checkAreaClear(c, xdim, ydim, v.owningType())) continue;
          
          if (verbose) I.say("Area clear: "+c.x+" "+c.y+" "+xdim+" "+ydim);
          int i = 0; for (Venue s : strip) {
            s.setPosition(
              minX + (i * deep * FastMath.abs(N_Y[dir])),
              minY + (i * deep * FastMath.abs(N_X[dir])),
              world
            );
            if (verbose) I.say("Checking at: "+s.origin());
            if (! s.canPlace()) {
              if (verbose) I.say("Blocked!");
              continue dirLoop;
            }
            else i++;
          }
          return true;
        }
        return false;
      }
    } ;
    search.verbose = verbose;
    search.doSearch();
    
    if (search.success()) {
      for (Venue s : strip) {
        s.placeFromOrigin() ;
        if (intact || GameSettings.buildFree) {
          s.structure.setState(Structure.STATE_INTACT, 1.0f) ;
        }
        else s.structure.setState(Structure.STATE_INSTALL, 0.0f) ;
        s.setAsEstablished(true) ;
      }
      return strip;
    }
    if (verbose) I.say("Failed to establish venue strip for: "+v);
    
    return null;
  }
  
  
  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final World world,
    Actor... employed
  ) {
    if (establishVenue(v, world.tileAt(atX, atY), intact, world) == null) {
      return null ;
    }
    for (Actor a : employed) {
      if (! a.inWorld()) {
        a.assignBase(v.base()) ;
        a.enterWorldAt(v, world) ;
        a.goAboard(v, world) ;
      }
      a.mind.setWork(v) ;
    }
    if (GameSettings.hireFree) Personnel.fillVacancies(v) ;
    return v ;
  }
  
  
  public static void establishRelations(Series <? extends Actor>... among) {
    for (Series <? extends Actor> sF : among) for (Actor f : sF) {
      for (Series <? extends Actor> tF : among) for (Actor t : tF) {
        if (f == t || f.memories.hasRelation(t)) continue;
        
        float initRelation = 0 ;
        for (int n = 10 ; n-- > 0 ;) {
          initRelation += Dialogue.tryChat(f, t) ;
        }
        f.memories.initRelation(t, initRelation, Rand.num()) ;
      }
    }
  }
  
  
  
  /*
  //  TODO:  Humans in general might want a method like this, during the setup
  //  process.
  public static void establishRelations(Venue venue) {
    
    final World world = venue.world() ;
    final Batch <Actor>
      from = new Batch <Actor> (),
      to = new Batch <Actor> () ;
    for (Actor a : venue.personnel.residents()) from.add(a) ;
    for (Actor a : venue.personnel.workers()) from.add(a) ;
    
    final Batch <Venue> nearby = new Batch <Venue> () ;
    world.presences.sampleFromKey(venue, world, 5, nearby, Venue.class) ;
    for (Venue v : nearby) {
      for (Actor a : v.personnel.residents()) to.add(a) ;
      for (Actor a : v.personnel.workers()) to.add(a) ;
    }
    
    for (Actor f : from) for (Actor t : to) {
      float initRelation = 0 ;
      for (int n = 10 ; n-- > 0 ;) {
        initRelation += Dialogue.tryChat(f, t) * 10 ;
      }
      f.memories.initRelation(t, initRelation, Rand.num()) ;
    }
  }
  //*/
}


