/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package util ;


public class Table <K, V> extends java.util.HashMap <K, V> {
  
  
  final static long serialVersionUID = 0 ;

  public Table() { super() ; }
  public Table(int size, float load) { super(size, load) ; }
  public Table(int size) { super(size) ; }
  
  
  /**  A dedicated class for tuple-keys.
    *  //  ...Try for arbitrary number of keys?
    */
  public static class DoubleKey {
    
    final public Object a, b ;
    final int hash ;
    
    public DoubleKey(Object a, Object b) {
      this.a = a ; this.b = b ;
      this.hash = (a.hashCode() * 13) + (b.hashCode() % 13) ;
    }
    
    public int hashCode() { return hash ; }
    
    public boolean equals(Object o) {
      //if (! (o instanceof DoubleKey)) return false ;
      final DoubleKey k = (DoubleKey) o ;
      return k.a == a && k.b == b ;
    }
    
    public String toString() { return "("+a+", "+b+")" ; }
  }
  
  
  
  /**  Convenience production methods-
    */
  public static Table make(Object... args) {
    final Table table = new Table() ;
    for (int i = 0 ; i < args.length ;) {
      table.put(args[i++], args[i++]) ;
    }
    return table ;
  }
  
  
  public static int hashFor(Object a, Object b) {
    return (a.hashCode() * 13) + (b.hashCode() % 13) ;
  }
  
  
  public static int hashFor(Object... args) {
    int hash = 0 ;
    for (Object o : args) {
      hash *= 13 ;
      hash += o.hashCode() % 13 ;
    }
    return hash ;
  }
  
  
  
  /**  Testing-
    */
  public static void main(String args[]) {
    final Table <Object, String> table = new Table() ;
    table.put("one", "one") ;
    table.put("one", "two") ;
    table.remove("one") ;
    table.put(new DoubleKey(1, 2), "one") ;
    table.put(new DoubleKey(1, 2), "two") ;
    table.remove(new DoubleKey(1, 2)) ;
    for (Object key : table.keySet()) I.say(key+" has value "+table.get(key)) ;
  }
}





