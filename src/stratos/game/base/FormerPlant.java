


package stratos.game.base;
import stratos.game.building.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.Composite;
import stratos.graphics.widgets.HUD;
import stratos.user.*;
import stratos.util.*;


//
//  TODO:  Get rid of the water and life support output, since I have that
//         covered by the Solar Array and Condensor?


public class FormerPlant extends Venue implements Economy {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  final static ModelAsset MODEL = CutoutModel.fromImage(
    FormerPlant.class, "media/Buildings/ecologist/air_processor.png", 3, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/air_processor_button.gif", FormerPlant.class
  );
  
  private static boolean verbose = false;
  
  
  protected List <DustCrawler> crawlers = new List <DustCrawler> ();
  protected float soilSamples = 0, monitorVal = 0;
  

  public FormerPlant(Base base) {
    super(3, 2, Venue.ENTRANCE_EAST, base);
    structure.setupStats(
      500, 15, 300,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    personnel.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public FormerPlant(Session s) throws Exception {
    super(s);
    s.loadObjects(crawlers);
    soilSamples = s.loadFloat();
    monitorVal = s.loadFloat();
    //I.say("Loaded...");
    //new Exception().printStackTrace();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(crawlers);
    s.saveFloat(soilSamples);
    s.saveFloat(monitorVal);
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    FormerPlant.class, "former_plant_upgrades"
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    
    CARBONS_CYCLING = new Upgrade(
      "Carbons Cycling",
      "Improves output of life support, speeds terraforming and reduces "+
      "pollution.",
      200,
      null, 1, null, ALL_UPGRADES
    ),
    
    EVAPORATION_CYCLING = new Upgrade(
      "Evaporation Cycling",
      "Increases efficiency around desert and oceans terrain, and increases "+
      "water output.",
      200,
      null, 1, null, ALL_UPGRADES
    ),
    
    DUST_PANNING = new Upgrade(
      "Dust Panning",
      "Permits modest output of metal ore and fuel cores, and installs "+
      "automated crawlers to gather soil samples.",
      150,
      null, 1, CARBONS_CYCLING, ALL_UPGRADES
    ),
    
    SPICE_REDUCTION = new Upgrade(
      "Spice Reduction",
      "Employs microbial cultures to capture minute quantities of spice from "+
      "the surrounding environment.",
      300,
      null, 1, EVAPORATION_CYCLING, ALL_UPGRADES
    );
  
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    //
    //  Consider upkeep, deliveries and supervision-
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null) choice.add(d);
    choice.add(new Repairs(actor, this));
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
    final int range = World.SECTOR_SIZE * 2;
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
    final boolean success = actor.traits.test(GEOPHYSICS, MODERATE_DC, 1);
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
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    final boolean report = verbose && I.talkAbout == this;
    
    if (report) I.say("\nUPDATING FORMER PLANT "+this);
    updateCrawlers();
    //
    //  Output the various goods, depending on terrain, supervision and upgrade
    //  levels-
    final float
      waterBonus  = 1 + structure.upgradeLevel(EVAPORATION_CYCLING),
      carbonBonus = 1 + structure.upgradeLevel(CARBONS_CYCLING),
      dustBonus   = 2 + structure.upgradeLevel(DUST_PANNING),
      spiceBonus  = structure.upgradeLevel(SPICE_REDUCTION) / 5f;
    final float
      SDL = World.STANDARD_DAY_LENGTH;
    
    int powerNeed = 4 + (structure.numUpgrades() * 2);
    stocks.incDemand(POWER, powerNeed, Stocks.TIER_CONSUMER, 1, this);
    stocks.bumpItem(POWER, powerNeed * -0.1f);
    float yield = 2 - stocks.shortagePenalty(POWER);
    
    if (report) I.say("  Basic yield is: "+yield);
    
    //
    //  Sample the local terrain and see if you get an extraction bonus-
    final Vec3D p = this.position(null);
    final Box2D area = new Box2D().set(p.x, p.y, 0, 0);
    area.expandBy(World.SECTOR_SIZE);
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
    final Actor mans = (Actor) Rand.pickFrom(personnel.workers());
    if (mans != null && mans.aboard() == this) {
      if (! mans.traits.test(GEOPHYSICS, SIMPLE_DC, 0.5f)) soilBonus /= 1.5f;
      if (mans.traits.test(GEOPHYSICS, DIFFICULT_DC, 0.5f)) soilBonus *= 1.5f;
    }
    else soilBonus /= 2;
    
    if (report) I.say("  Soil samples "+soilSamples+", bonus: "+soilBonus);
    
    //
    //  Here, we handle the lighter/more rarified biproducts-
    yield *= 1 + cycleBonus;
    if (report) I.say("  Yield/day with cycle bonus: "+yield);
    stocks.bumpItem(WATER, yield * (1 + waterBonus) * sumWater * 10 / SDL, 15);
    stocks.bumpItem(LIFE_SUPPORT, yield * carbonBonus * 100 / SDL, 15);
    //stocks.bumpItem(PETROCARBS, yield * carbonBonus / SDL, 15);
    
    //
    //  And here, the heavier elements-
    soilSamples = Visit.clamp(soilSamples - (10f / SDL), 0, 10);
    yield *= 1 + soilBonus;
    if (report) I.say("  Yield/day with soil bonus: "+yield);
    stocks.bumpItem(TRUE_SPICE, yield * spiceBonus / SDL, 10);
    stocks.bumpItem(METALS, yield * dustBonus / SDL, 10);
    stocks.bumpItem(FUEL_RODS, yield * dustBonus / SDL, 10);
    
    //
    //  In either cause, modify pollution and climate effects-
    //
    //  TODO:  Actually, arrange things so that the processor increases *local*
    //  pollution, while reducing global pollution (because it's messy and
    //  noisy, but good for the atmosphere.)  Not In My Backyard, IOW.
    structure.setAmbienceVal(-1 * yield);
    final int mag = World.SECTOR_SIZE;
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
  
  
  public Service[] services() {
    return new Service[] { METALS, FUEL_RODS, WATER, TRUE_SPICE };
  }
  
  
  
  /**  Rendering and interface-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
     0.0f, 0,
     0.5f, 0,
     1.0f, 0,
     1.5f, 0,
  };
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] { METALS, FUEL_RODS, TRUE_SPICE };
  }
  
  
  protected float goodDisplayAmount(Service good) {
    return Math.min(5, stocks.amountOf(good));
  }
  
  
  public String fullName() {
    return "Former Plant";
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
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST;
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    panel = super.configPanel(panel, UI);
    final Description d = panel.detail();
    if (panel.category() == CAT_STATUS) {
      d.append("\n\n  Soil Samples: "+(int) (soilSamples + 0.5f));
    }
    return panel;
  }
}









