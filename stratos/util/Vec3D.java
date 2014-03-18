/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util ;
import java.io.* ;
import org.apache.commons.math3.util.FastMath ;



/**  3-dimensional Vector class, also used for points by differing methods.
  */
public class Vec3D {
  
	private static Vec3D temp = new Vec3D() ;
	
  public float x, y, z ;
  
  
  public Vec3D() {}
  
  public Vec3D loadFrom(DataInputStream in) throws Exception {
    x = in.readFloat() ;
    y = in.readFloat() ;
    z = in.readFloat() ;
    return this ;
  }
  
  public Vec3D saveTo(DataOutputStream out) throws Exception {
    out.writeFloat(x) ;
    out.writeFloat(y) ;
    out.writeFloat(z) ;
    return this ;
  }
  
  
  public Vec3D(final float xv, final float yv, final float zv) {
    x = xv ; y = yv ; z = zv ;
  }
  
  public Vec3D(final Vec3D v) {
    this.x = v.x ; this.y = v.y ; this.z = v.z ;
  }
  
  
  /**  Sets the vector to given x y z values.
    */
  public Vec3D set(float xv, float yv, float zv) {
    x = xv ; y = yv ; z = zv ;
    return this ;
  }
  
  
  /**  Sets this vector to match the argument values.
    */
  public final Vec3D setTo(final Vec3D vector) {
    x = vector.x ;
    y = vector.y ;
    z = vector.z ;
    return this ;
  }
  
  
  /**  Sets the vector to given x y vector values (z is zero.)
    */
  public Vec3D setTo(Vec2D vector) {
    x = vector.x ;
    y = vector.y ;
    z = 0 ;
    return this ;
  }
  
  
  /**  Sets this vector to match the given quaternion's xyz values.
    */
  public Vec3D set(Quat quaternion) {
  	x = quaternion.x ;
  	y = quaternion.y ;
  	z = quaternion.z ;
    return this ;
  }
  
  
  /**  Adds itself to the argument vector and places it in result.
    */
  public Vec3D add(Vec3D vector, Vec3D result) {
    result.setTo(this) ;
    return result.add(vector) ;
  }
  

  /**  Subtracts the argument vector to this vector in-place and returns itself.
    */
  public Vec3D sub(Vec3D vector) { return sub(vector, this)  ; }
  
  
  /**  Stores the vectors' cross-product value with the first argument in this
    *  vector, and returns itself.
    */
  public Vec3D cross(Vec3D vector) { return cross(vector, this)  ; }
  
  
  /**  Adds the argument vector to this vector and stores the new values in
    *  result.  (If the result vector is null, a new Vec3D is initialised and
    *  returned.)
    */
  public final Vec3D add(final Vec3D vector) {
    x += vector.x ;
    y += vector.y ;
    z += vector.z ;
    return this ;
  }
  
  
  /**  Adds the first vector times the give scale factor to this vector and
    *  stores the new values in result.  (If the result vector is null, a new
    *  Vec3D is initialised and returned.)
    */
  public Vec3D add(Vec3D vector, float scale, Vec3D result) {
    if (result == null) result = new Vec3D() ;
    result.x = x + (vector.x * scale) ;
    result.y = y + (vector.y * scale) ;
    result.z = z + (vector.z * scale) ;
    return result ;
  }
  
  
  /**  Subtracts the argument vector to this vector and stores the new values
    *  in result.  (If the result vector is null, a new Vec3D is initialised
    *  and returned.)
    */
  public Vec3D sub(Vec3D vector, Vec3D result) {
    if (result == null) result = new Vec3D() ;
    result.x = x - vector.x ;
    result.y = y - vector.y ;
    result.z = z - vector.z ;
    return result ;
  }
  
  
  /**  Scales this vector by the first argument and stores the value in the
    *  second.  (If the result vector is null, a new Vec3D is initialised and
    *  returned.)
   */
  public Vec3D scale(float s, Vec3D result) {
    if (result == null) result = new Vec3D() ;
    result.x = x * s ;
    result.y = y * s ;
    result.z = z * s ;
    return result ;
  }
  
  
  /**  Scales this vector by the given argument.
   */
  public Vec3D scale(final float s) {
    x *= s ;
    y *= s ;
    z *= s ;
    return this ;
  }
  

  /**  Sets this Vec3D to length == 1 and returns itself.
    */
  public Vec3D normalise() {
    return normalise(this) ;
  }
  
  /**  Sets this Vec3D to length == 1, storing the result in the argument.  (If
    *  the result vector is null, a new Vec3D is initialised and returned.)
    */
  public Vec3D normalise(Vec3D result) {
    if (result == null) result = new Vec3D() ;
    float l = (float) (FastMath.sqrt((x * x) + (y * y) + (z * z))) ;
    if (l > 0) scale(1 / l, result) ;
    return result ;
  }
  
  
  /**  Returns this vector's dot product value with given argument.
    */
  public float dot(Vec3D vector) {
    return (x * vector.x) + (y * vector.y) + (z * vector.z) ;
  }
  
  
  /**  Returns the length of this vector.
    */
  public float length() {
    return (float) (FastMath.sqrt((x * x) + (y * y) + (z * z))) ;
  }
  
  
  /**  Returns the distance between this vector and the argument.
    */
  public float distance(Vec3D v) {
    final float dX = x - v.x, dY = y - v.y, dZ = z - v.z ;
    return (float) (FastMath.sqrt((dX * dX) + (dY * dY) + (dZ * dZ))) ;
  }
  
  
  public float distance(float vX, float vY, float vZ) {
    final float dX = x - vX, dY = y - vY, dZ = z - vZ ;
    return (float) (FastMath.sqrt((dX * dX) + (dY * dY) + (dZ * dZ))) ;
  }
  
  
  /**  Stores the vectors' cross-product value with the first argument in the
    *  second (which, if  null, is initialised and returned.)
    */
  public Vec3D cross(Vec3D vector, Vec3D result) {
    if (result == null) result = new Vec3D() ;
    else if (result == this) {
    	cross(vector, temp) ;
    	setTo(temp) ;
    	return this ;
    }
    result.x = (vector.y * z) - (vector.z * y) ;
    result.y = (vector.z * x) - (vector.x * z) ;
    result.z = (vector.x * y) - (vector.y * x) ;
    return result ;
  }
  
  
  public String toString() {
    return " ( " + x + " " + y + " " + z + " )" ;
  }
}
