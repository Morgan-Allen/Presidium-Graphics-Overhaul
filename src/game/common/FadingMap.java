


package src.game.common ;
import src.util.* ;



public class FadingMap implements TileConstants {
  
  
  final static int
    SHORT_INTERVAL = World.STANDARD_FADE_TIME,
    LONG_INTERVAL  = World.GROWTH_INTERVAL ;
  final static float
    OVERALL_MULT   = 1f / LONG_INTERVAL ;
  
  private static boolean verbose = false ;
  
  final World world ;
  final int grid, resolution ;
  
  private float
    shortTermSum[][],
    shortTermAvg[][],
    longTermSum[][],
    longTermAvg[][],
    overall ;  //TODO:  For both long & short term values?
  private int
    markShort[] = new int[3],
    markLong[] = new int[3] ;
  
  
  
  public FadingMap(World world, int resolution) {
    this.world = world ;
    this.resolution = resolution ;
    this.grid = world.size / resolution ;
    
    shortTermSum = new float[grid][grid] ;
    shortTermAvg = new float[grid][grid] ;
    longTermSum  = new float[grid][grid] ;
    longTermAvg  = new float[grid][grid] ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, grid, grid, 1)) {
      shortTermSum[c.x][c.y] = s.loadFloat() ;
      shortTermAvg[c.x][c.y] = s.loadFloat() ;
      longTermSum [c.x][c.y] = s.loadFloat() ; 
      longTermAvg [c.x][c.y] = s.loadFloat() ; 
    }
    overall = s.loadFloat() ;
    for (int i = 3 ; i-- > 0 ;) {
      markShort[i] = s.loadInt() ;
      markLong [i] = s.loadInt() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, grid, grid, 1)) {
      s.saveFloat(shortTermSum[c.x][c.y]) ;
      s.saveFloat(shortTermAvg[c.x][c.y]) ;
      s.saveFloat(longTermSum [c.x][c.y]) ;
      s.saveFloat(longTermAvg [c.x][c.y]) ;
    }
    s.saveFloat(overall) ;
    for (int i = 3 ; i-- > 0 ;) {
      s.saveInt(markShort[i]) ;
      s.saveInt(markLong [i]) ;
    }
  }
  
  
  
  /**  Methods for regularly updating, adjusting and querying danger values-
    */
  private void accumulate(float val, int x, int y) {
    shortTermSum[x][y] += val ;
    longTermSum[x][y] += val ;
    overall += val * OVERALL_MULT ;
    //if (verbose) I.say("Accumulating "+val+" at "+x+"|"+y) ;
  }
  
  
  private void updateVals(
    final int x, final int y, final int interval,
    final float sumVals[][], final float avgVals[][]
  ) {
    float oldVal = avgVals[x][y] ; int numNear = 1 ;
    
    for (int n : N_INDEX) try {
      oldVal += avgVals[x + N_X[n]][y + N_Y[n]] ;
      numNear++ ;
    } catch (ArrayIndexOutOfBoundsException e) {}
    
    avgVals[x][y] = (sumVals[x][y] * 0.5f / interval) ;
    sumVals[x][y] = interval * oldVal / numNear ;
  }
  
  
  private void performFade(
    final int interval, final int marking[],
    final float sumVals[][], final float avgVals[][]
  ) {
    final int gridSize = grid * grid ;
    final float timeMark = (world.currentTime() / interval) % 1 ;
    final int gridMark = (int) (gridSize * timeMark) ;
    int lastMark = marking[0], lastX = marking[1], lastY = marking[2] ;
    
    int numSteps = gridMark ;
    if (lastMark <= gridMark) numSteps -= lastMark ;
    else numSteps += (gridSize - lastMark) ;
    
    while (numSteps-- > 0) {
      updateVals(lastX, lastY, interval, sumVals, avgVals) ;
      if (++lastX >= grid) { lastX = 0 ; if (++lastY >= grid) lastY = 0 ; }
    }
    marking[0] = lastMark = gridMark ;
    marking[1] = lastX ;
    marking[2] = lastY ;
  }
  
  
  public void performFade() {
    performFade(SHORT_INTERVAL, markShort, shortTermSum, shortTermAvg) ;
    performFade(LONG_INTERVAL , markLong , longTermSum , longTermAvg ) ;
    overall *= (1 - OVERALL_MULT) ;
  }
  
  
  
  /**  External value impingement and queries-
    */
  public void impingeVal(Tile at, float power, boolean gradual) {
    accumulate(power, at.x / resolution, at.y / resolution) ;
  }
  
  
  public void impingeVal(Box2D bound, float val, boolean gradual) {
    final float fullVal = val * bound.area() ;
    final Box2D scaled = new Box2D().set(
      bound.xpos() / resolution,
      bound.ypos() / resolution,
      (bound.xdim() / resolution + 1),
      (bound.ydim() / resolution + 1)
    ) ;
    Box2D sample = new Box2D() ;
    
    for (Coord c : Visit.grid(scaled)) {
      sample.set(c.x, c.y, 1, 1).cropBy(scaled) ;
      final float inc = sample.area() * fullVal / scaled.area() ;
      accumulate(inc, c.x, c.y) ;
    }
  }
  
  
  public float shortTermVal(Tile at) {
    return Visit.sampleMap(world.size, shortTermAvg, at.x, at.y) ;
  }
  
  
  public float longTermVal(Tile at) {
    return Visit.sampleMap(world.size, longTermAvg, at.x, at.y) ;
  }
  
  
  public float overallSum() {
    return overall ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void presentVals(String label, float mult, boolean shortTerm) {
    final float squareA = resolution * resolution * mult ;
    final float map[][] = shortTerm ? shortTermAvg : longTermAvg ;
    I.present(map, label, 256, 256, squareA, -squareA) ;
  }
}


