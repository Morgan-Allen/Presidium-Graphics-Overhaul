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
import stratos.game.wild.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Rename to Chem-Panner and use to harvest Spyce and Chemicals.  And
//  maybe carbons.  Yeah, okay.


public class ChemPanner extends Venue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  protected static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    ChemPanner.class, "media/GUI/Buttons/former_plant_button.gif"
  );
  final static ModelAsset
    MODEL = CutoutModel.fromImage(
      ChemPanner.class, IMG_DIR+"former_plant.png", 3, 1
    );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    ChemPanner.class, "chemical_pan",
    "Chem Panner", UIConstants.TYPE_ECOLOGIST, ICON,
    "The Chem Panner extracts rare or volatile elements from the local "+
    "environment for use in industry and pharmaceuticals.",
    4, 1, Structure.IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY,
    25,  //integrity
    5,  //armour
    75,  //build cost
    Structure.SMALL_MAX_UPGRADES
  );
  
  final static float
    MIN_CLAIM_SIZE = 4;
  
  
  private Box2D areaClaimed = new Box2D();
  
  
  public ChemPanner(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(MODEL);
  }
  
  
  public ChemPanner(Session s) throws Exception {
    super(s);
    areaClaimed.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    areaClaimed.saveTo(s.output());
  }
  
  
  
  /**  Placement and supply-demand functions-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    areaClaimed.setTo(footprint()).expandBy((int) MIN_CLAIM_SIZE);
    if (area != null) areaClaimed.include(area);
    return true;
  }
  
  
  public boolean canPlace(Account reasons) {
    if (! super.canPlace(reasons)) return false;
    if (areaClaimed.maxSide() > Stage.ZONE_SIZE) {
      return reasons.setFailure("Area is too large!");
    }
    final Stage world = origin().world;
    if (! SiteUtils.pathingOkayAround(this, areaClaimed, owningTier(), world)) {
      return reasons.setFailure("Might obstruct pathing");
    }
    return true;
  }
  
  
  public Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  private Item[] estimateDailyOutput() {
    float sumTrees = 0, sumP;
    for (Tile t : world.tilesIn(areaClaimed, true)) {
      if (t.above() instanceof Flora) {
        sumTrees += Flora.growChance(t);
      }
      sumTrees += t.habitat().moisture();
    }
    sumP = sumTrees * Forestry.GROW_STAGE_POLYMER / 2f;
    sumP *= Flora.MAX_GROWTH / Flora.MATURE_DURATION;
    return new Item[] { Item.withAmount(POLYMER, sumP) };
  }
  
  
  
  /**  Economic functions-
    */
  //  TODO:  Integrate these upgrades to improve efficiency here.
  /*
  final public static Upgrade
    
    AIR_PROCESSING = new Upgrade(
      "Carbons Cycling",
      "Improves output of life support, speeds terraforming and reduces "+
      "pollution.",
      200,
      Upgrade.THREE_LEVELS, null, 1, 
      null, FormerPlant.class
    ),
    RESINS_PRESS = null,
    
    BIOMASS_REACTOR = null,
    
    SPICE_REDUCTION = new Upgrade(
      "Spice Reduction",
      "Employs microbial culture to capture minute quantities of spice from "+
      "the surrounding environment.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      BIOMASS_REACTOR, FormerPlant.class
    );
  //*/
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(Ambience.MILD_SQUALOR);
    stocks.incDemand(POLYMER, 5, 1, true);
  }
  
  
  public Background[] careers () {
    return new Background[] { FORMER_ENGINEER };
  }
  
  
  protected int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == FORMER_ENGINEER) return nO + 2;
    return 0;
  }
  
  
  protected Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 1, 5, 5
    );
    if (d != null) return d;
    final Choice choice = new Choice(actor);
    
    Venue source = (EcologistStation) world.presences.nearestMatch(
      EcologistStation.class, this, Stage.ZONE_SIZE
    );
    if (source == null) source = this;
    choice.add(Forestry.nextPlanting(actor, source));
    choice.add(Forestry.nextCutting (actor, this  ));
    return choice.weightedPick();
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  
  
  public Traded[] services() { return new Traded[] { POLYMER }; }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  private String compileOutputReport() {
    final StringBuffer report = new StringBuffer();
    report.append(super.helpInfo());
    
    final Item out[] = estimateDailyOutput();
    for (Item i : out) {
      final String amount = I.shorten(i.amount, 1);
      report.append("\n  Estimated "+i.type+" per day: "+amount);
    }
    return report.toString();
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileOutputReport();
    else return super.helpInfo();
  }
}



//  SAVED FOR LATER REFERENCE-
/*
public class FormerPlant extends Venue {
  

  /**  Data fields, constructors and save/load methods-
    */
/*
  private static boolean
    verbose = false;
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    FormerPlant.class, "media/Buildings/ecologist/air_processor.png", 3, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    FormerPlant.class, "media/GUI/Buttons/air_processor_button.gif"
  );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    FormerPlant.class, "former_plant",
    "Former Plant", UIConstants.TYPE_ECOLOGIST,
    3, 2, IS_NORMAL,
    EcologistStation.BLUEPRINT, Owner.TIER_FACILITY
  );
  
  
  protected List <DustCrawler> crawlers = new List <DustCrawler> ();
  protected float soilSamples = 0, monitorVal = 0;
  
  
  public FormerPlant(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      500, 15, 300,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public FormerPlant(Session s) throws Exception {
    super(s);
    s.loadObjects(crawlers);
    soilSamples = s.loadFloat();
    monitorVal  = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(crawlers);
    s.saveFloat(soilSamples);
    s.saveFloat(monitorVal );
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
/*
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  //*
  final public static Upgrade
    
    CARBONS_CYCLING = new Upgrade(
      "Carbons Cycling",
      "Improves output of life support, speeds terraforming and reduces "+
      "pollution.",
      200,
      Upgrade.THREE_LEVELS, null, 1, 
      null, FormerPlant.class
    ),
    
    EVAPORATION_CYCLING = new Upgrade(
      "Evaporation Cycling",
      "Increases efficiency around desert and oceans terrain, and increases "+
      "water output.",
      200,
      Upgrade.THREE_LEVELS, null, 1,
      null, FormerPlant.class
    ),
    
    DUST_PANNING = new Upgrade(
      "Dust Panning",
      "Permits modest output of metal ore and fuel cores, and installs "+
      "automated crawlers to gather soil samples.",
      150,
      Upgrade.THREE_LEVELS, null, 1,
      CARBONS_CYCLING, FormerPlant.class
    ),
    
    SPICE_REDUCTION = new Upgrade(
      "Spice Reduction",
      "Employs microbial cultures to capture minute quantities of spice from "+
      "the surrounding environment.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      EVAPORATION_CYCLING, FormerPlant.class
    );
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    //
    //  Consider upkeep, deliveries and supervision-
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null) choice.add(d);
    //choice.add(new Repairs(actor, this));
    //
    //  Have the climate engineer gather soil samples, but only if they're
    //  very low.  (Automated crawlers would do this in bulk.)
    final Tile toSample = pickSample();
    if (soilSamples < 2 && toSample != null) {
      final Action actionSample = new Action(
        actor, toSample,
        this, "actionSoilSample",
        Action.BUILD, "Gathering soil samples"
      );
      actionSample.setProperties(Action.QUICK);
      actionSample.setPriority(Action.ROUTINE);
      choice.add(actionSample);
    }
    final float numSamples = actor.gear.amountOf(SAMPLES);
    if (numSamples > 0) {
      final Action returnSample = new Action(
        actor, this,
        this, "actionReturnSample",
        Action.LOOK, "Returning soil samples"
      );
      returnSample.setProperties(Action.QUICK);
      returnSample.setPriority(Action.ROUTINE + numSamples - 1);
      choice.add(returnSample);
    }
    //
    //  Select and return-
    return choice.weightedPick();
  }
  
  
  protected Tile pickSample() {
    final int range = Stage.ZONE_SIZE * 2;
    Tile picked = null;
    float bestRating = 0;
    for (int n = 10; n-- > 0;) {
      final Tile s = Spacing.pickRandomTile(this, range, world);
      if (s == null || s.pathType() != Tile.PATH_CLEAR) continue;
      float rating = s.habitat().minerals();
      rating /= 10 + Spacing.distance(s, this);
      if (rating > bestRating) { picked = s; bestRating = rating; }
    }
    return picked;
  }
  
  
  public boolean actionSoilSample(Actor actor, Tile spot) {
    final boolean success = actor.skills.test(GEOPHYSICS, MODERATE_DC, 1);
    for (int n = success ? (1 + Rand.index(3)) : 1; n-- > 0;) {
      final Item sample = Item.withReference(SAMPLES, spot);
      actor.gear.addItem(sample);
      return true;
    }
    return false;
  }
  
  
  public boolean actionReturnSample(Actor actor, FormerPlant works) {
    for (Item sample : actor.gear.matches(SAMPLES)) {
      final Tile t = (Tile) sample.refers;
      actor.gear.removeItem(sample);
      works.soilSamples += (t.habitat().minerals() / 10f) + 0.5f;
    }
    return true;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.FORMER_ENGINEER) {
      return nO + 1;
    }
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    final boolean report = verbose && I.talkAbout == this;
    
    if (report) I.say("\nUPDATING FORMER PLANT "+this);
    updateCrawlers();
    //
    //  Output the various goods, depending on terrain, supervision and upgrade
    //  levels-
    final float
      waterBonus  = 1 + structure.upgradeLevel(EVAPORATION_CYCLING),
      carbonBonus = 1 + structure.upgradeLevel(CARBONS_CYCLING    ),
      dustBonus   = 2 + structure.upgradeLevel(DUST_PANNING       ),
      spiceBonus  = 0 + structure.upgradeLevel(SPICE_REDUCTION    ) / 5f,
      SDL         = Stage.STANDARD_DAY_LENGTH;
    
    int powerNeed = 4 + (structure.numUpgrades() * 2);
    stocks.forceDemand(POWER, powerNeed, false);
    float yield = 2 - stocks.relativeShortage(POWER);
    
    if (report) I.say("  Basic yield is: "+yield);
    
    //
    //  Sample the local terrain and see if you get an extraction bonus-
    final Vec3D p = this.position(null);
    final Box2D area = new Box2D().set(p.x, p.y, 0, 0);
    area.expandBy(Stage.ZONE_SIZE);
    area.cropBy(new Box2D().set(0, 0, world.size - 1, world.size - 1));
    float sumWater = 0, sumDesert = 0;
    //
    //  We employ random sampling for efficiency (and lolz.)
    for (int n = 10; n-- > 0;) {
      final Tile sampled = world.tileAt(
        Rand.rangeAvg(area.xpos(), area.xmax(), 2),
        Rand.rangeAvg(area.ypos(), area.ymax(), 2)
      );
      final Habitat h = sampled.habitat();
      if (h == Habitat.OCEAN) sumWater += 10;
      else sumWater += h.moisture() / 2f;
      if (h == Habitat.DUNE) sumDesert += 10;
      else sumDesert += (10 - h.moisture()) / 2f;
    }
    sumWater /= 100;
    sumDesert /= 100;
    final float cycleBonus = (waterBonus * sumWater * sumDesert * 4);
    
    if (report) I.say(
      "  Water cycle bonus: "+cycleBonus+
      ", water/desert: "+sumWater+"/"+sumDesert
    );
    //
    //  Also, see if any soil samples have been collected lately.  (This bonus
    //  is higher if the venue is presently well-supervised.)
    float soilBonus = soilSamples / 5f;
    final Actor mans = (Actor) Rand.pickFrom(staff.workers());
    if (mans != null && mans.aboard() == this) {
      if (! mans.skills.test(GEOPHYSICS, SIMPLE_DC, 0.5f)) soilBonus /= 1.5f;
      if (mans.skills.test(GEOPHYSICS, DIFFICULT_DC, 0.5f)) soilBonus *= 1.5f;
    }
    else soilBonus /= 2;
    
    if (report) I.say("  Soil samples "+soilSamples+", bonus: "+soilBonus);
    
    //
    //  Here, we handle the lighter/more rarified biproducts-
    yield *= 1 + cycleBonus;
    
    //  TODO:  MODIFY OUTPUT OF WATER/LIFE-SUPPORT
    /*
    if (report) I.say("  Yield/day with cycle bonus: "+yield);
    stocks.bumpItem(WATER, yield * (1 + waterBonus) * sumWater * 10 / SDL, 15);
    stocks.bumpItem(LIFE_SUPPORT, yield * carbonBonus * 100 / SDL, 15);
    //stocks.bumpItem(PETROCARBS, yield * carbonBonus / SDL, 15);
    //*/
    /*
    //
    //  And here, the heavier elements-
    soilSamples = Nums.clamp(soilSamples - (10f / SDL), 0, 10);
    yield *= 1 + soilBonus;
    if (report) I.say("  Yield/day with soil bonus: "+yield);
    stocks.bumpItem(SPYCE_H, yield * spiceBonus / SDL, 10);
    stocks.bumpItem(METALS   , yield * dustBonus  / SDL, 10);
    stocks.bumpItem(ISOTOPES  , yield * dustBonus  / SDL, 10);
    
    //
    //  In either cause, modify pollution and climate effects-
    //
    //  TODO:  Actually, arrange things so that the processor increases *local*
    //  pollution, while reducing global pollution (because it's messy and
    //  noisy, but good for the atmosphere.)  Not In My Backyard, IOW.
    structure.setAmbienceVal(-1 * yield);
    final int mag = Stage.ZONE_SIZE;
    world.ecology().pushClimate(Habitat.MEADOW, mag * mag * 5 * yield);
  }
  
  
  //
  //  TODO:  Ensure vehicles are listed under staffing.
  protected void updateCrawlers() {
    final boolean report = verbose && I.talkAbout == this;
    //
    //  Cull all any destroyed crawlers-
    for (DustCrawler c : crawlers) if (c.destroyed()) {
      if (report) I.say(c+" was destroyed!");
      crawlers.remove(c);
    }
    //
    //  Update the proper number of automated crawlers.
    final int numCrawlers = (1 + structure.upgradeLevel(DUST_PANNING)) / 2;
    if (report) I.say("Proper no. of crawlers: "+numCrawlers);
    if (crawlers.size() < numCrawlers) {
      final DustCrawler crawler = new DustCrawler();
      crawler.enterWorldAt(this, world);
      crawler.goAboard(this, world);
      crawler.setHangar(this);
      crawlers.add(crawler);
    }
    if (crawlers.size() > numCrawlers) {
      for (DustCrawler c : crawlers) if (c.aboard() == this) {
        if (report) I.say("Too many crawlers.  Salvaging: "+c);
        //  TODO:  PERFORM ACTUAL CONSTRUCTION/SALVAGE
        c.setAsDestroyed();
        crawlers.remove(c);
        return;
      }
    }
  }
  
  
  //
  //  TODO:  Get rid of the former engineer.  Allow to operate completely
  //  independantly.
  public Background[] careers() {
    return new Background[] { Backgrounds.FORMER_ENGINEER };
  }
  
  
  public Traded[] services() {
    return new Traded[] { METALS, ISOTOPES, SPYCE_H };
  }
  
  
  
  /**  Rendering and interface-
    */
/*
  final static float GOOD_DISPLAY_OFFSETS[] = {
     0.0f, 0,
     0.5f, 0,
     1.0f, 0,
     1.5f, 0,
  };
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS;
  }
  
  /*
  protected Traded[] goodsToShow() {
    return new Traded[] { ORES, TOPES, SPYCE_H };
  }
  //*/
  /*
  
  protected float goodDisplayAmount(Traded good) {
    return Nums.min(5, stocks.amountOf(good));
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "former_plant");
  }
  
  
  public String helpInfo() {
    return
      "Former Plants can modify the content of your planet's atmosphere, "+
      "helping to speed terraforming efforts and extract rare or heavy "+
      "elements as an economic biproduct.";
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    panel = super.configPanel(panel, UI);
    final Description d = panel.detail();
    if (true) {
      d.append("\n\n  Soil Samples: "+(int) (soilSamples + 0.5f));
    }
    return panel;
  }
}










//*/


