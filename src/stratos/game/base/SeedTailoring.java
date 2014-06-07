/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.building.* ;
import stratos.game.common.* ;
import stratos.game.actors.* ;
import stratos.game.maps.*;
import stratos.util.* ;



public class SeedTailoring extends Plan implements Economy {
  
  
  
  /**  Data, fields, constructors, setup and save/load functions-
    */
  final Plantation nursery ;
  final Species species ;
  final Item cropType, seedType ;
  
  
  SeedTailoring(Actor actor, Plantation plantation, Species s) {
    super(actor, plantation.belongs) ;
    this.nursery = plantation ;
    this.species = s ;
    this.cropType = Item.asMatch(SAMPLES, s) ;
    this.seedType = Item.asMatch(GENE_SEED, s) ;
  }
  
  
  public SeedTailoring(Session s) throws Exception {
    super(s) ;
    nursery = (Plantation) s.loadObject() ;
    species = (Species) s.loadObject() ;
    this.cropType = Item.asMatch(SAMPLES, species) ;
    this.seedType = Item.asMatch(GENE_SEED, species) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(nursery) ;
    s.saveObject(species) ;
  }
  
  
  public Plan copyFor(Actor other) {
    return new SeedTailoring(other, nursery, species);
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final SeedTailoring t = (SeedTailoring) p ;
    return t.species == this.species ;
  }
  
  
  
  /**  Obtaining and evaluating targets-
    */
  protected float getPriority() {
    if (nursery.type != Plantation.TYPE_NURSERY) return 0 ;
    final BotanicalStation station = nursery.belongs ;
    
    final Service yield = Crop.yieldType(species) ;
    return Visit.clamp(
      ROUTINE + (station.structure.upgradeBonus(yield) / 2f),
      0, URGENT
    ) ;
  }
  
  
  
  /**  Step implementation/sequence-
    */
  protected Behaviour getNextStep() {
    if (nursery.type != Plantation.TYPE_NURSERY) return null ;
    //
    //  If the nursery has adequate stocks, just return.
    if (nursery.stocks.amountOf(cropType) > 1) return null ;
    final BotanicalStation station = nursery.belongs ;
    //
    //  If the nursery has enough of the crop type, deliver it-
    if (station.stocks.amountOf(cropType) > 1) {
      final Batch <Item> matches = station.stocks.matches(cropType);
      return new Delivery(matches, station, nursery);
    }
    //
    //  If the nursery has enough of the seed type, culture it-
    if (station.stocks.amountOf(seedType) > 1) {
      final Action culture = new Action(
        actor, station,
        this, "actionCultureSeed",
        Action.STAND, "Culturing seed"
      ) ;
      return culture ;
    }
    //
    //  Otherwise, prepare the basic gene seed-
    if (station.stocks.amountOf(GENE_SEED) > 1) {
      final Action prepare = new Action(
        actor, station,
        this, "actionTailorGenes",
        Action.STAND, "Tailoring genes"
      ) ;
      return prepare ;
    }
    ///I.sayAbout(actor, "No next action for seed tailoring.") ;
    //
    //  Otherwise, if possible, get gene samples from nearby flora-
    //  TODO:  This is currently handled by the station.  Change that?
    return null ;
  }
  
  
  private float cultureTest(int DC, BotanicalStation lab) {
    final Service yield = Crop.yieldType(species) ;
    float skillRating = 5 ;
    if (! actor.traits.test(GENE_CULTURE, DC, 5.0f)) skillRating /= 2 ;
    if (! actor.traits.test(CULTIVATION , DC, 5.0f)) skillRating /= 2 ;
    skillRating += lab.structure.upgradeBonus(yield) ;
    skillRating *= (1 - lab.stocks.shortagePenalty(POWER)) ;
    return skillRating ;
  }
  
  
  public boolean actionCultureSeed(Actor actor, BotanicalStation lab) {
    final Batch <Item> seedMatch = lab.stocks.matches(seedType) ;
    if (seedMatch.size() == 0) return false ;
    final Item seed = seedMatch.atIndex(0) ;
    final float successChance = 0.5f ;
    final float skillRating = cultureTest(ROUTINE_DC, lab) ;
    //
    //  Average quality with the seed-tailoring result, store and return-
    if (Rand.num() < successChance * skillRating) {
      Item crop = cropType ;
      crop = Item.withQuality(crop, (int) ((seed.quality + skillRating) / 2)) ;
      crop = Item.withAmount(crop, 0.1f) ;
      lab.stocks.addItem(crop) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionTailorGenes(Actor actor, BotanicalStation lab) {
    //
    //  Calculate odds of success based on the skill of the researcher-
    final float successChance = 0.2f ;
    final float skillRating = cultureTest(MODERATE_DC, lab) ;
    //
    //  Use the seed in the lab to create seed for the different crop types.
    if (Rand.num() < successChance * skillRating) {
      float quality = skillRating * Rand.avgNums(2) ;
      
      Item seed = seedType ;
      seed = Item.withQuality(seed, (int) Visit.clamp(quality, 0, 5)) ;
      seed = Item.withAmount(seed, 0.1f) ;
      lab.stocks.addItem(seed) ;
      
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Tailoring seed")) {
      d.append(" at ");
      d.append(super.lastStepTarget());
    }
  }
}






