/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Conditions.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class Generator extends Venue {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    Generator.class, "media/Buildings/artificer/reactor.png", 4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Generator.class, "media/GUI/Buttons/reactor_button.gif"
  );
  final static String RISK_DESC[] = {
    "Negligible",
    "Minimal",
    "Low",
    "Moderate",
    "High",
    "Serious",
    "Critical"
  };
  final static String CORE_DESC[] = {
    "Secure",
    "Steady",
    "Stable",
    "Volatile",
    "Unstable",
    "Critical",
    "MELTDOWN"
  };
  
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Generator.class, "generator",
    "Generator", UIConstants.TYPE_ENGINEER, ICON,
    "The Generator provides copious "+POWER+" along with "+ANTIMASS+" output, "+
    "but can become an explosive liability.",
    4, 2, Structure.IS_NORMAL, Owner.TIER_FACILITY, 300, 10,
    POWER, ANTIMASS, CORE_TECHNICIAN
  );
  
  final public static Conversion
    FUEL_RODS_TO_ANTIMASS = new Conversion(
      BLUEPRINT, "isotopes_to_antimass",
      4, FUEL_RODS, TO, 1, ANTIMASS,
      MODERATE_DC, CHEMISTRY, STRENUOUS_DC, FIELD_THEORY
    ),
    FUEL_RODS_TO_POWER = new Conversion(
      BLUEPRINT, "isotopes_to_power",
      1, FUEL_RODS, TO, 25, POWER
    );
  
  
  private float meltdown = 0.0f;
  

  public Generator(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_HOURS);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public Generator(Session s) throws Exception {
    super(s);
    meltdown = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(meltdown);
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    WASTE_PROCESSING = new Upgrade(
      "Waste Processing",
      "Reduces the rate at which "+FUEL_RODS+" are consumed and ameliorates "+
      "pollution.",
      150,
      Upgrade.THREE_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    REACTIVE_CONTAINMENT = new Upgrade(
      "Reactive Containment",
      "Reduces the likelihood of meltdown occuring when the reactor is "+
      "damaged or under-supervised, and the risk of sabotage or infiltration.",
      200,
      Upgrade.THREE_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    COLD_FUSION = new Upgrade(
      "Cold Fusion",
      "Increases "+POWER+" output while limiting pollution and decreasing the "+
      "severity of any meltdowns.",
      500,
      Upgrade.THREE_LEVELS, REACTIVE_CONTAINMENT, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    PARTICLE_CIRCUIT = new Upgrade(
      "Particle Circuit",
      "Facilitates conversion of "+FUEL_RODS+" to "+ANTIMASS+", a volatile "+
      "energy source essential to space travel and atomics stockpiles.",
      450, Upgrade.THREE_LEVELS, WASTE_PROCESSING, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    )
  ;
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null;
    //
    //  First and foremost, check to see whether a meltdown is in progress, and
    //  arrest it if possible:
    final Choice choice = new Choice(actor);
    if (meltdown > 0) {
      final Action check = new Action(
        actor, this,
        this, "actionCheckMeltdown",
        Action.LOOK,
        meltdown < 0.5f ? "Correcting core condition" : "Containing Meltdown!"
      );
      check.setPriority(Plan.ROUTINE + (meltdown * Plan.PARAMOUNT));
      choice.add(check);
    }
    if (! staff.onShift(actor)) return choice.pickMostUrgent();
    //
    //  Then check to see if anything needs manufacture-
    Manufacture m = stocks.nextManufacture(actor, FUEL_RODS_TO_ANTIMASS);
    if (m != null) choice.add(m.setBonusFrom(this, true, PARTICLE_CIRCUIT));
    
    for (Item ordered : stocks.specialOrders()) {
      final Manufacture mO = new Manufacture(actor, this, ordered);
      mO.setBonusFrom(this, true, WASTE_PROCESSING);
      choice.add(mO);
    }
    //
    //  Failing that, just keep the place in order-
    choice.add(new Repairs(actor, this, true));
    choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  public boolean actionCheckMeltdown(Actor actor, Generator reactor) {
    float diagnoseDC = 5 + ((1 - meltdown) * 20);
    final int FB = structure.upgradeLevel(REACTIVE_CONTAINMENT);
    diagnoseDC -= FB * 5;
    
    boolean success = true;
    if (Rand.yes()) {
      success &= actor.skills.test(FIELD_THEORY, diagnoseDC, 0.5f);
      success &= actor.skills.test(CHEMISTRY, 5, 0.5f);
    }
    else {
      success &= actor.skills.test(ASSEMBLY, diagnoseDC, 0.5f);
      success &= actor.skills.test(SHIELD_AND_ARMOUR, 5, 0.5f);
    }
    if (success) {
      meltdown -= (1f + FB) / Stage.STANDARD_DAY_LENGTH;
      if (meltdown <= 0) meltdown = 0;
      if (verbose) I.say("Repairing core, meltdown level: "+meltdown);
    }
    return true;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == CORE_TECHNICIAN) return nO + 2;
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    checkMeltdownAdvance();
    if (! structure.intact()) return;
    //
    //  Calculate output of power and consumption of fuel-
    float fuelConsumed = 1f / Stage.STANDARD_DAY_LENGTH, powerOutput = 25;
    fuelConsumed *= 2 / (2f + structure.upgradeLevel(WASTE_PROCESSING));
    powerOutput *= (2f + structure.upgradeLevel(COLD_FUSION)) / 2;
    //
    //  TODO:  Load fuel into the core gradually- (make a supervision task.)
    final Item fuel = Item.withAmount(FUEL_RODS, fuelConsumed);
    if (stocks.hasItem(fuel)) stocks.removeItem(fuel);
    else powerOutput /= 2;
    stocks.forceDemand(POWER, powerOutput, true);
    //
    //  Update demand for raw materials-
    stocks.forceDemand(FUEL_RODS, 5, false);
    //
    //  Output pollution-
    int pollution = 10;
    pollution -= structure.upgradeLevel(WASTE_PROCESSING) * 2;
    pollution -= structure.upgradeLevel(COLD_FUSION);
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  private float meltdownChance() {
    float chance = 1.5f - structure.repairLevel();
    chance *= 1 + (stocks.demandFor(POWER) / 20f);
    if (stocks.amountOf(ANTIMASS) == 0) chance /= 5;
    chance /= (1f + structure.upgradeLevel(REACTIVE_CONTAINMENT));
    return chance;
  }
  
  
  private void checkMeltdownAdvance() {
    if ((! structure.intact()) && meltdown == 0) return;
    float chance = meltdownChance() / Stage.STANDARD_DAY_LENGTH;
    chance += meltdown / 10f;
    if (staff.manning() > 0) chance /= 2;
    if (Rand.num() < chance) {
      final float melt = 0.1f * Rand.num();
      meltdown += melt;
      if (verbose) I.say("  MELTDOWN LEVEL: "+meltdown);
      if (meltdown >= 1) performMeltdown();
      final float damage = melt * meltdown * 2 * Rand.num();
      structure.takeDamage(damage);
      structure.setBurning(true);
    }
  }
  
  
  public void onDestruction() {
    performMeltdown();
    super.onDestruction();
  }
  
  
  protected void performMeltdown() {
    final int safety = 1 + structure.upgradeLevel(COLD_FUSION);
    //
    //  Pollute the surroundings but cut back the meltdown somewhat-
    float radiationVal = (125 / safety) - 25;
    radiationVal *= meltdown * Rand.avgNums(3) * 2;
    //
    //  TODO:  You need some method to represent temporary contamination...
    //world.ecology().impingeSqualor(radiationVal, this, false);
    meltdown /= 1 + (Rand.num() * safety);
    //
    //  Determine the range and severity of the explosion-
    final int maxRange = 1 + (int) (radiationVal * 2 / 25f);
    final float maxDamage = (5 - safety) * (5 - safety) * 20;
    final Box2D area = this.area(null).expandBy(maxRange);
    final Batch <Element> inRange = new Batch <Element> ();
    //
    //  Then, deal with all the surrounding terrain-
    for (Tile t : world.tilesIn(area, true)) {
      final float dist = (Spacing.distance(t, this) - 2) / (maxRange - 2);
      if (dist > 1) continue;
      //
      //  Change the underlying terrain type-
      if (Rand.num() < 1 - dist) {
        if (Rand.index(10) != 0 && Rand.num() < (0.5f - dist)) {
          world.terrain().setHabitat(t, Habitat.CURSED_EARTH);
        }
        else world.terrain().setHabitat(t, Habitat.BARRENS);
      }
      //
      //  And deal damage to nearby objects-
      markForDamage(t.above(), inRange);
      for (Mobile m : t.inside()) markForDamage(m, inRange);
    }
    markForDamage(this, inRange);
    for (Element e : inRange) {
      e.flagWith(null);
      doDamageTo(e, maxDamage, radiationVal, maxRange);
    }
    //
    //  Add explosion FX-
    int multFX = 3; while (multFX -- > 0) {
      final PlaneFX blastFX = (PlaneFX) BuildingSprite.BLAST_MODEL.makeSprite();
      blastFX.scale = this.size * (multFX + 1) / 3f;
      blastFX.timeScale = 2.0f;
      this.viewPosition(blastFX.position);
      world.ephemera.addGhost(null, blastFX.scale, blastFX, 2.0f);
    }
  }
  
  
  private void markForDamage(Element e, Batch <Element> inRange) {
    if (e == null || e.flaggedWith() != null) return;
    e.flagWith(inRange);
    inRange.add(e);
    if (e instanceof Boarding) for (Mobile m : ((Boarding) e).inside()) {
      markForDamage(m, inRange);
    }
  }
  
  
  private void doDamageTo(
    Element e, float maxDamage, float radiation, float maxRange
  ) {
    final float dist = Spacing.distance(this, e) / maxRange;
    final float damage = maxDamage * (1 - dist);

    if (e instanceof Wreckage) return;
    else if (e instanceof Placeable) {
      I.say("Doing "+damage+" to "+e);
      ((Placeable) e).structure().takeDamage(damage);
    }
    else if (e instanceof Actor) {
      final Actor a = (Actor) e;
      a.health.takeInjury(damage / 2f, true);
      a.traits.setLevel(POISONING, radiation / 25f);
      if (Rand.index(100) < radiation) a.traits.incLevel(CANCER  , Rand.num());
      if (Rand.index(100) < radiation) a.traits.incLevel(MUTATION, Rand.num());
    }
    else {
      final float bulk = e.radius() * e.radius() * 4 * (e.height() + 0.5f);
      if ((Rand.num() * bulk) < damage / 10) e.setAsDestroyed();
    }
  }
  
  
  public void setMeltdown(float melt) {
    meltdown = melt;
  }
  
  
  
  /**  Rendering and interface-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { FUEL_RODS };
  }
  
  
  public String helpInfo() {
    String help = BLUEPRINT.description;
    
    if (inWorld() && structure.intact()) {
      if (! stocks.hasEnough(FUEL_RODS)) {
        help = "Power output will be limited without additional "+FUEL_RODS+".";
      }
      final int nC = CORE_DESC.length;
      final String descC = CORE_DESC[Nums.clamp((int) (meltdown * nC), nC)];
      help +="\n  Core condition: "+descC;
    }
    return help;
  }
}

