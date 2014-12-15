

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;




//  TODO:  JUST CACHE THREAT RATINGS LIKE YOU DO STRENGTH RATINGS, WITHOUT
//         DISTANCE FACTORS?


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
    estimate = Nums.clamp(estimate, 0, MAX_POWER);
    
    if (report) {
      I.say("\nGETTING POWER OF "+actor+" (Actor)");
      I.say("RELATIVE TO "+other+" (Other)");
      
      I.say("  Actor power: "+actorPower+" chance: "+actorChance);
      I.say("  Actor attack skill: "+actor.traits.usedLevel(attack));
      I.say("  Actor defend skill: "+actor.traits.usedLevel(defend));

      I.say("  Other power: "+otherPower+" chance: "+otherChance);
      I.say("  Other attack skill: "+other.traits.usedLevel(attack));
      I.say("  Other defend skill: "+other.traits.usedLevel(defend));
      
      I.say("  Estimated relative power level: "+estimate);
    }
    return estimate;
  }
  
  
  public static float hostileRating(Actor actor, Target near) {
    final boolean report = dangerVerbose && I.talkAbout == actor;
    
    //  Only consider conscious actors as capable of hostility.  Then, by
    //  default, base the rating off intrinsic dislike of the subject.
    if (! (near instanceof Actor)) return 0;
    final Actor other = (Actor) near;
    if (! other.health.alive()) return 0;
    
    final ActorRelations relations = actor.relations;
    float rating = 0 - relations.valueFor(other);
    if (report) I.say("\n  "+near+" dislike rating: "+rating);
    
    //  However, this is modified by the context of the subject's behaviour.
    //  If they are doing something harmful to another the actor cares about,
    //  (including self), then up the rating.
    final Target victim = other.planFocus(null);
    final float
      harmDone    = other.harmIntended(victim),
      protectUrge = harmDone * relations.valueFor(victim);
    rating += protectUrge;
    
    if (report) {
      I.say("  Victim: "+victim+", value: "+relations.valueFor(victim));
      I.say("  Protect urge: "+protectUrge);
    }
    
    //  Include a penalty if the subject is unarmed, based on ethics.
    //  TODO:  This might belong in the priority-method for Combat, rather than
    //  here?
    if (! isArmed(other)) {
      rating /= 1 + (actor.traits.relativeLevel(ETHICAL) * 2);
    }
    
    //  Limit to the range of +/-1, and return.
    return Nums.clamp(rating, -1, 1);
  }
  
  
  public static Base baseAttacked(Actor actor) {
    //  TODO:  Consider having the actor's agenda contain *only* plans:  
    //  Actions and Missions can be handled separately.
    final Behaviour current = actor.mind.rootBehaviour();
    if (! (current instanceof Plan)) return null;
    final Plan plan = (Plan) current;
    if (plan.harmFactor() <= 0) return null;
    else return plan.subject().base();
  }
  
  
  public static float homeDefenceBonus(Actor actor, Target from) {
    final Target haven = actor.mind.home();
    if (haven == null) return 0;// Plan.PARAMOUNT;
    else {
      float homeDist = Spacing.distance(from, haven);
      homeDist = Nums.clamp(homeDist / Stage.SECTOR_SIZE, 0, 1);
      return Plan.PARAMOUNT * (1 - homeDist);
    }
  }
  
  
  public static boolean isActiveHostile(Actor actor, Target near) {
    if (near instanceof Venue) {
      return false;
    }
    else if (near instanceof Actor) {
      final Actor other = (Actor) near;
      final Target victim = other.planFocus(null);
      final float harmDone = other.harmIntended(victim);
      return victim != null && actor.relations.likes(victim) && harmDone > 0;
    }
    else return false;
  }
  
  
  public static boolean isAllyOf(Actor actor, Target target) {
    if (! (target instanceof Actor)) return false;
    final Actor other = (Actor) target;
    if (other.relations.valueFor(actor) > 0) return true;
    return other.base() == actor.base();
  }
  
  
  public static boolean isArmed(Actor actor) {
    final DeviceType type = actor.gear.deviceType();
    final float baseDamage = actor.gear.baseDamage();
    return baseDamage > 0 || ((type != null) && type.baseDamage > 0);
  }
  
  
  public static boolean isDowned(Target subject, int object) {
    if (subject instanceof Actor) {
      final Actor struck = (Actor) subject;
      if (object == Combat.OBJECT_SUBDUE ) return ! struck.health.conscious();
      if (object == Combat.OBJECT_DESTROY) return ! struck.health.alive();
      return (! struck.health.conscious()) && (! struck.health.asleep());
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
  
  
  public static float successChance(Actor actor, Target other) {
    //  TODO:  Switch between these two evaluation methods based on
    //  intelligence?  (Or maybe the battle-tactics skill?)
    
    final float danger = Nums.max(
      actor.senses.fearLevel(),
      Plan.dangerPenalty(other, actor)
    );
    return Nums.clamp(1 - danger, 0, 1);
    /*
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (subject instanceof Actor) {
      final Actor struck = (Actor) subject;
      float chance = CombatUtils.powerLevelRelative(actor, struck) / 2f;
      chance = (chance + 1 - actor.senses.fearLevel()) / 2f;
      return Visit.clamp(chance, 0, 1);
    }
    else return 1;
    //*/
  }
  
  

  /**  Returns whatever nearby target seems to be most threatening to the given
    *  actor, partially weighted by distance from the 'primary' argument.  (If
    *  asThreat is true, primary is evaluated as a threat itself.)
    */
  public static Target bestTarget(
    Actor actor, Target primary, boolean asThreat
  ) {
    final boolean report = Combat.stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting best target for "+actor+" around "+primary);
      I.say("  Treating as threat? "+asThreat);
    }
    
    final boolean melee = actor.gear.meleeWeapon();
    Target best = asThreat ? primary : null;
    float bestValue = asThreat ? (hostileRating(actor, primary) * 1.5f) : 0;
    
    for (Target t : actor.senses.awareOf()) {
      final float distance = Spacing.distance(t, actor);
      if (distance > Stage.SECTOR_SIZE) continue;
      
      float value = hostileRating(actor, t);
      if (value <= 0) continue;
      if (melee) value /= 1 + distance;
      else       value /= 1 + (distance / (Stage.SECTOR_SIZE / 2));
      
      if (report) {
        I.say("    Value for "+t+" is "+value);
        I.say("    Distance: "+distance);
      }
      if (value > bestValue) { bestValue = value; best = t; }
    }
    
    if (report) I.say("  Final pick: "+best+", rating: "+bestValue);
    if (bestValue >= 0) return best;
    return asThreat ? primary : null;
  }
}




