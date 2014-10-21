

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;




//  TODO:  JUST CACHE THREAT RATINGS LIKE YOU DO STRENGTH RATINGS, WITHOUT
//         DISTANCE FACTORS.


public class CombatUtils {
  
  
  final static float
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
    //
    //  Get actor power as a fraction of total power, and scale by the average
    //  of actor chance and inverse of the other's chance.  Then clamp to a
    //  manageable range.
    float estimate = 1;
    estimate *= 2 * actorPower / (otherPower + actorPower);
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
  
  
  public static float hostileRating(Actor actor, Target near) {
    final boolean report = dangerVerbose && I.talkAbout == actor;
    //
    //  Only consider conscious actors as capable of hostility.  Then, by
    //  default, base the rating off intrinsic dislike of the subject.
    if (! (near instanceof Actor)) return 0;
    final Actor other = (Actor) near;
    if (! other.health.conscious()) return 0;
    float rating = 0;
    final ActorRelations mind = actor.relations;
    rating -= mind.valueFor(other);
    
    if (report) I.say("\n  "+near+" dislike rating: "+rating);
    //
    //  However, this is modified by the context of the subject's behaviour.
    //  If they are doing something harmful to another the actor cares about,
    //  (including self), then up the rating.
    final Target victim = other.focusFor(null);
    final float harmDone = other.hostilityTo(victim);
    if (victim != null && mind.likes(victim)) {
      final float likeGap = mind.valueFor(victim) - mind.valueFor(other);
      rating += likeGap * harmDone;
      
      if (report) {
        I.say("  Victim: "+victim+", value: "+mind.valueFor(victim));
        I.say("  Like gap: "+likeGap+" harm done: "+harmDone);
      }
    }
    //
    //  Limit to the range of normal plan priorities, and return...
    return Visit.clamp(rating, -1, 1) * Plan.PARAMOUNT;
  }
  
  
  public static boolean isHostileTo(Actor actor, Target near) {
    return hostileRating(actor, near) > 0;
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
    Actor actor, Target primary, boolean asThreat
  ) {
    final boolean report = threatsVerbose && I.talkAbout == actor;
    
    Target best = null;
    float bestValue = 0;
    final boolean melee = actor.gear.meleeWeapon();
    
    if (asThreat) {
      best = primary;
      bestValue = hostileRating(actor, primary) * 1.5f;
    }
    
    for (Target t : actor.senses.awareOf()) {
      final float distance = Spacing.distance(t, primary);
      if (distance > World.SECTOR_SIZE) continue;

      float value = hostileRating(actor, t);
      
      if (melee) value /= 1 + distance;
      else       value /= 1 + (distance / (World.SECTOR_SIZE / 2));
      if (value > bestValue) { bestValue = value; best = t; }
    }
    
    return bestValue >= 0 ? best : null;
  }
}




