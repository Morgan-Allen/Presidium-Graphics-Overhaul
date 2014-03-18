/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;




//  Merge this with the Combat class?


public class StrikeMission extends Mission {
  
  

  /**  Field definitions, constants and save/load methods-
    */
  int objective = Combat.OBJECT_EITHER ;
  
  
  public StrikeMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.STRIKE_MODEL, "Striking at "+subject
    ) ;
  }
  
  
  public StrikeMission(Session s) throws Exception {
    super(s) ;
    objective = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(objective) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    if (subject instanceof Actor) return Combat.combatPriority(
      actor, (Actor) subject,
      actor.mind.greedFor(rewardAmount(actor)) * ROUTINE,
      PARAMOUNT, false
    ) ;
    if (subject instanceof Venue) {
      return actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    }
    return 0 ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null ;
    return new Combat(
      actor, (Element) subject, Combat.STYLE_EITHER, objective
    ) ;
  }


  public boolean finished() {
    if (Combat.isDowned((Element) subject)) return true ;
    return false ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    d.append("\n\nObjective: ") ;
    if (begun()) d.append(Combat.OBJECT_NAMES[objective]) ;
    else d.append(new Description.Link(Combat.OBJECT_NAMES[objective]) {
      public void whenClicked() {
        objective = (objective + 1) % Combat.ALL_OBJECTS.length ;
      }
    }) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Strike Mission", this) ;
    d.append(" against ") ;
    d.append(subject) ;
  }
}








