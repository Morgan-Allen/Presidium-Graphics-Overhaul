/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
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
    LIMIT_TYPE    =  3,
    
    PRIORITY_NONE      = 0,
    PRIORITY_NOMINAL   = 1,
    PRIORITY_ROUTINE   = 2,
    PRIORITY_URGENT    = 3,
    PRIORITY_CRITICAL  = 4,
    PRIORITY_PARAMOUNT = 5,
    LIMIT_PRIORITY     = 6;
  
  final public static float REWARD_TYPE_MULTS[] = {
    0.75f, 0.5f, 0.25f
  };
  final public static String
    TYPE_DESC[] = {
      "Public Bounty", "Screened", "Covert"
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
    s.saveTarget(subject    );
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
  
  
  public Target subject() {
    return subject;
  }
  
  
  public Target selectionLocksOn() {
    return subject;
  }
  
  
  
  /**  General life-cycle, justification and setup methods-
    */
  public abstract float rateImportance(Base base);
  
  
  //  TODO:  Make use of this
  public void toggleActive(boolean is) {
  }

  
  public void updateMission() {
    if (missionType == TYPE_PUBLIC && priority > 0 && ! hasBegun()) {
      beginMission();
    }
    else if (shouldEnd()) endMission(true);
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
      I.say("\nMISSION ASSIGNED PRIORITY "+priority+" ("+this+")");
    }
  }
  
  
  public void setMissionType(int type) {
    if (this.missionType == type) return;
    this.missionType = type;
    resetMission();
    if (I.logEvents()) {
      I.say("\nMISSION ASSIGNED TYPE "+missionType+" ("+this+")");
    }
  }
  
  
  public void setObjective(int objectIndex) {
    this.objectIndex = objectIndex;
    if (I.logEvents()) {
      I.say("\nMISSION ASSIGNED OBJECTIVE "+objectIndex+" ("+this+")");
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
  
  
  protected abstract boolean shouldEnd();
  protected abstract Behaviour createStepFor(Actor actor);
  
  
  
  /**  Adding and screening applicants, plus confirmation and cancellation-
    */
  class Role {
    Actor applicant;
    boolean approved;
    Behaviour cached;
    
    //  TODO:  USE THIS!
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
  
  
  public Target applyPointFor(Actor actor) {
    if (missionType == TYPE_BASE_AI) return actor;
    if (missionType == TYPE_PUBLIC ) return actor;
    
    //  TODO:  BE STRICTER ABOUT THIS
    if (base.HQ() == null) return actor;
    
    return base.HQ();
  }
  
  
  //  NOTE:  This method should be called within the ActorMind.assignMission
  //  method, and not independantly.
  public void setApplicant(Actor actor, boolean is) {
    final boolean report = shouldReport(actor) || I.logEvents();
    if (report) I.say("\n"+actor+" APPLIED FOR "+this+"? "+is);
    
    final Role oldRole = roleFor(actor);
    if (is) {
      if (actor.mind.mission() != this) I.complain("MUST CALL setMission()!");
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
    if (done) return;
    
    final boolean report = (
      verbose && BaseUI.currentPlayed() == base
    ) || I.logEvents();
    if (report) I.say("\nMISSION COMPLETE: "+this);
    //
    //  Unregister yourself from the base's list of ongoing operations-
    base.tactics.removeMission(this);
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
    if (done) return null;
    
    final Role role = roleFor(actor);
    if (role == null) return create ? createStepFor(actor) : null;
    final Behaviour cached = role.cached;
    if (
      cached == null || cached.finished() ||
      cached.nextStepFor(actor) == null
    ) {
      return role.cached = create ? createStepFor(actor) : null;
    }
    return cached;
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
    return ! subject.destroyed();
  }
  
  
  public float priorityFor(Actor actor) {
    
    final Behaviour step = nextStepFor(actor, true);
    if (step == null) return -1;
    float priority = step.priorityFor(actor);
    
    if (priority < Plan.ROUTINE && actor.mind.mission() != this) return 0;
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
    rewardEval =  REWARD_AMOUNTS[priority];
    rewardEval *= REWARD_TYPE_MULTS[missionType];
    //
    //  We assume a worst-case scenario for division of the reward, just to
    //  ensure that applications remain stable.  Then weight by appeal to the
    //  actor's basic motives, and return:
    if (role == null || ! role.approved) {
      if (partySize >= limit) {
        if (report) I.say("  No room for application! "+partySize+"/"+limit);
        return -1;
      }
      rewardEval /= Nums.max(limit    , 1);
    }
    else {
      rewardEval /= Nums.max(partySize, 1);
    }
    
    float value = actor.motives.greedPriority((int) rewardEval);
    final int standing = actor.vocation().standing;
    value *= standing * 1f / Backgrounds.CLASS_STRATOI;
    
    final Actor ruler = base.ruler();
    if (ruler != null) {
      value += Plan.ROUTINE * actor.relations.valueFor(ruler);
    }
    //
    //  If the actor has been pledged a special reward, add the value of that
    //  pledge.
    if (role != null && role.specialReward != null) {
      final float specialVal = role.specialReward.valueFor(actor);
      if (report) I.say("  Value for pledged reward: "+specialVal);
      value += specialVal;
    }
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
      "button_summons.png"    ,
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
    
    final CutoutModel flagModel = positiveModel();
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
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this);
  }
  
  
  //  TODO:  Pass a renderFlagAt() method instead...
  
  public Sprite flagSprite() {
    return flagSprite;
  }
  
  
  public void renderFlag(Rendering rendering) {
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
    else if (subject instanceof Element) {
      BaseUI.current().selection.renderCircleOnGround(
        rendering, (Element) subject, hovered
      );
    }
    else return;
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
      BaseUI.current().selection.pushSelection((Selectable) subject);
    }
  }

  
  public abstract void describeMission(Description d);
  
  
  public String describeObjective(int objectIndex) {
    return objectiveDescriptions()[objectIndex];
  }
  
  
  public abstract String[] objectiveDescriptions();
}














