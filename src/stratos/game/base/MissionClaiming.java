/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.verse.*;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.widgets.Composite;
import stratos.user.BaseUI;
import stratos.util.*;



//  TODO:  Claims-missions, by default, are always screened.  (They cannot be
//  covert, and they cannot be purely military.)



public class MissionClaiming extends Mission {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  final VerseLocation claimed;
  final Expedition expedition;
  
  
  public MissionClaiming(Base base, VerseLocation claimed) {
    super(base, claimed, null, "Claiming "+claimed);
    this.claimed = claimed;
    this.expedition = new Expedition();
  }
  
  
  public MissionClaiming(Session s) throws Exception {
    super(s);
    this.claimed = (VerseLocation) subject;
    this.expedition = (Expedition) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(expedition);
  }
  
  
  
  /**  Strategic evaluation-
    */
  public float targetValue(Base base) {
    return 0;
  }
  
  
  public float harmLevel() {
    return Plan.NO_HARM;
  }
  
  
  
  /**  Assigning steps to actors-
    */
  protected boolean shouldEnd() {
    return false;
  }
  
  
  protected Behaviour createStepFor(Actor actor) {
    return null;
  }
  
  
  
  /**  Offworld effects-
    */
  
  
  
  /**  Rendering and interface methods-
    */
  protected ImageAsset iconForMission(BaseUI UI) {
    return Mission.CLAIMING_ICON;
  }
  
  
  protected Composite compositeForSubject(BaseUI UI) {
    return null;
    //return Composite.withImage(location.icon, "founding_"+location);
  }
  
  
  public void describeMission(Description d) {
    d.appendAll("Claiming ", subject);
  }
}
















