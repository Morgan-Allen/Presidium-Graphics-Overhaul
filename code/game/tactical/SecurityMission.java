/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package code.game.tactical ;
import code.game.actors.*;
import code.game.building.*;
import code.game.common.*;
import code.graphics.widgets.HUD;
import code.user.*;
import code.util.*;



public class SecurityMission extends Mission implements Abilities {
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final static int DURATION_LENGTHS[] = {
    World.STANDARD_DAY_LENGTH,
    World.STANDARD_DAY_LENGTH * 2,
    World.STANDARD_DAY_LENGTH * 4,
  } ;
  final static String DURATION_NAMES[] = {
    "Short, (1 day)",
    "Medium, (2 days)",
    "Long (4 days)",
  } ;
  private static boolean verbose = true ;
  
  int durationSetting = 0 ;
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
    durationSetting = s.loadInt() ;
    inceptTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(durationSetting) ;
    s.saveFloat(inceptTime) ;
  }
  
  
  public float duration() {
    return DURATION_LENGTHS[durationSetting] ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    if (actor == subject) return 0 ;
    
    float priority = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    priority -= Plan.dangerPenalty(subject, actor) ;
    priority -= duration() * 0.5f / World.STANDARD_DAY_LENGTH ;
    if (subject instanceof Actor) {
      priority += actor.mind.relationValue((Actor) subject) * ROUTINE ;
    }
    if (subject instanceof Venue) {
      priority += actor.mind.relationValue((Venue) subject) * ROUTINE ;
    }
    
    //
    //  Modify by possession of combat and surveillance skills-
    float ability = 1 ;
    ability *= actor.traits.useLevel(SURVEILLANCE) / 10f ;
    ability *= Combat.combatStrength(actor, null) * 1.5f ;
    ability = Visit.clamp(ability, 0.5f, 1.5f) ;
    priority = Visit.clamp(priority * ability, 0, PARAMOUNT) ;
    
    if (verbose && I.talkAbout == actor) {
      ///I.say("Security priority for "+actor+" is "+priority) ;
    }
    return priority ;
  }
  
  
  public boolean finished() {
    if (subject.destroyed()) return true ;
    if (inceptTime == -1) return false ;
    return (base.world.currentTime() - inceptTime) > duration() ;
  }
  
  
  public void beginMission() {
    super.beginMission() ;
    inceptTime = base.world.currentTime() ;
  }
  
  
  /**  Behaviour implementation-
    */
  public float priorityModifier(Behaviour b, Choice c) {
    //
    //  TODO:  Build this into the Choice algorithm?
    
    return 0 ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    final float priority = priorityFor(actor) ;
    
    final World world = actor.world() ;
    final Batch <Target> defended = new Batch <Target> () ;
    final Batch <Target> assailants = new Batch <Target> () ;
    defended.add(subject) ;
    for (Role role : roles) defended.add(role.applicant) ;
    
    
    //  TODO:  Only react to threats the actor is aware of directly.
    /*
    for (Element e : actor.mind.awareOf()) if (e instanceof Actor) {
      final Actor a = (Actor) e ;
      final Target victim = a.targetFor(Combat.class) ;
    }
    //*/
    //  TODO:  Also, transplant the defence-reaction code to the Patrolling
    //  class.
    
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

          float strength = Combat.combatStrength(actor, assails) ;
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
      final Target target = actor.targetFor(Combat.class) ;
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
      final Treatment TS = new Treatment(actor, (Actor) t, null) ;
      TS.priorityMod = priority * (t == subject ? 2 : 1) ;
      choice.add(TS) ;
    }
    
    if (subject instanceof Venue) {
      //  TODO:  Assign this higher priority?  It doesn't seem to get much
      //  play...
      final Repairs repairs = new Repairs(actor, (Venue) subject) ;
      repairs.priorityMod = priority ;
      choice.add(repairs) ;
    }
    
    /*
    if (subject instanceof ItemDrop) {
      final ItemDrop SI = (ItemDrop) subject ;
      final Recovery RS = new Recovery(actor, SA, admin) ;
      RS.priorityMod = priority ;
      choice.add(TS) ;
    }
    //*/
    
    final Patrolling p = Patrolling.securePerimeter(
      actor, (Element) subject, base.world
    ) ;
    p.priorityMod = Spacing.distance(actor, subject) * ROUTINE / maxDist ;
    choice.add(p) ;
    
    final Plan picked = (Plan) choice.pickMostUrgent() ;
    /*
    if (actor.matchFor(picked) == null) {
      actor.mind.pushFromParent(picked, this) ;
    }
    //*/
    return picked ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    d.append("\n\nDuration: ") ;
    if (begun()) d.append(DURATION_NAMES[durationSetting]) ;
    else d.append(new Description.Link(DURATION_NAMES[durationSetting]) {
      public void whenClicked() {
        durationSetting = (durationSetting + 1) % DURATION_LENGTHS.length ;
      }
    }) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Security Mission", this) ;
    d.append(" around ") ;
    d.append(subject) ;
  }
}



/*
final Combat defence = new Combat(actor, c.actor()) ;
c.priorityMod = priority * (t == subject ? 1.5f : 0.5f) ;
choice.add(defence) ;

if (verbose && I.talkAbout == actor) {
  I.say("  "+c.actor()+" is assaulting "+t) ;
  I.say("  Defence priority: "+defence.priorityFor(actor)) ;
}
//*/