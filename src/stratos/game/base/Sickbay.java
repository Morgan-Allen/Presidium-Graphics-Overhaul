


package stratos.game.base ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Sickbay extends Venue implements Economy {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Sickbay.class, "media/Buildings/physician/physician_clinic.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/hospice_button.gif", Sickbay.class
  );
  
  private static boolean verbose = false ;
  
  
  final List <Plan> neuralScans = new List <Plan> () ;  //TODO:  Use this?
  final List <Manufacture> cloneOrders = new List <Manufacture> () ;
  
  
  public Sickbay(Base base) {
    super(3, 2, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      200, 2, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Sickbay(Session s) throws Exception {
    super(s) ;
    s.loadObjects(neuralScans) ;
    s.loadObjects(cloneOrders) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(neuralScans) ;
    s.saveObjects(cloneOrders) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Sickbay.class, "sickbay_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    APOTHECARY = new Upgrade(
      "Apothecary",
      "A selection of therapeutic drugs and immune modulators help to curb "+
      "the spread of contagious disease and assist in birth control.",
      250,
      null, 1, null, ALL_UPGRADES
    ),
    EMERGENCY_AID = new Upgrade(
      "Emergency Aid",
      "Surgical tools, anaesthetics and plasma reserves ensure that serious "+
      "(but non-fatal) injuries can be dealt with quickly, and speeds the "+
      "local production of Stim Kits.",
      300,
      null, 1, null, ALL_UPGRADES
    ),
    MINDER_STATION = new Upgrade(
      "Minder Station",
      "Minders are essential to monitoring patients' condition and tending "+
      "to diet and sanitary needs, but are only familiar with more common "+
      "medications and standard emergency protocol.",
      50,
      Background.MEDIC, 1, APOTHECARY, ALL_UPGRADES
    ),
    NEURAL_SCANNING = new Upgrade(
      "Neural Scanning",
      "Permits neural scans and basic psych evaluation, aiding in detection "+
      "of mental disturbance or subversion, and permitting engram backups in "+
      "case of death.  Mandatory for key personnel.",
      350,
      null, 1, EMERGENCY_AID, ALL_UPGRADES
    ),
    INTENSIVE_CARE = new Upgrade(
      "Intensive Care",
      "Intensive care allows a chance for patients on death's door to make a "+
      "gradual comeback, covering everything from life support and tissue "+
      "grafting to cybernetic prosthesis and engram fusion.",
      400,
      null, 1, MINDER_STATION, ALL_UPGRADES
    ),
    PHYSICIAN_STATION = new Upgrade(
      "Physician Station",
      "Physicians undergo extensive education in every aspect of human "+
      "metabolism and anatomy, are adept as surgeons, and can tailor their "+
      "treatments to the idiosyncracies of a given patient.",
      150,
      Background.PHYSICIAN, 1, EMERGENCY_AID, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null ;
    if (numPatients() == 0 && ! personnel.onShift(actor)) return null ;
    final Choice choice = new Choice(actor) ;
    //
    //  Manufacture Stim Kits for later use-
    final Manufacture mS = stocks.nextManufacture(actor, MEDICINE_TO_STIM_KITS) ;
    if (mS != null) {
      mS.checkBonus = ((structure.upgradeLevel(EMERGENCY_AID) - 1) * 5) / 2 ;
      //mS.timeMult = 5 ;
      choice.add(mS) ;
    }
    //
    //  If anyone is waiting for treatment, tend to them- including outside the
    //  building.
    final Batch <Mobile> around = new Batch <Mobile> () ;
    world.presences.sampleFromKey(this, world, 5, around, Mobile.class) ;
    for (Mobile m : this.inside()) around.include(m) ;
    for (Mobile m : around) if (m instanceof Actor) {
      final FirstAid t = new FirstAid(actor, (Actor) m, this);
      t.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      choice.add(t) ;
    }
    final Behaviour picked = (numPatients() > 0) ?
      choice.pickMostUrgent() :
      choice.weightedPick() ;
    if (picked != null) return picked ;
    //
    //  Otherwise, just tend the desk...
    return new Supervision(actor, this) ;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    //  TODO:  RESTORE ONCE TREATMENT IS CLEANED UP
    //choice.add(new SickLeave(forActor, this)) ;
  }
  
  
  private int numPatients() {
    int count = 0 ;
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor actor = (Actor) m ;
      if (! actor.health.conscious()) count++ ;
      else if (actor.health.injuryLevel() > 0) count++ ;
      //else if (actor.isDoing(SickLeave.class, null)) count++ ;
    }
    return count ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    //updateCloneOrders(numUpdates) ;
    
    final int numU = (1 + structure.numUpgrades()) / 2 ;
    int medNeed = 2 + numU, powerNeed = 2 + numU ;
    //
    //  Sickbays consumes medicine and power based on current upgrade level,
    //  and have a mild positive effect on ambience-
    stocks.incDemand(MEDICINE , medNeed, VenueStocks.TIER_CONSUMER, 1, this) ;
    stocks.incDemand(STIM_KITS, medNeed, VenueStocks.TIER_TRADER, 1, this) ;
    stocks.forceDemand(POWER, powerNeed, VenueStocks.TIER_CONSUMER) ;
    structure.setAmbienceVal(numU * 2) ;
  }
  
  
  //  TODO:  Restore this once the Treatment behaviour is cleaned up!
  /*
  private void updateCloneOrders(int numUpdates) {
    if (numUpdates % 10 != 0) return ;
    //
    //  Clean out any orders that have expired.
    for (Manufacture order : cloneOrders) {
      final Actor patient = (Actor) order.made().refers ;
      final boolean done =
          patient.aboard() != this ||
          (! order.venue.structure.intact()) ||
          order.finished() ||
          (! order.venue.stocks.specialOrders().includes(order)) ;
      if (done) {
        if (verbose) I.sayAbout(this, "Removing order: "+order) ;
        cloneOrders.remove(order) ;
      }
    }
    //
    //  Place part-cloning orders for actors in a critical condition-
    //
    //  TODO:  Allow for placement of orders at the Artificer as well?
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor actor = (Actor) m ;
      if ((! actor.health.suspended()) || hasCloneOrder(actor)) continue ;
      final Venue venue = findCloningVenue() ;
      if (venue == null) continue ;
      final Item ordered = Treatment.replicantFor(actor) ;
      if (ordered == null) continue ;
      final Manufacture order = new Manufacture(
        null, venue, PROTEIN_TO_REPLICANTS, Item.withAmount(ordered, 1)
      ) ;
      venue.stocks.addSpecialOrder(order) ;
      cloneOrders.add(order) ;
      if (verbose) I.sayAbout(this, "Placing order: "+order) ;
    }
  }
  
  
  private boolean hasCloneOrder(Actor a) {
    for (Manufacture order : cloneOrders) {
      if (order.made().refers == a) return true ;
    }
    return false ;
  }
  
  
  private Venue findCloningVenue() {
    final Batch <Venue> near = new Batch <Venue> () ;
    world.presences.sampleFromKey(this, world, 5, near, CultureVats.class) ;
    Venue picked = null ;
    float bestRating = 0 ;
    for (Venue v : near) {
      final float rating = rateCloneVenue(v) ;
      if (rating > bestRating) { bestRating = rating ; picked = v ; }
    }
    return picked ;
  }
  
  
  private float rateCloneVenue(Venue v) {
    if (! v.structure.intact()) return -1 ;
    final int UL = v.structure.upgradeLevel(CultureVats.ORGAN_BANKS) ;
    if (UL <= 0) return -1 ;
    final int SS = World.SECTOR_SIZE * 2 ;
    float rating = 10 ;
    rating *= SS / (SS + Spacing.distance(this, v)) ;
    rating *= 1 + UL ;
    rating *= 2 / (2 + v.stocks.specialOrders().size()) ;
    return rating ;
  }
  //*/
  
  
  public Background[] careers() {
    return new Background[] { Background.MEDIC, Background.PHYSICIAN } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.MEDIC) return nO + 1 ;
    if (v == Background.PHYSICIAN) return nO + 1 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return new Service[] { STIM_KITS, SERVICE_TREAT } ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  protected Service[] goodsToShow() {
    return new Service[] { STIM_KITS, MEDICINE, REPLICANTS } ;
  }
  
  
  public String fullName() {
    return "Sickbay" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "sickbay");
  }
  
  
  public String helpInfo() {
    return
      "The Sickbay allows your citizens' injuries, diseases and trauma to be "+
      "treated quickly and effectively." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_PHYSICIAN ;
  }
}






