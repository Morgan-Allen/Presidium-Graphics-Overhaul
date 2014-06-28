

package stratos.graphics.common;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import org.apache.commons.math3.util.FastMath;



public class Viewport {
  
  
  /**  Data fields, constructors, etc.
    */
  final public static float
    DEFAULT_SCALE   = 60.0f,
    DEFAULT_ROTATE  = 45,
    DEFAULT_ELEVATE = 25 ;
  
  
  final public OrthographicCamera camera;
  final public Vec3D lookedAt = new Vec3D();
  public float
    rotation  = DEFAULT_ROTATE,
    elevation = DEFAULT_ELEVATE,
    zoomLevel = 1.0f ;
  
  final private Vector3 temp = new Vector3();
  final Vec3D
    originWtS = new Vec3D(),
    originStW = new Vec3D();
  
  
  public Viewport() {
    camera = new OrthographicCamera();
    update();
  }
  
  
  
  /**  Matrix updates-
    */
  public void update() {
    final float
      wide = Gdx.graphics.getWidth (),
      high = Gdx.graphics.getHeight();
    final float screenScale = screenScale();
    camera.setToOrtho(false, wide / screenScale, high / screenScale);
    
    final float
      ER  = (float) FastMath.toRadians(elevation),
      opp = (float) FastMath.sin(ER) * 100,
      adj = (float) FastMath.cos(ER) * 100;
    
    camera.position.set(adj, opp, 0);
    temp.set(0, 0, 0);
    camera.lookAt(temp);
    
    camera.rotateAround(temp, Vector3.Y, 180 + rotation);
    camera.near = 0.1f;
    camera.far = 200.1f;
    
    worldToGL(lookedAt, temp);
    camera.position.add(temp);
    camera.update();
    
    translateToScreen  (originWtS.set(0, 0, 0));
    translateFromScreen(originStW.set(0, 0, 0));
  }
  
  
  
  /**  UI utility methods (mouse-intersection, etc.)-
    */
  public float screenScale() {
    return DEFAULT_SCALE * zoomLevel;
  }
  

  public boolean intersects(Vec3D point, float radius) {
    worldToGL(point, temp);
    return camera.frustum.sphereInFrustumWithoutNearFar(temp, radius);
  }
  
  
  public boolean mouseIntersects(Vec3D point, float radius, HUD UI) {
    final Vec3D
      p = new Vec3D().setTo(point),
      m = new Vec3D().set(UI.mouseX(), UI.mouseY(), 0);
    translateToScreen(p).z = 0;
    final float distance = p.distance(m) / screenScale();
    return distance <= radius;
  }
  
  
  
  /**  Coordinate translation methods-
    */
  public Vec3D translateToScreen(Vec3D point) {
    return translateToScreen(point, true);
  }
  
  
  public Vec3D translateGLToScreen(Vec3D point) {
    return translateToScreen(point, false);
  }
  
  
  public Vec3D translateFromScreen(Vec3D point) {
    return translateFromScreen(point, true);
  }
  
  
  public Vec3D translateGLFromScreen(Vec3D point) {
    return translateFromScreen(point, false);
  }
  
  
  private Vec3D translateToScreen(Vec3D point, boolean WTG) {
    if (WTG) worldToGL(point, temp);
    else temp.set(point.x, point.y, point.z);
    camera.project(temp);
    point.x = temp.x;
    point.y = temp.y;
    point.z = temp.z;
    //  I find this more useful than a zero-to-1 range...
    point.z *= (camera.far - camera.near);
    return point;
  }
  
  
  private Vec3D translateFromScreen(Vec3D point, boolean GTW) {
    //  Note:  We have to treat the y values differently from screen
    //  translation, thanks to how LibGDX implements these functions.
    temp.x = point.x;
    temp.y = Gdx.graphics.getHeight() - point.y;
    temp.z = point.z;
    temp.z /= (camera.far - camera.near);
    camera.unproject(temp);
    if (GTW) GLToWorld(temp, point);
    else point.set(temp.x, temp.y, temp.z);
    return point;
  }
  
  
  public float screenDepth(Vec3D worldPoint) {
    worldToGL(worldPoint, temp);
    camera.project(temp);
    temp.z *= (camera.far - camera.near);
    return temp.z;
  }
  
  
  public Vec3D direction() {
    return GLToWorld(camera.direction, new Vec3D());
  }
  
  
  public static Vector3 worldToGL(Vec3D from, Vector3 to) {
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
  
  
  public static Vec3D GLToWorld(Vector3 from, Vec3D to) {
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
}





//  TODO:  DIG INTO THESE INSTEAD.  I need a uniform transform for the z
//  component.
/*
  public void project (Vector3 vec, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
      vec.prj(combined);
      vec.x = viewportWidth * (vec.x + 1) / 2 + viewportX;
      vec.y = viewportHeight * (vec.y + 1) / 2 + viewportY;
      vec.z = (vec.z + 1) / 2;
  }
  
  public void unproject (Vector3 vec, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
      float x = vec.x, y = vec.y;
      x = x - viewportX;
      y = Gdx.graphics.getHeight() - y - 1;
      y = y - viewportY;
      vec.x = (2 * x) / viewportWidth - 1;
      vec.y = (2 * y) / viewportHeight - 1;
      vec.z = 2 * vec.z - 1;
      vec.prj(invProjectionView);
  }
//*/

