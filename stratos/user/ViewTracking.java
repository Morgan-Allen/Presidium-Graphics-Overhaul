/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import org.apache.commons.math3.util.FastMath;
import org.lwjgl.input.Keyboard;

import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;




/**  This class handles camera motion around the world- also allowing for
  *  shifts in field-of-vision when an info-view is displayed.
  */
public class ViewTracking {
  
  
  private static boolean verbose = false;
  final static int
    FULL_LOCK = 1,
    PART_LOCK = 0,
    NO_LOCK   = -1;
  
  
  final BaseUI UI ;
  final Viewport view ;
  
  private Target lockTarget = null ;
  private float lockX = 0, lockY = 0 ;
  
  
  ViewTracking(BaseUI UI, Viewport view) {
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
    //  TODO:  try calculating this internally instead, based on the width of
    //  the current info-panel.
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
  public Vec3D screenPosFor(Target subject) {
    //  NOTE:  I'm doing some unnecesarily-complex-looking proximity-checking
    //  and interpolation here, or you otherwise don't get pixel-stable results.
    
    final Vec3D
      actualPos = view.translateToScreen(lockPosition(subject)),
      idealPos = new Vec3D(
        (UI.xdim() / 2) + lockX,
        (UI.ydim() / 2) + lockY,
        0
      );
    
    if (verbose) I.say("Actual/ideal pos: "+actualPos+"/"+idealPos);
    actualPos.z = 0;
    final float
      minLock = 0.2f * view.screenScale(),  //tile distance on screen.
      maxLock = 5.0f * view.screenScale(),  //tile distance on screen.
      distance = actualPos.distance(idealPos) - minLock;
    idealPos.z = FULL_LOCK;
    actualPos.z = NO_LOCK;
    
    if (distance <= 0) return idealPos;
    if (distance >= maxLock) return actualPos;
    
    final float
      alpha = (float) FastMath.sqrt(distance / maxLock),
      i = 1 - alpha;
    return new Vec3D(
      (actualPos.x * alpha) + (idealPos.x * i),
      (actualPos.y * alpha) + (idealPos.y * i),
      PART_LOCK
    );
  }
  
  
  public boolean fullyLocked(Target subject) {
    return screenPosFor(subject).z == FULL_LOCK;
  }
  
  
  public Vec3D lockPosition(Target subject) {
    return (subject instanceof Element) ?
      ((Element) subject).viewPosition(null) :
      subject.position(null);
  }
  
  
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
      lockPos = lockPosition(this.lockTarget),
      viewPos = new Vec3D().setTo(view.lookedAt),
      targPos = new Vec3D().setTo(lockPos).sub(lockOff),
      displace = targPos.sub(viewPos, new Vec3D()) ;
    
    final float distance = displace.length() ;
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


