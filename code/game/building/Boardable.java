/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.game.building ;
import code.game.common.*;
import code.util.*;



public interface Boardable extends Target {
  
  final int
    BOARDABLE_TILE    = 0,
    BOARDABLE_VENUE   = 1,
    BOARDABLE_VEHICLE = 2 ;
  
  
  void setInside(Mobile m, boolean is) ;
  Series <Mobile> inside() ;
  
  Box2D area(Box2D put) ;
  Boardable[] canBoard(Boardable batch[]) ;
  boolean isEntrance(Boardable b) ;
  
  int pathType() ;
  boolean allowsEntry(Mobile m) ;
  int boardableType() ;
  //boolean openPlan() ;
}