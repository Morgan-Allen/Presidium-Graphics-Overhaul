/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.actors.Backgrounds.*;



public class BotanicalStation extends HarvestVenue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    BotanicalStation.class, "media/GUI/Buttons/ecologist_station_button.gif"
  );
  final static ModelAsset
    STATION_MODEL = CutoutModel.fromImage(
      BotanicalStation.class, IMG_DIR+"botanical_station.png", 4, 2
    ),
    NURSERY_MODEL = CutoutModel.fromImage(
      BotanicalStation.class, IMG_DIR+"nursery.png", 2, 1
    );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    BotanicalStation.class, "ecologist_station",
    "Botanical Station", Target.TYPE_ECOLOGIST, ICON,
    "Botanical stations are centres for agriculture and forestry programs, "+
    "helping to secure food supplies and advance terraforming.",
    4, 2, Structure.IS_NORMAL | Structure.IS_ZONED,
    Owner.TIER_FACILITY, 150, 3,
    CARBS, GREENS, POWER, CULTIVATOR
  );
  
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.TWO_LEVELS, null,
      new Object[] { 10, CULTIVATION, 0, XENOZOOLOGY },
      450,
      600//,
      //750
    ),
    MONOCULTURE = new Upgrade(
      "Monoculture",
      "Improves cereal yields, which provide "+CARBS+".  Cereals provide "+
      "cheap, abundant calories, but cannot provide a complete diet.",
      100, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, CARBS, 10, CULTIVATION
    ),
    FLORAL_CULTURE = new Upgrade(
      "Floral Culture",
      "Improves broadleaf yields, which provide "+GREENS+", a luxury export. "+
      "Also helps to advances forestry programs.",
      150, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, GREENS, 10, CULTIVATION
    ),
    SOLAR_BANKS = new Upgrade(
      "Solar Banks",
      "Increases "+POWER+" output from covered tiles, especially in desert "+
      "environments.",
      100, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, Flora.class,
      10, ASSEMBLY, 5, FIELD_THEORY
    ),
    MOISTURE_FARMING = new Upgrade(
      "Moisture Farming",
      "Increases growth of all crops at the cost of "+WATER+" consumption, "+
      "especially in desert environments.",
      150, Upgrade.THREE_LEVELS, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, PROTEIN,
      10, CULTIVATION, 5, ASSEMBLY
    )
  ;
  
  final static int
    MIN_CLAIM_SIDE = BLUEPRINT.size + 0,
    MAX_CLAIM_SIDE = BLUEPRINT.size + 4;
  
  final public static Conversion
    LAND_TO_CARBS = new Conversion(
      BLUEPRINT, "land_to_carbs",
      10, HARD_LABOUR, 5, CULTIVATION, TO, 1, CARBS
    ),
    LAND_TO_POWER = new Conversion(
      BLUEPRINT, "land_to_power",
      TO, 2, POWER
    ),
    LAND_TO_GREENS = new Conversion(
      BLUEPRINT, "land_to_greens",
      10, HARD_LABOUR, 5, CULTIVATION, TO, 1, GREENS
    ),
    SAMPLE_EXTRACT = new Conversion(
      BLUEPRINT, "sample_extract",
      10, CULTIVATION, 5, BIOLOGY, TO, 1, GENE_SEED
    );
  
  
  
  public BotanicalStation(Base belongs) {
    super(BLUEPRINT, belongs, MIN_CLAIM_SIDE, MAX_CLAIM_SIDE);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(STATION_MODEL.makeSprite());
  }
  
  
  public BotanicalStation(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Claims and siting-
    */
  final static Siting SITING = new Siting(BLUEPRINT) {
    public float ratePointDemand(
      Base base, Target point, boolean exact, int claimRadius
    ) {
      final Stage world = point.world();
      final Tile  under = world.tileAt(point);
      
      float rating = super.ratePointDemand(base, point, exact, MAX_CLAIM_SIDE);
      rating *= world.terrain().fertilitySample(under) * 2;
      return rating;
    }
  };
  
  
  public boolean preventsClaimBy(Venue other) {
    return super.preventsClaimBy(other);
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    
    /*
    //
    //  If you're really short on food, consider foraging in the surrounds or
    //  farming, 24/7.
    final float shortages = (
      base.demands.primaryShortage(CARBS ) +
      base.demands.primaryShortage(GREENS)
    ) / 2f;
    if (shortages > 0.5f) choice.add(Gathering.asForaging(actor, this));
    //*/
    
    choice.add(nextHarvestFor(actor));
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 1, 5, 5, true
    );
    choice.add(d);
    choice.add(Gathering.asForestPlanting(actor, this));
    //
    //  Or, finally, fall back on supervising the venue...
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Decay any current seed stocks-
    final float needSeed = SeedTailoring.DESIRED_SAMPLES;
    final float decay = 1f / (
      Stage.STANDARD_DAY_LENGTH * SeedTailoring.SEED_DAYS_DECAY
    );
    for (Item seed : stocks.matches(GENE_SEED)) {
      stocks.removeItem(Item.withAmount(seed, decay));
    }
    for (Item sample : stocks.matches(SAMPLES)) {
      stocks.removeItem(Item.withAmount(sample, decay));
    }
    //
    //  Calculate need for power and water-
    final Vec2D middle = areaClaimed().centre();
    final Tile underMid = world.tileAt(middle.x, middle.y);
    float evapLevel = world().terrain().insolationSample(underMid) / 10;
    
    float powerOut = (reserved().length / 3f), waterIn = 0;
    powerOut *= 1 - this.needsTending;
    powerOut *= 1 + evapLevel;
    powerOut *= (structure.upgradeLevel(SOLAR_BANKS) + 1) / 2f;
    
    if (structure.hasUpgrade(MOISTURE_FARMING)) {
      waterIn = (reserved().length / 5f);
      waterIn *= 1 - this.needsTending;
      waterIn *= 0.5f + evapLevel;
    }
    //
    //  Update stocks-
    stocks.updateStockDemands(1, services());
    stocks.setConsumption(GENE_SEED, needSeed);
    stocks.forceDemand(POWER, 0, powerOut - 2);
    stocks.forceDemand(WATER, waterIn, 0     );
    //
    //  And update ambience-
    structure.setAmbienceVal(Ambience.MILD_AMBIENCE);
  }
  

  protected void checkTendStates() {
    super.checkTendStates();

    float numCarbs = 0, numGreens = 0;
    for (Tile t : reserved()) {
      final Crop c = plantedAt(t);
      if (c == null) continue;
      
      final float perDay = c.dailyYieldEstimate(t);
      final Item yield[] = c.materials();
      for (Item i : yield ) {
        if (i.type == CARBS ) numCarbs  += perDay * i.amount;
        if (i.type == GREENS) numGreens += perDay * i.amount;
      }
    }
    
    stocks.setDailyDemand(CARBS , 0, numCarbs );
    stocks.setDailyDemand(GREENS, 0, numGreens);
  }
  
  
  public float harvestMultiple(Target tended, Object type) {
    final Crop crop = (Crop) tended;
    float bonus = structure.upgradeLevel(MOISTURE_FARMING) / 2f;
    bonus *= 1 - stocks.relativeShortage(WATER, false);
    bonus *= crop.origin().habitat().insolation() / 10f;
    
    final Traded yield = crop.species().nutrients(0)[0].type;
    if (yield == CARBS) {
      bonus *= (structure.upgradeLevel(MONOCULTURE) + 1) / 2f;
    }
    if (yield == GREENS) {
      bonus *= (structure.upgradeLevel(GREENS     ) + 1) / 2f;
    }
    return bonus + 1;
  }
  
  
  public int numPositions(Background v) {
    final int level = structure.mainUpgradeLevel();
    if (v == CULTIVATOR) return level + 1;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] { GENE_SEED, GREENS, PROTEIN, CARBS };
  }
  
  
  public Background[] careers() {
    return new Background[] { CULTIVATOR };
  }
  
  
  public void addServices(Choice choice, Actor client) {
    choice.add(BringUtils.nextPersonalPurchase(client, this));
  }
  
  
  
  /**  Agricultural methods-
    */
  protected ClaimDivision updateDivision() {
    final ClaimDivision d = super.updateDivision();
    d.withUsageMarked(
      true, this,
      ClaimDivision.USE_NORMAL,
      ClaimDivision.USE_NORMAL,
      ClaimDivision.USE_SECONDARY
    );
    return d;
  }
  
  
  public boolean couldPlant(Tile t) {
    return claimDivision().useType(t) > 0;
  }
  
  
  public boolean shouldCover(Tile t) {
    return claimDivision().useType(t) != ClaimDivision.USE_NORMAL;
  }
  
  
  public Crop plantedAt(Tile t) {
    if (t == null || ! (t.above() instanceof Crop)) return null;
    return (Crop) t.above();
  }
  
  
  protected Gathering nextHarvestFor(Actor actor) {
    final Gathering g = Gathering.asFarming(actor, this);
    return needForTending(g) > 0 ? g : null;
  }
  
  
  public boolean needsTending(Tile t) {
    final Element e = ((Tile) t).above();
    if (! (e instanceof Crop)) return true;
    return ((Crop) e).needsTending();
  }
  
  
  public Tile[] getHarvestTiles(ResourceTending tending) {
    final Gathering g = (Gathering) tending;
    if (g.type == Gathering.TYPE_FARMING) {
      return super.getHarvestTiles(tending);
    }
    if (g.type == Gathering.TYPE_SAMPLE) {
      return Gathering.sampleFloraPoints(this, Stage.ZONE_SIZE);
    }
    return null;
  }
  
  
  public float needForTending(ResourceTending tending) {
    final Gathering g = (Gathering) tending;
    if (g.type == Gathering.TYPE_FARMING) {
      return super.needForTending(tending);
    }
    if (g.type == Gathering.TYPE_SAMPLE) {
      final float samples = SeedTailoring.numSamples(this);
      return 1 - (samples / SeedTailoring.DESIRED_SAMPLES);
    }
    if (g.type == Gathering.TYPE_FORAGING) {
      return 0;
    }
    return 0;
  }
}


