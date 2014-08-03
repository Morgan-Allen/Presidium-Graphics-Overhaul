

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class RoadsRepair extends Plan {
  
  
  /**  Data fields, setup and save/load methods-
    */
  private static boolean actionVerbose = true, evalVerbose = false;
  
  final Base base;
  final PavingMap map;
  private Tile around;
  
  
  public RoadsRepair(Actor actor, Tile subject) {
    super(actor, subject);
    this.base = actor.base();
    this.map = base.paving.map;
    this.around = subject;
  }
  
  
  public RoadsRepair(Session s) throws Exception {
    super(s);
    this.base = (Base) s.loadObject();
    this.map = base.paving.map;
    this.around = (Tile) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(base);
    s.saveObject(around);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Target evaluation and prioritisation-
    */
  final static Trait BASE_TRAITS[] = { URBANE, ENERGETIC };
  final static Skill BASE_SKILLS[] = { ASSEMBLY, HARD_LABOUR };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    return super.priorityForActorWith(
      actor, around, CASUAL,
      NO_HARM, FULL_COMPETITION,
      Repairs.BASE_SKILLS, Repairs.BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
  }
  
  
  
  /**  Behaviour implementation-
    */
  //  TODO:  Use a simpler technique of just checking adjacent tiles here to
  //  get next paving increments.
  
  protected Behaviour getNextStep() {
    final boolean report = actionVerbose && I.talkAbout == actor;
    
    //final Base base = actor.base();
    //final PavingMap p = base.paving.map;
    final WorldTerrain t = actor.world().terrain();
    
    if (around == null || ! map.needsPaving(around)) {
      final Tile next = map.nextTileToPave(actor, RoadsRepair.class);
      
      if (report) {
        I.say("\nCurrent tile: "+actor.aboard());
        I.say("  Next tile to pave: "+next);
      }
      if (next == null) return null;
      else around = next;
      //return null;
    }
    
    if (t.isRoad(around)) {
      final Action strip = new Action(
        actor, around,
        this, "actionStrip",
        Action.BUILD, "Stripping "
      );
      return strip;
    }
    else {
      final Action pave = new Action(
        actor, around,
        this, "actionPave",
        Action.BUILD, "Paving "
      );
      return pave;
    }
  }
  
  
  public boolean actionPave(Actor actor, Tile t) {
    //  TODO:  Deduct credits (or materials?)
    map.setPaveLevel(t, WorldTerrain.ROAD_LIGHT);
    return true;
  }
  
  
  public boolean actionStrip(Actor actor, Tile t) {
    //  TODO:  Reclaim credits (or materials?)
    map.setPaveLevel(t, WorldTerrain.ROAD_NONE);
    return true;
  }
  
  
  
  /**  Debug and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Paving ")) {
      d.append(around);
    }
  }
}


