

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.util.*;
import stratos.game.wild.Species;
import static stratos.game.actors.Qualities.*;



//  TODO:  This piggybacks off the Relations class, but determines primary
//         'desires'.

//  TODO:  Move greed, ambition, and contentment-evaluation over here.
//  TODO:  Record memories?


public class ActorMotives {
  
  
  private static boolean
    rateVerbose = false;
  
  final static int
    UPDATE_INTERVAL  = Stage.STANDARD_HOUR_LENGTH,
    MOTIVE_EVAL_TIME = Stage.STANDARD_DAY_LENGTH * 5;
  
  
  final Actor actor;
  private float solitude = 0.0f;
  
  
  
  public ActorMotives(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    solitude = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveFloat(solitude);
  }
  
  
  
  public void setSolitude(float s) {
    this.solitude = s;
  }
  
  
  public void updateValues(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    final float inc = (1f * UPDATE_INTERVAL) / MOTIVE_EVAL_TIME;
    
    float RS = rateSolitude();
    solitude = (solitude * (1 - inc)) + (RS * inc);
  }
  
  
  
  /**  Social motives-
    */
  private float rateSolitude() {
    
    final float
      trait = 1f + actor.traits.relativeLevel(OUTGOING),
      baseF = (int) (ActorRelations.BASE_NUM_FRIENDS * trait);
    if (baseF == 0) return 0;
    
    float numF = 0;
    for (Relation r : actor.relations.relations()) {
      numF += 2 * Nums.clamp(r.value() - r.novelty(), 0, 1);
    }
    return (baseF - numF) / baseF;
  }
  
  
  
  public float solitude() {
    return solitude;
  }
  
  
  public float attraction(Actor other) {
    if (actor.species() != Species.HUMAN) return 0.5f;
    if (other.species() != Species.HUMAN) return 0;
    if (actor.health.juvenile() || other.health.juvenile()) return 0;
    //
    //  TODO:  Create other exceptions based on kinship modifiers.
    //
    //  First, we establish a few facts about each actor's sexual identity:
    float actorG = 0, otherG = 0;
    if (actor.traits.hasTrait(GENDER_MALE  )) actorG = -1;
    if (actor.traits.hasTrait(GENDER_FEMALE)) actorG =  1;
    if (other.traits.hasTrait(GENDER_MALE  )) otherG = -1;
    if (other.traits.hasTrait(GENDER_FEMALE)) otherG =  1;
    float attraction = other.traits.traitLevel(HANDSOME) * 3.33f;
    attraction += otherG * other.traits.traitLevel(FEMININE) * 3.33f;
    attraction *= (actor.traits.relativeLevel(INDULGENT) + 1f) / 2;
    //
    //  Then compute attraction based on orientation-
    final String descO = actor.traits.description(ORIENTATION);
    float matchO = 0;
    if (descO.equals("Heterosexual")) {
      matchO = (actorG * otherG < 0) ? 1 : 0.33f;
    }
    else if (descO.equals("Bisexual")) {
      matchO = 0.66f;
    }
    else if (descO.equals("Homosexual")) {
      matchO = (actorG * otherG > 0) ? 1 : 0.33f;
    }
    return attraction * matchO / 10f;
  }
  
  
  public Trait preferredGender() {
    final boolean male = actor.traits.male();
    if (actor.traits.hasTrait(ORIENTATION, "Heterosexual")) {
      return male ? GENDER_FEMALE : GENDER_MALE;
    }
    if (actor.traits.hasTrait(ORIENTATION, "Homosexual")) {
      return male ? GENDER_MALE : GENDER_FEMALE;
    }
    return Rand.yes() ? GENDER_MALE : GENDER_FEMALE;
  }
  
  
  
  /**  Material motives-
    */
  //  TODO:  Have holdings refer to this?  Or refer to supply/demand for
  //         holdings?
  
  public static float rateDesire(Item item, Actor buys, Actor receives) {
    final boolean report = rateVerbose && I.talkAbout == buys;
    
    float rating = 0;
    if (Visit.arrayIncludes(Economy.ALL_FOOD_TYPES, item.type)) {
      final float hunger = receives.health.hungerLevel();
      if (hunger > 0.5f) rating += (hunger - 0.5f) * 10;
    }
    
    if (receives.mind.home() instanceof Venue) {
      final Venue home = (Venue) receives.mind.home();
      final float need = home.stocks.shortageOf(item.type);
      if (need > 0) rating += need;
    }
    
    if (item.type == receives.gear.deviceType()) {
      final Item device = receives.gear.deviceEquipped();
      if (item.quality > device.quality) {
        rating += 2 * (item.quality - device.quality);
      }
    }
    
    if (item.type == receives.gear.outfitType()) {
      final Item outfit = receives.gear.outfitEquipped();
      if (item.quality > outfit.quality) {
        rating += 2 * (item.quality - outfit.quality);
      }
    }
    
    final float pricedAt = item.defaultPrice();
    rating += greedPriority(receives, pricedAt);
    if (report) I.say("  Rating for "+item+" is: "+rating);
    
    if (buys != null) {
      rating /= 1 + (greedPriority(buys, pricedAt));
      if (report) I.say("    After pricing? "+rating);
    }
    return rating;
  }
  

  public static float greedPriority(Actor actor, float creditsPerDay) {
    //
    //  The evaluation here is based on credits value relative to daily income-
    //  e.g, if I got this every day, how much is it worth to me?  (And even
    //  then, we're fudging things a little- see below.)
    if (creditsPerDay <= 0) return 0;
    final boolean report = rateVerbose && I.talkAbout == actor;
    
    final float greed = 1 + actor.traits.relativeLevel(Qualities.ACQUISITIVE);
    final Profile p = actor.base().profiles.profileFor(actor);
    
    float baseUnit = actor.gear.credits();
    baseUnit += (100 + p.salary()) / 2;
    baseUnit /= Backgrounds.NUM_DAYS_PAY;
    
    float mag = 1f + (creditsPerDay / baseUnit);
    mag = Nums.log(2, mag) * greed;
    
    //  Value is taken as directly proportional when below average, and
    //  logarithmic/additive beyond that:
    final float level;
    if (mag <= 1) level = mag * Plan.ROUTINE;
    else          level = mag + Plan.ROUTINE - 1;
    
    if (report) {
      I.say("\nEvaluating greed value of "+creditsPerDay+" credits.");
      I.say("  Salary: "+p.salary()+", credits: "+actor.gear.credits());
      I.say("  Pay interval: "+Backgrounds.NUM_DAYS_PAY+", greed: "+greed);
      I.say("  Base unit: "+baseUnit+", magnitude: "+mag);
      I.say("  Final level: "+level);
    }
    return level;
  }
}
