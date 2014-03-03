


package src.game.tactical ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



//
//  TODO:  Try and convince a particular subject to join your settlement, not
//  be hostile, or simply accept a gift.  Applies to all actors encountered
//  that belong to the same base, but with particular emphasis on the primary
//  subject.
//  (Use the Dialogue class.)

//
//  ...Give gift or ask favour.  Player-selected?


public class ContactMission extends Mission implements Economy {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static int
    SETTING_TRIBUTE = 0,
    SETTING_FAVOUR  = 1,
    SETTING_FEALTY  = 2 ;
  final static String SETTING_NAMES[] = {
    "Send Tribute",
    "Secure Favour",
    "Demand Fealty"
  } ;
  
  int contactType = SETTING_FAVOUR ;
  
  
  
  
  public ContactMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.CONTACT_MODEL,
      "Making Contact with "+subject
    ) ;
  }
  
  
  public ContactMission(Session s) throws Exception {
    super(s) ;
    contactType = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(contactType) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    float reward = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    float priority = 1 * reward ;
    
    return priority ;
  }
  
  
  public void beginMission() {
    super.beginMission() ;
  }
  
  
  private float relationLevelNeeded() {
    if (contactType == SETTING_TRIBUTE) return 0    ;
    if (contactType == SETTING_FAVOUR ) return 0.5f ;
    if (contactType == SETTING_FEALTY ) return 1.0f ;
    return -1 ;
  }
  
  
  private Batch <Actor> talksTo() {
    final Batch <Actor> batch = new Batch <Actor> () ;
    if (subject instanceof Actor) {
      final Actor a = (Actor) subject ;
      batch.add(a) ;
    }
    else if (subject instanceof Venue) {
      final Venue v = (Venue) subject ;
      for (Actor a : v.personnel.residents()) batch.include(a) ;
      for (Actor a : v.personnel.workers()  ) batch.include(a) ;
    }
    return batch ;
  }


  public Behaviour nextStepFor(Actor actor) {
    final float minRelation = relationLevelNeeded() ;
    Dialogue picked = null ;
    float maxUrgency = Float.NEGATIVE_INFINITY ;
    
    for (Actor a : talksTo()) {
      final float relation = a.mind.relationValue(actor) ;
      if (relation >= minRelation) continue ;
      
      final Dialogue d = new Dialogue(actor, a, Dialogue.TYPE_CONTACT) ;
      float urgency = d.priorityFor(actor) ;
      urgency += (1 - a.mind.relationValue(actor)) * ROUTINE ;
      if (urgency > maxUrgency) { maxUrgency = urgency ; picked = d ; }
    }
    if (picked != null) return picked ;
    
    //  Otherwise, see if you can give them a gift, or find a suitable task
    //  from the subject's home or work venue.
    final Element around = (Element) subject ;
    return Patrolling.securePerimeter(actor, around, actor.world()) ;
  }
  
  
  public boolean finished() {
    //  Ensure mutual relations are ALL peachy.
    final float minRelation = relationLevelNeeded() ;
    ///I.say("\nMinimum relation needed: "+minRelation) ;
    boolean allOK = true ;
    for (Actor a : talksTo()) for (Role role : this.roles) {
      final float relation = a.mind.relationValue(role.applicant) ;
      ///I.say("  Relation between "+a+" and "+role.applicant+": "+relation) ;
      if (relation < minRelation) allOK = false ;
    }
    ///I.say("All Okay? "+allOK) ;
    return allOK ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    d.append("\n\nArea: ") ;
    if (begun()) d.append(SETTING_NAMES[contactType]) ;
    else d.append(new Description.Link(SETTING_NAMES[contactType]) {
      public void whenClicked() {
        contactType = (contactType + 1) % SETTING_NAMES.length ;
      }
    }) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Contact Mission", this) ;
    d.append(" around ") ;
    d.append(subject) ;
  }
}



