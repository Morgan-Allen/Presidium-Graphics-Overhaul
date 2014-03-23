/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.util.*;



public class Paving {
  
  
  /**  Field definitions, constructor and save/load methods-
    */
  final static int PATH_RANGE = World.SECTOR_SIZE / 2 ;
  private static boolean verbose = false, checkConsistency = false ;
  
  final World world ;
  PresenceMap junctions ;
  
  Table <Tile, List <Route>> tileRoutes = new Table(1000) ;
  Table <Route, Route> allRoutes = new Table <Route, Route> (1000) ;
  
  
  
  
  public Paving(World world) {
    this.world = world ;
    junctions = new PresenceMap(world, "junctions") ;
  }
  
  
  public void loadState(Session s) throws Exception {
    junctions = (PresenceMap) s.loadObject() ;
    
    int numR = s.loadInt() ;
    for (int n = numR ; n-- > 0 ;) {
      final Route r = Route.loadRoute(s) ;
      allRoutes.put(r, r) ;
      toggleRoute(r, r.start, true) ;
      toggleRoute(r, r.end  , true) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(junctions) ;
    
    s.saveInt(allRoutes.size()) ;
    for (Route r : allRoutes.keySet()) {
      Route.saveRoute(r, s) ;
    }
  }
  
  
  public void checkConsistency() {
    //
    //  Note:  This method only works when you only have a single base in the
    //         world...
    if (! checkConsistency) return ;
    
    //I.say("CHECKING PAVING CONSISTENCY:") ;
    
    final byte mask[][] = new byte[world.size][world.size] ;
    boolean okay = true ;
    
    for (Route route : allRoutes.keySet()) {
      for (Tile t : route.path) mask[t.x][t.y]++ ;
      if (route.start == route.end) continue ;
      //
      //  Check if non-perimeter routes are sane:
      Tile first = route.path[0], last = route.path[route.path.length - 1] ;
      final boolean
        noFirst = tileRoutes.get(first) == null,
        noLast  = tileRoutes.get(last ) == null ;
      
      if (noFirst || noLast) {
        if (noFirst) I.say("NO FIRST JUNCTION") ;
        if (noLast ) I.say("NO LAST JUNCTION" ) ;
        this.reportPath("  on path: ", route) ;
      }
    }
    
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y) ;
      final int pM = mask[c.x][c.y], tM = world.worldTerrain().roadMask(t) ;
      if (pM != tM) {
        I.say("Discrepancy at: "+c.x+" "+c.y+", "+pM+" =/= "+tM) ;
        okay = false ;
      }
    }
    //if (okay) I.say("No discrepancies in paving map found.") ;
  }
  
  
  
  
  /**  Methods related to installation, updates and deletion of junctions-
    */
  private void reportPath(String title, Route path) {
    I.add(""+title+": ") ;
    if (path == null) I.add("No path.") ;
    else {
      I.add("Route length: "+path.path.length+"\n  ") ;
      int i = 0 ; for (Tile t : path.path) {
        I.add(t.x+"|"+t.y+" ") ;
        if (((++i % 10) == 0) && (i < path.path.length)) I.add("\n  ");
      }
    }
    I.add("\n") ;
  }
  

  public void updatePerimeter(
    Fixture v, Batch <Tile> around, boolean isMember
  ) {
    final Tile o = v.origin() ;
    final Route key = new Route(o, o), match = allRoutes.get(key) ;
    
    if (isMember) {
      key.path = around.toArray(Tile.class) ;
      key.cost = -1 ;
      if (roadsEqual(key, match)) return ;
      //I.say("Installing perimeter for "+v) ;
      
      if (match != null) {
        world.worldTerrain().maskAsPaved(match.path, false) ;
        allRoutes.remove(match) ;
      }
      world.worldTerrain().maskAsPaved(key.path, true) ;
      clearRoad(key.path) ;
      allRoutes.put(key, key) ;
    }
    else if (match != null) {
      //I.say("Discarding perimeter for "+v) ;
      //reportPath("Old route", match) ;
      world.worldTerrain().maskAsPaved(match.path, false) ;
      allRoutes.remove(key) ;
    }
  }
  
  
  public void updatePerimeter(Fixture v, boolean isMember) {
    if (isMember) {
      final Batch <Tile> around = new Batch <Tile> () ;
      for (Tile t : Spacing.perimeter(v.area(), world)) if (t != null) {
        if (t.owningType() <= Element.ELEMENT_OWNS) around.add(t) ;
      }
      updatePerimeter(v, around, true) ;
    }
    else updatePerimeter(v, null, false) ;
  }
  
  
  public void updateJunction(Venue v, Tile t, boolean isMember) {
    if (t == null) {
      return ;
      //I.complain("CANNOT SUPPLY NULL TILE AS JUNCTION") ;
    }
    junctions.toggleMember(t, t, isMember) ;
    if (isMember) {
      ///I.say("Updating road junction "+t) ;
      for (Target o : junctions.visitNear(t, PATH_RANGE + 1, null)) {
        final Tile jT = (Tile) o ;
        routeBetween(t, jT) ;
      }
    }
    else {
      ///I.say("Discarding junctions for "+v) ;
      final List <Route> routes = tileRoutes.get(t) ;
      if (routes != null) {
        for (Route r : routes) {
          if (r.cost < 0) continue ;
          deleteRoute(r) ;
          r.cost = -1 ;
        }
      }
    }
  }
  
  
  private boolean routeBetween(Tile a, Tile b) {
    if (a == b) return false ;
    //
    //  Firstly, determine the correct current route.
    //  TODO:  Allow the road search to go through arbitrary Boardables, and
    //  screen out any non-tiles or blocked tiles?
    final Route route = new Route(a, b) ;
    final RoadSearch search = new RoadSearch(
      route.start, route.end, Element.FIXTURE_OWNS
    ) ;
    search.doSearch() ;
    route.path = search.fullPath(Tile.class) ;
    route.cost = search.totalCost() ;
    //
    //  If the new route differs from the old, delete it.  Otherwise return.
    final Route oldRoute = allRoutes.get(route) ;
    if (roadsEqual(route, oldRoute)) return false ;
    
    if (verbose) {
      I.say("Route between "+a+" and "+b+" has changed!") ;
      this.reportPath("Old route", oldRoute) ;
      this.reportPath("New route", route   ) ;
    }
    
    //
    //  If the route needs an update, clear the tiles and store the data.
    if (oldRoute != null) deleteRoute(oldRoute) ;
    if (search.success()) {
      allRoutes.put(route, route) ;
      toggleRoute(route, route.start, true) ;
      toggleRoute(route, route.end  , true) ;
      world.worldTerrain().maskAsPaved(route.path, true) ;
      clearRoad(route.path) ;
    }
    return true ;
  }
  
  
  private void deleteRoute(Route route) {
    world.worldTerrain().maskAsPaved(route.path, false) ;
    allRoutes.remove(route) ;
    toggleRoute(route, route.start, false) ;
    toggleRoute(route, route.end  , false) ;
  }
  
  
  private boolean roadsEqual(Route newRoute, Route oldRoute) {
    if (newRoute.path == null || oldRoute == null) return false ;
    boolean match = true ;
    for (Tile t : newRoute.path) t.flagWith(newRoute) ;
    int numMatched = 0 ;
    for (Tile t : oldRoute.path) {
      //if (t.blocked()) I.say("TILE BLOCKED AT: "+t) ;
      if (t.flaggedWith() != newRoute) {
        match = false ;
        break ;
      }
      else numMatched++ ;
    }
    for (Tile t : newRoute.path) t.flagWith(null) ;
    if (numMatched != newRoute.path.length) match = false ;
    return match ;
  }
  
  
  private void toggleRoute(Route route, Tile t, boolean is) {
    List <Route> atTile = tileRoutes.get(t) ;
    if (atTile == null) tileRoutes.put(t, atTile = new List <Route> ()) ;
    if (is) atTile.add(route) ;
    else atTile.remove(route) ;
    if (atTile.size() == 0) tileRoutes.remove(t) ;
  }
  
  
  
  /**  Methods related to physical road construction-
    */
  private void clearRoad(Tile path[]) {
    if (verbose) I.say("Clearing path...");
    for (Tile t : path) {
      if (verbose) I.say("Owner of "+t+" is "+t.owner());
      if (t.owningType() < Element.FIXTURE_OWNS) {
        if (t.owner() != null) t.owner().setAsDestroyed();
      }
    }
  }
  
  
  
  /**  Methods related to distribution of provisional goods-
    */
  //
  //  TODO:  See if there's any way you can make the provision less...
  //         distracting.  It keeps flickering on and off.
  
  
  //
  //  TODO:  YOU NEED TO RESTRICT THIS TO PARTICULAR BASES?  Or maybe you
  //  could share, given suitable alliance status?
  final private Batch <Target> tried = new Batch <Target> () ;
  final private Stack <Target> agenda = new Stack <Target> () ;
  
  
  private void insertA(Target t) {
    if (t.flaggedWith() != null) return ;
    t.flagWith(agenda) ;
    agenda.add(t) ;
    tried.add(t) ;
  }
  
  
  private Batch <Venue> venuesReached(Venue init) {
    if (init.flaggedWith() != null) return null ;
    final Batch <Venue> reached = new Batch <Venue> () ;
    agenda.add(init) ;
    
    while (agenda.size() > 0) {
      final Target next = agenda.removeFirst() ;
      final List <Route> routes = tileRoutes.get(next) ;
      
      if (routes == null) {
        final Venue v = (Venue) next ;
        reached.add(v) ;
        for (Tile t : Spacing.perimeter(v.area(), world)) if (t != null) {
          if (t.owner() instanceof Venue) insertA(t.owner()) ;
          else if (tileRoutes.get(t) != null) insertA(t) ;
        }
      }
      
      else for (Route r : routes) {
        final Tile o = r.end == next ? r.start : r.end ;
        if (o == null || o.flaggedWith() != null) continue ;
        insertA(o) ;
        for (Tile a : o.allAdjacent(Spacing.tempT8)) if (a != null) {
          if (a.owner() instanceof Venue) insertA(a.owner()) ;
        }
      }
    }
    //
    //  Clean up afterwards, and return-
    for (Target t : tried) t.flagWith(null) ;
    tried.clear() ;
    agenda.clear() ;
    for (Venue v : reached) v.flagWith(reached) ;
    return reached ;
  }
  
  
  private void distributeTo(Batch <Venue> reached, Service provided[]) {
    //
    //  First, tabulate total supply and demand within the area-
    float
      supply[] = new float[provided.length],
      demand[] = new float[provided.length] ;
    for (Venue venue : reached) {
      for (int i = provided.length ; i-- > 0 ;) {
        final Service type = provided[i] ;
        supply[i] += venue.stocks.amountOf(type) ;
        final float shortage = venue.stocks.shortageOf(type) ;
        if (shortage > 0) demand[i] += shortage ;
      }
    }
    //
    //  Then top up demand in whole or in part, depending on how much supply
    //  is available-
    for (int i = provided.length ; i-- > 0 ;) {
      if (demand[i] == 0) continue ;
      final Service type = provided[i] ;
      final float supplyRatio = Visit.clamp(supply[i] / demand[i], 0, 1) ;
      for (Venue venue : reached) {
        final float shortage = venue.stocks.shortageOf(type) ;
        venue.stocks.bumpItem(type, shortage * supplyRatio) ;
      }
    }
  }
  

  public void distribute(Service provided[]) {
    final Batch <Batch <Venue>> allReached = new Batch <Batch <Venue>> () ;
    //
    //  First, divide the set of all venues into discrete partitions based on
    //  mutual paving connections-
    final Tile at = world.tileAt(0, 0) ;
    for (Object o : world.presences.matchesNear(Venue.class, at, -1)) {
      final Batch <Venue> reached = venuesReached((Venue) o) ;
      if (reached != null) allReached.add(reached) ;
    }
    //
    //  Then, distribute water/power/et cetera within that area-
    for (Batch <Venue> reached : allReached) {
      distributeTo(reached, provided) ;
      for (Venue v : reached) v.flagWith(null) ;
    }
  }
}

