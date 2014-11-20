/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.CombatUtils;
import stratos.game.plans.Resting;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.user.*;
import stratos.util.*;



public abstract class Actor extends Mobile implements
  Inventory.Owner, Accountable, Selectable
{
  
  
  /**  Field definitions, constructors and save/load functionality-
    */
  private static boolean verbose = false;
  
  final public Healthbar healthbar = new Healthbar();
  final public Label label = new Label();
  final public TalkFX chat = new TalkFX();
  
  final public ActorHealth health = new ActorHealth(this);
  final public ActorTraits traits = new ActorTraits(this);
  final public ActorSkills skills = new ActorSkills(this);
  final public ActorGear   gear   = new ActorGear  (this);
  
  final public ActorMind mind = initAI();
  final public Senses senses = initSenses();
  final public ActorRelations relations = initRelations();
  
  private Action actionTaken;
  private Base base;
  
  
  public Actor() {
  }
  
  
  public Actor(Session s) throws Exception {
    super(s);
    
    health.loadState(s);
    traits.loadState(s);
    skills.loadState(s);
    gear  .loadState(s);
    
    mind.loadState(s);
    senses.loadState(s);
    relations.loadState(s);
    
    actionTaken = (Action) s.loadObject();
    base = (Base) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    health.saveState(s);
    traits.saveState(s);
    skills.saveState(s);
    gear  .saveState(s);
    
    mind.saveState(s);
    senses.saveState(s);
    relations.saveState(s);
    
    s.saveObject(actionTaken);
    s.saveObject(base);
  }
  
  
  protected abstract ActorMind initAI();
  
  protected Senses initSenses() { return new Senses(this); }
  protected ActorRelations initRelations() { return new ActorRelations(this); }
  protected Pathing initPathing() { return new Pathing(this); }
  
  public float height() {
    return 1.0f * GameSettings.actorScale;
  }
  
  public boolean isMoving() {
    if (actionTaken == null) return false;
    return actionTaken.isMoving();
  }
  
  public Background vocation() { return null; }
  public void setVocation(Background b) {}
  public Species species() { return null; }
  
  
  
  /**  Dealing with items and inventory-
    */
  public ActorGear inventory() {
    return gear;
  }
  
  
  public boolean privateProperty() {
    return true;
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
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
    if (verbose && I.talkAbout == this) {
      I.say("  ASSIGNING ACTION: "+action);
      if (action != null) I.add("  "+action.hashCode()+"\n");
    }
    //world.activities.toggleAction(actionTaken, false);
    this.actionTaken = action;
    if (actionTaken != null) actionTaken.updateAction(false);
    //world.activities.toggleAction(actionTaken, true);
  }
  
  
  protected void pathingAbort() {
    if (actionTaken == null) return;
    final Behaviour root = mind.rootBehaviour();
    //  TODO:  This needs some work.  Ideally, behaviours (particularly
    //  missions) should have some method of handling this more gracefully.
    mind.cancelBehaviour(root);
  }
  
  
  public Action currentAction() {
    return actionTaken;
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
    return true;
  }
  
  
  public void exitWorld() {
    if (verbose) I.say(this+" IS EXITING WORLD, LAST ACTION: "+actionTaken);
    assignAction(null);
    mind.cancelBehaviour(mind.topBehaviour());
    mind.onWorldExit();
    super.exitWorld();
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile();
    final boolean OK = health.conscious();
    if (! OK) pathing.updateTarget(null);
    
    if (actionTaken != null) {
      if (! pathing.checkPathingOkay()) {
        world.schedule.scheduleNow(this);
      }
      if (actionTaken.finished()) {
        //  TODO:  RE-IMPLEMENT THIS
        //if (verbose) I.sayAbout(this, "  ACTION COMPLETE: "+actionTaken);
        //world.schedule.scheduleNow(this);
      }
      actionTaken.updateAction(OK);
    }
    
    if (OK && mind.needsUpdate()) mind.getNextAction();
    
    if (aboard instanceof Mobile && (pathing.nextStep() == aboard || ! OK)) {
      aboard.position(nextPosition);
    }
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    final boolean report = verbose && I.talkAbout == this;
    super.updateAsScheduled(numUpdates);
    //
    //  Update our basic statistics and physical properties-
    health.updateHealth(numUpdates);
    gear  .updateGear  (numUpdates);
    traits.updateTraits(numUpdates);
    skills.updateSkills(numUpdates);
    if (health.isDead()) setAsDestroyed();
    
    if (report) I.say("\nUpdating actor!");
    
    //  Check to see what our current condition is-
    final boolean
      OK = health.conscious(),
      checkSleep = (health.asleep() && numUpdates % 10 == 0);
    if (! (OK || checkSleep)) return;
    
    //  Update our actions, pathing, and AI-
    if (OK) {
      if (report) I.say("Checking pathing...");
      
      if (actionTaken == null || actionTaken.finished()) {
        assignAction(mind.getNextAction());
      }
      if (! pathing.checkPathingOkay()) {
        pathing.refreshFullPath();
      }
      senses.updateSenses();
      mind.updateAI(numUpdates);
      relations.updateValues(numUpdates);
    }
    
    //  Check to see if you need to wake up-
    if (checkSleep) {
      senses.updateSenses();
      mind.updateAI(numUpdates);
      relations.updateValues(numUpdates);
      mind.getNextAction();
      
      final Behaviour root = mind.rootBehaviour();
      final float
        wakePriority  = root == null ? 0 : root.priorityFor(this),
        sleepPriority = new Resting(this, aboard()).priorityFor(this);
      
      if (wakePriority > sleepPriority + 1 + Plan.DEFAULT_SWITCH_THRESHOLD) {
        health.setState(ActorHealth.STATE_ACTIVE);
      }
    }
    
    //  Update the intel/danger maps associated with the world's bases.
    final float power = senses.powerLevel() * 10;
    for (Base b : world.bases()) {
      if (b == base()) {
        //
        //  Actually lift fog in an area slightly ahead of the actor-
        final Vec2D heads = new Vec2D().setFromAngle(rotation);
        heads.scale(health.sightRange() / 3f);
        heads.x += position.x;
        heads.y += position.y;
        b.intelMap.liftFogAround(heads.x, heads.y, health.sightRange());
      }
      if (! visibleTo(b)) continue;
      final float relation = relations.valueFor(b);
      final Tile o = origin();
      b.dangerMap.accumulate(0 - power * relation, 1.0f, o.x, o.y);
    }
  }
  
  
  
  /**  Dealing with state changes-
    */
  //
  //  TODO:  Consider moving these elsewhere?
  
  public void enterStateKO(String animName) {
    ///I.say(this+" HAS BEEN KO'D");
    if (isDoingAction("actionFall", null)) return;
    final Action falling = new Action(
      this, this, this, "actionFall",
      animName, "Stricken"
    );
    falling.setProperties(Action.NO_LOOP);
    pathing.updateTarget(null);
    mind.cancelBehaviour(mind.rootBehaviour());
    this.assignAction(falling);
  }
  
  
  protected boolean collides() {
    return health.conscious();
  }
  
  
  public boolean actionFall(Actor actor, Actor fallen) {
    return true;
  }
  
  
  
  //  TODO:  Move these to the Mind class-
  
  public boolean isDoingAction(String actionMethod, Target target) {
    if (actionTaken == null) return false;
    if (target != null && actionTaken.subject() != target) return false;
    return actionTaken.methodName().equals(actionMethod);
  }
  
  
  public boolean isDoing(Class <? extends Plan> planClass, Target target) {
    final Target focus = focusFor(planClass);
    return (target == null) ? (focus != null) : (focus == target);
  }
  
  
  public float harmDoneTo(Target subject) {
    for (Behaviour b : mind.agenda()) if (b instanceof Plan) {
      final Plan root = (Plan) b;
      if (subject != null && root.subject() != subject) return 0;
      return root.harmFactor();
    }
    return 0;
  }
  
  
  public Target focusFor(Class <? extends Plan> planClass) {
    if (currentAction() == null || ! currentAction().hasBegun()) return null;
    final Plan match = matchFor(planClass);
    return match == null ? null : match.subject();
  }
  
  
  public Plan matchFor(Class <? extends Plan> planClass) {
    if (planClass != null && ! Plan.class.isAssignableFrom(planClass)) {
      I.complain("NOT A PLAN CLASS!");
    }
    else if (planClass == null) planClass = Plan.class;
    
    for (Behaviour b : mind.agenda()) {
      if (planClass.isAssignableFrom(b.getClass())) {
        return (Plan) b;
      }
    }
    for (Behaviour b : mind.todoList) {
      if (planClass.isAssignableFrom(b.getClass())) {
        return (Plan) b;
      }
    }
    return null;
  }
  
  
  public Plan matchFor(Plan matchPlan) {
    for (Behaviour b : mind.agenda()) if (b instanceof Plan) {
      if (matchPlan.matchesPlan((Plan) b)) {
        return (Plan) b;
      }
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
    renderHealthbars(rendering, base);
    super.renderFor(rendering, base);
    //
    //  Finally, if you have anything to say, render the chat bubbles.
    if (chat.numPhrases() > 0) {
      chat.position.setTo(sprite().position);
      chat.position.z += height();
      chat.readyFor(rendering);
    }
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    if (health.dying()) return;
    if (! (this instanceof Human)) {
      if (! BaseUI.isSelectedOrHovered(this)) return;
    }
    
    label.matchTo(sprite());
    label.position.z -= radius() + 0.25f;
    label.phrase = fullName();
    label.readyFor(rendering);
    
    healthbar.matchTo(sprite());
    healthbar.level = (1 - health.injuryLevel());
    healthbar.colour = this.base().colour;
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
    Selection.renderSimpleCircle(
      this, viewPosition(null), rendering,
      Colour.transparency((hovered ? 0.5f : 1.0f) * (t ? 1 : 0.5f))
    );
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  

  public String toString() {
    return fullName();
  }
  
  
  public void whenTextClicked() {
    BaseUI.current().selection.pushSelection(this, false);
  }
  
  
  public void describeStatus(Description d) {
    if (! health.conscious()) { d.append(health.stateDesc()); return; }
    if (! inWorld()) { d.append("Offworld"); return; }
    final Behaviour rootB = mind.rootBehaviour();
    if (rootB != null) rootB.describeBehaviour(d);
    else d.append("Thinking");
  }
}










