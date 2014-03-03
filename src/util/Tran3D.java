/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;


/**  A transform class that incorporates both rotation and translation for the
  *  application's convenience.
  */
public class Tran3D {
  
  public final Vec3D position = new Vec3D().set(0, 0, 0) ;
  public final Mat3D rotation = new Mat3D().setIdentity() ;
  
  
  /**  Setup based on existing position and rotation vector/matrix.  Returns
    *  itself.
    */
  public Tran3D setup(Vec3D pos, Mat3D rot) {
  	position.setTo(pos) ;
  	rotation.setTo(rot) ;
    return this ;
  }
  
  
  /**  Sets the transform to match the given transform's values.  Returns
    *  itself.
    */
  public Tran3D setTo(Tran3D transform) {
  	rotation.setTo(transform.rotation) ;
  	position.setTo(transform.position) ;
    return this ;
  }
  
  
  /**  Sets this transform to the inverse of the argument.  Returns itself.
    */
  public Tran3D setInverse(Tran3D transform) {
    //
    //  NOTE:  matrix transforms are linear operations, i.e. (mat(a) + mat(b) =
    //  mat(a + b), so the following works.
  	transform.rotation.inverse(rotation) ;
  	position.setTo(transform.position) ;
  	position.scale(-1, position) ;
  	rotation.trans(position) ;
    return this ;
  }
  
  final public void trans(final Vec3D vector, final Vec3D result) {
    result.setTo(vector) ;
    trans(result) ;
  }
  
  
  /**  Performs a rotation + translation on the given vector and stores the
    *  answer in the result vector.
    */
  final public void trans(final Vec3D vector) {
    rotation.trans(vector) ;
    vector.add(position) ;
  }
  
  
  /**  Performs a rotation + translation on the given transforms' position, a
    *  rotation on the transform's own, and stores all values in the result
    *  transform.
    */
  public void trans(Tran3D transform, Tran3D result) {
    result.position.setTo(transform.position) ;
  	rotation.trans(result.position) ;
  	rotation.trans(transform.rotation, result.rotation) ;
  	result.position.add(position, result.position) ;
  }
}
