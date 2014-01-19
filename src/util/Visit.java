/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package util ;
import java.lang.reflect.Array;
import java.util.Iterator ;



/**  A selection of convenient utility methods for iterating over or sampling
  *  arrays and sets of objects or data-
  */
public class Visit <T> {
  
  
  public Visit() {}
  public void visit(T o) {} ;
  
  
  public static int clamp(int index, int range) {
    if (index < 0) return 0 ;
    if (index >= range) return range - 1 ;
    return index ;
  }
  
  public static float clamp(float value, float min, float max) {
    if (value >= min && value <= max) return value ;
    if (value < min) return min ;
    if (value > max) return max ;
    return 0 ;
  }
  
  
  public static Object last(Object o[]) {
    return o[o.length - 1] ;
  }
  
  
  public static int indexOf(Object o, Object a[]) {
    for (int i = a.length ; i-- > 0 ;) if (a[i] == o) return i ;
    return -1 ;
  }
  
  
  public static Object[] compose(Class arrayClass, Object[]... arrays) {
    int length = 0, i = 0 ;
    for (Object a[] : arrays) length += a.length ;
    final Object[] result = (Object[]) Array.newInstance(arrayClass, length) ;
    for (Object a[] : arrays) {
      for (Object o : a) result[i++] = o ;
    }
    return result ;
  }
  
  
  public static void wipe(Object array[]) {
    for (int i = array.length ; i-- > 0 ;) array[i] = null ;
  }
  
  
  /**  Returns true if the given array includes the given object.
    */
  public static boolean arrayIncludes(Object a[], Object e) {
    if (a == null || e == null) return false ;
    for (Object o : a) if (o == e) return true ;
    return false ;
  }
  
  
  /**  Used to convert an array of Float objects to an array of normal floats.
    */
  public static float[] fromFloats(Object[] a) {
    float f[] = new float[a.length] ;
    for (int i = f.length ; i-- > 0 ;) f[i] = (Float) a[i] ;
    return f ;
  }
  

  /**  Used to convert an array of Integer objects to an array of normal ints.
    */
  public static int[] fromIntegers(Object[] a) {
    int f[] = new int[a.length] ;
    for (int i = f.length ; i-- > 0 ;) f[i] = (Integer) a[i] ;
    return f ;
  }
  
  
  /**  Visits every point in the given area- a syntactic shortcut for array
    *  loops.
    */
  public static Iterable <Coord> grid(
    final int minX, final int minY,
    final int xD, final int yD,
    final int step
  ) {
    final int maxX = minX + xD, maxY = minY + yD ;
    final Coord passed = new Coord() ;
    final class iterates implements Iterator <Coord>, Iterable <Coord> {
      
      int x = minX, y = minY ;
      
      final public boolean hasNext() {
        return x < maxX && y < maxY ;
      }
      
      final public Coord next() {
        passed.x = x ;
        passed.y = y ;
        if ((y += step) == maxY) { y = minY; x += step ; }
        return passed ;
      }
      
      final public Iterator <Coord> iterator() { return this ; }
      public void remove() {}
    }
    return new iterates() ;
  }
  
  
  /*
  public static Iterable <Coord> grid(Box2D b) {
    final int
      minX = (int) (b.xpos() + 0.5f),
      minY = (int) (b.ypos() + 0.5f),
      dimX = (int) (b.xmax() + 0.5f) - minX,
      dimY = (int) (b.ymax() + 0.5f) - minY ;
    return grid(minX, minY, dimX, dimY, 1) ;
  }
  //*/
  
  
  public Iterable <T> grid(
    final int minX, final int minY,
    final int xD, final int yD,
    final T array[][]
  ) {
    final int maxX = minX + xD, maxY = minY + yD ;
    final class iterates implements Iterator <T>, Iterable <T> {
      int x = minX, y = minY ;
      
      final public boolean hasNext() {
        return x < maxX && y < maxY ;
      }
      
      final public T next() {
        T next = null ;
        try { next = array[x][y] ; }
        catch (ArrayIndexOutOfBoundsException e) {}
        if (++y == maxY) { y = minY; x++ ; }
        return next ;
      }
      
      final public Iterator <T> iterator() { return this ; }
      public void remove() {}
    }
    return new iterates() ;
  }
  
  
  
  /**  Sampling methods for within 2D arrays of data-
    */
  public static float sampleMap(
    int mapSize, byte vals[][], float mX, float mY
  ) {
    mX *= (vals.length - 1) * 1f / mapSize ;
    mY *= (vals.length - 1) * 1f / mapSize ;
    final int vX = (int) mX, vY = (int) mY ;
    final float rX = mX % 1, rY = mY % 1 ;
    return
      (vals[vX    ][vY    ] * (1 - rX) * (1 - rY)) +
      (vals[vX + 1][vY    ] * rX       * (1 - rY)) +
      (vals[vX    ][vY + 1] * (1 - rX) * rY      ) +
      (vals[vX + 1][vY + 1] * rX       * rY      ) ;
  }
  
  
  public static float sampleMap(
    int mapSize, float vals[][], float mX, float mY
  ) {
    mX *= (vals.length - 1) * 1f / mapSize ;
    mY *= (vals.length - 1) * 1f / mapSize ;
    final int vX = (int) mX, vY = (int) mY ;
    final float rX = mX % 1, rY = mY % 1 ;
    return
      (vals[vX    ][vY    ] * (1 - rX) * (1 - rY)) +
      (vals[vX + 1][vY    ] * rX       * (1 - rY)) +
      (vals[vX    ][vY + 1] * (1 - rX) * rY      ) +
      (vals[vX + 1][vY + 1] * rX       * rY      ) ;
  }
  
  
  
  /**  Comparison methods-
    */
  public float rate(T t) { return 0 ; }
  
  
  /*
  public T pickBest(Series <T> series) {
    T picked = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    for (T t : series) {
      final float rating = rate(t) ;
      if (rating > bestRating) { bestRating = rating ; picked = t ; }
    }
    return picked ;
  }
  //*/
  
  
  public T pickBest(T... series) {
    T picked = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    for (T t : series) {
      final float rating = rate(t) ;
      if (rating > bestRating) { bestRating = rating ; picked = t ; }
    }
    return picked ;
  }
  
  
  
  /**  Array visitation-
    */
  public void visArray(T a[]) {
    for(int n = 0 ; n < a.length ; n++)
      visit(a[n]) ;
  }
  
  public void visArray(T a[][]) {
    for(int n = 0 ; n < a.length ; n++)
      visArray(a[n]) ;
  }
  
  public void visArray(T a[][][]) {
    for(int n = 0 ; n < a.length ; n++)
      visArray(a[n]) ;
  }
  
  public void visArray(T a[][][][]) {
    for(int n = 0 ; n < a.length ; n++)
      visArray(a[n]) ;
  }
}







