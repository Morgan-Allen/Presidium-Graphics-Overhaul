/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.util.Description;



public abstract class ResourceHarvest extends Plan {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  Target toAssess[];
  Traded harvestTypes[];
  boolean coop;
  int stage;
  
  Venue depot;
  Item extracted[];
  float carryLimit;
  Suspensor tools;
  
  
  public ResourceHarvest(Actor actor, Target subject) {
    super(actor, subject, MOTIVE_JOB, NO_HARM);
  }


  public ResourceHarvest(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Priority-evaluation-
    */
  protected float getPriority() {
    return 0;
  }
  
  abstract float urgencyModifier();
  abstract Target[] targetsToAssess();
  abstract float rateTarget(Target t);
  
  
  
  /**  Step-implementation-
    */
  protected Behaviour getNextStep() {
    return null;
  }
  
  abstract boolean attemptHarvest(Target t);
  abstract Item[] afterHarvest(Target t);
  abstract void afterDepotDisposal();
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
  }
}







