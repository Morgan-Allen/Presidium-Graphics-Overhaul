/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;


//
//  I need the following function-implementations for a Siting...
//
//  1.  An overall rating of demand for a given venue within the settlement as
//      a whole.
//  2.  A measure of the degree to which an existing venue can satisfy such
//      demands at it's current location.
//  3.  Ratings for the suitability of siting within broad areas.
//  4.  Ratings for the suitability of siting at exact locations.
//  5.  The ability to time-slice these operations (and even save them.)
//  6.  The ability to customise or override these methods for use in once-off
//      placements or unique structure-types.

public abstract class Siting extends Constant {
  
  
  final static Index <Siting> INDEX = new Index();
  
  final Blueprint blueprint;
  
  
  public Siting(Blueprint origin) {
    super(INDEX, "siting_"+origin.keyID, origin.name+" (Siting)");
    this.blueprint = origin;
  }
  
  
  public Siting loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public void describeHelp(Description d, Selectable prior) {
    d.append(name);
  }






  abstract float rateSettlementDemand(Base base);
  abstract float ratePointDemand(Base base, Target point, boolean exact);
  
  abstract float estimateSupply(Base base, Traded needed, Conversion with);
  abstract boolean canPlaceAt(Tile t, int facing, Account reasons);
  
  
  
  
  public static class Pass {
    
    final static int
      STAGE_INIT        = -1,
      STAGE_REGION_SORT =  0,
      STAGE_TILES_SORT  =  1,
      STAGE_PLACE_CHECK =  2,
      STAGE_SUCCESS     =  3,
      STAGE_FAILED      =  4;
    
    final Stage  world ;
    final Base   base  ;
    final Siting siting;
    
    int stage = STAGE_INIT;
    StageRegion nextRegion;
    Tile        nextTile  ;
    PassList regionSort = new PassList();
    PassList tilesSort  = new PassList();
    Tile picked = null;
    
    
    public Pass(Base base, Siting siting) {
      this.world  = base.world;
      this.base   = base;
      this.siting = siting;
    }
    
    
    public void resetPass() {
      stage = STAGE_INIT;
      nextRegion = null;
      nextTile   = null;
      regionSort.clear();
    }
    
    
    public void performFullPass() {
      performSteps(-1);
    }
    
    
    public void performPassFraction(float fraction) {
      final int
        numTiles = world.size * world.size,
        grid     = world.sections.resolution,
        maxSteps = (numTiles * 2) + (numTiles / (grid * grid));
      performSteps((int) (maxSteps * fraction));
    }
    
    
    void performSteps(final int stepLimit) {
      int stepsDone = 0;
      while (true) {
        if (stepLimit > 0 && stepsDone >= stepLimit) break;
        if (stepLimit > 0 && world.schedule.timeUp()) break;
        if (stage >= STAGE_SUCCESS) break;
        
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
      
      if (nextTile == null) nextTile = world.tileAt(r.absX, r.absY);
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
      
      tilesSort.addFromPass(nextTile, ratePlacing(nextTile, true));
      return true;
    }
    
    
    boolean doPlaceCheck() {
      final Tile best = (Tile) tilesSort.nextForPass();
      if (best == null) {
        nextRegion = (StageRegion) regionSort.nextForPass();
        if (nextRegion == null) {
          stage = STAGE_FAILED;
          return false;
        }
        else {
          stage = STAGE_TILES_SORT;
          return false;
        }
      }
      
      //  TODO:  You could perform some fancier evaluations here!  (Also, you
      //  might consider passing in a venue as an argument to the constructor...
      //  which would allow you to dispense with the abstract methods for the
      //  moment.)  Hmm.
      if (canPlaceAt(best, 0, Account.NONE)) {
        picked = best;
        stage = STAGE_SUCCESS;
        return true;
      }
      return false;
    }
    
    
    public boolean success() {
      return stage == STAGE_SUCCESS;
    }
    
    
    protected float ratePlacing(Target point, boolean exact) {
      return siting.ratePointDemand(base, point, exact);
    }
    
    
    protected boolean canPlaceAt(Tile best, int facing, Account reasons) {
      return siting.canPlaceAt(best, facing, reasons);
    }
  }
  
  
  private static class PassEntry extends ListEntry {
    private float rating;
    private Target forPass;
  }
  
  
  private static class PassList extends List <PassEntry> {
    
    void addFromPass(Target t, float rating) {
      if (t == null || rating <= 0) return;
      final PassEntry entry = new PassEntry();
      entry.rating  = rating;
      entry.forPass = t;
      super.appendEntry(entry);
    }
    
    protected float queuePriority(PassEntry r) {
      return r.rating;
    }
    
    Target nextForPass() {
      final PassEntry next = super.removeFirst();
      return next == null ? null : next.forPass;
    }
  };
  

  
  public static void savePass(Pass pass, Session s) throws Exception {
    
    s.saveObject(pass.base  );
    s.saveObject(pass.siting);
    
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
  }
  
  
  public static Pass loadPass(Session s) throws Exception {
    final Base   base   = (Base  ) s.loadObject();
    final Siting siting = (Siting) s.loadObject();
    final Pass   pass   = new Pass(base, siting);
    
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
    
    return pass;
  }
}







