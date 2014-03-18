/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util ;



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
    N_X[]        = {  0,  1,  1,  1,  0, -1, -1, -1  },
    N_Y[]        = {  1,  1,  0, -1, -1, -1,  0,  1  },
    N_INDEX[]    = {  N, NE,  E, SE,  S, SW,  W, NW  },
    N_ADJACENT[] = {  N,      E,      S,      W      },
    N_DIAGONAL[] = {     NE,     SE,     SW,     NW  },
    //
    //  Used to indicate the direction of facing for linear installations-
    X_AXIS = 0,
    Y_AXIS = 1,
    CORNER = 2 ;
  

  /*
  public abstract static class TileMask {
    public abstract boolean maskAt(final int x, final int y) ;
    
    public byte varID(int x, int y) { return 0 ; }
    public boolean nullsCount() { return false ; }
    
    public int simpleLineIndex(boolean near[]) {
      int numNear = 0 ;
      for (int n : N_ADJACENT) if (near[n]) numNear++ ;
      if (numNear != 2) return CORNER ;
      if (near[N] && near[S]) return Y_AXIS ;
      if (near[W] && near[E]) return X_AXIS ;
      return CORNER ;
    }
  }
  //*/
  
}


