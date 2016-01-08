/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.verse.Journey;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Backgrounds.*;



/**  Trade ships come to deposit and collect personnel and cargo.
  */
//
//  NOTE:  This class has been prone to bugs where sprite position appears to
//  'jitter' when passing over blocked tiles below, due to the mobile class
//  attempting to 'correct' position after each update.  This class must treat
//  all tiles as passable to compensate.

public class Dropship extends Vehicle implements Owner {
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static String SHIP_NAMES[] = {
    "The Space Marine",
    "The Solar Wind",
    "The Blue Nebula",
    "The Dejah Thoris",
    "The Leia Organa",
    "The Princess Irulan",
    "The Aeon Falcon",
    "The Business End",
    "The Tranquillity",
    "The Arrow of Orion",
    "The Polaris",
    "The Water Bearer",
    "The Bottle of Klein",
    "The Occam Razor",
    "The Event Horizon",
    "The Lacrimosa",
    "The HMS Magellanic",
    "The Daedalus IV",
    "The Firebrat",
    "The Wing and Prayer",
    "The Prima Noctis",
    "The Nova Dodger",
    "The Apollo",
    "The HMS Halcyon",
    "The Zen Perigee"
  };
  
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset
    FREIGHTER_MODEL = MS3DModel.loadFrom(
      FILE_DIR, "dropship.ms3d", Dropship.class,
      XML_FILE, "Dropship"
    );
  
  final public static int
    MAX_CAPACITY   = 100,
    MAX_PASSENGERS = 5,
    MAX_CREW       = 5;
  
  private int nameID = -1;
  
  
  
  public Dropship(Base base) {
    super();
    assignBase(base);
    attachSprite(FREIGHTER_MODEL.makeSprite());
    this.nameID = Rand.index(SHIP_NAMES.length);
  }
  
  
  public Dropship(Session s) throws Exception {
    super(s);
    nameID = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(nameID);
  }
  
  
  public int pathType  () { return Tile.PATH_BLOCKS   ; }
  public int motionType() { return Mobile.MOTION_FLYER; }
  public float height() { return 1.5f; }
  public float radius() { return 1.5f; }
  
  
  
  /**  Economic and behavioural functions-
    */
  public void addTasks(Choice choice, Actor actor, Background b) {
    if (b == Backgrounds.AS_VISITOR) return;
    
    final boolean report = verbose && (
      I.talkAbout == actor || I.talkAbout == this
    );
    if (report) I.say("\nGetting next dropship job for "+actor);
    if (actor.isDoing(Bringing.class, null)) return;
    
    if (flightState() >= STATE_BOARDING) {
      final Smuggling boarding = Smuggling.asBoarding(actor, this);
      if (staff.assignedTo(Bringing.class) == 0) {
        boarding.addMotives(Plan.MOTIVE_EMERGENCY, Plan.PARAMOUNT);
      }
      choice.add(boarding);
      if (report) I.say("  Time to start boarding!");
      return;
    }
    
    if (b != Backgrounds.AS_RESIDENT) {
      final Choice jobs = new Choice(actor);
      jobs.isVerbose = report;
      if (report) I.say("  Getting best delivery...");
      
      final Batch <Venue> depots = BringUtils.nearbyDepots(
        this, world, SERVICE_COMMERCE
      );
      final Traded goods[] = cargo.demanded();
      
      jobs.add(BringUtils.bestBulkDeliveryFrom (
        this, goods, 2, 10, depots, true
      ));
      jobs.add(BringUtils.bestBulkCollectionFor(
        this, goods, 2, 10, depots, true
      ));
      if (jobs.empty()) {
        jobs.add(BringUtils.bestBulkDeliveryFrom (this, goods, 2, 10, 5, true));
        jobs.add(BringUtils.bestBulkCollectionFor(this, goods, 2, 10, 5, true));
      }
      choice.add(jobs.pickMostUrgent());
    }
  }

  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    PilotUtils.performTakeoffCheck(this, Journey.RAID_STAY_DURATION);
  }
  
  
  public float crowdRating(Actor actor, Background background) {
    if (background == Backgrounds.AS_VISITOR) {
      float crowding = 0;
      for (Mobile m : inside()) {
        if (m instanceof Actor) {
          if (((Actor) m).mind.work() == this) continue;
        }
        crowding++;
      }
      crowding /= MAX_PASSENGERS;
      return crowding;
    }
    else if (background == Backgrounds.AS_RESIDENT) {
      if (! staff().isWorker(actor)) return 1;
      return staff().lodgers().size() * 1f / MAX_CREW;
    }
    else return 0;
  }
  
  
  public Background[] careers() {
    return new Background[] { SHIP_CAPTAIN, DECK_HAND };
  }
  
  
  public int numPositions(Background b) {
    if (b == SHIP_CAPTAIN) return 1;
    if (b == DECK_HAND   ) return 2;
    return 0;
  }
  
  
  public Traded[] services() {
    return ALL_MATERIALS;
  }
  
  
  public int owningTier() {
    return TIER_SHIPPING;
  }
  

  public int spaceCapacity() {
    return MAX_CAPACITY;
  }
  
  
  public float priceFor(Traded service, boolean sold) {
    final BaseDemands d = base().demands;
    final float dockMult = Airfield.isGoodDockSite(dropPoint) ?
      1 : BaseDemands.NO_DOCK_MARGIN
    ;
    if (cargo.canDemand(service)) {
      if (sold) return d.importPrice(service) * dockMult;
      else      return d.exportPrice(service) / dockMult;
    }
    return service.defaultPrice() / dockMult;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite();
    final float fadeProgress = fadeWithHeight();
    s.colour = Colour.transparency(fadeProgress);
    
    final float animProgress = Nums.clamp(fadeProgress - 1, 0, 1);
    s.setAnimation("descend", Nums.clamp(animProgress, 0, 1), true);
    super.renderFor(rendering, base);
  }
  
  
  protected float fadeWithHeight() {
    final float height = viewPosition(null).z / INIT_HIGH;
    return (1 - height) * 2;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return;
    float alpha = Nums.clamp(fadeWithHeight(), 0, 1);
    alpha *= (hovered ? 0.5f : 1);
    Selection.renderSimpleCircle(
      this, viewPosition(null), rendering, Colour.transparency(alpha)
    );
  }
  
  
  protected PlaneFX createShadow(Sprite rendered) {
    final PlaneFX shadow = super.createShadow(rendered);
    shadow.colour = Colour.transparency(fadeWithHeight());
    return shadow;
  }
  
  
  public String fullName() {
    if (nameID == -1) return "Dropship";
    return SHIP_NAMES[nameID];
  }
  
  
  public Composite portrait(HUD UI) {
    return null;
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    final SelectionPane pane = super.configSelectPane(panel, UI);
    final Description d = pane.listing();
    
    //  TODO:  List homeworld and time remaining to Liftoff!
    d.append("\n\nGoods sought: ");
    for (Traded t : ALL_MATERIALS) {
      final int
        sought = (int) cargo.consumption(t),
        has    = (int) cargo.amountOf   (t);
      if (sought <= 0) continue;
      d.append("\n  "+t+" ("+has+"/"+sought+")");
    }
    
    d.append("\n\nPort Of Origin: ");
    d.append(base().visits.homeworld());
    
    return pane;
  }
  
  
  public String helpInfo() {
    return
      "Dropships ferry initial colonists and startup supplies to your "+
      "fledgling settlement, courtesy of your homeworld's generosity.";
  }
}





