

package stratos.game.tactical;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;



//  TODO:  Allow local danger evaluations to be cached for each actor?
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
    final boolean report = strengthVerbose && I.talkAbout == actor;
    
    //  First, obtain a general estimate of the actor's combat prowess (if
    //  possible, one cached.)
    float estimate = actor.strengthEstimate();
    if (estimate == -1) {
      estimate = (actor.gear.armourRating() + actor.gear.attackDamage()) / 20f;
      estimate *= (1 + (actor.health.maxHealth() / 10)) / 2f;
      estimate *= (1 - actor.health.injuryLevel());
      estimate *= 1 - actor.health.stressPenalty();
      //
      // Scale by general ranks in relevant skills-
      if (report) I.say("Native strength: " + estimate);
      estimate *= (2 +
        actor.traits.useLevel(HAND_TO_HAND) +
        actor.traits.useLevel(MARKSMANSHIP)
      ) / 20;
      estimate *= (2 +
        actor.traits.useLevel(HAND_TO_HAND) +
        actor.traits.useLevel(STEALTH_AND_COVER)
      ) / 20;
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
  private static float threatTo(
    Actor actor, Target m, float distance, boolean report
  ) {
    //  TODO:  Adapt to venues with defensive capability?
    
    if (m == actor || ! (m instanceof Actor)) return 0;
    final Actor other = (Actor) m;
    
    final float
      hostility = Plan.hostilityOf(other, actor),
      otherStrength = combatStrength(other, null);
    if (otherStrength <= 0) return 0;
    
    distance /= (World.SECTOR_SIZE / 2);
    float scale = 1.0f, threat = 0;
    if (distance > 1) scale /= distance;
    
    if (other.isDoing(Retreat.class, null)) scale /= 2;
    if (hostility <= 0) {
      threat = otherStrength * scale * hostility;
    }
    else {
      scale /= 2;
      threat = otherStrength * scale * hostility;
    }
    if (report) {
      final Target victim = other.focusFor(Combat.class);
      I.say("      Raw strength of: "+m+": "+otherStrength);
      I.say("      Distance: "+distance+", victim: "+victim);
      I.say("      Hostility and scale: "+hostility+" "+scale);
      I.say("      Final threat: "+threat);
    }
    return threat;
  }
  
  
  /**  Returns whatever nearby target seems to be most threatening to the given
    *  actor, partially weighted by distance from the 'primary' argument.  (If
    *  asThreat is true, primary is evaluated as a threat itself.)
    */
  public static Target mostThreatTo(
    Actor actor, Target primary, boolean asThreat
  ) {
    final boolean report = threatsVerbose && I.talkAbout == actor;
    
    Actor protects = actor;
    Target pick = primary;
    float maxThreat = 0;
    if (asThreat) maxThreat = threatTo(actor, primary, 0, report) * 1.5f;
    else if (primary instanceof Actor) protects = (Actor) primary;
    
    for (Target t : actor.senses.awareOf()) {
      final float distance = Spacing.distance(t, primary);
      if (distance > World.SECTOR_SIZE) continue;
      final float threat = threatTo(protects, t, distance, report);
      if (threat > maxThreat) { pick = t; maxThreat = threat; }
    }
    if (maxThreat <= 0) return null;
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
    if (report) I.say("  Evaluating danger at "+spot+" for "+actor);
    
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
    
    final Tile o = actor.world().tileAt(spot);
    final float ambientDanger = actor.base().dangerMap.sampleAt(o.x, o.y);
    if (ambientDanger >= 0) sumFoes += ambientDanger;
    else sumAllies -= ambientDanger;
    
    final float
      injury = actor.health.injuryLevel(),
      stress = actor.health.stressPenalty(),
      hurt = Visit.clamp(injury * stress * 2, 0, 2);
    
    if (sumFoes == 0 && hurt == 0) return 0;
    sumAllies = (sumAllies + baseStrength) / 2;
    if (sumAllies == 0) return MAX_DANGER;
    float danger = hurt + (sumFoes / (sumFoes + sumAllies));
    
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


