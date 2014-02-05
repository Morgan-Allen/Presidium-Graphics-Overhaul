/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package graphics.terrain ;
import util.* ;



public class LayerPattern implements TileConstants {
  
  
  /**  Converts a UV map expressed in terms of unit grid coordinates into a
    *  scaled version based on vertex coordinates within standard UV limits
    *  (i.e, [1, 0].  This allows UV to be specified more naturally below.)
    */
  final static float
    VERT_PATTERN[] = {
      0, 0, 0,
      0, 0, 1,
      1, 0, 0,
      1, 0, 1
    },
    INDEX_PATTERN[] = {
      0, 1, 2, 2, 1, 3
    },
    MIN_UV = 0.01f,  //allow a slight margin for safety's sake...
    MAX_UV = 1 - MIN_UV,
    UV_PATTERN[] = {
      MIN_UV, MAX_UV,
      MIN_UV, MIN_UV,
      MAX_UV, MAX_UV,
      MAX_UV, MIN_UV
    } ;
  private final static float[][] shrinkUVMap(float initMap[][], float maxUV) {
    float processedUV[][] = new float[initMap.length][] ;
    //
    //  Here, the specific coordinates are stored-
    final int dataSize = UV_PATTERN.length ;
    int i = 0 ; for (float[] UV : initMap) {
      final float MAP[] = processedUV[i++] = new float[dataSize] ;
      for (int n = 0 ; n < dataSize ;) {
        MAP[n] = (UV_PATTERN[n++] + UV[0]) / maxUV ;
        MAP[n] = (UV_PATTERN[n++] + UV[1]) / maxUV ;
      }
    }
    return processedUV ;
  }
  
  
  /**  This is code for the patterns associated with the road map or localised
    *  terrain splats, which remain neatly self-contained in their own tiles:
    */
  //
  //  These indices refer to x/y coordinates within the UV map of a texture
  //  that keeps it's fringe inside it's masked area.
  //  The texture image looks something like this (in terms of compass-point
  //  adjacency):
  //    | SE  | SEW  | SW  | S  |
  //    | NSE | NSEW | NSW | NS |
  //    | NE  | NEW  | NW  | N  |
  //    | E   | EW   | W   |    |
  //    And the lower half-
  //    | SE       | SE/SW | SW       | NE/NW/SW
  //    | NE/SE    | ALL 4 | NW/SW    | NW/SE
  //    | NE       | NE/NW | NW       | NE/SE/SW
  //    | NW/SE/SW | NE/SW | NE/NW/SE | (N/A)
  final public static float
    INNER_FRINGE_INDEX[][] = {
      {3, 3},  //0- none adjacent
      {2, 3},  //1- west
      {0, 3},  //2- east
      {1, 3},  //3- east and west
      {3, 0},  //4- south
      {2, 0},  //5- south and west
      {0, 0},  //6- south and east
      {1, 0},  //7- south, east and west
      {3, 2},  //8- north
      {2, 2},  //9- north and west
      {0, 2},  //10- north and east
      {1, 2},  //11- north, east and west
      {3, 1},  //12- north and south
      {2, 1},  //13- north, south and west
      {0, 1},  //14- north, south and east
      {1, 1},  //15- all 4 sides
      //  What follows are indices for diagonally adjacent areas-
      {3, 7},  //0- none adjacent
      {2, 6},  //1- northwest
      {2, 4},  //2- southwest
      {2, 5},  //3- northwest and southwest
      {0, 4},  //4- southeast
      {3, 5},  //5- northwest and southeast
      {1, 4},  //6- southwest and southeast
      {0, 7},  //7- southeast, southwest and northwest
      {0, 6},  //8- northeast
      {1, 6},  //9- northeast and northwest
      {1, 7},  //10- northeast and southwest
      {3, 4},  //11- northeast, southwest and northwest
      {0, 5},  //12- northeast and southeast
      {2, 7},  //13- northeast, southeast and northwest
      {3, 6},  //14- northeast, southeast and southwest
      {1, 5}   //15- all 4 corners
    },
    INNER_FRINGE_UV[][] = shrinkUVMap(INNER_FRINGE_INDEX, 8) ;
  private final static int innerIndices[] = new int[2] ;
  private static float innerUV[][] = new float[2][] ;
  
  
  protected static int[] innerFringeIndices(boolean near[]) {
    //
    //  Assemble the ascertained components-
    innerIndices[0] = innerIndices[1] = -1 ;
    final boolean
      nE = near[N] && near[E] && near[NE],
      sE = near[E] && near[S] && near[SE],
      sW = near[S] && near[W] && near[SW],
      nW = near[W] && near[N] && near[NW] ;
    if (nE && nW) near[N] = false ;
    if (nE && sE) near[E] = false ;
    if (sE && sW) near[S] = false ;
    if (sW && nW) near[W] = false ;
    final int diagIndex =
      (nE ? 8 : 0) +
      (sE ? 4 : 0) +
      (sW ? 2 : 0) +
      (nW ? 1 : 0) ;
    final int comboIndex =
      (near[N] ? 8 : 0) +
      (near[S] ? 4 : 0) +
      (near[E] ? 2 : 0) +
      (near[W] ? 1 : 0) ;
    int pI = 0 ;  //piece index.
    if (diagIndex == 0 || comboIndex != 0)
      innerIndices[pI++] = comboIndex ;
    if (diagIndex != 0)
      innerIndices[pI] = diagIndex + 16 ;
    return innerIndices ;
  }
  
  protected static float[][] innerFringeUV(boolean near[]) {
    final int indices[] = innerFringeIndices(near) ;
    for (int n = 2 ; n-- > 0 ;)
      innerUV[n] = (indices[n] == -1) ? null : INNER_FRINGE_UV[indices[n]] ;
    return innerUV ;
  }
  
  
  
  /**  This is code for the patterns associated with basic terrain types, which
    *  can overlap with or 'fringe' on eachother...
    */
  //
  //  These indices refer to x/y coordinates within the UV map of a terrain
  //  fringing texture for given combinations of adjacent tiles.
  //  The texture image looks something like this (in terms of compass-point
  //  adjacency):
  //    | NW  | NE  | NEW  |
  //    | SW  | SE  | SEW  |
  //    | NSW | NSE | NSEW |
  //    | CSE | S   | CSW  |
  //    | E   |     | W    |
  //    | CNE | N   | CNW  |
  //  (C stands for 'corner-adjacent'.  The blank space at (1, 4) is for the
  //  standard texture.)
  final public static float
    OUTER_FRINGE_INDEX[][] = {
      {1, 4},  //centre tile
      {1, 5},  //near north
      {0, 4},  //near east
      {1, 0},  //near north and east
      {1, 3},  //near south
      {0.5f, 2},  //near north and south (cheating slightly)
      {1, 1},  //near south and east
      {1, 2},  //near north, south and east
      {2, 4},  //near west
      {0, 0},  //near north and west
      {2, 0.5f},  //near east and west (again, cheating slightly)
      {2, 0},  //near north, east and west
      {0, 1},  //near south and west
      {0, 2},  //near north, south and west
      {2, 1},  //near south, east and west
      {2, 2},  //near north, south, east and west
      {0, 5},  //near northeast (corner)
      {0, 3},  //near southeast (corner)
      {2, 3},  //near southwest (corner)
      {2, 5}   //near northwest (corner)
    },
    OUTER_FRINGE_UV[][] = shrinkUVMap(OUTER_FRINGE_INDEX, 6),
    OUTER_FRINGE_CENTRE[] = OUTER_FRINGE_UV[0] ;
  //
  //  Having done that, we can now define some convenient constants:
  final static int
    CORNER_OFFSET = 16,
    NONE      = 0,
    NORTH_AND_SOUTH = 5,
    NORTH_OFF = 1,
    SOUTH_OFF = 4,
    EAST_AND_WEST = 10,
    EAST_OFF  = 2,
    WEST_OFF  = 8 ;
  private static int outerIndices[] = new int[4] ;
  private static float outerUV[][] = new float[4][] ;
  
  
  protected static int[] outerFringeIndices(boolean near[]) {
    //
    //  First, clear the data-
    for (int n = 4 ; n-- > 0 ;) outerIndices[n] = -1 ;
    //
    //  Check to see what main combination is required...
    int pI = 0 ;  //piece index.
    int comboIndex = 0 ;
    for (int n = 4 ; n-- > 0 ;) if (near[2 * n]) comboIndex |= (1 << n) ;
    switch(comboIndex) {
      case(NONE) :
      break ;
      case(NORTH_AND_SOUTH) :
        outerIndices[pI++] = NORTH_OFF ;
        outerIndices[pI++] = SOUTH_OFF ;
      break ;
      case(EAST_AND_WEST) :
        outerIndices[pI++] = EAST_OFF ;
        outerIndices[pI++] = WEST_OFF ;
      break ;
      default :
        outerIndices[pI++] = NONE + comboIndex ;
    }
    //
    //  Now, check for nearby corners, make sure they're 'clear', and, if so,
    //  render them:
    for (int n = 1 ; n < 8 ; n += 2) {
      if (
        near[n] &&
        (! near[(n + 7) % 8]) &&
        (! near[(n + 1) % 8])
      ) outerIndices[pI++] = CORNER_OFFSET + ((n - 1) / 2) ;
    }
    return outerIndices ;
  }
  
  
  protected static float[][] outerFringeUV(boolean near[]) {
    final int indices[] = outerFringeIndices(near) ;
    for (int n = 4 ; n-- > 0 ;) {
      outerUV[n] = (indices[n] == -1) ? null : OUTER_FRINGE_UV[indices[n]] ;
    }
    return outerUV ;
  }
  
  
  
  /**  Finally, we have code for the semi-randomised interior pieces of a
    *  texture-
    */
  final public static float
    OUTER_EXTRAS_INDEX[][] = {
      {1, 4},
      {4, 3},
      {4, 4},
      {4, 5}
    },
    OUTER_EXTRAS_UV[][] = shrinkUVMap(OUTER_EXTRAS_INDEX, 6),
    INNER_EXTRAS_INDEX[][] = {
      {5, 6},
      {6, 6},
      {7, 6},
      {5, 5},
      {6, 5},
      {7, 5}
    },
    INNER_EXTRAS_UV[][] = shrinkUVMap(INNER_EXTRAS_INDEX, 8) ;
  
  
  protected static float[][] extraFringeUV(int varID, boolean outer) {
    final float UV[][] = outer ? OUTER_EXTRAS_UV : INNER_EXTRAS_UV ;
    varID %= UV.length ;
    innerUV[0] = UV[varID] ;
    innerUV[1] = null ;
    return innerUV ;
  }
}













