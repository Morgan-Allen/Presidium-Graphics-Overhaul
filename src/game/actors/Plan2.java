

package src.game.actors;
import src.game.common.*;

import src.game.tactical.Combat;
import org.apache.commons.math3.util.FastMath;



public abstract class Plan2 {
  
  
  
  
  Actor actor;
  Target subject;
  int properties;
  
  
  //  Is a duty or mission?  Is a work shift?  Is a recreation?
  //  Is day activity?  Is night activity?  Is evening activity?
  //  Is novelty-based?  Is a competition?  
  
  
  protected abstract Trait[] keyTraits();
  protected abstract Skill[] keySkills();
  protected abstract float helpsSubject();
  
  
  
  
  public float priorityFor(Actor actor) {
    float sum = 0;
    
    //  +1 priority base (idle as default.)
    //  +1 priority per 10 points in relevant skills (averaged, not root.)
    //  +2 priority for every level in relevant traits (averaged, default 1.)
    sum += Behaviour.IDLE;
    
    final Skill KS[] = keySkills();
    for (Skill s : KS) {
      final float level = actor.traits.useLevel(s);
      sum += level / (KS.length * 10);
    }
    
    final Trait KT[] = keyTraits();
    for (Trait t : KT) {
      final float level = actor.traits.relativeLevel(t);
      sum += (-1 + ((level + 1) * 2)) / KT.length;
    }
    
    //  DISTANCE
    //  DANGER
    //  COMPETITION
    
    //  HARM TO SUBJECT
    //  SHIFTS AND DUTY
    //  SPECIAL MODIFIERS
    
    //  PAYMENT
    //  PARTY COMPOSITION
    //  LEGAL DETERENCE
    
    return sum;
  }
  
  
  
}





/*
Abilities.  Preferences (increase odds and desire.)
Distance & Danger (make offputting.)
Party & Payoff (for Missions.)
  
Okay-
  +1 priority base (idle as default.)
  +1 priority for every 10 points in relevant skills (averaged, not root.)
  +2 priority for every level in relevant traits (averaged, default 1.)
     (Opposite of trait:  -1  No trait:  +1  Possesses trait:  +3.)
  
  -1 priority for each distance step (log2 distance, by sector, over speed.)
  -5 priority for danger level on-site (relative to self, 1/2 en-route.)
  -2 per level of boredom/affection (talking/exploring/services.)
  -1 priority for every unit of homesickness ((hour x distance from home) / 5.)
  
  +/-10 priority for mortal danger to subject (soulmate / nemesis.)
  +/-2 priority for company relations (averaged, times gregariousness.)
  +5 priority for duty (part of work, or issued orders, minus rebellion.)
  
  +2 priority for each payment step (100 credits times greed.)
  +/-10 priority for maximum law-enforcement (death penalty, full arrest rate.)
  
  Special modifiers for self/environment (time of day, health needs, etc.)
  General risk/reward assessment based on skills & danger, with random element.
  

Up to 5-point range for choice priority- minus stubborn trait.
2 point threshold to change plans- plus stubborn trait.
Max. priority of 20.
  
  0- not significant
  5- routine duty
  10- mortal emergency or lifelong ambition
  15- the fate of a nation or community
  20- the fate of the universe

//*/



