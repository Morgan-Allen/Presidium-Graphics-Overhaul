/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.util.*;



//  So... what triggers them?  An EM-signature, meaning that a technologically
//  advanced, non-artilect enemy is present on the map.  That causes them to
//  bring various sub-systems online.  (Something like 40K necrons.)

//  TODO:  You need a 'Faction' class to handle this function on a setting-
//  wide level.

//  Well, for the present, from a gameplay perspective, the main function is to
//  apply gradual but consistent pressure on the player to explore, set up
//  useful defences, gather allies and eradicate a foe.


public class ArtilectBase extends Base {
  
  
  private float onLineLevel;
  
  
  
  public ArtilectBase(Stage world) {
    super(world, true);
  }
  
  
  public ArtilectBase(Session s) throws Exception {
    super(s);
    this.onLineLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(onLineLevel);
  }
  
  
  protected BaseTactics initTactics() {
    return new BaseTactics(this) {
      
    };
  }
  
  
  
}







