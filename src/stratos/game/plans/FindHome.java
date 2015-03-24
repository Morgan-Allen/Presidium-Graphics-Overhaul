/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class FindHome extends Plan {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final Property newHome;
  

  private FindHome(Actor actor, Property newHome) {
    super(actor, newHome, MOTIVE_PERSONAL, NO_HARM);
    this.newHome = newHome;
  }


  public FindHome(Session s) throws Exception {
    super(s);
    newHome = (Property) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(newHome);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation.
    */
  protected float getPriority() {
    if (! shouldSwitchTo(actor, newHome)) return -1;
    final int numO = (int) (
      (1 - newHome.crowdRating(actor, Backgrounds.AS_RESIDENT)) *
      HoldingUpgrades.OCCUPANCIES[0]
    );
    if (Plan.competition(this, newHome, actor) >= numO) return -1;
    return ROUTINE;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next look-for-home action... "+actor);
    }
    final Action finds = new Action(
      actor, newHome,
      this, "actionFindHome",
      Action.REACH_DOWN, "Finding home"
    );
    return finds;
  }
  
  
  public boolean actionFindHome(Actor client, Property best) {
    final boolean report = verbose && I.talkAbout == client;
    if (report) I.say("\nAttempting to move into "+best+", ID: "+hashCode());
    
    if (! shouldSwitchTo(actor, best)) {
      if (report) I.say("  Venue unsuitable!");
      interrupt(INTERRUPT_CANCEL);
      return false;
    }
    
    if (report) I.say("  Huge success!");
    client.mind.setHome(best);
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    if (! newHome.inWorld()) {
      d.append("Siting a new home");
    }
    else {
      d.append("Finding a home at ");
      d.append(newHome);
    }
  }
  
  
  /**  Site evaluation-
    */
  //TODO:  Implement the following-
  /*
    Only a child:  No crowding effects, friend/family effects doubled.
    Ordinary holding:  +2 per upgrade level, halved for nobles, 0 if unbuilt.
    Bastion:  +10.  Noble household only.
    Native hut:  Must defect to native base.
    Rent/tax level:  -5 for 50% of daily wages, scaled accordingly.
    
    Work in vehicle:  Cannot move out, must live there.
  //*/
  
  //  TODO:  Use some modification of this for the getPriority() method?
  
  private static float rateHolding(Actor actor, Property newHome) {
    if (newHome == null || newHome.base() != actor.base()) return -1;
    
    //  NOTE:  We allow maximum crowding at the current home venue, or capacity
    //  will never be filled
    final Property oldHome = actor.mind.home();
    if (oldHome != newHome && crowdingAt(newHome, actor) >= 1) return -1;
    
    float rating = 0;
    if (oldHome == null   ) rating += ROUTINE;
    if (newHome == oldHome) rating += DEFAULT_SWITCH_THRESHOLD;
    if (newHome == actor.mind.work()) return rating + PARAMOUNT;
    
    if (newHome instanceof Holding) {
      final float UL = ((Holding) newHome).upgradeLevel();
      rating += ROUTINE * UL / HoldingUpgrades.NUM_LEVELS;
      //
      //  TODO:  Figure out what the exact taxation scheme at home ought to be-
      //         possibly outsource to the Audit class?
      float credsPerDay = actor.base().profiles.profileFor(actor).salary();
      credsPerDay *= 0.5f / Backgrounds.NUM_DAYS_PAY;
      rating -= actor.motives.greedPriority(credsPerDay);
    }
    
    final Series <Actor> residents = newHome.staff().lodgers();
    if (residents.size() > 0) {
      float averageRelations = 0; for (Actor a : residents) {
        averageRelations += actor.relations.valueFor(a);
      }
      averageRelations /= residents.size();
      rating += averageRelations * 2;
    }
    
    rating -= Plan.rangePenalty(actor.base(), actor.mind.work(), newHome);
    rating -= Plan.dangerPenalty(newHome, actor);
    return rating;
  }
  
  
  private static float crowdingAt(Property v, Actor a) {
    if (v == null || a == null) return 1;
    return v.crowdRating(a, Backgrounds.AS_RESIDENT);
  }
  
  
  private static boolean shouldSwitchTo(Actor client, Property newHome) {
    final boolean report = verbose && I.talkAbout == client;
    
    final Property oldHome = client.mind.home();
    final float
      oldRating = rateHolding(client, oldHome),
      newRating = rateHolding(client, newHome);
    
    if (newRating <= 0) return false;
    final Property best = newRating > oldRating ? newHome : oldHome;
    
    //final Pick <Property> pick = new Pick <Property> (0);
    //pick.compare(oldHome, rateHolding(client, oldHome));
    //pick.compare(newHome, rateHolding(client, newHome));
    //final Property best = pick.result();
    if (best == null || best == oldHome) return false;
    
    if (report) {
      I.say("\nLooking for best home for "+client);
      I.say("  Rating for new: "+newRating+" ("+I.tagHash(newHome)+")");
      I.say("  Rating for old: "+oldRating+" ("+I.tagHash(oldHome)+")");
      I.say("  Best result: "+I.tagHash(best));
    }
    return true;
  }
  
  
  public static FindHome attemptFor(Actor client, Property newHome) {
    if (! shouldSwitchTo(client, newHome)) return null;
    return new FindHome(client, newHome);
  }
}








