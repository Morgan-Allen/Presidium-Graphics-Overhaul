

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;




//  TODO:  JUST CACHE THREAT RATINGS LIKE YOU DO STRENGTH RATINGS, WITHOUT
//         DISTANCE FACTORS?


public class CombatUtils {
  
  
  final public static float
    MAX_POWER   = 30,
    AVG_POWER   = 10,
    MIN_POWER   =  0,
    AVG_DAMAGE  = 10;
  private static boolean
    threatsVerbose  = false,
    powerVerbose    = false,
    dangerVerbose   = false;
  
  
  public static float powerLevel(Actor actor) {
    final boolean report = powerVerbose && I.talkAbout == actor;
    if (! actor.health.alive()) return 0;
    
    float estimate = 1;
    estimate *= actor.gear.totalArmour() + actor.gear.totalDamage ();
    estimate *= actor.health.maxHealth() + actor.gear.shieldCharge();
    estimate *= AVG_POWER / (4f * AVG_DAMAGE * AVG_DAMAGE);
    estimate *= (2 - actor.health.injuryLevel  ()) / 2f;
    estimate *= (2 - actor.health.stressPenalty()) / 2f;
    if (! actor.health.conscious()) estimate /= 2.5f;
    
    float sumPowers = 0;
    for (Technique t : actor.skills.availableTechniques()) {
      sumPowers += t.powerLevel;
    }
    estimate *= 1 + (sumPowers / Technique.UNIT_POWER_BASELINE);
    
    if (report) {
      I.say("\nESTIMATED POWER LEVEL OF "+actor+" IS "+estimate);
    }
    return estimate;
  }
  
  
  public static float powerLevelRelative(Actor actor, Actor other) {
    //
    //  TODO:  Also consider effects of psy abilities, techniques, et cetera.
    
    final boolean report = powerVerbose && (
      I.talkAbout == actor || I.talkAbout == other
    );
    
    final float
      otherPower = other.senses.powerLevel(),
      actorPower = actor.senses.powerLevel();
    if (otherPower <= 0) return MAX_POWER;
    
    //  TODO:  Get separate attack/defend skills for the other actor, depending
    //  on what type of weapon they have.
    final Skill attack, defend;
    if (actor.gear.meleeDeviceOnly()) {
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
  
  
  public static Base baseAttacked(Actor actor) {
    final Behaviour current = actor.mind.rootBehaviour();
    if (! (current instanceof Plan)) return null;
    final Plan plan = (Plan) current;
    if ((plan instanceof Arrest) || plan.harmFactor() <= 0) return null;
    else return plan.subject().base();
  }
  
  
  public static boolean isAllyOf(Actor actor, Target target) {
    if (! (target instanceof Actor)) return false;
    final Actor other = (Actor) target;
    if (other.relations.valueFor(actor) > 0) return true;
    return other.base() == actor.base();
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
  

  /**  Returns whatever nearby target seems to be most threatening to the given
    *  actor, partially weighted by distance from the 'primary' argument.  (If
    *  asThreat is true, primary is evaluated as a threat itself.)
    */
  public static Target bestTarget(
    final Actor actor, final Target primary, boolean asThreat
  ) {
    final boolean report = Combat.stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting best target for "+actor+" around "+primary);
      I.say("  Treating as threat? "+asThreat);
    }
    
    final boolean melee = actor.gear.meleeDeviceOnly();
    final float harm = Plan.REAL_HARM;
    
    final Pick <Target> pick = new Pick <Target> (0) {
      public void compare(Target t, float minRating) {
        if (PlanUtils.harmIntendedBy(t, actor, true) <= 0) return;
        
        final float distance = Spacing.distance(t, actor);
        float value = PlanUtils.combatPriority(actor, t, 0, 1, false, harm);
        if (value <= 0) return;
        if (melee) value /= 1 + distance;
        else       value /= 1 + (distance / (Stage.ZONE_SIZE / 2));
        
        if (report) {
          I.say("    Value for "+t+" is "+value);
          I.say("    Distance: "+distance);
        }
        super.compare(t, Nums.max(value, 0) + minRating);
      }
    };
    
    if (asThreat) pick.compare(primary, 1);
    for (Target t : actor.senses.awareOf()) pick.compare(t, 0);
    
    if (report) I.say("  Final pick: "+pick.result());
    return pick.result();
  }
}

