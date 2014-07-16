


package stratos.util;
import java.io.*;
import java.util.*;



//
//  Since I seem to be indexing things all over the place, a utility class for
//  the purpose seems in order.
public class Index <T extends Index.Member> implements Iterable <T> {
  
  
  
  final String indexID;
  final Class declares;
  private Batch <T> members = new Batch <T> ();
  private Object arrayM[];
  
  
  
  public Index(Class declares, String indexID) {
    this.declares = declares;
    this.indexID = indexID;
  }
  
  
  public void saveMember(T m, DataOutputStream out) throws Exception {
    if (m == null) { out.writeInt(-1); return; }
    members();
    out.writeInt(m.indexID);
  }
  
  
  public T loadMember(DataInputStream in) throws Exception {
    final int index = in.readInt();
    if (index == -1) return null;
    return (T) members()[index];
  }
  
  
  public Object[] members() {
    if (arrayM != null) return arrayM;
    arrayM = members.toArray();
    members = null;
    return arrayM;
  }
  

  final public Iterator <T> iterator() {
    members();
    return new Iterator <T> () {
      int index = 0;
      
      final public boolean hasNext() {
        return index < arrayM.length;
      }
      
      final public T next() {
        final T t = (T) arrayM[index];
        index++;
        return t;
      }
      
      public void remove() { I.complain("NOT SUPPORTED"); }
    };
  }
  
  
  
  /**  Intended for subclassing by external clients.
    */
  public static class Member {
    
    final public int indexID;
    final public Index index;
    
    
    protected Member(Index index) {
      if (index.arrayM != null) I.complain("CANNOT ADD MEMBERS AFTER INIT!");
      this.index = index;
      this.indexID = index.members.size();
      index.members.add(this);
    }
  }
}



