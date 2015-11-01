

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Consider being able to shift focus from one section to another?

public class RoadsRepair extends Plan {
  
  
  /**  Data fields, setup and save/load methods-
    */
  private static boolean
    stepsVerbose = false,
    evalVerbose  = false;
  
  final Base base;
  final PavingMap map;
  private Tile around;
  private StageRegion section;
  
  
  public RoadsRepair(Actor actor, Tile t) {
    super(actor, t.worldSection(), MOTIVE_JOB, NO_HARM);
    this.base    = actor.base();
    this.map     = base.transport.map;
    this.section = t.worldSection();
    this.around  = t;
  }
  
  
  public RoadsRepair(Session s) throws Exception {
    super(s);
    this.base    = (Base) s.loadObject();
    this.map     = base.transport.map;
    this.around  = (Tile) s.loadObject();
    this.section = (StageRegion) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(base  );
    s.saveObject(around);
    s.saveObject(section);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Target evaluation and prioritisation-
    */
  final static Trait BASE_TRAITS[] = { METICULOUS, ENERGETIC };
  
  
  protected float getPriority() {
    if (GameSettings.paveFree) return -1;
    
    setCompetence(successChanceFor(actor));
    return PlanUtils.jobPlanPriority(
      actor, this, 1, competence(), 0, MILD_FAIL_RISK, BASE_TRAITS
    );
  }
  
  
  //  TODO:  Merge this with the Repair class for simplicity..?
  public float successChanceFor(Actor actor) {
    float chance = 1;
    chance *= actor.skills.chance(HARD_LABOUR, 0);
    chance *= actor.skills.chance(ASSEMBLY   , 5);
    return (chance + 1) / 2;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    final StageTerrain t = actor.world().terrain();
    
    if (report) {
      I.say("\nCurrent tile: "+around);
      I.say("  Needs paving? "+map.needsPaving(around));
    }
    
    if (around == null || ! map.needsPaving(around)) {
      Tile next = null;
      if (next == null && hasBegun()) next = nextLocalTile();
      if (next == null) next = map.nextTileToPave(actor, section);
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
      strip.setMoveTarget(Spacing.nearestOpenTile(around, around));
      return strip;
    }
    else {
      final Action pave = new Action(
        actor, around,
        this, "actionPave",
        Action.BUILD, "Paving "
      );
      pave.setMoveTarget(Spacing.nearestOpenTile(around, around));
      return pave;
    }
  }
  
  
  private Tile nextLocalTile() {
    final Tile o = actor.origin();
    final Box2D area = o.area(null).expandBy(2);
    final Pick <Tile> pick = new Pick <Tile> ();
    
    for (Tile t : o.world.tilesIn(area, true)) {
      if (t.worldSection() != this.section || ! map.needsPaving(t)) continue;
      float rating = 0;
      for (Tile n : t.vicinity(null)) if (n != null) {
        if (map.needsPaving(n)) rating++;
      }
      pick.compare(t, rating);
    }
    return pick.result();
  }
  
  
  public static int updatePavingAround(Tile t, Base base) {
    int counter = 0;
    final Batch <Tile> toPave = new Batch <Tile> ();
    toPave.add(t);
    for (Tile n : t.edgeAdjacent(null)) if (n != null) toPave.add(n);
    
    final PavingMap map = base.transport.map;
    for (Tile n : toPave) {
      if (! map.needsPaving(n)) continue;
      final boolean pave = ! PavingMap.isRoad(n);
      if (pave) PavingMap.setPaveLevel(n, StageTerrain.ROAD_LIGHT, true );
      else      PavingMap.setPaveLevel(n, StageTerrain.ROAD_NONE , false);
      counter++;
    }
    return counter;
  }
  
  
  public boolean actionPave(Actor actor, Tile t) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nPaving tile: "+t);
      I.say("  Actor at:   "+actor.origin());
      I.say("  Is paved?   "+(t.pathType() == Tile.PATH_ROAD));
      I.say("  On top:     "+t.above());
      I.say("  Can pave?   "+t.canPave());
      I.say("  Habitat ok? "+t.habitat().pathClear);
    }
    
    final int paved = updatePavingAround(t, base);
    if (paved == 0) return false;
    //  TODO:  Deduct credits (or materials?)
    return true;
  }
  
  
  public boolean actionStrip(Actor actor, Tile t) {
    final int paved = updatePavingAround(t, base);
    if (paved == 0) return false;
    //  TODO:  Reclaim credits (or materials?)
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


