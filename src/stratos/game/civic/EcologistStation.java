/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
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


public class EcologistStation extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    EcologistStation.class, "media/GUI/Buttons/nursery_button.gif"
  );
  final static ModelAsset
    STATION_MODEL = CutoutModel.fromImage(
      EcologistStation.class, IMG_DIR+"botanical_station.png", 4, 3
    );
  final static VenueProfile PROFILE = new VenueProfile(
    EcologistStation.class, "ecologist_station", "Ecologist Station",
    4, 3, Venue.Type.TYPE_STANDARD,
    NO_REQUIREMENTS, Owner.TIER_FACILITY
  );
  
  
  public EcologistStation(Base belongs) {
    super(PROFILE, belongs);
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(STATION_MODEL.makeSprite());
  }
  
  
  public EcologistStation(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Holding) return false;
    if (other.objectCategory() == this.objectCategory()) return false;
    return super.preventsClaimBy(other);
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    CEREAL_LAB = new Upgrade(
      "Cereal Lab",
      "Improves cereal yields.  Cereals yield more calories than other crop "+
      "species, but lack the full range of nutrients required in a healthy "+
      "diet.",
      100,
      Upgrade.THREE_LEVELS, CARBS,
      1,
      null, EcologistStation.class, ALL_UPGRADES
    ),
    BROADLEAF_LAB = new Upgrade(
      "Broadleaf Lab",
      "Improves broadleaf yields.  Broadleaves provide a wider range of "+
      "nutrients, and are valued as luxury exports, but their yield is small.",
      150,
      Upgrade.THREE_LEVELS, GREENS,
      1,
      null, EcologistStation.class, ALL_UPGRADES
    ),
    CULTIVATOR_STATION = new Upgrade(
      "Cultivator Station",
      "Hire additional cultivators to plant and reap harvests more "+
      "quickly, maintain equipment, and bring land under cultivation.",
      50,
      Upgrade.THREE_LEVELS, Backgrounds.CULTIVATOR, 1,
      null, EcologistStation.class, ALL_UPGRADES
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as providing carbons for plastic production.",
      100,
      Upgrade.THREE_LEVELS, null,
      1,
      BROADLEAF_LAB, EcologistStation.class, ALL_UPGRADES
    ),
    INSECTRY_LAB = new Upgrade(
      "Insectry Lab",
      "Many plantations cultivate colonies of social insects or other "+
      "invertebrates, both as a source of protein and pollination, pest "+
      "control, or recycling services.",
      150,
      Upgrade.THREE_LEVELS, PROTEIN,
      1,
      BROADLEAF_LAB, EcologistStation.class, ALL_UPGRADES
    ),
    ECOLOGIST_STATION = new Upgrade(
      "Ecologist Station",
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification, capable of adapting species to local climate conditions.",
      150,
      Upgrade.THREE_LEVELS, Backgrounds.ECOLOGIST,
      1,
      TREE_FARMING, EcologistStation.class, ALL_UPGRADES
    );
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! structure.intact()) return null;
    
    final Choice choice = new Choice(actor);
    //
    //  If you're really short on food, consider foraging in the surrounds or
    //  farming 24/7.
    final float shortages = (
      stocks.relativeShortage(CARBS  ) +
      stocks.relativeShortage(GREENS ) +
      stocks.relativeShortage(PROTEIN)
    ) / 3f;
    //
    //  First of all, find a suitable nursery to tend:
    for (Target t : world.presences.sampleFromMap(
      this, world, 5, null, Nursery.class
    )) {
      final Nursery n = (Nursery) t;
      if (n.base() != this.base()) continue;
      if (Plan.competition(Farming.class, n, actor) > 0) continue;
      
      final float urgency = shortages + n.needForTending();
      if (urgency > 0.5f || onShift) {
        final Farming farming = new Farming(actor, this, n);
        choice.add(farming);
      }
    }
    if (shortages > 0.5f) {
      final Foraging foraging = new Foraging(actor, this);
      foraging.addMotives(Plan.MOTIVE_EMERGENCY, Plan.PARAMOUNT * shortages);
      choice.add(foraging);
    }
    
    if (actor.vocation() == ECOLOGIST && onShift) {
      addEcologistJobs(actor, onShift, choice);
    }
    if (actor.vocation() == CULTIVATOR && onShift) {
      addCultivatorJobs(actor, onShift, choice);
    }
    return choice.weightedPick();
  }
  
  
  protected void addEcologistJobs(
    Actor actor, boolean onShift, Choice choice
  ) {
    //
    //  Consider collecting gene samples-
    final boolean needsSeed = stocks.amountOf(GENE_SEED) < 5;
    if (needsSeed) {
      choice.add(Forestry.nextSampling(actor, this));
    }
    for (Target e : actor.senses.awareOf()) if (e instanceof Fauna) {
      final Fauna f = (Fauna) e;
      final Item sample = Item.withReference(GENE_SEED, f.species());
      if (stocks.hasItem(sample)) continue;
      else choice.add(Hunting.asSample(actor, f, this));
    }
    //
    //  Tailor seed varieties and consider breeding animals-
    for (Species s : Crop.ALL_VARIETIES) {
      final SeedTailoring t = new SeedTailoring(actor, this, s);
      if (staff.assignedTo(t) > 0) continue;
      choice.add(t);
    }
    if (stocks.amountOf(CARBS) > 1 && stocks.amountOf(PROTEIN) > 0.5f) {
      choice.add(AnimalBreeding.nextBreeding(actor, this));
    }
    //
    //  Otherwise, consider exploring the surrounds-
    final Exploring x = Exploring.nextExploration(actor);
    if (x != null) choice.add(x.addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE));
    //
    //  Or, finally, fall back on supervising the venue...
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
  }
  
  
  protected void addCultivatorJobs(
    Actor actor, boolean onShift, Choice choice
  ) {
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 1, 5, 5
    );
    choice.add(d);
    if (choice.empty()) {
      //  In addition to forestry operations-
      choice.add(Forestry.nextPlanting(actor, this));
      choice.add(Forestry.nextCutting (actor, this));
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Increment demand for gene seed, and decay current stocks-
    stocks.incDemand(GENE_SEED, 5, 1, false);
    final float decay = 0.1f / Stage.STANDARD_DAY_LENGTH;
    for (Item seed : stocks.matches(GENE_SEED)) {
      stocks.removeItem(Item.withAmount(seed, decay));
    }
    for (Item seed : stocks.matches(SAMPLES)) {
      stocks.removeItem(Item.withAmount(seed, decay));
    }
    //
    //  Demand supplies, if breeding is going on-
    final int numBred = AnimalBreeding.breedingAt(this).size() + 1;
    stocks.incDemand(CARBS  , numBred * 2, 1, true);
    stocks.incDemand(PROTEIN, numBred * 1, 1, true);
    //
    //  And update demand for nursery-placement:
    final float nurseryDemand = structure.upgradeLevel(CULTIVATOR_STATION) + 1;
    base.demands.impingeDemand(Nursery.class, nurseryDemand, 1, this);
    //
    //  An update ambience-
    structure.setAmbienceVal(2);
  }
  
  
  public void onDestruction() {
    super.onDestruction();
  }
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == ECOLOGIST ) return num + 2;
    if (v == CULTIVATOR) return num + 2;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] { GENE_SEED };
  }
  
  
  public Background[] careers() {
    return new Background[] { ECOLOGIST, CULTIVATOR };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return null;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "botanical_station");
  }
  
  
  public String helpInfo() {
    return
      "Ecologist Stations are responsible for agriculture and forestry, "+
      "helping to secure food supplies and advance terraforming efforts.";
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_ECOLOGIST;
  }
}
