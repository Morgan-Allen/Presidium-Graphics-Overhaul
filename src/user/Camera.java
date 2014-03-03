/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package src.user;
import org.lwjgl.input.Keyboard;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import src.game.common.*;
import src.graphics.common.*;
import src.start.PlayLoop;
import src.util.*;




/**  This class handles camera motion around the world- also allowing for
  *  shifts in field-of-vision when an info-view is displayed.
  */
public class Camera {
  
  
  final BaseUI UI ;
  final Viewport view ;
  
  private Target lockTarget = null ;
  private float lockX = 0, lockY = 0 ;
  
  
  Camera(BaseUI UI, Viewport view) {
    this.UI = UI ;
    this.view = view ;
  }
  
  void saveState(Session s) throws Exception {
    s.saveTarget(lockTarget) ;
    view.lookedAt.saveTo(s.output()) ;
  }
  
  void loadState(Session s) throws Exception {
    lockTarget = s.loadTarget() ;
    view.lookedAt.loadFrom(s.input());
  }
  
  
  
  /**  Causes the camera to follow a given target and centre on the given
    *  screen coordinates.  If these are null, the screen centre is used by
    *  default.
    */
  public void zoomNow(Target target) {
    lockTarget = target ;
    view.lookedAt.setTo(target.position(null)) ;
  }
  
  
  public void lockOn(Target target) {
    if (
      target == null
    ) {
      lockTarget = null ;
      //UI.selection.setSelected(null) ;
      //UI.voidSelection() ;
    }
    else {
      if (
        (target instanceof Element) &&
        ((Element) target).sprite() == null
      ) { lockOn(null) ; return ; }
      lockTarget = target ;
    }
  }
  
  
  protected void setLockOffset(float lX, float lY) {
    lockX = lX ;
    lockY = lY ;
  }
  
  
  Target lockTarget() {
    return lockTarget ;
  }
  
  
  void pushCamera(int x, int y) {
    final Vec3D p = view.lookedAt;
    p.x += x ;
    p.y += y ;
    UI.selection.pushSelection(null, true) ;
    lockTarget = null ;
  }
  
  
  /**  Updates general camera behaviour.
    */
  void updateCamera() {
    if (Gdx.input.isKeyPressed(Keys.UP   )) pushCamera( 1, -1) ;
    if (Gdx.input.isKeyPressed(Keys.DOWN )) pushCamera(-1,  1) ;
    if (Gdx.input.isKeyPressed(Keys.RIGHT)) pushCamera( 1,  1) ;
    if (Gdx.input.isKeyPressed(Keys.LEFT )) pushCamera(-1, -1) ;
    //if (KeyInput.isKeyDown(Keyboard.KEY_SPACE)) zoomHome() ;
    if (lockTarget != null) followLock() ;
  }
  
  
  /**  Zooms the camera onto the current lock-target.
    */
  private void followLock() {
    //
    //  Since the camera centre may be offset, we first calculate the
    //  displacement in in-world terms.
    Vec3D origin = new Vec3D(0, 0, 0), lockOff = new Vec3D(lockX, lockY, 0);
    view.translateFromScreen(origin);
    view.translateFromScreen(lockOff);
    lockOff.sub(origin);
    //
    //  Ascertain the difference between the current camera position and the
    //  the target's position.
    final Vec3D
      lockPos = (lockTarget instanceof Element) ?
        ((Element) lockTarget).viewPosition(null) :
        lockTarget.position(null),
      viewPos = new Vec3D().setTo(view.lookedAt),
      targPos = new Vec3D().setTo(lockPos).add(lockOff),
      displace = targPos.sub(viewPos, new Vec3D()) ;
    final float distance = displace.length() ;
    
    I.say("Lock position is: "+lockPos);
    //
    //  If distance is too large, just go straight to the point-
    if (distance > 32) {
      viewPos.add(displace) ;
    }
    else {
      //
      //  Otherwise, ascertain the rate at which one should 'drift' toward the
      //  target, and displace accordingly-
      final float drift = Math.min(1,
        ((distance + 2) * 2) / (Rendering.FRAMES_PER_SECOND * distance)
      ) ;
      viewPos.add(displace.scale(drift));
    }
    view.lookedAt.setTo(viewPos);
  }
}







//TODO:  Create a separate class for these functions.

/*
if (Gdx.input.isKeyPressed(Keys.W)) {
t1.set(camera.direction).y = 0;
translate(t1.nor().scl(KTF));
}

if (Gdx.input.isKeyPressed(Keys.S)) {
t1.set(camera.direction).y = 0;
translate(t1.nor().scl(-KTF));
}

if (Gdx.input.isKeyPressed(Keys.A)) {
t1.set(camera.direction).y = 0;
float tmp = t1.z;
t1.z = -t1.x;
t1.x = tmp;
translate(t1.nor().scl(KTF));
}

if (Gdx.input.isKeyPressed(Keys.D)) {
t1.set(camera.direction).y = 0;
float tmp = t1.z;
t1.z = -t1.x;
t1.x = tmp;
translate(t1.nor().scl(-KTF));
}
//*/

/*
public boolean touchDown(int screenX, int screenY, int pointer, int button) {
if (this.button < 0) {
startX = screenX;
startY = screenY;
this.button = button;
}
return true;
}

public boolean touchUp(int screenX, int screenY, int pointer, int button) {
if (button == this.button)
this.button = -1;
return true;
}

public boolean touchDragged(int screenX, int screenY, int pointer) {
final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();
startX = screenX;
startY = screenY;
process(deltaX, deltaY);
return true;
}

public boolean scrolled(int amount) {
// camera.translate(t1.set(camera.direction).scl(amount * -1f));
float am = amount > 0 ? 1.1f : 0.9f;
camera.viewportHeight *= am;
camera.viewportWidth *= am;
return true;
}

protected void onScreenResize(int width, int height) {
camera.viewportWidth = 20;
camera.viewportHeight = (float) height / width * 20;
camera.update();
}

private void process(float deltaX, float deltaY) {
if (button == Buttons.RIGHT) {
t1.set(camera.direction).crs(camera.up).y = 0f;
camera.rotateAround(target, Vector3.Y, deltaX * -MRF);
}
if (button == Buttons.LEFT) {
t1.set(camera.direction).y = 0;
t1.nor();
t2.set(t1).crs(Vector3.Y);
translate(t1.scl(deltaY * -MTF).add(t2.scl(deltaX * -MTF)));
}

camera.update();
}

private void translate(Vector3 vec) {
camera.translate(vec.scl(camera.viewportHeight * 0.08f));
target.add(vec);
}
//*/


