

package stratos.graphics.common;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



public class Viewport {
  
  
  /**  Data fields, constructors, etc.
    */
  final public static float
    DEFAULT_SCALE   = 60.0f,
    DEFAULT_ROTATE  = 45,
    DEFAULT_ELEVATE = 25;
  
  final static Quaternion
    isometricRotation,
    isometricInverted;
  
  static {
    final Quaternion
      invert = isometricInverted = new Quaternion(0, 0, 0, 0),
      rotate = isometricRotation = new Quaternion(0, 0, 0, 0),
      onAxis   = new Quaternion();
    
    rotate.set(Vector3.Z, 0);
    onAxis.set(Vector3.X, 0 + Viewport.DEFAULT_ELEVATE);
    rotate.mul(onAxis);
    onAxis.set(Vector3.Y, 45);
    rotate.mul(onAxis);
    
    invert.set(Vector3.Z, 0);
    onAxis.set(Vector3.Y, -45);
    invert.mul(onAxis);
    onAxis.set(Vector3.X, 0 - Viewport.DEFAULT_ELEVATE);
    invert.mul(onAxis);
  }
  
  
  final public OrthographicCamera camera;
  final public Vec3D lookedAt = new Vec3D();
  public float
    rotation  = DEFAULT_ROTATE,
    elevation = DEFAULT_ELEVATE,
    zoomLevel = 1.0f;
  
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
  //
  //  Modifies translation and screen scale for the purpose of fitting within
  //  a UI widget's rendering area-
  public void updateForWidget(
    Box2D area, float maxField,
    float rotDegrees, float elevDegrees
  ) {
    final float
      wide = Gdx.graphics.getWidth (),
      high = Gdx.graphics.getHeight();
    final Vec2D
      midArea = area.centre(),
      midScreen = new Vec2D(wide, high).scale(0.5f);
    
    final float spanX = wide / DEFAULT_SCALE, spanY = high / DEFAULT_SCALE;
    zoomLevel = Nums.min(
      (spanX / maxField) * area.xdim() / wide,
      (spanY / maxField) * area.ydim() / high
    );
    rotation = rotDegrees;
    elevation = elevDegrees;
    
    final Vector3 trans = new Vector3(
      0,
      midArea.y - midScreen.y,
      midArea.x - midScreen.x
    );
    trans.rotate(Vector3.Z, 0 - elevation);
    trans.rotate(Vector3.Y, rotation);
    
    trans.scl(-1f / screenScale());
    GLToWorld(trans, lookedAt);
    update();
  }
  
  
  public void update() {
    final float
      wide = Gdx.graphics.getWidth (),
      high = Gdx.graphics.getHeight();
    final float screenScale = screenScale();
    camera.setToOrtho(false, wide / screenScale, high / screenScale);

    worldToGL(lookedAt, temp);
    camera.position.set(temp);
    
    final float
      ER  = Nums.toRadians(elevation),
      opp = Nums.sin(ER) * 100,
      adj = Nums.cos(ER) * 100;
    camera.position.add(adj, opp, 0);
    camera.lookAt(temp);
    camera.rotateAround(temp, Vector3.Y, 180 + rotation);
    
    camera.near = 0.1f;
    camera.far = 200.1f;
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
  
  
  public Vec3D translateVectorFromScreen(Vec3D v) {
    Vec3D a = new Vec3D(0, 0, 0), b = new Vec3D(v);
    translateFromScreen(a);
    translateFromScreen(b);
    return b.sub(a);
  }
  
  
  public Vec3D screenHorizontal() {
    return translateVectorFromScreen(new Vec3D(1, 0, 0));
  }
  
  
  public Vec3D screenVertical() {
    return translateVectorFromScreen(new Vec3D(0, 1, 0));
  }
  
  
  public static Vector3 worldToGL(Vec3D from, Vector3 to) {
    if (to == null) to = new Vector3();
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
  
  
  public static Vec3D GLToWorld(Vector3 from, Vec3D to) {
    if (to == null) to = new Vec3D();
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
  
  
  public static Vector3 isometricRotation(Vector3 from, Vector3 to) {
    if (to == null) to = new Vector3();
    if (from != to) to.set(from);
    return to.mul(isometricRotation);
  }
  
  
  public static Vector3 isometricInverted(Vector3 from, Vector3 to) {
    if (to == null) to = new Vector3();
    if (from != to) to.set(from);
    return to.mul(isometricInverted);
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

