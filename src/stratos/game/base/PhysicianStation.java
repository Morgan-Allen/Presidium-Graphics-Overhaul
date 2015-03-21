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
      1, REAGENTS, 1, GREENS, TO, 1, MEDICINE,
      MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    )
  ;
  
  final static VenueProfile PROFILE = new VenueProfile(
    PhysicianStation.class, "physician_station", "Sickbay",
    3, 2, false, NO_REQUIREMENTS, REAGENTS_TO_MEDICINE
  );
  
  
  final List <Plan> neuralScans = new List <Plan> ();  //TODO:  Use this?
  final List <Manufacture> cloneOrders = new List <Manufacture> ();
  
  
  public PhysicianStation(Base base) {
    super(PROFILE, base);
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
  
  //  TODO:
  //  Apothecary.       Eugenics Lab.           Intensive Care.
  //  Stimulants.       Hypnotics & Sedatives.  Minder Training.
  //  Neural Scanning.  Cold Storage.           Reconstruction.
  //  
  //  Level 2 Upgrade.  Level 3 Upgrade.
  
  final public static Upgrade
    APOTHECARY = new Upgrade(
      "Apothecary",
      "A selection of therapeutic drugs and immune modulators help to curb "+
      "the spread of contagious disease and assist in birth control.",
      250,
      Upgrade.THREE_LEVELS, null, 1,
      null, PhysicianStation.class, ALL_UPGRADES
    ),
    EMERGENCY_AID = new Upgrade(
      "Emergency Aid",
      "Surgical tools, anaesthetics and plasma reserves ensure that serious "+
      "(but non-fatal) injuries can be dealt with quickly, and speeds the "+
      "local production of Stim Kits.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      null, PhysicianStation.class, ALL_UPGRADES
    ),
    MINDER_STATION = new Upgrade(
      "Minder Station",
      "Minders are essential to monitoring patients' condition and tending "+
      "to diet and sanitary needs, but are only familiar with more common "+
      "medications and standard emergency protocol.",
      50,
      Upgrade.THREE_LEVELS, Backgrounds.MINDER, 1,
      APOTHECARY, PhysicianStation.class, ALL_UPGRADES
    ),
    NEURAL_SCANNING = new Upgrade(
      "Neural Scanning",
      "Permits neural scans and basic psych evaluation, aiding in detection "+
      "of mental disturbance or subversion, and permitting engram backups in "+
      "case of death.  Mandatory for key personnel.",
      350,
      Upgrade.THREE_LEVELS, null, 1,
      EMERGENCY_AID, PhysicianStation.class, ALL_UPGRADES
    ),
    INTENSIVE_CARE = new Upgrade(
      "Intensive Care",
      "Intensive care allows a chance for patients on death's door to make a "+
      "gradual comeback, covering everything from life support and tissue "+
      "grafting to cybernetic prosthesis and engram fusion.",
      400,
      Upgrade.THREE_LEVELS, null, 1,
      MINDER_STATION, PhysicianStation.class, ALL_UPGRADES
    ),
    PHYSICIAN_STATION = new Upgrade(
      "Physician Station",
      "Physicians undergo extensive education in every aspect of human "+
      "metabolism and anatomy, are adept as surgeons, and can tailor their "+
      "treatments to the idiosyncracies of a given patient.",
      150,
      Upgrade.THREE_LEVELS, Backgrounds.PHYSICIAN, 1,
      EMERGENCY_AID, PhysicianStation.class, ALL_UPGRADES
    );
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! structure.intact()) return null;
    
    //  If there are patients inside, make sure somebody's available.
    if (numPatients() == 0 && ! staff.onShift(actor)) return null;
    final Choice choice = new Choice(actor);
    
    //  Manufacture basic medicines for later use.
    final Manufacture mS = stocks.nextManufacture(actor, REAGENTS_TO_MEDICINE);
    if (mS != null) {
      mS.setBonusFrom(this, false, APOTHECARY);
      choice.add(mS);
    }
    
    //  If anyone is waiting for treatment, tend to them- including outside the
    //  building.
    final Batch <Target> around = new Batch <Target> ();
    for (Target t : actor.senses.awareOf()) around.include(t);
    for (Mobile m : this.inside()) around.include(m);
    if (base.ruler() != null) around.include(base.ruler());
    
    for (Target m : around) if (m instanceof Actor) {
      final FirstAid t = new FirstAid(actor, (Actor) m, this);
      t.addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE);
      choice.add(t);
      choice.add(Treatment.nextTreatment(actor, (Actor) m, this));
    }
    
    //  Otherwise, just tend the desk.
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.pickMostUrgent();
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    choice.add(SickLeave.nextLeaveFor(forActor, this, VISIT_COST));
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
    int medNeed = 2 + numU, powerNeed = 2 + numU;
    //
    //  Sickbays consumes medicine and power based on current upgrade level,
    //  and have a mild positive effect on ambience-
    stocks.incDemand(MEDICINE, medNeed, Tier.PRODUCER, 1);
    stocks.incDemand(REAGENTS, medNeed, Tier.CONSUMER, 1);
    stocks.forceDemand(POWER, powerNeed, Tier.CONSUMER);
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
    return new Traded[] { REAGENTS, MEDICINE };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "sickbay");
  }
  
  
  public String helpInfo() {
    return
      "The Sickbay allows your citizens' injuries, diseases and trauma to be "+
      "treated quickly and effectively.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_PHYSICIAN;
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


