/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.maps.PathSearch;
import stratos.util.*;



//
//  NOTE:  Some venues may have a single fixed list of e.g, nearby tiles to
//  assess, in which case this plan simply asks the venue for that information.
//  Others may pass on a (small) random selection of possible targets, which
//  the plan itself must record.  (Hence the 'assessFromDepot' option below.)


public abstract class ResourceTending extends Plan {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean verbose = false;
  
  final protected static int
    STAGE_INIT    = -1,
    STAGE_PICKUP  =  0,
    STAGE_TEND    =  1,
    STAGE_DROPOFF =  2,
    STAGE_PROCESS =  3,
    STAGE_DONE    =  4;
  
  final Venue depot;
  final Traded harvestTypes[];
  final boolean assessFromDepot;
  
  protected boolean coop       = false;
  protected boolean useTools   = true ;
  protected Target  assessed[] = null ;
  protected Target  tended     = null ;
  
  private Suspensor tools = null      ;
  private int       stage = STAGE_INIT;
  
  
  protected ResourceTending(
    Actor actor, Venue depot, boolean depotAssess,
    Target toAssess[], Traded... harvestTypes
  ) {
    super(actor, depot == null ? actor : depot, MOTIVE_JOB, NO_HARM);
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
    
    this.assessed        = (Target[]) s.loadObjectArray(Target.class);
    this.tended          = (Target) s.loadObject();
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
    
    s.saveObjectArray(assessed       );
    s.saveObject     (tended         );
    s.saveBool       (assessFromDepot);
    
    s.saveObject(tools);
    s.saveInt   (stage);
  }
  
  
  
  /**  Priority and step evaluation-
    */
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && verbose;
    if (report) {
      I.say("\n"+actor+" getting priority for "+this+" ("+hashCode()+")");
    }
    //
    //  Priority cannot be properly assessed until the next/first step is
    //  determined.
    if (stage == STAGE_INIT) {
      getNextStep();
      if (report) I.say("  STEP PICKED: "+stage);
    }
    if (stage == STAGE_DONE) {
      if (report) I.say("  ALREADY DONE");
      return -1;
    }
    float baseMotive = 0;
    final boolean
      harvests = ! Visit.empty(harvestTypes),
      persists = persistent() && hasBegun();
    //
    //  We assign a motive bonus based on relative shortage of harvested goods
    //  and/or personal need or desire.  If you're tied to the needs to a given
    //  venue, assess urgency there.
    if (assessFromDepot && depot instanceof HarvestVenue) {
      baseMotive += ((HarvestVenue) depot).needForTending(this);
    }
    else if (harvests) {
      final boolean personal = depot == null || depot == actor.mind.home();
      for (Traded t : harvestTypes) {
        if (personal) {
          final Item sample = Item.withAmount(t, 1);
          final float motive = actor.motives.rateValue(sample);
          baseMotive += motive / PARAMOUNT;
        }
        else if (depot != null) {
          baseMotive += depot.stocks.relativeShortage(t, true);
        }
      }
      baseMotive /= harvestTypes.length;
    }
    baseMotive = Nums.max(baseMotive, persists ? 0.25f : 0);
    //
    //  We also, if possible, assess the actor's competence in relation to any
    //  relevant skills (and perhaps enjoyment.)  Then return an overall value.
    final Conversion process = tendProcess();
    if (process != null) setCompetence(process.testChance(actor, 0));
    else setCompetence(1);
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this,
      baseMotive, competence(),
      coop ? 2 : 1, NO_FAIL_RISK, enjoyTraits()
    );
    
    if (report) {
      I.say("  Competence:   "+competence());
      I.say("  Base motive:  "+baseMotive);
      I.say("  Coop?         "+coop);
      I.say("  Depot-assess? "+assessFromDepot);
      I.say("  FINAL PRIORITY: "+priority);
    }
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor && verbose;
    if (report) {
      I.say("\n"+actor+" getting step for "+this+" ("+hashCode()+")");
      I.say("  Stage: "+stage);
    }
    
    if (stage == STAGE_DONE) return null;
    //
    //  If you need to process a target and haven't picked up your tools, do
    //  so now.
    final float carried = totalCarried(), limit = useTools ? 10 : 5;
    final boolean offShift = assessFromDepot && depot.staff.offDuty(actor);
    final Target toTend = (carried >= limit || offShift) ? null : nextToTend();
    
    if (useTools && tools == null && toTend != null) {
      stage = STAGE_PICKUP;
      final Action pickup = new Action(
        actor, depot,
        this, "actionCollectTools",
        Action.REACH_DOWN, "Collecting tools"
      );
      if (report) I.say("  WILL PICKUP");
      return pickup;
    }
    //
    //  If you have tools and there's a target to tend to, do so now (assuming
    //  your load isn't full.)
    if (toTend != null) {
      stage = STAGE_TEND;
      final Action tending = new Action(
        actor, toTend,
        this, "actionTendTarget",
        Action.BUILD, "Tending to "+toTend
      );
      if (willBeBlocked(toTend)) {
        final Tile open = Spacing.pickFreeTileAround(toTend, actor);
        if (open == null) return null;
        else tending.setMoveTarget(open);
      }
      if (report) I.say("  WILL TEND "+toTend);
      return tending;
    }
    //
    //  If there's nothing left to tend to, but you have some resources
    //  gathered OR have taken out tools, return those to the depot.
    if (stage != STAGE_INIT && (carried > 0 || (tools != null && useTools))) {
      stage = STAGE_DROPOFF;
      final Action dropoff = new Action(
        actor, depot,
        this, "actionDropoff",
        Action.REACH_DOWN, "Returning"
      );
      if (report) I.say("  WILL DROPOFF");
      return dropoff;
    }
    //
    //  And if there's nothing left to do, end the activity.
    stage = STAGE_DONE;
    if (report) I.say("  AM DONE");
    return null;
  }
  
  
  protected boolean willBeBlocked(Target toTend) {
    return PathSearch.blockedBy(toTend, actor);
  }
  
  
  protected Target nextToTend() {
    final boolean report = I.talkAbout == actor && verbose;
    //
    //  Target-lists from depots tend to be long, so we don't do a fresh search
    //  unless you're in a squeezing-blood-from-stone scenario...
    if (
      tended != null && tended.inWorld() &&
      rateTarget(tended) > 0 && assessFromDepot
    ) {
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
    final Target toAssess[] = targetsToAssess(assessFromDepot);
    if (! assessFromDepot) this.assessed = toAssess;
    
    if (report) I.say("Getting next to tend: "+this);
    //
    //  Then, assess each target available and pick the highest-rated close-by.
    if (toAssess != null) for (Target t : toAssess) {
      if (t == null || ! t.inWorld()) continue;
      float rating = rateTarget(t);
      rating *= 2 / (1 + Spacing.zoneDistance(actor, t));
      
      if (rating > 0 && ! coop) for (Plan p : others) {
        if (p != this && p.actionFocus() == t) { rating = -1; break; }
      }
      
      if (report && rating > 0) I.say("  Rating for "+t+" is "+rating);
      pick.compare(t, rating);
    }
    return tended = pick.result();
  }
  
  
  protected Target[] targetsToAssess(boolean fromDepot) {
    return (fromDepot && depot instanceof HarvestVenue) ?
      ((HarvestVenue) depot).getHarvestTiles(this) :
      this.assessed
    ;
  }
  
  
  protected float totalCarried() {
    float total = 0;
    for (Traded t : harvestTypes) total += actor.gear.amountOf(t);
    return total;
  }
  
  
  protected int stage() {
    return stage;
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
    final Action a = action();
    if (TP != null && TP.performTest(actor, 0, 1, a) < 0.5f) return false;
    
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
    this.stage = STAGE_DONE;
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
  
  
  public Target tended() {
    return tended;
  }
  
  

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










