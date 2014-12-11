/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
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
  
  /*
  final static FacilityProfile PROFILE = new FacilityProfile(
    CultureLab.class, Structure.TYPE_VENUE,
    2, 400, 3, -3,
    new TradeType[] {},
    new Background[] { VATS_BREEDER },
    WASTE_TO_CARBS,
    CARBS_TO_SOMA,
    CARBS_TO_PROTEIN,
    PROTEIN_TO_REPLICANTS
  );
  //*/
  
  
  public CultureLab(Base base) {
    super(3, 2, ENTRANCE_NORTH, base);
    structure.setupStats(
      400, 3, 450,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    personnel.setShiftType(SHIFTS_BY_DAY);
    this.attachSprite(MODEL.makeSprite());
  }
  
  
  public CultureLab(Session s) throws Exception {
    super(s);
    //stocks.bumpItem(CARBS, 100);
    //stocks.bumpItem(PROTEIN, 100);
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
      200, null, 1, null,
      CultureLab.class, ALL_UPGRADES
    ),
    DRUG_SYNTHESIS = new Upgrade(
      "Drug Synthesis",
      "Employs gene-tailored microbes to synthesise complex molecules, "+
      "permitting manufacture of soma and reagents.",
      250, null, 1, null,
      CultureLab.class, ALL_UPGRADES
    ),
    TISSUE_CULTURE = new Upgrade(
      "Tissue Culture",
      "Allows production of protein for consumption and replicant organs for "+
      "use in medical emergencies.",
      400, null, 1, YEAST_DISPOSAL,
      CultureLab.class, ALL_UPGRADES
    ),
    SPYCE_CHEMISTRY = new Upgrade(
      "Spyce Chemistry",
      "Allows minute quantities of spyce to be synthesised from simpler "+
      "compounds, based on carb or protein production.",
      500, null, 1, DRUG_SYNTHESIS,
      CultureLab.class, ALL_UPGRADES
    ),
    
    //  TODO:  Just have a general structure-upgrade here-
    VAT_BREEDER_STATION = new Upgrade(
      "Vat Breeder Station",
      "Vat Breeders supervise the cultivation and harvesting of the chemical "+
      "and biological processes needed to produce pharmaceuticals and tissue "+
      "samples.",
      100, Backgrounds.VATS_BREEDER, 1, DRUG_SYNTHESIS,
      CultureLab.class, ALL_UPGRADES
    )
 ;
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    for (Traded t : this.services()) {
      stocks.incDemand(t, 0, TIER_PRODUCER, 1, this);
    }
    stocks.translateDemands(1, WASTE_TO_CARBS   , this);
    stocks.translateDemands(1, CARBS_TO_PROTEIN , this);
    stocks.translateDemands(1, WASTE_TO_SOMA    , this);
    stocks.translateDemands(1, WASTE_TO_REAGENTS, this);
    
    float needPower = 5;
    if (! isManned()) needPower /= 2;
    stocks.incDemand(POWER, needPower, TIER_CONSUMER, 1, this);
    stocks.bumpItem(POWER, needPower * -0.1f);
    
    final int cycleBonus = structure.upgradeLevel(YEAST_DISPOSAL);
    float pollution = 5 - cycleBonus;
    //
    //  TODO:  vary this based on current power and the number of ongoing
    //  cultures instead.
    if (! isManned()) pollution /= 2;
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
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
    //  And pharmaceuticals-
    final Manufacture mA = stocks.nextManufacture(actor, WASTE_TO_SOMA);
    if (mA != null) {
      choice.add(mA.setBonusFrom(this, false, DRUG_SYNTHESIS));
    }
    final Manufacture mM = stocks.nextManufacture(actor, WASTE_TO_REAGENTS);
    if (mM != null) {
      choice.add(mM.setBonusFrom(this, true, DRUG_SYNTHESIS));
    }
    //
    //  And spyce production-
    final Manufacture mT = stocks.nextManufacture(actor, CARBS_TO_NATRI_SPYCE);
    if (mT != null) {
      choice.add(mT.setBonusFrom(this, true, SPYCE_CHEMISTRY));
    }
    final Manufacture mN = stocks.nextManufacture(actor, PROTEIN_TO_TINER_SPYCE);
    if (mN != null) {
      choice.add(mN.setBonusFrom(this, true, SPYCE_CHEMISTRY));
    }
    //
    //  Replicants need to be delivered to their Sickbays once ready.
    for (Item match : stocks.matches(REPLICANTS)) {
      if (match.amount < 1) continue;
      final Actor a = (Actor) match.refers;
      if (a.aboard() instanceof Venue) {
        final Delivery d = new Delivery(match, this, (Venue) a.aboard());
        d.setMotive(Plan.MOTIVE_EMERGENCY, Plan.URGENT);
        choice.add(d);
      }
    }
    //
    //  Otherwise, see to custom manufacturing of said replicants-
    for (Manufacture o : stocks.specialOrders()) {
      choice.add(o.setBonusFrom(this, true, TISSUE_CULTURE));
    }
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    //  TODO:  Add functions here?
    //super.addServices(choice, forActor);
  }


  public Traded[] services() {
    return new Traded[] {
      CARBS, PROTEIN, SOMA, REAGENTS, HALEB_SPYCE, NATRI_SPYCE
    };
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.VATS_BREEDER };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.VATS_BREEDER) {
      return nO + 1;//+ structure.upgradeLevel(VATS_BREEDER_STATION);
    }
    return 0;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "culture_vats");
  }
  
  
  public String fullName() {
    return "Culture Lab";
  }
  
  public String helpInfo() {
    return
      "The Culture Labs manufacture soma, basic foodstuffs and even cloned "+
      "tissues for medical purposes.";
  }
  
  public String objectCategory() {
    return InstallTab.TYPE_PHYSICIAN;
  }
}





