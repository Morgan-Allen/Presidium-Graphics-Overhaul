/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.game.planet ;
import code.util.*;



public class Regions implements TileConstants {
  
  
  public static void main(String args[]) {
    
    /*
    float line[] = staggeredLine(17, 2.0f) ;
    I.say("Line is: ") ;
    for (float f : line) I.add(((int) (f * 10))+", ") ;
    //*/
    
    //*
    final int SIZE = 16 ;
    List <Coord[]> regions = genRegionsFor(SIZE, 3) ;
    I.say(regions.size()+" regions generated!") ;
    int ID = 0 ;
    int map[][] = new int[SIZE][SIZE] ;
    for (Coord c : Visit.grid(0, 0, SIZE, SIZE, 1)) map[c.x][c.y] = -1 ;
    for (Coord[] region : regions) {
      for (Coord c : region) map[c.x][c.y] = ID ;
      ID++ ;
    }
    for (int y = 0 ; y < SIZE ; y++) {
      I.say("\n") ;
      for (int x = 0 ; x < SIZE ; x++) {
        I.add(((char) ('A' + map[x][y]))+", ") ;
      }
    }
    //*/
  }
  
  
  private static class Part extends Coord {
    Region region = null ;
    ListEntry entry ;
  }
  
  
  private static class Region {
    Part core = null ;
    int ID = 0 ;
    List <Part> taken = new List <Part> () ;
    List <Region> borders = new List <Region> () ;
  }
  
  //
  //  I'll need to work on this some more- regions need better 'salting', I
  //  think, rather than a random scan system.
  
  
  static List <Coord[]> genRegionsFor(final int gridSize, final int MIN_RS) {
    //
    //  Okay, first of all, set up the grid of parts, and the list of parts
    //  remaining.
    List <Region> regions = new List <Region> () ;
    final Part partsGrid[][] = new Part[gridSize][gridSize] ;
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      final Part part = new Part() ;
      part.x = c.x ;
      part.y = c.y ;
      partsGrid[c.x][c.y] = part ;
    }
    
    
    final List <Part>
      partsLeft = new List <Part> (),
      removals  = new List <Part> () ;
    final RandomScan scan = new RandomScan(gridSize) {
      protected void scanAt(int x, int y) {
        final Part part = partsGrid[x][y] ;
        part.entry = partsLeft.addLast(part) ;
      }
    } ;
    scan.doFullScan() ;
    
    //
    //  Now, grab initial regions.
    for (Part part : partsLeft) {
      Region region = new Region() ;
      Part under = null ;
      boolean space = true ;
      for (Coord c : Visit.grid(
        part.x - (MIN_RS / 2),
        part.y - (MIN_RS / 2),
        MIN_RS, MIN_RS, 1
      )) {
        try { under = partsGrid[c.x][c.y] ; }
        catch (ArrayIndexOutOfBoundsException e) { space = false ; break ; }
        if (under.region != null) { space = false ; break ; }
        region.taken.add(under) ;
      }
      if (space) {
        for (Part taken : region.taken) {
          taken.region = region ;
          removals.add(taken) ;
        }
        regions.add(region) ;
        region.ID = regions.size() ;
      }
    }
    for (Part removed : removals) partsLeft.removeEntry(removed.entry) ;
    removals.clear() ;
    
    
    //
    //  Then, repeatedly assign parts to neighbouring regions.
    while (partsLeft.size() > 0) {
      for (Part part : partsLeft) {
        Part near = null ;
        final int off = Rand.index(4) ;
        for (int i = 4 ; i-- > 0 ;) {
          final int n = N_ADJACENT[(i + off) % 4] ;
          try { near = partsGrid[part.x + N_X[n]][part.y + N_Y[n]] ; }
          catch (ArrayIndexOutOfBoundsException e) { continue ; }
          if (near.region != null) {
            near.region.taken.add(part) ;
            part.region = near.region ;
            removals.add(part) ;
            break ;
          }
        }
        if (near.region == null) {
        }
      }
      for (Part removed : removals) partsLeft.removeEntry(removed.entry) ;
      if (removals.size() == 0) break ;
      removals.clear() ;
    }
    
    
    //
    //  And finally, return a list of coordinates for external access-
    List <Coord[]> asCoords = new List <Coord[]> () ;
    for (Region region : regions) {
      final Coord coords[] = new Coord[region.taken.size()] ;
      int i = 0 ; for (Part part : region.taken) {
        final Coord c = new Coord() ;
        c.x = part.x ;
        c.y = part.y ;
        coords[i++] = c ;
      }
      asCoords.add(coords) ;
    }
    return asCoords ;
  }
  
  
  
  public static float[] staggeredLine(final int length, float variation) {
    float line[] = new float[length] ;
    int dim = length - 1 ;
    while (dim > 1) {
      for (int i = 0 ; i < length - 1 ; i += dim) {
        float value = (line[i] + line[i + dim]) / 2 ;
        line[i + (dim / 2)] = value + (Rand.num() * variation) ;
      }
      dim /= 2 ;
      variation /= 2 ;
    }
    return line ;
  }
}





/*
final List <Part>
  partsLeft = new List <Part> (),
  removals  = new List <Part> () ;
final RandomScan scan = new RandomScan(gridSize) {
  protected void scanAt(int x, int y) {
    if (x >= gridSize || y >= gridSize) return ;
    final Part part = partsGrid[x][y] ;
    part.entry = partsLeft.addLast(part) ;
  }
} ;
scan.doFullScan() ;
//*/

/*
//
//  Now, grab initial regions.
for (Part part : partsLeft) {
  Region region = new Region() ;
  Part under = null ;
  boolean space = true ;
  for (Coord c : Visit.grid(
    part.x - (MIN_RS / 2),
    part.y - (MIN_RS / 2),
    MIN_RS, MIN_RS, 1
  )) {
    try { under = partsGrid[c.x][c.y] ; }
    catch (ArrayIndexOutOfBoundsException e) { space = false ; break ; }
    if (under.region != null) { space = false ; break ; }
    region.taken.add(under) ;
  }
  if (space) {
    for (Part taken : region.taken) {
      taken.region = region ;
      removals.add(taken) ;
    }
    regions.add(region) ;
    region.ID = regions.size() ;
  }
}
for (Part removed : removals) partsLeft.removeEntry(removed.entry) ;
removals.clear() ;
//*/
/*
//
//  Then, repeatedly assign parts to neighbouring regions.
while (partsLeft.size() > 0) {
  for (Part part : partsLeft) {
    Part near = null ;
    final int off = Rand.index(4) ;
    for (int i = 4 ; i-- > 0 ;) {
      final int n = N_ADJACENT[(i + off) % 4] ;
      try { near = partsGrid[part.x + N_X[n]][part.y + N_Y[n]] ; }
      catch (ArrayIndexOutOfBoundsException e) { continue ; }
      if (near.region != null) {
        near.region.taken.add(part) ;
        part.region = near.region ;
        removals.add(part) ;
        break ;
      }
    }
  }
  for (Part removed : removals) partsLeft.removeEntry(removed.entry) ;
  if (removals.size() == 0) break ;
  removals.clear() ;
}
//*/