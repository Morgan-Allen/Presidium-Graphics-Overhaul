/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;
import java.util.Random;



public class Rand {
  
  final public static Random
    GEN     = new Random(),
    PREVIEW = new Random();
  
  
  final public static float unseededNum() {
    return PREVIEW.nextFloat();
  }
  
  final public static float num() { return GEN.nextFloat(); }
  final public static boolean yes() { return GEN.nextBoolean(); }
  final public static int index(int s) { return GEN.nextInt(s); }
  
  
  final public static float saltFrom(Object o) {
    return (((o.hashCode() % 13) / 13f) + 1) % 1;
  }
  
  final public static float range(float min, float max) {
    return min + ((max - min) * GEN.nextFloat());
  }
  
  final public static float rangeAvg(float min, float max, int n) {
    float total = 0;
    for (int r = n; r-- > 0;) total += range(min, max);
    return total / n;
  }
  
  final public static int rollDice(int num, int sides) {
    int total = 0;
    while (num-- > 0) total += 1 + (int) (num() * sides);
    return total;
  }
  
  final public static float avgNums(final int n) {
    float total = 0;
    for (int r = n; r-- > 0;) total += num();
    return total / n;
  }
  
  final public static Object pickFrom(Object[] array) {
    if (array.length == 0) return null;
    return array[GEN.nextInt(array.length)];
  }
  
  
  final public static Object pickFrom(Series list) {
    return pickFrom(list.toArray());
  }
  
  
  final public static Object pickFrom(Series list, Series <Float> weights) {
    return pickFrom(list.toArray(), weights.toArray(), false);
  }
  
  
  final public static Object pickFrom(Object array[], Object weights[]) {
    return pickFrom(array, weights, false);
  }
  
  
  final public static Object pickFrom(Object array[], float weights[]) {
    return pickFrom(array, weights, true);
  }
  
  
  final private static Object pickFrom(
    Object array[], Object weightsArray, boolean floats
  ) {
    //
    //  First of all, we get the sum of all associated weights to 'roll' in...
    float sumWeights = 0;
    if (floats) for (float f : (float[]) weightsArray) sumWeights += f;
    else for (Object f : (Object[]) weightsArray) sumWeights += (Float) f;
    
    if (sumWeights == 0) return pickFrom(array);
    final float pickWith = Rand.num() * sumWeights;
    //
    //  Having summed the weights, and picked a random 'interval' within the
    //  'span' of values, pick the object entry underlying that interval.
    sumWeights = 0; int i = 0; for (Object o : array) {
      if (floats) sumWeights += ((float[]) weightsArray)[i];
      else sumWeights += (Float) ((Object[]) weightsArray)[i];
      
      if (pickWith < sumWeights) return o;
      else i++;
    }
    return null;
  }
}





