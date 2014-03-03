/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;

import java.io.DataInputStream;
import java.io.DataOutputStream;


/**  Used to describe a 3-dimensional bounding box with both position and size.
  */
public class Box3D {
	
	protected float
	  xpos,
	  ypos,
	  zpos,
	  xmax,
	  ymax,
	  zmax,
	  xdim,
	  ydim,
	  zdim ;
	
	
  //  Assorted no-brainer accessor/mutator methods for encapsulations' sake.
  final public float xpos() { return xpos ; }
  final public float ypos() { return ypos ; }
  final public float zpos() { return zpos ; }
  final public float xdim() { return xdim ; }
  final public float ydim() { return ydim ; }
  final public float zdim() { return zdim ; }
  final public float xmax() { return xmax ; }
  final public float ymax() { return ymax ; }
  final public float zmax() { return zmax ; }
  //  Slight complications involved.
  final public void xpos(float x) { xmax = (xpos = x) + xdim ; }
  final public void ypos(float y) { ymax = (ypos = y) + ydim ; }
  final public void zpos(float z) { zmax = (zpos = z) + zdim ; }
  final public void xdim(float x) { xmax = xpos + (xdim = x) ; }
  final public void ydim(float y) { ymax = ypos + (ydim = y) ; }
  final public void zdim(float z) { zmax = zpos + (zdim = z) ; }
  final public void xmax(float x) { xdim = (xmax = x) - xpos ; }
  final public void ymax(float y) { ydim = (ymax = y) - ypos ; }
  final public void zmax(float z) { zdim = (zmax = z) - zpos ; }
  
  
  public Box3D loadFrom(DataInputStream in) throws Exception {
    xpos = in.readFloat() ;
    ypos = in.readFloat() ;
    zpos = in.readFloat() ;
    xdim = in.readFloat() ;
    ydim = in.readFloat() ;
    zdim = in.readFloat() ;
    xmax = in.readFloat() ;
    ymax = in.readFloat() ;
    zmax = in.readFloat() ;
    return this ;
  }
  
  public Box3D saveTo(DataOutputStream out) throws Exception {
    out.writeFloat(xpos) ;
    out.writeFloat(ypos) ;
    out.writeFloat(zpos) ;
    out.writeFloat(xdim) ;
    out.writeFloat(ydim) ;
    out.writeFloat(zdim) ;
    out.writeFloat(xmax) ;
    out.writeFloat(ymax) ;
    out.writeFloat(zmax) ;
    return this ;
  }
  
  
  /**  Copies the argument's values naturally.
    */
  public Box3D setTo(Box3D box) {
    xpos = box.xpos ;
    ypos = box.ypos ;
    zpos = box.zpos ;
    xdim = box.xdim ;
    ydim = box.ydim ;
    zdim = box.zdim ;
    xmax = box.xmax ;
    ymax = box.ymax ;
    zmax = box.zmax ;
    return this ;
  }
  
  
  /**  Default setup method.  The first three arguments specify position, the
    *  last three specify size, in x, y and z, respectively.
    */
  public Box3D set(float x, float y, float z, float xs, float ys, float zs) {
    xpos = x ;
    ypos = y ;
    zpos = z ;
    xdim = xs ;
    ydim = ys ;
    zdim = zs ;
    xmax = xpos + xdim ;
    ymax = ypos + ydim ;
    zmax = zpos + zdim ;
    return this ;
  }
  
  public Box3D set(Vec3D v, float r) {
    return set(v.x - r, v.y - r, v.z - r, r * 2, r* 2, r * 2) ;
  }
  
  
  /**  Returns whether this box and the argument intersect.
    */
  public boolean intersects(Box3D box) {
    if (xmax < box.xpos) return false ;
    if (ymax < box.ypos) return false ;
    if (zmax < box.zpos) return false ;
    if (xpos > box.xmax) return false ;
    if (ypos > box.ymax) return false ;
    if (zpos > box.zmax) return false ;
    return true ;
  }
  
  
  /**  Returns whether this box is contained (bounded) by the argument box.
    */
  public boolean containedBy(Box3D box) {
    return
      (xpos >= box.xpos) &&
      (ypos >= box.ypos) &&
      (zpos >= box.zpos) &&
      (xmax <= box.xmax) &&
      (ymax <= box.ymax) &&
      (zmax <= box.zmax) ;
  }
	
  
  /**  Returns whether this box contains the given point.
    */
	public boolean contains(Vec3D vector) {
		return
		  (xpos < vector.x) &&
		  (ypos < vector.y) &&
		  (zpos < vector.z) &&
		  (xmax > vector.x) &&
		  (ymax > vector.y) &&
		  (zmax > vector.z) ;
	}
  
  
  /**  Expands this box to include the argument box.
    */
  public void include(Box3D box) {
    if (xpos > box.xpos) xpos = box.xpos ;
    if (ypos > box.ypos) ypos = box.ypos ;
    if (zpos > box.zpos) zpos = box.zpos ;
    if (xmax < box.xmax) xmax = box.xmax ;
    if (ymax < box.ymax) ymax = box.ymax ;
    if (zmax < box.zmax) zmax = box.zmax ;
    xdim = xmax - xpos ;
    ydim = ymax - ypos ;
    zdim = zmax - zpos ;
  }
  
  
  /**  Returns the euclidean distance of the given point from the boundaries of
    *  this box.
    */
  public float distance(final float xp, final float yp, final float zp) {
    final float
      xd = (xp < xpos) ? (xpos - xp) : ((xp > xmax) ? (xp - xmax) : 0),
      yd = (yp < ypos) ? (ypos - yp) : ((yp > ymax) ? (yp - ymax) : 0),
      zd = (zp < zpos) ? (zpos - zp) : ((zp > zmax) ? (zp - zmax) : 0) ;
    return (float) Math.sqrt((xd * xd) + (yd * yd) + (zd * zd)) ;
  }
  
  
  public Vec3D centre() {
    return new Vec3D().set(
      (xpos + xmax) / 2,
      (ypos + ymax) / 2,
      (zpos + zmax) / 2
    ) ;
  }
  
  
  public float diagonal() {
    return (float) Math.sqrt((xdim * xdim) + (ydim * ydim) + (zdim * zdim)) ;
  }
  

  /**  Expands this box to include a given radius about the given vector point.
    */
  public void include(Vec3D v, float r) {
    include(v.x, v.y, v.z, r) ;
  }
  

  /**  Expands this box to include a given radius about the given x/y/z point.
    */
  public void include(float xp, float yp, float zp, float r) {
    if (xpos > xp - r) xpos = xp - r ;
    if (ypos > yp - r) ypos = yp - r ;
    if (zpos > zp - r) zpos = zp - r ;
    if (xmax < xp + r) xmax = xp + r ;
    if (ymax < yp + r) ymax = yp + r ;
    if (zmax < zp + r) zmax = zp + r ;
    xdim = xmax - xpos ;
    ydim = ymax - ypos ;
    zdim = zmax - zpos ;
  }
  
  
  /**  Crops this box to fit within the given argument.
    */
  public void cropBy(Box3D box) {
    if (xpos < box.xpos) xpos = box.xpos ;
    if (ypos < box.ypos) ypos = box.ypos ;
    if (zpos < box.zpos) zpos = box.zpos ;
    if (xmax > box.xmax) xmax = box.xmax ;
    if (ymax > box.ymax) ymax = box.ymax ;
    if (zmax > box.zmax) zmax = box.zmax ;
    xdim = xmax - xpos ;
    ydim = ymax - ypos ;
    zdim = zmax - zpos ;
  }
  
  
  public String toString() {
    return
      "( " +
      xpos + " " +
      ypos + " " +
      zpos + " " +
      xmax + " " +
      ymax + " " +
      zmax + " )" ;
  }
}
