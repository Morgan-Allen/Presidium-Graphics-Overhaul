

package stratos.game.tactical;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;



//  TODO:  JUST CACHE THREAT RATINGS LIKE YOU DO STRENGTH RATINGS, WITHOUT
//         DISTANCE FACTOR

public class CombatUtils implements Qualities {
  
  
  final static float
    MAX_DANGER  = 2.0f;
  private static boolean
    threatsVerbose  = false,
    strengthVerbose = false,
    dangerVerbose   = false;
  
  
  /**  Returns the estimated combat strength of the given actor, either
    *  relative to a given enemy, or (if null) as a generalised estimate.
    */
  public static float combatStrength(Actor actor, Actor enemy) {
    if (actor == null) return 0;
    if (! actor.health.conscious()) return 0;
    final boolean report = strengthVerbose && I.talkAbout == actor;
    
    //  First, obtain a general estimate of the actor's combat prowess (if
    //  possible, one cached.)
    float estimate = actor.strengthEstimate();
    if (estimate == -1) {
      estimate = (actor.gear.armourRating() + actor.gear.attackDamage()) / 20f;
      estimate *= actor.health.maxHealth() / 10f;
      estimate *= (1 - actor.health.injuryLevel());
      estimate *= 1 - actor.health.stressPenalty();
      //
      // Scale by general ranks in relevant skills-
      float skillBonus = 1;
      if (report) I.say("Native strength: " + estimate);
      skillBonus *= (2 +
        actor.traits.useLevel(HAND_TO_HAND) +
        actor.traits.useLevel(MARKSMANSHIP)
      ) / 20;
      skillBonus *= (2 +
        actor.traits.useLevel(HAND_TO_HAND) +
        actor.traits.useLevel(STEALTH_AND_COVER)
      ) / 20;
      estimate *= (1 + skillBonus) / 2f;
      if (! actor.gear.armed()) estimate /= 2;
      if (report) I.say("With skills: "+estimate);
      actor.setStrengthEstimate(estimate);
    }
    
    //  But if you're considering combat against a specific adversary, scale by
    //  chance of victory-
    if (enemy != null) {
      final Skill attack, defend ;
      if (actor.gear.meleeWeapon()) {
        attack = defend = HAND_TO_HAND ;
      }
      else {
        attack = MARKSMANSHIP ;
        defend = STEALTH_AND_COVER ;
      }
      final float chance = actor.traits.chance(attack, enemy, defend, 0) ;
      if (report) {
        I.say("Chance to injure "+enemy+" is: "+chance) ;
        I.say("Native strength: "+estimate) ;
      }
      estimate *= 2 * chance ;
    }
    return estimate ;
  }
  
  
  
  /**  Returns the threat posed to the actor by the given target at a certain
    *  distance from another point of concern.  (If report is true, prints out
    *  calculation factors.)
    */
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
  public static float dangerAtSpot(
    Target spot, Actor actor, Actor enemy
  ) {
    final boolean report = dangerVerbose && I.talkAbout == actor;
    if (spot == null) return 0 ;
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
      //  TODO:  Samples actors near that point directly?
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
}


