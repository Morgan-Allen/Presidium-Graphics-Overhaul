

package stratos.util;
import java.io.*;
import java.lang.reflect.Array;
import java.util.Iterator;


//  TODO:  Sort additions by class of origin, instead of by key?

public class Index <T extends Index.Entry> implements Iterable <T> {
  
  
  final private Sorting <Entry> allEntries = new Sorting <Entry> () {
    public int compare(Entry a, Entry b) {
      return a.key.compareTo(b.key);
    }
  };
  
  public static class Entry {
    
    final String key;
    final Index index;
    private int uniqueID = -1;
    
    protected Entry(Index index, String key) {
      this.index = index;
      this.key = key;
      index.addEntry(this, key);
    }
    
    public int uniqueID() {
      index.assignIDs();
      return uniqueID;
    }
  }
  
  private Object asArray[] = null;
  private Batch <T> addedSoFar = new Batch <T> ();
  private Batch <T> allAdded = new Batch <T> ();
  
  
  
  /**  Saving and loading methods-
    */
  private void addEntry(T entry, String key) {
    if (asArray != null) {
      I.complain(
        "ENTRIES CANNOT BE ADDED TO INDEX AFTER UNIQUE IDS HAVE BEEN ASSIGNED!"
      );
      return;
    }
    
    allEntries.insert(entry);
    addedSoFar.add   (entry);
    allAdded  .add   (entry);
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
  
  
  public void saveEntry(T entry, DataOutputStream out) throws Exception {
    assignIDs();
    out.writeInt(entry == null ? -1 : entry.uniqueID);
  }
  
  
  public T loadFromEntry(DataInputStream in) throws Exception {
    assignIDs();
    final int ID = in.readInt();
    return ID == -1 ? null : (T) asArray[ID];
  }
  
  
  
  /**  Other commonly-used utility methods:
    */
  public T[] soFar(Class typeClass) {
    final T array[] = addedSoFar.toArray(typeClass);
    addedSoFar.clear();
    return array;
  }
  

  final public Iterator <T> iterator() {
    return new Iterator <T> () {
      int index = asArray == null ? -1 : 0;
      
      public boolean hasNext() {
        return index > -1 && index < asArray.length;
      }
      
      public T next() {
        final T next = (T) asArray[index];
        index++;
        return next;
      }
      
      public void remove() {
        I.complain("DELETION NOT SUPPORTED");
      }
    };
  }
}













