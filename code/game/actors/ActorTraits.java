/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.game.actors ;
import code.game.common.*;
import code.graphics.sfx.TalkFX;
import code.user.*;
import code.util.*;



public class ActorTraits implements Abilities {
  
  
  /**  Common fields, constructors, and save/load methods-
    */
  final static float
    MIN_FAIL_CHANCE    = 0.1f,
    MAX_SUCCEED_CHANCE = 0.9f ;
  final static int 
    DNA_SIZE         = 16,
    DNA_LETTERS      = 26,
    MUTATION_PERCENT = 5 ;
  private static boolean verbose = false ;
  

  private static class Level {
    float value, bonus ;
  }  //studyLevel  TODO:  use that.
  
  private Table <Trait, Level> levels = new Table <Trait, Level> () ;
  
  final Actor actor ;
  private String DNA = null ;
  private int geneHash = -1 ;
  
  
  
  protected ActorTraits(Actor actor) {
    this.actor = actor ;
  }
  
  
  public void loadState(Session s) throws Exception {
    DNA = s.loadString() ;
    geneHash = s.loadInt() ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Trait type = ALL_TRAIT_TYPES[s.loadInt()] ;
      final Level level = new Level() ;
      level.value = s.loadFloat() ;
      level.bonus = s.loadFloat() ;
      levels.put(type, level) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveString(DNA) ;
    s.saveInt(geneHash) ;
    s.saveInt(levels.size()) ;
    for (Trait type : levels.keySet()) {
      s.saveInt(type.traitID) ;
      final Level level = levels.get(type) ;
      s.saveFloat(level.value) ;
      s.saveFloat(level.bonus) ;
    }
  }
  
  
  public void initAtts(float physical, float sensitive, float cognitive) {
    //
    //  Replace these with just 3 attributes.  Simpler that way.
    actor.traits.setLevel(BRAWN    , physical ) ;
    actor.traits.setLevel(VIGOUR   , physical ) ;
    actor.traits.setLevel(REFLEX   , sensitive) ;
    actor.traits.setLevel(INSIGHT  , sensitive) ;
    actor.traits.setLevel(INTELLECT, cognitive) ;
    actor.traits.setLevel(WILL     , cognitive) ;
  }
  
  
  
  /**  Methods for dealing with DNA, used as a random seed for certain
    *  cosmetic or inherited traits, along with checks for inbreeding.
    */
  public void initDNA(int mutationMod, Actor... parents) {
    //
    //  First, if required, we sample the genetic material of the parents-
    final boolean free = parents == null || parents.length == 0 ;
    final char material[][] ;
    if (free) material = null ;
    else {
      material = new char[parents.length][] ;
      for (int n = parents.length ; n-- > 0 ;) {
        material[n] = parents[n].traits.DNA.toCharArray() ;
      }
    }
    //
    //  Then, we merge the source material along with a certain mutation rate.
    final StringBuffer s = new StringBuffer() ;
    for (int i = 0 ; i < DNA_SIZE ; i++) {
      if (free || Rand.index(100) < (MUTATION_PERCENT + mutationMod)) {
        s.append((char) ('a' + Rand.index(DNA_LETTERS))) ;
      }
      else {
        s.append(material[Rand.index(parents.length)][i]) ;
      }
    }
    DNA = s.toString() ;
    geneHash = DNA.hashCode() ;
  }
  
  
  public float inbreedChance(Actor a, Actor b) {
    //
    //  TODO:  Also use kinship networks.
    final char[]
      mA = a.traits.DNA.toCharArray(),
      mB = b.traits.DNA.toCharArray() ;
    int matches = 0 ;
    for (int n = DNA_SIZE ; n-- > 0 ;) if (mA[n] == mB[n]) matches++ ;
    return matches * 1f / DNA_SIZE ;
  }
  
  
  public int geneValue(String gene, int range) {
    final int value = (gene.hashCode() + geneHash) % range ;
    return (value > 0) ? value : (0 - value) ;
  }
  
  
  public boolean male() {
    return hasTrait(Trait.GENDER, "Male") ;
  }
  
  
  public boolean female() {
    return hasTrait(Trait.GENDER, "Female") ;
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
      incLevel(CANCER, increase * 2 * Rand.num()) ;
    }
    /*
    if (Rand.num() > increase / 5f) return ;
    final Trait MT[] = MUTANT_TRAITS ; final int ML = MT.length ;
    
    float roll = Rand.avgNums(3) ;
    if (selected) roll = (roll * roll) - 0.1f ;
    else {
      roll = 1 - (roll * roll) ;
      if (Rand.yes() || GameSettings.hardCore) incLevel(STERILE, Rand.num()) ;
    }
    if (GameSettings.hardCore) roll = Visit.clamp(roll, 0.25f, 0.75f) ;
    final Trait gained = MT[Visit.clamp((int) (roll * ML), ML)] ;
    incLevel(gained, 0.5f + Rand.num()) ;
    //*/
  }
  
  
  
  /**  Methods for assigning temporary bonuses-
    */
  protected void updateTraits(int numUpdates) {
    final Batch <Trait> allTraits = new Batch <Trait> (levels.size()) ;
    for (Trait t : levels.keySet()) allTraits.add(t) ;
    for (Trait t : allTraits) if (t.type != CONDITION) {
      final Level level = levels.get(t) ;
      if (level.bonus != 0) level.bonus = 0 ;
    }
    for (Trait t : allTraits) if (t.type == CONDITION) {
      t.affect(actor) ;
    }
  }
  
  
  public void incBonus(Trait type, float bonusInc) {
    final Level level = levels.get(type) ;
    if (level == null) return ;
    level.bonus += bonusInc ;
  }
  
  
  public void setBonus(Trait type, float b) {
    final Level level = levels.get(type) ;
    if (level == null) return ;
    level.bonus = b ;
  }
  
  
  
  
  /**  Methods for querying and modifying the levels of assorted traits-
    */
  public float traitLevel(Trait type) {
    final Level level = levels.get(type) ;
    if (level == null) return 0 ;
    return Visit.clamp(level.value, type.minVal, type.maxVal) ;
  }
  
  
  public float relativeLevel(Trait type) {
    //  Returns a value between -1 (for minimum) and +1 (for maximum).
    return (Visit.clamp(
      (traitLevel(type) - type.minVal) / (type.maxVal - type.minVal), 0, 1
    ) * 2) - 1 ;
  }
  
  
  public float scaleLevel(Trait type) {
    return (float) Math.pow(2, relativeLevel(type)) ;
  }
  
  
  public float effectBonus(Trait trait) {
    final Level level = levels.get(trait) ;
    if (level == null) return 0 ;
    return level.bonus ;
  }
  
  
  public int rootBonus(Skill skill) {
    final float ageMult = actor.health.ageMultiple() ;
    return (int) (0.5f + (traitLevel(skill.parent) * ageMult / 5f)) ;
  }
  
  
  public float useLevel(Trait type) {
    final Level TL = levels.get(type) ;
    float level = TL == null ? 0 : (TL.value + TL.bonus) ;
    
    if (type.type == PHYSICAL) {
      return level * actor.health.ageMultiple() ;
    }
    
    if (type.type == SKILL) {
      final Skill skill = (Skill) type ;
      if (skill.parent == null) {
        return level * actor.health.ageMultiple() ;
      }
      else {
        if (! actor.health.conscious()) level /= 2 ;
        level += rootBonus(skill) ;
      }
      if (verbose) I.sayAbout(actor, " level of "+type+" is "+level) ;
      if (verbose) I.sayAbout(actor, " root bonus: "+rootBonus(skill)) ;
      level *= 1 - actor.health.stressPenalty() ;
    }
    return level ;
  }
  
  
  public boolean hasTrait(Trait type, String desc) {
    int i = 0 ; for (String s : type.descriptors) {
      if (desc.equals(s)) {
        final float value = type.descValues[i] ;
        if (value > 0) return traitLevel(type) >= value ;
        else return traitLevel(type) <= value ;
      }
      else i++ ;
    }
    return false ;
  }
  
  
  public String levelDesc(Trait type) {
    return Trait.descriptionFor(type, useLevel(type)) ;
  }
  
  
  private void tryReport(Trait type, float diff) {
    if ((! actor.inWorld()) || Math.abs(diff) < 1) return ;
    final String prefix = diff > 0 ? "+" : "" ;
    actor.chat.addPhrase(prefix+type, TalkFX.NOT_SPOKEN) ;
  }
  
  
  public void setLevel(Trait type, float toLevel) {
    if (toLevel == 0) {
      levels.remove(type) ;
      return ;
    }
    
    Level level = levels.get(type) ;
    if (level == null) levels.put(type, level = new Level()) ;
    
    final float oldVal = level.value ;
    level.value = toLevel ;
    /*
    if (type == MUTATION) {
      afterMutation(level.value - oldVal, false) ;
    }
    if (level.value > oldVal && type.type == CONDITION) {
      type.affect(actor) ;
    }
    //*/
    tryReport(type, level.value - (int) oldVal) ;
  }
  
  
  public float incLevel(Trait type, float boost) {
    final float newLevel = traitLevel(type) + boost ;
    setLevel(type, newLevel) ;
    return newLevel ;
  }
  
  
  public void raiseLevel(Trait type, float toLevel) {
    if (toLevel < traitLevel(type)) return ;
    setLevel(type, toLevel) ;
  }
  
  
  public void setLevel(Trait type, String desc) {
    int i = 0 ; for (String s : type.descriptors) {
      if (desc.equals(s)) {
        setLevel(type, type.descValues[i]) ;
        return ;
      }
      else i++ ;
    }
  }
  
  
  
  /**  Accessing particular trait headings-
    */
  private Batch <Trait> getMatches(Batch <Trait> traits, Trait[] types) {
    if (traits == null) traits = new Batch <Trait> () ;
    for (Trait t : types) {
      final Level l = levels.get(t) ;
      if (l == null || Math.abs(l.value) < 0.5f) continue ;
      traits.add(t) ;
    }
    return traits ;
  }
  
  
  public Batch <Trait> personality() {
    return getMatches(null, Abilities.PERSONALITY_TRAITS) ;
  }
  
  
  public Batch <Trait> physique() {
    final Batch <Trait> matches = new Batch <Trait> () ;
    getMatches(matches, Abilities.PHYSICAL_TRAITS) ;
    getMatches(matches, Abilities.BLOOD_TRAITS) ;
    return matches ;
  }
  
  
  public Batch <Trait> characteristics() {
    return getMatches(null, Abilities.CATEGORIC_TRAITS) ;
  }
  
  
  public Batch <Skill> attributes() {
    return (Batch) getMatches(null, Abilities.ATTRIBUTES) ;
  }
  
  
  public Batch <Skill> skillSet() {
    final Batch <Trait> matches = new Batch <Trait> () ;
    getMatches(matches, Abilities.INSTINCT_SKILLS ) ;
    getMatches(matches, Abilities.PHYSICAL_SKILLS ) ;
    getMatches(matches, Abilities.SENSITIVE_SKILLS) ;
    getMatches(matches, Abilities.COGNITIVE_SKILLS) ;
    getMatches(matches, Abilities.PSYONIC_SKILLS  ) ;
    return (Batch) matches ;
  }
  
  
  public Batch <Condition> conditions() {
    return (Batch) getMatches(null, Abilities.CONDITIONS) ;
  }
  
  /*
  public Batch <Trait> mutations() {
    return (Batch) getMatches(null, Abilities.MUTANT_TRAITS) ;
  }
  //*/
  
  
  
  /**  Methods for performing actual skill tests against both static and active
    *  opposition-
    */
  public float chance(
    Skill checked,
    Actor b, Skill opposed,
    float bonus
  ) {
    float bonusA = useLevel(checked) + Math.max(0, bonus) ;
    float bonusB = 0 - Math.min(0, bonus) ;
    if (b != null && opposed != null) bonusB += b.traits.useLevel(opposed) ;
    final float chance = Visit.clamp(bonusA + 10 - bonusB, 0, 20) / 20 ;
    return Visit.clamp(chance, MIN_FAIL_CHANCE, MAX_SUCCEED_CHANCE) ;
  }
  
  
  public float chance(Skill checked, float DC) {
    return chance(checked, null, null, 0 - DC) ;
  }
  
  
  public int test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float duration,
    int range
  ) {
    final float chance = chance(checked, b, opposed, bonus) ;
    int success = 0 ;
    for (int tried = 0 ; tried < range ; tried++) {
      if (Rand.num() < chance) success++ ;
    }
    practice(checked, (1 - chance) * duration / 10) ;
    if (b != null) b.traits.practice(opposed, chance * duration / 10) ;
    return success ;
  }
  
  
  public boolean test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float fullXP
  ) {
    return test(checked, b, opposed, bonus, fullXP, 1) > 0 ;
  }
  
  
  public boolean test(Skill checked, float difficulty, float duration) {
    return test(checked, null, null, 0 - difficulty, duration, 1) > 0 ;
  }
  
  
  public void practice(Skill skillType, float practice) {
    incLevel(skillType, practice / (traitLevel(skillType) + 1)) ;
    if (skillType.parent != null) practice(skillType.parent, practice / 5) ;
  }
  
  
  public void practiceAgainst(int DC, float duration, Skill skillType) {
    final float chance = chance(skillType, null, null, 0 - DC) ;
    practice(skillType, chance * duration / 10) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d) {
    for (Skill s : attributes()) {
      d.append("\n  "+s.name+" "+((int) traitLevel(s))) ;
    }
    d.append("\n") ;
    for (Skill s : skillSet()) {
      d.append("\n  "+s.name+" "+((int) traitLevel(s))) ;
    }
    d.append("\n") ;
    for (Trait t : personality()) {
      d.append("\n  "+Trait.descriptionFor(t, traitLevel(t))) ;
    }
    d.append("\n") ;
    for (Trait t : physique()) {
      d.append("\n  "+Trait.descriptionFor(t, traitLevel(t))) ;
    }
  }
}














