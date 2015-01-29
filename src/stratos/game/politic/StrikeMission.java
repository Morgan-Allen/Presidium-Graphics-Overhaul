/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.politic;
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
  private static boolean
    rateVerbose = BaseTactics.updatesVerbose,
    verbose     = false;
  
  
  public StrikeMission(Base base, Target subject) {
    super(
      base, subject, STRIKE_MODEL,
      "Striking at "+subject
    );
  }
  
  
  public StrikeMission(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Importance assessment-
    */
  public float rateImportance(Base base) {
    final boolean report = verbose && rateVerbose && I.matchOrNull(
      base.title(), BaseTactics.verboseBase
    );
    if (report) I.say("\nRating importance of "+this+" for "+base);
    
    final Base enemy = subject.base();
    final float dislike = 0 - base.relations.relationWith(enemy);
    if (report) I.say("  Enemy dislike:  "+dislike);
    if (dislike <= 0) return -1;
    
    float targetValue = 0;
    if (subject instanceof Venue) {
      targetValue = ((Venue) subject).ratePlacing(subject, false);
      targetValue = Nums.clamp(targetValue / BaseSetup.MAX_PLACE_RATING, 0, 1);
    }
    if (subject instanceof Actor) {
      //  TODO:  GET A VALUE FOR THIS
    }
    if (report) I.say("  Target value:   "+targetValue);
    if (targetValue <= 0) return -1;
    
    final float
      baseForce  = 1 + base .tactics.forceStrength(),
      enemyForce = 1 + enemy.tactics.forceStrength(),
      risk       = enemyForce / (baseForce + enemyForce),
      rating     = (dislike * targetValue * 2) - risk;
    if (report) {
      I.say("  Base strength:  "+baseForce );
      I.say("  Enemy strength: "+enemyForce);
      I.say("  Overall risk:   "+risk      );
      I.say("  Final rating:   "+rating    );
    }
    return rating;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    
    final Combat combat = new Combat(
      actor, (Element) subject, Combat.STYLE_EITHER, objectIndex()
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
  
  
  public void describeMission(Description d) {
    d.append("On ");
    d.append("Strike Mission", this);
    d.append(" against ");
    d.append(subject);
  }
}








