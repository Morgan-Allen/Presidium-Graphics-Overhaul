/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



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
  

  
  public static boolean checkContagion(Actor actor) {
    //
    //  Let's say that under average squalor, you have a 10% chance of
    //  contracting an illness per day.  (Before immune function kicks in.)
    final Tile o = actor.origin();
    final float squalor = actor.world().ecology().ambience.valueAt(o) / -10;
    float infectChance = 0.1f;
    //
    //  Let's say that perfect hygiene reduces the chance by a factor of 2,
    //  and perfect squalor multiplies by a factor of 5.
    if (squalor > 0) infectChance *= 1 + (5 * squalor);
    else infectChance /= 1 - (2 * squalor);
    return checkContagion(
      actor, infectChance, Stage.STANDARD_DAY_LENGTH, SPONTANEOUS_DISEASE
    );
  }
  
  
  public static boolean checkContagion(
    Actor actor, float infectChance, int period, Condition... diseases
  ) {
    boolean infected = false;
    for (Condition c : diseases) {
      //
      //  Finally, let's say that each 5 points in virulence reduces the chance
      //  of contraction by half.  And that the chance is multiplied by spread.
      infectChance /= 1 << (int) ((c.virulence / 5) - 1);
      infectChance *= (c.spread + 0.1f) / 10.2f;
      
      if (Rand.num() > (infectChance / period)) continue;
      if (actor.skills.test(IMMUNE, c.virulence - 10, 1.0f, null)) continue;
      
      if (verbose) I.say("INFECTING "+actor+" WITH "+c);
      actor.traits.incLevel(c, 1f / period);
      infected = true;
    }
    return infected;
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
  
  
  
  /**  Listing of standard properties and the more common diseases-
    */
  final static Class BC = Condition.class;
  
  final public static int
    
    //  Number of days required for full development.
    NO_LATENCY     = 0,
    SHORT_LATENCY  = 1,
    MEDIUM_LATENCY = 10,
    LONG_LATENCY   = 100,
    
    //  Average number of other people infected.
    NO_SPREAD    = 0,
    SLOW_SPREAD  = 2,
    RAPID_SPREAD = 5,
    
    //  Intrinsic difficulty to treat or recover from.
    NO_VIRULENCE        = 0,
    MINIMAL_VIRULENCE   = 5,
    LOW_VIRULENCE       = 10,
    AVERAGE_VIRULENCE   = 15,
    HIGH_VIRULENCE      = 20,
    EXTREME_VIRULENCE   = 25;
  
  final public static Condition
    //
    //  Finally, listings for various conditions that might beset the actor-
    INJURY = new Condition(
      BC, "Injury", true,
      "Critical Injury",
      "Serious Injury",
      "Modest Injury",
      "Slight Injury",
      null
    ),
    FATIGUE = new Condition(
      BC, "Fatigue", true,
      "Extreme Fatigue",
      "Heavy Fatigue",
      "Modest Fatigue",
      "Mild Fatigue",
      null
    ),
    POOR_MORALE = new Condition(
      BC, "Morale", true,
      "Broken Morale",
      "Poor Morale",
      null,
      "Fair Morale",
      "Superb Morale"
    ),
    
    HUNGER = new Condition(
      BC, "Hunger", true,
      "Near Starvation",
      "Hungry",
      "Peckish",
      null,
      "Full"
    ),
    MALNOURISHMENT = new Condition(
      BC, "Malnourishment", true,
      "Malnourished",
      null
    ),
    POISONING = new Condition(
      BC, "Poisoning", null, null,
      SHORT_LATENCY, LOW_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -15, MUSCULAR, -15
      ),
      "Severe Poisoning",
      "Bad Poisoning",
      "Mild Poisoning",
      null,
      "Poison Immune"
    ),
    
    SOMA_HAZE = new Condition(
      BC, "Soma Haze", null, null,
      NO_LATENCY, MINIMAL_VIRULENCE, NO_SPREAD, Table.make(
        MOTOR, -3, COGNITION, -2, PERCEPT, 1
      ),
      "Soma Haze",
      "Soma Haze",
      "Soma Haze",
      null,
      "Haze Immune"
    ),
    ILLNESS = new Condition(
      BC, "Illness", null, null,
      SHORT_LATENCY, LOW_VIRULENCE, RAPID_SPREAD, Table.make(
        IMMUNE, -5, MUSCULAR, -5
      ),
      "Debilitating Illness",
      "Serious Illness",
      "Mild Illness",
       null,
       "Illness Immune"
    ),
    SPICE_ADDICTION = new Condition(
      BC, "Spice Addiction", null, null,
      LONG_LATENCY, AVERAGE_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -10, PERCEPT, -5, NERVE, -5, COGNITION, -5
      ),
      "Complete Spice Addiction",
      "Heavy Spice Addiction",
      "Mild Spice Addiction",
      null,
      "Addiction Immune"
    ),
    CANCER = new Condition(
      BC, "Cancer", null, null,
      LONG_LATENCY, AVERAGE_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -20, MUSCULAR, -10
      ),
      "Terminal Cancer",
      "Advanced Cancer",
      "Early Cancer",
      null,
      "Cancer Immune"
    ),
    RAGE_INFECTION = new Condition(
      BC, "Rage Infection", null, null,
      SHORT_LATENCY, HIGH_VIRULENCE, RAPID_SPREAD, Table.make(
        IMMUNE, 5, MUSCULAR, 5, DEFENSIVE, 5, COGNITION, -15
      ),
      "Rage Frenzy",
      "Rage Fever",
      "Rage Onset",
      null,
      "Rage Immune"
    ),
    HIREX_PARASITE = new Condition(
      BC, "Hirex Parasite", null, null,
      MEDIUM_LATENCY, HIGH_VIRULENCE, SLOW_SPREAD, Table.make(
        COGNITION, -5, MOTOR, -5, PERCEPT, -5, MUSCULAR, -5, HANDSOME, -5
      ),
      "Hirex Consumption",
      "Hirex Infestation",
      "Hirex Gestation",
      null,
      "Hirex Immune"
    ),
    ALBEDAN_STRAIN = new Condition(
      BC, "Albedan Strain", null, null,
      MEDIUM_LATENCY, EXTREME_VIRULENCE, SLOW_SPREAD, Table.make(
        INDULGENT, 2, IMMUNE, 5, PERCEPT, 5, MOTOR, -5
      ),
      "Albedan Strain",
      "Albedan Strain",
      "Albedan Strain",
      null,
      "Strain Immune"
    ),
    SILVERQUICK = new Condition(
      BC, "Silverquick", null, null,
      SHORT_LATENCY, EXTREME_VIRULENCE, RAPID_SPREAD, Table.make(
        IMPASSIVE, 5, IMMUNE, -20, MUSCULAR, -20
      ),
      "Silverquick Rictus",
      "Silverquick Scale",
      "Silverquick Taint",
      null,
      "Silverquick Immune"
    ),
    MOEBIUS_PLAGUE = new Condition(
      BC, "Moebius Plague", null, null,
      LONG_LATENCY, EXTREME_VIRULENCE, NO_SPREAD, Table.make(
        MOTOR, -20, MUSCULAR, -20
      ),
      "Moebius Sublimation",
      "Moebius Displacement",
      "Moebius Plague",
      null,
      "Plague Immune"
    ),
    
    SPONTANEOUS_DISEASE[] = {
      ILLNESS, CANCER, HIREX_PARASITE
    },
    ALL_CONDITIONS[] = {
      POISONING, ILLNESS, SPICE_ADDICTION,
      CANCER, RAGE_INFECTION, HIREX_PARASITE,
      ALBEDAN_STRAIN, SILVERQUICK, MOEBIUS_PLAGUE
    };
}




