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
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class CultureLab extends Venue {

  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    CultureLab.class, "media/Buildings/physician/culture_vats.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    CultureLab.class, "media/GUI/Buttons/culture_vats_button.gif"
  );
  
  
  final public static Conversion
    WASTE_TO_CARBS = new Conversion(
      CultureLab.class, "waste_to_carbs",
      TO, 1, CARBS,
      SIMPLE_DC, CHEMISTRY
    ),
    WASTE_TO_REAGENTS = new Conversion(
      CultureLab.class, "carbs_to_reagents",
      TO, 1, CATALYST,
      ROUTINE_DC, PHARMACY, ROUTINE_DC, CHEMISTRY
    ),
    CARBS_TO_SOMA = new Conversion(
      CultureLab.class, "waste_to_soma",
      2, CARBS, TO, 1, SOMA,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ),
    CARBS_TO_PROTEIN = new Conversion(
      CultureLab.class, "carbs_to_protein",
      2, CARBS, TO, 1, PROTEIN,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GENE_CULTURE
    ),
    PROTEIN_TO_REPLICANTS = new Conversion(
      CultureLab.class, "protein_to_replicants",
      5, PROTEIN, TO, 1, REPLICANTS,
      MODERATE_DC, GENE_CULTURE, ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ),
    PROTEIN_TO_SPYCE_T = new Conversion(
      CultureLab.class, "carbs_to_spyce_t",
      20, PROTEIN, 5, CATALYST, TO, 1, SPYCE_T,
      DIFFICULT_DC, PHARMACY, DIFFICULT_DC, CHEMISTRY
    );
  
  final static VenueProfile PROFILE = new VenueProfile(
    CultureLab.class, "culture_lab", "Culture Lab",
    3, 2, IS_NORMAL,
    new VenueProfile[] { EngineerStation.PROFILE, PhysicianStation.PROFILE },
    Owner.TIER_FACILITY,
    WASTE_TO_CARBS, WASTE_TO_REAGENTS,
    CARBS_TO_SOMA, CARBS_TO_PROTEIN,
    PROTEIN_TO_REPLICANTS, PROTEIN_TO_SPYCE_T
  );
  
  
  public CultureLab(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      400, 3, 450,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
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
    YEAST_DISPOSAL = new Upgrade(
      "Yeast Disposal",
      "Employs gene-tailored microbes to recycle waste emissions and produce "+
      "basic carbs.",
      200, Upgrade.THREE_LEVELS, null, 1,
      null, CultureLab.class, ALL_UPGRADES
    ),
    DRUG_SYNTHESIS = new Upgrade(
      "Drug Synthesis",
      "Employs gene-tailored microbes to synthesise complex molecules, "+
      "permitting manufacture of soma and reagents.",
      250, Upgrade.THREE_LEVELS, null, 1,
      null, CultureLab.class, ALL_UPGRADES
    ),
    TISSUE_CULTURE = new Upgrade(
      "Tissue Culture",
      "Allows production of protein for consumption and replicant organs for "+
      "use in medical emergencies.",
      400, Upgrade.THREE_LEVELS, null, 1,
      YEAST_DISPOSAL, CultureLab.class, ALL_UPGRADES
    ),
    SPYCE_CHEMISTRY = new Upgrade(
      "Spyce Chemistry",
      "Allows minute quantities of Tinerazine to be synthesised from simpler "+
      "compounds, based on protein production.",
      500, Upgrade.THREE_LEVELS, null, 1,
      DRUG_SYNTHESIS, CultureLab.class, ALL_UPGRADES
    ),
    
    //  TODO:  Just have a general structure-upgrade here-
    VAT_BREEDER_STATION = new Upgrade(
      "Vat Breeder Station",
      "Vat Breeders supervise the cultivation and harvesting of the chemical "+
      "and biological processes needed to produce pharmaceuticals and tissue "+
      "samples.",
      100, Upgrade.THREE_LEVELS, Backgrounds.VATS_BREEDER, 1,
      null, CultureLab.class, ALL_UPGRADES
    )
  ;
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    stocks.translateDemands(WASTE_TO_CARBS   , 1);
    stocks.translateDemands(CARBS_TO_PROTEIN , 1);
    stocks.translateDemands(CARBS_TO_SOMA    , 1);
    stocks.translateDemands(WASTE_TO_REAGENTS, 1);
    
    float needPower = 5 * (1 + (structure.numUpgrades() / 3f));
    stocks.forceDemand(POWER, needPower, false);
    final int cycleBonus = structure.upgradeLevel(YEAST_DISPOSAL);
    float pollution = 5 - cycleBonus;
    //
    //  TODO:  vary this based on current power and the number of ongoing
    //  cultures...
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    //
    //  Replicants need to be delivered to their Sickbays once ready, and other
    //  basic goods also need to be transported.
    for (Item match : stocks.matches(REPLICANTS)) {
      if (match.amount < 1) continue;
      final Actor a = (Actor) match.refers;
      if (a.aboard() instanceof Venue) {
        final Delivery d = new Delivery(match, this, (Venue) a.aboard());
        choice.add(d.addMotives(Plan.MOTIVE_EMERGENCY, Plan.URGENT));
      }
    }
    choice.add(DeliveryUtils.bestBulkDeliveryFrom(this, services(), 2, 10, 5));
    //
    //  Foodstuffs-
    final Manufacture mS = stocks.nextManufacture(actor, WASTE_TO_CARBS);
    if (mS != null) {
      choice.add(mS.setBonusFrom(this, true, YEAST_DISPOSAL));
    }
    final Manufacture mP = stocks.nextManufacture(actor, CARBS_TO_PROTEIN);
    if (mP != null) {
      choice.add(mP.setBonusFrom(this, true, TISSUE_CULTURE));
    }
    //
    //  Pharmaceuticals-
    final Manufacture mA = stocks.nextManufacture(actor, CARBS_TO_SOMA);
    if (mA != null) {
      choice.add(mA.setBonusFrom(this, false, DRUG_SYNTHESIS));
    }
    final Manufacture mM = stocks.nextManufacture(actor, WASTE_TO_REAGENTS);
    if (mM != null) {
      choice.add(mM.setBonusFrom(this, true, DRUG_SYNTHESIS));
    }
    final Manufacture mN = stocks.nextManufacture(actor, PROTEIN_TO_SPYCE_T);
    if (mN != null) {
      choice.add(mN.setBonusFrom(this, true, SPYCE_CHEMISTRY));
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
    return new Traded[] {
      CARBS, PROTEIN, SOMA, CATALYST, SPYCE_T
    };
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.VATS_BREEDER };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.VATS_BREEDER) {
      return nO + 3 + structure.upgradeLevel(VAT_BREEDER_STATION);
    }
    return 0;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "culture_vats");
  }
  
  
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      "The Culture Lab manufactures soma, basic foodstuffs and even cloned "+
      "tissues for medical purposes.",
      this, CARBS_TO_SOMA, DRUG_SYNTHESIS
    );
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_PHYSICIAN;
  }
}





