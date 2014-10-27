


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.tactical.IntelMap;
import stratos.util.*;



public class Exploring extends Plan implements Qualities {
  
  
  /**  Construction and save/load methods-
    */
  private static boolean
    evalVerbose = false,
    pickVerbose = false;
  
  final static int
    TYPE_WANDER  = 0,
    TYPE_EXPLORE = 1,
    TYPE_SURVEY  = 2;
  
  final Base base;
  final int  type;
  
  private Tile    lookedAt ;
  private float   travelled;
  //private boolean sample   ;
  
  
  private Exploring(
    Actor actor, Base base, int type, Tile lookedAt, float maxTravel
  ) {
    super(actor, lookedAt, false);
    this.base        = base     ;
    this.type        = type     ;
    this.lookedAt    = lookedAt ;
    this.travelled   = maxTravel;
  }
  
  
  public Exploring(Session s) throws Exception {
    super(s);
    base = (Base) s.loadObject();
    type = s.loadInt();
    lookedAt  = (Tile) s.loadTarget();
    travelled = s.loadFloat();
    //sample    = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(base);
    s.saveInt   (type);
    s.saveTarget(lookedAt );
    s.saveFloat (travelled);
    //s.saveBool  (sample   );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Exploring(other, base, type, lookedAt, travelled);
  }
  
  
  public static Exploring nextWandering(Actor actor) {
    final float range = actor.health.sightRange() * 2;
    Tile picked = Spacing.pickRandomTile(actor, range, actor.world());
    picked = Spacing.nearestOpenTile(picked, picked);
    if (picked == null) return null;
    return new Exploring(actor, actor.base(), TYPE_WANDER, picked, range);
  }
  
  
  public static Exploring nextExploration(Actor actor) {
    final Tile toExplore = getUnexplored(
      actor.base().intelMap, actor, actor, World.SECTOR_SIZE, -1
    );
    if (toExplore == null) return null;
    final float range = actor.health.sightRange();
    return new Exploring(actor, actor.base(), TYPE_EXPLORE, toExplore, range);
  }
  
  
  public static Exploring nextSurvey(
    Base base, Actor actor, Target point, float range
  ) {
    final Tile core = Spacing.nearestOpenTile(point, point);
    final Tile toExplore = (range > 0) ? getUnexplored(
      base.intelMap, actor, point, World.SECTOR_SIZE, range
    ) : core;
    if (toExplore == null) return null;
    
    final Exploring e = new Exploring(actor, base, TYPE_SURVEY, core, 0);
    e.lookedAt = toExplore;
    return e;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Skill BASE_SKILLS[] = { SURVEILLANCE, ATHLETICS };
  final static Trait BASE_TRAITS[] = { CURIOUS, ENERGETIC, NATURALIST };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    float basePriority = CASUAL;
    if (type == TYPE_WANDER) basePriority = IDLE   ;
    if (type == TYPE_SURVEY) basePriority = ROUTINE;
    
    //
    //  Make this less attractive as you get further from home/safety.
    final Target haven = actor.senses.haven();
    float distFactor = (haven == null) ? 0 : Plan.rangePenalty(haven, actor);
    
    final float priority = priorityForActorWith(
      actor, lookedAt,
      basePriority * Planet.dayValue(actor.world()), 0 - distFactor * 2,
      NO_HARM, MILD_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      HEAVY_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (travelled < 0) return null;
    
    final Action looking = new Action(
      actor, lookedAt,
      this, "actionLook",
      Action.LOOK, "Looking at "+lookedAt.habitat().name
    );
    if (lookedAt != subject()) looking.setProperties(Action.RANGED);
    return looking;
  }
  
  
  public boolean actionLook(Actor actor, Tile point) {
    final IntelMap map = base.intelMap;
    map.liftFogAround(point, actor.health.sightRange());
    
    if (travelled > 0) {
      final float range = actor.health.sightRange();
      lookedAt = Exploring.getUnexplored(
        base.intelMap, actor, actor, range, range
      );
      if (lookedAt == null) { lookedAt = actor.origin(); travelled = -1; }
      else travelled -= Spacing.distance(actor, lookedAt) * Rand.num();
    }
    else travelled = -1;
    return false;
  }
  
  
  
  /**  Helper methods for grabbing unexplored tiles-
    */
  public static Tile getUnexplored(
    IntelMap intelMap, Object client,
    Target centre, float distanceUnit, final float maxDist
  ) {
    final boolean report = pickVerbose && I.talkAbout == client;
    if (report) {
      I.say("\nGetting next unexplored area near "+centre);
      I.say("  Max. distance is: "+maxDist+", units: "+distanceUnit);
    }
    //
    //  Rather than searching exhaustively over every tile in the world, we're
    //  going to split it recursively into sub-sections and check the fog-
    //  density of each.
    final class Area extends Box2D {
      float rating;
    }
    final Sorting <Area> sorting = new Sorting <Area> () {
      public int compare(Area a, Area b) {
        if (a == b) return 0;
        return a.rating < b.rating ? -1 : 1;
      }
    };
    final MipMap map = intelMap.fogMap();
    final World world = centre.world();
    Tile picked = null;
    final Area kids[] = new Area[4];
    final Vec3D
      centrePos = centre.position(null),
      clientPos = Spacing.getPositionWithDefault(client, centrePos);
    //
    //  The complication here (which necessitates a sorted agenda), is that
    //  parent nodes can, in theory, have positive ratings even when none of
    //  their children are suitable (because fog is measured in aggregate, but
    //  the only unexplored children may lie outside the maximum distance.)  So
    //  we need the ability to retrace our steps back to ancestor areas if none
    //  of the children prove suitable.
    boolean init = true; while (init || sorting.size() > 0) {
      //
      //  We begin the search with the 'root node', covering the world as a
      //  whole.  After that, we simply take the highest-ranking descendants.
      final Area best;
      if (init) {
        best = new Area();
        best.setTo(world.area());
        init = false;
      }
      else best = sorting.removeGreatest();
      //
      //  If we've narrowed the area down to a single tile, just return that.
      if (best.xdim() < 2) {
        picked = world.tileAt(best.xpos() + 0.5f, best.ypos() + 0.5f);
        break;
      }
      //
      //  Otherwise, get the sub-quadrants of this section, and rate each of
      //  those that fit within the maximum distance.
      kids[0] = (Area) new Area().asQuadrant(best, 2, 0, 0);
      kids[1] = (Area) new Area().asQuadrant(best, 2, 1, 0);
      kids[2] = (Area) new Area().asQuadrant(best, 2, 0, 1);
      kids[3] = (Area) new Area().asQuadrant(best, 2, 1, 1);
      for (Area kid : kids) {
        final float centreDist = kid.distance(centrePos.x, centrePos.y);
        if (maxDist > 0 && centreDist >= maxDist) continue;
        final int size = (int) kid.xdim();
        final float fog = map.getAvgAt(
          (int) ((kid.xpos() + 1) / size),
          (int) ((kid.ypos() + 1) / size),
          MipMap.sizeToDepth(size)
        );
        if (fog >= 1) continue;
        //
        //  Having skipped any fully-explored areas outside of maximum range,
        //  we can add the remainder to the agenda, and add some randomness
        //  for spice.
        final float clientDist = kid.distance(clientPos.x, clientPos.y);
        kid.rating = (1 - fog) * distanceUnit / (clientDist + distanceUnit);
        kid.rating *= (1.5f + Rand.num()) / 2f;
        sorting.add(kid);
        if (report) {
          I.say("  X/Y:    "+kid.xpos()+"/"+kid.ypos()+", size "+size);
          I.say("  Rating: "+kid.rating);
        }
      }
    }
    return picked;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_WANDER) {
      d.append("Wandering");
    }
    if (type == TYPE_EXPLORE) {
      d.append("Exploring ");
      d.append(lookedAt.habitat().name);
    }
    if (type == TYPE_SURVEY) {
      d.append("Surveying ");
      d.append(lookedAt);
    }
  }
  
}




    /*
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
        if (level == 0 || (maxDist > 0 && (distance - s) > maxDist)) continue;
        
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
    //*/

/*
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
//*/


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
