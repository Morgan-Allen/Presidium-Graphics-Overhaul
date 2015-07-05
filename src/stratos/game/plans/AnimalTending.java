/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Make initial gene seed mandatory in 'hardCore' mode.
//  TODO:  Allow 'taming' of these specimens, so that mutual hostilities are
//         less likely.

public class AnimalTending extends Plan {
  
  final static int
    BREED_TIME_PER_10_HP = Stage.STANDARD_DAY_LENGTH,
    MINIMUM_TAILOR_TIME  = Stage.STANDARD_DAY_LENGTH;
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  
  final Venue station;
  final Fauna handled;
  final Target releasePoint;
  final Item asSeed;
  
  
  
  private AnimalTending(
    Actor actor, Venue station, Fauna handled, Target releasePoint
  ) {
    super(actor, station, MOTIVE_JOB, NO_HARM);
    this.station = station;
    this.handled = handled;
    this.releasePoint = releasePoint;
    this.asSeed   = Item.withReference(GENE_SEED, handled.species());
  }
  
  
  public AnimalTending(Session s) throws Exception {
    super(s);
    station       = (Venue) s.loadObject();
    handled       = (Fauna) s.loadObject();
    releasePoint  = s.loadTarget();
    this.asSeed   = Item.loadFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(station);
    s.saveObject(handled);
    s.saveTarget(releasePoint);
    Item.saveTo(s, asSeed  );
  }
  
  
  public Plan copyFor(Actor other) {
    return new AnimalTending(other, station, handled, releasePoint);
  }
  
  
  public Item asSeed() {
    return asSeed;
  }
  
  
  
  /**  External factory methods:
    */
  public static AnimalTending breedingFor(
    Actor actor, Venue station, Species species, Boarding releasePoint
  ) {
    final Stage world = station.world();
    final Fauna specimen = (Fauna) (
      species == null ? null : species.sampleFor(Base.wildlife(world))
    );
    if (specimen == null) return null;
    specimen.pathing.updateTarget(releasePoint);
    return new AnimalTending(actor, station, specimen, releasePoint);
  }
  
  
  public static Batch <Fauna> nestingAt(Venue station) {
    final Batch <Fauna> matches = new Batch <Fauna> ();
    for (Mobile m : station.inside()) if (m instanceof Fauna) {
      matches.add((Fauna) m);
    }
    return matches;
  }
  
  
  public static AnimalTending nextTending(
    Actor actor, Venue station
  ) {
    final boolean report = evalVerbose && (
      I.talkAbout == station || I.talkAbout == actor
    );
    if (report) I.say("\nGETTING NEXT FAUNA TO BREED");
    
    for (Fauna fauna : nestingAt(station)) {
      if (fauna.inWorld()) continue;
      final Target releasePoint = fauna.pathing.target();
      return new AnimalTending(actor, station, fauna, releasePoint);
    }
    
    final Stage world = station.world();
    final Pick <Species> pick = new Pick <Species> ();
    final Tile releasePoint = world.tileAt(
      world.size * Rand.num(), world.size * Rand.num()
    );
    
    for (Species species : Species.ANIMAL_SPECIES) {
      final float crowding = Nest.crowdingFor(releasePoint, species, world);
      if (report) I.say("  Crowding of "+species+" is "+crowding);
      if (crowding >= 1) continue;
      pick.compare(species, 10f / (1 + crowding));
    }
    
    final Species toBreed = pick.result();
    if (report) I.say("  Species picked: "+toBreed);
    return breedingFor(actor, station, toBreed, releasePoint);
  }
  
  
  
  /**  Priority and target evaluation-
    */
  final static Skill BASE_SKILLS[] = { GENE_CULTURE, XENOZOOLOGY };
  final static Trait BASE_TRAITS[] = { NATURALIST, EMPATHIC };
  
  
  protected float getPriority() {
    if (station.staff.offDuty(actor)) return 0;
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, 1, competence(), -1, MILD_FAIL_RISK, BASE_TRAITS
    );
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    return PlanUtils.successForActorWith(actor, BASE_SKILLS, ROUTINE_DC, false);
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next breeding step for "+handled);
    
    final FirstAid aids = new FirstAid(actor, handled);
    if (Plan.canFollow(actor, aids, true)) return aids;
    
    final Treatment treats = Treatment.nextTreatment(actor, handled, station);
    if (Plan.canFollow(actor, treats, true)) return treats;
    
    if (handled.health.agingStage() > ActorHealth.AGE_JUVENILE) {
      if (report) I.say("  Returning release!");
      final Tile point = Spacing.nearestOpenTile(releasePoint, actor);
      return new BringStretcher(actor, handled, point);
    }
    if (handled.inWorld()) {
      if (report) I.say("  Returning tend action!");
      final Action tends = new Action(
        actor, handled,
        this, "actionTendSpecimen",
        Action.BUILD, "Breeding "
      );
      tends.setMoveTarget(station);
      return tends;
    }
    else {
      if (report) I.say("  Returning gene-tailor!");
      final Action tailor = new Action(
        actor, station,
        this, "actionTailorSeed",
        Action.BUILD, "Tailoring gene seed for "
      );
      return tailor;
    }
  }
  
  
  public boolean actionTailorSeed(Actor actor, Venue station) {
    float success = 0;
    success += actor.skills.test(GENE_CULTURE, MODERATE_DC, 1, 2);
    success *= actor.skills.test(XENOZOOLOGY , ROUTINE_DC , 1, 2);
    
    if (success <= 0) return false;
    success /= MINIMUM_TAILOR_TIME;
    station.stocks.addItem(Item.withAmount(asSeed, success));
    
    if (station.stocks.hasItem(asSeed)) {
      handled.enterWorldAt(station, station.world());
      if (station instanceof Captivity) {
        handled.bindToMount((Captivity) station);
      }
      else I.say("\nWARNING: Animal-tending sites should allow captivity!");
    }
    return true;
  }
  
  
  public boolean actionTendSpecimen(Actor actor, Fauna fauna) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nTending to "+fauna);
    //
    //  First, determine your rate of progress based on intrinsic skill level,
    //  seed quality, and tech upgrades.
    //  TODO:  INCLUDE SEED QUALITY
    //  TODO:  INCLUDE EFFECTS OF UPGRADES (either that or a check bonus).
    final float upgrade = 0;
    
    float success = 0;
    if (actor.skills.test(XENOZOOLOGY, MODERATE_DC, 1)) success++;
    if (actor.skills.test(DOMESTICS  , ROUTINE_DC , 1)) success++;
    if (success <= 0) return false;
    success *= 1 + (upgrade / 2f);
    //
    //  Check whether the young scamp has enough to eat, and adjust stocks (or
    //  growth rate) accordingly-
    final boolean predator = fauna.species.predator();
    final float bulk = fauna.health.maxHealth();
    success *= 10 / (bulk * BREED_TIME_PER_10_HP);
    
    final Item eaten = Item.withAmount(predator ? CARBS : PROTEIN, success * 2);
    if (station.stocks.hasItem(eaten)) station.stocks.removeItem(eaten);
    else success /= 5;
    //
    //  Finally, adjust the specimen's progress and store the result:
    //
    //  TODO:  EITHER RESTORE INJURY OR ADJUST AGE!  AND IMPROVE RELATIONS WITH
    //  THE SPECIMEN IN SO DOING!
    
    final float age = fauna.health.ageLevel();
    fauna.health.setMaturity(age + (success / 4));
    
    if (report) {
      I.say("  Growth increment: "+success);
      I.say("  Maturity:         "+fauna.health.ageLevel());
      I.say("  Release point:    "+releasePoint);
    }
    return true;
  }
  
  
  //  TODO:  HANDLE THIS AUTOMATICALLY ONCE GROWTH/TENDING-PROGRESS IS
  //  COMPLETE, BUT BEFORE THE STRETCHER-DELIVERY!
  
  public boolean actionPrepRelease(Actor actor, Boarding point) {
    final Stage world = actor.world();
    handled.enterWorldAt(point, world);
    
    if (point instanceof Venue) {
      final Venue home = (Venue) point;
      handled.assignBase(home.base());
      handled.mind.setHome(home);
      handled.goAboard(home, world);
    }
    else {
      handled.mind.setHome(null);
      handled.assignBase(Base.wildlife(world));
    }
    
    if (stepsVerbose) {
      float crowding = Nest.crowdingFor(point, handled.species, world);
      I.say("  CROWDING AT RELEASE POINT: "+crowding);
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    //  TODO:  Use stages here?
    
    if (super.needsSuffix(d, "Breeding ")) {
      d.append(handled.species+" at ");
      
      Target at = actor.actionFocus();
      if (! (at instanceof Boarding)) at = station;
      d.append(at);
    }
  }
}







