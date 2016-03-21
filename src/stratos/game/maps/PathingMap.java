/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.util.*;



//  TODO:  Ensure places aren't refreshed more than once every 5 seconds?
//         Also, direct cache-queries for Routes would be useful.


public class PathingMap {
  
  
  /**  Constituent class and constant definitions-
    */
  final static boolean
    updatesVerbose = false,
    extraVerbose   = false;
  
  final static int
    ROUTE_CACHE_EXPIRY = 1,
    MAX_ROUTES_CACHED  = 5000;
  
  private static int
    numRoutes     = 0,
    numPlaces     = 0,
    numSets       = 0,
    numZones      = 0,
    numLiveRoutes = 0,
    numLivePlaces = 0,
    
    numIterations   = 0,
    numTilesScanned = 0,
    numTilesRouted  = 0,
    maxInQuery      = 0;
  
  
  private class Route {
    Place from, to;
    float cost;
    Tile path[];
    
    Route() { numRoutes++; }
    public void finalize() throws Throwable { numRoutes--; super.finalize(); }
  }
  
  private class Place {
    StagePatch region;
    Boarding core;
    Tile under[];
    
    Route routeCache[] = null;
    Stack <Route> routeList = new Stack();
    Object flagged;
    
    public String toString() {
      return "P"+under.length+" "+region.x+"|"+region.y+" ("+core+")";
    }
    
    Place() { numPlaces++; }
    public void finalize() throws Throwable { numPlaces--; super.finalize(); }
  }
  
  private class PlaceSet {
    Place places[];
    boolean needsRefresh = false;
    
    PlaceSet() { numSets++; }
    public void finalize() throws Throwable { numSets--; super.finalize(); }
  }
  
  private class PlaceRoute {
    Place path[];
    String hash;
    float expireTime;
  }
  
  
  final Stage world;
  final Place    tilePlaces[][];
  final PlaceSet placeSets [][];
  final List <PlaceSet> needRefresh = new List();
  final Table <String, PlaceRoute> placeRouteCache;
  final List <PlaceRoute> allPlaceRoutes = new List();
  private boolean setupDone;
  
  
  public PathingMap(Stage world) {
    this.world      = world;
    this.tilePlaces = new Place[world.size][world.size];
    final int gridSize = world.patches.gridSize;
    this.placeSets  = new PlaceSet[gridSize][gridSize];
    this.placeRouteCache = new Table(MAX_ROUTES_CACHED * 2);
    //  TODO:  Consider saving & loading this information?
  }
  
  
  public void initMap() {
    for (StagePatch p : world.patches.allGridPatches()) {
      refreshWithNeighbours(p);
    }
    updateMap();
    setupDone = true;
  }
  
  
  public void flagForUpdateAt(Tile tile) {
    for (Tile n : tile.vicinity(null)) if (n != null) {
      final StagePatch region = world.patches.patchAt(n.x, n.y);
      final PlaceSet   set    = placeSets[region.x][region.y];
      
      if (set != null && ! set.needsRefresh) {
        if (updatesVerbose && setupDone) {
          I.say("\nWILL REFRESH PLACES AT "+region);
          I.say("  First flagged by: "+tile+" ("+tile.above()+")");
          I.say("  Tile affected:    "+n+", area: "+region.area);
        }
        set.needsRefresh = true;
        needRefresh.add(set);
      }
    }
  }
  
  
  public void updateMap() {
    
    long initTime = System.currentTimeMillis();
    int numRefreshed = 0, neededRefresh = needRefresh.size();
    
    while (needRefresh.size() > 0 && ! world.schedule.timeUp()) {
      final PlaceSet set = needRefresh.removeFirst();
      refreshWithNeighbours(set.places[0].region);
      numRefreshed++;
    }
    
    if (updatesVerbose && numRefreshed > 0) {
      long timeSpent = System.currentTimeMillis() - initTime;
      long timeTotal = world.schedule.timeTakenOnUpdate();
      
      I.say("\nPERFORMED MAP UPDATE!  TIME SPENT: "+timeSpent);
      I.say("  Refreshed "+numRefreshed+"/"+neededRefresh);
      I.say("  Current schedule time: "+timeTotal);
    }
  }
  
  
  public static void reportObs() {
    I.say("\nReporting on pathing objects...");
    I.say("  Total routes: "+numRoutes);
    I.say("  Total places: "+numPlaces);
    I.say("  Total sets:   "+numSets  );
    I.say("  Total zones:  "+numZones );
    I.say("  Live routes:  "+numLiveRoutes);
    I.say("  Live places:  "+numLivePlaces);
    
    I.say("\nReporting on work done: ");
    float avgTile = numTilesScanned * 1f / numIterations;
    float avgRoute = numTilesRouted * 1f / numIterations;
    I.say("  Average scanned/second: "+avgTile );
    I.say("  Average routed/second:  "+avgRoute);
    I.say("  Max. in single query:   "+maxInQuery);
    
    numIterations++;
  }
  
  
  
  /**  Methods for refreshing the Places and Routes associated with each
    *  Section of the map:
    */
  private void refreshWithNeighbours(StagePatch region) {
    final StagePatch near[] = new StagePatch[9];
    world.patches.neighbours(region, near);
    near[8] = region;
    
    final PlaceSet oldSets[] = new PlaceSet[9];
    for (int i = 9; i-- > 0;) {
      final StagePatch n = near[i];
      if (n != null) oldSets[i] = placeSets[n.x][n.y];
    }
    
    for (StagePatch n : near) if (n != null) refreshPlaces(region, n);
    for (StagePatch n : near) if (n != null) refreshRoutesBetween(region, n);
    
    for (int i = 9; i-- > 0;) {
      final StagePatch n = near[i];
      if (n == null) continue;
      final PlaceSet oldSet = oldSets[i];
      final PlaceSet newSet = placeSets[n.x][n.y];
      //
      //  Finally, we update the route cache for any associated places-
      int placeIndex = Nums.max(
        newSet == null ? 0 : newSet.places.length,
        oldSet == null ? 0 : oldSet.places.length
      );
      while (placeIndex-- > 0) refreshRouteCache(oldSet, newSet, placeIndex);
    }
  }
  
  
  private void refreshRouteCache(PlaceSet oldSet, PlaceSet newSet, int index) {
    //
    //  TODO:  Consider doing an automatic refresh if the old and new number of
    //  places is different?  Might be simpler/safer...
    final Place n = (newSet == null || index >= newSet.places.length) ?
      null : newSet.places[index]
    ;
    final Place o = (oldSet == null || index >= oldSet.places.length) ?
      null : oldSet.places[index]
    ;
    refreshRouteCache(n);
    refreshRouteCache(o);
  }
  
  
  private void refreshRouteCache(Place p) {
    if (p == null || p.routeCache != null) return;
    p.routeCache = p.routeList.toArray(Route.class);
  }
  
  
  private void refreshPlaces(StagePatch from, StagePatch region) {
    //
    //  Check to see if we're due for a refresh or not.  If so, delete any
    //  pre-existing Places.
    final PlaceSet oldSet = placeSets[region.x][region.y];
    if (oldSet != null) {
      if (! oldSet.needsRefresh) return;
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
        places.add(createPlaceWithVenue((Boarding) t.reserves()));
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
      numTilesScanned += p.under.length;
      
      if (updatesVerbose && setupDone) {
        I.say("\nCREATED NEW PLACE: "+p);
      }
    }
    newSet.places = places.toArray(Place.class);
    placeSets[region.x][region.y] = newSet;
  }
  
  
  private Place createPlaceWithVenue(Boarding venue) {
    final Place p = new Place();
    
    final Batch <Tile> under = new Batch();
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
      for (Route route : place.routeList) {
        route.from.routeList.remove(route);
        route.to  .routeList.remove(route);
        route.from.routeCache = null;
        route.to  .routeCache = null;
        numLiveRoutes--;
      }
      for (Tile u : place.under) tilePlaces[u.x][u.y] = null;
      numLivePlaces--;
    }
  }
  
  
  private void refreshRoutesBetween(StagePatch a, StagePatch b) {
    final PlaceSet setA = placeSets[a.x][a.y], setB = placeSets[b.x][b.y];
    if (setA == null || setB == null) return;
    
    for (Place from : setA.places) for (Place to : setB.places) {
      if (to == from || matchingRoute(from, to) != null) continue;
      
      final Route route = findNewRoute(from, to);
      if (route == null) continue;
      from.routeList.add(route);
      to  .routeList.add(route);
      from.routeCache = null;
      to  .routeCache = null;
      numLiveRoutes++;
    }
  }
  
  
  private Route matchingRoute(Place from, Place to) {
    for (Route r : from.routeList) if (
      (r.from == from && r.to   == to) ||
      (r.to   == from && r.from == to)
    ) {
      return r;
    }
    return null;
  }
  
  
  private Route findNewRoute(final Place a, final Place b) {
    //
    //  In the case of pathing between two venue-places, we can just check
    //  directly for boardability:
    if (
      a.core.boardableType() != Boarding.BOARDABLE_TILE &&
      b.core.boardableType() != Boarding.BOARDABLE_TILE
    ) {
      if (! a.core.isEntrance(b.core)) return null;
      final Route route = new Route();
      route.from = a;
      route.to   = b;
      route.cost = Spacing.innerDistance(a.core, b.core) * 2.5f;
      route.path = new Tile[0];
      return route;
    }
    //
    //  Otherwise, check to ensure that a valid path exists between the core of
    //  these places, and restricted to their underlying area-
    final PathSearch search = new PathSearch(a.core, b.core, false) {
      protected boolean canEnter(Boarding spot) {
        if (! super.canEnter(spot)) {
          return false;
        }
        else {
          final Place p = placeFor(spot);
          return p == a || p == b;
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
    numTilesRouted += search.allSearchedCount();
    return route;
  }
  
  
  
  /**  The ultimate payoff for all this machination- localised paths between
    *  arbitrary destinations on the map- and a few other utility methods for
    *  diagnosis of bugs...
    */
  public Place placeFor(Boarding spot) {
    if (spot.boardableType() == Boarding.BOARDABLE_TILE) {
      return placeFor((Tile) spot);
    }
    else {
      return placeFor(((Element) spot).origin());
    }
  }
  
  
  private Place placeFor(Tile t) {
    return tilePlaces[t.x][t.y];
  }
  
  
  private Place[] placesBetween(
    Boarding initB, Boarding destB, Accountable client, boolean reports
  ) {
    final Place
      initP = placeFor(initB),
      destP = placeFor(destB);
    
    if (initP == null || destP == null) {
      if (reports) I.say("Initial places invalid: "+initP+"/"+destP);
      return null;
    }
    
    if (! hasPathBetween(initP, destP, client, reports)) {
      if (reports) I.say("NO PATH BETWEEN: "+initP+"/"+destP);
      return null;
    }
    
    final Place placesPath[] = placesPath(initP, destP, false, client, reports);
    if (placesPath == null || placesPath.length < 1) {
      if (reports) I.say("NO PLACES PATH!");
      return null;
    }
    return placesPath;
  }
  
  
  public Boarding[] getLocalPath(
    Boarding initB, Boarding destB, int maxLength,
    Accountable client, boolean reports
  ) {
    Boarding path[] = null;
    final int verbosity = reports ? Search.VERBOSE : Search.NOT_VERBOSE;
    
    if (client == null || client.base() == null) {
      I.complain("\nNO BASE-CLIENT SPECIFIED!");
      return null;
    }
    
    final float destDist = Spacing.distance(initB, destB);
    final Place placesPath[] = placesBetween(initB, destB, client, reports);
    
    if (reports && placesPath != null) {
      I.say("\nPlaces path is: ");
      for (Place p : placesPath) I.say("  "+p);
    }
    
    if (destDist <= world.patches.resolution || placesPath == null) {
      if (reports) I.say(
        "\nUsing simple agenda-bounded search between "+initB+" and "+destB
      );
      final PathSearch search = new PathSearch(initB, destB, true);
      search.assignClient(client);
      search.verbosity = verbosity;
      search.doSearch();
      path = search.fullPath(Boarding.class, maxLength);
    }
    
    if (path == null && placesPath != null) {
      if (reports) I.say(
        "\nUsing partial cordoned search between "+initB+" and "+destB
      );
      final PathSearch search = fullPathSearch(
        initB, destB, placesPath, maxLength, client, reports
      );
      search.verbosity = verbosity;
      search.doSearch();
      path = search.fullPath(Boarding.class);
    }
    
    return path;
  }
  
  
  public Tile[] placeTiles(Tile t) {
    final Place p = placeFor(t);
    return p == null ? null : p.under;
  }
  
  
  public Tile[][] placeRoutes(Tile t) {
    final Place p = tilePlaces[t.x][t.y];
    if (p == null) return null;
    final Tile tiles[][] = new Tile[p.routeList.size()][];
    int i = 0; for (Route route : p.routeList) tiles[i++] = route.path;
    return tiles;
  }
  
  
  
  /**  Then some utility methods for rapid checking of pathability between
    *  two points for a particular Base.
    */
  private boolean hasPathBetween(
    Place initP, Place destP, Accountable client, boolean reports
  ) {
    //  TODO:  This will have to be cached!  Urgently!  Somehow!
    final Place placeRoute[] = placesPath(initP, destP, false, client, reports);
    return placeRoute != null;
  }
  
  
  public boolean hasPathBetween(
    Boarding a, Boarding b, Accountable client, boolean reports
  ) {
    if (GameSettings.pathFree) return true;
    final Place
      initP = a == null ? null : placeFor(a),
      destP = b == null ? null : placeFor(b);
    if (initP == null || destP == null) return false;
    return hasPathBetween(initP, destP, client, reports);
  }
  
  
  public boolean hasPathBetween(
    Target a, Target b, Accountable client, boolean reports
  ) {
    if (GameSettings.pathFree) return true;
    if (hasPathBetween(
      PathSearch.accessLocation(a, client),
      PathSearch.accessLocation(b, client),
      client, reports
    )) return true;
    return false;
  }
  
  
  
  /**  Generating paths between nearby Places, and among the larger network of
    *  Places-
    */
  public PathSearch fullPathSearch(
    final Boarding initB, final Boarding destB,
    Object placesPathRef, final int maxLength,
    Accountable client, boolean reports
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
      placesPath = placesBetween(initB, destB, client, reports);
      if (placesPath == null) {
        if (reports) I.say("No places path exists!");
        return null;
      }
    }
    //
    //  We create a specialised form of pathing-search that always aims toward
    //  the next stop along the places-path.
    final PathSearch search = new PathSearch(initB, destB, false) {
      
      final int PPL = placesPath.length;
      private int PPI = PPL == 1 ? 0 : 1;
      private Target heading = PPL == 1 ? destB : placesPath[1].core;
      
      
      public PathSearch doSearch() {
        int index = 0;
        for (Place p : placesPath) p.flagged = index++;
        super.doSearch();
        for (Place p : placesPath) p.flagged = null;
        return this;
      }
      
      
      protected boolean endSearch(Boarding best) {
        if (super.endSearch(best)) return true;
        
        if (best instanceof Tile) {
          final Tile tile = (Tile) best;
          final Place under = placeFor(tile);
          
          if (under.flagged instanceof Integer) {
            PPI = (Integer) under.flagged;
            PPI = Nums.clamp(PPI + 1, PPL);
          }
          heading = (PPI == PPL - 1) ? destB : placesPath[PPI].core;
        }
        
        if (maxLength > 0 && pathLength(best) >= maxLength) return true;
        return false;
      }
      
      
      protected float estimate(Boarding spot) {
        float dist = Spacing.distance(spot, heading);
        dist += (PPL - (PPI + 1)) * world.patches.resolution;
        dist += Spacing.distance(spot, destB);
        return dist / 2f;
      }
      
      
      protected boolean canEnter(Boarding spot) {
        if (! super.canEnter(spot)) return false;
        final Place p = placeFor(spot);
        return p != null && p.flagged != null;
      }
      
    };
    search.assignClient(client);
    return search;
  }
  
  
  private Place[] placesPath(
    final Place init, final Place dest,
    final boolean zoneFill, final Accountable client,
    boolean reports
  ) {
    String hash = null;
    if (! zoneFill) {
      hash = init.hashCode()+"_"+dest.hashCode()+"_"+client.hashCode();
      final PlaceRoute oldRoute = placeRouteCache.get(hash);
      if (oldRoute != null && oldRoute.expireTime < world.currentTime()) {
        return oldRoute.path;
      }
    }
    
    if (reports && dest != null) {
      I.say("\nSearching for place path between "+init+" and "+dest);
    }
    if (reports && zoneFill) {
      I.say("\nSearching for places within zone starting from "+init);
    }
    
    final Search <Place> search = new Search <Place> (init, -1) {
      
      final Place tempP[] = new Place[8];
      
      protected Place[] adjacent(Place spot) {
        refreshRouteCache(spot);
        
        final Route routes[] = spot.routeCache;
        final Place near[] = routes.length > tempP.length ?
          new Place[routes.length] : tempP
        ;
        int i = 0;
        for (Route route : routes) {
          final Place n = (route.from == spot) ? route.to : route.from;
          if (PathSearch.blockedBy(n.core, client)) near[i++] = null;
          else near[i++] = n;
        }
        while (i < tempP.length) near[i++] = null;
        return near;
      }
      
      protected float cost(Place prior, Place spot) {
        for (Route r : prior.routeCache) {
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
    else {
      Place path[] = search.success() ? search.fullPath(Place.class) : null;
      if (! zoneFill) {
        final PlaceRoute route = new PlaceRoute();
        route.expireTime = world.currentTime() + ROUTE_CACHE_EXPIRY;
        route.hash = hash;
        route.path = path;
        placeRouteCache.put(hash, route);
        allPlaceRoutes.addFirst(route);
        
        while (allPlaceRoutes.size() > MAX_ROUTES_CACHED) {
          PlaceRoute dead = allPlaceRoutes.removeLast();
          placeRouteCache.remove(dead.hash);
        }
      }
      return path;
    }
  }
  
}











