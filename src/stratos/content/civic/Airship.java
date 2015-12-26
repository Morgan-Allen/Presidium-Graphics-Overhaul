/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.Background;
import stratos.game.actors.Backgrounds;
import stratos.game.actors.Choice;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//  TODO:  Have this extend Mount and then you can use it for in-world strike/
//         recon missions...

public class Airship extends Vehicle {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset
    AIRSHIP_MODEL = MS3DModel.loadFrom(
      FILE_DIR, "fighter.ms3d", Airship.class,
      XML_FILE, "Fighter"
    );
  
  final static String SHIP_NAMES[] = {
    "Yamato Thunder",
    "Corona 75",
    "Zeno's Javelin"
  };
  
  final public static int
    MAX_CAPACITY   = 20,
    MAX_PASSENGERS = 0,
    MAX_CREW       = 8;
  
  private int nameID = -1;
  
  
  public Airship(Base base) {
    super();
    assignBase(base);
    attachSprite(AIRSHIP_MODEL.makeSprite());
    this.nameID = Rand.index(SHIP_NAMES.length);
  }
  
  
  public Airship(Session s) throws Exception {
    super(s);
    nameID = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(nameID);
  }
  
  
  
  /**  Physical properties
    */
  public int pathType  () { return Tile.PATH_BLOCKS   ; }
  public int motionType() { return Mobile.MOTION_FLYER; }
  public float height() { return 1.5f; }
  public float radius() { return 1.5f; }
  
  
  
  /**  Economic and behavioural routines-
    */
  public Traded[] services() {
    return Economy.NO_GOODS;
  }
  

  public int spaceCapacity() {
    return MAX_CAPACITY;
  }
  

  public void addTasks(Choice choice, Actor actor, Background b) {
    if (b == Backgrounds.AS_RESIDENT || b == Backgrounds.AS_VISITOR) return;
    
    final boolean report = verbose && (
      I.talkAbout == actor || I.talkAbout == this
    );
    if (report) I.say("\nGetting next dropship job for "+actor);
    
    //  TODO:  Takeoff once everyone is KO'd or aboard, and either hiding or
    //  the normal visit duration is up.
    
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  private static boolean verbose = false;
  
  
  public String fullName() {
    return SHIP_NAMES[nameID];
  }
  
  
  public Composite portrait(HUD UI) {
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Airships provide atmospheric transit for covert or high-urgency "+
      "missions and can act as escorts for vulnerable civilian shipping.";
  }
  
}












