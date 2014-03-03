/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;


/**  A Quaternion class, typically used to interpolate rotations smoothly.
  *  NOTE:  The class is mainly intended to handle unit quaternions.
  */
public class Quat {
  
  public float
    w = 0,
    x = 0,
    y = 0,
    z = 1 ;
  
  private static float
    sr, cr, sp, cp, sy, cy,  //used in computation from euler values.
    rx, py, yz,  //...used to compute euler values.
    qw, qx, qy, qz,  //used during multiplication.
    trace, div ;
  private static Quat tempQ = new Quat() ;  //used in temporary calculations.
  final private static float EPS = 0.0001f ;  //avoids rounding/div-by-0 errors.
  

  /**  Sets this quaternion to match the given argument values, and returns
    *  itself.
    */
  public Quat set(float x, float y, float z, float w) {
    this.x = x ;
    this.y = y ;
    this.z = z ;
    this.w = w ;
    return this ;
  }
  
  
  /**  Sets this quaternion to match the given argument values, and returns
    *  itself.
    */
  public Quat set(Quat quaternion) {
    w = quaternion.w ;
    x = quaternion.x ;
    y = quaternion.y ;
    z = quaternion.z ;
    return this ;
  }
  
  
  /**  Sets this quaternion to represent the euler rotation given.  Returns
    *  itself.
    */
  public Quat setEuler(float er, float ep, float ey) {  //roll, pitch, and yaw.
    er /= 2 ;
    ep /= 2 ;
    ey /= 2 ;
    sr = (float) (Math.sin(er)) ;
    cr = (float) (Math.cos(er)) ;
    sp = (float) (Math.sin(ep)) ;
    cp = (float) (Math.cos(ep)) ;
    sy = (float) (Math.sin(ey)) ;
    cy = (float) (Math.cos(ey)) ;
    w = (cr * cp * cy) + (sr * sp * sy) ;
    x = (sr * cp * cy) - (cr * sp * sy) ;
    y = (cr * sp * cy) + (sr * cp * sy) ;
    z = (cr * cp * sy) - (sr * sp * cy) ;
    return this ;
  }
  
  
  /**  Sets this quaternion to represent the rotation given by the matrix
    *  supplied.  NOTE:  Functions only with orthogonal matrices.
    *  Returns itself.
    */
  public Quat setMatrix(Mat3D matrix) {
    //doesn't exactly correspond to real matrix trace, but similar.
    trace = (matrix.r0c0 + matrix.r1c1 + matrix.r2c2 + 1) / 4 ;
    trace = (float) (Math.sqrt(trace)) ;
    
    if (trace >= EPS) {
      w = trace ;
      div = 0.25f / w ;
      x = (matrix.r2c1 - matrix.r1c2) * div ;
      y = (matrix.r0c2 - matrix.r2c0) * div ;
      z = (matrix.r1c0 - matrix.r0c1) * div ;
    }
    else {
      w = x = y = 0 ;
      z = 1 ;  //default 0-degree z-axis rotation otherwise.
    }
    return this ;
  }
  
  
  /**  Stores the rotation represented by this quaternion in euler notation
    *  within the given vector, then returned.  (If the vector is null, a new
    *  Vec3D is initialised instead.)
    */
  public Vec3D getEuler(Vec3D vector) {
    if (vector == null) vector = new Vec3D() ;
    
    rx = 2 * ((w * x) + (y * z)) / (1 - (2 * ((x * x) + (y * y)))) ;
    vector.x = (float) (Math.atan(rx)) ;
    py = 2 * ((w * y) - (x * z)) ;
    vector.y = (float) (Math.asin(py)) ;
    yz = 2 * ((w * z) + (x * y)) / (1 - (2 * ((y * y) + (z * z)))) ;
    vector.z = (float) (Math.atan(yz)) ;
    return vector ;
  }
  
  
  /**  Stores the rotation representated by this quaternion within the given 
    *  matrix, which is returned.  (If the matrix given is null, a new Mat3D is
    *  initialised for the purpose.)  NOTE: This must be set to a unit quaternion
    *  first.
    */
  public Mat3D putMatrixForm(Mat3D matrix) {
    if (matrix == null) matrix = new Mat3D() ;
    matrix.r0c0 = 1 - (2 * ((y * y) + (z * z))) ;
    matrix.r0c1 = 2 * ((x * y) - (w * z)) ;
    matrix.r0c2 = 2 * ((w * y) + (x * z)) ;
    matrix.r1c0 = 2 * ((x * y) + (w * z)) ;
    matrix.r1c1 = 1 - (2 * ((x * x) + (z * z))) ;
    matrix.r1c2 = 2 * ((y * z) - (w * x)) ;
    matrix.r2c0 = 2 * ((x * z) - (w * y)) ;
    matrix.r2c1 = 2 * ((w * x) + (y * z)) ;
    matrix.r2c2 = 1 - (2 * ((x * x) + (y * y))) ;
    return matrix ;
  }
  
  
  /**  Sets the magnitude of this quaternion to 1.  Returns itself.
    */
  public Quat setUnit() {
    float m = (float)(Math.sqrt((w * w) + (x * x) + (y * y) + (z * z))) ;
    w /= m ;
    x /= m ;
    y /= m ;
    z /= m ;
    return this ;
  }
  
  
  /**  Multiplies this quaternion by the first argument, then stores the result
    *  in the second.  (If the second argument is null, a new Quat is initialised
    *  and returned.)
    */
  public Quat mult(Quat quaternion, Quat result) {
    if (result == this) {
      set(mult(quaternion, tempQ)) ;
      return this ;
    }
    else if (result == null) result = new Quat() ;
    qw = quaternion.w ;
    qx = quaternion.x ;
    qy = quaternion.y ;
    qz = quaternion.z ;
    result.w = (w * qw) - ((x * qx) + (y * qy) + (z * qz)) ;
    result.x = (w * qx) +  (x * qw) + (y * qz) - (z * qy)  ;
    result.y = (w * qy) +  (y * qw) + (z * qx) - (x * qz)  ;
    result.z = (w * qz) +  (x * qy) + (z * qw) - (y * qx)  ;
    return result ;
  }
  
  
  /**  Performs a spherical linear (or great circle) interpolation between this
    *  quaternion and the first argument quaternion, using relAlpha to determine the
    *  extent of change, and storing the new values in the result quaternion.
    *  (If this argument is null, a new Quat is initialised and then returned.)
    *  NOTE:  Functions properly only with unit quaternions.
    */
  public Quat SLerp(Quat quaternion, float alpha, Quat result) {
    qw = quaternion.w ;
    qx = quaternion.x ;
    qy = quaternion.y ;
    qz = quaternion.z ;
    if (result == null) result = new Quat() ;
    
    float
      dot = (w * qw) + (x * qx) + (y * qy) + (z * qz),
      tw = 1,
      aw = 0 ;  //weights for this quaternion and the target.
    if (dot < 0) {  //Apparently, this ensures a shortest path is taken.
      tw = -1 ;  //(saves negating the first quaternion:  i.e, this.)
      dot = -dot ;
    }
    
    if (1 - dot < EPS) {  //...close to parallel.
      aw = alpha ;
      tw *= 1 - aw ;  //avoids divide_by_zero/rounding-related errors over sine...
    }
    else {
      double
        rad = Math.acos(dot),
        sin = Math.sin(rad) ;  //see?
      aw  = (float) (Math.sin(alpha * rad) / sin) ;
      tw *= (float) (Math.sin((1 - alpha) * rad) / sin) ;
    }
    
    result.w = (w * tw) + (qw * aw) ;
    result.x = (x * tw) + (qx * aw) ;
    result.y = (y * tw) + (qy * aw) ;
    result.z = (z * tw) + (qz * aw) ;
    return result ;
  }
  
  
  /**  Performs a normalised linear interpolation toward the first argument
    *  quaternion, with the relAlpha argument determining weight of averaging.
    *  The result is commutative and computationally cheaper than great circle
    *  interpolation, but lacks a constant velocity.  Final values are stored in
    *  the third argument (which, if  null, is initialised as a new Quat, and
    *  returned.)
    *  (NOTE:  Largely unsuited to quaternions at or near 180 degrees to one
    *  another.)
    */
  public Quat NLerp(Quat quaternion, float alpha, Quat result) {
    if (result == null) result = new Quat() ;
    
    float na = 1 - alpha ;
    result.w = (w * na) + (quaternion.w * alpha) ;
    result.x = (x * na) + (quaternion.x * alpha) ;
    result.y = (y * na) + (quaternion.y * alpha) ;
    result.z = (z * na) + (quaternion.z * alpha) ;
    result.setUnit() ;
    
    return result ;
  }
  
  
  public String toString() {
    return " ( " + x + " " + y + " " + z + " " + w + " )" ;
  }
}
