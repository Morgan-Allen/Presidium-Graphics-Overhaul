


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.wild.Wreckage;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Conditions.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class Reactor extends Venue {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  final static ModelAsset MODEL = CutoutModel.fromImage(
    Reactor.class, "media/Buildings/artificer/reactor.png", 4.0f, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Reactor.class, "media/GUI/Buttons/reactor_button.gif"
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
  
  private static boolean verbose = false;
  
  
  private float meltdown = 0.0f;
  

  public Reactor(Base base) {
    super(4, 2, Venue.ENTRANCE_EAST, base);
    structure.setupStats(
      300, 10, 300,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    personnel.setShiftType(SHIFTS_BY_HOURS);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public Reactor(Session s) throws Exception {
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
      "Reduces the rate at which fuel rods are consumed and ameliorates "+
      "pollution.",
      150,
      null, 1, null,
      Reactor.class, ALL_UPGRADES
    ),
    
    ISOTOPE_CONVERSION = new Upgrade(
      "Isotope Conversion",
      "Allows metal ores to be synthesised into fuel rods and facilitates "+
      "production of atomics.",
      350,
      null, 1, WASTE_PROCESSING,
      Reactor.class, ALL_UPGRADES
    ),
    
    FEEDBACK_MONITORS = new Upgrade(
      "Feedback Monitors",
      "Reduces the likelihood of meltdown occuring when the reactor is "+
      "damaged or under-supervised, and reduces the likelihood of sabotage or "+
      "infiltration.",
      200,
      null, 1, null,
      Reactor.class, ALL_UPGRADES
    ),
    
    //
    //  TODO:  Consider replacing this a boost to Shields generation?  Make
    //  Fusion confinement dependant on that?
    QUALIA_WAVEFORM_INTERFACE = new Upgrade(
      "Qualia Waveform Interface",
      "Allows reactor output to contribute slightly towards regeneration of "+
      "psi points and range of psyon abilities.",
      250,
      null, 1, FEEDBACK_MONITORS,
      Reactor.class, ALL_UPGRADES
    ),
    
    FUSION_CONFINEMENT = new Upgrade(
      "Fusion Confinement",
      "Increases power output while limiting pollution and decreasing the "+
      "severity of any meltdowns.",
      500,
      null, 1, FEEDBACK_MONITORS,
      Reactor.class, ALL_UPGRADES
    ),
    
    CORE_TECHNICIAN_STATION = new Upgrade(
      "Core Technician Station",
      "Core Technicians provide the expertise and vigilance neccesary to "+
      "monitor core output and manufacture atomics or antimass.",
      100,
      Backgrounds.CORE_TECHNICIAN, 1, null,
      Reactor.class, ALL_UPGRADES
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
      check.setPriority(Action.ROUTINE * (meltdown + 1));
      choice.add(check);
    }
    if (! personnel.onShift(actor)) return choice.pickMostUrgent();
    //
    //  Then check to see if anything needs manufacture-
    final Manufacture m = stocks.nextManufacture(actor, METALS_TO_FUEL);
    if (m != null && stocks.amountOf(ORES) >= 1) {
      m.checkBonus = 5 * structure.upgradeLevel(ISOTOPE_CONVERSION);
      choice.add(m);
    }
    
    for (Manufacture o : stocks.specialOrders()) {
      o.checkBonus = 5 * structure.upgradeLevel(ISOTOPE_CONVERSION);
      choice.add(o);
    }
    //
    //  Failing that, just keep the place in order-
    choice.add(new Supervision(actor, this));
    return choice.weightedPick();
  }
  
  
  public boolean actionCheckMeltdown(Actor actor, Reactor reactor) {
    float diagnoseDC = 5 + ((1 - meltdown) * 20);
    final int FB = structure.upgradeLevel(FEEDBACK_MONITORS);
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
    if (v == Backgrounds.CORE_TECHNICIAN) {
      return nO + 1;
    }
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    checkMeltdownAdvance();
    if (! structure.intact()) return;
    
    //  Calculate output of power and consumption of fuel-
    float fuelConsumed = 0.01f, powerOutput = 25;
    fuelConsumed *= 2 / (2f + structure.upgradeLevel(WASTE_PROCESSING));
    powerOutput *= (2f + structure.upgradeLevel(FUSION_CONFINEMENT)) / 2;
    
    final Item fuel = Item.withAmount(TOPES, fuelConsumed);
    if (stocks.hasItem(fuel)) stocks.removeItem(fuel);
    else powerOutput /= 5;
    
    ///I.say("POWER OUTPUT: "+powerOutput);
    structure.assignOutputs(Item.withAmount(POWER, powerOutput));
    
    //  Update demand for raw materials-
    stocks.forceDemand(TOPES, 5 + (powerOutput / 5f), Stocks.TIER_CONSUMER);
    if (structure.upgradeLevel(ISOTOPE_CONVERSION) > 0) {
      stocks.translateDemands(1, METALS_TO_FUEL, this);
    }
    //
    //  If possible, assist in recovery of psi points-
    final int PB = structure.upgradeLevel(QUALIA_WAVEFORM_INTERFACE);
    final Actor ruler = base().ruler();
    if (PB > 0 && ruler != null && ruler.aboard() instanceof Bastion) {
      ruler.health.gainConcentration(PB / 100f);
    }
    //
    //  Output pollution-
    int pollution = 10;
    pollution -= structure.upgradeLevel(WASTE_PROCESSING) * 2;
    pollution -= structure.upgradeLevel(FUSION_CONFINEMENT);
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  private float meltdownChance() {
    float chance = 1.5f - structure.repairLevel();
    chance *= 1 + (stocks.demandFor(POWER) / 20f);
    if (stocks.amountOf(ANTIMASS) == 0) chance /= 5;
    chance /= (1f + structure.upgradeLevel(FEEDBACK_MONITORS));
    return chance;
  }
  
  
  private void checkMeltdownAdvance() {
    if ((! structure.intact()) && meltdown == 0) return;
    float chance = meltdownChance() / Stage.STANDARD_DAY_LENGTH;
    chance += meltdown / 10f;
    if (isManned()) chance /= 2;
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
    final int safety = 1 + structure.upgradeLevel(FUSION_CONFINEMENT);
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
      markForDamage(t.onTop(), inRange);
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
    else if (e instanceof Installation) {
      I.say("Doing "+damage+" to "+e);
      ((Installation) e).structure().takeDamage(damage);
    }
    else if (e instanceof Actor) {
      final Actor a = (Actor) e;
      a.health.takeInjury(damage / 2f, true);
      a.traits.setLevel(POISONING, radiation / 25f);
      if (Rand.index(100) < radiation) a.traits.incLevel(CANCER, Rand.num());
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
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.CORE_TECHNICIAN };
  }
  
  
  public Traded[] services() {
    return new Traded[] { POWER, ANTIMASS };
  }
  
  
  
  /**  Rendering and interface-
    */
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    panel = super.configPanel(panel, UI);
    final Description d = panel.detail();
    
    if (panel.category() == CAT_STATUS) {
      final float risk = meltdownChance() + meltdown;
      final int nR = RISK_DESC.length;
      final String descR = RISK_DESC[Nums.clamp((int) (risk * nR), nR)];
      d.append("\n\n  Meltdown risk: "+descR);
      final int nC = CORE_DESC.length;
      final String descC = CORE_DESC[Nums.clamp((int) (meltdown * nC), nC)];
      d.append("\n  Core condition: "+descC);
    }
    
    return panel;
  }
  
  
  public String fullName() {
    return "Reactor";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "reactor");
  }
  
  
  public String helpInfo() {
    return
      "The Reactor provides a copious supply of power to your settlement "+
      "and is essential to manufacturing atomics or antimass, but can "+
      "also be a dangerous liability.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_ARTIFICER;
  }
}






