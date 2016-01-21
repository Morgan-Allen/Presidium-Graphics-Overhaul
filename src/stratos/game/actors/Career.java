/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.verse.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.verse.Faction.*;



//  TODO:  MERGE THIS WITH THE ACTOR'S MEMORY-SET?


public class Career {
  
  
  private static boolean
    verbose = false;
  
  final static int
    MIN_PERSONALITY           = 3,
    NUM_RANDOM_CAREER_SAMPLES = 3,
    HOMEWORLD_RATING_BONUS    = 3;
  
  
  private Actor subject;
  private Background gender;
  private Background vocation, birth;
  private Sector homeworld;
  private String fullName = null;
  
  
  public Career(
    Background vocation, Background birth,
    Sector homeworld, Background gender
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
    homeworld = (Sector    ) s.loadObject();
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
  
  
  public Sector homeworld() {
    return homeworld;
  }
  
  
  public Background topBackground() {
    if (vocation  != null) return vocation;
    if (birth     != null) return birth   ;
    if (gender    != null) return gender  ;
    if (homeworld != null) return homeworld.asBackground;
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
  public void applyCareer(Human actor, Faction faction) {
    if (verbose) I.say("\nGENERATING NEW CAREER");
    subject = actor;
    
    applyBackgrounds(actor, faction);
    applySex(actor);
    setupAttributes(actor);
    fillPersonality(actor);
    
    //
    //  TODO:  Specify a few starter relationships here!  (And vary the base
    //  relation somewhat.  Also, consider encoding this data in Backgrounds?)
    final ActorTraits AT = actor.traits;
    final float
      likeFauna = Nums.clamp(AT.traitLevel(XENOZOOLOGY ) / 10, 0, 1),
      likeNativ = Nums.clamp(AT.traitLevel(NATIVE_TABOO) / 10, 0, 1),
      likeRobot = Nums.clamp(AT.traitLevel(ANCIENT_LORE) / 10, 0, 1);

    final ActorRelations AR = actor.relations;
    AR.setRelation(FACTION_WILDLIFE , likeFauna / 2, 1 - likeFauna);
    AR.setRelation(FACTION_NATIVES  , likeNativ / 2, 1 - likeNativ);
    AR.setRelation(FACTION_ARTILECTS, likeRobot / 2, 1 - likeRobot);
    AR.setRelation(faction, 0.5f, 0);
    
    
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
  private void applyBackgrounds(Human actor, Faction faction) {
    //
    //  If the target vocation is undetermined, we work forward at random from
    //  birth towards a final career stage:
    if (vocation == null) {
      pickBirthClass(actor, faction);
      applyBackground(birth, actor);
      
      pickHomeworld(actor, faction);
      if (homeworld != null) {
        applyBackground(homeworld.asBackground, actor);
        applySystem((Sector) homeworld, actor);
      }
      
      pickVocation(actor, faction);
      applyBackground(vocation, actor);
    }
    //
    //  Alternatively, we work backwards from the target vocation to determine
    //  a probable system and social class of origin:
    else {
      applyBackground(vocation, actor);
      
      pickHomeworld(actor, faction);
      if (homeworld != null) {
        applyBackground(homeworld.asBackground, actor);
        applySystem((Sector) homeworld, actor);
      }
      
      pickBirthClass(actor, faction);
      applyBackground(birth, actor);
    }
  }
  
  
  private void pickBirthClass(Human actor, Faction faction) {
    if (birth != null) {
      return;
    }
    else if (faction == FACTION_NATIVES) {
      birth = Backgrounds.BORN_NATIVE;
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
  
  
  private void pickHomeworld(Human actor, Faction faction) {
    final Sector home = faction.startSite();
    if (homeworld != null) {
      return;
    }
    else if (faction == FACTION_NATIVES) {
      homeworld = home;
    }
    else if (home != null) {
      //  TODO:  Include some weighting based off house relations?
      final Series <Sector> places  = home.siblings();
      final Batch  <Float > weights = new Batch();
      
      for (Sector p : places) {
        float rating = ratePromotion(p.asBackground, actor, verbose);
        if (p == home) rating *= HOMEWORLD_RATING_BONUS;
        weights.add(rating);
      }
      homeworld = (Sector) Rand.pickFrom(places, weights);
    }
  }
  
  
  private void pickVocation(Human actor, Faction faction) {
    final Pick <Background> pick = new Pick <Background> ();
    final Sector homeworld = faction.startSite();
    
    if (faction == FACTION_NATIVES || homeworld == null) {
      for (Background b : Backgrounds.NATIVE_CIRCLES) {
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num());
      }
    }
    else {
      for (Background b : homeworld.circles()) {
        final float weight = homeworld.weightFor(b);
        if (weight <= 0) continue;
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num() * weight);
      }
      for (int n = NUM_RANDOM_CAREER_SAMPLES; n-- > 0;) {
        final Background b = (Background) Rand.pickFrom(
          Backgrounds.ALL_STANDARD_CIRCLES
        );
        final float weight = (1 + homeworld.weightFor(b)) / 2;
        pick.compare(b, ratePromotion(b, actor, verbose) * Rand.num() * weight);
      }
    }
    this.vocation = pick.result();
  }
  
  
  private void applyBackground(Background v, Actor actor) {
    if (verbose) I.say("\nApplying vocation: "+v);
    
    for (Skill s : v.baseSkills.keySet()) {
      final float
        pastLevel = actor.traits.traitLevel(s),
        baseLevel = v.baseSkills.get(s) + (Rand.num() * 5) - 2.5f,
        bonus     = Nums.min(baseLevel, pastLevel) / 2,
        newLevel  = Nums.max(baseLevel, pastLevel) + bonus;
      actor.traits.setLevel(s, newLevel);
      if (s.parent != null) actor.traits.setLevel(s.parent, newLevel / 2);
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
  
  
  /**  Promotion-evaluation methods:
    */
  public static float ratePromotion(
    Background next, Actor actor, boolean report
  ) {
    float fitness = rateDefaultFitness(actor, next, report);
    float desire  = rateDefaultDesire (actor, next, report);
    return Nums.min(fitness, desire);
  }
  
  
  static float rateDefaultFitness(
    Actor actor, Background position, boolean report
  ) {
    float skillsRating = 0, sumSkills = 0;
    if (report) I.say("\nRating fitness of "+I.tagHash(actor)+" as "+position);
    //
    //  NOTE:  The numbers chosen here are quite sensitive, so please don't
    //  fiddle with them without some testing.
    for (Skill skill : position.baseSkills.keySet()) {
      final float
        jobLevel  = position.baseSkills.get(skill),
        haveLevel = actor.traits.traitLevel(skill),
        rating    = Nums.clamp((haveLevel + 10 - jobLevel) / 10, 0, 1.5f);
      sumSkills    += jobLevel;
      skillsRating += jobLevel * rating;
      if (report) {
        I.say("  "+skill+": "+rating+" (have "+haveLevel+"/"+jobLevel+")");
      }
    }
    if (sumSkills > 0) skillsRating = skillsRating /= sumSkills;
    else skillsRating = 1;
    
    if (report) I.say("  Overall rating: "+skillsRating);
    return skillsRating;
  }
  
  
  static float rateDefaultDesire(
    Actor actor, Background position, boolean report
  ) {
    float rating = 1.0f;
    if (report) I.say("\nRating desire by "+I.tagHash(actor)+" for "+position);
    //
    //  Citizens gravitate to jobs that suit their temperament, so we get a
    //  weighted average of those traits associated with the position, relative
    //  to how much the actor possesses them-
    if (position.traitChances.size() > 0) {
      float sumChances = 0, sumWeights = 0;
      for (Trait t : position.traitChances.keySet()) {
        final float posChance = position.traitChances.get(t);
        if (posChance == 0) continue;
        //
        //  NOTE: Personality traits are handled a little differently, since
        //  those can have opposites:
        final float ownChance = (t.type == PERSONALITY) ?
          Personality.traitChance(t, actor) :
          actor.traits.traitLevel(t)        ;
        
        if (report) I.say("  Chance due to "+t+" is "+ownChance+"/"+posChance);
        sumWeights += Nums.abs(posChance);
        sumChances += ownChance * (posChance > 0 ? 1 : -1);
      }
      
      if (sumWeights > 0) rating *= (1 + (sumChances / sumWeights)) / 2;
      if (report) {
        I.say("  Total trait chance: "+sumChances+"/"+sumWeights);
        I.say("  Subsequent rating:  "+rating);
      }
    }
    //
    //  Finally, we also favour transition to more prestigious vocations.
    int nextStanding = position.standing;
    if (actor instanceof Human && nextStanding != Backgrounds.NOT_A_CLASS) {
      final Background prior = ((Human) actor).career().topBackground();
      if (report) {
        I.say("  Prior standing: "+prior.standing);
        I.say("  Next standing:  "+nextStanding  );
      }
      while (nextStanding < prior.standing) { rating /= 2; nextStanding++; }
      while (nextStanding > prior.standing) { rating *= 2; nextStanding--; }
    }
    if (report) I.say("  Overall rating: "+rating);
    return rating;
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
    TRANS_CHANCE    = 0.05f,
    
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
        rateM = ratePromotion(Backgrounds.BORN_MALE  , actor, verbose),
        rateF = ratePromotion(Backgrounds.BORN_FEMALE, actor, verbose);
      if (rateM * Rand.avgNums(2) > rateF * Rand.avgNums(2)) {
        gender = Backgrounds.BORN_MALE;
      }
      else gender = Backgrounds.BORN_FEMALE;
    }
    applyBackground(gender, actor);
    
    //  TODO:  Do some of these traits need to be rendered 'dormant' in younger
    //  citizens?
    
    float ST = Nums.clamp(Rand.rangeAvg(-1, 3, 2), 0, 3);
    if (Rand.num() < TRANS_CHANCE) ST = -1;
    final int GT = gender == Backgrounds.BORN_FEMALE ? 1 : -1;
    actor.traits.setLevel(GENDER_FEMALE,      GT);
    actor.traits.setLevel(GENDER_MALE  ,     -GT);
    actor.traits.setLevel(FEMININE     , ST * GT);
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
      if (report) {
        I.say("  Faction chance for "+RACIAL_TRAITS[n]+" is: "+chance);
      }
      chance *= Rand.avgNums(2);
      sumChances += chance;
      racePick.compare(RACIAL_TRAITS[n], chance);
    }
    final Trait race = racePick.result();
    final float raceChance = racePick.bestRating() / sumChances;
    actor.traits.setLevel(race, (raceChance + 1) / 2);
    if (report) {
      I.say("  RACE PICKED: "+race+", CHANCE: "+raceChance);
      for (Trait t : RACIAL_TRAITS) {
        I.say("  Level of "+t+" is "+actor.traits.traitLevel(t));
      }
    }
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
    
    final float
      maxShields = actor.gear.maxShields   (),
      maxCells   = actor.gear.maxPowerCells(),
      maxAmmo    = actor.gear.maxAmmoUnits ();
    if (maxShields > 0) {
      actor.gear.boostShields(maxShields, true);
    }
    if (maxCells > 0) {
      float fill = (Rand.num() + 1) / 2;
      actor.gear.bumpItem(Outfits.POWER_CELLS, maxCells * fill);
    }
    if (maxAmmo > 0) {
      float fill = (Rand.num() + 1) / 2;
      actor.gear.bumpItem(Devices.AMMO_CLIPS, maxAmmo * fill);
    }
    
    //  TODO:  BASE THIS OFF THE ACQUISITIVE TRAIT?
    float cash = ((Rand.num() + 0.5f) * v.defaultSalary) + Rand.index(10);
    actor.gear.incCredits(cash * GameSettings.SPENDING_MULT / 2);
    actor.gear.taxDone();
  }
}










