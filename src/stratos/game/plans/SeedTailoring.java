/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



//  TODO:  Adapt this to arbitrary species (including humans.)

public class SeedTailoring extends Plan {
  
  /**  Data, fields, constructors, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false,
    fastTailor   = false;
  
  final public static float
    DESIRED_SAMPLES  = 5,
    SEED_DAYS_DECAY  = 5,
    SEED_TAILOR_TIME = Stage.STANDARD_HOUR_LENGTH,
    EGGS_TAILOR_TIME = Stage.STANDARD_DAY_LENGTH ,
    FAST_TAILOR_MULT = 20;
  final static int
    STAGE_INIT    = -1,
    STAGE_CULTURE =  0,
    STAGE_REARING =  1,
    STAGE_DONE    =  2;
  
  final Venue lab;
  final Species species;
  final Item seedType;
  
  private float speedMult = 1.0f;
  private int stage = STAGE_INIT;
  
  
  public SeedTailoring(Actor actor, Venue lab, Species s) {
    super(actor, lab, MOTIVE_JOB, NO_HARM);
    this.lab = lab;
    this.species = s;
    this.seedType = Item.asMatch(GENE_SEED, s);
  }
  
  
  public SeedTailoring(Session s) throws Exception {
    super(s);
    lab       = (Venue  ) s.loadObject();
    species   = (Species) s.loadObject();
    stage     = s.loadInt  ();
    speedMult = s.loadFloat();
    seedType  = Item.asMatch(GENE_SEED, species);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(lab      );
    s.saveObject(species  );
    s.saveInt   (stage    );
    s.saveFloat (speedMult);
  }
  
  
  public Plan copyFor(Actor other) {
    return new SeedTailoring(other, lab, species);
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final SeedTailoring t = (SeedTailoring) p;
    return t.species == this.species;
  }
  
  
  public void setSpeedMult(float mult) {
    this.speedMult = mult;
  }
  
  
  
  /**  Query methods used externally-
    */
  final public static Traded SAMPLE_TYPES[] = { SAMPLES };
  
  
  private static Species referred(Item i) {
    if (i.refers instanceof Species) return (Species) i.refers;
    return null;
  }
  
  
  public static float numSamples(Venue lab) {
    float samples = 0;
    for (Item i : lab.stocks.matches(SAMPLES)) {
      final Species s = referred(i);
      if (s != null && ! s.domesticated) samples++;
    }
    return samples;
  }
  
  
  public static boolean hasSample(Owner owner, Species s) {
    if (owner == null) return false;
    for (Item i : owner.inventory().matches(SAMPLES)) {
      if (s == referred(i)) return true;
    }
    return false;
  }
  
  
  public static Item sampleFrom(Flora flora) {
    return Item.with(SAMPLES, flora.species(), 1, Item.AVG_QUALITY);
  }
  
  
  public static float sampleValue(Flora flora, Actor actor, Venue depot) {
    if (flora.species().domesticated     ) return -1  ;
    if (hasSample(actor, flora.species())) return -1  ;
    if (hasSample(depot, flora.species())) return 0.5f;
    return 1;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = (
      I.talkAbout == actor || I.talkAbout == lab
    ) && evalVerbose;
    
    float lack = 1f - lab.stocks.amountOf(seedType);
    if (lack <= 0) return -1;
    if (hasBegun()) lack = 1;
    
    //  TODO:  USE THE PLAN-UTILS METHOD HERE?
    final Object yield = species.nutrients(0)[0].type;
    final float priority = (Nums.clamp(
      ROUTINE + (lab.structure.upgradeLevel(yield) * CASUAL / 2f),
      0, URGENT
    ) * lack) + motiveBonus();
    
    if (report) I.say("\nSeed-tailoring priority for "+actor+" is "+priority);
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && (
      I.talkAbout == actor || I.talkAbout == lab
    );
    if (report) I.say("\nGetting next seed-tailoring step for "+actor);
    
    if (stage == STAGE_DONE) return null;
    
    if (lab.stocks.amountOf(seedType) < 1) {
      if (report) I.say("  Will begin gene-tailoring");
      final Action prepare = new Action(
        actor, lab,
        this, "actionTailorGenes",
        Action.STAND, "Tailoring genes"
      );
      return prepare;
    }
    
    return null;
  }
  
  
  public boolean actionTailorGenes(Actor actor, Venue lab) {
    final boolean report = (
      I.talkAbout == actor || I.talkAbout == lab
    ) && stepsVerbose;
    //
    //  Okay.  We boost the max/min quality based on the upgrades available at
    //  the lab.
    final Stage  world    = actor.world();
    final Object yield    = species.nutrients(0)[0].type;
    final int    upgrade  = Nums.clamp(lab.structure.upgradeLevel(yield), 3);
    final int    minLevel = upgrade - 1, maxLevel = upgrade + 1;
    //
    //  There's also a partial bonus based on the quality of samples collected,
    //  and a larger bonus based on the skill of the gene-tailor.  If neither
    //  of those work out, then the tailoring-attempt fails.
    final Action a = action();
    float sampleBonus = numSamples(lab) / DESIRED_SAMPLES, skillCheck = -0.5f;
    sampleBonus += minLevel / 2f;
    skillCheck += actor.skills.test(BIOLOGY    , MODERATE_DC , 1, a) ? 1 : 0;
    skillCheck += actor.skills.test(CULTIVATION, DIFFICULT_DC, 1, a) ? 1 : 0;
    //
    //  The final quality of the result depends on the sum of the sample-bonus
    //  and skill-bonus, contrained by the upgrades available at the lab-
    int quality = Nums.ceil((skillCheck + sampleBonus) * 2);
    quality = (int) Nums.clamp(quality, minLevel, maxLevel);
    
    float duration = species.animal() ? EGGS_TAILOR_TIME : SEED_TAILOR_TIME;
    duration /= speedMult;
    if (species.animal()) duration *= species.metabolism() / 2;
    if (fastTailor) duration = Nums.max(1, duration / FAST_TAILOR_MULT);
    
    Item seed = seedType;
    seed = Item.withQuality(seed, quality);
    seed = Item.withAmount(seed, 1f / duration);
    lab.stocks.addItem(seed);
    
    
    //  TODO:  You need an additional step for release, somewhere away from the
    //  lab...
    
    //
    //  If we actually complete the task, there's special handling for animal
    //  species (and predators in particular.)
    if (species.animal() && lab.stocks.amountOf(seedType) >= 1) {
      
      Base belongs = species.predator() ? actor.base() : Base.wildlife(world);
      lab.stocks.removeMatch(seed);
      Fauna reared = (Fauna) species.sampleFor(belongs);
      reared.health.setupHealth(0, seed.quality / Item.MAX_QUALITY, 0);

      if (species.predator()) reared.setAsDomesticated(actor);
      reared.relations.setupRelation(actor                 , 0.50f, 0);
      reared.relations.setupRelation(actor.base().faction(), 0.25f, 0);
      actor.relations.incRelation(reared, 0.5f, 0.5f, 0);
      
      lab.stocks.addItem(Item.with(SAMPLES, species, 1, seed.quality));
      reared.enterWorldAt(lab.mainEntrance(), world);
      stage = STAGE_DONE;
    }
    
    if (report) I.reportVars(
      "\nAttempted seed-tailoring", "  ",
      "Food yield: "    , yield,
      "Upgrade level "  , upgrade,
      "Min/max quality ", minLevel+"/"+maxLevel,
      "Sample bonus"    , sampleBonus,
      "Prep duration"   , duration,
      "Skill check"     , skillCheck,
      "Final quality"   , quality,
      "Current stocks"  , lab.stocks.matchFor(seedType)
    );
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (species.animal()) {
      d.appendAll("Rearing ", species, " at ", lab);
    }
    else {
      d.appendAll("Preparing ", species, " at ", lab);
    }
  }
}







