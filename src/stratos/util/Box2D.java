/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util ;
import java.io.DataInputStream ;
import java.io.DataOutputStream ;
import com.badlogic.gdx.math.*;



/**  Used to describe a 2-dimensional bounding box with both position and size.
  */
public class Box2D {
  
	
  protected float
    xpos,
    ypos,
    xdim,
    ydim,
    xmax,
    ymax ;
  
  //  Assorted no-brainer accessor/mutator methods for encapsulation's sake.
  final public float xpos() { return xpos ; }
  final public float ypos() { return ypos ; }
  final public float xdim() { return xdim ; }
  final public float ydim() { return ydim ; }
  final public float xmax() { return xmax ; }
  final public float ymax() { return ymax ; }
  //  Slight complications involved.
  final public void xpos(float x) { xmax = (xpos = x) + xdim ; }
  final public void ypos(float y) { ymax = (ypos = y) + ydim ; }
  final public void xdim(float x) { xmax = xpos + (xdim = x) ; }
  final public void ydim(float y) { ymax = ypos + (ydim = y) ; }
  final public void xmax(float x) { xdim = (xmax = x) - xpos ; }
  final public void ymax(float y) { ydim = (ymax = y) - ypos ; }
  
  
  public Box2D loadFrom(DataInputStream in) throws Exception {
    xpos = in.readFloat() ;
    ypos = in.readFloat() ;
    xdim = in.readFloat() ;
    ydim = in.readFloat() ;
    xmax = in.readFloat() ;
    ymax = in.readFloat() ;
    return this ;
  }
  
  public Box2D saveTo(DataOutputStream out) throws Exception {
    out.writeFloat(xpos) ;
    out.writeFloat(ypos) ;
    out.writeFloat(xdim) ;
    out.writeFloat(ydim) ;
    out.writeFloat(xmax) ;
    out.writeFloat(ymax) ;
    return this ;
  }
  
  
  /**  Copies the argument's values naturally.
    */
  public Box2D setTo(Box2D box) {
    xpos = box.xpos ;
    ypos = box.ypos ;
    xdim = box.xdim ;
    ydim = box.ydim ;
    xmax = box.xmax ;
    ymax = box.ymax ;
    return this  ;
  }
  
  
  /**  Default setup method.  The first two arguments specify position, the
    *  last two specif y size, in x and y, respectively.
    */
  public Box2D set(float x, float y, float xs, float ys) {
    xpos = x ;
    ypos = y ;
    xdim = xs ;
    ydim = ys ;
    xmax = xpos + xdim ;
    ymax = ypos + ydim ;
    return this ;
  }
  
  
  /**  Returns whether this box and the argument intersect.
    */
  public boolean intersects(Box2D box) {
    if (xmax < box.xpos) return false ;
    if (ymax < box.ypos) return false ;
    if (xpos > box.xmax) return false ;
    if (ypos > box.ymax) return false ;
    return true ;
  }
  
  
  public Box2D expandBy(int e) {
    return set(xpos - e, ypos - e, xdim + (e * 2), ydim + (e * 2)) ;
  }
  
  
  public float minSide() {
    return (ydim < xdim) ? ydim : xdim ;
  }
  
  
  public float maxSide() {
    return (ydim > xdim) ? ydim : xdim ;
  }
  
  
  public float area() {
    return xdim * ydim ;
  }
  
  
  public void clipToMultiple(int m) {
    int xm = 0, ym = 0 ;
    while ((xm += m) < xdim) ;
    while ((ym += m) < ydim) ;
    xdim(xm) ;
    ydim(ym) ;
  }
  
  
  public Vector2 centre() {
    return new Vector2().set((xpos + xmax) / 2, (ypos + ymax) / 2) ;
  }
  
  
  
  /**  Returns whether this box is contained (bounded) by the argument box.
    */
  public boolean containedBy(Box2D box) {
    return
      (xpos >= box.xpos) &&
      (ypos >= box.ypos) &&
      (xmax <= box.xmax) &&
      (ymax <= box.ymax) ;
  }
  

  /**  Returns whether this box contains the given point (bounds inclusive.)
    */
  public boolean contains(float xp, float yp) {
    return
      (xpos <= xp) &&
      (ypos <= yp) &&
      (xmax >= xp) &&
      (ymax >= yp) ;
  }
  
  
  /**  Returns the euclidean distance of the given point from the boundaries of
    *  this box.
    */
  public float distance(final float xp, final float yp) {
    final float
      xd = (xp < xpos) ? (xpos - xp) : ((xp > xmax) ? (xp - xmax) : 0),
      yd = (yp < ypos) ? (ypos - yp) : ((yp > ymax) ? (yp - ymax) : 0) ;
    return (float) Math.sqrt((xd * xd) + (yd * yd)) ;
  }
  
  
  /**  Expands this box to include the argument box.
    */
  public Box2D include(Box2D box) {
    if (xpos > box.xpos) xpos = box.xpos ;
    if (ypos > box.ypos) ypos = box.ypos ;
    if (xmax < box.xmax) xmax = box.xmax ;
    if (ymax < box.ymax) ymax = box.ymax ;
    xdim = xmax - xpos ;
    ydim = ymax - ypos ;
    return this ;
  }
  

  /**  Expands this box to include a given radius about the given vector point.
    */
  public Box2D include(Vector2 v, float r) {
    return include(v.x, v.y, r) ;
  }
  

  /**  Expands this box to include a given radius about the given x/y point.
    */
  public Box2D include(float xp, float yp, float r) {
    if (xpos > xp - r) xpos = xp - r ;
    if (ypos > yp - r) ypos = yp - r ;
    if (xmax < xp + r) xmax = xp + r ;
    if (ymax < yp + r) ymax = yp + r ;
    xdim = xmax - xpos ;
    ydim = ymax - ypos ;
    return this ;
  }
  
  
  /**  Crops this box to fit within the given argument.
    */
  public Box2D cropBy(Box2D box) {
    if (xpos < box.xpos) xpos = box.xpos ;
    if (ypos < box.ypos) ypos = box.ypos ;
    if (xmax > box.xmax) xmax = box.xmax ;
    if (ymax > box.ymax) ymax = box.ymax ;
    if (xmax < xpos) { xmax = xpos ; xdim = 0 ; }
    else xdim = xmax - xpos ;
    if (ymax < ypos) { ymax = ypos ; ydim = 0 ; }
    else ydim = ymax - ypos ;
    return this ;
  }
  
  
  public String toString() {
    return
      "(Position: "+xpos+"/"+ypos+
      ", Size: "+xdim+"/"+ydim+
      ", Limits: "+xmax+"/"+ymax+")" ;
  }
}




