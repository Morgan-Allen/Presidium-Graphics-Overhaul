/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



//
//  TODO:  See if you can cache Boardings directly against themselves?
//  TODO:  Also, direct cache-queries for Routes would be useful!


public class PathingCache {
  
  
  /**  Constituent class and constant definitions-
    */
  final static int UPDATE_INTERVAL = Stage.STANDARD_HOUR_LENGTH;
  
  private static boolean verbose = false;
  
  
  private static int
    numRoutes = 0,
    numPlaces = 0,
    numSets   = 0,
    numZones  = 0,
    numLiveRoutes = 0,
    numLivePlaces = 0;
  
  
  public static void reportObs() {
    I.say("\nReporting on pathing objects...");
    I.say("  Total routes: "+numRoutes);
    I.say("  Total places: "+numPlaces);
    I.say("  Total sets:   "+numSets  );
    I.say("  Total zones:  "+numZones );
    I.say("  Live routes:  "+numLiveRoutes);
    I.say("  Live places:  "+numLivePlaces);
  }
  
  
  private class Route {
    Place from, to;
    float cost;
    Tile path[];
    boolean isDead;
    
    Route() { numRoutes++; }
    public void finalize() throws Throwable { numRoutes--; super.finalize(); }
  }
  
  private class Place {
    StageRegion region;
    Boarding core;
    Tile under[];
    
    Stack <Route> routes = new Stack <Route> ();
    Zone zones[] = new Zone[Base.MAX_BASES];
    
    Object flagged;
    boolean isDead;
    
    Place() { numPlaces++; }
    public void finalize() throws Throwable { numPlaces--; super.finalize(); }
  }
  
  private class PlaceSet {
    int expiry = -1;
    Place places[];
    
    PlaceSet() { numSets++; }
    public void finalize() throws Throwable { numSets--; super.finalize(); }
  }
  
  private class Zone {
    int expiry;
    Base client;
    Place places[] = null;
    
    Zone() { numZones++; }
    public void finalize() throws Throwable { numZones--; super.finalize(); }
  }
  
  
  final Stage world;
  final Place tilePlaces[][];
  final Table <StageRegion, PlaceSet> allCached;
  
  
  public PathingCache(Stage world) {
    this.world      = world;
    this.tilePlaces = new Place[world.size][world.size];
    this.allCached  = new Table(world.regions.gridCount * 2);
  }
  
  
  
  /**  Methods for refreshing the Places and Routes associated with each
    *  Section of the map:
    */
  private void refreshWithNeighbours(StageRegion region) {
    final StageRegion near[] = new StageRegion[9];
    world.regions.neighbours(region, near);
    near[8] = region;
    
    for (StageRegion n : near) if (n != null) refreshPlaces(n);
    for (StageRegion n : near) if (n != null) refreshRoutesBetween(region, n);
  }
  
  
  private void refreshPlaces(StageRegion region) {
    //
    //  Check to see if we're due for a refresh or not.  If so, delete any
    //  pre-existing Places.
    final int time = (int) world.currentTime();
    final PlaceSet oldSet = allCached.get(region);
    if (oldSet != null) {
      if (oldSet.expiry > time) return;
      else deletePlaceSet(oldSet);
    }
    //
    //  Then, scan all tiles within the region and create Places for either (A)
    //  occupying venues that originate there, or (B) contiguous areas of
    //  unblocked tiles within the region.
    final Batch <Place> places = new Batch();
    for (Tile t : world.tilesIn(region.area, false)) {
      if (t.flaggedWith() != null) continue;
      final Element a = t.reserves();
      
      if (a instanceof Boarding && a.origin() == t && t.blocked()) {
        places.add(createPlaceWithVenue((Boarding) t.above()));
      }
      else if (! t.blocked()) {
        places.add(createPlaceWithFloodFrom(t, region.area));
      }
    }
    //
    //  Clean up any flagging afterwards and store the results-
    final PlaceSet newSet = new PlaceSet();
    for (Place p : places) {
      for (Tile u : p.under) {
        u.flagWith(null);
        tilePlaces[u.x][u.y] = p;
      }
      p.region = region;
      numLivePlaces++;
    }
    newSet.places = places.toArray(Place.class);
    allCached.put(region, newSet);
    //
    //  As a final touch, we set an expiry for the new place-set (including a
    //  small random offset to ensure that updates don't all happen at once.)
    newSet.expiry = time + UPDATE_INTERVAL - Rand.index(UPDATE_INTERVAL / 2);
  }
  
  
  private Place createPlaceWithVenue(Boarding venue) {
    final Place p = new Place();
    
    final Stack <Tile> under = new Stack();
    for (Tile t : Spacing.under(venue.area(null), world)) {
      if (t.above() == venue && t.blocked()) {
        under.add(t);
        t.flagWith(p);
      }
    }
    
    p.core  = venue;
    p.under = under.toArray(Tile.class);
    return p;
  }
  
  
  private Place createPlaceWithFloodFrom(Tile t, Box2D area) {
    final Place p = new Place();
    
    final Tile tempB[] = new Tile[4];
    final Batch <Tile> flood  = new Batch();
    final Stack <Tile> fringe = new Stack();
    fringe.add(t);
    t.flagWith(p);
    
    while (fringe.size() > 0) {
      final Tile f = fringe.removeFirst();
      flood.add(f);
      for (Tile n : f.edgeAdjacent(tempB)) {
        if (n == null || ! area.contains(n.x, n.y)) continue;
        if (n.blocked() || n.flaggedWith() == p   ) continue;
        fringe.add(n);
        n.flagWith(p);
      }
    }
    
    final Tile under[] = flood.toArray(Tile.class);
    float avgX = 0, avgY = 0;
    for (Tile u : under) { avgX += u.x; avgY += u.y; }
    avgX /= under.length;
    avgY /= under.length;
    
    final Tile avg = world.tileAt(avgX, avgY);
    final Pick <Tile> pickCore = new Pick();
    for (Tile u : under) pickCore.compare(u, 0 - Spacing.distance(u, avg));
    
    p.core  = pickCore.result();
    p.under = under;
    return p;
  }
  
  
  private void deletePlaceSet(PlaceSet set) {
    for (Place place : set.places) {
      for (Route route : place.routes) {
        route.from.routes.remove(route);
        route.to  .routes.remove(route);
        route.isDead = true;
        numLiveRoutes--;
      }
      for (Tile u : place.under) tilePlaces[u.x][u.y] = null;
      place.isDead = true;
      numLivePlaces--;
    }
    set.places = null;
  }
  
  
  private void refreshRoutesBetween(StageRegion a, StageRegion b) {
    final PlaceSet setA = allCached.get(a), setB = allCached.get(b);
    
    for (Place from : setA.places) for (Place to : setB.places) {
      if (to == from || matchingRoute(from, to) != null) continue;
      
      final Route route = findNewRoute(from, to);
      if (route == null) continue;
      from.routes.add(route);
      to  .routes.add(route);
      numLiveRoutes++;
    }
  }
  
  
  private Route matchingRoute(Place from, Place to) {
    for (Route r : from.routes) if (
      (r.from == from && r.to   == to) ||
      (r.to   == from && r.from == to)
    ) {
      return r;
    }
    return null;
  }
  
  
  private Route findNewRoute(final Place a, final Place b) {
    //
    //  First, check to ensure that a valid path exists between the core of
    //  these places, and restricted to their underlying area-
    final PathSearch search = new PathSearch(a.core, b.core, false) {
      protected boolean canEnter(Boarding spot) {
        if (! super.canEnter(spot)) {
          return false;
        }
        else if (spot.boardableType() == Boarding.BOARDABLE_TILE) {
          final Place p = placeFor((Tile) spot, false);
          return p == a || p == b;
        }
        else {
          return spot == a.core || spot == b.core;
        }
      }
    };
    search.doSearch();
    if (! search.success()) return null;
    //
    //  If it does, create the Route object and assign proper data-
    final Route route = new Route();
    route.from = a;
    route.to   = b;
    route.cost = search.totalCost();
    final Batch <Tile> tiles = new Batch <Tile> ();
    for (Boarding onPath : search.fullPath(Boarding.class)) {
      if (onPath instanceof Tile) tiles.add((Tile) onPath);
    }
    route.path = tiles.toArray(Tile.class);
    return route;
  }
  
  
  
  /**  The ultimate payoff for all this machination- localised paths between
    *  arbitrary destinations on the map- and a few other utility methods for
    *  diagnosis of bugs...
    */
  private Place placeFor(Tile t, boolean refresh) {
    if (refresh) {
      refreshWithNeighbours(world.regions.regionAt(t.x, t.y));
    }
    return tilePlaces[t.x][t.y];
  }
  
  
  private Place[] placesBetween(
    Target initB, Target destB, Accountable client, boolean reports
  ) {
    final Tile
      initT = PathSearch.approachTile(initB, client),
      destT = PathSearch.approachTile(destB, client);
    
    if (initT == null || destT == null) {
      if (reports) I.say("Initial place-tiles invalid: "+initT+"/"+destT);
      return null;
    }
    
    final Place
      initP = placeFor(initT, true),
      destP = placeFor(destT, true);
    
    if (initP == null || destP == null) {
      if (reports) I.say("Initial places invalid: "+initP+"/"+destP);
      return null;
    }
    if (! hasPathBetween(initP, destP, null, reports)) {
      if (reports) I.say("NO PATH BETWEEN: "+initP+"/"+destP);
      return null;
    }
    final Place placesPath[] = placesPath(initP, destP, false, client, reports);
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
  
  
  public Boarding[] getLocalPath(
    Boarding initB, Boarding destB, int maxLength,
    Mobile client, boolean reports
  ) {
    Boarding path[] = null;
    final int verbosity = reports ? Search.VERBOSE : Search.NOT_VERBOSE;
    
    if (Spacing.distance(initB, destB) <= Stage.ZONE_SIZE) {
      if (reports) I.say(
        "\nUsing simple agenda-bounded search between "+initB+" and "+destB
      );
      final PathSearch search = new PathSearch(initB, destB, true);
      search.assignClient(client);
      search.verbosity = verbosity;
      search.doSearch();
      path = search.fullPath(Boarding.class);
      if (path != null) return path;
    }
    final Place placesPath[] = placesBetween(initB, destB, client, reports);
    
    if (placesPath != null && placesPath.length > 3) {
      if (reports) I.say(
        "\nUsing partial cordoned search between "+initB+" and "+destB
      );
      final PathSearch search = fullPathSearch(
        initB, placesPath[2].core, placesPath, maxLength
      );
      search.assignClient(client);
      search.verbosity = verbosity;
      search.doSearch();
      path = search.fullPath(Boarding.class);
      if (path != null) return path;
    }
    if (placesPath != null && placesPath.length <= 3) {
      if (reports) I.say(
        "\nUsing full cordoned search between "+initB+" and "+destB
      );
      final PathSearch search = fullPathSearch(
        initB, destB, placesPath, maxLength
      );
      search.assignClient(client);
      search.verbosity = verbosity;
      search.doSearch();
      path = search.fullPath(Boarding.class);
      if (path != null) return path;
    }
    if (path == null) {
      if (reports) I.say(
        "\nResorting to agenda-bounded search between "+initB+" and "+destB
      );
      final PathSearch search = new PathSearch(initB, destB, true);
      search.assignClient(client);
      search.verbosity = verbosity;
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
  
  
  public Tile[] placeTiles(Tile t) {
    final Place p = placeFor(t, true);
    return p == null ? null : p.under;
  }
  
  
  public Tile[][] placeRoutes(Tile t) {
    refreshWithNeighbours(world.regions.regionAt(t.x, t.y));
    final Place p = tilePlaces[t.x][t.y];
    if (p == null) return null;
    final Tile tiles[][] = new Tile[p.routes.size()][];
    int i = 0; for (Route route : p.routes) tiles[i++] = route.path;
    return tiles;
  }
  
  
  
  /**  Then some utility methods for rapid checking of pathability between
    *  two points for a particular Base.
    */
  private Zone zoneFor(Place p, Base client) {
    return p.zones[client.baseID()];
  }
  
  
  private void fillZoneFrom(Place init, Base client, boolean reports) {
    
    final int  time   = (int) world.currentTime();
    final Zone initZ  = zoneFor(init, client);
    final int  baseID = client.baseID();
    
    if (initZ != null) for (Place p : initZ.places) p.zones[baseID] = null;
    
    final Zone zone = new Zone();
    zone.places = placesPath(init, null, true, client, reports);
    zone.expiry = time + UPDATE_INTERVAL;
    zone.client = client;
    for (Place p : zone.places) p.zones[baseID] = zone;
  }
  
  
  private boolean hasPathBetween(
    Place initP, Place destP, Base client, boolean reports
  ) {
    final int time = (int) world.currentTime();
    final Zone initZ = zoneFor(initP, client), destZ = zoneFor(destP, client);
    
    if (initZ == null || initZ.expiry < time) {
      fillZoneFrom(initP, client, reports);
    }
    if (destZ == null || destZ.expiry < time) {
      fillZoneFrom(destP, client, reports);
    }
    return initZ == destZ;
  }
  
  
  public boolean hasPathBetween(
    Target a, Target b, Base client, boolean reports
  ) {
    final Tile
      initT = PathSearch.approachTile(a, client),
      destT = PathSearch.approachTile(b, client);
    final Place
      initP = placeFor(initT, true),
      destP = placeFor(destT, true);
    
    if (initP == null || destP == null) return false;
    return hasPathBetween(initP, destP, client, reports);
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
        if (verbose) I.say("No places path exists!");
        return null;
      }
    }
    //
    //  We create a specialised form of pathing-search that always aims toward
    //  the next stop along the places-path.
    //
    //  TODO:  Put this in a dedicated class lower down, or possibly even move
    //  to the PathingSearch class itself?
    final Tile initT = PathSearch.approachTile(initB, null);
    
    final PathSearch search = new PathSearch(initB, destB, false) {
      
      final int PPL = placesPath.length;
      private Place lastPlace = placesPath[0];
      private int PPI = PPL == 1 ? 0 : 1, oldPPI = -1;
      private Target heading = PPL == 1 ? destB : placesPath[1].core;
      private Tile closest = initT;
      private Box2D tempArea = new Box2D(-1, -1, 0, 0);
      
      protected boolean stepSearch() {
        final Boarding best = closest;
        if (best instanceof Tile) {
          final Tile tile = (Tile) best;
          final Place under = tilePlaces[tile.x][tile.y];
          if (under != lastPlace) {
            for (int i = PPL; i-- > 0;) if (placesPath[i] == under) {
              PPI = Nums.clamp(i + 1, PPL);
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
          if (PPI == oldPPI) return tempArea.contains(tile.x, tile.y);
          
          final Place
            curr = placesPath[PPI],
            next = placesPath[Nums.clamp(PPI + 1, PPL)],
            last = placesPath[Nums.clamp(PPI - 1, PPL)];
          tempArea.setTo  (curr.region.area);
          tempArea.include(next.region.area);
          tempArea.include(last.region.area);
          
          oldPPI = PPI;
          return tempArea.contains(tile.x, tile.y);
        }
        return true;
      }
      
    };
    return search;
  }
  
  
  private Place[] placesPath(
    final Place init, final Place dest,
    final boolean zoneFill, final Accountable client,
    boolean reports
  ) {
    if (reports && dest != null) {
      I.say("\nSearching for place path between "+init.core+" and "+dest.core);
    }
    if (reports && zoneFill) {
      I.say("\nSearching for places within zone starting from "+init.core);
    }
    
    final Search <Place> search = new Search <Place> (init, -1) {
      
      final Place tempP[] = new Place[8];
      
      protected Place[] adjacent(Place spot) {
        refreshWithNeighbours(spot.region);
        
        final Place near[] = spot.routes.size() > tempP.length ?
          new Place[spot.routes.size()] : tempP
        ;
        int i = 0; for (Route route : spot.routes) {
          final Place n = (route.from == spot) ? route.to : route.from;
          if (PathSearch.blockedBy(n.core, client)) near[i] = null;
          else near[i++] = n;
        }
        while (i < tempP.length) near[i++] = null;
        return near;
      }
      
      protected float cost(Place prior, Place spot) {
        for (Route r : prior.routes) {
          if (r.from == spot || r.to == spot) return r.cost;
        }
        return -1;
      }
      
      protected boolean endSearch(Place best) {
        if (zoneFill) return false;
        else return best == dest;
      }
      
      protected float estimate(Place spot) {
        if (zoneFill) return 0;
        else return Spacing.distance(spot.core, dest.core);
      }
      
      protected void setEntry(Place spot, Entry flag) {
        spot.flagged = flag;
      }
      
      protected Entry entryFor(Place spot) {
        return (Entry) spot.flagged;
      }
    };
    
    if (reports) search.verbosity = Search.VERBOSE;
    search.doSearch();
    
    if (zoneFill) {
      return search.allSearched(Place.class);
    }
    else if (! search.success()) {
      return null;
    }
    else {
      return search.fullPath(Place.class);
    }
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




