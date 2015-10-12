/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Demand reagents to perform gene tailoring?
//
//  Cereal Culture.  Flora Culture.    Canopy Culture.
//  Symbiote Lab.    Hydroponics.      Field Hand Station.
//  Zeno Pharma.     Animal Breeding.  Survival Training.


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
    );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    BotanicalStation.class, "botanical_station",
    "Botanical Station", Target.TYPE_ECOLOGIST, ICON,
    "Botanical Stations are responsible for agriculture and forestry, "+
    "helping to secure food supplies and advance terraforming efforts.",
    4, 2, Structure.IS_NORMAL | Structure.IS_ZONED,
    Owner.TIER_FACILITY, 150,
    3
  );
  
  final static int
    MIN_CLAIM_SIDE = BLUEPRINT.size + 2,
    MAX_CLAIM_SIDE = BLUEPRINT.size + 8;
  
  final public static Conversion
    LAND_TO_CARBS = new Conversion(
      BLUEPRINT, "land_to_carbs",
      10, HARD_LABOUR, 5, CULTIVATION, TO, 1, CARBS
    ),
    LAND_TO_GREENS = new Conversion(
      BLUEPRINT, "land_to_greens",
      10, HARD_LABOUR, 5, CULTIVATION, TO, 1, GREENS
    ),
    SAMPLE_EXTRACT = new Conversion(
      BLUEPRINT, "sample_extract",
      10, CULTIVATION, 5, GENE_CULTURE, TO, 1, GENE_SEED
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
    
    public float ratePointDemand(Base base, Target point, boolean exact) {
      final Stage world = point.world();
      final Tile under = world.tileAt(point);
      
      final Venue station = (Venue) world.presences.nearestMatch(
        BotanicalStation.class, point, -1
      );
      if (station == null || station.base() != base) return -1;
      final float distance = Spacing.distance(point, station);
      
      float rating = super.ratePointDemand(base, point, exact);
      rating *= world.terrain().fertilitySample(under) * 2;
      rating /= 1 + (distance / Stage.ZONE_SIZE);
      return rating;
    }
  };
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Holding) return false;
    if (other.objectCategory() == this.objectCategory()) return false;
    return super.preventsClaimBy(other);
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.THREE_LEVELS, null,
      new Object[] { 10, CULTIVATION, 0, XENOZOOLOGY },
      450,
      600,
      750
    ),
    MONOCULTURE = new Upgrade(
      "Monoculture",
      "Improves cereal yields, which provide "+CARBS+".  Cereals yield more "+
      "calories than other crops, but lack the nutrients for a complete diet.",
      100, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, CARBS,
      10, CULTIVATION
    ),
    FLORAL_CULTURE = new Upgrade(
      "Floral Culture",
      "Improves broadleaf yields, which provide "+GREENS+".  These are "+
      "valued as luxury exports, but their yield in calories is limited.",
      150, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, GREENS,
      10, CULTIVATION
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as permitting "+POLYMER+" production.",
      100, Upgrade.THREE_LEVELS, FLORAL_CULTURE, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, Flora.class,
      15, CULTIVATION
    ),
    SYMBIOTICS = new Upgrade(
      "Symbiotics",
      "Cultivates colonies of social insects as a source of "+PROTEIN+", and "+
      "assists in animal breeding programs.",
      150, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, PROTEIN,
      5, XENOZOOLOGY
    );
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  If you're really short on food, consider foraging in the surrounds or
    //  farming, 24/7.
    final boolean fieldHand = actor.mind.vocation() == CULTIVATOR;
    final float shortages = (
      base.commerce.primaryShortage(CARBS ) +
      base.commerce.primaryShortage(GREENS)
    ) / 2f;
    if (shortages >= 0.5f || fieldHand) {
      choice.add(nextHarvestFor(actor));
    }
    if (shortages > 0.5f && fieldHand) {
      choice.add(Gathering.asForaging(actor, this));
    }
    //
    //  Then add jobs specific to each vocation-
    if (fieldHand) {
      addCultivatorJobs(actor, choice);
    }
    else {
      addEcologistJobs(actor, choice);
    }
    return choice.weightedPick();
  }
  
  
  protected void addEcologistJobs(Actor actor, Choice choice) {
    //
    //  Consider collecting gene samples-
    choice.add(Gathering.asFloraSample(actor, this));
    //
    //  Consider performing research-
    //  TODO:  Decide on this.
    ///choice.add(Studying.asResearch(actor, this, Target.TYPE_ECOLOGIST));
    ///choice.isVerbose = true;
    //
    //  Tailor seed varieties and consider breeding animals or forestry-
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = Item.withReference(GENE_SEED, s);
      if (stocks.amountOf(seed) >= 1) continue;
      choice.add(new SeedTailoring(actor, this, s));
    }
    if (! choice.empty()) return;
    choice.add(Gathering.asForestPlanting(actor, this));
    //
    //  Otherwise, consider exploring the surrounds-
    final Exploring x = Exploring.nextExploration(actor);
    if (x != null) choice.add(x.addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE));
    //
    //  Or, finally, fall back on supervising the venue...
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
  }
  
  
  protected void addCultivatorJobs(Actor actor, Choice choice) {
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 1, 5, 5
    );
    choice.add(d);
    if (! choice.empty()) return;
    //
    //  Or, finally, fall back on supervising the venue...
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Increment demands, and decay current stocks-
    final float needSeed = SeedTailoring.DESIRED_SAMPLES;
    stocks.incDemand(GENE_SEED, needSeed, 1, false);
    stocks.incDemand(CARBS    , 5       , 1, true );
    stocks.incDemand(GREENS   , 5       , 1, true );
    stocks.incDemand(PROTEIN  , 5       , 1, true );
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
    //  An update ambience-
    structure.setAmbienceVal(Ambience.MILD_AMBIENCE);
  }
  
  
  public int numPositions(Background v) {
    final int level = structure.mainUpgradeLevel();
    if (v == CULTIVATOR) return level;
    if (v == ECOLOGIST ) return level;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] { GENE_SEED, GREENS, PROTEIN, CARBS };
  }
  
  
  public Background[] careers() {
    return new Background[] { ECOLOGIST, CULTIVATOR };
  }
  
  
  public void addServices(Choice choice, Actor client) {
    choice.add(BringUtils.nextHomePurchase(client, this));
  }
  
  
  
  /**  Agricultural methods-
    */
  protected ClaimDivision updateDivision() {
    final ClaimDivision d = super.updateDivision();
    return d.withUsageMarked(
      0.5f, true, false, this,
      ClaimDivision.USE_SECONDARY,
      ClaimDivision.USE_NORMAL
    );
  }
  
  
  public boolean couldPlant(Tile t) {
    return claimDivision().useType(t) > 0;
  }
  
  
  public boolean shouldCover(Tile t) {
    return claimDivision().useType(t) == 2;
  }
  
  
  public Crop plantedAt(Tile t) {
    if (t == null || ! (t.above() instanceof Crop)) return null;
    return (Crop) t.above();
  }
  
  
  protected Gathering nextHarvestFor(Actor actor) {
    final Gathering g = Gathering.asFarming(actor, this);
    return needForTending(g) > 0 ? g : null;
  }
  
  
  protected boolean needsTending(Tile t) {
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
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    POOR_SOILS_INFO =
      "The poor soils around this Station will hamper growth and yield a "+
      "stingy harvest.",
    WAITING_ON_SEED_INFO =
      "The land around this Station will have to be seeded by your "+
      ""+Backgrounds.CULTIVATOR+"s.",
    POOR_HEALTH_INFO =
      "The crops around this Station are sickly.  Try to improve seed stock "+
      "at the "+BotanicalStation.BLUEPRINT+".",
    AWAITING_GROWTH_INFO =
      "The crops around this Station have yet to mature.  Allow them a few "+
      "days to bear fruit.";
  
  private String compileOutputReport() {
    final StringBuffer s = new StringBuffer();
    final int numTiles = reserved().length;
    float
      health = 0, growth = 0, fertility = 0,
      numPlant = 0, numCarbs = 0, numGreens = 0;
    
    for (Tile t : reserved()) {
      final Crop c = plantedAt(t);
      fertility += t.habitat().moisture();
      if (c == null) continue;
      
      final float perDay = c.dailyYieldEstimate(t);
      final Item yield[] = c.materials();
      numPlant++;
      health += c.health   ();
      growth += c.growStage();
      for (Item i : yield ) {
        if (i.type == CARBS ) numCarbs  += perDay;
        if (i.type == GREENS) numGreens += perDay;
      }
    }
    
    if      (fertility < (numTiles * 0.5f)) s.append(POOR_SOILS_INFO     );
    else if (numPlant == 0                ) s.append(WAITING_ON_SEED_INFO);
    else if (health    < (numPlant * 0.5f)) s.append(POOR_HEALTH_INFO    );
    else if (growth    < (numPlant * 0.5f)) s.append(AWAITING_GROWTH_INFO);
    else s.append(BLUEPRINT.description);
    
    if (numCarbs  > 0) {
      s.append("\n  Estimated "+CARBS +" per day: "+I.shorten(numCarbs , 1));
    }
    if (numGreens > 0) {
      s.append("\n  Estimated "+GREENS+" per day: "+I.shorten(numGreens, 1));
    }
    return s.toString();
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return super.configSelectPane(panel, UI);
    //return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileOutputReport();
    else return super.helpInfo();
  }
}
