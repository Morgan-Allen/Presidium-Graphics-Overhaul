

package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
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
  
  final public static int
    TYPE_PUBLIC   = 0,
    TYPE_SCREENED = 1,
    TYPE_COVERT   = 2,
    TYPE_PARTY    = 3,
    LIMIT_TYPE    = 3,
    
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
    0, 3, 3, 4, 4, 5
  };
  
  private static boolean
    verbose     = false,
    evalVerbose = true ;
  
  
  final Base base;
  final Target subject;
  
  final Stack <Role> roles = new Stack <Role> ();
  private int
    priority,
    missionType,
    objectIndex;
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
    base = (Base) s.loadObject();
    subject = s.loadTarget();
    priority = s.loadInt();
    missionType = s.loadInt();
    objectIndex = s.loadInt();
    begun = s.loadBool();
    done = s.loadBool();
    
    for (int i = s.loadInt(); i-- > 0;) {
      final Role role = new Role();
      role.applicant = (Actor) s.loadObject();
      role.approved  = s.loadBool();
      role.cached   = (Behaviour) s.loadObject();
      roles.add(role);
    }
    
    flagSprite = (CutoutSprite) ModelAsset.loadSprite(s.input());
    //flagTex = ((CutoutModel) flagSprite.model()).texture();
    description = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base);
    s.saveTarget(subject);
    s.saveInt(priority);
    s.saveInt(missionType);
    s.saveInt(objectIndex);
    s.saveBool(begun);
    s.saveBool(done);
    
    s.saveInt(roles.size());
    for (Role role : roles) {
      s.saveObject(role.applicant);
      s.saveBool  (role.approved );
      s.saveObject(role.cached  );
    }
    
    ModelAsset.saveSprite(flagSprite, s.output());
    s.saveString(description);
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
  
  
  public void resetMission() {
    for (Role role : roles) role.applicant.mind.assignMission(null);
    begun = false;
  }
  
  
  
  /**  Adding and screening applicants-
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
  
  
  protected int objectIndex() {
    return objectIndex;
  }
  
  
  public int totalApplied() {
    return roles.size();
  }
  
  
  public List <Actor> approved() {
    final List <Actor> all = new List <Actor> ();
    for (Role r : roles) if (isApproved(r.applicant)) all.add(r.applicant);
    return all;
  }
  
  
  public boolean isApproved(Actor a) {
    final Role role = roleFor(a);
    if (missionType == TYPE_PUBLIC) return true;
    return role == null ? false : role.approved;
  }
  
  
  
  /**  Public access methods for setup purposes
    */
  public void assignPriority(int degree) {
    priority = degree;
  }
  
  
  public boolean openToPublic() {
    //if (roles.size() >= PARTY_LIMITS[missionType]) return false;
    if (missionType == TYPE_PUBLIC) return true ;
    if (missionType == TYPE_COVERT) return false;
    return ! begun;
  }
  
  
  public boolean hasBegun() {
    return begun;
  }
  
  
  public boolean isActive() {
    return begun && ! finished();
  }
  
  
  public int motionType(Actor actor) {
    return MOTION_ANY;
  }
  
  
  public boolean valid() {
    if (finished()) return false;
    return ! subject.destroyed();
  }
  
  
  protected Behaviour cachedStepFor(Actor actor, boolean create) {
    if (begun) updateMission();
    if (done) return null;
    
    final Role role = roleFor(actor);
    if (role == null) return create ? nextStepFor(actor) : null;
    final Behaviour cached = role.cached;
    if (cached == null || cached.finished()) {
      return role.cached = create ? nextStepFor(actor) : null;
    }
    return cached;
  }
  
  
  public float priorityFor(Actor actor) {
    //  TODO:  Vary this based on loyalty to the governor that declared the
    //  mission in question.
    
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
    
    float rewardEval = REWARD_AMOUNTS[priority];
    rewardEval *= REWARD_TYPE_MULTS[missionType];
    
    final int partySize = rolesApproved(), limit = PARTY_LIMITS[priority];
    if (! isApproved(actor)) {
      if (partySize >= limit) return -1;
      rewardEval /= limit;
    }
    else rewardEval /= partySize;
    
    float value = Pledge.greedPriority(actor, (int) rewardEval);
    final int standing = actor.vocation().standing;
    value *= standing * 1.5f / Backgrounds.CLASS_STRATOI;
    
    if (report) {
      I.say("\nEvaluating reward for "+this);
      I.say("  True reward total: "+REWARD_AMOUNTS[priority]);
      I.say("  Type multiplier:   "+REWARD_TYPE_MULTS[missionType]);
      I.say("  Party capacity:    "+partySize+"/"+limit);
      I.say("  Evaluated reward:  "+rewardEval);
      I.say("  Social standing:   "+standing);
      I.say("  Priority value:    "+value);
    }
    return value;
  }
  
  
  protected Behaviour cacheStepFor(Actor actor, Behaviour step) {
    final Role role = roleFor(actor);
    if (role == null) return step;
    return role.cached = step;
  }
  
  
  public void abortBehaviour() {
    //  TODO:  There needs to be a special-case handler for this.  You also
    //  need to identify the cancelling actor.
  }
  
  
  
  /**  NOTE:  This method should be called within the ActorMind.assignMission
    *  method, and not independantly.
    */
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
  
  
  public void setMissionType(int type) {
    this.missionType = type;
    resetMission();
  }
  
  
  public void setObjective(int objectIndex) {
    this.objectIndex = objectIndex;
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
    }
  }
  
  
  public void endMission() {
    if (verbose) I.say("\nMISSION COMPLETE: "+this);
    
    base.removeMission(this);
    done = true;
    
    final float reward = REWARD_AMOUNTS[priority] * 1f / roles.size();
    for (Role role : roles) {
      role.applicant.mind.assignMission(null);
      
      if (begun) {
        if (verbose) I.say("Dispensing "+reward+" to "+role.applicant);
        role.applicant.gear.incCredits(reward);
        base.finance.incCredits(0 - reward, BaseFinance.SOURCE_REWARDS);
      }
    }
    
    if (BaseUI.isSelected(this)) {
      BaseUI.current().selection.pushSelection(null, false);
    }
  }
  
  
  public void updateMission() {
    if (missionType == TYPE_PUBLIC && priority > 0) beginMission();
    if (shouldEnd()) endMission();
  }
  
  
  protected abstract boolean shouldEnd();
  
  public boolean finished() { return done; }
  public boolean persistent() { return true; }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() { return description; }
  public String toString() { return description; }
  
  
  public Composite portrait(BaseUI UI) {
    final String key = getClass().getSimpleName()+"_"+subject.hashCode();
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    final CutoutModel flagModel = (CutoutModel) flagSprite.model();
    int flagIndex = Visit.indexOf(flagModel, MissionsTab.ALL_MODELS);
    final ImageAsset icon = MissionsTab.ALL_ICONS[flagIndex];
    
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
    if (panel == null) panel = new SelectionInfoPane(UI, this, portrait(UI));
    final Description d = panel.detail();
    
    d.append("Mission Type:  ");
    if (hasBegun()) d.append(TYPE_DESC[missionType], Colour.GREY);
    else d.append(new Description.Link(TYPE_DESC[missionType]) {
      public void whenTextClicked() {
        setMissionType((missionType + 1) % LIMIT_TYPE);
      }
    });
    
    d.append("\nObjective:  ");
    describeObjective(d);
    
    d.append("\nPayment:  ");
    final String payDesc = priority == 0 ?
      "None" :
      REWARD_AMOUNTS[priority]+" credits";
    d.append(new Description.Link(payDesc) {
      public void whenTextClicked() {
        if (priority == PRIORITY_PARAMOUNT) return;
        assignPriority(priority + 1);
      }
    });
    
    final boolean
      mustConfirm = missionType != TYPE_PUBLIC && ! begun,
      emptyList = roles.size() == 0;
    
    d.append("\n\nApplications:");
    d.append(new Description.Link(" (ABORT)") {
      public void whenTextClicked() {
        endMission();
      }
    });
    if (rolesApproved() > 0 && mustConfirm) {
      d.append(" ");
      d.append(new Description.Link(" (CONFIRM)") {
        public void whenTextClicked() {
          beginMission();
        }
      });
    }
    
    if (emptyList) {
      if (missionType == TYPE_PUBLIC) d.append(
        "\n\nThis is a public contract, open to all comers."
      );
      if (missionType == TYPE_SCREENED) d.append(
        "\n\nThis is a screened mission.  Applicants will be subject to your "+
        "approval before they can embark."
      );
      if (missionType == TYPE_COVERT) d.append(
        "\n\nThis is a covert mission.  No agents or citizens will apply "+
        "unless recruited by interview."
      );
    }
    else for (final Role role : roles) {
      d.append("\n  ");
      final Actor a = role.applicant;
      ((Text) d).insert(a.portrait(UI).texture(), 40);
      d.append(a);
      if (a instanceof Human) {
        d.append(" ("+((Human) a).career().vocation()+")");
      }
      
      if (mustConfirm) {
        d.append("\n");
        final String option = role.approved ? "(DISMISS)" : "(APPROVE)";
        d.append(new Description.Link(option) {
          public void whenTextClicked() {
            setApprovalFor(role.applicant, ! role.approved);
          }
        });
      }
    }

    return panel;
  }
  
  
  protected void describeObjective(Description d) {
    final String
      descriptions[] = objectiveDescriptions(),
      desc = descriptions[objectIndex];
    if (hasBegun()) d.append(desc, Colour.GREY);
    else d.append(new Description.Link(desc) {
      public void whenTextClicked() {
        setObjective((objectIndex + 1) % descriptions.length);
      }
    });
    d.append(subject);
  }
  
  
  protected abstract String[] objectiveDescriptions();
  
  
  public void whenTextClicked() {
    BaseUI.current().selection.pushSelection(this, true);
  }
  
  
  public Sprite flagSprite() {
    placeFlag(flagSprite, subject);
    float alpha;
    if (BaseUI.isSelectedOrHovered(this)) alpha = 1.0f;
    else alpha = 0.75f;
    flagSprite.colour = Colour.glow(alpha);
    return flagSprite;
  }
  
  
  public Vec3D flagSelectionPos() {
    placeFlag(flagSprite, subject);
    final Vec3D selPos = new Vec3D(flagSprite.position);
    selPos.z += 0.5f;
    return selPos;
  }
  
  
  public static void placeFlag(Sprite flag, Target subject) {
    if (subject instanceof Element) {
      final Element e = (Element) subject;
      flag.position.setTo(e.viewPosition(null));
      flag.position.z += e.height() + 1;
    }
    else {
      flag.position.setTo(subject.position(null));
      flag.position.z += 1.5f;
    }
    flag.scale = 0.5f;
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
}




