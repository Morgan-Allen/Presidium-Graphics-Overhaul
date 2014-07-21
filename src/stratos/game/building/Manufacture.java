/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
import stratos.user.*;
import stratos.util.*;



//
//  TODO:  Quit after a certain total amount made.

public class Manufacture extends Plan implements Behaviour, Qualities {
  
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    MAX_UNITS_PER_DAY = 5,
    TIME_PER_UNIT     = World.STANDARD_DAY_LENGTH / (3 * MAX_UNITS_PER_DAY),
    DEVICE_TIME_MULT  = 2,
    OUTFIT_TIME_MULT  = 2;
  final static float
    SHORTAGE_DC_MOD    = 5,
    SHORTAGE_TIME_MULT = 5,
    FAILURE_TIME_MULT  = 5;
  
  private static boolean verbose = false;
  
  
  final public Employer venue;
  final public Conversion conversion;
  public int checkBonus = 0;
  
  private Item made, needed[];
  private float amountMade = 0;
  
  
  
  public Manufacture(
    Actor actor, Employer venue, Conversion conversion, Item made
  ) {
    super(actor, venue);
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
    //timeMult   = s.loadInt();
    checkBonus = s.loadInt();
    amountMade = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveObject(conversion);
    //Conversion.saveTo(s, conversion);
    Item.saveTo(s, made);
    //s.saveInt(timeMult  );
    s.saveInt(checkBonus);
    s.saveFloat(amountMade);
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
  
  
  
  /**  Vary this based on delay since inception and demand at the venue-
    */
  final Trait BASE_TRAITS[] = { ENERGETIC, URBANE, ACQUISITIVE };
  

  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    final int shift = venue.personnel().shiftFor(actor);
    if (shift == Venue.OFF_DUTY) return 0;
    
    final float priority = priorityForActorWith(
      actor, venue, shift == Venue.SECONDARY_SHIFT ? IDLE : ROUTINE,
      MILD_HELP, FULL_COMPETITION,
      conversion.skills, BASE_TRAITS,
      NO_MODIFIER, NO_DISTANCE_CHECK, MILD_FAIL_RISK, report
    );
    return priority;
  }
  
  
  protected float successChance() {
    final Conversion c = conversion;
    float chance = 1.0f;
    for (int i = c.skills.length; i-- > 0;) {
      chance *= actor.traits.chance(
        c.skills[i], c.skillDCs[i] - checkBonus
      );
    }
    chance = (chance + 1) / 2f;
    if (! hasNeeded()) return chance / 2;
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
    return
      (amountMade >= 2) || (amountMade >= made.amount) ||
      venue.inventory().hasItem(made);
  }
  
  
  public Behaviour getNextStep() {
    final float demand = venue.inventory().demandFor(made.type);
    if (demand > 0) made = Item.withAmount(made, demand + 5);
    if (venue.inventory().hasItem(made)) {
      amountMade = made.amount;
      return null;
    }
    if (GameSettings.hardCore && ! hasNeeded()) return null;
    return new Action(
      actor, venue,
      this, "actionMake",
      Action.REACH_DOWN, "Working"
    );
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
      success &= actor.traits.test(c.skills[i], c.skillDCs[i] + checkMod, 1);
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
    d.append("Manufacturing "+made.type);
    if (made.refers != null) {
      d.append(" for ");
      d.append(made.refers);
    }
  }
}








