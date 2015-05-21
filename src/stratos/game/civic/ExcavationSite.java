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
      TO, 1, METALS
    ),
    LAND_TO_ISOTOPES = new Conversion(
      ExcavationSite.class, "land_to_isotopes",
      TO, 1, FUEL_RODS
    );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    ExcavationSite.class, "excavation_site",
    "Excavation Site", UIConstants.TYPE_ENGINEER,
    4, 1, IS_NORMAL,
    EngineerStation.BLUEPRINT, Owner.TIER_FACILITY,
    LAND_TO_METALS, LAND_TO_ISOTOPES
  );
  
  
  private static boolean verbose = false;
  private Tile corridor[];
  
  
  public ExcavationSite(Base base) {
    super(BLUEPRINT, base);
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
      "Increases effective dig range while limiting pollution and improving "+
      "the chance to recover "+CURIO+"s.",
      100,
      Upgrade.THREE_LEVELS, null, 1,
      null, ExcavationSite.class
    ),
    
    METALS_SMELTING = new Upgrade(
      "Metals Smelting",
      "Allows veins of heavy metal ores to be sought out and processed more "+
      "reliably.",
      150,
      Upgrade.THREE_LEVELS, METALS, 2,
      null, ExcavationSite.class
    ),
    
    FUEL_RODS_SMELTING = new Upgrade(
      "Fuel Rods Smelting",
      "Allows deposits of radiactive isotopes to be sought out and extracted "+
      "more reliably.",
      200,
      Upgrade.THREE_LEVELS, ANTIMASS, 2,
      null, ExcavationSite.class
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to sustain an indefinite production of "+
      METALS+" and "+FUEL_RODS+" at the cost of heavy pollution.",
      350,
      Upgrade.THREE_LEVELS, null, 1,
      null, ExcavationSite.class
    )
 ;
  
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.EXCAVATOR };
  }
  
  
  public Traded[] services() {
    return new Traded[] { METALS, FUEL_RODS };
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
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null) return d;
    final Choice choice = new Choice(actor);
    
    if (report && corridor != null) {
      int numTaken = 0; for (Tile t : corridor) {
        if (world.terrain().mineralsAt(t) == 0) numTaken++;
      }
      I.say("  Faces processed: "+numTaken+"/"+corridor.length);
    }
    
    final Tile face = Mining.nextMineFace(this, corridor);
    if (report) I.say("  Mine face is: "+face);
    if (face != null) {
      choice.add(new Mining(actor, face, this));
    }
    return choice.weightedPick();
  }
  
  
  public int extractionBonus(Traded mineral) {
    if (mineral == METALS) {
      return (0 + structure.upgradeLevel(METALS_SMELTING)) * 2;
    }
    if (mineral == FUEL_RODS) {
      return (0 + structure.upgradeLevel(FUEL_RODS_SMELTING)) * 2;
    }
    if (mineral == CURIO) {
      return (0 + structure.upgradeLevel(SAFETY_PROTOCOL)) * 2;
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
}



