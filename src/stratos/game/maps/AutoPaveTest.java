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
  
  
  
  /**  Basic code for maintaining nodes & links-
    */
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
    boolean fresh = true;
    
    public String toString() { return origin.toString(); }
  }
  
  
  class Link {
    Node from, to;
    float distanceGuess;
    Unit tiles[] = new Unit[0];
    int useCounter = 0;
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
  
  
  void deleteNode(Node node) {
    nodes.remove(node);
    for (Coord c : Visit.grid(node.area)) {
      grid[c.x][c.y].claims = null;
    }
  }
  
  
  void includeNode(Node node) {
    nodes.include(node);
    for (Coord c : Visit.grid(node.area)) {
      grid[c.x][c.y].claims = node;
    }
  }
  
  
  
  /**  Searching for routes between nodes-
    */
  Node[] getLinkedWith(Node spot) {
    if (spot.linkedNodeCache != null) return spot.linkedNodeCache;
    final Batch <Node> linked = new Batch();
    final Batch <Link> links  = new Batch();
    for (Link l : links) {
      if (l.from == spot) linked.add(l.to  );
      if (l.to   == spot) linked.add(l.from);
    }
    spot.linkCache = links.toArray(Link.class);
    return spot.linkedNodeCache = linked.toArray(Node.class);
  }
  
  
  Node[] getNearby(Node spot) {
    final List <Node> sorting = new List <Node> () {
      protected float queuePriority(Node r) { return (Float) r.flag; }
    };
    for (Node n : nodes) if (n != spot) {
      final float dist = spot.origin.axisDistance(n.origin);
      if (dist > maxLinkDist) continue;
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
      if (l.from == to || l.to == to) return l;
    }
    return null;
  }
  
  
  Node[] pathBetween(final Node init, final Node dest, final Node nearby[]) {
    if (init == dest) return null;
    final float dist = init.origin.axisDistance(dest.origin);
    if (dist > maxLinkDist) return null;
    
    if (init.fresh && dist > nodeSize && nearby.length > 1) {
      I.say("\nFinding first path between "+init+" and "+dest);
    }
    final Search <Node> agenda = new Search <Node> (init, -1) {
      
      
      protected Node[] adjacent(Node spot) {
        if (spot == init) return nearby;
        return getLinkedWith(spot);
      }
      
      
      protected float cost(Node prior, Node spot) {
        final Link oldLink = linkBetween(prior, spot);
        if (oldLink != null) return oldLink.distanceGuess;
        return prior.origin.axisDistance(spot.origin) + nodeSize;
      }
      
      
      protected float estimate(Node spot) {
        return spot.origin.axisDistance(dest.origin);
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
    //  Check to ensure no overlap with other nodes-
    for (Unit i : path) {
      if (i.claims != from && i.claims != to && i.claims != null) return null;
    }
    final Link l = new Link();
    l.from  = from;
    l.to    = to  ;
    l.tiles = path;
    
    //  TODO:  MAKE THIS BIT SEEM MORE IMPORTANT
    l.distanceGuess = from.origin.axisDistance(to.origin) - (nodeSize / 2);
    l.distanceGuess = Nums.max(0, l.distanceGuess);
    links.add(l);
    from.linkedNodeCache = null;
    to  .linkedNodeCache = null;
    return l;
  }
  
  
  private void deleteLink(Link link) {
    links.remove(link);
    link.from.linkedNodeCache = null;
    link.to  .linkedNodeCache = null;
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
      final Node from = path[i], to = path[i - 1];
      Link link = linkBetween(from, to);
      if (link == null) link = establishLink(from, to, true);
      if (link == null) continue;
      
      if (added) link.useCounter++;
      else link.useCounter--;
      if (link.useCounter == 0) deleteLink(link);
    }
  }
  
  
  void updateAllLinks() {
    for (Node node : nodes) {
      //
      //  TODO:  You want to visit the nearest neighbour for *every* node first,
      //  then get the second-nearest for each and visit those, then the third-
      //  nearest, et cetera.
      final Node nearby[] = getNearby(node);
      for (Node other : nearby) {
        final String storeKey = storeKey(node, other);
        final Node
          newPath[] = pathBetween(node, other, nearby),
          oldPath[] = pathsCache.get(storeKey);
        final String
          keyOld = pointsKey(oldPath),
          keyNew = pointsKey(newPath);
        if (! keyOld.equals(keyNew)) {
          incrementLinks(oldPath, storeKey, false);
          incrementLinks(newPath, storeKey, true);
        }
        break;
      }
      node.fresh = false;
    }
  }
  
  
  
  /**  Actual iteration-
    */
  void iterLoop() {
    final String windowName = "auto-paving test";
    final boolean clicked = I.checkMouseClicked(windowName);
    final float v[][] = screenVals;
    
    updateAllLinks();
    
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      v[c.x][c.y] = 0;
    }
    for (Link l : links) {
      for (Coord c : l.tiles) {
        v[c.x][c.y] = 0.5f;
      }
    }
    for (Node n : nodes) {
      for (Coord c : Visit.grid(n.area)) {
        v[c.x][c.y] = 1.0f;
      }
    }
    
    final Coord coords = I.getDataCursor(windowName, false);
    coords.roundToUnit(nodeSize);
    if (clicked) {
      final Node old = nodeAt(coords);
      if (old != null) deleteNode(old);
      else {
        final Node made = nodeFor(coords);
        if (isSpaceFor(made)) includeNode(made);
      }
    }
    
    I.present(screenVals, windowName, screenSize, screenSize, 0, 1);
  }
}


















