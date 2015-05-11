/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Manufacture extends Plan implements Behaviour, Qualities {
  
  
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
  final static float
    SHORTAGE_DC_MOD    = 5,
    SHORTAGE_TIME_MULT = 5,
    FAILURE_TIME_MULT  = 5;
  
  
  final public Property venue;
  final public Conversion conversion;
  final public boolean commission;
  
  private float speedBonus = 1;
  private Item made, needed[];
  private float amountMade = 0;
  
  
  
  public Manufacture(
    Actor actor, Property venue,
    Conversion conversion, Item made, boolean commission
  ) {
    super(actor, venue, MOTIVE_JOB, NO_HARM);
    this.venue = venue;
    this.made = made == null ? conversion.out : made;
    this.conversion = conversion;
    this.needed = conversion.raw;
    this.commission = commission;
  }
  
  
  public Manufacture(Actor actor, Property venue, Item made) {
    this(actor, venue, made.type.materials(), made, true);
  }
  
  
  public Manufacture(Session s) throws Exception {
    super(s);
    venue = (Venue) s.loadObject();
    conversion = (Conversion) s.loadObject();
    made = Item.loadFrom(s);
    this.needed = conversion.raw;
    speedBonus = s.loadFloat();
    amountMade = s.loadFloat();
    commission = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveObject(conversion);
    Item.saveTo(s, made);
    s.saveFloat(speedBonus);
    s.saveFloat(amountMade);
    s.saveBool(commission);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Manufacture(other, venue, conversion, made, commission);
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
    //  TODO:  Limit the maximum quality that can be achieved in the absence of
    //  a suitable facility upgrade!
    this.speedBonus = estimatedOutput(works, conversion, upgrades);
    return this;
  }
  
  
  public static float estimatedOutput(
    Venue v, Conversion c, Upgrade... upgrades
  ) {
    float output = 1;
    for (Item r : c.raw) if (v.stocks.relativeShortage(r.type) >= 1) {
      output /= 2;
    }
    
    float upgradeBonus = 0;
    if (upgrades.length == 0) {
      upgradeBonus = 0.5f;
    }
    else for (Upgrade upgrade : upgrades) {
      final float bonus = v.structure.upgradeLevel(upgrade);
      upgradeBonus += bonus / (upgrade.maxLevel * upgrades.length);
    }
    output *= 1 + upgradeBonus;
    
    final float powerCut = v.stocks.relativeShortage(Economy.POWER);
    output *= (2 - powerCut) / 2;
    
    return output;
  }
  
  
  public static String statusMessageFor(
    String normal, Venue v, Conversion c, Upgrade... upgrades
  ) {
    if ((! v.structure.intact()) || (! v.inWorld())) {
      return normal;
    }
    final StringBuffer s = new StringBuffer();
    
    float output = Manufacture.estimatedOutput(v, c, upgrades) / 2f;
    output *= Manufacture.MAX_UNITS_PER_DAY * v.staff.workforce() / 2f;
    
    int numWorking = 0;
    for (Actor a : v.staff.workers()) {
      final Manufacture m = (Manufacture) a.matchFor(Manufacture.class, true);
      if (m == null || a.aboard() != v) continue;
      if (m.made().type == c.out.type) numWorking++;
    }
    
    boolean needsOkay = true;
    if (v.stocks.relativeShortage(POWER) >= 0.5f) {
      needsOkay = false;
      s.append(
        "Production will be slowed without enough "+POWER+"."
      );
    }
    else for (Item r : c.raw) if (v.stocks.relativeShortage(r.type) >= 1) {
      needsOkay = false;
      s.append(
        "Production would be faster with a supply of "+r.type+"."
      );
      break;
    }
    if (needsOkay) s.append(normal);
    s.append("\n  Estimated "+c.out.type+" per day: "+I.shorten(output, 1));
    s.append("\n  "+numWorking+" active workers");
    return s.toString();
  }
  
  
  
  /**  Vary this based on delay since inception and demand at the venue-
    */
  final Trait BASE_TRAITS[] = { ENERGETIC, URBANE, ACQUISITIVE };
  

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
      amount   = venue.inventory().amountOf (made     ) - 1,
      demand   = venue.inventory().demandFor(made.type) + 1;
    if (demand < amount) {
      if (report) I.say("  Insufficient demand: "+demand+"/"+amount);
      return 0;
    }
    
    final float urgency = (1 + ((demand - amount) / demand)) / 2;
    setCompetence(successChanceFor(actor));
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, urgency, competence(), 2, MILD_FAIL_RISK, BASE_TRAITS
    );
    if (report) {
      I.say("\n  Basic urgency: "+urgency);
      I.say("  Amount/Demand: "+amount+"/"+demand);
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
      
      I.say("  Success chance: "+successChanceFor(actor));
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    final Conversion c = conversion;
    float chance = 1.0f;
    for (int i = c.skills.length; i-- > 0;) {
      chance *= actor.skills.chance(c.skills[i], c.skillDCs[i]);
    }
    chance = (chance + 1) / 2f;
    return chance;
  }
  
  
  private boolean hasNeeded() {
    //
    //  TODO:  Average the shortage of each needed item, so that penalties are
    //  less stringent for output that demands multiple inputs?
    for (Item need : needed) {
      if (! venue.inventory().hasItem(need)) return false;
    }
    return true;
  }
  
  
  public boolean valid() {
    if ((GameSettings.hardCore || commission) && ! hasNeeded()) return false;
    return super.valid();
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true;
    //if (selfCommission()) return false;
    return
      (amountMade >= 2) || (amountMade >= made.amount) ||
      venue.inventory().hasItem(made);
  }
  
  /*
  private boolean selfCommission() {
    return
      commission != null && commission.actor() == actor &&
      ! commission.finished();
  }
  //*/
  
  
  public Behaviour getNextStep() {
    final float demand = venue.inventory().demandFor(made.type);
    if (made.type.form == Economy.FORM_MATERIAL) {
      made = Item.withAmount(made, demand + 1);
    }
    
    if (venue.inventory().hasItem(made)) {
      //if (selfCommission()) return commission;
      amountMade = made.amount;
      return null;
    }
    
    if (! hasNeeded()) {
      if (GameSettings.hardCore) return null;
      //  TODO:  if (venue.stocks.hasConversion(needed)) return etc.
    }
    
    return new Action(
      actor, venue,
      this, "actionMake",
      Action.REACH_DOWN, "Manufacturing "
    );
  }
  
  
  public boolean actionMake(Actor actor, Venue venue) {
    //
    //  First, check to make sure you have adequate raw materials.  (In hard-
    //  core mode, raw materials are strictly essential, and will be depleted
    //  regardless of success.)
    final boolean hasNeeded = hasNeeded();
    if (GameSettings.hardCore && ! hasNeeded) {
      interrupt(INTERRUPT_NO_PREREQ);
      return false;
    }
    final Conversion c = conversion;
    
    //  Secondly, make sure the skill tests all check out, and deplete any raw
    //  materials used up.
    final float checkMod = (hasNeeded ? 0 : SHORTAGE_DC_MOD);
    boolean success = true;
    //  TODO:  Have this average results, rather than '&' them...
    for (int i = c.skills.length; i-- > 0;) {
      success &= actor.skills.test(c.skills[i], c.skillDCs[i] + checkMod, 1);
    }
    
    float increment = 1f * speedBonus / (made.amount * TIME_PER_UNIT);
    if (made.type instanceof DeviceType) increment /= DEVICE_TIME_MULT;
    if (made.type instanceof OutfitType) increment /= OUTFIT_TIME_MULT;
    if (! hasNeeded) increment /= SHORTAGE_TIME_MULT;
    if (! success) increment /= FAILURE_TIME_MULT;
    
    if ((success || GameSettings.hardCore) && increment > 0) {
      for (Item r : c.raw) {
        final Item used = Item.withAmount(r, r.amount * increment);
        venue.inventory().removeItem(used);
      }
    }
    
    //
    //  Advance progress, and check if you're done yet.
    if (increment > 0) {
      amountMade += increment * made.amount;
      final Item added = Item.withAmount(made, increment * made.amount);
      venue.stocks.addItem(added);
      if (verbose && I.talkAbout == actor) {
        I.say("Progress increment on "+made+": "+increment);
      }
    }
    return venue.stocks.hasItem(made);
  }
  
  
  
  /**  Rendering and interface behaviour-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Manufacturing ")) {
      d.append(made.type);
      if (made.refers != null) {
        d.append(" for ");
        d.append(made.refers);
      }
    }
  }
}








