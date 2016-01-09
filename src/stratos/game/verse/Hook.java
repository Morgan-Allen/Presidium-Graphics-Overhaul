/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.user.notify.*;
import stratos.util.*;



/**  Hook, as in 'story hook'.  A problem or challenge intended to engage the
  *  player's interest and provide useful short-term goals.
  */

public abstract class Hook implements Session.Saveable {
  
  final static int
    STATE_INIT     = -1,
    STATE_DORMANT  =  0,
    STATE_ACTIVE   =  1,
    STATE_PASSED   =  2,
    STATE_FAILED   =  3,
    STATE_COMPLETE =  4;
  
  
  final String uniqueID;
  final MessageScript script;
  
  
  public Hook(String uniqueID, String scriptFile) {
    this.uniqueID = uniqueID;
    this.script   = new MessageScript(this, scriptFile);
  }
  
  
  public Hook(String uniqueID) {
    this.uniqueID = uniqueID;
    this.script   = null    ;
  }
  
  
  public Hook(Session s) throws Exception {
    s.cacheInstance(this);
    this.uniqueID = s.loadString();
    this.script   = (MessageScript) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveString(uniqueID);
    s.saveObject(script  );
  }
  
  
  
  
  protected abstract void updateHook();
  protected abstract void impactSector(Sector sector);
  protected abstract void impactWorld(Stage world);
  
  
  
  public abstract String fullName();
  public abstract void describeHook(Description d);
}










