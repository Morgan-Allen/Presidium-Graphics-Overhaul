/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



//
//  TODO:  Speed bonus needs to be upgraded constantly, in case new upgrades
//  get added or raw materials run short during a manufacture task!


public class Manufacture extends Plan implements Behaviour {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    evalVerbose = false,
    verbose     = false;
  
  final public static int
    MAX_UNITS_PER_DAY = 5,
    TIME_PER_UNIT     = Stage.STANDARD_DAY_LENGTH / (3 * MAX_UNITS_PER_DAY),
    DEVICE_TIME_MULT  = 2,
    OUTFIT_TIME_MULT  = 2;
  
  
  final public Property venue;
  final public Conversion conversion;
  final public boolean commission;
  
  private float speedBonus = 1;
  private Item made, needed[];
  private float amountMade = 0;
  
  
  
  public Manufacture(
    Actor actor, Property venue,
    Conversion conversion, Item made, Item needed[], boolean commission
  ) {
    super(actor, venue, MOTIVE_JOB, NO_HARM);
    this.venue = venue;
    this.made = made == null ? conversion.out : made;
    this.conversion = conversion;
    this.needed = conversion.raw;
    this.commission = commission;
  }
  
  
  public Manufacture(Actor actor, Property venue, Item made) {
    this(
      actor, venue,
      made.type.materials(), made, made.type.materials().raw, true
    );
  }
  
  
  public Manufacture(Session s) throws Exception {
    super(s);
    made       = Item.loadFrom(s);
    needed     = Item.loadItemsFrom(s);
    venue      = (Venue) s.loadObject();
    conversion = (Conversion) s.loadObject();
    speedBonus = s.loadFloat();
    amountMade = s.loadFloat();
    commission = s.loadBool ();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Item.saveTo     (s, made  );
    Item.saveItemsTo(s, needed);
    s.saveObject(venue     );
    s.saveObject(conversion);
    s.saveFloat(speedBonus );
    s.saveFloat(amountMade );
    s.saveBool (commission );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Manufacture(other, venue, conversion, made, needed, commission);
  }
  
  
  public Item made() {
    return made;
  }
  
  
  public Item[] needed() {
    return needed;
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final Manufacture m = (Manufacture) p;
    if (m.conversion != conversion) return false;
    if (! m.made().matchKind(made)) return false;
    return true;
  }
  
  
  
  /**  Estimates, calculation and display of manufacturing rates, upgrade boni,
    *  etc.
    */
  public Manufacture setBonusFrom(
    Venue works, boolean required, Upgrade... upgrades
  ) {
    speedBonus = estimatedOutput(works, conversion);
    if (commission && required) {
      final int topQuality = (int) (speedBonus * Item.MAX_QUALITY / 2f);
      speedBonus *= (Item.AVG_QUALITY + 0.5f) / (1 + made.quality);
      if (made.quality >= topQuality) speedBonus /= 2;
    }
    return this;
  }
  
  
  public Manufacture setSpeedBonus(float speedMult) {
    speedBonus = speedMult * 1;
    return this;
  }
  
  
  public static int topQuality(
    Venue v, Conversion c, Upgrade... upgrades
  ) {
    float speedBonus = estimatedOutput(v, c);
    return (int) (speedBonus * Item.MAX_QUALITY / 2f);
  }
  
  
  public static float estimatedOutput(
    Venue v, Conversion c, Upgrade... upgrades
  ) {
    float output = 1;
    for (Item r : c.raw) if (v.stocks.relativeShortage(r.type, false) >= 1) {
      output /= 2;
    }
    
    if (Visit.empty(upgrades)) upgrades = c.upgrades();
    
    float upgradeBonus = 0;
    if (Visit.empty(upgrades)) {
      upgradeBonus = 0.5f;
    }
    else for (Upgrade upgrade : upgrades) {
      final float bonus = v.structure.upgradeLevel(upgrade);
      upgradeBonus += bonus / (upgrade.maxLevel * upgrades.length);
    }
    output *= 1 + upgradeBonus;
    
    final float powerCut = v.stocks.relativeShortage(Economy.POWER, false);
    output *= (2 - powerCut) / 2;
    
    return output;
  }
  
  
  public static void updateProductionEstimates(Venue v, Conversion... cons) {
    for (Conversion c : cons) {
      v.stocks.setDailyDemand(c.out.type, 0, 0);
      for (Item r : c.raw) v.stocks.setDailyDemand(r.type, 0, 0);
    }
    for (Conversion c : cons) {
      float output = Manufacture.estimatedOutput(v, c, c.upgrades()) / 2f;
      output *= Manufacture.MAX_UNITS_PER_DAY * v.staff.workforce()  / 2f;
      v.stocks.incDailyDemand(c.out.type, 0, output);
      for (Item r : c.raw) {
        v.stocks.incDailyDemand(r.type, output * r.amount / c.out.amount, 0);
      }
    }
  }
  
  
  private boolean hasNeeded() {
    for (Item need : needed) {
      if (venue.inventory().amountOf(need) < 1) return false;
    }
    return true;
  }
  
  
  public boolean valid() {
    if ((GameSettings.hardCore || commission) && ! hasNeeded()) return false;
    return super.valid();
  }
  
  
  
  /**  Vary this based on delay since inception and demand at the venue-
    */
  final Trait BASE_TRAITS[] = { PERSISTENT, METICULOUS };
  

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nAssessing priority for manufacturing "+made);
    
    final int shift = venue.staff().shiftFor(actor);
    if (shift == Venue.OFF_DUTY) return 0;
    if (commission && venue.inventory().hasItem(made)) {
      if (report) I.say("  Commission done!");
      return 0;
    }
    
    final float
      amount   = venue.inventory().amountOf(made),
      shortage = venue.inventory().relativeShortage(made.type, true);
    if (shortage <= 0 && ! commission) {
      if (report) I.say("  No shortage!");
      return 0;
    }
    
    final float urgency = commission ?
      ((3 + amount  ) / 2) :
      ((1 + shortage) / 2) ;
    final float chance = conversion.testChance(actor, 0);
    setCompetence(Nums.clamp(chance + (speedBonus / 2), 0, 1));
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, urgency, competence(), 2, MILD_FAIL_RISK, BASE_TRAITS
    );
    if (report) {
      I.say("\n  Basic urgency: "+urgency);
      I.say("  Amount/Shortage: "+amount+"/"+shortage);
      I.say("  Speed bonus:   "+speedBonus);
      
      I.say("\n  Needed items:");
      for (Item need : needed) {
        I.say("    "+need+" (has "+venue.inventory().amountOf(need)+")");
      }
      
      I.say("\n  Needed skills:");
      final Conversion c = conversion;
      for (int i = c.skills.length; i-- > 0;) {
        final int knownLevel = (int) actor.traits.usedLevel(c.skills[i]);
        I.say("    "+c.skills[i]+" "+c.skillDCs[i]+" (has "+knownLevel+")");
      }
      
      I.say("  Success chance: "+chance  );
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true;
    return
      (amountMade >= 2) || (amountMade >= made.amount) ||
      venue.inventory().hasItem(made);
  }
  
  
  public Behaviour getNextStep() {
    
    if (made.type.form == Economy.FORM_MATERIAL) {
      final float demand = venue.inventory().totalDemand(made.type);
      made = Item.withAmount(made, demand + 1);
    }
    
    if (venue.inventory().hasItem(made)) {
      amountMade = made.amount;
      return null;
    }
    
    if (! hasNeeded()) {
      if (GameSettings.hardCore) return null;
    }
    
    return new Action(
      actor, venue,
      this, "actionMake",
      Action.BUILD, "Making "
    );
  }
  
  
  public boolean actionMake(Actor actor, Venue venue) {
    final boolean report = I.talkAbout == venue && verbose;
    if (report) {
      I.say("\nMaking "+made+" at "+venue);
      I.say("  Amount before:      "+venue.stocks.amountOf(made));
      I.say("  Speed bonus:        "+speedBonus);
    }
    //
    //  First, check to make sure you have adequate raw materials.  (In hard-
    //  core mode, raw materials are strictly essential, and will be depleted
    //  regardless of success.)
    final boolean hasNeeded = hasNeeded();
    if (GameSettings.hardCore && ! hasNeeded) {
      interrupt(INTERRUPT_NO_PREREQ);
      return false;
    }
    //
    //
    final Action a = action();
    final float success = conversion.performTest(actor, 0, 1, a);
    float increment = success * speedBonus / (made.amount * TIME_PER_UNIT);
    if (made.type instanceof DeviceType) increment /= DEVICE_TIME_MULT;
    if (made.type instanceof OutfitType) increment /= OUTFIT_TIME_MULT;
    //
    //  Advance progress, and check if you're done yet.
    if (increment > 0) {
      for (Item r : conversion.raw) {
        final Item used = Item.withAmount(r, r.amount * increment);
        venue.inventory().removeItem(used);
      }
      amountMade += increment * made.amount;
      final Item added = Item.withAmount(made, increment * made.amount);
      venue.stocks.addItem(added);
      if (report) {
        I.say("  Progress increment: "+increment);
        I.say("  Amount after:       "+venue.stocks.amountOf(made));
      }
    }
    return venue.stocks.hasItem(made);
  }
  
  
  
  /**  Rendering and interface behaviour-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Making ")) {
      d.append(made.type);
      if (made.refers != null) {
        d.append(" for ");
        d.append(made.refers);
      }
    }
  }
}








