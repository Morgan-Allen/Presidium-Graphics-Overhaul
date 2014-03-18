/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package code.util ;
import java.io.* ;
import org.apache.commons.math3.util.FastMath;



public class Vec2D {
  
  private static Vec2D temp = new Vec2D() ;
  
  public float
    x,
    y ;
  
  
  public Vec2D() {}
  
  public Vec2D loadFrom(DataInputStream in) throws Exception {
    x = in.readFloat() ;
    y = in.readFloat() ;
    return this ;
  }
  
  public Vec2D saveTo(DataOutputStream out) throws Exception {
    out.writeFloat(x) ;
    out.writeFloat(y) ;
    return this ;
  }
  
  public Vec2D(float xv, float yv) {
    set(xv, yv) ;
  }
  
  public Vec2D(Vec2D v) {
    setTo(v) ;
  }
  
  
  /**  Sets the vector to given x y values.
    */
  public Vec2D set(float xv, float yv) {
    x = xv ; y = yv ;
    return this ;
  }

  /**  Sets the vector to given x y vector values.
    */
  public Vec2D setTo(Vec3D vector) {
    x = vector.x ;
    y = vector.y ;
    return this ;
  }
  
  /**  Sets this vector to match the argument values.
    */
  public Vec2D setTo(Vec2D vector) {
    x = vector.x ;
    y = vector.y ;
    return this ;
  }
  

  /**  Adds the argument vector to this vector in-place and returns itself.
    */
  public Vec2D add(Vec2D vector) { return add(vector, this)  ; }
  

  /**  Subtracts the argument vector to this vector in-place and returns itself.
    */
  public Vec2D sub(Vec2D vector) { return sub(vector, this)  ; }
  

  /**  Scales this vector by the first argument in-place and returns itself.
    */
  public Vec2D scale(float s) { return scale(s, this)  ; }
  

  /**  Sets this vector to length == 1 and returns itself.
    */
  public Vec2D normalised() { return normalise(this)  ; }
  
  
  /**  Sets this vector as perpendicular to its former value, and returns
    *  itself.
    */
  public Vec2D perp() { return perp(this)  ; }
  
  
  /**  Adds the argument vector to this vector and stores the new values in
    *  result.  (If the result vector is null, a new Vec2D is initialised and
    *  returned.)
    */
  public Vec2D add(Vec2D vector, Vec2D result) {
    if (result == null) result = new Vec2D()  ;
    result.x = x + vector.x  ;
    result.y = y + vector.y  ;
    return result  ;
  }
  
  
  /**  Subtractss the argument vector to this vector and stores the new values
    *  in result.  (If the result vector is null, a new Vec2D is initialised and
    *  returned.)
    */
  public Vec2D sub(Vec2D vector, Vec2D result) {
    if (result == null) result = new Vec2D()  ;
    result.x = x - vector.x  ;
    result.y = y - vector.y  ;
    return result  ;
  }
  
  
  /**  Scales this vector by the first argument and stores the value in the
    *  second.  (If the result vector is null, a new Vec2D is initialised and
    *  returned.)
    */
  public Vec2D scale(float s, Vec2D result) {
    if (result == null) result = new Vec2D() ;
    result.x = x * s ;
    result.y = y * s ;
    return result ;
  }
  

  /**  Sets this Vec2D to length == 1.
    */
  public Vec2D normalise() {
    return normalise(this) ;
  }
  
  
  /**  Sets this Vec2D to length == 1, storing the result in the argument.  (If
    *  the result vector is null, a new Vec2D is initialised and returned.)
    */
  public Vec2D normalise(Vec2D result) {
    if (result == null) result = new Vec2D() ;
    float l = (float)(Math.sqrt((x * x) + (y * y))) ;
    if (l > 0) scale(1 / l, result) ;
    return result ;
  }
  
  
  /**  Returns the length of this vector.
    */
  public float length() {
    return (float) (Math.sqrt((x * x) + (y * y))) ;
  }
  
  
  /**  Returns the squared length of this vector.
    */
  public float lengthSquared() {
    return (x * x) + (y * y) ;
  }
  
  
  /**  Sets the argument vector as perpendicular to this Vec2D.
    */
  public Vec2D perp(Vec2D result) {
    if (result == null) result = new Vec2D() ;
    result.set(y, 0 - x) ;
    return result ;
  }
  
  
  /**  Returns this vector's dot product value with given argument.
    */
  public float dot(Vec2D vector) {
    return (x * vector.x) + (y * vector.y) ;
  }
  
  
  /**  Returns this vector's line distance from the point represented in the
    *  given argument vector.  This value may be either positive (if the given
    *  vector is to the 'right',) or negative, (if to the 'left',) and is
    *  multiplied by the length of this vector.
    */
  public float side(Vec2D vector) {
    return (y * vector.x) - (x * vector.y) ;
  }
  
  
  public float side(float x, float y) {
    return (y * this.x) - (x * this.y) ;
  }
  
  
  
  /**  Returns the given point vector's distance from the line represented by
    *  this vector.  This value is always positive, and not scaled by vector
    *  length.
    */
  public float lineDist(Vec2D vector) {
    return Math.abs(side(vector) / length()) ;
  }
  
  
  public float lineDist(float x, float y) {
    return Math.abs(side(x, y) / length()) ;
  }
  
  

  /**  Returns the given point vector's distance from the point represented by
    *  this vector.
    */
  public float pointDist(Vec2D vector) {
    temp.x = x - vector.x ;
    temp.y = y - vector.y ;
    return temp.length() ;
  }
  
  
  public float pointDist(float x, float y) {
    temp.x = x - this.x ;
    temp.y = y - this.y ;
    return temp.length() ;
  }
  
  
  /**  Returns this vector's value interpreted as an angle ((1, 0) == 0 degrees,
    *  (0, 1) == 90 degrees, (-1, 0) == 180 degree, etc.)  The value returned is
    *  in degrees.
    */
  public float toAngle() {
    return (float) (FastMath.atan2(y, x) * 180 / Math.PI) ;
  }
  

  /**  Helper method: returns the signed difference between two angles (between
    *  0 and 360 degrees).  (This is a1 *minus* a2.)
    */
  public static float degreeDif(final float a1, final float a2) {
    final float d = a1 - a2, aD = Math.abs(d) ;
    return (aD < 180) ? d : ((360 - aD) * ((d > 0) ? -1 : 1)) ;
  }
  
  
  /**  Sets this vector to match the given angle value (0 degrees == (1, 0), 90
    *  degrees == (0, 1), etc.)  The argument value must be in degrees.
    */
  public Vec2D setFromAngle(float angle) {
    angle *= Math.PI / 180 ;
    x = (float) Math.cos(angle) ;
    y = (float) Math.sin(angle) ;
    return this ;
  }
  
  
  public String toString() {
    return " ( " + x + " " + y + " )" ;
  }
}
