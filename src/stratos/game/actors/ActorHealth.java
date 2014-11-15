/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;

import static stratos.game.actors.Conditions.*;
import org.apache.commons.math3.util.FastMath;



//  TODO:  This could probably use a thorough cleanup for organisation/clarity.

public class ActorHealth implements Qualities {
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean verbose = false;
  
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
    STATE_DECOMP   = 4;
  final static String STATE_DESC[] = {
    "Active",
    "Asleep",
    "In Suspended Animation",
    "Dead",
    "Decomposed",
  };
  final public static int
    AGE_JUVENILE = 0,
    AGE_YOUNG    = 1,
    AGE_MATURE   = 2,
    AGE_SENIOR   = 3,
    AGE_MAX      = 4;
  final static String AGING_DESC[] = {
    "Juvenile",
    "Young",
    "Mature",
    "Senior"
  };
  
  final public static float
    DEFAULT_PRIME    = 25,
    DEFAULT_LIFESPAN = 60,
    LIFE_EXTENDS     = 0.1f,
    
    DEFAULT_BULK  = 1.0f,
    DEFAULT_SPEED = 1.0f,
    DEFAULT_SIGHT = 8.0f,

    DEFAULT_HEALTH  = 10,
    MAX_CALORIES    = 1.5f,
    STARVE_INTERVAL = Stage.STANDARD_DAY_LENGTH * 5,
    
    MAX_INJURY       =  1.5f,
    MAX_DECOMP       =  2.5f,
    MAX_FATIGUE      =  1.0f,
    MAX_MORALE       =  0.5f,
    MIN_MORALE       = -1.5f,
    REVIVE_THRESHOLD =  0.5f,
    RUN_FATIGUE_MULT = 10.0f,
    BLEED_OUT_TIME   =  Stage.STANDARD_HOUR_LENGTH * 2,
    DECOMPOSE_TIME   =  Stage.STANDARD_DAY_LENGTH,
    
    FATIGUE_GROW_PER_DAY = 0.33f,
    MORALE_DECAY_PER_DAY = 0.33f,
    INJURY_REGEN_PER_DAY = 0.33f,
    
    DEFAULT_CONCENTRATION  = 10,
    CONCENTRATE_REGEN_TIME = Stage.STANDARD_HOUR_LENGTH;
  
  
  final Actor actor;
  
  private float
    baseBulk  = DEFAULT_BULK,
    baseSpeed = DEFAULT_SPEED,
    baseSight = DEFAULT_SIGHT;
  
  private float
    lifespan    = DEFAULT_LIFESPAN,
    lifeExtend  = 0;
  private int
    metabolism = HUMAN_METABOLISM;
  
  private float
    currentAge = 0,
    maxHealth  = DEFAULT_HEALTH,
    calories   = DEFAULT_HEALTH / 2,
    nutrition  = 0.5f,
    injury     = 0,
    fatigue    = 0;
  private boolean
    bleeds = false;
  private float
    morale        = 0,
    concentration = 0;
  //  TODO:  Action limit and need for sleep.
  
  private int
    state    = STATE_ACTIVE;
  private float
    ageMultiple = 1.0f;
  
  //
  //  I don't save/load these, since they refresh frequently anyway
  private float stressCache = -1;
  
  
  
  ActorHealth(Actor actor) {
    this.actor = actor;
  }
  
  
  void loadState(Session s) throws Exception {
    baseBulk  = s.loadFloat();
    baseSpeed = s.loadFloat();
    baseSight = s.loadFloat();
    
    lifespan    = s.loadFloat();
    currentAge  = s.loadFloat();
    lifeExtend  = s.loadFloat();
    ageMultiple = s.loadFloat();
    metabolism  = s.loadInt()  ;
    
    maxHealth = s.loadFloat();
    calories  = s.loadFloat();
    nutrition = s.loadFloat();
    injury    = s.loadFloat();
    fatigue   = s.loadFloat();
    bleeds    = s.loadBool ();
    morale    = s.loadFloat();
    
    concentration = s.loadFloat();
    state = s.loadInt();
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveFloat(baseBulk );
    s.saveFloat(baseSpeed);
    s.saveFloat(baseSight);
    
    s.saveFloat(lifespan   );
    s.saveFloat(currentAge );
    s.saveFloat(lifeExtend );
    s.saveFloat(ageMultiple);
    s.saveInt  (metabolism );
    
    s.saveFloat(maxHealth);
    s.saveFloat(calories );
    s.saveFloat(nutrition);
    s.saveFloat(injury   );
    s.saveFloat(fatigue  );
    s.saveBool (bleeds   );
    s.saveFloat(morale   );
    
    s.saveFloat(concentration);
    s.saveInt(state);
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
    this.lifespan = lifespan;
    this.baseBulk  = baseBulk  * DEFAULT_BULK ;
    this.baseSight = baseSight * DEFAULT_SIGHT;
    this.baseSpeed = baseSpeed * DEFAULT_SPEED;
    this.metabolism = metabolicType;
  }
  
  
  public void setupHealth(
    float agingFactor,
    float overallHealth,
    float accidentChance
  ) {
    this.currentAge = lifespan * agingFactor;
    updateHealth(-1);
    if (metabolism != ARTILECT_METABOLISM) {
      calories = Visit.clamp(Rand.num() + (overallHealth / 2), 0, 1);
      calories *= maxHealth;
      nutrition = Visit.clamp(Rand.num() + overallHealth, 0, 1);
    }
    else {
      nutrition = 1;
      calories = maxHealth;
    }
    
    fatigue = Rand.num() * (1 - (calories / maxHealth)) * maxHealth / 2f;
    injury = Rand.num() * accidentChance * maxHealth / 2f;
    morale = (Rand.num() - (0.5f + accidentChance)) / 2f;
  }
  
  
  public void setInjuryLevel(float level) {
    injury = level * maxHealth * MAX_INJURY;
  }
  
  
  public void setFatigueLevel(float level) {
    fatigue = level * maxHealth * MAX_FATIGUE;
  }
  
  
  public void setMoraleLevel(float level) {
    morale = level;
  }
  
  
  public void setBleeding(boolean is) {
    bleeds = is;
  }
  
  
  
  /**  Methods related to growth, reproduction, aging and death.
    */
  public float maxCalories() {
    return 1 + (maxHealth * MAX_CALORIES);
  }
  
  
  public void takeCalories(float amount, float quality) {
    amount = Visit.clamp(amount, 0, maxCalories() - calories);
    final float oldQual = nutrition * calories;
    calories += amount;
    nutrition = (oldQual + (quality * amount)) / calories;
  }
  
  
  public void loseSustenance(float fraction) {
    calories -= fraction * maxHealth;
  }
  
  
  public void setMaturity(float ageLevel) {
    currentAge = lifespan * ageLevel;
  }
  
  
  public int     agingStage () { return Visit.clamp((int) (ageLevel() * 4), 4); }
  public float   ageLevel   () { return currentAge * 1f / lifespan; }
  public boolean juvenile   () { return agingStage() <= AGE_JUVENILE; }
  public int     exactAge   () { return (int) currentAge; }
  public String  agingDesc  () { return AGING_DESC[agingStage()]; }
  public float   ageMultiple() { return ageMultiple; }
  public float   lifespan   () { return lifespan; }
  
  public float caloryLevel() { return calories / maxHealth; }
  
  public boolean organic()  { return metabolism != ARTILECT_METABOLISM; }
  public boolean animal()   { return metabolism == ANIMAL_METABOLISM ; }
  public boolean artilect() { return metabolism == ARTILECT_METABOLISM; }
  public boolean human()    { return metabolism == HUMAN_METABOLISM  ; }
  
  
  
  /**  Methods related to sensing and motion-
    */
  public float baseBulk() {
    return baseBulk;
  }
  
  
  public float baseSpeed() {
    float rate = baseSpeed * GameSettings.actorScale;
    return rate * (float) FastMath.sqrt(ageMultiple);
  }
  
  
  public float sightRange() {
    float range = 0.5f + (actor.traits.usedLevel(SURVEILLANCE) / 10f);
    range *= (actor.world().dayValue() + 1) / 2;  //  TODO:  Take this out?
    range *= GameSettings.actorScale;
    return baseSight * (float) FastMath.sqrt(range * ageMultiple);
  }
  
  
  
  /**  State modifications-
    */
  public void takeInjury(float taken, boolean terminal) {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) {
      I.say("\n"+actor+" has been injured.");
      I.say("  Prior injury: "+injury+", taken: "+taken);
    }
    
    final float limit;
    if (terminal) limit = maxHealth * MAX_INJURY;
    else if (conscious()) limit = maxHealth;
    else limit = injury;
    
    if (organic()) {
      if ((Rand.num() * maxHealth / 2f) < taken) bleeds = true;
    }
    injury = Visit.clamp(injury + taken, 0, limit + 1);
    
    if (report) {
      I.say("  Injury capped at: "+limit);
      I.say("  Final injury:     "+injury+", bleeding? "+bleeds);
    }
  }
  
  
  public void liftInjury(float lifted) {
    injury -= lifted;
    if (Rand.num() > injuryLevel()) bleeds = false;
    if (injury < 0) injury = 0;
  }
  
  
  public void takeFatigue(float taken) {
    final float max = maxHealth * MAX_FATIGUE;
    fatigue = Visit.clamp(fatigue + taken, 0, max);
  }
  
  
  public void liftFatigue(float lifted) {
    fatigue -= lifted;
    if (fatigue < 0) fatigue = 0;
  }
  
  
  public void adjustMorale(float adjust) {
    morale = Visit.clamp(morale + adjust, MIN_MORALE, MAX_MORALE);
  }
  
  
  public void takeConcentration(float taken) {
    if (taken <= 0) return;
    concentration -= taken;
  }
  
  
  public void gainConcentration(float gains) {
    if (gains <= 0) return;
    concentration += gains;
  }
  
  
  public void setState(int state) {
    final int oldState = this.state;
    this.state = state;
    if (state != oldState && state != STATE_ACTIVE) {
      actor.enterStateKO(Action.FALL);
    }
  }
  
  
  
  
  /**  State queries-
    */
  public float hungerLevel() {
    return (maxHealth - calories) / maxHealth;
  }
  
  
  public float injuryLevel() {
    return injury / (maxHealth * MAX_INJURY);
  }
  
  
  public float fatigueLevel() {
    return fatigue / (maxHealth * MAX_FATIGUE);
  }
  
  
  public float moraleLevel() {
    return Visit.clamp(morale + 0.5f, 0, 1);
  }
  
  
  public boolean bleeding() {
    return bleeds && alive();
  }
  
  
  public boolean conscious() {
    return state == STATE_ACTIVE;
  }
  
  
  public boolean asleep() {
    return (state == STATE_RESTING) && (injuryLevel() < REVIVE_THRESHOLD);
  }
  
  
  public boolean suspended() {
    return state == STATE_SUSPEND;
  }
  
  
  public boolean alive() {
    return state <= STATE_RESTING;
  }
  
  
  public boolean dying() {
    return state >= STATE_DYING;
  }
  
  
  public boolean isDead() {
    return state == STATE_DECOMP;
  }
  
  
  public boolean isState(int state) {
    return this.state == state;
  }
  
  
  public boolean goodHealth() {
    return conscious() || (asleep() && actor.traits.effectBonus(IMMUNE) > -5);
  }
  
  
  public float maxHealth() {
    return maxHealth;
  }
  
  
  public float fatigueLimit() {
    return maxHealth - fatigue;
  }
  
  
  public float maxConcentration() {
    final float willLevel = actor.traits.usedLevel(NERVE) / 10f;
    return DEFAULT_CONCENTRATION * (1 + willLevel) / 2f;
  }
  
  
  public float concentration() {
    return concentration;
  }
  
  
  
  /**  Updates and internal state changes-
    */
  final static float AGE_STAGE_MULTS[] = { 0.2f, 0.7f, 1.0f, 0.85f, 0.65f };
  
  
  private float calcAgeMultiple() {
    if (metabolism == ARTILECT_METABOLISM) return 1;
    final float age = ageLevel();
    if (metabolism == ANIMAL_METABOLISM) {
      return 0.5f + age;
    }
    else {
      final int stage = (int) (age * 4);
      final float
        a  = (age * 4) % 1,
        m1 = AGE_STAGE_MULTS[stage],
        m2 = AGE_STAGE_MULTS[stage + 1];
      return (m2 * a) + (m1 * (1 - a));
    }
  }
  
  
  public float stressPenalty() {
    if (stressCache != -1) return stressCache;
    if (! organic()) return stressCache = 0;
    
    float disease = 0;
    for (Trait t : TREATABLE_CONDITIONS) {
      disease += ((Condition) t).virulence * actor.traits.traitLevel(t) / 100f;
    }
    
    float sum = 0;
    sum += injuryLevel();
    sum += fatigueLevel();
    sum += hungerLevel();
    
    sum -= (bleeds ? 0 : 0.25f) - disease;
    sum -= Visit.clamp(moraleLevel(), -0.5f, 0.5f);
    
    if (sum > 0) sum -= actor.skills.test(NERVE, null, null, sum * 10, 1, 0);
    
    return stressCache = Visit.clamp(sum, 0, 1);
  }
  
  
  protected void updateHealth(int numUpdates) {
    
    //  Define primary attributes-
    ageMultiple = calcAgeMultiple();
    if (metabolism == HUMAN_METABOLISM) {
      final float
        muscle   = actor.traits.traitLevel(MUSCULAR)     / 10f,
        fitness  = actor.traits.traitLevel(ATHLETICS)    / 20f,
        hardness = actor.traits.traitLevel(HAND_TO_HAND) / 20f;
      maxHealth = (1 + fitness + hardness + (muscle * muscle)) / 1.5f;
      maxHealth *= DEFAULT_HEALTH;
    }
    else maxHealth = DEFAULT_HEALTH;
    maxHealth *= baseBulk * ageMultiple;
    if (numUpdates < 0) return;
    
    //  Deal with injury, fatigue and stress.
    stressCache = -1;
    final int oldState = state;
    checkStateChange();
    updateStresses();
    advanceAge(numUpdates);
    
    //  Check for disease or sudden death due to senescence.
    if (oldState != state && state != STATE_ACTIVE) {
      if (state < STATE_DYING && ! organic()) state = STATE_DYING;
      I.say(actor+" has entered a non-active state: "+stateDesc());
      actor.enterStateKO(Action.FALL);
    }
    if (state <= STATE_RESTING && metabolism == HUMAN_METABOLISM) {
      Condition.checkContagion(actor);
    }
  }
  
  
  private void checkStateChange() {
    if (verbose && I.talkAbout == actor) {
      I.say("\nUpdating health state for "+actor);
      I.say("  Injury/fatigue: "+injury+"/"+fatigue+", max: "+maxHealth);
      I.say("  State is: "+STATE_DESC[state]);
    }
    //
    //  Check for state effects-
    if (state == STATE_SUSPEND) return;
    if (state == STATE_DYING) {
      injury += maxHealth * 1f / DECOMPOSE_TIME;
      if (injury > maxHealth * (MAX_INJURY + 1)) {
        state = STATE_DECOMP;
      }
      return;
    }
    if (fatigue + injury >= maxHealth) {
      state = STATE_RESTING;
    }
    if (actor.traits.usedLevel(IMMUNE) < -5) {
      I.say(actor+" has died of disease.");
      I.say("Effective vigour: "+actor.traits.usedLevel(IMMUNE));
      I.say("Maximum vigour: "+actor.traits.traitLevel(IMMUNE));
      I.say("Conditions: "); for (Trait t : CONDITIONS) {
        final float level = actor.traits.usedLevel(t);
        if (level <= 0) continue;
        I.add(t.toString()+": "+level+", ");
      }
      state = STATE_DYING;
    }
    if (injury >= maxHealth * MAX_INJURY) {
      I.say(actor+" has died of injury.");
      state = STATE_DYING;
    }
    if (fatigue <= 0 && asleep()) {
      if (verbose) I.say(actor+" has revived!");
      state = STATE_ACTIVE;
    }
    //
    //  Deplete your current calorie reserve-
    if (! organic()) return;
    calories -= (1f * maxHealth * baseSpeed) / STARVE_INTERVAL;
    calories = Visit.clamp(calories, 0, maxCalories());
    if (calories <= 0) {
      I.say(actor+" has died from starvation.");
      state = STATE_DYING;
    }
  }
  
  
  private void updateStresses() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nUpdating stresses for "+actor);
    //
    //  Inorganic targets get a different selection of perks and drawbacks-
    if (state >= STATE_SUSPEND || ! organic()) {
      bleeds = false;
      morale = 0;
      float fatigueRegen = maxHealth * FATIGUE_GROW_PER_DAY / DEFAULT_HEALTH;
      fatigue = Visit.clamp(fatigue - fatigueRegen, 0, maxHealth);
      return;
    }
    //
    //  Regeneration rates differ during sleep-
    final float DL = Stage.STANDARD_DAY_LENGTH;
    float MM = 1, FM = 1, IM = 1, PM = 1;
    final float regen = actor.skills.chance(IMMUNE, 10);
    if (state == STATE_RESTING) {
      FM = -3;
      IM =  2;
      MM =  1;
      PM =  0;
    }
    else if (actor.currentAction() != null) {
      final float mult = Action.moveRate(actor, true) / baseSpeed();
      if (mult > 1) FM += (mult - 1) * RUN_FATIGUE_MULT;
    }
    if (report) {
      I.say("  Fatigue multiple: "+FM);
      I.say("  Injury  multiple: "+IM);
      I.say("  Morale  multiple: "+MM);
      I.say("  Psych   multiple: "+PM);
    }
    
    if (bleeds) {
      final float
        bleedMargin  = MAX_INJURY - 1f,
        bleedAmount  = maxHealth * bleedMargin / BLEED_OUT_TIME,
        stableChance = regen * 2 * 1f          / BLEED_OUT_TIME;
      if (Rand.num() < stableChance) bleeds = false;
      else injury += bleedAmount;
      if (report) {
        I.say("  bleedout time:    "+BLEED_OUT_TIME);
        I.say("  bleeding for:     "+bleedAmount   );
        I.say("  stabilise chance: "+stableChance  );
      }
    }
    else if (injury > 0) {
      actor.skills.test(IMMUNE, 10, 1f / Stage.STANDARD_HOUR_LENGTH);
      injury -= INJURY_REGEN_PER_DAY * maxHealth * regen * IM / DL;
    }
    
    fatigue += FATIGUE_GROW_PER_DAY * baseSpeed * maxHealth * FM / DL;
    fatigue = Visit.clamp(fatigue, 0, MAX_FATIGUE * maxHealth);
    injury  = Visit.clamp(injury , 0, MAX_DECOMP  * maxHealth);
    //
    //  Have morale converge to a default based on the cheerful trait and
    //  current stress levels.
    final float
      stress        = stressPenalty(),
      defaultMorale = actor.traits.traitLevel(POSITIVE) / 10f,
      moraleInc     = MORALE_DECAY_PER_DAY * MM / DL;
    morale = (morale * (1 - moraleInc)) + (defaultMorale * moraleInc);
    morale -= stress / DL;
    //
    //  Last but not least, update your reserves of concentration-
    final float maxCon = maxConcentration();
    concentration += maxCon * (1 - stress) * PM / CONCENTRATE_REGEN_TIME;
    concentration = Visit.clamp(concentration, 0, maxCon);
  }
  
  
  private void advanceAge(int numUpdates) {
    if (! organic()) return;
    final boolean report = verbose && I.talkAbout == actor;
    
    final int DL = Stage.STANDARD_DAY_LENGTH;
    float ageInc = 0; 
    if ((numUpdates + 1) % DL == 0) {
      ageInc += DL * 1f / Stage.STANDARD_YEAR_LENGTH;
    }
    
    if (metabolism == ANIMAL_METABOLISM) {
      float growBonus = caloryLevel() * 1f / (AGE_MAX * DL);
      final int AS = agingStage();
      if      (AS == AGE_JUVENILE) growBonus *= 1.00f;
      else if (AS == AGE_YOUNG   ) growBonus *= 0.20f;
      else if (AS == AGE_MATURE  ) growBonus *= 0.04f;
      else                         growBonus *= 0.00f;
      ageInc += growBonus;
      if (report) I.say("  ___LIFESPAN: "+lifespan);
      if (report) I.say("  ___AGE INCREMENT: "+ageInc);
    }
    currentAge += ageInc;
    
    if (currentAge > lifespan * (1 + (lifeExtend / 10))) {
      float deathDC = ROUTINE_DC * (1 + lifeExtend);
      if (actor.skills.test(IMMUNE, deathDC, 0)) {
        lifeExtend++;
      }
      else {
        I.say(actor+" has died of old age.");
        state = STATE_DYING;
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String stateDesc() {
    if ((state == STATE_RESTING || state == STATE_ACTIVE) && bleeds) {
      return "Bleeding";
    }
    if (state == STATE_RESTING && ! asleep()) {
      return "Stable";
    }
    return STATE_DESC[state];
  }
  
  
  public String hungerDesc() {
    return descFor(HUNGER, 1 - (calories / maxHealth), -1);
  }
  
  
  public String nourishDesc() {
    return descFor(MALNOURISHMENT, 1 - nutrition, -1);
  }
  
  
  public String injuryDesc() {
    return descFor(INJURY, injury * 1f / maxHealth, maxHealth);
  }
  
  
  public String fatigueDesc() {
    return descFor(FATIGUE, fatigueLevel(), maxHealth);
  }
  
  
  public String moraleDesc() {
    return descFor(POOR_MORALE, 0 - morale / MAX_MORALE, -1);
  }
  
  
  private String descFor(Trait trait, float level, float max) {
    final String desc = Trait.descriptionFor(trait, level);
    if (desc == null) return null;
    if (max <= 0) return desc;
    return desc+" ("+(int) (level * max)+")";
  }
  
  
  public Batch <String> conditionsDesc() {
    final Batch <String> allDesc = new Batch <String> () {
      public void add(String s) { if (s != null) super.add(s); }
    };
    allDesc.add(hungerDesc() );
    allDesc.add(nourishDesc());
    allDesc.add(injuryDesc() );
    allDesc.add(fatigueDesc());
    allDesc.add(moraleDesc() );
    return allDesc;
  }
}





