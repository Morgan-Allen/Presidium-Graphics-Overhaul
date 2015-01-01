/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.util.*;



public class Career implements Qualities {
  
  
  private static boolean
    verbose = false;
  
  final static int
    MIN_PERSONALITY           = 3,
    NUM_RANDOM_CAREER_SAMPLES = 3;
  
  
  private Actor subject;
  private Background gender;
  private Background vocation, birth, homeworld;
  private String fullName = null;
  
  
  public Career(
    Background vocation, Background birth,
    Background homeworld, Background gender
  ) {
    this.gender    = gender   ;
    this.vocation  = vocation ;
    this.birth     = birth    ;
    this.homeworld = homeworld;
  }
  
  
  public Career(Background root) {
    vocation = root;
  }
  
  
  public Career(Actor subject) {
    this.subject = subject;
  }
  
  
  public void loadState(Session s) throws Exception {
    subject   = (Actor) s.loadObject();
    gender    = (Background) s.loadObject();
    birth     = (Background) s.loadObject();
    homeworld = (Background) s.loadObject();
    vocation  = (Background) s.loadObject();
    fullName  = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(subject);
    s.saveObject(gender   );
    s.saveObject(birth    );
    s.saveObject(homeworld);
    s.saveObject(vocation );
    s.saveString(fullName);
  }
  
  
  public Background vocation() {
    return vocation;
  }
  
  
  public Background birth() {
    return birth;
  }
  
  
  public Background homeworld() {
    return homeworld;
  }
  
  
  public Background topBackground() {
    if (vocation  != null) return vocation ;
    if (homeworld != null) return homeworld;
    if (birth     != null) return birth    ;
    if (gender    != null) return gender   ;
    return null;
  }
  
  
  public String fullName() {
    return fullName;
  }
  
  
  
  /**  Fresh developments after recruitment...
    */
  public void recordVocation(Background b) {
    //
    //  TODO:  Add this to a list of vocations taken!
    vocation = b;
  }
  
  
  
  /**  Binds this career to a specific in-world actor and configures their
    *  physique, aptitudes and motivations:
    */
  public void applyCareer(Human actor, Base base) {
    if (verbose) I.say("\nGENERATING NEW CAREER");
    subject = actor;
    
    applyBackgrounds(actor, base);
    applySex(actor);
    setupAttributes(actor);
    fillPersonality(actor);
    
    //
    //  TODO:  Specify a few starter relationships here!  (And vary the base
    //  relation somewhat.)
    actor.relations.setRelation(base, 0.5f, 0);
    
    //  We top up basic attributes to match.
    actor.traits.initDNA(0);
    actor.health.setupHealth(
      Nums.clamp(Rand.avgNums(2), 0.26f, 0.94f),
      1, 0
    );
    
    //  For now, we apply gender at random, though this might be tweaked a bit
    //  later.  We also assign some random personality and/or physical traits.
    //
    //  Finally, specify name and (TODO:) a few other details of appearance.
    for (String name : Wording.namesFor(actor)) {
      if (fullName == null) fullName = name;
      else fullName+=" "+name;
    }
    ///I.say("Full name: "+fullName);
    //
    //  Along with current wealth and equipment-
    applyGear(vocation, actor);
    
    if (verbose) {
      I.say("  GENERATION COMPLETE: "+actor);
      I.say("  Personality:");
      for (Trait t : actor.traits.personality()) {
        final float level = actor.traits.traitLevel(t);
        I.say("    "+actor.traits.description(t)+" ("+level+")");
      }
    }
  }
  
  
  
  /**  Methods for generating an actor's background life-story:
    */
  private void applyBackgrounds(Human actor, Base base) {
    //
    //  If the target vocation is undetermined, we work forward at random from
    //  birth towards a final career stage:
    if (vocation == null) {
      pickBirthClass(actor, base);
      applyBackground(birth, actor);
      
      pickHomeworld(actor, base);
      applyBackground(homeworld, actor);
      applySystem((Sector) homeworld, actor);
      
      pickVocation(actor, base);
      applyBackground(vocation, actor);
    }
    //
    //  Alternatively, we work backwards from the target vocation to determine
    //  a probably system and social class of origin:
    else {
      applyBackground(vocation, actor);
      
      pickHomeworld(actor, base);
      applyBackground(homeworld, actor);
      applySystem((Sector) homeworld, actor);
      
      pickBirthClass(actor, base);
      applyBackground(birth, actor);
    }
  }
  
  
  private void pickBirthClass(Human actor, Base base) {
    if (birth != null) {
      return;
    }
    else if (base.isNative()) {
      birth = Backgrounds.NATIVE_BIRTH;
    }
    else {
      //  TODO:  What about noble birth?  What triggers that?
      final Batch <Float> weights = new Batch <Float> ();
      for (Background v : Backgrounds.OPEN_CLASSES) {
        weights.add(ratePromotion(v, actor, verbose));
      }
      birth = (Background) Rand.pickFrom(
        Backgrounds.OPEN_CLASSES, weights.toArray()
      );
    }
  }
  
  
  private void pickHomeworld(Human actor, Base base) {
    if (homeworld != null) {
      return;
    }
    else if (base.isNative()) {
      homeworld = base.world.offworld.worldSector();
    }
    else {
      //  TODO:  Include some weighting based off house relations!
      final Batch <Float> weights = new Batch <Float> ();
      for (Background v : Sectors.ALL_PLANETS) {
        weights.add(ratePromotion(v, actor, verbose));
      }
      homeworld = (Background) Rand.pickFrom(
        Sectors.ALL_PLANETS, weights.toArray()
      );
    }
  }
  
  
  private void pickVocation(Human actor, Base base) {
    final Pick <Background> pick = new Pick <Background> ();
    
    if (base.isNative()) {
      for (Background b : Backgrounds.NATIVE_CIRCLES) {
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num());
      }
    }
    else for (Background circle[] : base.commerce.homeworld().circles()) {
      final float weight = base.commerce.homeworld().weightFor(circle);
      for (Background b : circle) {
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num() * weight);
      }
      for (int n = NUM_RANDOM_CAREER_SAMPLES; n-- > 0;) {
        final Background b = (Background) Rand.pickFrom(
          Backgrounds.ALL_STANDARD_CIRCLES
        );
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num() / 2);
      }
    }
    this.vocation = pick.result();
  }
  
  
  private void applyBackground(Background v, Actor actor) {
    if (verbose) I.say("Applying vocation: "+v);
    
    for (Skill s : v.baseSkills.keySet()) {
      final int level = v.baseSkills.get(s);
      actor.traits.raiseLevel(s, level + (Rand.num() * 10) - 5);
    }
    
    for (Trait t : v.traitChances.keySet()) {
      float chance = v.traitChances.get(t);
      chance += Personality.traitChance(t, actor) / 2;
      actor.traits.incLevel(t, chance * Rand.avgNums(2) * 2);
      if (verbose) {
        I.say("  Chance for "+t+" is "+chance);
        final float level = actor.traits.traitLevel(t);
        I.say("  Level is now: "+level);
      }
    }
  }
  
  
  public static float ratePromotion(Background next, Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    return ratePromotion(next, actor, report);
  }
  
  
  private static float ratePromotion(
    Background next, Actor actor, boolean report
  ) {
    if (report) I.say("\nRating promotion to "+next+" for "+I.tagHash(actor));
    
    //  TODO:  Try to use arrays instead of tables here?  For efficiency?
    float rating = 1;
    
    //  Check for similar skills.
    if (next.baseSkills.size() > 0) {
      for (Skill s : next.baseSkills.keySet()) {
        final float skillRating = rateSimilarity(s, next, actor);
        if (skillRating == 0) continue;
        rating += skillRating;
        if (report) I.say("  Bonus due to "+s+" is "+skillRating);
      }
      final Batch <Skill> skills = actor.traits.skillSet();
      for (Skill s : skills) {
        final float skillRating = rateSimilarity(s, next, actor);
        if (skillRating == 0) continue;
        rating += skillRating;
        if (report) I.say("  Bonus due to "+s+" is "+skillRating);
      }
      rating *= 2f / (1 + next.baseSkills.size() + skills.size());
    }
    
    //  Check for similar traits. (Personality traits are handled a little
    //  differently, as they can have 'opposites', while others are measured
    //  directly.)
    if (next.traitChances.size() > 0) {
      float sumChances = 0;
      for (Trait t : next.traitChances.keySet()) {
        float chance = next.traitChances.get(t);
        if (t.type == Trait.PERSONALITY) {
          chance *= Personality.traitChance(t, actor);
        }
        else {
          chance *= actor.traits.traitLevel(t);
        }
        if (report) I.say("  Chance due to "+t+" is "+chance);
        sumChances += (chance + 1) / 2;
      }
      if (report) I.say("  Total trait chance: "+sumChances);
      rating *= sumChances * 2 / (1 + next.traitChances.size());
    }
    
    //  Finally, we also favour transition to more prestigious vocations:
    if (rating < 0) {
      if (report) I.say("  No chance of promotion- quitting.");
      return 0;
    }
    else if (actor instanceof Human) {
      final Background prior = ((Human) actor).career().topBackground();
      if (next.standing < prior.standing) return rating / 10f;
    }
    
    if (report) I.say("  Final rating: "+rating);
    return rating;
  }
  
  
  static float rateSimilarity(Skill s, Background a, Actor actor) {
    Integer aL = a.baseSkills.get(s), bL = (int) actor.traits.traitLevel(s);
    if (aL == null || bL == null) return 0;
    return (aL > bL) ? ((bL + 5f) / (aL + 5f)) : ((aL + 5f) / (bL + 5f));
  }
  
  
  public static boolean qualifies(Actor a, Background b) {
    for (Skill s : b.baseSkills.keySet()) {
      final int level = b.baseSkills.get(s);
      if (a.traits.traitLevel(s) < level - 5) return false;
    }
    return true;
  }
  
  
  private void fillPersonality(Actor actor) {
    while (true) {
      final int numP = actor.traits.personality().size();
      if (numP >= MIN_PERSONALITY) break;
      final Trait t = (Trait) Rand.pickFrom(PERSONALITY_TRAITS);
      float chance = (Personality.traitChance(t, actor) / 2) + Rand.num();
      if (chance < 0 && chance > -0.5f) chance = -0.5f;
      if (chance > 0 && chance <  0.5f) chance =  0.5f;
      actor.traits.incLevel(t, chance);
    }
    
    actor.traits.incLevel(HANDSOME, Rand.rangeAvg(-2, 2, 2));
    actor.traits.incLevel(TALL    , Rand.rangeAvg(-2, 2, 2));
    actor.traits.incLevel(STOUT   , Rand.rangeAvg(-2, 2, 2));
  }
  
  
  
  /**  Methods for customising fundamental attributes, rather than life
    *  experience-
    */
  final static float
    STRAIGHT_CHANCE = 0.85f,
    BISEXUAL_CHANCE = 0.55f,
    
    RACE_CLIMATE_CHANCE  = 0.65f,
    HALF_CLIMATE_CHANCE  = 0.35f;
  
  
  private void setupAttributes(Actor actor) {
    float minPhys = 0, minSens = 0, minCogn = 0;
    for (Skill s : actor.traits.skillSet()) {
      final float level = actor.traits.traitLevel(s);
      actor.traits.raiseLevel(s.parent, level - Rand.index(5));
      if (s.form == FORM_COGNITIVE) minCogn = Nums.max(level, minCogn + 1);
      if (s.form == FORM_SENSITIVE) minSens = Nums.max(level, minSens + 1);
      if (s.form == FORM_PHYSICAL ) minPhys = Nums.max(level, minPhys + 1);
    }
    actor.traits.raiseLevel(MUSCULAR , (minPhys + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(IMMUNE   , (minPhys + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(MOTOR    , (minSens + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(PERCEPT  , (minSens + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(COGNITION, (minCogn + Rand.rollDice(3, 7)) / 2f);
    actor.traits.raiseLevel(NERVE    , (minCogn + Rand.rollDice(3, 7)) / 2f);
  }
  
  
  private void applySex(Human actor) {
    
    if (gender == null) {
      final float
        rateM = ratePromotion(Backgrounds.MALE_BIRTH  , actor, verbose),
        rateF = ratePromotion(Backgrounds.FEMALE_BIRTH, actor, verbose);
      if (rateM * Rand.avgNums(2) > rateF * Rand.avgNums(2)) {
        gender = Backgrounds.MALE_BIRTH;
      }
      else gender = Backgrounds.FEMALE_BIRTH;
    }
    applyBackground(gender, actor);
    
    //  TODO:  Do some of these traits need to be rendered 'dormant' in younger
    //  citizens?
    
    float ST = Nums.clamp(Rand.rangeAvg(-1, 3, 2), 0, 3);
    if (Rand.index(20) == 0) ST = -1;
    if (gender == Backgrounds.FEMALE_BIRTH) {
      actor.traits.setLevel(GENDER_FEMALE, 1);
      actor.traits.setLevel(FEMININE, ST);
    }
    else {
      actor.traits.setLevel(GENDER_MALE, 1);
      actor.traits.setLevel(FEMININE, 0 - ST);
    }
    actor.traits.setLevel(
      ORIENTATION,
       Rand.num() < STRAIGHT_CHANCE ? "Heterosexual" :
      (Rand.num() < BISEXUAL_CHANCE ? "Bisexual" : "Homosexual")
    );
  }
  
  
  //
  //  TODO:  Try incorporating these trait-FX into the rankings first.
  private void applySystem(Sector world, Actor actor) {
    //
    //  Assign skin texture (race) based on prevailing climate.  (Climate
    //  matching the parent homeworld is most likely, followed by races with
    //  similar skin tone- i.e, adjacent in the spectrum.)
    final boolean report = verbose;
    final Pick <Trait> racePick = new Pick <Trait> ();
    final int raceID = Visit.indexOf(world.climate, RACIAL_TRAITS);
    float sumChances = 0;
    if (report) {
      I.say("\nApplying effects of "+world);
      I.say("  Default climate:    "+world.climate+", ID: "+raceID);
    }
    
    for (int n = RACIAL_TRAITS.length; n-- > 0;) {
      float chance = 1;
      if (n                    == raceID) chance /= 1 - RACE_CLIMATE_CHANCE;
      if (Nums.abs(raceID - n) == 1     ) chance /= 1 - HALF_CLIMATE_CHANCE;
      sumChances += chance;
      racePick.compare(RACIAL_TRAITS[n], chance * Rand.avgNums(2));
    }
    final Trait race = racePick.result();
    final float raceChance = racePick.bestRating() / sumChances;
    actor.traits.setLevel(race, (raceChance + 1) / 2);
    if (report) I.say("  RACE PICKED: "+race+", CHANCE: "+raceChance);
    //  TODO:  Blend these a bit more, once you have the graphics in order?
    
    //
    //  Vary height/build based on gravity-
    //  TODO:  Have the citizen models actually reflect this.
    actor.traits.incLevel(TALL , Rand.num() * -1 * world.gravity);
    actor.traits.incLevel(STOUT, Rand.num() * 1 * world.gravity);
  }
  
  
  
  /**  And finally, some finishing touches for material and social assets-
    */
  public static void applyGear(Background v, Actor actor) {
    final int BQ = v.standing;
    
    for (Traded gear : v.gear) {
      if (gear instanceof DeviceType) {
        final int quality = Nums.clamp(BQ - 1 + Rand.index(3), 4);
        actor.gear.equipDevice(Item.withQuality(gear, quality));
      }
      else if (gear instanceof OutfitType) {
        final int quality = Nums.clamp(BQ - 1 + Rand.index(3), 4);
        actor.gear.equipOutfit(Item.withQuality(gear, quality));
      }
      else actor.gear.addItem(Item.withAmount(gear, 1 + Rand.index(3)));
    }
    
    final float cash = (50 + Rand.index(100)) * BQ / 2f;
    if (cash > 0) actor.gear.incCredits(cash);
    else actor.gear.incCredits(Rand.index(5));
    
    actor.gear.boostShields(actor.gear.maxShields() / 2f, true);
  }
}





