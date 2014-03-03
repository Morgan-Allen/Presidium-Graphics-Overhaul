

package src.graphics.common;
import src.graphics.widgets.*;
import src.util.*;

import org.apache.commons.math3.util.FastMath;
import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



public class Viewport {
  
  
  final public static float
    DEFAULT_SCALE = 40.0f,
    DEFAULT_ROTATE = (float) Math.toRadians(45),
    DEFAULT_ELEVATE = (float) Math.toRadians(65) ;
  
  
  final OrthographicCamera camera;
  final Vec3D lookedAt = new Vec3D();
  private float
    rotation = DEFAULT_ROTATE,
    elevation = DEFAULT_ELEVATE,
    zoomLevel = 1.0f ;
  
  final private Vector3 temp = new Vector3();
  
  
  public Viewport() {
    camera = new OrthographicCamera();
    update();
  }
  
  
  public void update() {
    final float
      wide = Gdx.graphics.getWidth(),
      high = Gdx.graphics.getHeight();
    final float screenScale = screenScale();
    camera.setToOrtho(false, wide / screenScale, high / screenScale);
    
    final float
      opp = (float) FastMath.sin(elevation) * 100,
      adj = (float) FastMath.cos(elevation) * 100;
    ///I.say("Opp/Adj are: "+opp+"/"+adj);
    camera.position.set(0, adj, opp);
    temp.set(0, 0, 0);
    camera.lookAt(temp);
    camera.rotateAround(temp, Vector3.Y, (float) (rotation * -180 / Math.PI));
    camera.near = 0.1f;
    camera.far = 300f;
    
    worldToGL(lookedAt, temp);
    camera.position.add(temp);
    camera.update();
  }
  
  
  private float screenScale() {
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
    return distance < radius;
  }
  
  
  public Vec3D translateToScreen(Vec3D point) {
    worldToGL(point, temp);
    temp.mul(camera.combined);
    return GLToWorld(temp, point);
  }
  
  
  public Vec3D translateFromScreen(Vec3D point) {
    worldToGL(point, temp);
    temp.mul(camera.invProjectionView);
    return GLToWorld(temp, point);
  }
  
  
  public Vec3D direction() {
    return GLToWorld(camera.direction, new Vec3D());
  }
  
  
  public Vec3D position() {
    return lookedAt;
  }
  
  
  public Vector3 worldToGL(Vec3D from, Vector3 to) {
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
  
  
  public Vec3D GLToWorld(Vector3 from, Vec3D to) {
    to.x = from.x;
    to.y = from.z;
    to.z = from.y;
    return to;
  }
  
  
  public void setPosition(Vec3D v) {
    lookedAt.setTo(v);
  }
}




