


package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.TileSpread;
import stratos.game.common.*;
import stratos.game.common.WorldSections.Section;
import stratos.game.planet.*;
import stratos.user.*;
import stratos.util.*;



public class Exploring extends Plan implements Abilities {
  
  
  /**  Construction and save/load methods-
    */
  final Base base ;
  private Tile lookedAt ;
  
  
  public static Exploring nextExplorationFor(Actor actor) {
    Tile toExplore = Exploring.getUnexplored(actor.base().intelMap, actor) ;
    if (toExplore == null) return null ;
    return new Exploring(actor, actor.base(), toExplore) ;
  }
  
  
  public Exploring(Actor actor, Base base, Tile lookedAt) {
    super(actor) ;
    this.base = base ;
    this.lookedAt = lookedAt ;
  }
  
  
  public Exploring(Session s) throws Exception {
    super(s) ;
    base = (Base) s.loadObject() ;
    lookedAt = (Tile) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(base) ;
    s.saveTarget(lookedAt) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    final float p = rateExplorePoint(actor, lookedAt, priorityMod) ;
    ///if (BaseUI.isPicked(actor))
    ///I.say("PRIORITY FOR EXPLORATION: "+p) ;
    //
    //  TODO:  Increase priority based on amount of map left unexplored.
    return p ;
  }
  
  
  public static float rateExplorePoint(
    Actor actor, Tile point, float winReward
  ) {
    float impetus = winReward ;
    
    impetus += actor.traits.traitLevel(NATURALIST) / 2f ;
    impetus += actor.traits.traitLevel(INQUISITIVE) / 2f ;
    impetus -= actor.traits.traitLevel(INDOLENT) ;
    impetus += actor.traits.traitLevel(SURVEILLANCE) / 10f ;
    
    impetus -= Plan.rangePenalty(actor, point) ;
    impetus -= Plan.dangerPenalty(point, actor) ;
    impetus *= actor.health.sightRange() * 2 / ActorHealth.DEFAULT_SIGHT ;
    
    return impetus ;
  }
  
  
  static Tile[] grabExploreArea(
    final IntelMap intelMap, final Tile point, final float radius
  ) {
    //
    //  Firstly, we grab all contiguous nearby tiles.
    final TileSpread spread = new TileSpread(point) {
      
      protected boolean canAccess(Tile t) {
        return Spacing.distance(t,  point) < radius ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        return false ;
      }
    } ;
    spread.doSearch() ;
    //
    //  As a final touch, we sort and return these tiles in random order.
    final List <Tile> sorting = new List <Tile> () {
      protected float queuePriority(Tile r) {
        return (Float) r.flaggedWith() ;
      }
    } ;
    for (Tile t : spread.allSearched(Tile.class)) {
      t.flagWith(Rand.num()) ;
      sorting.add(t) ;
    }
    sorting.queueSort() ;
    for (Tile t : sorting) t.flagWith(null) ;
    return (Tile[]) sorting.toArray(Tile.class) ;
  }
  
  
  public static Tile getUnexplored(
    IntelMap intelMap, Target target
  ) {
    //
    //  TODO:  Restrict this to within a given Box2D area if possible.
    
    
    final Vec3D pos = target.position(null) ;
    final MipMap map = intelMap.fogMap() ;
    int high = map.high() + 1, x = 0, y = 0, kX, kY ;
    final Coord kids[] = new Coord[] {
      new Coord(0, 0), new Coord(0, 1),
      new Coord(1, 0), new Coord(1, 1)
    } ;
    float mX, mY, rating = 0 ;
    //
    //  Work your way down from the topmost sections, picking the most
    //  appealing child at each point.
    while (high-- > 1) {
      final float s = 1 << high ;
      Coord picked = null ;
      float bestRating = 0 ;
      for (int i = 4 ; i-- > 0 ;) {
        //
        //  We calculate the coordinates for each child-section, both within
        //  the mip-map, and in terms of world-coordinates midpoint, and skip
        //  over anything outside the supplied bounds.
        final Coord c = kids[i] ;
        kX = (x * 2) + c.x ;
        kY = (y * 2) + c.y ;
        mX = (kX + 0.5f) * s ;
        mY = (kY + 0.5f) * s ;
        //
        //  Otherwise, favour closer areas that are partially unexplored.
        final float level = map.getAvgAt(kX, kY, high - 1) < 1 ? 1 : 0 ;
        final float distance = pos.distance(mX, mY, 0) ;
        rating = level * Rand.avgNums(2) ;
        rating /= 1 + (distance / World.SECTOR_SIZE) ;
        if (rating > bestRating) { picked = c ; bestRating = rating ; }
      }
      if (picked == null) return null ;
      x = (x * 2) + picked.x ;
      y = (y * 2) + picked.y ;
    }
    
    final Tile looks = intelMap.world().tileAt(x, y) ;
    if (intelMap.fogAt(looks) == 1) return null ;
    if (looks.blocked()) return Spacing.nearestOpenTile(looks, target) ;
    else return looks ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    //
    //  TODO:  Consider grabbing another nearby spot.
    if (actor.base().intelMap.fogAt(lookedAt) == 1) {
      ///I.say("TILE ALREADY EXPLORED!") ;
      return null ;
    }
    final Action looking = new Action(
      actor, lookedAt,
      this, "actionLook",
      Action.LOOK, "Looking at "+lookedAt.habitat().name
    ) ;
    looking.setProperties(Action.RANGED) ;
    return looking ;
  }
  
  
  public boolean actionLook(Actor actor, Tile point) {
    //  TODO:  Check for mission-completion here?
    final IntelMap map = base.intelMap ;
    map.liftFogAround(point, actor.health.sightRange() * 1.414f) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Exploring") ;
    d.append(" "+lookedAt.habitat().name) ;
  }
  
}



