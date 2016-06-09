/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.util;
import org.apache.commons.math3.util.FastMath;



/**  A selection of convenient/common math methods (that return floats for
  *  simplicity.)
  */
public final class Nums {
  
  final public static float
    PI    = (float) FastMath.PI     ,
    E     = (float) FastMath.E      ,
    ROOT2 = (float) FastMath.sqrt(2);
  
  private Nums() {}
  
  
  /**  Essential/basic math methods-
    */
  public static float max(float a, float b) {
    return FastMath.max(a, b);
  }
  
  
  public static int max(int a, int b) {
    return FastMath.max(a, b);
  }
  
  
  public static float min(float a, float b) {
    return FastMath.min(a, b);
  }
  
  
  public static int min(int a, int b) {
    return FastMath.min(a, b);
  }
  
  
  public static int floor(float a) {
    return (int) FastMath.floor(a);
  }
  
  
  public static int ceil(float a) {
    return (int) FastMath.ceil(a);
  }
  
  
  public static float abs(float f) {
    return FastMath.abs(f);
  }
  
  
  public static int abs(int f) {
    return FastMath.abs(f);
  }
  
  
  public static float log(float base, float f) {
    return (float) FastMath.log(base, f);
  }
  
  
  public static float pow(float base, float f) {
    return (float) FastMath.pow(base, f);
  }
  
  
  public static float sqrt(float f) {
    return (float) FastMath.sqrt(f);
  }
  
  
  public static float cos(float a) {
    return (float) FastMath.cos(a);
  }
  
  
  public static float acos(float a) {
    return (float) FastMath.acos(a);
  }
  
  
  public static float sin(float a) {
    return (float) FastMath.sin(a);
  }
  
  
  public static float asin(float a) {
    return (float) FastMath.asin(a);
  }
  
  
  public static float atan2(float y, float x) {
    return (float) FastMath.atan2(y, x);
  }
  
  
  public static float toRadians(float d) {
    return (float) FastMath.toRadians(d);
  }
  
  
  public static float toDegrees(float r) {
    return (float) FastMath.toDegrees(r);
  }
  
  
  public static int square(int i) {
    return i * i;
  }
  
  
  public static int cube(int i) {
    return i * i * i;
  }
  
  
  
  /**  Useful bounding methods:
    */
  public static int clamp(int index, int range) {
    if (index < 0) return 0;
    if (index >= range) return range - 1;
    return index;
  }
  
  
  public static float clamp(float value, float min, float max) {
    if (value >= min && value <= max) return value;
    if (value < min) return min;
    if (value > max) return max;
    return (min + max) / 2;
  }
  
  
  public static int round(float value, int unit, boolean up) {
    final float fraction = (value * 1f) / unit;
    if (up) return (int) (Math.ceil(fraction) * unit);
    return unit * (int) fraction;
  }
  
  
  
  /**  Sampling methods for within 2D arrays of data-
    */
  public static float sampleMap(
    int sizeX, int sizeY, byte vals[][], float mX, float mY
  ) {
    mX *= (vals   .length - 1) * 1f / sizeX;
    mY *= (vals[0].length - 1) * 1f / sizeY;
    final int vX = (int) mX, vY = (int) mY;
    final float rX = mX % 1, rY = mY % 1;
    return
      (vals[vX    ][vY    ] * (1 - rX) * (1 - rY)) +
      (vals[vX + 1][vY    ] * rX       * (1 - rY)) +
      (vals[vX    ][vY + 1] * (1 - rX) * rY      ) +
      (vals[vX + 1][vY + 1] * rX       * rY      );
  }
  
  
  public static float sampleMap(
    int sizeX, int sizeY, float vals[][], float mX, float mY
  ) {
    mX *= (vals   .length - 1) * 1f / sizeX;
    mY *= (vals[0].length - 1) * 1f / sizeY;
    final int vX = (int) mX, vY = (int) mY;
    final float rX = mX % 1, rY = mY % 1;
    return
      (vals[vX    ][vY    ] * (1 - rX) * (1 - rY)) +
      (vals[vX + 1][vY    ] * rX       * (1 - rY)) +
      (vals[vX    ][vY + 1] * (1 - rX) * rY      ) +
      (vals[vX + 1][vY + 1] * rX       * rY      );
  }
}







