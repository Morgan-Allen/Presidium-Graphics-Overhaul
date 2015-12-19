/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public class Journey {
  
  final public static int
    PURPOSE_SHIPPING = 0,
    PURPOSE_RAIDING  = 1,
    PURPOSE_ESCAPE   = 2;
  
  
  int purpose;
  Vehicle transport;
  Batch <Mobile> migrants = new Batch();
  
  Sector origin;
  Sector destination;
  float arriveTime;
  boolean returns;
}
