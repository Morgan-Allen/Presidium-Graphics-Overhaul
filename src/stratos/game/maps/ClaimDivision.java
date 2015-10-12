/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



//
//  I... would like the ability to expand the area being paved more gradually,
//  based on whether it's seeing use or not.
//
//  In addition, I'd like to be able to allocate different uses to different
//  plots of land.


public class ClaimDivision {
  
  
  /**  Data fields and save/load methods-
    */
  final public static ClaimDivision NONE = new ClaimDivision();
  static {
    NONE.reserved = new Tile[0];
    NONE.useMap   = new byte[0][0];
  }
  
  final public static byte
    USE_NONE      = -1,
    USE_PAVE      =  0,
    USE_NORMAL    =  1,
    USE_SECONDARY =  2,
    USE_TERTIARY  =  3;
  
  
  private Box2D area = new Box2D();
  private Stack <Box2D> plots = new Stack();
  private Tile reserved[];
  private byte useMap[][];
  
  
  private ClaimDivision() {}
  
  
  public static void saveTo(Session s, ClaimDivision d) throws Exception {
    if (d == null || d == NONE) { s.saveBool(false); return; }
    else s.saveBool(true);
    
    d.area.saveTo(s.output());
    s.saveInt(d.plots.size());
    for (Box2D plot : d.plots) plot.saveTo(s.output());
    s.saveObjectArray(d.reserved);
    s.saveInt(d.useMap.length   );
    s.saveInt(d.useMap[0].length);
    s.saveByteArray(d.useMap);
  }
  
  
  public static ClaimDivision loadFrom(Session s) throws Exception {
    if (! s.loadBool()) return NONE;
    
    final ClaimDivision d = new ClaimDivision();
    d.area.loadFrom(s.input());
    for (int n = s.loadInt(); n-- > 0;) {
      d.plots.add(new Box2D().loadFrom(s.input()));
    }
    d.reserved = (Tile[]) s.loadObjectArray(Tile.class);
    d.useMap   = new byte[s.loadInt()][s.loadInt()];
    s.loadByteArray(d.useMap);
    return d;
  }
  
  
  
  /**  Various factory methods-
    */
  public static ClaimDivision forEmptyArea(Venue v, Box2D area) {
    final ClaimDivision d = new ClaimDivision();
    d.area.setTo(area);
    d.reserved = new Tile[0];
    d.useMap   = new byte[1][0];
    return d;
  }
  
  
  public static ClaimDivision forArea(
    Venue v, Box2D area, int face, int spacing, int maxSideRatio,
    Fixture... excluded
  ) {
    if (v.origin() == null) return null;
    final ClaimDivision d = new ClaimDivision();
    
    final Stage world = v.origin().world;
    final Stack <Box2D> plots = d.divideIntoPlots(area, spacing, maxSideRatio);
    
    final Batch <Tile> claim = new Batch();
    final Tile o = world.tileAt(area.xpos(), area.ypos());
    final byte useMap[][] = new byte[(int) area.xdim()][(int) area.ydim()];
    
    for (Box2D plot : plots) {
      for (Tile t : world.tilesIn(plot, false)) {
        byte use = USE_NORMAL;
        //
        //  Any tiles under current or projected structures should be ignored,
        //  as should unbuildable terrain.
        if (use == 1) if (! t.buildable()) {
          use = USE_NONE;
        }
        if (use == 1) for (Fixture f : excluded) {
          if (! f.footprint().contains(t.x, t.y, -1)) continue;
          use = USE_NONE; break;
        }
        //
        //  Otherwise, mark the spot as used.
        final int x = t.x - o.x, y = t.y - o.y;
        useMap[x][y] = use;
        if (use >= 1) claim.add(t);
      }
    }
    
    d.area.setTo(area);
    d.plots    = plots;
    d.reserved = claim.toArray(Tile.class);
    d.useMap   = useMap;
    return d;
  }
  
  
  public ClaimDivision withUsageMarked(
    float fractionOfArea, boolean acrossAllowed, boolean downAllowed,
    Venue around, int... useMarking
  ) {
    final Stage world = around.origin().world;
    final Stack <Box2D> allowed = new Stack();
    float totalArea = 0;
    
    for (Box2D plot : plots) {
      totalArea += plot.area();
      final boolean across = plot.xdim() >= plot.ydim();
      if (   across  && ! acrossAllowed) continue;
      if ((! across) && ! downAllowed  ) continue;
      allowed.add(plot);
    }
    
    float areaDesired = totalArea * fractionOfArea, areaMarked = 0;
    
    while (allowed.size() > 0 && areaMarked < areaDesired) {
      final Box2D plot = allowed.atIndex(Rand.index(allowed.size()));
      final boolean across = plot.xdim() >= plot.ydim();
      markPlot(plot, world, across, useMarking);
      areaMarked += plot.area();
      allowed.remove(plot);
    }
    return this;
  }
  
  
  private void markPlot(
    Box2D plot, Stage world, boolean across, int... useMarking
  ) {
    final Tile
      aO = world.tileAt(area.xpos(), area.ypos()),
      pO = world.tileAt(plot.xpos(), plot.ypos());
    
    for (Tile t : world.tilesIn(plot, false)) {
      final int
        aX = t.x - aO.x, aY = t.y - aO.y,
        pX = t.x - pO.x, pY = t.y - pO.y;
      if (useMap[aX][aY] <= 0) continue;
      final int m = Nums.clamp(across ? pY : pX, useMarking.length);
      useMap[aX][aY] = (byte) useMarking[m];
    }
  }
  
  
  public Tile[] toPaveAround(Venue venue, Tile usageMask[]) {
    Batch <Tile> toPave = new Batch();
    
    //
    //  TODO:  You'll need to find a way to check for usage efficiently!
    
    if (usageMask != null) for (Tile t : usageMask) t.flagWith(toPave);
    
    for (Box2D plot : plots) {
      for (Tile t : Spacing.perimeter(plot, venue.world())) {
        if (t != null && t.buildable()) toPave.add(t);
      }
    }
    for (Tile t : Spacing.perimeter(venue.footprint(), venue.world())) {
      if (t != null && t.buildable()) toPave.add(t);
    }
    
    if (usageMask != null) for (Tile t : usageMask) t.flagWith(null);
    return toPave.toArray(Tile.class);
  }
  
  
  public Tile[] reserved() {
    return reserved;
  }
  
  
  
  /**  Carving up space.
    */
  private Stack <Box2D> divideIntoPlots(
    Box2D area, int prefSpacing, int maxSideRatio
  ) {
    final int idealSplit = 1 + (prefSpacing * 2);
    prefSpacing  = Nums.max(prefSpacing , 1);
    maxSideRatio = Nums.max(maxSideRatio, 1);
    final Stack <Box2D> bigPlots = new Stack(), finePlots = new Stack();
    bigPlots.add(area);
    
    while (bigPlots.size() > 0) {
      for (Box2D plot : bigPlots) {
        final float minSide = plot.minSide();
        
        if (minSide > prefSpacing) {
          boolean across = plot.xdim() < plot.ydim();
          if (minSide < idealSplit) across = ! across;
          dividePlot(plot, across, 0.5f, bigPlots);
        }
        else if (plot.maxSide() > minSide * maxSideRatio) {
          final float split = (Rand.num() + 0.5f) / 2;
          dividePlot(plot, plot.xdim() > plot.ydim(), split, bigPlots);
        }
        else {
          bigPlots.remove(plot);
          finePlots.add(plot);
        }
      }
    }
    return finePlots;
  }
  
  
  private void dividePlot(
    Box2D plot, boolean across, float split, Stack <Box2D> plots
  ) {
    final int
      side  = (int) (across ? plot.xdim() : plot.ydim()),
      sideA = (int) (side * split),
      sideB = side - (1 + sideA);
    final Box2D
      plotA = new Box2D(plot),
      plotB = new Box2D(plot);
    
    if (across) {
      plotA.xdim(sideA);
      plotB.xdim(sideB);
      plotB.incX(sideA + 1);
    }
    else {
      plotA.ydim(sideA);
      plotB.ydim(sideB);
      plotB.incY(sideA + 1);
    }
    plots.remove(plot);
    plots.add(plotA);
    plots.add(plotB);
  }
  
  
  
  /**  Utility methods for queries-
    */
  public byte useType(Tile t) {
    final Tile o = t.world.tileAt(area.xpos(), area.ypos());
    if (o == null) return -1;
    try { return useMap[t.x - o.x][t.y - o.y]; }
    catch (ArrayIndexOutOfBoundsException e) { return -1; }
  }
}











