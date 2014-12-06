/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;



public class Manufacture extends Plan implements Behaviour, Qualities {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    evalVerbose = false,
    verbose     = false;
  
  final static int
    MAX_UNITS_PER_DAY = 5,
    TIME_PER_UNIT     = Stage.STANDARD_DAY_LENGTH / (3 * MAX_UNITS_PER_DAY),
    DEVICE_TIME_MULT  = 2,
    OUTFIT_TIME_MULT  = 2;
  final static float
    SHORTAGE_DC_MOD    = 5,
    SHORTAGE_TIME_MULT = 5,
    FAILURE_TIME_MULT  = 5;
  
  
  final public Liveable venue;
  final public Conversion conversion;
  
  public int checkBonus = 0;
  public Commission commission = null;
  
  private Item made, needed[];
  private float amountMade = 0;
  
  
  
  public Manufacture(
    Actor actor, Liveable venue, Conversion conversion, Item made
  ) {
    super(actor, venue, true, NO_HARM);
    this.venue = venue;
    this.made = made == null ? conversion.out : made;
    this.conversion = conversion;
    this.needed = conversion.raw;
  }
  
  
  public Manufacture(Session s) throws Exception {
    super(s);
    venue = (Venue) s.loadObject();
    conversion = (Conversion) s.loadObject();
    made = Item.loadFrom(s);
    this.needed = conversion.raw;
    checkBonus = s.loadInt();
    amountMade = s.loadFloat();
    commission = (Commission) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveObject(conversion);
    Item.saveTo(s, made);
    s.saveInt(checkBonus);
    s.saveFloat(amountMade);
    s.saveObject(commission);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Manufacture(other, venue, conversion, made);
  }
  
  
  public Item made() {
    return made;
  }
  
  
  public Item[] needed() {
    return needed;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false;
    final Manufacture m = (Manufacture) p;
    if (m.conversion != conversion) return false;
    if (! m.made().matchKind(made)) return false;
    return true;
  }
  
  
  public Manufacture setBonusFrom(
    Venue works, boolean required, Upgrade... upgrades
  ) {
    float upgradeBonus = 0;
    for (Upgrade upgrade : upgrades) {
      final float bonus = works.structure.upgradeLevel(upgrade);
      upgradeBonus += bonus / (Structure.MAX_OF_TYPE * upgrades.length);
    }
    if (required && upgradeBonus <= 0) return null;
    
    //  TODO:  Limit the maximum quality that can be achieved in the absence of
    //  a suitable facility upgrade.
    
    final float powerCut = works.stocks.shortagePenalty(Economy.POWER);
    this.checkBonus = (int) (10 * (upgradeBonus - powerCut));
    return this;
  }
  
  
  
  /**  Vary this based on delay since inception and demand at the venue-
    */
  final Trait BASE_TRAITS[] = { ENERGETIC, URBANE, ACQUISITIVE };
  

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nAssessing priority for manufacturing "+made);
    
    final int shift = venue.personnel().shiftFor(actor);
    if (shift == Venue.OFF_DUTY) return 0;
    if (commission != null && commission.finished()) {
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
    
    float urgency = shift == Venue.SECONDARY_SHIFT ? IDLE : ROUTINE;
    if (! hasNeeded()) urgency /= 2;
    final float boost = IDLE * (demand - amount) / demand;
    
    final float priority = priorityForActorWith(
      actor, venue,
      urgency, boost,
      NO_HARM, NO_COMPETITION,
      MILD_FAIL_RISK, conversion.skills,
      BASE_TRAITS, NO_DISTANCE_CHECK,
      report
    );
    
    if (report) {
      I.say("\n  Basic urgency: "+urgency+", boost: "+boost);
      I.say("  Amount/Demand: "+amount+"/"+demand);
      I.say("  Check bonus:   "+checkBonus);
      
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
      
      I.say("  Success chance: "+successChance());
      I.say("  Final priority: "+priority);
    }
    return priority;
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
  
  
  protected float successChance() {
    final Conversion c = conversion;
    float chance = 1.0f;
    for (int i = c.skills.length; i-- > 0;) {
      chance *= actor.skills.chance(
        c.skills[i], c.skillDCs[i] - checkBonus
      );
    }
    chance = (chance + 1) / 2f;
    return chance;
  }
  
  
  public boolean valid() {
    if (GameSettings.hardCore && ! hasNeeded()) return false;
    return super.valid();
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true;
    if (selfCommission()) return false;
    return
      (amountMade >= 2) || (amountMade >= made.amount) ||
      venue.inventory().hasItem(made);
  }
  
  
  private boolean selfCommission() {
    return
      commission != null && commission.actor() == actor &&
      ! commission.finished();
  }
  
  
  public Behaviour getNextStep() {
    final float demand = venue.inventory().demandFor(made.type);
    if (made.type.form == Economy.FORM_MATERIAL) {
      made = Item.withAmount(made, demand + 1);
    }
    
    if (venue.inventory().hasItem(made)) {
      if (selfCommission()) return commission;
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
      abortBehaviour();
      return false;
    }
    final Conversion c = conversion;
    
    //  Secondly, make sure the skill tests all check out, and deplete any raw
    //  materials used up.
    final float checkMod = (hasNeeded ? 0 : SHORTAGE_DC_MOD) - checkBonus;
    boolean success = true;
    //  TODO:  Have this average results, rather than '&' them...
    for (int i = c.skills.length; i-- > 0;) {
      success &= actor.skills.test(c.skills[i], c.skillDCs[i] + checkMod, 1);
    }
    
    float increment = 1f / (made.amount * TIME_PER_UNIT);
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








