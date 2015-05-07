/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.util.*;



public class IntelMap {
  
  /**  Field definitions, constructors and save/load methods-
    */
  private static boolean
    fogVerbose  = false,
    pickVerbose = false;
  
  final public static float
    MIN_FOG        = 0,
    MAX_FOG        = 1.5f,
    FOG_DECAY_TIME = Stage.STANDARD_HOUR_LENGTH,
    FOG_SEEN_MIN   = 0.2f;
  
  final Base base;
  
  private Stage      world;
  private float      fogVals[][];
  private MipMap     fogMap;
  private FogOverlay fogOver;
  
  
  
  public IntelMap(Base base) {
    this.base = base;
  }
  
  
  public void initFog(Stage world) {
    this.world = world;
    final int size = world.size;
    fogVals = new float[size][size];
    fogMap  = new MipMap(size);
    fogOver = new FogOverlay(size);
  }
  
  
  public void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      fogVals[c.x][c.y] = s.loadFloat();
    }
    fogMap.loadFrom(s.input());
    fogOver.updateVals(-1, fogVals);
  }
  
  
  public void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      s.saveFloat(fogVals[c.x][c.y]);
    }
    fogMap.saveTo(s.output());
  }
  
  
  public MipMap fogMap() {
    return fogMap;
  }
  
  
  public Stage world() {
    return world;
  }
  
  
  public FogOverlay fogOver() {
    if (GameSettings.fogFree || base.primal) return null;
    return fogOver;
  }
  
  
  
  /**  Visual refreshment-
    */
  public void updateAndRender(float fogTime, Rendering rendering) {
    if (GameSettings.fogFree || base.primal) return;
    fogOver.updateVals(fogTime, fogVals);
    fogOver.registerFor(rendering);
  }
  
  
  public float displayFog(Tile t, Object client) {
    if (GameSettings.fogFree || base.primal) return 1;
    return Nums.clamp(fogOver.sampleAt(t.x, t.y, client), 0, 1);
  }
  
  
  public float displayFog(float x, float y, Object client) {
    if (GameSettings.fogFree || base.primal) return 1;
    return Nums.clamp(fogOver.sampleAt(x, y, client), 0, 1);
  }
  
  
  
  /**  Queries and modifications-
    */
  public void updateFogValues() {
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      
      //  Fog values decay steadily over time, but those that have been
      //  thoroughly explored never decay below a minimum value.
      float val = fogVals[c.x][c.y];
      final boolean seen = val >= FOG_SEEN_MIN;
      val -= 1f / FOG_DECAY_TIME;
      val = Nums.clamp(val, seen ? FOG_SEEN_MIN : MIN_FOG, MAX_FOG);
      fogVals[c.x][c.y] = val;
      
      //  We mask out any tiles that are considered unexplorable (i.e, cannot
      //  be pathed upon.)  Otherwise, we flag as unexplored any tiles where
      //  the fog value has decayed below a minimum threshold.
      final Tile t = world.tileAt(c.x, c.y);
      if (t.blocked()) {
        fogMap.set(1, c.x, c.y);
        continue;
      }
      else if (val < FOG_SEEN_MIN) fogMap.set(0, c.x, c.y);
    }
  }
  
  
  public float fogAt(Tile t) {
    if (GameSettings.fogFree || base.primal) return 1;
    return t == null ? -1 : fogVals[t.x][t.y];
  }
  
  
  public float fogAt(int x, int y) {
    if (GameSettings.fogFree || base.primal) return 1;
    return fogVals[Nums.clamp(x, world.size)][Nums.clamp(y, world.size)];
  }
  
  
  public float fogAt(Target t) {
    if (GameSettings.fogFree || base.primal) return 1;
    return fogAt(world.tileAt(t));
  }
  
  
  public int liftFogAround(Target t, float radius) {
    if (GameSettings.fogFree || base.primal) return (int) radius;
    final Vec3D p = t.position(null);
    return liftFogAround(p.x, p.y, radius);
  }
  
  
  public int liftFogAround(float x, float y, float radius) {
    if (GameSettings.fogFree || base.primal) return (int) radius;
    //
    //  We record and return the number of new tiles seen-
    final Box2D area = new Box2D().set(
      x - radius, y - radius,
      radius * 2, radius * 2
    );
    float tilesSeen = 0;
    //
    //  Iterate over any tiles within a certain distance of the target point-
    for (Tile t : world.tilesIn(area, true)) {
      final float xd = t.x - x, yd = t.y - y;
      final float distance = (float) Nums.sqrt((xd * xd) + (yd * yd));
      if (distance > radius) continue;
      //
      //  Calculate the minimum fog value, based on target proximity-
      final float oldVal = fogVals[t.x][t.y];
      final float lift = (1 - (distance / radius)) * MAX_FOG;
      final float newVal = Nums.max(lift, oldVal);
      fogVals[t.x][t.y] = newVal;
      //
      //  If there's been a change in fog value, update the reference and
      //  rendering data-
      if (oldVal != newVal && newVal >= FOG_SEEN_MIN) fogMap.set(1, t.x, t.y);
      tilesSeen += lift - oldVal;
    }
    return (int) tilesSeen;
  }
  
  
  
  /**  Helper method for grabbing unexplored tiles-
    */
  //  TODO:  You'll need to use large-scale pathing-culling here as soon as
  //  it's available.
  
  public static Tile getUnexplored(
    Base base, Target client,
    Target centre, float distanceUnit, final float maxDist
  ) {
    if (GameSettings.fogFree || base.primal) return null;
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
    final MipMap map = base.intelMap.fogMap();
    final Stage world = centre.world();
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
        //  Having skipped any fully-explored areas, or those outside of
        //  maximum range, we can add the remainder to the agenda, and add some
        //  randomness for spice.
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
    //
    //  Finally, we check to ensure that the path selected is, in fact,
    //  possible to path toward:
    if (picked == null) return null;
    if (client instanceof Mobile) {
      if (world.pathingCache.hasPathBetween(
        client, picked, (Mobile) client, report
      )) return picked;
      else return null;
    }
    else return picked;
  }
}







