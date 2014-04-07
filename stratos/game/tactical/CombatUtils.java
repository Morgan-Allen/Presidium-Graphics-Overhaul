

package stratos.game.tactical ;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.*;



//  TODO:  Allow local danger and combat-strength evaluations to be cached for
//  each actor.
public class CombatUtils implements Qualities {
  
  
  final static float
    MAX_DANGER  = 2.0f;
  private static boolean
    strengthVerbose = false,
    dangerVerbose   = true;
  
  
  //  Note:  it's acceptable to pass null as the enemy argument, for a general
  //  estimate of combat prowess.  (TODO:  Put in a separate method for that?)
  
  public static float combatStrength(Actor actor, Actor enemy) {
    final boolean report = strengthVerbose && I.talkAbout == actor;
    
    float strength = 0 ;
    strength += (actor.gear.armourRating() + actor.gear.attackDamage()) / 20f ;
    strength *= (1 + (actor.health.maxHealth() / 10)) / 2f ;
    strength *= (1 - actor.health.injuryLevel()) ;
    strength *= 1 - actor.health.stressPenalty() ;
    //
    //  Scale by general ranks in relevant skills-
    if (report) I.say("Native strength: "+strength) ;
    strength *= (2 +
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(MARKSMANSHIP)
    ) / 20 ;
    strength *= (2 +
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(STEALTH_AND_COVER)
    ) / 20 ;
    if (report) I.say("With skills: "+strength) ;
    //
    //  And if you're measuring combat against a specific adversary, scale by
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
        I.say("Native strength: "+strength) ;
      }
      strength *= 2 * chance ;
    }
    return strength ;
  }
  
  
  
  public static float dangerAtSpot(
    Target spot, Actor actor, Actor enemy
  ) {
    final boolean report = dangerVerbose && I.talkAbout == actor;
    if (spot == null) return 0 ;
    if (report) I.say("\nEvaluating danger at "+spot+" for "+actor);
    
    final float range = actor.health.sightRange();
    final World world = actor.world();
    Series <Target> seen;
    
    //  First, get the set of elements to sample from to determine danger
    //  levels at a given point.
    if (Spacing.distance(actor, spot) < range * 2) {
      seen = actor.senses.awareOf();
    }
    else {
      final Tile t = world.tileAt(spot);
      float danger = actor.base().dangerMap.sampleAt(t.x, t.y);
      danger /= combatStrength(actor, null);
      return danger;
    }
    
    final float baseStrength = combatStrength(actor, enemy);
    float sumAllies = baseStrength, sumFoes = 0;//, sumTargeting = 0;
    
    for (Target m : seen) {
      if (m == actor || ! (m instanceof Actor)) continue;
      final Actor other = (Actor) m;

      final float
        relation = other.mind.relationValue(actor),
        otherStrength = combatStrength(other, null),
        distance = Spacing.distance(actor, other) / World.SECTOR_SIZE;
      if (otherStrength <= 0) continue;
      final Target victim = other.focusFor(Combat.class);
      float scale = 1.0f;
      
      if (distance > 1) scale /= distance;
      if (other.isDoing(Retreat.class, null)) scale /= 2;
      
      if (relation >= 0) {
        sumAllies += otherStrength * scale * relation;
      }
      else {
        if (victim != actor) scale *= relation / -2;
        //if (! other.isDoing(Combat.class, actor)) scale *= 0 - relation;
        //if (! other.senses.awareOf(actor)) scale /= 2;
        sumFoes += otherStrength * scale;
      }
      
      if (report) {
        I.say("    Raw strength of: "+m+": "+otherStrength);
        I.say("    Distance: "+distance+", victim: "+victim);
        //I.say("   Root behaviour: "+other.mind.rootBehaviour());
        I.say("    Relation and scale: "+relation+" "+scale);
      }
    }
    

    final Tile o = actor.world().tileAt(spot);
    final float ambientDanger = actor.base().dangerMap.sampleAt(o.x, o.y);
    if (ambientDanger >= 0) sumFoes += ambientDanger;
    else sumAllies -= ambientDanger;
    
    final float
      injury = actor.health.injuryLevel(),
      stress = actor.health.stressPenalty(),
      hurt = Visit.clamp((injury + stress) / 2, 0, 1);
    
    if (sumFoes == 0 && hurt == 0) return 0;
    sumAllies = (sumAllies + baseStrength) / 2;
    if (sumAllies == 0) return MAX_DANGER;
    float danger = sumFoes / (sumFoes + sumAllies);
    danger = (danger + hurt) * (1 + hurt);

    if (report) {
      I.say("  Sum allied/enemy strength: "+sumAllies+" / "+sumFoes);
      I.say("  Ambient danger: "+ambientDanger);
      I.say("  Injury & stress: "+injury+" / "+stress);
      I.say("  Final danger rating: "+danger);
    }
    return Visit.clamp(danger, 0, MAX_DANGER);
  }
}




