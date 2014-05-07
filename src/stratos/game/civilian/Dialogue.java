


package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.util.*;



//  ...Actors also need to use dialogue to 'object' when they see someone
//  doing something they don't approve of *to* someone else.  (Or just build
//  that into basic decision-making?)

//  TODO:  You need to restore the use of a communal ChatFX for a given
//  instance of dialogue.  (And have everything fade once complete.)


public class Dialogue extends Plan implements Qualities {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  final public static int
    TYPE_CONTACT   = 0,
    TYPE_CASUAL    = 1,
    TYPE_OBJECTION = 2;
  
  final static int
    STAGE_INIT  = -1,
    STAGE_GREET =  0,
    STAGE_CHAT  =  1,
    STAGE_BYE   =  2,
    STAGE_DONE  =  3;
  
  private static boolean
    evalVerbose   = false,
    eventsVerbose = false;
  
  
  final Actor starts, other;
  final int type;
  private int stage = STAGE_INIT;
  private Boardable location = null, stands = null;
  
  
  
  public Dialogue(Actor actor, Actor other, int type) {
    this(actor, other, actor, type) ;
  }
  
  
  private Dialogue(Actor actor, Actor other, Actor starts, int type) {
    super(actor, other) ;
    if (actor == other) I.complain("CANNOT TALK TO SELF!") ;
    this.other = other ;
    this.starts = starts ;
    this.type = type ;
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s) ;
    other  = (Actor) s.loadObject() ;
    starts = (Actor) s.loadObject() ;
    type = s.loadInt() ;
    stage = s.loadInt() ;
    location = (Boardable) s.loadTarget() ;
    stands = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(other ) ;
    s.saveObject(starts) ;
    s.saveInt(type) ;
    s.saveInt(stage) ;
    s.saveTarget(location) ;
    s.saveTarget(stands) ;
  }
  
  
  public Plan copyFor(Actor other) {
    return new Dialogue(other, this.other, type);
  }
  
  
  
  /**  Target selection and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, POSITIVE, EMPATHIC };
  final static Skill BASE_SKILLS[] = { SUASION, TRUTH_SENSE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (stage == STAGE_DONE) return 0;
    if (stage == STAGE_BYE) return CASUAL;  //Move down?
    
    float urgency = 0;
    float distCheck = NORMAL_DISTANCE_CHECK;
    
    final boolean casual = type == TYPE_CASUAL;
    if (casual) {
      if (! canTalk(other, report)) return 0;
      urgency = urgency();
      if (urgency <= 0) return 0;
      urgency = Visit.clamp(urgency, 0.5f, 1);
      distCheck = HEAVY_DISTANCE_CHECK;
    }
    
    final float priority = priorityForActorWith(
      actor, other, casual ? (ROUTINE * urgency) : URGENT,
      MILD_HELP, NO_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, distCheck, NO_FAIL_RISK,
      report
    );
    if (report) {
      I.say("  Urgency of dialogue was: "+urgency);
      ///I.say("  Prior relationship? "+(r != null)+", curiosity: "+curiosity);
    }
    return priority;
  }
  
  
  private float urgency() {
    final float curiosity = (1 + actor.traits.relativeLevel(CURIOUS)) / 2;
    final Relation r = actor.memories.relationWith(other);
    final float
      value   = r == null ? actor.memories.relationValue(other) : r.value(),
      novelty = r == null ? 1 : r.novelty();
    
    float urgency = 0;
    urgency += novelty * curiosity;
    urgency += (solitude(actor) + value) / 2f;
    urgency = Visit.clamp(urgency, -1, 1);
    return urgency;
  }
  
  
  private float solitude(Actor actor) {
    //  TODO:  Only count positive relations?
    final float
      baseF = Relation.BASE_NUM_FRIENDS,
      numF  = actor.memories.relations().size();
    return (baseF - numF) / baseF;
  }
  
  
  private boolean canTalk(Actor other, boolean report) {
    if (! other.health.conscious()) return false;
    if (! other.health.human()) return false;
    if (other == starts && ! hasBegun()) return true;
    final Target talksWith = other.focusFor(Dialogue.class);
    if (talksWith == actor) return true;
    if (talksWith != null) return false;
    
    final Dialogue d = new Dialogue(other, actor, actor, type);
    final boolean can = ! other.mind.mustIgnore(d);

    if (report) {
      I.say("  Talk priority for other: "+d.priorityFor(other));
      I.say("  Can talk? "+can);
    }
    return can;
  }
  
  
  private Batch <Dialogue> sides() {
    final World world = actor.world() ;
    final Batch <Dialogue> batch = new Batch <Dialogue> () ;
    
    batch.include(this) ;
    Dialogue oD = (Dialogue) other.matchFor(Dialogue.class) ;
    if (oD != null) batch.include(oD) ;
    
    for (Behaviour b : world.activities.targeting(other)) {
      if (b instanceof Dialogue) {
        final Dialogue d = (Dialogue) b ;
        batch.include(d) ;
      }
    }
    return batch ;
  }
  
  
  private void setLocationFor(Action talkAction) {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    final Batch <Dialogue> sides = sides() ;
    if (location == null) {
      
      //  If a location has not already been assigned, look for one either used
      //  by existing conversants, or find a new spot nearby.
      for (Dialogue d : sides) if (d.location != null) {
        this.location = d.location ; break ;
      }
      if (location == null) {
        if (other.indoors() && starts.indoors()) location = other.aboard();
        else location = Spacing.bestMidpoint(starts, other);
        if (report) I.say("Initialising talk location: "+location);
      }
    }
    
    if (location instanceof Tile) {
      
      //  In the case of an open-air discussion, you need to find appropriate
      //  spots to stand around in.  Mark any spot claimed by other conversants
      //  and try to find one unused-
      for (Dialogue s : sides) if (s != this && s.stands != null) {
        s.stands.flagWith(s) ;
      }
      float minDist = Float.POSITIVE_INFINITY ;
      this.stands = null ;
      for (Tile t : ((Tile) location).allAdjacent(null)) {
        if (t == null) continue ;
        if (t.blocked() || t.flaggedWith() != null) continue ;
        final float dist = Spacing.distance(t, actor) ;
        if (dist < minDist) { stands = t ; minDist = dist ; }
      }
      for (Dialogue s : sides) if (s != this && s.stands != null) {
        s.stands.flagWith(null) ;
      }
      
      //  If none is available, quit and return.  Otherwise, walk over there.
      if (stands == null) { abortBehaviour() ; return ; }
      talkAction.setMoveTarget(stands) ;
    }
    else if (location instanceof Boardable) {
      
      //  In the case of an indoor discussion, just set the right venue.  If
      //  nothing else was available, quit and return.
      talkAction.setMoveTarget(location) ;
    }
    else abortBehaviour() ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (stage >= STAGE_DONE) return null ;
    final boolean report = eventsVerbose && I.talkAbout == actor;
    
    if (type != TYPE_CONTACT && ! canTalk(other, report)) {
      abortBehaviour() ;
      return null ;
    }
    
    if (starts == actor && stage == STAGE_INIT) {
      final Action greeting = new Action(
        actor, other.aboard(),
        this, "actionGreet",
        Action.TALK, "Greeting "
      ) ;
      greeting.setProperties(Action.RANGED) ;
      return greeting ;
    }
    
    if (stage == STAGE_CHAT) {
      Actor chatsWith = other;
      if (type == TYPE_CASUAL) {
        chatsWith = ((Dialogue) Rand.pickFrom(sides())).actor;
        if (chatsWith == actor) chatsWith = other;
      }
      final Action chats = new Action(
        actor, chatsWith,
        this, "actionChats",
        Action.TALK_LONG, "Chatting with "
      ) ;
      setLocationFor(chats) ;
      return chats ;
    }
    
    if (stage == STAGE_BYE) {
      final Action farewell = new Action(
        actor, other,
        this, "actionFarewell",
        Action.TALK, "Saying farewell to "
      ) ;
      farewell.setProperties(Action.RANGED) ;
      return farewell ;
    }
    
    return null ;
  }
  
  
  public boolean actionGreet(Actor actor, Boardable aboard) {
    if (! other.isDoing(Dialogue.class, null)) {
      if (! canTalk(other, false)) {
        if (type == TYPE_CASUAL) abortBehaviour() ;
        return false ;
      }
      final Dialogue d = new Dialogue(other, actor, type) ;
      d.stage = STAGE_CHAT ;
      other.mind.assignBehaviour(d) ;
    }
    this.stage = STAGE_CHAT ;
    return true ;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    DialogueUtils.tryChat(actor, other);
    if (urgency() <= 0) {
      
      
      stage = STAGE_BYE;
    }
    return true;
  }
  
  
  private void promptAccompaniment(Actor actor, Actor other) {
    //  TODO:  You'll need a way to copy plans for another actor, for purposes
    //  of comparison.  Yeah.  That will require potential overhauls to ALL
    //  plans.  ...Safer that way, I suppose.
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    //  Used to close a dialogue.
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  /*
  public boolean actionAskFavour(Actor actor, Actor other) {
    //  (Base on relationship strength.)
    Plan activity = Choice.pickJointActivity(actor, other);
    if (activity == null) return false;
    other.mind.assignBehaviour(activity);
    stage = STAGE_DONE;
    //  TODO:  Create a copy of your upcoming behaviour (something new, or on
    //  the todo-list,) pass it to the other for evaluation, and if they agree,
    //  you can both head off for that.
    return true ;
  }
  //*/
  
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Talking to ")) {
      d.append(other);
    }
  }
}





/*

float success = 0;

//  Suasion vs. Suasion.  Half DC.  Bonus for truth sense, esp. vs deceit.
//  Plus a routine check for language.

//  Another modifier for attitude.

//  Counsel or command for instruction.  Level of skill taught for DC.
//  ...Maybe that should be a different activity, though?

//  Suasion for pleas.  Level of reluctance for DC.  Command for inferiors.



return success;
/*
final float attitude = other.memories.relationValue(actor) ;
final int DC = ROUTINE_DC - (int) (attitude * MODERATE_DC) ;
float success = -1 ;

success += actor.traits.test(language, null, null, ROUTINE_DC, 10, 2) ;
success += actor.traits.test(plea, other, opposeSkill, 0 - DC, 10, 2) ;
success /= 3 ;
return success ;
//*/

/*
float
  value    = actor.memories.relationValue(other),
  novelty  = actor.memories.relationNovelty(other),
  solitude = actor.mind.solitude() ;

novelty *= (actor.traits.relativeLevel(CURIOUS) + 1) / 2f ;
if (! actor.mind.hasRelation(other)) novelty *= solitude ;
else novelty *= (1 + solitude) / 2f ;

float impetus = (CASUAL * value * 2) + (novelty * ROUTINE) ;
if (other.base() == actor.base()) {
  impetus += actor.base().communitySpirit() ;
}
impetus *= 1 + actor.traits.relativeLevel(OUTGOING) ;

if (verbose && I.talkAbout == actor) {
  I.say("\n  Priority for talking to "+other+" is: "+impetus) ;
  I.say("  Value/novelty: "+value+"/"+novelty) ;
}
return Visit.clamp(impetus, 0, URGENT) ;
//*/


/**  Helper methods for gifts and favours-
  */
/*
private Action assistanceFrom(Actor other) {
  
  //  TODO:  Clone actions from the actor's own behavioural repertoire and
  //  see if any of those suit the other actor.
  
  //Plan favour = actor.mind.createBehaviour() ;
  //favour.assignActor(other) ;
  //favour.setMotive(
    //Plan.MOTIVE_LEISURE, ROUTINE * other.memories.relationValue(actor)
  //) ;
  //return favour ;
  return null ;
}
//*/


/*
public boolean actionIntro(Actor actor, Actor other) {
  //  Used when making a first impression.
  float success = talkResult(SUASION, SUASION, TRUTH_SENSE, other) ;
  if (other.mind.hasRelation(actor)) {
    other.mind.initRelation(actor, success / 2) ;
  }
  else other.mind.incRelation(actor, success) ;
  stage = STAGE_CHAT ;
  return true ;
}
//*/
