/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.wild.Species;



public abstract class Actor extends Mobile implements
  Owner, Accountable, Selectable
{
  
  
  /**  Field definitions, constructors and save/load functionality-
    */
  private static boolean
    verbose    = false,
    bigVerbose = false;
  
  final public ActorHealth health = new ActorHealth(this);
  final public ActorTraits traits = new ActorTraits(this);
  final public ActorSkills skills = new ActorSkills(this);
  final public ActorGear   gear   = new ActorGear  (this);
  
  final public ActorMind      mind      = initMind     ();
  final public ActorSenses    senses    = initSenses   ();
  final public ActorMotives   motives   = initMotives  ();
  final public ActorRelations relations = initRelations();
  
  private Action actionTaken;
  private float  lastUpdate;
  private Mount  mount;
  private Base   base;
  
  final public Healthbar healthbar = new Healthbar();
  final public Label     label     = new Label    ();
  final public TalkFX    chat      = new TalkFX   ();
  
  
  public Actor() {
  }
  
  
  public Actor(Session s) throws Exception {
    super(s);
    
    health.loadState(s);
    traits.loadState(s);
    skills.loadState(s);
    gear  .loadState(s);
    
    mind     .loadState(s);
    senses   .loadState(s);
    motives  .loadState(s);
    relations.loadState(s);
    
    actionTaken = (Action) s.loadObject();
    mount       = (Mount ) s.loadObject();
    base        = (Base  ) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    health.saveState(s);
    traits.saveState(s);
    skills.saveState(s);
    gear  .saveState(s);
    
    mind     .saveState(s);
    senses   .saveState(s);
    motives  .saveState(s);
    relations.saveState(s);
    
    s.saveObject(actionTaken);
    s.saveObject(mount      );
    s.saveObject(base       );
  }
  
  
  protected abstract ActorMind initMind();
  public abstract Species species();
  
  protected ActorSenses    initSenses   () { return new ActorSenses   (this); }
  protected ActorMotives   initMotives  () { return new ActorMotives  (this); }
  protected ActorRelations initRelations() { return new ActorRelations(this); }
  protected Pathing        initPathing  () { return new Pathing       (this); }
  
  
  
  /**  Dealing with items, inventory and mounting-
    */
  public ActorGear inventory() {
    return gear;
  }
  
  
  public int owningTier() {
    return TIER_PRIVATE;
  }
  
  
  public float priceFor(Traded service) {
    return service.basePrice() * 2;
  }
  
  
  public int spaceFor(Traded good) {
    return (int) health.maxHealth() / 2;
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  public TalkFX chat() {
    return chat;
  }
  
  
  public void bindToMount(Mount newMount) {
    final Mount oldMount = this.mount;
    if (oldMount == newMount) return;
    this.mount = newMount;
    if (oldMount != null) oldMount.setMounted(this, false);
    if (newMount != null) newMount.setMounted(this, true );
  }
  
  
  public Mount currentMount() {
    return mount;
  }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
    final boolean report = verbose && I.talkAbout == this;
    if (report) {
      I.say("\nASSIGNING ACTION: "+I.tagHash(action));
      I.say("  Previous action: "+I.tagHash(actionTaken));
      if (actionTaken != null) I.say("  Finished? "+actionTaken.finished());
    }
    
    if (actionTaken != null) actionTaken.toggleActive(false);
    this.actionTaken = action;
    if (actionTaken != null) {
      actionTaken.toggleActive(true);
      actionTaken.updateAction(false, this);
    }
  }
  
  
  protected void pathingAbort() {
    if (actionTaken == null) return;
    final Behaviour root = mind.rootBehaviour();
    root.interrupt(Plan.INTERRUPT_LOSE_PATH);
  }
  
  
  public Action currentAction() {
    return actionTaken;
  }
  
  
  public boolean isMoving() {
    if (actionTaken == null) return false;
    return actionTaken.isMoving();
  }
  
  
  public void assignBase(Base base) {
    if (! inWorld()) { this.base = base; return; }
    final Tile o = origin();
    world.presences.togglePresence(this, o, false);
    this.base = base;
    world.presences.togglePresence(this, o, true);
  }
  
  
  public Base base() {
    return base;
  }
  
  
  
  /**  Life cycle and updates-
    */
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (base == null) I.complain("ACTOR MUST HAVE BASE ASSIGNED: "+this);
    if (! super.enterWorldAt(x, y, world)) return false;
    
    if (verbose || I.logEvents()) {
      I.say("\n"+this+" ("+mind.vocation()+") ENTERING WORLD AT "+x+"/"+y);
    }
    return true;
  }
  
  
  public void exitToOffworld() {
    exitWorld(false);
  }
  
  
  public void exitWorld() {
    exitWorld(true);
  }
  
  
  private void exitWorld(boolean normal) {
    if (verbose || I.logEvents()) {
      I.say("\n"+this+" ("+mind.vocation()+") EXITING WORLD FROM "+origin());
      I.say("  Last action:    "+actionTaken);
      I.say("  Going offworld? "+(! normal));
    }
    assignAction(null);
    if (normal) {
      mind.cancelBehaviour(mind.topBehaviour(), "Exiting world!");
      mind.onWorldExit();
    }
    super.exitWorld();
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile();
    final boolean report = I.talkAbout == this && verbose;
    //
    //  NOTE:  We try to avoid calling anything computationally-intensive here,
    //  because mobile-updates occur at a fixed rate, leaving limited time for
    //  results to get back (particularly if several mobiles needed complex
    //  updates simultaneously.)  Instead, any updates to pathing or behaviour-
    //  evaluation get deferred to the time-sliced external scheduling system.
    final boolean OK = health.conscious() && ! doingPhysFX();
    final Action action = actionTaken;
    
    boolean needsBigUpdate = false;
    if (report) I.say("\nUpdating "+this+" as mobile, action: "+action);
    
    if (action != null) action.updateAction(OK, this);
    
    if (action == null || ! OK) pathing.updateTarget(null);
    else if (! pathing.checkPathingOkay()) {
      if (report) I.say("  Needs fresh pathing!");
      needsBigUpdate = true;
    }
    
    if (action != null && ! Plan.canFollow(this, action)) {
      if (report) I.say("  Have completed action: "+action);
      assignAction(null);
      needsBigUpdate = true;
    }
    
    //  TODO:  Include the effects of mount-riding here:
    if (aboard instanceof Mobile && (pathing.nextStep() == aboard || ! OK)) {
      aboard.position(nextPosition);
    }
    if (needsBigUpdate) {
      if (report) I.say("  SCHEDULING BIG UPDATE");
      world.schedule.scheduleNow(this);
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    final boolean report = I.talkAbout == this && bigVerbose;
    //
    //  Check to see what our current condition is-
    final float time = world.currentTime() - lastUpdate;
    final boolean
      OK         = health.conscious() && ! doingPhysFX(),
      checkSleep = (health.asleep() && numUpdates % 10 == 0),
      tooSoon    = time < 2 && (! instant);
    if (report) {
      I.say("\nUpdating actor!  Instant? "+instant);
      I.say("    Num updates:      "+numUpdates);
      I.say("    Current time:     "+world.currentTime());
      I.say("    Okay/Check-sleep: "+OK+"/"+checkSleep);
    }
    //
    //  Update our actions, pathing, and AI-
    if ((OK || checkSleep) && (! tooSoon)) {
      senses.updateSenses();
      mind.updateAI(numUpdates);
      relations.updateValues(numUpdates);
      motives  .updateValues(numUpdates);
      if (report) I.say("  Updated senses, AI, relations and motives.");
      final Action nextAction = mind.getNextAction();
      if (checkSleep) Resting.checkForWaking(this);
      else if (OK) {
        if (report) I.say("  Next action is: "+nextAction+" vs. "+actionTaken);
        if (nextAction != actionTaken) assignAction(nextAction);
        if (! pathing.checkPathingOkay()) pathing.refreshFullPath();
      }
      lastUpdate = world.currentTime();
    }
    //
    //  Update the intel/danger maps associated with the world's bases.
    final float power = senses.powerLevel();
    if (! instant) for (Base b : world.bases()) {
      if (OK && b == base()) {
        //
        //  Actually lift fog in an area slightly ahead of the actor-
        final Vec2D heads = new Vec2D().setFromAngle(rotation);
        heads.scale(health.sightRange() / 2f);
        heads.x += position.x;
        heads.y += position.y;
        b.intelMap.liftFogAround(heads.x, heads.y, health.sightRange());
      }
      else if (! visibleTo(b)) continue;
      final float relation = relations.valueFor(b);
      final Tile o = origin();
      b.dangerMap.accumulate(0 - power * relation, 1.0f, o.x, o.y);
    }
    //
    //  Lastly, update our basic statistics and physical properties-
    if (! instant) {
      health.updateHealth(numUpdates);
      gear  .updateGear  (numUpdates);
      traits.updateTraits(numUpdates);
      skills.updateSkills(numUpdates);
    }
    if (health.isDead()) setAsDestroyed();
  }
  
  
  
  /**  Dealing with state changes-
    */
  //
  //  TODO:  Consider moving these elsewhere?  Like an... ActorUtils class?
  
  protected boolean doingPhysFX() {
    return actionTaken != null && actionTaken.physFX();
  }
  
  
  public void enterStateKO(String animName) {
    if (isDoingAction("actionFall", null)) return;
    final Action falling = new Action(
      this, this, this, "actionFall",
      animName, "Stricken"
    );
    falling.setProperties(Action.NO_LOOP | Action.PHYS_FX);
    pathing.updateTarget(null);
    mind.cancelBehaviour(mind.rootBehaviour(), "Actor knocked out!");
    this.assignAction(falling);
  }
  
  
  protected boolean collides() {
    return health.conscious();
  }
  
  
  public boolean actionFall(Actor actor, Actor fallen) {
    return true;
  }
  
  
  public boolean actionInProgress() {
    if (actionTaken == null) return false;
    return actionTaken.isClosed() && ! actionTaken.finished();
  }
  
  
  //  TODO:  Move these to the Mind class-
  public boolean isDoingAction(String actionMethod, Target target) {
    if (actionTaken == null) return false;
    if (target != null && actionTaken.subject() != target) return false;
    return actionTaken.methodName().equals(actionMethod);
  }
  
  
  public boolean isDoing(Class <? extends Plan> planClass, Target target) {
    final Target focus = planFocus(planClass, true);
    return (target == null) ? (focus != null) : (focus == target);
  }
  
  
  public float harmIntended(Target subject) {
    if (subject == null) return 0;
    for (Behaviour b : mind.agenda()) if (b instanceof Plan) {
      final Plan root = (Plan) b;
      if (subject != null && root.subject() != subject) return 0;
      return root.harmFactor();
    }
    if (subject == actionFocus() && mind.topBehaviour() instanceof Plan) {
      return ((Plan) mind.topBehaviour()).harmFactor();
    }
    return 0;
  }
  
  
  public Target actionFocus() {
    if (actionTaken == null || ! actionTaken.hasBegun()) return null;
    return actionTaken.subject();
  }
  
  
  public Target planFocus(Class planClass, boolean active) {
    final Plan match = matchFor(planClass, active);
    return match == null ? null : match.subject();
  }
  
  
  public Plan matchFor(Class <? extends Plan> planClass, boolean active) {
    if (planClass != null && ! Plan.class.isAssignableFrom(planClass)) {
      I.complain("NOT A PLAN CLASS!");
    }
    else if (planClass == null) planClass = Plan.class;
    
    for (Behaviour b : mind.agenda()) {
      if (planClass.isAssignableFrom(b.getClass())) {
        return (Plan) b;
      }
    }
    if (! active) for (Behaviour b : mind.todoList()) {
      if (planClass.isAssignableFrom(b.getClass())) {
        return (Plan) b;
      }
    }
    return null;
  }
  
  
  public Plan matchFor(Plan matchPlan) {
    for (Behaviour b : mind.agenda()) if (matchPlan.matchesPlan(b)) {
      return (Plan) b;
    }
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderAt(
    Vec3D position, float rotation, Rendering rendering
  ) {
    final Sprite s = sprite();
    if (actionTaken != null) actionTaken.configSprite(s, rendering);
    super.renderAt(position, rotation, rendering);
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    if (mount != null) {
      mount.configureSpriteFrom(this, actionTaken, sprite());
      if (! mount.actorVisible(this)) return;
    }
    //
    //  We render health-bars after the main sprite, as the label/healthbar are
    //  anchored off the main sprite.
    super.renderFor(rendering, base);
    renderHealthbars(rendering, base);
    //
    //  Finally, if you have anything to say, render the chat bubbles.
    if (chat.numPhrases() > 0) {
      chat.position.setTo(sprite().position);
      chat.position.z += height();
      chat.readyFor(rendering);
    }
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    label.matchTo(sprite());
    label.position.z -= radius() + 0.25f;
    label.phrase = fullName();
    label.readyFor(rendering);

    if (health.dying()) return;
    
    healthbar.matchTo(sprite());
    healthbar.level = (1 - health.injuryLevel());
    healthbar.colour = base().colour();
    healthbar.size = 35;
    healthbar.position.z -= radius();
    healthbar.readyFor(rendering);
  }
  
  
  protected float moveAnimStride() {
    return 1;
  }
  
  
  public TargetOptions configInfo(TargetOptions info, BaseUI UI) {
    if (info == null) info = new TargetOptions(UI, this);
    return info;
  }

  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return;
    final boolean t = aboard() instanceof Tile;
    
    final Vec3D viewPos = viewPosition(null);
    viewPos.z = Nums.max(viewPos.z, 0);
    
    Selection.renderSimpleCircle(
      this, viewPos, rendering,
      Colour.transparency((hovered ? 0.5f : 1.0f) * (t ? 1 : 0.5f))
    );
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  

  public String toString() {
    return fullName();
  }
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this);
  }
  
  
  public String helpInfo() {
    final Background b = mind.vocation();
    if (b != null) return b.info;
    final Species s = species();
    if (s != null) return s.info;
    return "NO HELP ON THIS ITEM";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_ACTOR;
  }
  
  
  public void describeStatus(Description d) {
    if (! health.conscious()) { d.append(health.stateDesc()); return; }
    if (! inWorld()) {
      final VerseJourneys journeys = base.world.offworld.journeys;
      final Vehicle ship = journeys.carries(this);
      if (ship != null && ship.inWorld()) {
        d.append("Aboard ");
        d.append(ship);
      }
      else {
        d.append("Offworld");
        float ETA = journeys.arrivalETA(this, BaseUI.currentPlayed());
        ETA /= Stage.STANDARD_HOUR_LENGTH;
        d.append(" (ETA: "+Nums.round(ETA, 1, true)+" hours)");
      }
      return;
    }
    
    final Behaviour rootB = mind.rootBehaviour();
    final Mission mission = mind.mission();
    if (mission != null && rootB == mission.cachedStepFor(this)) {
      mission.describeMission(d);
    }
    else if (rootB != null) rootB.describeBehaviour(d);
    else d.append("Thinking");
  }
}










