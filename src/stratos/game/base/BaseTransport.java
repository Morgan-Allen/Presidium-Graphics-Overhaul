/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;


//  TODO:  Also check if these routes are fully-paved for distribution
//  purposes.



public class BaseTransport {
  
  
  /**  Field definitions, constructor and save/load methods-
    */
  final static int PATH_RANGE = Stage.SECTOR_SIZE / 2;
  private static boolean
    paveVerbose      = false,
    distroVerbose    = false,
    checkConsistency = false,
    extraVerbose     = false;
  
  final Stage world;
  final public PavingMap map;
  protected PresenceMap junctions;
  
  Table <Tile, List <Route>> tileRoutes = new Table(1000);
  Table <Route, Route> allRoutes = new Table <Route, Route> (1000);
  
  
  public BaseTransport(Stage world) {
    this.world = world;
    this.map = new PavingMap(world, this);
    junctions = new PresenceMap(world, "junctions");
  }
  
  
  public void loadState(Session s) throws Exception {
    junctions = (PresenceMap) s.loadObject();
    map.loadState(s);
    
    int numR = s.loadInt();
    for (int n = numR; n-- > 0;) {
      final Route r = Route.loadRoute(s);
      allRoutes.put(r, r);
      toggleRoute(r, r.start, true);
      toggleRoute(r, r.end  , true);
    }
    
    provSupply = s.loadFloatArray(null);
    provDemand = s.loadFloatArray(null);
    allSupply  = s.loadFloatArray(null);
    allDemand  = s.loadFloatArray(null);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(junctions);
    map.saveState(s);
    
    s.saveInt(allRoutes.size());
    for (Route r : allRoutes.keySet()) {
      Route.saveRoute(r, s);
    }
    
    s.saveFloatArray(provSupply);
    s.saveFloatArray(provDemand);
    s.saveFloatArray(allSupply );
    s.saveFloatArray(allDemand );
  }
  
  
  public void checkConsistency() {
    //
    //  Note:  This method only works when you only have a single base in the
    //         world...
    if (! checkConsistency) return;
    
    final byte mask[][] = new byte[world.size][world.size];
    boolean okay = true;
    
    for (Route route : allRoutes.keySet()) {
      for (Tile t : route.path) mask[t.x][t.y]++;
      if (route.start == route.end) continue;
      //
      //  Check if non-perimeter routes are sane:
      Tile first = route.path[0], last = route.path[route.path.length - 1];
      final boolean
        noFirst = tileRoutes.get(first) == null,
        noLast  = tileRoutes.get(last ) == null;
      
      if (noFirst || noLast) {
        if (noFirst) I.say("NO FIRST JUNCTION");
        if (noLast ) I.say("NO LAST JUNCTION" );
        this.reportPath("  on path: ", route);
      }
    }
    
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      final int pM = mask[c.x][c.y], tM = map.roadCounter(t);
      final boolean iR = PavingMap.isRoad(t);
      final boolean flagged = map.isFlagged(t);
      
      if (pM != tM) {
        I.say("Discrepancy at: "+c+", "+pM+" =/= "+tM);
        okay = false;
      }
      if (iR != tM > 0 && ! flagged) {
        I.say("Unflagged discrepancy at "+c+", is road: "+iR+" "+tM);
        map.refreshPaving(t);
        I.say("  Flagged now? "+map.isFlagged(t));
      }
    }
    if (okay) I.say("No discrepancies in paving map found.");
  }
  
  
  
  /**  Methods related to installation, updates and deletion of junctions-
    */
  private void reportPath(String title, Route route) {
    I.add(""+title+": ");
    if (route == null || route.path == null) I.add("No path.");
    else {
      I.add("Route length: "+route.path.length+"\n  ");
      int i = 0; for (Tile t : route.path) {
        I.add(t.x+"|"+t.y+" ");
        if (((++i % 10) == 0) && (i < route.path.length)) I.add("\n  ");
      }
    }
    I.add("\n");
  }
  
  
  public void updatePerimeter(Fixture v, boolean isMember) {
    final boolean report = paveVerbose && I.talkAbout == v;
    if (report) I.say("\nUpdating perimeter for "+v);
    
    if (! isMember) {
      updatePerimeter(v, null, false, false);
      return;
    }
    final Batch <Tile> around = new Batch <Tile> ();
    for (Tile t : Spacing.perimeter(v.footprint(), world)) around.add(t);
    updatePerimeter(v, around, true, true);
  }
  

  public void updatePerimeter(
    Fixture v, Batch <Tile> around, boolean isMember, boolean filter
  ) {
    final boolean report = paveVerbose && I.talkAbout == v;
    if (report) I.say("Updating perimeter for "+v+", member? "+isMember);
    //
    //  If necessary, filter out any un-paveable tiles, and obtain the previous
    //  route corresponding to this fixture's perimeter-
    if (around != null && filter) {
      final Batch <Tile> filtered = new Batch <Tile> ();
      for (Tile t : around) if (t != null && t.canPave()) filtered.add(t);
      around = filtered;
    }
    final Tile o = v.origin();
    final Route key = new Route(o, o), match = allRoutes.get(key);
    //
    //  
    if (isMember) {
      key.path = around.toArray(Tile.class);
      key.cost = -1;
      if (key.routeEquals(match) && map.refreshPaving(key.path)) return;
      if (report) I.say("Installing perimeter for "+v);
      
      if (match != null) {
        map.flagForPaving(match.path, false);
        allRoutes.remove(match);
      }
      map.flagForPaving(key.path, true);
      allRoutes.put(key, key);
    }
    else if (match != null) {
      if (report) I.say("Discarding perimeter for "+v);
      map.flagForPaving(match.path, false);
      allRoutes.remove(key);
    }
  }
  
  
  private void deleteRoute(Route route) {
    if (route.cost < 0) return;
    map.flagForPaving(route.path, false);
    allRoutes.remove(route);
    toggleRoute(route, route.start, false);
    toggleRoute(route, route.end  , false);
    route.cost = -1;
  }
  
  
  private void toggleRoute(Route route, Tile t, boolean is) {
    List <Route> atTile = tileRoutes.get(t);
    if (atTile == null) tileRoutes.put(t, atTile = new List <Route> ());
    if (is) atTile.add(route);
    else atTile.remove(route);
    if (atTile.size() == 0) tileRoutes.remove(t);
  }
  
  
  
  /**  Methods related to distribution of provisional goods (power, water, and
    *  life support.)
    */
  final private Batch <Target> tried = new Batch <Target> (40);
  final private Stack <Target> agenda = new Stack <Target> ();
  private float
    provSupply[], provDemand[],
    allSupply [], allDemand [];
  
  
  public float allSupply(Traded t) {
    int index = Visit.indexOf(t, Economy.ALL_PROVISIONS);
    if (index == -1 || allSupply == null) return -1;
    return allSupply[index];
  }
  
  
  public float allDemand(Traded t) {
    int index = Visit.indexOf(t, Economy.ALL_PROVISIONS);
    if (index == -1 || allDemand == null) return -1;
    return allDemand[index];
  }
  
  
  private void insertAgenda(Target t) {
    if (t.flaggedWith() != null) return;
    t.flagWith(agenda);
    agenda.add(t);
    tried.add(t);
  }
  
  
  private Batch <Venue> venuesReached(Structural init, Base base) {
    if (init.flaggedWith() != null) return null;
    final boolean report = distroVerbose;
    if (report) I.say("\nDetermining provision access from "+init);
    
    final Batch <Venue> reached = new Batch <Venue> ();
    insertAgenda(init);
    final Box2D tempB = new Box2D();
    final Tile edgeB[] = new Tile[4];
    
    //  The agenda could include either tiles or structures, depending on how
    //  they are encountered.
    while (agenda.size() > 0) {
      final Target next = agenda.removeFirst();
      final List <Route> routes = tileRoutes.get(next);
      
      //  In the case of a structure, allow transmission up to 2 tiles away, or
      //  from any exit point.
      if (routes == null) {
        final Venue v = (Venue) next;
        if (v.base() != base) continue;
        reached.add(v);
        if (report) I.say("  Have reached: "+v);
        
        tempB.setTo(v.footprint()).expandBy(2 + 1);
        for (Venue c : world.claims.venuesClaiming(tempB)) {
          if (c.footprint().overlaps(tempB)) insertAgenda(c);
        }
        for (Boarding t : v.canBoard()) {
          if (tileRoutes.get(t) != null) insertAgenda(t);
        }
      }
      
      //  In the case of a road junction, add whatever structures lie at the
      //  other end of the route.
      else for (Route r : routes) {
        final Tile o = r.opposite((Tile) next);
        if (o == null) continue;
        insertAgenda(o);
        for (Tile b : o.edgeAdjacent(edgeB)) if (b != null) {
          if (b.onTop() instanceof Venue) insertAgenda((Venue) b.onTop());
        }
      }
    }
    
    //  Clean up afterwards, and return-
    for (Target t : tried) t.flagWith(null);
    tried.clear();
    agenda.clear();
    for (Structural v : reached) v.flagWith(reached);
    return reached;
  }
  
  
  private void distributeTo(Batch <Venue> reached, Traded provided[]) {
    //
    //  First, tabulate total supply and demand within the area-
    final boolean report = distroVerbose;
    if (report) I.say("\nDistributing provisions through paving network-");
    
    provSupply = new float[provided.length];
    provDemand = new float[provided.length];
    
    for (Venue s : reached) {
      if (report) I.say("  Have reached: "+s);
      
      for (int i = provided.length; i-- > 0;) {
        final Traded type = provided[i];
        
        final float in = s.structure.outputOf(type);
        if (in > 0) {
          provSupply[i] += in;
          if (report) I.say("    "+type+" supply: "+in);
        }
        
        final float out = s.stocks.demandFor(type);
        if (out > 0) {
          if (report) I.say("    "+type+" demand: "+out);
          provDemand[i] += out;
        }
      }
    }
    if (report) {
      I.say("\nSupply/demand tally complete.");
      for (int i = provided.length; i-- > 0;) {
        final Traded type = provided[i];
        final float supply = provSupply[i], demand = provDemand[i];
        if (supply + demand == 0) continue;
        I.say("  Supply|Demand of "+type+": "+supply+"|"+demand);
      }
    }
    //
    //  Then top up demand in whole or in part, depending on how much supply
    //  is available-
    for (int i = provided.length; i-- > 0;) {
      allDemand[i] += provDemand[i];
      allSupply[i] += provSupply[i];
      
      if (provDemand[i] == 0) continue;
      final Traded type = provided[i];
      float supplyRatio = Nums.clamp(provSupply[i] / provDemand[i], 0, 1);
      
      for (Venue venue : reached) {
        final float d = venue.stocks.demandFor(type);
        venue.stocks.setAmount(type, d * supplyRatio);
      }
    }
  }
  
  
  public void distributeProvisions(Base base) {
    final boolean report = distroVerbose;
    if (report) I.say("\n\nDistributing provisions for base: "+base);
    final Batch <Batch <Venue>> allReached = new Batch();
    final Traded provided[] = Economy.ALL_PROVISIONS;

    allSupply = new float[provided.length];
    allDemand = new float[provided.length];
    
    //
    //  First, divide the set of all venues into discrete partitions based on
    //  mutual paving connections-
    for (Object o : world.presences.matchesNear(base, null, -1)) {
      final Batch <Venue> reached = venuesReached((Venue) o, base);
      if (reached != null) allReached.add(reached);
    }
    //
    //  Then, distribute water/power/et cetera within that area-
    for (Batch <Venue> reached : allReached) {
      distributeTo(reached, provided);
      for (Structural v : reached) v.flagWith(null);
    }
  }
}




/*
public void updateJunction(Venue v, Tile t, boolean isMember) {
  final boolean report = paveVerbose && I.talkAbout == v;
  if (t == null) {
    if (report) I.say("CANNOT SUPPLY NULL TILE AS JUNCTION");
    return;
  }
  
  if (isMember) {
    final Batch <Tile> routesTo = new Batch <Tile> ();
    final Box2D area = new Box2D(v.areaClaimed()).expandBy(PATH_RANGE + 1);
    
    if (report) I.say("\nUpdating road junction: "+t+", Area: "+area);
    //
    //  First, we visit all registered junctions nearby, and include those
    //  for subsequent routing to-
    for (Target o : junctions.visitNear(null, -1, area)) {
      final Tile jT = (Tile) o;
      if (o == t || jT.flaggedWith() != null) continue;
      jT.flagWith(routesTo);
      routesTo.add(jT);
    }
    //
    //  We also include all nearby base venues with entrances registered as
    //  junctions.  (Any results are flagged to avoid duplicated work.)
    for (Object o : t.world.presences.matchesNear(Venue.class, v, area)) {
      final Venue n = (Venue) o;
      if (n == v || n.base() != v.base()) continue;
      final Tile jT = n.mainEntrance();
      if (jT == null || jT == t || jT.flaggedWith() != null) continue;
      if (! junctions.hasMember(jT, jT)) continue;
      jT.flagWith(routesTo);
      routesTo.add(jT);
    }
    //
    //  (NOTE:  We perform the un-flag op in a separate pass to avoid any
    //  interference with pathing-searches.)
    for (Tile jT : routesTo) jT.flagWith(null);
    updateJunction(v, t, routesTo, true);
  }
  else updateJunction(v, t, null, false);
}


public void updateJunction(
  Fixture v, Tile t, Batch <Tile> routesTo, boolean isMember
) {
  final boolean report = paveVerbose && I.talkAbout == v;
  final List <Route> oldRoutes = tileRoutes.get(t);
  final Batch <Route> toDelete = new Batch <Route> ();
  junctions.toggleMember(t, t, isMember);
  
  if (isMember) {
    //
    //  Any old routes that lack termini are assumed to be obsolete, and must
    //  be deleted-
    if (oldRoutes != null) for (Route r : oldRoutes) {
      if (! checkRouteEfficiency(r, v)) toDelete.add(r);
    }
    //
    //  Otherwise, establish routes to all the nearby junctions compiled.
    for (Tile jT : routesTo) {
      if (! checkRouteEfficiency(new Route(t, jT), v)) continue;
      if (report) I.say("  Routing to: "+jT+" ("+jT.entranceFor()+")");
      routeBetween(t, jT, report);
    }
  }
  //
  //  All routes are flagged as obsolete if the fixture is no longer a
  //  map-member.  Either way, any obsolete routes are finally deleted.
  else if (oldRoutes != null) {
    if (report) I.say("\nDeleting old routes for: "+v);
    Visit.appendTo(toDelete, oldRoutes);
  }
  for (Route r : toDelete) {
    if (report) I.say("  Discarding unused route: "+r);
    deleteRoute(r);
  }
}


private boolean checkRouteEfficiency(final Route r, Fixture f) {
  final boolean report = I.talkAbout == f && paveVerbose && extraVerbose;
  
  final Tile tempB[] = new Tile[10];
  final Search <Tile> routeSearch = new Search <Tile> (r.start, 25) {
    
    protected Tile[] adjacent(Tile spot) {
      //  We consider the opposite end-points of any routes attached to a
      //  given tile to be 'adjacent' for search purposes.
      final List <Route> routes = tileRoutes.get(spot);
      if (report && routes != null) {
        I.say("    "+routes.size()+" routes at "+spot.entranceFor());
      }
      int i = 0;
      Tile temp[] = tempB;
      if (routes != null) {
        if (routes.size() > 10) temp = new Tile[routes.size()];
        for (Route r : routes) temp[i++] = r.opposite(spot);
      }
      while (i < temp.length) temp[i++] = null;
      return tempB;
    }
    
    protected float cost(Tile prior, Tile spot) {
      //  In essence the purpose of this is to favour a series of short hops
      //  between buildings over roads that leap long distances.
      float dist = Spacing.distance(prior, spot);
      dist = dist * dist / 10;
      if (report) {
        final Object from = prior.entranceFor(), to = spot.entranceFor();
        I.say("  Getting cost between:"+from+" and "+to);
        I.say("    Cost: "+dist);
      }
      return dist;
    }
    
    protected float estimate(Tile spot) {
      //  We also use an 'optimistic' estimate of pathing costs to the end-
      //  point, so that cost-so-far is weighed more heavily than cost-to-
      //  come (and which more accurately reflects actual road-transport
      //  efficiency.)
      return Spacing.distance(spot, r.end) / 2;
    }
    
    protected void setEntry(Tile spot, Entry flag) {
      spot.flagWith(flag);
      if (report && flag != null) {
        final Object at = spot.entranceFor();
        I.say("  Total cost to "+at+": "+fullCostEstimate(spot));
      }
    }
    
    protected Entry entryFor(Tile spot) {
      return (Entry) spot.flaggedWith();
    }
    
    protected boolean endSearch(Tile best) {
      return best == r.end;
    }
  };
  
  float dist = Spacing.distance(r.start, r.end);
  final float directCost = dist * dist / 10;
  
  if (report) {
    I.say("\nChecking route efficiency between...");
    I.say("  From: "+r.start.entranceFor());
    I.say("  To:   "+r.end  .entranceFor());
  }
  routeSearch.doSearch();
  final Tile  junctions[] = routeSearch.bestPath(Tile.class);
  final float fullCost    = routeSearch.totalCost();
  final boolean bestRoute = fullCost == -1 || (fullCost + 1) > directCost;
  
  if (report) {
    I.say("  Final path:");
    for (Tile t : junctions) I.say("    "+t.entranceFor());
    I.say("  Path cost:   "+fullCost);
    I.say("  Direct cost: "+directCost);
    I.say("  Best route?  "+bestRoute);
  }
  return bestRoute;
}


private boolean routeBetween(Tile a, Tile b, boolean report) {
  if (a == b || a == null || b == null) return false;
  //
  //  Firstly, determine the correct current route.
  final Route route = new Route(a, b);
  final RoadSearch search = new RoadSearch(route.start, route.end);
  search.doSearch();
  route.path = search.fullPath(Tile.class);
  route.cost = search.totalCost();
  //
  //  If the new route differs from the old, delete it, and install the new
  //  version.  Otherwise return.
  final Route oldRoute = allRoutes.get(route);
  if (route.routeEquals(oldRoute) && map.refreshPaving(route.path)) {
    return false;
  }
  if (report) {
    I.say("Route between "+a+" and "+b+" has changed!");
    this.reportPath("Old route", oldRoute);
    this.reportPath("New route", route   );
  }
  //
  //  If the route needs an update, clear the tiles and store the data:
  if (oldRoute != null) deleteRoute(oldRoute);
  if (search.success()) {
    allRoutes.put(route, route);
    toggleRoute(route, route.start, true);
    toggleRoute(route, route.end  , true);
    map.flagForPaving(route.path, true);
  }
  else if (report) {
    I.say("Could not find route between "+a+" and "+b+"!");
  }
  return true;
}
//*/



