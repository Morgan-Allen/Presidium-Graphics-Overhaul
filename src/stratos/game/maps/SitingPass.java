/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;


public class SitingPass {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static int
    STAGE_INIT        = -1,
    STAGE_REGION_SORT =  0,
    STAGE_TILES_SORT  =  1,
    STAGE_PLACE_CHECK =  2,
    STAGE_SUCCESS     =  3,
    STAGE_FAILED      =  4;
  
  final public Stage  world ;
  final public Base   base  ;
  final public Siting siting;
  final public Venue  placed;
  
  public float rating = -1;
  
  int stage = STAGE_INIT;
  StageRegion nextRegion;
  Tile        nextTile  ;
  PassList regionSort = new PassList();
  PassList tilesSort  = new PassList();
  Tile picked = null;
  
  
  public SitingPass(Base base, Siting siting, Venue placed) {
    this.world  = base.world;
    this.base   = base      ;
    this.siting = siting    ;
    this.placed = placed    ;
  }
  
  
  public static void savePass(SitingPass pass, Session s) throws Exception {
    
    s.saveObject(pass.base  );
    s.saveObject(pass.siting);
    s.saveObject(pass.placed);
    
    s.saveObject(pass.nextRegion);
    s.saveObject(pass.nextTile  );
    s.saveInt(pass.regionSort.size());
    for (PassEntry e : pass.regionSort) {
      s.saveTarget(e.forPass);
      s.saveFloat (e.rating );
    }
    s.saveInt(pass.tilesSort .size());
    for (PassEntry e : pass.tilesSort ) {
      s.saveTarget(e.forPass);
      s.saveFloat (e.rating );
    }
    
    s.saveInt   (pass.stage );
    s.saveObject(pass.picked);
    s.saveFloat (pass.rating);
  }
  
  
  public static SitingPass loadPass(Session s) throws Exception {
    final Base       base   = (Base  ) s.loadObject();
    final Siting     siting = (Siting) s.loadObject();
    final Venue      placed = (Venue ) s.loadObject();
    final SitingPass pass   = new SitingPass(base, siting, placed);
    
    pass.nextRegion = (StageRegion) s.loadObject();
    pass.nextTile   = (Tile       ) s.loadObject();
    for (int i = s.loadInt(); i-- > 0;) {
      pass.regionSort.addFromPass(s.loadTarget(), s.loadFloat());
    }
    for (int i = s.loadInt(); i-- > 0;) {
      pass.tilesSort.addFromPass (s.loadTarget(), s.loadFloat());
    }
    
    pass.stage  = s.loadInt();
    pass.picked = (Tile) s.loadObject();
    pass.rating = s.loadFloat();
    
    return pass;
  }
  
  
  /**  Some utility classes to help support saveable state...
    */
  private static class PassEntry {
    private float rating;
    private Target forPass;
  }
  
  
  private static class PassList extends List <PassEntry> {
    
    void addFromPass(Target t, float rating) {
      if (t == null || rating <= 0) return;
      final PassEntry entry = new PassEntry();
      entry.rating  = rating;
      entry.forPass = t;
      super.add(entry);
    }
    
    protected float queuePriority(PassEntry r) {
      return r.rating;
    }
    
    Target nextForPass() {
      final PassEntry next = (PassEntry) this.removeLast();
      return next == null ? null : next.forPass;
    }
  }
  
  
  
  /**  External usage-calls subsequent to setup, methods to override, and final
    *  state-queries.
    */
  public void performFullPass() {
    performSteps(-1);
  }
  
  
  public void performPassFraction(float fraction) {
    final int
      numTiles = world.size * world.size,
      grid     = world.sections.resolution,
      maxSteps = (numTiles * 2) + (numTiles / (grid * grid));
    performSteps(Nums.round(maxSteps * fraction, 1, true));
  }
  
  
  protected float ratePlacing(Target point, boolean exact) {
    return siting.ratePointDemand(base, point, exact);
  }
  
  
  protected boolean canPlaceAt(Tile best, int facing, Account reasons) {
    if (! placed.setupWith(best, null)) return false;
    placed.setFacing(facing);
    return placed.canPlace(reasons);
  }
  
  
  public boolean success() {
    return stage == STAGE_SUCCESS;
  }
  
  
  public boolean complete() {
    return stage >= STAGE_SUCCESS;
  }
  
  
  public Tile pointPicked() {
    return picked;
  }
  
  
  
  /**  Time-slicing methods to ensure good use of CPU time-
    */
  void performSteps(final int stepLimit) {
    int stepsDone = 0;
    while (! complete()) {
      if (stepLimit > 0 && stepsDone >= stepLimit) break;
      if (stepLimit > 0 && world.schedule.timeUp()) break;
      
      if (stage <= STAGE_REGION_SORT) addRegionToSort();
      if (stage == STAGE_TILES_SORT ) addTileToSort  ();
      if (stage == STAGE_PLACE_CHECK) doPlaceCheck   ();
      stepsDone++;
    }
  }
  
  
  boolean addRegionToSort() {
    if (nextRegion == null) nextRegion = world.sections.sectionAt(0, 0);
    else {
      final int stepSize = world.sections.resolution;
      int x = nextRegion.absX + stepSize, y = nextRegion.absY;
      if (x >= world.size) { x -= world.size; y += stepSize; }
      if (y >= world.size) {
        stage = STAGE_TILES_SORT;
        regionSort.queueSort();
        return false;
      }
      nextRegion = world.sections.sectionAt(x, y);
    }
    
    regionSort.addFromPass(nextRegion, ratePlacing(nextRegion, false));
    return true;
  }
  
  
  boolean addTileToSort() {
    final StageRegion r = nextRegion;
    if (r == null) return false;
    
    if (nextTile == null) {
      I.say("  Have begun tile-sort for "+r);
      nextTile = world.tileAt(r.absX, r.absY);
    }
    else {
      final int stepSize = Stage.UNIT_GRID_SIZE;
      int x = nextTile.x + stepSize, y = nextTile.y;
      if (x >= r.absX + r.size) { x -= r.size; y += stepSize; }
      if (y >= r.absY + r.size) {
        stage = STAGE_PLACE_CHECK;
        tilesSort.queueSort();
        I.say("Moving on to place check at "+r);
        return false;
      }
      nextTile = world.tileAt(x, y);
    }
    
    tilesSort.addFromPass(nextTile, ratePlacing(nextTile, true));
    return true;
  }
  
  
  boolean doPlaceCheck() {
    final Tile best = (Tile) tilesSort.nextForPass();
    if (best == null) {
      nextRegion = (StageRegion) regionSort.nextForPass();
      nextTile   = null;
      if (nextRegion == null) {
        stage = STAGE_FAILED;
        return false;
      }
      else {
        stage = STAGE_TILES_SORT;
        return false;
      }
    }
    
    
    //
    //  TODO:  Try out multiple facings here, if entrance-blockage was a
    //  problem...
    if (canPlaceAt(best, 0, Account.NONE)) {
      picked = best;
      stage = STAGE_SUCCESS;
      return true;
    }
    return false;
  }
}



