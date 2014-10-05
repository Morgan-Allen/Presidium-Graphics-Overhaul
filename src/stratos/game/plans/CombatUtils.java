

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
      float value = isHostileTo(actor, other) ? 0 : 5;
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
  
  
  public static boolean isHostileTo(Actor actor, Target target) {
    if (! (target instanceof Actor)) return false;
    final Actor other = (Actor) target;
    
    if (! other.health.conscious()) return false;
    if (other.focusFor(Combat.class) == actor) return true;
    if (actor.relations.relationValue(other) < 0) return true;
    //if (actor.base().relations.relationWith(other.base()) < 0) return true;
    return false;
  }
  
  
  public static boolean isAllyOf(Actor actor, Target target) {
    if (! (target instanceof Actor)) return false;
    final Actor other = (Actor) target;
    
    if (! other.health.conscious()) return false;
    if (other.focusFor(Retreat.class) != null) return false;
    if (other.relations.relationValue(actor) > 0.5f) return true;
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
}






/**  Returns the estimated combat strength of the given actor, either
  *  relative to a given enemy, or (if null) as a generalised estimate.
  */
/*
public static float combatStrength(Actor actor, Actor enemy) {
  if (actor == null) return 0;
  if (! actor.health.conscious()) return 0;
  final boolean report = strengthVerbose && I.talkAbout == actor;
  if (report) I.say("\nAssessing combat strength for: "+actor);
  //
  //  First, obtain a general estimate of the actor's combat prowess (if
  //  possible, once cached.)
  float estimate = actor.strengthEstimate();
  if (estimate == -1) {
    estimate = (actor.gear.armourRating() + actor.gear.attackDamage()) / 10f;
    estimate *= (actor.health.maxHealth() + actor.gear.shieldCharge()) / 10f;
    estimate *= 1 - actor.health.injuryLevel();
    estimate *= 1 - actor.health.stressPenalty();
    //
    // Scale by general ranks in relevant skills-
    float skillBonus = 1;
    if (report) I.say("  Native strength: " + estimate);
    skillBonus *= (5 +
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(MARKSMANSHIP)
    ) / 25;
    skillBonus *= (5 +
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(STEALTH_AND_COVER)
    ) / 25;
    estimate *= (1 + skillBonus) / 2f;
    if (! actor.gear.armed()) estimate /= 2;
    if (report) I.say("  With skills: "+estimate);
    actor.setStrengthEstimate(estimate);
  }
  
  //  But if you're considering combat against a specific adversary, scale by
  //  chance of victory-
  if (enemy != null) {
    final Skill attack, defend;
    if (actor.gear.meleeWeapon()) {
      attack = defend = HAND_TO_HAND;
    }
    else {
      attack = MARKSMANSHIP;
      defend = STEALTH_AND_COVER;
    }
    final float chance = actor.skills.chance(attack, enemy, defend, 0);
    estimate *= 2 * chance;
    if (report) I.say("  Chance to injure "+enemy+" is: "+chance);
  }
  if (report) I.say("  Final strength: "+estimate);
  return estimate;
}



/**  Returns the threat posed to the actor by the given target at a certain
  *  distance from another point of concern.  (If report is true, prints out
  *  calculation factors.)
  */
/*
public static float threatTo(
  Actor actor, Target m, float distance, boolean report
) {
  //  TODO:  Adapt to venues or vehicles with defensive capability?
  
  if (m == actor || ! (m instanceof Actor)) return 0;
  distance /= (World.SECTOR_SIZE / 2);
  if (distance > 2) return 0;
  final Actor other = (Actor) m;
  if (other.indoors()) return 0;
  
  final float
    hostility     = Plan.hostilityOf(other, actor, true),
    otherStrength = combatStrength(other, null);
  if (otherStrength <= 0) return 0;
  
  float threat = 1;
  if (distance > 0) threat /= (1f + distance);
  if (other.isDoing(Retreat.class, null)) threat /= 2;
  threat *= otherStrength * hostility;
  
  if (report) {
    final Target victim = other.focusFor(Combat.class);
    I.say("\n    Raw strength of: "+m+": "+otherStrength);
    I.say("      Distance: "+distance+", victim: "+victim);
    I.say("      Hostility: "+hostility);
    I.say("      Final threat: "+threat);
  }
  return threat;
}


/**  Returns whatever nearby target seems to be most threatening to the given
  *  actor, partially weighted by distance from the 'primary' argument.  (If
  *  asThreat is true, primary is evaluated as a threat itself.)
  */
/*
public static Target bestTarget(
  Actor actor, Target primary, boolean asThreat
) {
  final boolean report = threatsVerbose && I.talkAbout == actor;
  
  final boolean melee = actor.gear.meleeWeapon();
  Actor protects = actor;
  Target pick = primary;
  float maxRating = 0;
  if (asThreat) maxRating = threatTo(actor, primary, 0, report) + 0.5f;
  else if (primary instanceof Actor) protects = (Actor) primary;
  
  for (Target t : actor.senses.awareOf()) {
    final float distance = Spacing.distance(t, primary);
    if (distance > World.SECTOR_SIZE) continue;
    
    float rating = Plan.hostilityOf(t, actor, false);
    if (rating < 0) continue;
    
    if (melee) rating /= 1 + distance;
    else rating -= distance / (World.SECTOR_SIZE / 2);

    final float threat = threatTo(protects, t, distance, report);
    if (protects == actor) rating -= threat;
    else rating += threat;
    
    if (rating > maxRating) { pick = t; maxRating = rating; }
  }
  if (maxRating <= 0) return null;
  
  if (report) I.say("  BEST TARGET: "+pick);
  return pick;
}



/**  Returns the estimated aggregate danger to a given actor at the specified
  *  spot, with particular emphasis on an (optional) intended enemy.
  */
/*
public static float dangerAtSpot(
  Target spot, Actor actor, Actor enemy
) {
  final boolean report = dangerVerbose && I.talkAbout == actor;
  if (spot == null) return 0;
  if (report) I.say("\n  Evaluating danger at "+spot+" for "+actor);
  
  final float range = actor.health.sightRange();
  final World world = actor.world();
  Series <Target> seen;
  
  //  First, get the set of elements to sample from to determine danger
  //  levels at a given point.
  if (Spacing.distance(actor, spot) < range * 2) {
    seen = actor.senses.awareOf();
  }
  else {
    //  TODO:  Sample actors near that point directly?
    final Tile t = world.tileAt(spot);
    float danger = actor.base().dangerMap.sampleAt(t.x, t.y);
    danger /= combatStrength(actor, null);
    return danger;
  }
  
  final float baseStrength = combatStrength(actor, enemy);
  float sumAllies = baseStrength, sumFoes = combatStrength(enemy, actor);
  
  for (Target m : seen) if (m != enemy) {
    final float distance = Spacing.distance(actor, m);
    final float threat = threatTo(actor, m, distance, report);
    if (threat > 0) sumFoes += threat;
    else sumAllies += -threat;
  }
  if (sumFoes == 0) return 0;
  
  final float
    injury = actor.health.injuryLevel(),
    stress = actor.health.stressPenalty(),
    hurt = Visit.clamp((injury + stress) * 2, 0, 2);
  
  final Tile o = actor.world().tileAt(spot);
  final float ambientDanger = actor.base().dangerMap.sampleAt(o.x, o.y);
  if (ambientDanger >= 0) sumFoes += ambientDanger;
  else sumAllies -= ambientDanger;
  
  sumAllies = (sumAllies + baseStrength) / 2;
  if (sumAllies == 0) return MAX_DANGER;
  float danger = hurt + (sumFoes * 2 / (sumFoes + sumAllies));
  
  if (report) {
    I.say("    Sum allied/enemy strength: "+sumAllies+" / "+sumFoes);
    I.say("    Ambient danger: "+ambientDanger);
    I.say("    Injury & stress: "+injury+" / "+stress);
    I.say("    Intrinsic danger: "+danger);
  }
  danger *= (2 + actor.traits.relativeLevel(NERVOUS)) / 2;
  danger = Visit.clamp(danger, 0, MAX_DANGER);
  if (report) I.say("    Perceived danger: "+danger);
  return danger;
}
//*/



/*
public static float hostilityOf(
  Target threat, Actor actor, boolean general
) {
  //  TODO:  Generalise this to activities beside combat, based on the
  //  help/harm ratings supplied in plans' priority calculations!
  if (threat instanceof Actor) {
    final Actor other = (Actor) threat;
    final Target victim = other.focusFor(Combat.class);
    if (victim == actor) return 1;
    float hostility = 0;
    if (victim != null) hostility += actor.relations.relationValue(victim);
    hostility -= actor.relations.relationValue(other);
    return hostility;
  }
  
  if (threat instanceof Venue) {
    final Venue venue = (Venue) threat;
  }
  
  //  TODO:  Implement an assessment for venues as well?
  return 0;
}
//*/