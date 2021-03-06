/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  There's a problem here in the case where unexplored areas lie in
//  impossible-to-reach areas (such as islands in the middle of an ocean.)
//  You'll need to set up large-scale path-culling soon.

public class Exploring extends Plan {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean
    evalVerbose = false,
    stepVerbose = false;
  
  final static int
    TYPE_WANDER  = 0,
    TYPE_EXPLORE = 1,
    TYPE_SURVEY  = 2,
    TYPE_SAMPLES = 3;
  
  final Base base;
  final int  type;
  
  private Tile  lookedAt ;
  private float travelled;
  
  
  protected Exploring(
    Actor actor, Base base, int type, Tile lookedAt, float maxTravel
  ) {
    super(actor, lookedAt, MOTIVE_LEISURE, NO_HARM);
    this.base      = base     ;
    this.type      = type     ;
    this.lookedAt  = lookedAt ;
    this.travelled = maxTravel;
  }
  
  
  public Exploring(Session s) throws Exception {
    super(s);
    base      = (Base) s.loadObject();
    type      = s.loadInt();
    lookedAt  = (Tile) s.loadObject();
    travelled = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(base);
    s.saveInt   (type);
    s.saveObject(lookedAt );
    s.saveFloat (travelled);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Exploring(other, base, type, lookedAt, travelled);
  }
  
  
  
  /**  External factory methods and supplementary evaluation-
    */
  public static Exploring nextWandering(Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next wandering for "+actor);
    
    final float range = actor.health.sightRange() / 2;
    Tile picked = Spacing.pickRandomTile(actor, range, actor.world());
    picked = Spacing.nearestOpenTile(picked, picked);
    if (picked == null) picked = actor.origin();
    
    final Exploring wander = new Exploring(
      actor, actor.base(), TYPE_WANDER, picked, range
    );
    if (report) {
      I.say("  Point picked: "+picked);
    }
    return wander;
  }
  
  
  public static Exploring nextExploration(Actor actor) {
    final Tile toExplore = IntelMap.getUnexplored(
      actor.base(), actor, actor, Stage.ZONE_SIZE, -1
    );
    if (toExplore == null) return null;
    final float range = actor.health.sightRange();
    return new Exploring(actor, actor.base(), TYPE_EXPLORE, toExplore, range);
  }
  
  
  public static Exploring nextSurvey(
    Base base, Actor actor, Tile point, float range
  ) {
    Tile core      = Spacing.nearestOpenTile(point, point);
    Tile toExplore = (range > 0) ? IntelMap.getUnexplored(
      base, actor, point, Stage.ZONE_SIZE, range
    ) : core;
    if (toExplore == null) return null;
    if (core == null) core = toExplore;
    
    final Exploring e = new Exploring(actor, base, TYPE_SURVEY, point, 0);
    e.lookedAt = toExplore;
    return e;
  }
  
  
  public static boolean canExplore(Base base, Target point) {
    final Tile toExplore = IntelMap.getUnexplored(
      base, null, point, Stage.ZONE_SIZE, Stage.ZONE_SIZE / 2
    );
    return toExplore != null;
  }
  
  
  public static float rateCompetence(Actor actor) {
    float chance = 1;
    chance *= actor.skills.chance(SURVEILLANCE, ROUTINE_DC);
    chance *= actor.skills.chance(ATHLETICS   , ROUTINE_DC);
    if (PlanUtils.isArmed(actor)) chance += 0.5f;
    else chance -= 0.25f;
    return Nums.clamp(chance, 0, 1);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Skill BASE_SKILLS[] = { SURVEILLANCE, ATHLETICS };
  final static Trait BASE_TRAITS[] = { CURIOUS, OUTGOING, RUGGED };
  
  
  protected float getPriority() {
    setCompetence(rateCompetence(actor));
    
    return PlanUtils.explorePriority(
      actor, lookedAt, motiveBonus(),
      type == TYPE_WANDER, competence()
    );
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (travelled < 0) return null;
    
    final Action looking = new Action(
      actor, lookedAt,
      this, "actionLook",
      Action.LOOK, "Looking at "+lookedAt.habitat().name
    );
    if (lookedAt != subject()) looking.setProperties(Action.RANGED);
    return looking;
  }
  
  
  public boolean actionLook(Actor actor, Tile point) {
    final IntelMap map = base.intelMap;
    map.liftFogAround(point, actor.health.sightRange());
    
    if (travelled > 0) {
      final float range = actor.health.sightRange();
      lookedAt = IntelMap.getUnexplored(
        base, actor, actor, range, range
      );
      if (lookedAt == null) { lookedAt = actor.origin(); travelled = -1; }
      else travelled -= Spacing.distance(actor, lookedAt) * Rand.num();
    }
    else travelled = -1;
    return false;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_WANDER) {
      d.append("Wandering");
    }
    if (type == TYPE_EXPLORE) {
      d.append("Exploring ");
      d.append(lookedAt.habitat().name);
    }
    if (type == TYPE_SURVEY) {
      d.append("Surveying ");
      d.append(lookedAt);
    }
  }
}




