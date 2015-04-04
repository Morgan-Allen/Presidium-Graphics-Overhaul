


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;



//  TODO:  There's a problem here in the case where unexplored areas lie in
//  impossible-to-reach areas (such as islands in the middle of an ocean.)
//  You'll need to set up large-scale path-culling soon.

public class Exploring extends Plan implements Qualities {
  
  
  /**  Construction and save/load methods-
    */
  private static boolean
    evalVerbose = false;
  
  final static int
    TYPE_WANDER  = 0,
    TYPE_EXPLORE = 1,
    TYPE_SURVEY  = 2;
  
  final Base base;
  final int  type;
  
  private Tile    lookedAt ;
  private float   travelled;
  //private boolean sample   ;
  
  
  private Exploring(
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
    lookedAt  = (Tile) s.loadTarget();
    travelled = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(base);
    s.saveInt   (type);
    s.saveTarget(lookedAt );
    s.saveFloat (travelled);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Exploring(other, base, type, lookedAt, travelled);
  }
  
  
  public static Exploring nextWandering(Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next wandering for "+actor);
    
    final float range = actor.health.sightRange() * 2;
    Tile picked = Spacing.pickRandomTile(actor, range, actor.world());
    picked = Spacing.nearestOpenTile(picked, picked);
    if (picked == null) picked = actor.origin();
    
    final Exploring wander = new Exploring(
      actor, actor.base(), TYPE_WANDER, picked, range
    );
    if (report) {
      I.say("  Point picked: "+picked);
      I.say("  Priority: "+wander.priorityFor(actor));
    }
    return wander;
  }
  
  
  public static Exploring nextExploration(Actor actor) {
    final Tile toExplore = IntelMap.getUnexplored(
      actor.base(), actor, actor, Stage.SECTOR_SIZE, -1
    );
    if (toExplore == null) return null;
    final float range = actor.health.sightRange();
    return new Exploring(actor, actor.base(), TYPE_EXPLORE, toExplore, range);
  }
  
  
  public static Exploring nextSurvey(
    Base base, Actor actor, Target point, float range
  ) {
    Tile core      = Spacing.nearestOpenTile(point, point);
    Tile toExplore = (range > 0) ? IntelMap.getUnexplored(
      base, actor, point, Stage.SECTOR_SIZE, range
    ) : core;
    if (toExplore == null) return null;
    if (core == null) core = toExplore;
    
    final Exploring e = new Exploring(actor, base, TYPE_SURVEY, core, 0);
    e.lookedAt = toExplore;
    return e;
  }
  
  
  public static boolean canExplore(Base base, Target point) {
    final Tile toExplore = IntelMap.getUnexplored(
      base, null, point, Stage.SECTOR_SIZE, Stage.SECTOR_SIZE / 2
    );
    return toExplore != null;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Skill BASE_SKILLS[] = { SURVEILLANCE, ATHLETICS };
  final static Trait BASE_TRAITS[] = { CURIOUS, ENERGETIC, NATURALIST };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    float basePriority = CASUAL * Planet.dayValue(actor.world());
    if (type == TYPE_WANDER) basePriority = IDLE   ;
    if (type == TYPE_SURVEY) basePriority += CASUAL;
    
    //  Make this less attractive as you get further from home/safety.
    final Target haven = actor.senses.haven();
    float distFactor = (haven == null) ? 0 : Plan.rangePenalty(
      actor.base(), haven, actor
    );
    
    if (report) {
      I.say("\nExtra parameters for "+this);
      I.say("  Base priority:         "+basePriority);
      I.say("  Haven distance factor: "+distFactor  );
    }
    final float priority = priorityForActorWith(
      actor, lookedAt,
      basePriority, 0 - (distFactor + 0),
      NO_HARM, MILD_COMPETITION,
      MILD_FAIL_RISK, BASE_SKILLS,
      BASE_TRAITS, PARTIAL_DISTANCE_CHECK,
      report
    );
    return priority;
  }
  
  
  public float competence() {
    if (actor == null) return 0;
    if (PlanUtils.isArmed(actor)) return (super.competence() + 1f) / 2;
    else return super.competence();
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




