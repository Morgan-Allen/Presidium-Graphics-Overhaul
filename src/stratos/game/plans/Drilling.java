/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Merge with the Study class?

/*
public class Drilling extends Plan {
  
  
  
  /**  Data fields, static constants, constructors and save/load methods-
    */
  /*
  final static float MAX_TIME = Stage.STANDARD_HOUR_LENGTH;
  
  private static boolean
    evalVerbose  = false,
    eventVerbose = false;
  
  final DrillYard yard;
  private int inceptTime = -1;
  
  

  public Drilling(
    Actor actor, DrillYard grounds
  ) {
    super(actor, grounds, false, NO_HARM);
    this.yard = grounds;
  }
  
  
  public Drilling(Session s) throws Exception {
    super(s);
    yard = (DrillYard) s.loadObject();
    inceptTime = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(yard);
    s.saveInt(inceptTime);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Drilling(other, yard);
  }
  
  
  
  /**  Priority and target evaluation-
  final static Trait BASE_TRAITS[] = { DEFENSIVE, DUTIFUL, ENERGETIC };
  final static Skill ALL_DRILL_SKILLS[][] = {
    { HAND_TO_HAND, SHIELD_AND_ARMOUR },
    { MARKSMANSHIP, SURVEILLANCE      },
    { ATHLETICS   , STEALTH_AND_COVER },
    { ANATOMY     , PHARMACY          }
  };
  
  
  protected float getPriority() {
    if (! CombatUtils.isArmed(actor)) return 0;
    
    final boolean report = evalVerbose && I.talkAbout == actor;
    final int type = yard.drillType();
    if (type == -1) return 0;
    
    final boolean melee = actor.gear.meleeWeapon();
    if (type == DrillYard.DRILL_MELEE && ! melee) return 0;
    if (type == DrillYard.DRILL_RANGED &&  melee) return 0;
    
    final Skill baseSkills[] = ALL_DRILL_SKILLS[yard.drillType()];
    
    final float priority = priorityForActorWith(
      actor, yard, CASUAL, //* Planet.dayValue(actor.world()),
      NO_MODIFIER, NO_HARM,
      NO_COMPETITION, NO_FAIL_RISK,
      baseSkills, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    //  TODO:  VARY ASSOCIATED TRAITS (not defensive for first aid, say.)
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
  protected Behaviour getNextStep() {
    final boolean report = eventVerbose && I.talkAbout == actor;
    final int time = (int) actor.world().currentTime();
    if (inceptTime != -1 && time - inceptTime > MAX_TIME) {
      if (report) I.say("\nDRILLING COMPLETE, TIME: "+time);
      return null;
    }
    
    final int drillType = yard.drillType();
    final String actionName = "actionDrill", animName;
    
    switch (drillType) {
      case (DrillYard.DRILL_MELEE) :
        animName = Action.STRIKE;
      break;
      case (DrillYard.DRILL_RANGED) :
        animName = Action.FIRE;
      break;
      case (DrillYard.DRILL_ENDURANCE) :
        animName = Action.LOOK;
      break;
      case (DrillYard.DRILL_AID) :
        animName = Action.BUILD;
      break;
      default : return null;
    }
    
    final String DN = DrillYard.DRILL_STATE_NAMES[drillType];
    final Action drill = new Action(
      actor, yard,
      this, actionName,
      animName, "Training "+DN
    );
    if (actor.aboard() == yard) drill.setProperties(Action.QUICK);
    return drill;
  }
  
  
  public boolean actionDrill(Actor actor, DrillYard yard) {
    final boolean report = eventVerbose && I.talkAbout == actor;
    if (inceptTime == -1) inceptTime = (int) actor.world().currentTime();
    
    final int type = yard.drillType();
    if (type == -1) return false;
    final Skill baseSkills[] = ALL_DRILL_SKILLS[type];
    final Upgrade bonus = yard.bonusFor(type);
    final int DC = (1 + yard.belongs().structure.upgradeLevel(bonus)) * 5;
    
    boolean success = true;
    success &= actor.skills.test(baseSkills[0], DC    , 0.5f);
    success &= actor.skills.test(baseSkills[1], DC - 5, 0.5f);
    
    if (report) {
      I.say("Training "+baseSkills[0]+" and "+baseSkills[1]);
      I.say("Basic DC: "+DC+", success: "+success);
    }
    
    if (type == DrillYard.DRILL_RANGED || type == DrillYard.DRILL_MELEE) {
      final Target dummy = yard.dummyFor(actor);
      if (dummy == null) return true;
      CombatFX.applyFX(actor.gear.deviceType(), actor, dummy, success);
    }
    return true;
  }
  


  public boolean actionDrillCommand(Actor actor, DrillYard yard) {
    //
    //  ...Intended for the officer corps.
    //
    //  TODO:  Incorporate a bonus to skill acquisition when you have an
    //  officer present with enough expertise and the Command skill.
    return true;
  }
  
  
  
  /**  Rendering and interface-
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Training at ")) {
      d.append(yard);
    }
  }
}
//*/





