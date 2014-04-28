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



public class StrikeMission extends Mission {
  
  /**  Field definitions, constants and save/load methods-
    */
  private static boolean verbose = false;
  
  
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
  //  TODO:  Learn to cache steps for each actor engaged in a mission?
  
  public float priorityFor(Actor actor) {
    final Combat combat = new Combat(
      actor, (Element) subject, Combat.STYLE_EITHER, objectIndex()
    );
    final float BP = basePriority(actor);
    combat.setMotive(Plan.MOTIVE_MISSION, BP);
    final float priority = combat.priorityFor(actor);
    
    if (verbose && I.talkAbout == actor) {
      I.say("\nBase and final strike mission priority: "+BP+"/"+priority);
    }
    return priority;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (! isActive()) return null;
    return new Combat(
      actor, (Element) subject,
      Combat.STYLE_EITHER, objectIndex()
    );
  }
  
  
  protected boolean shouldEnd() {
    if (Combat.isDowned((Element) subject, objectIndex())) return true ;
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








