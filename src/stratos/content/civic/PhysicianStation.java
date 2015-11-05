/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.Ambience;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
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
    "media/Buildings/physician/physician_clinic.png", 3, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    PhysicianStation.class, "media/GUI/Buttons/hospice_button.gif"
  );
  
  final static float
    VISIT_COST = Backgrounds.MIN_DAILY_EXPENSE * 2;
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    PhysicianStation.class, "physician_station",
    "Physician Station", Target.TYPE_PHYSICIAN, ICON,
    "The Physician Station allows your citizens' injuries or diseases to be "+
    "treated quickly and effectively.",
    4, 2, Structure.IS_NORMAL, Owner.TIER_FACILITY, 200, 2,
    MEDICINE, SERVICE_HEALTHCARE, MINDER, PHYSICIAN
  );
  
  
  final List <Manufacture> cloneOrders = new List <Manufacture> ();
  
  
  public PhysicianStation(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public PhysicianStation(Session s) throws Exception {
    super(s);
    s.loadObjects(cloneOrders);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(cloneOrders);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.THREE_LEVELS, null,
      new Object[] { 10, ANATOMY, 10, PHARMACY, 5, GENE_CULTURE },
      600,
      800,
      1000
    ),
    
    MEDICAL_LAB = new Upgrade(
      "Medical Lab",
      "Speeds the production of "+MEDICINE+" and "+SOMA+" by 33%.  Benefits "+
      "the treatment and diagnosis of most disease.",
      250,
      Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    GENE_THERAPIES = new Upgrade(
      "Gene Therapies",
      "Allows for screening of hereditary illness and DNA damage, helping to"+
      "to treat chronic disease and prevent birth defects.",
      350,
      Upgrade.TWO_LEVELS, MEDICAL_LAB, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    INTENSIVE_CARE = new Upgrade(
      "Intensive Care",
      "Surgical tools, anaesthetics and plasma reserves ensure that serious "+
      "injuries can be dealt with quickly.",
      300,
      Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    
    CLONING_PROGRAM = new Upgrade(
      "Cloning Program",
      "Replacement organs and suspended animation allow the clinically dead "+
      "to make a gradual comeback.",
      400,
      Upgrade.TWO_LEVELS, INTENSIVE_CARE, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    //
    //  Psy-boosting meds that consume spyce.
    PNEUMA_LAB = null,
    
    //  Personal ability just for physicians.
    PAX_CONDITIONING = null;
    /*
    MINDER_POST = new Upgrade(
      "Minder Post",
      MINDER.info,
      50,
      Upgrade.THREE_LEVELS, MEDICAL_LAB, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, Backgrounds.MINDER
    ),
    PHYSICIAN_OFFICE = new Upgrade(
      "Physician Office",
      PHYSICIAN.info,
      150,
      Upgrade.THREE_LEVELS, MINDER_POST, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, Backgrounds.PHYSICIAN
    );
  //*/
  
  final public static Conversion
    REAGENTS_TO_MEDICINE = new Conversion(
      BLUEPRINT, "reagents_to_medicine",
      1, REAGENTS, 1, GREENS, TO, 2, MEDICINE,
      MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ),
    REAGENTS_TO_SOMA = new Conversion(
      BLUEPRINT, "waste_to_soma",
      1, REAGENTS, TO, 3, SOMA,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    );
  
  
  public Behaviour jobFor(Actor actor) {
    //
    //  If anyone is waiting for treatment, tend to them- including outside the
    //  building.
    final boolean onShift = staff.onShift(actor);
    final Choice choice = new Choice(actor);
    final Batch <Target> around = new Batch <Target> ();
    for (Mobile m : inside()) tryAdding(m, around);
    //
    //  The ruler and his household also get special treatment.
    final Actor ruler = base.ruler();
    if (ruler != null) {
      final Property home = ruler.mind.home();
      tryAdding(ruler, around);
      if (home != null) for (Actor a : home.staff().lodgers()) {
        tryAdding(a, around);
      }
    }
    for (Target t : around) t.flagWith(null);
    if (around.size() == 0 && staff.offDuty(actor)) return null;
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
    final Plan treats = (Plan) choice.pickMostUrgent(Plan.ROUTINE);
    if (treats != null) return treats;
    //
    //  Manufacture basic medicines for later use.
    final Manufacture mS = stocks.nextManufacture(actor, REAGENTS_TO_MEDICINE);
    if (mS != null) choice.add(mS.setBonusFrom(this, false, MEDICAL_LAB));

    final Plan works = (Plan) choice.pickMostUrgent(Plan.ROUTINE);
    if (works != null) return works;
    //
    //  Consider performing research-
    //choice.add(Studying.asResearch(actor, this, Target.TYPE_PHYSICIAN));
    //
    //  Otherwise, just tend the desk...
    choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    choice.add(SickLeave.nextLeaveFor(client, this, VISIT_COST));
    choice.add(BringUtils.nextPersonalPurchase(client, this));
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
    final float numU = (1 + structure.numOptionalUpgrades()) / 2f;
    //
    //  The station consumes medicine and power based on current upgrade level,
    //  and has a mild positive effect on ambience-
    stocks.incDemand(MEDICINE, 2 + numU, 1, true);
    stocks.forceDemand(POWER, 2 + numU, false);
    structure.setAmbienceVal(2 + numU);
    stocks.translateRawDemands(REAGENTS_TO_MEDICINE, 1);
    stocks.translateRawDemands(REAGENTS_TO_SOMA    , 1);
  }
  
  
  public int numPositions(Background v) {
    final int level = structure.mainUpgradeLevel();
    if (v == MINDER   ) return level;
    if (v == PHYSICIAN) return level;
    return 0;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { GREENS, REAGENTS, MEDICINE };
  }
  
  
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      super.helpInfo(), this, REAGENTS_TO_MEDICINE, MEDICAL_LAB
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


