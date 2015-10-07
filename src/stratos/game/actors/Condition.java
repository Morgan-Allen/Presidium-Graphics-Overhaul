/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;



public class Condition extends Trait {
  
  
  private static boolean
    verbose       = false,
    effectVerbose = false;
  
  final public float latency, virulence, spread;
  final public Trait affected[];
  final public int modifiers[];
  
  
  public Condition(
    Class baseClass, String keyID, boolean basic, String... names
  ) {
    this(baseClass, keyID, null, null, 0, 0, 0, new Table(), names);
  }
  
  
  public Condition(
    Class baseClass, String keyID, Table effects, String... names
  ) {
    this(baseClass, keyID, null, null, 0, 0, 0, effects, names);
  }
  
  
  public Condition(
    Class baseClass, String keyID, String description, String iconPath,
    float latency, float virulence, float spread,
    Table effects,
    String... names
  ) {
    super(baseClass, keyID, description, iconPath, Qualities.CONDITION, names);
    this.latency   = Nums.max(latency, 0.1f);
    this.virulence = virulence;
    this.spread    = spread   ;
    
    this.affected  = new Trait[effects.size()];
    this.modifiers = new int  [effects.size()];
    int i = 0; for (Object k : effects.keySet()) {
      modifiers[i] = (Integer) effects.get(k);
      affected[i++] = (Trait) k;
    }
  }
  

  /*
  private static float cleanFactor(Actor actor) {
    float factor = 1;
    final Target at = actor.aboard();
    //  TODO:  CONSIDER RESTORING.
    if (at instanceof stratos.game.base.Sickbay) {
      factor /= 2;
    }
    if (actor.gear.outfitType() == stratos.game.building.Economy.SEALSUIT) {
      factor /= 5;
    }
    return factor;
  }
    //*/
  
  
  //
  //  Assuming a lifespan of 50 years of 60 days each, then a 2/10000 chance
  //  for cancer gives a 3000/5000 ~= 60% chance of getting cancer some day,
  //  assuming no immune response.  However, an adult with average vigour of 10
  //  has a 75% chance to fight it off, even without acquired resistance.
  
  public static void checkContagion(Actor actor) {
    final Tile o = actor.origin();
    final float squalor = actor.world().ecology().ambience.valueAt(o) / -10;
    
    for (Object d : Conditions.SPONTANEOUS_DISEASE) {
      final Condition c = (Condition) d;
      //
      //  Let's say that under average squalor, you have a 10% chance of
      //  contracting an illness per day.  (Before immune function kicks in.)
      float infectChance = 0.1f;// * cleanFactor(actor);
      //
      //  Let's say that perfect hygiene reduces the chance by a factor of 2,
      //  and perfect squalor multiplies by a factor of 5.
      if (squalor > 0) infectChance *= 1 + (5 * squalor);
      else infectChance /= 1 - (2 * squalor);
      //
      //  Finally, let's say that each 5 points in virulence reduces the chance
      //  of contraction by half.  And that the chance is multiplied by spread.
      infectChance /= 1 << (int) ((c.virulence / 5) - 1);
      infectChance *= (c.spread + 0.1f) / 10.2f;
      
      ///I.sayAbout(actor, "Contract chance/day for "+c+" is: "+infectChance);
      if (Rand.num() > (infectChance / Stage.STANDARD_DAY_LENGTH)) continue;
      if (actor.skills.test(IMMUNE, c.virulence - 10, 1.0f, null)) continue;
      
      if (verbose) I.say("INFECTING "+actor+" WITH "+c);
      actor.traits.incLevel(c, 0.1f);
    }
  }
  
  
  protected float transmitChance(Actor has, Actor near) {
    //
    //  
    if (! near.health.organic()) return 0;
    if (has.species() != near.species()) return 0;
    if (near.traits.traitLevel(this) != 0) return 0;
    if (near.skills.test(IMMUNE, virulence / 2, 0.1f, null)) return 0;
    //
    //  TODO:  THERE HAS GOT TO BE A MORE ELEGANT WAY TO EXPRESS THIS
    float chance = 0.1f;
    /*
    //
    //  Increase the risk substantially if you're fighting or humping the
    //  subject-
    float chance = 0.1f;
    if (has.isDoing(Combat.class, near)) chance *= 2;
    final Performance PR = new Performance(
      has, has.aboard(), Recreation.TYPE_EROTICS, near
    );
    if (has.isDoing(PR)) chance *= 2;
    //*/
    return chance;
  }
  
  
  protected void affectAsDisease(Actor a, float progress, float response) {
    final boolean report = effectVerbose && I.talkAbout == a;
    //
    //  If this is contagious, consider spreading to nearby actors.
    if (spread > 0 && Rand.index(10) < spread) {
      for (Object o : a.world().presences.matchesNear(Mobile.class, a, 2)) {
        if (! (o instanceof Actor)) continue;
        final Actor near = (Actor) o;
        final float chance = transmitChance(a, near);
        if (chance <= 0 || Rand.num() > chance) continue;
        near.traits.incLevel(this, 0.1f);
      }
    }
    //
    //  Next, consider effects upon the host-
    final float
      noticeDC = 5 * (3 - (progress + response)),
      ageBonus = 1.5f - a.health.ageLevel(),
      immuneDC = (virulence + noticeDC) / (1 + ageBonus);
    final float
      inc = 1 * 1f / (latency * Stage.STANDARD_DAY_LENGTH);
    //
    //  If you've acquired an immunity, have it fade over time-
    if (progress <= 0) {
      a.traits.setLevel(this, Nums.clamp(progress + (inc / 5), -1, 0));
    }
    //
    //  Otherwise, see if your immune system can respond, based on how much of
    //  an immune response is already marshalled, and how advanced the disease
    //  is-
    else if (a.skills.test(IMMUNE, immuneDC, inc, null)) {
      a.traits.incBonus(this, 0 - (inc * 2));
      a.traits.incLevel(this, 0 - inc);
      if (a.traits.usedLevel(this) < 0) {
        final float immunity = virulence / -10f;
        a.traits.setLevel(this, immunity);
        a.traits.setBonus(this, 0);
      }
    }
    //
    //  If that fails, advance the disease-
    else {
      a.traits.setBonus(this, Nums.clamp((inc / 2) - response, -3, 0));
      a.traits.setLevel(this, Nums.clamp(progress + inc, 0, 3));
    }
    if (report) {
      I.say("\nReporting on progress of "+this+" for "+a);
      final float
        chance     = a.skills.chance(IMMUNE, immuneDC),
        usedImmune = a.traits.usedLevel (IMMUNE),
        trueImmune = a.traits.traitLevel(IMMUNE);
      I.say("  Immune DC          "+immuneDC+" (chance "+chance+")");
      I.say("  Immune strength:   "+usedImmune+"/"+trueImmune);
      I.say("  Progress/response: "+progress+"/"+response);
    }
  }
  
  
  public void affect(Actor a) {
    final boolean report = effectVerbose && I.talkAbout == a;
    //
    //  Impose penalties/bonuses to various attributes, if still symptomatic-
    final float
      progress =     a.traits.traitLevel (this),
      response = 0 - a.traits.effectBonus(this),
      symptoms = progress - response;
    
    if (report) {
      I.say("\n"+this+" has symptoms: "+symptoms);
    }
    //  ...Shoot.  It really does seem to be potentially fatal.
    
    if (symptoms > 0) for (int i = affected.length; i-- > 0;) {
      final float impact = modifiers[i] * symptoms / 2;
      if (report) {
        final float normal = a.traits.traitLevel(affected[i]);
        I.say("  Affecting:  "+affected[i]+" ("+impact+"/"+normal+")");
      }
      a.traits.incBonus(affected[i], impact);
    }
    //
    //  Check to see if the condition spreads/worsens or fades-
    if (virulence > 0) {
      affectAsDisease(a, progress, response);
    }
    else {
      final float inc = 1 * 1f / (latency * Stage.STANDARD_DAY_LENGTH);
      a.traits.setLevel(this, Nums.max(0, progress - inc));
    }
  }
}




