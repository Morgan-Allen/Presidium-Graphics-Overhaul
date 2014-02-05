/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package util ;
import java.lang.reflect.Array ;
import java.util.Iterator ;


/**  Implements a singly-linked list, or sequence.  Faster and more space-
  *  efficient than the List class, but deletion takes O(n), rather than
  *  constant time (if a list entry is retained,) and more limited in terms of
  *  sorting/search/composition facilities.
  */
public class Stack <T> implements Series <T> {
  
  public static void main(String args[]) {
    Stack <Integer> seq = new Stack <Integer> () ;
    seq.addFirst(3) ;
    seq.addFirst(7) ;
    seq.addFirst(1) ;
    seq.addLast(5)  ;
    seq.remove(5) ;
    //for (int i : seq) if (i == 5) seq.remove(i) ;
    //seq.addFirst(5) ;
    I.say("  Sequence of length "+seq.size()+" is: " + seq) ;
    //seq.clear() ;
    //I.say("  Sequence is: " + seq) ;
  }
  
  
  /**  Returns an array with identical contents to this Stack.
    */
  final public Object[] toArray(Class typeClass) {
    final Object[] array = (Object[]) Array.newInstance(typeClass, size) ;
    int i = 0 ; for (T t : this) array[i++] = t ;
    return array ;
  }
  final public Object[] toArray() { return toArray(Object.class) ; }
  
  final public void add(final T t) { addLast(t) ; }
  
  
  protected Entry <T> first, last ;
  int size = 0 ;
  
  /**  Returns the length of this sequence.
    */
  final public int size() {
    return size ;
  }
  
  /**  Adds the given argument to the head of the sequence.
    */
  final public void addFirst(final T t) {
    first = new Entry <T> (first, t) ;
    if (size++ < 1) last = first ;
  }
  
  /**  Adds the given argument to the tail of the sequence.
    */
  final public void addLast(final T t) {
    if (size++ < 1)
      first = last = new Entry <T> (first, t) ;
    else last = new Entry <T> (t, last) ;
  }
  
  /**  Removes and returns the first object in this sequence.
    */
  final public T removeFirst() {
    if (first == null) return null ;
    final T t = first.refers ;
    first = (--size > 0) ? first.next : (last = null) ;
    return t ;
  }
  
  /**  Returns the first object in this sequence, which is itself unaffected.
    */
  final public T getFirst() {
    return (first == null) ? null : first.refers ;
  }
  
  /**  Returns the last object in this sequence, which is itself unaffected.
    */
  final public T getLast() {
    return (last == null) ? null : last.refers ;
  }
  
  /**  Returns the object at the given index in this sequence, which is itself
    *  unaffected.
    */
  final public T getIndex(int n) {
    if (n == -1) return null ;
    for (T t : this) if (n-- == 0) return t ;
    return null ;
  }

  /**  Returns the object at the given index in this sequence, which is remove
    *  from the sequence in the process.
    */
  final public T removeIndex(int n) {
    return remove(atIndex(n)) ;
  }
  
  /**  Returns the nth entry in this stack-
    */
  final public T atIndex(int n) {
    for (T t : this) if (n-- == 0) return t ;
    return null ;
  }
  
  /**  Returns the place of the given object within the list-
    */
  final public int indexOf(T t) {
    int i = 0 ;
    for (T o : this) if (o == t) return i ; else ++i ;
    return -1 ;
  }
  
  /**  Adds the given item to this sequence directly.
    */
  final public void addEntry(Entry <T> item) {
    item.next = first ;
    first = item ;
    if (size++ < 1) last = first ;
  }
  
  /**  Clears this sequence.
    */
  final public void clear() {
    first = last = null ;
    size = 0 ;
  }
  
  /**  Returns true if this sequence includes the given object.
    */
  final public boolean includes(final T r) {
    for (T t : this) if (t == r) return true ;
    return false ;
  }
  
  /**  Adds the given object to this sequence if not already present.
    */
  final public void include(final T r) {
    if (! includes(r)) addFirst(r) ;
  }
  
  /**  Removes the first item in this sequence whose content matches the given
    *  argument.
    */
  final public T remove(final T t) {
    Entry <T> c = first, prior = null ;
    while (c != null) {
      if (c.refers == t) {
        //I.say("Removing: "+t) ;
        if (prior == null) first = c.next ;
        else prior.next = c.next ;
        if (last == c) last = prior ;
        size-- ;
        return t ;
      }
      else {
        prior = c ;
        c = c.next ;
      }
    }
    return null ;
  }
  
  /**  Returns a standard iterator over this sequence.  Removal functionality is
    *  not implemented here- please use the main remove() method.
    */
  final public Iterator <T> iterator() {
    return new Iterator <T> () {
      Entry <T> current = first ;
      //
      final public boolean hasNext() {
        return current != null ;
      }
      //
      final public T next() {
        final T t = current.refers ;
        current = current.next ;
        return t ;
      }
      //
      public void remove() {}
    } ;
  }
  
  
  /**  Returns this sequence in string form.
    */
  public String toString() {
    final StringBuffer sB = new StringBuffer("( ") ;
    Entry <T> c = first ;
    for (; c != null ; c = c.next) {
      sB.append(c.refers) ;
      if (c != last) sB.append(", ") ;
    }
    sB.append(" )") ;
    return sB.toString() ;
  }
  
  
  public static class Entry <T> {
    
    final T refers ;
    Entry <T> next = null ;
    
    public Entry(final T r) {
      refers = r ;
    }
    
    Entry(final T r, final Entry <T> l) {
      refers = r ;
      l.next = this ;
    }
    
    Entry(final Entry <T> n, final T r) {
      refers = r ;
      next = n ;
    }
  }
}

