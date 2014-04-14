/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class SecurityMission extends Mission implements Qualities {
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final static int DURATION_LENGTHS[] = {
    World.STANDARD_DAY_LENGTH / 2,
    World.STANDARD_DAY_LENGTH * 1,
    World.STANDARD_DAY_LENGTH * 2,
  };
  final static String DURATION_NAMES[] = {
    "12 hours security for ",
    "24 hours security for ",
    "48 hours security for ",
  };
  private static boolean verbose = true;
  
  float inceptTime = -1 ;
  
  
  
  public SecurityMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.SECURITY_MODEL,
      "Securing "+subject
    ) ;
  }
  
  
  public SecurityMission(Session s) throws Exception {
    super(s) ;
    inceptTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(inceptTime) ;
  }
  
  
  public float duration() {
    return DURATION_LENGTHS[objectIndex()] ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    if (actor == subject) return 0 ;
    
    float priority = basePriority(actor);
    priority -= Plan.dangerPenalty(subject, actor) ;
    priority -= duration() * 0.5f / World.STANDARD_DAY_LENGTH ;
    if (subject instanceof Actor) {
      priority += actor.memories.relationValue((Actor) subject) * ROUTINE ;
    }
    if (subject instanceof Venue) {
      priority += actor.memories.relationValue((Venue) subject) * ROUTINE ;
    }
    
    //
    //  Modify by possession of combat and surveillance skills-
    float ability = 1 ;
    ability *= actor.traits.useLevel(SURVEILLANCE) / 10f ;
    ability *= CombatUtils.combatStrength(actor, null) * 1.5f ;
    ability = Visit.clamp(ability, 0.5f, 1.5f) ;
    priority = Visit.clamp(priority * ability, 0, PARAMOUNT) ;
    
    if (verbose && I.talkAbout == actor) {
      I.say("  SECURITY priority for "+actor+" is "+priority);
    }
    return priority ;
  }
  
  
  public boolean finished() {
    if (subject.destroyed()) return true ;
    if (inceptTime == -1) return false ;
    return (base.world.currentTime() - inceptTime) > duration() ;
  }
  
  
  public void beginMission() {
    if (hasBegun()) return;
    super.beginMission();
    inceptTime = base.world.currentTime();
  }
  
  /*
  public void updateMission() {
    super.updateMission();
    //  TODO:  Get rid of this.  Shouldn't be needed.
    if (hasBegun() && inceptTime == -1) {
      inceptTime = base.world.currentTime() - (DURATION_LENGTHS[0] * 0.66f);
    }
  }
  //*/
  
  
  /**  Behaviour implementation-
    */
  public Behaviour nextStepFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    if (! isActive()) return null;
    
    Behaviour picked = null;
    float bestPriority = 0;

    if (subject instanceof Venue) {
      final Repairs repairs = new Repairs(actor, (Venue) subject) ;
      final float rP = repairs.priorityFor(actor);
      if (rP > bestPriority) { picked = repairs; bestPriority = rP; }
    }
    
    final Batch <Target> defended = new Batch <Target> () ;
    defended.add(subject) ;
    for (Role role : roles) defended.add(role.applicant) ;
    
    for (Target defends : defended) {
      if (defends instanceof Actor) {
        final FirstAid treats = new FirstAid(actor, (Actor) defends) ;
        final float tP = treats.priorityFor(actor);
        if (tP > bestPriority) { picked = treats; bestPriority = tP; }
      }
    }
    
    for (Target e : actor.senses.awareOf()) if (e instanceof Actor) {
      final Combat fend = new Combat(actor, (Actor) e);
      final float fP = fend.priorityFor(actor);
      if (fP > bestPriority) { picked = fend; bestPriority = fP; }
    }
    
    
    //TODO:  ...Implement shifts here?
    final Plan patrol = Patrolling.aroundPerimeter(
      actor, (Element) subject, base.world
    );
    final float pP = patrol.priorityFor(actor);
    if (picked == null || pP > bestPriority) picked = patrol;
    //  TODO:  get the list of the actor's reactions, modify the priorities,
    //  and choose from those again.  Huh.  That could work.
    
    if (report) I.say("Picked is: "+picked);
    return picked;
    //  TODO:  Implement item salvage.
    /*
    if (subject instanceof ItemDrop) {
      final ItemDrop SI = (ItemDrop) subject ;
      final Recovery RS = new Recovery(actor, SA, admin) ;
      RS.setMotive(Plan.MOTIVE_DUTY, priority) ;
      choice.add(TS) ;
    }
    //*/
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void describeObjective(Description d) {
    super.describeObjective(d);
    int hours = (int) (
      (base.world.currentTime() - inceptTime) *
      24f / World.STANDARD_DAY_LENGTH
    );
    d.append(" ("+hours+" hours elapsed)");
  }
  
  protected String[] objectiveDescriptions() {
    return DURATION_NAMES;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Security Mission", this) ;
    d.append(" around ") ;
    d.append(subject) ;
  }
}




/*
final float priority = priorityFor(actor);
final World world = actor.world() ;
final Batch <Target> defended = new Batch <Target> () ;
final Batch <Target> assailants = new Batch <Target> () ;
defended.add(subject) ;
for (Role role : roles) defended.add(role.applicant) ;

//  TODO:  Only react to threats the actor is aware of directly.
/*
for (Element e : actor.senses.awareOf()) if (e instanceof Actor) {
  final Actor a = (Actor) e ;
  final Target victim = a.targetFor(Combat.class) ;
}
//*/
//  TODO:  Also, transplant the defence-reaction code to the Patrolling
//  class.
/*
final float maxDist = World.SECTOR_SIZE ;
Actor toRepel = null ;
float maxUrgency = 0 ;

for (Target t : defended) {
  for (Behaviour b : world.activities.targeting(t)) {
    if (b instanceof Combat) {
      final Combat c = (Combat) b ;
      final Actor assails = c.actor() ;
      if (Spacing.distance(t, assails) > maxDist) continue ;
      assailants.add(assails) ;
      
      float strength = CombatUtils.combatStrength(actor, assails) ;
      float urgency = 1 * Rand.avgNums(2) / (1f + strength) ;
      urgency *= t == subject ? 1.5f : 0.5f ;
      //urgency *= 2 - t.condition() ;
      if (urgency > maxUrgency) {
        maxUrgency = urgency ;
        toRepel = assails ;
      }
    }
  }
}
if (toRepel != null) {
  I.sayAbout(actor, "Repelling: "+toRepel) ;
  final Combat repels = new Combat(actor, toRepel) ;
  final Target target = actor.focusFor(Combat.class) ;
  //
  //  TODO:  I think it needs to be possible to generalise this sort of
  //  priority-switch.
  if (! assailants.includes(target)) {
    I.sayAbout(actor, "PUSHING NEW DEFENCE ACTION: "+repels) ;
    actor.mind.pushFromParent(repels, this) ;
  }
  return repels ;
}


final Choice choice = new Choice(actor) ;
for (Target t : defended) if (t instanceof Actor) {
  final FirstAid TS = new FirstAid(actor, (Actor) t) ;
  TS.setMotive(Plan.MOTIVE_EMERGENCY, priority * (t == subject ? 2 : 1)) ;
  choice.add(TS) ;
}

if (subject instanceof Venue) {
  //  TODO:  Assign this higher priority?  It doesn't seem to get much
  //  play...
  final Repairs repairs = new Repairs(actor, (Venue) subject) ;
  repairs.setMotive(Plan.MOTIVE_EMERGENCY, priority);
  choice.add(repairs) ;
}

/*
final Plan p = Patrolling.aroundPerimeter(
  actor, (Element) subject, base.world
);
p.setMotive(
  Plan.MOTIVE_DUTY, Spacing.distance(actor, subject) * ROUTINE / maxDist
);
choice.add(p) ;

final Plan picked = (Plan) choice.pickMostUrgent() ;
/*
if (actor.matchFor(picked) == null) {
  actor.mind.pushFromParent(picked, this) ;
}
//*/
//return picked ;