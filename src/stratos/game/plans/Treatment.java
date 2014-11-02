

package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.building.Economy.*;
import static stratos.game.actors.Conditions.INJURY;
import static stratos.game.actors.Qualities.*;



public class Treatment extends Plan implements Item.Passive {
  
  
  private static boolean
    evalVerbose  = true ,
    eventVerbose = true ;
  
  final Actor patient;
  final Condition sickness;
  protected Boarding sickbay;
  
  
  protected Treatment(
    Actor treats, Actor patient, Condition treated, Boarding sickbay
  ) {
    super(treats, patient, true);
    this.patient = patient;
    this.sickness = treated;
    this.sickbay = sickbay;
  }
  
  
  public Treatment(Session s) throws Exception {
    super(s);
    patient = (Actor    ) s.loadObject();
    sickness = (Condition) s.loadObject();
    sickbay = (Boarding ) s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(patient);
    s.saveObject(sickness);
    s.saveTarget(sickbay);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Treatment(other, patient, sickness, sickbay);
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
    for (Condition c : Conditions.TREATABLE_CONDITIONS) {
      pick.compare(c, dangerRating(c, patient));
    }
    if (pick.empty()) return null;
    if (report) {
      I.say("\nConsidering treatment for "+patient);
      I.say("  Most pressing condition: "+pick.result());
      I.say("  Danger level: "+pick.bestRating());
    }
    
    return new Treatment(treats, patient, pick.result(), sickbay);
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
      speed  = Visit.clamp((1f / (1 + c.latency)) + level, 0, 1),
      danger = (c.virulence + c.spread);
    return level <= 0 ? 0 : (speed * danger / Conditions.EXTREME_VIRULENCE);
  }
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == patient
    );
    if (report) I.say("Getting treatment priority for "+patient);
    
    if (patient.health.conscious() && ! patient.indoors()) {
      if (report) I.say("  Patient is up and about!");
      return 0;
    }
    if (hasTreatment(sickness, patient, hasBegun())) {
      if (report) I.say("  Patient already treated.");
      return 0;
    }
    /*
    final float bonus = getVenueBonus(false, PhysicianStation.APOTHECARY);
    if (bonus <= 0) {
      if (report) I.say("  Cannot treat without facilities!");
      return 0;
    }
    //*/
    
    final float severity = severity(), modifier = typeModifier();
    final float priority = priorityForActorWith(
      actor, patient,
      CASUAL + (severity * ROUTINE), modifier,
      REAL_HELP, FULL_COOPERATION,
      BASE_SKILLS, BASE_TRAITS,
      PARTIAL_DISTANCE_CHECK, MILD_FAIL_RISK * severity,
      report
    );
    return priority;
  }
  
  
  protected float typeModifier() {
    float modifier = NO_MODIFIER;
    //  TODO:  You need a generalised method for this?
    if (patient.base() != actor.base()) {
      modifier -= (1 - actor.relations.valueFor(patient.base())) * ROUTINE;
    }
    //  TODO:  Dat's racist!
    if (patient.species() != actor.species()) {
      modifier -= ROUTINE;
    }
    return modifier / 2;
  }
  
  
  protected float severity() {
    final float level = patient.traits.usedLevel(sickness);
    return level * sickness.virulence / Conditions.EXTREME_VIRULENCE;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    /*
    final boolean report = eventVerbose && (
      I.talkAbout == actor || I.talkAbout == patient
    );
    //*/

    final Action aids = new Action(
      actor, patient,
      this, "actionDoTreatment",
      Action.BUILD, "Treating "
    );
    return aids;
  }
  

  //  TODO:  Include a diagnosis step/bonus?
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
    
    Item current = existingTreatment(sickness, patient);
    if (current == null) current = Item.withReference(TREATMENT, this);
    
    final float
      severity = severity(),
      bonus    = getVenueBonus(true, PhysicianStation.APOTHECARY);
    boolean success = true;
    success &= actor.skills.test(PHARMACY    , (severity * 10) - bonus, 5f);
    success &= actor.skills.test(GENE_CULTURE, 5 - bonus              , 5f);
    
    if (success) {
      patient.gear.addItem(Item.withAmount(current, 0.1f));
      return true;
    }
    else return false;
  }
  
  
  protected float getVenueBonus(boolean use, Upgrade tech) {
    if (! (sickbay instanceof Venue)) return 0;
    final Venue sickbay = (Venue) this.sickbay;
    
    float bonus = 0;
    if (sickbay.stocks.amountOf(MEDICINE) > 0.1f) {
      if (use) sickbay.stocks.removeItem(Item.withAmount(MEDICINE, 0.01f));
      bonus += 5;
    }
    bonus += 5 * sickbay.structure.upgradeLevel(tech);
    
    return bonus > 0 ? bonus : -5;
  }
  
  
  protected float successChance() {
    final float
      severity = severity(),
      bonus    = getVenueBonus(false, PhysicianStation.APOTHECARY);
    float chance = 1;
    chance *= actor.skills.chance(PHARMACY    , (severity * 10) - bonus);
    chance *= actor.skills.chance(GENE_CULTURE, 5 - bonus              );
    return chance;
  }
  
  
  public void applyPassiveItem(Actor carries, Item from) {
    float effect = 1.0f / Stage.STANDARD_DAY_LENGTH;
    //  TODO:  Modify effectiveness based on item quality.
    
    carries.traits.incLevel(sickness, 0 - effect);
    carries.gear.removeItem(Item.withAmount(from, effect));
    
    I.say("\nLevel of "+sickness+" is "+carries.traits.traitLevel(sickness));
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





