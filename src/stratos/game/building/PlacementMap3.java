

package stratos.game.building;
import stratos.game.common.*;
import stratos.game.building.*;
import stratos.util.*;

import stratos.game.common.WorldSections.Section;
import org.apache.commons.math3.util.FastMath;



public class PlacementMap3 {
  
  
  final World world;
  
  static class Claim {
    Box2D area = new Box2D();
    Venue owner;
  }
  
  final Table <Venue, Claim> venueClaims = new Table();
  final Table <Section, List <Claim>> areaClaims = new Table();
  
  
  PlacementMap3(World world) {
    this.world = world;
  }
  
  
  private Batch <Claim> claimsConflicting(Box2D newClaim) {
    final Batch <Claim> conflict = new Batch <Claim> ();
    
    for (Section s : world.sections.sectionsUnder(newClaim)) {
      final List <Claim> claims = areaClaims.get(s);
      if (claims != null) for (Claim claim : claims) {
        if (claim.area.intersects(newClaim)) conflict.add(claim);
      }
    }
    
    return conflict;
  }
  
  
  private void cropArea(Box2D claim, Box2D other) {
    if (! claim.intersects(other)) return;
    
    if (claim.xpos() < other.xpos()) claim.xmax(other.xpos());
    else                             claim.xpos(other.xmax());
    if (claim.ypos() < other.ypos()) claim.ymax(other.ypos());
    else                             claim.ypos(other.ymax());
    //if (claim.xmax() <= claim.xpos()) claim.setX(-1, 0);
    //if (claim.ymax() <= claim.ypos()) claim.setY(-1, 0);
  }
  
  
  private void cropNewClaim(Box2D area) {
    for (Claim c : claimsConflicting(area)) {
      cropArea(area, c.area);
    }
  }
  
  
  private void assertNewClaim(Venue owner, Box2D area) {
    
    final Claim newClaim = new Claim();
    newClaim.area = area;
    newClaim.owner = owner;
    if (owner != null) venueClaims.put(owner, newClaim);
    
    for (Section s : world.sections.sectionsUnder(area)) {
      List <Claim> claims = areaClaims.get(s);
      if (claims == null) areaClaims.put(s, claims = new List <Claim> ());
      claims.add(newClaim);
    }
  }
  
  
  private void removeClaim(Claim claim) {
    for (Section s : world.sections.sectionsUnder(claim.area)) {
      final List <Claim> claims = areaClaims.get(s);
      claims.remove(claim);
      if (claims.size() == 0) areaClaims.remove(s);
    }
  }
  
  
  
  /**  Prototype code for dividing up the map into a laticework of spaces for
    *  ease of deterministic placement.
    */
  private Batch <Box2D> createLattice() {
    final Batch <Box2D> lattice = new Batch <Box2D> ();
    final Batch <Tile> flagged = new Batch <Tile> ();
    
    final Box2D worldBound = new Box2D();
    worldBound.set(0.5f, 0.5f, world.size - 2, world.size - 2);
    //
    //  
    for (Coord c : Visit.grid(worldBound)) {
      final Tile t = world.tileAt(c.x, c.y);
      
      if (t.flaggedWith() == lattice) continue;
      final Box2D around = new Box2D();
      around.xpos(t.x - 0.5f);
      around.ypos(t.y - 0.5f);
      
      if (Rand.yes()) {
        around.xdim(8);
        around.ydim(2);
      }
      else {
        around.xdim(2);
        around.ydim(8);
      }
      
      around.cropBy(worldBound);
      cropNewClaim(around);
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
    venueClaims.clear();
    areaClaims.clear();
    for (Box2D claimed : lattice) claimed.expandBy(-1);
    return lattice;
  }
  
  
  //  Okay.  Once you get the lattice in place, set up a bitmask based on
  //  coverage.
  
  //  TODO:  Set up space for roads.
}











