

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;




//  TODO:  JUST CACHE THREAT RATINGS LIKE YOU DO STRENGTH RATINGS, WITHOUT
//         DISTANCE FACTORS.

//  TODO:  The entire combat/retreat system needs a good cleanup.


public class CombatUtils {
  
  
  final static float
    MAX_DANGER  = 2.0f,
    MAX_POWER   = 20;
  private static boolean
    threatsVerbose  = false,
    powerVerbose    = false,
    dangerVerbose   = false;
  
  
  public static float powerLevel(Actor actor) {
    final boolean report = powerVerbose && I.talkAbout == actor;
    
    float estimate = 1;
    estimate *= (actor.gear.armourRating() + actor.gear.attackDamage()) / 10f;
    estimate *= (actor.health.maxHealth()  + actor.gear.shieldCharge()) / 10f;
    estimate *= (2 - actor.health.injuryLevel  ()) / 2f;
    estimate *= (2 - actor.health.stressPenalty()) / 2f;
    
    if (report) {
      I.say("\nESTIMATED POWER LEVEL OF "+actor+" IS "+estimate);
    }
    return estimate;
  }
  
  
  public static float powerLevelRelative(Actor actor, Actor other) {
    //  TODO:  Also consider effects of psy abilities, techniques, et cetera.
    final boolean report = powerVerbose && I.talkAbout == actor;
    
    final float
      otherPower = other.senses.powerLevel(),
      actorPower = actor.senses.powerLevel();
    if (otherPower <= 0) return MAX_POWER;
    
    //  TODO:  Get separate attack/defend skills for the other actor, depending
    //  on what type of weapon they have.
    final Skill attack, defend;
    if (actor.gear.meleeWeapon()) {
      attack = HAND_TO_HAND;
      defend = HAND_TO_HAND;
    }
    else {
      attack = MARKSMANSHIP;
      defend = STEALTH_AND_COVER;
    }
    final float
      actorChance = actor.skills.chance(attack, other, defend, 0),
      otherChance = other.skills.chance(attack, actor, defend, 0);
    
    float estimate = 1;
    estimate *= actorPower / (otherPower + actorPower);
    estimate *= 2 * (actorChance + 1 - otherChance) / 2f;
    estimate = Visit.clamp(estimate, 0, MAX_POWER);
    
    if (report) {
      I.say("\nGETTING POWER OF "+actor+" (Actor)");
      I.say("RELATIVE TO "+other+" (Other)");
      
      I.say("  Actor power: "+actorPower+" chance: "+actorChance);
      I.say("  Actor attack skill: "+actor.traits.useLevel(attack));
      I.say("  Actor defend skill: "+actor.traits.useLevel(defend));

      I.say("  Other power: "+otherPower+" chance: "+otherChance);
      I.say("  Other attack skill: "+other.traits.useLevel(attack));
      I.say("  Other defend skill: "+other.traits.useLevel(defend));
      
      I.say("  Estimated relative power level: "+estimate);
    }
    return estimate;
  }
  
  
  public static boolean isHostileTo(Actor actor, Target target) {
    if (! (target instanceof Actor)) return false;
    final Actor other = (Actor) target;
    final ActorRelations relations = actor.relations;
    
    if (! other.health.conscious()) return false;
    if (relations.dislikes(other)) return true;
    
    final Target victim = other.focusFor(Combat.class);
    if (victim != null && relations.likes(victim)) {
      return relations.valueFor(victim) > relations.valueFor(other);
    }
    return false;
  }
  
  
  public static boolean isAllyOf(Actor actor, Target target) {
    if (! (target instanceof Actor)) return false;
    final Actor other = (Actor) target;
    
    if (! other.health.conscious()) return false;
    if (other.focusFor(Retreat.class) != null) return false;
    if (other.relations.valueFor(actor) > 0.5f) return true;
    return other.base() == actor.base();
  }
  
  
  public static boolean isDowned(Target subject, int object) {
    if (subject instanceof Actor) {
      final Actor struck = (Actor) subject;
      if (object == Combat.OBJECT_DESTROY) return ! struck.health.alive();
      return ! struck.health.conscious();
    }
    if (subject instanceof Venue) {
      return ((Venue) subject).structure.destroyed();
    }
    return false;
  }
  
  
  public static boolean isFleeing(Target subject) {
    if (! (subject instanceof Actor)) return false;
    return ((Actor) subject).isDoing(Retreat.class, null);
  }
  
  

  /**  Returns whatever nearby target seems to be most threatening to the given
    *  actor, partially weighted by distance from the 'primary' argument.  (If
    *  asThreat is true, primary is evaluated as a threat itself.)
    */
  public static Target bestTarget(
    Actor actor, Target primary, boolean asThreat, float harmLevel
  ) {
    final boolean report = threatsVerbose && I.talkAbout == actor;
    
    Target best = null;
    float bestValue = 0;
    final boolean melee = actor.gear.meleeWeapon();
    
    if (asThreat) {
      best = primary;
      bestValue = combatValue(primary, actor, harmLevel) * 1.5f;
    }
    
    for (Target t : actor.senses.awareOf()) {
      final float distance = Spacing.distance(t, primary);
      if (distance > World.SECTOR_SIZE) continue;

      float value = combatValue(t, actor, harmLevel);
      
      if (melee) value /= 1 + distance;
      else       value /= 1 + (distance / (World.SECTOR_SIZE / 2));
      if (value > bestValue) { bestValue = value; best = t; }
    }
    
    return bestValue >= 0 ? best : null;
  }
  
  
  public static float combatValue(
    Target target, Actor actor, float harmLevel
  ) {
    if (target instanceof Actor) {
      final Actor struck = (Actor) target;
      float value = isHostileTo(actor, struck) ? 5 : 0;
      
      //  TODO:  Include other modifiers.
      
      return value;
    }
    else if (target instanceof Element) {
      return actor.relations.valueFor(target);
    }
    else return -1;
  }
  
  
  /*
  public static float combatValue(
    Target target, Actor actor, float harmLevel
  ) {
    if (! (target instanceof Element)) {
      return -1;
    }
    else if (! (target instanceof Actor)) {
      return actor.relations.relationValue(target);
    }
    else {
      final Actor other = (Actor) target;
      float value = isHostileTo(actor, other) ? 5 : 0;
      value -= actor.relations.relationValue(other) * 10 * harmLevel;
      value -= actor.traits.relativeLevel(EMPATHIC) * 5  * harmLevel;
      
      if (! other.gear.armed()) {
        value -= actor.traits.relativeLevel(ETHICAL) * 5;
      }
      
      final Behaviour doing = other.mind.rootBehaviour();
      if (doing instanceof Plan) {
        final Plan plan = (Plan) doing;
        final float relation = actor.relations.relationValue(plan.subject);
        value += plan.harmFactor() * relation;
      }
      
      //value /= 1 + powerLevelRelative(actor, other);
      return value;
    }
  }
  //*/
  
}




