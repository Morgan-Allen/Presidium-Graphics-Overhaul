/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.economic.Devices.*;
import static stratos.game.economic.Outfits.*;



//  TODO:  This piggybacks off the Relations class, but determines primary
//         'desires'.

//  TODO:  Move greed, ambition, and contentment-evaluation over here.
//  TODO:  Record memories?

//  TODO:  This is primarily useful for humans.  So you need to be able to
//         override this for specific actor-types.


public class ActorMotives {
  
  
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
    for (Relation r : actor.relations.relations()) {
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
  
  
  
  /**  Ambition motives-
    */
  
  
  
  
  /**  Material motives-
    */
  //  The real problem here is that... certain techniques (and items that
  //  grant techniques) have pre-requisite raw materials for use.  There needs
  //  to be an access method for those.
  
  //  I also have to distinguish between 'things wanted as personal gear' and
  //  'things wanted for home or work'.  How do I do that?
  
  //  Food (based on hunger.)
  //  Goods for home.
  //  Personal gear.
  //  Raw materials for techniques or item-use.
  
  
  private void updateItemValues() {
    final ActorRelations r = actor.relations;
    final Property home = actor.mind.home();
    final OutfitType OT = actor.gear.outfitType();
    final DeviceType DT = actor.gear.deviceType();
    final float hunger = Nums.clamp(actor.health.hungerLevel(), 0, 1);
    //
    //  We flag desire for any items needed at home, and anything needed to
    //  prevent starvation...
    if (home != null && home.inventory() instanceof Stocks) {
      final Stocks s = (Stocks) actor.mind.home().inventory();
      for (Traded t : s.shortageTypes()) {
        final float rating = Nums.clamp(s.relativeShortage(t) / 2, 0, 1);
        r.setRelation(t, rating, Relation.TYPE_TRADED);
      }
    }
    for (Traded f : ALL_FOOD_TYPES) {
      r.setRelation(f, hunger, Relation.TYPE_TRADED);
    }
    //
    //  Then we increment desire for personal gear, and any items needed for
    //  techniques or attack/shields to function-
    for (Traded t : actor.skills.getProficiencies()) {
      r.setRelation(t, 0.5f, Relation.TYPE_GEAR);
    }
    for (Technique t : actor.skills.knownTechniques()) {
      final Traded c = t.itemNeeded();
      if (c != null) r.setRelation(c, 0.5f, Relation.TYPE_GEAR);
    }
    if (OT != null && OT.shieldBonus > 0 && ! OT.natural()) {
      r.setRelation(POWER_CELLS, 0.5f, Relation.TYPE_GEAR);
    }
    if (DT != null && DT.baseDamage > 0 && ! DT.natural()) {
      r.setRelation(AMMO_CLIPS, 0.5f, Relation.TYPE_GEAR);
    }
  }
  
  
  public float rateValue(Item i) {
    float value = actor.relations.valueFor(i.type);
    return value * Nums.min(1, i.amount) * Action.PARAMOUNT;
  }
  
  
  public Traded[] valuedForTrade() {
    final Batch <Traded> valued = new Batch();
    for (Relation r : actor.relations.relations()) {
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

    final float greed = 1 + actor.traits.relativeLevel(Qualities.ACQUISITIVE);
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




/*

public static float rateDesire(Item item, Actor buys, Actor receives) {
  final boolean report = rateVerbose && I.talkAbout == buys;
  
  float rating = 0;
  if (Visit.arrayIncludes(Economy.ALL_FOOD_TYPES, item.type)) {
    final float hunger = Nums.clamp(receives.health.hungerLevel(), 0, 1);
    return hunger * hunger * 10;
  }
  
  if (receives.mind.home() instanceof Venue) {
    final Venue home = (Venue) receives.mind.home();
    final float need = home.stocks.relativeShortage(item.type);
    final float amount = home.stocks.demandFor(item.type);
    if (need > 0 && amount > 0) rating += need * 5 * (item.amount / amount);
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
  if (receives.species().sapient()) {
    rating += receives.motives.greedPriority(pricedAt);
    if (report) I.say("  Rating for "+item+" is: "+rating);
  }
  if (buys != null) {
    rating /= 1 + receives.motives.greedPriority(pricedAt);
    if (report) I.say("    After pricing? "+rating);
  }
  return rating;
}
//*/



/*
//  TODO:  Put together a list of things that the character might want...

private Stack <Traded> desired = new Stack();


private void updateItemDesires() {
  
  boolean needsMed = false;
  desired.clear();
  
  if (actor.mind.home() instanceof Venue) {
    final Venue home = (Venue) actor.mind.home();
    
    for (Traded t : home.stocks.demanded()) {
      final float need = home.stocks.relativeShortage(t);
      if (need <= 0) continue;
      desired.add(t);
      final float rating = Action.PARAMOUNT * need / 10;
      actor.relations.setRelation(t, rating, 0);
    }
  }
  
  final OutfitType OT = actor.gear.outfitType();
  if (OT != null && ! OT.natural()) {
    desired.add(OT);
    actor.relations.setRelation(OT, Action.ROUTINE, 0);
    
    if (OT.shieldBonus > 0) {
      desired.add(POWER_CELLS);
      actor.relations.setRelation(POWER_CELLS, Action.ROUTINE, 0);
      needsMed = true;
    }
  }
  
  final DeviceType DT = actor.gear.deviceType();
  if (DT != null && ! DT.natural()) {
    desired.add(DT);
    actor.relations.setRelation(DT, Action.ROUTINE, 0);
    
    if (DT.baseDamage > 0) {
      desired.add(AMMO_CLIPS);
      actor.relations.setRelation(AMMO_CLIPS, Action.ROUTINE, 0);
      needsMed = true;
    }
  }
  
  if (Visit.arrayIncludes(actor.mind.work().services(), SERVICE_HEALTHCARE)) {
    needsMed = true;
  }
  if (needsMed) {
    desired.add(MEDICINE);
    actor.relations.setRelation(MEDICINE, Action.ROUTINE, 0);
  }
}


public float rateItem(Item item) {
  return -1;
}


public float amountDesired(Item item) {
  return -1;
}
//*/