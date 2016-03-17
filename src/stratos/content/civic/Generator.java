/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.Text;
import stratos.user.*;
import stratos.user.notify.MessageTopic;
import stratos.util.*;
import static stratos.game.actors.Condition.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.actors.Backgrounds.*;



public class Generator extends Venue {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    Generator.class, "generator_model",
    "media/Buildings/artificer/reactor.png", 3.75f, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Generator.class, "generator_icon",
    "media/GUI/Buttons/reactor_button.gif"
  );
  
  final static float
    MELTDOWN_RISK_PER_DAY = 0.2f;
  
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
    "Generator", Target.TYPE_ENGINEER, ICON,
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
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.TWO_LEVELS, null,
      new Object[] { 10, FIELD_THEORY, 10, ASSEMBLY },
      750, 1000
    ),
    WASTE_PROCESSING = new Upgrade(
      "Waste Processing",
      "Reduces the rate at which "+FUEL_RODS+" are consumed and ameliorates "+
      "pollution.",
      150, Upgrade.TWO_LEVELS,
      LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, FIELD_THEORY, 5, ASSEMBLY
    ),
    AUTOMATED_SENSORS = new Upgrade(
      "Automated Sensors",
      "Reduces the likelihood of meltdown or sabotage, even when staff are "+
      "not present.",
      500, Upgrade.SINGLE_LEVEL,
      LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      15, FIELD_THEORY, 10, ASSEMBLY
    ),
    FUSION_CONTAINMENT = new Upgrade(
      "Fusion Containment",
      "Increases "+POWER+" output while limiting pollution and decreasing the "+
      "severity of any meltdowns.",
      500, Upgrade.TWO_LEVELS,
      new Upgrade[] { AUTOMATED_SENSORS, LEVELS[1] }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      15, FIELD_THEORY, 15, ASSEMBLY
    ),
    MATTER_INVERSION = new Upgrade(
      "Matter Inversion",
      "Increases your stockpile of "+ANTIMASS+", a volatile energy source "+
      "essential to space travel and military offensives.",
      450, Upgrade.THREE_LEVELS,
      new Upgrade[] { WASTE_PROCESSING, AUTOMATED_SENSORS }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      20, FIELD_THEORY, 15, ASSEMBLY
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
    if (m != null) choice.add(m.setBonusFrom(this, true, MATTER_INVERSION));
    
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
    final int FB = structure.upgradeLevel(AUTOMATED_SENSORS);
    diagnoseDC -= FB * 5;

    final Action a = actor.currentAction();
    boolean success = true;
    if (Rand.yes()) {
      success &= actor.skills.test(FIELD_THEORY, diagnoseDC, 0.5f, a);
      success &= actor.skills.test(CHEMISTRY, 5, 0.5f, a);
    }
    else {
      success &= actor.skills.test(ASSEMBLY, diagnoseDC, 0.5f, a);
      success &= actor.skills.test(FIELD_THEORY, 5, 0.5f, a);
    }
    if (success) {
      meltdown -= (1f + FB) / Stage.STANDARD_DAY_LENGTH;
      if (meltdown <= 0) meltdown = 0;
      if (verbose) I.say("Repairing core, meltdown level: "+meltdown);
    }
    return true;
  }
  
  
  public int numPositions(Background v) {
    final int level = structure.mainUpgradeLevel();
    if (v == CORE_TECHNICIAN) return level + 1;
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
    powerOutput *= (2f + structure.upgradeLevel(FUSION_CONTAINMENT)) / 2;
    //
    //  TODO:  Load fuel into the core gradually- (make a supervision task.)
    final Item fuel = Item.withAmount(FUEL_RODS, fuelConsumed);
    if (stocks.hasItem(fuel)) stocks.removeItem(fuel);
    else powerOutput /= 2;
    stocks.forceDemand(POWER, 0, powerOutput);
    //
    //  Update demand for raw materials-
    stocks.forceDemand(FUEL_RODS, 5, 0);
    //
    //  Output pollution-
    int pollution = 10;
    pollution -= structure.upgradeLevel(WASTE_PROCESSING) * 2;
    pollution -= structure.upgradeLevel(FUSION_CONTAINMENT);
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  private float meltdownChance() {
    float chance = 1.5f - structure.repairLevel();
    chance *= 1 + (stocks.production(POWER) / 20f);
    if (stocks.amountOf(ANTIMASS) == 0) chance /= 5;
    chance /= (1f + structure.upgradeLevel(FUSION_CONTAINMENT));
    return chance;
  }
  
  
  private void checkMeltdownAdvance() {
    if ((! structure.intact()) && meltdown == 0) return;
    final float oldMelt = meltdown;
    
    float chance = meltdownChance();
    chance -= structure.upgradeLevel(AUTOMATED_SENSORS) / 3f;
    if (staff.manning() == 0           ) chance *= 2;
    if (stocks.amountOf(FUEL_RODS) == 0) chance /= 5;
    chance *= MELTDOWN_RISK_PER_DAY / Stage.STANDARD_DAY_LENGTH;

    meltdown += chance;
    
    float breakdownRisk = meltdown / Stage.STANDARD_DAY_LENGTH;
    if (Rand.num() < breakdownRisk) {
      final float damage = chance * structure.maxIntegrity() * Rand.num();
      structure.takeDamage(damage);
      structure.setBurning(true);
    }
    if (oldMelt < 0.5f && meltdown >= 0.5f) {
      TOPIC_MELTDOWN_RISK.dispatchMessage("Meltdown Risk", base, this);
    }
    if (meltdown >= 1) {
      performMeltdown();
      TOPIC_MELTDOWN.dispatchMessage("MELTDOWN!", base, this);
    }
  }
  
  
  public void setAsDestroyed(boolean salvaged) {
    if (! salvaged) performMeltdown();
    super.setAsDestroyed(salvaged);
  }
  
  
  protected void performMeltdown() {
    final int safety = 1 + structure.upgradeLevel(FUSION_CONTAINMENT);
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
    final StageTerrain terrain = world.terrain();
    //
    //  Then, deal with all the surrounding terrain-
    for (Tile t : world.tilesIn(area, true)) {
      final float dist = (Spacing.distance(t, this) - 2) / (maxRange - 2);
      if (dist > 1) continue;
      //
      //  Change the underlying terrain type-
      if (Rand.num() < 1 - dist) {
        if (Rand.index(10) != 0 && Rand.num() < (0.5f - dist)) {
          terrain.setHabitat(t, Habitat.CURSED_EARTH);
        }
        else terrain.setHabitat(t, Habitat.BARRENS);
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
      world.ephemera.addGhost(null, blastFX.scale, blastFX, 2.0f, 1);
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
      if ((Rand.num() * bulk) < damage / 10) e.setAsDestroyed(false);
    }
  }
  
  
  public void setMeltdown(float melt) {
    meltdown = melt;
  }
  
  
  
  /**  Rendering and interface-
    */
  final public static MessageTopic
    TOPIC_MELTDOWN_RISK = new MessageTopic(
      "topic_meltdown_risk", true, Generator.class
    ) {
      protected void configMessage(final BaseUI UI, Text d, Object... args) {
        final Generator at = (Generator) args[0];
        d.appendAll(
          "There is a significant risk of a core meltdown at ", at, ".  ",
          "Perhaps you should assign more staff to the plant, install safety "+
          "upgrades, or consider a decomissioning?"
        );
      }
    },
    TOPIC_MELTDOWN = new MessageTopic(
      "topic_meltdown", true, Generator.class
    ) {
      protected void configMessage(final BaseUI UI, Text d, Object... args) {
        final Generator at = (Generator) args[0];
        d.appendAll(
          "A core meltdown has occured at ", at, "!  There may be casualties ",
          "and lingering radiation effects- zoom to the site to find out."
        );
      }
    };
  
  
  protected Traded[] goodsToShow() {
    return new Traded[] { FUEL_RODS };
  }
  
  
  public String helpInfo() {
    String help = BLUEPRINT.description;
    
    if (inWorld() && structure.intact()) {
      if (stocks.relativeShortage(FUEL_RODS, false) > 0) {
        help = "Power output will be limited without additional "+FUEL_RODS+".";
      }
      final int nC = CORE_DESC.length;
      final String descC = CORE_DESC[Nums.clamp((int) (meltdown * nC), nC)];
      help +="\n  Core condition: "+descC;
    }
    return help;
  }
}





