/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;


//  TODO:  If you wanted, you could break this down into successively-finer
//  quadrants, mip-map style...

public class SitingPass {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose        = false,
    regionsVerbose = true ,
    tilesVerbose   = true ;
  
  final public static int
    NO_PLACING    = 0,
    PLACE_RESERVE = 1,
    PLACE_INTACT  = 2;
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
  public int placeState = PLACE_RESERVE;
  public boolean isVerbose = false;

  int stage = STAGE_INIT;
  StageRegion nextRegion;
  Tile        nextTile  ;
  PassList regionSort = new PassList();
  PassList tilesSort  = new PassList();
  Tile picked = null;
  
  
  public SitingPass(Base base, Siting siting) {
    this(base, siting, siting.blueprint.createVenue(base));
  }
  
  
  public SitingPass(Base base, Venue placed) {
    this(base, placed.blueprint.siting(), placed);
  }
  
  
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
    
    s.saveInt   (pass.stage     );
    s.saveObject(pass.picked    );
    s.saveFloat (pass.rating    );
    s.saveInt   (pass.placeState);
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
    
    pass.stage      = s.loadInt  ();
    pass.picked     = (Tile) s.loadObject();
    pass.rating     = s.loadFloat();
    pass.placeState = s.loadInt  ();
    
    return pass;
  }
  
  
  
  /**  Some utility classes to help support saveable state...
    */
  private static class PassEntry {
    private float rating;
    private Target forPass;
    
    public String toString() { return forPass+" ("+rating+")"; }
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
  
  
  protected boolean canPlaceAt(Tile best, Account reasons) {
    if (placed == null) return false;
    if (isVerbose) {
      I.say("Checking placing at "+best);
    }
    
    if (! placed.setupWith(best, null)) return false;
    return placed.canPlace(reasons);
  }
  
  
  protected void doPlacementAt(Tile best, int facing) {
    if (placed == null || ! placed.setupWith(best, null)) return;
    placed.doPlacement(placeState == PLACE_INTACT);
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
    final boolean report = verbose || isVerbose;
    if (report) {
      I.say("\nPerforming siting pass for: "+placed);
      I.say("  Steps allowed: "+stepLimit);
    }
    
    int stepsDone = 0;
    while (! complete()) {
      if (stepLimit > 0 && stepsDone >= stepLimit ) break;
      if (stepLimit > 0 && world.schedule.timeUp()) break;
      
      if (stage <= STAGE_REGION_SORT) addRegionToSort();
      if (stage == STAGE_TILES_SORT ) addTileToSort  ();
      if (stage == STAGE_PLACE_CHECK) doPlaceCheck   ();
      stepsDone++;
    }
  }
  
  
  boolean addRegionToSort() {
    final boolean report = (verbose || isVerbose) && regionsVerbose;
    
    if (nextRegion == null) nextRegion = world.sections.sectionAt(0, 0);
    else {
      final int stepSize = world.sections.resolution;
      int x = nextRegion.absX + stepSize, y = nextRegion.absY;
      if (x >= world.size) { x -= world.size; y += stepSize; }
      if (y >= world.size) {
        regionSort.queueSort();
        nextRegion = (StageRegion) regionSort.nextForPass();
        stage      = (nextRegion != null) ? STAGE_TILES_SORT : STAGE_FAILED;
        return false;
      }
      nextRegion = world.sections.sectionAt(x, y);
    }
    
    final float rating = ratePlacing(nextRegion, false);
    if (report) {
      I.say("  Adding region to sort: "+nextRegion);
      I.say("  Rating is: "+rating);
    }
    regionSort.addFromPass(nextRegion, rating);
    return true;
  }
  
  
  boolean addTileToSort() {
    final StageRegion r = nextRegion;
    
    final boolean report = (verbose || isVerbose) && tilesVerbose;
    
    if (nextTile == null) {
      nextTile = world.tileAt(r.absX, r.absY);
    }
    else {
      final int stepSize = Stage.UNIT_GRID_SIZE;
      int x = nextTile.x + stepSize, y = nextTile.y;
      if (x >= r.absX + r.size) { x -= r.size; y += stepSize; }
      if (y >= r.absY + r.size) {
        stage = STAGE_PLACE_CHECK;
        tilesSort.queueSort();
        return false;
      }
      nextTile = world.tileAt(x, y);
    }
    
    final float rating = ratePlacing(nextTile, false);
    if (report) {
      I.say("  Adding tile to sort: "+nextTile);
      I.say("  Rating is: "+rating);
    }
    
    tilesSort.addFromPass(nextTile, ratePlacing(nextTile, true));
    return true;
  }
  
  
  boolean doPlaceCheck() {
    final boolean report = verbose || isVerbose;
    
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
    
    if (canPlaceAt(best, Account.NONE)) {
      if (report) {
        I.say("  Placement successful!");
        I.say("  Can insert at: "+best+" ("+nextRegion+")");
      }
      picked = best;
      stage = STAGE_SUCCESS;
      if (placeState != NO_PLACING) doPlacementAt(picked, 0);
      return true;
    }
    return false;
  }
}





