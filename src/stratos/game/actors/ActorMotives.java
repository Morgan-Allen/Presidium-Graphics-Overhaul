/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Devices.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.craft.Outfits.*;



//  TODO:  This piggybacks off the Relations class, but determines primary
//         'desires'.

//  TODO:  Move greed, ambition, and contentment-evaluation over here.
//  TODO:  Record memories?

//  TODO:  This is primarily useful for humans.  So you need to be able to
//         override this for specific actor-types.


public class ActorMotives {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    rateVerbose = false;
  
  final static int
    UPDATE_INTERVAL  = Stage.STANDARD_HOUR_LENGTH,
    MOTIVE_EVAL_TIME = Stage.STANDARD_DAY_LENGTH * 5;
  
  
  final Actor actor;
  private float solitude = 0.5f;
  private Background ambition;
  
  
  
  
  public ActorMotives(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    solitude = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveFloat(solitude);
  }
  
  
  public void onWorldExit() {
    return;
  }
  
  
  
  /**  Regular updates-
    */
  public void setSolitude(float s) {
    this.solitude = s;
  }
  
  
  public void updateValues(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    final float inc = (1f * UPDATE_INTERVAL) / MOTIVE_EVAL_TIME;
    
    updateItemValues();
    
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
    for (Relation r : actor.relations.allRelations()) {
      numF += 2 * Nums.clamp(r.value() - r.novelty(), 0, 1);
    }
    return (baseF - numF) / baseF;
  }
  
  
  
  public float solitude() {
    return solitude;
  }
  
  
  public float attraction(Actor other) {
    if (actor.species() != Human.SPECIES) return 0.5f;
    if (other.species() != Human.SPECIES) return 0;
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
    attraction *= (actor.traits.relativeLevel(RELAXED) + 1f) / 2;
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
  
  
  
  /**  Ambition motives-
    */
  
  
  
  
  /**  Material motives-
    */
  private void updateItemValues() {
    final ActorRelations r = actor.relations;
    final Property home = actor.mind.home();
    final float hunger = Nums.clamp(actor.health.hungerLevel(), 0, 1);
    //
    //  We flag desire for any items needed at home, and anything needed to
    //  prevent starvation...
    if (home != null && home.inventory() instanceof Stocks) {
      final Stocks s = (Stocks) home.inventory();
      for (Traded t : s.shortageTypes(false)) {
        final float rating = Nums.clamp(s.relativeShortage(t, false) / 2, 0, 1);
        r.setupRelation(t, rating, Relation.TYPE_TRADED);
      }
    }
    for (Traded f : actor.species().canEat()) {
      r.setupRelation(f, hunger, Relation.TYPE_TRADED);
    }
    //
    //  Then we increment desire for personal gear, and any items needed for
    //  techniques or attack/shields to function-
    for (Traded t : actor.skills.getProficiencies()) {
      r.setupRelation(t, 0.5f, Relation.TYPE_GEAR);
    }
    for (Technique t : actor.skills.knownTechniques()) {
      final Traded c = t.itemNeeded();
      if (c != null) r.setupRelation(c, 0.5f, Relation.TYPE_GEAR);
    }
    if (actor.gear.maxPowerCells() > 0) {
      r.setupRelation(POWER_CELLS, 0.5f, Relation.TYPE_GEAR);
    }
    if (actor.gear.maxAmmoUnits() > 0) {
      r.setupRelation(AMMO_CLIPS, 0.5f, Relation.TYPE_GEAR);
    }
  }
  
  
  public float rateValue(Item i) {
    float value = actor.relations.valueFor(i.type);
    return value * Nums.min(1, i.amount) * Action.PARAMOUNT;
  }
  
  
  public Traded[] valuedForTrade() {
    final Batch <Traded> valued = new Batch();
    for (Relation r : actor.relations.allRelations()) {
      final Traded t = (Traded) I.cast(r.subject, Traded.class);
      if (t == null || t.form != FORM_MATERIAL) continue;
      valued.add(t);
    }
    return valued.toArray(Traded.class);
  }
  

  public float greedPriority(float creditsPerDay) {
    //
    //  The evaluation here is based on credits value relative to daily income-
    //  e.g, if I got this every day, how much is it worth to me?  (And even
    //  then, we're fudging things a little- see below.)
    final boolean report = I.talkAbout == actor && rateVerbose;
    final Background b = actor.mind.vocation();
    if (creditsPerDay <= 0 || (b != null && b.defaultSalary <= 0)) return 0;
    
    final float greed = 1 + actor.traits.relativeLevel(Qualities.SELFISH);
    float baseUnit = 0;
    baseUnit += b.defaultSalary / Backgrounds.NUM_DAYS_PAY;
    baseUnit += ((actor.gear.allCredits() - 25) * 2);
    baseUnit /= Backgrounds.NUM_DAYS_PAY;
    if (baseUnit <= 0) return Plan.PARAMOUNT;
    
    float mag = 1f + (creditsPerDay / baseUnit);
    mag = Nums.log(2, mag) * greed;
    
    //  Value is taken as directly proportional when below average, and
    //  logarithmic/additive beyond that:
    final float level;
    if (mag <= 1) level = mag * Plan.ROUTINE * 1;
    else          level = mag + Plan.ROUTINE - 1;
    
    if (report) {
      I.say("\nEvaluating greed value of "+creditsPerDay+" credits.");
      I.say("  Salary: "+b.defaultSalary+", credits: "+actor.gear.allCredits());
      I.say("  Pay interval: "+Backgrounds.NUM_DAYS_PAY+", greed: "+greed);
      I.say("  Base unit: "+baseUnit+", magnitude: "+mag);
      I.say("  Final level: "+level);
    }
    return level;
  }
}

