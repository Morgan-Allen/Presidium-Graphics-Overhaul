

package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;



//  TODO:  Allow full repairs if you have proper facilities and raw
//  materials (as an initiate would.)
//  TODO:  Allow manufacture of cybrids, if the subject is organic.


public class SpawnArtilect extends Plan implements Qualities {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose   = false,
    effectVerbose = false;
  
  final static float
    TIME_PER_10_HP = World.STANDARD_DAY_LENGTH,
    MIN_DAMAGE     = 0.4f / ActorHealth.MAX_INJURY,
    INIT_HEALTH    = 0.1f;
  
  final Actor repairs;
  final Venue works;
  
  
  public SpawnArtilect(Actor actor, Actor repairs, Venue works) {
    super(actor, repairs);
    this.repairs = repairs;
    this.works = works;
  }
  
  
  public SpawnArtilect(Session s) throws Exception {
    super(s);
    repairs = (Actor) s.loadObject();
    works = (Venue) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(repairs);
    s.saveObject(works);
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean valid() {
    if (actor == null || ! actor.inWorld()) return false;
    if (! works.structure.intact()) return false;
    if (repairs.health.organic()) return false;
    
    if (repairs.inWorld()) {
      if (repairs.health.injuryLevel() < MIN_DAMAGE) return false;
      if (! repairs.indoors()) return false;
    }
    return true;
  }
  
  
  final static Skill BASE_SKILLS[] = { ASSEMBLY, INSCRIPTION };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final float priority = priorityForActorWith(
      actor, repairs, ROUTINE,
      MILD_HELP, MILD_COOPERATION,
      BASE_SKILLS, NO_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, MILD_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    if (repairs.inWorld() && repairs.health.injuryLevel() < MIN_DAMAGE) {
      return null;
    }
    final Action makes = new Action(
      actor, works,
      this, "actionMake",
      Action.BUILD, "Repairing"
    );
    return makes;
  }
  
  
  private float repairDC() {
    float DC = 100;
    if (repairs instanceof Drone) DC = 5;
    if (repairs instanceof Tripod) DC = 15;
    if (repairs instanceof Cranial) DC = 25;
    return DC;
  }
  
  
  protected float successChance() {
    final float DC = repairDC();
    float chance = 0;
    chance += actor.traits.chance(ASSEMBLY, DC) / 2;
    chance += actor.traits.chance(INSCRIPTION, DC) / 2;
    return chance;
  }
  
  
  public boolean actionMake(Actor actor, Venue location) {
    final boolean report = effectVerbose && I.talkAbout == actor;
    if (report) I.say("\nPERFORMING REPAIRS ON "+repairs);
    
    final float DC = repairDC() + (repairs.inWorld() ? 0 : 5);
    final float inc = 10f / TIME_PER_10_HP;
    float success = 0;
    success += actor.traits.test(ASSEMBLY, DC, 1) ? 1 : 0;
    success += actor.traits.test(INSCRIPTION, DC, 1) ? 1 : 0;
    location.structure.repairBy(-1.0f * Rand.num() * inc);
    if (success <= 0) return false;
    
    if (report) I.say("  Success was: "+success);
    if (repairs != actor) {
      
      if (! repairs.inWorld()) {
        repairs.enterWorldAt(location, actor.world());
        repairs.goAboard(location, actor.world());
        repairs.mind.setHome(location);
        
        repairs.health.setupHealth(0, 0.5f, 1);
        repairs.health.setInjuryLevel(1 - (INIT_HEALTH * success));
        repairs.health.setState(ActorHealth.STATE_RESTING);
        if (report) I.say("  Starting injury: "+repairs.health.injuryLevel());
      }
      else if (! repairs.health.alive()) {
        repairs.health.setState(ActorHealth.STATE_SUSPEND);
      }
      
      final Action stay = new Action(
        repairs, location,
        this, "actionStay",
        Action.FALL, "Staying put"
      );
      stay.setProperties(Action.NO_LOOP);
      stay.setPriority(Action.PARAMOUNT);
      repairs.mind.assignBehaviour(stay);
    }
    
    repairs.health.liftInjury(success * inc);
    if (repairs != actor && repairs.health.injuryLevel() < MIN_DAMAGE) {
      repairs.health.setState(ActorHealth.STATE_RESTING);
    }
    
    if (report) I.say("  Current injury: "+repairs.health.injuryLevel());
    return true;
  }
  
  
  public boolean actionStay(Actor actor, Venue location) {
    return true;
  }
  
  
  
  /**  UI and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Refurbishing ");
    d.append(repairs);
  }
}




