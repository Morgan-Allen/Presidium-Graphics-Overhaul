/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.common.*;
import stratos.game.base.*;



//
//  NOTE:  Any Venues which implement this interface must ALSO register with
//  SERVICE_SECURITY in their range of services.
public interface Conscription extends Property {
  
  
  public boolean canConscript(Mobile unit, boolean checkDowntime);
  public float payMultiple(Actor conscript);
  public float motiveBonus(Actor conscript);
  
  public void beginConscription(Mobile unit, Mission mission);
  public void beginDowntime(Actor conscript);
}