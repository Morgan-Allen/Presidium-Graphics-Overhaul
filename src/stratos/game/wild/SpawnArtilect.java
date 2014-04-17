

package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;



//  TODO:  Allow full repairs if you have proper facilities and raw
//  materials (as an initiate would.)

//  TODO:  Allow manufacture of cyborgs, if the subject is organic.

public class SpawnArtilect extends Plan implements Qualities {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean verbose = true;
  
  
  final Actor repairs;
  
  
  public SpawnArtilect(Actor actor, Actor repairs) {
    super(actor, repairs);
    this.repairs = repairs;
  }
  
  
  public SpawnArtilect(Session s) throws Exception {
    super(s);
    repairs = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(repairs);
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean valid() {
    if (actor == null || ! actor.inWorld()) return false;
    if (repairs.health.organic()) return false;
    if (repairs.health.injuryLevel() < 0.5f) return false;
    if ((! repairs.indoors()) && (! repairs.inWorld())) return false;
    return true;
  }
  
  
  final static Skill BASE_SKILLS[] = { ASSEMBLY, INSCRIPTION };
  
  
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    
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
    if (repairs.health.injuryLevel() < 0.5f) return null;
    final Action makes = new Action(
      actor, repairs,
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
    
    final float DC = repairDC();
    float success = 0;
    success += actor.traits.test(ASSEMBLY, DC, 1) ? 1 : 0;
    success += actor.traits.test(INSCRIPTION, DC, 1) ? 1 : 0;
    location.structure.takeDamage(1.0f * Rand.num());
    if (success <= 0) return false;

    if (repairs != actor) {
      
      if (! repairs.inWorld()) {
        repairs.health.setupHealth(0, 0.1f * success, 0.4f);
        repairs.enterWorldAt(location, actor.world());
      }
      else if (! repairs.health.alive()) {
        repairs.health.setState(ActorHealth.STATE_RESTING);
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
    repairs.health.liftInjury(success);
    
    return true;
  }
  
  
  public boolean actionStay(Actor actor, Venue location) {
    return true;
  }
  
  
  
  /**  UI and interface methods-
    */
  public void describeBehaviour(Description d) {
  }
}
