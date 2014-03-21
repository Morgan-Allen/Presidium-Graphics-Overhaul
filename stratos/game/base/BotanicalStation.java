/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.planet.* ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;


//
//  TODO:  Get rid of the Petrocarbs production from forestry, since the Air
//  Processor already has that covered.  (That and mining of course.)


public class BotanicalStation extends Venue implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/" ;
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/nursery_button.gif", BotanicalStation.class
  );
  final static ModelAsset
    STATION_MODEL = CutoutModel.fromImage(
      BotanicalStation.class, IMG_DIR+"botanical_station.png", 4, 3
    ) ;
  
  
  
  final static int MAX_PLANT_RANGE = 16 ;
  
  final List <Plantation> allotments = new List <Plantation> () ;
  
  
  
  public BotanicalStation(Base belongs) {
    super(4, 3, Venue.ENTRANCE_SOUTH, belongs) ;
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(STATION_MODEL.makeSprite()) ;
  }
  
  
  public BotanicalStation(Session s) throws Exception {
    super(s) ;
    s.loadObjects(allotments) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(allotments) ;
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    BotanicalStation.class, "botanical_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    CEREAL_LAB = new Upgrade(
      "Cereal Lab",
      "Improves cereal yields.  Cereals yield more calories than other crop "+
      "species, but lack the full range of nutrients required in a healthy "+
      "diet.",
      100,
      CARBS, 1,
      null, ALL_UPGRADES
    ),
    BROADLEAF_LAB = new Upgrade(
      "Broadleaf Lab",
      "Improves broadleaf yields.  Broadleaves provide a wider range of "+
      "nutrients, and are valued as luxury exports, but their yield is small.",
      150,
      GREENS, 1,
      null, ALL_UPGRADES
    ),
    FIELD_HAND_STATION = new Upgrade(
      "Field Hand Station",
      "Hire additional field hands to plant and reap the harvest more "+
      "quickly, maintain equipment, and bring land under cultivation.",
      50,
      Background.CULTIVATOR, 1,
      null, ALL_UPGRADES
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as providing carbons for plastic production.",
      100,
      null, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    INSECTRY_LAB = new Upgrade(
      "Insectry Lab",
      "Many plantations cultivate colonies of social insects or other "+
      "invertebrates, both as a source of protein and pollination, pest "+
      "control, or recycling services.",
      150,
      PROTEIN, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    ECOLOGIST_STATION = new Upgrade(
      "Ecologist Station",
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification, capable of adapting species to local climate conditions.",
      150,
      Background.ECOLOGIST, 1,
      TREE_FARMING, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact() || Planet.isNight(world)) return null ;
    //
    //  If the harvest is really coming in, pitch in regardless-
    final Choice choice = new Choice(actor) ;
    final boolean needsSeed = stocks.amountOf(GENE_SEED) < 5 ;
    for (Plantation p : allotments) {
      if (p.needForTending() > 0.5f) choice.add(new Farming(actor, p)) ;
    }
    if (choice.size() > 0) return choice.pickMostUrgent() ;
    //
    //  Otherwise, perform deliveries and more casual work-
    if (! personnel.onShift(actor)) return null ;
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 10, world
    ) ;
    choice.add(d) ;
    //
    //  Forestry may have to be performed, depending on need for gene samples-
    final Forestry f = new Forestry(actor, this) ;
    if (needsSeed && actor.vocation() == Background.ECOLOGIST) {
      f.priorityMod += Plan.ROUTINE ;
      f.configureFor(Forestry.STAGE_SAMPLING) ;
    }
    else {
      f.priorityMod = structure.upgradeLevel(TREE_FARMING) ;
      f.configureFor(Forestry.STAGE_GET_SEED) ;
    }
    choice.add(f) ;
    //
    //  If you're really short on food, consider foraging in the surrounds-
    final float shortages =
      stocks.shortagePenalty(CARBS) +
      stocks.shortagePenalty(GREENS) ;
    if (actor.vocation() == Background.CULTIVATOR && shortages > 0) {
      final Foraging foraging = new Foraging(actor, this) ;
      foraging.priorityMod = Plan.CASUAL * shortages ;
      choice.add(foraging) ;
    }
    //
    //  And lower-priority tending and upkeep also gets an appearance-
    for (Plantation p : allotments) if (p.type == Plantation.TYPE_NURSERY) {
      for (Species s : Plantation.ALL_VARIETIES) {
        if (actor.vocation() == Background.ECOLOGIST) {
          final SeedTailoring t = new SeedTailoring(actor, p, s) ;
          if (personnel.assignedTo(t) > 0) continue ;
          choice.add(t) ;
        }
      }
      //
      //  TODO:  Do this for every crop type?
      choice.add(new Farming(actor, p)) ;
    }
    return choice.weightedPick() ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    updateAllotments(numUpdates) ;
    //
    //  Increment demand for gene seed, and decay current stocks-
    stocks.incDemand(GENE_SEED, 5, VenueStocks.TIER_CONSUMER, 1) ;
    final float decay = 0.1f / World.STANDARD_DAY_LENGTH ;
    for (Item seed : stocks.matches(GENE_SEED)) {
      stocks.removeItem(Item.withAmount(seed, decay)) ;
    }
    for (Item seed : stocks.matches(SAMPLES)) {
      stocks.removeItem(Item.withAmount(seed, decay)) ;
    }
    structure.setAmbienceVal(2) ;
  }
  
  
  //
  //  TODO:  You need to cover the case of de-commissioning aura structures.
  //  ...in fact, try to generalise that more robustly.
  public void onDecommission() {
    super.onDecommission() ;
  }
  
  
  public void onDestruction() {
    super.onDestruction() ;
  }


  protected void updateAllotments(int numUpdates) {
    //
    //  Then update the current set of allotments-
    if (numUpdates % 10 == 0) {
      final int STRIP_SIZE = 4 ;
      int numCovered = 0 ;
      //
      //  First of all, remove any missing allotments (and their siblings in
      //  the same strip.)
      for (Plantation p : allotments) {
        if (p.destroyed()) {
          allotments.remove(p) ;
          for (Plantation s : p.strip) if (s != p) {
            s.structure.setState(Structure.STATE_SALVAGE, -1) ;
          }
        }
        else if (p.type == Plantation.TYPE_COVERED) numCovered++ ;
      }
      //
      //  Then, calculate how many allotments one should have.
      int maxAllots = 3 + (structure.upgradeBonus(Background.CULTIVATOR) * 2) ;
      maxAllots *= STRIP_SIZE ;
      if (maxAllots > allotments.size()) {
        //
        //  If you have too few, try to find a place for more-
        final boolean covered = numCovered <= allotments.size() / 3 ;
        Plantation allots[] = Plantation.placeAllotment(
          this, covered ? STRIP_SIZE : STRIP_SIZE, covered
        ) ;
        if (allots != null) for (Plantation p : allots) {
          allotments.add(p) ;
        }
      }
      if (maxAllots + STRIP_SIZE < allotments.size()) {
        //
        //  And if you have too many, flag the least productive for salvage.
        float minRating = Float.POSITIVE_INFINITY ;
        Plantation toRemove[] = null ;
        for (Plantation p : allotments) {
          final float rating = Plantation.rateArea(p.strip, world) ;
          if (rating < minRating) { toRemove = p.strip ; minRating = rating ; }
        }
        if (toRemove != null) for (Plantation p : toRemove) {
          p.structure.setState(Structure.STATE_SALVAGE, -1) ;
        }
      }
    }
  }
  

  public int numOpenings(Background v) {
    int num = super.numOpenings(v) ;
    if (v == Background.CULTIVATOR) return num + 1 ;
    if (v == Background.ECOLOGIST ) return num + 1 ;
    return 0 ;
  }
  
  
  protected List <Plantation> allotments() {
    return allotments ;
  }
  
  
  protected float growBonus(Tile t, Species s, boolean natural) {
    final float pollution = t.world.ecology().ambience.valueAt(t) / -10f ;
    if (pollution > 0) return 0 ;
    final float hB = 1 - pollution ;
    float bonus = 1 ;
    
    if (s == Species.HIVE_GRUBS || s == Species.BLUE_VALVES) {
      bonus = Math.min(1, world.ecology().biomassRating(t)) ;
      if (natural) return hB * 2.0f * bonus ;
      return structure.upgradeBonus(PROTEIN) * 0.2f * bonus * hB ;
    }
    else bonus = Math.max(0, (t.habitat().moisture() - 5) / 5f) ;
    
    if (s == Species.DURWHEAT || s == Species.BROADFRUITS) {
      bonus = (1 - bonus) / 2f ;
    }
    if (s == Species.ONI_RICE || s == Species.DURWHEAT) {
      if (natural) return 1.0f * bonus * hB ;
      return (structure.upgradeBonus(CARBS) + 2) * 1.0f * bonus * hB ;
    }
    else {
      if (natural) return 0.5f * bonus * hB ;
      return (structure.upgradeBonus(GREENS) + 2) * 0.5f * bonus * hB ;
    }
  }
  
  
  public Service[] services() {
    return new Service[] { GREENS, PROTEIN, CARBS } ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.ECOLOGIST, Background.CULTIVATOR } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
     -0.0f, 0,
     -1.0f, 0,
     -2.0f, 0,
     -0.0f, 1,
  } ;
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] { GENE_SEED, GREENS, PROTEIN, CARBS } ;
  }
  
  protected float goodDisplayAmount(Service good) {
    if (good == GENE_SEED) return stocks.amountOf(good) > 0 ? 5 : 0 ;
    return super.goodDisplayAmount(good) ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    final Composite cached = Composite.fromCache("botanical_station");
    if (cached != null) return cached;
    return Composite.withImage(ICON, "botanical_station");
  }
  
  
  public String fullName() { return "Botanical Station" ; }
  
  
  public String helpInfo() {
    return
      "Botanical Stations are responsible for agriculture and forestry, "+
      "helping to secure food supplies and advance terraforming efforts." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}







