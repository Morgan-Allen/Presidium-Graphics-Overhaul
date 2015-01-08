

package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.Venue;
import stratos.game.wild.Species;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.start.PlayLoop;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Different mission types need to have different options.
/*
MISSION OPTIONS
  Public Contract  Screened  Covert
  Idle Casual Routine Urgent Critical
  Base Payment:  100/250/500/1000/2500

  Credits.  Goods.  Rare Artifacts.
  Obedience.  Loyalty.  Blackmail.
  Promotion.  Marriage.  Legislation.
  
  
//  TODO:  You must create a location interface.
//  Cash.  Promotion.  Artifact.
//  Policy.  Pardon.  Marriage.
//  Declare Mission.
//  Under Orders.


RECON FACTORS
  Area to explore.  Populated/wild area.  Bugs & sampling.

STRIKE FACTORS
  Degree of force.  Retreat policy.  Target vulnerability.

SECURITY FACTORS
  Patrol duration.  Client emergencies.  Defend vs. scout.

CONTACT FACTORS
  Service asked vs. tribute offered.  Fear.  Interview.


Also apply to off-world missions-

  Request/demand support (money/goods/troops)
  Supply request/demands (money/goods/troops)
  Marriage alliance / Trade envoy / Secure Peace
  Launch raid / Capture/rescue / Exploring/Spies


//  TODO:  Add option to visit your household when recruiting members.
//*/


public abstract class Mission implements
  Behaviour, Session.Saveable, Selectable
{
  
  private static boolean
    verbose     = false,
    evalVerbose = false,
    allVisible  = true ;
  
  final public static int
    TYPE_BASE_AI  = -1,
    TYPE_PUBLIC   =  0,
    TYPE_SCREENED =  1,
    TYPE_COVERT   =  2,
    LIMIT_TYPE    =  3,
    
    PRIORITY_NONE      = 0,
    PRIORITY_NOMINAL   = 1,
    PRIORITY_ROUTINE   = 2,
    PRIORITY_URGENT    = 3,
    PRIORITY_CRITICAL  = 4,
    PRIORITY_PARAMOUNT = 5,
    LIMIT_PRIORITY     = 6;
  
  final static float REWARD_TYPE_MULTS[] = {
    0.75f, 0.5f, 0.25f
  };
  final static String
    TYPE_DESC[] = {
      "Public Contract", "Screened", "Covert"
    },
    PRIORITY_DESC[] = {
      "None", "Nominal", "Routine", "Urgent", "Critical", "Paramount"
    };
  final static Integer REWARD_AMOUNTS[] = {
    0, 100, 250, 500, 1000, 2500
  };
  final static Integer PARTY_LIMITS[] = {
    1, 3, 3, 4, 4, 5
  };
  final public static int
    MAX_PARTY_LIMIT = 5;
  
  
  final Base base;
  final Target subject;
  
  final Stack <Role> roles = new Stack <Role> ();
  private int
    priority,
    missionType,
    objectIndex;
  private float
    inceptTime = -1;
  private boolean
    begun = false,
    done  = false;
  
  final CutoutSprite flagSprite;
  final String description;
  
  
  
  protected Mission(
    Base base, Target subject,
    CutoutModel flagModel, String description
  ) {
    if (subject == null) I.complain("CANNOT HAVE NULL SUBJECT!");
    this.base = base;
    this.subject = subject;
    this.flagSprite = (CutoutSprite) flagModel.makeSprite();
    this.description = description;
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this);
    base        = (Base) s.loadObject();
    subject     = s.loadTarget();
    priority    = s.loadInt();
    missionType = s.loadInt();
    objectIndex = s.loadInt();
    inceptTime  = s.loadFloat();
    begun       = s.loadBool();
    done        = s.loadBool();
    
    for (int i = s.loadInt(); i-- > 0;) {
      final Role role = new Role();
      role.applicant = (Actor) s.loadObject();
      role.approved  = s.loadBool();
      role.cached   = (Behaviour) s.loadObject();
      roles.add(role);
    }
    
    flagSprite = (CutoutSprite) ModelAsset.loadSprite(s.input());
    description = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base       );
    s.saveTarget(subject    );
    s.saveInt   (priority   );
    s.saveInt   (missionType);
    s.saveInt   (objectIndex);
    s.saveFloat (inceptTime );
    s.saveBool  (begun      );
    s.saveBool  (done       );
    
    s.saveInt(roles.size());
    for (Role role : roles) {
      s.saveObject(role.applicant);
      s.saveBool  (role.approved );
      s.saveObject(role.cached  );
    }
    
    ModelAsset.saveSprite(flagSprite, s.output());
    s.saveString(description);
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (p.getClass() != this.getClass()) return false;
    final Mission other = (Mission) p;
    if (other.base    != this.base   ) return false;
    if (other.subject != this.subject) return false;
    return true;
  }
  
  
  public int missionType() {
    return missionType;
  }
  
  
  public Base base() {
    return base;
  }
  
  
  public Target subject() {
    return subject;
  }
  
  
  public Target selectionLocksOn() {
    return subject;
  }
  
  
  
  /**  General life-cycle, justification and setup methods-
    */
  public abstract float rateImportance(Base base);
  
  
  protected float successChance() {
    float sumChances = 0;
    for (Role r : roles) if (r.cached instanceof Plan) {
      sumChances += ((Plan) r.cached).successChanceFor(r.applicant);
    }
    return sumChances;
  }

  
  public void updateMission() {
    if (missionType == TYPE_PUBLIC && priority > 0 && ! hasBegun()) {
      beginMission();
    }
    else if (shouldEnd()) endMission(true);
    //
    //  By default, we also terminate any missions that have been completely
    //  abandoned-
    if (hasBegun() && missionType != TYPE_PUBLIC) {
      boolean anyActive = false;
      for (Role r : roles) if (r.cached instanceof Plan) {
        if (r.applicant.matchFor((Plan) r.cached) != null) anyActive = true;
      }
      else anyActive = true;
      if (! anyActive) endMission(false);
    }
  }
  
  
  public void resetMission() {
    for (Role role : roles) role.applicant.mind.assignMission(null);
    begun = false;
  }
  
  
  public void assignPriority(int degree) {
    priority = Nums.clamp(degree, LIMIT_PRIORITY);
    if (inceptTime == -1) inceptTime = base.world.currentTime();
  }
  
  
  public void setMissionType(int type) {
    if (this.missionType == type) return;
    this.missionType = type;
    resetMission();
  }
  
  
  public void setObjective(int objectIndex) {
    this.objectIndex = objectIndex;
  }
  
  
  public int assignedPriority() {
    return priority;
  }
  
  
  protected int objectIndex() {
    return objectIndex;
  }
  
  
  public boolean hasBegun() {
    return begun;
  }
  
 
  public boolean finished() {
    return done;
  }
  
  
  public boolean persistent() {
    return true;
  }
  
  
  public boolean isActive() {
    return begun && ! finished();
  }
  
  
  public float timeOpen() {
    if (inceptTime == -1) return 0;
    return base.world.currentTime() - inceptTime;
  }
  
  
  protected abstract boolean shouldEnd();
  
  
  
  /**  Adding and screening applicants, plus confirmation and cancellation-
    */
  class Role {
    Actor applicant;
    boolean approved;
    Behaviour cached;
  }
  
  
  protected Role roleFor(Actor actor) {
    for (Role r : roles) if (r.applicant == actor) return r;
    return null;
  }
  
  
  protected int rolesApproved() {
    int count = 0;
    for (Role role : roles) if (role.approved) count++;
    return count;
  }
  
  
  public int totalApplied() {
    return roles.size();
  }
  
  
  public List <Actor> approved() {
    final List <Actor> all = new List <Actor> ();
    for (Role r : roles) if (isApproved(r.applicant)) all.add(r.applicant);
    return all;
  }
  
  
  public List <Actor> applicants() {
    final List <Actor> all = new List <Actor> ();
    for (Role r : roles) all.add(r.applicant);
    return all;
  }
  
  
  public boolean isApproved(Actor a) {
    final Role role = roleFor(a);
    if (missionType == TYPE_PUBLIC) return true;
    return role == null ? false : role.approved;
  }
  
  
  public boolean canApply(Actor actor) {
    if (done) return false;
    if (roleFor(actor) != null) return true;
    if (missionType == TYPE_BASE_AI ) return true;
    
    final Behaviour step = cachedStepFor(actor, true);
    step.priorityFor(actor);
    if (step instanceof Plan && ((Plan) step).competence() < 1) return false;
    
    if (missionType == TYPE_PUBLIC  ) return true;
    if (missionType == TYPE_SCREENED) return ! begun;
    if (missionType == TYPE_COVERT  ) return false;
    return false;
  }
  
  
  public Target applyPointFor(Actor actor) {
    if (missionType == TYPE_BASE_AI) return actor;
    if (missionType == TYPE_PUBLIC ) return actor;
    return base.HQ();
  }
  
  
  //  NOTE:  This method should be called within the ActorMind.assignMission
  //  method, and not independantly.
  public void setApplicant(Actor actor, boolean is) {
    final Role oldRole = roleFor(actor);
    if (is) {
      if (actor.mind.mission() != this) I.complain("MUST CALL setMission()!");
      if (oldRole != null) return;
      Role role = new Role();
      role.applicant = actor;
      role.approved = missionType == TYPE_PUBLIC ? true : false;
      roles.add(role);
      //I.say("Role added for "+actor+"!");
      //I.reportStackTrace();
    }
    else {
      if (actor.mind.mission() == this) I.complain("MUST CALL setMission()!");
      if (oldRole == null) return;
      roles.remove(oldRole);
    }
  }
  
  
  public void setApprovalFor(Actor actor, boolean is) {
    final Role role = roleFor(actor);
    if (role == null) I.complain(actor+" never applied for "+this);
    role.approved = is;
  }
  
  
  public void beginMission() {
    if (hasBegun()) return;
    begun = true;
    ///I.say("Beginning mission: "+this);
    
    for (Role role : roles) {
      if (! role.approved) {
        final Actor rejected = role.applicant;
        rejected.mind.assignMission(null);
      }
      else {
        final Actor active = role.applicant;
        active.mind.assignBehaviour(nextStepFor(active));
      }
    }
  }
  
  
  public void endMission(boolean withReward) {
    final boolean report = verbose && BaseUI.currentPlayed() == base;
    if (report) I.say("\nMISSION COMPLETE: "+this);
    //
    //  Unregister yourself from the base's list of ongoing operations-
    base.removeMission(this);
    done = true;
    //
    //  Determine the reward, and dispense among any agents engaged-
    final float reward;
    if ((! withReward) || this.missionType == TYPE_BASE_AI) reward = 0;
    else reward = REWARD_AMOUNTS[priority] * 1f / roles.size();
    
    for (Role role : roles) {
      if (begun && reward > 0) {
        if (report) I.say("  Dispensing "+reward+" credits to "+role.applicant);
        role.applicant.gear.incCredits(reward);
        base.finance.incCredits(0 - reward, BaseFinance.SOURCE_REWARDS);
      }
      role.applicant.mind.assignMission(null);
    }
    //
    //  And perform some cleanup of graphical odds and ends-
    returnSelectionAfterward();
  }
  
  
  
  /**  Behaviour implementation for the benefit of any applicants/agents:
    */
  protected Behaviour cachedStepFor(Actor actor, boolean create) {
    if (begun) updateMission();
    if (done) return null;
    
    final Role role = roleFor(actor);
    if (role == null) return create ? nextStepFor(actor) : null;
    final Behaviour cached = role.cached;
    if (
      cached == null || cached.finished() ||
      cached.nextStepFor(actor) == null
    ) {
      return role.cached = create ? nextStepFor(actor) : null;
    }
    return cached;
  }
  
  
  protected Behaviour cacheStepFor(Actor actor, Behaviour step) {
    final Role role = roleFor(actor);
    if (role == null) return step;
    return role.cached = step;
  }
  
  
  public boolean valid() {
    if (finished()) return false;
    return ! subject.destroyed();
  }
  
  
  public float priorityFor(Actor actor) {
    
    final Behaviour step = cachedStepFor(actor, true);
    if (step == null) return -1;
    float priority = step.priorityFor(actor);
    
    final Actor ruler = base.ruler();
    if (ruler != null) {
      priority += ROUTINE * actor.relations.valueFor(ruler);
    }
    
    if (priority < ROUTINE && actor.mind.mission() != this) return 0;
    return priority;
  }
  
  
  protected float basePriority(Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nEvaluating priority for "+this);
      I.say("  Mission type:  "+missionType);
      I.say("  True priority: "+priority   );
    }
    //
    //  Missions created as TYPE_BASE_AI exist to allow evaluation by base-
    //  command prior to actual execution.
    final boolean baseAI = missionType == TYPE_BASE_AI;
    final int partySize = rolesApproved(), limit = PARTY_LIMITS[priority];
    float rewardEval;
    if (baseAI) {
      rewardEval = Plan.ROUTINE + priority;
      if (report) I.say("  Value for Base AI: "+rewardEval);
      return rewardEval;
    }
    //
    //  By default, the basic priority depends on the size of the reward on
    //  offer for completing the task, together with a multiplier based on
    //  mission type- the more control you have, the more you must pay your
    //  candidates.
    else {
      rewardEval =  REWARD_AMOUNTS[priority];
      rewardEval *= REWARD_TYPE_MULTS[missionType];
    }
    //
    //  We assume a worst-case scenario for division of the reward, just to
    //  ensure that applications remain stable.  Then weight by appeal to the
    //  actor's basic motives, and return:
    if (! isApproved(actor)) {
      if (partySize >= limit) {
        if (report) I.say("  No room for application! "+partySize+"/"+limit);
        return -1;
      }
      rewardEval /= Nums.max(limit    , 1);
    }
    else {
      rewardEval /= Nums.max(partySize, 1);
    }
    
    float value = ActorMotives.greedPriority(actor, (int) rewardEval);
    final int standing = actor.vocation().standing;
    value *= standing * 1.5f / Backgrounds.CLASS_STRATOI;
    if (report) {
      I.say("  True reward total: "+REWARD_AMOUNTS[priority]);
      I.say("  Type multiplier:   "+REWARD_TYPE_MULTS[missionType]);
      I.say("  Party capacity:    "+partySize+"/"+limit);
      I.say("  Evaluated reward:  "+rewardEval);
      I.say("  Social standing:   "+standing);
      I.say("  Priority value:    "+value);
    }
    return value;
  }
  
  
  public int motionType(Actor actor) {
    return MOTION_ANY;
  }
  
  
  public void interrupt(String cause) {
    final boolean report = verbose && BaseUI.current().played() == base;
    //  TODO:  There needs to be a special-case handler for this.  You also
    //  need to identify the cancelling actor.
    if (report) {
      I.say("\nCancelling mission: "+this);
      I.say("  Cause: "+cause);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    IMG_DIR = "media/GUI/Missions/";
  final public static ImageAsset
    ALL_ICONS[] = ImageAsset.fromImages(
      Mission.class, IMG_DIR,
      "button_strike.png",
      "button_recon.png",
      "button_contact.png",
      "button_security.png",
      "button_summons.png",
      "mission_button_lit.png"
    ),
    STRIKE_ICON   = ALL_ICONS[0],
    RECON_ICON    = ALL_ICONS[1],
    CONTACT_ICON  = ALL_ICONS[2],
    SECURITY_ICON = ALL_ICONS[3],
    SUMMONS_ICON  = ALL_ICONS[2],
    MISSION_ICON_LIT = ALL_ICONS[5];
  //
  //  These icons need to be worked on a little more...
  final public static CutoutModel
    ALL_MODELS[] = CutoutModel.fromImages(
      Mission.class, IMG_DIR, 1, 2, false,
      "flag_strike.gif",
      "flag_recon.gif",
      "flag_contact.gif",
      "flag_security.gif"
    ),
    STRIKE_MODEL   = ALL_MODELS[0],
    RECON_MODEL    = ALL_MODELS[1],
    CONTACT_MODEL  = ALL_MODELS[2],
    SECURITY_MODEL = ALL_MODELS[3];
  
  public String fullName() { return description; }
  public String toString() { return description; }
  
  
  public Composite portrait(BaseUI UI) {
    final String key = getClass().getSimpleName()+"_"+subject.hashCode();
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    final CutoutModel flagModel = (CutoutModel) flagSprite.model();
    int flagIndex = Visit.indexOf(flagModel, ALL_MODELS);
    final ImageAsset icon = ALL_ICONS[flagIndex];
    
    final int size = SelectionInfoPane.PORTRAIT_SIZE;
    final Composite c = Composite.withSize(size, size, key);
    c.layerFromGrid(icon, 0, 0, 1, 1);
    
    if (subject instanceof Selectable) {
      final Selectable s = (Selectable) subject;
      c.layerInBounds(s.portrait(UI), 0.1f, 0.1f, 0.4f, 0.4f);
    }
    
    return c;
  }
  
  
  public TargetOptions configInfo(TargetOptions info, BaseUI UI) {
    return null;
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    //
    //  Set up the panel itself if needed, and acquire a few bits of data for
    //  reference...
    if (panel == null) panel = new SelectionInfoPane(
      UI, this, portrait(UI), true
    );
    if (UI.played() == base) return MissionDescription.configOwningPanel(
      this, panel, UI
    );
    else if (allVisible || missionType == TYPE_PUBLIC) {
      return MissionDescription.configPublicPanel(this, panel, UI);
    }
    else if (missionType == TYPE_SCREENED) {
      return MissionDescription.configScreenedPanel(this, panel, UI);
    }
    else return panel;
  }
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this, true);
  }
  
  
  public Sprite flagSprite() {
    flagSprite.scale = 0.5f;

    float alpha;
    if (BaseUI.isSelectedOrHovered(this)) alpha = 1.0f;
    else alpha = 0.75f;
    flagSprite.colour = new Colour(base.colour()).withGlow(alpha);
    
    return flagSprite;
  }
  
  
  public String helpInfo() {
    return description;
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_MISSION;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (subject instanceof Selectable) {
      ((Selectable) subject).renderSelection(rendering, hovered);
      return;
    }
    final Vec3D pos = (subject instanceof Mobile) ?
      ((Mobile) subject).viewPosition(null) :
      subject.position(null);
    
    BaseUI.current().selection.renderPlane(
      rendering, base.world,
      pos, subject.radius() + 0.5f,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE,
      true, this
    );
  }
  
  
  public boolean visibleTo(Base player) {
    if (player == base) return true;
    if (! allVisible) {
      if (missionType == TYPE_COVERT ) return false;
      if (missionType == TYPE_BASE_AI) return false;
    }
    if (player.intelMap.fogAt(subject) <= 0) return false;
    return true;
  }
  
  
  private void returnSelectionAfterward() {
    if (BaseUI.isSelected(this) && (subject instanceof Selectable)) {
      BaseUI.current().selection.pushSelection((Selectable) subject, false);
    }
  }
  
  
  protected String describeObjective(int objectIndex) {
    return objectiveDescriptions()[objectIndex];
  }
  
  
  protected abstract String[] objectiveDescriptions();
}




