/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;



//  TODO:  All of this needs to be saved & loaded, or you'll get major slowdown
//  after loading from a saved game.


public class PathingCache {
  
  
  
  /**  Constituent class and constant definitions-
    */
  final static int UPDATE_INTERVAL = 10;
  
  private static boolean verbose = false;
  private boolean printSearch = false;
  
  private class Route {
    Place from, to;
    float cost;
    Tile path[];
  }
  
  private class Place {
    Tile core, under[];
    Caching caching;
    Stack <Route> routes = new Stack <Route> ();
    Zone zone;
    
    Place nearCache[];
    Object flagged;
    boolean isDead = false;
  }
  
  private class Caching {
    WorldSection section;
    Place places[];
    float lastUpdateTime;
  }
  
  private class Zone {
    Batch <Place> places = new Batch <Place> ();
  }
  
  
  final Stage world;
  final Place tilePlaces[][];
  final Table <WorldSection, Caching> allCached = new Table <WorldSection, Caching> ();
  //final MipMap pathMipMap;
  
  
  
  public PathingCache(Stage world) {
    this.world = world;
    this.tilePlaces = new Place[world.size][world.size];
    //this.pathMipMap = new MipMap(world.size);
  }
  
  
  
  /**  The ultimate payoff for all this machination- localised paths between
    *  arbitrary destinations on the map- and a few other utility methods for
    *  diagnosis of bugs...
    */
  //
  //  TODO:  Unify this with the location() method in MobileMotion.
  private Tile tilePosition(Boarding b, Mobile client) {
    if (b == null) return null;
    if (client == null || b.allowsEntry(client)) {
      if (b instanceof Venue) return ((Venue) b).mainEntrance();
      if (b instanceof Tile) {
        final Tile t = (Tile) b;
        return t.blocked() ? null : t;
      }
      if (b instanceof Boarding) {
        for (Boarding e : ((Boarding) b).canBoard()) {
          if (e instanceof Tile) return (Tile) e;
        }
        return null;
      }
    }
    return Spacing.nearestOpenTile(b, b);
  }
  
  
  private Place[] placesBetween(
    Boarding initB, Boarding destB, Mobile client, boolean reports
  ) {
    final Tile
      initT = tilePosition(initB, client),
      destT = tilePosition(destB, client);
    if (initT == null || destT == null) {
      if (reports) I.say("Initial place-tiles invalid!");
      return null;
    }
    final Place
      initP = placeFor(initT),
      destP = placeFor(destT);
    if (initP == null || destP == null) {
      if (reports) I.say("Initial places invalid!");
      return null;
    }
    final Place placesPath[] = placesPath(initP, destP, reports);
    if (placesPath == null || placesPath.length < 1) {
      if (reports) I.say("NO PLACES PATH!");
      return null;
    }
    if (! verifyPlacesPath(placesPath)) {
      I.complain("Places path is broken...");
      return null;
    }
    return placesPath;
  }
  
  
  //
  //  TODO:  Move this to the mobile pathing class?
  public Boarding[] getLocalPath(
    Boarding initB, Boarding destB, int maxLength,
    Mobile client, boolean reports
  ) {
    Boarding path[] = null;
    //*
    if (Spacing.distance(initB, destB) <= Stage.SECTOR_SIZE * 2) {
      if (reports) I.say(
        "Using simple agenda-bounded pathing between "+initB+" "+destB
      );
      final PathSearch search = new PathSearch(initB, destB);
      search.client = client;
      search.verbose = reports;
      search.doSearch();
      path = search.fullPath(Boarding.class);
      if (path != null) return path;
    }
    final Place placesPath[] = placesBetween(initB, destB, client, reports);
    
    if (placesPath != null && placesPath.length < 3) {
      if (reports) I.say(
        "Using full cordoned path-search between "+initB+" "+destB
      );
      final PathSearch search = cordonedSearch(
        initB, destB, placesPath[0].caching.section,
        placesPath[placesPath.length - 1].caching.section
      );
      search.client = client;
      search.verbose = reports;
      search.doSearch();
      path = search.fullPath(Boarding.class);
      if (path != null) return path;
    }
    if (placesPath != null) {
      if (reports) I.say(
        "Using partial cordoned path-search between "+initB+" "+destB
      );
      final PathSearch search = fullPathSearch(
        initB, destB, placesPath, maxLength
      );
      search.client = client;
      search.verbose = reports;
      search.doSearch();
      path = search.fullPath(Boarding.class);
      if (path != null) return path;
    }
    if (path == null) {
      if (reports) I.say(
        "Resorting to agenda-bounded pathfinding between "+initB+" "+destB
      );
      final PathSearch search = new PathSearch(initB, destB);
      search.client = client;
      search.verbose = reports;
      search.doSearch();
      path = search.fullPath(Boarding.class);
      if (path != null) return path;
    }
    return null;
  }
  
  
  private boolean verifyPlacesPath(Place placesPath[]) {
    for (int i = 0; i < placesPath.length - 1; i++) {
      final Place next = placesPath[i + 1], curr = placesPath[i];
      if (next.isDead || curr.isDead) {
        if (verbose) {
          if (next.isDead) I.say("DEAD PLACE: "+next.core);
          if (curr.isDead) I.say("DEAD PLACE: "+curr.core);
          I.say("PATH BROKE AT INDEX: "+i+"/"+placesPath.length);
        }
        return false;
      }
      boolean linked = false;
      for (Route r : curr.routes) {
        if (r.from == next || r.to == next) linked = true;
      }
      if (! linked) {
        if (verbose) {
          I.say("NO ROUTE BETWEEN "+curr.core+" AND "+next.core);
          I.say("PATH BROKE AT INDEX: "+i+"/"+placesPath.length);
        }
        return false;
      }
    }
    return true;
  }
  
  
  private Place placeFor(Tile t) {
    refreshWithNeighbours(world.sections.sectionAt(t.x, t.y));
    return tilePlaces[t.x][t.y];
  }
  
  
  public Tile[] placeTiles(Tile t) {
    final Place p = placeFor(t);
    return p == null ? null : p.under;
  }
  
  
  public Tile[][] placeRoutes(Tile t) {
    refreshWithNeighbours(world.sections.sectionAt(t.x, t.y));
    final Place p = tilePlaces[t.x][t.y];
    if (p == null) return null;
    final Tile tiles[][] = new Tile[p.routes.size()][];
    int i = 0; for (Route route : p.routes) tiles[i++] = route.path;
    return tiles;
  }
  
  
  
  /**  Methods for refreshing the Places and Routes associated with each
    *  Section of the map:
    */
  private void refreshWithNeighbours(WorldSection section) {
    final WorldSection near[] = new WorldSection[9];
    world.sections.neighbours(section, near);
    near[8] = section;
    for (int i = 9; i-- > 0;) if (! refreshPlaces(near[i])) near[i] = null;
    for (WorldSection n : near) refreshRoutes(n);
  }
  
  
  private boolean refreshPlaces(WorldSection section) {
    //
    //  First of all, check to ensure that an update is required.  If so,
    //  generate new places for underlying tiles:
    if (section == null) return false;
    final float time = world.currentTime();
    Caching caching = allCached.get(section);
    if (caching == null) {
      caching = new Caching();
      caching.section = section;
      allCached.put(section, caching);
    }
    else if ((time - caching.lastUpdateTime) > UPDATE_INTERVAL) {
      for (Place place : caching.places) deletePlace(place);
    }
    else return false;
    if (verbose || printSearch) {
      I.say("Refreshing places at: "+section.x+"|"+section.y+", time: "+time);
    }
    caching.places = grabPlacesFor(caching, section);
    caching.lastUpdateTime = time;
    return true;
  }
  
  
  private void refreshRoutes(WorldSection section) {
    //
    //  Grab all nearby Places first, including those in the same or adjacent
    //  sections-
    if (section == null) return;
    if (verbose || printSearch) {
      I.say("Refreshing ROUTES at: "+section.x+"|"+section.y);
    }
    final Caching caching = allCached.get(section);
    final Batch <Place> near = new Batch <Place> ();
    for (Place place : caching.places) near.add(place);
    for (WorldSection nS : world.sections.neighbours(section, null)) {
      if (nS == null) continue;
      final Caching nC = allCached.get(nS);
      if (nC != null) for (Place place : nC.places) near.add(place);
    }
    //
    //  Having done so, establish routes between all distinct places where
    //  possible-
    for (Place place : caching.places) {
      for (Place other : near) if (other != place) {
        final Route route = routeBetween(place, other);
        if (route == null) continue;
        place.routes.add(route);
        other.routes.add(route);
        place.nearCache = other.nearCache = null;
      }
    }
  }
  
  
  private void deletePlace(Place place) {
    for (Route route : place.routes) {
      route.from.routes.remove(route);
      route.to.routes.remove(route);
      route.from.nearCache = route.to.nearCache = null;
    }
    for (Tile u : place.under) tilePlaces[u.x][u.y] = null;
    place.isDead = true;
  }
  
  
  
  /**  Methods for establishing Places in the first place-
    */
  private Place[] grabPlacesFor(Caching caching, final WorldSection section) {
    ///I.say("Grabbing new places at: "+section.area);
    //
    //  We scan through every tile in this section, and grab any contiguous
    //  areas of unblocked tiles.  (These must be flagged just after
    //  acquisition, so that we know to skip over them.)
    final Batch <Tile[]> allUnder = new Batch <Tile[]> ();
    for (Coord c : Visit.grid(section.area)) {
      final Tile t = world.tileAt(c.x, c.y);
      if (t.flaggedWith() != null || t.blocked()) continue;
      final TileSpread spread = new TileSpread(t) {
        protected boolean canAccess(Tile t) {
          return section.area.contains(t.x, t.y) && ! t.blocked();
        }
        protected boolean canPlaceAt(Tile t) { return false; }
      };
      spread.doSearch();
      final Tile under[] = spread.allSearched(Tile.class);
      for (Tile u : under) u.flagWith(allUnder);
      allUnder.add(under);
    }
    //
    //  Having obtained each block of tiles, we create corresponding Place
    //  objects, and assign them a core-
    final Place places[] = new Place[allUnder.size()];
    int i = 0; for (Tile[] under : allUnder) {
      final Place place = places[i++] = new Place();
      place.under = under;
      place.core = findCore(under);
      place.caching = caching;
      if (verbose) I.say(under.length+" tiles grabbed...");
      for (Tile u : under) {
        tilePlaces[u.x][u.y] = place;
        u.flagWith(null);
      }
    }
    return places;
  }
  
  
  private Tile findCore(Tile tiles[]) {
    //
    //  First, we get the average position of all the tiles-
    final Vec2D avg = new Vec2D(), pos = new Vec2D();
    for (Tile t : tiles) { avg.x += t.x; avg.y += t.y; }
    avg.scale(1f / tiles.length);
    //
    //  Then return the tile closest to this centre-
    Tile closest = null;
    float minDist = Float.POSITIVE_INFINITY;
    for (Tile t : tiles) {
      final float dist = avg.pointDist(pos.set(t.x, t.y));
      if (dist < minDist) { closest = t; minDist = dist; }
    }
    return closest;
  }
  
  
  
  /**  Generating paths between nearby Places, and among the larger network of
    *  Places-
    */
  public PathSearch fullPathSearch(
    final Boarding initB, final Boarding destB,
    Object placesPathRef, final int maxLength
  ) {
    if (placesPathRef != null && ! (placesPathRef instanceof Place[])) {
      I.complain("PATH REF IS NOT A PLACE ARRAY!");
      return null;
    }
    final Place placesPath[];
    if (placesPathRef instanceof Place[]) {
      placesPath = (Place[]) placesPathRef;
    }
    else {
      placesPath = placesBetween(initB, destB, null, verbose);
      if (placesPath == null) {
        I.say("No places path exists!");
        return null;
      }
    }
    //
    //  We create a specialised form of pathing-search that always aims toward
    //  the next stop along the places-path.
    //
    //  TODO:  Put this in a dedicated class lower down, or possibly even move
    //  to the PathingSearch class itself?
    final Tile initT = tilePosition(initB, null);
    
    final PathSearch search = new PathSearch(initB, destB, -1) {
      
      final int PPL = placesPath.length;
      private Place lastPlace = placesPath[0];
      private int PPI = PPL == 1 ? 0 : 1;
      private Target heading = PPL == 1 ? destB : placesPath[1].core;
      private Tile closest = initT;
      private Box2D tempArea = new Box2D();
      
      protected boolean stepSearch() {
        final Boarding best = closest;
        if (best instanceof Tile) {
          final Tile tile = (Tile) best;
          final Place under = tilePlaces[tile.x][tile.y];
          if (under != lastPlace) {
            for (int i = PPL; i-- > 0;) if (placesPath[i] == under) {
              if (this.verbose) I.say("  Best Tile: "+best);
              PPI = Visit.clamp(i + 1, PPL);
              break;
            }
          }
          lastPlace = under;
          heading = (PPI == PPL - 1) ? destB : placesPath[PPI].core;
        }
        return super.stepSearch();
      }
      
      protected boolean endSearch(Boarding best) {
        if (super.endSearch(best)) return true;
        if (best != closest) return false;
        if (maxLength > 0 && pathLength(best) >= maxLength) return true;
        return false;
      }
      
      protected float estimate(Boarding spot) {
        float dist = Spacing.distance(spot, heading);
        final float closestDist = Spacing.distance(closest, heading);
        if (spot instanceof Tile && dist < closestDist) {
          closest = (Tile) spot;
        }
        dist += (PPL - (PPI + 1)) * Stage.PATCH_RESOLUTION;
        final Boarding best = bestFound();
        if (best != null) dist += Spacing.distance(closest, spot) / 3.0f;
        return dist * 1.1f;
      }
      
      protected boolean canEnter(Boarding spot) {
        if (! super.canEnter(spot)) return false;
        if (spot instanceof Tile) {
          final Tile tile = (Tile) spot;
          final Place
            curr = placesPath[PPI],
            next = placesPath[Visit.clamp(PPI + 1, PPL)],
            last = placesPath[Visit.clamp(PPI - 1, PPL)];
          tempArea.setTo(curr.caching.section.area);
          tempArea.include(next.caching.section.area);
          tempArea.include(last.caching.section.area);
          return tempArea.contains(tile.x, tile.y);
        }
        return true;
      }
      //*/
    };
    return search;
  }
  
  
  private PathSearch cordonedSearch(
    Boarding a, Boarding b, WorldSection sA, WorldSection sB
  ) {
    //
    //  Creates a pathing search between two points restricted to the given
    //  sections.
    final Box2D
      cordon = new Box2D().setTo(sA.area).include(sB.area),
      tB = new Box2D();
    final PathSearch search = new PathSearch(a, b, -1) {
      protected boolean canEnter(Boarding spot) {
        if (! super.canEnter(spot)) return false;
        if (spot instanceof Tile) {
          final Tile t = (Tile) spot;
          return cordon.contains(t.x, t.y);
        }
        else {
          spot.area(tB);
          return cordon.intersects(tB);
        }
      }
    };
    return search;
  }
  
  
  private Route routeBetween(Place a, Place b) {
    //
    //  First, check to ensure that a valid path exists-
    final PathSearch search = cordonedSearch(
      a.core, b.core, a.caching.section, b.caching.section
    );
    search.doSearch();
    if (! search.success()) return null;
    //
    //  If it does, create the Route object and assign proper data-
    final Route route = new Route();
    route.from = a;
    route.to = b;
    route.cost = search.totalCost();
    ///if (route.cost < 0) I.say("NEGATIVE COST BETWEEN "+a.core+" and "+b.core);
    final Batch <Tile> tiles = new Batch <Tile> ();
    for (Boarding onPath : search.fullPath(Boarding.class)) {
      if (onPath instanceof Tile) tiles.add((Tile) onPath);
    }
    route.path = tiles.toArray(Tile.class);
    return route;
  }
  
  
  private Place[] placesPath(
    final Place init, final Place dest, boolean reports
  ) {
    if (printSearch) I.say("Searching from "+init.core+" to "+dest.core);
    
    final Search <Place> search = new Search <Place> (init, -1) {
      
      protected Place[] adjacent(Place spot) {
        //
        //  TODO:  Apparently, the nearCache was the problem.  It was causing
        //  dead places to stay in circulation within the agenda following
        //  their obsolescence.
        //if (spot.nearCache != null) return spot.nearCache;
        refreshWithNeighbours(spot.caching.section);
        final Place near[] = spot.nearCache = new Place[spot.routes.size()];
        int i = 0; for (Route route : spot.routes) {
          near[i++] = (route.from == spot) ? route.to : route.from;
        }
        return near;
      }
      
      protected float cost(Place prior, Place spot) {
        for (Route r : prior.routes) {
          if (r.from == spot || r.to == spot) return r.cost;
          ///if (r.from == spot && r.to == prior) return r.cost;
          ///if (r.to == spot && r.from == prior) return r.cost;
        }
        return -1;
      }
      
      protected boolean endSearch(Place best) {
        return best == dest;
      }
      
      protected float estimate(Place spot) {
        return Spacing.distance(spot.core, dest.core);
      }
      
      protected void setEntry(Place spot, Entry flag) {
        spot.flagged = flag;
      }
      
      protected Entry entryFor(Place spot) {
        return (Entry) spot.flagged;
      }
    };
    search.verbose = reports;
    search.doSearch();
    if (! search.success()) return null;
    return search.fullPath(Place.class);
  }
}



//
//  TODO:  It might be best if this only updated when buildings in a section
//  are placed/deleted, fog levels change, etc. etc.- and no more than once per
//  10 seconds.
//
//  TODO:  Next, ideally, you'll want to build up a recursive tree-structure
//  out of Regions so that the viability of pathing attempts can be determined
//  as quickly as possible (when querying nearby venues, etc.)



