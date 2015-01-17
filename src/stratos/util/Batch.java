/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;
import java.lang.reflect.*;
import java.util.Iterator;


/**  Functionally speaking, this class should be used to quickly assemble a
  *  (temporary) list of elements, without the need for deletion.  This class
  *  gives better efficiency in insertion and iteration than the List or Stack
  *  classes, at the cost of some slight memory overhead.  It's intended for
  *  quick-and-dirty iteration results.
  */
public class Batch <K> implements Series <K> {
  
  
  final public static int DEFAULT_SECTOR_SIZE = 16;
  
  final int sectorSize;
  Object[] first, last;
  int index, size;
  
  
  public Batch() {
    this(DEFAULT_SECTOR_SIZE);
  }
  
  
  public Batch(int sectorSize) {
    this.sectorSize = index = sectorSize;
    size = 0;
  }
  
  
  public Batch(K... startWith) {
    this(startWith.length);
    for (K k : startWith) if (k != null) add(k);
  }
  
  
  public int size() { return size; }
  public boolean empty() { return size == 0; }
  
  
  
  /**  Returns an array with identical contents to this Batch.
    */
  final public K[] toArray(Class typeClass) {
    final Object[] array = (Object[]) Array.newInstance(typeClass, size);
    int i = 0; for (K t : this) array[i++] = t;
    return (K[]) array;
  }
  
  
  final public Object[] toArray() {
    return toArray(Object.class);
  }
  
  
  public K atIndex(int i) {
    if (i < 0 || i >= size) return null;
    Object[] sector = first;
    while (true) {
      if (i < sectorSize) return (K) sector[i];
      i -= sectorSize;
      sector = (Object[]) sector[i];
    }
  }
  
  
  public K last() {
    if (size < 1) return null;
    return (K) last[index - 1];
  }
  
  
  public K first() {
    if (size < 1) return null;
    return (K) first[0];
  }
  
  
  
  /**  Adds an element to the end of this list.
    */
  public void add(K k) {
    if (index == sectorSize) {
      if (first == null) {
        first = last = new Object[sectorSize + 1];
      }
      else {
        last[sectorSize] = new Object[sectorSize + 1];
        last = (Object[]) last[sectorSize];
      }
      index = 0;
    }
    last[index++] = k;
    size++;
  }
  
  
  public void include(K k) {
    if (includes(k)) return;
    add(k);
  }
  
  
  
  /**  Clears this list entirely.
    */
  public void clear() {
    first = last = null;
    index = sectorSize;
    size = 0;
  }
  
  
  /**  Checks whether this batch contains the given entry.
    */
  public boolean includes(K k) {
    for (K e : this) if (e == k) return true;
    return false;
  }
  
  
  /**  Iterates across the elements of this list in the same order they were
    *  added.
    */
  final public Iterator <K> iterator() {
    final K k = (first != null) ? (K) first[0] : null;
    final class iterates implements Iterator <K> {
      Object[] current = first;
      int index = 0, total = 0;
      K next = k;
      
      final public boolean hasNext() {
        return total < size;
      }
      
      final public K next() {
        total++;
        final K k = next;
        if (++index == sectorSize) {
          current = (Object[]) current[sectorSize];
          index = 0;
          if (current == null) return k;
        }
        next = (K) current[index];
        return k;
      }
      
      public void remove() {}
    }
    return new iterates();
  }
  
  
  /**  Returns this batch in printable form.
    */
  final public String toString() {
    final StringBuffer sB = new StringBuffer("( ");
    for (K t : this) {
      sB.append(t);
      if (t != last()) sB.append(", ");
    }
    sB.append(" )");
    return sB.toString();
  }
  
  
  /**  Basic testing method.
    */
  public static void main(String s[]) {
    Batch <Integer> t = new Batch <Integer> (3);
    for (int n = 8; n-- > 0;) t.add((int) (Math.random() * n) + 1);
    I.say("Contents: ");
    for (int n : t) I.add(" "+n);
    I.say("Contents: ");
    for (int n : (Integer[]) t.toArray(Integer.class)) I.add(" "+n);
  }
}

