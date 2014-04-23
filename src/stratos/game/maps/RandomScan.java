/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.maps ;
import stratos.game.common.*;
import stratos.util.*;



/**  This class is responsible for visiting every tile within the world in a
  *  pseudo-random order.  (It does so using a linear feedback shift register
  *  with a period matching the area of the map.)
  */
public abstract class RandomScan {
  
  
  final int size ;
  final int totalTiles ;
  final private int seedBits ;
  private LFSR lfsr ;
  
  private int lastScanIndex = 0 ;
  private int scan, seed ;
  
  
  
  /**  Sets up the growth map and calculates the bit-depth of the random seeds
    *  required (matching the power of the size of the world.)
    */
  public RandomScan(int size) {
    this.size = size ;
    totalTiles = size * size ;
    int s = 1, d = 1 ; while ((s *= 2) < size) d++ ; seedBits = d * 2 ;
    initSeeds() ;
  }
  
  
  public void loadState(Session s) throws Exception {
    lastScanIndex = s.loadInt() ;
    seed = s.loadInt() ;
    scan = s.loadInt() ;
    lfsr = new LFSR(seedBits, scan) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(lastScanIndex) ;
    s.saveInt(seed) ;
    s.saveInt(scan) ;
  }
  
  
  
  /**  Generates the random seeds needed-
    */
  private void initSeeds() {
    scan = seed = Rand.index(totalTiles - 1) + 1 ;
    lfsr = new LFSR(seedBits, seed) ;
  }
  
  
  
  /**  Scans through every tile on the map.
    */
  public void doFullScan() {
    initSeeds() ;
    scanThrough(totalTiles) ;
  }
  
  public void scanThroughTo(int index) {
    if (index < 0 || index == lastScanIndex) return ;
    index %= totalTiles ;
    if (index > lastScanIndex) scanThrough(index - lastScanIndex) ;
    else scanThrough(index + totalTiles - lastScanIndex) ;
  }
  
  
  final static int
    GATE = genGate(),
    INVG = ~GATE ;
  private static int genGate() {
    int gate = 0 ;
    for (int i = 32, g = 1 ; i-- > 0 ; g = g << 1) gate |= g ;
    return gate ;
  }
  
  
  
  /**  Scans through a fixed number of tiles on the map surface-
    */
  public void scanThrough(int numTiles) {
    for (int n = 0 ; n < numTiles ;) {
      final int scanX, scanY ;
      if (scan == 0) {
        scanX = scanY = 0 ;
        initSeeds() ;
      }
      else {
        scan = lfsr.nextVal() ;
        scanX = scan / size ;
        scanY = scan % size ;
        if (scan == seed) scan = 0 ;
      }
      if (scanX >= size || scanY >= size) continue ;
      scanAt(scanX, scanY) ;
      n++ ;
    }
    lastScanIndex = (lastScanIndex + numTiles) % totalTiles ;
  }
  
  
  
  /**  Scans a single tile of the map.
    */
  protected abstract void scanAt(int x, int y) ;
}




