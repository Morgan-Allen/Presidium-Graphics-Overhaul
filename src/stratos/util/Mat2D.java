/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;
import java.io.*;
import org.apache.commons.math3.util.FastMath;


public class Mat2D {
	
	public float
	  r0c0, r0c1,
	  r1c0, r1c1;
	private static Mat2D tempM = new Mat2D();

  
  public Mat2D() {}
  
  public Mat2D loadFrom(DataInputStream in) throws Exception {
    r0c0 = in.readFloat();
    r0c1 = in.readFloat();
    r1c0 = in.readFloat();
    r1c1 = in.readFloat();
    return this;
  }
  
  public Mat2D saveTo(DataOutputStream out) throws Exception {
    out.writeFloat(r0c0);
    out.writeFloat(r0c1);
    out.writeFloat(r1c0);
    out.writeFloat(r1c1);
    return this;
  }
	

  /**  Sets the matrix to the specif ied values, row first, column second.
    */
  public Mat2D set(
      float m00, float m01,
      float m10, float m11)
  {
    r0c0 = m00;
    r0c1 = m01;
    r1c0 = m10;
    r1c1 = m11;
    return this;
  }
  
  
  /**  Sets the matrix to the specif ied array values, (row first, column
   *  second.)
   */
  public Mat2D set(float m[][]) {
    return set(
      m[0][0], m[0][1],
      m[1][0], m[1][1]
    );
  }
  
  
  /**  Sets the matrix to a copy of the given matrix' values.
    */
  public Mat2D setTo(Mat2D m) {
    return set(
      m.r0c0, m.r0c1,
      m.r1c0, m.r1c1
    );
  }

  
  /**  Sets the matrix to identity (all zero, excepting diagonally down-right.)
    *  Returns itself.
    */
  public Mat2D setIdentity() {
    r0c0 = r1c1 = 1;
    r0c1 = r1c0 = 0;
    return this;
  }
  
  
  /**  Sets the matrix to all zeroes.  Returns itself.
    */
  public Mat2D setZero() {
    r0c0 = r0c1 =
      r1c0 = r1c1 = 0;
    return this;
  }
  
  
  /**  Rotates this matrix anticlockwise by the given value.
    */
  public Mat2D rotate(float radians) {
    float
      cr = (float)(FastMath.cos(radians)),
      sr = (float)(FastMath.sin(radians));
    tempM.setIdentity();
    tempM.r0c0 = cr;
    tempM.r0c1 = sr;
    tempM.r1c0 = 0 - sr;
    tempM.r1c1 = cr;
    trans(tempM, tempM);
    return setTo(tempM);
  }
  

  /**  Multiplies the vector given in-place and return the same vector.
    */
  public Vec2D transform(Vec2D vector) { return trans(vector, vector); }
  
  
  /**  Multiplies the matrix given in-place and return the same matrix.
    */
  public Mat2D trans(Mat2D matrix) { return trans(matrix, matrix); }
  

  /**  Inverts this matrix in-place and returns itself.
    */
  public Mat2D invert() { return inverse(this); }
  
  
  /**  Places the inverse values for this matrix within the argument (which, if 
    *  null, is initialised as a new Mat3D and returned.)
    */
	public Mat2D inverse(Mat2D result) {
	  if (result == null)
	    result = new Mat2D();
    else if (result == this) {
      //don't go altering your own data in mid-algorithm...
      inverse(tempM);
      setTo(tempM);
      return this;
    }
	  
	  float det = determinant();
	  result.r0c0 = r1c1 / det;
	  result.r0c1 = 0 - r0c1 / det;
	  result.r1c0 = 0 - r1c0 / det;
	  result.r1c1 = r0c0 / det;
		return result;
	}
	
  
  /**  Transforms the vector given and places the result in the second vector.
    *  (If this second vector is null, a new Vec3D is initialised and returned.)
    */
  public Vec2D trans(Vec2D vector, Vec2D result) {
    if (result == null)
      result = new Vec2D();
    float
      vx = vector.x,
      vy = vector.y;
    result.x = (vx * r0c0) + (vy * r0c1);
    result.y = (vx * r1c0) + (vy * r1c1);
    return result;
  }
  
  
  /**  Multiplies the matrix given and places the result in a second matrix.
    *  (If the second argument is null, a new Mat3D is initialised and
    *  returned.)
    */
  public Mat2D trans(Mat2D matrix, Mat2D result) {
    if (result == null)
      result = new Mat2D();
    else if (this == result) {
      //don't go altering your own data in mid-algorithm...
      trans(matrix, tempM);
      setTo(tempM);
      return this;
    }
    float vx, vy;
    vx = matrix.r0c0;
    vy = matrix.r1c0;
    result.r0c0 = (vx * r0c0) + (vy * r0c1);
    result.r1c0 = (vx * r1c0) + (vy * r1c1);
    vx = matrix.r0c1;
    vy = matrix.r1c1;
    result.r0c1 = (vx * r0c0) + (vy * r0c1);
    result.r1c1 = (vx * r1c0) + (vy * r1c1);
    
    return result;
  }
  
  
  /**  Returns this matrix's determinant.
    */
  public float determinant() {
    return (r0c0 * r1c1) - (r0c1 * r1c0);
  }
  
  
  public String toString() {
    return
      "\n( " + r0c0 + " " + r0c1 + " )\n( " +
             r1c0 + " " + r1c1 + " )";
  }
}
