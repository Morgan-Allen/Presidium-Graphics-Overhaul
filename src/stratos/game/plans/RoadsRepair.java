

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class RoadsRepair extends Plan {
  
  
  /**  Data fields, setup and save/load methods-
    */
  private static boolean
    eventsVerbose = false,
    evalVerbose   = false;
  
  final Base base;
  final PavingMap map;
  private StageSection section;
  private Tile around;
  
  
  public RoadsRepair(Actor actor, Tile t) {
    super(actor, t.worldSection(), true, MILD_HELP);
    this.base    = actor.base();
    this.map     = base.paveRoutes.map;
    this.section = t.worldSection();
    this.around  = t;
  }
  
  
  public RoadsRepair(Session s) throws Exception {
    super(s);
    this.base    = (Base) s.loadObject();
    this.map     = base.paveRoutes.map;
    this.around  = (Tile) s.loadObject();
    this.section = (StageSection) s.loadObject();
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
  final static Trait BASE_TRAITS[] = { URBANE, ENERGETIC };
  final static Skill BASE_SKILLS[] = { ASSEMBLY, HARD_LABOUR };
  
  
  protected float getPriority() {
    if (GameSettings.paveFree) return -1;
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (Plan.competition(this, section, actor) > 0) {
      if (report) I.say("\nToo much competition for paving!  Will quit.");
      return -1;
    }
    return priorityForActorWith(
      actor, around,
      CASUAL, NO_MODIFIER,
      MILD_HELP, NO_COMPETITION, MILD_FAIL_RISK,
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
      if (t.worldSection() != this.section) continue;
      if (! map.needsPaving(t)) continue;
      float rating = 0;
      for (Tile n : t.vicinity(null)) if (n != null) {
        if (map.needsPaving(n)) rating++;
      }
      pick.compare(t, rating);
    }
    return pick.result();
  }
  
  
  private int setPavingAround(Tile t, boolean is) {
    int counter = 0;
    for (Tile n : t.vicinity(null)) if (n != null) {
      if (! map.needsPaving(n)) continue;
      if (n.owningType() > Element.ELEMENT_OWNS) continue;
      if (is) PavingMap.setPaveLevel(n, StageTerrain.ROAD_LIGHT, true);
      else PavingMap.setPaveLevel(t, StageTerrain.ROAD_NONE , false);
      counter++;
    }
    return counter;
  }
  
  
  public boolean actionPave(Actor actor, Tile t) {
    final int paved = setPavingAround(t, true);
    if (paved == 0) return false;
    //  TODO:  Deduct credits (or materials?)
    return true;
  }
  
  
  public boolean actionStrip(Actor actor, Tile t) {
    final int paved = setPavingAround(t, false);
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


