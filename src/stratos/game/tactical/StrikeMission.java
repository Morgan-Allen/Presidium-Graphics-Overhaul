/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.tactical;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
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
    );
  }
  
  
  public StrikeMission(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = cachedStepFor(actor, false);
    if (cached != null) return cached;
    
    final Combat combat = new Combat(
      actor, (Element) subject,
      Combat.STYLE_EITHER, objectIndex(), true
    );
    combat.setMotive(Plan.MOTIVE_MISSION, basePriority(actor));
    
    return cacheStepFor(actor, combat);
  }
  
  
  protected boolean shouldEnd() {
    if (CombatUtils.isDowned((Element) subject, objectIndex())) return true;
    return false;
  }
  
  
  
  /**  Rendering and interface-
    */
  protected String[] objectiveDescriptions() {
    return Combat.OBJECT_NAMES;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ");
    d.append("Strike Mission", this);
    d.append(" against ");
    d.append(subject);
  }
}








