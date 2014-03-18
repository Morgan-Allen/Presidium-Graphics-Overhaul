/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package code.util ;


public abstract class QuadTree {
  
  protected Node
    nodes[][][] ;
  int
    size,
    depth ;
  
  protected abstract Node newNode() ;
  
  protected final static Node NO_KIDS[] = {} ;
  
  
  public QuadTree(int minSize) {
    size = depth = 1 ;
    while (size < minSize) { size *= 2 ; depth++ ; }
    nodes = new Node[depth][][] ;
    for (int d = 0, s = size ; d < depth ; d++, s /= 2) {
      nodes[d] = new Node[s][s] ;
      for (int x = 0 ; x < s ; x++)
        for (int y = 0 ; y < s ; y++)
          (nodes[d][x][y] = newNode()).setup(this, x, y, d) ;
    }
  }
  
  final public Node root() { return nodes[depth - 1][0][0] ; }
  
  final public Node nodeAt(final int d, final float x, final float y) {
    try { return nodes[d][(int) x][(int) y] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  public void applyDescent(Descent descent) {
    descent.descendTo(root()) ;
  }
  
  
  public abstract static class Descent <T extends Node> {
    
    protected abstract boolean enterNode(final T node) ;
    protected abstract void processNode(final T node) ;
    protected abstract void processLeaf(final T node) ;
    
    void descendTo(final Node node) {
      final T n = (T) node ;
      if (! enterNode(n))
        return ;
      processNode(n) ;
      if (n.deep == 0) processLeaf(n) ;
      else for (int k = n.kids.length ; k-- > 0 ;)
        descendTo(n.kids[k]) ;
    }
  }
  
  
  public abstract static class Node {
    
    protected Node
      parent,
      kids[] = NO_KIDS ;
    protected int
      x, y, deep ;
    protected QuadTree
      tree ;
    
    final public int xpos() { return x ; }
    final public int ypos() { return x ; }
    final public int depth() { return deep ; }
    final public Node parent() { return parent ; }
    final public Node[] kids() { return kids ; }
    
    public abstract void updateNode() ;
    
    void setup(final QuadTree tree, final int x, final int y, final int deep) {
      this.x = x ; this.y = y ; this.deep = deep ;
      this.tree = tree ;
      if (deep > 0) {
        kids = new Node[4] ;
        kids[0] = tree.nodes[deep - 1][x * 2][y * 2] ;
        kids[1] = tree.nodes[deep - 1][(x * 2) + 1][y * 2] ;
        kids[2] = tree.nodes[deep - 1][x * 2][(y * 2) + 1] ;
        kids[3] = tree.nodes[deep - 1][(x * 2) + 1][(y * 2) + 1] ;
        for (Node kid : kids)
          kid.parent = this ;
      }
    }
  }
}
