


package src.game.tactical ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.start.PlayLoop;
import src.user.* ;
import src.util.* ;


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

//*/



public abstract class Mission implements
  Behaviour, Session.Saveable, Selectable
{
  
	
  final static int
    AMOUNT_NONE   = 0,
    AMOUNT_TINY   = 1,
    AMOUNT_SMALL  = 2,
    AMOUNT_MEDIUM = 3,
    AMOUNT_LARGE  = 4,
    AMOUNT_HUGE   = 5,
    REWARD_AMOUNTS[] = {
      0, 100, 250, 500, 1000, 2500
    },
    
    TYPE_BOUNTY   = 0,
    TYPE_SCREENED = 1,
    TYPE_COVERT   = 2 ;
  final static String TYPE_DESC[] = {
    "Public Bounty", "Screened", "Covert"
  } ;
  //
  //  TODO:  Allow arbitrary reward settings?
  //  Type_Bounty, Type_Screened, Type_Covert
  //  Idle, Casual, Routine, Urgent, Critical
  //  100,   250,    500,     1000,    2500
  
  
  final Base base ;
  final Target subject ;
  protected int rewardAmount, missionType ;
  private boolean begun ;
  protected List <Role> roles = new List <Role> () ;
  
  
  CutoutSprite flagSprite ;
  //Texture flagTex ;
  String description ;
  
  
  
  protected Mission(
    Base base, Target subject,
    CutoutModel flagModel, String description
  ) {
    this.base = base ;
    this.subject = subject ;
    this.flagSprite = (CutoutSprite) flagModel.makeSprite() ;
    //this.flagTex = ((CutoutModel) flagSprite.model()).texture() ;
    this.description = description ;
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this) ;
    base = (Base) s.loadObject() ;
    subject = s.loadTarget() ;
    rewardAmount = s.loadInt() ;
    begun = s.loadBool() ;
    
    for (int i = s.loadInt() ; i-- > 0 ;) {
      final Role role = new Role() ;
      role.applicant = (Actor) s.loadObject() ;
      role.approved = s.loadBool() ;
      roles.add(role) ;
    }
    
    flagSprite = (CutoutSprite) ModelAsset.loadSprite(s.input()) ;
    //flagTex = ((CutoutModel) flagSprite.model()).texture() ;
    description = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base) ;
    s.saveTarget(subject) ;
    s.saveInt(rewardAmount) ;
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
    return subject ;
  }
  
  
  
  /**  Adding and screening applicants-
    */
  class Role {
    Actor applicant ;
    ///Pledge pledgeMade ;  //Not used at the moment.
    boolean approved ;
  }
  
  
  protected Role roleFor(Actor actor) {
    for (Role r : roles) if (r.applicant == actor) return r ;
    return null ;
  }
  
  
  public void assignReward(int amount) {
    rewardAmount = amount ;
  }
  
  
  public float priorityFor(Actor actor) {
    return actor.mind.greedFor(rewardAmount) ;
  }
  
  
  //
  //  TODO:  Replace with a general 'rewardAppeal' method, so that you can
  //  employ different enticements?
  public int rewardAmount(Actor actor) {
    return rewardAmount ;
  }
  
  
  public boolean begun() {
    return begun ;
  }
  
  
  public boolean active() {
    return begun && ! finished() ;
  }
  
  
  public boolean open() {
    return (! begun) ;
  }
  
  
  public int numApproved() {
    int count = 0 ;
    for (Role role : roles) if (role.approved) count++ ;
    return count ;
  }
  
  
  public int numApplied() {
    return roles.size() ;
  }
  
  
  public void setApplicant(Actor actor, boolean is) {
    final Role oldRole = roleFor(actor) ;
    if (is) {
      if (oldRole != null) return ;
      Role role = new Role() ;
      role.applicant = actor ;
      role.approved = false ;
      roles.add(role) ;
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
    I.say("Beginning mission: "+this) ;
    for (Role role : roles) {
      if (! role.approved) roles.remove(role) ;
      else {
        role.applicant.mind.assignMission(this) ;
        role.applicant.mind.assignBehaviour(this) ;
      }
    }
    I.say("Mission begun...") ;
    begun = true ;
  }
  
  
  public void endMission(boolean cancelled) {
    final float reward = rewardAmount ;
    for (Role role : roles) {
      role.applicant.mind.assignMission(null) ;
      if (! cancelled) role.applicant.gear.incCredits(reward) ;
    }
    base.incCredits(0 - reward * roles.size()) ;
    base.removeMission(this) ;
  }
  
  
  public void updateMission() {
    if (finished()) endMission(false) ;
  }
  
  
  
  /**  Default behaviour implementation and utility methods-
    */
  public int motionType(Actor actor) { return MOTION_ANY ; }
  public void abortBehaviour() {}
  
  public void setPriority(float priority) {}
  public boolean valid() { return ! subject.destroyed() ; }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() { return description ; }
  public String helpInfo() { return description ; }
  public String toString() { return description ; }
  
  
  public Composite portrait(HUD UI) {
    return null;
    //final Composite c = new Composite(UI, flagTex) ;
    //return c ;
  }
  
  public String[] infoCategories() { return null ; }
  
  
  
  public void writeInformation(
    Description d, int categoryID, final HUD UI
  ) {
    //
    //  Here, you can approve the mission, cancel the mission, or visit your
    //  personnel listings (full household.)
    if (! begun) {
      d.append("CHOOSE PAYMENT:") ;
      for (final int amount : REWARD_AMOUNTS) {
        d.append("\n  ") ;
        d.append(new Description.Link(""+amount) {
          public void whenClicked() { assignReward(amount) ; }
        }, amount == rewardAmount ? Colour.GREEN : Colour.BLUE) ;
      }
      d.append("\n") ;
      d.append("\n("+rewardAmount+" credits per applicant)") ;
    }
    else {
      d.append("\n("+rewardAmount+" credits per applicant)") ;
    }
    //
    //  First, list the team members that have been approved-
    if (begun) d.append("\n\nTEAM MEMBERS:") ;
    else d.append("\n\nAPPLICANTS:") ;
    for (final Role role : roles) {
      d.append("\n  ") ;
      d.append(role.applicant) ;
      d.append(" ("+role.applicant.vocation()+") ") ;
      if (! begun) {
        final String option = role.approved ? "(DISMISS)" : "(APPROVE)" ;
        d.append(new Description.Link(option) {
          public void whenClicked() {
            setApprovalFor(role.applicant, ! role.approved) ;
          }
        }) ;
      }
    }
    if (numApplied() == 0) d.append("\n  (None)") ;
    //
    //  Then, present options for beginning or cancelling the mission-
    d.append("\n\n") ;
    if (! begun && numApproved() > 0) {
      d.append(new Description.Link("(APPROVE) ") {
        public void whenClicked() {
          beginMission() ;
        }
      }) ;
    }
    else d.append("(APPROVE) ", Colour.GREY) ;
    d.append(new Description.Link("(ABORT)") {
      public void whenClicked() {
        if (begun) endMission(true) ;
        else endMission(false) ;
        if (UI instanceof BaseUI) {
          ((BaseUI) UI).selection.pushSelection(null, false) ;
        }
      }
    }, Colour.RED) ;
  }
  

  public void whenClicked() {
    ((BaseUI) PlayLoop.currentUI()).selection.pushSelection(this, true) ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    //  Have a dedicated MissionPanel?
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
  
  
  public CutoutSprite flagSprite() {
    placeFlag(flagSprite, subject) ;
    return flagSprite ;
  }
  
  
  public static void placeFlag(Sprite flag, Target subject) {
    if (subject instanceof Element) {
      final Element e = (Element) subject ;
      flag.position.setTo(e.viewPosition(null, null)) ;
      flag.position.z += e.height() + 1 ;
    }
    else {
      flag.position.setTo(subject.position(null)) ;
      flag.position.z += 1.5f ;
    }
    flag.scale = 0.5f ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    final Vec3D pos = (subject instanceof Mobile) ?
      ((Mobile) subject).viewPosition(null, null) :
      subject.position(null) ;
    Selection.renderPlane(
      rendering, pos, subject.radius() + 0.5f,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}



