/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;

import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;




public class ExcavationSite extends HarvestVenue {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static String IMG_DIR = "media/Buildings/artificer/";
  final static CutoutModel SHAFT_MODEL = CutoutModel.fromImage(
    ExcavationSite.class, IMG_DIR+"excavation_shaft.gif", 4, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    ExcavationSite.class, "media/GUI/Buttons/excavation_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    ExcavationSite.class, "excavation_site",
    "Excavation Site", Target.TYPE_ENGINEER, ICON,
    "Excavation Sites expedite extraction of minerals and artifacts "+
    "from surrounding terrain.",
    4, 1, Structure.IS_NORMAL | Structure.IS_ZONED,
    Owner.TIER_FACILITY, 200, 15,
    METALS, FUEL_RODS, POLYMER, EXCAVATOR
  );
  
  final static int
    MIN_CLAIM_SIZE = BLUEPRINT.size + 0,
    MAX_CLAIM_SIZE = BLUEPRINT.size + 4;
  
  final public static Conversion
    LAND_TO_METALS = new Conversion(
      BLUEPRINT, "land_to_metals",
      15, HARD_LABOUR, 0, CHEMISTRY, TO, 1, METALS
    ),
    LAND_TO_ISOTOPES = new Conversion(
      BLUEPRINT, "land_to_isotopes",
      15, HARD_LABOUR, 0, CHEMISTRY, TO, 1, FUEL_RODS
    ),
    FLORA_TO_POLYMER = new Conversion(
      BLUEPRINT, "flora_to_polymer",
      10, HARD_LABOUR, TO, 1, POLYMER
    );
  
  
  public ExcavationSite(Base base) {
    super(BLUEPRINT, base, MIN_CLAIM_SIZE, MAX_CLAIM_SIZE);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(SHAFT_MODEL);
  }
  
  
  public ExcavationSite(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Siting and output-estimation:
    */
  final static Siting SITING = new Siting(BLUEPRINT) {
    
  };
  
  
  
  /**  Economic functions-
    */
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.TWO_LEVELS, EngineerStation.LEVELS[0],
      new Object[] { 15, ASSEMBLY, 5, CHEMISTRY },
      400, 550
    ),
    SAFETY_PROTOCOL = new Upgrade(
      "Safety Protocol",
      "Increases effective dig range while limiting pollution and improving "+
      "the chance to recover "+FOSSILS+".",
      100,
      Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    METALS_SMELTING = new Upgrade(
      "Metals Smelting",
      "Allows veins of heavy "+METALS+" to be sought out and processed "+
      "more reliably.",
      150,
      Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, METALS
    ),
    
    FUEL_RODS_SMELTING = new Upgrade(
      "Fuel Rods Smelting",
      "Allows deposits of radioactive "+FUEL_RODS+" to be sought out and "+
      "extracted more reliably.",
      200,
      Upgrade.TWO_LEVELS, SAFETY_PROTOCOL, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, ANTIMASS
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to sustain an indefinite production of "+
      METALS+" and "+FUEL_RODS+" at the cost of heavy pollution.",
      350,
      Upgrade.SINGLE_LEVEL, METALS_SMELTING, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    );
  
  
  public int numPositions(Background v) {
    final int level = structure.mainUpgradeLevel();
    if (v == Backgrounds.EXCAVATOR) return 1 + (level * 2);
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5, true
    );
    if (d != null) return d;
    
    final Choice choice = new Choice(actor);
    choice.add(Mining.asMining   (actor, this, METALS   ));
    choice.add(Mining.asStripping(actor, this, METALS   ));
    choice.add(Mining.asMining   (actor, this, FUEL_RODS));
    choice.add(Mining.asStripping(actor, this, FUEL_RODS));
    choice.add(Mining.asDumping  (actor, this));
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(structure.upgradeLevel(SAFETY_PROTOCOL) - 3);
    stocks.updateStockDemands(1, services());
  }
  
  
  protected void checkTendStates() {
    super.checkTendStates();

    final Tile openFaces[] = claimDivision().reserved();
    if (openFaces == null) return;
    float sumM = 0, sumF = 0, outM, outF;
    
    for (Tile t : openFaces) {
      final Item i = Outcrop.mineralsAt(t);
      if (i == null || ! canDig(t)) continue;
      if (i.type == METALS   ) sumM += i.amount;
      if (i.type == FUEL_RODS) sumF += i.amount;
    }
    sumM /= openFaces.length;
    sumF /= openFaces.length;
    
    outM = sumM;
    outF = sumF;
    
    float mineMult = Mining.HARVEST_MULT * staff.workforce();
    mineMult *= Stage.STANDARD_SHIFT_LENGTH / Mining.TILE_DIG_TIME;
    outM *= mineMult * harvestMultiple(null, METALS   );
    outF *= mineMult * harvestMultiple(null, FUEL_RODS);
    
    stocks.setDailyDemand(METALS   , 0, outM);
    stocks.setDailyDemand(FUEL_RODS, 0, outF);
  }
  
  
  
  /**  Utility methods for handling dig-output and tile-assignment:
    */
  protected ClaimDivision updateDivision() {
    final ClaimDivision d = super.updateDivision();
    return d.withUsageMarked(
      false, this,
      ClaimDivision.USE_SECONDARY
    );
  }
  
  
  public boolean canDig(Tile at) {
    return claimDivision().useType(at) == ClaimDivision.USE_NORMAL;
  }
  
  
  public boolean canDump(Tile at) {
    return claimDivision().useType(at) == ClaimDivision.USE_SECONDARY;
  }
  
  
  public float harvestMultiple(Target tended, Object type) {
    final Traded mineral = (Traded) type;
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
  
  
  public boolean needsTending(Tile t) {
    return Outcrop.mineralsAt(t) != null;
  }
  
  
  public float needForTending(ResourceTending tending) {
    final Mining m = (Mining) tending;
    final Traded type = m.oreType();
    if (type == null) return -1;
    final float need = stocks.relativeShortage(type, true);
    
    if (m.type == Mining.TYPE_MINING) {
      if (super.needForTending(tending) <= 0) return 0;
      return (need + super.needForTending(tending)) / 2;
    }
    if (m.type == Mining.TYPE_STRIPPING) {
      return (1 + need) / 2;
    }
    if (m.type == Mining.TYPE_DUMPING) {
      return stocks.amountOf(SLAG) * 2f / Mining.TAILING_LIMIT;
    }
    return 0;
  }
}







