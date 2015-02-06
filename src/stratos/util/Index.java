

package stratos.util;
import java.io.*;
import java.lang.reflect.Array;
import java.util.Iterator;


//  TODO:  Sort additions by class of origin, instead of just by key?

public class Index <T extends Index.Entry> implements Iterable <T> {
  
  
  final private Sorting <T> allEntries = new Sorting <T> () {
    public int compare(T a, T b) {
      return a.key.compareTo(b.key);
    }
  };
  
  public static class Entry {
    
    final String key;
    final Index index;
    private int uniqueID = -1;
    
    
    protected Entry(Index index, String key) {
      if (key == null) I.complain("KEY MUST NOT BE NULL!");
      this.index = index;
      this.key = key;
      index.addEntry(this, key);
    }
    
    
    public int uniqueID() {
      index.assignIDs();
      return uniqueID;
    }
    
    
    public String entryKey() {
      return key;
    }
  }
  
  
  private Object asArray[] = null;
  private T asTypedArray[] = null;
  
  private Batch <T> addedSoFar = new Batch <T> ();
  private Batch <T> allAdded   = new Batch <T> ();
  private Table <String, T> keyTable = new Table <String, T> (1000);
  
  
  
  /**  Saving and loading methods-
    */
  private void addEntry(T entry, String key) {
    if (asArray != null) {
      I.complain(
        "ENTRIES CANNOT BE ADDED TO INDEX AFTER UNIQUE IDS HAVE BEEN ASSIGNED!"
      );
      return;
    }
    final Entry old = keyTable.get(key);
    if (old != null) {
      I.complain(
        "ENTRY KEYS MUST BE UNIQUE: "+key+" is used by both:"+
        "\n  "+entry+" ("+entry.getClass()+") and "+old+" ("+old.getClass()+")"
      );
      return;
    }
    allEntries.insert(entry);
    addedSoFar.add   (entry);
    allAdded  .add   (entry);
    keyTable.put(key, entry);
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
  
  
  public T[] allEntries(Class typeClass) {
    if (asTypedArray != null) return asTypedArray;
    assignIDs();
    asTypedArray = (T[]) Array.newInstance(typeClass, asArray.length);
    for (int i = 0; i < asArray.length; i++) {
      asTypedArray[i] = (T) asArray[i];
    }
    return asTypedArray;
  }
  

  final public Iterator <T> iterator() {
    assignIDs();
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













