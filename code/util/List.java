/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package code.util ;
import java.lang.reflect.Array;
import java.util.Iterator ;


/**  This class essentially implements a doubly-linked list, the distinction
  *  being that each insertion returns an entry referencing that new element's
  *  place in the list.  This entry, if kept, then allows for deletion in constant
  *  time.
  *  The other distinction (albeit largely internal) is that the 'head' and
  *  'tail' nodes of the list are identical, and in fact represented by the
  *  list object itself.
  */
public class List <T> extends ListEntry <T> implements Series <T> {
  
  int size ;
  
  
  /**  Returns an array with identical contents to this List- unless the list
    *  has zero elements- in which case null is returned.
    */
  final public T[] toArray(Class typeClass) {
    final Object[] array = (Object[]) Array.newInstance(typeClass, size) ;
    int i = 0 ; for (T t : this) array[i++] = t ;
    return (T[]) array ;
  }
  
  final public Object[] toArray() {
    return toArray(Object.class) ;
  }
  
  final public void add(final T r) { addLast(r) ; }
  
  
  /**  Adds the given member at the head of the list.
    */
  final public ListEntry <T> addFirst(final T r) {
    return new ListEntry <T> (r, this, this, next) ;
  }
  
  /**  Adds the given member at the tail of the list.
    */
  final public ListEntry <T> addLast(final T r) {
    return new ListEntry <T> (r, this, last, this) ;
  }
  
  /**  Adds all the given members at the head of the list.
    */
  final public void addFirst(final T[] r) {
    for (int n = r.length ; n-- > 0 ;) addFirst(r[n]) ;
  }
  
  /**  Adds all the given members at the tail of the list.
    */
  final public void addLast(final T[] r) {
    for(T t : r) addLast(t) ;
  }
  
  /**  Removes the first element from this list.
    */
  final public T removeFirst() {
    return removeEntry(next).refers ;
  }
  
  /**  Removes the first element from this list.
    */
  final public T removeLast() {
    return removeEntry(last).refers ;
  }

  /**  Returns the size of this list.
    */
  final public int size() {
    return size ;
  }
  
  /**  Adds all the given list's elements to this list's own, starting from the
    *  front.  Order is preserved.  The argument list is cleared.
    */
  final public void mergeAll(final List <T> c) {
    for (ListEntry <T> l = c ; (l = l.next) != c ;) l.list = this ;
    couple(c.last, next) ;
    couple(this, c.next) ;
    size += c.size ;
    c.clear() ;
  }
  
  /**  Returns the entry at the specified place in the list.
    */
  final public ListEntry <T> getEntryAt(final int n) {
    int i = 0 ;
    for (ListEntry <T> l = this ; (l = l.next) != this ; i++)
      if (i == n) return l ;
    return null ;
  }
  
  
  /**  Return the index of the given entry-
    */
  final public int indexOf(T t) {
    int i = 0 ;
    for (ListEntry <T> l = this ; (l = l.next) != this ; i++)
      if (t == l.refers) return i ;
    return -1 ;
  }
  
  
  /**  Returns the member at the specified index-
    */
  final public T atIndex(int i) {
    if (i < 0 || i >= size) return null ;
    int d = 0 ;
    for (ListEntry <T> l = this ; (l = l.next) != this ; d++)
      if (d == i) return l.refers ;
    return null ;
  }
  
  
  
  /**  Removes the specified entry from the list.  (This method has no effect if
    *  the given entry does not belong to the list.)
    */
  final public ListEntry <T> removeEntry(final ListEntry <T> l) {
    if ((l == null) || (l.list != this)) {
      //I.r("Invalid list entry!") ;
      return null ;
    }
    couple(l.last, l.next) ;
    //
    //  I actually don't want this:  "l.last = l.next = null ;"
    //  ...Because then I can't interate and remove at the same time!
    l.list = null ;
    size-- ;
    return l ;
  }
  
  /**  Adds the specified element directly after the given entry.  (This method
    *  has no effect if the given entry does not belong to the list.)
    */
  final public ListEntry <T> addAfter(final ListEntry <T> l, final T r) {
    if ((l == null) || (l.list != this)) return null ;
    return new ListEntry <T> (r, this, l, l.next) ;
  }
  
  /**  Empties this list of all members.
    */
  final public void clear() {
    for (ListEntry <T> l = this ; (l = l.next) != this ;)
      couple(l.last, l.next) ;
    size = 0 ;
  }
  
  /**  Returns an exact copy of this list.
    */
  final public List <T> copy() {
    final List <T> list = new List <T> () ;
    for (T t : this) list.addFirst(t) ;
    return list ;
  }
  
  /**  Finds the entry matching the given element, if present.  If more than one
    *  exists, only the first is returned.
    */
  final public ListEntry <T> match(final T r) {
    for (ListEntry <T> l = this ; (l = l.next) != this ;)
      if (l.refers == r)
        return l ;
    return null ;
  }
  
  
  /**  Returns whether the given element is present in the list.
    */
  final public boolean includes(final T r) {
    return match(r) != null ;
  }
  
  
  /**  Includes the given element in this list if not already present.
    */
  final public void include(final T r) {
    if (match(r) == null) addLast(r) ;
  }
  
  
  /**  Discards the given element from the list if present- if included more than
    *  once, only the first is returned.
    */
  final public void remove(final T r) {
    removeEntry(match(r)) ;
  }
  
  
  final public T first() {
    return next.refers ;
  }
  
  
  final public T last() {
    return last.refers ;
  }
  
  
  /**  This method is intended for override by subclasses.  Like it says, it
    *  returns the queue priority of a given list element.  It isn't abstract,
    *  because you might not need it, and it's default return value is zero.
    */
  protected float queuePriority(final T r) {
    return 0 ;
  }
  
  
  /**  Adds the given element while maintaining queue priority (descending
    *  order) within the list.
    */
  final public ListEntry <T> queueAdd(final T r) {
    ListEntry <T> l = this ;
    while ((l = l.next) != this)
      if (queuePriority(r) > queuePriority(l.refers)) break ;
    return new ListEntry <T> (r, this, l.last, l) ;
  }
  
  
  /**  Sorts the elements of this list in descending order after their addition
    *  en masse.  As nLog(n) comparisons are performed, it is strongly
    *  recommended that the queuePriority method be efficient (e.g, by simply
    *  returning precalculated values that can be referenced from member
    *  elements themselves.)
    */
  final public void queueSort() {
    if (size() == 0) return ;
    //  The merge-sort has been implemented as an internal class so that
    //  assorted 'loop' variables can be cheaply parameterised but do not have
    //  to be reallocated or passed for each function call.
    final class queueSorts {
      ListEntry <T> firstSorted, current, greater ;  //pointers used in sorting.
      int iA, iB, iS ;  //indices used in sorting.
      
      //  Here's the merge-sort itself.
      final ListEntry <T> mergeSort(
          final ListEntry <T> first,
          final int length
        ) {
        //  Handle special cases first.
        if (length == 1) {
          first.next = null ;
          return first ;
        }
        if (length == 2) {
          final ListEntry <T> after = first.next ;
          if (queuePriority(after.refers) > queuePriority(first.refers)) {
            after.next = first ;
            first.next = null ;
            return after ;
          }
          else {
            after.next = null ;
            return first ;
          }
        }
        //  Otherwise, split the list in two and perform the sort on both
        //  sub-lists separately:
        final int
          lenA = length / 2,
          lenB = length - lenA ;
        ///I.say("\nLength of a/b: "+lenA+" "+lenB) ;
        //  Iterate over the first sub-list's members to get to the second
        //  sub-list.
        ListEntry <T>
          nextA = first,
          nextB = first ;
        iA = 0 ;
        while (iA++ < lenA)
          nextB = nextB.next ;
        //  Then, perform recursive merge-sorts on both those sub-lists:
        nextA = mergeSort(nextA, lenA) ;
        nextB = mergeSort(nextB, lenB) ;
        ///I.say("\nNow merging lists... "+length) ;
        //  Now, perform a merging of the sorted lists-
        current = firstSorted = null ;
        iA = iB = iS = 0 ;
        while (iS++ < length) {
          //  Pick the greater member from the head of each sublist:
          if (iA == lenA)
            greater = nextB ;
          else if (iB == lenB)
            greater = nextA ;
          else {
            ///I.say("\ncomparing "+nextA.refers+" "+nextB.refers) ;
            if (queuePriority(nextA.refers) > queuePriority(nextB.refers))
              greater = nextA ;
            else
              greater = nextB ;
          }
          //  Advance the reference into the favoured list,
          if (greater == nextA) { nextA = nextA.next ; iA++ ; }
          else                  { nextB = nextB.next ; iB++ ; }
          ///I.say("\n"+greater.refers+" was greater "+iA+" "+iB) ;
          //  ...and append the chosen element to the sorted list.
          if (firstSorted == null)
            current = firstSorted = greater ;
          else {
            current.next = greater ;
            current = greater ;
          }
        }
        current.next = null ;
        ///I.say("\nSorted sublist is: ") ;
        ///for (ListEntry lE = firstSorted ; lE != null ; lE = lE.next)
          ///I.say(lE.refers+" ") ;
        //  (Allows later cleanup to know where to terminate.)
        return firstSorted ;
      }
    }
    queueSorts qS = new queueSorts() ;
    ListEntry <T>
      previous = this,
      current ;
    this.next = current = qS.mergeSort(this.next, size) ;
    //  Finally, the full list must be reconstructed and the element entrys
    //  stitched together correctly- the above algorithm only keeps 'next'
    //  references to maintain ordering, so 'last' references must be restored.
    for (; current != null ; previous = current, current = current.next)
      current.last = previous ;
    couple(previous, this) ;
  }
  
  
  /**  Returns a standard iterator over this list.
    */
  final public Iterator <T> iterator() {
    final class iterates implements Iterator <T> {
      ListEntry <T> current = list ;
      //
      public boolean hasNext() {
        return (current.next != list) ;
      }
      //
      public T next() {
        current = current.next ;
        return current.refers ;
      }
      //
      public void remove() {
        removeEntry(current) ;
        current = current.next ;
      }
    }
    return new iterates() ;
  }
  
  
  /**  Returns this list in printable form.
    */
  final public String toString() {
    final StringBuffer sB = new StringBuffer("( ") ;
    for (T t : this) {
      sB.append(t) ;
      if (t != last.refers) sB.append(", ") ;
    }
    sB.append(" )") ;
    return sB.toString() ;
  }
  
  
  /**  Reasonably thorough testing method:
    * 
    */
  public static void main(String a[]) {
    List <Integer> list = new List <Integer> () {
      protected float queuePriority(Integer i) { return i.intValue() ; }
    } ;
    list.addLast(1) ;
    list.addLast(4) ;
    list.addLast(3) ;
    list.addLast(5) ;
    list.addLast(2) ;
    list.addLast(0) ;
    list.queueSort() ;
    for (int i : list) {
      if (i == 4) list.remove(i) ;
      I.say("Entry is: "+i) ;
    }
    /*
    list.queueAdd(1) ;
    list.queueAdd(4) ;
    list.queueAdd(3) ;
    list.queueAdd(5) ;
    list.queueAdd(2) ;
    list.queueAdd(0) ;
    //*/
    I.say("  List contents: " + list) ;
    I.say("  First member is:  " + list.removeFirst()) ;
    I.say("  List contents: " + list) ;
    for (int n = 2 ; n-- > 0 ;)
      I.say("  First member is:  " + list.removeFirst()) ;
    list.clear() ;
    I.say("  List contents: " + list) ;
  }
}
