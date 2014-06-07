/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class CultureVats extends Venue implements Economy {

  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    CultureVats.class, "media/Buildings/physician/culture_vats.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/culture_vats_button.gif", CultureVats.class
  );
  
  
  public CultureVats(Base base) {
    super(3, 2, ENTRANCE_NORTH, base) ;
    structure.setupStats(
      400, 3, 450,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public CultureVats(Session s) throws Exception {
    super(s) ;
    //stocks.bumpItem(CARBS, 100) ;
    //stocks.bumpItem(PROTEIN, 100) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    CultureVats.class, "culture_vats_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    WASTE_DISPOSAL = new Upgrade(
      "Waste Disposal",
      "Reduces pollution, increases marginal efficiency, and permits some "+
      "degree of life support.",
      200, null, 1, null, ALL_UPGRADES
    ),
    PROTEIN_ASSEMBLY = new Upgrade(
      "Protein Assembly",
      "Permits direct manufacture of Protein, a basic foodstuff needed to "+
      "keep your population healthy.",
      150, null, 1, null, ALL_UPGRADES
    ),
    DRUG_SYNTHESIS = new Upgrade(
      "Drug Synthesis",
      "Employs gene-tailored microbes to synthesise complex molecules, "+
      "permitting manufacture of medicines and stimulants.",
      200, null, 1, null, ALL_UPGRADES
    ),
    SOMA_CULTURE = new Upgrade(
      "Soma Culture",
      "Allows mass production of Soma, a cheap recreational narcotic with "+
      "minimal side effects.",
      250, null, 1, DRUG_SYNTHESIS, ALL_UPGRADES
    ),
    ORGAN_BANKS = new Upgrade(
      "Organ Banks",
      "Allows production of spare organs for use in medical emergencies, up "+
      "to and including full-body cloning.",
      400, null, 1, PROTEIN_ASSEMBLY, ALL_UPGRADES
    ),
    VAT_BREEDER_STATION = new Upgrade(
      "Vat Breeder Station",
      "Vat Breeders supervise the cultivation and harvesting of the chemical "+
      "and biological processes needed to produce pharmaceuticals and tissue "+
      "samples.",
      100, Backgrounds.VATS_BREEDER, 1, null, ALL_UPGRADES
    )
  ;
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    stocks.translateDemands(1, WASTE_TO_CARBS   , this) ;
    stocks.translateDemands(1, CARBS_TO_PROTEIN , this) ;
    stocks.translateDemands(1, CARBS_TO_SOMA   , this) ;
    stocks.translateDemands(1, SPICE_TO_MEDICINE, this) ;
    
    float needPower = 5 ;
    if (! isManned()) needPower /= 2 ;
    stocks.incDemand(POWER, needPower, Stocks.TIER_CONSUMER, 1, this) ;
    stocks.bumpItem(POWER, needPower * -0.1f) ;
    
    final int cycleBonus = bonusFor(WASTE_DISPOSAL, 1) ;
    float pollution = 5 - cycleBonus ;
    //
    //  TODO:  vary this based on current power and the number of ongoing
    //  cultures instead.
    if (! isManned()) pollution /= 2 ;
    structure.setAmbienceVal(0 - pollution) ;
  }
  
  
  private int bonusFor(Upgrade u, float mult) {
    return (int) (mult * structure.upgradeLevel(u)) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    //
    //  Replicants need to be delivered to their Sickbays once ready.
    for (Item match : stocks.matches(REPLICANTS)) {
      if (match.amount < 1) continue ;
      final Actor a = (Actor) match.refers ;
      if (a.aboard() instanceof Venue) {
        final Delivery d = new Delivery(match, this, (Venue) a.aboard()) ;
        d.setMotive(Plan.MOTIVE_EMERGENCY, Plan.URGENT);
        choice.add(d) ;
      }
    }
    //
    //  Otherwise, see to custom manufacturing-
    final float powerCut = stocks.shortagePenalty(POWER) * 10 ;
    final int cycleBonus = bonusFor(WASTE_DISPOSAL, 1) ;
    
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null) {
      o.checkBonus = cycleBonus + bonusFor(ORGAN_BANKS, 2) ;
      choice.add(o) ;
    }
    //
    //  Foodstuffs-
    final Manufacture
      mS = stocks.nextManufacture(actor, WASTE_TO_CARBS),
      mP = stocks.nextManufacture(actor, CARBS_TO_PROTEIN) ;
    if (mS != null) {
      mS.checkBonus = cycleBonus * 2 ;
      mS.checkBonus -= powerCut ;
      choice.add(mS) ;
    }
    if (mP != null) {
      mP.checkBonus = cycleBonus + bonusFor(PROTEIN_ASSEMBLY, 1.5f) ;
      mP.checkBonus -= powerCut ;
      choice.add(mP) ;
    }
    //
    //  And pharmaceuticals-
    final Manufacture
      mA = stocks.nextManufacture(actor, CARBS_TO_SOMA),
      mM = stocks.nextManufacture(actor, SPICE_TO_MEDICINE) ;
    if (mA != null) {
      mA.checkBonus = cycleBonus + bonusFor(SOMA_CULTURE, 1.5f) ;
      mA.checkBonus -= powerCut ;
      choice.add(mA) ;
    }
    if (mM != null) {
      mM.checkBonus = cycleBonus + bonusFor(DRUG_SYNTHESIS, 2) ;
      mM.checkBonus -= powerCut ;
      choice.add(mM) ;
    }
    
    return choice.weightedPick() ;
  }
  
  
  public Service[] services() {
    return new Service[] {
      CARBS, PROTEIN, SOMA, MEDICINE, LIFE_SUPPORT, REPLICANTS
    } ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.VATS_BREEDER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Backgrounds.VATS_BREEDER) {
      return nO + 1 ;//+ structure.upgradeLevel(VATS_BREEDER_STATION) ;
    }
    return 0 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "culture_vats");
  }
  
  
  public String fullName() {
    return "Culture Vats" ;
  }
  
  public String helpInfo() {
    return
      "The Culture Vats manufacture soma, medicines, tissue cultures and "+
      "basic foodstuffs." ;
  }
  
  public String buildCategory() {
    return InstallTab.TYPE_PHYSICIAN ;
  }
}





