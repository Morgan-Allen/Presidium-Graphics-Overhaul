/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



//  Rename to Pharma Lab.  Allow for direct harvests?


public class CultureLab extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    CultureLab.class, "media/Buildings/physician/culture_vats.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    CultureLab.class, "media/GUI/Buttons/culture_vats_button.gif"
  );
  
  
  final static Blueprint BLUEPRINT = new Blueprint(
    CultureLab.class, "culture_lab",
    "Culture Lab", UIConstants.TYPE_PHYSICIAN, ICON,
    "The Culture Lab manufactures "+REAGENTS+", basic foodstuffs "+
    "and even cloned tissues for medical purposes.",
    4, 2, Structure.IS_NORMAL,
    new Blueprint[] { EngineerStation.BLUEPRINT, PhysicianStation.BLUEPRINT },
    Owner.TIER_FACILITY,
    400, 3, 450, Structure.NORMAL_MAX_UPGRADES
  );
  
  final public static Conversion
    WASTE_TO_CARBS = new Conversion(
      BLUEPRINT, "waste_to_carbs",
      TO, 1, CARBS,
      SIMPLE_DC, CHEMISTRY
    ),
    WASTE_TO_REAGENTS = new Conversion(
      BLUEPRINT, "waste_to_reagents",
      TO, 1, REAGENTS,
      ROUTINE_DC, PHARMACY, ROUTINE_DC, CHEMISTRY
    ),
    CARBS_TO_PROTEIN = new Conversion(
      BLUEPRINT, "carbs_to_protein",
      2, CARBS, TO, 1, PROTEIN,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GENE_CULTURE
    ),
    PROTEIN_TO_REPLICANTS = new Conversion(
      BLUEPRINT, "protein_to_replicants",
      5, PROTEIN, TO, 1, REPLICANTS,
      MODERATE_DC, GENE_CULTURE, ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    );
  
  
  public CultureLab(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    this.attachSprite(MODEL.makeSprite());
  }
  
  
  public CultureLab(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Upgrades, economic functions and employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    CARBS_CULTURE = new Upgrade(
      "Carbs Culture",
      "Employs gene-tailored yeast strains to provide "+CARBS+", cycle waste "+
      "products and output "+ATMO+".",
      200, Upgrade.TWO_LEVELS, null, 1,
      null, BLUEPRINT
    ),
    DRUG_SYNTHESIS = new Upgrade(
      "Drug Synthesis",
      "Employs gene-tailored microbes to synthesise complex molecules, "+
      "permitting manufacture of "+REAGENTS+".",
      250, Upgrade.TWO_LEVELS, null, 1,
      null, BLUEPRINT
    ),
    TISSUE_CULTURE = new Upgrade(
      "Tissue Culture",
      "Allows production of "+PROTEIN+" for consumption and "+REPLICANTS+" "+
      "for use in medical emergencies.",
      400, Upgrade.TWO_LEVELS, null, 1,
      CARBS_CULTURE, BLUEPRINT
    ),
    
    CHEMICAL_VATS = null,
    
    CORPSE_CYCLING = null,
    
    //  TODO:  Just have a general structure-upgrade here-
    VATS_BREEDER_POST = new Upgrade(
      "Vats Breeder Post",
      VATS_BREEDER.info,
      100, Upgrade.TWO_LEVELS, VATS_BREEDER, 1,
      null, BLUEPRINT
    )
  ;
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    stocks.translateRawDemands(WASTE_TO_CARBS   , 1);
    stocks.translateRawDemands(CARBS_TO_PROTEIN , 1);
    stocks.translateRawDemands(WASTE_TO_REAGENTS, 1);
    
    final float needPower = 5 * (1 + (structure.numUpgrades() / 3f));
    final int cycleBonus = structure.upgradeLevel(CARBS_CULTURE);
    final float pollution = 5 - cycleBonus;
    stocks.forceDemand(POWER, needPower, false);
    structure.setAmbienceVal(0 - pollution);
    stocks.forceDemand(ATMO, cycleBonus * 2, true);
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    final Choice choice = new Choice(actor);
    final boolean noShift = staff.shiftFor(actor) == OFF_DUTY;
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
    choice.add(BringUtils.bestBulkDeliveryFrom(this, services(), 2, 10, 5));
    if ((! noShift) && ! choice.empty()) return choice.pickMostUrgent();
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
  
  
  public void addServices(Choice choice, Actor forActor) {
    //  TODO:  Add functions here?
    //super.addServices(choice, forActor);
  }


  public Traded[] services() {
    return new Traded[] { CARBS, PROTEIN, SOMA, REAGENTS };
  }
  
  
  public Background[] careers() {
    return new Background[] { VATS_BREEDER };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == VATS_BREEDER) return nO + 2;
    return 0;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      super.helpInfo(), this, WASTE_TO_CARBS, CARBS_CULTURE
    );
  }
}





