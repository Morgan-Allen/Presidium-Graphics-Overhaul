


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




//  TODO:  Have Holdings appear spontaneously (much like nurseries), and then
//  have actors set up residence inside.  Don't make them directly responsible
//  for initial siting.


public class FindHome extends Plan {
  
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  private static boolean
    verbose = false;
  
  
  final Property newHome;
  
  
  public static FindHome attemptFor(Actor actor, Property at) {
    Property newHome = lookForHome(actor, actor.base());
    if (newHome == null || newHome == actor.mind.home()) return null;
    return new FindHome(actor, newHome);
  }
  

  private FindHome(Actor actor, Property newHome) {
    super(actor, newHome, true, NO_HARM);
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
    return ROUTINE;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("Getting next site action ");
    if (actor.mind.home() == newHome) return null;
    
    if (! newHome.inWorld()) {
      if (! canPlace()) { interrupt(INTERRUPT_NO_PREREQ); return null; }
      
      Tile goes = ((Venue) newHome).mainEntrance();
      goes = Spacing.nearestOpenTile(goes, actor);
      
      final Action sites = new Action(
        actor, goes,
        this, "actionSiteHome",
        Action.REACH_DOWN, "Siting home"
      );
      return sites;
    }
    
    final Action finds = new Action(
      actor, newHome,
      this, "actionFindHome",
      Action.REACH_DOWN, "Finding home"
    );
    if (newHome.boardableType() == Boarding.BOARDABLE_VENUE) {
      final Venue v = (Venue) newHome;
      if (actor.aboard() != v || ! v.structure.intact()) {
        finds.setMoveTarget(v.mainEntrance());
      }
    }
    return finds;
  }
  
  
  public boolean valid() {
    return actor.inWorld() && (newHome.inWorld() || canPlace());
  }
  
  
  private boolean canPlace() {
    final Venue v = (Venue) newHome;
    return v.canPlace();
  }
  
  
  public boolean actionSiteHome(Actor client, Target site) {
    if (! canPlace()) { interrupt(INTERRUPT_CANCEL); return false; }
    final Venue v = (Venue) newHome;
    v.doPlacement();
    client.mind.setHome(v);
    return true;
  }
  
  
  public boolean actionFindHome(Actor client, Property best) {
    final boolean report = verbose && I.talkAbout == client;
    
    if (report) {
      I.say("\nAttempting to move into "+best+", ID: "+hashCode());
      I.say("  Rating is: "+rateHolding(client, best));
    }
    
    if (rateHolding(client, best) <= 0) {
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
  
  
  
  /**  Static helper methods for home placement/location-
    */
  public static float crowdingAt(Property v, Actor a) {
    if (v == null || a == null) return 1;
    return v.crowdRating(a, Backgrounds.AS_RESIDENT);
  }
  
  
  public static Property lookForHome(Actor client, Base base) {
    final boolean report = verbose && I.talkAbout == client;
    
    final Stage world = base.world;
    final Property oldHome = client.mind.home(), work = client.mind.work();
    if (crowdingAt(work, client) < 1) return work;
    
    final Pick <Property> pick = new Pick <Property> ();
    if (oldHome != null) pick.compare(oldHome, rateHolding(client, oldHome));
    
    for (Object o : world.presences.sampleFromMap(
      client, world, 3, null, Holding.class
    )) {
      final Holding h = (Holding) o;
      final float rating = rateHolding(client, h);
      if (report) I.say("Rating for "+h+" is "+rating);
      pick.compare(h, rating);
    }
    
    if (pick.result() == null || Rand.index(10) == 0) {
      final Holding h = newHoldingFor(client);
      final float rating = rateHolding(client, h);
      if (report) I.say("Rating for new site "+h+" is "+rating);
      pick.compare(h, rating);
    }
    
    final Property best = pick.result();
    if (report && best != null) {
      I.say("Looking for home, best site: "+best);
      I.say("Crowding is: "+crowdingAt(best, client));
    }
    return best;
  }
  
  
  private static Holding newHoldingFor(Actor client) {
    //  TODO:  ESTABLISH HUTS INSTEAD?
    if (client.base().primal) return null;
    
    final Stage world = client.world();
    final int maxDist = Stage.SECTOR_SIZE;
    final Holding holding = new Holding(client.base());
    final Tile origin = searchPoint(client);
    final Vars.Bool found = new Vars.Bool();
    
    final TileSpread spread = new TileSpread(origin) {
      
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, origin) > maxDist) return false;
        return ! t.blocked();
      }
      
      protected boolean canPlaceAt(Tile t) {
        holding.setPosition(t.x, t.y, world);
        if (holding.canPlace()) { found.val = true; return true; }
        return false;
      }
    };
    spread.doSearch();
    
    if (found.val == true) return holding;
    else return null;
  }
  
  
  private static Tile searchPoint(Actor client) {
    final Presences presences = client.world().presences;
    final Batch <Target> amenities = new Batch <Target> ();
    
    vetAmenity(client.mind.work(), amenities, client);
    final Target refuge = presences.nearestMatch(SERVICE_REFUGE, client, -1);
    vetAmenity(refuge, amenities, client);
    final Target nearby = presences.nearestMatch(Venue.class, client, -1);
    vetAmenity(nearby, amenities, client);
    if (amenities.size() == 0) amenities.add(client);
    
    return Spacing.bestMidpoint(amenities.toArray(Target.class));
  }
  
  
  private static void vetAmenity(Object t, Batch <Target> toAdd, Actor client) {
    if (! (t instanceof Venue)) return;
    final Venue v = (Venue) t;
    if (v.base() != client.base()) return;
    toAdd.add(v);
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
  
  private static float rateHolding(Actor actor, Property newHome) {
    if (newHome == null || newHome.base() != actor.base()) return -1;
    
    final Property oldHome = actor.mind.home();
    float rating = 0;
    if (oldHome == null) rating += ROUTINE;
    if (newHome == oldHome) rating += DEFAULT_SWITCH_THRESHOLD;
    else if (crowdingAt(newHome, actor) >= 1) return 0;
    
    if (newHome instanceof Holding) {
      final int UL = ((Holding) newHome).upgradeLevel();
      final float TL = HoldingUpgrades.TAX_LEVELS[UL];
      rating -= ActorMotives.greedPriority(actor, TL);
      rating += UL;
    }
    
    final Series <Actor> residents = newHome.staff().residents();
    if (residents.size() > 0) {
      float averageRelations = 0; for (Actor a : residents) {
        averageRelations += actor.relations.valueFor(a);
      }
      averageRelations /= residents.size();
      rating += averageRelations * 2;
    }
    
    rating -= Plan.rangePenalty(actor.mind.work(), newHome);
    final Tile o = actor.world().tileAt(newHome);
    rating -= actor.base().dangerMap.sampleAt(o.x, o.y) * ROUTINE;
    
    return rating;
  }
}






/*
final Series <Actor> residents = holding.personnel.residents();
final int
  UL = holding.upgradeLevel(),
  maxPop = HoldingUpgrades.OCCUPANCIES[UL];
final float crowding = residents.size() * 1f / maxPop;

float rating = (1 + UL) / HoldingUpgrades.NUM_LEVELS;
if (holding == actor.mind.home()) {
  rating *= 1.5f;
}
else if (crowding >= 1) return -1;
if (holding.inWorld()) {
  rating *= 1.5f;
}
if (actor.mind.home() == null) rating += ROUTINE;

rating *= (2f - crowding);
rating -= Plan.greedBonus(actor, HoldingUpgrades.TAX_LEVELS[UL]) / ROUTINE;
rating -= Plan.rangePenalty(actor.mind.work(), holding);

if (residents.size() > 0) {
  float averageRelations = 0; for (Actor a : residents) {
    averageRelations += actor.memories.relationValue(a);
  }
  averageRelations /= residents.size();
  rating += averageRelations;
}

if (verbose) I.sayAbout(actor, "  Rating for holding is: "+rating);
return rating;
//*/
