/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building;
import stratos.game.common.*;
import stratos.util.*;



public interface Boarding extends Target {
  
  final int
    BOARDABLE_TILE    = 0,
    BOARDABLE_VENUE   = 1,
    BOARDABLE_VEHICLE = 2,
    BOARDABLE_OTHER   = 3;
  
  
  void setInside(Mobile m, boolean is);
  Series <Mobile> inside();
  
  Box2D area(Box2D put);
  Boarding[] canBoard();
  boolean isEntrance(Boarding b);
  
  int pathType();
  boolean allowsEntry(Mobile m);
  int boardableType();
}