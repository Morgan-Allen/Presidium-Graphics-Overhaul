


package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;




public class FindHome extends Plan implements Economy {
  
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  private static boolean
    verbose = false;
  
  
  final Employer newHome ;
  
  
  public static FindHome attemptFor(Actor actor) {
    Employer newHome = lookForHome(actor, actor.base()) ;
    if (newHome == null || newHome == actor.mind.home()) return null ;
    return new FindHome(actor, newHome) ;
  }
  

  private FindHome(Actor actor, Employer newHome) {
    super(actor, newHome) ;
    this.newHome = newHome ;
  }


  public FindHome(Session s) throws Exception {
    super(s) ;
    newHome = (Employer) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(newHome) ;
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation.
    */
  protected float getPriority() {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("Getting next site action ") ;
    if (actor.mind.home() == newHome) return null ;
    
    if (! newHome.inWorld()) {
      if (! canPlace()) { abortBehaviour() ; return null ; }
      
      Tile goes = ((Venue) newHome).mainEntrance();
      goes = Spacing.nearestOpenTile(goes, actor);
      
      final Action sites = new Action(
        actor, goes,
        this, "actionSiteHome",
        Action.REACH_DOWN, "Siting home"
      );
      return sites ;
    }
    
    final Action finds = new Action(
      actor, newHome,
      this, "actionFindHome",
      Action.REACH_DOWN, "Finding home"
    );
    if (newHome.boardableType() == Boardable.BOARDABLE_VENUE) {
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
    final Venue v = (Venue) newHome ;
    return v.canPlace() ;
  }
  
  
  public boolean actionSiteHome(Actor client, Target site) {
    if (! canPlace()) { abortBehaviour(); return false ; }
    final Venue v = (Venue) newHome ;
    v.placeFromOrigin() ;
    client.mind.setHome(v) ;
    if (verbose) I.sayAbout(actor, "siting home at: "+v.origin()) ;
    return true ;
  }
  
  
  public boolean actionFindHome(Actor client, Employer best) {
    if (best.homeCrowding(client) >= 1) {
      if (verbose) I.sayAbout(actor, "No space!") ;
      abortBehaviour();
      return false;
    }
    client.mind.setHome(best) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    if (! newHome.inWorld()) {
      d.append("Siting a new home") ;
    }
    else {
      d.append("Finding a home at ") ;
      d.append(newHome) ;
    }
  }
  
  
  
  /**  Static helper methods for home placement/location-
    */
  public static Employer lookForHome(Actor client, Base base) {
    final boolean report = verbose && I.talkAbout == client;
    
    final World world = base.world ;
    final Employer oldHome = client.mind.home(), work = client.mind.work() ;
    
    if (work instanceof Vehicle) return work;
    if (work instanceof Bastion) return work;
    
    Employer best = oldHome ;
    float bestRating = oldHome == null ? 0 : rateHolding(client, best) ;
    
    for (Object o : world.presences.sampleFromMap(
      client, world, 3, null, Holding.class
    )) {
      final Holding h = (Holding) o ;
      final float rating = rateHolding(client, h) ;
      if (report) I.say("Rating for "+h+" is "+rating);
      if (rating > bestRating) { bestRating = rating ; best = h ; }
    }
    
    if (best == null || Rand.index(10) == 0) {
      //  TODO:  IMPLEMENT CONSTRUCTION OF NATIVE HUTS
      final Venue refuge = (Venue) world.presences.nearestMatch(
        SERVICE_REFUGE, client, World.SECTOR_SIZE
      ) ;
      if (report) I.say("Refuge is: "+refuge);
      
      final Holding h = (refuge == null || refuge.base() != client.base()) ?
        null : newHoldingFor(client) ;  //  Use newHutFor(client)!
      //final Holding h = newHoldingFor(client);
      final float rating = rateHolding(client, h);
      if (report) I.say("Rating for new site "+h+" is "+rating);
      if (rating > bestRating) { bestRating = rating; best = h; }
    }
    
    if (report && best != null) {
      I.say("Looking for home, best site: "+best) ;
      I.say("Crowding is: "+best.homeCrowding(client));
    }
    return best ;
  }
  
  
  private static Holding newHoldingFor(Actor client) {
    final World world = client.world() ;
    final int maxDist = World.SECTOR_SIZE ;
    final Holding holding = new Holding(client.base()) ;
    final Tile origin = searchPoint(client) ;
    final Vars.Bool found = new Vars.Bool() ;
    
    final TileSpread spread = new TileSpread(origin) {
      
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, origin) > maxDist) return false ;
        return ! t.blocked() ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        holding.setPosition(t.x, t.y, world) ;
        if (holding.canPlace()) { found.val = true ; return true ; }
        return false ;
      }
    } ;
    spread.doSearch() ;
    
    if (found.val == true) return holding ;
    else return null ;
  }
  
  
  //private static NativeHut newHutFor(Actor client) {
    //return null ;
  //}
  
  
  private static Tile searchPoint(Actor client) {
    final Presences presences = client.world().presences;
    final Batch <Target> amenities = new Batch <Target> ();
    
    if (client.mind.work() instanceof Venue) {
      amenities.add(client.mind.work());
    }
    final Target first = amenities.size() == 0 ? client : amenities.first();
    
    final Target refuge = presences.nearestMatch(SERVICE_REFUGE, first, -1);
    if (refuge != null) amenities.add(refuge);
    final Target nearby = presences.nearestMatch(Venue.class, first, -1);
    if (nearby != null) amenities.add(nearby);
    if (amenities.size() == 0) amenities.add(client);
    
    return Spacing.bestMidpoint(amenities.toArray(Target.class));
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
  
  private static float rateHolding(Actor actor, Employer newHome) {
    if (newHome == null || newHome.base() != actor.base()) return -1 ;
    
    final Employer oldHome = actor.mind.home();
    float rating = 0;
    if (oldHome == null) rating += ROUTINE;
    if (newHome == oldHome) rating += DEFAULT_SWITCH_THRESHOLD;
    else if (newHome.homeCrowding(actor) >= 1) return 0;
    
    if (newHome instanceof Holding) {
      final int UL = ((Holding) newHome).upgradeLevel();
      rating -= Plan.greedLevel(actor, HoldingUpgrades.TAX_LEVELS[UL]) * ROUTINE;
      rating += UL;
    }
    
    final Series <Actor> residents = newHome.personnel().residents() ;
    if (residents.size() > 0) {
      float averageRelations = 0 ; for (Actor a : residents) {
        averageRelations += actor.memories.relationValue(a) ;
      }
      averageRelations /= residents.size() ;
      rating += averageRelations * 2 ;
    }
    
    rating -= Plan.rangePenalty(actor.mind.work(), newHome);
    final Tile o = actor.world().tileAt(newHome);
    rating -= actor.base().dangerMap.sampleAt(o.x, o.y) * ROUTINE;
    
    return rating;
  }
}






/*
final Series <Actor> residents = holding.personnel.residents() ;
final int
  UL = holding.upgradeLevel(),
  maxPop = HoldingUpgrades.OCCUPANCIES[UL] ;
final float crowding = residents.size() * 1f / maxPop ;

float rating = (1 + UL) / HoldingUpgrades.NUM_LEVELS ;
if (holding == actor.mind.home()) {
  rating *= 1.5f ;
}
else if (crowding >= 1) return -1 ;
if (holding.inWorld()) {
  rating *= 1.5f ;
}
if (actor.mind.home() == null) rating += ROUTINE ;

rating *= (2f - crowding) ;
rating -= Plan.greedBonus(actor, HoldingUpgrades.TAX_LEVELS[UL]) / ROUTINE ;
rating -= Plan.rangePenalty(actor.mind.work(), holding) ;

if (residents.size() > 0) {
  float averageRelations = 0 ; for (Actor a : residents) {
    averageRelations += actor.memories.relationValue(a) ;
  }
  averageRelations /= residents.size() ;
  rating += averageRelations ;
}

if (verbose) I.sayAbout(actor, "  Rating for holding is: "+rating) ;
return rating ;
//*/
