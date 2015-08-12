/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Adapt this to arbitrary species (including humans.)

public class SeedTailoring extends Plan {
  
  /**  Data, fields, constructors, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final public static float
    DESIRED_SAMPLES = 5,
    SEED_DAYS_DECAY = 5;
  
  final Venue lab;
  final Species species;
  final Item seedType;
  
  
  public SeedTailoring(Actor actor, Venue lab, Species s) {
    super(actor, lab, MOTIVE_JOB, NO_HARM);
    this.lab = lab;
    this.species = s;
    this.seedType = Item.asMatch(GENE_SEED, s);
  }
  
  
  public SeedTailoring(Session s) throws Exception {
    super(s);
    lab     = (Venue  ) s.loadObject();
    species = (Species) s.loadObject();
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
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final SeedTailoring t = (SeedTailoring) p;
    return t.species == this.species;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == lab
    );
    //  TODO:  USE THE PLAN-UTILS METHOD HERE
    final float priority = Nums.clamp(
      ROUTINE + (lab.structure.upgradeLevel(species) / 2f),
      0, URGENT
    );
    if (report) I.say("\nSeed-tailoring priority for "+actor+" is "+priority);
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && (
      I.talkAbout == actor || I.talkAbout == lab
    );
    if (report) I.say("\nGetting next seed-tailoring step for "+actor);
    
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
  
  
  private static float numSamples(Venue lab) {
    float samples = 0;
    for (Item i : lab.stocks.matches(SAMPLES)) {
      if (i.refers == Flora.WILD_FLORA) samples += Nums.max(1, i.amount);
    }
    return samples;
  }
  
  
  public static float needForSamples(Venue lab) {
    return 1 - (numSamples(lab) / DESIRED_SAMPLES);
  }
  
  
  public boolean actionTailorGenes(Actor actor, Venue lab) {
    final boolean report = stepsVerbose && (
      I.talkAbout == actor || I.talkAbout == lab
    );
    //
    //  Okay.  We boost the max/min quality based on the upgrades available at
    //  the lab.
    final Traded yield = Crop.yieldType(species);
    final int upgrade = Nums.clamp(lab.structure.upgradeLevel(yield), 3);
    final int minLevel = upgrade - 1, maxLevel = upgrade + 1;
    //
    //  There's also a partial bonus based on the quality of samples collected,
    //  and a larger bonus based on the skill of the gene-tailor.  If neither
    //  of those work out, then the tailoring-attempt fails.
    float sampleBonus = numSamples(lab) / DESIRED_SAMPLES, skillCheck = -0.5f;
    skillCheck += actor.skills.test(GENE_CULTURE, MODERATE_DC , 1) ? 1 : 0;
    skillCheck += actor.skills.test(CULTIVATION , DIFFICULT_DC, 1) ? 1 : 0;
    if (skillCheck + sampleBonus <= 0) return false;
    //
    //  The final quality of the result depends on the sum of the sample-bonus
    //  and skill-bonus, contrained by the upgrades available at the lab-
    int quality = Nums.round((skillCheck + sampleBonus) * 2, 1, false);
    quality = (int) Nums.clamp(quality, minLevel, maxLevel);
    
    Item seed = seedType;
    seed = Item.withQuality(seed, (int) Nums.clamp(quality, 0, 5));
    seed = Item.withAmount(seed, 0.1f);
    lab.stocks.addItem(seed);
    
    if (report) I.reportVars(
      "\nAttempted seed-tailoring", "  ",
      "Food yield "     , yield,
      "Upgrade level "  , upgrade,
      "Min/max quality ", minLevel+"/"+maxLevel,
      "Sample bonus"    , sampleBonus,
      "Skill check"     , skillCheck,
      "Final quality"   , quality,
      "Current stocks"  , lab.stocks.matchFor(seedType)
    );
    return true;
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


