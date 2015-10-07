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
    CENTRE = 8,
    T_X[]         = {  1,  1,  0, -1, -1, -1,  0,  1,  0  },
    T_Y[]         = {  0,  1,  1,  1,  0, -1, -1, -1,  0  },
    T_INDEX[]     = {  N, NE,  E, SE,  S, SW,  W, NW  },
    T_ADJACENT[]  = {  N,      E,      S,      W      },
    T_DIAGONAL[]  = {     NE,     SE,     SW,     NW  },
    T_ON_CENTRE[] = {  N, NE,  E, SE,  S, SW,  W, NW,  CENTRE  },
    //
    //  Used to indicate the direction of facing for linear installations-
    X_AXIS =  0,
    Y_AXIS =  1,
    CORNER =  2,
    UNUSED = -1;
  
  final static Mat3D
    R0   = new Mat3D().setIdentity().rotateZ(Nums.toRadians(0   )),
    R90  = new Mat3D().setIdentity().rotateZ(Nums.toRadians(-90 )),
    R180 = new Mat3D().setIdentity().rotateZ(Nums.toRadians(-180)),
    R270 = new Mat3D().setIdentity().rotateZ(Nums.toRadians(-270)),
    Z_ROTATIONS[] = { R0, R90, R180, R270 },
    ROTATE_X = new Mat3D().setIdentity().rotateX(Nums.toRadians(-90)),
    ROTATE_Y = new Mat3D().setIdentity().rotateY(Nums.toRadians(-90));
  
  final public static String DIR_NAMES[] = {
    "North", "Northeast", "East", "Southeast",
    "South", "Southwest", "West", "Northwest",
    "Centre"
  };
  
}


