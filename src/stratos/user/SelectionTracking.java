/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.KeyInput;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;




/**  This class handles camera motion around the world- also allowing for
  *  shifts in field-of-vision when an info-view is displayed.
  */
public class SelectionTracking {
  
  
  private static boolean verbose = false;
  final static int
    FULL_LOCK =  1,
    PART_LOCK =  0,
    NO_LOCK   = -1,
    MAX_DRIFT_DISTANCE = 32;
  
  
  final BaseUI UI;
  final Viewport view;
  
  private Target lockTarget = null;
  private float lockX = 0, lockY = 0;
  
  
  SelectionTracking(BaseUI UI, Viewport view) {
    this.UI = UI;
    this.view = view;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveTarget(lockTarget);
    view.lookedAt.saveTo(s.output());
  }
  
  
  void loadState(Session s) throws Exception {
    lockTarget = s.loadTarget();
    view.lookedAt.loadFrom(s.input());
  }
  
  
  
  /**  Causes the camera to follow a given target and centre on the given
    *  screen coordinates.  If these are null, the screen centre is used by
    *  default.
    */
  public void zoomNow(Target target) {
    lockTarget = target;
    view.lookedAt.setTo(target.position(null));
  }
  
  
  public void lockOn(Target target) {
    if (target == null) {
      lockTarget = null;
    }
    else {
      if (
        (target instanceof Element) &&
        ((Element) target).sprite() == null
      ) { lockOn(null); return; }
      lockTarget = target;
    }
  }
  
  
  protected Target lockTarget() {
    return lockTarget;
  }
  
  
  protected void pushCamera(int x, int y) {
    //
    //  First, we calculate a positional offset within the world and without
    //  exiting it's bounds:
    final Stage world = UI.world();
    final Vec3D nextPos = new Vec3D(view.lookedAt);
    nextPos.x = Nums.clamp(nextPos.x + x, 0, world.size - 1);
    nextPos.y = Nums.clamp(nextPos.y + y, 0, world.size - 1);
    //
    //  We can't continue tracking our lock-target once our field-of-view has
    //  changed.  So if the current pane is dedicated to that target, we need
    //  to dismiss it and focus on whatever comes in view instead.
    if (lockTarget != null && lockTarget == paneSelection()) {
      final Tile          under   = world.tileAt(nextPos.x, nextPos.y);
      final SelectionPane pane    = under.configPanel(null, UI);
      final TargetOptions options = under.configInfo (null, UI);
      UI.setInfoPanel  (pane   );
      UI.setOptionsList(options);
    }
    //
    //  Then clean up after.
    UI.selection.pushSelection(null);
    lockTarget = null;
    view.lookedAt.setTo(nextPos);
  }
  
  
  protected Selectable paneSelection() {
    if (! (UI.currentPane() instanceof SelectionPane)) return null;
    return ((SelectionPane) UI.currentPane()).selected;
  }
  
  
  
  /**  Updates general camera behaviour.
    */
  protected void updateTracking() {
    
    if (KeyInput.wasTyped(' ')) {
      final Selectable ruler = UI.played().ruler();
      if (ruler != null) UI.selection.pushSelection(ruler);
    }
    
    lockX = lockY = 0;
    /*
    if (UI.currentPane() instanceof SelectionInfoPane) {
      final SelectionInfoPane pane = (SelectionInfoPane) UI.currentPane();
      final Vec2D
        trackPos = pane.screenTrackPosition(),
        centre   = UI.trueBounds().centre();
      lockX = trackPos.x - centre.x;
      lockY = trackPos.y - centre.y;
    }
    //*/
    
    if (pressed(Keys.UP   ) || pressed(Keys.W)) pushCamera( 1, -1);
    if (pressed(Keys.DOWN ) || pressed(Keys.S)) pushCamera(-1,  1);
    if (pressed(Keys.RIGHT) || pressed(Keys.D)) pushCamera( 1,  1);
    if (pressed(Keys.LEFT ) || pressed(Keys.A)) pushCamera(-1, -1);
    if (lockTarget != null) followLock();
  }
  
  
  private boolean pressed(int code) {
    return KeyInput.isPressed(code);
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
      alpha = Nums.sqrt(distance / maxLock),
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
    Vec3D pos = (subject instanceof Element) ?
      ((Element) subject).viewPosition(null) :
      subject.position(null);
    pos.z += (subject.height() / Nums.ROOT2) - 1;
    return pos;
  }
  
  
  private void followLock() {
    //
    //  Since the camera centre may be offset, we first calculate the
    //  displacement in in-world terms.
    Vec3D origin = new Vec3D(0, 0, 0), lockOff = new Vec3D(lockX, lockY, 0);
    view.translateFromScreen(origin );
    view.translateFromScreen(lockOff);
    lockOff.sub(origin);
    //
    //  Ascertain the difference between the current camera position and the
    //  the target's position.
    final Vec3D
      lockPos  = lockPosition(lockTarget),
      viewPos  = new Vec3D(view.lookedAt),
      targPos  = new Vec3D(lockPos).sub(lockOff),
      displace = targPos.sub(viewPos, new Vec3D());
    final float
      distance = displace.length(),
      drift = ((distance + 2) * 2) / (Rendering.FRAMES_PER_SECOND * distance);
    //
    //  If distance is too large, or drift would cause overshoot, just go
    //  straight to the point.  Otherwise, displace gradually-
    if (distance > MAX_DRIFT_DISTANCE || drift >= 1) viewPos.setTo(targPos);
    else viewPos.add(displace.scale(drift));
    view.lookedAt.setTo(viewPos);
  }
}






