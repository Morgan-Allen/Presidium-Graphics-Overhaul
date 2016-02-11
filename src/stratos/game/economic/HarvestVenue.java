/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.util.*;



public abstract class HarvestVenue extends Venue {
  
  
  /**  Data fields, constructors, and save/load methods.
    */
  private static boolean
    verbose = false;
  
  final int minClaimSize, maxClaimSize;
  private ClaimDivision division = ClaimDivision.NONE;
  private Box2D areaClaimed = new Box2D();
  private float needsTending = 0;
  
  
  
  public HarvestVenue(
    Blueprint blueprint, Base base, int minClaim, int maxClaim
  ) {
    super(blueprint, base);
    this.minClaimSize = minClaim;
    this.maxClaimSize = maxClaim;
  }
  
  
  public HarvestVenue(Session s) throws Exception {
    super(s);
    this.minClaimSize = s.loadInt();
    this.maxClaimSize = s.loadInt();
    this.areaClaimed.loadFrom(s.input());
    this.division = ClaimDivision.loadFrom(s);
    this.needsTending = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(minClaimSize);
    s.saveInt(maxClaimSize);
    areaClaimed.saveTo(s.output());
    ClaimDivision.saveTo(s, division);
    s.saveFloat(needsTending);
  }
  
  
  
  
  /**  Initial placement, claims-setup and life-cycle.
    */
  public void doPlacement(boolean intact) {
    if (division == ClaimDivision.NONE) division = updateDivision();
    super.doPlacement(intact);
    for (Tile t : division.reserved()) t.setReserves(this, false);
  }
  
  
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    if (area == null || area.maxSide() <= Stage.UNIT_GRID_SIZE) {
      final Box2D claim = position.world.claims.findBestClaim(
        this, minClaimSize, maxClaimSize
      );
      if (claim == null) return false;
      this.areaClaimed.setTo(claim);
    }
    else {
      this.areaClaimed.setTo(area);
    }
    return true;
  }
  
  
  public boolean canPlace(Account reasons) {
    if (! super.canPlace(reasons)) {
      return false;
    }
    final Stage world = origin().world;
    if (! SiteUtils.checkAreaClear(areaClaimed, world, this, reasons, null)) {
      return false;
    }
    if (areaClaimed.maxSide() > maxClaimSize) {
      return reasons.setFailure("Area is too large!");
    }
    if (! footprint().containedBy(areaClaimed)) {
      return reasons.setFailure("Area is too small!");
    }
    if (areaClaimed.maxSide() < minClaimSize) {
      return reasons.setFailure("Area is too small!");
    }
    return true;
  }
  
  
  protected ClaimDivision updateDivision() {
    return ClaimDivision.forArea(this, areaClaimed, facing(), 3, 4, this);
  }
  
  
  public Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  public Tile[] reserved() {
    if (! inWorld()) division = updateDivision();
    return division.reserved();
  }
  
  
  public void exitWorld() {
    for (Tile t : division.reserved()) t.setReserves(null, false);
    super.exitWorld();
  }
  
  
  public ClaimDivision claimDivision() {
    return division;
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    return true;
  }
  
  
  
  /**  Regular updates-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    if (numUpdates % 10 == 0) checkTendStates();
  }
  
  
  protected void updatePaving(boolean inWorld) {
    //base.transport.updatePerimeter(this, inWorld);
    //*
    final Tile toPave[] = division.toPaveAround(this, null);
    if (toPave == null || toPave.length == 0) {
      base.transport.updatePerimeter(this, inWorld);
      base.transport.updateJunction(this, origin(), inWorld);
    }
    else {
      base.transport.updatePerimeter(this, inWorld, toPave);
    }
    //*/
  }
  
  
  protected void checkTendStates() {
    final boolean report = verbose && I.talkAbout == this;
    
    final Tile tended[] = division.reserved();
    if (Visit.empty(tended)) {
      if (report) I.say("\nNO TILES TO CHECK");
      needsTending = 0;
      return;
    }
    
    if (report) I.say("\nCHECKING TILES TO TEND");
    needsTending = 0;
    for (Tile t : tended) {
      if (needsTending(t)) needsTending++;
    }
    
    if (report) I.say("NEEDS TENDING: "+needsTending);
    needsTending /= tended.length;
  }
  
  
  public float needForTending(ResourceTending tending) {
    return needsTending;
  }
  
  
  public Tile[] getHarvestTiles(ResourceTending tending) {
    return division.reserved();
  }
  
  
  protected abstract boolean needsTending(Tile t);
  
}











