/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors ;
import org.apache.commons.math3.util.FastMath;

import stratos.game.common.*;
import stratos.util.*;



public class ActorHealth implements Qualities {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean verbose = false ;
  
  final public static int
    HUMAN_METABOLISM    = 0,
    ANIMAL_METABOLISM   = 1,
    ARTILECT_METABOLISM = 2,
    FOOD_TO_CALORIES   = 10;
  final public static int
    STATE_ACTIVE   = 0,
    STATE_RESTING  = 1,
    STATE_SUSPEND  = 2,
    STATE_DYING    = 3,
    STATE_DECOMP   = 4 ;
  final static String STATE_DESC[] = {
    "Active",
    "Asleep",
    "In Suspended Animation",
    "Dying",
    "Decomposed",
  } ;
  final public static int
    AGE_JUVENILE = 0,
    AGE_YOUNG    = 1,
    AGE_MATURE   = 2,
    AGE_SENIOR   = 3,
    AGE_MAX      = 4 ;
  final static String AGING_DESC[] = {
    "Juvenile",
    "Young",
    "Mature",
    "Senior"
  } ;
  
  final public static float
    DEFAULT_PRIME    = 25,
    DEFAULT_LIFESPAN = 60,
    LIFE_EXTENDS     = 0.1f,
    
    DEFAULT_BULK  = 1.0f,
    DEFAULT_SPEED = 1.0f,
    DEFAULT_SIGHT = 8.0f,

    DEFAULT_HEALTH  = 10,
    MAX_CALORIES    = 1.5f,
    STARVE_INTERVAL = World.STANDARD_DAY_LENGTH * 5,
    
    MAX_INJURY       =  1.5f,
    MAX_FATIGUE      =  1.0f,
    MAX_MORALE       =  0.5f,
    MIN_MORALE       = -1.5f,
    REVIVE_THRESHOLD =  0.5f,
    STABILISE_CHANCE =  0.2f,
    DECOMPOSE_TIME   =  World.STANDARD_DAY_LENGTH / 2,
    
    FATIGUE_GROW_PER_DAY = 0.33f,
    MORALE_DECAY_PER_DAY = 0.33f,
    INJURY_REGEN_PER_DAY = 0.33f,
    
    MAX_PSY_MULTIPLE = 10,
    PSY_REGEN_TIME   = World.STANDARD_DAY_LENGTH / 10 ;
  
  
  final Actor actor ;
  
  private float
    baseBulk  = DEFAULT_BULK,
    baseSpeed = DEFAULT_SPEED,
    baseSight = DEFAULT_SIGHT ;
  
  private float
    lifespan    = DEFAULT_LIFESPAN,
    lifeExtend  = 0 ;
  private int
    metabolism = HUMAN_METABOLISM ;
  
  private float
    currentAge = 0,
    maxHealth  = DEFAULT_HEALTH,
    calories   = DEFAULT_HEALTH / 2,
    nutrition  = 0.5f,
    injury     = 0,
    fatigue    = 0 ;
  private boolean
    bleeds = false ;
  private float
    morale    = 0,
    psyPoints = 0 ;
  
  private int
    state    = STATE_ACTIVE ;
  private float
    ageMultiple = 1.0f ;
  
  //
  //  I don't save/load these, since they refresh frequently anyway(?)
  private float stressCache = -1 ;
  
  
  
  ActorHealth(Actor actor) {
    this.actor = actor ;
  }
  
  
  void loadState(Session s) throws Exception {
    baseBulk  = s.loadFloat() ;
    baseSpeed = s.loadFloat() ;
    baseSight = s.loadFloat() ;
    
    lifespan    = s.loadFloat() ;
    currentAge  = s.loadFloat() ;
    lifeExtend  = s.loadFloat() ;
    ageMultiple = s.loadFloat() ;
    metabolism  = s.loadInt()   ;
    
    maxHealth = s.loadFloat() ;
    calories  = s.loadFloat() ;
    nutrition = s.loadFloat() ;
    injury    = s.loadFloat() ;
    fatigue   = s.loadFloat() ;
    bleeds    = s.loadBool () ;
    morale    = s.loadFloat() ;
    psyPoints = s.loadFloat() ;
    
    state = s.loadInt() ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveFloat(baseBulk ) ;
    s.saveFloat(baseSpeed) ;
    s.saveFloat(baseSight) ;
    
    s.saveFloat(lifespan   ) ;
    s.saveFloat(currentAge ) ;
    s.saveFloat(lifeExtend ) ;
    s.saveFloat(ageMultiple) ;
    s.saveInt  (metabolism ) ;
    
    s.saveFloat(maxHealth) ;
    s.saveFloat(calories ) ;
    s.saveFloat(nutrition) ;
    s.saveFloat(injury   ) ;
    s.saveFloat(fatigue  ) ;
    s.saveBool (bleeds   ) ;
    s.saveFloat(morale   ) ;
    s.saveFloat(psyPoints) ;
    
    s.saveInt(state) ;
  }
  
  
  
  /**  Supplementary setup/calibration methods-
    */
  public void initStats(
    int lifespan,
    float baseBulk,
    float baseSight,
    float baseSpeed,
    int metabolicType
  ) {
    this.lifespan = lifespan ;
    this.baseBulk  = baseBulk  * DEFAULT_BULK  ;
    this.baseSight = baseSight * DEFAULT_SIGHT ;
    this.baseSpeed = baseSpeed * DEFAULT_SPEED ;
    this.metabolism = metabolicType ;
  }
  
  
  public void setupHealth(
    float agingFactor,
    float overallHealth,
    float accidentChance
  ) {
    this.currentAge = lifespan * agingFactor ;
    updateHealth(-1) ;
    if (metabolism != ARTILECT_METABOLISM) {
      calories = Visit.clamp(Rand.num() + (overallHealth / 2), 0, 1) ;
      calories *= maxHealth ;
      nutrition = Visit.clamp(Rand.num() + overallHealth, 0, 1) ;
    }
    else {
      nutrition = 1 ;
      calories = maxHealth ;
    }
    
    fatigue = Rand.num() * (1 - (calories / maxHealth)) * maxHealth / 2f ;
    injury = Rand.num() * accidentChance * maxHealth / 2f ;
    morale = (Rand.num() - (0.5f + accidentChance)) / 2f ;
  }
  
  
  
  /**  Methods related to growth, reproduction, aging and death.
    */
  public float maxCalories() {
    return 1 + (maxHealth * MAX_CALORIES) ;
  }
  
  
  public void takeCalories(float amount, float quality) {
    amount = Visit.clamp(amount, 0, maxCalories() - calories) ;
    final float oldQual = nutrition * calories ;
    calories += amount ;
    nutrition = (oldQual + (quality * amount)) / calories ;
  }
  
  
  public void loseSustenance(float fraction) {
    calories -= fraction * maxHealth ;
  }
  
  
  public int agingStage() {
    return Visit.clamp((int) (ageLevel() * 4), 4) ;
  }
  
  
  public float ageLevel() {
    return currentAge * 1f / lifespan ;
  }
  
  
  public boolean juvenile() {
    return agingStage() <= AGE_JUVENILE ;
  }
  
  
  public void setMaturity(float ageLevel) {
    currentAge = lifespan * ageLevel ;
  }
  
  
  public int exactAge() {
    return (int) currentAge ;
  }
  
  
  public String agingDesc() {
    return AGING_DESC[agingStage()] ;
  }
  
  
  private float calcAgeMultiple() {
    if (metabolism == ARTILECT_METABOLISM) return 1 ;
    final float stage = agingStage() ;
    if (actor.species() != null) {  //Make this more precise.  Use Traits.
      return 0.5f + (stage * 0.25f) ;
    }
    if (stage == 0) return 0.70f ;
    if (stage == 1) return 1.00f ;
    if (stage == 2) return 0.85f ;
    if (stage == 3) return 0.65f ;
    return -1 ;
  }
  
  
  public float ageMultiple() {
    return ageMultiple ;
  }
  
  
  public float lifespan() {
    return lifespan ;
  }
  
  
  public float energyLevel() {
    return calories / maxHealth ;
  }
  
  
  public boolean organic() {
    return metabolism != ARTILECT_METABOLISM ;
  }
  
  
  
  /**  Methods related to sensing and motion-
    */
  public float baseSpeed() {
    float rate = baseSpeed * GameSettings.actorScale ;
    return rate * (float) FastMath.sqrt(ageMultiple) ;
  }
  
  
  public int sightRange() {
    float range = 0.5f + (actor.traits.useLevel(SURVEILLANCE) / 10f) ;
    range *= (actor.world().dayValue() + 1) / 2 ;
    return (int) (baseSight * (float) Math.sqrt(range * ageMultiple)) ;
  }
  
  
  
  /**  State modifications-
    */
  public void takeInjury(float taken) {
    injury += taken ;
    if (organic() && Rand.num() * maxHealth < taken) bleeds = true ;
    final float max ;
    if (! conscious()) max = maxHealth * (MAX_INJURY + 1) ;
    else {
      max = (maxHealth * MAX_INJURY) + 1 ;
      morale -= taken * 0.1f / max ;
    }
    injury = Visit.clamp(injury, 0, max) ;
  }
  
  
  public void liftInjury(float lifted) {
    injury -= lifted ;
    if (conscious()) morale += lifted * injuryLevel() / maxHealth ;
    if (Rand.num() > injuryLevel()) bleeds = false ;
    if (injury < 0) injury = 0 ;
  }
  
  
  public void takeFatigue(float taken) {
    fatigue += taken ;
    final float max = maxHealth * MAX_FATIGUE ;
    if (conscious()) morale -= taken * 0.5f / max ;
    //if (fatigue > max) fatigue = max ;
  }
  
  
  public void liftFatigue(float lifted) {
    fatigue -= lifted ;
    if (fatigue < 0) fatigue = 0 ;
  }
  
  
  public void adjustMorale(float adjust) {
    morale = Visit.clamp(morale + adjust, MIN_MORALE, MAX_MORALE) ;
  }
  
  
  public void adjustPsy(float adjust) {
    psyPoints += adjust ;
  }
  
  
  public void setState(int state) {
    final int oldState = this.state ;
    this.state = state ;
    if (state != oldState && state != STATE_ACTIVE) {
      actor.enterStateKO(Action.FALL) ;
    }
  }
  
  
  
  
  /**  State queries-
    */
  public float hungerLevel() {
    return (maxHealth - calories) / maxHealth ;
  }
  
  
  public float injuryLevel() {
    return injury / (maxHealth * MAX_INJURY) ;
  }
  
  
  public float fatigueLevel() {
    return fatigue / (maxHealth * MAX_FATIGUE) ;
  }
  
  
  public float moraleLevel() {
    return Visit.clamp(morale + 0.5f, 0, 1) ;
  }
  
  
  public boolean bleeding() {
    return bleeds && alive() ;
  }
  
  
  public boolean conscious() {
    return state == STATE_ACTIVE ;
  }
  
  
  public boolean asleep() {
    return (state == STATE_RESTING) && (injuryLevel() < REVIVE_THRESHOLD) ;
  }
  
  
  public boolean suspended() {
    return state == STATE_SUSPEND ;
  }
  
  
  public boolean alive() {
    return state <= STATE_RESTING ;
  }
  
  
  public boolean dying() {
    return state >= STATE_DYING ;
  }
  
  
  public boolean isDead() {
    return state == STATE_DECOMP ;
  }
  
  
  public boolean isState(int state) {
    return this.state == state ;
  }
  
  
  public boolean goodHealth() {
    return conscious() || (asleep() && actor.traits.effectBonus(IMMUNE) > -5) ;
  }
  
  
  public float stressPenalty() {
    if (stressCache != -1) return stressCache ;
    if (! organic()) return stressCache = 0 ;
    
    float sumDisease = 0 ;
    for (Trait t : Qualities.TREATABLE_CONDITIONS) {
      sumDisease += ((Condition) t).virulence * actor.traits.traitLevel(t) ;
    }
    float sum = Visit.clamp((fatigue + injury) / maxHealth, 0, 1) ;
    final float hunger = (1 - (calories / maxHealth)) + (1 - nutrition) ;
    if (hunger > 0.5f) sum += hunger - 0.5f ;
    if (bleeds) sum += 0.25f ;
    sum += sumDisease / 100f ;
    sum -= moraleLevel() + 0.25f ;
    
    if (sum < 0) return stressCache = 0 ;
    return stressCache = Visit.clamp(sum * sum, 0, 0.5f) ;
  }
  
  
  public float maxHealth() {
    return maxHealth ;
  }
  
  
  public float maxPsy() {
    final float
      psyLevel = Math.abs(actor.traits.traitLevel(PSYONIC)),
      maxPsy = (float) Math.sqrt(psyLevel) * MAX_PSY_MULTIPLE ;
    return maxPsy == 0 ? 0 : (maxPsy + (MAX_PSY_MULTIPLE / 2)) ;
  }
  
  
  public float psyPoints() {
    return psyPoints ;
  }
  
  
  
  /**  Updates and internal state changes-
    */
  void updateHealth(int numUpdates) {
    //
    //  Define primary attributes-
    ageMultiple = calcAgeMultiple() ;
    maxHealth = baseBulk * ageMultiple * (DEFAULT_HEALTH +
      (actor.traits.traitLevel(IMMUNE) / 3f) +
      (actor.traits.traitLevel(MUSCULAR ) / 3f)
    ) ;
    if (numUpdates < 0) return ;
    //
    //  Deal with injury, fatigue and stress.
    stressCache = -1 ;
    final int oldState = state ;
    checkStateChange() ;
    updateStresses() ;
    advanceAge(numUpdates) ;
    //
    //  Check for disease or sudden death due to senescence.
    if (oldState != state && state != STATE_ACTIVE) {
      if (state < STATE_DYING && ! organic()) state = STATE_DYING ;
      I.say(actor+" has entered a non-active state: "+stateDesc()) ;
      actor.enterStateKO(Action.FALL) ;
    }
    if (state <= STATE_RESTING && metabolism == HUMAN_METABOLISM) {
      Condition.checkContagion(actor) ;
    }
  }
  
  
  private void checkStateChange() {
    if (verbose && I.talkAbout == actor) {
      I.say("Injury/fatigue:"+injury+"/"+fatigue+", max: "+maxHealth) ;
      I.say("STATE IS: "+state) ;
    }
    //
    //  Check for state effects-
    if (state == STATE_SUSPEND) return ;
    if (state == STATE_DYING) {
      injury += maxHealth * 1f / DECOMPOSE_TIME ;
      if (injury > maxHealth * (MAX_INJURY + 1)) {
        state = STATE_DECOMP ;
      }
      return ;
    }
    if (fatigue + injury >= maxHealth) {
      state = STATE_RESTING ;
    }
    if (actor.traits.useLevel(IMMUNE) < -5) {
      I.say(actor+" has died of disease.") ;
      I.say("Effective vigour: "+actor.traits.useLevel(IMMUNE)) ;
      I.say("Maximum vigour: "+actor.traits.traitLevel(IMMUNE)) ;
      I.say("Conditions: ") ; for (Trait t : CONDITIONS) {
        final float level = actor.traits.useLevel(t) ;
        if (level <= 0) continue ;
        I.add(t.toString()+": "+level+", ") ;
      }
      state = STATE_DYING ;
    }
    if (injury >= maxHealth * MAX_INJURY) {
      I.say(actor+" has died of injury.") ;
      state = STATE_DYING ;
    }
    if (fatigue <= 0 && asleep()) {
      if (verbose) I.sayAbout(actor, actor+" has revived!") ;
      state = STATE_ACTIVE ;
    }
    //
    //  Deplete your current calorie reserve-
    if (! organic()) return ;
    calories -= (1f * maxHealth * baseSpeed) / STARVE_INTERVAL ;
    calories = Visit.clamp(calories, 0, maxCalories()) ;
    if (calories <= 0) {
      I.say(actor+" has died from lack of energy.") ;
      state = STATE_DYING ;
    }
  }
  
  
  private void updateStresses() {
    if (state >= STATE_SUSPEND || ! organic()) return ;
    final float DL = World.STANDARD_DAY_LENGTH ;
    float MM = 1, FM = 1, IM = 1, PM = 1 ;
    //
    //  Regeneration rates differ during sleep-
    if (state == STATE_RESTING) {
      FM = -3 ;
      IM =  2 ;
      MM =  1 ;
      PM =  0 ;
    }
    else if (actor.currentAction() != null) {
      FM *= Action.moveRate(actor, true) ;
    }
    if (bleeds) {
      injury++ ;
      if (actor.traits.test(IMMUNE, 10, 1) && Rand.num() < STABILISE_CHANCE) {
        bleeds = false ;
      }
    }
    else injury -= INJURY_REGEN_PER_DAY * maxHealth * IM / DL ;
    fatigue += FATIGUE_GROW_PER_DAY * baseSpeed * maxHealth * FM / DL ;
    fatigue = Visit.clamp(fatigue, 0, MAX_FATIGUE * maxHealth) ;
    injury = Visit.clamp(injury, 0, (MAX_INJURY + 1) * maxHealth) ;
    //
    //  Have morale converge to a default based on the cheerful trait and
    //  current stress levels.
    final float
      stress = stressPenalty(),
      defaultMorale = actor.traits.traitLevel(POSITIVE) / 10f,
      moraleInc = MORALE_DECAY_PER_DAY * MM / DL ;
    morale = (morale * (1 - moraleInc)) + (defaultMorale * moraleInc) ;
    morale -= stress / DL ;
    //
    //  Last but not least, update your psy points-
    final float maxPsy = maxPsy() ;
    psyPoints += maxPsy * (1 - stress) * PM / PSY_REGEN_TIME ;
    psyPoints = Visit.clamp(psyPoints, 0, maxPsy) ;
  }
  
  
  private void advanceAge(int numUpdates) {
    if (! organic()) return ;
    
    final int DL = World.STANDARD_DAY_LENGTH ;
    float ageInc = 0 ; 
    if ((numUpdates + 1) % DL == 0) {
      ageInc += DL * 1f / World.STANDARD_YEAR_LENGTH ;
    }
    
    if (metabolism == ANIMAL_METABOLISM) {
      float growBonus = energyLevel() * 1f / (AGE_MAX * DL) ;
      final int AS = agingStage() ;
      if      (AS == AGE_JUVENILE) growBonus *= 1.00f ;
      else if (AS == AGE_YOUNG   ) growBonus *= 0.20f ;
      else if (AS == AGE_MATURE  ) growBonus *= 0.04f ;
      else                         growBonus *= 0.00f ;
      ageInc += growBonus ;
      if (verbose) I.sayAbout(actor, "  ___LIFESPAN: "+lifespan) ;
      if (verbose) I.sayAbout(actor, "  ___AGE INCREMENT: "+ageInc) ;
    }
    currentAge += ageInc ;
    
    if (currentAge > lifespan * (1 + (lifeExtend / 10))) {
      float deathDC = ROUTINE_DC * (1 + lifeExtend) ;
      if (actor.traits.test(IMMUNE, deathDC, 0)) {
        lifeExtend++ ;
      }
      else {
        I.say(actor+" has died of old age.") ;
        state = STATE_DYING ;
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String stateDesc() {
    if ((state == STATE_RESTING || state == STATE_ACTIVE) && bleeds) {
      return "Bleeding" ;
    }
    if (state == STATE_RESTING && ! asleep()) {
      return "Stable" ;
    }
    return STATE_DESC[state] ;
  }
  
  
  public String hungerDesc() {
    return descFor(HUNGER, 1 - (calories / maxHealth), -1) ;
  }
  
  
  public String nourishDesc() {
    return descFor(MALNOURISHMENT, 1 - nutrition, -1) ;
  }
  
  
  public String injuryDesc() {
    return descFor(INJURY, injury * 1f / maxHealth, maxHealth) ;
  }
  
  
  public String fatigueDesc() {
    return descFor(FATIGUE, fatigueLevel(), maxHealth) ;
  }
  
  
  public String moraleDesc() {
    return descFor(POOR_MORALE, morale * -2, -1) ;
  }
  
  
  private String descFor(Trait trait, float level, float max) {
    final String desc = Trait.descriptionFor(trait, level * trait.maxVal) ;
    if (desc == null) return null ;
    if (max <= 0) return desc ;
    return desc+" ("+(int) (level * max)+"/"+(int) max+")" ;
  }
  
  
  public Batch <String> conditionsDesc() {
    final Batch <String> allDesc = new Batch <String> () {
      public void add(String s) { if (s != null) super.add(s) ; }
    } ;
    allDesc.add(hungerDesc() ) ;
    allDesc.add(nourishDesc()) ;
    allDesc.add(injuryDesc() ) ;
    allDesc.add(fatigueDesc()) ;
    allDesc.add(moraleDesc() ) ;
    return allDesc ;
  }
}





