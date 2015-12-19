/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
import stratos.util.*;




public class MissionStrike extends Mission {
  
  
  /**  Field definitions, constants and save/load methods-
    */
  private static boolean
    rateVerbose = FactionAI.updatesVerbose,
    verbose     = false;
  
  
  public MissionStrike(Base base, Element subject) {
    super(
      base, subject, STRIKE_MODEL,
      "Striking at "+subject
    );
  }
  
  
  public MissionStrike(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Importance/suitability assessment-
    */
  public static MissionStrike strikeFor(Object target, Base base) {
    if ((
      target instanceof Actor ||
      target instanceof Venue
    ) && ((Target) target).base() != base) {
      return new MissionStrike(base, (Element) target);
    }
    return null;
  }
  
  
  public float targetValue(Base base) {
    final boolean report = I.matchOrNull(
      base.title(), FactionAI.verboseBase
    ) && rateVerbose && verbose;
    
    float targetValue = 0;
    if (subject instanceof Venue) {
      final Venue v = (Venue) subject;
      final Siting s = v.blueprint.siting();
      targetValue = s == null ? 1 : s.ratePointDemand(v.base(), v, false);
      targetValue = Nums.clamp(targetValue / BaseSetup.MAX_PLACE_RATING, 0, 1);
    }
    if (subject instanceof Actor) {
      //  TODO:  GET A VALUE FOR THIS
    }
    
    if (report) {
      I.say("\nRating "+this+" for "+base);
      I.say("  Target value:   "+targetValue);
    }
    return targetValue;
  }
  
  
  public float harmLevel() {
    if (objective() == Combat.OBJECT_SUBDUE ) return Plan.MILD_HARM;
    if (objective() == Combat.OBJECT_EITHER ) return Plan.REAL_HARM;
    if (objective() == Combat.OBJECT_DESTROY) return Plan.EXTREME_HARM;
    return 0;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    
    final Combat combat = new Combat(
      actor, (Element) subject, Combat.STYLE_EITHER, objective()
    );
    combat.addMotives(Plan.MOTIVE_MISSION, basePriority(actor));
    
    return cacheStepFor(actor, combat);
  }
  
  
  protected boolean shouldEnd() {
    if (CombatUtils.isDowned((Element) subject, objective())) return true;
    return false;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String[] objectiveDescriptions() {
    return Combat.OBJECT_NAMES;
  }
  
  
  public void describeMission(Description d) {
    d.append("Strike Mission", this);
    d.append(" against ");
    d.append(subject);
  }
}








