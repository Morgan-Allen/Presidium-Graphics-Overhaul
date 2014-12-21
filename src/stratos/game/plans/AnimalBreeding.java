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



//  TODO:  Speed production if you have gene seed (or make mandatory?)

//  TODO:  Predators, by contrast, are domesticated and used to assist in
//  hunting and patrols.  (Possibly as cavalry?)


public class AnimalBreeding extends Plan {
  
  
  final static int
    BREED_TIME_PER_10_HP = Stage.STANDARD_DAY_LENGTH;
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = true ;
  
  
  final Venue station;
  final Fauna handled;
  final Target releasePoint;
  final Item asStock;
  
  
  
  private AnimalBreeding(
    Actor actor, Venue station, Fauna handled, Target releasePoint
  ) {
    super(actor, station, true, NO_HARM);
    this.station = station;
    this.handled = handled;
    this.releasePoint = releasePoint;
    this.asStock = Item.withReference(GENE_SEED, handled);
  }
  
  
  public AnimalBreeding(Session s) throws Exception {
    super(s);
    station = (Venue) s.loadObject();
    handled = (Fauna) s.loadObject();
    releasePoint = s.loadTarget();
    this.asStock = Item.withReference(GENE_SEED, handled);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(station);
    s.saveObject(handled);
    s.saveTarget(releasePoint);
  }
  
  
  public Plan copyFor(Actor other) {
    return new AnimalBreeding(other, station, handled, releasePoint);
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
    final boolean report = evalVerbose && I.talkAbout == station;
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
    
    return breedingFor(actor, station, pick.result(), releasePoint);
  }
  
  
  
  /**  Priority and target evaluation-
    */
  final static Skill BASE_SKILLS[] = { XENOZOOLOGY, DOMESTICS };
  final static Trait BASE_TRAITS[] = { NATURALIST, EMPATHIC };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final Property work = actor.mind.work();
    final boolean atWork = work != null && work.personnel().onShift(actor);
    
    final float priority = priorityForActorWith(
      actor, station, atWork ? ROUTINE : IDLE,
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
    
    //  TODO:  Use samples instead of gene-seed?  Break into two steps?

    if (actor.gear.hasItem(asStock)) {
      if (report) I.say("  Returning release!");
      final Tile point = Spacing.nearestOpenTile(releasePoint, actor);
      final Action release = new Action(
        actor, point,
        this, "actionRelease",
        Action.REACH_DOWN, "Releasing "
      );
      return release;
    }
    
    if (station.stocks.hasItem(asStock)) {
      if (report) I.say("  Returning pickup!");
      final Action pickup = new Action(
        actor, station,
        this, "actionPickup",
        Action.REACH_DOWN, "Releasing "
      );
      return pickup;
    }
    
    if (! handled.inWorld()) {
      if (report) I.say("  Returning tend action!");
      final Action tends = new Action(
        actor, handled,
        this, "actionTendSpecimen",
        Action.BUILD, "Breeding "
      );
      tends.setMoveTarget(station);
      return tends;
    }
    
    return null;
  }
  
  
  public boolean actionTendSpecimen(Actor actor, Fauna fauna) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nTending to "+fauna);
    
    final float upgrade = station.structure.upgradeLevel(
      KommandoLodge.CAPTIVE_BREEDING
    );
    float success = 0;
    if (actor.skills.test(XENOZOOLOGY, ROUTINE_DC, 1)) success++;
    if (actor.skills.test(DOMESTICS  , ROUTINE_DC, 1)) success++;
    if (success <= 0) return false;
    success *= 1 + (upgrade / 2f);
    
    final float bulk = fauna.health.maxHealth();
    float inc = 10 / bulk;
    inc *= success / BREED_TIME_PER_10_HP;
    if (report) I.say("  Growth increment: "+inc);
    
    final Item eaten = Item.withAmount(CARBS, inc * 2);
    if (station.stocks.hasItem(eaten)) {
      station.stocks.removeItem(eaten);
    }
    else inc /= 5;
    
    Item basis = station.stocks.matchFor(asStock);
    if (basis == null) {
      basis = Item.with(GENE_SEED, fauna, inc, 0);
      fauna.health.setupHealth(0, 1, 0);
      station.stocks.addItem(basis);
    }
    if (basis.amount < 1) {
      station.stocks.addItem(Item.withAmount(basis, inc));
      final float age = fauna.health.ageLevel();
      fauna.health.setMaturity(age + (inc / 4));
    }
    
    if (report) {
      I.say("  Stocked: "+basis.amount);
      I.say("  Maturity: "+fauna.health.ageLevel());
      I.say("  Release point: "+releasePoint);
    }
    return true;
  }
  
  
  public boolean actionPickup(Actor actor, Venue station) {
    final Item basis = station.stocks.matchFor(asStock);
    if (basis == null) return false;
    station.stocks.transfer(basis, actor);
    return true;
  }
  
  
  public boolean actionRelease(Actor actor, Boarding point) {
    final Stage world = actor.world();
    actor.gear.removeMatch(asStock);
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
    if (super.needsSuffix(d, "Breeding ")) {
      d.append(handled.species+" at ");
      d.append(station);
    }
  }
}







