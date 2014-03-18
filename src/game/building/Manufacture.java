/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.user.* ;
import src.util.* ;



//
//  TODO:  Quit after a certain total amount made.

public class Manufacture extends Plan implements Behaviour {
  
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    TIME_PER_UNIT = 30 ;
  
  private static boolean verbose = false ;
  
  
  final public Venue venue ;
  final public Conversion conversion ;
  public int timeMult = 1, checkBonus = 0 ;
  
  private Item made, needed[] ;
  private float amountMade = 0 ;
  
  
  
  public Manufacture(
    Actor actor, Venue venue, Conversion conversion, Item made
  ) {
    super(actor, venue) ;
    this.venue = venue ;
    this.made = made == null ? conversion.out : made ;
    this.conversion = conversion ;
    this.needed = conversion.raw ;
  }
  
  
  public Manufacture(Session s) throws Exception {
    super(s) ;
    venue = (Venue) s.loadObject() ;
    conversion = Conversion.loadFrom(s) ;
    made = Item.loadFrom(s) ;
    this.needed = conversion.raw ;
    timeMult   = s.loadInt() ;
    checkBonus = s.loadInt() ;
    amountMade = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(venue) ;
    Conversion.saveTo(s, conversion) ;
    Item.saveTo(s, made) ;
    s.saveInt(timeMult  ) ;
    s.saveInt(checkBonus) ;
    s.saveFloat(amountMade) ;
  }
  
  
  public Item made() {
    return made ;
  }
  
  
  public Item[] needed() {
    return needed ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Manufacture m = (Manufacture) p ;
    if (m.conversion != conversion) return false ;
    if (! m.made().matchKind(made)) return false ;
    return true ;
  }
  
  
  
  /**  Vary this based on delay since inception and demand at the venue-
    */
  public float priorityFor(Actor actor) {
    if (GameSettings.hardCore && ! hasNeeded()) return 0 ;
    //
    //  Don't work on this outside your shift (or at least make it more
    //  casual.)
    final int shift = venue.personnel.shiftFor(actor) ;
    if (shift == Venue.OFF_DUTY) return 0 ;
    if (shift == Venue.SECONDARY_SHIFT) return IDLE ;
    final boolean hasNeeded = hasNeeded() ;
    float competition = begun() ? 0 : venue.personnel.assignedTo(this) ;
    //
    //  Vary priority based on how qualified to perform the task you are.
    final Conversion c = conversion ;
    float chance = 1.0f ;
    for (int i = c.skills.length ; i-- > 0 ;) {
      chance *= actor.traits.chance(
        c.skills[i], c.skillDCs[i] - checkBonus
      ) ;
    }
    chance = (chance + 1) / 2f ;
    ///I.sayAbout(actor, "Chance: "+chance+" for "+this) ;
    float impetus = (URGENT * chance) + priorityMod - competition ;
    if (! hasNeeded) impetus /= 2 ;
    return Visit.clamp(impetus, IDLE, URGENT) ;
  }
  
  
  public boolean valid() {
    if (GameSettings.hardCore && ! hasNeeded()) return false ;
    return super.valid() ;
  }
  
  
  private boolean hasNeeded() {
    //
    //  TODO:  Average the shortage of each needed item, so that penalties are
    //  less stringent for output that demands multiple inputs.
    for (Item need : needed) {
      if (! venue.stocks.hasItem(need)) return false ;
    }
    return true ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true ;
    return (amountMade >= 2) || venue.stocks.hasItem(made) ;
  }
  
  
  public Behaviour getNextStep() {
    final float demand = venue.stocks.demandFor(made.type) ;
    if (demand > 0) made = Item.withAmount(made, demand + 5) ;
    if (venue.stocks.hasItem(made)) {
      return null ;
    }
    if (GameSettings.hardCore && ! hasNeeded()) return null ;
    return new Action(
      actor, venue,
      this, "actionMake",
      Action.REACH_DOWN, "Working"
    ) ;
  }
  
  
  public boolean actionMake(Actor actor, Venue venue) {
    //
    //  First, check to make sure you have adequate raw materials.  (In hard-
    //  core mode, raw materials are strictly essential, and will be depleted
    //  regardless of success.)
    final boolean hasNeeded = hasNeeded() ;
    if (GameSettings.hardCore && ! hasNeeded) {
      abortBehaviour() ;
      return false ;
    }
    final Conversion c = conversion ;
    final int checkMod = (hasNeeded ? 0 : 5) - checkBonus ;
    final float timeTaken = made.amount * TIME_PER_UNIT * timeMult ;
    final float progInc = (hasNeeded ? 1 : 0.5f) / timeTaken ;
    //
    //  Secondly, make sure the skill tests all check out, and deplete any raw
    //  materials used up.
    boolean success = true ;
    for (int i = c.skills.length ; i-- > 0 ;) {
      success &= actor.traits.test(c.skills[i], c.skillDCs[i] + checkMod, 1) ;
    }
    if ((success || GameSettings.hardCore) && progInc > 0) {
      for (Item r : c.raw) {
        final Item used = Item.withAmount(r, r.amount * progInc) ;
        venue.inventory().removeItem(used) ;
      }
    }
    //
    //  Advance progress, and check if you're done yet.
    final float oldAmount = venue.stocks.amountOf(made) ;
    float progress = (success ? progInc : (progInc / 10f)) * made.amount ;
    if (progress + oldAmount > made.amount) progress = made.amount - oldAmount ;
    if (progress > 0) {
      amountMade += progress ;
      final Item added = Item.withAmount(made, progress) ;
      venue.stocks.addItem(added) ;
      if (verbose && I.talkAbout == actor) {
        I.say("Time taken/success: "+timeTaken+"/"+success) ;
        I.say("Time mult: "+timeMult) ;
        I.say("Progress on "+made+": "+progress) ;
      }
    }
    return venue.stocks.hasItem(made) ;
  }
  
  
  
  /**  Rendering and interface behaviour-
    */
  public void describeBehaviour(Description d) {
    d.append("Manufacturing "+made.type) ;
    if (made.refers != null) {
      d.append(" for ") ;
      d.append(made.refers) ;
    }
  }
}








