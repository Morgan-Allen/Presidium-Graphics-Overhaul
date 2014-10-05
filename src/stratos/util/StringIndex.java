

package stratos.util;
import java.io.*;
import java.lang.reflect.Array;



public class StringIndex <T> {
  
  private class Entry {
    T refers;
    String key;
    int uniqueID;
  }
  
  final private Sorting <Entry> allEntries = new Sorting <Entry> () {
    public int compare(Entry a, Entry b) {
      return a.key.compareTo(b.key);
    }
  };
  private Object asArray[] = null;
  private Batch <T> addedSoFar = new Batch <T> ();
  
  
  
  /**  Saving and loading methods-
    */
  final public Object addEntry(T refers, String key) {
    if (asArray != null) {
      I.complain(
        "ENTRIES CANNOT BE ADDED TO INDEX AFTER UNIQUE IDS HAVE BEEN ASSIGNED!"
      );
      return null;
    }
    
    final Entry e = new Entry();
    e.refers = refers;
    e.key = key;
    allEntries.insert(e);
    addedSoFar.add(refers);
    return e;
  }
  
  
  private void assignIDs() {
    //  This method is only intended to be called once, so if new entries are
    //  added after this point, you're screwed.
    if (asArray != null) return;
    asArray = new Object[allEntries.size()];
    int nextID = 0;
    for (Entry e : allEntries) {
      e.uniqueID = nextID;
      asArray[nextID++] = e;
    }
  }
  
  
  public void saveEntry(Object entry, DataOutputStream out) throws Exception {
    assignIDs();
    out.writeInt(((Entry) entry).uniqueID);
  }
  
  
  public T loadFromEntry(DataInputStream in) throws Exception {
    assignIDs();
    return ((Entry) asArray[in.readInt()]).refers;
  }
  
  
  
  /**  Other commonly-used utility methods:
    */
  public T[] soFar(Class typeClass) {
    final T array[] = addedSoFar.toArray(typeClass);
    addedSoFar.clear();
    return array;
  }
}









