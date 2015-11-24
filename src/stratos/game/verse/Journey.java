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
  
  Vehicle transport;
  Batch <Mobile> migrants = new Batch();
  
  VerseLocation origin;
  VerseLocation destination;
  float arriveTime;
  boolean returns;
}
