/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.actors ;
import src.game.building.* ;
import src.game.campaign.System ;
import src.game.common.* ;
import src.util.* ;



public class Career implements Abilities {
  
  
  
  private Actor subject ;
  private Background gender ;
  private Background vocation, birth, homeworld ;
  private String fullName = null ;
  
  
  public Career(
    boolean male, Background vocation, Background birth, Background homeworld
  ) {
    this.gender = male ? Background.MALE_BIRTH : Background.FEMALE_BIRTH ;
    this.vocation = vocation ;
    this.birth = birth ;
    this.homeworld = homeworld ;
  }
  
  
  public Career(Background root) {
    vocation = root ;
  }
  
  
  public Career(Actor subject) {
    this.subject = subject ;
  }
  
  
  public void loadState(Session s) throws Exception {
    subject = (Actor) s.loadObject() ;
    gender    = (Background) s.loadObject() ;
    birth     = (Background) s.loadObject() ;
    homeworld = (Background) s.loadObject() ;
    vocation  = (Background) s.loadObject() ;
    fullName = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(subject) ;
    s.saveObject(gender   ) ;
    s.saveObject(birth    ) ;
    s.saveObject(homeworld) ;
    s.saveObject(vocation ) ;
    s.saveString(fullName) ;
  }
  
  
  public Background vocation() {
    return vocation ;
  }
  
  
  public Background birth() {
    return birth ;
  }
  
  
  public Background homeworld() {
    return homeworld ;
  }
  
  
  public Background topBackground() {
    if (vocation  != null) return vocation  ;
    if (homeworld != null) return homeworld ;
    if (birth     != null) return birth     ;
    if (gender    != null) return gender    ;
    return null ;
  }
  
  
  public String fullName() {
    return fullName ;
  }
  
  
  
  /**  Fresh developments after recruitment...
    */
  public void recordVocation(Background b) {
    //
    //  TODO:  Add this to a list of vocations taken!
    vocation = b ;
  }
  
  
  
  /**  Binds this career to a specific in-world actor and configures their
    *  physique, aptitudes and motivations:
    */
  public void applyCareer(Human actor) {
    subject = actor ;
    applyBackgrounds(actor) ;
    //
    //  We top up basic attributes to match.
    actor.traits.initDNA(0) ;
    actor.health.setupHealth(
      Visit.clamp(Rand.avgNums(2), 0.26f, 0.94f),
      1, 0
    ) ;
    //
    //  For now, we apply gender at random, though this might be tweaked a bit
    //  later.  We also assign some random personality and/or physical traits.
    while (true) {
      final int numP = actor.traits.personality().size() ;
      if (numP >= 5) break ;
      final Trait t = (Trait) Rand.pickFrom(PERSONALITY_TRAITS) ;
      actor.traits.incLevel(t, Rand.range(-2, 2)) ;
      if (numP >= 3 && Rand.yes()) break ;
    }
    actor.traits.incLevel(HANDSOME, Rand.rangeAvg(-2, 2, 2)) ;
    actor.traits.incLevel(TALL    , Rand.rangeAvg(-2, 2, 2)) ;
    actor.traits.incLevel(STOUT   , Rand.rangeAvg(-2, 2, 2)) ;
    applySystem((System) homeworld, actor) ;
    applySex(actor) ;
    //
    //  Finally, specify name and (TODO:) a few other details of appearance.
    for (String name : Wording.namesFor(actor)) {
      if (fullName == null) fullName = name ;
      else fullName+=" "+name ;
    }
    ///I.say("Full name: "+fullName) ;
    //
    //  Along with current wealth and equipment-
    applyGear(vocation, actor) ;
  }
  
  
  private void applyBackgrounds(Human actor) {
    Background root = vocation ;
    if (birth == null) {
      final Batch <Float> weights = new Batch <Float> () ;
      for (Background v : Background.OPEN_CLASSES) {
        weights.add(ratePromotion(root, actor)) ;
      }
      birth = (Background) Rand.pickFrom(
        Background.OPEN_CLASSES, weights.toArray()
      ) ;
    }
    if (homeworld == null) {
      final Batch <Float> weights = new Batch <Float> () ;
      for (Background v : Background.ALL_PLANETS) {
        weights.add(ratePromotion(root, actor)) ;
      }
      homeworld = (Background) Rand.pickFrom(
        Background.ALL_PLANETS, weights.toArray()
      ) ;
    }
    applyVocation(homeworld, actor) ;
    applyVocation(birth    , actor) ;
    applyVocation(vocation , actor) ;
    setupAttributes(actor) ;
  }
  
  
  private void setupAttributes(Actor actor) {
    float minPhys = 0, minSens = 0, minCogn = 0 ;
    for (Skill s : actor.traits.skillSet()) {
      final float level = actor.traits.traitLevel(s) ;
      actor.traits.raiseLevel(s.parent, level - Rand.index(5)) ;
      if (s.form == FORM_COGNITIVE) minCogn = Math.max(level, minCogn + 1) ;
      if (s.form == FORM_SENSITIVE) minSens = Math.max(level, minSens + 1) ;
      if (s.form == FORM_PHYSICAL ) minPhys = Math.max(level, minPhys + 1) ;
    }
    actor.traits.raiseLevel(BRAWN    , (minPhys + Rand.rollDice(3, 7)) / 2f) ;
    actor.traits.raiseLevel(VIGOUR   , (minPhys + Rand.rollDice(3, 7)) / 2f) ;
    actor.traits.raiseLevel(REFLEX   , (minSens + Rand.rollDice(3, 7)) / 2f) ;
    actor.traits.raiseLevel(INSIGHT  , (minSens + Rand.rollDice(3, 7)) / 2f) ;
    actor.traits.raiseLevel(INTELLECT, (minCogn + Rand.rollDice(3, 7)) / 2f) ;
    actor.traits.raiseLevel(WILL     , (minCogn + Rand.rollDice(3, 7)) / 2f) ;
  }
  
  
  private void applySex(Human actor) {
    if (gender == null) {
      final float
        rateM = ratePromotion(Background.MALE_BIRTH  , actor),
        rateF = ratePromotion(Background.FEMALE_BIRTH, actor) ;
      if (rateM * Rand.avgNums(2) > rateF * Rand.avgNums(2)) {
        gender = Background.MALE_BIRTH ;
      }
      else gender = Background.FEMALE_BIRTH ;
    }
    //
    //  TODO:  Some of these traits need to be rendered 'dormant' in younger
    //  citizens...
    applyVocation(gender, actor) ;
    float ST = Visit.clamp(Rand.rangeAvg(-1, 3, 2), 0, 3) ;
    if (Rand.index(20) == 0) ST = -1 ;
    if (gender == Background.FEMALE_BIRTH) {
      actor.traits.setLevel(GENDER, "Female") ;
      actor.traits.setLevel(FEMININE, ST) ;
    }
    else {
      actor.traits.setLevel(GENDER, "Male") ;
      actor.traits.setLevel(FEMININE, 0 - ST) ;
    }
    actor.traits.setLevel(
      ORIENTATION,
      Rand.index(10) != 0 ? "Heterosexual" :
      (Rand.yes() ? "Homosexual" : "Bisexual")
    ) ;
  }
  
  
  //
  //  TODO:  Try incorporating these trait-FX into the rankings first.
  private void applySystem(System world, Actor actor) {
    //
    //  Assign skin texture based on prevailing climate-
    //  TODO:  Blend these a bit more, once you have the graphics in order.
    final Trait bloods[] = {
      DESERT_BLOOD,
      FOREST_BLOOD,
      TUNDRA_BLOOD,
      WASTES_BLOOD
    } ;
    Trait pickBlood = null ;
    ///I.say("Applying system: "+world.name+", climate: "+world.climate) ;
    for (int n = 4 ; n-- > 0 ;) {
      if (bloods[n] == world.climate) {
        final float roll = Rand.num() ;
        final int index ;
        if (roll < 0.65f) index = 0 ;
        else if (roll < 0.80f) index = 1 ;
        else if (roll < 0.95f) index = 3 ;
        else index = 2 ;
        pickBlood = bloods[(n + index) % 4] ;
      }
    }
    if (pickBlood != null) actor.traits.setLevel(pickBlood, 1) ;
    //
    //  Vary height/build based on gravity-
    //  TODO:  Have the citizen models actually reflect this.
    actor.traits.incLevel(TALL, Rand.num() * -1 * world.gravity) ;
    actor.traits.incLevel(STOUT, Rand.num() * 1 * world.gravity) ;
  }
  

  //
  //  TODO:  Check for similar traits?
  public static float ratePromotion(Background next, Actor actor) {
    float rating = 1 ;
    //
    //  Check for similar skills.
    for (Skill s : next.baseSkills.keySet()) {
      rating += rateSimilarity(s, next, actor) ;
    }
    final Batch <Skill> skills = actor.traits.skillSet() ;
    for (Skill s : actor.traits.skillSet()) {
      rating += rateSimilarity(s, next, actor) ;
    }
    rating /= 1 + next.baseSkills.size() + skills.size() ;
    //
    //  Favour transition to more prestigious vocations-
    if (actor instanceof Human) {
      final Background prior = ((Human) actor).career().topBackground() ;
      if (next.standing < prior.standing) return rating / 10f ;
    }
    return rating ;
  }
  
  
  static float rateSimilarity(Skill s, Background a, Actor actor) {
    Integer aL = a.baseSkills.get(s), bL = (int) actor.traits.traitLevel(s) ;
    if (aL == null || bL == null) return 0 ;
    return (aL > bL) ? ((bL + 5f) / (aL + 5f)) : ((aL + 5f) / (bL + 5f)) ;
  }
  
  
  public static boolean qualifies(Actor a, Background b) {
    for (Skill s : b.baseSkills.keySet()) {
      final int level = b.baseSkills.get(s) ;
      if (a.traits.traitLevel(s) < level) return false ;
    }
    return true ;
  }
  
  
  private void applyVocation(Background v, Actor actor) {
    ///I.say("Applying vocation: "+v) ;
    
    for (Skill s : v.baseSkills.keySet()) {
      final int level = v.baseSkills.get(s) ;
      actor.traits.raiseLevel(s, level + (Rand.num() * 5)) ;
    }
    
    for (Trait t : v.traitChances.keySet()) {
      float chance = v.traitChances.get(t) ;
      while (Rand.index(10) < Math.abs(chance) * 10) {
        actor.traits.incLevel(t, chance > 0 ? 0.5f : -0.5f) ;
        chance /= 2 ;
      }
      actor.traits.incLevel(t, chance * Rand.num()) ;
    }
  }
  
  
  public static void applyGear(Background v, Actor actor) {
    int BQ = v.standing ;
    for (Service gear : v.gear) {
      if (gear instanceof DeviceType) {
        actor.gear.equipDevice(Item.withQuality(gear, BQ + Rand.index(3))) ;
      }
      else if (gear instanceof OutfitType) {
        actor.gear.equipOutfit(Item.withQuality(gear, BQ + Rand.index(3))) ;
      }
      else actor.gear.addItem(Item.withAmount(gear, 1 + Rand.index(3))) ;
    }
    if (actor.gear.credits() == 0) {
      actor.gear.incCredits((50 + Rand.index(100)) * BQ / 2f) ;
    }
    actor.gear.boostShields(actor.gear.maxShields() / 2f, true) ;
  }
}







/*
public static float ratePromotion(Background next, Background prior) {
  float rating = 1 ;
  //
  //  Check for similar skills.
  for (Skill s : next.baseSkills.keySet()) {
    rating += rateSimilarity(s, next, prior) ;
  }
  for (Skill s : prior.baseSkills.keySet()) {
    rating += rateSimilarity(s, next, prior) ;
  }
  rating /= 1 + next.baseSkills.size() + prior.baseSkills.size() ;
  
  //
  //  Favour transition to more prestigous vocations-
  if (next.standing < prior.standing) return rating / 10f ;
  return rating ;
}
//*/


