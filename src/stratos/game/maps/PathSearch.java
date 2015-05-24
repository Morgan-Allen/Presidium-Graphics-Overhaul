/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public class PathSearch extends Search <Boarding> {
  
  
  /**  Data fields and construction-
    */
  private static boolean
    blocksVerbose = false;
  
  final protected Boarding destination;
  private Mobile   client   = null;
  private Boarding aimPoint = null;
  private boolean useDanger = false;
  
  private Boarding closest;
  private float    closestDist;
  private boolean  limit;
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
    this.limit       = limit;
  }
  
  
  
  /**  Additional utility methods for setup and screening-
    */
  public void assignClient(Mobile client) {
    this.client    = client;
    this.useDanger = (client instanceof Actor);
  }
  
  
  public static boolean canApproach(Target aims, Mobile client) {
    return approachTile(aims, client) != null;
  }
  
  
  public static Tile approachTile(Target aims, Mobile client) {
    while (true) {
      if (blockedBy(aims, client)) return null;
      if (aims instanceof Tile) return (Tile) aims;
      
      if (aims instanceof Property) {
        aims = ((Property) aims).mainEntrance();
      }
      if (aims instanceof Boarding) {
        for (Boarding c : ((Boarding) aims).canBoard()) {
          if (c instanceof Tile) { aims = (Tile) c; break; }
        }
      }
      if (aims instanceof Mobile) {
        aims = ((Mobile) aims).aboard();
      }
    }
  }
  
  
  public static boolean blockedBy(Target t, Mobile m) {
    if (t == null || ! t.inWorld()) return true;
    if (! (t instanceof Boarding)) return false;
    return blockedBy((Boarding) t, m);
  }
  
  
  //
  //  TODO:  This all seems terribly complicated for such a frequently-used
  //  function.  Any way to simplify?
  
  private static boolean blockedBy(final Boarding b, final Mobile client) {
    if (b.boardableType() == Boarding.BOARDABLE_TILE) {
      return b.pathType() == Tile.PATH_BLOCKS;
    }
    else if (client != null) {
      final boolean
        exists = b.inWorld(),
        allows = (b == client.aboard()) || b.allowsEntry(client),
        blocks = b.pathType() == Tile.PATH_BLOCKS;
      
      if (blocksVerbose && I.talkAbout == client) {
        I.say("Assessing blockage at: "+b);
        I.say("  Still in world? "+exists );
        I.say("  Forbids entry? "+! allows);
        I.say("  Blocks passage? "+blocks );
      }
      if (exists && (allows || ! blocks)) return false;
      //if (mobile != null && mobile.position().z > b.height()) return false;
      return true;
    }
    return false;
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
  
  
  
  /**  Actual search performance/execution-
    */
  public PathSearch doSearch() {
    this.aimPoint    = approachTile(destination, client);
    this.closestDist = Spacing.distance(init, destination);
    if (aimPoint == null) aimPoint = destination;
    if (limit) this.maxSearched = searchLimit(init, destination, aimPoint);
    
    if (verbose) {
      I.say("Searching for path between "+init+" and "+destination);
      I.say("  Search limit: "+maxSearched+", aim point: "+aimPoint);
    }
    super.doSearch();
    if (verbose) {
      if (success()) I.say("\n  Success!");
      else I.say("\n  Failed.");
      I.say("  Closest approach: "+closest+", aimed for "+aimPoint);
      I.say("  Total searched: "+flagged.size()+"/"+maxSearched);
      I.say("");
    }
    return this;
  }
  
  
  protected void tryEntry(Boarding spot, Boarding prior, float cost) {
    final float spotDist = Spacing.distance(spot, aimPoint);
    
    if (spot == aimPoint) {
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
  
  
  protected Boarding[] adjacent(Boarding spot) {
    return spot.canBoard();
  }
  
  
  protected float cost(Boarding prior, Boarding spot) {
    if (spot == null) return -1;
    if (spot == destination) return 0;
    float mods = 0;

    if (spot.boardableType() == Boarding.BOARDABLE_TILE) {
      mods += spot.inside().size() * 2;
    }
    if (useDanger) {
      //  TODO:  Implement this...
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



