/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.util.*;
import stratos.user.*;
import static stratos.game.common.Stage.*;



public class ClaimsGrid {
  
  /**  Data fields, setup and save/load methods-
    */
  private static boolean
    verbose      = false,
    extraVerbose = false;
  
  final Stage world;
  final Table <Venue, Claim> venueClaims;
  final List <Claim> areaClaims[][];
  
  private Batch <Box2D> areaBits = new Batch();
  final Base baseClaims[][];
  
  
  static class Claim {
    Box2D area = new Box2D();
    Venue owner = null;
    private float flag = -1;
  }
  
  
  public ClaimsGrid(Stage world) {
    this.world = world;
    final int NS = world.size / world.patches.resolution;
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
    final int NS = world.size / world.patches.resolution;
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
    final int NS = world.size / world.patches.resolution;
    for (Coord c : Visit.grid(0, 0, NS, NS, 1)) {
      s.saveObject(baseClaims[c.x][c.y]);
    }
  }
  
  
  
  /**  Methods for querying base-ownership:
    */
  //  TODO:  Is there any way to make this more precise?  Based off the danger-
  //  map, say?
  public Base baseClaiming(int tX, int tY) {
    final StagePatch s = world.patches.patchAt(tX, tY);
    return baseClaims[s.x][s.y];
  }
  
  
  public Base baseClaiming(Tile t) {
    return baseClaiming(t.x, t.y);
  }
  
  
  public Base baseClaiming(Target t) {
    return baseClaiming(t.world().tileAt(t));
  }
  
  
  public boolean inWilds(Target t) {
    final Base claims = baseClaiming(t);
    return claims == null || claims.isPrimal();
  }
  
  
  
  /**  Public methods for querying and asserting area-specific claims.
    */
  public Venue[] venuesClaiming(Box2D area) {
    final Batch <Venue> venues = new Batch <Venue> ();
    
    for (StagePatch s : world.patches.patchesUnder(area, 0)) {
      final List <Claim> claims = areaClaims[s.x][s.y];
      if (claims != null) for (Claim claim : claims) {
        if (! claim.area.overlaps(area)) continue;
        venues.add(claim.owner);
      }
    }
    return venues.toArray(Venue.class);
  }
  
  
  public boolean venueClaims(Box2D area) {
    return venuesClaiming(area).length > 0;
  }
  
  
  public Venue[] venuesConflicting(Venue owner) {
    return venuesConflicting(owner.areaClaimed(), owner);
  }
  
  
  public Venue[] venuesConflicting(Box2D newClaim, Venue owner) {
    final Series <Claim> against = claimsConflicting(newClaim, owner);
    final Venue conflict[] = new Venue[against.size()];
    int n = 0;
    for (Claim c : against) conflict[n++] = c.owner;
    return conflict;
  }
  
  
  private Series <Claim> claimsConflicting(Box2D area, Venue owner) {
    final boolean report = verbose && owner.owningTier() > Owner.TIER_PRIVATE;
    //boolean report = I.used60Frames && PlacingTask.isBeingPlaced(owner);
    
    final Batch <Claim> conflict = new Batch <Claim> ();
    if (report) {
      I.say("\nChecking for conflicts with claim by "+owner+"...");
      I.say("  Area checked: "+area);
    }
    //
    //  We pass over every stage-region that might intersect with the area
    //  being claimed, and check to see if other claims are registered there.
    for (StagePatch s : world.patches.patchesUnder(area, UNIT_GRID_SIZE)) {
      final List <Claim> claims = areaClaims[s.x][s.y];
      if (report) I.say("  Checking region: "+s.area);
      if (claims != null) for (Claim claim : claims) {
        //
        //  Anything previously processed (or one's self) can be skipped...
        if (report) I.say("  Potential conflict: "+claim.owner);
        final Venue other = claim.owner;
        if (claim.flag > 0 || other == owner) continue;
        //
        //  Zoned structures need a minimum spacing around their perimeter,
        //  while others can be placed adjacent.
        final int margin = SiteUtils.minSpacing(owner, other);
        if (claim.area.axisDistance(area) >= margin) continue;
        //
        //  However, venues might or might not clash with eachother, even if
        //  within another's claim.
        final boolean
          ownerClash = owner.preventsClaimBy(other),
          otherClash = other.preventsClaimBy(owner),
          clash      = ownerClash || otherClash;
        if (! clash) continue;
        //
        //  Failing that, flag the conflict and continue.
        if (report) {
          I.say("  CONFLICTS WITH: "+other);
          I.say("    Own clash?   "+ownerClash);
          I.say("    Other clash? "+otherClash);
          I.say("    Footprint:   "+other.footprint());
        }
        conflict.add(claim);
        claim.flag = 1;
      }
    }
    //
    //  Clean up and return the results.
    for (Claim c : conflict) c.flag = -1;
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
    
    for (StagePatch s : world.patches.patchesUnder(area, 0)) {
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
    for (StagePatch s : world.patches.patchesUnder(claim.area, 0)) {
      final List <Claim> claims = areaClaims[s.x][s.y];
      claims.remove(claim);
      if (claims.size() == 0) areaClaims[s.x][s.y] = null;
      if (report) I.say("  Un-registering in section: "+s);
    }
  }
  
  
  public void clearAllClaims() {
    venueClaims.clear();
    final int NS = world.size / world.patches.resolution;
    for (Coord c : Visit.grid(0, 0, NS, NS, 1)) areaClaims[c.x][c.y] = null;
  }
  
  
  
  /**  Methods for dynamically resizing new claims-
    */
  //  TODO:  Move to the ClaimDivision class?
  
  public Box2D findBestClaim(
    Venue venue, int minClaimSize, int maxClaimSize
  ) {
    //
    //  Ensure proper grid alignment...
    minClaimSize = Nums.round(minClaimSize, UNIT_GRID_SIZE, true );
    maxClaimSize = Nums.round(maxClaimSize, UNIT_GRID_SIZE, false);
    //
    //  Initially, we set the claimed area to at least double the maximum area,
    //  and then crop to avoid overlapping any neighbouring claims.
    final Box2D claim = new Box2D();
    claim.setTo(venue.footprint());
    claim.xdim(maxClaimSize);
    claim.ydim(maxClaimSize);
    //
    //  We also need to ensure the venue's entrance faces *outside* of the
    //  claimed area.
    final int facing = venue.facing();
    if (facing == Venue.FACE_NORTH || facing == Venue.FACE_WEST) {
      claim.xpos(0 - maxClaimSize);
    }
    if (facing == Venue.FACE_EAST || facing == Venue.FACE_NORTH) {
      claim.ypos(0 - maxClaimSize);
    }
    
    cropNewClaim(venue, claim);
    claim.include(venue.footprint());
    //
    //  In the event that the claim is still too large, we crop incrementally
    //  toward the central venue.
    final Vec2D centre = venue.footprint().centre();
    final Box2D crops = new Box2D();
    
    while (true) {
      final int maxSide = (int) claim.maxSide();
      final boolean perimOkay = SiteUtils.pathingOkayAround(
        venue, claim, venue.owningTier(), world
      );
      if (perimOkay && maxSide <= maxClaimSize) break;
      
      crops.setTo(claim);
      if (crops.xdim() >= crops.ydim()) {
        if (
          Nums.abs(centre.x - crops.xmax()) <
          Nums.abs(centre.x - crops.xpos())
        ) crops.incX(UNIT_GRID_SIZE);
        crops.incWide(0 - UNIT_GRID_SIZE);
      }
      else {
        if (
          Nums.abs(centre.y - crops.ymax()) <
          Nums.abs(centre.y - crops.ypos())
        ) crops.incY(UNIT_GRID_SIZE);
        crops.incHigh(0 - UNIT_GRID_SIZE);
      }
      if (! venue.footprint().containedBy(crops)) break;
      claim.setTo(crops);
    }
    return claim;
  }
  
  
  private Box2D cropNewClaim(final Venue centre, Box2D original) {
    final Box2D cropped = new Box2D(original);
    cropped.cropBy(world.area());
    final Vec2D vecC = centre.footprint().centre();
    
    final boolean report =
      I.used60Frames && PlacingTask.isBeingPlaced(centre) && verbose
    ;
    if (report) I.say("\nCropping claim for "+centre);
    
    //
    //  First, resolve any conflicts with other venues.
    final List <Claim> conflicts = new List <Claim> () {
      protected float queuePriority(Claim r) {
        if (r.flag < 0) r.flag = Spacing.distance(centre, r.owner);
        return r.flag;
      }
    };
    Visit.appendTo(conflicts, claimsConflicting(original, centre));
    conflicts.queueSort();
    for (Claim c : conflicts) {
      final int margin = SiteUtils.minSpacing(centre, c.owner);
      if (report) I.say("  Margin for "+c.owner+" is "+margin);
      cropToExclude(cropped, c.area, vecC, margin);
    }
    for (Claim c : conflicts) c.flag = -1;
    
    //*
    //
    //  Then, resolve any conflicts with individual tiles-
    final List <Tile> tileClash = new List <Tile> () {
      protected float queuePriority(Tile t) {
        return Spacing.distance(t, centre);
      }
    };
    SiteUtils.checkAreaClear(cropped, world, centre, Account.NONE, tileClash);
    tileClash.queueSort();
    final Box2D tempB = new Box2D();
    for (Tile t : tileClash) {
      if (report) I.say("  Clash with tile at: "+t.x+"|"+t.y);
      t.area(tempB);
      tempB.expandToUnit(2);
      cropToExclude(cropped, tempB, vecC, 0);
    }
    //*/
    
    if (report) I.say("Final area is: "+cropped);
    return original.setTo(cropped);
  }
  
  
  private boolean cropToExclude(
    Box2D cropped, Box2D blocks, Vec2D centre, int margin
  ) {
    if (cropped.axisDistance(blocks) >= margin) return true;
    final Vec2D cB = blocks.centre(), dir = cB.sub(centre, null);
    final float absX = Nums.abs(dir.x), absY = Nums.abs(dir.y);
    //
    //  Is the blockage above?
    if (dir.y >= absX) {
      cropped.ymax(blocks.ypos() - margin);
    }
    //
    //  Is the blockage below?
    if (dir.y < 0 - absX) {
      final float limitY = blocks.ymax() + margin;
      cropped.setY(limitY, cropped.ymax() - limitY);
    }
    //
    //  Is the blockage to the right?
    if (dir.x >= absY) {
      cropped.xmax(blocks.xpos() - margin);
    }
    //
    //  Is the blockage to the left?
    if (dir.x < 0 - absY) {
      final float limitX = blocks.xmax() + margin;
      cropped.setX(limitX, cropped.xmax() - limitX);
    }
    return true;
  }
}



