/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class ExcavationSite extends Venue implements TileConstants {
  
  
  /**  Constants, fields, constructors and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/artificer/";
  final static CutoutModel SHAFT_MODEL = CutoutModel.fromImage(
    ExcavationSite.class, IMG_DIR+"excavation_shaft.gif", 4, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    ExcavationSite.class, "media/GUI/Buttons/excavation_button.gif"
  );
  
  final static int
    DIG_LIMITS[]      = { 8, 12, 15, 16 },
    EXTRA_CLAIM_RANGE = 4,
    DIG_FACE_REFRESH  = Stage.STANDARD_DAY_LENGTH / 10,
    SMELTER_REFRESH   = 10;

  final public static Conversion
    LAND_TO_METALS = new Conversion(
      ExcavationSite.class, "land_to_metals",
      TO, 1, ORES
    ),
    LAND_TO_ISOTOPES = new Conversion(
      ExcavationSite.class, "land_to_isotopes",
      TO, 1, ISOTOPES
    );
  
  final static VenueProfile PROFILE = new VenueProfile(
    ExcavationSite.class, "excavation_site", "Excavation Site",
    4, 1, IS_NORMAL,
    EngineerStation.PROFILE, Owner.TIER_FACILITY,
    LAND_TO_METALS, LAND_TO_ISOTOPES
  );
  
  
  private static boolean verbose = false;
  private Tile corridor[];
  
  
  public ExcavationSite(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      200, 15, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(SHAFT_MODEL);
  }
  
  
  public ExcavationSite(Session s) throws Exception {
    super(s);
    corridor = (Tile[]) s.loadTargetArray(Tile.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTargetArray(corridor);
  }
  
  
  
  /**  Presence in the world and boardability-
    */
  public boolean enterWorldAt(int x, int y, Stage world) {
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
  
  
  public Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(EXTRA_CLAIM_RANGE);
  }
  
  
  
  /**  Economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    SAFETY_PROTOCOL = new Upgrade(
      "Safety Protocol",
      "Increases effective dig range while limiting pollution and reducing "+
      "the likelihood of artilect release.",
      100,
      Upgrade.THREE_LEVELS, null, 1,
      null, ExcavationSite.class, ALL_UPGRADES
    ),
    
    METAL_ORES_MINING = new Upgrade(
      "Metal Ores Mining",
      "Allows veins of heavy metals to be detected and excavated more "+
      "reliably.",
      150,
      Upgrade.THREE_LEVELS, ORES, 2,
      null, ExcavationSite.class, ALL_UPGRADES
    ),
    
    FUEL_CORES_MINING = new Upgrade(
      "Fuel Cores Mining",
      "Allows deposits of radiactive isotopes to be sought out and extracted "+
      "more reliably.",
      200,
      Upgrade.THREE_LEVELS, ANTIMASS, 2,
      null, ExcavationSite.class, ALL_UPGRADES
    ),
    
    EXCAVATOR_STATION = new Upgrade(
      "Excavator Station",
      "Excavators are responsible for seeking out subterranean mineral "+
      "deposits and bringing them to the surface.",
      50,
      Upgrade.THREE_LEVELS, Backgrounds.EXCAVATOR, 1,
      null, ExcavationSite.class, ALL_UPGRADES
    ),
    
    ARTIFACT_ASSEMBLY = new Upgrade(
      "Artifact Assembly",
      "Allows fragmentary artifacts to be reconstructed with greater skill "+
      "and confidence.",
      150,
      Upgrade.THREE_LEVELS, null, 1,
      EXCAVATOR_STATION, ExcavationSite.class, ALL_UPGRADES
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to bring up an indefinite supply of "+
      "metals and isotopes from the planet's molten core, at the cost of "+
      "heavy pollution.",
      350,
      Upgrade.THREE_LEVELS, null, 1,
      METAL_ORES_MINING, ExcavationSite.class, ALL_UPGRADES
    )
 ;
  
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.EXCAVATOR };
  }
  
  
  public Traded[] services() {
    return new Traded[] { ORES, ISOTOPES };
  }
  
  
  public int numOpenings(Background v) {
    final int NO = super.numOpenings(v);
    if (v == Backgrounds.EXCAVATOR) return NO + 3;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final boolean report = verbose && I.talkAbout == actor;
    
    if (report) I.say("\nGETTING NEXT EXCAVATION TASK");
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    
    if (d != null) return d;
    final Choice choice = new Choice(actor);
    
    if (corridor != null) {
      int numTaken = 0;
      for (Tile t : corridor) if (world.terrain().mineralsAt(t) == 0) numTaken++;
      if (report) I.say("  Faces processed: "+numTaken+"/"+corridor.length);
    }
    
    final Tile face = Mining.nextMineFace(this, corridor);
    if (report) I.say("  Mine face is: "+face);
    if (face != null) {
      choice.add(new Mining(actor, face, this));
    }
    return choice.weightedPick();
  }
  
  
  public int extractionBonus(Traded mineral) {
    if (mineral == ORES) {
      return (0 + structure.upgradeLevel(METAL_ORES_MINING)) * 2;
    }
    if (mineral == ISOTOPES) {
      return (0 + structure.upgradeLevel(FUEL_CORES_MINING)) * 2;
    }
    if (mineral == ARTIFACTS) {
      return (0 + structure.upgradeLevel(ARTIFACT_ASSEMBLY)) * 2;
    }
    return -1;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(structure.upgradeLevel(SAFETY_PROTOCOL) - 3);
    
    if (corridor == null || numUpdates % DIG_FACE_REFRESH == 0) {
      corridor = Mining.getTilesUnder(this);
    }
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "excavation_site");
  }
  
  
  public String helpInfo() {
    return
      "Excavation Sites expedite the extraction of mineral wealth and "+
      "buried artifacts from the terrain surrounding your settlement.";
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_ENGINEER;
  }
}



