

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Division of responsibility here isn't nearly efficient enough.  Work
//  on that.


public class RoadsRepair extends Plan {
  
  
  /**  Data fields, setup and save/load methods-
    */
  private static boolean actionVerbose = false, evalVerbose = false;
  
  final Base base;
  final PavingMap map;
  private Tile around;
  
  
  public RoadsRepair(Actor actor, Tile t) {
    super(actor, actor.world().sections.sectionAt(t.x, t.y), true);
    this.base = actor.base();
    this.map = base.paving.map;
    this.around = t;
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
      actor, around, ROUTINE,
      NO_HARM, FULL_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = actionVerbose && I.talkAbout == actor;
    final WorldTerrain t = actor.world().terrain();
    
    if (around == null || ! map.needsPaving(around)) {
      Tile next = null;
      if (next == null) next = nextLocalTile();
      if (next == null) next = map.nextTileToPave(actor, RoadsRepair.class);
      
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
  
  
  private Tile nextLocalTile() {
    final World world = actor.world();
    final Tile centre = world.tileAt(actor);
    
    for (Tile t : centre.vicinity(Spacing.tempT9)) {
      if (t == null || ! map.needsPaving(t)) continue;
      return t;
    }
    return null;
  }
  
  
  public boolean actionPave(Actor actor, Tile t) {
    //  TODO:  Deduct credits (or materials?)
    PavingMap.setPaveLevel(t, WorldTerrain.ROAD_LIGHT);
    return true;
  }
  
  
  public boolean actionStrip(Actor actor, Tile t) {
    //  TODO:  Reclaim credits (or materials?)
    PavingMap.setPaveLevel(t, WorldTerrain.ROAD_NONE );
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


