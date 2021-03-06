/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.wip;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.craft.Outfits.*;

import stratos.content.civic.Fractal;

import static stratos.game.actors.Backgrounds.*;



public class Fabricator extends Venue {
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Fabricator.class, "fabricator_model",
    "media/Buildings/aesthete/fabricator_new.png", 4, 1
  );
  
  final static ImageAsset ICON = ImageAsset.fromImage(
    Fabricator.class, "fabricator_icon",
    "media/GUI/Buttons/fabricator_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Fabricator.class, "fabricator",
    "Fabricator", Target.TYPE_WIP, ICON,
    "Fabricators manufacture "+DECOR+", "+PRESSFEED+" and finery for "+
    "the upper-crust.",
    4, 1, Structure.IS_NORMAL,
    Owner.TIER_FACILITY, 125,
    2
  );
  
  
  public Fabricator(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public Fabricator(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.SINGLE_LEVEL, null,
      new Object[] { 5, CHEMISTRY, 10, ASSEMBLY, 10, HANDICRAFTS },
      400//,
      //450
    ),
    POLYMER_LOOM = new Upgrade(
      "Polymer Loom",
      "Speeds production of fine garments for the upper classes by 50%, and "+
      "improves ambience.",
      400, Upgrade.TWO_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    DECOR_STUDIO = new Upgrade(
      "Decor Studio",
      "Facilitates production of customised "+DECOR+" for "+Fractal.class+
      " and "+Edifice.class+" construction.",
      500, Upgrade.TWO_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    FILM_BASE = new Upgrade(
      "Film Base",
      "Allows production of raw "+PRESSFEED+" for use in public relations at "+
      "the "+EnforcerBloc.class+".",
      250, Upgrade.TWO_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    FABRICATOR_STATION = new Upgrade(
      "Fabricator Station", "",
      200, Upgrade.THREE_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    );
    //  TODO:  Level 2 Upgrade?
  
  final public static Conversion
    PLASTICS_TO_DECOR = new Conversion(
      BLUEPRINT, "plastics_to_decor",
      2, PLASTICS, TO, 1, DECOR,
      STRENUOUS_DC, HANDICRAFTS, MODERATE_DC, ASSEMBLY,
      DECOR_STUDIO
    ),
    PLASTICS_TO_PRESSFEED = new Conversion(
      BLUEPRINT, "plastics_to_pressfeed",
      1, PLASTICS, TO, 2, PRESSFEED,
      SIMPLE_DC, CHEMISTRY, DIFFICULT_DC, HANDICRAFTS,
      FILM_BASE
    );
  
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final int levelPC = structure.upgradeLevel(POLYMER_LOOM);
    structure.setAmbienceVal((levelPC - 1) * Ambience.MILD_AMBIENCE);
    
    final float powerNeed = 2 + (structure.numOptionalUpgrades() / 2f);
    stocks.forceDemand(POWER, powerNeed, 0);
    stocks.updateStockDemands(1, services(),
      PLASTICS_TO_DECOR,
      PLASTICS_TO_PRESSFEED
    );
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    
    final Choice choice = new Choice(actor);
    
    for (Item ordered : stocks.specialOrders()) {
      final Manufacture mO = new Manufacture(actor, this, ordered);
      choice.add(mO.setBonusFrom(this, true, POLYMER_LOOM));
    }
    if (! choice.empty()) return choice.pickMostUrgent();
    
    final Manufacture mP = stocks.nextManufacture(actor, PLASTICS_TO_PRESSFEED);
    if (mP != null) {
      mP.setBonusFrom(this, false, FILM_BASE);
      choice.add(mP);
    }
    final Manufacture mD = stocks.nextManufacture(actor, PLASTICS_TO_DECOR);
    if (mD != null) {
      mD.setBonusFrom(this, false, DECOR_STUDIO);
      choice.add(mD);
    }
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  public int numPositions(Background v) {
    int nO = super.numPositions(v);
    //if (v == FABRICATOR) return nO + 2;
    return 0;
  }
  
  
  public void addServices(Choice choice, Actor client) {
    final Item gets = GearPurchase.nextGearToPurchase(client, this);
    if (gets != null) {
      final Upgrade limit = POLYMER_LOOM;
      choice.add(GearPurchase.nextCommission(client, this, gets, limit));
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { POLYMER, DECOR, PLASTICS };
  }
}




