/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;
import java.lang.reflect.Array;
import java.util.Iterator;


/**  This class essentially implements a doubly-linked list, the distinction
  *  being that each insertion returns an entry referencing that new element's
  *  place in the list.  This entry, if kept, then allows for deletion in constant
  *  time.
  *  The other distinction (albeit largely internal) is that the 'head' and
  *  'tail' nodes of the list are identical, and in fact represented by the
  *  list object itself.
  */
public class List <T> extends ListEntry <T> implements Series <T> {
  
  int size;
  
  
  /**  Returns an array with identical contents to this List- unless the list
    *  has zero elements- in which case null is returned.
    */
  final public T[] toArray(Class typeClass) {
    final Object[] array = (Object[]) Array.newInstance(typeClass, size);
    int i = 0; for (T t : this) array[i++] = t;
    return (T[]) array;
  }
  
  final public Object[] toArray() {
    return toArray(Object.class);
  }
  
  final public void add(final T r) { addLast(r); }
  
  
  /**  Adds the given member at the head of the list.
    */
  final public ListEntry <T> addFirst(final T r) {
    return new ListEntry <T> (r, this, this, next);
  }
  
  /**  Adds the given member at the tail of the list.
    */
  final public ListEntry <T> addLast(final T r) {
    return new ListEntry <T> (r, this, last, this);
  }
  
  /**  Adds all the given members at the head of the list.
    */
  final public void addFirst(final T[] r) {
    for (int n = r.length; n-- > 0;) addFirst(r[n]);
  }
  
  /**  Adds all the given members at the tail of the list.
    */
  final public void addLast(final T[] r) {
    for(T t : r) addLast(t);
  }
  
  /**  Removes the first element from this list.
    */
  final public T removeFirst() {
    return removeEntry(next).refers;
  }
  
  /**  Removes the first element from this list.
    */
  final public T removeLast() {
    return removeEntry(last).refers;
  }

  /**  Returns the size of this list.
    */
  final public int size() {
    return size;
  }
  
  /**  Adds all the given list's elements to this list's own, starting from the
    *  front.  Order is preserved.  The argument list is cleared.
    */
  final public void mergeAll(final List <T> c) {
    for (ListEntry <T> l = c; (l = l.next) != c;) l.list = this;
    couple(c.last, next);
    couple(this, c.next);
    size += c.size;
    c.clear();
  }
  
  /**  Returns the entry at the specified place in the list.
    */
  final public ListEntry <T> getEntryAt(final int n) {
    int i = 0;
    for (ListEntry <T> l = this; (l = l.next) != this; i++)
      if (i == n) return l;
    return null;
  }
  
  
  /**  Return the index of the given entry-
    */
  final public int indexOf(T t) {
    int i = 0;
    for (ListEntry <T> l = this; (l = l.next) != this; i++)
      if (t == l.refers) return i;
    return -1;
  }
  
  
  /**  Returns the member at the specified index-
    */
  final public T atIndex(int i) {
    if (i < 0 || i >= size) return null;
    int d = 0;
    for (ListEntry <T> l = this; (l = l.next) != this; d++)
      if (d == i) return l.refers;
    return null;
  }
  
  
  
  /**  Removes the specified entry from the list.  (This method has no effect if
    *  the given entry does not belong to the list.)
    */
  final public ListEntry <T> removeEntry(final ListEntry <T> l) {
    if ((l == null) || (l.list != this)) {
      //I.r("Invalid list entry!");
      return null;
    }
    couple(l.last, l.next);
    //
    //  I actually don't want this:  "l.last = l.next = null;"
    //  ...Because then I can't interate and remove at the same time!
    l.list = null;
    size--;
    return l;
  }
  
  /**  Adds the specified element directly after the given entry.  (This method
    *  has no effect if the given entry does not belong to the list.)
    */
  final public ListEntry <T> addAfter(final ListEntry <T> l, final T r) {
    if ((l == null) || (l.list != this)) return null;
    return new ListEntry <T> (r, this, l, l.next);
  }
  
  /**  Empties this list of all members.
    */
  final public void clear() {
    for (ListEntry <T> l = this; (l = l.next) != this;)
      couple(l.last, l.next);
    size = 0;
  }
  
  /**  Returns an exact copy of this list.
    */
  final public List <T> copy() {
    final List <T> list = new List <T> ();
    for (T t : this) list.addFirst(t);
    return list;
  }
  
  /**  Finds the entry matching the given element, if present.  If more than one
    *  exists, only the first is returned.
    */
  final public ListEntry <T> match(final T r) {
    for (ListEntry <T> l = this; (l = l.next) != this;)
      if (l.refers == r)
        return l;
    return null;
  }
  
  
  /**  Returns whether the given element is present in the list.
    */
  final public boolean includes(final T r) {
    return match(r) != null;
  }
  
  
  /**  Includes the given element in this list if not already present.
    */
  final public void include(final T r) {
    if (match(r) == null) addLast(r);
  }
  
  
  /**  Discards the given element from the list if present- if included more than
    *  once, only the first is returned.
    */
  final public void remove(final T r) {
    removeEntry(match(r));
  }
  
  
  final public T first() {
    return next.refers;
  }
  
  
  final public T last() {
    return last.refers;
  }
  
  
  /**  This method is intended for override by subclasses.  Like it says, it
    *  returns the queue priority of a given list element.  It isn't abstract,
    *  because you might not need it, and it's default return value is zero.
    */
  protected float queuePriority(final T r) {
    return 0;
  }
  
  
  /**  Adds the given element while maintaining queue priority (descending
    *  order) within the list.
    */
  final public ListEntry <T> queueAdd(final T r) {
    ListEntry <T> l = this;
    while ((l = l.next) != this) {
      if (queuePriority(r) > queuePriority(l.refers)) break;
    }
    return new ListEntry <T> (r, this, l.last, l);
  }
  
  
  
  /**  Sorts this list's entries from lowest to highest queue priority.
    */
  final public void queueSort() {
    final boolean verbose = false;
    sortListFrom(next, last, size, verbose);
    if (verbose) I.say("FINAL SORTING: "+this+"\n\n");
  }
  
  
  private ListEntry <T> sortListFrom(
    ListEntry <T> head, ListEntry <T> tail, int size, boolean verbose
  ) {
    if (size <= 1) return head;
    
    final ListEntry <T> front = head.last, back = tail.next;
    String indent = "";
    if (verbose) {
      for (int i = this.size; i-- > size;) indent += " ";
      I.say(indent+"Sorting between "+front.refers+" and "+back.refers);
    }
    
    if (size == 2) {
      final float
        hP = queuePriority(head.refers),
        tP = queuePriority(tail.refers);
      if (hP > tP) {
        couple(front, tail);
        couple(tail, head);
        couple(head, back);
        return tail;
      }
      return head;
    }
    
    int sizeA = size / 2, sizeB = size - sizeA;
    ListEntry <T> headA = head, headB = head;
    for (int n = sizeA; n-- > 0;) headB = headB.next;
    
    if (verbose) I.say(indent+"Sorting sub-lists (size "+size+"): "+this);
    headA = sortListFrom(headA, headB.last, sizeA, verbose);
    if (verbose) I.say(indent+"After list A (size "+sizeA+"): "+this);
    headB = sortListFrom(headB, tail, sizeB, verbose);
    if (verbose) I.say(indent+"After list B (size "+sizeB+"): "+this);
    
    ListEntry <T> pick = null, lastPick = front, firstPick = null;
    for (int n = size; n-- > 0;) {
      if (sizeA <= 0) pick = headB;
      else if (sizeB <= 0) pick = headA;
      else {
        final float
          aP = queuePriority(headA.refers),
          bP = queuePriority(headB.refers);
        pick = aP < bP ? headA : headB;
      }
      
      if (verbose) {
        I.say(indent+"  Heads of A/B: "+headA.refers+"/"+headB.refers);
        I.say(indent+"  Next pick is: "+pick.refers);
      }
      
      if (pick == headA) { headA = headA.next; sizeA--; }
      if (pick == headB) { headB = headB.next; sizeB--; }
      if (firstPick == null) firstPick = pick;
      
      couple(lastPick, pick);
      lastPick = pick;
    }
    couple(lastPick, back);
    if (verbose) I.say("");
    return firstPick;
  }
  
  
  
  /**  Returns a standard iterator over this list.
    */
  final public Iterator <T> iterator() {
    return new Iterator <T> () {
      ListEntry <T> current = list;
      //
      public boolean hasNext() {
        return (current.next != list);
      }
      //
      public T next() {
        current = current.next;
        return current.refers;
      }
      //
      public void remove() {
        removeEntry(current);
        current = current.next;
      }
    };
  }
  
  
  final public Iterable <ListEntry <T>> entries() {
    final class entryList implements
      Iterable <ListEntry <T>>, Iterator <ListEntry <T>>
    {
      ListEntry <T> current = list;
      //
      public boolean hasNext() {
        return (current.next != list);
      }
      //
      public ListEntry <T> next() {
        current = current.next;
        return current;
      }
      //
      public void remove() {
        removeEntry(current);
        current = current.next;
      }
      
      public Iterator <ListEntry<T>> iterator() {
        return this;
      }
    };
    return new entryList();
  }
  
  
  /**  Returns this list in printable form.
    */
  final public String toString() {
    final StringBuffer sB = new StringBuffer("( ");
    for (T t : this) {
      sB.append(t);
      if (t != last.refers) sB.append(", ");
    }
    sB.append(" )");
    return sB.toString();
  }
  
  
  /**  Reasonably thorough testing method:
    * 
    */
  public static void main(String a[]) {
    List <Integer> list = new List <Integer> () {
      protected float queuePriority(Integer i) { return i.intValue(); }
    };
    
    for (int n = 32; n-- > 0;) list.add(new Integer(Rand.index(32)));
    list.queueSort();
    
    /*
    list.addLast(1);
    list.addLast(4);
    list.addLast(3);
    list.addLast(5);
    list.addLast(2);
    list.addLast(0);
    list.addLast(new Integer(0));
    list.addLast(new Integer(0));
    
    list.queueSort();
    //*/
    
    
    /*
    for (int i : list) {
      //if (i == 4) list.remove(i);
      I.say("Entry is: "+i);
    }
    I.say("  List contents: " + list);
    I.say("  First member is:  " + list.removeFirst());
    I.say("  List contents: " + list);
    for (int n = 2; n-- > 0;)
      I.say("  First member is:  " + list.removeFirst());
    list.clear();
    I.say("  List contents: " + list);
    //*/
  }
}



