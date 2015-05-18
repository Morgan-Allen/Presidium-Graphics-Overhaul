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
import stratos.user.BaseUI;
import stratos.util.*;


//  TODO:  Use a single one of these for the entire world?

public class BaseTransport {
  
  
  /**  Field definitions, constructor and save/load methods-
    */
  final static int PATH_RANGE = Stage.ZONE_SIZE / 2;
  private static boolean
    paveVerbose      = false,
    distroVerbose    = false,
    checkConsistency = false;
  
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
  
  
  
  /**  Debugging methods:
    */
  public void checkConsistency() {
    //
    //  NOTE:  This method only works with a single base in the world!
    if (! checkConsistency) return;
    I.say("\nChecking consistency for paving map...");
    
    final byte mask[][] = new byte[world.size][world.size];
    boolean okay = true;
    
    for (Route route : allRoutes.keySet()) {
      boolean routeOkay = true;
      for (Tile t : route.path) {
        mask[t.x][t.y]++;
        if (! t.canPave()) {
          I.say("  Should not pave at "+t+"!");
          routeOkay = false;
          okay = false;
        }
      }
      final boolean
        noStart = ! checkEndpoint(route.start),
        noEnd   = ! checkEndpoint(route.end  );
      if (noStart || noEnd) {
        if (noStart) I.say("  START POINT INVALID");
        if (noEnd  ) I.say("  END   POINT INVALID");
        routeOkay = false;
      }
      if (! routeOkay) this.reportPath("\n  Unsuitable route", route);
    }
    
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      final int pM = mask[c.x][c.y], tM = map.roadCounter(t);
      if (pM != tM) {
        I.say("  Discrepancy at: "+c+", "+pM+" =/= "+tM);
        okay = false;
      }
    }
    if (okay) I.say("No discrepancies in paving map found.");
  }
  

  private void reportPath(String title, Route route) {
    I.add(""+title+": ");
    if (route == null || route.path == null) { I.add("No path.\n"); return; }
    
    Target atBeg = route.start.entranceFor().first();
    if (atBeg == null) atBeg = route.start.onTop();
    Target atEnd = route.end.entranceFor().first();
    if (atEnd == null) atEnd = route.end.onTop();
    
    I.add("Route length: "+route.path.length+"\n  ");
    int i = 0; for (Tile t : route.path) {
      I.add(t.x+"|"+t.y+" ");
      if (((++i % 10) == 0) && (i < route.path.length)) I.add("\n  ");
    }
    I.add("\n  Starts: "+atBeg+"  Ends: "+atEnd);
    
    I.add("\n");
  }
  
  
  
  /**  Methods related to installation, updates and deletion of junctions-
    */
  public void updatePerimeter(Fixture v, boolean isMember) {
    final boolean report = paveVerbose && I.talkAbout == v;
    if (report) I.say("\nUpdating perimeter for "+v);
    if (isMember) {
      final Batch <Tile> around = new Batch <Tile> ();
      for (Tile t : Spacing.perimeter(v.footprint(), world)) around.add(t);
      updatePerimeter(v, true, around);
    }
    else updatePerimeter(v, false, null);
  }
  

  public void updatePerimeter(
    Fixture v, boolean isMember, Batch <Tile> around
  ) {
    final boolean report = paveVerbose && I.talkAbout == v;
    if (report) I.say("Updating perimeter for "+v+", member? "+isMember);
    //
    //  Update the route in question, deleting the old one if no longer needed.
    final Tile o = v.origin();
    final Route after = new Route(o, o), prior = allRoutes.get(after);
    if (isMember && around != null) {
      final Batch <Tile> filtered = new Batch <Tile> ();
      for (Tile t : around) if (t != null && t.canPave()) filtered.add(t);
      after.path = filtered.toArray(Tile.class);
      after.cost = -1;
      updateRoute(prior, after);
      if (report) reportPath("\n  Perimeter is: ", after);
    }
    else if (prior != null) disownRoute(prior);
  }
  
  
  public void updateJunction(Venue v, Tile t, boolean isMember) {
    //
    //  Basic sanity checks and setup-
    if (t == null) return;
    final boolean report = paveVerbose && I.talkAbout == v;
    final List <Route> fromTile = tileRoutes.get(t);
    //
    //  In essence, we visit every nearby venue and try to path toward either
    //  their main entrance or their centre.
    if (isMember) {
      final Box2D area = new Box2D(v.footprint()).expandBy(PATH_RANGE);
      if (report) I.say("\nUpdating junction for "+v+" ("+t+")");
      
      for (Object o : t.world.presences.matchesNear(Venue.class, v, area)) {
        final Venue n = (Venue) o;
        if (n == v || n.base() != v.base()) continue;
        Tile aims = n.mainEntrance();
        if (n.blueprint.isFixture()) aims = world.tileAt(n);
        else if (aims == null) continue;
        if (report) I.say("  Will try route to "+n+" ("+aims+")");
        
        final Route
          key   = new Route(t, aims),
          prior = allRoutes.get(key),
          after = getAxisPath(key, v, n);
        
        if (updateRoute(prior, after)) {
          if (report) reportPath("\n  Success!", after);
        }
      }
    }
    //
    //  Any routes that have not been actively routed to are assumed to be
    //  redundant.
    if (fromTile != null) for (Route r : fromTile) {
      if ((! isMember) || (! checkRouteValid(r))) {
        disownRoute(r);
        if (report) reportPath("\n  Discarding old route", r);
      }
    }
  }
  
  
  private Route getAxisPath(Route route, Venue vA, Venue vB) {
    //
    //  We have to be a little picky about the route taken, or you can wind up
    //  with ugly results:
    final Route
      alongX = getAxisPath(route, vA, vB, true ),
      alongY = getAxisPath(route, vA, vB, false);
    if (alongX == null || alongY == null) return null;
    return alongY;
  }
  
  
  private Route getAxisPath(Route route, Venue vA, Venue vB, boolean xFirst) {
    //
    //  First, we determine the upper and lower limits to the path area-
    final Tile a = route.start, b = route.end;
    final int xd = a.x - b.x, yd = a.y - b.y;
    Tile path[] = new Tile[Nums.abs(xd) + Nums.abs(yd) + 1];
    int index = 0;
    int minX = xd < 0 ? a.x : b.x, maxX = xd < 0 ? b.x : a.x;
    int minY = yd < 0 ? a.y : b.y, maxY = yd < 0 ? b.y : a.y;
    //
    //  Then we either trace along the x-axis, then the y-axis, or vice versa:
    if (xFirst) {
      for (int x = minX; x < maxX; x++) {
        path[index++] = world.tileAt(x, a.y);
      }
      for (int y = minY; y < maxY; y++) {
        path[index++] = world.tileAt(b.x, y);
      }
    }
    else {
      for (int y = minY; y < maxY; y++) {
        path[index++] = world.tileAt(a.x, y);
      }
      for (int x = minX; x < maxX; x++) {
        path[index++] = world.tileAt(x, b.y);
      }
    }
    path[index++] = route.end;
    //
    //  Any intermediate tiles that are occupied by something *other* than the
    //  origin and destination venue flag the route as invalid.  Otherwise
    //  return okay.
    final Batch <Tile> filtered = new Batch <Tile> ();
    while (index-- > 0) {
      final Tile under = path[index];
      if (under.canPave()) filtered.add(under);
      else if (under.onTop() != vA && under.onTop() != vB) return null;
    }
    route.path = filtered.toArray(Tile.class);
    route.cost = route.path.length;
    return route;
  }
  
  
  private boolean updateRoute(Route prior, Route after) {
    if (after == null) return false;
    if (after.routeEquals(prior) && map.refreshPaving(after.path)) return true;
    if (prior != null) disownRoute(prior);
    map.flagForPaving(after.path, true);
    allRoutes.put(after, after);
    toggleRoute(after, after.start, true);
    toggleRoute(after, after.end  , true);
    after.refCount++;
    return true;
  }
  
  
  private boolean checkRouteValid(Route r) {
    for (Tile t : r.path) if (! t.canPave()) return false;
    return checkEndpoint(r.start) && checkEndpoint(r.end);
  }
  
  
  private boolean checkEndpoint(Tile t) {
    return t.isEntrance() || t.onTop() instanceof Venue;
  }
  
  
  private void disownRoute(Route route) {
    route.refCount--;
    if (route.refCount > 0) return;
    map.flagForPaving(route.path, false);
    allRoutes.remove(route);
    toggleRoute(route, route.start, false);
    toggleRoute(route, route.end  , false);
    route.cost = -1;
  }
  
  
  private void toggleRoute(Route route, Tile t, boolean is) {
    if (t == null) return;
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
    final boolean report = distroVerbose && base == BaseUI.currentPlayed();
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
  
  
  private void distributeTo(
    Batch <Venue> reached, Traded provided[], Base base
  ) {
    //
    //  First, tabulate total supply and demand within the area-
    final boolean report = distroVerbose && BaseUI.currentPlayed() == base;
    Venue lastRep = null;
    if (report) I.say("\nDistributing provisions through paving network-");
    
    provSupply = new float[provided.length];
    provDemand = new float[provided.length];
    
    for (Venue s : reached) {
      for (int i = provided.length; i-- > 0;) {
        final Traded type = provided[i];
        final float in  = s.stocks.demandFor(type, false);
        final float out = s.stocks.demandFor(type, true );
        if (report && (in > 0 || out > 0) && lastRep != s) {
          I.say("  Have reached: "+s);
          lastRep = s;
        }
        if (in > 0) {
          provSupply[i] += in;
          if (report) I.say("    "+type+" supply: "+in);
        }
        if (out > 0) {
          provDemand[i] += out;
          if (report) I.say("    "+type+" demand: "+out);
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
      
      final Traded type = provided[i];
      float supplyRatio = Nums.clamp(provSupply[i] / provDemand[i], 0, 1);
      
      for (Venue venue : reached) {
        final float d = venue.stocks.demandFor(type);
        if (d == 0) continue;
        if (venue.stocks.producer(type)) venue.stocks.setAmount(type, d);
        else venue.stocks.setAmount(type, d * supplyRatio);
      }
    }
  }
  
  
  public void distributeProvisions(Base base) {
    if (base == BaseUI.currentPlayed()) checkConsistency();
    
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
      distributeTo(reached, provided, base);
      for (Structural v : reached) v.flagWith(null);
    }
  }
}



//  TODO:  There might still be some use for this.  See later.

/*

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
//*/



