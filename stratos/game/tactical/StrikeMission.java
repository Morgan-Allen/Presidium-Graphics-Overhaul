/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.widgets.HUD;
import stratos.user.*;
import stratos.util.*;




//  Merge this with the Combat class?


public class StrikeMission extends Mission {
  
  

  /**  Field definitions, constants and save/load methods-
    */
  public StrikeMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.STRIKE_MODEL, "Striking at "+subject
    ) ;
  }
  
  
  public StrikeMission(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    if (subject instanceof Actor) return Combat.combatPriority(
      actor, (Actor) subject, basePriority(actor),
      PARAMOUNT, false
    );
    if (subject instanceof Venue) {
      return basePriority(actor);
    }
    return 0 ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (! isActive()) return null;
    return new Combat(
      actor, (Element) subject,
      Combat.STYLE_EITHER, objectIndex()
    );
  }


  public boolean finished() {
    if (Combat.isDowned((Element) subject)) return true ;
    return false ;
  }
  
  
  
  /**  Rendering and interface-
    */
  protected String[] objectiveDescriptions() {
    return Combat.OBJECT_NAMES;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Strike Mission", this) ;
    d.append(" against ") ;
    d.append(subject) ;
  }
}








