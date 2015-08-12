/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
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
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    ExcavationSite.class, "excavation_site",
    "Excavation Site", UIConstants.TYPE_ENGINEER, ICON,
    "Excavation Sites expedite extraction of minerals and artifacts "+
    "from surrounding terrain.",
    4, 1, Structure.IS_NORMAL, Owner.TIER_FACILITY, 200, 15,
    METALS, FUEL_RODS, POLYMER, EXCAVATOR
  );
  
  final public static Conversion
    LAND_TO_METALS = new Conversion(
      BLUEPRINT, "land_to_metals",
      TO, 1, METALS
    ),
    LAND_TO_ISOTOPES = new Conversion(
      BLUEPRINT, "land_to_isotopes",
      TO, 1, FUEL_RODS
    ),
    FLORA_TO_POLYMER = new Conversion(
      BLUEPRINT, "flora_to_polymer",
      TO, 1, POLYMER
    );
  
  
  private static boolean verbose = false;
  
  private Box2D areaClaimed = new Box2D();
  private SiteDivision division = SiteDivision.NONE;
  private Tile openFaces[] = new Tile[0];
  
  
  public ExcavationSite(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(SHAFT_MODEL);
  }
  
  
  public ExcavationSite(Session s) throws Exception {
    super(s);
    areaClaimed.loadFrom(s.input());
    division  = SiteDivision.loadFrom(s);
    openFaces = (Tile[]) s.loadObjectArray(Tile.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    areaClaimed.saveTo(s.output());
    SiteDivision.saveTo(s, division);
    s.saveObjectArray(openFaces);
  }
  
  
  
  /**  Siting and output-estimation:
    */
  final static Siting SITING = new Siting(BLUEPRINT) {
    
  };
  
  
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    //  TODO:  Unify the methods here with something similar from the Nursery
    //  class, and make that a utility-function for SiteDivision.
    
    if (area == null) {
      final Stage world = position.world;
      areaClaimed.setTo(footprint()).expandBy(EXTRA_CLAIM_RANGE);
      areaClaimed.cropBy(world.area());
    }
    else {
      areaClaimed.setTo(area);
    }
    //
    //  NOTE:  Facing must be set before dig-tiles are settled on, as this
    //  affects row-orientation!
    setFacing(areaClaimed.xdim() > areaClaimed.ydim() ?
      FACE_SOUTH : FACE_EAST
    );
    return true;
  }
  
  
  public Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  public Tile[] reserved() {
    if (! inWorld()) updateDivision();
    return division.reserved;
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    return true;
  }
  
  
  public void doPlacement(boolean intact) {
    if (division == SiteDivision.NONE) updateDivision();
    super.doPlacement(intact);
    for (Tile t : division.reserved) t.setReserves(this, false);
  }
  
  
  public void exitWorld() {
    for (Tile t : division.reserved) t.setReserves(null, false);
    super.exitWorld();
  }
  
  
  
  /**  Utility methods for handling dig-output and tile-assignment:
    */
  private void updateDivision() {
    division = SiteDivision.forArea(this, areaClaimed, facing(), 3, this);
  }
  
  
  public boolean canDig(Tile at) {
    return division.useType(at, areaClaimed) == 1;
  }
  
  
  public boolean canDump(Tile at) {
    return division.useType(at, areaClaimed) == 2;
  }
  
  
  private Item[] estimateDailyOutput() {
    if (openFaces == null) return new Item[0];
    float sumM = 0, sumF = 0, outM, outF;
    
    for (Tile t : openFaces) {
      final Item i = Outcrop.mineralsAt(t);
      if (i == null) continue;
      if (i.type == METALS   ) sumM++;
      if (i.type == FUEL_RODS) sumF++;
    }
    sumM /= openFaces.length;
    sumF /= openFaces.length;
    
    outM = sumM;
    outF = sumF;
    
    float mineMult = Mining.HARVEST_MULT * staff.workforce() / 2f;
    mineMult *= Stage.STANDARD_SHIFT_LENGTH / Mining.DEFAULT_TILE_DIG_TIME;
    outM *= mineMult * extractMultiple(METALS   );
    outF *= mineMult * extractMultiple(FUEL_RODS);
    
    return new Item[] {
      Item.withAmount(METALS   , outM),
      Item.withAmount(FUEL_RODS, outF)
    };
  }
  
  
  public float extractMultiple(Traded mineral) {
    if (mineral == METALS   ) {
      return 1 + (structure.upgradeLevel(METALS_SMELTING   ) / 3f);
    }
    if (mineral == FUEL_RODS) {
      return 1 + (structure.upgradeLevel(FUEL_RODS_SMELTING) / 3f);
    }
    if (mineral == FOSSILS  ) {
      return 1 + (structure.upgradeLevel(SAFETY_PROTOCOL   ) / 3f);
    }
    return 1;
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
      "the chance to recover "+FOSSILS+".",
      100,
      Upgrade.THREE_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    METALS_SMELTING = new Upgrade(
      "Metals Smelting",
      "Allows veins of heavy "+METALS+" to be sought out and processed "+
      "more reliably.",
      150,
      Upgrade.THREE_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, METALS
    ),
    
    FUEL_RODS_SMELTING = new Upgrade(
      "Fuel Rods Smelting",
      "Allows deposits of radioactive "+FUEL_RODS+" to be sought out and "+
      "extracted more reliably.",
      200,
      Upgrade.THREE_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, ANTIMASS
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to sustain an indefinite production of "+
      METALS+" and "+FUEL_RODS+" at the cost of heavy pollution.",
      350,
      Upgrade.THREE_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    );
  
  
  public int numOpenings(Background v) {
    final int NO = super.numOpenings(v);
    if (v == Backgrounds.EXCAVATOR) return NO + 3;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final boolean report =  verbose && I.talkAbout == actor;
    
    if (report) {
      I.say("\nGETTING NEXT EXCAVATION TASK...");
    }
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null) return d;
    
    final Choice choice = new Choice(actor);
    choice.add(Forestry.nextCutting(actor, this));
    
    final Tile face = Mining.nextMineFace(this, openFaces);
    if (report) I.say("  Mine face is: "+face);
    if (face != null) choice.add(new Mining(actor, face, this));
    
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(structure.upgradeLevel(SAFETY_PROTOCOL) - 3);
    
    if (Visit.empty(openFaces) || numUpdates % DIG_FACE_REFRESH == 0) {
      openFaces = Mining.getOpenFaces(this);
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {
    base.transport.updatePerimeter(this, inWorld, division.toPave);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  private String compileOutputReport() {
    final StringBuffer report = new StringBuffer();
    report.append(super.helpInfo());
    
    final Item out[] = estimateDailyOutput();
    for (Item i : out) {
      final String amount = I.shorten(i.amount, 1);
      report.append("\n  Estimated "+i.type+" per day: "+amount);
    }
    return report.toString();
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileOutputReport();
    else return super.helpInfo();
  }
}







