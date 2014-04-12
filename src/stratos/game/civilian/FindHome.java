


package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;



//  ...This needs to centre on the bastion/bunker system more reliably, then
//  the place of work.  Also, the results have to be more stable.

//  Implement the following-
/*  TODO:  HOUSE-SITING ALGORITHM:

Place of work.  (-5 for every sector's distance.)
Vital services.  (+2 for each, divided by 1-plus-sector-distance.)
Danger and Ambience.  (up to +/-5 modifier each.)
Friends and Family.  (up to +/-2 modifier per inhabitant, based on relation,
divided by 1-plus-sector-distance.)

No residents:  +2
1 resident:  +1
More than 2 residents:  -5
Only a child:  No crowding effects, friend/family effects doubled.

Ordinary holding:  +2 per upgrade level, halved for nobles, 0 if unbuilt.
Bastion:  +10.  Noble household only.
Native hut:  Must defect to native base.
Rent/tax level:  -5 for 50% of daily wages, scaled accordingly.

Work in vehicle:  Cannot move out, must live there.
//*/



public class FindHome extends Plan implements Economy {
  
  
  private static boolean verbose = false;
  
  
  final Employment newHome ;
  
  
  public static FindHome attemptFor(Actor actor) {
    Employment newHome = lookForHome(actor, actor.base()) ;
    if (newHome == null || newHome == actor.mind.home()) return null ;
    return new FindHome(actor, newHome) ;
  }
  

  private FindHome(Actor actor, Employment newHome) {
    super(actor, newHome) ;
    this.newHome = newHome ;
  }


  public FindHome(Session s) throws Exception {
    super(s) ;
    newHome = (Employment) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(newHome) ;
  }
  
  

  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    if (actor.mind.home() == newHome) return null ;
    
    if (verbose) I.sayAbout(actor, "Getting next site action ") ;
    if (! newHome.inWorld()) {
      if (! canPlace()) { abortBehaviour() ; return null ; }
      final Tile goes = actor.world().tileAt(newHome) ;
      final Action sites = new Action(
        actor, Spacing.nearestOpenTile(goes, actor),
        this, "actionSiteHome",
        Action.LOOK, "Siting home"
      ) ;
      sites.setProperties(Action.RANGED) ;
      return sites ;
    }
    final Action finds = new Action(
      actor, newHome,
      this, "actionFindHome",
      Action.LOOK, "Finding home"
    ) ;
    return finds ;
  }
  
  
  private boolean canPlace() {
    final Venue v = (Venue) newHome ;
    return v.canPlace() ;
  }
  
  
  public boolean actionSiteHome(Actor client, Target site) {
    if (! canPlace()) return false ;
    final Venue v = (Venue) newHome ;
    v.placeFromOrigin() ;
    client.mind.setHome(v) ;
    if (verbose) I.sayAbout(actor, "siting home at: "+v.origin()) ;
    return true ;
  }
  
  
  public boolean actionFindHome(Actor client, Employment best) {
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
  public static Holding lookForHome(Actor client, Base base) {
    final World world = base.world ;
    final Employment oldHome = client.mind.home() ;
    
    Holding best = null ;
    float bestRating = 0 ;
    
    //  TODO:  Also, Native Huts and the Bastion need to count here!
    if (oldHome instanceof Holding) {
      final Holding h = (Holding) oldHome ;
      best = h ;
      bestRating = rateHolding(client, h) ;
    }
    
    for (Object o : world.presences.sampleFromKey(
      client, world, 3, null, Holding.class
    )) {
      final Holding h = (Holding) o ;
      final float rating = rateHolding(client, h) ;
      if (rating > bestRating) { bestRating = rating ; best = h ; }
    }
    
    //  TODO:  You need to allow for construction of native hutments if there's
    //  no more conventional refuge available.  ...Or would that be equivalent
    //  to 'defection' to the natives faction?
    
    if (best == null || Rand.index(10) == 0) {
      final Venue refuge = (Venue) world.presences.nearestMatch(
        SERVICE_REFUGE, client, World.SECTOR_SIZE
      ) ;
      final Holding h = (refuge == null || refuge.base() != client.base()) ?
        null : newHoldingFor(client) ;  //  Use newHutFor(client)!
      final float rating = rateHolding(client, h) ;
      if (rating > bestRating) { bestRating = rating ; best = h ; }
    }
    
    if (verbose && I.talkAbout == client) {
      I.say("Looking for home, best site: "+best) ;
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
    if (client.mind.work() instanceof Venue) {
      return ((Venue) client.mind.work()).mainEntrance() ;
    }
    return client.origin() ;
  }


  private static float rateHolding(Actor actor, Holding holding) {
    if (holding == null || holding.base() != actor.base()) return -1 ;
    
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
    rating -= actor.mind.greedFor(HoldingUpgrades.TAX_LEVELS[UL]) / ROUTINE ;
    rating -= Plan.rangePenalty(actor.mind.work(), holding) ;
    
    if (residents.size() > 0) {
      float averageRelations = 0 ; for (Actor a : residents) {
        averageRelations += actor.mind.relationValue(a) ;
      }
      averageRelations /= residents.size() ;
      rating += averageRelations ;
    }
    
    if (verbose) I.sayAbout(actor, "  Rating for holding is: "+rating) ;
    return rating ;
  }
}






