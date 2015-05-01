/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;


/**  Serves as a 'bookmark' within a given List, allowing for deletion in
  *  constant time and various other iterator functionality.
  */
public class ListEntry <T> {
  
  final public T refers;
  protected ListEntry <T> next, last;
  protected List <T> list;
  
  
  ListEntry() {
    refers = null;
    list = (List <T>) this;
    next = last = this;
  }
  
  
  ListEntry(
    final T r,
    final List <T> c,
    final ListEntry <T> l,
    final ListEntry <T> n
  ) {
    list = c;
    refers = r;
    couple(this, n);
    couple(l, this);
    list.size++;
  }
  
  
  /**  Returns the owning list-
    */
  public List <T> list() {
    return list;
  }
  
  
  /**  Deletes this entry from it's own list.
    */
  final public void delete() {
    list.removeEntry(this);
  }
  
  /**  Links the two given entries within their list.
    */
  final static void couple(final ListEntry l, final ListEntry n) {
    l.next = n;
    n.last = l;
  }
  
  final public ListEntry <T> nextEntry() { return next; }
  final public ListEntry <T> lastEntry() { return last; }
}
