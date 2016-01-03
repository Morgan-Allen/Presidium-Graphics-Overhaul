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
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.wild.Species;



public abstract class Actor extends Mobile implements
  Owner, Accountable, Selectable
{
  
  
  /**  Field definitions, constructors and save/load functionality-
    */
  private static boolean
    verbose      = false,
    basicVerbose = false;
  
  final public ActorHealth health = new ActorHealth(this);
  final public ActorTraits traits = new ActorTraits(this);
  final public ActorSkills skills = new ActorSkills(this);
  final public ActorGear   gear   = new ActorGear  (this);
  
  final public ActorMind      mind      = initMind     ();
  final public ActorSenses    senses    = initSenses   ();
  final public ActorMotives   motives   = initMotives  ();
  final public ActorRelations relations = initRelations();
  
  private Action actionTaken;
  private Mount  mount;
  
  private Sprite disguise;
  final public Healthbar healthbar = new Healthbar();
  final public Label     label     = new Label    ();
  final public TalkFX    chat      = new TalkFX   ();
  private Stack <Sprite> statusFX = null;
  
  
  public Actor() {
    return;
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
    disguise  = ModelAsset.loadSprite(s.input());
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
    ModelAsset.saveSprite(disguise, s.output());
  }
  
  
  protected abstract ActorMind initMind();
  public abstract Species species();
  
  protected ActorSenses    initSenses   () { return new ActorSenses   (this); }
  protected ActorMotives   initMotives  () { return new ActorMotives  (this); }
  protected ActorRelations initRelations() { return new ActorRelations(this); }
  protected Pathing        initPathing  () { return new Pathing       (this); }
  
  
  public void removeWorldReferences(Stage world) {
    super.removeWorldReferences(world);
    assignAction(null);
    bindToMount (null);
    health   .onWorldExit();
    traits   .onWorldExit();
    skills   .onWorldExit();
    gear     .onWorldExit();
    mind     .onWorldExit();
    senses   .onWorldExit();
    motives  .onWorldExit();
    relations.onWorldExit();
  }
  
  
  
  /**  Dealing with items, inventory and mounting-
    */
  public ActorGear inventory() {
    return gear;
  }
  
  
  public int owningTier() {
    return TIER_PRIVATE;
  }
  
  
  public float priceFor(Traded service, boolean sold) {
    if (sold) return service.defaultPrice() * 1.5f;
    else      return service.defaultPrice() / 1.5f;
  }
  
  
  public int spaceCapacity() {
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
  
  
  public void releaseFromMount() {
    bindToMount(null);
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
      I.reportStackTrace();
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
    final Behaviour root = mind.rootBehaviour();
    if (root != null) root.interrupt(Plan.INTERRUPT_LOSE_PATH);
  }
  
  
  public Action currentAction() {
    return actionTaken;
  }
  
  
  public boolean isMoving() {
    if (actionTaken == null) return false;
    return actionTaken.isMoving();
  }
  
  
  
  /**  Life cycle and updates-
    */
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    
    if (verbose || I.logEvents()) {
      I.say("\n"+this+" ("+mind.vocation()+") ENTERING WORLD AT "+x+"/"+y);
    }
    return true;
  }
  
  
  public void exitToOffworld() {
    if (! indoors()) world.ephemera.addGhost(origin(), 1, sprite(), 0.5f, 1);
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
    bindToMount (null);
    if (normal) removeWorldReferences(world);
    super.exitWorld();
  }
  
  
  protected void updateAsMobile() {
    final boolean report = I.talkAbout == this && verbose;
    super.updateAsMobile();
    //
    //  NOTE:  We try to avoid calling anything computationally-intensive here,
    //  because mobile-updates occur at a fixed rate, leaving limited time for
    //  results to get back (particularly if several mobiles needed complex
    //  updates simultaneously.)  Instead, any updates to pathing or behaviour-
    //  evaluation get deferred to the time-sliced external scheduling system.
    final boolean OK = health.conscious() && ! doingPhysFX();
    final Action action = actionTaken;
    
    boolean needsBigUpdate = false;
    if (report) {
      I.say("\nUpdating "+this+" as mobile, action: "+action);
      I.say("  Time:      "+world.currentTime());
      I.say("  Conscious: "+OK);
    }
    
    if (action != null) {
      action.updateAction(OK, this);
    }
    if (action == null || ! OK) {
      pathing.updateTarget(null);
    }
    else if (! pathing.checkPathingOkay()) {
      if (report) I.say("  Needs fresh pathing!");
      needsBigUpdate = true;
    }
    
    
    if (mount != null) {
      final Plan activity = action == null ? null : action.parentPlan();
      mount.position(nextPosition);
      
      if (activity != null && ! mount.allowsActivity(activity)) {
        if (report) I.say("  Action not permitted by mount!");
        assignAction(null);
        needsBigUpdate = true;
      }
    }
    if (! Plan.canFollow(this, action, false)) {
      if (report) {
        I.say("  Could not follow action: "+action);
        Plan.reportPlanDetails(action, this);
      }
      assignAction(null);
      needsBigUpdate = true;
    }
    if (aboard instanceof Mobile && (pathing.nextStep() == aboard || ! OK)) {
      aboard.position(nextPosition);
    }
    
    if (needsBigUpdate && inWorld()) {
      if (report) I.say("  SCHEDULING BIG UPDATE");
      world.schedule.scheduleNow(this);
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    final boolean report = I.talkAbout == this && basicVerbose;
    //
    //  Check to see what our current condition is-
    final boolean
      OK         = health.conscious() && ! doingPhysFX(),
      checkSleep = (health.asleep() && numUpdates % 10 == 0);
    if (report) {
      I.say("\nUpdating actor!  Instant? "+instant);
      I.say("    Num updates:      "+numUpdates);
      I.say("    Current time:     "+world.currentTime());
      I.say("    Okay/Check-sleep: "+OK+"/"+checkSleep);
      I.say("    Current action:   "+actionTaken);
    }
    //
    //  Update our actions, pathing, and AI-
    if (OK || checkSleep) {
      if (! instant) {
        senses   .updateSenses();
        mind     .updateAI    (numUpdates);
        relations.updateValues(numUpdates);
        motives  .updateValues(numUpdates);
      }
      if (report) I.say("  Updated senses, AI, relations and motives.");
      final Action nextAction = mind.getNextAction();
      if (checkSleep) Resting.checkForWaking(this);
      
      else if (OK) {
        if (report) I.say("  Next action is: "+nextAction);
        if (nextAction != actionTaken) {
          if (report) I.say("  ASSIGNING ACTION");
          assignAction(nextAction);
        }
        if (! pathing.checkPathingOkay()) {
          if (report) I.say("  REFRESHING PATH!");
          pathing.refreshFullPath();
        }
      }
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
    if (! health.conscious()) {
      mind.cancelBehaviour(mind.rootBehaviour(), "Actor knocked out!");
    }
    if (health.isDead()) {
      setAsDestroyed(false);
    }
  }
  
  
  
  /**  Dealing with state changes-
    */
  //
  //  TODO:  Consider moving these elsewhere?  Like an... ActorUtils class?
  
  protected boolean doingPhysFX() {
    return actionTaken != null && actionTaken.physFX();
  }
  
  
  public void forceReflex(String animName, boolean stun) {
    if (isDoingAction("actionFall", null) || ! inWorld()) return;
    final Action falling = new Action(
      this, this, this, "actionFall",
      animName, "Stricken"
    );
    falling.setProperties(Action.NO_LOOP | Action.PHYS_FX);
    if (stun) {
      pathing.updateTarget(null);
      mind.cancelBehaviour(mind.rootBehaviour(), "Actor knocked out!");
    }
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
  
  
  
  //  TODO:  Move all these to either the Mind, Plan or PlanUtils class!
  
  public boolean isDoingAction(String actionMethod, Target target) {
    if (actionTaken == null) return false;
    if (target != null && actionTaken.subject() != target) return false;
    return actionTaken.methodName().equals(actionMethod);
  }
  
  
  public boolean isDoing(Class <? extends Plan> planClass, Target target) {
    final Target focus = planFocus(planClass, true);
    return (target == null) ? (focus != null) : (focus == target);
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
    //
    //  We allow either our current mount, and/or current action, to configure
    //  our sprite's position and rotation as desired.
    final Sprite s = sprite();
    s.position.setTo(position);
    s.rotation = rotation;
    if (mount != null) {
      mount.configureSpriteFrom(this, actionTaken, s, rendering);
    }
    else if (actionTaken != null) {
      actionTaken.configSprite(s, rendering);
    }
    position.setTo(s.position);
    rotation = s.rotation;
    super.renderAt(position, rotation, rendering);
    //
    //  We render health-bars after the main sprite, as the label/healthbar are
    //  anchored off the main sprite.  In addition, we skip this while in
    //  disguise...
    if (disguise == null) renderInformation(rendering, base());
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base);
  }
  
  
  public Sprite sprite() {
    if (disguise != null) return disguise;
    else return super.sprite();
  }
  
  
  protected PlaneFX createShadow(Sprite rendered) {
    if (disguise == rendered) return null;
    else return super.createShadow(rendered);
  }
  
  
  public void attachDisguise(Sprite app) {
    viewPosition(sprite().position);
    viewPosition(app     .position);
    world.ephemera.addGhost(this, 1, sprite(), 1.0f, 1);
    this.disguise = app;
  }
  
  
  public void detachDisguise() {
    world.ephemera.addGhost(this, 1, disguise, 1.0f, 1);
    this.disguise = null;
  }
  
  
  public boolean visibleTo(Base base) {
    if (mount != null && ! mount.actorVisible(this)) return false;
    return super.visibleTo(base);
  }
  
  
  protected void renderInformation(Rendering rendering, Base base) {
    final boolean focused = BaseUI.isSelectedOrHovered(this);
    final boolean alarm =
      health.alive() && (base == base() || focused) &&
      (health.bleeding() || health.healthLevel() < 0.25f);
    final Batch <Condition> status = traits.conditions();
    
    if (status.size() > 0) {
      //
      //  TODO:  You need a dedicated FX-class to handle this sort of thing!
      //         (Unify with the shortage-displays at venues!)
      
      if (statusFX == null) {
        statusFX = new Stack();
      }
      loop: for (Condition c : status) {
        if (c.iconModel == null) continue;
        for (Sprite s : statusFX) if (s.model() == c.iconModel) continue loop;
        statusFX.add(c.iconModel.makeSprite());
      }
      for (Sprite s : statusFX) {
        boolean used = false;
        for (Condition c : status) if (c.iconModel == s.model()) used = true;
        if (! used) statusFX.remove(s);
      }
      
      float baseTime = world.currentTime(), scale = 0.25f;
      CutoutSprite.layoutAbove(
        sprite().position, 0, height() + 0.5f, 0,
        rendering.view, scale, statusFX
      );
      for (Sprite s : statusFX) {
        float time = baseTime;
        time = (time + (statusFX.indexOf(s) * 0.25f)) % 1;
        time =  time * (1 - time) * 4;
        
        s.scale  = scale;
        s.colour = Colour.transparency(time);
        s.readyFor(rendering);
      }
    }
    else statusFX = null;
    
    if (focused || alarm) {
      label.matchTo(sprite());
      label.position.z += height() + 0.25f;
      label.phrase = fullName();
      label.readyFor(rendering);
      
      if (health.dying()) return;
      
      healthbar.matchTo(sprite());
      healthbar.hurtLevel  =  health.injuryLevel();
      healthbar.tireLevel  =  health.fatigueLevel();
      healthbar.colour     =  base().colour();
      healthbar.size       =  (35 + health.maxHealth()) / 2f;
      healthbar.position.z += height() + 0.1f;
      healthbar.readyFor(rendering);
    }
    
    if (chat.numPhrases() > 0) {
      chat.position.setTo(sprite().position);
      chat.position.z += height();
      chat.readyFor(rendering);
    }
  }
  
  
  protected float moveAnimStride() {
    return 1;
  }
  
  
  public SelectionOptions configSelectOptions(SelectionOptions info, HUD UI) {
    return SelectionOptions.configOptions(this, info, UI);
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
  
  
  public String helpInfo() {
    final Background b = mind.vocation();
    if (b != null) return b.info;
    final Species s = species();
    if (s != null) return s.info;
    return "NO HELP ON THIS ITEM";
  }
  
  
  public String objectCategory() {
    return Target.TYPE_ACTOR;
  }
  
  
  public Constant infoSubject() {
    if (mind.vocation() != null) return mind.vocation();
    else return species();
  }
  
  
  public void describeStatus(Description d, Object client) {
    if (! health.conscious()) {
      if (mount != null) mount.describeActor(this, d);
      else d.append(health.stateDesc());
      return;
    }
    if (base() != null && ! inWorld()) {
      //
      //  TODO:  Move this to the BaseCommerce or VerseJourneys class, I would
      //  suggest- either that or ActorDescription...
      final VerseJourneys journeys = base().world.offworld.journeys;
      final Vehicle ship = journeys.carries(this);
      if (ship != null && ship.inWorld()) {
        d.append("Aboard ");
        d.append(ship);
      }
      else {
        d.append("Offworld");
        float ETA = journeys.arrivalETA(this, BaseUI.currentPlayed());
        if (ETA >= 0) {
          ETA /= Stage.STANDARD_HOUR_LENGTH;
          d.append(" (ETA: "+Nums.round(ETA, 1, true)+" hours)");
        }
      }
      return;
    }
    
    final Action technique = Technique.currentTechniqueBy(this);
    if (technique != null) {
      Technique.describeAction(technique, this, d);
      return;
    }
    
    final Behaviour rootB = mind.rootBehaviour();
    final Mission mission = mind.mission();
    final boolean offMissionView = mission != null && mission != client;
    if (offMissionView && rootB == mission.cachedStepFor(this)) {
      mission.describeMission(d);
    }
    else if (rootB != null) rootB.describeBehaviour(d);
    else if (mount != null) mount.describeActor(this, d);
    else d.append("Thinking");
  }
}










