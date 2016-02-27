/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.actors.Backgrounds.*;



public class CultureVats extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    CultureVats.class, "culture_vats_model",
    "media/Buildings/physician/culture_vats.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    CultureVats.class, "culture_vats_icon",
    "media/GUI/Buttons/culture_vats_button.gif"
  );
  
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    CultureVats.class, "culture_vats",
    "Culture Vats", Target.TYPE_PHYSICIAN, ICON,
    "The Culture Vats manufacture "+REAGENTS+", basic foodstuffs "+
    "and even cloned tissues for medical purposes.",
    4, 2, Structure.IS_NORMAL, Owner.TIER_FACILITY, 400, 3,
    CARBS, PROTEIN, REAGENTS, VATS_BREEDER
  );
  
  
  public CultureVats(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    this.attachSprite(MODEL.makeSprite());
  }
  
  
  public CultureVats(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Upgrades, economic functions and employee behaviour-
    */
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.SINGLE_LEVEL,
      new Upgrade[] { PhysicianStation.LEVELS[0], EngineerStation.LEVELS[0] },
      new Object[] { 15, CHEMISTRY, 5, BIOLOGY },
      400//, 550
    ),
    CARBS_CULTURE = new Upgrade(
      "Carbs Culture",
      "Employs gene-tailored yeast strains to provide "+CARBS+", cycle waste "+
      "products and output "+ATMO+".",
      200, Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    DRUG_SYNTHESIS = new Upgrade(
      "Drug Synthesis",
      "Employs gene-tailored microbes to synthesise complex molecules, "+
      "permitting manufacture of "+REAGENTS+".",
      250, Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    TISSUE_CULTURE = new Upgrade(
      "Tissue Culture",
      "Allows production of "+PROTEIN+" for consumption and "+REPLICANTS+" "+
      "for use in medical emergencies.",
      400, Upgrade.TWO_LEVELS, CARBS_CULTURE, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    CHEMICAL_VATS = null,
    
    CORPSE_CYCLING = null
    /*
    //  TODO:  Just have a general structure-upgrade here-
    VATS_BREEDER_POST = new Upgrade(
      "Vats Breeder Post",
      VATS_BREEDER.info,
      100, Upgrade.TWO_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, VATS_BREEDER
    )
    //*/
  ;
  
  final public static Conversion
    WASTE_TO_CARBS = new Conversion(
      BLUEPRINT, "waste_to_carbs",
      TO, 1, CARBS,
      SIMPLE_DC, CHEMISTRY,
      CARBS_CULTURE
    ),
    WASTE_TO_REAGENTS = new Conversion(
      BLUEPRINT, "waste_to_reagents",
      TO, 1, REAGENTS,
      ROUTINE_DC, BIOLOGY, ROUTINE_DC, CHEMISTRY,
      DRUG_SYNTHESIS
    ),
    CARBS_TO_PROTEIN = new Conversion(
      BLUEPRINT, "carbs_to_protein",
      2, CARBS, TO, 1, PROTEIN,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, BIOLOGY,
      TISSUE_CULTURE
    ),
    PROTEIN_TO_REPLICANTS = new Conversion(
      BLUEPRINT, "protein_to_replicants",
      5, PROTEIN, TO, 1, REPLICANTS,
      MODERATE_DC, BIOLOGY, ROUTINE_DC, CHEMISTRY, SIMPLE_DC,
      TISSUE_CULTURE
    );
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    stocks.updateStockDemands(1, services(),
      CARBS_TO_PROTEIN,
      WASTE_TO_CARBS,
      WASTE_TO_REAGENTS
    );
    final float needPower = 5 * (1 + (structure.numOptionalUpgrades() / 3f));
    final int cycleBonus = structure.upgradeLevel(CARBS_CULTURE);
    final float pollution = 5 - cycleBonus;
    
    stocks.forceDemand(POWER, needPower, 0);
    stocks.forceDemand(ATMO, 0, cycleBonus * 2);
    Manufacture.updateProductionEstimates(this,
      CARBS_TO_PROTEIN ,
      WASTE_TO_CARBS   ,
      WASTE_TO_REAGENTS
    );
    
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  public Behaviour jobFor(Actor actor) {
    final Choice choice = new Choice(actor);
    final boolean noShift = staff.offDuty(actor);
    //
    //  Replicants need to be delivered to their Sickbays once ready, and other
    //  basic goods also need to be transported.
    for (Item match : stocks.matches(REPLICANTS)) {
      if (match.amount < 1) continue;
      final Actor a = (Actor) match.refers;
      if (a.aboard() instanceof Venue) {
        final Bringing d = new Bringing(match, this, (Venue) a.aboard());
        choice.add(d.addMotives(Plan.MOTIVE_EMERGENCY, Plan.URGENT));
      }
    }
    if ((! noShift) && ! choice.empty()) return choice.pickMostUrgent();
    choice.add(
      BringUtils.bestBulkDeliveryFrom(this, services(), 2, 10, 5, true
    ));
    //
    //  Foodstuffs-
    final Manufacture mS = stocks.nextManufacture(actor, WASTE_TO_CARBS);
    if (mS != null) {
      choice.add(mS.setBonusFrom(this, true, CARBS_CULTURE));
    }
    final Manufacture mP = stocks.nextManufacture(actor, CARBS_TO_PROTEIN);
    if (mP != null) {
      choice.add(mP.setBonusFrom(this, true, TISSUE_CULTURE));
    }
    //
    //  Pharmaceuticals-
    final Manufacture mM = stocks.nextManufacture(actor, WASTE_TO_REAGENTS);
    if (mM != null) {
      choice.add(mM.setBonusFrom(this, true, DRUG_SYNTHESIS));
    }
    //
    //  And custom manufacturing-
    for (Item ordered : stocks.specialOrders()) {
      final Manufacture mO = new Manufacture(actor, this, ordered);
      choice.add(mO.setBonusFrom(this, true, TISSUE_CULTURE));
    }
    return choice.pickMostUrgent();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    choice.add(BringUtils.nextPersonalPurchase(client, this));
  }
  
  
  public int numPositions(Background v) {
    final int level = structure.mainUpgradeLevel();
    if (v == VATS_BREEDER) return level + 1;
    return 0;
  }
  
  
  
  /**  Rendering and interface methods-
    */
}











