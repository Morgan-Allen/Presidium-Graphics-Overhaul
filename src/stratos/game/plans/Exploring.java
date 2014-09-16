


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.tactical.IntelMap;
//import stratos.game.base.SensorPost;
import stratos.util.*;



public class Exploring extends Plan implements Qualities {
  
  
  /**  Construction and save/load methods-
    */
  private static boolean
    evalVerbose = false;
  
  final   Base  base       ;
  private Tile  lookedAt   ;
  private float travelLimit;
  
  
  public static Exploring nextExplorationFor(Actor actor) {
    final Tile toExplore = getUnexplored(
      actor.base().intelMap, actor, World.SECTOR_SIZE, -1
    );
    if (toExplore == null) return null;
    final float travelLimit = actor.health.sightRange();
    
    return new Exploring(actor, actor.base(), toExplore, travelLimit);
  }
  
  
  public Exploring(Actor actor, Base base, Tile lookedAt, float maxTravel) {
    super(actor, lookedAt);
    this.base        = base     ;
    this.lookedAt    = lookedAt ;
    this.travelLimit = maxTravel;
  }
  
  
  public Exploring(Session s) throws Exception {
    super(s);
    base = (Base) s.loadObject();
    lookedAt = (Tile) s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(base);
    s.saveTarget(lookedAt);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Exploring(other, base, lookedAt, 0);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Skill BASE_SKILLS[] = { SURVEILLANCE, ATHLETICS };
  final static Trait BASE_TRAITS[] = { CURIOUS, ENERGETIC, NATURALIST };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final float priority = priorityForActorWith(
      actor, lookedAt, CASUAL * Planet.dayValue(actor.world()),
      NO_HARM, MILD_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, HEAVY_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (actor.base().intelMap.fogAt(lookedAt) == 1) {
      if (travelLimit <= 0) return null;
      
      final float
        range = actor.health.sightRange(),
        maxTravel = range * 2 * (1 + actor.traits.relativeLevel(STUBBORN));
      
      final Tile nextLook = getUnexplored(
        actor.base().intelMap, actor, range, maxTravel + range
      );
      
      if (nextLook == null) return null;
      lookedAt = nextLook;
      travelLimit -= Spacing.distance(actor, lookedAt) * (Rand.num() + 0.5f);
    }
    
    final Action looking = new Action(
      actor, lookedAt,
      this, "actionLook",
      Action.LOOK, "Looking at "+lookedAt.habitat().name
    );
    looking.setProperties(Action.RANGED);
    return looking;
  }
  
  
  public boolean actionLook(Actor actor, Tile point) {
    //  TODO:  Check for mission-completion here?
    final IntelMap map = base.intelMap;
    map.liftFogAround(point, actor.health.sightRange() * 1.414f);
    return true;
  }
  
  
  //  TODO:  Consider integrating these...
  /*
    final SensorPost newPost = SensorPost.locateNewPost(this);
    if (newPost != null) {
      final Action collects = new Action(
        actor, newPost,
        this, "actionCollectSensor",
        Action.REACH_DOWN, "Collecting sensor"
      );
      collects.setMoveTarget(this);
      final Action plants = new Action(
        actor, newPost.origin(),
        this, "actionPlantSensor",
        Action.REACH_DOWN, "Planting sensor"
      );
      plants.setMoveTarget(Spacing.pickFreeTileAround(newPost, actor));
      choice.add(new Steps(actor, this, Plan.ROUTINE, collects, plants));
    }
  
  
  public boolean actionCollectSensor(Actor actor, SensorPost post) {
    actor.gear.addItem(Item.withReference(SAMPLES, post));
    return true;
  }
  
  
  public boolean actionPlantSensor(Actor actor, Tile t) {
    SensorPost post = null;
    for (Item i : actor.gear.matches(SAMPLES)) {
      if (i.refers instanceof SensorPost) {
        post = (SensorPost) i.refers;
        actor.gear.removeItem(i);
      }
    }
    if (post == null) return false;
    post.setPosition(t.x, t.y, world);
    if (! Spacing.perimeterFits(post)) return false;
    post.enterWorld();
    return true;
  }
  //*/
  
  
  
  /**  Helper methods for grabbing unexplored tiles-
    */
  public static Tile[] grabExploreArea(
    final IntelMap intelMap, final Tile point, final float radius
  ) {
    //
    //  Firstly, we grab all contiguous nearby tiles.
    final TileSpread spread = new TileSpread(point) {
      
      protected boolean canAccess(Tile t) {
        return Spacing.distance(t,  point) < radius;
      }
      
      protected boolean canPlaceAt(Tile t) {
        return false;
      }
    };
    spread.doSearch();
    //
    //  As a final touch, we sort and return these tiles in random order.
    final List <Tile> sorting = new List <Tile> () {
      protected float queuePriority(Tile r) {
        return (Float) r.flaggedWith();
      }
    };
    for (Tile t : spread.allSearched(Tile.class)) {
      t.flagWith(Rand.num());
      sorting.add(t);
    }
    sorting.queueSort();
    for (Tile t : sorting) t.flagWith(null);
    return (Tile[]) sorting.toArray(Tile.class);
  }
  
  
  public static Tile getUnexplored(
    IntelMap intelMap, Target target, float distanceUnit, float maxDist
  ) {
    //
    //  TODO:  Restrict this to within a given Box2D area if possible.
    
    
    final Vec3D pos = target.position(null);
    final MipMap map = intelMap.fogMap();
    int high = map.high() + 1, x = 0, y = 0, kX, kY;
    final Coord kids[] = new Coord[] {
      new Coord(0, 0), new Coord(0, 1),
      new Coord(1, 0), new Coord(1, 1)
    };
    float mX, mY, rating = 0;
    //
    //  Work your way down from the topmost sections, picking the most
    //  appealing child at each point.
    while (high-- > 1) {
      final float s = 1 << high;
      Coord picked = null;
      float bestRating = 0;
      for (int i = 4; i-- > 0;) {
        //
        //  We calculate the coordinates for each child-section, both within
        //  the mip-map, and in terms of world-coordinates midpoint, and skip
        //  over anything outside the supplied bounds.
        final Coord c = kids[i];
        kX = (x * 2) + c.x;
        kY = (y * 2) + c.y;
        mX = (kX + 0.5f) * s;
        mY = (kY + 0.5f) * s;
        //
        //  Otherwise, favour closer areas that are partially unexplored.
        final float level = map.getAvgAt(kX, kY, high - 1) < 1 ? 1 : 0;
        final float distance = pos.distance(mX, mY, 0);
        if (level == 0 || (maxDist > 0 && distance > maxDist)) continue;
        
        rating = level * Rand.avgNums(2);
        rating /= 1 + (distance / distanceUnit);
        if (rating > bestRating) { picked = c; bestRating = rating; }
      }
      if (picked == null) return null;
      x = (x * 2) + picked.x;
      y = (y * 2) + picked.y;
    }
    
    final Tile looks = intelMap.world().tileAt(x, y);
    if (intelMap.fogAt(looks) == 1) return null;
    if (looks.blocked()) return Spacing.nearestOpenTile(looks, target);
    else return looks;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Exploring");
    d.append(" "+lookedAt.habitat().name);
  }
  
}



