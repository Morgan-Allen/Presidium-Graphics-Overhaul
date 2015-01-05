/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.util;
import java.lang.reflect.Array;
import java.util.Iterator;



/**  In essence, a self-balancing binary tree used to maintain entries in a
  *  well-sorted order.
  */
public abstract class Sorting <K> implements Series <K> {
  
  
  
  /**  Private fields, data structures, etc-
    */
  private int size;
  private Node root;
  
  static boolean verbose = false;
  static enum Side { L, R, BOTH, NEITHER };
  
  public static class Node {
    Sorting belongs = null;
    Object value;
    
    Node parent, kidL, kidR;
    Side side;
    int height = 0;
    
    protected Node() {}
    
    protected void clear() {
      belongs = null;
      value = null;
      side = null;
      height = 0;
      parent = kidL = kidR = null;
    }
  }
  
  
  /**  Assigning children and parents-
    */
  void setParent(final Node p, final Node k, final Side s) {
    if (k != null) {
      k.side = s;
      k.parent = p;
    }
    if (p != null) {
      if (s == Side.L) p.kidL = k;
      if (s == Side.R) p.kidR = k;
    }
    else root = k;
  }
  
  
  final static Node kidFor(final Node n, final Side s) {
    if (n == null) return null;
    return (s == Side.L) ? n.kidL : n.kidR;
  }
  
  
  final static void flagForUpdate(final Node n) {
    for (Node f = n; f != null && f.height != -1; f = f.parent) {
      f.height = -1;
    }
  }
  
  
  final static int updateHeight(final Node n) {
    if (n == null) return -1;
    if (n.height != -1) return n.height;
    final int hL = updateHeight(n.kidL), hR = updateHeight(n.kidR);
    n.height = 1 + ((hL > hR) ? hL : hR);
    return n.height;
  }
  
  
  final static int heightFor(final Node n) {
    return n == null ? -1 : n.height;
  }
  
  
  //
  //  Let me draw a diagram here.  In the case of a right-rotation, we have the
  //  following setup of nodes-
  //        Y
  //       / \
  //      X   C
  //     / \
  //    A   B
  //  Which we want to transform as follows-
  //      X
  //     / \
  //    A   Y
  //       / \
  //      B   C
  //  (We then null the height of each node, thereby propagating a signal for
  //   height recalculation back up the tree to the root node.)
  //
  //  In the case of a left rotation, we simply flip the sides (see below.)
  //  However, we need to cover the exception case where the real culprit
  //  behind an imbalance is Node B (above) being longer than node A, in which
  //  case the rotation does nothing to correct the imbalance!  (Branch Y is
  //  then heavier than A, rather than X being heavier than C.)  So, we have to
  //  rotate B into X in the opposite direction first.
  
  
  void rotate(Node n, Side s, boolean first) {
    final Side o = (s == Side.L) ? Side.R : Side.L;
    final Side nS = n.side;
    final Node
      p = n.parent,
      x = kidFor(n, o),
      y = n,
      a = kidFor(x, o),
      b = kidFor(x, s),
      c = kidFor(y, s);
    if (first && heightFor(b) > heightFor(a)) {
      rotate(x, o, false);
      rotate(n, s, false);
      return;
    }
    setParent(p, x, nS);
    setParent(x, y, s);
    setParent(y, c, s);
    setParent(y, b, o);
    setParent(x, a, o);
    flagForUpdate(x);
    flagForUpdate(y);
    if (verbose) {
      I.say("\n\n...AFTER ROTATION:");
      I.say(this.toString());
    }
  }
  
  
  Node furthestBranch(Node n, Side s) {
    while (true) {
      final Node k = kidFor(n, s);
      if (k == null) return n;
      n = k;
    }
  }
  
  
  void balanceFrom(Node n) {
    if (n == null) return;
    final int
      hR = updateHeight(kidFor(n, Side.R)),
      hL = updateHeight(kidFor(n, Side.L));
    if (hR > hL + 1) rotate(n, Side.L, true);
    if (hL > hR + 1) rotate(n, Side.R, true);
    updateHeight(n);
    balanceFrom(n.parent);
  }

  
  
  /**  Public interface-
    */
  public K greatest() {
    final Object ref = greatestRef();
    return ref == null ? null : (K) ((Node) ref).value;
  }
  
  
  public K least() {
    final Object ref = leastRef();
    return ref == null ? null : (K) ((Node) ref).value;
  }
  
  
  public K first() {
    return least();
  }
  
  
  public K last() {
    return greatest();
  }
  
  
  public K removeGreatest() {
    final Object ref = greatestRef();
    if (ref == null) return null;
    deleteRef(ref);
    return (K) ((Node) ref).value;
  }
  
  
  public K removeLeast() {
    final Object ref = leastRef();
    if (ref == null) return null;
    deleteRef(ref);
    return (K) ((Node) ref).value;
  }
  

  public Object greatestRef() {
    return furthestBranch(root, Side.R);
  }
  
  
  public Object leastRef() {
    return furthestBranch(root, Side.L);
  }
  
  
  public int size() {
    return size;
  }
  
  
  public boolean containsRef(Object ref) {
    return ((Node) ref).belongs == this;
  }
  
  
  public K refValue(Object ref) {
    if (! containsRef(ref)) I.complain(
      "Querying value from wrong tree: "+((Node) ref).belongs+
      " node value: "+((Node) ref).value
    );
    return (K) ((Node) ref).value;
  }
  
  
  public void add(K k) {
    insert(k);
  }
  
  
  public Object addAsEntry(Node i) {
    i.value = i;
    i.belongs = this;
    return insertEntry(i);
  }
  
  
  public Object insert(K value) {
    final Node i = new Node();
    i.value = value;
    i.belongs = this;
    return insertEntry(i);
  }
  
  
  private Object insertEntry(Node i) {
    //
    //  We initialise the node first, based on the assumption we can always add
    //  more.  In the special case of an empty tree, assign this as the root.
    if (root == null) {
      i.side = Side.R;
      root = i;
    }
    else {
      //
      //  Otherwise, search for the first unoccupied leaf node intermediate in
      //  value between existing entries-
      Node n = root; while (true) {
        final int comp = compare((K) i.value, (K) n.value);
        final Side s = (comp > 0) ? Side.R : Side.L;
        final Node k = kidFor(n, s);
        if (k == null) { setParent(n, i, s); break; }
        else n = k;
      }
      //
      //  Finally, propagate the change as a signal up the tree structure,
      //  rebalancing as required, and return the original node.
      flagForUpdate(i);
      updateHeight(i.parent);
      balanceFrom(i.parent.parent);
    }
    size++;
    if (verbose) {
      I.say("\n\n...AFTER INSERTION:");
      I.say(this.toString());
    }
    return i;
  }
  
  
  public void deleteRef(Object ref) {
    //
    //  First, some basic sanity checks, chiefly for the sake of debugging.
    if (! (ref instanceof Node)) {
      I.complain("Invalid reference.");
    }
    final Node n = (Node) ref;
    if (n.belongs != this) {
      if (n.belongs == null) I.complain("ATTEMPTING TO REMOVE DEAD NODE!");
      else I.complain("REMOVING NODE FROM WRONG TREE!");
    }
    //
    //  In the simple case of a leaf node, we merely detach from the parent.
    if (n.height == 0) {
      setParent(n.parent, null, n.side);
      flagForUpdate(n.parent);
      balanceFrom(n.parent);
    }
    else {
      //
      //  Otherwise, we identify the right or left-branching node closest in
      //  value to the node being deleted (performing a rotation if necessary
      //  to ensure it is a leaf.  This node is at most height == 1, so only
      //  one rotation should be needed, thanks to balancing guarantees.)
      final int
        hR = heightFor(kidFor(n, Side.R)),
        hL = heightFor(kidFor(n, Side.L));
      final Side
        s = (hR > hL) ? Side.R : Side.L,
        o = (s == Side.R) ? Side.L : Side.R;
      final Node closest = furthestBranch(kidFor(n, s), o);
      if (closest.height > 0) rotate(closest, o, false);
      //
      //  Detach the replacement node from it's former parent, and install in
      //  place of the deleted node.  Rebalance from the lowest node affected.
      final Node lowest = closest.parent == n ? closest : closest.parent;
      setParent(closest.parent, null, closest.side);
      setParent(n.parent, closest, n.side);
      setParent(closest, n.kidL, Side.L);
      setParent(closest, n.kidR, Side.R);
      flagForUpdate(lowest);
      balanceFrom(lowest);
    }
    size--;
    n.belongs = null;
    if (verbose) {
      I.say("\n\n...AFTER DELETION:");
      I.say(this.toString());
    }
  }
  
  
  private Batch <Node> matchesFor(K value) {
    final Batch <Node> matches = new Batch <Node> ();
    if (root == null) return matches;
    matchFrom(value, root, matches);
    return matches;
  }
  
  
  private void matchFrom(K value, Node node, Batch <Node> matches) {
    final int comp = compare(value, (K) node.value);
    if (comp == 0) matches.add(node);
    if (node.kidL != null) {
      if (comp <= 0 || compare((K) node.kidL.value, (K) node.value) != -1) {
        matchFrom(value, node.kidL, matches);
      }
    }
    if (node.kidR != null) {
      if (comp >= 0 || compare((K) node.kidR.value, (K) node.value) !=  1) {
        matchFrom(value, node.kidR, matches);
      }
    }
  }
  
  
  public boolean includes(K value) {
    final Batch <Node> matches = matchesFor(value);
    for (Node node : matches) {
      if (compare(value, (K) node.value) == 0) return true;
    }
    return false;
  }
  
  
  public void delete(K value) {
    ///I.say("  ___Attempting deletion: "+value);
    final Batch <Node> matches = matchesFor(value);
    for (Node node : matches) {
      if (node.value == value) {
        ///I.say("  ___Succesfully deleted: "+value);
        deleteRef(node);
        return;
      }
    }
    if (size <= 100) for (K k : this) if (k == value) {
      I.complain("Match-seeking defective for: "+value);
      I.say(this.toString());
    }
    I.complain("Unable to find "+value);
  }
  
  
  public void deleteIfLive(Object ref) {
    if (! containsRef(ref)) return;
    deleteRef(ref);
  }
  
  
  public void clear() {
    root = null;
    size = 0;
  }
  
  
  
  /**  Abstract methods for override by subclasses-
    */
  public abstract int compare(K a, K b);
  
  
  
  /**  Satisfying the Series contract-
    */
  public K[] toArray(Class typeClass) {
    final Object[] array = (Object[]) Array.newInstance(typeClass, size);
    int i = 0; for (K k : this) array[i++] = k;
    return (K[]) array;
  }
  
  
  public Object[] toArray() {
    return toArray(Object.class);
  }
  

  final public Iterator <K> iterator() {
    final int height = heightFor(root);
    final Node nodeStack[] = new Node[height + 1];
    final Side sideStack[] = new Side[height + 1];
    if (root != null) {
      nodeStack[0] = root;
      sideStack[0] = Side.L;
    }
    return new Iterator <K> () {
      int level = root == null ? -1 : 0;
      Node current = getNext();
      
      Node getNext() {
        while (level >= 0) {
          final Node next = nodeStack[level];
          final Side nextSide = sideStack[level];
          if (nextSide == Side.L) {
            sideStack[level] = Side.R;
            if (next.kidL != null) {
              level++;
              nodeStack[level] = next.kidL;
              sideStack[level] = Side.L;
            }
          }
          else if (nextSide == Side.R) {
            sideStack[level] = Side.NEITHER;
            if (next.kidR != null) {
              level++;
              nodeStack[level] = next.kidR;
              sideStack[level] = Side.L;
            }
            return next;
          }
          else if (nextSide == Side.NEITHER) {
            level--;
          }
        }
        return null;
      }
      
      public boolean hasNext() {
        return current != null;
      }
      
      public K next() {
        final K returned = (K) current.value;
        current = getNext();
        return returned;
      }
      
      public void remove() {}
    };
  }
  
  
  
  /**  Testing routine.
    */
  public static void main(String args[]) {
    final Sorting <Integer> testTree = new Sorting <Integer> () {
      public int compare(Integer a, Integer b) {
        if ((int) a == (int) b) return 0;
        return a > b ? 1 : -1;
      }
    };
    final int NUM_TEST_RUNS = 100;
    final float DELETE_CHANCE = 0.5f;
    final int MIN_INSERTIONS = 100, RUN_BONUS = 5;
    //
    //  
    for (int runID = 0; runID < NUM_TEST_RUNS; runID++) {
      final int numInsertions = MIN_INSERTIONS + (runID * RUN_BONUS);
      int numDeletions = 0;
      final Stack <Object> nodeRefs = new Stack <Object> ();
      //
      //  We insert random integers onto the tree, along with a certain chance
      //  of deleting random entries.
      for (int n = numInsertions; n-- > 0;) {
        final int val = (int) (Math.random() * numInsertions);
        nodeRefs.addLast(testTree.insert(val));
        if (Math.random() < DELETE_CHANCE) {
          final int nodeIndex = (int) (Math.random() * nodeRefs.size());
          final Object gone = nodeRefs.removeIndex(nodeIndex);
          testTree.deleteRef(gone);
          numDeletions++;
        }
      }
      //
      //  We then check to ensure that all nodes are accounted for, and clear
      //  the remaining nodes-
      if (testTree.size() + numDeletions != numInsertions) {
        I.complain("INCORRECT NUMBER OF INSERTIONS/DELETIONS!");
      }
      I.add(testTree.toString());
      I.add("\n  In Sequence: ");
      for (Integer i : testTree) I.add(i+" ");
      I.add(
        "\n  RUN NO. "+runID+": "+numInsertions+
        " INSERTIONS AND "+numDeletions+" DELETIONS"
      );
      while (nodeRefs.size > 0) testTree.deleteRef(nodeRefs.removeFirst());
      if (testTree.size != 0) I.complain("DID NOT DELETE ALL NODES!");
      I.add("\n  ALL TESTS OKAY");
    }
    I.add("\n\n  ALL RUNS OKAY");
  }
  

  public String toString() {
    final StringBuffer sB = new StringBuffer();
    sB.append("\n\nTREE CONTENTS ARE:\n");
    reportNode(null, root, "\n  ", sB);
    sB.append("\n  Total Size: "+size);
    return sB.toString();
  }
  
  
  private void reportNode(
    Side s, Node node,
    String indent, StringBuffer sB
  ) {
    if (node == null) return;
    else {
      final String tick = (s == null) ? "" : ((s == Side.L) ? " \\" : " /");
      final String cross = (s == null) ?
        ">-|- " :
        ((node.height > 0)) ? "|- " : ">- ";
      reportNode(Side.R, node.kidR, indent + indentor(Side.R, node), sB);
      sB.append(indent+tick+cross+"("+node.height);
      sB.append(((node.side == Side.L) ? "L) " : "R) ")+node.value);
      reportNode(Side.L, node.kidL, indent + indentor(Side.L, node), sB);
    }
  }
  
  
  private String indentor(Side k, Node node) {
    if (node.parent == null) return "  ";
    return (k == node.side) ? "  " : "| ";
  }
}



