/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class SeedTailoring extends Plan {
  
  
  
  /**  Data, fields, constructors, setup and save/load functions-
    */
  final Venue lab;
  final Species species;
  final Item cropType, seedType;
  
  
  public SeedTailoring(Actor actor, Venue lab, Species s) {
    super(actor, lab, true, NO_HARM);
    this.lab = lab;
    this.species = s;
    this.cropType = Item.asMatch(SAMPLES  , s);
    this.seedType = Item.asMatch(GENE_SEED, s);
  }
  
  
  public SeedTailoring(Session s) throws Exception {
    super(s);
    lab     = (Venue  ) s.loadObject();
    species = (Species) s.loadObject();
    this.cropType = Item.asMatch(SAMPLES  , species);
    this.seedType = Item.asMatch(GENE_SEED, species);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(lab    );
    s.saveObject(species);
  }
  
  
  public Plan copyFor(Actor other) {
    return new SeedTailoring(other, lab, species);
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false;
    final SeedTailoring t = (SeedTailoring) p;
    return t.species == this.species;
  }
  
  
  
  /**  Obtaining and evaluating targets-
    */
  protected float getPriority() {
    //final EcologistStation station = nursery.belongs;
    return Visit.clamp(
      ROUTINE + (lab.structure.upgradeBonus(species) / 2f),
      0, URGENT
    );
  }
  
  
  
  /**  Step implementation/sequence-
    */
  protected Behaviour getNextStep() {
    //
    //  If the laboratory has adequate stocks, just return.
    if (lab.stocks.amountOf(cropType) > 1) return null;
    //
    //  If the nursery has enough of the seed type, culture it-
    if (lab.stocks.amountOf(seedType) > 1) {
      final Action culture = new Action(
        actor, lab,
        this, "actionCultureSeed",
        Action.STAND, "Culturing seed"
      );
      return culture;
    }
    //
    //  Otherwise, prepare the basic gene seed-
    if (lab.stocks.amountOf(GENE_SEED) > 1) {
      final Action prepare = new Action(
        actor, lab,
        this, "actionTailorGenes",
        Action.STAND, "Tailoring genes"
      );
      return prepare;
    }
    ///I.sayAbout(actor, "No next action for seed tailoring.");
    //
    //  Otherwise, if possible, get gene samples from nearby flora-
    //  TODO:  This is currently handled by the station.  Change that?
    return null;
  }
  
  
  private float cultureTest(int DC, Venue lab) {
    final Traded yield = Crop.yieldType(species);
    float skillRating = 5;
    if (! actor.skills.test(GENE_CULTURE, DC, 5.0f)) skillRating /= 2;
    if (! actor.skills.test(CULTIVATION , DC, 5.0f)) skillRating /= 2;
    skillRating += lab.structure.upgradeBonus(yield);
    skillRating *= (1 - lab.stocks.shortagePenalty(POWER));
    return skillRating;
  }
  
  
  public boolean actionCultureSeed(Actor actor, Venue lab) {
    final Batch <Item> seedMatch = lab.stocks.matches(seedType);
    if (seedMatch.size() == 0) return false;
    final Item seed = seedMatch.atIndex(0);
    final float successChance = 0.5f;
    final float skillRating = cultureTest(ROUTINE_DC, lab);
    //
    //  Average quality with the seed-tailoring result, store and return-
    if (Rand.num() < successChance * skillRating) {
      Item crop = cropType;
      crop = Item.withQuality(crop, (int) ((seed.quality + skillRating) / 2));
      crop = Item.withAmount(crop, 0.1f);
      lab.stocks.addItem(crop);
      return true;
    }
    return false;
  }
  
  
  public boolean actionTailorGenes(Actor actor, Venue lab) {
    //
    //  Calculate odds of success based on the skill of the researcher-
    final float successChance = 0.2f;
    final float skillRating = cultureTest(MODERATE_DC, lab);
    //
    //  Use the seed in the lab to create seed for the different crop types.
    if (Rand.num() < successChance * skillRating) {
      float quality = skillRating * Rand.avgNums(2);
      
      Item seed = seedType;
      seed = Item.withQuality(seed, (int) Visit.clamp(quality, 0, 5));
      seed = Item.withAmount(seed, 0.1f);
      lab.stocks.addItem(seed);
      
      return true;
    }
    return false;
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






