/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.game.base.*;



public class FaunaBase extends Base {

  
  
  public FaunaBase(Stage world) {
    super(world, Faction.FACTION_WILDLIFE);
  }
  
  
  public FaunaBase(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  public void updateVisits() {
    
  }
  
}
