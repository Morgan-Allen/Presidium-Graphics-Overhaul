/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public abstract class Mission implements Session.Saveable, Selectable {
  
  protected static boolean
    verbose     = false,
    evalVerbose = false,
    allVisible  = true ;
  
  protected boolean shouldReport(Object about) {
    return verbose && (
      I.talkAbout == about || I.talkAbout == this
    );
  }
  
  final public static int
    TYPE_BASE_AI  = -1,
    TYPE_PUBLIC   =  0,
    TYPE_SCREENED =  1,
    TYPE_COVERT   =  2,
    TYPE_MILITARY =  3,
    LIMIT_TYPE    =  4,
    
    PRIORITY_NONE      = 0,
    PRIORITY_NOMINAL   = 1,
    PRIORITY_ROUTINE   = 2,
    PRIORITY_URGENT    = 3,
    PRIORITY_CRITICAL  = 4,
    PRIORITY_PARAMOUNT = 5,
    LIMIT_PRIORITY     = 6;
  
  final public static float REWARD_TYPE_MULTS[] = {
    0.75f, 0.5f, 0.25f, 0f
  };
  final public static String
    TYPE_DESC[] = {
      "Public Bounty", "Screened", "Covert", "Military"
    },
    PRIORITY_DESC[] = {
      "None", "Nominal", "Routine", "Urgent", "Critical", "Paramount"
    };
  final public static Integer REWARD_AMOUNTS[] = {
    0, 100, 250, 500, 1000, 2500
  };
  final public static Integer PARTY_LIMITS[] = {
    1, 3, 3, 4, 4, 5
  };
  final public static int
    MIN_PARTY_LIMIT = 3,
    AVG_PARTY_LIMIT = 4,
    MAX_PARTY_LIMIT = 5;
  
  
  final Base base;
  final Session.Saveable subject;
  
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
    Base base, Session.Saveable subject,
    CutoutModel flagModel, String description
  ) {
    if (subject == null) I.complain("CANNOT HAVE NULL SUBJECT!");
    this.base        = base;
    this.subject     = subject;
    this.flagSprite  = flagModel == null ? null : flagModel.makeSprite();
    this.description = description;
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this);
    base        = (Base) s.loadObject();
    subject     = s.loadObject();
    priority    = s.loadInt();
    missionType = s.loadInt();
    objectIndex = s.loadInt();
    inceptTime  = s.loadFloat();
    begun       = s.loadBool();
    done        = s.loadBool();
    
    for (int i = s.loadInt(); i-- > 0;) {
      final Role role = new Role();
      role.approved      = s.loadBool();
      role.applicant     = (Actor    ) s.loadObject();
      role.cached        = (Behaviour) s.loadObject();
      role.specialReward = (Pledge   ) s.loadObject();
      roles.add(role);
    }
    
    flagSprite = (CutoutSprite) ModelAsset.loadSprite(s.input());
    description = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base       );
    s.saveObject(subject    );
    s.saveInt   (priority   );
    s.saveInt   (missionType);
    s.saveInt   (objectIndex);
    s.saveFloat (inceptTime );
    s.saveBool  (begun      );
    s.saveBool  (done       );
    
    s.saveInt(roles.size());
    for (Role role : roles) {
      s.saveBool  (role.approved);
      s.saveObject(role.applicant    );
      s.saveObject(role.cached       );
      s.saveObject(role.specialReward);
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
  
  
  public Session.Saveable subject() {
    return subject;
  }
  
  
  public Target subjectAsTarget() {
    if (subject instanceof Target) return (Target) subject;
    else return null;
  }
  
  
  public static void quickSetup(
    Mission mission, int priority, int type, Actor... toAssign
  ) {
    mission.assignPriority(priority > 0 ? priority : PRIORITY_ROUTINE);
    mission.setMissionType(type     > 0 ? type     : TYPE_PUBLIC     );
    for (Actor meets : toAssign) {
      meets.mind.assignMission(mission);
      mission.setApprovalFor(meets, true);
    }
    mission.base.tactics.addMission(mission);
    mission.beginMission();
  }
  
  
  public static float rewardFor(int priority) {
    return REWARD_AMOUNTS[Nums.clamp(priority, LIMIT_PRIORITY)];
  }
  
  
  
  /**  General life-cycle, justification and setup methods-
    */
  public void updateMission() {
    if (missionType == TYPE_PUBLIC && priority > 0 && ! hasBegun()) {
      beginMission();
    }
    else if (shouldEnd()) endMission(true);
    //
    //  Remove any applicants that have abandoned the mission-
    for (Role role : roles) if (role.approved) {
      final Actor a = role.applicant;
      if (! a.health.conscious()) a.mind.assignMission(null);
    }
    //
    //  By default, we also terminate any missions that have been completely
    //  abandoned-
    if (missionType != TYPE_PUBLIC && hasBegun() && rolesApproved() == 0) {
      I.say("\nNOBODY INVOLVED IN MISSION: "+this);
      endMission(false);
    }
  }
  
  
  public void resetMission() {
    for (Role role : roles) role.applicant.mind.assignMission(null);
    roles.clear();
    begun = false;
  }
  
  
  public void assignPriority(int degree) {
    priority = Nums.clamp(degree, LIMIT_PRIORITY);
    if (inceptTime == -1) inceptTime = base.world.currentTime();
    if (I.logEvents()) {
      I.say("\nMISSION ASSIGNED PRIORITY "+priority+" ("+base+" "+this+")");
    }
  }
  
  
  public void setMissionType(int type) {
    if (this.missionType == type) return;
    this.missionType = type;
    resetMission();
    if (I.logEvents()) {
      I.say("\nMISSION ASSIGNED TYPE "+missionType+" ("+base+" "+this+")");
    }
  }
  
  
  public void setObjective(int objectIndex) {
    this.objectIndex = objectIndex;
    if (I.logEvents()) {
      I.say("\nMISSION ASSIGNED GOAL ID "+objectIndex+" ("+base+" "+this+")");
    }
  }
  
  
  public int assignedPriority() {
    return priority;
  }
  
  
  public int objective() {
    return objectIndex;
  }
  
  
  public boolean hasBegun() {
    return begun;
  }
  
 
  public boolean finished() {
    return done;
  }
  
  
  public boolean isActive() {
    return begun && ! finished();
  }
  
  
  public float timeOpen() {
    if (inceptTime == -1) return 0;
    return base.world.currentTime() - inceptTime;
  }
  
  
  public abstract float targetValue(Base base);
  public abstract float harmLevel();
  
  protected abstract boolean shouldEnd();
  protected abstract Behaviour createStepFor(Actor actor);
  
  
  
  /**  Adding and screening applicants, plus confirmation and cancellation-
    */
  class Role {
    Actor applicant;
    boolean approved;
    Behaviour cached;
    Pledge specialReward;
  }
  
  
  public boolean setSpecialRewardFor(Actor actor, Pledge reward) {
    final Role r = roleFor(actor);
    r.specialReward = reward;
    return true;
  }
  
  
  protected Role roleFor(Actor actor) {
    for (Role r : roles) if (r.applicant == actor) return r;
    return null;
  }
  
  
  public int rolesApproved() {
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
    if (done) {
      return false;
    }
    if (roleFor(actor) != null) {
      return true;
    }
    if (missionType == TYPE_COVERT) {
      return false;
    }
    if (missionType == TYPE_SCREENED && begun) {
      return false;
    }
    if (missionType == TYPE_BASE_AI) {
      return base.tactics.shouldAllow(actor, this);
    }
    return true;
  }
  
  
  public boolean visibleTo(Base player) {
    if (player != base && ! allVisible) {
      if (missionType == TYPE_COVERT ) return false;
      if (missionType == TYPE_BASE_AI) return false;
    }
    if (subject instanceof Element) {
      if (! ((Element) subject).visibleTo(player)) return false;
    }
    return true;
  }
  
  
  
  /**  Toggling applicant-permissions, plus commencement & cancellation-
    */
  //
  //  NOTE:  This method should be called within the ActorMind.assignMission
  //  method, and not independantly!
  public void setApplicant(Actor actor, boolean is) {
    final boolean report = shouldReport(actor) || I.logEvents();
    if (report) I.say("\n"+actor+" APPLIED FOR "+this+"? "+is);
    
    final Role oldRole = roleFor(actor);
    if (is) {
      if (actor.mind.mission() != this) {
        I.complain("MUST CALL assignMission() for "+actor+"!");
      }
      if (oldRole != null) return;
      Role role = new Role();
      role.applicant = actor;
      role.approved = missionType == TYPE_PUBLIC ? true : false;
      roles.add(role);
    }
    else {
      if (actor.mind.mission() == this) I.complain("MUST CALL setMission()!");
      if (oldRole == null) return;
      roles.remove(oldRole);
    }
  }
  
  
  public void setApprovalFor(Actor actor, boolean is) {
    final boolean report = shouldReport(actor) || I.logEvents();
    if (report) I.say("\n"+actor+" APPROVED FOR "+this+"? "+is);
    
    final Role role = roleFor(actor);
    if (role == null) I.complain(actor+" never applied for "+this);
    role.approved = is;
  }
  
  
  public void beginMission() {
    if (hasBegun()) return;
    begun = true;

    final boolean report = (
      verbose && BaseUI.currentPlayed() == base
    ) || I.logEvents();
    if (report) I.say("\nMISSION BEGUN: "+this);
    
    for (Role role : roles) {
      if (! role.approved) {
        final Actor rejected = role.applicant;
        rejected.mind.assignMission(null);
        if (report) I.say("  Rejected "+rejected);
      }
      else {
        final Actor active = role.applicant;
        active.mind.assignBehaviour(createStepFor(active));
        if (report) I.say("  Active "+active);
      }
    }
  }
  
  
  public void endMission(boolean withReward) {
    //
    //  Unregister yourself from the base's list of ongoing operations-
    base.tactics.removeMission(this);
    if (done) return;
    done = true;
    
    final boolean report = (
      verbose && BaseUI.currentPlayed() == base
    ) || I.logEvents();
    if (report) {
      I.say("\nMISSION COMPLETE: "+this);
    }
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
      //
      //  Be sure to de-register this mission from the actor's agenda:
      role.applicant.mind.assignMission(null);
      if (role.cached != null) role.cached.interrupt(Plan.INTERRUPT_CANCEL);
    }
    //
    //  And perform some cleanup of graphical odds and ends-
    returnSelectionAfterward();
  }
  
  
  
  /**  Behaviour implementation for the benefit of any applicants/agents:
    */
  public Behaviour nextStepFor(Actor actor, boolean create) {
    if (begun) updateMission();
    
    final Role role = roleFor(actor);
    if (done || (priority <= 0 && role == null)) return null;
    
    final Action waiting = nextWaitAction(actor, role);
    if (waiting != null) return waiting;
    
    if (role == null || ! Plan.canFollow(actor, role.cached, true)) {
      if (! create) return null;
      final Behaviour step = createStepFor(actor);
      if (role == null) return step;
      role.cached = step;
    }
    return role.cached;
  }
  
  
  private Action nextWaitAction(Actor actor, Role role) {
    if (role == null || ! role.approved) return null;
    if (actor.senses.isEmergency()) return null;
    
    for (Actor a : approved()) {
      if (a == actor || a.planFocus(null, true) != subject) continue;
      
      final float dist = Spacing.distance(a, actor);
      if (dist < Stage.ZONE_SIZE * 2.5f && dist > Stage.ZONE_SIZE / 2) {
        final Action waits = new Action(
          actor, a,
          this, "actionWait",
          Action.TALK, "Waiting for "+a
        );
        waits.setPriority  (Action.URGENT);
        waits.setProperties(Action.RANGED | Action.QUICK);
        return waits;
      }
    }
    return null;
  }
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  
  
  public Behaviour cachedStepFor(Actor actor) {
    final Role role = roleFor(actor);
    if (role == null) return null;
    return role.cached;
  }
  
  
  protected Behaviour cacheStepFor(Actor actor, Behaviour step) {
    final Role role = roleFor(actor);
    if (step != null) {
      step.priorityFor(actor);
      step.nextStepFor(actor);
    }
    if (role == null) return step;
    return role.cached = step;
  }
  
  
  public boolean valid() {
    if (finished()) return false;
    if (subjectAsTarget() == null) return true;
    return ! subjectAsTarget().destroyed();
  }
  
  
  public float priorityFor(Actor actor) {
    
    final Behaviour step = nextStepFor(actor, true);
    if (step == null) return -1;
    float priority = step.priorityFor(actor);
    return priority;
  }
  
  
  protected float basePriority(Actor actor) {
    final boolean report = I.talkAbout == actor && evalVerbose;
    if (! visibleTo(actor.base())) return -1;
    
    if (report) {
      I.say("\nEvaluating priority for "+this);
      I.say("  Mission type:  "+missionType);
      I.say("  True priority: "+priority   );
    }
    //
    //  Missions created as TYPE_BASE_AI exist to allow evaluation by base-
    //  command prior to actual execution.
    final boolean baseAI = missionType == TYPE_BASE_AI;
    final int
      partySize = rolesApproved(),
      applySize = applicants().size(),
      limit     = PARTY_LIMITS[priority];
    final Role role = roleFor(actor);
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
    rewardEval =  REWARD_AMOUNTS   [priority   ];
    rewardEval *= REWARD_TYPE_MULTS[missionType];
    //
    //  We assume a worst-case scenario for division of the reward, just to
    //  ensure that applications remain stable.  Then weight by appeal to the
    //  actor's basic motives, and return:
    if (role == null || ! role.approved) {
      if ((partySize >= limit) || (applySize > limit + partySize)) {
        if (report) I.say("  No room for application! "+partySize+"/"+limit);
        return -1;
      }
      rewardEval /= Nums.max(limit    , 1);
    }
    else {
      rewardEval /= Nums.max(partySize, 1);
    }
    float
      greedVal = actor.motives.greedPriority((int) rewardEval),
      standing = actor.mind.vocation().standing,
      loyalAdd = standing * Plan.PARAMOUNT * 1f / Backgrounds.CLASS_STRATOI,
      loyalVal = actor.relations.valueFor(base.ruler()) * loyalAdd,
      offerVal = 0,
      totalVal = Nums.max(greedVal, loyalVal);
    //
    //  If the actor has been pledged a special reward, add the value of that
    //  pledge.
    if (role != null && role.specialReward != null) {
      totalVal += offerVal = role.specialReward.valueFor(actor);
    }
    if (report) {
      I.say("  True reward total: "+REWARD_AMOUNTS[priority]);
      I.say("  Type multiplier:   "+REWARD_TYPE_MULTS[missionType]);
      I.say("  Party capacity:    "+partySize+"/"+limit);
      I.say("  Evaluated reward:  "+rewardEval);
      float pay = actor.mind.vocation().defaultSalary;
      pay /= Backgrounds.NUM_DAYS_PAY;
      I.say("  Salary per day:    "+pay);
      I.say("  Greed value:       "+greedVal);
      I.say("  Pledge value:      "+offerVal);
      I.say("  Social standing    "+standing);
      I.say("  Loyalty value:     "+loyalVal);
      I.say("  Priority value:    "+totalVal);
    }
    return totalVal;
  }
  
  
  public void interrupt(String cause) {
    //final boolean report = verbose && BaseUI.current().played() == base;
    //  TODO:  There needs to be a special-case handler for this.  You also
    //  need to identify the cancelling actor, and *only* remove them.
    
    if (I.logEvents()) I.say("\nMISSION INTERRUPTED: "+cause+" ("+this+")");
    endMission(true);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    IMG_DIR = "media/GUI/Missions/";
  final public static ImageAsset
    ALL_ICONS[] = ImageAsset.fromImages(
      Mission.class, IMG_DIR,
      "button_strike.png"     ,
      "button_recon.png"      ,
      "button_contact.png"    ,
      "button_security.png"   ,
      "button_research.png"   ,
      "mission_button_lit.png"
    ),
    STRIKE_ICON   = ALL_ICONS[0],
    RECON_ICON    = ALL_ICONS[1],
    CONTACT_ICON  = ALL_ICONS[2],
    SECURITY_ICON = ALL_ICONS[3],
    RESEARCH_ICON = ALL_ICONS[4],
    
    MISSION_ICON_LIT = ALL_ICONS[5];
  //
  //  These icons need to be worked on a little more...
  final public static CutoutModel
    ALL_MODELS[] = CutoutModel.fromImages(
      Mission.class, IMG_DIR, 1, 2, false,
      "flag_strike.gif"  ,
      "flag_recon.gif"   ,
      "flag_contact.gif" ,
      "flag_security.gif"
    ),
    ALL_NEGATIVES[] = CutoutModel.fromImages(
      Mission.class, IMG_DIR, 1, 2, false,
      "flag_strike_negative.gif"  ,
      "flag_recon_negative.gif"   ,
      "flag_contact_negative.gif" ,
      "flag_security_negative.gif"
    ),
    STRIKE_MODEL   = ALL_MODELS[0],
    RECON_MODEL    = ALL_MODELS[1],
    CONTACT_MODEL  = ALL_MODELS[2],
    SECURITY_MODEL = ALL_MODELS[3],
    
    FLAG_HIGHLIGHT = CutoutModel.fromImage(
      Mission.class, IMG_DIR+"flag_highlight.png", 1, 2
    );
  final public static float
    FLAG_SCALE = 0.25f;
  
  
  private CutoutModel positiveModel() {
    final int index = Visit.indexOf(flagSprite.model(), ALL_NEGATIVES);
    if (index == -1) return (CutoutModel) flagSprite.model();
    else return ALL_MODELS[index];
  }
  
  
  private CutoutModel negativeModel() {
    final int index = Visit.indexOf(flagSprite.model(), ALL_MODELS);
    if (index == -1) return (CutoutModel) flagSprite.model();
    else return ALL_NEGATIVES[index];
  }
  
  
  public String fullName() { return description; }
  public String toString() { return description; }
  
  
  public Composite portrait(BaseUI UI) {
    final String key = getClass().getSimpleName()+"_"+subject.hashCode();
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    final int size = SelectionPane.PORTRAIT_SIZE;
    final ImageAsset icon = iconForMission(UI);
    final Composite inset = compositeForSubject(UI);
    return Composite.imageWithCornerInset(icon, inset, size, key);
  }
  
  
  protected ImageAsset iconForMission(BaseUI UI) {
    final CutoutModel flagModel = positiveModel();
    int flagIndex = Visit.indexOf(flagModel, ALL_MODELS);
    final ImageAsset icon = ALL_ICONS[flagIndex];
    return icon;
  }
  
  
  protected Composite compositeForSubject(BaseUI UI) {
    if (subject instanceof Selectable) {
      final Selectable s = (Selectable) subject;
      return s.portrait(UI);
    }
    return null;
  }
  
  
  public SelectionOptions configSelectOptions(
    SelectionOptions info, BaseUI UI
  ) {
    return null;
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    if (panel == null) panel = new MissionPane(UI, this);
    final MissionPane MP = (MissionPane) panel;
    
    if (UI.played() == base) {
      return MP.configOwningPanel();
    }
    else if (allVisible || missionType == TYPE_PUBLIC) {
      return MP.configPublicPanel();
    }
    else if (missionType == TYPE_SCREENED) {
      return MP.configScreenedPanel();
    }
    else return panel;
  }
  
  
  public Target selectionLocksOn() {
    if (visibleTo(BaseUI.currentPlayed())) return subjectAsTarget();
    return null;
  }
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this);
  }
  
  
  public Sprite flagSprite() {
    return flagSprite;
  }
  
  
  public void renderFlag(Rendering rendering) {
    if (flagSprite == null) return;
    flagSprite.scale = FLAG_SCALE;
    float glow = BaseUI.isHovered(this) ? 0.5f : 0;
    
    if (BaseUI.currentPlayed() != base) {
      flagSprite.setModel(negativeModel());
      flagSprite.colour = new Colour(base.colour());
      flagSprite.colour.blend(Colour.WHITE, 0.5f);
      flagSprite.colour.calcFloatBits();
    }
    else {
      flagSprite.setModel(positiveModel());
      flagSprite.colour = new Colour(Colour.WHITE);
    }
    
    flagSprite.readyFor(rendering);
    
    if (glow > 0) {
      CutoutSprite glowSprite = (CutoutSprite) FLAG_HIGHLIGHT.makeSprite();
      glowSprite.matchTo(flagSprite);
      glowSprite.colour = Colour.transparency(glow);
      glowSprite.readyFor(rendering);
    }
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (! visibleTo(BaseUI.currentPlayed())) return;
    
    if (subject instanceof Selectable) {
      ((Selectable) subject).renderSelection(rendering, hovered);
      return;
    }
    else if (subject instanceof Element) {
      BaseUI.current().selection.renderCircleOnGround(
        rendering, (Element) subject, hovered
      );
    }
    else return;
  }
  
  
  private void returnSelectionAfterward() {
    if (! BaseUI.paneOpenFor(this)) return;
    final BaseUI UI = BaseUI.current();
    
    if (visibleTo(UI.played()) && (subject instanceof Selectable)) {
      UI.selection.pushSelection((Selectable) subject);
    }
    else {
      UI.selection.pushSelection(null);
      UI.clearInfoPane();
    }
  }
  
  
  public String helpInfo() {
    if (! applicants().empty()) return description;
    if (! visibleTo(BaseUI.currentPlayed())) return
      "The target's current location is unknown.  This mission cannot proceed "+
      "until they are found.";
    if (missionType == TYPE_PUBLIC  ) return
      "This mission is a public bounty, open to all comers.";
    if (missionType == TYPE_SCREENED) return
      "This is a screened mission.  Applicants will be subject to your "+
      "approval before they can embark.";
    if (missionType == TYPE_COVERT  ) return
      "This is a covert mission.  No agents or citizens will apply "+
      "unless recruited by interview.";
    if (missionType == TYPE_MILITARY) return
      "This is a military operation.  You may conscript any members of your "+
      "standing armed forces to join.";
    return description;
  }
  
  
  public String objectCategory() {
    return Target.TYPE_MISSION;
  }
  
  
  public Constant infoSubject() {
    //  TODO:  Add some basic info here!
    return null;
  }

  
  public abstract void describeMission(Description d);
  
  
  public String progressDescriptor() {
    if (hasBegun()) return "In Progress";
    else return "Recruiting";
  }
}














