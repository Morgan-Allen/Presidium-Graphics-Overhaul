/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.common ;
import stratos.util.*;

import java.awt.Color ;
import java.io.* ;

import org.apache.commons.math3.util.FastMath;



/**  Standard RGBA colour class with utility functions for conversion to other
  *  formats and setting non-standard qualities such as brightness and
  *  complement.
  */
public class Colour {
  
  
  final public static Colour
    HIDE  = new Colour().set(1, 1, 1,  0),
    //GLOW = new Colour().set(1, 1, 1, -1),
    WHITE = new Colour().set(1, 1, 1, 1),
    NONE  = new Colour().set(0, 0, 0, 0),
    
    RED     = new Colour().set(1, 0, 0, 1),
    GREEN   = new Colour().set(0, 1, 0, 1),
    BLUE    = new Colour().set(0, 0, 1, 1),
    YELLOW  = new Colour().set(1, 1, 0, 1),
    CYAN    = new Colour().set(0, 1, 1, 1),
    MAGENTA = new Colour().set(1, 0, 1, 1),
    
    SOFT_RED     = new Colour().set(1, 0, 0, 0.5f),
    SOFT_GREEN   = new Colour().set(0, 1, 0, 0.5f),
    SOFT_BLUE    = new Colour().set(0, 0, 1, 0.5f),
    SOFT_YELLOW  = new Colour().set(1, 1, 0, 0.5f),
    SOFT_CYAN    = new Colour().set(0, 1, 1, 0.5f),
    SOFT_MAGENTA = new Colour().set(1, 0, 1, 0.5f),
    
    DARK_GREY   = new Colour().set(0.2f, 0.2f, 0.2f, 1),
    GREY        = new Colour().set(0.5f, 0.5f, 0.5f, 1),
    LIGHT_GREY  = new Colour().set(0.8f, 0.8f, 0.8f, 1),
    BLACK       = new Colour().set(0, 0, 0, 1),
    TRANSLUCENT = new Colour().set(1, 1, 1, 0.5f) ;
  final public static int
    ALPHA_BITS = 0xff000000,
    RGB_BITS   = 0x00ffffff;
  
  public float r = 1, g = 1, b = 1, a = 1;
  public float bitValue;
  
  
  public Colour() {
  }
  
  
  public Colour(Colour c) {
    set(c);
  }

  public Colour(float r, float g, float b, float a) {
    set(r, g, b, a);
  }

  public Colour(float r, float g, float b) {
    set(r, g, b, 1);
  }

  public void saveTo(DataOutputStream out) throws Exception {
    out.writeFloat(r);
    out.writeFloat(g);
    out.writeFloat(b);
    out.writeFloat(a);
  }

  public void loadFrom(DataInputStream in) throws Exception {
    r = in.readFloat();
    g = in.readFloat();
    b = in.readFloat();
    a = in.readFloat();
  }
  
  
  
  /**  Helper methods for format conversion-
    */
  public void calcBitValue() {
    bitValue = toFloatBits();
  }
  
  private float toFloatBits() {
    final float o = FastMath.abs(a);
    final int color =
      ((int) (255 * o) << 24) |
      ((int) (255 * b) << 16) |
      ((int) (255 * g) << 8 ) |
      ((int) (255 * r) << 0 ) ;
    return Float.intBitsToFloat(color);
  }
  
  
  public int getRGBA() {
    return
      ((int) (255 * a) << 0 ) |
      ((int) (255 * b) << 8 ) |
      ((int) (255 * g) << 16) |
      ((int) (255 * r) << 24) ;
  }
  
  
  public void setFromRGBA(int value) {
    r = ((value >> 24) & 0xff) / 255f;
    g = ((value >> 16) & 0xff) / 255f;
    b = ((value >> 8 ) & 0xff) / 255f;
    a = ((value >> 0 ) & 0xff) / 255f;
    bitValue = toFloatBits();
  }
  
  
  public float[] toFloatVals() {
    return new float[] { r, g, b, a };
  }
  
  
  /**
   * Sets this colour to match the argument Colour values.
   */
  public Colour set(Colour colour) {
    r = colour.r;
    g = colour.g;
    b = colour.b;
    a = colour.a;
    bitValue = toFloatBits();
    return this;
  }
  
  
  /**
   * Sets this colour to match given RGBA component values.
   */
  public Colour set(float rc, float gc, float bc, float ac) {
    r = rc;
    g = gc;
    b = bc;
    a = ac;
    bitValue = toFloatBits();
    return this;
  }

  /**
   * Sets the Value (or brightness) of this colour to the desired value. An RGB
   * colour's maximum component defines brightness.
   */
  public Colour setValue(float v) {
    final float val = value();
    if (val > 0) {
      r *= v / val;
      g *= v / val;
      b *= v / val;
    } else
      r = g = b = v;
    bitValue = toFloatBits();
    return this;
  }

  /**
   * Returns the Value (or Brightness) of this colour, (defined as the maximum
   * of RGB components.)
   */
  public float value() {
    return Math.max(r, Math.max(g, b));
  }
  
  public boolean blank() {
    return r == 1 && g == 1 && b == 1;
  }
  
  public boolean transparent() {
    return a < 1;
  }
  
  public boolean glows() {
    return a < 0;
  }
  

  /**
   * Sets the argument colour to the complement of this Colour- opposite on the
   * HSV spectrum, 1 - value, and identical saturation. (If the argument is
   * null, a new Colour is initialised.)
   */
  public Colour complement(Colour result) {
    if (result == this) {
      final Colour temp = new Colour();
      complement(temp);
      set(temp);
      return this;
    }
    else if (result == null) result = new Colour();
    result.r = g + b / 2;
    result.g = r + b / 2;
    result.b = r + g / 2;
    result.a = a;
    result.setValue(((1 - value()) + 0.5f) / 2f);
    return result;
  }

  /**
   * Returns the Hue/Saturation/Value coordinates of this Colour encoded in a 3
   * value float array (which, if null, is initialised and then returned.)
   */
  public float[] getHSV(float[] result) {
    if ((result == null) || (result.length != 3))
      result = new float[3];
    Color.RGBtoHSB((int) (r * 255), (int) (g * 255), (int) (b * 255), result);
    return result;
  }

  /**
   * Sets this RGBA colour to represent the given Hue/Saturation/Value (or
   * Brightness) coordinates as a 3-valued float array: Alpha is not affected.
   */
  public Colour setHSV(float hsv[]) {
    int bytes = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
    r = (((bytes >> 16) & 0xff) / 255f);
    g = (((bytes >> 8) & 0xff) / 255f);
    b = (((bytes >> 0) & 0xff) / 255f);
    a = 1;
    bitValue = toFloatBits();
    return this;
  }
  
  
  /**  Helper methods for obtaining transparency and fog values-
    */
  final public static Colour TRANSPARENCIES[] = new Colour[100];
  final public static Colour GREYSCALES[]     = new Colour[100];
  final public static Colour GLOWS[]          = new Colour[100];
  static {
    for (int n = 100; n-- > 0;) {
      final float l = n / 100f;
      final Colour t = TRANSPARENCIES[n] = new Colour();
      t.set(1, 1, 1, l);
      final Colour f = GREYSCALES[n] = new Colour();
      f.set(l, l, l, 1);
      final Colour g = GLOWS[n] = new Colour();
      g.set(1, 1, 1, -l);
    }
  }

  public static Colour transparency(float a) {
    return TRANSPARENCIES[Visit.clamp((int) (a * 100), 100)];
  }
  
  public static Colour greyscale(float a) {
    return GREYSCALES[Visit.clamp((int) (a * 100), 100)];
  }
  
  public static Colour glow(float a) {
    return GLOWS[Visit.clamp((int) (a * 100), 100)];
  }
  
  public static float combineAlphaBits(Colour base, Colour alpha) {
    return Float.intBitsToFloat(
      (Float.floatToRawIntBits(base .bitValue) & RGB_BITS  ) |
      (Float.floatToRawIntBits(alpha.bitValue) & ALPHA_BITS)
    );
  }
  
  
  
  /**
   * String description of this colour-
   */
  public String toString() {
    return "(Colour RGBA: " + r + " " + g + " " + b + " " + a + ")";
  }
}





/*
public void storeByteValue(byte puts[], int i) {
  puts[i + 0] = (byte) (r * 255);
  puts[i + 1] = (byte) (g * 255);
  puts[i + 2] = (byte) (b * 255);
  puts[i + 3] = (byte) (a * 255);
}


public void setFromBytes(byte vals[], int i) {
  r = (vals[i + 0] & 0xff) / 255f;
  g = (vals[i + 1] & 0xff) / 255f;
  b = (vals[i + 2] & 0xff) / 255f;
  a = (vals[i + 3] & 0xff) / 255f;
  bitValue = toFloatBits();
}
//*/
