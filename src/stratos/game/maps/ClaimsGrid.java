

package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.BaseUI;
import stratos.util.*;



//  TODO:  Tiles should no longer store ownership-information, only pathing-
//  information.


public class ClaimsGrid {
  
  
  /**  Data fields, setup and save/load methods-
    */
  private static boolean
    verbose      = true ,
    extraVerbose = false;
  
  final Stage world;
  final Table <Venue, Claim> venueClaims;
  final List <Claim> areaClaims[][];
  final Base         baseClaims[][];
  
  
  static class Claim {
    Box2D area = new Box2D();
    Venue owner = null;
    boolean flag = false;
  }
  
  
  public ClaimsGrid(Stage world) {
    this.world = world;
    final int NS = world.size / world.sections.resolution;
    this.venueClaims = new Table <Venue, Claim> (NS * NS * 4);
    this.areaClaims = new List[NS][NS];
    this.baseClaims = new Base[NS][NS];
  }
  
  
  public void loadState(Session s) throws Exception {
    final Box2D temp = new Box2D();
    for (int n = s.loadInt(); n-- > 0;) {
      assertNewClaim(
        (Venue) s.loadObject(),
        temp.loadFrom(s.input())
      );
    }
    final int NS = world.size / world.sections.resolution;
    for (Coord c : Visit.grid(0, 0, NS, NS, 1)) {
      baseClaims[c.x][c.y] = (Base) s.loadObject();
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(venueClaims.size());
    for (Venue v : venueClaims.keySet()) {
      final Claim c = venueClaims.get(v);
      s.saveObject(v);
      c.area.saveTo(s.output());
    }
    final int NS = world.size / world.sections.resolution;
    for (Coord c : Visit.grid(0, 0, NS, NS, 1)) {
      s.saveObject(baseClaims[c.x][c.y]);
    }
  }
  
  
  
  /**  Methods for querying base-ownership:
    */
  //  TODO:  Is there any way to make this more precise?  Based off the danger-
  //  map, say?
  public Base baseClaiming(Tile t) {
    final StageSection s = world.sections.sectionAt(t.x, t.y);
    return baseClaims[s.x][s.y];
  }
  
  
  
  /**  Public methods for querying and asserting area-specific claims.
    */
  public Venue[] venuesConflicting(Box2D newClaim, Venue owner) {
    final Batch <Claim> against = claimsConflicting(newClaim, owner);
    final Venue conflict[] = new Venue[against.size()];
    int n = 0;
    for (Claim c : against) conflict[n++] = c.owner;
    return conflict;
  }
  
  
  private Batch <Claim> claimsConflicting(Box2D area, Venue owner) {
    final boolean report = verbose && extraVerbose && (
      BaseUI.currentPlayed() == owner.base()
    );
    final Batch <Claim> conflict = new Batch <Claim> ();
    if (report) {
      I.say("\nChecking for conflicts with claim by "+owner);
      I.say("  Area checked: "+area);
    }
    
    for (StageSection s : world.sections.sectionsUnder(area)) {
      final List <Claim> claims = areaClaims[s.x][s.y];
      
      if (claims != null) for (Claim claim : claims) {
        if ((! claim.flag) && claim.area.overlaps(area)) {
          
          final boolean clash = owner == null  ||
            claim.owner.preventsClaimBy(owner) ||
            claim.owner.footprint().overlaps(area);
          if (! clash) continue;
          
          if (report) I.say("  CONFLICTS WITH: "+claim.owner);
          conflict.add(claim);
          claim.flag = true;
        }
      }
    }
    
    for (Claim c : conflict) c.flag = false;
    return conflict;
  }
  
  
  
  /**  Claim-assertions and deletions-
    */
  public Claim assertNewClaim(Venue owner) {
    return assertNewClaim(owner, owner.footprint());
  }
  
  
  public Claim assertNewClaim(Venue owner, Box2D area) {
    final boolean report = verbose && BaseUI.currentPlayed() == owner.base();
    if (report) {
      I.say("\nAsserting new claim for "+owner);
      I.say("  Area claimed: "+area);
    }
    
    final Base belongs = owner.base();
    final Claim newClaim = new Claim();
    newClaim.area.setTo(area);
    newClaim.owner = owner;
    if (owner != null) venueClaims.put(owner, newClaim);
    
    for (StageSection s : world.sections.sectionsUnder(area)) {
      List <Claim> claims = areaClaims[s.x][s.y];
      if (claims == null) areaClaims[s.x][s.y] = claims = new List <Claim> ();
      claims.add(newClaim);
      baseClaims[s.x][s.y] = belongs;
      if (report) I.say("  Registering in section: "+s);
    }
    
    return newClaim;
  }
  
  
  //  TODO:  Have the venues themselves remember their own claims?
  public void removeClaim(Venue owner) {
    final boolean report = verbose && BaseUI.currentPlayed() == owner.base();
    final Claim claim = venueClaims.get(owner);
    if (claim == null) { I.complain("NO CLAIM MADE BY "+owner); return; }
    
    if (report) {
      I.say("\nRemoving claim made by: "+owner);
      I.say("  Area claimed: "+claim.area);
    }
    removeClaim(claim, report);
    venueClaims.remove(owner);
  }
  
  
  private void removeClaim(Claim claim, boolean report) {
    for (StageSection s : world.sections.sectionsUnder(claim.area)) {
      final List <Claim> claims = areaClaims[s.x][s.y];
      claims.remove(claim);
      if (claims.size() == 0) areaClaims[s.x][s.y] = null;
      if (report) I.say("  Un-registering in section: "+s);
    }
  }
  
  
  public void clearAllClaims() {
    venueClaims.clear();
    final int NS = world.size / world.sections.resolution;
    for (Coord c : Visit.grid(0, 0, NS, NS, 1)) areaClaims[c.x][c.y] = null;
  }
}






  
  
  
  
  
  /**  Utility methods for finding the largest claim which might fit within
    *  currently free space.
    */
  /*
  public Box2D cropNewClaim(Box2D area) {
    final Box2D cropped = new Box2D().setTo(area);
    final Box2D sides[] = {
      new Box2D(), new Box2D(), new Box2D(), new Box2D()  
    }, temp = new Box2D();
    
    for (Claim c : claimsConflicting(area)) {
      if (! cropToExclude(cropped, c.area, sides, temp)) break;
    }
    return cropped;
  }
  
  
  private boolean cropToExclude(
    Box2D b, Box2D o, Box2D sides[], Box2D temp
  ) {
    //
    //  Basically, create 4 areas for each side of this box, intersect with
    //  those, and see which gives you the biggest remainder.
    sides[0].set(o.xmax()           , o.ypos(), b.xdim(), o.ydim());
    sides[1].set(o.xpos() - b.xdim(), o.ypos(), b.xdim(), o.ydim());
    sides[2].set(o.xpos(), o.ymax()           , o.xdim(), b.ydim());
    sides[3].set(o.xpos(), o.ypos() - b.ydim(), o.xdim(), b.ydim());
    //
    float bestArea = 0;
    Box2D bestSide = null;
    for (Box2D side : sides) {
      temp.setTo(b).cropBy(side);
      final float area = temp.area();
      if (area > bestArea) { bestArea = area; bestSide = side; }
    }
    //
    if (bestSide == null) {
      b.set(-1, -1, 0, 0);
      return false;
    }
    b.cropBy(bestSide);
    return true;
  }
  
  
  
  /**  Prototype code for dividing up the map into a laticework of spaces for
    *  ease of deterministic placement.
    */
  /*
  //  TODO:  This can probably be gotten rid of.  Not used
  public Box2D[] placeLatticeWithin(
    Box2D area, Venue client, int laneSize, int maxUnits, boolean useLeftovers
  ) {
    final Batch <Claim> lattice = new Batch <Claim> ();
    final Batch <Tile > flagged = new Batch <Tile > ();
    final Box2D limit = new Box2D().setTo(area).expandBy(1);
    final float
      unit   = laneSize                         ,
      minDim = (useLeftovers ? 0 : unit) + 1.99f,
      shrink = 0 - (useLeftovers ? 1 : unit)    ;
    
    tileLoop: for (Coord c : Visit.grid(area)) {
      
      final Tile t = world.tileAt(c.x, c.y);
      if (t == null || t.flaggedWith() == lattice) continue;
      
      final Box2D around = new Box2D();
      around.xpos(t.x - 0.5f);
      around.ypos(t.y - 0.5f);
      final boolean horiz = Rand.yes();
      
      if (horiz) {
        around.xdim(unit * (1 + Rand.index(maxUnits)));
        around.ydim(unit);
      }
      else {
        around.ydim(unit * (1 + Rand.index(maxUnits)));
        around.xdim(unit);
      }
      around.expandBy(1);
      
      final Batch <Claim> conflicts = claimsConflicting(around);
      while (needsShrinking(around, limit, client, conflicts)) {
        if (horiz) around.incWide(shrink);
        else       around.incHigh(shrink);
        if (around.xdim() < minDim || around.ydim() < minDim) {
          continue tileLoop;
        }
      }
      if (around.xdim() < minDim || around.ydim() < minDim) continue;
      
      around.expandBy(-1);
      lattice.add(assertNewClaim(null, around));
      
      for (Tile under : world.tilesIn(around, true)) {
        under.flagWith(lattice);
        flagged.add(under);
      }
    }
    for (Tile t : flagged) t.flagWith(null);
    //
    //  And, last but not least, cleanup and return-
    final Box2D out[] = new Box2D[lattice.size()];
    int i = 0;
    for (Claim claim : lattice) {
      removeClaim(claim);
      out[i++] = claim.area;
    }
    return out;
  }
  
  
  private boolean needsShrinking(
    Box2D plot, Box2D limit, Venue client, Batch <Claim> conflicts
  ) {
    if (! plot.containedBy(world.area())) return true;
    if (! plot.containedBy(limit)) return true;
    if (client != null && plot.overlaps(client.area())) return true;
    
    for (Claim claim : conflicts) {
      if (claim.owner == client) continue;
      if (claim.area.overlaps(plot)) return true;
    }
    return false;
  }
  //*/
