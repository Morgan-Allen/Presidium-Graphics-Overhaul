

package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Conditions.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  There's room for some additional refactoring here...

public class Treatment extends Plan implements Item.Passive {

  final static int
    STANDARD_TREAT_TIME  = Stage.STANDARD_HOUR_LENGTH,
    STANDARD_EFFECT_TIME = Stage.STANDARD_DAY_LENGTH ,
    TIME_XP_MULT         = 4;
  
  private static boolean
    evalVerbose   = false,
    eventVerbose  = false;
  
  final Actor patient;
  final Condition sickness;
  protected Boarding sickbay;
  
  
  protected Treatment(
    Actor treats, Actor patient,
    Condition treated, Plan parent, Boarding sickbay
  ) {
    super(treats, patient, MOTIVE_JOB, REAL_HELP);
    this.patient  = patient;
    this.sickness = treated;
    this.sickbay  = sickbay;
    if (parent != null) setMotivesFrom(parent, 0);
  }
  
  
  public Treatment(Session s) throws Exception {
    super(s);
    patient  = (Actor    ) s.loadObject();
    sickness = (Condition) s.loadObject();
    sickbay  = (Boarding ) s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(patient );
    s.saveObject(sickness);
    s.saveTarget(sickbay );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Treatment(other, patient, sickness, this, sickbay);
  }
  
  
  
  /**  Target and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { EMPATHIC, CURIOUS, DUTIFUL };
  final static Skill BASE_SKILLS[] = { PHARMACY, GENE_CULTURE };
  
  
  public static Treatment nextTreatment(
    Actor treats, Actor patient, Boarding sickbay
  ) {
    final boolean report = evalVerbose && (
      I.talkAbout == treats || I.talkAbout == patient
    );
    //
    //  By default, we pick the most severe conditions first.
    final Pick <Condition> pick = new Pick <Condition> (null, 0);
    for (Condition c : Conditions.ALL_CONDITIONS) {
      pick.compare(c, dangerRating(c, patient));
    }
    if (pick.empty()) return null;
    if (report) {
      I.say("\nConsidering treatment for "+patient);
      I.say("  Most pressing condition: "+pick.result());
      I.say("  Danger level: "+pick.bestRating());
    }
    
    final Treatment treatment = new Treatment(
      treats, patient, pick.result(), null, sickbay
    );
    treatment.toggleMotives(MOTIVE_EMERGENCY, true);
    return treatment;
  }
  
  
  public static Item existingTreatment(Condition c, Actor patient) {
    for (Item i : patient.gear.matches(TREATMENT)) {
      final Treatment t = (Treatment) i.refers;
      if (t.sickness == c) return i;
    }
    return null;
  }
  
  
  public static boolean hasTreatment(Condition c, Actor patient, boolean full) {
    final Item i = existingTreatment(c, patient);
    return i != null && i.amount >= (full ? 1 : 0.5f);
  }
  
  
  public static float dangerRating(Condition c, Actor patient) {
    final float
      level  = patient.traits.usedLevel(c),
      speed  = Nums.clamp((1f / (1 + c.latency)) + level, 0, 1),
      danger = (c.virulence + c.spread);
    return level <= 0 ? 0 : (speed * danger / Conditions.EXTREME_VIRULENCE);
  }
  
  
  protected float severity() {
    final float level = patient.traits.usedLevel(sickness);
    return level * sickness.virulence / Conditions.EXTREME_VIRULENCE;
  }
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == patient
    );
    
    final float severity = severity();
    if (report) {
      I.say("Getting treatment priority for "+patient);
      I.say("  Severity: "+severity);
    }
    
    if (patient.health.conscious() && ! patient.indoors()) {
      if (report) I.say("  Patient is up and about!");
      return 0;
    }
    if (hasTreatment(sickness, patient, hasBegun())) {
      if (report) I.say("  Patient already treated.");
      return 0;
    }
    
    setCompetence(successChanceFor(actor));
    return PlanUtils.supportPriority(
      actor, patient, motiveBonus(), competence(), severity
    );
  }
  
  
  public float successChanceFor(Actor actor) {
    return tryTreatment(
      actor, patient,
      sickness, PhysicianStation.MEDICAL_LAB,
      PHARMACY, GENE_CULTURE, false
    );
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final Action aids = new Action(
      actor, patient,
      this, "actionDoTreatment",
      Action.BUILD, "Treating "
    );
    return aids;
  }
  

  //  TODO:  Re-include this step.
  /*
  private float diagnoseBonus() {
    float manners = -5;
    manners += 5 * patient.memories.relationValue(actor);
    manners += 5 * actor.memories.relationValue(patient);
    manners /= 2;
    if (actor.aboard() != theatre) return manners;
    
    Upgrade u = null;
    if (type == TYPE_FIRST_AID  ) u = Sickbay.EMERGENCY_AID;
    if (type == TYPE_MEDICATION ) u = Sickbay.APOTHECARY   ;
    if (type == TYPE_PSYCH_EVAL ) {
      u = Sickbay.NEURAL_SCANNING;
      manners *= 2;
    }
    if (type == TYPE_RECONSTRUCT) {
      u = Sickbay.INTENSIVE_CARE ;
      manners = 0;
    }
    return (((1 + theatre.structure.upgradeBonus(u)) * 5) / 2f) + manners;
  }
  //*/
  
  
  public boolean actionDoTreatment(Actor actor, Actor patient) {
    return tryTreatment(
      actor, patient,
      sickness, PhysicianStation.MEDICAL_LAB,
      PHARMACY, GENE_CULTURE, true
    ) > 0;
  }
  
  
  protected float tryTreatment(
    Actor actor, Actor patient,
    Condition sickness, Upgrade tech,
    Skill primary, Skill secondary,
    boolean realAction
  ) {
    //
    //  Firstly, we calculate the overall difficulty of treatment, and any
    //  circumstances bonuses or penalties that might apply (such as facility
    //  upgrades, indoors or out, etc.  Note that a secondary skill cannot be
    //  used without medicine!)
    final float DC = severity() * 10;
    float bonus = 0, check = 0;
    Owner hasMeds = null;
    
    if (sickbay instanceof Venue) {
      final Venue sickbay = (Venue) this.sickbay;
      bonus += 5 * sickbay.structure().upgradeLevel(tech);
      if (sickbay.stocks.amountOf(MEDICINE) > 0.1f) hasMeds = sickbay;
    }
    else bonus -= 2.5f;
    if (actor.gear.amountOf(MEDICINE) > 0.1f) {
      hasMeds = actor;
    }
    if (hasMeds != null) bonus += 5;
    else { bonus /= 2; secondary = null; }
    //
    //  In the case of a real treatment attempt, we deduct a small portion of
    //  any carried medicine, and add a treatment item to the patient's gear.
    if (realAction) {
      Item current = existingTreatment(sickness, patient);
      if (current == null) current = Item.with(TREATMENT, this, 0, 0);
      
      final float timeInc = 1f / STANDARD_TREAT_TIME;
      if (actor.skills.test(primary  , DC - bonus, TIME_XP_MULT)) check++;
      if (actor.skills.test(secondary, 5  - bonus, TIME_XP_MULT)) check++;
      
      final float quality = current.amount == 0 ? 0 : (
        (Item.MAX_QUALITY + 1) * check / 2
      );
      current = Item.with(current.type, current.refers, timeInc, quality);
      patient.gear.addItem(current);
      
      final Item used = Item.withAmount(MEDICINE, 0.1f * timeInc);
      if (hasMeds != null) hasMeds.inventory().removeItem(used);
    }
    //
    //  If this is strictly for estimation purposes, we average the chance of
    //  successful skill-tests, and return this result.
    else {
      check += actor.skills.chance(primary  , DC - bonus);
      check += actor.skills.chance(secondary, 5  - bonus);
      check = (check + 1) / 3;
    }
    return check;
  }
  
  
  public void applyPassiveItem(Actor carries, Item from) {
    if (carries.traits.traitLevel(sickness) <= 0) return;
    final boolean report = eventVerbose && I.talkAbout == carries;
    
    float effect = 1.0f / Stage.STANDARD_DAY_LENGTH;
    float bonus = (5 + from.quality) / 5f;
    carries.traits.incLevel(sickness, 0 - effect * bonus);
    carries.gear.removeItem(Item.withAmount(from, effect));
    
    if (report) {
      final float
        level   = carries.traits.traitLevel(sickness),
        symptom = carries.traits.usedLevel (sickness);
      final String
        desc = carries.traits.description(sickness);
      I.say("\nApply treatment for "+sickness+" to "+carries);
      I.say("  Disease progression: "+level);
      I.say("  Symptom level:       "+symptom+" ("+desc+")");
      I.say("  Bonus/effect:        "+bonus+"/"+effect);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Treating ");
    d.append(patient);
    d.append(" for ");
    d.append(sickness);
  }
  
  
  public String describePassiveItem(Item from) {
    return " for "+sickness;
  }
}


/*
Item current = existingTreatment(sickness, patient);
if (current == null) current = Item.with(TREATMENT, this, 0, 0);
final float
  inc   = 1f / STANDARD_TREAT_TIME,
  DC    = severity() * 10,
  bonus = getVenueBonus(true, tech);

float check = Rand.yes() ? -1 : 1;
if (actor.skills.test(primary  , DC - bonus, TIME_XP_MULT)) check++;
if (actor.skills.test(secondary, 5  - bonus, TIME_XP_MULT)) check++;

if (check > 0) {
  final float quality = current.amount == 0 ? 1 :
    (Item.MAX_QUALITY * (check - 1) / 2);
  current = Item.with(current.type, current.refers, inc, quality);
  patient.gear.addItem(current);
  return true;
}
else return false;
//*/


/*
protected float getVenueBonus(boolean use, Upgrade tech) {
  if (! (sickbay instanceof Venue)) return -2.5f;
  
  final Venue sickbay = (Venue) this.sickbay;
  float bonus = 0, used = 0.1f / STANDARD_TREAT_TIME;
  bonus += 5 * sickbay.structure.upgradeLevel(tech);
  
  if (sickbay.stocks.amountOf(MEDICINE) > 0.1f) {
    if (use) sickbay.stocks.removeItem(Item.withAmount(MEDICINE, used));
    bonus += 5;
  }
  else bonus /= 2;
  return bonus;
}
//*/



