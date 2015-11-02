/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.economic.*;
import stratos.game.wild.Flora;
import stratos.util.*;

import java.util.Iterator;



public class PresenceMap implements Session.Saveable {
  //TODO:  Do not make Saveable.
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean verbose = false;
  
  final static int DISTANCE_UNIT = Stage.PATCH_RESOLUTION;
  
  final Object key;  //  TODO:  Move this stuff to the Presences class(?)
  final Stage world;
  final Node root;
  
  static class Node extends List {
    final StageRegion section;
    int population = 0;
    Node(StageRegion s) { this.section = s; }
  }
  
  
  
  public PresenceMap(Stage world, Object key) {
    this.world = world;
    this.root = new Node(world.regions.root);
    //
    //  Check to ensure you've been given a valid key-
    boolean keyOkay = false;
    if (key instanceof String) keyOkay = true;
    if (key instanceof Class) keyOkay = true;
    if (key instanceof Session.Saveable) keyOkay = true;
    if (key instanceof Traded) keyOkay = true;
    if (! keyOkay) I.complain("INVALID FLAGGING KEY: "+key);
    this.key = key;
  }
  
  
  public PresenceMap(Session s) throws Exception {
    s.cacheInstance(this);
    world = s.world();
    key = s.loadkey();
    //
    //  Load the root node from disk-
    root = new Node(world.regions.root);
    final int numLoad = s.loadInt();
    for (int n = numLoad; n-- > 0;) loadMember(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveKey(key);
    s.saveInt(root.population);
    saveNode(root, s);
  }
  
  
  private void loadMember(Session s) throws Exception {
    final int pX = s.loadInt(), pY = s.loadInt();
    final Target t = s.loadTarget();
    if (! t.inWorld()) I.say(t+" NOT IN WORLD! "+this.key);
    else toggleAt(root, pX, pY, t, true);
  }
  
  
  private void saveNode(Node n, Session s) throws Exception {
    final boolean leaf = n.section.depth == 0;
    for (Object k : n) if (k != null) {
      if (leaf) {
        //
        //  TODO:  Later, if the section resolution goes down to the tile,
        //         you will need to refine this.
        final Box2D b = n.section.area;
        //temp.set(b.xpos() + 1, b.ypos() + 1, 0);
        s.saveInt((int) b.xpos() + 1);
        s.saveInt((int) b.ypos() + 1);
        s.saveTarget((Target) k);
      }
      else saveNode((Node) k, s);
    }
  }
  
  
  public int population() {
    return root.population;
  }
  
  
  
  /**  Inserting and deleting members-
    */
  //
  //  NOTE:  This method should ONLY be used if you are very confident that the
  //  target in question either is or immediately WILL be at the given tile.
  public void toggleMember(Target t, Tile at, boolean is) {
    if (at == null) return;
    toggleAt(root, at.x, at.y, t, is);
  }
  
  
  public boolean hasMember(Target t, Tile at) {
    return presentAt(root, at.x, at.y, t);
  }
  
  
  private boolean presentAt(Node n, int x, int y, Target t) {
    if (n.section.depth == 0) {
      return n.includes(t);
    }
    final Node kid = kidAround(n, x, y);
    if (kid == null || kid.population == 0) return false;
    return presentAt(kid, x, y, t);
  }
  
  
  protected void printSectionFor(Target t, Node n) {
    if (n == null) {
      n = root;
    }
    if (n.section.depth == 0) {
      if (n.includes(t)) {
        I.say("  "+key+" FOUND SECTION MATCHES FOR "+t);
        I.say("    SECTION AT "+n.section.area);
      }
    }
    else for (Object k : n) printSectionFor(t, (Node) k);
  }
  
  
  private void toggleAt(Node n, int x, int y, Target t, boolean is) {
    if (n.section.depth == 0) {
      final int oldPop = n.size();
      if (is) {
        n.include(t);
      }
      else if (n.size() > 0) {
        n.remove(t);
      }
      n.population += n.size() - oldPop;
    }
    else {
      Node nodeKid = kidAround(n, x, y);
      
      if (nodeKid == null) {
        if (is) {
          StageRegion worldKid = null;
          for (StageRegion k : n.section.kids) if (k.area.contains(x, y)) {
            worldKid = k;
            break;
          }
          n.add(nodeKid = new Node(worldKid));
        }
        else return;
      }
      toggleAt(nodeKid, x, y, t, is);
      if (nodeKid.size() == 0) n.remove(nodeKid);
      
      n.population = 0;
      for (Object k : n) n.population += ((Node) k).population;
    }
  }
  
  
  private Node kidAround(Node parent, int x, int y) {
    for (ListEntry e = parent; (e = e.nextEntry()) != parent;) {
      final Node kid = (Node) e.refers;
      if (kid.section.area.contains(x, y)) return kid;
    }
    return null;
  }
  
  
  
  /**  Private utility methods for iterating efficiently through members:
    */
  //  TODO:  Get rid of this.  Ideally, you want to be able to save/load
  //  iteration-state instead, which means the possibility of multiple
  //  iterations existing at once, and for that to work you can't have any
  //  static references.
  
  final static int MARKER_CACHE_SIZE = 100;
  
  //
  //  I'm extending Sorting.Node directly here and caching some objects
  //  persistently in order to save on allocation time.
  //  TODO:  Try something similar for pathing?
  private static final class NodeMarker extends Sorting.Node {
    Object node;
    float distance;
    boolean leaf;
    
    protected void clear() { super.clear(); node = null; }
  }
  
  private NodeMarker markers[] = initMarkers();
  private int markUseIndex = 0;
  private static Vec3D temp = new Vec3D();
  
  
  private NodeMarker[] initMarkers() {
    markers = new NodeMarker[MARKER_CACHE_SIZE];
    for (int n = MARKER_CACHE_SIZE; n-- > 0;) markers[n] = new NodeMarker();
    return markers;
  }
  
  
  final private NodeMarker nextMarker(
    final Object n, final boolean leaf, final Tile origin
  ) {
    final NodeMarker m = (markUseIndex >= MARKER_CACHE_SIZE) ?
      new NodeMarker()       :
      markers[markUseIndex++];
    m.node = n;
    m.leaf = leaf;
    m.distance = heuristic(n, leaf, origin);
    return m;
  }
  
  
  final private void clearMarkers() {
    while (markUseIndex > 0) markers[--markUseIndex].clear();
  }
  
  
  final private static float heuristic(
    final Object n, final boolean leaf, final Tile origin
  ) {
    return origin == null ? (DISTANCE_UNIT + 0) : (leaf ?
      Spacing.distance(origin, (Target) n) :
      ((Node) n).section.area.distance(origin.x, origin.y)
    );
  }
  
  
  final private static boolean checkArea(
    final Object n, final boolean leaf, final Box2D area
  ) {
    if (area == null) return true;
    if (leaf) return area.contains(((Target) n).position(temp));
    else      return area.intersects(((Node) n).section.area);
  }
  
  
  private Tile tileAt(Target origin) {
    if (origin instanceof Tile) return (Tile) origin;
    return origin == null ? null : world.tileAt(origin);
  }
  
  
  
  /**  Visits all map-targets within a the given range and area (if those 
    *  are specified.)  Arguments may also be null, or -1 for range.
    *  
    *  NOTE:  This method is *NOT* thread-friendly, and in fact should only
    *  be used in strict sequence- finish one iteration before you even attempt
    *  to perform another.
    */
  public Iterable <Target> visitNear(
    final Target origin, final float range, final Box2D area
  ) {
    final Tile oT = tileAt(origin);
    final boolean checkRange = origin != null && range > 0;
    clearMarkers();
    
    final Sorting <NodeMarker> agenda = new Sorting <NodeMarker> () {
      public int compare(NodeMarker a, NodeMarker b) {
        if (a.node == b.node) return 0;
        return a.distance < b.distance ? 1 : -1;
      }
    };
    agenda.addAsEntry(nextMarker(root, false, oT));
    //
    //  Then, define a method of iterating over these entries, and return it-
    final class nearIter implements Iterator, Iterable {
      
      private Object next = nextTarget();
      
      private Object nextTarget() {
        while (true) {
          if (agenda.size() == 0) return null;
          //
          //  We obtain the next entry in the agenda-
          final NodeMarker marker = (NodeMarker) agenda.greatestRef();
          agenda.deleteRef(marker);
          final boolean leaf = marker.leaf;
          //
          //  If it's not a node, return this.  Otherwise, add the children of
          //  the node to the agenda.  Reject anything out of range.
          if (checkRange && marker.distance > range) continue;
          if (! checkArea(marker.node, leaf, area) ) continue;
          if (leaf) return marker.node;
          
          final Node node = (Node) marker.node;
          final boolean kidLeaf = node.section.depth == 0;
          for (Object o : node) {
            if (o != null) agenda.addAsEntry(nextMarker(o, kidLeaf, oT));
          }
        }
      }
      
      public boolean hasNext() {
        if (next == null) { clearMarkers(); return false; }
        return true;
      }
      
      public Object next() {
        final Object target = next;
        next = nextTarget();
        return target;
      }
      
      public void remove() {}
      public Iterator iterator() { return this; }
    }
    return new nearIter();
  }
  
  
  public int samplePopulation(Target origin, float range) {
    final Tile o = tileAt(origin);
    int p = 0;
    for (Target t : this.visitNear(o, range, null)) p++;
    return p;
  }
  
  
  public Target pickNearest(Target origin, float range) {
    final Tile o = tileAt(origin);
    for (Target t : visitNear(o, range, null)) return t;
    return null;
  }
  
  
  
  /**  Returns a random map-target within the given range and area (if those 
    *  are specified.)  Arguments may also be null, or -1 for range.
    */
  public Target pickRandomAround(
    final Target origin, final float range, final Box2D area
  ) {
    final boolean report = verbose && I.talkAbout == origin;
    if (report) I.say("\nPicking random target around "+origin);
    
    final boolean checkRange = origin != null && range > 0;
    final Tile oT = tileAt(origin);
    Node node = root;
    
    while (true) {
      final boolean leaf = node.section.depth == 0;
      float weights[] = new float[node.size()], sumWeights = 0;
      if (report) I.say("  "+node.size()+" kids from "+node.hashCode());
      //
      //  For a given node level, iterate across all children and calculate the
      //  probability of visiting those.
      int i = 0; for (Object k : node) {
        final float dist = heuristic(k, leaf, oT);
        final int   pop  = leaf ? 1 : ((Node) k).population;
        final float rating;
        if      (checkRange && dist > range) rating = 0;
        else if (! checkArea(k, leaf, area)) rating = 0;
        else rating = pop / (1 + (dist / DISTANCE_UNIT));
        sumWeights += weights[i++] = rating;
        if (report) {
          I.say("  Rating for "+I.tagHash(k)+" is "+rating);
          I.say("    Distance:  "+dist+", population: "+pop);
        }
      }
      //
      //  If no child is a valid selection, quit.  Otherwise, choose one child
      //  at random.
      if (sumWeights == 0) return null;
      final float roll = Rand.num() * sumWeights;
      int kidIndex = -1; sumWeights = 0;
      do { sumWeights += weights[++kidIndex]; } while (sumWeights < roll);
      //
      //  If it's a leaf, return it.  Otherwise, move down the next level.
      final Object next = node.getEntryAt(kidIndex).refers;
      if (leaf) return (Target) next;
      node = (Node) next;
    }
  }
}




