/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.actors.* ;
import stratos.game.maps.Planet;
import stratos.game.tactical.* ;
import stratos.game.building.* ;
import stratos.user.* ;
import stratos.util.* ;



public class Drilling extends Plan implements Economy, Qualities {
  
  
  
  /**  Data fields, static constants, constructors and save/load methods-
    */
  final static float MAX_TIME = World.STANDARD_HOUR_LENGTH;
  
  private static boolean
    evalVerbose  = false,
    eventVerbose = false;
  
  final DrillYard yard;
  private int inceptTime = -1;
  
  

  public Drilling(
    Actor actor, DrillYard grounds
  ) {
    super(actor, grounds) ;
    this.yard = grounds ;
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
  
  
  
  /**  Priority and target evaluation-
    */
  final static Trait BASE_TRAITS[] = { DEFENSIVE, DUTIFUL, ENERGETIC };
  final static Skill ALL_DRILL_SKILLS[][] = {
    { HAND_TO_HAND, SHIELD_AND_ARMOUR },
    { MARKSMANSHIP, SURVEILLANCE      },
    { ATHLETICS   , STEALTH_AND_COVER },
    { ANATOMY     , PHARMACY          }
  };
  
  
  protected float getPriority() {
    if (! actor.gear.armed()) return 0;
    
    final boolean report = evalVerbose && I.talkAbout == actor;
    final int type = yard.drillType();
    if (type == -1) return 0;
    final Skill baseSkills[] = ALL_DRILL_SKILLS[yard.drillType()];
    
    final float priority = priorityForActorWith(
      actor, yard, CASUAL * Planet.dayValue(actor.world()),
      NO_HARM, NO_COMPETITION,
      baseSkills, BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    //  TODO:  VARY ASSOCIATED TRAITS (not defensive for first aid, say.)
    return priority;
  }
  
  
  public static Drilling nextDrillFor(Actor actor) {
    if (actor.base() == null) return null ;
    //if (! (actor.mind.work() instanceof Venue)) return null ;
    
    final World world = actor.world() ;
    final Batch <DrillYard> yards = new Batch <DrillYard> () ;
    world.presences.sampleFromMap(actor, world, 5, yards, DrillYard.class) ;
    
    final Choice choice = new Choice(actor) ;
    for (DrillYard yard : yards) if (yard.base() == actor.base()) {
      ///I.sayAbout(actor, "Can drill at "+yard) ;
      final Drilling drilling = new Drilling(actor, yard) ;
      choice.add(drilling) ;
    }
    return (Drilling) choice.pickMostUrgent() ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = eventVerbose && I.talkAbout == actor;
    final int time = (int) actor.world().currentTime();
    if (inceptTime != -1 && time - inceptTime > MAX_TIME) {
      if (report) I.say("\nDRILLING COMPLETE, TIME: "+time);
      return null;
    }
    
    final int drillType = yard.drillType() ;
    final String actionName = "actionDrill", animName ;
    
    switch (drillType) {
      case (DrillYard.DRILL_MELEE) :
        animName = Action.STRIKE ;
      break ;
      case (DrillYard.DRILL_RANGED) :
        animName = Action.FIRE ;
      break ;
      case (DrillYard.DRILL_ENDURANCE) :
        animName = Action.LOOK ;
      break ;
      case (DrillYard.DRILL_AID) :
        animName = Action.BUILD ;
      break ;
      default : return null ;
    }
    
    final String DN = DrillYard.DRILL_STATE_NAMES[drillType] ;
    final Action drill = new Action(
      actor, yard,
      this, actionName,
      animName, "Training "+DN
    ) ;
    if (actor.aboard() == yard) drill.setProperties(Action.QUICK) ;
    return drill ;
  }
  
  
  public boolean actionDrill(Actor actor, DrillYard yard) {
    final boolean report = eventVerbose && I.talkAbout == actor;
    if (inceptTime == -1) inceptTime = (int) actor.world().currentTime();
    
    final int type = yard.drillType();
    if (type == -1) return false;
    final Skill baseSkills[] = ALL_DRILL_SKILLS[type];
    final Upgrade bonus = yard.bonusFor(type);
    final int DC = (1 + yard.belongs.structure.upgradeLevel(bonus)) * 5;
    
    boolean success = true;
    success &= actor.traits.test(baseSkills[0], DC    , 0.5f);
    success &= actor.traits.test(baseSkills[1], DC - 5, 0.5f);
    
    if (report) {
      I.say("Training "+baseSkills[0]+" and "+baseSkills[1]);
      I.say("Basic DC: "+DC+", success: "+success);
    }
    
    if (type == DrillYard.DRILL_RANGED || type == DrillYard.DRILL_MELEE) {
      final Target dummy = yard.dummyFor(actor);
      if (dummy == null) return true;
      DeviceType.applyFX(actor.gear.deviceType(), actor, dummy, success);
    }
    return true;
  }
  


  public boolean actionDrillCommand(Actor actor, DrillYard yard) {
    //
    //  ...Intended for the officer corps.
    //
    //  TODO:  Incorporate a bonus to skill acquisition when you have an
    //  officer present with enough expertise and the Command skill.
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    final int DT = yard.drillType();
    if ((! describedByStep(d)) && DT >= 0) {
      d.append("Training "+DrillYard.DRILL_STATE_NAMES[DT]);
    }
    d.append(" at ");
    d.append(yard);
  }
}



//  TODO:  Get rid of this whole nonsense with dummies and wot-not...

/*
if (
  (yard.nextDrill != yard.drill && ! yard.belongs.destroyed()) &&
  (Plan.competition(Drilling.class, yard.belongs, actor) < 1)
) {
  final Action pickup = new Action(
    actor, yard.belongs,
    this, "actionPickupEquipment",
    Action.REACH_DOWN, "Picking up equipment"
  ) ;
  final Action equips = new Action(
    actor, yard,
    this, "actionEquipYard",
    Action.BUILD, "Installing equipment"
  ) ;
  final Element dummy = (Element) Rand.pickFrom(yard.dummies) ;
  equips.setMoveTarget(Spacing.nearestOpenTile(dummy.origin(), actor)) ;
  final Steps steps = new Steps(
    actor, yard, Plan.ROUTINE, pickup, equips
  ) ;
  return steps ;
}
if (yard.equipQuality <= 0) return null ;

//
//  TODO:  If you're a commanding officer, consider supervising the drill-

final Target
  dummy = yard.nextDummyFree(drillType, actor),
  moveTarget = yard.nextMoveTarget(dummy, actor) ;
if (dummy == null || moveTarget == null) return null ;
//*/

/*
public boolean actionPickupEquipment(Actor actor, Garrison store) {
  if (verbose) I.sayAbout(actor, "Picking up drill equipment...") ;
  final Item equipment = Item.withReference(SAMPLES, store) ;
  final Upgrade bonus = yard.bonusFor(yard.nextDrill) ;
  final int quality = bonus == null ? 0 :
    (1 + store.structure.upgradeLevel(bonus)) ;
  
  actor.gear.addItem(Item.withQuality(equipment, quality)) ;
  return true ;
}


public boolean actionEquipYard(Actor actor, Element dummy) {
  if (verbose) I.sayAbout(actor, "Installing drill equipment...") ;
  final Item equipment = actor.gear.bestSample(SAMPLES, yard.belongs, 1) ;
  yard.drill = yard.nextDrill ;
  yard.equipQuality = (int) (equipment.quality * 5) ;
  actor.gear.removeItem(equipment) ;
  
  yard.updateSprite() ;
  return true ;
}
//*/

/*
public boolean actionDrillMelee(Actor actor, Target dummy) {
  final int DC = yard.drillDC(DrillYard.DRILL_MELEE) ;
  actor.traits.test(HAND_TO_HAND, DC, 0.5f) ;
  actor.traits.test(SHIELD_AND_ARMOUR, DC - 5, 0.5f) ;
  DeviceType.applyFX(actor.gear.deviceType(), actor, dummy, true) ;
  return true ;
}


public boolean actionDrillRanged(Actor actor, Target dummy) {
  final int DC = yard.drillDC(DrillYard.DRILL_RANGED) ;
  actor.traits.test(MARKSMANSHIP, DC, 0.5f) ;
  actor.traits.test(SURVEILLANCE, DC - 5, 0.5f) ;
  DeviceType.applyFX(actor.gear.deviceType(), actor, dummy, true) ;
  return true ;
}


public boolean actionDrillEndurance(Actor actor, Target target) {
  final int DC = yard.drillDC(DrillYard.DRILL_ENDURANCE) ;
  actor.traits.test(ATHLETICS, DC, 0.5f) ;
  actor.traits.test(STEALTH_AND_COVER, DC - 5, 0.5f) ;
  return true ;
}


public boolean actionDrillAid(Actor actor, Target target) {
  final int DC = yard.drillDC(DrillYard.DRILL_AID) ;
  actor.traits.test(PHARMACY, DC, 0.5f) ;
  actor.traits.test(ANATOMY, DC - 5, 0.5f) ;
  return true ;
}
//*/