/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.util.*;



public class AutoPaveTest {
  

  /**  Setup functions and data fields-
    */
  public static void main(String s[]) {
    AutoPaveTest test = new AutoPaveTest();
    test.gridSize    = 40 ;
    test.screenSize  = 400;
    test.nodeSize    = 3  ;
    test.maxLinkDist = 12 ;
    test.runTest();
  }
  
  
  int gridSize, screenSize;
  float screenVals[][];
  
  Unit grid[][];
  Box2D gridArea;
  int nodeSize, maxLinkDist;
  List <Node> nodes = new List();
  List <Link> links = new List();
  Table <String, Node[]> pathsCache = new Table();
  
  
  void runTest() {
    this.screenVals = new float[gridSize][gridSize];
    
    this.gridArea = new Box2D(0, 0, gridSize, gridSize);
    this.grid     = new Unit[gridSize][gridSize];
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      (grid[c.x][c.y] = new Unit()).setTo(c);
    }
    
    while (true) {
      iterLoop();
      try { Thread.sleep(40); }
      catch (Exception e) {}
    }
  }
  
  
  
  /**  Basic definitions for nodes and links-
    */
  final static int INIT = -1, LIVE = 1, DEAD = 0;
  static int nextID = 0;
  
  class Unit extends Coord {
    Object flag;
    Node claims;
  }
  
  
  class Node {
    Unit origin;
    Box2D area = new Box2D();
    
    Node linkedNodeCache[];
    Link linkCache[];
    Object flag;
    int stage = INIT;
    
    final String ID = ""+(char) (nextID++ + 'A');
    public String toString() {
      //String coord = " ("+(origin.x / nodeSize)+"|"+(origin.y / nodeSize)+")";
      String coord ="";
      return ID+coord;
    }
  }
  
  
  class Link {
    Node from, to;
    Unit tiles[] = new Unit[0];
    int useCounter = 0;
    
    public String toString() { return from+"<->"+to; }
  }
  
  
  Node nodeFor(Coord c) {
    final Node n = new Node();
    n.origin = grid[c.x][c.y];
    n.area.set(c.x, c.y, nodeSize, nodeSize);
    return n;
  }
  
  
  Node nodeAt(Coord c) {
    for (Node n : nodes) {
      if (n.area.contains(c.x + 0.5f, c.y + 0.5f)) return n;
    }
    return null;
  }
  
  
  boolean isSpaceFor(Node node) {
    if (! node.area.containedBy(gridArea)) return false;
    for (Node n : nodes) {
      if (n.area.overlaps(node.area)) return false;
    }
    return true;
  }
  
  
  
  /**  Searching for routes between nodes-
    */
  Node[] getLinkedWith(Node spot) {
    if (spot.linkedNodeCache != null) return spot.linkedNodeCache;
    final Batch <Node> nearN = new Batch();
    final Batch <Link> nearL = new Batch();
    for (Link l : this.links) {
      if (l.from == spot) { nearN.add(l.to  ); nearL.add(l); }
      if (l.to   == spot) { nearN.add(l.from); nearL.add(l); }
    }
    spot.linkCache       = nearL.toArray(Link.class);
    spot.linkedNodeCache = nearN.toArray(Node.class);
    
    I.say("\nRefreshing node-adjacency: "+spot);
    I.say("  Links are: "+nearL);
    I.say("  Nodes are: "+nearN);
    return spot.linkedNodeCache;
  }
  
  
  Node[] getNearby(Node spot) {
    final List <Node> sorting = new List <Node> () {
      protected float queuePriority(Node r) { return (Float) r.flag; }
    };
    for (Node n : nodes) if (n != spot) {
      final float dist = spot.origin.axisDistance(n.origin);
      if (n.stage == DEAD || dist > maxLinkDist) continue;
      n.flag = dist;
      sorting.add(n);
    }
    sorting.queueSort();
    for (Node n : sorting) n.flag = null;
    return sorting.toArray(Node.class);
  }
  
  
  Link linkBetween(Node from, Node to) {
    getLinkedWith(from);
    for (Link l : from.linkCache) {
      if (l.from == to   && l.to == from) return l;
      if (l.from == from && l.to == to  ) return l;
    }
    return null;
  }
  
  
  Node[] pathBetween(
    final Node init, final Node dest, final Node nearby[], Node oldPath[]
  ) {
    if (init == dest) return null;
    final float dist = init.origin.axisDistance(dest.origin);
    if (dist > maxLinkDist) return null;
    final boolean report = oldPath == null && dist > nodeSize;
    
    if (report) {
      I.say("\nFinding first path between "+init+" and "+dest);
    }
    final Search <Node> agenda = new Search <Node> (init, -1) {
      
      
      protected Node[] adjacent(Node spot) {
        //  TODO:  This is potentially pretty explosive.  Be careful...
        Node linked[] = getLinkedWith(spot);
        Node adjacent[] = (Node[]) Visit.compose(Node.class, linked, nearby);
        return adjacent;
      }
      
      
      protected float cost(Node prior, Node spot) {
        final Link oldLink = linkBetween(prior, spot);
        final float dist = prior.origin.axisDistance(spot.origin);
        return (oldLink == null) ? dist : (dist / 2);
      }
      
      
      protected float estimate(Node spot) {
        return spot.origin.axisDistance(dest.origin) / 2f;
      }
      
      
      protected boolean endSearch(Node best) {
        return best == dest;
      }
      
      
      protected void setEntry(Node spot, Entry flag) {
        spot.flag = flag;
      }
      
      
      protected Entry entryFor(Node spot) {
        return (Entry) spot.flag;
      }
    };
    if (report) agenda.verbosity = Search.SUPER_VERBOSE;
    agenda.doSearch();
    return agenda.bestPath(Node.class);
  }
  
  
  Link establishLink(Node from, Node to, boolean xFirst) {
    //
    //  First, we determine the upper and lower limits to the path area-
    final Unit a = from.origin, b = to.origin;
    final int xd = a.x - b.x, yd = a.y - b.y;
    final Unit path[] = new Unit[Nums.abs(xd) + Nums.abs(yd) + 1];
    int index = 0;
    int minX = xd < 0 ? a.x : b.x, maxX = xd < 0 ? b.x : a.x;
    int minY = yd < 0 ? a.y : b.y, maxY = yd < 0 ? b.y : a.y;
    //
    //  Then we either trace along the x-axis, then the y-axis, or vice versa:
    if (xFirst) {
      for (int x = minX; x < maxX; x++) {
        path[index++] = grid[x][a.y];
      }
      for (int y = minY; y < maxY; y++) {
        path[index++] = grid[b.x][y];
      }
    }
    else {
      for (int y = minY; y < maxY; y++) {
        path[index++] = grid[a.x][y];
      }
      for (int x = minX; x < maxX; x++) {
        path[index++] = grid[x][b.y];
      }
    }
    path[index++] = to.origin;
    //
    //  Finally, create the link itself:
    final Link l = new Link();
    l.from  = from;
    l.to    = to  ;
    l.tiles = path;
    links.add(l);
    from.linkedNodeCache = null;
    to  .linkedNodeCache = null;
    I.say("\nESTABLISHED LINK: "+l.from+"->"+l.to);
    return l;
  }
  
  
  private void deleteLink(Link link) {
    links.remove(link);
    link.from.linkedNodeCache = null;
    link.to  .linkedNodeCache = null;
    I.say("\nDELETED LINK: "+link.from+"->"+link.to);
  }
  
  
  private String pointsKey(Node path[]) {
    if (path == null) return "none";
    String s = "";
    for (Node n : path) s+=n.toString();
    return s;
  }
  
  
  private String storeKey(Node from, Node to) {
    return from+"_to_"+to;
  }
  
  
  private void incrementLinks(Node path[], String storeKey, boolean added) {
    if (path == null) return;
    if (added) {
      I.say("\nAdding new path: "+I.list(path));
      pathsCache.put(storeKey, path);
    }
    else {
      I.say("\nRemoving old path: "+I.list(path));
      pathsCache.remove(storeKey);
    }
    
    for (int i = path.length; i-- > 1;) {
      final Node from = path[i - 1], to = path[i];
      Link link = linkBetween(from, to);
      if (added) {
        if (link == null) link = establishLink(from, to, true);
        if (link == null) continue;
        link.useCounter++;
        I.say("  "+link+" USE COUNTER: "+link.useCounter);
      }
      else {
        if (link == null) continue;
        link.useCounter--;
        I.say("  "+link+" USE COUNTER: "+link.useCounter);
        if (link.useCounter <= 0) deleteLink(link);
      }
    }
  }
  
  
  
  /**  Regular update methods-
    */
  void updateAllLinks() {
    //
    //  Prune any connections to dead nodes:
    for (Node path[] : pathsCache.values().toArray(new Node[0][0])) {
      final Node from = path[0], to = path[path.length - 1];
      if (from.stage != DEAD && to.stage != DEAD) continue;
      final String storeKey = storeKey(from, to);
      incrementLinks(path, storeKey, false);
    }
    //
    //  Then update the current nodes and their surrounding network-
    for (Node node : nodes) {
      final boolean init = node.stage == INIT, dead = node.stage == DEAD;
      if (init || dead) for (Coord c : Visit.grid(node.area)) {
        if (dead) grid[c.x][c.y].claims = null;
        if (init) grid[c.x][c.y].claims = node;
      }
      if (dead) nodes.remove(node);
      //
      //  Update connections to any nodes currently nearby:
      //
      //  TODO:  Actually, don't base the nearby-rating off absolute distance.
      //  Always grab the closest neighbour, and anything else within a similar
      //  radius (e.g, x1.5, say?)
      final Node nearby[] = getNearby(node);
      if (init) {
        I.say("\nFirst update for node: "+node);
        I.say("  Nodes nearby: "+I.list(nearby));
        node.stage = LIVE;
      }
      for (Node other : nearby) {
        final String storeKey = storeKey(node, other);
        final Node
          oldPath[] = pathsCache.get(storeKey),
          newPath[] = dead ? null : pathBetween(node, other, nearby, oldPath);
        final String
          keyOld = pointsKey(oldPath),
          keyNew = pointsKey(newPath);
        if (! keyOld.equals(keyNew)) {
          I.say("\nUpdating path between "+node+" and "+other);
          incrementLinks(oldPath, storeKey, false);
          incrementLinks(newPath, storeKey, true );
        }
      }
    }
  }
  
  
  void flagAsDead(Node node) {
    I.say("\n\nDELETING NODE AT: "+node);
    node.stage = DEAD;
    reportFullState();
    updateAllLinks();
    I.say("\nAFTER DELETION:");
    reportFullState();
  }
  
  
  void addNode(Node node) {
    I.say("\n\nADDING NODE AT: "+node);
    nodes.include(node);
    reportFullState();
    updateAllLinks();
    I.say("\nAFTER ADDITION:");
    reportFullState();
  }
  
  
  private void reportFullState() {
    I.say("All nodes: ");
    for (Node n : nodes) {
      I.say("  "+n+" : "+n.origin);
      if (n.stage == INIT) I.add(" (init)");
      if (n.stage == LIVE) I.add(" (live)");
      if (n.stage == DEAD) I.add(" (dead)");
    }
    I.say("All links: ");
    for (Link l : links) {
      I.say("  "+l);
      I.add(" (uses: "+l.useCounter+")");
    }
    I.say("All paths: ");
    for (Node path[] : pathsCache.values()) {
      I.say("  "+I.list(path));
    }
  }
  
  
  
  /**  Actual iteration-
    */
  void iterLoop() {
    final String windowName = "auto-paving test";
    final boolean clicked = I.checkMouseClicked(windowName);
    final float v[][] = screenVals;
    
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      v[c.x][c.y] = 0;
    }
    for (Link l : links) {
      for (Coord c : l.tiles) {
        v[c.x][c.y] += 0.2f;
      }
    }
    for (Node n : nodes) {
      for (Coord c : Visit.grid(n.area)) {
        v[c.x][c.y] += 0.5f;
      }
    }
    
    final Coord coords = I.getDataCursor(windowName, false);
    coords.roundToUnit(nodeSize);
    if (clicked) {
      final Node old = nodeAt(coords);
      if (old != null) flagAsDead(old);
      else {
        final Node made = nodeFor(coords);
        if (isSpaceFor(made)) addNode(made);
      }
    }
    
    I.present(screenVals, windowName, screenSize, screenSize, 0, 1);
  }
}


















