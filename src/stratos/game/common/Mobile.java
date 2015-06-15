/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.economic.*;
import stratos.game.maps.IntelMap;
import stratos.graphics.common.*;
import stratos.util.*;
import stratos.graphics.sfx.PlaneFX;



public abstract class Mobile extends Element
  implements Schedule.Updates, Accountable
{
  
  private static boolean
    verbose = Pathing.verbose;
  
  final public static PlaneFX.Model SHADOW_MODEL = new PlaneFX.Model(
    "ground_shadow_model", Mobile.class,
    "media/SFX/ground_shadow.png", 1, 0, 0, false, false
  );
  
  final public static int
    MOTION_WALKS = 0,
    MOTION_HOVER = 1,
    MOTION_FLYER = 2,
    MOTION_WATER = 3;
  final static int
    MAX_PATH_SCAN = Stage.ZONE_SIZE;
  
  protected float
    rotation    ,
    nextRotation;
  protected final Vec3D
    position = new Vec3D(),
    nextPosition = new Vec3D();
  
  private ListEntry <Mobile> worldEntry;
  protected Boarding aboard = null;
  final public Pathing pathing = initPathing();
  
  
  /**  Basic constructors and save/load functionality-
    */
  public Mobile() {
  }
  
  
  public Mobile(Session s) throws Exception {
    super(s);
    this.rotation     = s.loadFloat();
    this.nextRotation = s.loadFloat();
    position.    loadFrom(s.input());
    nextPosition.loadFrom(s.input());
    aboard = (Boarding) s.loadTarget();
    if (pathing != null) pathing.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(rotation    );
    s.saveFloat(nextRotation);
    position    .saveTo(s.output());
    nextPosition.saveTo(s.output());
    s.saveTarget(aboard);
    if (pathing != null) pathing.saveState(s);
  }
  
  
  public abstract Base base();
  
  protected Pathing initPathing() { return null; }
  public abstract boolean isMoving();
  
  
  
  /**  Again, more data-definition methods subclasses might well override.
    */
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D();
    return v.setTo(position);
  }
  
  public float rotation() {
    return rotation;
  }
  
  public float radius() { return 0.25f; }
  public int pathType() { return Tile.PATH_CLEAR; }
  public boolean isMobile() { return true; }
  
  
  
  /**  Methods for handling world entry and exits- including any residence
    *  offworld (see the Stage and VerseBase classes.)
    */
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    goAboard(origin(), world);
    world().schedule.scheduleForUpdates(this);
    world().toggleActive(this, true);
    return true;
  }
  
  
  public boolean enterWorldAt(Target t, Stage world) {
    final Vec3D p = t.position(null);
    if (! setPosition(p.x, p.y, world)) return false;
    //
    //  As a convenient shortcut, we allow direct entry at boardable targets
    //  where possible...
    if (t instanceof Boarding) {
      final Boarding b = (Boarding) t;
      if (b.allowsEntry(this)) {
        super.enterWorld();
        goAboard(b, world);
        return true;
      }
    }
    return super.enterWorldAt(t, world);
  }
  

  public void exitToOffworld() {
    exitWorld();
  }
  
  
  public void exitWorld() {
    if (! inWorld()) I.complain("Already exited world: "+this);
    world.toggleActive(this, false);
    world().schedule.unschedule(this);
    //
    //  To be on the safe side, unregister from both current and next tiles-
    final Vec3D nP = nextPosition;
    final Tile nT = world.tileAt(nP.x, nP.y);
    world.presences.togglePresence(this, nT, false);
    if (aboard != null) aboard.setInside(this, false);
    //
    //  And update position, so you don't get odd jitter effects if selected-
    position.setTo(nextPosition);
    rotation = nextRotation;
    super.exitWorld();
  }
  
  
  public void setWorldEntry(ListEntry <Mobile> e) {
    worldEntry = e;
  }
  
  
  public ListEntry <Mobile> worldEntry() {
    return worldEntry;
  }
  
  
  
  /**  Dealing with pathing-
    */
  public Boarding aboard() {
    return aboard;
  }
  
  
  public void goAboard(Boarding toBoard, Stage world) {
    if (toBoard == aboard) return;
    
    final boolean report = verbose && I.talkAbout == this;
    if (report) {
      I.say("\nGoing aboard "+toBoard);
    }
    
    if (aboard != null) aboard.setInside(this, false);
    aboard = toBoard;
    if (aboard != null) aboard.setInside(this, true);
    
    final Vec3D p = this.nextPosition;
    if (! aboard.area(null).contains(p.x, p.y)) {
      final Vec3D pos = toBoard.position(null);
      pos.z += aboveGroundHeight();
      setHeading(pos, nextRotation, true, world);
    }
  }
  

  public boolean setPosition(float xp, float yp, Stage world) {
    nextPosition.set(xp, yp, aboveGroundHeight());
    return setHeading(nextPosition, nextRotation, true, world);
  }
  
  
  public boolean setHeading(
    Vec3D pos, float rotation, boolean instant, Stage world
  ) {
    final Tile
      oldTile = origin(),
      newTile = world.tileAt(pos.x, pos.y);
    if (! super.setPosition(pos.x, pos.y, world)) return false;
    if (pos      != null) nextPosition.setTo(pos);
    if (rotation != -1  ) nextRotation = rotation;
    
    if (aboard == null || ! aboard.area(null).contains(pos.x, pos.y)) {
      goAboard(newTile, world);
    }
    if (instant) {
      this.position.setTo(pos);
      this.rotation = rotation;
      if (inWorld() && oldTile != newTile) {
        onTileChange(oldTile, newTile);
      }
    }
    return true;
  }
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    world.presences.togglePresence(this, oldTile, false);
    world.presences.togglePresence(this, newTile, true );
  }
  
  
  public boolean indoors() {
    return
      aboard != null &&
      aboard.boardableType() != Boarding.BOARDABLE_TILE;
  }
  

  public float scheduledInterval() {
    return 1.0f;
  }
  
  
  protected void updateAsMobile() {
    final boolean report = verbose && I.talkAbout == this;
    //  
    final Boarding step = pathing == null ? null : pathing.nextStep();
    final Tile oldTile = origin();
    final Vec3D p = nextPosition;
    final boolean outOfBounds =
      (! aboard.area(null).contains(p.x, p.y)) ||
      (aboard.destroyed());
    //
    //  We allow mobiles to 'jump' between dissimilar objects, or track the
    //  sudden motions of mobile boardables (i.e, vehicles)-
    if (report) {
      I.say("\nNext step is: "+step);
      I.say("  Currently aboard: "+aboard);
    }
    if (aboard instanceof Mobile && outOfBounds) {
      aboard.position(nextPosition);
    }
    else if (step != null && step.boardableType() != aboard.boardableType()) {
      if (report) I.say("Jumping to: "+step);
      goAboard(step, world);
      step.position(nextPosition);
    }
    //
    //  If you're not in either your current 'aboard' object, or the area
    //  corresponding to the next step in pathing, you need to default to the
    //  nearest clear tile.
    final Tile newTile = world().tileAt(nextPosition.x, nextPosition.y);
    if (oldTile != newTile || outOfBounds) {
      if (oldTile != newTile) onTileChange(oldTile, newTile);
      
      final boolean awry = step != null && Spacing.distance(step, this) > 1;
      if (step != null && step.area(null).contains(p.x, p.y)) {
        goAboard(step, world);
      }
      else if (outOfBounds) {
        if (awry) onMotionBlock(newTile);
        if (report) I.say("Entering tile: "+newTile);
        goAboard(newTile, world);
      }
    }
    
    //  Escape any currently blocked tile-
    if (
      aboard.boardableType() == Boarding.BOARDABLE_TILE &&
      collides() && aboard.pathType() == Tile.PATH_BLOCKS
    ) {
      final Tile free = Spacing.nearestOpenTile(aboard, this);
      if (free == null) I.complain("MOBILE IS TRAPPED! "+this);
      
      if (report) {
        I.say(this+" IS ABOARD: "+aboard);
        I.say("  ESCAPING TO: "+free);
      }
      nextPosition.x = free.x;
      nextPosition.y = free.y;
      return;
    }
    
    //
    //  Either way, update current position-
    position.setTo(nextPosition);
    rotation = nextRotation;
    super.setPosition(position.x, position.y, world);
    nextPosition.z = boardHeight() + aboveGroundHeight();
    
    if (report) {
      I.say("Aboard: "+aboard);
      I.say("Position "+nextPosition);
      I.say("Next step: "+step);
    }
  }

  
  private float boardHeight() {
    if (aboard == origin()) {
      return world.terrain().trueHeight(position.x, position.y);
    }
    else return aboard.position(null).z;
  }
  
  
  protected void onMotionBlock(Tile t) {
    final boolean canRoute = pathing != null && pathing.refreshFullPath();
    if (! canRoute) pathingAbort();
  }
  
  
  //  TODO:  Make this abstract?
  //  TODO:  Outsource these methods to the Pathing class, I'd say.  Make them
  //  configurable.
  protected void pathingAbort() {}
  protected float aboveGroundHeight() { return 0; }
  protected boolean collides() { return true; }
  
  
  public int     motionType()  { return MOTION_WALKS; }
  public boolean motionWalks() { return motionType() == MOTION_WALKS; }
  public boolean motionHover() { return motionType() == MOTION_HOVER; }
  public boolean motionFlyer() { return motionType() == MOTION_FLYER; }
  public boolean motionWater() { return motionType() == MOTION_WATER; }
  
  
  /**  Rendering and interface methods-
    */
  private PlaneFX shadow = null;
  
  
  protected PlaneFX createShadow(Sprite rendered) {
    if (shadow == null) shadow = (PlaneFX) SHADOW_MODEL.makeSprite();
    shadow.scale = radius() * rendered.scale * Nums.ROOT2;
    
    final Vec3D p = shadow.position;
    p.setTo(rendered.position);
    p.z = world.terrain().trueHeight(p.x, p.y);
    
    return shadow;
  }
  
  
  protected float fogFor(Base base) {
    float baseFog = base.intelMap.displayFog(position.x, position.y, this);
    final float offset = IntelMap.FOG_SEEN_MIN + 0.01f;
    baseFog -= offset;
    baseFog *= 1f / (1 - offset);
    return baseFog;
  }
  
  
  public Vec3D viewPosition(Vec3D v) {
    if (v == null) v = new Vec3D();
    final float alpha = Rendering.frameAlpha();
    v.setTo(position).scale(1 - alpha);
    v.add(nextPosition, alpha, v);
    return v;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    if (indoors()) return;
    
    final Sprite s = this.sprite();
    viewPosition(s.position);
    final float alpha = Rendering.frameAlpha();
    final float rotateChange = Vec2D.degreeDif(nextRotation, rotation);
    s.rotation = (rotation + (rotateChange * alpha) + 360) % 360;
    renderAt(s.position, s.rotation, rendering);
  }
  
  
  public void renderAt(
    Vec3D position, float rotation, Rendering rendering
  ) {
    final Sprite s = this.sprite();
    float scale = spriteScale();
    s.scale = scale;
    s.position.setTo(position);
    s.rotation = rotation;
    s.readyFor(rendering);
    //
    //  Render your shadow, either on the ground or on top of occupants-
    final PlaneFX shadow = createShadow(s);
    if (shadow != null) shadow.readyFor(rendering);
  }
  
  
  protected float spriteScale() {
    return 1;
  }
  
  
  public abstract void describeStatus(Description d);
}







