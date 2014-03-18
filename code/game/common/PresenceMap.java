/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.game.common ;
import java.util.Iterator ;
//import java.util.TreeSet ;
//import java.util.Comparator ;

import code.game.building.*;
import code.game.common.WorldSections.Section;
import code.util.*;



public class PresenceMap implements Session.Saveable {  //Do not make Saveable.
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    MAX_VISITED = 100 ;
  
  final Object key ;  //  Move this stuff to the Presences class.
  final World world ;
  final Node root ;
  
  static class Node extends List {
    final Section section ;
    int population = 0 ;
    Node(Section s) { this.section = s ; }
  }
  
  private static final class NodeMarker {
    Object node ;
    float distance ;
    boolean leaf ;
  }
  
  private NodeMarker markers[] = initMarkers() ;
  private int markUseIndex = -1 ;
  
  private Vec3D temp = new Vec3D() ;
  
  
  
  public PresenceMap(World world, Object key) {
    this.world = world ;
    this.root = new Node(world.sections.root) ;
    //
    //  Check to ensure you've been given a valid key-
    boolean keyOkay = false ;
    if (key instanceof String) keyOkay = true ;
    if (key instanceof Class) keyOkay = true ;
    if (key instanceof Session.Saveable) keyOkay = true ;
    if (key instanceof Service) keyOkay = true ;
    if (! keyOkay) I.complain("INVALID FLAGGING KEY: "+key) ;
    this.key = key ;
  }
  
  
  private NodeMarker[] initMarkers() {
    markers = new NodeMarker[MAX_VISITED] ;
    for (int n = MAX_VISITED ; n-- > 0 ;) markers[n] = new NodeMarker() ;
    return markers ;
  }
  
  
  public PresenceMap(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.world() ;
    
    final int keyType = s.loadInt() ;
    if (keyType == 0) key = s.loadClass() ;
    else if (keyType == 1) {
      key = s.loadString() ;
      ///I.say("Loading presence map with key: "+key) ;
    }
    else key = s.loadObject() ;
    //
    //  Load the root node from disk-
    root = new Node(world.sections.root) ;
    final int numLoad = s.loadInt() ;
    for (int n = numLoad ; n-- > 0 ;) loadMember(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    if (key instanceof Class) {
      s.saveInt(0) ;
      s.saveClass((Class) key) ;
    }
    if (key instanceof String) {
      s.saveInt(1) ;
      s.saveString((String) key) ;
      ///I.say("Saving presence map with key: "+key) ;
    }
    if (key instanceof Session.Saveable) {
      s.saveInt(2) ;
      s.saveObject((Session.Saveable) key) ;
    }
    //
    //  Save the root node to disk-
    s.saveInt(root.population) ;
    saveNode(root, s) ;
  }
  
  
  private void loadMember(Session s) throws Exception {
    final int pX = s.loadInt(), pY = s.loadInt() ;
    final Target t = s.loadTarget() ;
    if (! t.inWorld()) I.say(t+" NOT IN WORLD! "+this.key) ;
    else toggleAt(root, pX, pY, t, true) ;
  }
  
  
  private void saveNode(Node n, Session s) throws Exception {
    final boolean leaf = n.section.depth == 0 ;
    for (Object k : n) if (k != null) {
      if (leaf) {
        //
        //  TODO:  Later, if the section resolution goes down to the tile,
        //         you will need to refine this.
        final Box2D b = n.section.area ;
        temp.set(b.xpos() + 1, b.ypos() + 1, 0) ;
        s.saveInt((int) b.xpos() + 1) ;
        s.saveInt((int) b.ypos() + 1) ;
        s.saveTarget((Target) k) ;
      }
      else saveNode((Node) k, s) ;
    }
  }
  
  
  public int population() {
    return root.population ;
  }
  
  
  
  /**  Inserting and deleting members-
    */
  /*
  public void toggleMember(Target t, boolean is) {
    t.position(temp) ;
    toggleAt(root, (int) temp.x, (int) temp.y, t, is) ;
  }
  //*/
  //
  //  NOTE:  This method should ONLY be used if you are very confident that the
  //  target in question either is or immediately WILL be at the given tile.
  public void toggleMember(Target t, Tile at, boolean is) {
    if (at == null) return ;
    toggleAt(root, at.x, at.y, t, is) ;
  }
  
  
  protected void printSectionFor(Target t, Node n) {
    if (n == null) {
      n = root ;
    }
    if (n.section.depth == 0) {
      if (n.includes(t)) {
        I.say("  "+key+" FOUND SECTION MATCHES FOR "+t) ;
        I.say("    SECTION AT "+n.section.area) ;
      }
    }
    else for (Object k : n) printSectionFor(t, (Node) k) ;
  }
  
  
  private void toggleAt(Node n, int x, int y, Target t, boolean is) {
    if (n.section.depth == 0) {
      final int oldPop = n.size() ;
      if (is) n.include(t) ;
      else if (n.size() > 0) {
        n.remove(t) ;
      }
      n.population += n.size() - oldPop ;
    }
    else {
      Section worldKid = null ; for (Section k : n.section.kids) {
        if (k.area.contains(x, y)) { worldKid = k ; break ; }
      }
      Node nodeKid = null ; for (Object o : n) {
        final Node k = (Node) o ;
        if (k.section == worldKid) { nodeKid = k ; break ; }
      }
      if (nodeKid == null) {
        if (is) n.add(nodeKid = new Node(worldKid)) ;
        else return ;
      }
      toggleAt(nodeKid, x, y, t, is) ;
      if (nodeKid.size() == 0) n.remove(nodeKid) ;
      
      n.population = 0 ;
      for (Object k : n) n.population += ((Node) k).population ;
    }
  }
  
  
  
  /**  Iterating through members-
    */
  final private NodeMarker nextMarker(
    final Object n, final boolean leaf, final Tile origin
  ) {
    final NodeMarker m = markers[markUseIndex++] ;
    m.node = n ;
    m.leaf = leaf ;
    m.distance = leaf ?
      Spacing.distance(origin, (Target) n) :
      ((Node) n).section.area.distance(origin.x, origin.y) ;
    return m ;
  }
  
  
  public Iterable <Target> visitNear(
    final Tile origin, final float range, final Box2D area
  ) {
    markUseIndex = 0 ;
    final Sorting <NodeMarker> agenda = new Sorting <NodeMarker> () {
      public int compare(NodeMarker a, NodeMarker b) {
        if (a.node == b.node) return 0 ;
        return a.distance < b.distance ? 1 : -1 ;
      }
    } ;
    agenda.add(nextMarker(root, false, origin)) ;
    //
    //  Then, define a method of iterating over these entries, and return it-
    final class nearIter implements Iterator, Iterable {
      
      private Object next = nextTarget() ;
      
      private Object nextTarget() {
        ///int numI = 0 ;
        while (agenda.size() > 0) {
          //
          //  We obtain the next entry in the agenda-
          final Object ref = agenda.greatestRef() ;
          final NodeMarker marker = agenda.refValue(ref) ;
          //final Object nearest = entry.node ;
          agenda.deleteRef(ref) ;
          ///numI++ ;
          //
          //  If it's not a node, return this.  Otherwise, add the children of
          //  the node to the agenda.  Reject anything out of range.
          if (range > 0 && marker.distance > range) continue ;
          if (marker.leaf) {
            if (area != null) {
              ((Target) marker.node).position(temp) ;
              if (! area.contains(temp.x, temp.y)) continue ;
            }
            return marker.node ;
          }
          else {
            final Node node = (Node) marker.node ;
            if (area != null) {
              final Box2D b = node.section.area ;
              if (! area.intersects(b)) continue ;
            }
            final boolean leaf = node.section.depth == 0 ;
            for (Object o : node) if (o != null) {
              if (markUseIndex >= MAX_VISITED) return null ;
              agenda.add(nextMarker(o, leaf, origin)) ;
            }
          }
        }
        return null ;
      }
      
      public boolean hasNext() {
        return next != null ;
      }
      
      public Object next() {
        final Object target = next ;
        next = nextTarget() ;
        return target ;
      }
      
      public void remove() {}
      public Iterator iterator() { return this ; }
    }
    return new nearIter() ;
  }
  
  
  public Target pickNearest(Target origin, float range) {
    final Tile o = world.tileAt(origin) ;
    for (Target t : visitNear(o, range, null)) return t ;
    return null ;
  }
  
  
  public Target pickRandomAround(final Target origin, final float range) {
    origin.position(temp) ;
    final int oX = (int) temp.x, oY = (int) temp.y ;
    Node node = root ;
    while (true) {
      //
      //  For a given node level, iterate across all children and calculate the
      //  probability of visiting those.
      final Section quadKids[] = node.section.kids ;
      final boolean leaf = node.section.depth == 0 ;
      float weights[] = new float[node.size()], sumWeights = 0 ;
      int i = 0 ; for (Object k : node) {
        final float dist, pop ;
        if (leaf) {
          dist = Spacing.distance((Target) k, origin) ;
          pop = 1 ;
        }
        else {
          dist = ((Node) k).section.area.distance(oX, oY) ;
          pop = ((Node) k).population ;
        }
        if (range > 0 && dist > range) continue ;
        sumWeights += weights[i++] =
          pop * World.PATCH_RESOLUTION / (World.PATCH_RESOLUTION + dist) ;
      }
      //
      //  If no child is a valid selection, quit.  Otherwise, choose one child
      //  at random.
      if (sumWeights == 0) return null ;
      final float roll = Rand.num() * sumWeights ;
      int kidIndex = -1 ; sumWeights = 0 ;
      do { sumWeights += weights[++kidIndex] ; } while (sumWeights < roll) ;
      //
      //  If it's a leaf, return it.  Otherwise, move down the next level.
      if (leaf) return (Target) node.getEntryAt(kidIndex).refers ;
      node = (Node) node.getEntryAt(kidIndex).refers ;
    }
  }
}













