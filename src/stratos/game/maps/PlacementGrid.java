

package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.building.*;
import stratos.util.*;
import stratos.game.common.WorldSections.Section;



public class PlacementGrid {
  
  
  /**  Data fields, setup and save/load methods-
    */
  private static boolean verbose = true;
  
  final World world;
  final Table <Venue, Claim> venueClaims;
  final List <Claim> areaClaims[][];
  
  final Bitmap freeLattice;
  
  static class Claim {
    Box2D area = new Box2D();
    Venue owner = null;
    boolean flag = false;
  }
  
  
  public PlacementGrid(World world) {
    this.world = world;
    this.venueClaims = new Table <Venue, Claim> ();
    final int NS = world.size / world.sections.resolution;
    this.areaClaims = new List[NS][NS];
    this.freeLattice = new Bitmap(world.size, world.size);
  }
  
  
  public void loadState(Session s) throws Exception {
    final Box2D temp = new Box2D();
    for (int n = s.loadInt(); n-- > 0;) {
      assertNewClaim(
        (Venue) s.loadObject(),
        temp.loadFrom(s.input())
      );
    }
    
    freeLattice.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(venueClaims.size());
    for (Venue v : venueClaims.keySet()) {
      final Claim c = venueClaims.get(v);
      s.saveObject(v);
      c.area.saveTo(s.output());
    }
    
    freeLattice.saveTo(s.output());
  }
  
  
  
  /**  Public methods for querying and asserting area-specific claims.
    */
  public Venue[] venuesConflicting(Box2D newClaim) {
    final Batch <Claim> against = claimsConflicting(newClaim);
    final Venue conflict[] = new Venue[against.size()];
    int n = 0;
    for (Claim c : against) conflict[n++] = c.owner;
    return conflict;
  }
  
  
  private Batch <Claim> claimsConflicting(Box2D newClaim) {
    final Batch <Claim> conflict = new Batch <Claim> ();
    
    for (Section s : world.sections.sectionsUnder(newClaim)) {
      final List <Claim> claims = areaClaims[s.x][s.y];
      if (claims != null) for (Claim claim : claims) {
        if ((! claim.flag) && claim.area.overlaps(newClaim)) {
          conflict.add(claim);
          claim.flag = true;
        }
      }
    }
    
    for (Claim c : conflict) c.flag = false;
    return conflict;
  }
  
  
  public Claim assertNewClaim(Venue owner) {
    return assertNewClaim(owner, owner.area());
  }
  
  
  public Claim assertNewClaim(Venue owner, Box2D area) {
    
    final Claim newClaim = new Claim();
    newClaim.area.setTo(area);
    newClaim.owner = owner;
    if (owner != null) venueClaims.put(owner, newClaim);
    
    for (Section s : world.sections.sectionsUnder(area)) {
      List <Claim> claims = areaClaims[s.x][s.y];
      if (claims == null) areaClaims[s.x][s.y] = claims = new List <Claim> ();
      claims.add(newClaim);
    }
    
    return newClaim;
  }
  
  
  //  TODO:  Have the venues themselves remember their own claims?
  public void removeClaim(Venue owner) {
    final Claim claim = venueClaims.get(owner);
    if (claim == null) I.complain("NO CLAIM MADE BY "+owner);
    else removeClaim(claim);
  }
  
  
  private void removeClaim(Claim claim) {
    for (Section s : world.sections.sectionsUnder(claim.area)) {
      final List <Claim> claims = areaClaims[s.x][s.y];
      claims.remove(claim);
      if (claims.size() == 0) areaClaims[s.x][s.y] = null;
    }
  }
  
  
  private void clearAllClaims() {
    venueClaims.clear();
    final int NS = world.size / world.sections.resolution;
    for (Coord c : Visit.grid(0, 0, NS, NS, 1)) areaClaims[c.x][c.y] = null;
  }
  
  
  
  /**  Utility methods for finding the largest claim which might fit within
    *  currently free space.
    */
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
  public Batch <Box2D> createLattice() {
    final Batch <Box2D> lattice = new Batch <Box2D> ();
    final Batch <Tile> flagged = new Batch <Tile> ();
    
    final Box2D worldBound = new Box2D(), sectorBound = new Box2D();
    worldBound.set(0.5f, 0.5f, world.size - 2, world.size - 2);
    final int SS = World.SECTOR_SIZE;
    //
    //  
    tileLoop: for (Coord c : Visit.grid(worldBound)) {
      final Tile t = world.tileAt(c.x, c.y);
      
      if (t.flaggedWith() == lattice) continue;
      if (! sectorBound.contains(t.x, t.y)) {
        sectorBound.set((t.x / SS) * SS, (t.y / SS) * SS, SS, SS);
      }
      
      final Box2D around = new Box2D();
      around.xpos(t.x - 0.5f);
      around.ypos(t.y - 0.5f);
      final boolean horiz = Rand.yes();
      
      if (horiz) {
        around.xdim(2 * (1 + Rand.index(4)));
        around.ydim(2);
      }
      else {
        around.ydim(2 * (1 + Rand.index(4)));
        around.xdim(2);
      }
      around.cropBy(sectorBound);
      
      for (Claim claim : claimsConflicting(around)) {
        while (claim.area.overlaps(around)) {
          if (horiz) around.incWide(-2);
          else       around.incHigh(-2);
          if (around.xdim() < 1 || around.ydim() < 1) continue tileLoop;
        }
      }
      if (around.xdim() < 2 || around.ydim() < 2) continue;
      
      around.expandBy(1);
      lattice.add(around);
      assertNewClaim(null, around);
      
      for (Tile under : world.tilesIn(around, true)) {
        under.flagWith(lattice);
        flagged.add(under);
      }
    }
    //
    //  And, last but not least, final cleanup-
    for (Tile t : flagged) t.flagWith(null);
    clearAllClaims();
    
    final Bitmap map = this.freeLattice;
    float greyVals[][] = new float[world.size][world.size];
    
    for (Box2D claimed : lattice) {
      claimed.expandBy(-1);
      I.say("Claimed is: "+claimed);
      for (Tile u : world.tilesIn(claimed, false)) {
        map.setVal(u.x, u.y, true);
        greyVals[u.x][u.y] = 1;
      }
    }
    
    I.present(greyVals, "FREE LATTICE", 200, 200, 0, 1);
    
    return lattice;
  }
  
  
  
}






