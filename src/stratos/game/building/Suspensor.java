


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.planet.Species;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.user.*;
import stratos.util.*;



//
//  TODO:  Only disappear once inside a venue.  Otherwise, stay where you are,
//  and wait for someone to pick you up.

//
//  TODO:  Have this extend the Vehicle class, and be open-plan?  ...Yeah.
//  That can work.

public class Suspensor extends Mobile {
  
  

  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml" ;
  final static ModelAsset SUSPENSOR_MODEL = MS3DModel.loadFrom(
    FILE_DIR, "Barge.ms3d", Suspensor.class,
    XML_FILE, "Suspensor"
  );
  
  
  final Actor followed ;
  final Behaviour tracked ;
  
  public Actor passenger = null ;
  public Item cargo = null ;
  
  
  
  public Suspensor(Actor followed, Behaviour tracked) {
    super() ;
    this.followed = followed ;
    this.tracked = tracked ;
    attachSprite(SUSPENSOR_MODEL.makeSprite()) ;
  }
  
  
  public Suspensor(Session s) throws Exception {
    super(s) ;
    followed = (Actor) s.loadObject() ;
    tracked = (Behaviour) s.loadObject() ;
    passenger = (Actor) s.loadObject() ;
    cargo = Item.loadFrom(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(followed) ;
    s.saveObject(tracked) ;
    s.saveObject(passenger) ;
    Item.saveTo(s, cargo) ;
  }
  
  
  
  /**  Performing regular updates-
    */
  public void updateAsScheduled(int numUpdates) {
  }
  
  
  public static Actor carrying(Actor other) {
    for (Mobile m : other.aboard().inside()) {
      if (m instanceof Suspensor) {
        final Suspensor s = (Suspensor) m ;
        if (s.passenger == other) return s.followed;
      }
    }
    return null;
  }
  
  
  
  protected void updateAsMobile() {
    //
    //  Firstly, check whether you even need to exist any more-
    super.updateAsMobile() ;
    if (
      (! followed.inWorld()) ||
      ! followed.mind.agenda().includes(tracked)
    ) {
      if (passenger != null) {
        ////I.say("Depositing passenger...") ;
        final Tile o = origin() ;
        passenger.setPosition(o.x, o.y, world) ;
      }
      exitWorld() ;
      return ;
    }
    //
    //  If so, update your position so as to follow behind the actor-
    final Vec3D FP = followed.position(null) ;
    final Vec2D FR = new Vec2D().setFromAngle(followed.rotation()) ;
    final float idealDist = followed.radius() + this.radius() ;
    FR.scale(0 - idealDist) ;
    FP.x += FR.x ;
    FP.y += FR.y ;
    nextPosition.setTo(FP) ;
    nextPosition.z = aboveGroundHeight() ;
    nextRotation = followed.rotation() ;
    //
    //  And if you have a passenger, update their position.
    if (passenger != null) {
      if (followed.indoors()) {
        final Boardable toBoard = followed.aboard() ;
        ///I.say("Transferring "+passenger+" to: "+toBoard) ;
        passenger.goAboard(toBoard, world) ;
        passenger.setHeading(toBoard.position(null), 0, false, world) ;
      }
      else {
        final Vec3D raise = new Vec3D(nextPosition) ;
        raise.z += 0.15f * GameSettings.actorScale ;
        passenger.setHeading(raise, nextRotation, false, world) ;
      }
    }
  }
  
  
  protected float aboveGroundHeight() { return 0.15f ; }
  public float radius() { return 0.0f ; }
  public Base base() { return followed.base() ; }
  
  protected float spriteScale() {
    return super.spriteScale() * GameSettings.actorScale;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (followed.indoors()) return ;
    //if (origin().owner() != null) return ;
    super.renderFor(rendering, base) ;
  }
  
  
  public void describeStatus(Description d) {
    if (passenger != null) {
      d.append("Carrying ") ;
      d.append(passenger) ;
    }
    else if (cargo != null) {
      d.append("Carrying "+cargo) ;
    }
    else d.append("Idling") ;
  }
}








