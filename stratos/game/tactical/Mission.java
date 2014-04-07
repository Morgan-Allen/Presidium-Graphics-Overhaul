

package stratos.game.tactical;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.start.PlayLoop;
import stratos.user.*;
import stratos.util.*;




/*
MISSION OPTIONS
  Public Contract  Screened  Covert
  Idle Casual Routine Urgent Critical
  Base Payment:  100/250/500/1000/2500

  Credits.  Goods.  Rare Artifacts.
  Obedience.  Loyalty.  Blackmail.
  Promotion.  Marriage.  Legislation.


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
  
  
  final Base base ;
  final Target subject ;
  
  //final Table <Actor, Role> roles = new Table <Actor, Role> ();
  final Stack <Role> roles = new Stack <Role> ();
  private int
    priority,
    missionType,
    objectIndex ;
  private boolean begun = false ;
  
  final CutoutSprite flagSprite ;
  final String description ;
  
  
  
  protected Mission(
    Base base, Target subject,
    CutoutModel flagModel, String description
  ) {
    this.base = base ;
    this.subject = subject ;
    this.flagSprite = (CutoutSprite) flagModel.makeSprite() ;
    this.description = description ;
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this) ;
    base = (Base) s.loadObject() ;
    subject = s.loadTarget() ;
    priority = s.loadInt() ;
    missionType = s.loadInt();
    objectIndex = s.loadInt();
    begun = s.loadBool() ;
    
    for (int i = s.loadInt() ; i-- > 0 ;) {
      final Role role = new Role() ;
      role.applicant = (Actor) s.loadObject() ;
      role.approved = s.loadBool() ;
      roles.add(role);
    }
    
    flagSprite = (CutoutSprite) ModelAsset.loadSprite(s.input()) ;
    //flagTex = ((CutoutModel) flagSprite.model()).texture() ;
    description = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base) ;
    s.saveTarget(subject) ;
    s.saveInt(priority) ;
    s.saveInt(missionType);
    s.saveInt(objectIndex);
    s.saveBool(begun) ;
    
    s.saveInt(roles.size()) ;
    for (Role role : roles) {
      s.saveObject(role.applicant) ;
      s.saveBool(role.approved) ;
    }
    
    ModelAsset.saveSprite(flagSprite, s.output()) ;
    s.saveString(description) ;
  }
  
  
  public Base base() {
    return base ;
  }
  
  
  public Target subject() {
    return subject;
  }
  
  
  public Target selectionLocksOn() {
    return subject ;
  }
  
  
  public void resetMission() {
    for (Role role : roles) role.applicant.mind.assignMission(null);
    begun = false;
  }
  
  
  
  /**  Adding and screening applicants-
    */
  class Role {
    Actor applicant;
    ///Pledge pledgeMade ;  //Not used at the moment.  TODO:  IMPLEMENT
    boolean approved;
  }
  
  
  protected Role roleFor(Actor actor) {
    for (Role r : roles) if (r.applicant == actor) return r;
    return null;
  }
  
  /*
  protected float rewardFor(Actor actor) {
    return REWARD_AMOUNTS[priority] / roles.size();
  }
  //*/
  
  
  protected float basePriority(Actor actor) {
    float rewardEval = REWARD_AMOUNTS[priority];
    rewardEval *= REWARD_TYPE_MULTS[missionType];
    
    if (! isApproved(actor)) {
      final float fill = rolesApproved() * 1f / PARTY_LIMITS[priority];
      if (fill >= 1) return -1;
      rewardEval /= 1f + fill;
    }
    
    float value = actor.mind.greedFor((int) rewardEval);
    return value;
  }
  
  
  protected int rolesApproved() {
    int count = 0 ;
    for (Role role : roles) if (role.approved) count++;
    return count ;
  }
  
  
  protected int totalApplied() {
    return roles.size() ;
  }
  
  
  protected boolean isApproved(Actor a) {
    final Role role = roleFor(a);
    if (missionType == TYPE_PUBLIC) return role != null;
    return role == null ? false : role.approved;
  }
  
  
  protected int objectIndex() {
    return objectIndex;
  }
  
  
  
  /**  Public access methods for setup purposes
    */
  public void assignPriority(int degree) {
    priority = degree;
  }
  
  
  public boolean openToPublic() {
    if (missionType == TYPE_PUBLIC) return true;
    if (missionType == TYPE_COVERT) return false;
    return ! begun;
  }
  
  
  public boolean hasBegun() {
    return begun ;
  }
  
  
  public boolean isActive() {
    return begun && ! finished();
  }
  
  
  public int motionType(Actor actor) { return MOTION_ANY ; }
  public void abortBehaviour() {}
  public boolean valid() { return ! subject.destroyed() ; }
  
  
  
  /**  NOTE:  This method should be called within the ActorMind.assignMission
    *  method, and not independantly.
    */
  public void setApplicant(Actor actor, boolean is) {
    final Role oldRole = roleFor(actor) ;
    if (is) {
      if (oldRole != null) return;
      Role role = new Role();
      role.applicant = actor;
      role.approved = missionType == TYPE_PUBLIC ? true : false ;
      roles.add(role);
    }
    else {
      if (oldRole == null) return ;
      roles.remove(oldRole) ;
    }
  }
  
  
  public void setApprovalFor(Actor actor, boolean is) {
    final Role role = roleFor(actor) ;
    if (role == null) I.complain(actor+" never applied for "+this) ;
    role.approved = is ;
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
    ///I.say("Mission begun...");
  }
  
  
  public void endMission(boolean cancelled) {
    final float reward = REWARD_AMOUNTS[priority] * 1f / roles.size();
    for (Role role : roles) {
      role.applicant.mind.assignMission(null);
      if (! cancelled) role.applicant.gear.incCredits(reward);
      base.incCredits(0 - reward);
    }
    base.removeMission(this);
    
    if (BaseUI.isSelected(this)) {
      BaseUI.current().selection.pushSelection(null, false);
    }
  }
  
  
  public void updateMission() {
    if (missionType == TYPE_PUBLIC && priority > 0) beginMission();
    if (finished()) endMission(false);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() { return description ; }
  public String toString() { return description ; }
  
  
  public Composite portrait(BaseUI UI) {
    //  TODO:  RESTORE THIS.
    return null;
  }
  
  
  public TargetInfo configInfo(TargetInfo info, BaseUI UI) {
    return null;
  }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    if (panel == null) panel = new InfoPanel(UI, this, null);
    final Description d = panel.detail();
    
    d.append("Mission Type:  ");
    if (hasBegun()) d.append(TYPE_DESC[missionType], Colour.GREY);
    else d.append(new Description.Link(TYPE_DESC[missionType]) {
      public void whenClicked() {
        missionType = (missionType + 1) % LIMIT_TYPE;
        resetMission();
      }
    });
    
    d.append("\nObjective:  ");
    describeObjective(d);
    
    d.append("\nPayment:  ");
    final String payDesc = priority == 0 ?
      "None" :
      REWARD_AMOUNTS[priority]+" credits";
    //  TODO:  Allow payment to be put down too?
    d.append(new Description.Link(payDesc) {
      public void whenClicked() {
        if (priority == PRIORITY_PARAMOUNT) return;
        assignPriority(priority + 1);
      }
    });
    
    final boolean
      mustConfirm = missionType != TYPE_PUBLIC && ! begun,
      emptyList = roles.size() == 0;
    
    d.append("\n\nApplications:");
    d.append(new Description.Link(" (ABORT)") {
      public void whenClicked() {
        if (begun) endMission(true);
        else endMission(false);
      }
    });
    if (rolesApproved() > 0 && mustConfirm) {
      d.append(" ");
      d.append(new Description.Link(" (CONFIRM)") {
        public void whenClicked() {
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
        final String option = role.approved ? "(DISMISS)" : "(APPROVE)" ;
        d.append(new Description.Link(option) {
          public void whenClicked() {
            setApprovalFor(role.applicant, ! role.approved) ;
          }
        }) ;
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
      public void whenClicked() {
        objectIndex = (objectIndex + 1) % descriptions.length;
      }
    });
    d.append(subject);
  }
  
  
  protected abstract String[] objectiveDescriptions();
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this, true) ;
  }
  
  
  public Sprite flagSprite() {
    placeFlag(flagSprite, subject);
    float alpha;
    if (BaseUI.isSelectedOrHovered(this)) alpha = 1.0f;
    else alpha = 0.75f;
    flagSprite.colour = Colour.glow(alpha);
    return flagSprite ;
  }
  
  
  public Vec3D flagSelectionPos() {
    placeFlag(flagSprite, subject);
    final Vec3D selPos = new Vec3D(flagSprite.position);
    selPos.z += 0.5f;
    return selPos;
  }
  
  
  public static void placeFlag(Sprite flag, Target subject) {
    if (subject instanceof Element) {
      final Element e = (Element) subject ;
      flag.position.setTo(e.viewPosition(null)) ;
      flag.position.z += e.height() + 1 ;
    }
    else {
      flag.position.setTo(subject.position(null)) ;
      flag.position.z += 1.5f ;
    }
    flag.scale = 0.5f ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (subject instanceof Selectable) {
      ((Selectable) subject).renderSelection(rendering, hovered);
      return;
    }
    final Vec3D pos = (subject instanceof Mobile) ?
      ((Mobile) subject).viewPosition(null) :
      subject.position(null) ;
    Selection.renderPlane(
      rendering, pos, subject.radius() + 0.5f,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}




