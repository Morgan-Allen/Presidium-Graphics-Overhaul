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



public class PhysicianStation extends Venue {
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    PhysicianStation.class,
    "media/Buildings/physician/physician_clinic.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    PhysicianStation.class, "media/GUI/Buttons/hospice_button.gif"
  );
  
  final static float
    VISIT_COST = Backgrounds.MIN_DAILY_EXPENSE / 2;
  
  final public static Conversion
    REAGENTS_TO_MEDICINE = new Conversion(
      PhysicianStation.class, "reagents_to_medicine",
      1, CATALYST, 1, GREENS, TO, 2, MEDICINE,
      MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    )
  ;
  
  final static Blueprint BLUEPRINT = new Blueprint(
    PhysicianStation.class, "physician_station",
    "Physician Station", UIConstants.TYPE_PHYSICIAN,
    3, 2, IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY, REAGENTS_TO_MEDICINE
  );
  
  
  final List <Plan> neuralScans = new List <Plan> ();  //TODO:  Use this?
  final List <Manufacture> cloneOrders = new List <Manufacture> ();
  
  
  public PhysicianStation(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      200, 2, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public PhysicianStation(Session s) throws Exception {
    super(s);
    s.loadObjects(neuralScans);
    s.loadObjects(cloneOrders);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(neuralScans);
    s.saveObjects(cloneOrders);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  
  final public static Upgrade
    MEDICAL_LAB = new Upgrade(
      "Medical Lab",
      "Speeds the production of medicines and benefits the treatment and "+
      "diagnosis of most disease.",
      250,
      Upgrade.THREE_LEVELS, null, 1,
      null, PhysicianStation.class
    ),
    INTENSIVE_CARE = new Upgrade(
      "Intensive Care",
      "Surgical tools, anaesthetics and plasma reserves ensure that serious "+
      "injuries can be dealt with quickly.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      null, PhysicianStation.class
    ),
    GENE_THERAPIES = new Upgrade(
      "Gene Therapies",
      "Allows for screening of genetic illness and birth defects, helping to "+
      "ensure a vigorous next generation.",
      350,
      Upgrade.THREE_LEVELS, null, 1,
      MEDICAL_LAB, PhysicianStation.class
    ),
    CRYONICS_WARD = new Upgrade(
      "Cryonics Ward",
      "Allows a chance for the critically injured or clinically dead to make "+
      "an eventual comeback.",
      400,
      Upgrade.THREE_LEVELS, null, 1,
      INTENSIVE_CARE, PhysicianStation.class
    ),
    
    //  TODO:  Add Combat Stims and Truth Serum.
    
    MINDER_STATION = new Upgrade(
      "Minder Ward",
      Backgrounds.MINDER.info,
      50,
      Upgrade.THREE_LEVELS, Backgrounds.MINDER, 1,
      MEDICAL_LAB, PhysicianStation.class
    ),
    PHYSICIAN_STATION = new Upgrade(
      "Physician Ward",
      Backgrounds.PHYSICIAN.info,
      150,
      Upgrade.THREE_LEVELS, Backgrounds.PHYSICIAN, 1,
      MINDER_STATION, PhysicianStation.class
    );
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! structure.intact()) return null;
    //
    //  If there are patients inside, make sure somebody's available.
    if (numPatients() == 0 && ! staff.onShift(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  If anyone is waiting for treatment, tend to them- including outside the
    //  building.
    final Batch <Target> around = new Batch <Target> ();
    for (Mobile m : inside()) tryAdding(m, around);
    //
    //  The ruler and his household also get special treatment-
    final Actor ruler = base.ruler();
    if (ruler != null) {
      tryAdding(ruler, around);
      final Property home = ruler.mind.home();
      if (home != null) for (Actor a : home.staff().lodgers()) {
        tryAdding(a, around);
      }
    }
    for (Target t : around) t.flagWith(null);
    //
    //  Then, compare the urgency of treatment for each compiled patient:
    for (Target m : around) if (m instanceof Actor) {
      final Actor patient = (Actor) m;
      
      final FirstAid aid = new FirstAid(actor, patient, this);
      aid.addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE);
      choice.add(aid);
      
      final Treatment t = Treatment.nextTreatment(actor, patient, this);
      if (t != null && (t.priorityFor(actor) >= Plan.URGENT || onShift)) {
        choice.add(t);
      }
    }
    //
    //  Manufacture basic medicines for later use.
    final Manufacture mS = stocks.nextManufacture(actor, REAGENTS_TO_MEDICINE);
    if (mS != null && (choice.empty() || ! onShift)) {
      mS.setBonusFrom(this, false, MEDICAL_LAB);
      choice.add(mS);
    }
    //
    //  Otherwise, just tend the desk...
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.pickMostUrgent();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    choice.add(SickLeave.nextLeaveFor(client, this, VISIT_COST));
    choice.add(BringUtils.nextHomePurchase(client, this));
  }
  
  
  private void tryAdding(Target patient, Batch <Target> around) {
    if (patient.flaggedWith() != null) return;
    around.add(patient);
    patient.flagWith(around);
  }
  
  
  private int numPatients() {
    int count = 0;
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor actor = (Actor) m;
      if      (actor.health.conscious() == false   ) count++;
      else if (actor.health.injuryLevel() > 0      ) count++;
      else if (actor.isDoing(SickLeave.class, null)) count++;
    }
    return count;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    final int numU = (1 + structure.numUpgrades()) / 2;
    int powerNeed = 2 + numU;
    //
    //  Sickbays consumes medicine and power based on current upgrade level,
    //  and have a mild positive effect on ambience-
    stocks.incDemand(MEDICINE, 5, 1, true);
    stocks.translateDemands(REAGENTS_TO_MEDICINE, 1);
    stocks.forceDemand(POWER, powerNeed, false);
    structure.setAmbienceVal(4 + numU);
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.MINDER, Backgrounds.PHYSICIAN };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.MINDER   ) return nO + 2;
    if (v == Backgrounds.PHYSICIAN) return nO + 1;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] { MEDICINE, SERVICE_HEALTHCARE };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { GREENS, CATALYST, MEDICINE };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "sickbay");
  }
  
  
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      "Your Physicians allow your citizens' injuries, diseases and physical "+
      "trauma to be treated quickly and effectively.",
      this, REAGENTS_TO_MEDICINE, MEDICAL_LAB
    );
  }
}






//  TODO:  Restore this once the Treatment behaviour is cleaned up!
/*
private void updateCloneOrders(int numUpdates) {
  if (numUpdates % 10 != 0) return;
  //
  //  Clean out any orders that have expired.
  for (Manufacture order : cloneOrders) {
    final Actor patient = (Actor) order.made().refers;
    final boolean done =
        patient.aboard() != this ||
        (! order.venue.structure.intact()) ||
        order.finished() ||
        (! order.venue.stocks.specialOrders().includes(order));
    if (done) {
      if (verbose) I.sayAbout(this, "Removing order: "+order);
      cloneOrders.remove(order);
    }
  }
  //
  //  Place part-cloning orders for actors in a critical condition-
  //
  //  TODO:  Allow for placement of orders at the Artificer as well?
  for (Mobile m : inside()) if (m instanceof Actor) {
    final Actor actor = (Actor) m;
    if ((! actor.health.suspended()) || hasCloneOrder(actor)) continue;
    final Venue venue = findCloningVenue();
    if (venue == null) continue;
    final Item ordered = Treatment.replicantFor(actor);
    if (ordered == null) continue;
    final Manufacture order = new Manufacture(
      null, venue, PROTEIN_TO_REPLICANTS, Item.withAmount(ordered, 1)
    );
    venue.stocks.addSpecialOrder(order);
    cloneOrders.add(order);
    if (verbose) I.sayAbout(this, "Placing order: "+order);
  }
}


private boolean hasCloneOrder(Actor a) {
  for (Manufacture order : cloneOrders) {
    if (order.made().refers == a) return true;
  }
  return false;
}


private Venue findCloningVenue() {
  final Batch <Venue> near = new Batch <Venue> ();
  world.presences.sampleFromKey(this, world, 5, near, CultureVats.class);
  Venue picked = null;
  float bestRating = 0;
  for (Venue v : near) {
    final float rating = rateCloneVenue(v);
    if (rating > bestRating) { bestRating = rating; picked = v; }
  }
  return picked;
}


private float rateCloneVenue(Venue v) {
  if (! v.structure.intact()) return -1;
  final int UL = v.structure.upgradeLevel(CultureVats.ORGAN_BANKS);
  if (UL <= 0) return -1;
  final int SS = World.SECTOR_SIZE * 2;
  float rating = 10;
  rating *= SS / (SS + Spacing.distance(this, v));
  rating *= 1 + UL;
  rating *= 2 / (2 + v.stocks.specialOrders().size());
  return rating;
}
//*/


