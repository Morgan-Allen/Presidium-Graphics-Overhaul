/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.graphics.sfx.*;  //TODO:  Get rid of direct reference?
import stratos.user.*;
import stratos.util.*;



//  TODO:  Consider putting skills, traits and physique in separate classes.

public class ActorTraits implements Qualities {
  
  
  /**  Common fields, constructors, and save/load methods-
    */
  final static int 
    DNA_SIZE         = 16,
    DNA_LETTERS      = 26,
    MUTATION_PERCENT = 5;
  private static boolean verbose = false;
  

  private static class Level {
    float value, bonus;
  }
  
  private Table <Trait, Level> levels = new Table <Trait, Level> ();
  
  final Actor actor;
  private String DNA = null;
  private int geneHash = -1;
  
  
  
  public ActorTraits(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    DNA = s.loadString();
    geneHash = s.loadInt();
    for (int n = s.loadInt(); n-- > 0;) {
      final Trait type = Trait.loadConstant(s);//   ALL_TRAIT_TYPES[s.loadInt()];
      final Level level = new Level();
      level.value = s.loadFloat();
      level.bonus = s.loadFloat();
      levels.put(type, level);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveString(DNA);
    s.saveInt(geneHash);
    s.saveInt(levels.size());
    for (Trait type : levels.keySet()) {
      type.saveState(s);
      //s.saveInt(type.traitID);
      final Level level = levels.get(type);
      s.saveFloat(level.value);
      s.saveFloat(level.bonus);
    }
  }
  
  
  public void initAtts(float physical, float sensitive, float cognitive) {
    //
    //  Replace these with just 3 attributes.  Simpler that way.
    actor.traits.setLevel(MUSCULAR , physical );
    actor.traits.setLevel(IMMUNE   , physical );
    actor.traits.setLevel(MOTOR    , sensitive);
    actor.traits.setLevel(PERCEPT  , sensitive);
    actor.traits.setLevel(COGNITION, cognitive);
    actor.traits.setLevel(NERVE    , cognitive);
  }
  
  
  
  /**  Methods for dealing with DNA, used as a random seed for certain
    *  cosmetic or inherited traits, along with checks for inbreeding.
    */
  public void initDNA(int mutationMod, Actor... parents) {
    //
    //  First, if required, we sample the genetic material of the parents-
    final boolean free = parents == null || parents.length == 0;
    final char material[][];
    if (free) material = null;
    else {
      material = new char[parents.length][];
      for (int n = parents.length; n-- > 0;) {
        material[n] = parents[n].traits.DNA.toCharArray();
      }
    }
    //
    //  Then, we merge the source material along with a certain mutation rate.
    final StringBuffer s = new StringBuffer();
    for (int i = 0; i < DNA_SIZE; i++) {
      if (free || Rand.index(100) < (MUTATION_PERCENT + mutationMod)) {
        s.append((char) ('a' + Rand.index(DNA_LETTERS)));
      }
      else {
        s.append(material[Rand.index(parents.length)][i]);
      }
    }
    DNA = s.toString();
    geneHash = DNA.hashCode();
  }
  
  
  public float inbreedChance(Actor a, Actor b) {
    //
    //  TODO:  Also use kinship networks.
    final char[]
      mA = a.traits.DNA.toCharArray(),
      mB = b.traits.DNA.toCharArray();
    int matches = 0;
    for (int n = DNA_SIZE; n-- > 0;) if (mA[n] == mB[n]) matches++;
    return matches * 1f / DNA_SIZE;
  }
  
  
  public int geneValue(String gene, int range) {
    final int value = (gene.hashCode() + geneHash) % range;
    return (value > 0) ? value : (0 - value);
  }
  
  
  public boolean male() {
    return traitLevel(Trait.GENDER_MALE) > 0;
  }
  
  
  public boolean female() {
    return traitLevel(Trait.GENDER_FEMALE) > 0;
  }
  
  
  protected void afterMutation(float increase, boolean selected) {
    //
    //  Here, we pick randomly from the mutation table.  (Which means that,
    //  yes, it is theoretically possible to get superpowers this way.)  The
    //  difference between selected and non-selected mutations is that the
    //  former are inherited within a particular population and tend to be
    //  beneficial, whereas the latter are the result of random chance, and
    //  tend to be harmful.
    if (GameSettings.hardCore && (! selected) && Rand.num() < increase) {
      incLevel(Conditions.CANCER, increase * 2 * Rand.num());
    }
    /*
    if (Rand.num() > increase / 5f) return;
    final Trait MT[] = MUTANT_TRAITS; final int ML = MT.length;
    
    float roll = Rand.avgNums(3);
    if (selected) roll = (roll * roll) - 0.1f;
    else {
      roll = 1 - (roll * roll);
      if (Rand.yes() || GameSettings.hardCore) incLevel(STERILE, Rand.num());
    }
    if (GameSettings.hardCore) roll = Visit.clamp(roll, 0.25f, 0.75f);
    final Trait gained = MT[Visit.clamp((int) (roll * ML), ML)];
    incLevel(gained, 0.5f + Rand.num());
    //*/
  }
  
  
  
  /**  Methods for assigning temporary bonuses-
    */
  public void updateTraits(int numUpdates) {
    final Batch <Trait> allTraits = new Batch <Trait> (levels.size());
    for (Trait t : levels.keySet()) allTraits.add(t);
    
    for (Trait t : allTraits) if (t.type != CONDITION) {
      final Level level = levels.get(t);
      if (level.bonus != 0) level.bonus = 0;
    }
    for (Trait t : allTraits) if (t.type == CONDITION) {
      t.affect(actor);
    }
  }
  
  
  public void incBonus(Trait type, float bonusInc) {
    final Level level = levels.get(type);
    if (level == null) return;
    level.bonus += bonusInc;
  }
  
  
  public void setBonus(Trait type, float b) {
    final Level level = levels.get(type);
    if (level == null) return;
    level.bonus = b;
  }
  
  
  
  
  /**  Methods for querying and modifying the levels of assorted traits-
    */
  public float traitLevel(Trait type) {
    final Level level = levels.get(type);
    if (level == null) {
      final Trait o = type.opposite();
      if (o != null && levels.get(o) != null) return 0 - traitLevel(o);
      return 0;
    }
    return Nums.clamp(level.value, type.minVal, type.maxVal);
  }
  
  
  public float relativeLevel(Trait type) {
    //  Returns a value between -1 (for minimum) and +1 (for maximum).
    final float level = usedLevel(type);
    if (level > 0) return level / type.maxVal;
    else return 0 - level / type.minVal;
  }
  
  
  public float effectBonus(Trait trait) {
    final Level level = levels.get(trait);
    if (level == null) return 0;
    return level.bonus;
  }
  
  
  public int bonusFrom(Skill parent) {
    if (parent == null) return 0;
    final float ageMult = actor.health.ageMultiple();
    return (int) (0.5f + (traitLevel(parent) * ageMult / 5f));
  }
  
  
  public float usedLevel(Trait type) {
    if (type.type == PERSONALITY) {
      return traitLevel(type);
    }
    
    final boolean report = verbose && I.talkAbout == actor;
    final Level TL = levels.get(type);
    float level = TL == null ? 0 : (TL.value + TL.bonus);
    
    if (type.type == PHYSICAL) {
      return level * actor.health.ageMultiple();
    }
    
    if (type.type == SKILL) {
      final Skill skill = (Skill) type;
      if (skill.parent == null) {
        return level * actor.health.ageMultiple();
      }
      else {
        if (! actor.health.conscious()) level /= 2;
        level += bonusFrom(skill.parent);
      }
      if (report) I.say(" level of "+type+" is "+level);
      if (report) I.say(" root bonus: "+bonusFrom(skill.parent));
      level *= 1 - actor.health.stressPenalty();
    }
    
    return level;
  }
  
  
  public boolean hasTrait(Trait type) {
    return traitLevel(type) > 0;
  }
  
  
  public boolean hasTrait(Trait type, String desc) {
    int i = 0; for (String s : type.labels) {
      if (desc.equals(s)) {
        final float value = type.labelValues[i];
        if (value > 0) return traitLevel(type) >= value;
        else return traitLevel(type) <= value;
      }
      else i++;
    }
    return false;
  }
  
  
  public void setLevel(Trait type, float toLevel) {
    if (toLevel < 0 && type.opposite() != null) {
      setLevel(type, 0);
      setLevel(type.opposite(), 0 - toLevel);
      return;
    }
    
    if (toLevel == 0) {
      type.onRemoval(actor);
      levels.remove(type);
      return;
    }
    
    Level level = levels.get(type);
    if (level == null) {
      levels.put(type, level = new Level());
      type.onAddition(actor);
    }
    
    final float oldVal = level.value;
    level.value = toLevel;
    /*
    if (type == MUTATION) {
      afterMutation(level.value - oldVal, false);
    }
    if (level.value > oldVal && type.type == CONDITION) {
      type.affect(actor);
    }
    //*/
    tryReport(type, level.value - (int) oldVal);
  }
  
  
  public void remove(Trait type) {
    setLevel(type, 0);
  }
  
  
  public float incLevel(Trait type, float boost) {
    final float newLevel = traitLevel(type) + boost;
    setLevel(type, newLevel);
    return newLevel;
  }
  
  
  public void raiseLevel(Trait type, float toLevel) {
    if (toLevel < traitLevel(type)) return;
    setLevel(type, toLevel);
  }
  
  
  public void setLevel(Trait type, String desc) {
    int i = 0; for (String s : type.labels) {
      if (desc.equals(s)) {
        setLevel(type, type.labelValues[i]);
        return;
      }
      else i++;
    }
  }
  
  
  
  /**  Accessing particular trait headings-
    */
  private Batch <Trait> getMatches(Batch <Trait> traits, Trait[] types) {
    if (traits == null) traits = new Batch <Trait> ();
    for (Trait t : types) {
      final Level l = levels.get(t);
      if (l == null || Nums.abs(l.value) < 0.5f) continue;
      traits.add(t);
    }
    return traits;
  }
  
  
  public Batch <Trait> personality() {
    return getMatches(null, Qualities.PERSONALITY_TRAITS);
  }
  
  
  public Batch <Trait> physique() {
    final Batch <Trait> matches = new Batch <Trait> ();
    getMatches(matches, Qualities.PHYSICAL_TRAITS);
    getMatches(matches, Qualities.RACIAL_TRAITS);
    return matches;
  }
  
  
  public Batch <Trait> characteristics() {
    return getMatches(null, Qualities.CATEGORIC_TRAITS);
  }
  
  
  public Batch <Skill> attributes() {
    return (Batch) getMatches(null, Qualities.ATTRIBUTES);
  }
  
  
  public Batch <Skill> skillSet() {
    final Batch <Trait> matches = new Batch <Trait> ();
    getMatches(matches, Qualities.INSTINCT_SKILLS );
    getMatches(matches, Qualities.PHYSICAL_SKILLS );
    getMatches(matches, Qualities.SENSITIVE_SKILLS);
    getMatches(matches, Qualities.COGNITIVE_SKILLS);
    getMatches(matches, Qualities.PSYONIC_SKILLS  );
    return (Batch) matches;
  }
  
  
  public Batch <Condition> conditions() {
    final Batch <Condition> matches = new Batch();
    for (Trait t : levels.keySet()) {
      if (t instanceof Condition) matches.add((Condition) t);
    }
    return matches;
    //return (Batch) getMatches(null, Conditions.ALL_CONDITIONS);
  }
  
  /*
  public Batch <Trait> mutations() {
    return (Batch) getMatches(null, Abilities.MUTANT_TRAITS);
  }
  //*/
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String description(Trait type) {
    return Trait.descriptionFor(type, relativeLevel(type));
  }
  
  
  public String genderDescription() {
    if (male  ()) return "Male"  ;
    if (female()) return "Female";
    return "Neuter";
  }
  
  
  private void tryReport(Trait type, float diff) {
    if ((! actor.inWorld()) || Nums.abs(diff) < 1) return;
    final String prefix = diff > 0 ? "+" : "";
    actor.chat.addPhrase(prefix+type, TalkFX.NOT_SPOKEN);
  }
}


