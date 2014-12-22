/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.plans;

import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Make initial gene seed mandatory?
//  TODO:  Allow 'taming' of these specimens- i.e, so they start with good
//         relations toward their caretaker?


public class AnimalBreeding extends Plan {
  
  
  final static int
    BREED_TIME_PER_10_HP = Stage.STANDARD_DAY_LENGTH,
    MINIMUM_TAILOR_TIME  = Stage.STANDARD_DAY_LENGTH;
  
  private static boolean
    evalVerbose  = true ,
    stepsVerbose = false;
  
  
  final Venue station;
  final Fauna handled;
  final Target releasePoint;
  final Item asSeed, asSample;
  
  
  
  private AnimalBreeding(
    Actor actor, Venue station, Fauna handled, Target releasePoint
  ) {
    super(actor, station, true, NO_HARM);
    this.station = station;
    this.handled = handled;
    this.releasePoint = releasePoint;
    this.asSeed   = Item.withReference(GENE_SEED, handled.species());
    this.asSample = Item.withReference(SAMPLES  , handled          );
  }
  
  
  public AnimalBreeding(Session s) throws Exception {
    super(s);
    station       = (Venue) s.loadObject();
    handled       = (Fauna) s.loadObject();
    releasePoint  = s.loadTarget();
    this.asSeed   = Item.loadFrom(s);
    this.asSample = Item.loadFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(station);
    s.saveObject(handled);
    s.saveTarget(releasePoint);
    Item.saveTo(s, asSeed  );
    Item.saveTo(s, asSample);
  }
  
  
  public Plan copyFor(Actor other) {
    return new AnimalBreeding(other, station, handled, releasePoint);
  }
  
  
  public Item asSeed() {
    return asSeed;
  }
  
  
  public Item asSample() {
    return asSample;
  }
  
  
  
  /**  External factory methods:
    */
  public static AnimalBreeding breedingFor(
    Actor actor, Venue station, Species species, Boarding releasePoint
  ) {
    final Stage world = station.world();
    final Fauna specimen = (Fauna) (
      species == null ? null : species.newSpecimen(Base.wildlife(world))
    );
    if (specimen == null) return null;
    specimen.pathing.updateTarget(releasePoint);
    return new AnimalBreeding(actor, station, specimen, releasePoint);
  }
  
  
  public static Batch <Fauna> breedingAt(Venue station) {
    final Batch <Fauna> matches = new Batch <Fauna> ();
    for (Item match : station.stocks.matches(GENE_SEED)) {
      if (match.refers instanceof Fauna) matches.add((Fauna) match.refers);
    }
    return matches;
  }
  
  
  public static AnimalBreeding nextBreeding(
    Actor actor, Venue station
  ) {
    final boolean report = evalVerbose && (
      I.talkAbout == station || I.talkAbout == actor
    );
    if (report) I.say("\nGETTING NEXT FAUNA TO BREED");
    
    for (Fauna fauna : breedingAt(station)) {
      if (fauna.inWorld()) continue;
      final Target releasePoint = fauna.pathing.target();
      return new AnimalBreeding(actor, station, fauna, releasePoint);
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
  final static Skill BASE_SKILLS[] = { XENOZOOLOGY, DOMESTICS };
  final static Trait BASE_TRAITS[] = { NATURALIST, EMPATHIC };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final boolean active =
      actor.gear.hasItem(asSample) ||
      station.staff().onShift(actor);
    
    final float priority = priorityForActorWith(
      actor, station, active ? ROUTINE : 0,
      NO_MODIFIER, NO_HARM,
      NO_COMPETITION, MILD_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next breeding step for "+handled);
    
    if (handled.inWorld()) return null;
    
    if (actor.gear.hasItem(asSample)) {
      if (report) I.say("  Returning release!");
      final Tile point = Spacing.nearestOpenTile(releasePoint, actor);
      final Action release = new Action(
        actor, point,
        this, "actionRelease",
        Action.REACH_DOWN, "Releasing "
      );
      return release;
    }

    final float progress = station.stocks.amountOf(asSample);
    if (progress >= 1) {
      if (report) I.say("  Returning pickup!");
      final Action pickup = new Action(
        actor, station,
        this, "actionPickup",
        Action.REACH_DOWN, "Releasing "
      );
      return pickup;
    }
    
    if ((station.stocks.amountOf(asSeed) + progress) < 1) {
      if (report) I.say("  Returning gene-tailor!");
      final Action tailor = new Action(
        actor, station,
        this, "actionTailorSeed",
        Action.BUILD, "Tailoring gene seed for "
      );
      return tailor;
    }
    
    else {
      if (report) I.say("  Returning tend action!");
      final Action tends = new Action(
        actor, handled,
        this, "actionTendSpecimen",
        Action.BUILD, "Breeding "
      );
      tends.setMoveTarget(station);
      return tends;
    }
  }
  
  
  public boolean actionTailorSeed(Actor actor, Venue station) {
    float success = 0;
    success += actor.skills.test(GENE_CULTURE, MODERATE_DC, 1, 2);
    success *= actor.skills.test(XENOZOOLOGY , ROUTINE_DC , 1, 2);
    
    if (success <= 0) return false;
    success /= MINIMUM_TAILOR_TIME;
    station.stocks.addItem(Item.withAmount(asSeed, success));
    return true;
  }
  
  
  public boolean actionTendSpecimen(Actor actor, Fauna fauna) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nTending to "+fauna);
    //
    //  First, determine your rate of progress based on intrinsic skill level,
    //  seed quality, and tech upgrades.
    //  TODO:  INCLUDE SEED QUALITY
    final float upgrade = station.structure.upgradeLevel(
      KommandoLodge.CAPTIVE_BREEDING
    );
    float success = 0;
    if (actor.skills.test(XENOZOOLOGY, MODERATE_DC, 1)) success++;
    if (actor.skills.test(DOMESTICS  , ROUTINE_DC , 1)) success++;
    if (success <= 0) return false;
    success *= 1 + (upgrade / 2f);
    //
    //  Check whether the young scamp has enough to eat, and adjust stocks (or
    //  growth rate) accordingly-
    //  TODO:  INCLUDE PROTEIN CONSUMPTION
    final float bulk = fauna.health.maxHealth();
    success *= 10 / (bulk * BREED_TIME_PER_10_HP);
    final Item eaten = Item.withAmount(CARBS, success * 2);
    if (station.stocks.hasItem(eaten)) {
      station.stocks.removeItem(eaten);
    }
    else success /= 5;
    //
    //  Finally, adjust the specimen's progress and store the result:
    //  TODO:  USE A SUMMONS HERE INSTEAD
    Item basis = station.stocks.matchFor(asSample);
    if (basis == null) {
      basis = Item.withAmount(asSample, success);
      fauna.health.setupHealth(0, 1, 0);
    }
    if (basis.amount < 1) {
      station.stocks.addItem(Item.withAmount(basis, success));
      final float age = fauna.health.ageLevel();
      fauna.health.setMaturity(age + (success / 4));
    }
    if (report) {
      I.say("  Growth increment: "+success);
      I.say("  Stocked:          "+basis.amount);
      I.say("  Maturity:         "+fauna.health.ageLevel());
      I.say("  Release point:    "+releasePoint);
    }
    return true;
  }
  
  
  public boolean actionPickup(Actor actor, Venue station) {
    final Item basis = station.stocks.matchFor(asSample);
    if (basis == null) return false;
    station.stocks.transfer(basis, actor);
    return true;
  }
  
  
  public boolean actionRelease(Actor actor, Boarding point) {
    final Stage world = actor.world();
    actor.gear.removeMatch(asSample);
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







