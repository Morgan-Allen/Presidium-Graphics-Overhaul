

package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



//  First aid.  Intensive care.  Cosmetic options.
//  Disease treatment.  Birth control.  Neural backups.


//
//  ...There's also an occasional bug where more than one actor can wind up
//  trying to deliver the patient.  That'll likely have to be sorted out with
//  suspensor-persistence in general, since they're related.


//
//  TODO:  Only include First Aid here.  Split off other forms of treatment to
//  a separate class, specifically intended for Sickbay employees (or possibly
//  native Shamans.)



/*
public class Treatment extends Plan implements Economy {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
/*
  final public static int
    TYPE_FIRST_AID    = 0, FIRST_AID_DC    = 5 , FIRST_AID_XP    = 10,
    TYPE_MEDICATION   = 1, MEDICATION_DC   = 10, MEDICATION_XP   = 20,
    TYPE_PSYCH_EVAL   = 2, PSYCH_EVAL_DC   = 15, PSYCH_EVAL_XP   = 40,
    TYPE_RECONSTRUCT  = 3, RECONSTRUCT_DC  = 20, RECONSTRUCT_XP  = 75,
    TYPE_GENE_THERAPY = 4, GENE_THERAPY_DC = 25, GENE_THERAPY_XP = 150,
    TYPE_CONDITIONING = 5, CONDITIONING_DC = 30, CONDITIONING_XP = 250 ;
  final static int
    STAGE_NONE      = 0,
    STAGE_EMERGENCY = 1,
    STAGE_TRANSPORT = 2,
    STAGE_FOLLOW_UP = 3 ;
  final static float
    SHORT_DURATION  = World.STANDARD_DAY_LENGTH,
    MEDIUM_DURATION = World.STANDARD_DAY_LENGTH * 10,
    LONG_DURATION   = World.STANDARD_DAY_LENGTH * 100 ;
  
  private static boolean verbose = false ;
  
  
  final Actor patient ;
  final Venue theatre ;
  protected int type = -1 ;
  
  private int treatDC = -1 ;
  private Skill majorSkill, minorSkill ;
  private Item accessory ;
  
  private Trait applied = null ;
  private Item result = null ;
  
  
  
  //  TODO:  Allow the type of treatment to be optionally specified here-
  public Treatment(Actor actor, Actor patient, Venue theatre) {
    super(actor, patient) ;
    this.patient = patient ;
    
    configFor(patient, theatre, actor == null) ;
    if (result == null) {
      final Action asEffect = new Action(
        patient, patient,
        this, "actionAsItem",
        Action.STAND, descForAction()
      ) ;
      result = Item.asMatch(SERVICE_TREAT, asEffect) ;
    }
    
    final boolean valid = canTreat() ;
    
    if (theatre == null && actor != null) {
      final Target t = Retreat.nearestHaven(actor, Sickbay.class) ;
      if (t instanceof Venue) this.theatre = (Venue) t ;
      else this.theatre = null ;
    }
    else this.theatre = theatre ;
    
    if (verbose && I.talkAbout == actor && valid) {
      I.say("\nConsidering treatment of "+patient+" at "+theatre) ;
      I.say("Treatment type is: "+type) ;
      I.say("Trait applied is: "+applied) ;
      I.say("Treat DC is: "+treatDC) ;
      I.say("Accessory is: "+accessory) ;
    }
  }
  
  
  public Treatment(Session s) throws Exception {
    super(s) ;
    patient = (Actor) s.loadObject() ;
    theatre = (Venue) s.loadObject() ;
    type = s.loadInt() ;
    
    treatDC = s.loadInt() ;
    majorSkill = (Skill) Trait.loadFrom(s) ;
    minorSkill = (Skill) Trait.loadFrom(s) ;
    accessory = Item.loadFrom(s) ;
    
    applied = Trait.loadFrom(s) ;
    result = Item.loadFrom(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(patient) ;
    s.saveObject(theatre) ;
    s.saveInt(type) ;
    
    s.saveInt(treatDC) ;
    Trait.saveTo(s, majorSkill) ;
    Trait.saveTo(s, minorSkill) ;
    Item.saveTo(s, accessory) ;
    
    Trait.saveTo(s, applied) ;
    Item.saveTo(s, result) ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Treatment t = (Treatment) p ;
    return t.type == this.type && t.applied == this.applied ;
  }
  
  
  private void configFor(Actor patient, Venue theatre, boolean forLeave) {
    
    treatDC = 0 ;
    accessory = null ;
    if (
      patient.aboard() != theatre && patient.health.goodHealth() &&
      ! forLeave
    ) {
      return ;
    }
    
    final Batch <Item> OT = patient.gear.matches(SERVICE_TREAT) ;
    
    final Item aidResult = treatResult(OT, TYPE_FIRST_AID, null) ;
    if (
      ofType(TYPE_FIRST_AID, null) &&
      (patient.health.bleeding() || patient.health.injuryLevel() > 0) &&
      (aidResult == null || aidResult.amount < 1)
    ) {
      treatDC = (int) (patient.health.injuryLevel() * 15) ;
      if (patient.health.bleeding()) treatDC += 5 ;
      majorSkill = ANATOMY ;
      minorSkill = PHARMACY ;
      accessory  = Item.withAmount(STIM_KITS, 1) ;
      type = TYPE_FIRST_AID ;
      applied = INJURY ;
      if (aidResult != null) result = aidResult ;
      return ;
    }
    else if (theatre == null) return ;
    
    for (Condition condition : TREATABLE_CONDITIONS) {
      if (! ofType(TYPE_MEDICATION, condition)) continue ;
      final Item medResult = treatResult(OT, TYPE_MEDICATION, condition) ;
      final float symptoms = patient.traits.useLevel(condition) ;
      if (symptoms > 0 && (medResult == null || medResult.amount < 1)) {
        treatDC = (int) (condition.virulence * (1 + symptoms) / 2) ;
        majorSkill = PHARMACY ;
        minorSkill = GENE_CULTURE ;
        accessory  = Item.withAmount(MEDICINE, 1) ;
        type = TYPE_MEDICATION ;
        applied = condition ;
        if (medResult != null) result = medResult ;
        return ;
      }
    }
    
    if (
      actor != patient && ofType(TYPE_PSYCH_EVAL, null) &&
      theatre.structure.upgradeLevel(Sickbay.NEURAL_SCANNING) > 0
    ) {
      final Item psyResult = treatResult(OT, TYPE_PSYCH_EVAL, null) ;
      final Profile profile = theatre.base().profiles.profileFor(patient) ;
      
      if (
        profile.daysSincePsychEval(theatre.world()) > 10 &&
        (psyResult == null || psyResult.amount < 1)
      ) {
        treatDC = PSYCH_EVAL_DC ;
        treatDC += (1 - patient.health.moraleLevel()) * 5 ;
        majorSkill = PSYCHOANALYSIS ;
        minorSkill = PHARMACY ;
        accessory  = Item.withAmount(STIM_KITS, 1) ;
        type = TYPE_PSYCH_EVAL ;
        if (psyResult != null) result = psyResult ;
        return ;
      }
    }
    
    final Item recResult = treatResult(OT, TYPE_RECONSTRUCT, null) ;
    if (
      ofType(TYPE_RECONSTRUCT, null) &&
      patient.health.suspended() && (recResult == null || recResult.amount < 1)
    ) {
      treatDC = 20 ;
      majorSkill = ANATOMY ;
      minorSkill = PHARMACY ;
      
      accessory = Item.asMatch(REPLICANTS, patient) ;
      type = TYPE_RECONSTRUCT ;
      if (recResult != null) result = recResult ;
      return ;
    }
  }
  
  
  private boolean ofType(int type, Trait applied) {
    if (this.type == -1) return true ;
    if (applied != null && applied != this.applied) return false ;
    return this.type == type ;
  }
  
  
  private float diagnoseBonus() {
    float manners = -5 ;
    manners += 5 * patient.mind.relationValue(actor) ;
    manners += 5 * actor.mind.relationValue(patient) ;
    manners /= 2 ;
    if (actor.aboard() != theatre) return manners ;
    
    Upgrade u = null ;
    if (type == TYPE_FIRST_AID  ) u = Sickbay.EMERGENCY_AID ;
    if (type == TYPE_MEDICATION ) u = Sickbay.APOTHECARY    ;
    if (type == TYPE_PSYCH_EVAL ) {
      u = Sickbay.NEURAL_SCANNING ;
      manners *= 2 ;
    }
    if (type == TYPE_RECONSTRUCT) {
      u = Sickbay.INTENSIVE_CARE  ;
      manners = 0 ;
    }
    return (((1 + theatre.structure.upgradeBonus(u)) * 5) / 2f) + manners ;
  }
  
  
  
  /**  Evaluating targets and priorities-
    */
/*
  public float priorityFor(Actor actor) {
    if (! canTreat()) return 0 ;
    if (patient.health.goodHealth() && patient.aboard() != theatre) {
      return 0 ;
    }
    if (! hasBegun() && Plan.competition(Treatment.class, patient, actor) > 1) {
      return 0 ;
    }
    float impetus = treatDC / 2f ;
    //
    //  Modify priority based on fitness for the task, relative to skill DCs-
    float chance = 1 ;
    float testDC = treatDC - diagnoseBonus() ;
    chance *= actor.traits.chance(majorSkill, testDC) ;
    chance *= actor.traits.chance(minorSkill, testDC) ;
    impetus *= (1 + chance) / 2f ;
    //
    //  Next, modify based on personality, distance and danger, et cetera-
    impetus *= actor.traits.scaleLevel(EMPATHIC) ;
    impetus -= Plan.rangePenalty(actor, patient) ;
    impetus -= Plan.dangerPenalty(patient, actor) ;
    impetus += priorityMod ;
    
    if (patient.aboard() != theatre && ! patient.health.conscious()) {
      impetus += ROUTINE ;
    }
    
    if (verbose && I.talkAbout == actor) {
      I.say("Treatment impetus "+impetus) ;
      I.say("Treat DC: "+treatDC+", theatre: "+theatre) ;
    }
    final float max = patient.health.alive() ? PARAMOUNT : URGENT ;
    return Visit.clamp(impetus, IDLE, max) ;
  }
  
  
  public int motionType(Actor actor) {
    if (! patient.health.conscious()) return MOTION_FAST ;
    return super.motionType(actor) ;
  }


  protected float baseUrgency() {
    configFor(patient, theatre, actor == null) ;
    return treatDC * 2f / 5 ;
  }
  
  
  private boolean canTreat() {
    //
    //  TODO:  Dat's racist!
    if (! (patient instanceof Human)) return false ;
    if (theatre != null && theatre.destroyed()) return false ;
    if (type == -1 || accessory == null || result == null) return false ;
    if (GameSettings.hardCore || type == TYPE_RECONSTRUCT) {
      if (actor.gear.amountOf(accessory) >= 0.1f) return true ;
      if (theatre != null && theatre.stocks.hasItem(accessory)) return true ;
      return false ;
    }
    return true ;
  }
  
  
  private static Item treatResult(
    Batch <Item> matches, int type, Trait applied
  ) {
    for (Item match : matches) {
      final Action action = (Action) match.refers ;
      final Treatment treatment = (Treatment) action.basis ;
      if (treatment.type != type) continue ;
      if (applied != null && treatment.applied != applied) continue ;
      return match ;
    }
    return null ;
  }
  
  
  protected Item treatResult() {
    final Batch <Item> matches = patient.gear.matches(SERVICE_TREAT) ;
    return treatResult(matches, type, applied) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
/*
  protected Behaviour getNextStep() {
    if (! canTreat()) return null ;
    
    final Item treatResult = treatResult() ;
    final boolean
      unsafe = (! patient.health.goodHealth()) && patient.aboard() != theatre,
      canDeliver = theatre != null,
      fullTreat = (treatResult != null && treatResult.amount >= 1) ;
    
    if (verbose && treatResult != null && I.talkAbout == actor) {
      I.say("Treatment progress: "+treatResult.amount+", "+this.hashCode()) ;
      I.say("Can complete? "+(fullTreat && ! (unsafe && canDeliver))) ;
    }
    //
    //  Check to see if you're finished-
    if (fullTreat && ! (unsafe && canDeliver)) {
      //
      //  But clean up afterwards-
      if (actor.gear.amountOf(accessory) > 0 && theatre != null) {
        final Action cleanup = new Action(
          actor, theatre,
          this, "actionCleanup",
          Action.REACH_DOWN, "Cleaning up after"
        ) ;
        return cleanup ;
      }
      else return null ;
    }
    //
    //  Pick up accessories if you can-
    if (
      actor.aboard() == theatre && theatre.stocks.hasItem(accessory) &&
      actor.gear.amountOf(accessory) <= 0
    ) {
      final Action pickup = new Action(
        actor, theatre,
        this, "actionPickupAccessories",
        Action.REACH_DOWN, "Picking up "+accessory.type+" for"
      ) ;
      return pickup ;
    }
    //
    //  If needed, get the patient inside.
    final boolean
      needsAid = patient.health.bleeding() &&
      type == TYPE_FIRST_AID ;
    if (unsafe && canDeliver && ! needsAid) {
      if (! Suspensor.canCarry(actor, patient)) return null ;
      final Delivery d = new Delivery(patient, theatre) ;
      return d ;
    }
    //
    //  Otherwise, administer treatment-
    if (result == null) {
      I.say("NO RESULT FOR "+this.hashCode()+", VALID? "+canTreat()) ;
    }
    
    if (patient.gear.amountOf(result) < 1) {
      final Action treating = new Action(
        actor, patient,
        this, "actionDoTreat",
        Action.BUILD, "Treating"
      ) ;
      return treating ;
    }
    return null ;
  }
  
  
  public boolean actionPickupAccessories(Actor actor, Venue theatre) {
    final Item item = theatre.stocks.matchFor(accessory) ;
    if (item == null) return false ;
    theatre.stocks.transfer(item, actor) ;
    return true ;
  }
  
  
  public boolean actionCleanup(Actor actor, Venue theatre) {
    final Item item = actor.gear.matchFor(accessory) ;
    if (item == null) return false ;
    actor.gear.removeItem(item) ;
    if (item.type.form == FORM_COMMODITY) theatre.stocks.addItem(item) ;
    return true ;
  }
  
  
  public boolean actionDoTreat(Actor actor, Actor patient) {
    if (! canTreat()) return false ;
    
    final Item used = Item.withAmount(accessory, 0.1f) ;
    final boolean hasUsed = actor.gear.hasItem(used) ;
    
    float checkMod = hasUsed ? 0 : +5 ;
    checkMod -= diagnoseBonus() ;
    float success = Rand.num(), speed = 1 ;
    if (actor.traits.test(majorSkill, treatDC + checkMod, 0.5f)) {
      success++ ;
    }
    else success-- ;
    if (actor.traits.test(minorSkill, treatDC + checkMod - 5, 0.5f)) {
      success++ ;
    }
    else success-- ;
    if (actor.gear.hasItem(used) && success > 0) {
      success += 2 ;
      speed++ ;
      actor.gear.removeItem(used) ;
    }
    
    if (success > 0) {
      final Item treatResult = treatResult() ;
      if (treatResult != null && treatResult != result) result = treatResult ;
      
      Item added = Item.withAmount(result, speed * 0.1f) ;
      added = Item.withQuality(added, (int) success) ;
      patient.gear.addItem(added) ;
      actionAsItem(patient, patient) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionAsItem(Actor patient, Actor same) {
    final Item treatResult = patient.gear.matchFor(result) ;
    if (treatResult == null) return false ;
    final float effect = treatResult.quality / 5 ;
    
    if (verbose) {
      I.sayAbout(patient, "Treatment quality: "+effect) ;
    }
    
    if (type == TYPE_FIRST_AID) {
      if (patient.health.dying()) {
        if (verbose) I.sayAbout(patient, "  Putting in stasis: "+patient) ;
        //
        //  Place the actor in a state of stasis-
        if (Rand.num() < effect / 10f) {
          patient.health.setState(ActorHealth.STATE_SUSPEND) ;
        }
      }
      else if (patient.health.alive()) {
        //
        //  Otherwise, regenerate injury using natural vigour-
        float regen = ActorHealth.INJURY_REGEN_PER_DAY ;
        regen *= 3 * effect * patient.health.maxHealth() ;
        if (verbose) I.sayAbout(patient, "  Regenerating injury/day: "+regen) ;
        regen /= World.STANDARD_DAY_LENGTH ;
        patient.health.liftInjury(regen) ;
        depleteResult(patient, SHORT_DURATION) ;
      }
      return true ;
    }
    
    if (type == TYPE_MEDICATION) {
      //
      //  Combat the current infection-
      final float inc = 1f / SHORT_DURATION ;
      //if (verbose) I.sayAbout(patient, "  Reducing infection: "+(3 * effect)) ;
      patient.traits.incLevel(applied, -inc * 3 * effect) ;
      patient.gear.removeItem(Item.withAmount(result, inc)) ;
      depleteResult(patient, SHORT_DURATION) ;
      return true ;
    }
    
    if (type == TYPE_RECONSTRUCT) {
      //
      //  Gradually restore health.  Revive once in decent shape.
      final float maxHealth = patient.health.maxHealth() ;
      float regen = 5 * maxHealth * effect ;
      if (verbose) {
        I.sayAbout(patient, "Regen effect/10 days: "+regen) ;
        I.sayAbout(patient, "Injury level: "+patient.health.injuryLevel()) ;
      }
      patient.health.liftInjury(regen / MEDIUM_DURATION) ;
      if (
        patient.health.injuryLevel() < ActorHealth.REVIVE_THRESHOLD &&
        patient.health.isState(ActorHealth.STATE_SUSPEND)
      ) {
        patient.health.setState(ActorHealth.STATE_RESTING) ;
        patient.health.takeFatigue(maxHealth / 2f) ;
        reconstructFaculties(patient) ;
      }
      else patient.health.setState(ActorHealth.STATE_SUSPEND) ;
      depleteResult(patient, MEDIUM_DURATION) ;
      return true ;
    }
    
    if (type == TYPE_PSYCH_EVAL) {
      //
      //  Combat poor morale and update engram records-
      if (actor.gear.amountOf(result) == 1) {
        final Profile p = actor.base().profiles.profileFor(patient) ;
        p.setPsychEvalTime(actor.world().currentTime()) ;
      }
      patient.health.adjustMorale((1 + effect) / (2 * MEDIUM_DURATION)) ;
      depleteResult(patient, MEDIUM_DURATION) ;
      return true ;
    }
    
    return true ;
  }
  
  
  private void depleteResult(Actor patient, float duration) {
    patient.gear.removeItem(Item.withAmount(result, 1f / duration)) ;
    ///if (patient.gear.amountOf(result) == 0) I.say("TREATMENT EXPIRED!") ;
  }
  
  
  public static Item replicantFor(Actor patient) {
    final Batch <Item> matches = patient.gear.matches(SERVICE_TREAT) ;
    final Item treatResult = treatResult(matches, TYPE_RECONSTRUCT, null) ;
    if (treatResult != null) return null ;

    float injury = patient.health.injuryLevel() - 0.5f ;
    injury = Visit.clamp(injury * 2, 0, 1) ;
    final int quality = (int) (injury * 5) ;
    final Item ordered = Item.with(REPLICANTS, patient, 1, quality) ;
    
    return ordered ;
  }
  
  
  //
  //  TODO:  This should really require a separate Psychoanalysis check, stored
  //  as a separate treatment item...
  private void reconstructFaculties(Actor patient) {
    final World world = patient.world() ;
    final Profile p = theatre.base().profiles.profileFor(patient) ;
    float memoryLoss = p.daysSincePsychEval(world) / (MEDIUM_DURATION * 2) ;
    memoryLoss = Visit.clamp(memoryLoss, 0.1f, 0.9f) ;
    
    for (Relation r : patient.mind.relations()) {
      float level = r.value() ;
      level *= 1 - (memoryLoss * Rand.avgNums(2) * (1 - Math.abs(level))) ;
      patient.mind.initRelation(r.subject, level, Rand.num()) ;
    }
    
    for (Skill s : patient.traits.skillSet()) {
      float level = patient.traits.traitLevel(s) ;
      level *= 1 - (memoryLoss * Rand.avgNums(2) / 2f) ;
      patient.traits.setLevel(s, level) ;
    }
  }
  
  
  
  /**  Rendering and interface-
    */
/*
  private String descForAction() {
    String effectDesc = "?" ;
    if (applied != null) effectDesc = "for "+applied ;
    else if (type == TYPE_RECONSTRUCT) effectDesc = "reconstruction" ;
    else if (type == TYPE_PSYCH_EVAL ) effectDesc = "psych evaluation" ;
    return effectDesc ;
  }
  
  
  public void describeBehaviour(Description d) {
    if (applied != null) {
      ///if (stage == STAGE_TRANSPORT) super.describedByStep(d) ;
      if (! super.describedByStep(d)) d.append("Treating") ;
      d.append(" ") ;
      d.append(patient) ;
      d.append(" for ") ;
      d.append(applied) ;
    }
    else if (type == TYPE_RECONSTRUCT) {
      d.append("Reconstructing ") ;
      d.append(patient) ;
    }
    else if (type == TYPE_PSYCH_EVAL) {
      d.append("Performing Psych Eval for ") ;
      d.append(patient) ;
    }
    else {
      d.append("Treating ") ;
      d.append(patient) ;
    }
  }
  
  
  protected void descForPatient(Description d) {
    if (applied != null) {
      d.append("Seeking treatment for ") ;
      d.append(applied) ;
    }
    else if (type == TYPE_PSYCH_EVAL) {
      d.append("Undergoing neural scan") ;
    }
    d.append(" at ") ;
    d.append(theatre) ;
  }
}
//*/





