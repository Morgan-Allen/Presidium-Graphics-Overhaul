/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.Delivery;
import stratos.game.plans.DeliveryUtils;
import stratos.game.plans.Mining;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



public class ExcavationSite extends Venue implements
  TileConstants
{
  
  
  /**  Constants, fields, constructors and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/artificer/";
  final static CutoutModel SHAFT_MODEL = CutoutModel.fromImage(
    ExcavationSite.class, IMG_DIR+"excavation_site.png", 4, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    ExcavationSite.class, "media/GUI/Buttons/excavation_button.gif"
  );
  
  final static int
    DIG_LIMITS[] = { 8, 12, 15, 16 },
    DIG_FACE_REFRESH = World.STANDARD_DAY_LENGTH / 10,
    SMELTER_REFRESH  = 10;
  
  /*
  final static FacilityProfile PROFILE = new FacilityProfile(
    ExcavationSite.class, Structure.TYPE_VENUE,
    4, 200, 15, -5,
    new TradeType[] {},
    new Background[] { EXCAVATOR },
    Conversion.parse(EcologistStation.class, new Object[][] {
      { MINERALS, TO, METALS    },
      { MINERALS, TO, FUEL_RODS }
    })
  );
  //*/
  
  private static boolean verbose = false;
  
  
  private Tile underFaces[];
  //  TODO:  List open shafts instead
  ///private List <Smelter> smelters = new List <Smelter> ();
  //private List <Tailing> tailings = new List <Tailing> ();
  private Box2D stripArea = new Box2D();
  
  
  
  public ExcavationSite(Base base) {
    super(4, 1, Venue.ENTRANCE_EAST, base);
    structure.setupStats(
      200, 15, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    personnel.setShiftType(SHIFTS_BY_DAY);
    attachModel(SHAFT_MODEL);
  }


  public ExcavationSite(Session s) throws Exception {
    super(s);
    underFaces = (Tile[]) s.loadTargetArray(Tile.class);
    //s.loadObjects(tailings);
    stripArea.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTargetArray(underFaces);
    //s.saveObjects(tailings);
    stripArea.saveTo(s.output());
  }
  
  
  
  /**  Presence in the world and boardability-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    return true;
  }
  
  
  public void exitWorld() {
    super.exitWorld();
    //
    //  TODO:  Close all your shafts?  Eject occupants?
  }
  
  
  public void onDestruction() {
    super.onDestruction();
  }
  
  
  public void onCompletion() {
    super.onCompletion();
  }
  
  
  
  /**  Methods for sorting and returning mine-faces in order of promise.
    */
  public int digLimit() {
    final int level = structure.upgradeLevel(SAFETY_PROTOCOL);
    return DIG_LIMITS[level];
  }
  
  
  
  /**  Economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    ExcavationSite.class, "excavation_upgrades"
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    SAFETY_PROTOCOL = new Upgrade(
      "Safety Protocol",
      "Increases effective dig range while limiting pollution and reducing "+
      "the likelihood of artilect release.",
      100,
      null, 1, null, ALL_UPGRADES
    ),
    
    METAL_ORES_MINING = new Upgrade(
      "Metal Ores Mining",
      "Allows veins of heavy metals to be detected and excavated more "+
      "reliably.",
      150,
      ORES, 2, null, ALL_UPGRADES
    ),
    
    FUEL_CORES_MINING = new Upgrade(
      "Fuel Cores Mining",
      "Allows deposits of radiactive isotopes to be sought out and extracted "+
      "more reliably.",
      200,
      FUEL_RODS, 2, null, ALL_UPGRADES
    ),
    
    EXCAVATOR_STATION = new Upgrade(
      "Excavator Station",
      "Excavators are responsible for seeking out subterranean mineral "+
      "deposits and bringing them to the surface.",
      50,
      Backgrounds.EXCAVATOR, 1, null, ALL_UPGRADES
    ),
    
    ARTIFACT_ASSEMBLY = new Upgrade(
      "Artifact Assembly",
      "Allows fragmentary artifacts to be reconstructed with greater skill "+
      "and confidence.",
      150,
      null, 1, EXCAVATOR_STATION, ALL_UPGRADES
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to bring up an indefinite supply of "+
      "metals and isotopes from the planet's molten core, at the cost of "+
      "heavy pollution.",
      350,
      null, 1, METAL_ORES_MINING, ALL_UPGRADES
    )
 ;
  
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.EXCAVATOR };
  }
  
  
  public TradeType[] services() {
    return new TradeType[] { ORES, ISOTOPES };
  }
  
  
  public int numOpenings(Background v) {
    final int NO = super.numOpenings(v);
    if (v == Backgrounds.EXCAVATOR) return NO + 3;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final boolean report = verbose && I.talkAbout == actor;
    
    if (report) I.say("\nGETTING NEXT EXCAVATION TASK");
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    /*
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 5, world
    );
    //*/
    if (d != null) return d;
    final Choice choice = new Choice(actor);
    
    /*
    for (Smelter s : smelters) {
      choice.add(new OreProcessing(actor, s, s.output));
    }
    //  TODO:  RESTORE AND TEST THIS
    /*
    if (structure.upgradeLevel(ARTIFACT_ASSEMBLY) > 0) {
      choice.add(new OreProcessing(actor, this, ARTIFACTS));
    }
    //*/
    
    final Target face = Mining.nextMineFace(this, underFaces);
    if (report) I.say("  Mine face is: "+face);
    if (face != null) {
      choice.add(new Mining(actor, face, this));
    }
    return choice.weightedPick();
  }
  
  
  public int extractionBonus(TradeType mineral) {
    if (mineral == ORES) {
      return (1 + structure.upgradeLevel(METAL_ORES_MINING)) * 2;
    }
    if (mineral == ISOTOPES) {
      return (1 + structure.upgradeLevel(FUEL_CORES_MINING)) * 2;
    }
    /*
    if (mineral == ARTIFACTS) {
      return (1 + structure.upgradeLevel(ARTIFACT_ASSEMBLY)) * 2;
    }
    //*/
    return -1;
  }
  
  /*
  protected Venue smeltingSite(Service mineral) {
    if (mineral == ARTIFACTS ) return this;
    for (Smelter s : smelters) {
      if (s.output == mineral) return s;
    }
    return null;
  }
  
  
  public Tailing nextTailing() {
    for (Tailing t : tailings) {
      if ((! t.inWorld()) && ! t.canPlace()) {
        tailings.remove(t);
        continue;
      }
      if (t.fillLevel() < 1) return t;
    }
    
    Tailing strip[] = new Tailing[4];
    for (int i = 4; i-- > 0;) strip[i] = new Tailing(base(), strip);
    strip = (Tailing[]) Placement.establishVenueStrip(
      strip, this, false, world
    );
    if (strip == null) return null;
    for (int i = 4; i-- > 0;) tailings.add(strip[i]);
    return nextTailing();
  }
  //*/
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    structure.setAmbienceVal(structure.upgradeLevel(SAFETY_PROTOCOL) - 3);
    
    //  TODO:  Remove later?
    //nextTailing();
    
    /*
    for (Smelter kid : smelters) if (kid.destroyed()) {
      smelters.remove(kid);
    }
    //
    //  TODO:  Come up with limits for each of the smelter types, based on
    //  staff size and underlying/surrounding terrain.
    //final int numDrills = structure.upgradeLevel(MANTLE_DRILLING);
    if (numUpdates % SMELTER_REFRESH == 0) {
      if (smeltingSite(METALS) == null) {
        final Smelter strip = Smelter.siteSmelter(this, METALS);
        if (strip != null) smelters.add(strip);
      }
      if (
        smeltingSite(FUEL_RODS) == null &&
        true //structure.upgradeLevel(FUEL_PROCESSING) > 0
      ) {
        final Smelter strip = Smelter.siteSmelter(this, FUEL_RODS);
        if (strip != null) smelters.add(strip);
      }
    }
    //*/
    
    if (underFaces == null || numUpdates % DIG_FACE_REFRESH == 0) {
      underFaces = Mining.getTilesUnder(this);
    }
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Excavation Site";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "excavation_site");
  }
  
  
  public String helpInfo() {
    return
      "Excavation Sites expedite the extraction of mineral wealth and "+
      "buried artifacts from the terrain surrounding your settlement.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER;
  }
}



