/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.common;
//import stratos.start.Disposal;
import stratos.game.building.*;
import stratos.start.Assets;
import stratos.util.*;

import org.apache.commons.math3.util.FastMath;




public final class Spacing implements TileConstants {
  
  
  private Spacing() {}
  
  final static int
    CLUSTER_SIZE = 8;

  private final static Vec3D pA = new Vec3D(), pB = new Vec3D();
  private final static Box2D tA = new Box2D(), tB = new Box2D();
  
  public final static Tile
    tempT4[] = new Tile[4],
    tempT8[] = new Tile[8],
    tempT9[] = new Tile[9];
  public final static Boarding
    tempB4[] = new Boarding[4],
    tempB8[] = new Boarding[8];
  
  final static Tile PERIM_ARRAYS[][] = {
    new Tile[8 ],
    new Tile[12],
    new Tile[16],
    new Tile[20]
  };
  final static Element NEAR_ARRAYS[][] = {
    new Element[8 ],
    new Element[12],
    new Element[16],
    new Element[20]
  };
  
  final static Assets.Loadable DISPOSAL = new Assets.Loadable(
    "SPACING_DISPOSAL", Spacing.class, true
  ) {
    protected void loadAsset() {}
    public boolean isLoaded() { return true; }
    
    protected void disposeAsset() {
      Visit.wipe(tempT4);
      Visit.wipe(tempT8);
      Visit.wipe(tempT9);
      Visit.wipe(tempB4);
      Visit.wipe(tempB8);
      for (Tile t[] : PERIM_ARRAYS) Visit.wipe(t);
      for (Element e[] : NEAR_ARRAYS) Visit.wipe(e);
      
      Assets.registerForLoading(this);
    }
  };
  
  
  //
  //  Method for getting all tiles around the perimeter of a venue/area.
  public static Tile[] perimeter(Box2D area, World world) {
    final int
      minX = (int) Math.floor(area.xpos()),
      minY = (int) Math.floor(area.ypos()),
      maxX = (int) (minX + area.xdim() + 1),
      maxY = (int) (minY + area.ydim() + 1),
      wide = 1 + maxX - minX,
      high = 1 + maxY - minY;
    ///if (wide < 3) I.say("WIDE IS: "+wide+", area: "+area);
    final Tile perim[];
    if (wide == high && wide <= 6) perim = PERIM_ARRAYS[wide - 3];
    else perim = new Tile[(wide + high - 2) * 2];
    int tX, tY, pI = 0;
    for (tX = minX; tX++ < maxX;) perim[pI++] = world.tileAt(tX, minY);
    for (tY = minY; tY++ < maxY;) perim[pI++] = world.tileAt(maxX, tY);
    for (tX = maxX; tX-- > minX;) perim[pI++] = world.tileAt(tX, maxY);
    for (tY = maxY; tY-- > minY;) perim[pI++] = world.tileAt(minX, tY);
    return perim;
  }
  
  
  public static Tile[] under(Box2D area, World world) {
    final Batch <Tile> under = new Batch <Tile> ();
    for (Tile t : world.tilesIn(area, true)) under.add(t);
    return (Tile[]) under.toArray(Tile.class);
  }
  

  public static int[] entranceCoords(int xdim, int ydim, float face) {
    if (face == Venue.ENTRANCE_NONE) return new int[] { 0, 0 };
    face = (face + 0.5f) % Venue.NUM_SIDES;
    float edgeVal = face % 1;
    
    int enterX = 1, enterY = -1;
    if (face < Venue.ENTRANCE_EAST) {
      //  This is the north edge.
      enterX = xdim;
      enterY = (int) (ydim * edgeVal);
    }
    else if (face < Venue.ENTRANCE_SOUTH) {
      //  This is the east edge.
      enterX = (int) (ydim * (1 - edgeVal));
      enterY = xdim;
    }
    else if (face < Venue.ENTRANCE_WEST) {
      //  This is the south edge.
      enterX = -1;
      enterY = (int) (ydim * (1 - edgeVal));
    }
    else {
      //  This is the west edge.
      enterX = (int) (ydim * edgeVal);
      enterY = -1;
    }
    return new int[] { enterX, enterY };
  }

  
  
  /**  Methods for assisting placement-viability checks:
    */
  //
  //  This method checks whether the placement of the given element in this
  //  location would create secondary 'gaps' along it's perimeter that might
  //  lead to the existence of inaccessible 'pockets' of terrain- that would
  //  cause pathing problems.
  public static boolean perimeterFits(Element element) {
    final Box2D area = element.area(tA);
    final Tile perim[] = perimeter(area, element.origin().world);
    //
    //  Here, we check the first perimeter.  First, determine where the first
    //  taken (reserved) tile after a contiguous gap is-
    boolean inClearSpace = false;
    int index = perim.length - 1;
    while (index >= 0) {
      final Tile t = perim[index];
      if (t == null || t.owningType() >= element.owningType()) {
        if (inClearSpace) break;
      }
      else { inClearSpace = true; }
      index--;
    }
    //
    //  Then, starting from that point, and scanning in the other direction,
    //  ensure there's no more than a single contiguous clear space-
    final int
      firstTaken = (index + perim.length) % perim.length,
      firstClear = (firstTaken + 1) % perim.length;
    inClearSpace = false;
    int numSpaces = 0;
    for (index = firstClear; index != firstTaken;) {
      final Tile t = perim[index];
      if (t == null || t.owningType() >= element.owningType()) {
        inClearSpace = false;
      }
      else if (! inClearSpace) { inClearSpace = true; numSpaces++; }
      index = (index + 1) % perim.length;
    }
    if (numSpaces > 1) return false;
    return true;
  }
  
  
  
  public static int numNeighbours(Element element, World world) {
    if (element.xdim() > 4 || element.xdim() != element.ydim()) {
      I.complain("This method is intended only for small, regular elements.");
    }
    final int size = element.xdim() - 1;
    int numNeighbours = 0;
    final Element near[] = NEAR_ARRAYS[size];
    final Tile perim[] = (size == 0) ?
      element.origin().allAdjacent(PERIM_ARRAYS[0]) :
      Spacing.perimeter(element.area(tA), world);
    
    for (Tile t : perim) if (t != null) {
      final Element o = t.onTop();
      if (o != null && o.flaggedWith() == null) {
        near[numNeighbours++] = o;
        o.flagWith(element);
      }
    }
    
    for (int i = numNeighbours; i-- > 0;) near[i].flagWith(null);
    return numNeighbours;
  }
  
  
  public static boolean isEntrance(Tile t) {
    for (Tile n : t.edgeAdjacent(tempT4)) {
      if (n == null || ! (n.onTop() instanceof Boarding)) continue;
      for (Boarding b : ((Boarding) n.onTop()).canBoard()) {
        if (b == t) return true;
      }
    }
    return false;
  }
  
  
  public static Batch <Element> entranceFor(Tile t) {
    final Batch <Element> batch = new Batch <Element> ();
    for (Tile n : t.edgeAdjacent(tempT4)) {
      if (n == null || ! (n.onTop() instanceof Boarding)) continue;
      for (Boarding b : ((Boarding) n.onTop()).canBoard()) {
        if (b == t) batch.add((Element) n.onTop());
      }
    }
    return batch;
  }
  
  
  
  /**  Proximity methods-
    */
  public static Target nearest(
    Series <? extends Target> targets, final Target client
  ) {
    final Visit <Target> v = new Visit <Target> () {
      public float rate(Target t) { return 0 - distance(t, client); }
    };
    return v.pickBest((Series) targets);
  }
  

  public static Tile nearestOpenTile(
    Box2D area, Target client, World world
  ) {
    final Vec3D p = client.position(pA);
    final Tile o = world.tileAt(p.x, p.y);
    float minDist = Float.POSITIVE_INFINITY;
    Tile nearest = null;
    int numTries = 0;
    while (nearest == null && numTries++ < (CLUSTER_SIZE / 2)) {
      for (Tile t : perimeter(area, world)) {
        if (t == null || t.blocked()) continue;
        if (t != o && t.inside().size() > 0) continue;
        final float dist = distance(o, t);
        if (dist < minDist) { minDist = dist; nearest = t; }
      }
      area.expandBy(1);
    }
    return nearest;
  }
  
  
  public static Tile nearestOpenTile(Target t, Target client) {
    final Tile under = client.world().tileAt(t);
    return nearestOpenTile(under, client);
  }
  
  
  public static Tile nearestOpenTile(Tile tile, Target client) {
    if (tile == null) return null;
    ///if (! tile.blocked()) return tile;
    return nearestOpenTile(tile.area(tB), client, tile.world);
  }
  
  
  public static Tile nearestOpenTile(
    Element element, Target client, World world
  ) {
    if (element.pathType() >= Tile.PATH_HINDERS) {
      return nearestOpenTile(element.area(tA), client, world);
    }
    else {
      final Vec3D p = element.position(pA);
      return world.tileAt(p.x, p.y);
    }
  }
  
  
  public static Vec3D getPositionWithDefault(Object o, Vec3D d) {
    if (o instanceof Target) return ((Target) o).position(null);
    else return d;
  }
  
  
  
  /**  Returns a semi-random unblocked tile around the given element.
    */
  //  TODO:  Move this to the Placement class.
  public static Tile pickFreeTileAround(Target t, Element client) {
    final Tile perim[];
    if (t instanceof Tile) {
      perim = ((Tile) t).allAdjacent(null);
    }
    else if (t instanceof Element) {
      final Element e = (Element) t;
      perim = perimeter(e.area(tA), client.world());
    }
    else return null;
    //
    //  We want to avoid any tiles that are obstructed, including by other
    //  actors, and probably to stay in the same spot if possible.
    final Tile l = client.origin();
    final boolean inPerim = Visit.arrayIncludes(perim, l);
    ///if (inPerim && Rand.num() > 0.2f) return l;
    
    final float weights[] = new float[perim.length];
    float sumWeights = 0;
    int index = -1;
    for (Tile p : perim) {
      index++;
      if (p == null || p.blocked()) continue;
      if (p.inside().size() > 0) continue;
      final float dist = Spacing.distance(p, l);
      if (inPerim && dist > 4) continue;
      final float weight = 1f / (1 + dist);
      weights[index] = weight;
      sumWeights += weight;
    }
    
    float roll = Rand.num() * sumWeights;
    sumWeights = 0;
    for (int n = 0; n < perim.length; n++) {
      if (roll < sumWeights) return perim[n];
      sumWeights += weights[n];
    }
    
    return nearestOpenTile(t, client);
  }
  
  
  public static Tile pickRandomTile(Target t, float range, World world) {
    final double angle = Rand.num() * Math.PI * 2;
    final float dist = Rand.num() * range, max = world.size - 1;
    final Vec3D o = t.position(pA);
    return world.tileAt(
      Visit.clamp(o.x + (float) (Math.cos(angle) * dist), 0, max),
      Visit.clamp(o.y + (float) (Math.sin(angle) * dist), 0, max)
    );
  }
  
  
  public static Tile bestMidpoint(Target... targets) {
    if (targets == null || targets.length == 0) return null;
    final Target client = targets[0];
    final Vec2D avg = new Vec2D();
    for (Target t : targets) {
      t.position(pA);
      avg.x += pA.x / targets.length;
      avg.y += pA.y / targets.length;
    }
    Tile open = client.world().tileAt(avg.x, avg.y);
    return Spacing.nearestOpenTile(open, client);
  }
  
  
  
  /**  Distance calculation methods-
    */
  final public static float distance(final Target a, final Target b) {
    if (a == null || b == null) I.complain("NULL POINT! "+a+" "+b);
    final float dist = innerDistance(a, b) - (a.radius() + b.radius());
    return (dist < 0) ? 0 : dist;
  }
  

  final public static float sectorDistance(final Target a, final Target b) {
    return distance(a, b) / World.SECTOR_SIZE;
  }
  
  
  final public static float innerDistance(final Target a, final Target b) {
    a.position(pA);
    b.position(pB);
    final float xd = pA.x - pB.x, yd = pA.y - pB.y;
    return (float) FastMath.sqrt((xd * xd) + (yd * yd));
  }
  
  
  final public static int outerDistance(final Target a, final Target b) {
    final float dist = innerDistance(a, b);
    return (int) FastMath.ceil(dist + a.radius() + b.radius());
  }
  
  
  final public static float distance(final Tile a, final Tile b) {
    final int xd = a.x - b.x, yd = a.y - b.y;
    return (float) FastMath.sqrt((xd * xd) + (yd * yd));
  }
  
  
  public static int maxAxisDist(Tile a, Tile b) {
    final int xd = Math.abs(a.x - b.x), yd = Math.abs(a.y - b.y);
    return Math.max(xd, yd);
  }
  
  
  public static int sumAxisDist(Tile a, Tile b) {
    final int xd = Math.abs(a.x - b.x), yd = Math.abs(a.y - b.y);
    return xd + yd;
  }
  
  
  public static boolean adjacent(Tile t, Element e) {
    if (t == null || e == null) return false;
    e.area(tA);
    tA.expandBy(1);
    return tA.contains(t.x, t.y);
  }
  
  
  public static boolean edgeAdjacent(Tile a, Tile b) {
    if (a.x == b.x) return a.y == b.y + 1 || a.y == b.y - 1;
    if (a.y == b.y) return a.x == b.x + 1 || a.x == b.x - 1;
    return false;
  }
  
  
  public static boolean adjacent(Element a, Element b) {
    if (a == null || b == null) return false;
    a.area(tA);
    b.area(tB);
    return tA.intersects(tB);
  }
}







/*
public static Tile[] traceSurrounding(Element element, int maxLen) {
  final Tile perim[] = perimeter(element.area(), element.world());
  Tile temp[] = new Tile[8];
  Tile lastClear = null, lastBlock = null;
  
  for (Tile t : perim) if (! t.blocked()) { lastClear = t; break; }
  if (lastClear != null) for (Tile t : lastClear.edgeAdjacent(temp)) {
    if (t.blocked()) { lastBlock = t; break; }
  }
  if (lastClear == null || lastBlock == null) return new Tile[0];
  
  
  final Batch <Tile> clear = new Batch <Tile> ();
  Tile nextClear, nextBlock;
  
  //  TODO:  Figure out how this works.
  
  return (Tile[]) clear.toArray(Tile.class);
}
//*/

/*
private static boolean checkClustering(Element a, Tile t, boolean checkType) {
  final Element b = t.owner();
  if (checkType && (b == null || b.owningType() < a.owningType()))
    return true;
  final Tile oA = a.origin(), oB = b.origin();
  return
    ((oA.x / CLUSTER_SIZE) != (oB.x / CLUSTER_SIZE)) ||
    ((oA.y / CLUSTER_SIZE) != (oB.y / CLUSTER_SIZE));
}
//*/


