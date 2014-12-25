/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.util.*;




public class PathSearch extends Search <Boarding> {
  
  
  
  /**  Field definitions and constructors-
    */
  private static boolean blocksVerbose = false;
  
  
  final protected Boarding destination;
  public Mobile client = null;
  private Boarding aimPoint = null;
  
  private Boarding closest;
  private float closestDist;
  //private Boarding batch[] = new Boarding[8];
  //
  //  TODO:  Incorporate the Places-search constraint code here.
  //  TODO:  Allow for airborne pathing.
  //  TODO:  Allow for larger actors.
  //  TODO:  In the case of tiles, perform diagonals-culling here.
  
  
  public PathSearch(Boarding init, Boarding dest, boolean limit) {
    super(init, -1);
    if (init == null) I.complain("NO ORIGIN!"     );
    if (dest == null) I.complain("NO DESTINATION!");
    this.destination = dest;
    this.closest     = init;
    this.closestDist = Spacing.distance(init, dest);
    this.aimPoint    = boardPoint(destination);
    if (limit) this.maxSearched = searchLimit(init, dest, aimPoint);
  }
  
  
  protected Boarding boardPoint(Boarding aims) {
    while (true) {
      if (aims instanceof Tile) break;
      Boarding entrance = null;
      if (aims instanceof Property) {
        entrance = ((Property) aims).mainEntrance();
      }
      if (entrance == null) break;
      else aims = entrance;
    }
    return aims;
  }
  
  
  protected int searchLimit(Boarding init, Boarding dest, Boarding aims) {
    int limit = (int) Nums.max(
      Spacing.outerDistance(init, dest),
      Spacing.outerDistance(init, aims)
    );
    limit = Nums.max(limit + 2, Stage.PATCH_RESOLUTION * 2);
    limit *= TileConstants.T_INDEX.length;
    return limit;
  }
  
  
  public PathSearch doSearch() {
    if (verbose) I.say(
      "Searching for path between "+init+" and "+destination+
      ", search limit: "+maxSearched
    );
    super.doSearch();
    if (verbose) {
      if (success()) I.say("\n  Success!");
      else {
        I.say("\n  Failed.");
        if (client != null) {

          if (! Visit.arrayIncludes(destination.canBoard(), aimPoint)) {
            I.say("NO EXIT!");
          }
          if (! Visit.arrayIncludes(aimPoint.canBoard(), destination)) {
            I.say("NO ENTRY!");
          }
          
          I.say("Origin      open? "+canEnter(init       ));
          I.say("Destination open? "+canEnter(destination));
        }
      }
      I.say("  Closest approach: "+closest+", aimed for "+aimPoint);
      I.say("  Total searched: "+flagged.size()+"/"+maxSearched);
      I.say("");
    }
    return this;
  }
  
  
  protected void tryEntry(Boarding spot, Boarding prior, float cost) {
    final float spotDist = Spacing.distance(spot, aimPoint);
    if (spot == aimPoint) {
      if (verbose) {
        I.say("\nMET AIM POINT: "+aimPoint);
        final boolean couldEnter =
          (aimPoint == destination) || Visit.arrayIncludes(
            aimPoint.canBoard(), destination
          );
        final float DC = cost(aimPoint, destination);
        I.say("COULD ENTER DESTINATION? "+couldEnter+", COST: "+DC);
      }
      closest = spot;
      closestDist = spotDist;
    }
    else if (spotDist < closestDist) {
      closest = spot;
      closestDist = spotDist;
    }
    super.tryEntry(spot, prior, cost);
  }
  
  
  protected void setEntry(Boarding spot, Entry flag) {
    spot.flagWith(flag);
  }
  
  
  protected Entry entryFor(Boarding spot) {
    return (Entry) spot.flaggedWith();
  }
  
  
  
  /**  Actual search-execution methods-
    */
  protected Boarding[] adjacent(Boarding spot) {
    return spot.canBoard();
  }
  
  
  protected float cost(Boarding prior, Boarding spot) {
    if (spot == null) return -1;
    if (spot == destination) return 0;
    float mods = 0;
    
    if (client != null) {
      //
      //  TODO:  Stay out of the unfogged areas of hostile bases, and fogged
      //  areas of your own.
      
      //  Avoid areas that you consider dangerous...
      //  TODO:  Can you guarantee this will be an actor?...
      final Senses s = ((Actor) client).senses;
      final float directionDanger = s.dangerFromDirection(prior);
      mods += directionDanger * 10;
      
      //  TODO:  This is removed for the moment, until collision is worked out.
      //  Restore later
      //  If the area or tile has other actors in it, increase the perceived
      //  cost.
      //if (spot != client.aboard()) mods += spot.inside().size() * 10;
    }
    
    //  Finally, return a value based on pathing difficulties in the terrain-
    final float baseCost = Spacing.distance(prior, spot);
    switch (spot.pathType()) {
      case (Tile.PATH_CLEAR  ) : return (1.0f * baseCost) + mods;
      case (Tile.PATH_ROAD   ) : return (0.5f * baseCost) + mods;
      case (Tile.PATH_HINDERS) : return (2.0f * baseCost) + mods;
      default : return baseCost;
    }
  }
  
  
  public static boolean blockedBy(Target t, Mobile m) {
    if (t == null) return true;
    if (! (t instanceof Boarding)) return false;
    return blockedBy((Boarding) t, m);
  }
  
  
  //
  //  TODO:  This all seems terribly complicated for such a frequently-used
  //  function.  Any way to simplify?
  
  //  TODO:  Each client should be able to override a method to specify this.
  
  private static boolean blockedBy(final Boarding b, final Mobile mobile) {
    if (b.boardableType() == Boarding.BOARDABLE_TILE) {
      return b.pathType() == Tile.PATH_BLOCKS;
      //  TODO:  RESTORE THIS LATER, once alternative transport modes are
      //  worked out.
      /*
      if (b.pathType() != Tile.PATH_BLOCKS) return false;
      if (mobile != null) {
        final Tile t = (Tile) b;
        final Element owns = t.owner();
        if (owns != null) {
          if (owns.height() <= mobile.position.z) return true;
          return false;
        }
        if (t.habitat().isOcean() && ! mobile.motionWater()) return true;
        return false;
      }
      return true;
      //*/
    }
    else if (mobile != null) {
      final boolean
        exists = b.inWorld(),
        allows = (b == mobile.aboard()) || b.allowsEntry(mobile),
        blocks = b.pathType() == Tile.PATH_BLOCKS;
      if (exists && allows && ! blocks) return true;
      if (mobile != null && mobile.position.z > b.height()) return false;
      
      if (blocksVerbose && I.talkAbout == mobile) {
        I.say("Problem with end point: "+b);
        I.say("  Still in world? "+exists );
        I.say("  Forbids entry? "+! allows);
        I.say("  Blocks passage? "+blocks );
      }
    }
    return false;
  }
  
  
  protected boolean canEnter(final Boarding spot) {
    return spot != null && ! blockedBy(spot, client);
  }
  
  
  protected float estimate(Boarding spot) {
    float dist = Spacing.distance(spot, aimPoint);
    dist += Spacing.distance(closest, spot) / 3.0f;
    return dist * 1.1f;
  }
  
  
  protected boolean endSearch(Boarding best) {
    return best == destination;
  }
}



/*
if (m == null || m.motion == null || m.aboard() == b) return false;
final Tile o = m.origin();
final Series <Mobile> inside = t.inside();
if (inside == null || inside.size() < 1) return false;
int xd = o.x - t.x, yd = t.y - o.y;
if (xd < 0) xd *= -1;
if (yd < 0) yd *= -1;
final Target PT = m.motion.target();

if (xd <= 2 && yd <= 2) {
  for (Mobile i : inside) if (i != m && i != PT) {
    return true;
  }
}
//*/

//  TODO:  
/*
if (aimPoint != null) {
  if (! venue.isEntrance(aimPoint)) {
    I.complain(
      "DESTINATION "+destination+" CANNOT ACCESS AIM POINT: "+aimPoint
    );
  }
  if (! Visit.arrayIncludes(aimPoint.canBoard(null), destination)) {
    I.complain(
      "AIM POINT: "+aimPoint+" CANNOT ACCESS DESTINATION: "+destination
    );
  }
}
else aimPoint = venue;
//*/

