/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;
import java.io.*;


/**  3x3 Matrix used commonly in vector rotations, optimised for certain
  *  purposes.
  */
public class Mat3D {
  
	
  public float
    r0c0, r0c1, r0c2,
    r1c0, r1c1, r1c2,
    r2c0, r2c1, r2c2;  //row and column indices.
  
  private static float vx, vy, vz;  //used in computation.
  private static float cr, sr, cp, sp, cy, sy;
  private static Mat3D tempM = new Mat3D();  //...likewise.
  
  public Mat3D() {}
  
  public Mat3D loadFrom(DataInputStream in) throws Exception {
    r0c0 = in.readFloat();
    r0c1 = in.readFloat();
    r0c2 = in.readFloat();
    r1c0 = in.readFloat();
    r1c1 = in.readFloat();
    r1c2 = in.readFloat();
    r2c0 = in.readFloat();
    r2c1 = in.readFloat();
    r2c2 = in.readFloat();
    return this;
  }
  
  public Mat3D saveTo(DataOutputStream out) throws Exception {
    out.writeFloat(r0c0);
    out.writeFloat(r0c1);
    out.writeFloat(r0c2);
    out.writeFloat(r1c0);
    out.writeFloat(r1c1);
    out.writeFloat(r1c2);
    out.writeFloat(r2c0);
    out.writeFloat(r2c1);
    out.writeFloat(r2c2);
    return this;
  }
  
  
  /**  Sets the matrix to the specif ied values, row first, column second.
    */
  public Mat3D set(
      float m00, float m01, float m02,
      float m10, float m11, float m12,
      float m20, float m21, float m22)
  {
    r0c0 = m00;
    r0c1 = m01;
    r0c2 = m02;
    r1c0 = m10;
    r1c1 = m11;
    r1c2 = m12;
    r2c0 = m20;
    r2c1 = m21;
    r2c2 = m22;
    return this;
  }
  
  /**  Sets the matrix to the specif ied array values, (row first, column
    *  second.)
    */
  public Mat3D set(float m[][]) {
    return set(
      m[0][0], m[0][1], m[0][2],
      m[1][0], m[1][1], m[1][2],
      m[2][0], m[2][1], m[2][2]
    );
  }
  
  /**  Sets the matrix to a copy of the given matrix' values.
    */
  public Mat3D setTo(Mat3D m) {
    return set(
      m.r0c0, m.r0c1, m.r0c2,
      m.r1c0, m.r1c1, m.r1c2,
      m.r2c0, m.r2c1, m.r2c2
    );
  }
  
  /**  Sets this matrix to match a rotation as represented by the given euler
    *  values (i.e roll, pitch, and yaw.)
    */
  public Mat3D setEuler(float er, float ep, float ey) {
    cr = Nums.cos(er);
    sr = Nums.sin(er);
    cp = Nums.cos(ep);
    sp = Nums.sin(ep);
    cy = Nums.cos(ey);
    sy = Nums.sin(ey);
    
    r0c0 = cp * cy;
    r1c0 = cp * sy;
    r2c0 = 0 - sp;
    r0c1 = (sr * sp * cy) - (cr * sy);
    r1c1 = (sr * sp * sy) + (cr * cy);
    r2c1 = sr * cp;
    r0c2 = (cr * sp * cy) + (sr * sy);
    r1c2 = (cr * sp * sy) - (sr * cy);
    r2c2 = cr * cp;
    return this;
  }
  
  /**  Rotates this matrix by the given value about the X axis.
    */
  public Mat3D rotateX(float radians) {
    cr = Nums.cos(radians);
    sr = Nums.sin(radians);
    tempM.setIdentity();
    tempM.r1c1 = cr;
    tempM.r1c2 = sr;
    tempM.r2c1 = 0 - sr;
    tempM.r2c2 = cr;
    trans(tempM, tempM);
    return setTo(tempM);
  }
  
  /**  Rotates this matrix by the given value about the Y axis.
    */
  public Mat3D rotateY(float radians) {
    cr = Nums.cos(radians);
    sr = Nums.sin(radians);
    tempM.setIdentity();
    tempM.r0c0 = cr;
    tempM.r0c2 = sr;
    tempM.r2c0 = 0 - sr;
    tempM.r2c2 = cr;
    trans(tempM, tempM);
    return setTo(tempM);
  }
  
  /**  Rotates this matrix by the given value about the Z axis.
    */
  public Mat3D rotateZ(float radians) {
    cr = Nums.cos(radians);
    sr = Nums.sin(radians);
    tempM.setIdentity();
    tempM.r0c0 = cr;
    tempM.r0c1 = sr;
    tempM.r1c0 = 0 - sr;
    tempM.r1c1 = cr;
    trans(tempM, tempM);
    return setTo(tempM);
  }
  
  /**  Sets the matrix to identity (all zero, excepting diagonally down-right.)
    *  Returns itself.
    */
  public Mat3D setIdentity() {
    r0c0 = r1c1 = r2c2 = 1;
    r0c1 = r0c2 = r1c0 = r1c2 = r2c0 = r2c1 = 0;
    return this ;
  }
  
  /**  Sets the matrix to all zeroes.  Returns itself.
    */
  public Mat3D setZero() {
    r0c0 = r0c1 = r0c2 = 
      r1c0 = r1c1 = r1c2 = 
        r2c0 = r2c1 = r2c2 = 0;
    return this ;
  }
  
  /**  Transforms the vector represented by the float array given at 3x the
    *  given index, then stores in the corresponding place at the result array.
    */
  public void trans(float vecs[], int index, float result[]) {
    index *= 3;
    vx = vecs[index++];
    vy = vecs[index++];
    vz = vecs[index];
    index -= 2;
    
    result[index++] = (vx * r0c0) + (vy * r0c1) + (vz * r0c2);
    result[index++] = (vx * r1c0) + (vy * r1c1) + (vz * r1c2);
    result[index] = (vx * r2c0) + (vy * r2c1) + (vz * r2c2);
  }
  
  public void trans(float vecs[], int start, int end, float result[]) {
    start *= 3;
    end *= 3;
    while (start < end) {
      vx = vecs[start    ];
      vy = vecs[start + 1];
      vz = vecs[start + 2];
      result[start++] = (vx * r0c0) + (vy * r0c1) + (vz * r0c2);
      result[start++] = (vx * r1c0) + (vy * r1c1) + (vz * r1c2);
      result[start++] = (vx * r2c0) + (vy * r2c1) + (vz * r2c2);
    }
  }

  /**  Multiplies the vector given in-place and return the same vector.
    */
  public Vec3D trans(Vec3D vector, Vec3D result) {
    if (result == null) result = new Vec3D();
    result.setTo(vector);
    return trans(result);
  }
  
  /**  Multiplies the matrix given in-place and return the same matrix.
    */
  public Mat3D trans(Mat3D matrix) { return trans(matrix, matrix); }

  /**  Inverts this matrix in-place and returns itself.
    */
  public Mat3D invert() { return inverse(this); }
  
  /**  Transforms the vector given.
    */
  public final Vec3D trans(final Vec3D vector) {
    final float
      vx = vector.x,
      vy = vector.y,
      vz = vector.z;
    vector.x = (vx * r0c0) + (vy * r0c1) + (vz * r0c2);
    vector.y = (vx * r1c0) + (vy * r1c1) + (vz * r1c2);
    vector.z = (vx * r2c0) + (vy * r2c1) + (vz * r2c2);
    return vector;
  }
  
  
  /**  Multiplies the matrix given and places the result in a second matrix.
    *  (If the second argument is null, a new Mat3D is initialised and
    *  returned.)
    */
  public Mat3D trans(Mat3D matrix, Mat3D result) {
    if (result == null) result = new Mat3D();
    else if (this == result) {
      //don't go altering your own data in mid-algorithm...
      trans(matrix, tempM);
      setTo(tempM);
      return this;
    }
    
    vx = matrix.r0c0;
    vy = matrix.r1c0;
    vz = matrix.r2c0;
    result.r0c0 = (vx * r0c0) + (vy * r0c1) + (vz * r0c2);
    result.r1c0 = (vx * r1c0) + (vy * r1c1) + (vz * r1c2);
    result.r2c0 = (vx * r2c0) + (vy * r2c1) + (vz * r2c2);
    vx = matrix.r0c1;
    vy = matrix.r1c1;
    vz = matrix.r2c1;
    result.r0c1 = (vx * r0c0) + (vy * r0c1) + (vz * r0c2);
    result.r1c1 = (vx * r1c0) + (vy * r1c1) + (vz * r1c2);
    result.r2c1 = (vx * r2c0) + (vy * r2c1) + (vz * r2c2);
    vx = matrix.r0c2;
    vy = matrix.r1c2;
    vz = matrix.r2c2;
    result.r0c2 = (vx * r0c0) + (vy * r0c1) + (vz * r0c2);
    result.r1c2 = (vx * r1c0) + (vy * r1c1) + (vz * r1c2);
    result.r2c2 = (vx * r2c0) + (vy * r2c1) + (vz * r2c2);
    
    return result;
  }
  
  /**  Places the inverse values for this matrix within the argument (which, if 
    *  null, is initialised as a new Mat3D and returned.)
    */
  public Mat3D inverse(Mat3D result) {
    if (result == null) result = new Mat3D();
    else if (result == this) {
      //  Don't go altering your own data in mid-algorithm...
      inverse(tempM);
      setTo(tempM);
      return this;
    }
    float det = determinant();
    if (Nums.abs(det) < 0.0001f) det = 1.0f;
    else det = 1 / det;
    //
    //  Straightforward cookbook formula used:
    result.r0c0 = ((r2c2 * r1c1) - (r2c1 * r1c2)) * det;
    result.r0c1 = ((r2c1 * r0c2) - (r2c2 * r0c1)) * det;
    result.r0c2 = ((r1c2 * r0c1) - (r1c1 * r0c2)) * det;
    result.r1c0 = ((r2c0 * r1c2) - (r2c2 * r1c0)) * det;
    result.r1c1 = ((r2c2 * r0c0) - (r2c0 * r0c2)) * det;
    result.r1c2 = ((r1c0 * r0c2) - (r1c2 * r0c0)) * det;
    result.r2c0 = ((r2c1 * r1c0) - (r2c0 * r1c1)) * det;
    result.r2c1 = ((r2c0 * r0c1) - (r2c1 * r0c0)) * det;
    result.r2c2 = ((r1c1 * r0c0) - (r1c0 * r0c1)) * det;
    return result;
  }
  
  /**  Returns this matrix's determinant.
    */
  public float determinant() {
    return
      (r0c0 * ((r2c2 * r1c1) - (r2c1 * r1c2))) -
      (r1c0 * ((r2c2 * r0c1) - (r2c1 * r0c2))) +
      (r2c0 * ((r1c2 * r0c1) - (r1c1 * r0c2)));
  }
  
  public String toString() {
  	return "\n( "+ r0c0 + " " + r0c1 + " " + r0c2 + " )\n( " +
  	               r1c0 + " " + r1c1 + " " + r1c2 + " )\n( " +
  	               r2c0 + " " + r2c1 + " " + r2c2 + " )";
  }
}
