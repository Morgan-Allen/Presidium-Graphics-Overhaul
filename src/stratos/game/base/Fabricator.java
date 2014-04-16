/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.civilian.Commission;
import stratos.game.common.* ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.*;
import stratos.user.* ;
import stratos.util.* ;



public class Fabricator extends Venue implements Economy {

  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Fabricator.class, "media/Buildings/aesthete/fabricator.png", 4, 2
  ) ;
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/fabricator_button.gif", Fabricator.class
  );
  
  
  public Fabricator(Base base) {
    super(4, 2, ENTRANCE_EAST, base) ;
    structure.setupStats(
      125, 2, 200,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Fabricator(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Foundry.class, "fabricator_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    POLYMER_LOOM = new Upgrade(
      "Polymer Loom",
      "Speeds the production of standard plastics and functional clothing.",
      200, PLASTICS, 2, null, ALL_UPGRADES
    ),
    ORGANIC_BONDING = new Upgrade(
      "Organic Bonding",
      "Allows for direct conversion of carbs to plastics, reduces squalor"+
      "and provides a mild bonus to plastics production.",
      250, CARBS, 1, null, ALL_UPGRADES
    ),
    FABRICATOR_STATION = new Upgrade(
      "Fabricator Station",
      "Fabricators are responsible for the bulk production of textiles, "+
      "domestic utensils and other lightweight goods.",
      100, Background.FABRICATOR, 1, POLYMER_LOOM, ALL_UPGRADES
    ),
    CUTTING_FLOOR = new Upgrade(
      "Cutting Floor",
      "Substantially eases the production of all outfit types.",
      150, null, 2, null, ALL_UPGRADES
    ),
    
    //
    //  TODO:  You might move these to the Sculptor Studio instead.
    
    DESIGN_STUDIO = new Upgrade(
      "Design Studio",
      "Facilitates the design and production of custom decor and commissions "+
      "for luxury outfits.",
      300, null, 1, CUTTING_FLOOR, ALL_UPGRADES
    ),
    AESTHETE_STATION = new Upgrade(
      "Aesthete Station",
      "Aesthetes are gifted, but often somewhat tempestuous individuals "+
      "with a flair for visual expression and eye-catching designs, able and "+
      "willing to cater to demanding patrons.",
      150, Background.AESTHETE, 1, DESIGN_STUDIO, ALL_UPGRADES
    )
  ;
  
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    stocks.translateDemands(1, CARBS_TO_PLASTICS, this) ;
    
    final float powerNeed = 2 + (structure.numUpgrades() / 2f) ;
    stocks.bumpItem(POWER, powerNeed / -10) ;
    stocks.forceDemand(POWER, powerNeed, VenueStocks.TIER_CONSUMER) ;
    
    int pollution = 3 - (structure.upgradeBonus(ORGANIC_BONDING) * 2) ;
    structure.setAmbienceVal(0 - pollution) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    final float powerCut = stocks.shortagePenalty(POWER) * 5 ;
    final int
      loomBonus = (5 * structure.upgradeLevel(POLYMER_LOOM)) / 2,
      bondBonus = 1 + structure.upgradeLevel(ORGANIC_BONDING) ;
    
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null) {
      if (o.made().type == FIXTURES) {
        o.checkBonus = (5 * structure.upgradeLevel(DESIGN_STUDIO)) / 2 ;
        if (stocks.amountOf(TROPHIES) > 0) o.checkBonus += bondBonus / 2 ;
      }
      else if (o.made().type == FINERY) {
        o.checkBonus = structure.upgradeLevel(CUTTING_FLOOR) + 2 ;
        o.checkBonus += (1 + structure.upgradeLevel(DESIGN_STUDIO)) / 2 ;
      }
      else {
        o.checkBonus = structure.upgradeLevel(CUTTING_FLOOR) + 2 ;
        o.checkBonus += structure.upgradeLevel(POLYMER_LOOM) ;
      }
      o.checkBonus -= powerCut ;
      choice.add(o) ;
    }
    
    final Manufacture m = stocks.nextManufacture(actor, CARBS_TO_PLASTICS) ;
    if (m != null) {
      m.checkBonus = (loomBonus / 2) + bondBonus ;
      m.checkBonus -= powerCut ;
      choice.add(m) ;
    }
    
    return choice.weightedPick() ;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    Commission.addCommissions(forActor, this, choice) ;
  }
  
  
  public int numOpenings(Background v) {
    int nO = super.numOpenings(v) ;
    if (v == Background.FABRICATOR) return nO + 1 ;
    if (v == Background.AESTHETE  ) return nO + 1 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return new Service[] {
      PLASTICS, FIXTURES, FINERY,
      OVERALLS, CAMOUFLAGE, SEALSUIT
    } ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.FABRICATOR, Background.AESTHETE } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Service[] goodsToShow() {
    return new Service[] { FIXTURES, PLASTICS, CARBS } ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "fabricator");
  }
  
  
  public String fullName() {
    return "Fabricator" ;
  }
  
  
  public String helpInfo() {
    return
      "The Fabricator manufactures plastics, pressfeed, decor and outfits "+
      "for your citizens." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_AESTHETE ;
  }
}




