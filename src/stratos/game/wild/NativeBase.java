/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.verse.*;



public class NativeBase extends Base {

  
  
  public NativeBase(Stage world, String title) {
    super(world, Faction.FACTION_NATIVES);
    setTitle(title);
  }
  
  
  public NativeBase(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  public void updateVisits() {
    
  }
  
}
