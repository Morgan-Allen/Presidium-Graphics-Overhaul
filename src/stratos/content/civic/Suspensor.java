/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//  TODO:  I think the constructor here needs to be private, and any references
//  to the suspensor should be made strictly on a static basis- e.g, to avoid
//  duplicates being created or instances being missing.


public class Suspensor extends Mobile implements Mount {
  
  
  private static boolean
    verbose = false;

  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset SUSPENSOR_MODEL = MS3DModel.loadFrom(
    FILE_DIR, "Barge.ms3d", Suspensor.class,
    XML_FILE, "Suspensor"
  );
  
  
  final Actor followed;
  final Plan tracked;
  
  private Actor passenger = null;
  private Item  cargo     = null;
  
  
  
  public Suspensor(Actor followed, Plan tracked) {
    super();
    this.followed = followed;
    this.tracked  = tracked ;
    assignBase(followed.base());
    attachSprite(SUSPENSOR_MODEL.makeSprite());
  }
  
  
  public Suspensor(Session s) throws Exception {
    super(s);
    followed  = (Actor) s.loadObject();
    tracked   = (Plan ) s.loadObject();
    passenger = (Actor) s.loadObject();
    cargo     = Item.loadFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(followed );
    s.saveObject(tracked  );
    s.saveObject(passenger);
    Item.saveTo(s, cargo);
  }
  
  
  
  /**  Satisfying contract as Mount-
    */
  public Actor mounted() {
    return passenger;
  }
  
  
  public boolean setMounted(Actor mounted, boolean is) {
    if (is) {
      if (this.passenger != null) return false;
      else this.passenger = mounted;
    }
    else {
      if (this.passenger != mounted) return false;
      else this.passenger = null;
    }
    return true;
  }
  
  
  public Property mountStoresAt() {
    return null;
  }
  
  
  public boolean allowsActivity(Plan activity) {
    return false;
  }
  
  
  public boolean actorVisible(Actor mounted) {
    return true;
  }
  
  
  public void configureSpriteFrom(
    Actor mounted, Action action, Sprite actorSprite, Rendering r
  ) {
    actorSprite.setAnimation(Action.FALL, 1, false);
    viewPosition(actorSprite.position);
    actorSprite.rotation = rotation();
  }
  
  
  public void describeActor(Actor mounted, Description d) {
    d.append("Being carried by ");
    d.append(followed);
  }
  
  
  
  /**  Performing regular updates-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (verbose) I.say("  ENTERING WORLD: "+this+" "+this.hashCode());
    return super.enterWorldAt(x, y, world, intact);
  }


  public void exitWorld() {
    if (verbose) I.say("  EXITING WORLD: "+this+" "+this.hashCode());
    if (passenger != null) passenger.bindToMount(null);
    super.exitWorld();
  }
  
  
  protected void updateAsMobile() {
    final boolean report = (
      I.talkAbout == passenger || I.talkAbout == followed
    ) && verbose;
    super.updateAsMobile();
    //
    //  Firstly, check whether you even need to exist any more-
    boolean valid = true;
    if (tracked.finished()              ) valid = false;
    if (! followed.inWorld()            ) valid = false;
    if (! followed.mind.hasToDo(tracked)) valid = false;
    if (! valid) {
      if (report) {
        I.say("\nSuspensor exiting world!");
        I.say("  Actor followed:   "+followed);
        I.say("  In world?         "+followed.inWorld());
        I.say("  Activity tracked: "+tracked);
        I.say("  Finished?         "+(tracked.finished()));
        I.say("  On agenda?        "+followed.mind.hasToDo(tracked));
      }
      if (passenger != null) {
        final Vec3D ground = this.position(null);
        ground.z = world.terrain().trueHeight(ground.x, ground.y);
        passenger.setHeading(ground, this.rotation, false, world);
      }
      exitWorld();
      return;
    }
    //
    //  If so, update your position so as to follow behind the actor-
    final Vec3D disp = followed.position(null).sub(this.position);
    final float idealDist = followed.radius() + this.radius();
    disp.normalise().scale(idealDist * -1);
    final float nextRot = new Vec2D(disp).scale(-1).toAngle();
    disp.add(followed.position(null));
    
    nextPosition.setTo(disp);
    nextPosition.z = aboveGroundHeight();
    nextRotation = nextRot;
    //
    //  And if you have a passenger, update their position.
    if (passenger != null) {
      if (followed.indoors()) {
        final Boarding toBoard = followed.aboard();
        goAboard(toBoard, world);
        passenger.goAboard(toBoard, world);
        passenger.setHeading(toBoard.position(null), 0, false, world);
      }
      else {
        final Vec3D raise = new Vec3D(nextPosition);
        raise.z += 0.15f * GameSettings.peopleScale();
        passenger.setHeading(raise, nextRotation, false, world);
      }
    }
  }
  
  
  public boolean isMoving() {
    return followed.isMoving();
  }
  

  protected boolean collides() {
    return false;
  }
  
  
  protected float boardHeight(Boarding aboard) {
    return super.boardHeight(followed.aboard());
  }
  
  
  protected float aboveGroundHeight() {
    return 0.15f;
  }
  
  
  public float radius() { return 0.0f; }
  
  
  public Base base() { return followed.base(); }
  
  
  protected float spriteScale() {
    return super.spriteScale() * GameSettings.peopleScale();
  }
  
  
  /**  Utility methods for external use-
    */
  public static Actor carrying(Actor other) {
    final Mount mount = other.currentMount();
    if (mount instanceof Actor) {
      return (Actor) mount;
    }
    if (mount instanceof Suspensor) {
      return ((Suspensor) mount).followed;
    }
    return null;
  }
  
  
  public static Plan deliveryTask(Actor actor, Actor carried, Target to) {
    if (! (to instanceof Boarding)) return null;
    final Actor carries = carrying(carried);
    if (carries != null && carries != actor) return null;
    else return new BringPerson(actor, carried, (Boarding) to);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (followed.indoors()) return;
    super.renderFor(rendering, base);
  }
  
  
  public Target selectionLocksOn() {
    return null;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    //  TODO:  Revisit this later.
    return;
  }
  
  
  public void describeStatus(Description d, Object client) {
    if (passenger != null) {
      d.append("Carrying ");
      d.append(passenger);
    }
    else if (cargo != null) {
      d.append("Carrying "+cargo);
    }
    else d.append("Idling");
  }
  
  
  public String fullName() {
    return "Suspensor";
  }
  
  
  public Composite portrait(HUD UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Suspensors help to transport goods, tools or captives from place "+
      "to place.";
  }
}






