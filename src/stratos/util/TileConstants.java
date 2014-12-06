/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;



public interface TileConstants {
  
  final public static int
    //
    //  Starts north, going clockwise:
    N  = 0,
    NE = 1,
    E  = 2,
    SE = 3,
    S  = 4,
    SW = 5,
    W  = 6,
    NW = 7,
    T_X[]        = {  0,  1,  1,  1,  0, -1, -1, -1  },
    T_Y[]        = {  1,  1,  0, -1, -1, -1,  0,  1  },
    T_INDEX[]    = {  N, NE,  E, SE,  S, SW,  W, NW  },
    T_ADJACENT[] = {  N,      E,      S,      W      },
    T_DIAGONAL[] = {     NE,     SE,     SW,     NW  },
    //
    //  Used to indicate the direction of facing for linear installations-
    X_AXIS = 0,
    Y_AXIS = 1,
    CORNER = 2;
  
  final public static String DIR_NAMES[] = {
    "North", "Northeast", "East", "Southeast",
    "South", "Southwest", "West", "Northwest"
  };
  
}


