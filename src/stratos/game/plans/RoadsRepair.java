

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
  private static boolean
    eventsVerbose = false,
    evalVerbose   = false;
  
  final Base base;
  final PavingMap map;
  private Tile around;
  
  
  public RoadsRepair(Actor actor, Tile t) {
    super(actor, actor.world().sections.sectionAt(t.x, t.y), true);
    this.base = actor.base();
    this.map = base.paveRoutes.map;
    this.around = t;
  }
  
  
  public RoadsRepair(Session s) throws Exception {
    super(s);
    this.base = (Base) s.loadObject();
    this.map = base.paveRoutes.map;
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
    if (GameSettings.paveFree) return 0;
    
    final boolean report = evalVerbose && I.talkAbout == actor;
    return super.priorityForActorWith(
      actor, around, CASUAL,
      NO_MODIFIER, NO_HARM,
      FULL_COMPETITION, MILD_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
  }
  
  
  //  TODO:  Merge this with the Repair class for simplicity?
  protected float successChance() {
    float chance = 1;
    chance *= actor.skills.chance(HARD_LABOUR, 0);
    chance *= actor.skills.chance(ASSEMBLY   , 5);
    return (chance + 1) / 2;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    final WorldTerrain t = actor.world().terrain();
    
    if (report) {
      I.say("\nCurrent tile: "+around);
      I.say("  Needs paving? "+map.needsPaving(around));
    }
    
    if (around == null || ! map.needsPaving(around)) {
      Tile next = null;
      if (next == null) next = nextLocalTile();
      if (next == null) next = map.nextTileToPave(actor, RoadsRepair.class);
      if (report) I.say("  Next tile to pave: "+next);
      if (next == null) return null;
      else around = next;
    }
    
    if (t.isRoad(around)) {
      final Action strip = new Action(
        actor, around,
        this, "actionStrip",
        Action.BUILD, "Stripping "
      );
      strip.setMoveTarget(Spacing.nearestOpenTile(around, actor));
      return strip;
    }
    else {
      final Action pave = new Action(
        actor, around,
        this, "actionPave",
        Action.BUILD, "Paving "
      );
      pave.setMoveTarget(Spacing.nearestOpenTile(around, actor));
      return pave;
    }
  }
  
  
  private Tile nextLocalTile() {
    for (Tile t : actor.origin().vicinity(Spacing.tempT9)) {
      if (t != null && map.needsPaving(t)) return t;
    }
    return null;
  }
  
  
  public boolean actionPave(Actor actor, Tile t) {
    if (t.owningType() > Element.ELEMENT_OWNS) return false;
    //  TODO:  Deduct credits (or materials?)
    PavingMap.setPaveLevel(t, WorldTerrain.ROAD_LIGHT, true );
    return true;
  }
  
  
  public boolean actionStrip(Actor actor, Tile t) {
    if (t.owningType() > Element.ELEMENT_OWNS) return false;
    //  TODO:  Reclaim credits (or materials?)
    PavingMap.setPaveLevel(t, WorldTerrain.ROAD_NONE , false);
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


