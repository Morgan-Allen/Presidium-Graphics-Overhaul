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
  
  
  final static int
    MIN_PERSONALITY           = 3,
    NUM_RANDOM_CAREER_SAMPLES = 3;
  
  private static boolean verbose = false;
  
  
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
    applySystem((Sector) homeworld, actor);
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
        weights.add(ratePromotion(v, actor));
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
        weights.add(ratePromotion(v, actor));
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
        pick.compare(b, ratePromotion(b, actor) * Rand.num());
      }
    }
    else for (Background circle[] : base.commerce.homeworld().circles()) {
      final float weight = base.commerce.homeworld().weightFor(circle);
      for (Background b : circle) {
        pick.compare(b, ratePromotion(b, actor) * Rand.num() * weight);
      }
      for (int n = NUM_RANDOM_CAREER_SAMPLES; n-- > 0;) {
        final Background b = (Background) Rand.pickFrom(
          Backgrounds.ALL_STANDARD_CIRCLES
        );
        pick.compare(b, ratePromotion(b, actor) * Rand.num() / 2);
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
    float rating = 1;
    
    //  Check for similar skills.
    for (Skill s : next.baseSkills.keySet()) {
      rating += rateSimilarity(s, next, actor);
    }
    final Batch <Skill> skills = actor.traits.skillSet();
    for (Skill s : actor.traits.skillSet()) {
      rating += rateSimilarity(s, next, actor);
    }
    rating *= 2f / (1 + next.baseSkills.size() + skills.size());
    
    //  Check for similar traits.
    float sumChances = 1;
    for (Trait t : next.traitChances.keySet()) {
      float chance = next.traitChances.get(t);
      chance *= Personality.traitChance(t, actor);
      sumChances += (1 + chance) / 2f;
    }
    rating *= sumChances * 2f / (1 + next.traitChances.size());
    if (rating < 0) return 0;
    
    //  And favour transition to more prestigious vocations.
    if (actor instanceof Human) {
      final Background prior = ((Human) actor).career().topBackground();
      if (next.standing < prior.standing) return rating / 10f;
    }
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
        rateM = ratePromotion(Backgrounds.MALE_BIRTH  , actor),
        rateF = ratePromotion(Backgrounds.FEMALE_BIRTH, actor);
      if (rateM * Rand.avgNums(2) > rateF * Rand.avgNums(2)) {
        gender = Backgrounds.MALE_BIRTH;
      }
      else gender = Backgrounds.FEMALE_BIRTH;
    }
    //
    //  TODO:  Some of these traits need to be rendered 'dormant' in younger
    //  citizens...
    applyBackground(gender, actor);
    float ST = Nums.clamp(Rand.rangeAvg(-1, 3, 2), 0, 3);
    if (Rand.index(20) == 0) ST = -1;
    if (gender == Backgrounds.FEMALE_BIRTH) {
      actor.traits.setLevel(GENDER, "Female");
      actor.traits.setLevel(FEMININE, ST);
    }
    else {
      actor.traits.setLevel(GENDER, "Male");
      actor.traits.setLevel(FEMININE, 0 - ST);
    }
    actor.traits.setLevel(
      ORIENTATION,
      Rand.index(10) != 0 ? "Heterosexual" :
      (Rand.yes() ? "Homosexual" : "Bisexual")
    );
  }
  
  
  //
  //  TODO:  Try incorporating these trait-FX into the rankings first.
  private void applySystem(Sector world, Actor actor) {
    //
    //  Assign skin texture based on prevailing climate-
    //  TODO:  Blend these a bit more, once you have the graphics in order.
    final Trait bloods[] = {
      DESERT_BLOOD,
      FOREST_BLOOD,
      TUNDRA_BLOOD,
      WASTES_BLOOD
    };
    Trait pickBlood = null;
    ///I.say("Applying system: "+world.name+", climate: "+world.climate);
    for (int n = 4; n-- > 0;) {
      if (bloods[n] == world.climate) {
        final float roll = Rand.num();
        final int index;
        if (roll < 0.65f) index = 0;
        else if (roll < 0.80f) index = 1;
        else if (roll < 0.95f) index = 3;
        else index = 2;
        pickBlood = bloods[(n + index) % 4];
      }
    }
    if (pickBlood != null) actor.traits.setLevel(pickBlood, 1);
    //
    //  Vary height/build based on gravity-
    //  TODO:  Have the citizen models actually reflect this.
    actor.traits.incLevel(TALL, Rand.num() * -1 * world.gravity);
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





