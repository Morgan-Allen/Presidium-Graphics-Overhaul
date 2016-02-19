/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.util.*;



//  TODO:  Ensure places aren't refreshed more than once every 5 seconds?
//         Also, direct cache-queries for Routes would be useful.


public class PathingMap {
  
  
  /**  Constituent class and constant definitions-
    */
  private static boolean updatesVerbose = false;
  
  private static int
    numRoutes = 0,
    numPlaces = 0,
    numSets   = 0,
    numZones  = 0,
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
    
    Stack <Route> routes = new Stack <Route> ();
    ListEntry zoneEntries[] = new ListEntry[Base.MAX_BASES];
    
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
  
  private class Zone extends List <Place> {
    Base client;
    boolean needsRefresh = false;
    
    Zone() { numZones++; }
    public void finalize() throws Throwable { numZones--; super.finalize(); }
  }
  
  
  final Stage world;
  final Place tilePlaces[][];
  final PlaceSet placeSets[][];
  final List <PlaceSet> needRefresh = new List();
  
  
  public PathingMap(Stage world) {
    this.world      = world;
    this.tilePlaces = new Place[world.size][world.size];
    final int gridSize = world.patches.gridSize;
    this.placeSets  = new PlaceSet[gridSize][gridSize];
  }
  
  
  public void initMap() {
    for (StagePatch p : world.patches.allGridPatches()) {
      refreshWithNeighbours(p);
    }
    updateMap();
  }
  
  
  public void flagForUpdateAt(Tile tile) {
    for (Tile at : tile.vicinity(null)) if (at != null) {
      final Place p = placeFor(at, false);
      final PlaceSet set = p == null ? null : placeSets[p.region.x][p.region.y];
      if (set != null && ! set.needsRefresh) {
        if (updatesVerbose) {
          I.say("\nWILL REFRESH PLACES AT "+set.places[0].region);
          I.say("  First flagged by: "+at+" ("+at.above()+")");
        }
        set.needsRefresh = true;
        needRefresh.add(set);
      }
    }
  }
  
  
  public void updateMap() {
    while (needRefresh.size() > 0 && ! world.schedule.timeUp()) {
      final PlaceSet set = needRefresh.removeFirst();
      refreshWithNeighbours(set.places[0].region);
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
    
    for (StagePatch n : near) if (n != null) refreshPlaces(n);
    for (StagePatch n : near) if (n != null) refreshRoutesBetween(region, n);
  }
  
  
  private void refreshPlaces(StagePatch region) {
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
      
      if (updatesVerbose) I.say("\nCREATED NEW PLACE: "+p);
    }
    newSet.places = places.toArray(Place.class);
    placeSets[region.x][region.y] = newSet;
    //
    //  Finally, we check each place generated to see how well it matches any
    //  predecessor (in which case it might either retain the same Zone or
    //  discard them.)
    int placeIndex = Nums.max(
      newSet == null ? 0 : newSet.places.length,
      oldSet == null ? 0 : oldSet.places.length
    );
    while (placeIndex-- > 0) checkPlacesMatch(oldSet, newSet, placeIndex);
  }
  
  
  private void checkPlacesMatch(PlaceSet oldSet, PlaceSet newSet, int index) {
    //
    //  TODO:  Consider doing an automatic refresh if the old and new number of
    //  places is different?  Might be simpler/safer...
    final Place n = (newSet == null || index >= newSet.places.length) ?
      null : newSet.places[index]
    ;
    final Place o = (oldSet == null || index >= oldSet.places.length) ?
      null : oldSet.places[index]
    ;
    //
    //  Any Places that have a different pattern of connections to their
    //  neighbours (compared to the previous set) will prompt a refresh for
    //  any associated Zones.  Otherwise, they retain the old batch.
    boolean routesMatch = true;
    if ((n == null) || (o == null) || (n.routes.size() != o.routes.size())) {
      routesMatch = false;
    }
    else for (int r = 0; r < o.routes.size(); r++) {
      final Route rN   = n.routes.atIndex(r);
      final Route rO   = o.routes.atIndex(r);
      final Place oppN = rN.from == n ? rN.to : rN.from;
      final Place oppO = rO.from == o ? rO.to : rO.from;
      if (oppN != oppO) routesMatch = false;
    }
    if (updatesVerbose) {
      if (routesMatch) I.say("\nROUTES ARE IDENTICAL, WILL RETAIN ZONE/S");
      else I.say("\nDIFFERENT ROUTES CREATED, WILL REFRESH ZONES/S");
    }
    //
    //  We delete all zone-entries for the older place-set, but if the routes
    //  matched, we immediately register the newer set instead:
    if (o != null) for (int b = o.zoneEntries.length; b-- > 0;) {
      final ListEntry e = o.zoneEntries[b];
      if (e == null) continue;
      final Zone z = (Zone) e.list();
      z.removeEntry(e);
      if (routesMatch) n.zoneEntries[b] = z.addLast(n);
      else             z.needsRefresh = true;
    }
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
        numLiveRoutes--;
      }
      for (Tile u : place.under) tilePlaces[u.x][u.y] = null;
      numLivePlaces--;
    }
  }
  
  
  private void refreshRoutesBetween(StagePatch a, StagePatch b) {
    final PlaceSet setA = placeSets[a.x][a.y], setB = placeSets[b.x][b.y];
    
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
          final Place p = placeFor(spot, false);
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
  public Place placeFor(Boarding spot, boolean refresh) {
    if (spot.boardableType() == Boarding.BOARDABLE_TILE) {
      return placeFor((Tile) spot, refresh);
    }
    else {
      return placeFor(((Element) spot).origin(), refresh);
    }
  }
  
  
  private Place placeFor(Tile t, boolean refresh) {
    if (refresh) {
      refreshWithNeighbours(world.patches.patchAt(t.x, t.y));
    }
    return tilePlaces[t.x][t.y];
  }
  
  
  private Place[] placesBetween(
    Boarding initB, Boarding destB, Accountable client, boolean reports
  ) {
    final Place
      initP = placeFor(initB, true),
      destP = placeFor(destB, true);
    
    if (initP == null || destP == null) {
      if (reports) I.say("Initial places invalid: "+initP+"/"+destP);
      return null;
    }
    if (! hasPathBetween(initP, destP, client.base(), reports)) {
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
    
    final float destDist     = Spacing.distance(initB, destB);
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
      if (path != null) return path;
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
      if (path != null) return path;
    }
    
    return null;
  }
  
  
  public Tile[] placeTiles(Tile t) {
    final Place p = placeFor(t, true);
    return p == null ? null : p.under;
  }
  
  
  public Tile[][] placeRoutes(Tile t) {
    refreshWithNeighbours(world.patches.patchAt(t.x, t.y));
    final Place p = tilePlaces[t.x][t.y];
    if (p == null) return null;
    final Tile tiles[][] = new Tile[p.routes.size()][];
    int i = 0; for (Route route : p.routes) tiles[i++] = route.path;
    return tiles;
  }
  
  
  
  /**  Then some utility methods for rapid checking of pathability between
    *  two points for a particular Base.
    */
  private Zone zoneFor(
    Place init, Base client, boolean refresh, boolean reports
  ) {
    final int       baseID  = client.baseID();
    final ListEntry entry   = init.zoneEntries[baseID];
    final Zone      oldZone = entry == null ? null : (Zone) entry.list();
    
    if (oldZone != null && ! oldZone.needsRefresh) return oldZone;
    if (! refresh) return null;
    
    if (updatesVerbose) {
      I.say("\nCREATING NEW ZONE FOR "+client+" (ID "+client.baseID()+")");
    }
    final int evalBefore = numTilesScanned + numTilesRouted;

    if (oldZone != null) {
      for (Place p : oldZone) p.zoneEntries[baseID] = null;
      oldZone.clear();
    }
    final Place inZone[] = placesPath(init, null, true, client, reports);
    
    final int evalAfter = numTilesScanned + numTilesRouted;
    maxInQuery = Nums.max(maxInQuery, evalAfter - evalBefore);
    
    final Zone zone = new Zone();
    zone.client = client;
    for (Place p : inZone) p.zoneEntries[baseID] = zone.addLast(p);
    
    if (updatesVerbose) {
      I.say("\nDONE CREATING NEW ZONE FOR "+client);
      for (Place p : inZone) I.say("  "+p);
      I.say("  Total places:    "+inZone.length);
      I.say("  Tiles evaluated: "+(evalAfter - evalBefore));
    }
    return zone;
  }
  
  
  private boolean hasPathBetween(
    Place initP, Place destP, Base client, boolean reports
  ) {
    final Zone
      initZ = zoneFor(initP, client, true, reports),
      destZ = zoneFor(destP, client, true, reports);
    return initZ != null && destZ != null && initZ == destZ;
  }
  
  
  public boolean hasPathBetween(
    Boarding a, Boarding b, Base client, boolean reports
  ) {
    if (GameSettings.pathFree) return true;
    final Place
      initP = a == null ? null : placeFor(a, true),
      destP = b == null ? null : placeFor(b, true);
    if (initP == null || destP == null) return false;
    return hasPathBetween(initP, destP, client, reports);
  }
  
  
  public boolean hasPathBetween(
    Target a, Target b, Mobile client, boolean reports
  ) {
    if (GameSettings.pathFree) return true;
    return hasPathBetween(
      PathSearch.accessLocation(a, client),
      PathSearch.accessLocation(b, client),
      client.base(), reports
    );
  }
  
  
  public Boarding[] compileZoneCoresFor(Base client, Tile at) {
    
    final Place place = placeFor(at, false);
    if (place == null) return new Boarding[0];
    final Zone zone = zoneFor(place, client, true, false);
    if (zone == null) return new Boarding[0];
    
    final Batch <Boarding> cores = new Batch();
    for (Place p : zone) cores.add(p.core);
    return cores.toArray(Boarding.class);
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
          final Place under = placeFor(tile, false);
          
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
        final Place p = placeFor(spot, false);
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
    if (reports && dest != null) {
      I.say("\nSearching for place path between "+init+" and "+dest);
    }
    if (reports && zoneFill) {
      I.say("\nSearching for places within zone starting from "+init);
    }
    
    final Search <Place> search = new Search <Place> (init, -1) {
      
      final Place tempP[] = new Place[8];
      
      protected Place[] adjacent(Place spot) {
        //if (! zoneFill)
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





