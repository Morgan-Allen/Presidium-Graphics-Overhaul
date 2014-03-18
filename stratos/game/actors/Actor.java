/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.planet.*;
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
  private static boolean verbose = false ;
  
  final public Healthbar healthbar = new Healthbar() ;
  final public TalkFX chat = new TalkFX() ;
  
  final public ActorHealth health = new ActorHealth(this) ;
  final public ActorTraits traits = new ActorTraits(this) ;
  final public ActorGear   gear   = new ActorGear  (this) ;
  
  final public ActorMind mind = initAI() ;
  private Action actionTaken ;
  private Base base ;
  
  
  public Actor() {
  }
  
  
  public Actor(Session s) throws Exception {
    super(s) ;
    
    health.loadState(s) ;
    traits.loadState(s) ;
    gear.loadState(s) ;
    mind.loadState(s) ;
    
    actionTaken = (Action) s.loadObject() ;
    base = (Base) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    health.saveState(s) ;
    traits.saveState(s) ;
    gear.saveState(s) ;
    mind.saveState(s) ;
    
    s.saveObject(actionTaken) ;
    s.saveObject(base) ;
  }
  
  
  protected abstract ActorMind initAI() ;
  
  protected MobileMotion initMotion() { return new MobileMotion(this) ; }
  
  public Background vocation() { return null ; }
  public void setVocation(Background b) {}
  
  public Species species() { return null ; }
  
  
  
  /**  Dealing with items and inventory-
    */
  public ActorGear inventory() {
    return gear ;
  }
  
  
  public float priceFor(Service service) {
    return service.basePrice * 2 ;
  }
  
  
  public int spaceFor(Service good) {
    return (int) health.maxHealth() / 2 ;
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
    if (verbose && I.talkAbout == this) {
      I.say("  ASSIGNING ACTION: "+action) ;
      if (action != null) I.add("  "+action.hashCode()+"\n") ;
    }
    world.activities.toggleAction(actionTaken, false) ;
    this.actionTaken = action ;
    if (actionTaken != null) actionTaken.updateAction(false) ;
    world.activities.toggleAction(actionTaken, true) ;
  }
  
  
  protected void pathingAbort() {
    if (actionTaken == null) return ;

    final Behaviour root = mind.rootBehaviour() ;
    //  TODO:  This needs some work.  Ideally, behaviours (particularly
    //  missions) should have some method of handling this more gracefully.
    mind.cancelBehaviour(root) ;
  }
  
  
  public Action currentAction() {
    return actionTaken ;
  }
  
  
  public void assignBase(Base base) {
    this.base = base ;
  }
  
  
  public Base base() {
    return base ;
  }
  
  
  
  /**  Life cycle and updates-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (base == null) I.complain("ACTOR MUST HAVE BASE ASSIGNED: "+this) ;
    if (! super.enterWorldAt(x, y, world)) return false ;
    return true ;
  }
  
  
  public void exitWorld() {
    if (verbose) I.say(this+" IS EXITING WORLD, LAST ACTION: "+actionTaken) ;
    assignAction(null) ;
    mind.cancelBehaviour(mind.topBehaviour()) ;
    mind.onWorldExit() ;
    super.exitWorld() ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    final boolean OK = health.conscious() ;
    if (! OK) motion.updateTarget(null) ;
    
    if (actionTaken != null) {
      if (! motion.checkPathingOkay()) {
        world.schedule.scheduleNow(this) ;
      }
      if (actionTaken.finished()) {
        if (verbose) I.sayAbout(this, "  ACTION COMPLETE: "+actionTaken) ;
        //world.schedule.scheduleNow(this) ;
      }
      actionTaken.updateAction(OK) ;
    }
    
    final Behaviour root = mind.rootBehaviour() ;
    if (root != null && root != actionTaken && root.finished() && OK) {
      if (verbose && I.talkAbout == this) {
        I.say("  ROOT BEHAVIOUR COMPLETE... "+root) ;
        I.say("  PRIORITY: "+root.priorityFor(this)) ;
        I.say("  NEXT STEP: "+root.nextStepFor(this)) ;
      }
      mind.cancelBehaviour(root) ;
    }
    
    if (aboard instanceof Mobile && (motion.nextStep() == aboard || ! OK)) {
      aboard.position(nextPosition) ;
    }
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    //
    //  Update our basic statistics and physical properties-
    health.updateHealth(numUpdates) ;
    gear.updateGear(numUpdates) ;
    traits.updateTraits(numUpdates) ;
    if (health.isDead()) setAsDestroyed() ;
    
    //  Check to see what our current condition is-
    final boolean
      OK = health.conscious(),
      checkSleep = (health.asleep() && numUpdates % 10 == 0) ;
    if (! (OK || checkSleep)) return ;
    
    //  Update our actions, pathing, and AI-
    if (OK) {
      if (actionTaken == null || actionTaken.finished()) {
        assignAction(mind.getNextAction()) ;
      }
      if (! motion.checkPathingOkay()) {
        motion.refreshFullPath() ;
      }
      mind.updateAI(numUpdates) ;
    }
    
    //  Check to see if you need to wake up-
    if (checkSleep) {
      mind.updateAI(numUpdates) ;
      mind.getNextAction() ;
      final Behaviour root = mind.rootBehaviour() ;
      final float
        wakePriority  = root == null ? 0 : root.priorityFor(this),
        sleepPriority = Resting.ratePoint(this, aboard(), 0) ;
      if (wakePriority + 1 > sleepPriority + Choice.DEFAULT_PRIORITY_RANGE) {
        health.setState(ActorHealth.STATE_ACTIVE) ;
      }
    }
    
    //  Update the intel/danger maps associated with the world's bases.
    final float power = Combat.combatStrength(this, null) * 10 ;
    for (Base b : world.bases()) {
      if (b == base()) {
        //
        //  Actually lift fog in an area slightly ahead of the actor-
        final Vec2D heads = new Vec2D().setFromAngle(rotation) ;
        heads.scale(health.sightRange() / 3f) ;
        heads.x += position.x ;
        heads.y += position.y ;
        b.intelMap.liftFogAround(heads.x, heads.y, health.sightRange()) ;
      }
      if (! visibleTo(b)) continue ;
      final float relation = mind.relationValue(b) ;
      b.dangerMap.impingeVal(origin(), 0 - power * relation, true) ;
    }
  }
  
  
  
  /**  Dealing with state changes-
    */
  //
  //  TODO:  Consider moving these elsewhere?
  
  public void enterStateKO(String animName) {
    ///I.say(this+" HAS BEEN KO'D") ;
    if (isDoing("actionFall", null)) return ;
    final Action falling = new Action(
      this, this, this, "actionFall",
      animName, "Stricken"
    ) ;
    motion.updateTarget(null) ;
    mind.cancelBehaviour(mind.rootBehaviour()) ;
    this.assignAction(falling) ;
  }
  
  
  public boolean actionFall(Actor actor, Actor fallen) {
    return true ;
  }
  
  
  public boolean isDoing(Class planClass, Target target) {
    if (target != null) {
      if (actionTaken == null || actionTaken.target() != target) return false ;
    }
    for (Behaviour b : mind.agenda()) {
      if (planClass.isAssignableFrom(b.getClass())) return true ;
    }
    return false ;
  }
  
  
  public Plan matchFor(Plan matchPlan) {
    for (Behaviour b : mind.agenda()) if (b instanceof Plan) {
      if (matchPlan.matchesPlan((Plan) b)) {
        return (Plan) b ;
      }
    }
    return null ;
  }
  
  
  public Plan matchFor(Class planClass) {
    for (Behaviour b : mind.agenda()) if (b instanceof Plan) {
      if (b.getClass() == planClass) {
        return (Plan) b ;
      }
    }
    return null ;
  }
  
  
  public boolean isDoing(String actionMethod, Target target) {
    if (actionTaken == null) return false ;
    if (target != null && actionTaken.target() != target) return false ;
    return actionTaken.methodName().equals(actionMethod) ;
  }
  
  
  public Target targetFor(Class planClass) {
    if (actionTaken == null) return null ;
    if (planClass != null && ! isDoing(planClass, null)) return null ;
    return actionTaken.target() ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    //
    //  ...Maybe include equipment/costume configuration here as well?
    final Sprite s = sprite() ;
    renderHealthbars(rendering, base) ;
    if (actionTaken != null) actionTaken.configSprite(s, rendering);
    super.renderFor(rendering, base) ;
    //
    //  Finally, if you have anything to say, render the chat bubbles.
    if (chat.numPhrases() > 0) {
      chat.position.setTo(s.position) ;
      chat.position.z += height() ;
      chat.readyFor(rendering);
    }
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    if (health.dying()) return;
    healthbar.level =  (1 - health.injuryLevel());
    healthbar.full = this.base().colour;
    healthbar.size = 45;
    healthbar.matchTo(sprite());
    healthbar.position.z -= radius();
    healthbar.readyFor(rendering);
  }
  
  
  protected float moveAnimStride() {
    return 1 ;
  }
  
  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, 0);
  }

  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return ;
    final boolean t = aboard() instanceof Tile ;
    Selection.renderPlane(
      rendering, viewPosition(null),
      (radius() + 0.5f) * (t ? 1 : 0.5f),
      Colour.transparency((hovered ? 0.5f : 1.0f) * (t ? 1 : 0.5f)),
      Selection.SELECT_CIRCLE
    ) ;
  }
  
  
  public Target subject() {
    return this ;
  }
  

  public String toString() {
    return fullName() ;
  }
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this, false);
  }
  
  
  public void describeStatus(Description d) {
    if (! health.conscious()) { d.append(health.stateDesc()) ; return ; }
    if (! inWorld()) { d.append("Offworld") ; return ; }
    final Behaviour rootB = mind.rootBehaviour() ;
    if (rootB != null) rootB.describeBehaviour(d) ;
    else d.append("Thinking") ;
  }
}










