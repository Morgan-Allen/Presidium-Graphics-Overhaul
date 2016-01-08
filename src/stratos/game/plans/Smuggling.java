/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.verse.*;
import stratos.util.*;



public class Smuggling extends Plan implements Journey.Purpose {
  
  
  /**  Data fields, construction, and save/load methods:
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  private Venue warehouse;
  private Vehicle vessel;
  private Sector origin;
  
  private boolean tripDone;
  private float profits;
  private Item[] moved;
  
  
  private Smuggling(Actor actor, Target subject) {
    super(actor, subject, MOTIVE_JOB, NO_HARM);
  }
  
  
  public static Smuggling asImmigration(Actor actor, Stage world) {
    final Smuggling s = new Smuggling(actor, actor);
    s.warehouse = null;
    s.vessel    = null;
    s.moved     = new Item[0];
    s.origin    = world.localSector();
    s.tripDone  = true;
    return s;
  }
  
  
  public static Smuggling asBoarding(Actor actor, Vehicle vessel) {
    final Smuggling s = new Smuggling(actor, vessel);
    s.warehouse = null;
    s.vessel    = vessel;
    s.moved     = new Item[0];
    s.origin    = actor.world().localSector();
    s.tripDone  = false;
    return s;
  }
  
  
  public static Smuggling asSmuggling(
    Actor actor, Venue warehouse, Vehicle vessel, Item moved[]
  ) {
    final Smuggling s = new Smuggling(actor, vessel);
    s.warehouse = warehouse;
    s.vessel    = vessel;
    s.moved     = moved;
    s.origin    = actor.world().localSector();
    s.tripDone  = false;
    return s;
  }
  
  
  public Smuggling(Session s) throws Exception {
    super(s);
    warehouse = (Venue  ) s.loadObject();
    vessel    = (Vehicle) s.loadObject();
    tripDone  = s.loadBool ();
    profits   = s.loadFloat();
    origin    = (Sector) s.loadObject();
    moved     = Item.loadItemsFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(warehouse);
    s.saveObject(vessel   );
    s.saveBool  (tripDone );
    s.saveFloat (profits  );
    s.saveObject(origin   );
    Item.saveItemsTo(s, moved);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public static Smuggling bestSmugglingFor(
    Venue depot, Vehicle ship, Actor actor, int maxAmount
  ) {
    final Pick <Item> pick = new Pick <Item> ();
    for (Item i : depot.stocks.allItems()) {
      float rating = depot.base().demands.exportPrice(i.type);
      pick.compare(Item.withAmount(i, Nums.min(maxAmount, i.amount)), rating);
    }
    if (pick.empty()) return null;
    
    final Item toMove[] = new Item[] { pick.result() };
    return Smuggling.asSmuggling(actor, depot, ship, toMove);
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Skill BASE_SKILLS[] = {};
  final static Trait BASE_TRAITS[] = {};
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting smuggling priority: "+actor);
    //
    //  TODO:  Make this a little more elaborate?
    final float priority = ROUTINE + motiveBonus();
    if (report) I.say("  Final priority: "+priority);
    return priority;
  }
  
  
  public boolean finished() {
    if (vessel == null) return actor.inWorld();
    if (actor.aboard() != vessel && ! vessel.landed()) return false;
    
    return super.finished();
  }
  
  
  protected Behaviour getNextStep() {
    if (vessel == null) return null;
    if (actor.aboard() != vessel && ! vessel.landed()) return null;
    
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next step in smuggling: "+actor);
    //
    //  Before you board the vessel, make sure to collect the goods.  Then,
    //  once the ship in question has landed, hop aboard.
    if (! tripDone) {
      if (warehouse != null) for (Item i : moved) if (! actor.gear.hasItem(i)) {
        if (report) I.say("  Collecting goods from "+warehouse);
        final Action collect = new Action(
          actor, warehouse,
          this, "actionCollect",
          Action.REACH_DOWN, "Collecting goods"
        );
        return collect;
      }
      if (report) I.say("  Boarding vessel: "+vessel);
      final Action board = new Action(
        actor, vessel,
        this, "actionBoard",
        Action.STAND, "Boarding vessel"
      );
      if (priorityEval >= URGENT) board.setProperties(Action.QUICK);
      return board;
    }
    //
    //  Once you arrive back from offworld, return to the warehouse to split
    //  the proceeds.  Otherwise you're done.
    else if (warehouse != null && profits > 0) {
      if (report) I.say("  Returning proceeds to "+warehouse);
      final Action returns = new Action(
        actor, warehouse,
        this, "actionReturnProfits",
        Action.TALK_LONG, "Returning profits"
      );
      return returns;
    }
    if (report) I.say("  Task completed.");
    return null;
  }
  
  
  public boolean actionCollect(Actor actor, Venue warehouse) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nCollecting goods: "+actor.currentAction().hashCode());
    for (Item i : moved) {
      warehouse.stocks.transfer(i, actor);
      if (report) I.say("  Transferred "+i);
    }
    return true;
  }
  
  
  public boolean actionBoard(Actor actor, Vehicle vessel) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nNow boarding "+vessel);
    return true;
  }
  
  
  public boolean actionReturnProfits(Actor actor, Venue warehouse) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nReturning profits to "+warehouse);
    
    //  TODO:  Include a split for the runner?  (Maybe on the side?)
    actor.gear.incCredits(0 - profits);
    warehouse.stocks.incCredits(profits);
    profits = 0;
    return true;
  }
  
  
  
  /**  Offworld activity-
    */
  public void onWorldExit() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nHave exited world.");
  }
  
  
  public void onWorldEntry() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nHave re-entered world.");
  }
  
  
  public void whileOffworld() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    final boolean honest = moved == null || moved.length == 0;
    if (honest || tripDone) { tripDone = true; return; }
    
    if (report) I.say("\nSelling goods offworld:");
    for (Item i : moved) {
      final float price = i.defaultPrice() * BaseDemands.SMUGGLE_MARGIN;
      if (report) I.say("  "+price+" credits for "+i);
      profits += price;
      actor.gear.removeItem(i);
    }
    if (report) I.say("  Total profit: "+profits);
    
    actor.gear.incCredits(profits);
    tripDone = true;
  }
  
  
  public boolean doneOffworld() {
    return tripDone;
  }
  
  
  public Sector origin() {
    return origin;
  }
  
  
  public boolean acceptsTransport(Vehicle t, Journey j) {
    if (t != null && t == vessel) return true;
    return j.destination() == origin() && j.forTrading();
  }
  


  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    final boolean honest = Visit.empty(moved);
    if (! tripDone) {
      if (actor.aboard() == vessel) {
        d.append("Waiting aboard ");
        d.append(vessel);
      }
      else if (honest) {
        d.append("Boarding ");
        d.append(vessel);
      }
      else {
        d.appendList("Smuggling ", (Object[]) moved);
        d.append(" from ");
        d.append(warehouse);
        d.append(" aboard ");
        d.append(vessel);
      }
    }
    else {
      if (honest) {
        d.append("Returning home");
      }
      else {
        d.append("Reporting earnings at ");
        d.append(warehouse);
      }
    }
  }
}





