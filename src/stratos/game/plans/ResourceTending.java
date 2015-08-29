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
import stratos.util.*;



//
//  NOTE:  Some venues may have a single fixed list of e.g, nearby tiles to
//  assess, in which case this plan simply asks the venue for that information.
//  Others may pass on a (small) random selection of possible targets, which
//  the plan itself must record.  (Hence the 'assessFromDepot' option below.)


public abstract class ResourceTending extends Plan {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final protected static int
    STAGE_INIT    = -1,
    STAGE_PICKUP  =  0,
    STAGE_TEND    =  1,
    STAGE_DROPOFF =  2,
    STAGE_PROCESS =  3,
    STAGE_DONE    =  4;
  
  final Venue depot;
  final Traded harvestTypes[];
  protected boolean coop     = false;
  protected boolean useTools = true ;
  
  //  TODO:  How do you re-calibrate motives?
  
  private Target  assessed[]      = null ;
  private Target  tended          = null ;
  private boolean assessFromDepot = false;
  
  private Suspensor tools = null      ;
  private int       stage = STAGE_INIT;
  
  
  protected ResourceTending(
    Actor actor, Venue depot, boolean depotAssess,
    Target toAssess[], Traded... harvestTypes
  ) {
    //  TODO:  What properties should this have?
    
    super(actor, depot == null ? actor : depot, NO_PROPERTIES, NO_HARM);
    this.depot           = depot;
    this.assessed        = toAssess;
    this.harvestTypes    = harvestTypes;
    this.assessFromDepot = depotAssess;
  }
  
  
  protected ResourceTending(
    Actor actor, HarvestVenue depot, Traded... harvestTypes
  ) {
    this(actor, depot, true, null, harvestTypes);
  }


  public ResourceTending(Session s) throws Exception {
    super(s);
    
    this.depot        = (Venue) s.loadObject();
    this.harvestTypes = (Traded[]) s.loadObjectArray(Traded.class);
    this.coop         = s.loadBool();
    this.useTools     = s.loadBool();
    
    this.assessed        = s.loadTargetArray(Target.class);
    this.tended          = s.loadTarget();
    this.assessFromDepot = s.loadBool();
    
    this.tools = (Suspensor) s.loadObject();
    this.stage = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject     (depot       );
    s.saveObjectArray(harvestTypes);
    s.saveBool       (coop        );
    s.saveBool       (useTools    );
    
    s.saveTargetArray(assessed       );
    s.saveTarget     (tended         );
    s.saveBool       (assessFromDepot);
    
    s.saveObject(tools);
    s.saveInt   (stage);
  }
  
  
  
  /**  Priority and step evaluation-
    */
  protected float getPriority() {
    
    //  TODO:  This needs to be customised per subclass.  I think.
    
    //
    //  Priority cannot be properly assessed until the next/first step is
    //  determined.
    if (stage == STAGE_INIT) {
      getNextStep();
    }
    if (stage == STAGE_DONE) {
      return -1;
    }
    final boolean personal = depot == null || depot == actor.mind.home();
    float baseMotive = 0;
    //
    //  We assign a motive bonus based on relative shortage of harvested goods
    //  and/or personal need or desire.
    for (Traded t : harvestTypes) {
      if (depot != null) baseMotive += depot.stocks.relativeShortage(t);
      if (personal) {
        final Item sample = Item.withAmount(t, 1);
        baseMotive += ActorMotives.rateDesire(sample, null, actor);
      }
    }
    baseMotive /= harvestTypes.length * 2;
    if (assessFromDepot) {
      final float need = ((HarvestVenue) depot).needForTending();
      if (need <= 0) return -1;
      else baseMotive += need;
    }
    else if (baseMotive <= 0) return -1;
    //
    //  We also, if possible, assess the actor's competence in relation to any
    //  relevant skills (and perhaps enjoyment.)
    final Conversion process = tendProcess();
    final Trait enjoys[] = enjoyTraits();
    if (process != null) setCompetence(process.testChance(actor, 0));
    //
    //  And finally, return an overall priority assessment:
    return PlanUtils.jobPlanPriority(
      actor, this, baseMotive, competence(),
      coop ? 1 : 0, NO_FAIL_RISK, enjoys
    );
  }
  
  
  protected Behaviour getNextStep() {
    //
    //  If you need to process a target and haven't picked up your tools, do
    //  so now.
    final float carried = totalCarried(), limit = useTools ? 10 : 5;
    final Target tended = carried >= limit ? null : nextToTend();
    if (useTools && tools == null && tended != null) {
      stage = STAGE_PICKUP;
      final Action pickup = new Action(
        actor, depot,
        this, "actionCollectTools",
        Action.REACH_DOWN, "Collecting tools"
      );
      return pickup;
    }
    //
    //  If you have tools and there's a target to tend to, do so now (assuming
    //  your load isn't full.)
    if (tended != null) {
      stage = STAGE_TEND;
      final Action tending = new Action(
        actor, tended,
        this, "actionTendTarget",
        Action.BUILD, "Tending to "+tended
      );
      final Tile open = Spacing.nearestOpenTile(tended, actor);
      tending.setMoveTarget(open);
      return tending;
    }
    //
    //  If there's nothing left to tend to, but you have some resources
    //  gathered OR have taken out tools, return those to the depot.
    if (carried > 0 || (tools != null && useTools)) {
      stage = STAGE_DROPOFF;
      final Action dropoff = new Action(
        actor, depot,
        this, "actionDropoff",
        Action.REACH_DOWN, "Returning"
      );
      return dropoff;
    }
    //
    //  And if there's nothing left to do, end the activity.
    stage = STAGE_DONE;
    return null;
  }
  
  
  protected Target nextToTend() {
    //
    //  Target-lists from depots tend to be long, so we don't do a fresh search
    //  unless you're in a squeezing-blood-from-stone scenario...
    if (tended != null && rateTarget(tended) > 0 && assessFromDepot) {
      return tended;
    }
    //
    //  Non-cooperative harvests/tending must ensure that the same target isn't
    //  assigned to more than one worker.
    final Activities activities = actor.world().activities;
    final Plan others[] = coop ? null : activities.activePlanMatches(
      depot, getClass()
    ).toArray(Plan.class);
    //
    //  If we haven't been assigned a list to begin with, use the list of
    //  tiles reserved by the depot.
    final Pick <Target> pick = new Pick <Target> (0);
    final Target toAssess[] = assessFromDepot ?
      ((HarvestVenue) depot).reserved() :
      this.assessed
    ;
    //
    //  Then, assess each target available and pick the highest-rated close-by.
    for (Target t : toAssess) {
      float rating = rateTarget(t);
      rating *= 2 / (1 + Spacing.zoneDistance(actor, t));
      
      if (rating > 0 && ! coop) for (Plan p : others) {
        if (p != this && p.actionFocus() == t) { rating = -1; break; }
      }
      pick.compare(t, rating);
    }
    return tended = pick.result();
  }
  
  
  protected float totalCarried() {
    float total = 0;
    for (Traded t : harvestTypes) total += actor.gear.amountOf(t);
    return total;
  }
  
  
  protected int stage() {
    return stage;
  }
  
  
  protected Target tended() {
    return tended;
  }
  
  
  protected Target[] assessed() {
    return assessed;
  }
  
  
  
  /**  Action interface:
    */
  public boolean actionCollectTools(Actor actor, Venue depot) {
    this.tools = new Suspensor(actor, this);
    tools.enterWorldAt(depot, actor.world());
    return true;
  }
  
  
  public boolean actionTendTarget(Actor actor, Target t) {
    final Conversion TP = tendProcess();
    if (TP != null && TP.performTest(actor, 0, 1) < 0.5f) return false;
    
    final Item got[] = afterHarvest(t);
    if (got != null) for (Item i : got) actor.gear.addItem(i);
    return true;
  }
  
  
  public boolean actionDropoff(Actor actor, Venue depot) {
    for (Traded t : harvestTypes) {
      actor.gear.transfer(t, depot);
    }
    if (tools != null) { tools.exitWorld(); tools = null; }
    
    afterDepotDisposal();
    return true;
  }
  
  
  public boolean actionProcessHarvest(Actor actor, Venue depot) {
    //
    //  TODO:  This is basically a Conversion/Manufacturing step.  Return that?
    return true;
  }
  
  
  protected abstract float rateTarget(Target t);
  protected abstract Trait[] enjoyTraits();
  protected abstract Conversion tendProcess();
  protected abstract Item[] afterHarvest(Target t);
  protected abstract void afterDepotDisposal();
  

  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stage <= STAGE_PICKUP) {
      d.append("Collecting tools at ");
      d.append(depot);
    }
    if (stage == STAGE_TEND) {
      d.append("Tending to ");
      d.append(tended);
    }
    if (stage >= STAGE_DROPOFF) {
      d.append("Returning to ");
      d.append(depot);
    }
  }
}










