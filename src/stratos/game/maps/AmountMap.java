

package stratos.game.maps;
import stratos.game.common.*;
import stratos.util.*;


//  I want to replace:
//  *  Stocks-based supply/demand adjustment
//  *  The Presence-Maps and FadingMaps
//  *  The ecology-species-maps
//  *  The danger & ambience maps
//  *  The terrain-type-maps
//  *  The flag-based MipMap for paving, etc.

//  The supply/demand adjustment (and various others) will also, arguably, need
//  to be applied to offworld sectors or rival bases, which makes things a
//  little more elaborate.

//  (And you'll need to keep supply/demand levels specific to venues, for the
//  same of micro-economic decision-making.)


public class AmountMap {
  
  
  private class Node {
    Node parent;
    Entry first;
    int deep;
    Box2D area;
  }
  
  private class Entry {
    Object refers;
    int amount;
    Entry next;
  }
  
  
  int maxDeep;
  Box2D fullArea;
  Node root = null;
  
  
  
  
  
  
  private Entry entryFor(Object refers, Node node, boolean create) {
    for (Entry next = node.first; next != null; next = next.next) {
      if (next.refers == refers) return next;
    }
    if (! create) return null;
    final Entry entry = new Entry();
    entry.refers = refers;
    entry.next = node.first;
    node.first = entry;
    return entry;
  }
  
  
  private void removeEntryFor(Object refers, Node node) {
    
    Entry prior = null, next = node.first;
    while (next != null) {
      if (next.refers == refers) {
        if (prior == null) node.first = next.next;
        else prior.next = next.next;
        break;
      }
      prior = next;
      next = next.next;
    }
    
    if (node.first == null) {
      if (node == root) root = null;
      else removeEntryFor(node, node.parent);
    }
  }
  
  
  private Node createKid(Node parent, Coord around) {
    
    Node kid = new Node();
    kid.deep   = parent == null ? maxDeep : parent.deep - 1;
    kid.area   = new Box2D();
    kid.parent = parent;
    
    final int size = 1 << kid.deep;
    if (parent == null) {
      kid.area.setTo(fullArea);
      this.root = kid;
    }
    else {
      for (int n = 4; n-- > 0;) {
        kid.area.asQuadrant(parent.area, size, n / 2, n % 2);
        if (kid.area.contains(around.x, around.y)) break;
      }
      entryFor(kid, parent, true);
    }
    
    return kid;
  }
  
  
  private Node getNodeAt(Coord at, boolean create) {
    Node node = root, parent = null;
    
    while (true) {
      if (node == null) {
        if (create) node = createKid(parent, at);
        else return null;
      }
      
      if (node.deep == 0) return node;
      
      Entry nextEntry = node.first;
      parent = node;
      node = null;
      
      for (Entry next = parent.first; next != null; next = next.next) {
        final Node kid = (Node) nextEntry.refers;
        if (kid.area.contains(at.x, at.y)) { node = kid; break; }
      }
    }
  }
  
  
  
  
  
  public void setAmount(Object key, int amount, Coord at) {
    if (amount > 0) {
      final Node node = getNodeAt(at, true);
      final Entry entry = entryFor(key, node, true);
      entry.amount = amount;
    }
    else {
      final Node node = getNodeAt(at, false);
      if (node == null) return;
      final Entry entry = entryFor(key, node, false);
      if (entry == null) return;
      removeEntryFor(key, node);
    }
  }
  
  
  public int getAmount(Object key, Coord at) {
    final Node node = getNodeAt(at, false);
    if (node == null) return 0;
    final Entry entry = entryFor(key, node, false);
    if (entry == null) return 0;
    return entry.amount;
  }
  
  
  //  TODO:  You also need those iteration-methods for getting random and
  //  nearby samples!  (And ideally, something for getting a 'blurry sample' as
  //  well.)
  
  
}










