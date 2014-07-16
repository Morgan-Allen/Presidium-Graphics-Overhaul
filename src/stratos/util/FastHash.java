/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.util;

/**  This class is still unfinished and UNTESTED!  TODO:  Finish and test.
 */
public abstract class FastHash <K, V> {
  
  
  
  public static void main(String s[]) {
    final FastHash <Integer, Boolean> hash = new FastHash <Integer, Boolean> (3) {
      boolean equals(Integer a, Integer b) { return a == b; }
      int[] keyData(Integer key) { return new int[] { key }; }
    };
    hash.put(4, true);
    hash.put(5, false);
    hash.put(1, false);
    hash.put(15, true);
    I.say("Retrieved: "+hash.get(4)+" "+hash.get(6)+" "+hash.get(15)+" "+hash.get(1));
  }
  
  
  final private Entry <K, V> table[];
  final private List entries = new List();
  final private int size;
  
  
  public FastHash(final int size) {
    table = new Entry[this.size = size];
  }
  
  abstract int[] keyData(final K key);
  abstract boolean equals(final K a, final K b);
  
  
  final private static class Entry <K, V> {
    final private K key;
    private V value;
    private Entry <K, V> next;
    
    Entry(final K k, final V v) {
      key = k; value = v;
    }
  }
  
  
  final public void put(final K key, final V value) {
    int index = computeHash(keyData(key)) % size;
    if (index < 0) index += size;
    if (table[index] != null) {
      for (Entry <K, V> link = table[index]; link != null; link = link.next)
        if (equals(link.key, key)) {
          link.value = value;
          return;
        }
    }
    final Entry <K, V> entry = new Entry <K, V> (key, value);
    entry.next = table[index];
    table[index] = entry;
  }
  
  final public V get(final K key) {
    int index = computeHash(keyData(key)) % size;
    if (index < 0) index += size;
    if (table[index] == null)
      return null;
    Entry <K, V> link = table[index];
    if (link.next == null)
      return link.value;
    for (; link != null; link = link.next) if (equals(link.key, key))
      return link.value;
    return null;
  }
  
  final public boolean delete(final K key) {
    int index = computeHash(keyData(key)) % size;
    if (index < 0) index += size;
    if (table[index] == null)
      return false;
    Entry <K, V> link = table[index];
    if (link.next == null) {
      table[index] = null;
      return true;
    }
    Entry <K, V> prior = null;
    for (; link != null; link = link.next, prior = link)
      if (equals(link.key, key)) {
        if (prior != null) prior.next   = link.next;
        else               table[index] = link.next;
        return true;
      }
    return false;
  }
  
  
  /*
  public final void put(final Object o, final int key) {
    put(o, computeHash(key) % size);
  }
  
  private final void put(final Object o, final int index) {
    table[index] = new Link(o, key, table[index]);  //might be null;
  }
  
  public final Object get(final int key) {
    final int index = computeHash(key) % size;
    Link
      link = table[index],
      prev = null;
    
    while (link != null) {
      if (link.key == key) {
        if (prev != null)
          prev.next = link.next;
        else
          table[index] = link.next;
        
        return link.object;
      }
      prev = link;
      link = link.next;
    }
    return null;
  }
  
  final private static class Link {
    
    final private Object object;
    final private int key;
    private Link next;
    
    private Link(final Object o, final int h, final Link n) {
      object = o; key = h; next = n;
    }
  }
  //*/
  
  
  /*  The following is based on (a greatly cut-down version of) Bob Jenkins'
   *  Lookup2 algorithm, details of which may be found at-
   *  http://burtleburtle.net/bob/hash/doobs.html
   */
  private static int a, b, c;

  final private static void mix() {
    a -= b; a -= c; a ^= (c >> 13);
    b -= c; b -= a; b ^= (a << 8) ;
    c -= a; c -= b; c ^= (b >> 13);
    a -= b; a -= c; a ^= (c >> 12);
    b -= c; b -= a; b ^= (a << 16);
    c -= a; c -= b; c ^= (b >> 5) ;
    a -= b; a -= c; a ^= (c >> 3) ;
    b -= c; b -= a; b ^= (a << 10);
    c -= a; c -= b; c ^= (b >> 15);
  }
  
  protected final static int computeHash(final int data) {
    a = b = 0x9e3779b9; c = 0;
    a += data;
    mix(); return c;
  }

  protected final static int computeHash(
    final int d1,
    final int d2
  ) {
    a = b = 0x9e3779b9; c = 0;
    a += d1; b += d2;
    mix(); return c;
  }

  protected final static int computeHash(
    final int d1,
    final int d2,
    final int d3
  ) {
    a = b = 0x9e3779b9; c = 0;
    a += d1; b += d2; c += d3;
    mix(); return c;
  }

  protected final static int computeHash(
    final int d1,
    final int d2,
    final int d3,
    final int d4
  ) {
    a = b = 0x9e3779b9; c = 0;
    a += d1; b += d2; c += d3;
    mix();
    a += d4;
    mix(); return c;
  }

  protected final static int computeHash(
    final int d1,
    final int d2,
    final int d3,
    final int d4,
    final int d5
  ) {
    a = b = 0x9e3779b9; c = 0;
    a += d1; b += d2; c += d3;
    mix();
    a += d4; b += d5;
    mix(); return c;
  }

  protected final static int computeHash(
    final int d1,
    final int d2,
    final int d3,
    final int d4,
    final int d5,
    final int d6
  ) {
    a = b = 0x9e3779b9; c = 0;
    a += d1; b += d2; c += d3;
    mix();
    a += d4; b += d5; c += d6;
    mix(); return c;
  }
  
  //*
  protected final static int computeHash(final int[] data) {
    final int len = data.length;
    a = b = 0x9e3779b9; c = 0;
    int i = 0;
    while(i + 3 <= len) {
      a += data[i++];
      b += data[i++];
      c += data[i++];
      mix();
    }
    if (i < len) a += data[i++];
    if (i < len) b += data[i++];
    if (i < len) c += data[i++];
    mix(); return c;
  }
  //*/
  /*
  protected final static int computeHash(byte[] data) {
    final int len = data.length;
    a = b = 0x9e3779b9;
    c = 0;
    
    int i = 0;
    while (i + 12 <= len) {
      a +=
        (int) data[i++]         |
        ((int) data[i++] << 8)  |
        ((int) data[i++] << 16) |
        ((int) data[i++] << 24);
      b +=
        (int) data[i++]         |
        ((int) data[i++] << 8)  |
        ((int) data[i++] << 16) |
        ((int) data[i++] << 24);
      c +=
        (int) data[i++]         |
        ((int) data[i++] << 8)  |
        ((int) data[i++] << 16) |
        ((int) data[i++] << 24);
      mix();
    }
    c += (int) len;
    if (i < len) a += data[i++]      ;
    if (i < len) a += data[i++] << 8 ;
    if (i < len) a += data[i++] << 16;
    if (i < len) a += data[i++] << 24;
    if (i < len) b += data[i++]      ;
    if (i < len) b += data[i++] << 8 ;
    if (i < len) b += data[i++] << 16;
    if (i < len) b += data[i++] << 24;
    if (i < len) c += data[i++]      ;
    if (i < len) c += data[i++] << 8 ;
    if (i < len) c += data[i++] << 16;
    if (i < len) c += data[i++] << 24;
    mix();
    return c;
  }
  //*/
}